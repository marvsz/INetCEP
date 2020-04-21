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
* You need java 11 to build it.

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
    If you encounter any issues with the jar file, use
	```bash
    sbt clean
    ```
    to clean the project and build it again.

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

## Startup
There are two options to start the INetCEP system.
Basically Option 1 automates the entire process and option 2 explains everything in detail in order for the reader to create his own scenario.
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
    ```bash publishRemotely.sh all queryNumber placementType executionSpaceOfWindowOperator runTime
    ```
* An example configuration with query 1, query 1 using the UCL and the nfn-scala window operator with a distributed centralized placement and a runtime of 20 Minutes:
    ```bash
    cd INetCEP/publish_scripts/
    bash publishRemotely.sh all 1 Centralized scala 12000
    ```
* An example configuration with query 1, query 1 using the UCL and the builtin window operator with a distributed centralized placement and a runtime of 20 Minutes:
    ```bash
    cd INetCEP/publish_scripts/
    bash publishRemotely.sh all 1 Centralized builtin 12000
    ```
 * An example configuration with query 1, query 1 using the PRA and the nfn-scala window operator with a distributed centralized placement and a runtime of 20 Minutes:
    ```bash
    cd INetCEP/publish_scripts/
    bash publishRemotely.sh all 6 Centralized scala 12000
    ```

### Setup on GENI resources (OUTDATED)
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
In this part we describe incrementally how each part of the system is started up and how to use everything. We first cover a topology of only one node without the INetCEP Engine in NFN-Scala on top of it. So just the forwarding layer setup, how to add content to the content store of the node, how to start a sensor and how to retrieve content in both user and kernel space. After that we describe how to build interlink nodes in order to build a topology.
Then we describe how to run everything together with INetCEP, how to send queries to the system.
###  Node Startup
Starting up nodes requires the following:
* Startup of the ccn-lite node: Starting a local ccn-lite node with the udp port 9001 and the socket mgmt-nfn-relay-a.sock:
```bash
~/INetCEP/ccn-lite/bin/ccn-lite-relay -v trace -s ndn2013 -u 9001 -x /tmp/mgmt-nfn-relay-a.sock
```
* Linking a nfn-scala compute server with that node on port 9011
```bash
java -jar ~/INetCEP/nfn-scala/target/scala-2.13/nfn-assembly-0.2.2.jar --mgmtsocket /tmp/mgmt-nfn-relay-a.sock --ccnl-port 9001 --cs-port 9011 --debug --ccnl-already-running /node/nodeA
```
###  Node Shutdown
To shut down a node use:
```bash
~/INetCEP/ccn-lite/bin/ccn-lite-ctrl -x /tmp/mgmt-relay-a.sock debug halt | ~/INetCEP/ccn-lite/bin/ccn-lite-ccnb2xml
```
### Inserting content into the content store
To put Data into the content store of a node you first have to create the binary content file and then insert it into the content store. To create a binary content file from a file "victims1" that is located in the directory you are in right now and have it as named data with the name /node/nodeA/sensor/victims/1 use
```bash
~/INetCEP/ccn-lite/bin/ccn-lite-mkC -s ndn2013 "node/nodeA/sensor/victims/1" -i victims1.txt > content.ndntlv
```
Now you have to insert this content into the Content store. Remember, if you send content to the content store and there is no entry in the PIT it will be deleted. That is why we use a add2cache message that generates an interest for the content in order to insert it into the cache. To add the content produced via the above command that resulted in the creation of the binary file "content.ndntlv" to the content store of the node execute:
```bash
~/INetCEP/ccn-lite/bin/ccn-lite-ctrl -x /tmp/mgmt-nfn-relay-a.sock addContentToCache content.ndntlv
```
### Starting a sensor that periodically inserts content into the content store
In order to simulate or emulate a producer we have to use the ccn-lite-mkS command line tool. The command line tool takes several options that are described below:
* -n: The name and of the sensor. This can be one of {victims, survivors, plug, gps} since these are the ones that are implemented for the sensor schema.
* -i: The ID of the sensor, should be unique in combination with the sensor name
* -t: The type of the sensor, 1 for emulation, 2 for simulation. If you chose emulation the -d option becomes mandatory and you have to provide a directory where the trace data for an emulated sensor is located.
* -s: the sampling rate in microseconds
* -x: the unix socket the sensor sends its data to. Is mutually exclusive with the -u option. Can not be used when running the ccn-lite kernel version since you need sudo rights.
* -u: the udp socket the sensor sends its data to. Is mutually exclusive with the -x option. Best to always use this
* -v: The debug level
* -d: The directory when chosing to run an emulated sensor. Here the trace is located.
#### Emulated Sensor
Starting an emulated gps sensor with id 1, sampling rate every 500ms (2 messages per second) and the directory for the data trace at INetCEP/sensors/gps that constantly publishes the data to the ccn-lite node with the socket mgmt-nfn-relay-a.sock:
```bash
~/INetCEP/ccn-lite/bin/ccn-lite-mkS -n gps -i 1 -t 1 -s 500000 -x mgmt-nfn-relay-a.sock -v trace -d ~/INetCEP/sensors/gps
```
Starting an emulated gps sensor with id 1, sampling rate every 500ms (2 messages per second) and the directory for the data trace at INetCEP/sensors/gps that constantly publishes the data to the ccn-lite node with the udp socket 9001:
```bash
~/INetCEP/ccn-lite/bin/ccn-lite-mkS -n gps -i 1 -t 1 -s 500000 -u 127.0.0.1/9001 -v trace -d ~/INetCEP/sensors/gps
```
#### Simulated Sensor
To start a simulated victims sensor with id 1, sampling rate every 500ms (2 messages per second) and that constantly publishes the data to the ccn-lite node with the socket mgmt-nfn-relay-a.sock:
```bash
~/INetCEP/ccn-lite/bin/ccn-lite-mkS -n victims -i 1 -t 2 -s 500000 -x mgmt-nfn-relay-a.sock -v trace
```
To start a simulated victims sensor with id 1, sampling rate every 500ms (2 messages per second) and that constantly publishes the data to the ccn-lite node with the unix socket 9001:
```bash
~/INetCEP/ccn-lite/bin/ccn-lite-mkS -n victims -i 1 -t 2 -s 500000 -u 127.0.0.1/9001 -v trace
```
### Sensor Shutdown
In order to shut down a sensor the following command has to be executed on the machine the sensor runs. The options are
* -n: The name of the sensor
* -i: the id of the sensor
* -v: the log level

