#!/usr/bin/env bash
cd testserver
java -Dfile.encoding=UTF-8 -Dlogback.configurationFile=logback-tests.xml -jar test_client.jar \
    --name1 Jumper-1.6.0 --player1 ../start-1.6.0.sh \
    --name2 Jumper-new --player2 ../start-new.sh \
    --tests 1000
