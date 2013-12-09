(ns clojure.tools.cli
  {:author "Sung Pae"}
  (:require [clojure.string :as s]
            goog.string.format
            [goog.string :as gs]))

(defn- tokenize-args
  "Reduce arguments sequence into [opt-type opt ?optarg?] vectors and a vector
  of remaining arguments. Returns as [option-tokens remaining-args].

  Expands clumped short options like \"-abc\" into:
  [[:short-opt \"-a\"] [:short-opt \"-b\"] [:short-opt \"-c\"]]

  If \"-b\" were in the set of options that require arguments, \"-abc\" would
  then be interpreted as: [[:short-opt \"-a\"] [:short-opt \"-b\" \"c\"]]

  Long options with `=` are always parsed as option + optarg, even if nothing
  follows the `=` sign.

  If the :in-order flag is true, the first non-option, non-optarg argument
  stops options processing. This is useful for handling subcommand options."
  [required-set args & options]
  (let [{:keys [in-order]} options]
    (loop [opts [] argv [] [car & cdr] args]
      (if car
        (condp re-seq car
          ;; Double dash always ends options processing
          #"^--$" (recur opts (into argv cdr) [])
          ;; Long options with assignment always passes optarg, required or not
          #"^--.+=" (recur (conj opts (into [:long-opt] (s/split car #"=" 2)))
                           argv cdr)
          ;; Long options, consumes cdr head if needed
          #"^--" (let [[optarg cdr] (if (contains? required-set car)
                                      [(first cdr) (rest cdr)]
                                      [nil cdr])]
                   (recur (conj opts (into [:long-opt car] (if optarg [optarg] [])))
                          argv cdr))
          ;; Short options, expands clumped opts until an optarg is required
          #"^-." (let [[os cdr] (loop [os [] [c & cs] (rest car)]
                                  (let [o (str \- c)]
                                    (if (contains? required-set o)
                                      (if (seq cs)
                                        ;; Get optarg from rest of car
                                        [(conj os [:short-opt o (s/join cs)]) cdr]
                                        ;; Get optarg from head of cdr
                                        [(conj os [:short-opt o (first cdr)]) (rest cdr)])
                                      (if (seq cs)
                                        (recur (conj os [:short-opt o]) cs)
                                        [(conj os [:short-opt o]) cdr]))))]
                   (recur (into opts os) argv cdr))
          (if in-order
            (recur opts (into argv (cons car cdr)) [])
            (recur opts (conj argv car) cdr)))
        [opts argv]))))

(def ^{:private true} spec-keys
  [:id :short-opt :long-opt :required :desc :default :default-desc :parse-fn
   :assoc-fn :validate-fn :validate-msg])

