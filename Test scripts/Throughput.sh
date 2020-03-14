#!/bin/bash
Interval=$(bc <<< "scale = 0; 1000 / $1")
echo $Interval
screen -d -m bash -c "bash /home/johannes/INetCEP/Test\ scripts/makeSensor.sh 1 2 $Interval mgmt-nfn-relay-a.sock trace &> Logs/producer.log"
