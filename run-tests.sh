#!/bin/sh

for v in 9 10 11 12
do
  echo ""
  echo "Running tests for Clojure 1.$v..."
  clojure -M:test:runner:1.$v
  if [ $? -ne 0 ]; then
    echo "Tests failed for Clojure 1.$v"
    exit 1
  fi
done

echo ""
echo "Running tests for ClojureScript..."
clojure -M:test:cljs-runner
if [ $? -ne 0 ]; then
  echo "Tests failed for ClojureScript"
  exit 1
fi
