#!/bin/bash
for ((c=1; c<=$1; c++))
do
	~/INetCEP/ccn-lite/bin/ccn-lite-mkC -s ndn2013 "/node/nodeA/FillerContent/$c" -i ~/INetCEP/LatencyTests/filler.txt > ~/INetCEP/LatencyTests/fillerContent$c.ndntlv
done
for ((c=1; c<=$1; c++))
do
	~/INetCEP/ccn-lite/bin/ccn-lite-ctrl -u 127.0.0.1/9003 addContentToCache ~/INetCEP/LatencyTests/fillerContent$c.ndntlv
done
rm fillerContent*
