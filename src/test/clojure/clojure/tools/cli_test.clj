(ns clojure.tools.cli-test
  (:use [clojure.tools.cli] :reload)
  (:use [clojure.test]))

(testing "help"

  (deftest should-print-help-shortform
    (let [help-called (atom nil)]
      (binding [help-and-quit (partial reset! help-called)]
        (clargon '("-h") (optional ["--port"]))
        (is (not (nil? @help-called))))))

  (deftest should-print-help-longform
    (let [help-called (atom nil)]
      (binding [help-and-quit (partial reset! help-called)]
        (clargon '("--help") (optional ["--port"]))
        (is (not (nil? @help-called)))))))

(testing "syntax"

  (deftest should-handle-simple-strings
    (is (= {:host "localhost"}
           (clargon '("--host" "localhost") (optional ["--host"])))))

  (testing "booleans"
    (deftest should-handle-trues
      (is (= {:verbose true}
             (clargon '("--verbose") (optional ["--verbose"])))))

    (deftest should-handle-falses
      (is (= {:verbose false}
             (clargon '("--no-verbose") (optional ["--verbose"]))))))

  (testing "default values"
    (deftest should-default-when-no-value
      (is (= {:server "10.0.1.10"}
             (clargon '() (optional ["--server" :default "10.0.1.10"])))))
    (deftest should-override-when-supplied
      (is (= {:server "127.0.0.1"}
             (clargon '("--server" "127.0.0.1") (optional ["--server" :default "10.0.1.10"]))))))

  (deftest should-apply-parse-fn
    (is (= {:names ["john" "jeff" "steve"]}
           (clargon '("--names" "john,jeff,steve")
                    (optional ["--names"] #(vec (.split % ",")))))))

  (testing "aliases"
    (deftest should-support-multiple-aliases
      (is (= {:server "localhost"}
             (clargon '("-s" "localhost")
                      (optional ["-s" "--server"])))))
    (deftest should-use-last-alias-provided-as-name-in-map
      (is (= {:sizzle "localhost"}
             (clargon '("-s" "localhost")
                      (optional ["-s" "--server" "--sizzle"]))))))

  (testing "required"
    (deftest should-succeed-when-provided
      (clargon '("--server" "localhost")
               (required ["--server"])))
    (deftest should-fail-when-not-provided
      (try
        (binding [print-and-fail (fn [x] (throw (Exception. "success!")))]
          (is (thrown-with-msg? Exception #"success!" 
                (clargon '() (required ["--server"]))))))))

  (testing "grouped parameters"
    (deftest should-support-groups
      (is (= {:server {:name "localhost"
                       :port 9090}}
             (clargon '("--server--name" "localhost" "--server--port" "9090")
                      (group "--server"
                             (optional ["--name"])
                             (optional ["--port"] #(Integer. %)))))))
    (deftest should-support-nested-groups
      (is (= {:servers {:client {:host {:name "localhost" :port 1234}}}}
             (clargon '("--servers--client--host--name" "localhost")
                      (group "--servers"
                             (group "--client"
                                    (group "--host"
                                           (optional ["--name"])
                                           (optional ["--port" :default 1234]))))))))))

(deftest all-together-now
  (is (= {:port 8080
          :host "localhost"
          :verbose false
          :log-directory "/tmp"
          :server {:name "localhost"
                   :port 9090
                   :paths {:inbound "/dev/null"
                           :outbound "/tmp/outbound"}}}
         (clargon '("-p" "8080"
                    "--no-verbose"
                    "--log-directory" "/tmp"
                    "--server--name" "localhost"
                    "--server--port" "9090"
                    "--server--paths--inbound" "/dev/null")
                  (required ["-p" "--port"] #(Integer. %))
                  (optional ["--host" :default "localhost"])
                  (optional ["--verbose" :default true])
                  (optional ["--log-directory" :default "/some/path"])
                  (group "--server"
                         (optional ["-n" "--name"])
                         (optional ["-p" "--port"] #(Integer. %))
                         (group "--paths"
                                (optional ["--inbound" :default "/tmp/inbound"])
                                (optional ["--outbound" :default "/tmp/outbound"])))))))




