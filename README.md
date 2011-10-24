# tools.cli

tools.cli is a command line argument parser for Clojure.

## An Example

    (cli args
         ["-p" "--port" "Listen on this port" :required true :parse-fn #(Integer. %)] 
         ["-t" "--host" "The hostname" :default "localhost"]
         ["-v" "--[no-]verbose" :default true]
         ["-l" "--log-directory" :default "/some/path"])
         
with args of:

    ["-p" "8080"
     "--no-verbose"
     "--log-directory" "/tmp"]

will produce a clojure map with the names picked out for you as keywords:

     {:port 8080
      :host "localhost"
      :verbose false
      :log-directory "/tmp"}

A flag of -h or --help is provided which will print a documentation
string to STDOUT and call System/exit:

    Usage:

     Switches                    Default        Required  Desc          
     --------                    -------        --------  ----          
     -p, --port                                 Yes       Listen on this port              
     -h, --host                  localhost      No        The hostname     
     -v, --no-verbose --verbose  true           No                      
     -l, --log-directory         /some/path     No        

## Options

An argument is specified by providing a vector of information:

Switches should be provided first, from least to most specific. The
last switch you provide will be used as the name for the argument in
the resulting hash-map. The following:

    ["-p" "--port"]

defines an argument with two possible switches, the name of which will
be :port in the resulting hash-map.

Next is an optional doc string:

    ["-p" "--port" "The port to listen on"]

This will be printed in the 'Desc' column if the -h or --help flags
are provided.

Following that are optional parameters, provided in key-value pairs:

    ["-p" "--port" "The port to listen on" :default 8080 :parse-fn #(Integer. %) :required true]

These should be self-explanatory. The defaults if not provided are as follows:

    {:default  nil
     :parse-fn identity
     :required false}

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
  
    => {:verbose true}

    (cli ["--no-verbose"]
         ["-v" "--[no-]verbose"])

    => {:verbose false}

Note: there is no short-form to set the flag to false (-no-v will not
work!). 

## Trailing Arguments

After all of your arguments have been parsed, any trailing arguments
given will be available to your program under a key called :args in
the resulting hash-map:

    (cli ["--port" "9999" "some" "extra" "arguments"]
         ["--port" :parse-fn #(Integer. %)])

    => {:port 9999, :args ["some" "extra" "arguments"]}

This allows you to deal with parameters such as filenames which are
commonly provided at the end of an argument list.

## License

Copyright (c) Rich Hickey and contributors. All rights reserved.

The use and distribution terms for this software are covered by the
Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
which can be found in the file epl.html at the root of this distribution.
By using this software in any fashion, you are agreeing to be bound by
the terms of this license.
You must not remove this notice, or any other, from this software.


