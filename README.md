# tools.cli

Tools for working with command line arguments.

## Stable Releases and Dependency Information

This project follows the version scheme MAJOR.MINOR.COMMITS where MAJOR and MINOR provide some relative indication of the size of the change, but do not follow semantic versioning. In general, all changes endeavor to be non-breaking (by moving to new names rather than by breaking existing names). COMMITS is an ever-increasing counter of commits since the beginning of this repository.

Latest stable release: 1.0.219

* [All Released Versions](http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22org.clojure%22%20AND%20a%3A%22tools.cli%22)

* [Development Snapshot Versions](https://oss.sonatype.org/index.html#nexus-search;gav~org.clojure~tools.cli~~~)

[clj/deps.edn](https://clojure.org/guides/deps_and_cli) dependency information:
```clojure
org.clojure/tools.cli {:mvn/version "1.0.219"}
```

[Leiningen](https://github.com/technomancy/leiningen) dependency information:
```clojure
[org.clojure/tools.cli "1.0.219"]
```
[Maven](http://maven.apache.org/) dependency information:
```xml
<dependency>
  <groupId>org.clojure</groupId>
  <artifactId>tools.cli</artifactId>
  <version>1.0.219</version>
 </dependency>
```
The 0.4.x series of tools.cli supports use with `clj`/`deps.edn` and brings
the legacy API to ClojureScript by switching to `.cljc` files. This means it
requires Clojure(Script) 1.8 or later.

The 0.3.x series of tools.cli features a new flexible API, better adherence
to GNU option parsing conventions, and ClojureScript support.

The function `clojure.tools.cli/cli` has been superseded by
`clojure.tools.cli/parse-opts`, and should not be used in new programs.

The previous function will remain for the foreseeable future. It has also been
adapted to use the new tokenizer, so upgrading is still worthwhile even if you
are not ready to migrate to `parse-opts`.

## Quick Start

```clojure
(ns my.program
  (:require [clojure.tools.cli :refer [parse-opts]])
  (:gen-class))

(def cli-options
  ;; An option with a required argument
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
and refer to the docstring of `parse-opts` for comprehensive documentation:

http://clojure.github.io/tools.cli/index.html#clojure.tools.cli/parse-opts

## See Also

An interesting library built on top of `tool.cli` that provides a more compact,
higher-level API is [cli-matic](https://github.com/l3nz/cli-matic).

## Since Release 0.3.x

### Better Option Tokenization

In accordance with the [GNU Program Argument Syntax Conventions][GNU], two
features have been added to the options tokenizer:

* Short options may be grouped together.

  For instance, `-abc` is equivalent to `-a -b -c`. If the `-b` option
  requires an argument, the same `-abc` is interpreted as `-a -b "c"`.

* Long option arguments may be specified with an equals sign.

  `--long-opt=ARG` is equivalent to `--long-opt "ARG"`.

  If the argument is omitted, it is interpreted as the empty string.
  e.g. `--long-opt=` is equivalent to `--long-opt ""`

[GNU]: https://www.gnu.org/software/libc/manual/html_node/Argument-Syntax.html

### In-order Processing for Subcommands

Large programs are often divided into subcommands with their own sets of
options. To aid in designing such programs, `clojure.tools.cli/parse-opts`
accepts an `:in-order` option that directs it to stop processing arguments at
the first unrecognized token.

For instance, the `git` program has a set of top-level options that are
unrecognized by subcommands and vice-versa:

    git --git-dir=/other/proj/.git log --oneline --graph

By default, `clojure.tools.cli/parse-opts` interprets this command line as:

    options:   [[--git-dir /other/proj/.git]
                [--oneline]
                [--graph]]
    arguments: [log]

When :in-order is true however, the arguments are interpreted as:

    options:   [[--git-dir /other/proj/.git]]
    arguments: [log --oneline --graph]

Note that the options to `log` are not parsed, but remain in the unprocessed
arguments vector. These options could be handled by another call to
`parse-opts` from within the function that handles the `log` subcommand.

### Options Summary

`parse-opts` returns a minimal options summary string:

      -p, --port NUMBER  8080       Required option with default
          --host HOST    localhost  Short and long options may be omitted
      -d, --detach                  Boolean option
      -h, --help

This may be inserted into a larger usage summary, but it is up to the caller.

If the default formatting of the summary is unsatisfactory, a `:summary-fn`
may be supplied to `parse-opts`. This function will be passed the sequence
of compiled option specification maps and is expected to return an options
summary.

The default summary function `clojure.tools.cli/summarize` is public and may
be useful within your own `:summary-fn` for generating the default summary.

### Option Argument Validation

By default, option validation is performed immediately after parsing, which
means that "flag" arguments will have a Boolean value, even if a `:default`
is specified with a different type of value.

You can choose to perform validation after option processing instead, with
the `:post-validation true` flag. During option processing, `:default` values
are applied and `:assoc-fn` and `:update-fn` are invoked. If an option is
specified more than once, `:post-validation true` will cause validation to
be performed after each new option value is processed.

There is a new option entry `:validate`, which takes a tuple of
`[validation-fn validation-msg]`. The validation-fn receives an option's
argument *after* being parsed by `:parse-fn` if it exists. The validation-msg
can either be a string or a function of one argument that can be called on
the invalid option argument to produce a string:

    ["-p" "--port PORT" "A port number"
     :parse-fn #(Integer/parseInt %)
     :validate [#(< 0 % 0x10000) #(str % " is not a number between 0 and 65536")]]

If the validation-fn returns a falsey value, the validation-msg is added to the
errors vector.

### Error Handling and Return Values

Instead of throwing errors, `parse-opts` collects error messages into a vector
and returns them to the caller. Unknown options, missing required arguments,
validation errors, and exceptions thrown during `:parse-fn` are all added to
the errors vector.

Any option can be flagged as required by providing a `:missing` key in the
option spec with a string that should be used for the error message if the
option is omitted.

The error message when a required argument is omitted (either a short opt with
`:require` or a long opt describing an argument) is:

`Missing required argument for ...`

Correspondingly, `parse-opts` returns the following map of values:

    {:options     A map of default options merged with parsed values from the command line
     :arguments   A vector of unprocessed arguments
     :summary     An options summary string
     :errors      A vector of error messages, or nil if no errors}

During development, parse-opts asserts the uniqueness of option `:id`,
`:short-opt`, and `:long-opt` values and throws an error on failure.

### ClojureScript Support

As of 0.4.x, the namespace is `clojure.tools.cli` for both Clojure and
ClojureScript programs. The entire API, including the legacy (pre-0.3.x)
functions, is now available in both Clojure and ClojureScript.

For the 0.3.x releases, the ClojureScript namespace was `cljs.tools.cli` and
only `parse-opts` and `summarize` were available.

## Example Usage

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
   ;; If no required argument description is given, the option is assumed to
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
    :default []
    ;; with :multi true, the :update-fn is passed both the existing parsed
    ;; value(s) and the new parsed value from each option
    :update-fn conj]
   ;; A boolean option that can explicitly be set to false
   ["-d" "--[no-]daemon" "Daemonize the process" :default true]
   ["-h" "--help"]])

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

* [Bug Tracker](http://clojure.atlassian.net/browse/TCLI)

* [Continuous Integration](http://build.clojure.org/job/tools.cli/)

* [Compatibility Test Matrix](http://build.clojure.org/job/tools.cli-test-matrix/)

## License

Copyright (c) Rich Hickey and contributors. All rights reserved.

The use and distribution terms for this software are covered by the
Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
which can be found in the file epl.html at the root of this distribution.
By using this software in any fashion, you are agreeing to be bound by
the terms of this license.

You must not remove this notice, or any other, from this software.
