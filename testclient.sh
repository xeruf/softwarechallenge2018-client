#!/usr/bin/env bash
cd testserver
if (( $# < 3 )); then
    tests=1000
else
    tests=$3
fi
java -jar -Dlogback.configurationFile=logback-tests.xml test_client.jar \
    --name1 ${1##*/} --player1 "../$1.sh" \
    --name2 ${2##*/} --player2 "../$2.sh" \
    --tests $tests
