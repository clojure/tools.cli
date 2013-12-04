(ns clojure.tools.cli-test
  (:use [clojure.string :only [join split]]
        [clojure.test :only [deftest is testing]]
        [clojure.tools.cli :as cli :only [cli parse-opts]]))

(testing "syntax"
  (deftest should-handle-simple-strings
    (is (= {:host "localhost"}
           (first (cli ["--host" "localhost"]
                       ["--host"])))))

  (testing "booleans"
    (deftest should-handle-trues
      (is (= {:verbose true}
             (first (cli ["--verbose"]
                         ["--[no-]verbose"])))))
    (deftest should-handle-falses
      (is (= {:verbose false}
             (first (cli ["--no-verbose"]
                         ["--[no-]verbose"])))))

    (testing "explicit syntax"
      (is (= {:verbose true}
             (first (cli ["--verbose"]
                         ["--verbose" :flag true]))))
      (is (= {:verbose false}
             (first (cli ["--no-verbose"]
                         ["--verbose" :flag true]))))))

  (testing "default values"
    (deftest should-default-when-no-value
      (is (= {:server "10.0.1.10"}
             (first (cli []
                         ["--server" :default "10.0.1.10"])))))
    (deftest should-override-when-supplied
      (is (= {:server "127.0.0.1"}
             (first (cli ["--server" "127.0.0.1"]
                         ["--server" :default "10.0.1.10"])))))
    (deftest should-omit-key-when-no-default
      (is (= false
             (contains? (cli ["--server" "127.0.0.1"]
                             ["--server" :default "10.0.1.10"]
                             ["--names"])
                        :server)))))

  (deftest should-apply-parse-fn
    (is (= {:names ["john" "jeff" "steve"]}
           (first (cli ["--names" "john,jeff,steve"]
                       ["--names" :parse-fn #(vec (split % #","))])))))

  (testing "aliases"
    (deftest should-support-multiple-aliases
      (is (= {:server "localhost"}
             (first (cli ["-s" "localhost"]
                         ["-s" "--server"])))))

    (deftest should-use-last-alias-provided-as-name-in-map
      (is (= {:server "localhost"}
             (first (cli ["-s" "localhost"]
                         ["-s" "--server"]))))))

  (testing "merging args"
    (deftest should-merge-identical-arguments
      (let [assoc-fn (fn [previous key val]
                       (assoc previous key
                              (if-let [oldval (get previous key)]
                                (merge oldval val)
                                (hash-set val))))
            [options args _] (cli ["-p" "1" "--port" "2"]
                                  ["-p" "--port" "description"
                                   :assoc-fn assoc-fn
                                   :parse-fn #(Integer/parseInt %)])]
        (is (= {:port #{1 2}} options)))))

  (testing "extra arguments"
    (deftest should-provide-access-to-trailing-args
      (let [[options args _] (cli ["--foo" "bar" "a" "b" "c"]
                                  ["-f" "--foo"])]
        (is (= {:foo "bar"} options))
        (is (= ["a" "b" "c"] args))))

    (deftest should-work-with-trailing-boolean-args
      (let [[options args _] (cli ["--no-verbose" "some-file"]
                                  ["--[no-]verbose"])]
        (is (= {:verbose false}))
        (is (= ["some-file"] args))))

    (deftest should-accept-double-hyphen-as-end-of-args
      (let [[options args _] (cli ["--foo" "bar" "--verbose" "--" "file" "-x" "other"]
                                  ["--foo"]
                                  ["--[no-]verbose"])]
        (is (= {:foo "bar" :verbose true} options))
        (is (= ["file" "-x" "other"] args)))))

  (testing "description"
    (deftest should-be-able-to-supply-description
      (let [[options args banner]
            (cli ["-s" "localhost"]
                 "This program does something awesome."
                 ["-s" "--server" :description "Server name"])]
        (is (= {:server "localhost"} options))
        (is (empty? args))
        (is (re-find #"This program does something awesome" banner)))))

  (testing "handles GNU option parsing conventions"
    (deftest should-handle-gnu-option-parsing-conventions
      (is (= (take 2 (cli ["foo" "-abcp80" "bar" "--host=example.com"]
                          ["-a" "--alpha" :flag true]
                          ["-b" "--bravo" :flag true]
                          ["-c" "--charlie" :flag true]
                          ["-h" "--host" :flag false]
                          ["-p" "--port" "Port number"
                           :flag false :parse-fn #(Integer/parseInt %)]))
             [{:alpha true :bravo true :charlie true :port 80 :host "example.com"}
              ["foo" "bar"]])))))

(deftest all-together-now
  (let [[options args _] (cli ["-p" "8080"
                               "--no-verbose"
                               "--log-directory" "/tmp"
                               "--server" "localhost"
                               "filename"]
                              ["-p" "--port" :parse-fn #(Integer/parseInt %)]
                              ["--host" :default "localhost"]
                              ["--[no-]verbose" :default true]
                              ["--log-directory" :default "/some/path"]
                              ["--server"])]
    (is (= {:port 8080
            :host "localhost"
            :verbose false
            :log-directory "/tmp"
            :server "localhost"} options))
    (is (= ["filename"] args))))

(def tokenize-args
  #'cli/tokenize-args)

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

(def normalize-args
  #'cli/normalize-args)

(deftest test-normalize-args
  (testing "expands clumped short options"
    (is (= (normalize-args [] ["-abc" "foo"])
           ["-a" "-b" "-c" "foo"]))
    (is (= (normalize-args [{:switches ["-p"] :flag false}] ["-abcp80" "foo"])
           ["-a" "-b" "-c" "-p" "80" "foo"])))
  (testing "expands long options with assignment"
    (is (= (normalize-args [{:switches ["--port"] :flag false}] ["--port=80" "--noopt=" "foo"])
           ["--port" "80" "--noopt" "" "foo"])))
  (testing "preserves double dash"
    (is (= (normalize-args [] ["-ab" "--" "foo" "-c"])
           ["-a" "-b" "--" "foo" "-c"])))
  (testing "hoists all options and optargs to the front"
    (is (= (normalize-args
             [{:switches ["-x"] :flag false}
              {:switches ["-y"] :flag false}
              {:switches ["--zulu"] :flag false}]
             ["foo" "-axray" "bar" "-by" "yankee" "-c" "baz" "--zulu" "zebra"
              "--" "--full" "stop"])
           ["-a" "-x" "ray" "-b" "-y" "yankee" "-c" "--zulu" "zebra"
            "foo" "bar" "baz" "--" "--full" "stop"]))))

(def compile-option-specs
  #'cli/compile-option-specs)

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
    (is (thrown? AssertionError (compile-option-specs [["-a" :id nil]])))
    (is (thrown? AssertionError (compile-option-specs [["-a" "--alpha"] ["-b" :id :alpha]])))
    (is (thrown? AssertionError (compile-option-specs [{:id :a :short-opt "-a"}
                                                       {:id :b :short-opt "-a"}])))
    (is (thrown? AssertionError (compile-option-specs [{:id :alpha :long-opt "--alpha"}
                                                       {:id :beta :long-opt "--alpha"}]))))
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

(def parse-option-tokens
  #'cli/parse-option-tokens)

(defn has-error? [re coll]
  (seq (filter (partial re-seq re) coll)))

(deftest test-parse-option-tokens
  (testing "parses and validates option arguments"
    (let [specs (compile-option-specs
                  [["-p" "--port NUMBER"
                    :parse-fn #(Integer/parseInt %)
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

(def summarize
  #'cli/summarize)

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
                                              :parse-fn #(Integer/parseInt %)]]))
           {:alpha true :beta true :port (int 80)})))
  (testing "collects error messages into :errors"
    (let [specs [["-f" "--file PATH"
                  :validate [#(not= \/ (first %)) "Must be a relative path"]]
                 ["-p" "--port PORT"
                  :parse-fn #(Integer/parseInt %)
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
                                               (format "Usage: myprog [%s] arg1 arg2"
                                                       (join \| (map :long-opt specs))))))
           "Usage: myprog [--alpha|--beta] arg1 arg2"))))
