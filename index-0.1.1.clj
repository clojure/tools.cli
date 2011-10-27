{:namespaces
 ({:source-url
   "https://github.com/clojure/tools.cli/blob/b08f1002bd2be33664a93150cfe41e1439a0b258/src/main/clojure/clojure/tools/cli.clj",
   :wiki-url
   "http://clojure.github.com/tools.cli/clojure.tools.cli-api.html",
   :name "clojure.tools.cli",
   :author "Gareth Jones",
   :doc nil}),
 :vars
 ({:arglists ([args & spec-fns]),
   :name "cli",
   :namespace "clojure.tools.cli",
   :source-url
   "https://github.com/clojure/tools.cli/blob/b08f1002bd2be33664a93150cfe41e1439a0b258/src/main/clojure/clojure/tools/cli.clj#L128",
   :raw-source-url
   "https://github.com/clojure/tools.cli/raw/b08f1002bd2be33664a93150cfe41e1439a0b258/src/main/clojure/clojure/tools/cli.clj",
   :wiki-url
   "http://clojure.github.com/tools.cli//clojure.tools.cli-api.html#clojure.tools.cli/cli",
   :doc
   "Takes a list of args from the command line and applies the spec-fns\nto generate a map of options.\n\nSpec-fns are calls to 'optional', 'required', and 'group'.",
   :var-type "function",
   :line 128,
   :file "src/main/clojure/clojure/tools/cli.clj"}
  {:arglists ([name & spec-fns]),
   :name "group",
   :namespace "clojure.tools.cli",
   :source-url
   "https://github.com/clojure/tools.cli/blob/b08f1002bd2be33664a93150cfe41e1439a0b258/src/main/clojure/clojure/tools/cli.clj#L118",
   :raw-source-url
   "https://github.com/clojure/tools.cli/raw/b08f1002bd2be33664a93150cfe41e1439a0b258/src/main/clojure/clojure/tools/cli.clj",
   :wiki-url
   "http://clojure.github.com/tools.cli//clojure.tools.cli-api.html#clojure.tools.cli/group",
   :doc
   "Generates a function for parsing a named group of 'optional' and\n'required' arguments.",
   :var-type "function",
   :line 118,
   :file "src/main/clojure/clojure/tools/cli.clj"}
  {:arglists ([params & [parse-fn]]),
   :name "optional",
   :namespace "clojure.tools.cli",
   :source-url
   "https://github.com/clojure/tools.cli/blob/b08f1002bd2be33664a93150cfe41e1439a0b258/src/main/clojure/clojure/tools/cli.clj#L76",
   :raw-source-url
   "https://github.com/clojure/tools.cli/raw/b08f1002bd2be33664a93150cfe41e1439a0b258/src/main/clojure/clojure/tools/cli.clj",
   :wiki-url
   "http://clojure.github.com/tools.cli//clojure.tools.cli-api.html#clojure.tools.cli/optional",
   :doc
   "Generates a function for parsing optional arguments. Params is a\nvector containing string aliases for the argument (from less to more\nspecific), a doc string, and optionally a default value prefixed\nby :default.\n\nParse-fn is an optional parsing function for converting\na string value into something more useful.\n\nExample:\n\n(optional [\"-p\" \"--port\"\n           \"Listen for connections on this port\"\n           :default 8080]\n          #(Integer. %))",
   :var-type "function",
   :line 76,
   :file "src/main/clojure/clojure/tools/cli.clj"}
  {:arglists ([params & [parse-fn]]),
   :name "required",
   :namespace "clojure.tools.cli",
   :source-url
   "https://github.com/clojure/tools.cli/blob/b08f1002bd2be33664a93150cfe41e1439a0b258/src/main/clojure/clojure/tools/cli.clj#L110",
   :raw-source-url
   "https://github.com/clojure/tools.cli/raw/b08f1002bd2be33664a93150cfe41e1439a0b258/src/main/clojure/clojure/tools/cli.clj",
   :wiki-url
   "http://clojure.github.com/tools.cli//clojure.tools.cli-api.html#clojure.tools.cli/required",
   :doc
   "Generates a function for parsing required arguments. Takes same\nparameters as 'optional'. Not providing this argument to clargon\nwill cause an error to be printed and program execution to be\nhalted.",
   :var-type "function",
   :line 110,
   :file "src/main/clojure/clojure/tools/cli.clj"})}
