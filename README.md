# RoutingManger-API
Brihaspati-4
This is java code for Routing Manager API for Brihaspati-4.
The Main class is RoutingManager.
All the functions associated with this API can be accessed by creating an object of RoutingManager using getInstance method.
Different layers can dynamically be created using this API.
For logging purpose log4j is used.Ensure that log4j jar file is added to the class path.
For cryptographic Module BountyCastle is used,so add corresponding jar file to the class path.
Ensure configuration file is available in the configuration folder before running the code.

**RoutingManager**-It is the main Class of the Routing ManagerAPI.All the methods that the outside world can access, written in this class. It is a singleton class so that only one instance can be made at any point in time. The constructor is used to initialise the application. The various functions performed by the constructor includes checking for a file named NodeDetails.txt from the previous login. This file contains all the Node information and created when the NodeID generated for the first time. Suppose the file exists from the previous login, the data taken from the file for initialisation. Else a new NodeID is generated, and all other associated initialisation will be executed. Thereafter NodeDetails.txt file is created, and the Node information stored for future use. The constructor also checks the Routing table and Neighbour table existence from the previous login(i.e. to checkRoutingTable.xml is available in the path). If the routing table file exists, data is taken from the XML file and added to the localBaseRoutingTable(whichis the routingTable for current Node) and to the localBaseNeighbourTable(which is the neighbour table for the current Node). Suppose file is not available then it will create a routing table(localBaseRoutingTable) and neighbour table(localBaseNeighbourTable).Initial entries of localBaseRoutingTable andlocalBaseNeighbourTable will be an object of B4_Node with only bootstrap node entry.
Dynamic Layering is an important concept related to this API. Few layers implemented as default like BaseRoutingTable, given a Layer ID =0 and StorageRoutingTable, have been given LayerID = 1. New Layer can be added by calling the createNewLayer() method in the routing table mangerAPI. Layer ID is assigned automatically to this Layer. Access to layers can be changed in the config.properties file except for the base layer. 
 
**The Various methods that can be accessible by the outside world are explained in the subsequent paragraphs. The glue code can access these methods to perform various functions on the Routing Module.**

This method returns the B4_Node Object of the current Node. This Object contains all the information about the Node like its NodeID, IP Address, Transport Address, Port Address, RTT value, HashID and Public Key.

This method takes two arguments; first, the name of the file fetched from the input buffer and second is the layer ID of the routing table to which the received routing table needs to be merged. The primary function of this method is to merge the routing table obtained from other nodes in the network to the routing table specified by the layer ID. Merging is done by comparing the Nodes present in the received routing table with the existing node ID one at a time. Initial merging of RoutingTable happens with the routingTable obtained from the Bootstrap Node. Nibble wise comparison is made (between the mergerNodeID and local node ID) to obtain the column in Routing Table at which the data is to be updated. Based on the algorithm, B4_Node will be placed in the predecessor, successor or middle row of the obtained column.

This method takes two arguments; first, the name of the file fetched from the input buffer and second is the layer ID of the Neighbour Table to which the received Neighbour table needs to be merged. The primary function of this method is to merge the Neighbour table obtained from other nodes in the network to the Neighbour table specified by the layer ID. Merging is done by finding the RTT value of all the nodes present in the received Neighbour table and choosing the first sixteen closest nodes based on the RTT value.

This method takes two arguments; first, hash ID received as a query to find the next hop, and second is layer ID on which the operation is to be performed. The function will return a NULL value if the next hop is the current Node, else the B4_Node Object of that particular Node will be returned. This method finds the next hop for a hash ID/Node ID given as an input argument. At first, it is checked whether the hash ID/Node ID  is equal to the local node ID. After that, check whether the local Node is the root node for the given hash ID/Node ID. Thereafter nibble wise comparison is made, and the first nibble mismatch between hash ID and Local Node ID is identified. It will give the value of k (i.e. column at which we start looking for next-hop). Suppose this column is not empty, then check predecessor successor and middle row one by one based on the logic defined to get the next hop.

This method takes three arguments; first, the layer ID, which specifies the routing layer on which the operation is to be performed, second is the Routing table reference, and third is the neighbour table reference. This method will start a separate thread that will continuously monitor the neighbour table and routing table after a predefined time (which changes dynamically). The Nodes that are not alive will be removed from the routing and the neighbour table. The number of times the loop will run to check the Node reachability can be changed by changing the value of PurgeLoopCount in the config file.

This method takes one argument: the reference of the file that needs to be added to the input buffer. Once the file is added successfully, it will return “True”. The glue code can use this method to add a file in the input buffer of the routing manager API.

This method takes one argument: the reference of the file that needs to be added to the output buffer. Once the file is added successfully, it will return “True”. The routing manager API will use this method to add a file to the output buffer.

This method fetches files from the input buffer one at a time. A separate thread will start when this method is called. It will continuously monitor the input buffer and fetch the file one by one and give it to the respective functions for processing.

This method fetches files from the output buffer one at a time. This method will be used by the glue code to fetch files from the output buffer.



This method takes three arguments; first, the digital signature: the hash ID associated with a Node, second is the Public Key, and third is the Node ID. This function will return “True” if the Node is verified. The actual implementation of this function is in the cryptographic module of the Routing Manager API.

This method will return the IP Address of the system in which the application is running.

This method will return the MAC Address of the system in which the application is running.

This method takes one argument, i.e. layer Name; It is the name that the user wants to give to the new Layer. This function will return a “True” value when the Layer created successfully. Thereafter the routing table and neighbour table for that particular Layer will be generated.

This method will return the IPAddress of the current Node.


