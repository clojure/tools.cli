(ns ^{:author "Gareth Jones"}
  clojure.tools.cli
  (:use [clojure.string :only (replace)]
        [clojure.pprint :only (pprint cl-format)])
  (:refer-clojure :exclude [replace]))

(defn build-doc [{:keys [switches docs options]}]
  [(apply str (interpose ", " switches))
   (or (str (options :default)) "")
   (if (options :required) "Yes" "No")
   (or docs "")])

(defn show-help [specs]
  (println "Usage:")
  (println)
  (let [docs (into (map build-doc specs)
                   [["--------" "-------" "--------" "----"]
                    ["Switches" "Default" "Required" "Desc"]])
        max-cols (->> (for [d docs] (map count d))
                      (apply map (fn [& c] (apply vector c)))
                      (map #(apply max %)))
        vs (for [d docs]
             (mapcat (fn [& x] (apply vector x)) max-cols d))]
    (doseq [v vs]
      (cl-format true "~{ ~vA  ~vA  ~vA  ~vA ~}" v)
      (prn))))

(defn print-and-fail [msg]
  (println msg)
  (System/exit 1))

(defn help-and-quit [specs]
  (show-help specs)
  (System/exit 0))

(defn name-for [k]
  (replace k #"^--no-|^--|^-" ""))

(defn flag-for [v]
  (not (.startsWith v "--no-")))

(defn opt? [x]
  (.startsWith x "-"))

(defn strip-parents [alias]
  (last (.split alias "--")))

(defn path-for [alias]
  (map keyword (.split alias "--")))

(defn parse-args [args]
  (into {}
        (map (fn [[k v]]
               (if (and (opt? k) (or (nil? v) (opt? v)))
                 [(name-for k) (flag-for k)]
                 [(name-for k) v]))
             (filter (fn [[k v]] (opt? k))
                     (partition-all 2 1 args)))))

(defn parse-spec [spec args]
  (let [{:keys [parse-fn aliases options path]} spec
        raw (->> (map #(args %) aliases)
                 (remove nil?)
                 (first))
        raw (if (nil? raw)
              (:default options)
              raw)]
    (if (and (nil? raw)
             (:required options))
      (print-and-fail (str (last aliases) " is a required parameter"))
      (try
        [path (parse-fn raw)]
        (catch Exception _
          (print-and-fail (str "could not parse " (last aliases) " with value of " raw)))))))

(defn optional
  "Generates a function for parsing optional arguments. Params is a
  vector containing string aliases for the argument (from less to more
  specific), a doc string, and optionally a default value prefixed
  by :default.

  Parse-fn is an optional parsing function for converting
  a string value into something more useful.

  Example:

  (optional [\"-p\" \"--port\"
             \"Listen for connections on this port\"
             :default 8080]
            #(Integer. %))"
  [params & [parse-fn]]
  (fn [parent args]
    (let [parse-fn (or parse-fn identity)
          options (apply hash-map (drop-while string? params))
          switches (->> (take-while #(and (string? %) (opt? %)) params)
                        (map #(str parent %)))
          aliases (map name-for switches)
          docs (first (filter #(and (string? %) (not (opt? %))) params))
          name (or (options :name)
                   (strip-parents (last aliases)))
          path (path-for (last aliases))]
      {:parse-fn parse-fn
       :options options
       :aliases aliases
       :switches switches
       :docs docs
       :name name
       :path path})))

(defn required
  "Generates a function for parsing required arguments. Takes same
  parameters as 'optional'. Not providing this argument to clargon
  will cause an error to be printed and program execution to be
  halted."
  [params & [parse-fn]]
  (optional (into params [:required true]) parse-fn))

(defn group
  "Generates a function for parsing a named group of 'optional' and
  'required' arguments."
  [name & spec-fns]
  (fn [parent args]
    (let [full-name (if (empty? parent)
                      name
                      (str parent name))]
      (map #(% full-name args) spec-fns))))

(defn cli
  "Takes a list of args from the command line and applies the spec-fns
  to generate a map of options.

  Spec-fns are calls to 'optional', 'required', and 'group'."
  [args & spec-fns]
  (let [args (parse-args args)
        specs (flatten (map #(% "" args) spec-fns))]
    (if (some #(contains? #{"h" "help"} %) (keys args))
      (help-and-quit specs)
      (reduce (fn [h [path value]] (assoc-in h path value))
              {} (map #(parse-spec % args) specs)))))
