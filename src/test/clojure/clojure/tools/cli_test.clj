(ns clojure.tools.cli-test
  (:use [clojure.test]
        [clojure.tools.cli]))

(testing "syntax"
  (deftest should-handle-simple-strings
    (is (= {:host "localhost"
            :args []}
           (cli ["--host" "localhost"]
                ["--host"]))))

  (testing "booleans"
    (deftest should-handle-trues
      (is (= {:verbose true
              :args []}
             (cli ["--verbose"]
                  ["--[no-]verbose"]))))
    (deftest should-handle-falses
      (is (= {:verbose false
              :args []}
             (cli ["--no-verbose"]
                  ["--[no-]verbose"]))))
    
    (testing "explicit syntax"
      (is (= {:verbose true
              :args []}
             (cli ["--verbose"]
                  ["--verbose" :flag true])))
      (is (= {:verbose false
              :args []}
             (cli ["--no-verbose"]
                  ["--verbose" :flag true])))))

  (testing "default values"
    (deftest should-default-when-no-value
      (is (= {:server "10.0.1.10"
              :args []}
             (cli []
                  ["--server" :default "10.0.1.10"]))))
    (deftest should-override-when-supplied
      (is (= {:server "127.0.0.1"
              :args []}
             (cli ["--server" "127.0.0.1"]
                  ["--server" :default "10.0.1.10"])))))

  (deftest should-apply-parse-fn
    (is (= {:names ["john" "jeff" "steve"]
            :args []}
           (cli ["--names" "john,jeff,steve"]
                ["--names" :parse-fn #(vec (.split % ","))]))))

  (testing "aliases"
    (deftest should-support-multiple-aliases
      (is (= {:server "localhost"
              :args []}
             (cli ["-s" "localhost"]
                  ["-s" "--server"]))))

    (deftest should-use-last-alias-provided-as-name-in-map
      (is (= {:server "localhost"
              :args []}
             (cli ["-s" "localhost"]
                  ["-s" "--server"])))))

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
      (is (= {:foo "bar"
              :args ["a" "b" "c"]}
             (cli ["--foo" "bar" "a" "b" "c"]
                  ["-f" "--foo"]))))

    (deftest should-work-with-trailing-boolean-args
      (is (= {:verbose false
              :args ["some-file"]}
             (cli ["--no-verbose" "some-file"]
                  ["--[no-]verbose"]))))

    (deftest should-accept-double-hyphen-as-end-of-args
      (is (= {:foo "bar"
              :verbose true
              :args ["file" "-x" "other"]}
             (cli ["--foo" "bar" "--verbose" "--" "file" "-x" "other"]
                  ["--foo"]
                  ["--[no-]verbose"]))))))

(deftest all-together-now
  (is (= {:port 8080
          :host "localhost"
          :verbose false
          :log-directory "/tmp"
          :server "localhost"
          :args []}
         (cli ["-p" "8080"
               "--no-verbose"
               "--log-directory" "/tmp"
               "--server" "localhost"]
              ["-p" "--port" :parse-fn #(Integer. %)]
              ["--host" :default "localhost"]
              ["--[no-]verbose" :default true]
              ["--log-directory" :default "/some/path"]
              ["--server"]))))