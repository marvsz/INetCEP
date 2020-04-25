#!/bin/bash
~/INetCEP/ccn-lite/bin/ccn-lite-ctrl -x /tmp/mgmt-nfn-relay-a.sock debug halt | ~/INetCEP/ccn-lite/bin/ccn-lite-ccnb2xml
~/INetCEP/ccn-lite/bin/ccn-lite-ctrl -x /tmp/mgmt-nfn-relay-b.sock debug halt | ~/INetCEP/ccn-lite/bin/ccn-lite-ccnb2xml
pkill -f nfn-relay
pkill -f iperf
pkill -f ccn
pkill -f nfn
pkill -f "nfn"