To shut down the victims sensor with id 1 execute
```bash
~/INetCEP/ccn-lite/bin/ccn-lite-rmS -n victims -i 1 -v trace
```

### Consumer initiated communication: Sending an Interest for the data node/nodeA/sensor/victims/1

1. Starting a local ccn-lite node
```bash
~/INetCEP/ccn-lite/bin/ccn-lite-relay -v trace -s ndn2013 -u 9001 -x /tmp/mgmt-nfn-relay-a.sock
```
2. Send an interest to the node:
```bash
~/INetCEP/bin/ccn-lite-peek /node/nodeA/sensor/victims/1
```
This does only return content if it actually exists in the relays content store. How to insert content into the content store see above.

### Producer initiated communication: Sending a persistent Interest for the data node/nodeA/sensor/victims/1

1. Starting a local ccn-lite node
```bash
~/INetCEP/ccn-lite/bin/ccn-lite-relay -v trace -s ndn2013 -u 9001 -x /tmp/mgmt-nfn-relay-a.sock
```
2. Send a persistent interest to the node:
```bash
~/INetCEP/bin/ccn-lite-peekConstant node/nodeA/sensor/victims/1
```
This does only return content if it actually exists on the node. If you now start a sensor you will see on your terminal that the data gets forwarded to you. If a sensor is already started you will receive the data immediately.
Note: The peekConstant command line tool resends its persistent interest after three seconds for two times. After that it times out.

