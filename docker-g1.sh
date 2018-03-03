#!/bin/bash
docker run -it --volume $PWD:/client --workdir /client --memory 1500000000 --cpu-quota=100000 --cap-drop=ALL --cap-add=SETUID --cap-add=SETGID --net=host openjdk:8u151-jre ./start-g1.sh
read -rsp $'Press enter to exit'
