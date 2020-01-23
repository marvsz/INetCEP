#!/bin/bash

java -jar /home/johannes/INetCEP/nfn-scala/target/scala-2.13/nfn-assembly-0.2.2.jar --mgmtsocket /tmp/mgmt-nfn-relay-$1.sock --ccnl-port $2 --cs-port $3 --debug --ccnl-already-running /node/nodeA
