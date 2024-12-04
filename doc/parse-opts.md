# `clojure.tools.cli/parse-opts`

[`parse-opts`][parse-opts] is the primary function in this library.

## docstring

This is the current docstring for `parse-opts` (I plan to expand
this into complete documentation for the library, with examples, over time):

```
  Parse arguments sequence according to given option specifications and the
  GNU Program Argument Syntax Conventions:

    https://www.gnu.org/software/libc/manual/html_node/Argument-Syntax.html

  Option specifications are a sequence of vectors with the following format:

    [short-opt long-opt-with-required-description description
     :property value]

  The first three string parameters in an option spec are positional and
  optional, and may be nil in order to specify a later parameter.

  By default, options are toggles that default to nil, but the second string
  parameter may be used to specify that an option requires an argument.

    e.g. ["-p" "--port PORT"] specifies that --port requires an argument,
         of which PORT is a short description.

  The :property value pairs are optional and take precedence over the
  positional string arguments. The valid properties are:

    :id           The key for this option in the resulting option map. This
                  is normally set to the keywordized name of the long option
                  without the leading dashes.

                  Multiple option entries can share the same :id in order to
                  transform a value in different ways, but only one of these
                  option entries may contain a :default(-fn) entry.

                  This option is mandatory if no long option is provided.

    :short-opt    The short format for this option, normally set by the first
                  positional string parameter: e.g. "-p". Must be unique.

    :long-opt     The long format for this option, normally set by the second
                  positional string parameter; e.g. "--port". Must be unique.

    :required     A description of the required argument for this option if
                  one is required; normally set in the second positional
                  string parameter after the long option: "--port PORT",
                  which would be equivalent to :required "PORT".

                  The absence of this entry indicates that the option is a
                  boolean toggle that is set to true when specified on the
                  command line.

    :missing      Indicates that this option is required (not just an argument),
                  and provides the string to use as an error message if omitted.

    :desc         A optional short description of this option.

    :default      The default value of this option. If none is specified, the
                  resulting option map will not contain an entry for this
                  option unless set on the command line. Also see :default-fn
                  (below).

                  This default is applied before any arguments are parsed so
                  this is a good way to seed values for :assoc-fn or :update-fn
                  as well as the simplest way to provide defaults.

                  If you need to compute a default based on other command line
                  arguments, or you need to provide a default separate from the
                  seed for :assoc-fn or :update-fn, see :default-fn below.

    :default-desc An optional description of the default value. This should be
                  used when the string representation of the default value is
                  too ugly to be printed on the command line, or :default-fn
                  is used to compute the default.

    :default-fn   A function to compute the default value of this option, given
                  the whole, parsed option map as its one argument. If no
                  function is specified, the resulting option map will not
                  contain an entry for this option unless set on the command
                  line. Also see :default (above).

                  If both :default and :default-fn are provided, if the
                  argument is not provided on the command-line, :default-fn will
                  still be called (and can override :default).

    :parse-fn     A function that receives the required option argument and
                  returns the option value.

                  If this is a boolean option, parse-fn will receive the value
                  true. This may be used to invert the logic of this option:

                  ["-q" "--quiet"
                   :id :verbose
                   :default true
                   :parse-fn not]

    :assoc-fn     A function that receives the current option map, the current
                  option :id, and the current parsed option value, and returns
                  a new option map. The default is 'assoc'.

                  For non-idempotent options, where you need to compute a option
                  value based on the current value and a new value from the
                  command line. If you only need the the current value, consider
                  :update-fn (below).

                  You cannot specify both :assoc-fn and :update-fn for an
                  option.

    :update-fn    Without :multi true:

                  A function that receives just the existing parsed option value,
                  and returns a new option value, for each option :id present.
                  The default is 'identity'.

                  This may be used to create non-idempotent options where you
                  only need the current value, like setting a verbosity level by
                  specifying an option multiple times. ("-vvv" -> 3)

                  ["-v" "--verbose"
                   :default 0
                   :update-fn inc]

                  :default is applied first. If you wish to omit the :default
                  option value, use fnil in your :update-fn as follows:

                  ["-v" "--verbose"
                   :update-fn (fnil inc 0)]

                  With :multi true:

                  A function that receives both the existing parsed option value,
                  and the parsed option value from each instance of the option,
                  and returns a new option value, for each option :id present.
                  The :multi option is ignored if you do not specify :update-fn.

                  For non-idempotent options, where you need to compute a option
                  value based on the current value and a new value from the
                  command line. This can sometimes be easier than use :assoc-fn.

                  ["-f" "--file NAME"
                   :default []
                   :update-fn conj
                   :multi true]

                  :default is applied first. If you wish to omit the :default
                  option value, use fnil in your :update-fn as follows:

                  ["-f" "--file NAME"
                   :update-fn (fnil conj [])
                   :multi true]

                  Regardless of :multi, you cannot specify both :assoc-fn
                  and :update-fn for an option.

    :multi        true/false, applies only to options that use :update-fn.

    :validate     A vector of [validate-fn validate-msg ...]. Multiple pairs
                  of validation functions and error messages may be provided.

    :validate-fn  A vector of functions that receives the parsed option value
                  and returns a falsy value or throws an exception when the
                  value is invalid. The validations are tried in the given
                  order.

    :validate-msg A vector of error messages corresponding to :validate-fn
                  that will be added to the :errors vector on validation
                  failure. Can be plain strings, or functions to be applied
                  to the (invalid) option argument to produce a string.

    :post-validation true/false. By default, validation is performed after
                  parsing an option, prior to assoc/default/update processing.
                  Specifying true here will cause the validation to be
                  performed after assoc/default/update processing, instead.

  parse-opts returns a map with four entries:

    {:options     The options map, keyed by :id, mapped to the parsed value
     :arguments   A vector of unprocessed arguments
     :summary     A string containing a minimal options summary
     :errors      A possible vector of error message strings generated during
                  parsing; nil when no errors exist}

  A few function options may be specified to influence the behavior of
  parse-opts:

    :in-order     Stop option processing at the first unknown argument. Useful
                  for building programs with subcommands that have their own
                  option specs.

    :no-defaults  Only include option values specified in arguments and do not
                  include any default values in the resulting options map.
                  Useful for parsing options from multiple sources; i.e. from a
                  config file and from the command line.

    :strict       Parse required arguments strictly: if a required argument value
                  matches any other option, it is considered to be missing (and
                  you have a parse error).

    :summary-fn   A function that receives the sequence of compiled option specs
                  (documented at #'clojure.tools.cli/compile-option-specs), and
                  returns a custom option summary string.
```
