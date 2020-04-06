#!/bin/bash
~/INetCEP/ccn-lite/bin/ccn-lite-sendI -s $1 -u 127.0.0.1/9001 "$2" -v trace -r $3 