### Interconnecting two nodes
In order for two node to interconnect, they need to know their prefixes. In this example two nodes A and B with the prefixes "node/nodeA" and "node/nodeB" are connected.
First we have to start the two nodes in seperate terminals.
Startup node A with UDP address 9001 and UNIX socket mgmt-nfn-relay-a:
```bash
~/INetCEP/ccn-lite/bin/ccn-lite-relay -v trace -s ndn2013 -u 9001 -x /tmp/mgmt-nfn-relay-a.sock
```
Startup node B with UDP address 9002 and UNIX socket mgmt-nfn-relay-b:
```bash
~/INetCEP/ccn-lite/bin/ccn-lite-relay -v trace -s ndn2013 -u 9002 -x /tmp/mgmt-nfn-relay-b.sock
```
Now we have to create forwarding rules. To create the forwarding rule for nodeA to node B:
```bash
FACEID=`~/INetCEP/ccn-lite/bin/ccn-lite-ctrl -x /tmp/mgmt-nfn-relay-a.sock newUDPface any 127.0.0.1 9002 | ~/INetCEP/ccn-lite/bin/ccn-lite-ccnb2xml | grep FACEID | sed -e 's/^[^0-9]*\([0-9]\+\).*/\1/'`
```
Now node A is connected through the UDP face with relay B, but A does not yet have the logical forwarding state to reach B. We have to make a forwarding rule (node/nodeB --> B) in A. This is done via
```bash
~/INetCEP/ccn-lite/bin/ccn-lite-ctrl -x /tmp/mgmt-nfn-relay-a.sock prefixreg node/nodeB $FACEID ndn2013 | ~/INetCEP/ccn-lite/bin/ccn-lite-ccnb2xml
```
Now node B has to be connected to node A via
```bash
FACEID=`~/INetCEP/ccn-lite/bin/ccn-lite-ctrl -x /tmp/mgmt-nfn-relay-b.sock newUDPface any 127.0.0.1 9001 | ~/INetCEP/ccn-lite/bin/ccn-lite-ccnb2xml | grep FACEID | sed -e 's/^[^0-9]*\([0-9]\+\).*/\1/'`
```
and
```bash
~/INetCEP/ccn-lite/bin/ccn-lite-ctrl -x /tmp/mgmt-nfn-relay-b.sock prefixreg node/nodeA $FACEID ndn2013 | ~/INetCEP/ccn-lite/bin/ccn-lite-ccnb2xml
```
respectively.

### Builtin NFN Functions

CCN-Lite offers a way to resolve builtin NFN functions. It offers an add, sub and find function, additionally with the window operator we implemented. The builtin window operator only works together with the UCL.
Example:
Execute the add function to add 1 and 2 on an already started node:
```bash
~/INetCEP/ccn-lite/bin/ccn-lite-simplenfn -s ndn2013 -u 127.0.0.1/9001 "add 1 2" | ~/INetCEP/ccn-lite/bin/ccn-lite-pktdump -f 3
```
We now describe how to run the window operator in ccn-lite:
1. Start the node
	```bash
	~/INetCEP/ccn-lite/bin/ccn-lite-relay -v trace -s ndn2013 -u 9001 -x /tmp/mgmt-nfn-relay-a.sock
	```
2. Start a simulated victims sensor with id 1, sampling rate every 500ms (2 messages per second) and that constantly publishes the data to the ccn-lite node with the UDP socket 9001
	```bash
	~/INetCEP/ccn-lite/bin/ccn-lite-mkS -n victims -i 1 -t 2 -s 500000 -u 127.0.0.1/9001 -v trace
	```
3. Send an NFN Interest for a window with tuple size 5 that reacts to the previously started sensor:
	```bash
	~/INetCEP/ccn-lite/bin/ccn-lite-simplenfnConstant -s ndn2013 -v trace -u 127.0.0.1/9001 "window /node/nodeA/sensor/victims/1 5 1"
	```
In the terminal you sent the constant NFN Interets to you will see the output.

## INetCEP
In order to use the INetCEP streaming system you have to start a node and a NFN-Scala compute server and interconnect those two. We refer to this interconnected system as a "relay". This is not tested with the Kernel version since the startup of NFN-Scala would require sudo rights for nfn-scala on the system.
### Build

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
    If you encounter any issues with the jar file, use
	```bash
    sbt clean
    ```
    to clean the project and build it again.

### Startup
1. Start a ccn-lite node
	```bash
	~/INetCEP/ccn-lite/bin/ccn-lite-relay -v trace -s ndn2013 -u 9001 -x /tmp/mgmt-nfn-relay-a.sock
	```
2. Connect the nfn-scala compute server
	```bash
	java -jar ~/INetCEP/nfn-scala/target/scala-2.13/nfn-assembly-0.2.2.jar --mgmtsocket /tmp/mgmt-nfn-relay-a.sock --ccnl-port 9001 --cs-port 9002 --debug --ccnl-already-running /node/nodeA
	```
