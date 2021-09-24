#!/usr/bin/env bash
PID=`ps -C java -o pid=`
kill -9 $PID
echo "Stop"
mkdir -p /home/ubuntu/app
echo "Removing the existing processes"
sudo rm -rf /home/ubuntu/app/
cd /opt
sudo mkdir cloudwatch
sudo rm -rf /opt/cloudwatch/cloudwatch-config.json