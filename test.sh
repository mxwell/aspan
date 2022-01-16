#! /bin/bash

set -xe

export TEST_JS="BUILD/test.js"

tsc --lib es2015,dom types.ts str_manip.ts rules.ts phonetics.ts question.ts verb.ts test.ts -outFile $TEST_JS

node $TEST_JS