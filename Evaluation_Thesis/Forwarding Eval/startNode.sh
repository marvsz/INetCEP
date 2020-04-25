#!/bin/bash
sleep 0.1

~/INetCEP/ccn-lite/bin/ccn-lite-relay -v evaluation -s ndn2013 -u $1 -x $2
