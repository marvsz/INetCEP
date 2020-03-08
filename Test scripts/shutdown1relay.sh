#!/bin/bash
~/INetCEP/ccn-lite/bin/ccn-lite-ctrl -x /tmp/mgmt-nfn-relay-a.sock debug halt | ~/INetCEP/ccn-lite/bin/ccn-lite-ccnb2xml
sleep 10s
pkill -f "nfn"