(defn- compile-spec [spec]
  (let [sopt-lopt-desc (take-while #(or (string? %) (nil? %)) spec)
        spec-map (apply hash-map (drop (count sopt-lopt-desc) spec))
        [short-opt long-opt desc] sopt-lopt-desc
        long-opt (or long-opt (:long-opt spec-map))
        [long-opt req] (when long-opt
                         (rest (re-find #"^(--[^ =]+)(?:[ =](.*))?" long-opt)))
        id (when long-opt
             (keyword (subs long-opt 2)))
        [validate-fn validate-msg] (:validate spec-map)]
    (merge {:id id
            :short-opt short-opt
            :long-opt long-opt
            :required req
            :desc desc
            :validate-fn validate-fn
            :validate-msg validate-msg}
           (select-keys spec-map spec-keys))))

(defn- distinct?* [coll]
  (if (seq coll)
    (apply distinct? coll)
    true))

(defn- compile-option-specs
  "Map a sequence of option specification vectors to a sequence of:

  {:id           Keyword  ; :server
   :short-opt    String   ; \"-s\"
   :long-opt     String   ; \"--server\"
   :required     String   ; \"HOSTNAME\"
   :desc         String   ; \"Remote server\"
   :default      Object   ; #<Inet4Address example.com/93.184.216.119>
   :default-desc String   ; \"example.com\"
   :parse-fn     IFn      ; #(InetAddress/getByName %)
   :assoc-fn     IFn      ; assoc
   :validate-fn  IFn      ; (partial instance? Inet4Address)
   :validate-msg String   ; \"Must be an IPv4 host\"
   }

  :id defaults to the keywordized name of long-opt without leading dashes, but
  may be overridden in the option spec.

  The option spec entry `:validate [fn msg]` desugars into the two entries
  :validate-fn and :validate-msg.

  A :default entry will not be included in the compiled spec unless specified.

  An option spec may also be passed as a map containing the entries above,
  in which case that subset of the map is transferred directly to the result
  vector.

  An assertion error is thrown if any :id values are unset, or if there exist
  any duplicate :id, :short-opt, or :long-opt values."
  [specs]
  {:post [(every? (comp identity :id) %)
          (distinct?* (map :id %))
          (distinct?* (remove nil? (map :short-opt %)))
          (distinct?* (remove nil? (map :long-opt %)))]}
  (map (fn [spec]
         (if (map? spec)
           (select-keys spec spec-keys)
           (compile-spec spec)))
       specs))

(defn- default-option-map [specs]
  (reduce (fn [m s]
            (if (contains? s :default)
              (assoc m (:id s) (:default s))
              m))
          {} specs))

(defn- find-spec [specs opt-type opt]
  (first (filter #(= opt (opt-type %)) specs)))

(defn- pr-join [& xs]
  (pr-str (s/join \space xs)))

(defn- missing-required-error [opt example-required]
  (str "Missing required argument for " (pr-join opt example-required)))

(defn- parse-error [opt optarg msg]
  (str "Error while parsing option " (pr-join opt optarg) ": " msg))

(defn- validate-error [opt optarg msg]
  (str "Failed to validate " (pr-join opt optarg)
       (if msg (str ": " msg) "")))

(defn- validate [value spec opt optarg]
  (let [{:keys [validate-fn validate-msg]} spec]
    (if (or (nil? validate-fn)
            (try (validate-fn value) (catch js/Error _)))
      [value nil]
      [::error (validate-error opt optarg validate-msg)])))

(defn- parse-value [value spec opt optarg]
  (let [{:keys [parse-fn]} spec
        [value error] (if parse-fn
                        (try
                          [(parse-fn value) nil]
                          (catch js/Error e
                            [nil (parse-error opt optarg (str e))]))
                        [value nil])]
    (if error
      [::error error]
      (validate value spec opt optarg))))

(defn- parse-optarg [spec opt optarg]
  (let [{:keys [required]} spec]
    (if (and required (nil? optarg))
      [::error (missing-required-error opt required)]
      (parse-value (if required optarg true) spec opt optarg))))

(defn- parse-option-tokens
  "Reduce sequence of [opt-type opt ?optarg?] tokens into a map of
  {option-id value} merged over the default values in the option
  specifications.

  Unknown options, missing required arguments, option argument parsing
  exceptions, and validation failures are collected into a vector of error
  message strings.

  Returns [option-map error-messages-vector]."
  [specs tokens]
  (reduce
    (fn [[m errors] [opt-type opt optarg]]
      (if-let [spec (find-spec specs opt-type opt)]
        (let [[value error] (parse-optarg spec opt optarg)]
          (if-not (= value ::error)
            [((:assoc-fn spec assoc) m (:id spec) value) errors]
            [m (conj errors error)]))
        [m (conj errors (str "Unknown option: " (pr-str opt)))]))
    [(default-option-map specs) []] tokens))

(defn- make-summary-parts [all-boolean? specs]
  (let [{:keys [short-opt long-opt required default default-desc desc]} specs
        opt (cond (and short-opt long-opt) (str short-opt ", " long-opt)
                  long-opt (str "    " long-opt)
                  short-opt short-opt)
        [opt dd] (if required
                   [(str opt \space required)
                    (or default-desc (if default (str default) ""))]
                   [opt ""])]
    (if all-boolean?
      [opt (or desc "")]
      [opt dd (or desc "")])))

(defn- format-lines [lens parts]
  (let [fmt (case (count lens)
              2 "  %%-%ds  %%-%ds"
              3 "  %%-%ds  %%-%ds  %%-%ds")
        fmt (apply gs/format fmt lens)]
    (map #(s/trimr (apply gs/format fmt %)) parts)))

(defn summarize
  "Reduce options specs into a options summary for printing at a terminal."
  [specs]
  (let [all-boolean? (every? (comp not :required) specs)
        parts (map (partial make-summary-parts all-boolean?) specs)
        lens (apply map (fn [& cols] (apply max (map count cols))) parts)
        lines (format-lines lens parts)]
    (s/join \newline lines)))
