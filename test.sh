#! /bin/bash

set -xe

export TEST_JS="BUILD/test.js"

tsc --lib es2015,dom src/*.ts test/test.ts -outFile $TEST_JS

node $TEST_JS