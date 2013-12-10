# tools.cli

tools.cli contains a command line options parser for Clojure.

## Next Release: 0.3.0-beta1

The next release of tools.cli features a new flexible API, better adherence to
GNU option parsing conventions, and ClojureScript support.

[Leiningen](https://github.com/technomancy/leiningen) dependency information:

    [org.clojure/tools.cli "0.3.0-beta1"]

[Maven](http://maven.apache.org/) dependency information:

    <dependency>
      <groupId>org.clojure</groupId>
      <artifactId>tools.cli</artifactId>
      <version>0.3.0-beta1</version>
    </dependency>

The function `clojure.tools.cli/cli` has been superseded by
`clojure.tools.cli/parse-opts`, and should not be used in new programs.

The previous function will remain for the forseeable future. It has also been
adapted to use the new tokenizer, so upgrading is still worthwhile even if you
are not ready to migrate to `clojure.tools.cli/parse-opts`.

## New Features

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

There is a new option entry `:validate`, which takes a tuple of
`[validation-fn validation-msg]`. The validation-fn receives an option's
argument *after* being parsed by `:parse-fn` if it exists.

    ["-p" "--port PORT" "A port number"
     :parse-fn #(Integer/parseInt %)
     :validate [#(< 0 % 0x10000) "Must be a number between 0 and 65536"]]

If the validation-fn returns a falsy value, the validation-msg is added to the
errors vector.

### Error Handling and Return Values

Instead of throwing errors, `parse-opts` now collects error messages into
a vector and returns them to the caller. Unknown options, missing required
arguments, validation errors, and exceptions thrown during `:parse-fn` are all
added to the errors vector.

Correspondingly, `parse-opts` returns the following map of values:

    {:options     The map of options -> parsed values
     :arguments   A vector of unprocessed arguments
     :summary     An options summary string
     :errors      A vector of error messages, nil if no errors
     }

During development, parse-opts asserts the uniqueness of option `:id`,
`:short-opt`, and `:long-opt` values and throws an error on failure.

### ClojureScript Support

The `cljs.tools.cli` namespace is now available for use in ClojureScript
programs! Both `parse-opts` and `summarize` have been ported, and have
complete feature parity with their Clojure counterparts.

### API documentation

Detailed documentation for `parse-opts` and `summarize` is available in their
respective docstrings:

http://clojure.github.io/tools.cli/index.html#clojure.tools.cli/parse-opts

### Example Usage

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
    ;; Use assoc-fn to create non-idempotent options
    :assoc-fn (fn [m k _] (update-in m [k] inc))]
   ["-h" "--help"]])

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

(defn exit [status msg]
  (println msg)
  (System/exit status))

(defn -main [& args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    ;; Handle help and error conditions
    (cond
      (:help options) (exit 0 (usage summary))
      (not= (count arguments) 1) (exit 1 (usage summary))
      errors (exit 1 (error-msg errors)))
    ;; Execute program with options
    (case (first arguments)
      "start" (server/start! options)
      "stop" (server/stop! options)
      "status" (server/status! options)
      (exit 1 (usage summary)))))
```

## Stable Releases and Dependency Information

Latest stable release: 0.2.4

* [All Released Versions](http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22org.clojure%22%20AND%20a%3A%22tools.cli%22)
* [Development Snapshot Versions](https://oss.sonatype.org/index.html#nexus-search;gav~org.clojure~tools.cli~~~)

[Leiningen](https://github.com/technomancy/leiningen) dependency information:

    [org.clojure/tools.cli "0.2.4"]

[Maven](http://maven.apache.org/) dependency information:

    <dependency>
      <groupId>org.clojure</groupId>
      <artifactId>tools.cli</artifactId>
      <version>0.2.4</version>
    </dependency>

## Example Usage

    (use '[clojure.tools.cli :only [cli]])

    (cli args
         ["-p" "--port" "Listen on this port" :parse-fn #(Integer. %)
          :assoc-fn (fn [previous key val]
                       (assoc previous key
                              (if-let [oldval (get previous key)]
                                (merge oldval val)
                                (hash-set val))))]
         ["-h" "--host" "The hostname" :default "localhost"]
         ["-v" "--[no-]verbose" :default true]
         ["-l" "--log-directory" :default "/some/path"])

with args of:

    ["-p" "8080"
     "-p" "9090"
     "--no-verbose"
     "--log-directory" "/tmp"
     "some-file"]

will return a vector containing three elements:

a clojure map with the names picked out for you as keywords:

     {:port          #{8080 9090}
      :host          "localhost"
      :verbose       false
      :log-directory "/tmp"}

a vector of trailing arguments that are not options:

    ["some-file"]

and a documentation string to use to provide help:

    "Switches                    Default     Desc
     --------                    -------     ----
     -p, --port                              Listen on this port
     -h, --host                  localhost   The hostname
     -v, --no-verbose --verbose  true
     -l, --log-directory         /some/path"

### Custom description

You can pass an optional description argument that will be shown
between "Usage:" and the description of the switches. For example:

    (cli args
         "This program does something extraordinary."
         ["-p" "--port" "Listen on this port" :parse-fn #(Integer. %)]
         ["-h" "--host" "The hostname" :default "localhost"])

The documentation string will now look like:

    "This program does something extraordinary.

     Switches                    Default     Desc
     --------                    -------     ----
     -p, --port                              Listen on this port
     -h, --host                  localhost   The hostname"

## Options

An option is specified by providing a vector of information:

Switches should be provided first, from least to most specific. The
last switch you provide will be used as the name for the argument in
the resulting hash-map. The following:

    ["-p" "--port"]

defines an argument with two possible switches, the name of which will
be :port in the resulting hash-map.

Next is an optional doc string:

    ["-p" "--port" "The port to listen on"]

This will be printed in the 'Desc' column of the help banner.

Following that are optional parameters, provided in key-value pairs:

    ["-p" "--port" "The port to listen on" :default 8080 :parse-fn #(Integer. %)
     :assoc-fn (fn [previous key val]
                 (assoc previous key
                        (if-let [oldval (get previous key)]
                          (merge oldval val)
                          (hash-set val))))]

These should be self-explanatory. The defaults if not provided are as follows:

    {:default  nil
     :parse-fn identity
     :assoc-fn assoc
     :flag     false}

If you provide the same option multiple times, the `assoc-fn` will be
called for each value after the first. This gives you another way to
build collections of values (the other being using `parse-fn` to split
the value).

### Boolean Flags

Flags are indicated either through naming convention:

    ["-v" "--[no-]verbose" "Be chatty"]

(note the [no-] in the argument name).

Or you can explicitly mark them as flags:

    ["-v" "--verbose" "Be chatty" :flag true]

Either way, when providing them on the command line, using the name
itself will set to true, and using the name prefixed with 'no-' will
set the argument to false:

    (cli ["-v"]
         ["-v" "--[no-]verbose"])

    => [{:verbose true}, ...]

    (cli ["--no-verbose"]
         ["-v" "--[no-]verbose"])

    => [{:verbose false}, ...]

Note: there is no short-form to set the flag to false (-no-v will not
work!).

## Trailing Arguments

Any trailing arguments given to `cli` are returned as the second item
in the resulting vector:

    (cli ["--port" "9999" "some" "extra" "arguments"]
         ["--port" :parse-fn #(Integer. %)])

    => [{:port 9999}, ["some" "extra" "arguments"], ...]

This allows you to deal with parameters such as filenames which are
commonly provided at the end of an argument list.

If you wish to explicitly signal the end of arguments, you can use a
double-hyphen:

    (cli ["--port" "9999" "--" "some" "--extra" "arguments"]
         ["--port" :parse-fn #(Integer. %)])

    => [{:port 9999}, ["some" "--extra" "arguments"], ...]

This is useful when your extra arguments look like switches.

## Banner

The third item in the resulting vector is a banner useful for
providing help to the user:

    (let [[options args banner] (cli ["--faux" "bar"]
                                     ["-h" "--help" "Show help" :default false :flag true]
                                     ["-f" "--faux" "The faux du fafa"])]
      (when (:help options)
        (println banner)
        (System/exit 0))
      (println options))

## Developer Information

* [GitHub project](https://github.com/clojure/tools.cli)
* [Bug Tracker](http://dev.clojure.org/jira/browse/TCLI)
* [Continuous Integration](http://build.clojure.org/job/tools.cli/)
* [Compatibility Test Matrix](http://build.clojure.org/job/tools.cli-test-matrix/)

## Change Log

* Release 0.2.4 on 2013-08-06
  * Applying patch for [TCLI-2](http://dev.clojure.org/jira/browse/TCLI-2)
    (support an assoc-fn option)
* Release 0.2.3 on 2013-08-06
  * Add optional description string to prefix the returned banner
* Release 0.2.2 on 2012-08-09
  * Applying patch for [TCLI-1](http://dev.clojure.org/jira/browse/TCLI-1)
    (do not include keys when no value provided by :default)
* Release 0.2.1 on 2011-11-03
  * Removing the :required option. Hangover from when -h and --help were
    implemented by default, causes problems if you want help and dont
    provide a :required argument.
* Release 0.2.0 on 2011-10-31
  * Remove calls to System/exit
  * Remove built-in help options
* Release 0.1.0
  * Initial import of Clargon codebase

## License

Copyright (c) Rich Hickey and contributors. All rights reserved.

The use and distribution terms for this software are covered by the
Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
which can be found in the file epl.html at the root of this distribution.
By using this software in any fashion, you are agreeing to be bound by
the terms of this license.
You must not remove this notice, or any other, from this software.
