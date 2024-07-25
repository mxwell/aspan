#! /usr/bin/bash

set -xe

GC_ENDPOINT=${GC_ENDPOINT:-http://127.0.0.1:5000}

curl --verbose \
    -X POST --location "${GC_ENDPOINT}/api/v1/add_translation" \
    -H "Content-Type: application/json" \
    -d @scripts/post_add_translation.json
