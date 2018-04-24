cd testserver
if (( $# < 3 )); then
    tests=500
else
    tests=$3
fi
java -jar -Dlogback.configurationFile=logback-tests.xml test_client.jar \
    --tests $tests \
    --name1 ${1##*/} \
    --player1 "../$1.jar" \
    --timeout1 true \
    --name2 ${2##*/} \
    --player2 "../$2.jar" \
    --timeout2 true
