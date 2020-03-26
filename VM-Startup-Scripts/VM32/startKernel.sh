#!/bin/bash
cd ~/INetCEP/ccn-lite/src/ccnl-lnxkernel/ccnl-lxkernel
insmod ./ccnl-lxkernel.ko e=eth0 v=trace u=9005 x=/tmp/mgmt-nfn-relay-e.sock ccnl_lxkernel
