package main;

import org.apache.log4j.Logger;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.KeyStore;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by S/L Umesh U Nair
 * <p>
 * <br> Aim is to create a Routing Manager API for Brihaspati-4
 * <br> This is the main class.
 * <br> The functions associated with the routing manager API can be accessed using this class.
 * <br> Few  layers were implemented as default:-
 * <br> 1. BaseRoutingTable - LayerID = 0
 * <br> 2. StorageRoutingTable - LayerID = 1
 * <br> Rest of the layer will be created as per the requirement of the user.
 * <br> Access to various layers can be controlled through the config.properties file.
 * <br> New Layer can be added by calling createNewLayer() method in the RoutingManager.
 */
public class RoutingManager {
    private static final Logger log = Logger.getLogger(RoutingManager.class);
    private static RoutingManager routingManager;
    private static RoutingManagerBuffer routingManagerBuffer;
    private static ConfigData config;
    private final ArrayList<B4_RoutingTable> routingTables;
    private final ArrayList<B4_Node> differentialNodes;
    private final String layerFile;
    private final int rt_dimension;
    private final int nt_dimension;
    private final long incrementTime;
    private final long sleepTime;
    private final DateTimeCheck dateTimeCheck;
    private final NodeCryptography nodeCryptography;
    private final B4_NodeGeneration b4_nodeGeneration;
    private Utility utility;
    private B4_Node localNode;
    private String selfIPAddress;
    private String selfTransportAddress;
    private String selfPortAddress;

    /**
     * Constructor
     * <br> Main job of constructor are as follows:-
     * <br> Check NodeDetails.txt file exits from the previous login, if true take data from the file else generate nodeDetails.txt file and nodeDetails generated for future use.
     * <br> NodeDetails.txt file contain all the information about the node which is generated from the previous login.
     * <br> Check routing table and neighbour table exist from the previous login(ie to check RoutingTable.xml is available in the path).
     * <br> If RT exists then data is taken from the xml file and added to the localBaseRoutingTable(which is the routingTable for current node)
     * and to the localBaseNeighbourTable(which is the neighbourTable for current node).
     * <br> If not available then create a routing table(localBaseRoutingTable) and neighbour table (localBaseNeighbourTable).
     * <br> Initial entries of localBaseRoutingTable and localBaseNeighbourTable should be object of main.resources.B4_Node with only bootstrap node entry.
     */
    private RoutingManager() {
        config = ConfigData.getInstance();
        routingTables = new ArrayList<>();
        differentialNodes = new ArrayList<>();
        dateTimeCheck = new DateTimeCheck();
        routingManagerBuffer = RoutingManagerBuffer.getInstance();
        nodeCryptography = NodeCryptography.getInstance();
        String nodeDetailFilePath = config.getValue("NodeDetailsPath");
        layerFile = config.getValue("LayerDetailsPath");
        boolean nodeDetailsExists;
        File nodeFile = new File(nodeDetailFilePath);
        nodeDetailsExists = nodeFile.exists();
        b4_nodeGeneration = B4_NodeGeneration.getInstance();
        if (!nodeDetailsExists) {
            b4_nodeGeneration.initiateNodeGenerationProcess();
            utility = new Utility();
            selfIPAddress = getSystemIP();
            selfPortAddress = String.valueOf(config.getPortAddress());
            selfTransportAddress = config.getTransportAddress();
            generateNodeDetailsFile(nodeDetailFilePath);
        } else {
            try {
                FileReader reader = new FileReader(nodeDetailFilePath);
                Properties properties = new Properties();
                properties.load(reader);
                String selfNodeID = properties.getProperty("NodeID");
                String selfPublicKey = properties.getProperty("PublicKey");
                String selfHashID = properties.getProperty("HashID");
                selfIPAddress = properties.getProperty("IPAddress");
                selfPortAddress = properties.getProperty("PortAddress");
                selfTransportAddress = properties.getProperty("TransportAddress");
                utility = new Utility();
                b4_nodeGeneration.setNodeID(selfNodeID);
                b4_nodeGeneration.setPublicKey(utility.strToPub(selfPublicKey));
                b4_nodeGeneration.setHashID(selfHashID);
            } catch (IOException e) {
                log.error("NodeDetails File not Found or Issue in file fetching\n", e);
            }
        }
        rt_dimension = config.getRoutingTableLength();
        nt_dimension = config.getNeighbourTableLength();
        incrementTime = config.getIncrementTime();
        sleepTime = config.getSleepTime();
        setLocalNode();
        addToArrayList();
        initialLayerLoading();
        getFileFromInputBuffer();
        Runtime runtime = Runtime.getRuntime();
        runtime.addShutdownHook(new Thread(() -> {
            log.info("System is going to shutdown");
            log.info("Backing up system configuration ");
            dateTimeCheck.setLastLogoutTime();
        }));
    }

    /**
     * @return RoutingManger Object
     * <br>This method is required to create an instance of RoutingManager.
     * <br>Instance of RoutingManager will be obtained by calling this function.
     * <br>Only one Instance will be created for RoutingManager class.
     */
    public static synchronized RoutingManager getInstance() {
        if (routingManager == null) {
            routingManager = new RoutingManager();
        }
        return routingManager;
    }

    /**
     * @param rtTag          Tag name to be added to the XML file.
     * @param rtFileName     Name of the routing table which we desired to give for later identification.
     * @param routingTable   Object of Routing Table.
     * @param neighbourTable Object of Neighbour Table.
     *                       <br>All the initialisation w.r.t routing Manager will be performed here.
     *                       <br>This function is called by the constructor for initialisation of routing manager.
     *                       <br>Initialisation includes creating routingTable and neighbour table,creating a routing table file for future references etc.
     */
    private void init(String rtTag, String rtFileName, B4_Node[][] routingTable, B4_Node[] neighbourTable) {
        boolean rtExists;
        File rtFile = new File(rtFileName + ".xml");
        rtExists = rtFile.exists();
        if (!rtExists) {
            /* Routing Table */
            for (int i = 0; i < rt_dimension; i++) {
                for (int j = 0; j < 3; j++) {
                    routingTable[i][j] = new B4_Node(new B4_NodeTuple("", null, ""), "", "", "");
                }
            }
            B4_Node bootStrapNode = config.getBootStrapNode();
            mergerRT(bootStrapNode, routingTable);
            /* Neighbour Table */
            for (int i = 0; i < nt_dimension; i++) {
                neighbourTable[i] = new B4_Node(new B4_NodeTuple("", null, ""), "", "", "", -1);
            }
            routingTableToXML(rtTag, rtFileName, routingTable, neighbourTable);
        } else {
            fetchFromXML(rtFileName, routingTable, neighbourTable);
            log.debug("New RoutingTable file created for future use");
        }
    }

    /**
     * @return B4_Node Object
     * <br>This method is used for getting Local Node Information.
     * <br>This method can be called by any function to get complete information about the current Node.
     */
    public B4_Node getLocalNode() {
        return localNode;
    }

    /**
     * @param fileName Name of the file that is fetched from the input buffer of the routingManger API.
     * @param layerID  Specify the layer Id of the routing table which needs to be merged.
     *                 <br>This method is used for merging routing table obtained from other B4_Node in to localBaseRoutingTable.
     *                 <br>Merging is performed by one by one comparing of nodeID obtained from the received node with existing nodeID in the localBaseRoutingTable.
     *                 <br>Initial merging of localBaseRoutingTable happens with the routing Table obtained from the Bootstrap Node.
     *                 <br>Nibble wise comparison is done(b/w mergerTableNodeId and localNodeID) to obtain the column in localBaseRoutingTable
     *                 Array at which the data is to be updated.
     *                 <br>Based on the algorithm the B4_Node will be place in the predecessor, successor or middle row of the obtained column.
     */

    public void mergeRoutingTable(File fileName, int layerID) {
        B4_Node[][] routingTableLayer = routingTables.get(layerID).getRoutingTable();
        B4_Node selfNodeOfMergerTable = getSelfNodeOfMergerTable(fileName.getAbsolutePath());
        B4_Node[][] mergerRoutingTable = getMergerRoutingTable(fileName.getAbsolutePath());
        mergerRT(selfNodeOfMergerTable, routingTableLayer);
        for (int i = 0; i < rt_dimension; i++) {
            for (int j = 0; j < 3; j++) {
                mergerRT(mergerRoutingTable[i][j], routingTableLayer);
            }
        }
        B4_Layer b4_layer = new B4_Layer();
        String layerName = b4_layer.getLayerName(layerID);
        routingTableToXML(layerName, layerID + "_" + layerName + "_" + localNode.getB4node().getNodeID(), routingTables.get(layerID).getRoutingTable(), routingTables.get(layerID).getNeighbourTable());
        log.info(layerName + " Merging completed Successfully");
        String selfNodeID=getNodeID();
        File file = createDifferentialTable("DifferentialRoutingTableNodes","DiffR_"+layerID+"_RoutingTable_"+selfNodeID);
        addFileToOutputBuffer(file);
        log.info(layerName + " Differential Routing Table File added to the Output Buffer");
    }

