# tools.cli

Tools for working with command line arguments.

## Stable Releases and Dependency Information

This project follows the version scheme MAJOR.MINOR.COMMITS where MAJOR and MINOR provide some relative indication of the size of the change, but do not follow semantic versioning. In general, all changes endeavor to be non-breaking (by moving to new names rather than by breaking existing names). COMMITS is an ever-increasing counter of commits since the beginning of this repository.

Latest stable release: 1.1.230

* [All Released Versions](https://central.sonatype.com/artifact/org.clojure/tools.cli/versions)
* [Development Snapshot Versions](https://oss.sonatype.org/index.html#nexus-search;gav~org.clojure~tools.cli~~~)

[clj/deps.edn](https://clojure.org/guides/deps_edn) dependency information:
```clojure
org.clojure/tools.cli {:mvn/version "1.1.230"}
```

[Leiningen](https://leiningen.org/) dependency information:
```clojure
[org.clojure/tools.cli "1.1.230"]
```
[Maven](https://maven.apache.org/) dependency information:
```xml
<dependency>
  <groupId>org.clojure</groupId>
  <artifactId>tools.cli</artifactId>
  <version>1.1.230</version>
 </dependency>
```

### Historical Release Notes

Starting with 0.4.x, `tools.cli` supports use with `clj`/`deps.edn` and brings
the legacy API to ClojureScript by switching to `.cljc` files. This means it
requires Clojure(Script) 1.9 or later.

The 0.3.x series of tools.cli introduced a new flexible API, better adherence
to GNU option parsing conventions, and ClojureScript support.

The old function `clojure.tools.cli/cli` was superseded by
`clojure.tools.cli/parse-opts`, and should not be used in new programs.

The older function will remain for the foreseeable future. It has also been
adapted to use the new tokenizer, so upgrading is still worthwhile even if you
are not ready to migrate to `parse-opts`.

## Quick Start

```clojure
(ns my.program
  (:require [clojure.tools.cli :refer [parse-opts]])
  (:gen-class))

(def cli-options
  ;; An option with an argument
  [["-p" "--port PORT" "Port number"
    :default 80
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 0 % 0x10000) "Must be a number between 0 and 65536"]]
   ;; A non-idempotent option (:default is applied first)
   ["-v" nil "Verbosity level"
    :id :verbosity
    :default 0
    :update-fn inc] ; Prior to 0.4.1, you would have to use:
                   ;; :assoc-fn (fn [m k _] (update-in m [k] inc))
   ;; A boolean option defaulting to nil
   ["-h" "--help"]])

(defn -main [& args]
  (parse-opts args cli-options))
```

Execute the command line:

    clojure -M -m my.program -vvvp8080 foo --help --invalid-opt

(or use `lein run` or however you run your program instead of `clojure -M -m my.program`)

to produce the map:

```clojure
{:options   {:port 8080
             :verbosity 3
             :help true}

 :arguments ["foo"]

 :summary   "  -p, --port PORT  80  Port number
               -v                   Verbosity level
               -h, --help"

 :errors    ["Unknown option: \"--invalid-opt\""]}
```

**Note** that exceptions are _not_ thrown on parse errors, so errors must be
handled explicitly after checking the `:errors` entry for a truthy value.

Please see the [example program](#example-usage) for a more detailed example
and refer to the docstring of `parse-opts` for comprehensive documentation
(as part of the [API Documentation](https://clojure.github.io/tools.cli/)):

https://clojure.github.io/tools.cli/index.html#clojure.tools.cli/parse-opts

## See Also

An interesting library built on top of `tool.cli` that provides a more compact,
higher-level API is [cli-matic](https://github.com/l3nz/cli-matic).

## Example Usage

This is an example of a program that uses most of the `tools.cli` features.
For detailed documentation, please see the docstring of `parse-opts`.

```clojure
(ns cli-example.core
  (:require [cli-example.server :as server]
            [clojure.string :as string]
            [clojure.tools.cli :refer [parse-opts]])
  (:import (java.net InetAddress))
  (:gen-class))

(def cli-options
  [;; First three strings describe a short-option, long-option with optional
   ;; example argument description, and a description. All three are optional
   ;; and positional.
   ["-p" "--port PORT" "Port number"
    :default 80
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 0 % 0x10000) "Must be a number between 0 and 65536"]]
   ["-H" "--hostname HOST" "Remote host"
    :default (InetAddress/getByName "localhost")
    ;; Specify a string to output in the default column in the options summary
    ;; if the default value's string representation is very ugly
    :default-desc "localhost"
    :parse-fn #(InetAddress/getByName %)]
   ;; If no argument description is given, the option is assumed to
   ;; be a boolean option defaulting to nil
   [nil "--detach" "Detach from controlling process"]
   ["-v" nil "Verbosity level; may be specified multiple times to increase value"
    ;; If no long-option is specified, an option :id must be given
    :id :verbosity
    :default 0
    ;; Use :update-fn to create non-idempotent options (:default is applied first)
    :update-fn inc]
   ["-f" "--file NAME" "File names to read"
    :multi true ; use :update-fn to combine multiple instance of -f/--file
    ;; if no -f/--file options are given, return this error:
    :missing "At least one file name is required"
    ;; with :multi true, the :update-fn is passed both the existing parsed
    ;; value(s) and the new parsed value from each option; using fnil lets
    ;; us avoid specifying a :default value
    :update-fn (fnil conj [])]
   ["-t" nil "Timeout in seconds"
    ;; Since there is no long option, we need to specify the name used for
    ;; the argument to the option...
    :id :timeout
    ;; ...and we need to specify the description of argument that is required
    ;; for the option:
    :required "TIMEOUT"
    ;; parse-long was added in Clojure 1.11:
    :parse-fn parse-long]
   ;; A boolean option that can explicitly be set to false
   ["-d" "--[no-]daemon" "Daemonize the process" :default true]
   ["-h" "--help"]])

;; The :required specification provides the name shown in the usage summary
;; for the argument that an option expects. It is only needed when the long
;; form specification of the option is not given, only the short form. In
;; addition, :id must be specified to provide the internal keyword name for
;; the option. If you want to indicate that an option itself is required,
;; you can use the :missing key to provide a message that will be shown
;; if the option is not present.

;; The :default values are applied first to options. Sometimes you might want
;; to apply default values after parsing is complete, or specifically to
;; compute a default value based on other option values in the map. For those
;; situations, you can use :default-fn to specify a function that is called
;; for any options that do not have a value after parsing is complete, and
;; which is passed the complete, parsed option map as it's single argument.
;; :default-fn (constantly 42) is effectively the same as :default 42 unless
;; you have a non-idempotent option (with :update-fn or :assoc-fn) -- in which
;; case any :default value is used as the initial option value rather than nil,
;; and :default-fn will be called to compute the final option value if none was
;; given on the command-line (thus, :default-fn can override :default)
;; Note: validation is *not* performed on the result of :default-fn (this is
;; an open issue for discussion and is not currently considered a bug).

(defn usage [options-summary]
  (->> ["This is my program. There are many like it, but this one is mine."
        ""
        "Usage: program-name [options] action"
        ""
        "Options:"
        options-summary
        ""
        "Actions:"
        "  start    Start a new server"
        "  stop     Stop an existing server"
        "  status   Print a server's status"
        ""
        "Please refer to the manual page for more information."]
       (string/join \newline)))

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (string/join \newline errors)))

(defn validate-args
  "Validate command line arguments. Either return a map indicating the program
  should exit (with an error message, and optional ok status), or a map
  indicating the action the program should take and the options provided."
  [args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    (cond
      (:help options) ; help => exit OK with usage summary
      {:exit-message (usage summary) :ok? true}
      errors ; errors => exit with description of errors
      {:exit-message (error-msg errors)}
      ;; custom validation on arguments
      (and (= 1 (count arguments))
           (#{"start" "stop" "status"} (first arguments)))
      {:action (first arguments) :options options}
      :else ; failed custom validation => exit with usage summary
      {:exit-message (usage summary)})))

(defn exit [status msg]
  (println msg)
  (System/exit status))

(defn -main [& args]
  (let [{:keys [action options exit-message ok?]} (validate-args args)]
    (if exit-message
      (exit (if ok? 0 1) exit-message)
      (case action
        "start"  (server/start! options)
        "stop"   (server/stop! options)
        "status" (server/status! options)))))
```

## Developer Information

* [GitHub project](https://github.com/clojure/tools.cli)
* [Bug Tracker](https://clojure.atlassian.net/browse/TCLI)
* [Continuous Integration](https://github.com/clojure/tools.cli/actions/workflows/test.yml)

## License

Copyright (c) Rich Hickey and contributors. All rights reserved.

The use and distribution terms for this software are covered by the
Eclipse Public License 1.0 (https://opensource.org/license/epl-1-0/)
which can be found in the file epl.html at the root of this distribution.
By using this software in any fashion, you are agreeing to be bound by
the terms of this license.

You must not remove this notice, or any other, from this software.
