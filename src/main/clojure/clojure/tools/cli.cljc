;   Copyright (c) Rich Hickey. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns ^{:author "Gareth Jones, Sung Pae, Sean Corfield"
      :doc "Tools for working with command line arguments."}
  clojure.tools.cli
  (:require [clojure.string :as s]
            #?(:cljs goog.string.format)))

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
  (let [{:keys [in-order]} (apply hash-map options)]
    (loop [opts [] argv [] [car & cdr] args]
      (if car
        (condp re-seq car
          ;; Double dash always ends options processing
          #"^--$" (recur opts (into argv cdr) [])
          ;; Long options with assignment always passes optarg, required or not
          #"^--\S+=" (recur (conj opts (into [:long-opt] (s/split car #"=" 2)))
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

(defn- make-format
  "Given a sequence of column widths, return a string suitable for use in
  format to print a sequences of strings in those columns."
  [lens]
  (s/join (map #(str "  %" (when-not (zero? %) (str "-" %)) "s") lens)))
;;
;; Legacy API
;;

(defn- build-doc [{:keys [switches docs default]}]
  [(apply str (interpose ", " switches))
   (or (str default) "")
   (or docs "")])

#?(:cljs
   ;; alias to Google Closure string format
   (defn format
     [fmt & args]
     (apply goog.string.format fmt args)))

(defn- banner-for [desc specs]
  (when desc
    (println desc)
    (println))
  (let [docs (into (map build-doc specs)
                   [["--------" "-------" "----"]
                    ["Switches" "Default" "Desc"]])
        max-cols (->> (for [d docs] (map count d))
                      (apply map (fn [& c] (apply vector c)))
                      (map #(apply max %)))
        vs (for [d docs]
             (mapcat (fn [& x] (apply vector x)) max-cols d))]
    (doseq [v vs]
      (let [fmt (make-format (take-nth 2 v))]
        (print (apply format fmt (take-nth 2 (rest v)))))
      (prn))))

(defn- name-for [k]
  (s/replace k #"^--no-|^--\[no-\]|^--|^-" ""))

(defn- flag-for [^String v]
  (not (s/starts-with? v "--no-")))

(defn- opt? [^String x]
  (s/starts-with? x "-"))

(defn- flag? [^String x]
  (s/starts-with? x "--[no-]"))

(defn- end-of-args? [x]
  (= "--" x))

(defn- spec-for
  [arg specs]
  (->> specs
       (filter (fn [s]
                   (let [switches (set (s :switches))]
                     (contains? switches arg))))
       first))

(defn- default-values-for
  [specs]
  (reduce (fn [m s]
            (if (contains? s :default)
              ((:assoc-fn s) m (:name s) (:default s))
              m))
          {} specs))

(defn- apply-specs
  [specs args]
  (loop [options    (default-values-for specs)
         extra-args []
         args       args]
    (if-not (seq args)
      [options extra-args]
      (let [opt  (first args)
            spec (spec-for opt specs)]
        (cond
         (end-of-args? opt)
         (recur options (into extra-args (vec (rest args))) nil)

         (and (opt? opt) (nil? spec))
         (throw #?(:clj (Exception. (str "'" opt "' is not a valid argument"))
                   :cljs (js/Error. (str "'" opt "' is not a valid argument"))))

         (and (opt? opt) (spec :flag))
         (recur ((spec :assoc-fn) options (spec :name) (flag-for opt))
                extra-args
                (rest args))

         (opt? opt)
         (recur ((spec :assoc-fn) options (spec :name) ((spec :parse-fn) (second args)))
                extra-args
                (drop 2 args))

         :else
         (recur options (conj extra-args (first args)) (rest args)))))))

(defn- switches-for
  [switches flag]
  (-> (for [^String s switches]
        (cond (and flag (flag? s))
              [(s/replace s #"\[no-\]" "no-") (s/replace s #"\[no-\]" "")]

              (and flag (s/starts-with? s "--"))
              [(s/replace s #"--" "--no-") s]

              :else
              [s]))
      flatten))

(defn- generate-spec
  [raw-spec]
  (let [[switches raw-spec] (split-with #(and (string? %) (opt? %)) raw-spec)
        [docs raw-spec]     (split-with string? raw-spec)
        options             (apply hash-map raw-spec)
        aliases             (map name-for switches)
        flag                (or (flag? (last switches)) (options :flag))]
    (merge {:switches (switches-for switches flag)
            :docs     (first docs)
            :aliases  (set aliases)
            :name     (keyword (last aliases))
            :parse-fn identity
            :assoc-fn assoc
            :flag     flag}
           (when flag {:default false})
           options)))

(defn- normalize-args
  "Rewrite arguments sequence into a normalized form that is parsable by cli."
  [specs args]
  (let [required-opts (->> specs
                           (filter (complement :flag))
                           (mapcat :switches)
                           (into #{}))
        ;; Preserve double-dash since this is a pre-processing step
        largs (take-while (partial not= "--") args)
        rargs (drop (count largs) args)
        [opts largs] (tokenize-args required-opts largs)]
    (concat (mapcat rest opts) largs rargs)))

(defn cli
  "THIS IS A LEGACY FUNCTION and may be deprecated in the future. Please use
  clojure.tools.cli/parse-opts in new applications.

  Parse the provided args using the given specs. Specs are vectors
  describing a command line argument. For example:

  [\"-p\" \"--port\" \"Port to listen on\" :default 3000 :parse-fn #(Integer/parseInt %)]

  First provide the switches (from least to most specific), then a doc
  string, and pairs of options.

  Valid options are :default, :parse-fn, and :flag. See
  https://github.com/clojure/tools.cli/wiki/Documentation-for-0.2.4 for more
  detailed examples.

  Returns a vector containing a map of the parsed arguments, a vector
  of extra arguments that did not match known switches, and a
  documentation banner to provide usage instructions."
  [args & specs]
  (let [[desc specs] (if (string? (first specs))
                       [(first specs) (rest specs)]
                       [nil specs])
        specs (map generate-spec specs)
        args (normalize-args specs args)
        [options extra-args] (apply-specs specs args)
        banner (with-out-str (banner-for desc specs))]
    [options extra-args banner]))

;;
;; New API
;;

(def ^{:private true} spec-keys
  [:id :short-opt :long-opt :required :desc :default :default-desc :default-fn
   :parse-fn :assoc-fn :update-fn :multi :post-validation
   :validate-fn :validate-msg :missing])

(defn- select-spec-keys
  "Select only known spec entries from map and warn the user about unknown
   entries at development time."
  [map]
  (when *assert*
    (let [unknown-keys (keys (apply dissoc map spec-keys))]
      (when (seq unknown-keys)
        (let [msg (str "Warning: The following options to parse-opts are unrecognized: "
                       (s/join ", " unknown-keys))]
          #?(:clj  (binding [*out* *err*] (println msg))
             :cljs (binding [*print-fn* *print-err-fn*] (println msg)))))))

  (select-keys map spec-keys))

(defn- compile-spec [spec]
  (let [sopt-lopt-desc (take-while #(or (string? %) (nil? %)) spec)
        spec-map (apply hash-map (drop (count sopt-lopt-desc) spec))
        [short-opt long-opt desc] sopt-lopt-desc
        long-opt (or long-opt (:long-opt spec-map))
        [long-opt req] (when long-opt
                         (rest (re-find #"^(--[^ =]+)(?:[ =](.*))?" long-opt)))
        id (when long-opt
             (keyword (nth (re-find #"^--(\[no-\])?(.*)" long-opt) 2)))
        validate (:validate spec-map)
        [validate-fn validate-msg] (when (seq validate)
                                     (->> (partition 2 2 (repeat nil) validate)
                                          (apply map vector)))]
    (merge {:id id
            :short-opt short-opt
            :long-opt long-opt
            :required req
            :desc desc
            :validate-fn validate-fn
            :validate-msg validate-msg}
           (select-spec-keys (dissoc spec-map :validate)))))

(defn- distinct?* [coll]
  (if (seq coll)
    (apply distinct? coll)
    true))

(defn- wrap-val [map key]
  (if (contains? map key)
    (update-in map [key] #(cond (nil? %) nil
                                (coll? %) %
                                :else [%]))
    map))

(defn- compile-option-specs
  "Map a sequence of option specification vectors to a sequence of:

  {:id           Keyword  ; :server
   :short-opt    String   ; \"-s\"
   :long-opt     String   ; \"--server\"
   :required     String   ; \"HOSTNAME\"
   :desc         String   ; \"Remote server\"
   :default      Object   ; #<Inet4Address example.com/93.184.216.119>
   :default-desc String   ; \"example.com\"
   :default-fn   IFn      ; (constantly 0)
   :parse-fn     IFn      ; #(InetAddress/getByName %)
   :assoc-fn     IFn      ; assoc
   :update-fn    IFn      ; identity
   :validate-fn  [IFn]    ; [#(instance? Inet4Address %)
                          ;  #(not (.isMulticastAddress %)]
   :validate-msg [String] ; [\"Must be an IPv4 host\"
                          ;  \"Must not be a multicast address\"]
                          ; can also be a function (of the invalid argument)
   :post-validation Boolean ; default false
   :missing      String   ; \"server must be specified\"
   }

  :id defaults to the keywordized name of long-opt without leading dashes, but
  may be overridden in the option spec.

  The option spec entry `:validate [fn msg ...]` desugars into the two vector
  entries :validate-fn and :validate-msg. Multiple pairs of validation
  functions and error messages may be provided.

  A :default(-fn) entry will not be included in the compiled spec unless
  specified. The :default is applied before options are parsed, the :default-fn
  is applied after options are parsed (only where an option was not specified,
  and is passed the whole options map as its single argument, so defaults can
  be computed from other options if needed).

  An option spec may also be passed as a map containing the entries above,
  in which case that subset of the map is transferred directly to the result
  vector.

  An assertion error is thrown if any :id values are unset, or if there exist
  any duplicate :id, :short-opt, or :long-opt values, or if both :assoc-fn and
  :update-fn are provided for any single option."
  [option-specs]
  {:post [(every? :id %)
          (distinct?* (map :id (filter :default %)))
          (distinct?* (map :id (filter :default-fn %)))
          (distinct?* (remove nil? (map :short-opt %)))
          (distinct?* (remove nil? (map :long-opt %)))
          (every? (comp not (partial every? identity))
                  (map (juxt :assoc-fn :update-fn) %))]}
  (map (fn [spec]
         (-> (if (map? spec)
               (select-spec-keys spec)
               (compile-spec spec))
             (wrap-val :validate-fn)
             (wrap-val :validate-msg)))
       option-specs))

(defn- default-option-map [specs default-key]
  (reduce (fn [m s]
            (if (contains? s default-key)
              (assoc m (:id s) (default-key s))
              m))
          {} specs))

(defn- missing-errors
  "Given specs, returns a map of spec id to error message if missing."
  [specs]
  (reduce (fn [m s]
            (if (:missing s)
              (assoc m (:id s) (:missing s))
              m))
          {} specs))

(defn- find-spec [specs opt-type opt]
  (first
   (filter
    (fn [spec]
      (when-let [spec-opt (get spec opt-type)]
        (let [flag-tail (second (re-find #"^--\[no-\](.*)" spec-opt))
              candidates (if flag-tail
                           #{(str "--" flag-tail) (str "--no-" flag-tail)}
                           #{spec-opt})]
          (contains? candidates opt))))
    specs)))

(defn- pr-join [& xs]
  (pr-str (s/join \space xs)))

(defn- missing-required-error [opt example-required]
  (str "Missing required argument for " (pr-join opt example-required)))

(defn- parse-error [opt optarg msg]
  (str "Error while parsing option " (pr-join opt optarg) ": " msg))

(defn- validation-error [value opt optarg msg]
  (str "Failed to validate " (pr-join opt optarg)
       (if msg (str ": " (if (string? msg) msg (msg value))) "")))

(defn- validate [value spec opt optarg]
  (let [{:keys [validate-fn validate-msg]} spec]
    (or (loop [[vfn & vfns] validate-fn [msg & msgs] validate-msg]
          (when vfn
            (if (try (vfn value) (catch #?(:clj Throwable :cljs :default) _))
              (recur vfns msgs)
              [::error (validation-error value opt optarg msg)])))
        [value nil])))

(defn- parse-value [value spec opt optarg]
  (let [{:keys [parse-fn]} spec
        [value error] (if parse-fn
                        (try
                          [(parse-fn value) nil]
                          (catch #?(:clj Throwable :cljs :default) e
                            [nil (parse-error opt optarg (str e))]))
                        [value nil])]
    (cond error
          [::error error]
          (:post-validation spec)
          [value nil]
          :else
          (validate value spec opt optarg))))

(defn- neg-flag? [spec opt]
  (and (:long-opt spec)
       (re-find #"^--\[no-\]" (:long-opt spec))
       (re-find #"^--no-" opt)))

(defn- parse-optarg [spec opt optarg]
  (let [{:keys [required]} spec]
    (if (and required (nil? optarg))
      [::error (missing-required-error opt required)]
      (let [value (if required
                    optarg
                    (not (neg-flag? spec opt)))]
        (parse-value value spec opt optarg)))))

(defn- parse-option-tokens
  "Reduce sequence of [opt-type opt ?optarg?] tokens into a map of
  {option-id value} merged over the default values in the option
  specifications.

  If the :no-defaults flag is true, only options specified in the tokens are
  included in the option-map.

  Unknown options, missing options, missing required arguments, option
  argument parsing exceptions, and validation failures are collected into
  a vector of error message strings.

  If the :strict flag is true, required arguments that match other options
  are treated as missing, instead of a literal value beginning with - or --.

  Returns [option-map error-messages-vector]."
  [specs tokens & options]
  (let [{:keys [no-defaults strict]} (apply hash-map options)
        defaults (default-option-map specs :default)
        default-fns (default-option-map specs :default-fn)
        requireds (missing-errors specs)]
    (-> (reduce
          (fn [[m ids errors] [opt-type opt optarg]]
            (if-let [spec (find-spec specs opt-type opt)]
              (let [[value error] (parse-optarg spec opt optarg)
                    id (:id spec)]
                (if-not (= value ::error)
                  (if (and strict
                           (or (find-spec specs :short-opt optarg)
                               (find-spec specs :long-opt optarg)))
                    [m ids (conj errors (missing-required-error opt (:required spec)))]
                    (let [m' (if-let [update-fn (:update-fn spec)]
                               (if (:multi spec)
                                 (update m id update-fn value)
                                 (update m id update-fn))
                               ((:assoc-fn spec assoc) m id value))]
                      (if (:post-validation spec)
                        (let [[value error] (validate (get m' id) spec opt optarg)]
                          (if (= value ::error)
                            [m ids (conj errors error)]
                            [m' (conj ids id) errors]))
                        [m' (conj ids id) errors])))
                  [m ids (conj errors error)]))
              [m ids (conj errors (str "Unknown option: " (pr-str opt)))]))
          [defaults [] []] tokens)
        (#(reduce
           (fn [[m ids errors] [id error]]
             (if (contains? m id)
               [m ids errors]
               [m ids (conj errors error)]))
           % requireds))
        (#(reduce
           (fn [[m ids errors] [id f]]
             (if (contains? (set ids) id)
               [m ids errors]
               [(assoc m id (f (first %))) ids errors]))
           % default-fns))
        (#(let [[m ids errors] %]
            (if no-defaults
              [(select-keys m ids) errors]
              [m errors]))))))

(defn ^{:added "0.3.0"} make-summary-part
  "Given a single compiled option spec, turn it into a formatted string,
  optionally with its default values if requested."
  [show-defaults? spec]
  (let [{:keys [short-opt long-opt required desc
                default default-desc default-fn]} spec
        opt (cond (and short-opt long-opt) (str short-opt ", " long-opt)
                  long-opt (str "    " long-opt)
                  short-opt short-opt)
        [opt dd] (if required
                   [(str opt \space required)
                    (or default-desc
                        (when (contains? spec :default)
                          (if (some? default)
                            (str default)
                            "nil"))
                        (when default-fn
                          "<computed>")
                        "")]
                   [opt ""])]
    (if show-defaults?
      [opt dd (or desc "")]
      [opt (or desc "")])))

(defn ^{:added "0.3.0"} format-lines
  "Format a sequence of summary parts into columns. lens is a sequence of
  lengths to use for parts. There are two sequences of lengths if we are
  not displaying defaults. There are three sequences of lengths if we
  are showing defaults."
  [lens parts]
  (let [fmt (make-format lens)]
    (map #(s/trimr (apply format fmt %)) parts)))

(defn- required-arguments [specs]
  (reduce
    (fn [s {:keys [required short-opt long-opt]}]
      (if required
        (into s (remove nil? [short-opt long-opt]))
        s))
    #{} specs))

(defn ^{:added "0.3.0"} summarize
  "Reduce options specs into a options summary for printing at a terminal.
  Note that the specs argument should be the compiled version. That effectively
  means that you shouldn't call summarize directly. When you call parse-opts
  you get back a :summary key which is the result of calling summarize (or
  your user-supplied :summary-fn option) on the compiled option specs."
  [specs]
  (if (seq specs)
    (let [show-defaults? (some #(and (:required %)
                                     (or (contains? % :default)
                                         (contains? % :default-fn))) specs)
          parts (map (partial make-summary-part show-defaults?) specs)
          lens (apply map (fn [& cols] (apply max (map count cols))) parts)
          lines (format-lines lens parts)]
      (s/join \newline lines))
    ""))

(defn ^{:added "0.3.2"} get-default-options
  "Extract the map of default options from a sequence of option vectors.

  As of 0.4.1, this also applies any :default-fn present."
  [option-specs]
  (let [specs (compile-option-specs option-specs)
        vals  (default-option-map specs :default)]
    (reduce (fn [m [id f]]
              (if (contains? m id)
                m
                (update-in m [id] (f vals))))
            vals
            (default-option-map specs :default-fn))))


(defn ^{:added "0.3.0"} parse-opts
  "Parse arguments sequence according to given option specifications and the
  GNU Program Argument Syntax Conventions:

    https://www.gnu.org/software/libc/manual/html_node/Argument-Syntax.html

  Option specifications are a sequence of vectors with the following format:

    [short-opt long-opt-with-required-description description
     :property value]

  The first three string parameters in an option spec are positional and
  optional, and may be nil in order to specify a later parameter.

  By default, options are toggles that default to nil, but the second string
  parameter may be used to specify that an option requires an argument.

    e.g. [\"-p\" \"--port PORT\"] specifies that --port requires an argument,
         of which PORT is a short description.

  The :property value pairs are optional and take precedence over the
  positional string arguments. The valid properties are:

    :id           The key for this option in the resulting option map. This
                  is normally set to the keywordized name of the long option
                  without the leading dashes.

                  Multiple option entries can share the same :id in order to
                  transform a value in different ways, but only one of these
                  option entries may contain a :default(-fn) entry.

                  This option is mandatory.

    :short-opt    The short format for this option, normally set by the first
                  positional string parameter: e.g. \"-p\". Must be unique.

    :long-opt     The long format for this option, normally set by the second
                  positional string parameter; e.g. \"--port\". Must be unique.

    :required     A description of the required argument for this option if
                  one is required; normally set in the second positional
                  string parameter after the long option: \"--port PORT\".

                  The absence of this entry indicates that the option is a
                  boolean toggle that is set to true when specified on the
                  command line.

    :desc         A optional short description of this option.

    :default      The default value of this option. If none is specified, the
                  resulting option map will not contain an entry for this
                  option unless set on the command line. Also see :default-fn
                  (below).

                  This default is applied before any arguments are parsed so
                  this is a good way to seed values for :assoc-fn or :update-fn
                  as well as the simplest way to provide defaults.

                  If you need to compute a default based on other command line
                  arguments, or you need to provide a default separate from the
                  seed for :assoc-fn or :update-fn, see :default-fn below.

    :default-desc An optional description of the default value. This should be
                  used when the string representation of the default value is
                  too ugly to be printed on the command line, or :default-fn
                  is used to compute the default.

    :default-fn   A function to compute the default value of this option, given
                  the whole, parsed option map as its one argument. If no
                  function is specified, the resulting option map will not
                  contain an entry for this option unless set on the command
                  line. Also see :default (above).

                  If both :default and :default-fn are provided, if the
                  argument is not provided on the command-line, :default-fn will
                  still be called (and can override :default).

    :parse-fn     A function that receives the required option argument and
                  returns the option value.

                  If this is a boolean option, parse-fn will receive the value
                  true. This may be used to invert the logic of this option:

                  [\"-q\" \"--quiet\"
                   :id :verbose
                   :default true
                   :parse-fn not]

    :assoc-fn     A function that receives the current option map, the current
                  option :id, and the current parsed option value, and returns
                  a new option map. The default is 'assoc'.

                  For non-idempotent options, where you need to compute a option
                  value based on the current value and a new value from the
                  command line. If you only need the the current value, consider
                  :update-fn (below).

                  You cannot specify both :assoc-fn and :update-fn for an
                  option.

    :update-fn    Without :multi true:

                  A function that receives just the existing parsed option value,
                  and returns a new option value, for each option :id present.
                  The default is 'identity'.

                  This may be used to create non-idempotent options where you
                  only need the current value, like setting a verbosity level by
                  specifying an option multiple times. (\"-vvv\" -> 3)

                  [\"-v\" \"--verbose\"
                   :default 0
                   :update-fn inc]

                  :default is applied first. If you wish to omit the :default
                  option value, use fnil in your :update-fn as follows:

                  [\"-v\" \"--verbose\"
                   :update-fn (fnil inc 0)]

                  With :multi true:

                  A function that receives both the existing parsed option value,
                  and the parsed option value from each instance of the option,
                  and returns a new option value, for each option :id present.
                  The :multi option is ignored if you do not specify :update-fn.

                  For non-idempotent options, where you need to compute a option
                  value based on the current value and a new value from the
                  command line. This can sometimes be easier than use :assoc-fn.

                  [\"-f\" \"--file NAME\"
                   :default []
                   :update-fn conj
                   :multi true]

                  :default is applied first. If you wish to omit the :default
                  option value, use fnil in your :update-fn as follows:

                  [\"-f\" \"--file NAME\"
                   :update-fn (fnil conj [])
                   :multi true]

                  Regardless of :multi, you cannot specify both :assoc-fn
                  and :update-fn for an option.

    :validate     A vector of [validate-fn validate-msg ...]. Multiple pairs
                  of validation functions and error messages may be provided.

    :validate-fn  A vector of functions that receives the parsed option value
                  and returns a falsy value or throws an exception when the
                  value is invalid. The validations are tried in the given
                  order.

    :validate-msg A vector of error messages corresponding to :validate-fn
                  that will be added to the :errors vector on validation
                  failure. Can be plain strings, or functions to be applied
                  to the (invalid) option argument to produce a string.

  parse-opts returns a map with four entries:

    {:options     The options map, keyed by :id, mapped to the parsed value
     :arguments   A vector of unprocessed arguments
     :summary     A string containing a minimal options summary
     :errors      A possible vector of error message strings generated during
                  parsing; nil when no errors exist}

  A few function options may be specified to influence the behavior of
  parse-opts:

    :in-order     Stop option processing at the first unknown argument. Useful
                  for building programs with subcommands that have their own
                  option specs.

    :no-defaults  Only include option values specified in arguments and do not
                  include any default values in the resulting options map.
                  Useful for parsing options from multiple sources; i.e. from a
                  config file and from the command line.

    :strict       Parse required arguments strictly: if a required argument value
                  matches any other option, it is considered to be missing (and
                  you have a parse error).

    :summary-fn   A function that receives the sequence of compiled option specs
                  (documented at #'clojure.tools.cli/compile-option-specs), and
                  returns a custom option summary string.
  "
  [args option-specs & options]
  (let [{:keys [in-order no-defaults strict summary-fn]} (apply hash-map options)
        specs (compile-option-specs option-specs)
        req (required-arguments specs)
        [tokens rest-args] (tokenize-args req args :in-order in-order)
        [opts errors] (parse-option-tokens specs tokens
                                           :no-defaults no-defaults :strict strict)]
    {:options opts
     :arguments rest-args
     :summary ((or summary-fn summarize) specs)
     :errors (when (seq errors) errors)}))