    /**
     * @param fileName File fetched from the inputBuffer of routing Table
     * @param layerID  The layer in which the operation is to be performed
     *                 Function is used to merge neighbour table received file from other nodes with the neighbour table of current node.
     */
    public void mergeNeighbourTable(File fileName, int layerID) {
        B4_Node[] neighbourTable = routingTables.get(layerID).getNeighbourTable();
        boolean rttFileExists;
        B4_Node[] mergerNeighbourTable = new B4_Node[nt_dimension];
        B4_Node selfMergerNode = null;
        File rttFile = new File(fileName.getName());
        rttFileExists = rttFile.exists();
        if (!rttFileExists) {
            log.error("RTT updated file does not exist");
        } else {
            //Get Document builder
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder;
            try {
                documentBuilder = builderFactory.newDocumentBuilder();
                Document doc = documentBuilder.parse(new File(fileName.getName()));
                doc.getDocumentElement().normalize();
                String selfNodeID = doc.getDocumentElement().getAttribute("SELF_NODE_ID");
                String selfNodePub = doc.getDocumentElement().getAttribute("SELF_PUBLIC_KEY");
                String selfHashID = doc.getDocumentElement().getAttribute("SELF_HASHID");
                String selfIPAddress = doc.getDocumentElement().getAttribute("SELF_IP_ADDRESS");
                String selfPortAddress = doc.getDocumentElement().getAttribute("SELF_PORT_ADDRESS");
                String selfTransport = doc.getDocumentElement().getAttribute("SELF_TRANSPORT");
                String selfRTT = doc.getDocumentElement().getAttribute("SELF_RTT");
                selfMergerNode = new B4_Node(new B4_NodeTuple(selfNodeID, utility.strToPub(selfNodePub), selfHashID), selfIPAddress, selfPortAddress, selfTransport, Float.parseFloat(selfRTT));

                NodeList nodeList = doc.getElementsByTagName("NEIGHBOUR");
                for (int i = 0; i < nodeList.getLength(); i++) {
                    Node node = nodeList.item(i);
                    if (node.getNodeType() == node.ELEMENT_NODE) {
                        Element element = (Element) node;
                        //Get the value of ID attribute
                        String index = node.getAttributes().getNamedItem("INDEX").getNodeValue();
                        //Get value of all sub-Elements
                        String nodeID = element.getElementsByTagName("NODEID").item(0).getTextContent();
                        String nodePub = element.getElementsByTagName("PUBLICKEY").item(0).getTextContent();
                        String hashID = element.getElementsByTagName("HASHID").item(0).getTextContent();
                        String nodeIP = element.getElementsByTagName("NODEIP").item(0).getTextContent();
                        String nodePort = element.getElementsByTagName("NODEPORT").item(0).getTextContent();
                        String nodeTransport = element.getElementsByTagName("NODETRANSPORT").item(0).getTextContent();
                        String nodeRTT = element.getElementsByTagName("NODERTT").item(0).getTextContent();
                        Pattern pattern = Pattern.compile("\\[([^]]+)\\]");
                        Matcher matcher = pattern.matcher(index);
                        matcher.find();
                        int index1 = Integer.parseInt(matcher.group(1));
                        mergerNeighbourTable[index1] = new B4_Node(new B4_NodeTuple(nodeID, utility.strToPub(nodePub), hashID), nodeIP, nodePort, nodeTransport, Float.parseFloat(nodeRTT));
                    }
                }
            } catch (ParserConfigurationException | SAXException | IOException e) {
                log.error("Exception Occurred", e);
            }
            for (int i = 0; i < nt_dimension; i++) {
                assert selfMergerNode != null;
                if (selfMergerNode.getRtt() == -1) break;
                assert neighbourTable != null;
                if (neighbourTable[i].getRtt() == -1) {
                    neighbourTable[i] = selfMergerNode;
                    break;
                } else if (neighbourTable[i].getRtt() <= selfMergerNode.getRtt()) {
                } else {
                    for (int j = nt_dimension - 1; j >= i + 1; j--) {
                        neighbourTable[j] = neighbourTable[j - 1];
                    }
                    neighbourTable[i] = selfMergerNode;
                }
            }
            for (int i = 0; i < nt_dimension; i++) {
                for (int j = 0; j < nt_dimension; j++) {
                    if (mergerNeighbourTable[i].getRtt() == -1) break;
                    assert neighbourTable != null;
                    if (neighbourTable[j].getRtt() == -1) {
                        neighbourTable[j] = mergerNeighbourTable[i];
                        break;
                    } else if (mergerNeighbourTable[i].getRtt() >= neighbourTable[j].getRtt()) {
                    } else {
                        for (int k = nt_dimension - 1; k >= j + 1; k--) {
                            neighbourTable[k] = neighbourTable[k - 1];
                        }
                        neighbourTable[j] = mergerNeighbourTable[i];
                        break;
                    }
                }
            }
            for (int i = 0; i < nt_dimension; i++) {
                assert neighbourTable != null;
            }
            B4_Layer b4_layer = new B4_Layer();
            String layerName = b4_layer.getLayerName(layerID);
            routingTableToXML(layerName, layerID + "_" + layerName + "_" + localNode.getB4node().getNodeID(), routingTables.get(layerID).getRoutingTable(), routingTables.get(layerID).getNeighbourTable());
            log.info(layerName + " Merged successfully");
        }
    }

