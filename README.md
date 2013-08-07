# tools.cli

tools.cli is a command line argument parser for Clojure.

## Releases and Dependency Information

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


