(ns clojure.tools.cli-test (:require [clojure.tools.cli :as cli]
            #_(:cljs cemerick.cljs.test))
  (:use [clojure.string :only [join]]
        [clojure.tools.cli :only [parse-opts summarize]]
        ^:clj [clojure.test :only [deftest is testing]])
  #_(:cljs (:require-macros [cemerick.cljs.test :refer [deftest is testing]])))

;; Refer private vars
(def tokenize-args        ^:clj #'cli/tokenize-args        #_(:cljs cli/tokenize-args))
(def compile-option-specs ^:clj #'cli/compile-option-specs #_(:cljs cli/compile-option-specs))
(def parse-option-tokens  ^:clj #'cli/parse-option-tokens  #_(:cljs cli/parse-option-tokens))

(deftest test-tokenize-args
  (testing "expands clumped short options"
    (is (= (tokenize-args #{"-p"} ["-abcp80"])
           [[[:short-opt "-a"] [:short-opt "-b"] [:short-opt "-c"] [:short-opt "-p" "80"]] []])))
  (testing "detects arguments to long options"
    (is (= (tokenize-args #{"--port" "--host"} ["--port=80" "--host" "example.com"])
           [[[:long-opt "--port" "80"] [:long-opt "--host" "example.com"]] []]))
    (is (= (tokenize-args #{} ["--foo=bar" "--noopt="])
           [[[:long-opt "--foo" "bar"] [:long-opt "--noopt" ""]] []])))
  (testing "stops option processing on double dash"
    (is (= (tokenize-args #{} ["-a" "--" "-b"])
           [[[:short-opt "-a"]] ["-b"]])))
  (testing "finds trailing options unless :in-order is true"
    (is (= (tokenize-args #{} ["-a" "foo" "-b"])
           [[[:short-opt "-a"] [:short-opt "-b"]] ["foo"]]))
    (is (= (tokenize-args #{} ["-a" "foo" "-b"] :in-order true)
           [[[:short-opt "-a"]] ["foo" "-b"]]))))

(deftest test-compile-option-specs
  (testing "does not set values for :default unless specified"
    (is (= (map #(contains? % :default) (compile-option-specs
                                          [["-f" "--foo"]
                                           ["-b" "--bar=ARG" :default 0]]))
           [false true])))
  (testing "interprets first three string arguments as short-opt, long-opt=required, and desc"
    (is (= (map (juxt :short-opt :long-opt :required :desc)
                (compile-option-specs [["-a" :id :alpha]
                                       ["-b" "--beta"]
                                       [nil nil "DESC" :id :gamma]
                                       ["-f" "--foo=FOO" "desc"]]))
           [["-a" nil nil nil]
            ["-b" "--beta" nil nil]
            [nil nil nil "DESC"]
            ["-f" "--foo" "FOO" "desc"]])))
  (testing "throws AssertionError on unset :id or duplicate :id, :short-opt, :long-opt"
    (is (thrown? ^:clj AssertionError #_(:cljs js/Error)
                 (compile-option-specs [["-a" :id nil]])))
    (is (thrown? ^:clj AssertionError #_(:cljs js/Error)
                 (compile-option-specs [["-a" "--alpha"] ["-b" :id :alpha]])))
    (is (thrown? ^:clj AssertionError #_(:cljs js/Error)
                 (compile-option-specs [{:id :a :short-opt "-a"} {:id :b :short-opt "-a"}])))
    (is (thrown? ^:clj AssertionError #_(:cljs js/Error)
                 (compile-option-specs [{:id :alpha :long-opt "--alpha"} {:id :beta :long-opt "--alpha"}]))))
  (testing "desugars `--long-opt=value`"
    (is (= (map (juxt :id :long-opt :required)
                (compile-option-specs [[nil "--foo FOO"] [nil "--bar=BAR"]]))
           [[:foo "--foo" "FOO"]
            [:bar "--bar" "BAR"]])))
  (testing "desugars :validate [fn msg]"
    (is (= (map (juxt :validate-fn :validate-msg)
                (compile-option-specs
                  [[nil "--name NAME" :validate [seq "Must be present"]]]))
           [[seq "Must be present"]])))
  (testing "accepts maps as option specs without munging values"
    (is (= (compile-option-specs [{:id ::foo :short-opt "-f" :long-opt "--foo" :bad-key nil}])
           [{:id ::foo :short-opt "-f" :long-opt "--foo"}]))))

(defn has-error? [re coll]
  (seq (filter (partial re-seq re) coll)))

(defn parse-int [x]
  ^:clj (Integer/parseInt x)
  #_(:cljs (do (assert (re-seq #"^\d" x))
               (js/parseInt x))))

(deftest test-parse-option-tokens
  (testing "parses and validates option arguments"
    (let [specs (compile-option-specs
                  [["-p" "--port NUMBER"
                    :parse-fn parse-int
                    :validate [#(< 0 % 0x10000) "Must be between 0 and 65536"]]
                   ["-f" "--file PATH"
                    :validate [#(not= \/ (first %)) "Must be a relative path"]]
                   ["-q" "--quiet"
                    :id :verbose
                    :default true
                    :parse-fn not]])]
      (is (= (parse-option-tokens specs [[:long-opt "--port" "80"] [:short-opt "-q"]])
             [{:port (int 80) :verbose false} []]))
      (is (has-error? #"Unknown option"
                      (peek (parse-option-tokens specs [[:long-opt "--unrecognized"]]))))
      (is (has-error? #"Missing required"
                      (peek (parse-option-tokens specs [[:long-opt "--port"]]))))
      (is (has-error? #"Must be between"
                      (peek (parse-option-tokens specs [[:long-opt "--port" "0"]]))))
      (is (has-error? #"Error while parsing"
                      (peek (parse-option-tokens specs [[:long-opt "--port" "FOO"]]))))
      (is (has-error? #"Must be a relative path"
                      (peek (parse-option-tokens specs [[:long-opt "--file" "/foo"]]))))))
  (testing "merges values over default option map"
    (let [specs (compile-option-specs
                  [["-a" "--alpha"]
                   ["-b" "--beta" :default false]
                   ["-g" "--gamma=ARG"]
                   ["-d" "--delta=ARG" :default "DELTA"]])]
      (is (= (parse-option-tokens specs [])
             [{:beta false :delta "DELTA"} []]))
      (is (= (parse-option-tokens specs [[:short-opt "-a"]
                                         [:short-opt "-b"]
                                         [:short-opt "-g" "GAMMA"]
                                         [:short-opt "-d" "delta"]])
             [{:alpha true :beta true :gamma "GAMMA" :delta "delta"} []]))))
  (testing "associates :id and value with :assoc-fn"
    (let [specs (compile-option-specs
                  [["-a" "--alpha"
                    :default true
                    :assoc-fn (fn [m k v] (assoc m k (not v)))]
                   ["-v" "--verbose"
                    :default 0
                    :assoc-fn (fn [m k _] (assoc m k (inc (m k))))]])]
      (is (= (parse-option-tokens specs [])
             [{:alpha true :verbose 0} []]))
      (is (= (parse-option-tokens specs [[:short-opt "-a"]])
             [{:alpha false :verbose 0} []]))
      (is (= (parse-option-tokens specs [[:short-opt "-v"]
                                         [:short-opt "-v"]
                                         [:long-opt "--verbose"]])
             [{:alpha true :verbose 3} []])))))

(deftest test-summarize
  (testing "summarizes options"
    (is (= (summarize (compile-option-specs
                        [["-s" "--server HOST" "Upstream server"
                          :default :some-object-whose-string-representation-is-awful
                          :default-desc "example.com"]
                         ["-p" "--port=PORT" "Upstream port number"
                          :default 80]
                         ["-o" nil "Output file"
                          :id :output
                          :required "PATH"]
                         ["-v" nil "Verbosity level; may be specified more than once"
                          :id :verbose
                          :default 0]
                         [nil "--help"]]))
           (join \newline
                 ["  -s, --server HOST  example.com  Upstream server"
                  "  -p, --port PORT    80           Upstream port number"
                  "  -o PATH                         Output file"
                  "  -v                              Verbosity level; may be specified more than once"
                  "      --help"]))))
  (testing "does not print :default column when all options are boolean"
    (is (= (summarize (compile-option-specs [["-m" "--minimal" "A minimal option summary"]]))
           "  -m, --minimal  A minimal option summary"))))

(deftest test-parse-opts
  (testing "parses options to :options"
    (is (= (:options (parse-opts ["-abp80"] [["-a" "--alpha"]
                                             ["-b" "--beta"]
                                             ["-p" "--port PORT"
                                              :parse-fn parse-int]]))
           {:alpha true :beta true :port (int 80)})))
  (testing "collects error messages into :errors"
    (let [specs [["-f" "--file PATH"
                  :validate [#(not= \/ (first %)) "Must be a relative path"]]
                 ["-p" "--port PORT"
                  :parse-fn (fn [x]
                              ^:clj parse-int
                              #_(:cljs (do (assert (re-seq #"^\d" x)) (js/parseInt x))))
                  :validate [#(< 0 % 0x10000) "Must be between 0 and 65536"]]]
          errors (:errors (parse-opts ["-f" "/foo/bar" "-p0"] specs))]
      (is (has-error? #"Must be a relative path" errors))
      (is (has-error? #"Must be between 0 and 65536" errors))))
  (testing "collects unprocessed arguments into :arguments"
    (is (= (:arguments (parse-opts ["foo" "-a" "bar" "--" "-b" "baz"]
                                   [["-a" "--alpha"] ["-b" "--beta"]]))
           ["foo" "bar" "-b" "baz"])))
  (testing "provides an option summary at :summary"
    (is (re-seq #"-a\W+--alpha" (:summary (parse-opts [] [["-a" "--alpha"]])))))
  (testing "processes arguments in order if :in-order is true"
    (is (= (:arguments (parse-opts ["-a" "foo" "-b"]
                                   [["-a" "--alpha"] ["-b" "--beta"]]
                                   :in-order true))
           ["foo" "-b"])))
  (testing "accepts optional summary-fn for generating options summary"
    (is (= (:summary (parse-opts [] [["-a" "--alpha"] ["-b" "--beta"]]
                                 :summary-fn (fn [specs]
                                               (str "Usage: myprog ["
                                                    (join \| (map :long-opt specs))
                                                    "] arg1 arg2"))))
           "Usage: myprog [--alpha|--beta] arg1 arg2"))))

(comment
  ;; Chas Emerick's PhantomJS test runner
  (spit "target/runner.js"
        (slurp (clojure.java.io/resource "cemerick/cljs/test/runner.js")))

  ;; CLJS test runner; same as `lein cljsbuild test`
  (defn run-tests []
    (println
      (clojure.java.shell/sh
        "phantomjs"
        "target/runner.js"
        "target/cli_test.js")))
  )
