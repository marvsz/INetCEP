# Building CCN-lite

## Prerequisites

CCN-lite requires OpenSSL. Use the following to install it:
* Ubuntu: `sudo apt-get install libssl-dev`
* OS X: `brew install openssl`

## Build

1.  Clone the repository:
    ```bash
    git clone https://github.com/marvsz/INetCEP
    ```
    Or clone the ccn-lite folder from this repository.

2.  Set environment variable `$CCNL_HOME` and add the binary folder of CCN-lite to your `$PATH`:
    Default:
    ```bash
    export CCNL_HOME="`pwd`/InetCEP/ccn-lite"
    export PATH=$PATH:"$CCNL_HOME/bin"
    ```
    To make these variables permanent, add them to your shell's `.rc` file, e.g. `~/.bashrc`.
    

3.  Build CCN-lite using `make`:
    ```bash
    cd $CCNL_HOME/
    mkdir build
    cd build
    export USE_NFN=1
    export USE_NACK=1
    cmake ../src
    make clean all
    ```

# Building NFN-Scala

## Prerequisites

* Get the customized nfn-scala folder from the working repository.
* Place it in your preferred directory. E.g. /home/project/nfn-scala/
* Make sure that SBT compiler is available on your machine.

## Build

1.  Navigate to the path where nfn-scala folder is placed (example):
    
    ```bash
    cd /home/project/nfn-scala/
    ```

2.  This directory contains the build.sbt file. Use this file to compile your sources:

    ```bash
    sbt compile
    ```

3.  Once the nfn-scala code compiles successfully, produce a JAR file for the Compute Server using:

    ```bash
    sbt assembly
    ```

    Once your assembly file (.JAR) has been created, it will be placed in the ../nfn-scala/target/scala-2.13/ folder. Use the .jar file to start the compute server.


* This completes the build procedures for CCN-Lite and NFN-Scala.

# INetCEP

## Prerequisites
* Copy your public key to the remote machine using `copyKeys.sh` script
```
bash copyKeys.sh
```
* Install dependencies and copy the binaries to remote machines by 
```
bash publishRemotely.sh install
```
* Set environment variables on the remote machines using `setEnvironment.sh` script
```
bash setEnvironment.sh
```

There are two options to start SACEPICN system. Basically Option 1 automates the entire process of option 2. 
Note: Follow only one of the below options.

## Option 1

To start INetCEP on the cluster of resources (see VMS.cfg), please follow the steps below: 

### Setup on MAKI compute resources

1. create your personalized VM configiration file `VMS.cfg` (refer VMS_MAKI1.cfg and VMS_MAKI2.cfg) for your user and IP addresses. 
2. auto generate node data for the respective machines with the following topology:
```
             (3) -- (7)
             
              |
              
      (1) -- (2) -- (5)
      
              |      |
              
             (4)    (6)
```             
using `python generate_node_info.py`. Note: this script uses VMS.cfg as input for IP address information. Ports used are 9001, 9001,..,9001+n. (n: number of nodes).

In the Publishremotely file are different pre-defined queries. They can be executed by
    ```bash publishRemotely.sh all "Placement" queryNumber timeOut runTime
    ```
A example configuration with query 1, timeout of 20s and a runTime of 20 Minutes is:
    ```bash
    cd INetCEP/publish_scripts/
    bash publishRemotely.sh all "Placement" 1 20 1200
    ```

### Setup on GENI resources
This needs to be reworked and is currently not working anymore.
1. generate the Rspec file to request resources on GENI using `python generate_rspec.py <number of nodes> <out dir>`
2. upload the rspec, select site and reserve resources. 
3. download the manifest rspec file with IP address and port information. 
4. auto generate VMS.cfg file (refer VMS_GENI.cfg) and node data using `python manifest_to_config.py <manifest.xml>`

After the above setup execute the publishRemotely.sh script that publishes the application on either of the above resources based on the VMS.cfg file. Refer `publish_scripts/publishRemotely.sh` script. Usage:

