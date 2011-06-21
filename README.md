# tools.cli

tools.cli is a command line argument parser, with the added bonus of
nested groups of arguments.

## Usage

Example:

    (cli args
         (required ["-p" "--port" "Listen on this port"] #(Integer. %))
         (optional ["--host" "The hostname" :default "localhost"])
         (optional ["--verbose" :default true])
         (optional ["--log-directory" :default "/some/path"])
         (group "--server"
                (optional ["-n" "--name"])
                (optional ["-p" "--port"] #(Integer. %))
                (group "--paths"
                       (optional ["--inbound" :default "/tmp/inbound"])
                       (optional ["--outbound" :default "/tmp/outbound"]))))

with args of:

    '("-p" "8080"
      "--no-verbose"
      "--log-directory" "/tmp"
      "--server--name" "localhost"
      "--server--port" "9090"
      "--server--paths--inbound" "/dev/null")

will produce a clojure map with the names picked out for you as keywords:

     {:port 8080
      :host "localhost"
      :verbose false
      :log-directory "/tmp"
      :server {:name "localhost"
               :port 9090
               :paths {:inbound "/dev/null"
                       :outbound "/tmp/outbound"}}}

A flag of -h or --help is provided which will currently give a
documentation string:

    Usage:

     Switches                    Default        Required  Desc          
     --------                    -------        --------  ----          
     -p, --port                                 Yes       Listen on this port              
     --host                      localhost      No        The hostname     
     --verbose                   true           No                      
     --log-directory             /some/path     No        /some/path    
     --server-n, --server--name                 No                      
     --server-p, --server--port                 No                      
     --server--paths--inbound    /tmp/inbound   No        /tmp/inbound  
     --server--paths--outbound   /tmp/outbound  No        /tmp/outbound 

Required parameters will halt program execution if not provided,
optionals will not. Defaults can be provided as shown above. Errors
caused by parsing functions (such as #(Integer. %) above) will halt
program execution. Doc strings are provided as shown above on port and
host.

See the tests for more example usage.

## License

Copyright (c) Rich Hickey and contributors. All rights reserved.

The use and distribution terms for this software are covered by the
Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
which can be found in the file epl.html at the root of this distribution.
By using this software in any fashion, you are agreeing to be bound by
the terms of this license.
You must not remove this notice, or any other, from this software.


