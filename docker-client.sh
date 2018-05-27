#!/bin/bash
docker run -t -v $(cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd)/clients:/client --workdir /client --memory 1500000000 --cpus=0.8 --user $(id -u) --net=host openjdk:8u151-jre ./start-client.sh "$@"