```
bash publish_scripts/publishRemotely.sh all "Placement" [Query] [QueryStorageReadPeriod] [ShutdownTimer]
```
Query is a number that determines, which query is to be executed: 
{1: Window, 2: Filter(Window), 3: Join(Filter(Window),Filter(Window), 4: Join(Predict2(Window),Predict2(Window)), 5: Filter(Join(Predict2(Window),Predict2(Window))), 6: Heatmap(Join(Window,Window))}.
QueryStorageReadPeriod is a value in seconds that determines the interval at which the Query Store is read.
ShutdownTimer is a value in Seconds that determnies after how many seconds the nodes are shut down.

## Option 2

### Local Node Startup
Starting up nodes requires the following:
* Startup of the ccn-lite relay: Starting a local ccn-lite relay with the udp port 9998 and the socket mgmt-nfn-relay-a.sock:
```
bash ~/INetCEP/ccn-lite/bin/ccn-lite-relay -v trace -s ndn2013 -u 9998 -x /tmp/mgmt-nfn-relay-a.sock
```
This is automated and can be executed through
```
bash start1node.sh
```
* Linking a nfn-scala compute server with that relay on port 9999
```
java -jar ~/INetCEP/nfn-scala/target/scala-2.13/nfn-assembly-0.2.2.jar --mgmtsocket /tmp/mgmt-nfn-relay-a.sock --ccnl-port 9998 --cs-port 9999 --debug --ccnl-already-running /node/nodeA
```
The whole process of starting a relay and connecting the nfn-scala compute server can be automated with
```
bash start1relay.sh
```

### Sending an Interest for the data nodeA/sensor/victims/ -> Consumer initiated communication

1. Starting a local ccn-lite relay and nfn-scala server:
```
bash start1relay.sh
```
2. Sending an interest to the relay:
```
bash ~/INetCEP/bin/ccn-lite-peek /ndoeA/sensor/victims/1
```
This does only return content if it acutally exists on the relay

### Sending a persistent Interest for the data nodeA/sensor/victims/1 -> Producer initiated communication

1. Starting a local ccn-lite relay and nfn-scala server:
```
bash start1relay.sh
```
2. Sending a persistent to the relay:
```
bash ~/INetCEP/bin/ccn-lite-peekConstant /ndoeA/sensor/victims/1
```
This does only return content if it acutally exists on the relay

### Getting producer initiated content -> Producer initiated communication

1. Starting a local ccn-lite relay and nfn-scala server:
```
bash start1relay.sh
```

2. Starting an emulated gps sensor with id 1, sampling rate every 500ms and the directory for the data trace at INetCEP/sensors/gps that constantly publishes the data to the ccn-lite relay with the socket mgmt-nfn-relay-a.sock:
```
~/INetCEP/ccn-lite/bin/ccn-lite-mkS -n gps -i 1 -t 1 -s 500000 -x mgmt-nfn-relay-a.sock -v trace -d ~/INetCEP/sensors/gps
```
Or you start a simulated victims sensor with id 1, sampling rate every 500ms and that constantly publishes the data to the ccn-lite relay with the socket mgmt-nfn-relay-a.sock:
```
~/INetCEP/ccn-lite/bin/ccn-lite-mkS -n victims -i 1 -t 2 -s 500000 -x mgmt-nfn-relay-a.sock -v trace
```

3. Sending a persistent interest in the victims sensor to the relay:
```
bash ~/INetCEP/bin/ccn-lite-peek /node/nodeA/sensor/victims/1
```
This returns content whenever the sensor sends it to the relay.

### Local Query Execution

Execution NFN Functions works in different ways. We can use the builtin NFN Functions from ccn-lite or the NFN Functions provided by NFN-Scala. We describe different ways to execute those functions.

1. NFN-Scala sliding Window Operator of 5 Seconds:
```
~/INetCEP/ccn-lite/bin/ccn-lite-simplenfn -s ndn2013 -u 127.0.0.1/9998 -w 10 "call 3 /node/nodeA/nfn_service_Window 'Pull' 'nodeA/sensor/victims/1' '5' 'S'" | ~/INetCEP/ccn-lite/bin/ccn-lite-pktdump -f 2
```

2. Persistent NFN-Scala sliding Window Operator of 5 Seconds:
```
~/INetCEP/ccn-lite/bin/ccn-lite-simplenfn -s ndn2013 -u 127.0.0.1/9998 -w 10 "call 3 /node/nodeA/nfn_service_Window 'Push' 'nodeA/sensor/victims/1' '5' 'S'" | ~/INetCEP/ccn-lite/bin/ccn-lite-pktdump -f 2
```

3. Persistent buildin Window Operator of 5 Seconds:
```
~/INetCEP/ccn-lite/bin/ccn-lite-simplenfn -s ndn2013 -u 127.0.0.1/9998 "window /nodeA/sensor/victims/1 4 1" | ~/INetCEP/ccn-lite/bin/ccn-lite-pktdump -f 3
```

4. To execute a placement service on the local node that places a persistent query that filters the data of the sliding window of 5 seconds of the victims sensor by Male and people above 30 years of age while using the nfn-scala window operator:
```
$CCNL_HOME/bin/ccn-lite-simplenfn -s ndn2013 -u 127.0.0.1/9998 -w 20 "call 9 /node/nodeA/nfn_service_PlacementServices_QueryPlacement 'local' 'local' '1' 'Source' 'Client1' 'FILTER(WINDOW(Push,nodeA/sensor/victims/1,5,S,scala),gender=M&age>30)' 'Region1' '12:06:58.200'" | $CCNL_HOME/bin/ccn-lite-pktdump -f 2
```

5. To execute a placement service on the local node that places a persistent query that filters the data of the sliding window of 5 seconds of the victims sensor by Male and people above 30 years of age while using the builtin window operator:
```
$CCNL_HOME/bin/ccn-lite-simplenfn -s ndn2013 -u 127.0.0.1/9998 -w 20 "call 9 /node/nodeA/nfn_service_PlacementServices_QueryPlacement 'local' 'local' '1' 'Source' 'Client1' 'FILTER(WINDOW(Push,nodeA/sensor/victims/1,5,S,builtin),gender=M&age>30)' 'Region1' '12:06:58.200'" | $CCNL_HOME/bin/ccn-lite-pktdump -f 2
```

## Kernel Version
We advise to run the kernel version in a secure environment since it is under developement and kernel panicks can occure. Read this https://www.linuxjournal.com/content/oops-debugging-kernel-panics-0 and install a crashkernel or you use a VM in VMS.cfg where the crashkernel is already installed.
This tutorial assumes you are on VM28.
To compile the kernel version navigate to
```
cd INetCEP/ccn-lite/src/ccnl-lnxkernel/
```
Herer you have to compile the kernel with
```
cmake .
make
```
After that navigate to 
```
cd ccnl-lxkernel/
```
To actually see the kernel output on in the terminal you have to run 
```
sudo tail -f /var/log/syslog
```
in a seperate Terminal that is ssh'd into the VM.
Now we insert the kernel module with
```
sudo insmod ./ccnl-lxkernel.ko e=eth0 v=trace x=/tmp/mgmt-nfn-relay-a.sock u=9001
```
This tells the kernel to have the socket at /tmp/mgmt-nfn-relay-a.sock and to listen to udp port 9001

To stop the kernel module simply run
```
sudo rmmod ccnl-lxkernel
```
If you start the kernel version and encounter this log message:
ccnl_lxkernel:
```
Apr 17 16:45:07 unassigned-hostname kernel: [1402381.848375] Error -98 binding UNIX socket to /tmp/mgmt-nfn-relay-a.sock (remove first, check access rights)
```
That means that you first have to remove the previous socket with 
```
sudo rm /tmp/mgmt-nfn-relay-a.sock
```
When the kernel module is up and running we can use the unified communication layer by first starting a sensor in a seperate terminal via
```
~/INetCEP/ccn-lite/bin/ccn-lite-mkS -n victims -i 1 -t 2 -s 500000 -u 127.0.0.1/9001 -v trace
```

In another seperate terminal you can run the constantPeek to see that you get the output from the sensor with
```
ccn-lite-peekConstant /node/nodeA/sensor/victims/1 -u 127.0.0.1/9001 -v trace
```

In order to execute the window operator for this sensor simply run
```
ccn-lite-simplenfn -s ndn2013 -v trace -u 127.0.0.1/9001 "window /node/nodeA/sensor/victims/1 4 1" | ~/INetCEP/ccn-lite/bin/ccn-lite-pktdump -f 3
```
## Query Execution

In order to carry out query execution, we can access any node in the network and issue the following query. Here, any node in the network can act as a placement coordinator. Therefore, the query can be issued from any node to any node in the network.


    $CCNL_HOME/bin/ccn-lite-simplenfn -s ndn2013 -u 10.2.1.28/9001 -w 20 "call 9 /node/nodeA/nfn_service_Placement 'Centralized' 'Centralized' '1' 'Source' 'Client1' 'FILTER(WINDOW(Push,nodeA/sensor/victims/1,5,S,builtin),gender=M&age>30)' 'Region1' '16:22:00.200'" | $CCNL_HOME/bin/ccn-lite-pktdump -f 2


The above query issues a persistent ccn-lite-simplenfn interest to nodeID 28 for a decentralized query processing. The complex query is a filter that works on a window. The filter reduces the contents of the window to only the tuples where the gender is male and the age above 30. The result of the window operator is fed to its parent operator, which once complete, is returned back to the consumer node.

## Placement Logic
OUTDATED
In order to understand the placement logic, we will describe the overall query execution process along with the invoked methods and their uses.

The overall query resolution process has the following steps:
* Issue a complex query (as shown above)
* The complex query is received by the ccn-lite node
* The ccn-lite node looks up the interest, performs lambda-calculus based reduction/closure operations on the interest. Once the call keyword is encountered, the ccn-lite node understands that this is a complex query that has to be send to a compute server. The ccn-lite node then looks at the node prefix passed for computation. Here /node/nodeA is the prefix. Previously, when the compute server started and linked itself with the ccn-lite node, it added this prefix to the face list of the relay. Therefore, the ccn-lite node forwards the interest to the compute server by looking up its face list.
* On receiving the interest from the ccn-lite relay, the compute server performs function resolution through the help of the abstract machine. The abstract machine tells the interest decomposer, the service to invoke (query) and the parameters.
* Once the service has been determined, it is invoked by the interest handler.
* On service invocation, the default class handler is invoked, that matches the arguments and invokes the appropriate handler method. This in usual cases is the 'processQuery' function.
* The processQuery function then sets up the required variables, stores the query in the query store and then creates an operator tree. Each method in the code-base is accompanied with relevant documentation that can be looked up for more information. Once the operator tree has been created, network state discovery is done.
* After network state has been gathered, the paths are build up for placement. Here each path, regardless of hops is looked up, and unique 1-hop...n-hop paths are made. While the paths are being created, on each hop, the adaptive weight application is carried out. Once this is done, each path object then contains the path node information, hops and path cost.
* After the paths have been created, the path that matches the operator count and has the minimum path cost is selected. In decentralized placement, if no path is found, then the query is sent to the best 1-hop neighboring node that carries out this process.
* The selected path is then sent to the ProcessPlacement function, that traverses the path and sets in the appropriate complex queries in it.
* Once the complex query has been set, the ProcessDeployment function is invoked, that executes these queries in the network. Upon gathering the evaluated results for child operators, the top most operator is then evaluated and then the result is returned back to the consumer.




