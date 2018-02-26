#!/bin/sh
java -Dfile.encoding=UTF-8 \
  -XX:MaxGCPauseMillis=200 -XX:GCPauseIntervalMillis=2050 \
  -XX:TargetSurvivorRatio=90 \
  -XX:+UseG1GC \
  -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps \
  -jar Jumper-1.4.1.jar "$@"
