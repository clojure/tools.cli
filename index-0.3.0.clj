{:namespaces
 ({:source-url
   "https://github.com/clojure/tools.cli/blob/e9816bf3361b9a2de5601daf402a3cc435034dd9/src/main/clojure/clojure/tools/cli.clj",
   :wiki-url
   "http://clojure.github.com/tools.cli/clojure.tools.cli-api.html",
   :name "clojure.tools.cli",
   :author "Gareth Jones, Sung Pae",
   :doc nil}),
 :vars
 ({:arglists ([args & specs]),
   :name "cli",
   :namespace "clojure.tools.cli",
   :source-url
   "https://github.com/clojure/tools.cli/blob/e9816bf3361b9a2de5601daf402a3cc435034dd9/src/main/clojure/clojure/tools/cli.clj#L180",
   :raw-source-url
   "https://github.com/clojure/tools.cli/raw/e9816bf3361b9a2de5601daf402a3cc435034dd9/src/main/clojure/clojure/tools/cli.clj",
   :wiki-url
   "http://clojure.github.com/tools.cli//clojure.tools.cli-api.html#clojure.tools.cli/cli",
   :doc
   "Parse the provided args using the given specs. Specs are vectors\ndescribing a command line argument. For example:\n\n[\"-p\" \"--port\" \"Port to listen on\" :default 3000 :parse-fn #(Integer/parseInt %)]\n\nFirst provide the switches (from least to most specific), then a doc\nstring, and pairs of options.\n\nValid options are :default, :parse-fn, and :flag. See\nhttps://github.com/clojure/tools.cli/blob/master/README.md for more\ndetailed examples.\n\nReturns a vector containing a map of the parsed arguments, a vector\nof extra arguments that did not match known switches, and a\ndocumentation banner to provide usage instructions.",
   :var-type "function",
   :line 180,
   :file "src/main/clojure/clojure/tools/cli.clj"}
  {:arglists ([args option-specs & options]),
   :name "parse-opts",
   :namespace "clojure.tools.cli",
   :source-url
   "https://github.com/clojure/tools.cli/blob/e9816bf3361b9a2de5601daf402a3cc435034dd9/src/main/clojure/clojure/tools/cli.clj#L384",
   :raw-source-url
   "https://github.com/clojure/tools.cli/raw/e9816bf3361b9a2de5601daf402a3cc435034dd9/src/main/clojure/clojure/tools/cli.clj",
   :wiki-url
   "http://clojure.github.com/tools.cli//clojure.tools.cli-api.html#clojure.tools.cli/parse-opts",
   :doc
   "Parse arguments sequence according to given option specifications and the\nGNU Program Argument Syntax Conventions:\n\n  https://www.gnu.org/software/libc/manual/html_node/Argument-Syntax.html\n\nOption specifications are a sequence of vectors with the following format:\n\n  [short-opt long-opt-with-required-description description\n   :property value]\n\nThe first three string parameters in an option spec are positional and\noptional, and may be nil in order to specify a later parameter.\n\nBy default, options are boolean flags that are set to true when toggled, but\nthe second string parameter may be used to specify that an option requires\nan argument.\n\n  e.g. [\"-p\" \"--port PORT\"] specifies that --port requires an argument,\n       of which PORT is a short description.\n\nThe :property value pairs are optional and take precedence over the\npositional string arguments. The valid properties are:\n\n  :id           The key for this option in the resulting option map. This\n                is normally set to the keywordized name of the long option\n                without the leading dashes.\n\n                Must be a unique truthy value.\n\n  :short-opt    The short format for this option, normally set by the first\n                positional string parameter: e.g. \"-p\". Must be unique.\n\n  :long-opt     The long format for this option, normally set by the second\n                positional string parameter; e.g. \"--port\". Must be unique.\n\n  :required     A description of the required argument for this option if\n                one is required; normally set in the second positional\n                string parameter after the long option: \"--port PORT\".\n\n                The absence of this entry indicates that the option is a\n                boolean toggle that is set to true when specified on the\n                command line.\n\n  :desc         A optional short description of this option.\n\n  :default      The default value of this option. If none is specified, the\n                resulting option map will not contain an entry for this\n                option unless set on the command line.\n\n  :default-desc An optional description of the default value. This should be\n                used when the string representation of the default value is\n                too ugly to be printed on the command line.\n\n  :parse-fn     A function that receives the required option argument and\n                returns the option value.\n\n                If this is a boolean option, parse-fn will receive the value\n                true. This may be used to invert the logic of this option:\n\n                [\"-q\" \"--quiet\"\n                 :id :verbose\n                 :default true\n                 :parse-fn not]\n\n  :assoc-fn     A function that receives the current option map, the current\n                option :id, and the current parsed option value, and returns\n                a new option map.\n\n                This may be used to create non-idempotent options, like\n                setting a verbosity level by specifying an option multiple\n                times. (\"-vvv\" -> 3)\n\n                [\"-v\" \"--verbose\"\n                 :default 0\n                 :assoc-fn (fn [m k _] (update-in m [k] inc))]\n\n  :validate     A vector of [validate-fn validate-msg].\n\n  :validate-fn  A function that receives the parsed option value and returns\n                a falsy value when the value is invalid.\n\n  :validate-msg An optional message that will be added to the :errors vector\n                on validation failure.\n\nparse-opts returns a map with four entries:\n\n  {:options     The options map, keyed by :id, mapped to the parsed value\n   :arguments   A vector of unprocessed arguments\n   :summary     A string containing a minimal options summary\n   :errors      A possible vector of error message strings generated during\n                parsing; nil when no errors exist\n   }\n\nA few function options may be specified to influence the behavior of\nparse-opts:\n\n  :in-order     Stop option processing at the first unknown argument. Useful\n                for building programs with subcommands that have their own\n                option specs.\n\n  :summary-fn   A function that receives the sequence of compiled option specs\n                (documented at #'clojure.tools.cli/compile-option-specs), and\n                returns a custom option summary string.\n",
   :var-type "function",
   :line 384,
   :file "src/main/clojure/clojure/tools/cli.clj"}
  {:arglists ([specs]),
   :name "summarize",
   :namespace "clojure.tools.cli",
   :source-url
   "https://github.com/clojure/tools.cli/blob/e9816bf3361b9a2de5601daf402a3cc435034dd9/src/main/clojure/clojure/tools/cli.clj#L367",
   :raw-source-url
   "https://github.com/clojure/tools.cli/raw/e9816bf3361b9a2de5601daf402a3cc435034dd9/src/main/clojure/clojure/tools/cli.clj",
   :wiki-url
   "http://clojure.github.com/tools.cli//clojure.tools.cli-api.html#clojure.tools.cli/summarize",
   :doc
   "Reduce options specs into a options summary for printing at a terminal.",
   :var-type "function",
   :line 367,
   :file "src/main/clojure/clojure/tools/cli.clj"})}
