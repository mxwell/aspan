#! /usr/bin/bash

set -xe

GC_ENDPOINT=${GC_ENDPOINT:-http://127.0.0.1:5000}

curl --verbose \
    -X GET --location "${GC_ENDPOINT}/gcapi/v1/get_untranslated?dst=ru"