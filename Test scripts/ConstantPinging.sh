#!/bin/bash
Interval=$(bc <<< "scale = 0; 1000 / $1")
echo $Interval
screen -d -m bash -c "bash /home/johannes/INetCEP/Test\ scripts/SendI.sh ndn2013 node/nodeA/sensor/victims $Interval trace &> Logs/consumer.log"

