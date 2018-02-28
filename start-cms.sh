#!/bin/sh
java -Dfile.encoding=UTF-8 \
  -XX:MaxGCPauseMillis=100 -XX:GCPauseIntervalMillis=1600 \
  -XX:TargetSurvivorRatio=90 \
  -XX:+UseConcMarkSweepGC -XX:+CMSParallelRemarkEnabled \
  -XX:+UseCMSInitiatingOccupancyOnly -XX:CMSInitiatingOccupancyFraction=50 \
  -XX:+ScavengeBeforeFullGC -XX:+CMSScavengeBeforeRemark \
  -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps \
  -jar Jumper-1.5.0.jar "$@"
