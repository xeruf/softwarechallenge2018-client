#!/bin/bash
docker run -t -v $(cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd)/clients:/client --workdir /client --memory 1500000000 --cpu-quota=100000 --cap-drop=ALL --cap-add=SETUID --cap-add=SETGID --net=host openjdk:8u151-jre ./start-client.sh "$@"
