#!/bin/sh
deployedOperators=""
FILE=DeployedOperators.txt
if [[ -f "$FILE" ]]; then
	deployedOperators=`cat DeployedOperators.txt`
fi
echo "$deployedOperators"

Content="nodeA|whatever|0,156|60%|$deployedOperators"
echo $Content > nodeStatusContent
$CCNL_HOME/bin/ccn-lite-mkDSC -s ndn2013 "mgmt/nodeState" -i nodeStatusContent > binaryNodeStatusContent
$CCNL_HOME/bin/ccn-lite-ctrl -u 127.0.0.1/9001 addContentToCache binaryNodeStatusContent -v trace
