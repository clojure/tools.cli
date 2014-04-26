{:namespaces
 ({:source-url
   "https://github.com/clojure/tools.cli/blob/ac2c5cc24eff7662c19e9f79c1cec4bfeebe15f7/src/main/clojure/clojure/tools/cli.clj",
   :wiki-url
   "http://clojure.github.com/tools.cli/clojure.tools.cli-api.html",
   :name "clojure.tools.cli",
   :author "Gareth Jones, Sung Pae",
   :doc "Tools for working with command line arguments."}),
 :vars
 ({:arglists ([args & specs]),
   :name "cli",
   :namespace "clojure.tools.cli",
   :source-url
   "https://github.com/clojure/tools.cli/blob/ac2c5cc24eff7662c19e9f79c1cec4bfeebe15f7/src/main/clojure/clojure/tools/cli.clj#L180",
   :raw-source-url
   "https://github.com/clojure/tools.cli/raw/ac2c5cc24eff7662c19e9f79c1cec4bfeebe15f7/src/main/clojure/clojure/tools/cli.clj",
   :wiki-url
   "http://clojure.github.com/tools.cli//clojure.tools.cli-api.html#clojure.tools.cli/cli",
   :doc
   "THIS IS A LEGACY FUNCTION and may be deprecated in the future. Please use\nclojure.tools.cli/parse-opts in new applications.\n\nParse the provided args using the given specs. Specs are vectors\ndescribing a command line argument. For example:\n\n[\"-p\" \"--port\" \"Port to listen on\" :default 3000 :parse-fn #(Integer/parseInt %)]\n\nFirst provide the switches (from least to most specific), then a doc\nstring, and pairs of options.\n\nValid options are :default, :parse-fn, and :flag. See\nhttps://github.com/clojure/tools.cli/wiki/Documentation-for-0.2.4 for more\ndetailed examples.\n\nReturns a vector containing a map of the parsed arguments, a vector\nof extra arguments that did not match known switches, and a\ndocumentation banner to provide usage instructions.",
   :var-type "function",
   :line 180,
   :file "src/main/clojure/clojure/tools/cli.clj"}
  {:arglists ([option-specs]),
   :name "get-default-options",
   :namespace "clojure.tools.cli",
   :source-url
   "https://github.com/clojure/tools.cli/blob/ac2c5cc24eff7662c19e9f79c1cec4bfeebe15f7/src/main/clojure/clojure/tools/cli.clj#L430",
   :added "0.3.2",
   :raw-source-url
   "https://github.com/clojure/tools.cli/raw/ac2c5cc24eff7662c19e9f79c1cec4bfeebe15f7/src/main/clojure/clojure/tools/cli.clj",
   :wiki-url
   "http://clojure.github.com/tools.cli//clojure.tools.cli-api.html#clojure.tools.cli/get-default-options",
   :doc
   "Extract the map of default options from a sequence of option vectors.",
   :var-type "function",
   :line 430,
   :file "src/main/clojure/clojure/tools/cli.clj"}
  {:arglists ([args option-specs & options]),
   :name "parse-opts",
   :namespace "clojure.tools.cli",
   :source-url
   "https://github.com/clojure/tools.cli/blob/ac2c5cc24eff7662c19e9f79c1cec4bfeebe15f7/src/main/clojure/clojure/tools/cli.clj#L435",
   :added "0.3.0",
   :raw-source-url
   "https://github.com/clojure/tools.cli/raw/ac2c5cc24eff7662c19e9f79c1cec4bfeebe15f7/src/main/clojure/clojure/tools/cli.clj",
   :wiki-url
   "http://clojure.github.com/tools.cli//clojure.tools.cli-api.html#clojure.tools.cli/parse-opts",
   :doc
   "Parse arguments sequence according to given option specifications and the\nGNU Program Argument Syntax Conventions:\n\n  https://www.gnu.org/software/libc/manual/html_node/Argument-Syntax.html\n\nOption specifications are a sequence of vectors with the following format:\n\n  [short-opt long-opt-with-required-description description\n   :property value]\n\nThe first three string parameters in an option spec are positional and\noptional, and may be nil in order to specify a later parameter.\n\nBy default, options are toggles that default to nil, but the second string\nparameter may be used to specify that an option requires an argument.\n\n  e.g. [\"-p\" \"--port PORT\"] specifies that --port requires an argument,\n       of which PORT is a short description.\n\nThe :property value pairs are optional and take precedence over the\npositional string arguments. The valid properties are:\n\n  :id           The key for this option in the resulting option map. This\n                is normally set to the keywordized name of the long option\n                without the leading dashes.\n\n                Multiple option entries can share the same :id in order to\n                transform a value in different ways, but only one of these\n                option entries may contain a :default entry.\n\n                This option is mandatory.\n\n  :short-opt    The short format for this option, normally set by the first\n                positional string parameter: e.g. \"-p\". Must be unique.\n\n  :long-opt     The long format for this option, normally set by the second\n                positional string parameter; e.g. \"--port\". Must be unique.\n\n  :required     A description of the required argument for this option if\n                one is required; normally set in the second positional\n                string parameter after the long option: \"--port PORT\".\n\n                The absence of this entry indicates that the option is a\n                boolean toggle that is set to true when specified on the\n                command line.\n\n  :desc         A optional short description of this option.\n\n  :default      The default value of this option. If none is specified, the\n                resulting option map will not contain an entry for this\n                option unless set on the command line.\n\n  :default-desc An optional description of the default value. This should be\n                used when the string representation of the default value is\n                too ugly to be printed on the command line.\n\n  :parse-fn     A function that receives the required option argument and\n                returns the option value.\n\n                If this is a boolean option, parse-fn will receive the value\n                true. This may be used to invert the logic of this option:\n\n                [\"-q\" \"--quiet\"\n                 :id :verbose\n                 :default true\n                 :parse-fn not]\n\n  :assoc-fn     A function that receives the current option map, the current\n                option :id, and the current parsed option value, and returns\n                a new option map.\n\n                This may be used to create non-idempotent options, like\n                setting a verbosity level by specifying an option multiple\n                times. (\"-vvv\" -> 3)\n\n                [\"-v\" \"--verbose\"\n                 :default 0\n                 :assoc-fn (fn [m k _] (update-in m [k] inc))]\n\n  :validate     A vector of [validate-fn validate-msg ...]. Multiple pairs\n                of validation functions and error messages may be provided.\n\n  :validate-fn  A vector of functions that receives the parsed option value\n                and returns a falsy value or throws an exception when the\n                value is invalid. The validations are tried in the given\n                order.\n\n  :validate-msg A vector of error messages corresponding to :validate-fn\n                that will be added to the :errors vector on validation\n                failure.\n\nparse-opts returns a map with four entries:\n\n  {:options     The options map, keyed by :id, mapped to the parsed value\n   :arguments   A vector of unprocessed arguments\n   :summary     A string containing a minimal options summary\n   :errors      A possible vector of error message strings generated during\n                parsing; nil when no errors exist}\n\nA few function options may be specified to influence the behavior of\nparse-opts:\n\n  :in-order     Stop option processing at the first unknown argument. Useful\n                for building programs with subcommands that have their own\n                option specs.\n\n  :no-defaults  Only include option values specified in arguments and do not\n                include any default values in the resulting options map.\n                Useful for parsing options from multiple sources; i.e. from a\n                config file and from the command line.\n\n  :summary-fn   A function that receives the sequence of compiled option specs\n                (documented at #'clojure.tools.cli/compile-option-specs), and\n                returns a custom option summary string.\n",
   :var-type "function",
   :line 435,
   :file "src/main/clojure/clojure/tools/cli.clj"}
  {:arglists ([specs]),
   :name "summarize",
   :namespace "clojure.tools.cli",
   :source-url
   "https://github.com/clojure/tools.cli/blob/ac2c5cc24eff7662c19e9f79c1cec4bfeebe15f7/src/main/clojure/clojure/tools/cli.clj#L419",
   :added "0.3.0",
   :raw-source-url
   "https://github.com/clojure/tools.cli/raw/ac2c5cc24eff7662c19e9f79c1cec4bfeebe15f7/src/main/clojure/clojure/tools/cli.clj",
   :wiki-url
   "http://clojure.github.com/tools.cli//clojure.tools.cli-api.html#clojure.tools.cli/summarize",
   :doc
   "Reduce options specs into a options summary for printing at a terminal.",
   :var-type "function",
   :line 419,
   :file "src/main/clojure/clojure/tools/cli.clj"})}