    /**
     * @param hashID  hash ID received as a query to find the next hop.
     * @param layerID layer id on which the operation is to be performed.
     * @return null if next hop is selfNode else return B4_Node object.
     * <p>
     * <br>This method is used to find the nextHop for a hashID/NodeID which is given as an input argument.
     * <br>Initially check whether the hashId/nodeId is equal to localNodeID.
     * <br>Thereafter check whether the localNode is the root node for the given hashId/NodeId.
     * <br>Thereafter Nibble wise comparison is made and the first nibble mismatch between hashId and LocalNodeId is identified.
     * <br>This will give value of k (i.e column at which we start looking for next hop).
     * <br>If this column is not empty check predecessor successor and middle row one by one based on the logic defined to get the next hop.
     * <br>Check whether hashId/NodeId lies between predecessor and localNodeId.
     * <br>Since nibbles are arranged in the form of a ring ranging from 0-15, all possible conditions needs to be checked.
     * <br>Like predecessor < localNodeId or predecessor > localNodeId and hashId< localNodeID or hashId > localNodeId in a circle
     * <br>If true move to next column and check next nibble lies between next nibble of predecessor and localNodeId.
     * <br>This is Iterated till last column and still hashId nibble lies between predecessor and localNodeId then localNodeId is the root Node.
     * <br>Hence return Null.
     * <br>Else Check whether hashId/NodeId lies between localNodeId and successor.
     * <br>Since nibbles are arranged in the form of a ring ranging from 0-15, all possible conditions needs to be checked.
     * <br>If true return successorNodeId.
     * <br>Else Check whether hashId/NodeId lies between middle and predecessor.
     * <br>Since nibbles are arranged in the form of a ring ranging from 0-15, all possible conditions needs to be checked.
     * <br>If true return middleNodeId.
     */
    public B4_Node findNextHop(String hashID, int layerID) {
        B4_Node[][] routingTable = routingTables.get(layerID).getRoutingTable();
        String localNodeID = localNode.getB4node().getNodeID();
        //System.out.println("HashID  : " + hashID + " " + "  LocalID  : " + localNodeID);
        char[] hashIdC = hashID.toCharArray();
        char[] localNodeIdC = localNodeID.toCharArray();
        if (hashID.equals(localNodeID)) {
            log.info("Current Node is the Root Node");
        } else {
            for (int k = 0; k < rt_dimension; k++) {
                if (hashIdC[k] != localNodeIdC[k]) {
                    String hashIdChar = Character.toString(hashID.charAt(k));
                    String localNodeIdChar = Character.toString(localNodeID.charAt(k));
                    int hashIdHex = Integer.parseInt(hashIdChar, 16);
                    int localNodeIdInHex = Integer.parseInt(localNodeIdChar, 16);
                    if (!routingTables.get(0).getRoutingTable()[k][0].getB4node().getNodeID().isEmpty()) {
                        assert routingTable != null;
                        String preNodeIdChar = Character.toString(routingTable[k][0].getB4node().getNodeID().charAt(k));
                        String sucNodeIdChar = Character.toString(routingTable[k][1].getB4node().getNodeID().charAt(k));
                        int preNodeIdHex = Integer.parseInt(preNodeIdChar, 16);
                        int sucNodeIdHex = Integer.parseInt(sucNodeIdChar, 16);

                        if (preNodeIdHex <= hashIdHex && hashIdHex < localNodeIdInHex || preNodeIdHex > localNodeIdInHex && preNodeIdHex - 16 <= hashIdHex && hashIdHex < localNodeIdInHex || preNodeIdHex > localNodeIdInHex && preNodeIdHex - 16 <= hashIdHex - 16 && hashIdHex - 16 < localNodeIdInHex - 16) {
                            if (k != rt_dimension - 1) {
                                for (int i = k + 1; i < rt_dimension; i++) {
                                    if (!routingTable[i][0].getB4node().getNodeID().isEmpty()) {
                                        String nxtPreNodeIdChar = Character.toString(routingTable[i][0].getB4node().getNodeID().charAt(i));
                                        String nxtHashIdChar = Character.toString(hashID.charAt(i));
                                        String nxtLocalNodeIdChar = Character.toString(localNodeID.charAt(i));
                                        String nxtSucNodeIdChar = Character.toString(routingTable[i][1].getB4node().getNodeID().charAt(i));

                                        int nxtPreNodeIdHex = Integer.parseInt(nxtPreNodeIdChar, 16);
                                        int nxtHashIdHex = Integer.parseInt(nxtHashIdChar, 16);
                                        int nxtLocalNodeIdInHex = Integer.parseInt(nxtLocalNodeIdChar, 16);
                                        int nxtSucNodeIdHex = Integer.parseInt(nxtSucNodeIdChar, 16);

                                        if (nxtPreNodeIdHex <= nxtHashIdHex && nxtHashIdHex < nxtLocalNodeIdInHex || nxtPreNodeIdHex > nxtLocalNodeIdInHex && nxtPreNodeIdHex - 16 <= nxtHashIdHex && nxtHashIdHex < nxtLocalNodeIdInHex || nxtPreNodeIdHex > nxtLocalNodeIdInHex && nxtPreNodeIdHex - 16 <= nxtHashIdHex - 16 && nxtHashIdHex - 16 < nxtLocalNodeIdInHex - 16) {
                                            if (i != rt_dimension - 1) continue;
                                            else return null;
                                        } else if (nxtSucNodeIdHex >= nxtHashIdHex && nxtHashIdHex > nxtLocalNodeIdInHex || nxtSucNodeIdHex < nxtLocalNodeIdInHex && nxtSucNodeIdHex + 16 >= nxtHashIdHex && nxtHashIdHex > nxtLocalNodeIdInHex || nxtSucNodeIdHex < nxtLocalNodeIdInHex && nxtSucNodeIdHex + 16 >= nxtHashIdHex + 16 && nxtHashIdHex + 16 > nxtLocalNodeIdInHex) {
                                            return routingTable[i][1];
                                        } else if (!routingTable[i][2].getB4node().getNodeID().isEmpty()) {
                                            String nxtMidNodeIdChar = Character.toString(routingTable[i][2].getB4node().getNodeID().charAt(i));
                                            int nxtMidNodeIdHex = Integer.parseInt(nxtMidNodeIdChar, 16);
                                            if (nxtSucNodeIdHex < nxtHashIdHex && nxtHashIdHex < nxtMidNodeIdHex || nxtSucNodeIdHex > nxtMidNodeIdHex && nxtSucNodeIdHex - 16 < nxtHashIdHex && nxtHashIdHex < nxtMidNodeIdHex || nxtSucNodeIdHex > nxtMidNodeIdHex && nxtSucNodeIdHex - 16 < nxtHashIdHex - 16 && nxtHashIdHex - 16 < nxtMidNodeIdHex) {
                                                return routingTable[i][1];
                                            } else if (nxtPreNodeIdHex > nxtHashIdHex && nxtHashIdHex > nxtMidNodeIdHex || nxtPreNodeIdHex < nxtMidNodeIdHex && nxtPreNodeIdHex + 16 > nxtHashIdHex && nxtHashIdHex > nxtMidNodeIdHex || nxtPreNodeIdHex < nxtMidNodeIdHex && nxtPreNodeIdHex + 16 > nxtHashIdHex + 16 && nxtHashIdHex + 16 > nxtMidNodeIdHex) {
                                                return routingTable[i][2];
                                            }
                                        } else if (routingTable[i][2].getB4node().getNodeID().isEmpty()) {
                                            return routingTable[i][1];
                                        }
                                        if (i == rt_dimension - 1) return null;
                                    }
                                }
                            } else return null;
                        } else if (sucNodeIdHex >= hashIdHex && hashIdHex > localNodeIdInHex || sucNodeIdHex < localNodeIdInHex && sucNodeIdHex + 16 >= hashIdHex && hashIdHex > localNodeIdInHex || sucNodeIdHex < localNodeIdInHex && sucNodeIdHex + 16 >= hashIdHex + 16 && hashIdHex + 16 > localNodeIdInHex) {
                            return routingTable[k][1];
                        } else if (!routingTable[k][2].getB4node().getNodeID().isEmpty()) {
                            String midNodeIdChar = Character.toString(routingTable[k][2].getB4node().getNodeID().charAt(k));
                            int midNodeIdHex = Integer.parseInt(midNodeIdChar, 16);
                            if (sucNodeIdHex < hashIdHex && hashIdHex < midNodeIdHex || sucNodeIdHex > midNodeIdHex && sucNodeIdHex - 16 < hashIdHex && hashIdHex < midNodeIdHex || sucNodeIdHex > midNodeIdHex && sucNodeIdHex - 16 < hashIdHex - 16 && hashIdHex - 16 < midNodeIdHex) {
                                return routingTable[k][1];
                            } else if (preNodeIdHex > hashIdHex && hashIdHex > midNodeIdHex || preNodeIdHex < midNodeIdHex && preNodeIdHex + 16 > hashIdHex && hashIdHex > midNodeIdHex || preNodeIdHex < midNodeIdHex && preNodeIdHex + 16 > hashIdHex + 16 && hashIdHex + 16 > midNodeIdHex) {
                                return routingTable[k][2];
                            }
                        } else if (routingTable[k][2].getB4node().getNodeID().isEmpty()) {
                            return routingTable[k][1];
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * @param layerID        layerID of the routing table.
     * @param routingTable   Routing Table name based on the layer which it defines.
     * @param neighbourTable Neighbour table name based on layer which it defines.
     *                       <p>
     *                       <br>The number of times the loop will run to check whether the node is reachable/alive can be changed by changing the PurgeLoopCount
     *                       value in the config file.
     */
    public void purgeRTEntry(int layerID, B4_Node[][] routingTable, B4_Node[] neighbourTable) {
        //Two counter arrays were created to keep track of no of failed ping.
        int[][] counter_rtable = new int[rt_dimension][3];
        int[] counter_neighbour = new int[nt_dimension];
        int purgeLoopCount = config.getPurgeLoopCount();
        B4_Layer b4_layer = new B4_Layer();
        String layerName = b4_layer.getLayerName(layerID);
        Thread purgeThread = new Thread(() -> {
            int count = 0;
            int dataPurged_RT = 0;
            int dataPurged_Neighbour = 0;
            long sleepingTime = 0;
            while (true) {
                while (!(count >= purgeLoopCount)) {
                    for (int i = 0; i < rt_dimension; i++) {
                        for (int j = 0; j < 3; j++) {
                            String ipAddressBase = routingTable[i][j].getIpAddress();
                            //System.out.println(ipAddressBase);
                            if (!ipAddressBase.isEmpty()) {
                                try {
                                    InetAddress ping = InetAddress.getByName(ipAddressBase);
                                    if (ping.isReachable(1000)) {
                                        System.out.println("Host is Reachable");
                                        counter_rtable[i][j] = 0;
                                    } else {
                                        System.out.println("Not reachable");
                                        counter_rtable[i][j] = counter_rtable[i][j] + 1;
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                            if (counter_rtable[i][j] == purgeLoopCount) {
                                routingTable[i][j] = new B4_Node(new B4_NodeTuple("", null, ""), "", "", "");
                                System.out.println("Data is purged");
                                dataPurged_RT = dataPurged_RT + 1;
                                counter_rtable[i][j] = 0;
                            }
                        }
                    }
                    for (int k = 0; k < nt_dimension; k++) {
                        String ipAddressNeighbour = neighbourTable[k].getIpAddress();
                        if (!ipAddressNeighbour.isEmpty()) {
                            try {
                                InetAddress ping = InetAddress.getByName(ipAddressNeighbour);
                                if (ping.isReachable(1000)) {
                                    System.out.println("Host is Reachable");
                                    counter_neighbour[k] = 0;
                                } else {
                                    System.out.println("Not reachable");
                                    counter_neighbour[k] = counter_neighbour[k] + 1;
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        if (counter_neighbour[k] == purgeLoopCount) {
                            neighbourTable[k] = new B4_Node(new B4_NodeTuple("", null, ""), "", "", "", -1);
                            System.out.println("Data is purged");
                            dataPurged_Neighbour = dataPurged_Neighbour + 1;
                            counter_neighbour[k] = 0;
                        }
                    }
                    routingTableToXML(layerName, layerID + "_" + layerName + "_" + localNode.getB4node().getNodeID(), routingTable, neighbourTable);
                    count = count + 1;
                }
                count = 0;
                if (dataPurged_RT == 0 && dataPurged_Neighbour == 0) {
                    try {
                        sleepingTime = sleepingTime + incrementTime;
                        log.debug("Purge function sleeping for " + sleepingTime);
                        Thread.sleep(sleepingTime);
                        dataPurged_Neighbour = 0;
                        dataPurged_RT = 0;
                    } catch (InterruptedException e) {
                        log.error("Exception Occurred", e);
                    }
                } else {
                    try {
                        sleepingTime = sleepTime;
                        System.out.println("going for sleeping for " + sleepingTime);
                        Thread.sleep(sleepingTime);
                        dataPurged_Neighbour = 0;
                        dataPurged_RT = 0;
                    } catch (InterruptedException e) {
                        log.error("Exception Occurred", e);
                    }
                }
            }
        });
        purgeThread.start();
    }

    /**
     * @param file File needs to be added to the inputBuffer.
     * @return True if file is added successfully.
     * <br> This method is used to add a file to the input buffer.This method will be called by the glue code to add the file to the input buffer.
     */
    public boolean addFileToInputBuffer(File file) {
        boolean isAdded = false;
        isAdded = routingManagerBuffer.addToInputBuffer(file);
        return isAdded;
    }

    /**
     * @param file File needs to be added to the outputBuffer.
     * @return True if file is added successfully.
     * <br> This method is used to add file to the output buffer.
     * <br> The file added to the output buffer will be take by the glue code and send it to the intended external module.
     */
    public boolean addFileToOutputBuffer(File file) {
        boolean isAdded = false;
        isAdded = routingManagerBuffer.addToOutputBuffer(file);
        return isAdded;
    }

    /**
     * <br> This method is used to fetch file from the input buffer one by one.
     * <br> When this method is called a separate thread will run which will continuously scan input buffer for any file.
     * <br> If any file is found it will be fetch and given to respective functions for execution.
     */
    public void getFileFromInputBuffer() {
        Thread fetchThread = new Thread(() -> {
            while (true) {
                File file = routingManagerBuffer.fetchFromInputBuffer();
                if (!(file == null)) {
                    log.debug(file.getName());
                    log.debug("New file fetched from InputBuffer");
                    B4_Layer b4_layer = new B4_Layer();
                    int layerID = b4_layer.fetchMaxLayerID();
                    for (int i = 0; i <= layerID; i++) {
                        if (file.getName().startsWith("" + i + "")) {
                            boolean isAccess = config.isLayerAccess(b4_layer.getLayerName(i));
                            if (isAccess) {
                                mergeRoutingTable(file, i);
                                generateRTTMergerTable(file, i);
                                log.info("Routing Table Updated !!!");
                            }
                        }
                        if (file.getName().startsWith("RcvRTT_" + i)) {
                            mergeNeighbourTable(file, i);
                            log.info("Neighbour Table updated !!!");
                        }
                        if (file.getName().startsWith("Table"+i)){
                            responseForIndexingManager("Table"+i+"_RootNodeCheck.xml");
                            log.info("Indexing response Generation completed !!!");
                        }
                        if(file.getName().startsWith(("DiffR_"+i))){

                            log.info("Routing Table Updated !!!");
                        }
                    }
                }
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    log.error("Exception Occurred", e);
                }
            }
        });
        fetchThread.start();
    }

    /**
     * @return File
     * <br> This function can be used by the glue code to fetch file from the output buffer in a sequential order.
     */
    public File getFileFromOutputBuffer() {
        return routingManagerBuffer.fetchFromOutputBuffer();
    }

    /**
     * @param digitalSignature Digital signature as input argument.
     * @param publicKey        Public key as input argument.
     * @param nodeID           NodeID as input argument.
     * @return True if the signature is verified successfully.
     * <br> This function verify a node for its authentication.
     * <br> The function is implemented internally.It can only be accessed from the routingManager class.
     */
    public boolean verifySignature(String digitalSignature, PublicKey publicKey, String nodeID) {
        boolean verify;
        verify = b4_nodeGeneration.verifySignature(digitalSignature, publicKey, nodeID);
        return verify;
    }

    /**
     * @return System IP address.
     */
    public String getSystemIP() {
        NetworkInterface networkInterface;
        String ethernet;
        String selfIPAddress = "";
        String regex = "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";
        try {
            String OSName = System.getProperty("os.name");
            if (OSName.contains("Windows")) {
                selfIPAddress = InetAddress.getLocalHost().getHostAddress();
            } else {
                try {
                    for (Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces(); interfaces.hasMoreElements(); ) {
                        networkInterface = interfaces.nextElement();
                        ethernet = networkInterface.getDisplayName();
                        if (!(ethernet.equals("lo"))) {
                            if (!(ethernet.contains("br"))) {
                                InetAddress inetAddress = null;
                                for (Enumeration<InetAddress> ips = networkInterface.getInetAddresses(); ips.hasMoreElements(); ) {
                                    inetAddress = ips.nextElement();
                                    if (Pattern.matches(regex, inetAddress.getCanonicalHostName())) {
                                        selfIPAddress = inetAddress.getCanonicalHostName();
                                        return selfIPAddress;
                                    }
                                }
                                assert inetAddress != null;
                                String pip = inetAddress.toString();
                                int mark = pip.indexOf("/");
                                int cutAt = mark + 1;
                                selfIPAddress = pip.substring(cutAt);
                                return selfIPAddress;
                            }
                        }
                    }
                } catch (SocketException e) {
                    log.error("Exception Occurred", e);
                }
            }
            return selfIPAddress;
        } catch (Exception e) {
            log.error("Exception Occurred", e);
            return null;
        }
    }

    /**
     * @return System MAC Address in String.
     */
    public String getSystemMACAddress() {
        String macaddr = "";
        try {
            String ipAddress = getSystemIP();
            NetworkInterface network = NetworkInterface.getByInetAddress(InetAddress.getByName(ipAddress));
            byte[] mac = network.getHardwareAddress();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < mac.length; i++) {
                sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
            }
            log.debug("Current MAC address : " + sb.toString());
            macaddr = sb.toString();
        } catch (Exception e) {
            log.error("Exception Occurred", e);
        }
        return macaddr;
    }

    /**
     * @param layerName Layer name is given as input argument.
     * @return True if the layer is created successfully.
     * <br> This function is used to create a new layer.
     */
    public int createNewLayer(String layerName) {
        boolean hasAccess;
        int layerID = 0;
        B4_Layer newLayer = new B4_Layer();
        String nodeID = getLocalNode().getB4node().getNodeID();
        boolean isAdded = newLayer.addToLayeringDetailsFile(layerName);
        if (isAdded) {
            newLayer.amendLayerFile();
            hasAccess = config.checkLayerName(layerName);
            if (!hasAccess) {
                config.addToConfigFile(layerName);
            }
            layerID = addNewLayerToArrayList();
            System.out.println(layerID + "_" + layerName + "_" + nodeID);
            init(layerName, layerID + "_" + layerName + "_" + nodeID, routingTables.get(layerID).getRoutingTable(), routingTables.get(layerID).getNeighbourTable());
        }
        return layerID;
    }

    /**
     * @return IP Address of the local Node in String.
     */
    public String getIPAddress() {
        return localNode.getIpAddress();
    }

    /**
     * @return NodeID of the local Node in String.
     */
    public String getNodeID() {
        return localNode.getB4node().getNodeID();
    }

    /**
     * @return PortAddress of the local Node in String.
     */
    public String getPortAddress() {
        return localNode.getPortAddress();
    }

    /**
     * @return Rtt Value of the local Node in float.
     */
    public float getRTT() {
        return localNode.getRtt();
    }

    /**
     * @return HashID of the local Node in String.
     */
    public String getHashID() {
        return localNode.getB4node().getHashID();
    }

    /**
     * @return PublicKey of local Node.
     */
    public PublicKey getPublicKey() {
        return localNode.getB4node().getPublicKey();
    }

    /**
     * @param layerID LayerID is given as the input argument.
     * @return Routing Table of local node in B4_Node[][].
     */
    public B4_Node[][] getRoutingTable(int layerID) {
        return routingTables.get(layerID).getRoutingTable();
    }

    /**
     * @param layerID LayerID is given as the input argument.
     * @return Neighbour Table of local Node in B4_Node[].
     */
    public B4_Node[] getNeighbourTable(int layerID) {
        return routingTables.get(layerID).getNeighbourTable();
    }

    /**
     * @return True if date and time of the system matched with the authentication server.
     */
    public boolean dateTimeCheck() {
        return dateTimeCheck.checkDateTime();
    }

    /**
     * @return current date and time in String format.
     */
    public String getCurrentDateTime() {
        return dateTimeCheck.getCurrentDateTime();
    }

    /**
     * @return last logout time of the node from the network.
     */
    public String getLastLogoutTime() {
        return dateTimeCheck.getLastLogoutTime();
    }

    /**
     * @return True if the self signed certificate is renewed.
     */
    public boolean renewSelfSignedCertificate() {
        return nodeCryptography.updateCertificate();
    }

    /**
     * @return Keystore of the local node.
     */
    public KeyStore getKeystore() {
        return nodeCryptography.getKeyStore();
    }

    /**
     * <br> If the initial generated nodeId is already available in the network then this function can be called and
     * a new nodeID will be created.
     * <br> It also update the nodeDetails.txt file and old nodeID details will be deleted.
     */
    public void generateNewNodeID() {
        String filePath = "KeyStore.ks";
        File keyStoreFile = new File(filePath);
        boolean keyStoreFileExist = keyStoreFile.exists();
        if (keyStoreFileExist) {
            boolean isDeleted = keyStoreFile.delete();
            if (isDeleted) log.debug("Keystore file deleted");
        }
        String nodeDetailFilePath = config.getValue("NodeDetailsPath");
        File nodeFile = new File(nodeDetailFilePath);
        boolean nodeDetailsExists;
        nodeDetailsExists = nodeFile.exists();
        if (nodeDetailsExists) {
            boolean isDeleted = nodeFile.delete();
            if (isDeleted) log.debug("NodeDetails file deleted");
        }
        nodeCryptography.newNodeIDProcess();
        b4_nodeGeneration.newNodeGenProcess();
        generateNodeDetailsFile(nodeDetailFilePath);
        B4_Layer b4_layer = new B4_Layer();
        int maxLayerID = b4_layer.fetchMaxLayerID();
        for (int i = 0; i <= maxLayerID; i++) {
            String layerName = b4_layer.getLayerName(i);
            File fileRoutingTable = getRoutingXMLFile(layerName, i);
            boolean isRTFileDeleted = fileRoutingTable.delete();
            if (isRTFileDeleted) log.debug("Initial " + layerName + " file deleted");
        }
        setLocalNode();
        addToArrayList();
        initialLayerLoading();
        getFileFromInputBuffer();
    }

    /**
     * @param routingTableName Name of the routingTable
     * @param layerID          Layer which want to access.
     * @return File - The XML file of routingTable and NeighbourTable of local node of respective layer.
     * <br> The glue code can send this file other node for merging.
     */
    public File getRoutingXMLFile(String routingTableName, int layerID) {
        File routingTableFile = new File(layerID + "_" + routingTableName + "_" + localNode.getB4node().getNodeID() + ".xml");
        boolean isExist = routingTableFile.exists();
        if (isExist) return routingTableFile;
        else return null;
    }

    /**
     * @return File Object of routingTable.
     */
    public File getRoutingTableXMLFile(String XMLTag, String fileName, B4_Node[][] routingTable) {
        File file = routingTable1ToXML(XMLTag, fileName, routingTable);
        boolean isDeleted = file.delete();
        if (!isDeleted)
            log.debug("NodeDetails file not deleted");
        return file;
    }

    /**
     * @return File Object of NeighbourTable.
     */
    public File getNeighbourTableXMLFile(String XMLTag, String fileName, B4_Node[] neighbourTable) {
        File file = neighbourTableToXML(XMLTag, fileName, neighbourTable);
        boolean isDeleted = file.delete();
        if (!isDeleted)
            log.debug("NodeDetails file not deleted");
        return file;
    }

    /**
     * @param indexFile Name of the Index file whose response needs to be send.
     * @return response in XML File for indexingManger
     */
    public boolean responseForIndexingManager(String indexFile) {
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = null;
        File file = null;
        boolean fileAdded= false;
        try {
            documentBuilder = builderFactory.newDocumentBuilder();
            Document doc = documentBuilder.parse(new File(indexFile));
            doc.getDocumentElement().normalize();
            Element element1 = doc.getDocumentElement();
            Element element2 = doc.createElement("ResponseToIndexingManager");
            String layerIDS = doc.getDocumentElement().getAttribute("LayerID");
            int layerID = Integer.parseInt(layerIDS);
            NamedNodeMap attrs = element1.getAttributes();
            for (int i = 0; i < attrs.getLength(); i++) {
                Attr attr2 = (Attr) doc.importNode(attrs.item(i), true);
                element2.getAttributes().setNamedItem(attr2);
            }
            while ((element1.hasChildNodes())) {
                element2.appendChild(element1.getFirstChild());
            }
            element1.getParentNode().replaceChild(element2, element1);

            NodeList nodeList1 = doc.getElementsByTagName("DATA");
            for (int i = 0; i < nodeList1.getLength(); i++) {
                Node node = nodeList1.item(i);
                if (node.getNodeType() == node.ELEMENT_NODE) {
                    Element element = (Element) node;
                    String index = node.getAttributes().getNamedItem("INDEX").getNodeValue();
                    String key = element.getElementsByTagName("KEY").item(0).getTextContent();
                    if (findNextHop(key, layerID) == null) {
                        element.getElementsByTagName("NEXTHOP").item(0).setTextContent("RootNode");
                    } else {
                        element.getElementsByTagName("NEXTHOP").item(0).setTextContent(findNextHop(key, layerID).getB4node().getNodeID());
                    }
                }
            }
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource domSource = new DOMSource(doc);
            file = new File("ResponseToIndexManager.xml");
            StreamResult streamResult = new StreamResult(file);
            transformer.transform(domSource, streamResult);
            log.debug("ResponseToIndexManager.xml" + "file created");
            addFileToOutputBuffer(file);
            fileAdded=true;
        } catch (ParserConfigurationException | SAXException | IOException | TransformerException e) {
            log.error("Exception Occurred", e);
        }
        log.info("ResponseToIndexManager.xml" + "file send to output buffer");
        return fileAdded;
    }

    /**
     * @param nodeDetailFilePath File path.
     *                           <p>
     *                           <br> This function is private. Cannot be accessed by outside world.
     *                           <br> It will populate initial entry in to NodeDetails.txt file.
     */
    private void generateNodeDetailsFile(String nodeDetailFilePath) {
        try {
            FileWriter writer = new FileWriter(nodeDetailFilePath);
            PrintWriter printWriter = new PrintWriter(writer);
            printWriter.println("#  Self Node Details  #");
            printWriter.println("..................................");
            printWriter.println("NodeID=" + b4_nodeGeneration.getNodeID());
            printWriter.println("PublicKey=" + utility.pubToStr(b4_nodeGeneration.getPublicKey()));
            printWriter.println("HashID=" + b4_nodeGeneration.getHashID());
            printWriter.println("IPAddress=" + selfIPAddress);
            printWriter.println("PortAddress=" + selfPortAddress);
            printWriter.println("TransportAddress=" + selfTransportAddress);
            printWriter.flush();
            printWriter.close();
            log.debug("NodeDetail File created successfully");
        } catch (IOException e) {
            log.error("Exception Occurred", e);
        }
    }

    /**
     * <br>This method is used for setting Local Node information.
     * <br>Presently it is hardcoded (will be amended later).
     */
    private void setLocalNode() {
        localNode = new B4_Node(new B4_NodeTuple(b4_nodeGeneration.getNodeID(), b4_nodeGeneration.getPublicKey(), b4_nodeGeneration.getHashID()), selfIPAddress, selfPortAddress, selfTransportAddress);
    }

    /**
     * @param mergerFile The file from which the merger routing table needs to be created.
     * @return B4_RoutingTable Object
     */
    private B4_Node[][] getMergerRoutingTable(String mergerFile) {
        B4_MergeRoutingTable mergerTable = new B4_MergeRoutingTable(mergerFile);
        return mergerTable.getRoutingTable();
    }

    /**
     * @param mergerFile The file from which the merger neighbour table needs to be created.
     * @return B4_RoutingTable Object
     */
    private B4_Node[] getMergerNeighbourTable(String mergerFile) {
        B4_MergeRoutingTable mergerTable = new B4_MergeRoutingTable(mergerFile);
        return mergerTable.getNeighbourTable();
    }

    /**
     * @param mergerFile-Merger File need to be added
     * @return B4_Node Object
     */
    private B4_Node getSelfNodeOfMergerTable(String mergerFile) {
        B4_MergeRoutingTable mergerTable = new B4_MergeRoutingTable(mergerFile);
        return mergerTable.getSelfNode();
    }

    /**
     * @param mergerNode   - The node which need to be merged.
     * @param routingTable All the algorithm for merging the routing and neighbour table is defined in this function.
     *                     <br>Check for the first mismatch in nibble between mergerNodeId and localNodeId.
     *                     <br>IF predecessor,successor and middle entry is empty, mergerNode is added to predecessor and successor.
     *                     <br>Else, conditions were checked one by one.
     *                     <br>First condition is if mergerNodeId lies between the existing predecessor and localNodeId
     *                     <br>Second condition is mergerNodeId lies between the localNodeId and existing SuccessorNodeId
     *                     <br>Third condition is if mergerNodeId lies between successor and predecessor.
     *                     <br>Following is for checking the first Condition ie mergerNodeId lies between predecessor and localNodeId.
     *                     <br>Since we are looking into a circular ring with nibble value range from 0-15,all possible conditions need to be checked.
     *                     <br>Like Predecessor > LocalNodeId or Predecessor < LocalNodeId  and similarly for all cases of mergerNodeId.
     *                     <br>Like mergerNodeId > LocalNodeId or mergerNodeId < LocalNodeId.
     *                     <br>Following is for checking the second condition ie mergerNodeId lies between successor and localNodeId.
     *                     <br>Since we are looking into a circular ring with nibble value range from 0-15,all possible conditions need to be checked.
     *                     <br>Like Successor > LocalNodeId or Successor < LocalNodeId  and similarly for all cases of mergerNodeId.
     *                     <br>Like mergerNodeId > LocalNodeId or mergerNodeId < LocalNodeId.
     *                     <br>Following is for checking the Third condition ie mergerNodeId lies between predecessor and successor.
     *                     <br>Since we are looking into a circular ring with nibble value range from 0-15,all possible conditions need to be checked.
     */
    private void mergerRT(B4_Node mergerNode, B4_Node[][] routingTable) {
        int preNodeIdInHex;
        int sucNodeIdInHex;
        String mergerNodeID = mergerNode.getB4node().getNodeID();
        String localNodeID = localNode.getB4node().getNodeID();
        char[] mergerNodeidInCharArray = mergerNodeID.toCharArray();
        char[] localNodeidInCharArray = localNodeID.toCharArray();

        for (int k = 0; k < rt_dimension; k++) {
            char[] preNodeIdCharArray = routingTable[k][0].getB4node().getNodeID().toCharArray();
            char[] sucNodeIdCharArray = routingTable[k][1].getB4node().getNodeID().toCharArray();
            if (mergerNodeidInCharArray[k] != localNodeidInCharArray[k]) {
                String mergerNodeIdChar = Character.toString(mergerNodeID.charAt(k));
                String localNodeIdChar = Character.toString(localNodeID.charAt(k));
                int mergerNodeIdInHex = Integer.parseInt(mergerNodeIdChar, 16);
                int localNodeIdInHex = Integer.parseInt(localNodeIdChar, 16);

                if (routingTable[k][0].getB4node().getNodeID().isEmpty() && routingTable[k][1].getB4node().getNodeID().isEmpty() && routingTable[k][2].getB4node().getNodeID().isEmpty()) {
                    routingTable[k][0] = mergerNode;
                    routingTable[k][1] = mergerNode;
                    differentialNodes.add(mergerNode);
                    break;
                } else {
                    String preNodeIdChar = Character.toString(routingTable[k][0].getB4node().getNodeID().charAt(k));
                    preNodeIdInHex = Integer.parseInt(preNodeIdChar, 16);
                    String sucNodeIdChar = Character.toString(routingTable[k][1].getB4node().getNodeID().charAt(k));
                    sucNodeIdInHex = Integer.parseInt(sucNodeIdChar, 16);

                    if (preNodeIdInHex <= mergerNodeIdInHex && mergerNodeIdInHex < localNodeIdInHex) {
                        if (preNodeIdInHex == mergerNodeIdInHex) {
                            for (int i = k + 1; i < rt_dimension; i++) {
                                if (mergerNodeidInCharArray[i] != preNodeIdCharArray[i]) {
                                    String nxtPreIdInChar = Character.toString(routingTable[k][0].getB4node().getNodeID().charAt(i));
                                    String nxtMergerIdChar = Character.toString(mergerNodeID.charAt(i));
                                    int nxtPreNodeIdInHex = Integer.parseInt(nxtPreIdInChar, 16);
                                    int nxtMergerNodeIdInHex = Integer.parseInt(nxtMergerIdChar, 16);
                                    if (nxtMergerNodeIdInHex > nxtPreNodeIdInHex) {
                                        routingTable[k][0] = mergerNode;
                                        differentialNodes.add(mergerNode);
                                    }
                                    break;
                                }
                            }
                        } else if (preNodeIdInHex == sucNodeIdInHex) {
                            routingTable[k][0] = mergerNode;
                            differentialNodes.add(mergerNode);
                            break;
                        } else {
                            routingTable[k][2] = routingTable[k][0];
                            routingTable[k][0] = mergerNode;
                            differentialNodes.add(mergerNode);
                            break;
                        }
                    } else if (preNodeIdInHex > localNodeIdInHex) {
                        if (preNodeIdInHex - 16 < mergerNodeIdInHex && mergerNodeIdInHex < localNodeIdInHex) {
                            if (preNodeIdInHex != sucNodeIdInHex) {
                                routingTable[k][2] = routingTable[k][0];
                            }
                            routingTable[k][0] = mergerNode;
                            differentialNodes.add(mergerNode);
                            break;

                        } else if (mergerNodeIdInHex > localNodeIdInHex && preNodeIdInHex - 16 < mergerNodeIdInHex - 16 && mergerNodeIdInHex - 16 < localNodeIdInHex) {
                            if (preNodeIdInHex != sucNodeIdInHex) {
                                routingTable[k][2] = routingTable[k][0];
                            }
                            routingTable[k][0] = mergerNode;
                            differentialNodes.add(mergerNode);
                            break;

                        } else if (preNodeIdInHex == mergerNodeIdInHex) {
                            for (int i = k + 1; i < rt_dimension; i++) {
                                if (mergerNodeidInCharArray[i] != preNodeIdCharArray[i]) {
                                    String nxtPreIdInChar = Character.toString(routingTable[k][0].getB4node().getNodeID().charAt(i));
                                    String nxtMergerIdChar = Character.toString(mergerNodeID.charAt(i));
                                    int nxtPreNodeIdInHex = Integer.parseInt(nxtPreIdInChar, 16);
                                    int nxtMergerNodeIdInHex = Integer.parseInt(nxtMergerIdChar, 16);
                                    if (nxtMergerNodeIdInHex > nxtPreNodeIdInHex) {
                                        routingTable[k][0] = mergerNode;
                                        differentialNodes.add(mergerNode);
                                    }
                                    break;
                                }
                            }
                        }
                    }

                    if (sucNodeIdInHex >= mergerNodeIdInHex && mergerNodeIdInHex > localNodeIdInHex) {
                        if (sucNodeIdInHex == mergerNodeIdInHex) {
                            for (int i = k + 1; i < rt_dimension; i++) {
                                if (mergerNodeidInCharArray[i] != sucNodeIdCharArray[i]) {
                                    String nxtSucIdInChar = Character.toString(routingTable[k][1].getB4node().getNodeID().charAt(i));
                                    String nxtMergerIdChar = Character.toString(mergerNodeID.charAt(i));
                                    int nxtSucNodeIdInHex = Integer.parseInt(nxtSucIdInChar, 16);
                                    int nxtMergerNodeIdInHex = Integer.parseInt(nxtMergerIdChar, 16);
                                    if (nxtMergerNodeIdInHex < nxtSucNodeIdInHex) {
                                        routingTable[k][1] = mergerNode;
                                        differentialNodes.add(mergerNode);
                                    }
                                    break;
                                }
                            }
                        } else if (preNodeIdInHex == sucNodeIdInHex) {
                            routingTable[k][1] = mergerNode;
                            break;
                        } else {
                            routingTable[k][2] = routingTable[k][1];
                            routingTable[k][1] = mergerNode;
                            differentialNodes.add(mergerNode);
                            break;
                        }
                    } else if (sucNodeIdInHex < localNodeIdInHex) {
                        if (sucNodeIdInHex + 16 > mergerNodeIdInHex && mergerNodeIdInHex > localNodeIdInHex) {
                            if (preNodeIdInHex != sucNodeIdInHex) {
                                routingTable[k][2] = routingTable[k][1];
                            }
                            routingTable[k][1] = mergerNode;
                            differentialNodes.add(mergerNode);
                            break;

                        } else if (mergerNodeIdInHex < localNodeIdInHex && sucNodeIdInHex + 16 > mergerNodeIdInHex + 16 && mergerNodeIdInHex + 16 > localNodeIdInHex) {
                            if (preNodeIdInHex != sucNodeIdInHex) {
                                routingTable[k][2] = routingTable[k][1];
                            }
                            routingTable[k][1] = mergerNode;
                            differentialNodes.add(mergerNode);
                            break;
                        } else if (sucNodeIdInHex == mergerNodeIdInHex) {
                            for (int i = k + 1; i < rt_dimension; i++) {
                                if (mergerNodeidInCharArray[i] != sucNodeIdCharArray[i]) {
                                    String nxtSucIdInChar = Character.toString(routingTable[k][1].getB4node().getNodeID().charAt(i));
                                    String nxtMergerIdChar = Character.toString(mergerNodeID.charAt(i));
                                    int nxtSucNodeIdInHex = Integer.parseInt(nxtSucIdInChar, 16);
                                    int nxtMergerNodeIdInHex = Integer.parseInt(nxtMergerIdChar, 16);
                                    if (nxtMergerNodeIdInHex < nxtSucNodeIdInHex) {
                                        routingTable[k][1] = mergerNode;
                                        differentialNodes.add(mergerNode);
                                    }
                                    break;
                                }
                            }
                        }
                    }

                    if (sucNodeIdInHex < mergerNodeIdInHex && mergerNodeIdInHex < preNodeIdInHex || sucNodeIdInHex < mergerNodeIdInHex && mergerNodeIdInHex < preNodeIdInHex + 16 || sucNodeIdInHex < mergerNodeIdInHex + 16 && mergerNodeIdInHex + 16 < preNodeIdInHex + 16) {
                        if (!routingTable[k][2].getB4node().getNodeID().isEmpty()) {
                            String existingMidNodeIdChar = Character.toString(routingTable[k][2].getB4node().getNodeID().charAt(k));
                            int existingMidNodeIdHex = Integer.parseInt(existingMidNodeIdChar, 16);
                            if (Math.abs(((localNodeIdInHex + 8) % 16) - mergerNodeIdInHex) < Math.abs(((localNodeIdInHex + 8) % 16) - existingMidNodeIdHex)) {
                                routingTable[k][2] = mergerNode;
                                differentialNodes.add(mergerNode);
                                break;
                            }
                        } else {
                            routingTable[k][2] = mergerNode;
                            differentialNodes.add(mergerNode);
                            break;
                        }
                    }
                }
                break;
            }
        }
    }

    /**
     * @param rtTag          XML tag
     * @param fileName       Desired name of the file
     * @param routingTable   Object of Routing table which need to be converted to XML
     * @param neighbourTable Object of Neighbour table which need to be converted to XML
     *                       <br>This function is used to convert the Routing Table in the form of an array to xml format
     *                       <br>Here XML parsing is used.
     */
    private void routingTableToXML(String rtTag, String fileName, B4_Node[][] routingTable, B4_Node[] neighbourTable) {
        String selfNodeId = localNode.getB4node().getNodeID();
        String selfNodePub = utility.pubToStr(localNode.getB4node().getPublicKey());
        String selfHashID = localNode.getB4node().getHashID();
        String selfIPAddress = localNode.getIpAddress();
        String selfPortAddress = localNode.getPortAddress();
        String selfTransport = localNode.getTransport();

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();

            //create root Element
            Element root = doc.createElement(rtTag);
            doc.appendChild(root);
            root.setAttribute("SELF_NODE_ID", selfNodeId);
            root.setAttribute("SELF_PUBLIC_KEY", selfNodePub);
            root.setAttribute("SELF_HASHID", selfHashID);
            root.setAttribute("SELF_IP_ADDRESS", selfIPAddress);
            root.setAttribute("SELF_PORT_ADDRESS", selfPortAddress);
            root.setAttribute("SELF_TRANSPORT", selfTransport);

            for (int i = 0; i < rt_dimension; i++) {
                for (int j = 0; j < 3; j++) {
                    Element row = doc.createElement("B4_Node");
                    root.appendChild(row);
                    row.setAttribute("INDEX", "[" + i + "]" + "[" + j + "]");

                    Element nodeID = doc.createElement("NODEID");
                    nodeID.appendChild(doc.createTextNode(routingTable[i][j].getB4node().getNodeID()));
                    row.appendChild(nodeID);

                    Element nodePub = doc.createElement("PUBLICKEY");
                    nodePub.appendChild(doc.createTextNode(utility.pubToStr(routingTable[i][j].getB4node().getPublicKey())));
                    row.appendChild(nodePub);

                    Element hashID = doc.createElement("HASHID");
                    hashID.appendChild(doc.createTextNode(routingTable[i][j].getB4node().getHashID()));
                    row.appendChild(hashID);

                    Element nodeIP = doc.createElement("NODEIP");
                    nodeIP.appendChild(doc.createTextNode(routingTable[i][j].getIpAddress()));
                    row.appendChild(nodeIP);

                    Element nodePort = doc.createElement("NODEPORT");
                    nodePort.appendChild(doc.createTextNode(routingTable[i][j].getPortAddress()));
                    row.appendChild(nodePort);

                    Element nodeTransport = doc.createElement("NODETRANSPORT");
                    nodeTransport.appendChild(doc.createTextNode(routingTable[i][j].getTransport()));
                    row.appendChild(nodeTransport);
                }
            }
            for (int i = 0; i < nt_dimension; i++) {
                Element row1 = doc.createElement("NEIGHBOUR");
                root.appendChild(row1);
                row1.setAttribute("INDEX", "[" + i + "]");

                Element nodeID = doc.createElement("NODEID");
                nodeID.appendChild(doc.createTextNode(neighbourTable[i].getB4node().getNodeID()));
                row1.appendChild(nodeID);

                Element nodePub = doc.createElement("PUBLICKEY");
                nodePub.appendChild(doc.createTextNode(utility.pubToStr(neighbourTable[i].getB4node().getPublicKey())));
                row1.appendChild(nodePub);

                Element hashID = doc.createElement("HASHID");
                hashID.appendChild(doc.createTextNode(neighbourTable[i].getB4node().getHashID()));
                row1.appendChild(hashID);

                Element nodeIP = doc.createElement("NODEIP");
                nodeIP.appendChild(doc.createTextNode(neighbourTable[i].getIpAddress()));
                row1.appendChild(nodeIP);

                Element nodePort = doc.createElement("NODEPORT");
                nodePort.appendChild(doc.createTextNode(neighbourTable[i].getPortAddress()));
                row1.appendChild(nodePort);

                Element nodeTransport = doc.createElement("NODETRANSPORT");
                nodeTransport.appendChild(doc.createTextNode(neighbourTable[i].getTransport()));
                row1.appendChild(nodeTransport);

                Element nodeRTT = doc.createElement("NODERTT");
                nodeRTT.appendChild(doc.createTextNode(String.valueOf(neighbourTable[i].getRtt())));
                row1.appendChild(nodeRTT);
            }
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource domSource = new DOMSource(doc);
            StreamResult streamResult = new StreamResult(new File(fileName + ".xml"));
            transformer.transform(domSource, streamResult);
            log.debug(fileName + " file updated");
        } catch (ParserConfigurationException | TransformerException e) {
            log.error("Exception Occurred", e);
        }
    }

    /**
     * This function is used to add the layer to the array list one after the other.
     */
    private void addToArrayList() {
        B4_Layer b4_layer = new B4_Layer();
        int totalLayer = b4_layer.fetchMaxLayerID();
        for (int i = 0; i <= totalLayer; i++) {
            routingTables.add(i, new B4_RoutingTable(rt_dimension, nt_dimension));
        }
    }

    /**
     * @return Total number of layer available in the system presently.
     */
    private int addNewLayerToArrayList() {
        B4_Layer b4_layer = new B4_Layer();
        int totalLayer = b4_layer.fetchMaxLayerID();
        routingTables.add(totalLayer, new B4_RoutingTable(rt_dimension, nt_dimension));
        return totalLayer;
    }

    /**
     * Initial layer is loaded using this function.
     */
    private void initialLayerLoading() {
        B4_Layer b4_layer = new B4_Layer();
        int totalLayer = b4_layer.fetchMaxLayerID();
        FileReader reader;
        String nodeID = getLocalNode().getB4node().getNodeID();
        for (int i = 0; i <= totalLayer; i++) {
            try {
                reader = new FileReader(layerFile);
                Properties properties = new Properties();
                properties.load(reader);
                String layerName = properties.getProperty("" + i + "");
                boolean access = config.isLayerAccess(layerName);
                if (access)
                    init(layerName, i + "_" + layerName + "_" + nodeID, routingTables.get(i).getRoutingTable(), routingTables.get(i).getNeighbourTable());
            } catch (IOException e) {
                log.error("Exception Occurred", e);
            }
        }
    }

    /**
     * @param rtFileName     Name of the routing table which we desired to give for later identification.
     * @param routingTable   Object of Routing Table.
     * @param neighbourTable Object of Neighbour Table.
     *                       <br>This function is used to fetch the file from XML and convert it into routing table and neighbour table object.
     */
    private void fetchFromXML(String rtFileName, B4_Node[][] routingTable, B4_Node[] neighbourTable) {
        try {
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = builderFactory.newDocumentBuilder();
            Document doc = documentBuilder.parse(new File(rtFileName + ".xml"));
            doc.getDocumentElement().normalize();
            //String rootElement = doc.getDocumentElement().getNodeName();

            NodeList nodeList = doc.getElementsByTagName("B4_Node");
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if (node.getNodeType() == node.ELEMENT_NODE) {
                    Element element = (Element) node;

                    //Get the value of ID attribute
                    String index = node.getAttributes().getNamedItem("INDEX").getNodeValue();
                    //Get value of all sub-Elements
                    String nodeID = element.getElementsByTagName("NODEID").item(0).getTextContent();
                    String nodePub = element.getElementsByTagName("PUBLICKEY").item(0).getTextContent();
                    String nodeHash = element.getElementsByTagName("HASHID").item(0).getTextContent();
                    String nodeIP = element.getElementsByTagName("NODEIP").item(0).getTextContent();
                    String nodePort = element.getElementsByTagName("NODEPORT").item(0).getTextContent();
                    String nodeTransport = element.getElementsByTagName("NODETRANSPORT").item(0).getTextContent();
                    Pattern pattern = Pattern.compile("\\[([^]]+)\\]");
                    Matcher matcher = pattern.matcher(index);
                    matcher.find();
                    int index1 = Integer.parseInt(matcher.group(1));
                    matcher.find();
                    int index2 = Integer.parseInt(matcher.group(1));
                    routingTable[index1][index2] = new B4_Node(new B4_NodeTuple(nodeID, utility.strToPub(nodePub), nodeHash), nodeIP, nodePort, nodeTransport);
                }
            }
            NodeList nodeList1 = doc.getElementsByTagName("NEIGHBOUR");
            for (int i = 0; i < nodeList1.getLength(); i++) {
                Node node = nodeList1.item(i);
                if (node.getNodeType() == node.ELEMENT_NODE) {
                    Element element = (Element) node;
                    String index = node.getAttributes().getNamedItem("INDEX").getNodeValue();
                    //Get value of all sub-Elements
                    String nodeID = element.getElementsByTagName("NODEID").item(0).getTextContent();
                    String nodePub = element.getElementsByTagName("PUBLICKEY").item(0).getTextContent();
                    String hashID = element.getElementsByTagName("HASHID").item(0).getTextContent();
                    String nodeIP = element.getElementsByTagName("NODEIP").item(0).getTextContent();
                    String nodePort = element.getElementsByTagName("NODEPORT").item(0).getTextContent();
                    String nodeTransport = element.getElementsByTagName("NODETRANSPORT").item(0).getTextContent();
                    String nodeRTT = element.getElementsByTagName("NODERTT").item(0).getTextContent();

                    Pattern pattern = Pattern.compile("\\[([^]]+)\\]");
                    Matcher matcher = pattern.matcher(index);
                    matcher.find();
                    int index1 = Integer.parseInt(matcher.group(1));
                    neighbourTable[index1] = new B4_Node(new B4_NodeTuple(nodeID, utility.strToPub(nodePub), hashID), nodeIP, nodePort, nodeTransport, Float.parseFloat(nodeRTT));
                }
            }
        } catch (ParserConfigurationException | IOException | SAXException | NullPointerException e) {
            log.error("Exception Occurred", e);
        }
    }

    /**
     * @param mergerTableDataFile The routingTable file for which the rtt value of the neighbour table need to be calculated. <br>
     * @param layerID             layer Id on which the operation needs to be performed.
     */
    private void generateRTTMergerTable(File mergerTableDataFile, int layerID) {
        B4_Node selfNodeOfMergerTable = getSelfNodeOfMergerTable(mergerTableDataFile.getName());
        B4_Node[] mergerNeighbourTable = getMergerNeighbourTable(mergerTableDataFile.getName());
        String selfNodeIdMerger = selfNodeOfMergerTable.getB4node().getNodeID();
        String selfNodePubMerger = utility.pubToStr(selfNodeOfMergerTable.getB4node().getPublicKey());
        String selfHashIDMerger = selfNodeOfMergerTable.getB4node().getHashID();
        String selfIPAddressMerger = selfNodeOfMergerTable.getIpAddress();
        String selfPortAddressMerger = selfNodeOfMergerTable.getPortAddress();
        String selfTransportMerger = selfNodeOfMergerTable.getTransport();
        String selfRTTMerger = "-1";
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();
            //create root Element
            Element root = doc.createElement("Merger_Neighbour_table");
            doc.appendChild(root);
            root.setAttribute("SELF_NODE_ID", selfNodeIdMerger);
            root.setAttribute("SELF_PUBLIC_KEY", selfNodePubMerger);
            root.setAttribute("SELF_HASHID", selfHashIDMerger);
            root.setAttribute("SELF_IP_ADDRESS", selfIPAddressMerger);
            root.setAttribute("SELF_PORT_ADDRESS", selfPortAddressMerger);
            root.setAttribute("SELF_TRANSPORT", selfTransportMerger);
            root.setAttribute("SELF_RTT", selfRTTMerger);

            for (int i = 0; i < nt_dimension; i++) {
                Element row1 = doc.createElement("NEIGHBOUR");
                root.appendChild(row1);
                row1.setAttribute("INDEX", "[" + i + "]");

                Element nodeID = doc.createElement("NODEID");
                nodeID.appendChild(doc.createTextNode(mergerNeighbourTable[i].getB4node().getNodeID()));
                row1.appendChild(nodeID);

                Element nodePub = doc.createElement("PUBLICKEY");
                nodePub.appendChild(doc.createTextNode(utility.pubToStr(mergerNeighbourTable[i].getB4node().getPublicKey())));
                row1.appendChild(nodePub);

                Element hashID = doc.createElement("HASHID");
                hashID.appendChild(doc.createTextNode(mergerNeighbourTable[i].getB4node().getHashID()));
                row1.appendChild(hashID);

                Element nodeIP = doc.createElement("NODEIP");
                nodeIP.appendChild(doc.createTextNode(mergerNeighbourTable[i].getIpAddress()));
                row1.appendChild(nodeIP);

                Element nodePort = doc.createElement("NODEPORT");
                nodePort.appendChild(doc.createTextNode(mergerNeighbourTable[i].getPortAddress()));
                row1.appendChild(nodePort);

                Element nodeTransport = doc.createElement("NODETRANSPORT");
                nodeTransport.appendChild(doc.createTextNode(mergerNeighbourTable[i].getTransport()));
                row1.appendChild(nodeTransport);

                Element nodeRTT = doc.createElement("NODERTT");
                nodeRTT.appendChild(doc.createTextNode("-1"));
                row1.appendChild(nodeRTT);
            }
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource domSource = new DOMSource(doc);

            StreamResult streamResult = new StreamResult(new File("GetRTT_" + layerID + "_" + selfNodeIdMerger + ".xml"));
            transformer.transform(domSource, streamResult);
            log.debug("getRTT File is created");
        } catch (ParserConfigurationException | TransformerException e) {
            log.error("Exception Occurred", e);
        }
        File file1 = new File("GetRTT_" + layerID + "_" + selfNodeIdMerger + ".xml");
        addFileToOutputBuffer(file1);
    }

    private File routingTable1ToXML(String rtTag, String fileName, B4_Node[][] routingTable) {
        File file = null;
        String selfNodeId = localNode.getB4node().getNodeID();
        String selfNodePub = utility.pubToStr(localNode.getB4node().getPublicKey());
        String selfHashID = localNode.getB4node().getHashID();
        String selfIPAddress = localNode.getIpAddress();
        String selfPortAddress = localNode.getPortAddress();
        String selfTransport = localNode.getTransport();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();

            //create root Element
            Element root = doc.createElement(rtTag);
            doc.appendChild(root);
            root.setAttribute("SELF_NODE_ID", selfNodeId);
            root.setAttribute("SELF_PUBLIC_KEY", selfNodePub);
            root.setAttribute("SELF_HASHID", selfHashID);
            root.setAttribute("SELF_IP_ADDRESS", selfIPAddress);
            root.setAttribute("SELF_PORT_ADDRESS", selfPortAddress);
            root.setAttribute("SELF_TRANSPORT", selfTransport);

            for (int i = 0; i < rt_dimension; i++) {
                for (int j = 0; j < 3; j++) {
                    Element row = doc.createElement("B4_Node");
                    root.appendChild(row);
                    row.setAttribute("INDEX", "[" + i + "]" + "[" + j + "]");

                    Element nodeID = doc.createElement("NODEID");
                    nodeID.appendChild(doc.createTextNode(routingTable[i][j].getB4node().getNodeID()));
                    row.appendChild(nodeID);

                    Element nodePub = doc.createElement("PUBLICKEY");
                    nodePub.appendChild(doc.createTextNode(utility.pubToStr(routingTable[i][j].getB4node().getPublicKey())));
                    row.appendChild(nodePub);

                    Element hashID = doc.createElement("HASHID");
                    hashID.appendChild(doc.createTextNode(routingTable[i][j].getB4node().getHashID()));
                    row.appendChild(hashID);

                    Element nodeIP = doc.createElement("NODEIP");
                    nodeIP.appendChild(doc.createTextNode(routingTable[i][j].getIpAddress()));
                    row.appendChild(nodeIP);

                    Element nodePort = doc.createElement("NODEPORT");
                    nodePort.appendChild(doc.createTextNode(routingTable[i][j].getPortAddress()));
                    row.appendChild(nodePort);

                    Element nodeTransport = doc.createElement("NODETRANSPORT");
                    nodeTransport.appendChild(doc.createTextNode(routingTable[i][j].getTransport()));
                    row.appendChild(nodeTransport);
                }
            }
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource domSource = new DOMSource(doc);
            file = new File(fileName + ".xml");
            StreamResult streamResult = new StreamResult(file);
            transformer.transform(domSource, streamResult);
            log.debug(fileName + " file updated");
        } catch (ParserConfigurationException | TransformerException e) {
            log.error("Exception Occurred", e);
        }
        return file;
    }

    private File neighbourTableToXML(String rtTag, String fileName, B4_Node[] neighbourTable) {
        File file = null;
        String selfNodeId = localNode.getB4node().getNodeID();
        String selfNodePub = utility.pubToStr(localNode.getB4node().getPublicKey());
        String selfHashID = localNode.getB4node().getHashID();
        String selfIPAddress = localNode.getIpAddress();
        String selfPortAddress = localNode.getPortAddress();
        String selfTransport = localNode.getTransport();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();

            //create root Element
            Element root = doc.createElement(rtTag);
            doc.appendChild(root);
            root.setAttribute("SELF_NODE_ID", selfNodeId);
            root.setAttribute("SELF_PUBLIC_KEY", selfNodePub);
            root.setAttribute("SELF_HASHID", selfHashID);
            root.setAttribute("SELF_IP_ADDRESS", selfIPAddress);
            root.setAttribute("SELF_PORT_ADDRESS", selfPortAddress);
            root.setAttribute("SELF_TRANSPORT", selfTransport);
            for (int i = 0; i < nt_dimension; i++) {
                Element row1 = doc.createElement("NEIGHBOUR");
                root.appendChild(row1);
                row1.setAttribute("INDEX", "[" + i + "]");

                Element nodeID = doc.createElement("NODEID");
                nodeID.appendChild(doc.createTextNode(neighbourTable[i].getB4node().getNodeID()));
                row1.appendChild(nodeID);

                Element nodePub = doc.createElement("PUBLICKEY");
                nodePub.appendChild(doc.createTextNode(utility.pubToStr(neighbourTable[i].getB4node().getPublicKey())));
                row1.appendChild(nodePub);

                Element hashID = doc.createElement("HASHID");
                hashID.appendChild(doc.createTextNode(neighbourTable[i].getB4node().getHashID()));
                row1.appendChild(hashID);

                Element nodeIP = doc.createElement("NODEIP");
                nodeIP.appendChild(doc.createTextNode(neighbourTable[i].getIpAddress()));
                row1.appendChild(nodeIP);

                Element nodePort = doc.createElement("NODEPORT");
                nodePort.appendChild(doc.createTextNode(neighbourTable[i].getPortAddress()));
                row1.appendChild(nodePort);

                Element nodeTransport = doc.createElement("NODETRANSPORT");
                nodeTransport.appendChild(doc.createTextNode(neighbourTable[i].getTransport()));
                row1.appendChild(nodeTransport);

                Element nodeRTT = doc.createElement("NODERTT");
                nodeRTT.appendChild(doc.createTextNode(String.valueOf(neighbourTable[i].getRtt())));
                row1.appendChild(nodeRTT);
            }
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource domSource = new DOMSource(doc);
            file = new File(fileName + ".xml");
            StreamResult streamResult = new StreamResult(file);
            transformer.transform(domSource, streamResult);
            log.debug(fileName + " file updated");
        } catch (ParserConfigurationException | TransformerException e) {
            log.error("Exception Occurred", e);
        }
        return file;
    }

    private File createDifferentialTable(String rtTag,String fileName){
        File file = null;
        String selfNodeId = localNode.getB4node().getNodeID();
        String selfNodePub = utility.pubToStr(localNode.getB4node().getPublicKey());
        String selfHashID = localNode.getB4node().getHashID();
        String selfIPAddress = localNode.getIpAddress();
        String selfPortAddress = localNode.getPortAddress();
        String selfTransport = localNode.getTransport();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();

            //create root Element
            Element root = doc.createElement(rtTag);
            doc.appendChild(root);
            root.setAttribute("SELF_NODE_ID", selfNodeId);
            root.setAttribute("SELF_PUBLIC_KEY", selfNodePub);
            root.setAttribute("SELF_HASHID", selfHashID);
            root.setAttribute("SELF_IP_ADDRESS", selfIPAddress);
            root.setAttribute("SELF_PORT_ADDRESS", selfPortAddress);
            root.setAttribute("SELF_TRANSPORT", selfTransport);

            for (int i = 0; i < differentialNodes.size(); i++) {
                Element row1 = doc.createElement("DIFFNODES");
                root.appendChild(row1);
                row1.setAttribute("INDEX", "[" + i + "]");

                Element nodeID = doc.createElement("NODEID");
                nodeID.appendChild(doc.createTextNode(differentialNodes.get(i).getB4node().getNodeID()));
                row1.appendChild(nodeID);

                Element nodePub = doc.createElement("PUBLICKEY");
                nodePub.appendChild(doc.createTextNode(utility.pubToStr(differentialNodes.get(i).getB4node().getPublicKey())));
                row1.appendChild(nodePub);

                Element hashID = doc.createElement("HASHID");
                hashID.appendChild(doc.createTextNode(differentialNodes.get(i).getB4node().getHashID()));
                row1.appendChild(hashID);

                Element nodeIP = doc.createElement("NODEIP");
                nodeIP.appendChild(doc.createTextNode(differentialNodes.get(i).getIpAddress()));
                row1.appendChild(nodeIP);

                Element nodePort = doc.createElement("NODEPORT");
                nodePort.appendChild(doc.createTextNode(differentialNodes.get(i).getPortAddress()));
                row1.appendChild(nodePort);

                Element nodeTransport = doc.createElement("NODETRANSPORT");
                nodeTransport.appendChild(doc.createTextNode(differentialNodes.get(i).getTransport()));
                row1.appendChild(nodeTransport);

                Element nodeRTT = doc.createElement("NODERTT");
                nodeRTT.appendChild(doc.createTextNode(String.valueOf(differentialNodes.get(i).getRtt())));
                row1.appendChild(nodeRTT);
            }
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource domSource = new DOMSource(doc);
            file = new File(fileName + ".xml");
            StreamResult streamResult = new StreamResult(file);
            transformer.transform(domSource, streamResult);
            log.debug(fileName + " file updated");
        } catch (ParserConfigurationException | TransformerException e) {
            log.error("Exception Occurred", e);
        }
        return file;

    }
}
