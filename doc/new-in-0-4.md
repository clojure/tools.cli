## Improvements in 0.4.x

This section highlights the changes/improvents in the 0.4.x series of
releases, compared to the earlier 0.3.x series.

As a general note, `clojure.tools.cli/cli` is deprecated and you should
use `clojure.tools.cli/parse-opts` instead. The legacy function will remain
for the foreseeable future, but will not get bug fixes or new features.

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
