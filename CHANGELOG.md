# Change Log

* Release 1.0.219 2023-05-08
  * Add ClojureCLR support [TCLI-102](https://clojure.atlassian.net/browse/TCLI-102) [@dmiller](https://github.com/dmiller).

* Release 1.0.214 2022-10-08
  * Document `:missing`, `:multi`, and `:post-validation` in the docstrings and in the README.
  * In the help summary, display default values for all options that provide them [TCLI-100](https://clojure.atlassian.net/browse/TCLI-100). Previously, only options that had required arguments would have their defaults shown.

* Release 1.0.206 2021-02-27
  * Allow validation to be performed either after parsing (before option processing) -- current default -- or after option processing, via the `:post-validation true` flag [TCLI-98](https://clojure.atlassian.net/browse/TCLI-98).
  * Allow validation message to be a function (of the invalid argument) in addition to being a plain string [TCLI-97](https://clojure.atlassian.net/browse/TCLI-97).
  * Add `:multi true` to modify behavior of `:update-fn` [TCLI-96](https://clojure.atlassian.net/browse/TCLI-96).

* Release 1.0.194 2020-02-20
  * Switch to 1.0.x versioning.
  * Document the `:missing` option [TCLI-95](https://clojure.atlassian.net/browse/TCLI-95).
* Release 0.4.2 2019-03-26
  * Restore ClojureScript compatibility (Martin Klepsch)
    [TCLI-94](https://clojure.atlassian.net/browse/TCLI-94).
  * Replace `clojure.pprint/cl-format` for better compatibility with GraalVM
    [TCLI-93](https://clojure.atlassian.net/browse/TCLI-93).
* Release 0.4.1 2018-09-22
  * Add `:update-fn` as the preferred way to handle non-idempotent options. It
    is a simpler alternative to using `:assoc-fn` for some such options.
  * Add `:default-fn` as a way to compute default option values after parsing.
    This is particularly useful with `:update-fn` since you can use it to
    override the `:default` value if necessary
    [TCLI-90](https://clojure.atlassian.net/browse/TCLI-90).
* Release 0.4.0 on 2018-09-12
  * Convert everything to use `.cljc` files and add `clj`/`deps.edn` support
    [TCLI-91](https://clojure.atlassian.net/browse/TCLI-91). This **drops
    support for Clojure 1.7 and earlier** but brings full feature parity to
    ClojureScript. Tests for Clojure can be run with `clj -A:test:runner` and
    for ClojureScript with `clj -A:test:cljs-runner`. Multi-version testing is
    possible with aliases `:1.8`, `:1.9`, and `:master`.
* Release 0.3.7 on 2018-04-25
  * Fix NPE from `nil` long option
    [TCLI-89](https://clojure.atlassian.net/browse/TCLI-89) (Peter Schwarz).
* Release 0.3.6 on 2018-04-11
  * Restore support for `--no` prefix in long options
    [TCLI-88](https://clojure.atlassian.net/browse/TCLI-88) (Arne Brasseur).
* Release 0.3.5 on 2016-05-04
  * Fix `summarize` in cljs after renaming during TCLI-36 below
    [TCLI-85](https://clojure.atlassian.net/browse/TCLI-85).
* Release 0.3.4 on 2016-05-01
  * Clarify use of `summarize` via expanded docstring and make both of the
    functions it calls public so it is easier to build your own `:summary-fn`.
    [TCLI-36](https://clojure.atlassian.net/browse/TCLI-36).
 * Release 0.3.3 on 2015-08-21
  * Add `:missing` to option specification to produce the given error message
    if the option is not provided (and has no default value).
    [TCLI-12](https://clojure.atlassian.net/browse/TCLI-12)
  * Add `:strict` to `parse-opts`:
    If true, treats required option arguments that match other options as a
    parse error (missing required argument).
    [TCLI-10](https://clojure.atlassian.net/browse/TCLI-10)
* Release 0.3.2 on 2015-07-28
  * Add `:no-defaults` to `parse-opts`:
    Returns sequence of options that excludes defaulted ones. This helps
    support constructing options from multiple sources (command line, config file).
  * Add `get-default-options`:
    Returns sequence of options that have defaults specified.
  * Support multiple validations [TCLI-9](https://clojure.atlassian.net/browse/TCLI-9)
  * Support in-order arguments [TCLI-5](https://clojure.atlassian.net/browse/TCLI-5):
    `:in-order` processes arguments up to the first unknown option;
    A warning is displayed when unknown options are encountered.
* Release 0.3.1 on 2014-01-02
  * Apply patch for [TCLI-8](https://clojure.atlassian.net/browse/TCLI-8):
    Correct test that trivially always passes
  * Apply patch for [TCLI-7](https://clojure.atlassian.net/browse/TCLI-7):
    summarize throws when called with an empty sequence of options
* Release 0.3.0 on 2013-12-15
  * Add public functions `parse-opts` and `summarize` to supersede `cli`,
    addressing [TCLI-3](https://clojure.atlassian.net/browse/TCLI-3),
    [TCLI-4](https://clojure.atlassian.net/browse/TCLI-4), and
    [TCLI-6](https://clojure.atlassian.net/browse/TCLI-6)
  * Add ClojureScript port of `parse-opts` and `summarize`, available in
    `cljs.tools.cli`.
  * Move extra documentation of `cli` function to
    https://github.com/clojure/tools.cli/wiki/Documentation-for-0.2.4
* Release 0.2.4 on 2013-08-06
  * Applying patch for [TCLI-2](https://clojure.atlassian.net/browse/TCLI-2)
    (support an assoc-fn option)
* Release 0.2.3 on 2013-08-06
  * Add optional description string to prefix the returned banner
* Release 0.2.2 on 2012-08-09
  * Applying patch for [TCLI-1](https://clojure.atlassian.net/browse/TCLI-1)
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