3. Start a victims window sensor with a sampling rate of 500 ms
	```bash
	~/INetCEP/ccn-lite/bin/ccn-lite-mkS -n victims -i 1 -t 2 -s 500000 -u 127.0.0.1/9001 -v trace
	```
### Query Execution
We assume that the relay is up and running for the next part.
We execute queries by using the placement functions.
#### UCL
* Execute a placement service on the local node that places a persistent query that filters the data of the sliding window of 5 seconds of the victims sensor by gender = male and age above 30 years while using the nfn-scala window operator:
	```
	$CCNL_HOME/bin/ccn-lite-simplenfn -s ndn2013 -u 127.0.0.1/9001 -w 20 "call 9 /node/nodeA/nfn_service_PlacementServices_QueryPlacement 'local' 'ucl' '1' 'Source' 'Client1' 'FILTER(WINDOW(ucl,node/nodeA/sensor/victims/1,5,S,scala),gender=M&age>30)' 'Region1' '12:06:58.200'" | $CCNL_HOME/bin/ccn-lite-pktdump -f 2
	```
* Execute a placement service on the local node that places a persistent query that filters the data of the sliding window of 5 tuples of the victims sensor by gender = male and age above 30 years while using the builtin window operator:
	```
	$CCNL_HOME/bin/ccn-lite-simplenfn -s ndn2013 -u 127.0.0.1/9001 -w 20 "call 9 /node/nodeA/nfn_service_PlacementServices_QueryPlacement 'local' 'ucl' '1' 'Source' 'Client1' 'FILTER(WINDOW(ucl,node/nodeA/sensor/victims/1,5,S,builtin),gender=M&age>30)' 'Region1' '12:06:58.200'" | $CCNL_HOME/bin/ccn-lite-pktdump -f 2
	```
#### PRA
We need to preface that with the PRA:
1. The placement service needs to know how often to run and
2.  The window operator needs to know the sampling rate of the sensor it sends its requests to.
* Execute a placement service on the local node that places a persistent query that filters the data of the sliding window of 5 seconds of the victims sensor by gender = male and age above 30 years while using the nfn-scala window operator:
	```
	$CCNL_HOME/bin/ccn-lite-simplenfn -s ndn2013 -u 127.0.0.1/9001 -w 20 "call 9 /node/nodeA/nfn_service_PlacementServices_QueryPlacement 'local' 'pra' '1' 'Source' 'Client1' 'FILTER(WINDOW(pra,node/nodeA/sensor/victims/1,5,S,scala,500,MS),gender=M&age>30)' '500' 'MS' 'Region1' '12:06:58.200'" | $CCNL_HOME/bin/ccn-lite-pktdump -f 2
	```
### Distributed Build and Startup
Above we described how to interconnect two nodes. If you use different machines with different IP adresses, you have to do that on them respectively. We will set up our distributed system automatically with the following topology:
```
             (3) -- (7)
             
              |
              
      (1) -- (2) -- (5)
      
              |      |
              
             (4)    (6)
```             
The steps are the following:
1. Navigate to the publish scripts
	```bash
	cd INetCEP/publish_scripts/
	```
2. Run the Setup script. This copies and compiles everything you need to the remote VMs.
	```bash
	bash publishRemotely.sh readmeSetup
	```
3. Start the topology and the sensors needed for the examples below
	```bash
	bash publishRemotely.sh readmeTopology
	```
Now we are ready to execute Queries. 
### Distributed Query Execution
In order to carry out query execution, we can access any node in the network and issue the following query. Here, any node in the network can act as a placement coordinator. Therefore, the query can be issued from any node to any node in the network.
The below query issues a persistent ccn-lite-simplenfn interest to nodeID 28 for a decentralized query processing. The complex query is a filter that works on a window. The filter reduces the contents of the window to only the tuples where the gender is male and the age above 30. The result of the window operator is fed to its parent operator, which once complete, is returned back to the consumer node.
#### UCL
* Execute a placement service on the local node that places a persistent query that filters the data of the sliding window of 5 seconds of the victims sensor by gender = male and age above 30 years while using the nfn-scala window operator:
	```
	$CCNL_HOME/bin/ccn-lite-simplenfn -s ndn2013 -u 127.0.0.1/9001 -w 20 "call 9 /node/nodeA/nfn_service_PlacementServices_QueryPlacement 'Centralized' 'ucl' '1' 'Source' 'Client1' 'FILTER(WINDOW(ucl,node/nodeA/sensor/victims/1,5,S,scala),gender=M&age>30)' 'Region1' '12:06:58.200'" | $CCNL_HOME/bin/ccn-lite-pktdump -f 2
	```
