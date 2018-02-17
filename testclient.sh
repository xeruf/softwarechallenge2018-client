read -p "Player names" player1 player2
cd Testserver
java -jar -Dlogback.configurationFile=logback-tests.xml test_client.jar \
    --tests 100 \
    --name1 $player1 \
    --player1 "../$player1.jar" \
    --timeout1 true \
    --name2 $player2 \
    --player2 "../$player2.jar" \
    --timeout2 true
pause
