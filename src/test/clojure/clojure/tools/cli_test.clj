(ns clojure.tools.cli-test
  (:use [clojure.test]
        [clojure.tools.cli]))

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
                         ["--server" :default "10.0.1.10"]))))))

  (deftest should-apply-parse-fn
    (is (= {:names ["john" "jeff" "steve"]}
           (first (cli ["--names" "john,jeff,steve"]
                       ["--names" :parse-fn #(vec (.split % ","))])))))

  (testing "aliases"
    (deftest should-support-multiple-aliases
      (is (= {:server "localhost"}
             (first (cli ["-s" "localhost"]
                         ["-s" "--server"])))))

    (deftest should-use-last-alias-provided-as-name-in-map
      (is (= {:server "localhost"}
             (first (cli ["-s" "localhost"]
                         ["-s" "--server"]))))))

  (testing "required"
    (deftest should-succeed-when-provided
      (cli ["--server" "localhost"]
           ["--server" :required true]))

    (deftest should-raise-when-missing
      (is (thrown-with-msg? Exception #"server is a required argument"
            (cli []
                 ["--server" :required true])))))

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
        (is (= ["file" "-x" "other"] args))))))

(deftest all-together-now
  (let [[options args _] (cli ["-p" "8080"
                               "--no-verbose"
                               "--log-directory" "/tmp"
                               "--server" "localhost"
                               "filename"]
                              ["-p" "--port" :parse-fn #(Integer. %)]
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