* Execute a placement service on the local node that places a persistent query that filters the data of the sliding window of 5 tuples of the victims sensor by gender = male and age above 30 years while using the builtin window operator:
	```
	$CCNL_HOME/bin/ccn-lite-simplenfn -s ndn2013 -u 127.0.0.1/9001 -w 20 "call 9 /node/nodeA/nfn_service_PlacementServices_QueryPlacement 'Centralized' 'ucl' '1' 'Source' 'Client1' 'FILTER(WINDOW(ucl,node/nodeA/sensor/victims/1,5,S,builtin),gender=M&age>30)' 'Region1' '12:06:58.200'" | $CCNL_HOME/bin/ccn-lite-pktdump -f 2
	```
#### PRA
We need to preface that with the PRA:
1. The placement service needs to know how often to run and
2.  The window operator needs to know the sampling rate of the sensor it sends its requests to.
* Execute a placement service on the local node that places a persistent query that filters the data of the sliding window of 5 seconds of the victims sensor by gender = male and age above 30 years while using the nfn-scala window operator:
	```
	$CCNL_HOME/bin/ccn-lite-simplenfn -s ndn2013 -u 127.0.0.1/9001 -w 20 "call 9 /node/nodeA/nfn_service_PlacementServices_QueryPlacement 'Centralized' 'pra' '1' 'Source' 'Client1' 'FILTER(WINDOW(pra,node/nodeA/sensor/victims/1,5,S,scala,500,MS),gender=M&age>30)' '500' 'MS' 'Region1' '12:06:58.200'" | $CCNL_HOME/bin/ccn-lite-pktdump -f 2
	```
## Kernel Version
We advise to run the kernel version in a secure environment since it is under development and kernel panics can occur. [Read this] and install a crash kernel or you use a VM in VMS.cfg where the crash kernel is already installed.
[Read this]: https://www.linuxjournal.com/content/oops-debugging-kernel-panics-0
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
If you start the kernel version and encounter this log message (Date may vary):

```
ccnl_lxkernel:
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

## Placement Logic
In order to understand the placement logic, we will describe the overall query execution process along with the invoked methods and their uses.

The overall query resolution process has the following steps:
* Issue a complex query (as shown above)
* The complex query is received by the ccn-lite node
* The ccn-lite node looks up the interest, performs lambda-calculus based reduction/closure operations on the interest. Once the call keyword is encountered, the ccn-lite node understands that this is a complex query that has to be send to a compute server. The ccn-lite node then looks at the node prefix passed for computation. Here /node/nodeA is the prefix. Previously, when the compute server started and linked itself with the ccn-lite node, it added this prefix to the face list of the relay. Therefore, the ccn-lite node forwards the interest to the compute server by looking up its face list.
* On receiving the interest from the ccn-lite relay, the compute server performs function resolution through the help of the krivine abstract machine. The abstract machine tells the interest decomposer, the service to invoke (query) and the parameters.
* Once the service has been determined, it is invoked by the interest handler.
* On service invocation, the default class handler is invoked, that matches the arguments and invokes the appropriate handler method. This in usual cases is the 'processQuery' function.
* The processQuery function then sets up the required variables, stores the query in the query store and then creates an operator tree. Each method in the code-base is accompanied with relevant documentation that can be looked up for more information. Once the operator tree has been created, network state discovery is done.
* After network state has been gathered, the paths are build up for placement. Here each path, regardless of hops is looked up, and unique 1-hop...n-hop paths are made. While the paths are being created, on each hop, the adaptive weight application is carried out. Once this is done, each path object then contains the path node information, hops and path cost.
* After the paths have been created, the path that matches the operator count and has the minimum path cost is selected. In decentralized placement, if no path is found, then the query is sent to the best 1-hop neighboring node that carries out this process.
* The selected path is then sent to the ProcessPlacement function, that traverses the path and sets in the appropriate complex queries in it.
* Once the complex query has been set, the ProcessDeployment function is invoked, that executes these queries in the network. Upon gathering the evaluated results for child operators, the top most operator is then evaluated and then the result is returned back to the consumer.
