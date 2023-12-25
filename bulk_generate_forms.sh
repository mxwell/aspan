#! /bin/bash

set -xe

export COMPILED="BUILD/compiled.js"
export ASPAN_JS="BUILD/aspan.js"
export GENERATOR="scripts/bulk_generate.js"

tsc --lib es2015 src/*.ts -t es6 -outFile $COMPILED

# add lines that export stuff
cat $COMPILED module_export.js > $ASPAN_JS

echo "Reading verbs from input.txt and writing forms to output.txt."
node $GENERATOR
echo "Generating has finished."
