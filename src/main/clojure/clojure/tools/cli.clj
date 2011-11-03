(ns ^{:author "Gareth Jones"}
  clojure.tools.cli
  (:use [clojure.string :only (replace)]
        [clojure.pprint :only (pprint cl-format)])
  (:refer-clojure :exclude [replace]))

(defn build-doc [{:keys [switches docs default]}]
  [(apply str (interpose ", " switches))
   (or (str default) "")
   (or docs "")])

(defn banner-for [specs]
  (println "Usage:")
  (println)
  (let [docs (into (map build-doc specs)
                   [["--------" "-------" "----"]
                    ["Switches" "Default" "Desc"]])
        max-cols (->> (for [d docs] (map count d))
                      (apply map (fn [& c] (apply vector c)))
                      (map #(apply max %)))
        vs (for [d docs]
             (mapcat (fn [& x] (apply vector x)) max-cols d))]
    (doseq [v vs]
      (cl-format true "~{ ~vA  ~vA  ~vA ~}" v)
      (prn))))

(defn name-for [k]
  (replace k #"^--no-|^--\[no-\]|^--|^-" ""))

(defn flag-for [^String v]
  (not (.startsWith v "--no-")))

(defn opt? [^String x]
  (.startsWith x "-"))

(defn flag? [^String x]
  (.startsWith x "--[no-]"))

(defn end-of-args? [x]
  (= "--" x))

(defn spec-for
  [arg specs]
  (->> specs
       (filter (fn [s]
                   (let [switches (set (s :switches))]
                     (contains? switches arg))))
       first))

(defn default-values-for
  [specs]
  (into {} (for [s specs] [(s :name) (s :default)])))

(defn apply-specs
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
         (throw (Exception. (str "'" opt "' is not a valid argument")))
         
         (and (opt? opt) (spec :flag))
         (recur (assoc options (spec :name) (flag-for opt))
                extra-args
                (rest args))

         (opt? opt)
         (recur (assoc options (spec :name) ((spec :parse-fn) (second args)))
                extra-args
                (drop 2 args))

         :default
         (recur options (conj extra-args (first args)) (rest args)))))))

(defn switches-for
  [switches flag]
  (-> (for [^String s switches]
        (cond
         (and flag (flag? s))            [(replace s #"\[no-\]" "no-") (replace s #"\[no-\]" "")]
         (and flag (.startsWith s "--")) [(replace s #"--" "--no-") s]
         :default                        [s]))
      flatten))

(defn generate-spec
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
            :default  (if flag false nil)
            :flag     flag}
           options)))

(defn cli
  [args & specs]
  (let [specs (map generate-spec specs)]
    (let [[options extra-args] (apply-specs specs args)
          banner  (with-out-str (banner-for specs))]
      [options extra-args banner])))

