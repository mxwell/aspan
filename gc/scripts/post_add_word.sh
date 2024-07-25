#! /usr/bin/bash

set -xe

if [[ -z "$JWT_TOKEN" ]] ; then
    echo "Error: variable JWT_TOKEN is unset"
    exit 1
fi

GC_ENDPOINT=${GC_ENDPOINT:-http://127.0.0.1:5000}

curl --verbose \
    -X POST --location "${GC_ENDPOINT}/api/v1/add_word" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $JWT_TOKEN" \
    -d @scripts/post_add_word.json
