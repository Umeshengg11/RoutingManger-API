import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Created by S/L Umesh U Nair
 * <br>Aim is to create an Routing Manager API for Brehaspati-4
 */
public class RoutingManager {
    private static RoutingManager routingManager;
    private final B4_Node[][] localBaseRoutingTable;
    private final B4_Node[] localBaseNeighbourTable;
    private final B4_Node[][] storageRoutingTable;
    private final B4_Node[] storageNeighbourTable;
    private B4_Node[] mergerNeighbourTable;
    private B4_Node[][] mergerRoutingTable;
    private B4_Node localNode;
    private final int rt_dimension;
    private final int nt_dimension;
    private long incrementTime;
    private long sleepTime;

    /**
     * Constructor
     * <br>Main job of constructor are as follows:-
     * <br>Check routing table and neighbour table exist from the previous login(ie to check RoutingTable.xml is available in the path).
     * <br>If RT exists then data is taken from the xml file and added to the localBaseRoutingTable(which is the routingTable for current node)
     * and to the localBaseNeighbourTable(which is the neighbourTable for current node).
     * <br>If not available then create a routing table(localBaseRoutingTable) and neighbour table (localBaseNeighbourTable).
     * <br>Initial entries of localBaseRoutingTable and localBaseNeighbourTable should be object of B4_Node with only bootstrap node entry.
     */
    private RoutingManager() {
        rt_dimension = getRT_length();
        nt_dimension = getNT_length();
        incrementTime = getIncrementTime();
        sleepTime = getSleepTime();
        setLocalNode();
        localBaseRoutingTable = new B4_Node[rt_dimension][3];
        localBaseNeighbourTable = new B4_Node[nt_dimension];
        storageRoutingTable = new B4_Node[rt_dimension][3];
        storageNeighbourTable = new B4_Node[nt_dimension];
        init("BaseRoutingTable", localBaseRoutingTable, localBaseNeighbourTable);
        boolean access = serviceAccess("StorageAccess");
        if (access) init("StorageRoutingTable", storageRoutingTable, storageNeighbourTable);
    }

    /**
     * @param rtFileName <br>@param routingTable
     *                   <br>@param neighbourTable
     *                   <br>All the initialisation w.r.t routing Manager will be performed here.
     *                   <br>This function is called by the constuctor for initialisation of routing manager.
     *                   <br>Initialisation includes creating routingTable and neighbour table,creating a routing table file for future references etc.
     */
    private void init(String rtFileName, B4_Node[][] routingTable, B4_Node[] neighbourTable) {
        boolean rtExists;
        File rtFile = new File(rtFileName + ".xml");
        rtExists = rtFile.exists();

        if (!rtExists) {

            /* Routing Table */
            for (int i = 0; i < rt_dimension; i++) {
                for (int j = 0; j < 3; j++) {
                    routingTable[i][j] = new B4_Node("", "", "", "");
                }
            }
            B4_Node bootStrapNode = getBootStrapNodeID();
            mergerRT(bootStrapNode, routingTable);

            /* Neighbour Table */
            for (int i = 0; i < nt_dimension; i++) {
                neighbourTable[i] = new B4_Node("", "", "", "", -1);
            }
            localBaseTablesToXML(rtFileName, routingTable, neighbourTable);

        } else {
            try {
                //Get Document builder
                DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder documentBuilder = builderFactory.newDocumentBuilder();

                //Load the input XML document,parse it and return an instance of Document class
                Document doc = documentBuilder.parse(new File(rtFileName + ".xml"));
                doc.getDocumentElement().normalize();
                //String rootElement = doc.getDocumentElement().getNodeName();
                //System.out.println(rootElement);

                NodeList nodeList = doc.getElementsByTagName("B4_Node");
                for (int i = 0; i < nodeList.getLength(); i++) {
                    Node node = nodeList.item(i);

                    if (node.getNodeType() == node.ELEMENT_NODE) {
                        Element element = (Element) node;

                        //Get the value of ID attribute
                        String index = node.getAttributes().getNamedItem("INDEX").getNodeValue();

                        //Get value of all sub-Elements
                        String nodeID = element.getElementsByTagName("NODEID").item(0).getTextContent();
                        String nodeIP = element.getElementsByTagName("NODEIP").item(0).getTextContent();
                        String nodePort = element.getElementsByTagName("NODEPORT").item(0).getTextContent();
                        String nodeTransport = element.getElementsByTagName("NODETRANSPORT").item(0).getTextContent();

                        Pattern pattern = Pattern.compile("\\[([^]]+)\\]");
                        Matcher matcher = pattern.matcher(index);
                        matcher.find();
                        int index1 = Integer.parseInt(matcher.group(1));
                        matcher.find();
                        int index2 = Integer.parseInt(matcher.group(1));
                        routingTable[index1][index2] = new B4_Node(nodeID, nodeIP, nodePort, nodeTransport);
                    }
                }
                NodeList nodeList1 = doc.getElementsByTagName("NEIGHBOUR");
                for (int i = 0; i < nodeList1.getLength(); i++) {
                    Node node = nodeList1.item(i);

                    if (node.getNodeType() == node.ELEMENT_NODE) {
                        Element element = (Element) node;

                        //Get the value of ID attribute
                        String index = node.getAttributes().getNamedItem("INDEX").getNodeValue();

                        //Get value of all sub-Elements
                        String nodeID = element.getElementsByTagName("NODEID").item(0).getTextContent();
                        String nodeIP = element.getElementsByTagName("NODEIP").item(0).getTextContent();
                        String nodePort = element.getElementsByTagName("NODEPORT").item(0).getTextContent();
                        String nodeTransport = element.getElementsByTagName("NODETRANSPORT").item(0).getTextContent();
                        String nodeRTT = element.getElementsByTagName("NODERTT").item(0).getTextContent();

                        Pattern pattern = Pattern.compile("\\[([^]]+)\\]");
                        Matcher matcher = pattern.matcher(index);
                        matcher.find();
                        int index1 = Integer.parseInt(matcher.group(1));
                        neighbourTable[index1] = new B4_Node(nodeID, nodeIP, nodePort, nodeTransport, Float.parseFloat(nodeRTT));
                    }
                }
            } catch (ParserConfigurationException | IOException | SAXException | NullPointerException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * @return RoutingManger Object
     * <br>This method is required to create an instance of RoutingManager.
     * <br>Instance of RoutingManager will be obtained by calling this function.
     */
    public static synchronized RoutingManager getInstance() {
        if (routingManager == null) {
            routingManager = new RoutingManager();
        }
        return routingManager;
    }

    /**
     * This method is used for setting Local Node information.
     * <br>Presently it is hardcoded (will be ammended later).
     */
    private void setLocalNode() {
        localNode = new B4_Node("4589ABAA1234ABC1234591111ABCDFE123456789", "192.168.0.105", "6666", "TCP");
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
     * @param mergerTableDataFile
     * @param layerID             <br>This method is used for merging routing table obtained from other B4_Node in to localBaseRoutingTable.
     *                            <br>Merging is performed by one by one comparing of nodeID obtained from the received node with existing nodeID in the localBaseRoutingTable.
     *                            <br>Initial merging of localBaseRoutingTable happens with the routing Table obtained from the Bootstrap Node.
     *                            <br>Nibble wise comparison is done(b/w mergerTableNodeId and localNodeID) to obtain the column in localBaseRoutingTable
     *                            Array at which the data is to be updated.
     *                            <br>Based on the algorithm the B4_Node will be place in the predecessor ,successor or middle row of the obtained column.
     */
    public void mergeRoutingTable(String mergerTableDataFile, int layerID) {

        B4_Node[][] routingTableLayer = null;
        if (layerID == 0) {
            routingTableLayer = localBaseRoutingTable;
        } else if (layerID == 1) {
            routingTableLayer = storageRoutingTable;
        }
        B4_Node selfNodeOfMergerTable = getSelfNodeOfMergerTable(mergerTableDataFile);
        mergerRoutingTable = getMergerRoutingTable(mergerTableDataFile);

        mergerRT(selfNodeOfMergerTable, routingTableLayer);
        for (int i = 0; i < rt_dimension; i++) {
            for (int j = 0; j < 3; j++) {
                mergerRT(mergerRoutingTable[i][j], routingTableLayer);
            }
        }
        if (routingTableLayer == localBaseRoutingTable) {
            localBaseTablesToXML("BaseRoutingTable", localBaseRoutingTable, localBaseNeighbourTable);
            System.out.println("Base Routing Table Merged successfully");
        }
        if (routingTableLayer == storageRoutingTable) {
            localBaseTablesToXML("StorageRoutingTable", storageRoutingTable, storageNeighbourTable);
            System.out.println("Storage Routing Table Merged successfully");
        }
    }

    /**
     * @param mergerTableDataFile
     * @param layerID
     */
    public void mergeNeighbourTable(String mergerTableDataFile, int layerID) {
        B4_Node[] neighbourTable = null;
        if (layerID == 0) {
            neighbourTable = localBaseNeighbourTable;
        } else if (layerID == 1) {
            neighbourTable = storageNeighbourTable;
        }

        boolean rttFileExists;
        mergerNeighbourTable = new B4_Node[nt_dimension];
        B4_Node selfMergerNode = null;
        B4_Node selfNodeOfMergerTable = getSelfNodeOfMergerTable(mergerTableDataFile);
        String mergerNodeID = selfNodeOfMergerTable.getNodeID();
        String fileName = "RcvRTT_" + mergerNodeID;
        File rttFile = new File(fileName + ".xml");
        rttFileExists = rttFile.exists();
        if (!rttFileExists) {
            System.out.println("RTT updated file does not exist");
        } else {
            //Get Document builder
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder;
            try {
                documentBuilder = builderFactory.newDocumentBuilder();
                //Load the input XML document,parse it and return an instance of Document class
                Document doc = documentBuilder.parse(new File(fileName + ".xml"));
                doc.getDocumentElement().normalize();
                //String rootElement = doc.getDocumentElement().getNodeName();
                //System.out.println(rootElement);
                String selfNodeID = doc.getDocumentElement().getAttribute("SELF_NODE_ID");
                String selfIPAddress = doc.getDocumentElement().getAttribute("SELF_IP_ADDRESS");
                String selfPortAddress = doc.getDocumentElement().getAttribute("SELF_PORT_ADDRESS");
                String selfTransport = doc.getDocumentElement().getAttribute("SELF_TRANSPORT");
                String selfRTT = doc.getDocumentElement().getAttribute("SELF_RTT");
                selfMergerNode = new B4_Node(selfNodeID, selfIPAddress, selfPortAddress, selfTransport, Float.parseFloat(selfRTT));

                NodeList nodeList = doc.getElementsByTagName("NEIGHBOUR");
                for (int i = 0; i < nodeList.getLength(); i++) {
                    Node node = nodeList.item(i);

                    if (node.getNodeType() == node.ELEMENT_NODE) {
                        Element element = (Element) node;

                        //Get the value of ID attribute
                        String index = node.getAttributes().getNamedItem("INDEX").getNodeValue();

                        //Get value of all sub-Elements
                        String nodeID = element.getElementsByTagName("NODEID").item(0).getTextContent();
                        String nodeIP = element.getElementsByTagName("NODEIP").item(0).getTextContent();
                        String nodePort = element.getElementsByTagName("NODEPORT").item(0).getTextContent();
                        String nodeTransport = element.getElementsByTagName("NODETRANSPORT").item(0).getTextContent();
                        String nodeRTT = element.getElementsByTagName("NODERTT").item(0).getTextContent();
                        Pattern pattern = Pattern.compile("\\[([^]]+)\\]");
                        Matcher matcher = pattern.matcher(index);
                        matcher.find();
                        int index1 = Integer.parseInt(matcher.group(1));
                        mergerNeighbourTable[index1] = new B4_Node(nodeID, nodeIP, nodePort, nodeTransport, Float.parseFloat(nodeRTT));
                    }
                }
            } catch (ParserConfigurationException | SAXException | IOException e) {
                e.printStackTrace();
            }
            for (int i = 0; i < nt_dimension; i++) {
                assert selfMergerNode != null;
                if (selfMergerNode.getRtt() == -1) break;
                assert neighbourTable != null;
                if (neighbourTable[i].getRtt() == -1) {
                    neighbourTable[i] = selfMergerNode;
                    break;
                } else if (neighbourTable[i].getRtt() <= selfMergerNode.getRtt()) {
                    continue;
                } else {
                    for (int j = nt_dimension-1; j >= i + 1; j--) {
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
                        continue;
                    } else {
                        for (int k = nt_dimension-1; k >= j + 1; k--) {
                            neighbourTable[k] = neighbourTable[k - 1];
                        }
                        neighbourTable[j] = mergerNeighbourTable[i];
                        break;
                    }
                }
            }
            for (int i = 0; i < nt_dimension; i++) {
                assert neighbourTable != null;
                System.out.println(neighbourTable[i].getRtt());
            }
            if (neighbourTable == localBaseNeighbourTable) {
                localBaseTablesToXML("BaseRoutingTable", localBaseRoutingTable, localBaseNeighbourTable);
                System.out.println("Base NeighbourTable Merged successfully");
            }

            if (neighbourTable == storageNeighbourTable) {
                localBaseTablesToXML("StorageRoutingTable", storageRoutingTable, storageNeighbourTable);
                System.out.println("Storage NeighbourTable Merged successfully");
            }
        }
    }

    /**
     * @param mergerTableDataFile
     */
    public void getRTTMergerTable(String mergerTableDataFile) {
        B4_Node selfNodeOfMergerTable = getSelfNodeOfMergerTable(mergerTableDataFile);
        B4_Node[] mergerNeighbourTable = getMergerNeighbourTable(mergerTableDataFile);

        String selfNodeIdMerger = selfNodeOfMergerTable.getNodeID();
        String selfIPAddressMerger = selfNodeOfMergerTable.getIpAddress();
        String selfPortAddressMerger = selfNodeOfMergerTable.getPortAddress();
        String selfTransportMerger = selfNodeOfMergerTable.getTransport();
        String selfRTTMerger = "";

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();

            //create root Element
            Element root = doc.createElement("Merger_Neighbour_table");
            doc.appendChild(root);
            root.setAttribute("SELF_NODE_ID", selfNodeIdMerger);
            root.setAttribute("SELF_IP_ADDRESS", selfIPAddressMerger);
            root.setAttribute("SELF_PORT_ADDRESS", selfPortAddressMerger);
            root.setAttribute("SELF_TRANSPORT", selfTransportMerger);
            root.setAttribute("SELF_RTT", selfRTTMerger);


            for (int i = 0; i < nt_dimension; i++) {
                Element row1 = doc.createElement("NEIGHBOUR");
                root.appendChild(row1);
                row1.setAttribute("INDEX", "[" + i + "]");

                Element nodeID = doc.createElement("NODEID");
                nodeID.appendChild(doc.createTextNode(mergerNeighbourTable[i].getNodeID()));
                row1.appendChild(nodeID);

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
            /**
             * For debugging//q
             * StreamResult streamResult = new StreamResult(System.out);
             **/

            StreamResult streamResult = new StreamResult(new File("GetRTT_" + selfNodeIdMerger + ".xml"));
            transformer.transform(domSource, streamResult);
            System.out.println("getRTT File is created");

        } catch (ParserConfigurationException | TransformerConfigurationException e) {
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();
        }

    }

    /**
     * @param hashID
     * @param layerID
     * @return null if next hop is selfNode else return B4_Node object
     * 1. This method is used to find the nextHop for a hashID/NodeID which is received as a query.
     * 2. Initially check whether the hashId/nodeId is equal to localNodeID.
     * 3. Thereafter check whether the localNode is the root node for the given hashId/NodeId.
     */
    public B4_Node findNextHop(String hashID, int layerID) {
        B4_Node[][] routingTable = null;
        if (layerID == 0) {
            routingTable = localBaseRoutingTable;
        } else if (layerID == 1) {
            routingTable = storageRoutingTable;
        }
        String localNodeID = localNode.getNodeID();
        //System.out.println("HashID  : " + hashID + " " + "  LocalID  : " + localNodeID);
        char[] hashIdC = hashID.toCharArray();
        char[] localNodeIdC = localNodeID.toCharArray();
        if (hashID.equals(localNodeID)) {
            System.out.println("Current Node is the Root Node");

            /**
             * Nibble wise comparison is made and the first nibble mismatch between hashId and LocalNodeId is identified.
             * <br>This will give value of k (i.e column at which we start looking for next hop).
             * <br>IF this column is not empty check predecessor successor and middle row one by one based on the logic defined to get the next hop.
             */
        } else {
            for (int k = 0; k < rt_dimension; k++) {
                if (hashIdC[k] != localNodeIdC[k]) {
                    //System.out.println("k :" + k);
                    String hashIdChar = Character.toString(hashID.charAt(k));
                    String localNodeIdChar = Character.toString(localNodeID.charAt(k));
                    int hashIdHex = Integer.parseInt(hashIdChar, 16);
                    int localNodeIdInHex = Integer.parseInt(localNodeIdChar, 16);
                    //System.out.println("hashIdHex " + hashIdHex + " localNodeIdInHex " + localNodeIdInHex);
                    if (!localBaseRoutingTable[k][0].getNodeID().isEmpty()) {
                        assert routingTable != null;
                        String preNodeIdChar = Character.toString(routingTable[k][0].getNodeID().charAt(k));
                        String sucNodeIdChar = Character.toString(routingTable[k][1].getNodeID().charAt(k));
                        int preNodeIdHex = Integer.parseInt(preNodeIdChar, 16);
                        int sucNodeIdHex = Integer.parseInt(sucNodeIdChar, 16);

                        /**
                         * Check whether hashId/NodeId lies between predecessor and localNodeId.
                         * Since nibbles are arranged in the form of a ring ranging from 0-15, all possible conditions needs to be checked.
                         * Like predecessor < localNodeId or predecessor > localNodeId and hashId< localNodeID or hashId > localNodeId in a circle
                         * IF true move to next column and check next nibble lies between next nibble of predecessor and localNodeId.
                         * This is Iterated till last column and still hashId nibble lies between predecessor and localNodeId then localNodeId is the root Node.
                         * Hence return Null.
                         */
                        if (preNodeIdHex <= hashIdHex && hashIdHex < localNodeIdInHex || preNodeIdHex > localNodeIdInHex && preNodeIdHex - 16 <= hashIdHex && hashIdHex < localNodeIdInHex || preNodeIdHex > localNodeIdInHex && preNodeIdHex - 16 <= hashIdHex - 16 && hashIdHex - 16 < localNodeIdInHex - 16) {
                            if (k != rt_dimension - 1) {
                                for (int i = k + 1; i < rt_dimension; i++) {
                                    if (!routingTable[i][0].getNodeID().isEmpty()) {
                                        String nxtPreNodeIdChar = Character.toString(routingTable[i][0].getNodeID().charAt(i));
                                        String nxtHashIdChar = Character.toString(hashID.charAt(i));
                                        String nxtLocalNodeIdChar = Character.toString(localNodeID.charAt(i));
                                        String nxtSucNodeIdChar = Character.toString(routingTable[i][1].getNodeID().charAt(i));

                                        int nxtPreNodeIdHex = Integer.parseInt(nxtPreNodeIdChar, 16);
                                        int nxtHashIdHex = Integer.parseInt(nxtHashIdChar, 16);
                                        int nxtLocalNodeIdInHex = Integer.parseInt(nxtLocalNodeIdChar, 16);
                                        int nxtSucNodeIdHex = Integer.parseInt(nxtSucNodeIdChar, 16);

                                        if (nxtPreNodeIdHex <= nxtHashIdHex && nxtHashIdHex < nxtLocalNodeIdInHex || nxtPreNodeIdHex > nxtLocalNodeIdInHex && nxtPreNodeIdHex - 16 <= nxtHashIdHex && nxtHashIdHex < nxtLocalNodeIdInHex || nxtPreNodeIdHex > nxtLocalNodeIdInHex && nxtPreNodeIdHex - 16 <= nxtHashIdHex - 16 && nxtHashIdHex - 16 < nxtLocalNodeIdInHex - 16) {
                                            if (i != rt_dimension - 1) continue;
                                            else return null;
                                        } else if (nxtSucNodeIdHex >= nxtHashIdHex && nxtHashIdHex > nxtLocalNodeIdInHex || nxtSucNodeIdHex < nxtLocalNodeIdInHex && nxtSucNodeIdHex + 16 >= nxtHashIdHex && nxtHashIdHex > nxtLocalNodeIdInHex || nxtSucNodeIdHex < nxtLocalNodeIdInHex && nxtSucNodeIdHex + 16 >= nxtHashIdHex + 16 && nxtHashIdHex + 16 > nxtLocalNodeIdInHex) {
                                            return routingTable[i][1];
                                        } else if (!routingTable[i][2].getNodeID().isEmpty()) {
                                            String nxtMidNodeIdChar = Character.toString(routingTable[i][2].getNodeID().charAt(i));
                                            int nxtMidNodeIdHex = Integer.parseInt(nxtMidNodeIdChar, 16);
                                            if (nxtSucNodeIdHex < nxtHashIdHex && nxtHashIdHex < nxtMidNodeIdHex || nxtSucNodeIdHex > nxtMidNodeIdHex && nxtSucNodeIdHex - 16 < nxtHashIdHex && nxtHashIdHex < nxtMidNodeIdHex || nxtSucNodeIdHex > nxtMidNodeIdHex && nxtSucNodeIdHex - 16 < nxtHashIdHex - 16 && nxtHashIdHex - 16 < nxtMidNodeIdHex) {
                                                return routingTable[i][1];
                                            } else if (nxtPreNodeIdHex > nxtHashIdHex && nxtHashIdHex > nxtMidNodeIdHex || nxtPreNodeIdHex < nxtMidNodeIdHex && nxtPreNodeIdHex + 16 > nxtHashIdHex && nxtHashIdHex > nxtMidNodeIdHex || nxtPreNodeIdHex < nxtMidNodeIdHex && nxtPreNodeIdHex + 16 > nxtHashIdHex + 16 && nxtHashIdHex + 16 > nxtMidNodeIdHex) {
                                                return routingTable[i][2];
                                            }
                                        } else if (routingTable[i][2].getNodeID().isEmpty()) {
                                            return routingTable[i][1];
                                        }
                                        if (i == rt_dimension - 1) return null;
                                    }
                                }
                            } else return null;

                            /**
                             * Else Check whether hashId/NodeId lies between localNodeId and successor.
                             * Since nibbles are arranged in the form of a ring ranging from 0-15, all possible conditions needs to be checked.
                             * IF true return successorNodeId.
                             */
                        } else if (sucNodeIdHex >= hashIdHex && hashIdHex > localNodeIdInHex || sucNodeIdHex < localNodeIdInHex && sucNodeIdHex + 16 >= hashIdHex && hashIdHex > localNodeIdInHex || sucNodeIdHex < localNodeIdInHex && sucNodeIdHex + 16 >= hashIdHex + 16 && hashIdHex + 16 > localNodeIdInHex) {
                            return routingTable[k][1];

                        } else if (!routingTable[k][2].getNodeID().isEmpty()) {
                            String midNodeIdChar = Character.toString(routingTable[k][2].getNodeID().charAt(k));
                            int midNodeIdHex = Integer.parseInt(midNodeIdChar, 16);

                            /**
                             * Else Check whether hashId/NodeId lies between successor and middle.
                             * Since nibbles are arranged in the form of a ring ranging from 0-15, all possible conditions needs to be checked.
                             * IF true return successorNodeId.
                             */
                            if (sucNodeIdHex < hashIdHex && hashIdHex < midNodeIdHex || sucNodeIdHex > midNodeIdHex && sucNodeIdHex - 16 < hashIdHex && hashIdHex < midNodeIdHex || sucNodeIdHex > midNodeIdHex && sucNodeIdHex - 16 < hashIdHex - 16 && hashIdHex - 16 < midNodeIdHex) {
                                return routingTable[k][1];

                                /**
                                 * Else Check whether hashId/NodeId lies between middle and predecessor.
                                 * Since nibbles are arranged in the form of a ring ranging from 0-15, all possible conditions needs to be checked.
                                 * IF true return middleNodeId.
                                 */
                            } else if (preNodeIdHex > hashIdHex && hashIdHex > midNodeIdHex || preNodeIdHex < midNodeIdHex && preNodeIdHex + 16 > hashIdHex && hashIdHex > midNodeIdHex || preNodeIdHex < midNodeIdHex && preNodeIdHex + 16 > hashIdHex + 16 && hashIdHex + 16 > midNodeIdHex) {
                                return routingTable[k][2];
                            }
                        } else if (routingTable[k][2].getNodeID().isEmpty()) {
                            return routingTable[k][1];
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * @param mergerFile
     * @return B4_RoutingTable Object
     */
    private B4_Node[][] getMergerRoutingTable(String mergerFile) {
        B4_RoutingTables mergerTable = new B4_RoutingTables(mergerFile);
        return mergerTable.getRoutingTable();
    }

    /**
     * @param mergerFile
     * @return B4_RoutingTable Object
     */
    private B4_Node[] getMergerNeighbourTable(String mergerFile) {
        B4_RoutingTables mergerTable = new B4_RoutingTables(mergerFile);
        return mergerTable.getNeighbourTable();
    }

    /**
     * @param mergerFile
     * @return B4_Node Object
     */
    private B4_Node getSelfNodeOfMergerTable(String mergerFile) {
        B4_RoutingTables mergerTable = new B4_RoutingTables(mergerFile);
        return mergerTable.getSelfNodeID();
    }

    /**
     * @param mergerNode
     * @param routingTable All the algorithm for merging the routing and neighbour table is defined in this function.
     */
    private void mergerRT(B4_Node mergerNode, B4_Node[][] routingTable) {
        int preNodeIdInHex;
        int sucNodeIdInHex;
        String mergerNodeID = mergerNode.getNodeID();
        String localNodeID = localNode.getNodeID();
        //System.out.println();
        //System.out.println("mergerNodeID  " + mergerNodeID + "\nLocalNodeID  " + localNodeID);
        char[] mergerNodeidInCharArray = mergerNodeID.toCharArray();
        char[] localNodeidInCharArray = localNodeID.toCharArray();

        /**
         * 1. Check for the first mismatch in nibble between mergerNodeId and localNodeId.
         * 2. IF predecessor,successor and middle entry is empty, mergerNode is added to predecessor and successor.
         * 3. Else, conditions were checked one by one.
         * 4. First condition is if mergerNodeId lies between the existing predecessor and localNodeId
         * 5. Second condition is mergerNodeId lies between the localNodeId and existing SuccessorNodeId
         * 6. Third condition is if mergerNodeId lies between successor and predecessor.
         */
        for (int k = 0; k < rt_dimension; k++) {
            //System.out.println(k);
            char[] preNodeIdCharArray = routingTable[k][0].getNodeID().toCharArray();
            char[] sucNodeIdCharArray = routingTable[k][1].getNodeID().toCharArray();
            if (mergerNodeidInCharArray[k] != localNodeidInCharArray[k]) {
                String mergerNodeIdChar = Character.toString(mergerNodeID.charAt(k));
                String localNodeIdChar = Character.toString(localNodeID.charAt(k));
                int mergerNodeIdInHex = Integer.parseInt(mergerNodeIdChar, 16);
                int localNodeIdInHex = Integer.parseInt(localNodeIdChar, 16);
                //System.out.println("K " + k);
                //System.out.println("mergerNodeIDHex  " + mergerNodeIdInHex + "  LocalNodeIDHex  " + localNodeIdInHex);

                if (routingTable[k][0].getNodeID().isEmpty() && routingTable[k][1].getNodeID().isEmpty() && routingTable[k][2].getNodeID().isEmpty()) {
                    //System.out.println(k + " th column is empty");
                    routingTable[k][0] = mergerNode;
                    routingTable[k][1] = mergerNode;
                    break;
                } else {
                    String preNodeIdChar = Character.toString(routingTable[k][0].getNodeID().charAt(k));
                    preNodeIdInHex = Integer.parseInt(preNodeIdChar, 16);
                    String sucNodeIdChar = Character.toString(routingTable[k][1].getNodeID().charAt(k));
                    sucNodeIdInHex = Integer.parseInt(sucNodeIdChar, 16);
                    //System.out.println("preNodeIdHex  " + preNodeIdInHex + "  sucNodeIdHex  " + sucNodeIdInHex);

                    /**
                     * 1. Following is for checking the first Condition ie mergerNodeId lies between predecessor and localNodeId.
                     * 2. Since we are looking into a circular ring with nibble value range from 0-15,all possible conditions need to be checked.
                     * 3. Like Predecessor > LocalNodeId or Predecessor < LocalNodeId  and similarly for all cases of mergerNodeId.
                     * 4. Like mergerNodeId > LocalNodeId or mergerNodeId < LocalNodeId.
                     */
                    if (preNodeIdInHex <= mergerNodeIdInHex && mergerNodeIdInHex < localNodeIdInHex) {
                        //System.out.println("In Predecessor:First part");
                        if (preNodeIdInHex == mergerNodeIdInHex) {
                            for (int i = k + 1; i < rt_dimension; i++) {
                                if (mergerNodeidInCharArray[i] != preNodeIdCharArray[i]) {
                                    String nxtPreIdInChar = Character.toString(routingTable[k][0].getNodeID().charAt(i));
                                    String nxtMergerIdChar = Character.toString(mergerNodeID.charAt(i));
                                    int nxtPreNodeIdInHex = Integer.parseInt(nxtPreIdInChar, 16);
                                    int nxtMergerNodeIdInHex = Integer.parseInt(nxtMergerIdChar, 16);
                                    if (nxtMergerNodeIdInHex > nxtPreNodeIdInHex) {
                                        routingTable[k][0] = mergerNode;
                                    }
                                    break;
                                }
                            }
                        } else if (preNodeIdInHex == sucNodeIdInHex) {
                            routingTable[k][0] = mergerNode;
                            break;
                        } else {
                            routingTable[k][2] = routingTable[k][0];
                            routingTable[k][0] = mergerNode;
                            break;
                        }
                    } else if (preNodeIdInHex > localNodeIdInHex) {
                        //System.out.println("In Predecessor : 2nd Part");
                        if (preNodeIdInHex - 16 < mergerNodeIdInHex && mergerNodeIdInHex < localNodeIdInHex) {
                            if (preNodeIdInHex != sucNodeIdInHex) {
                                routingTable[k][2] = routingTable[k][0];
                            }
                            routingTable[k][0] = mergerNode;
                            break;

                        } else if (mergerNodeIdInHex > localNodeIdInHex && preNodeIdInHex - 16 < mergerNodeIdInHex - 16 && mergerNodeIdInHex - 16 < localNodeIdInHex) {
                            if (preNodeIdInHex != sucNodeIdInHex) {
                                routingTable[k][2] = routingTable[k][0];
                            }
                            routingTable[k][0] = mergerNode;
                            break;

                        } else if (preNodeIdInHex == mergerNodeIdInHex) {
                            for (int i = k + 1; i < rt_dimension; i++) {
                                if (mergerNodeidInCharArray[i] != preNodeIdCharArray[i]) {
                                    String nxtPreIdInChar = Character.toString(routingTable[k][0].getNodeID().charAt(i));
                                    String nxtMergerIdChar = Character.toString(mergerNodeID.charAt(i));
                                    int nxtPreNodeIdInHex = Integer.parseInt(nxtPreIdInChar, 16);
                                    int nxtMergerNodeIdInHex = Integer.parseInt(nxtMergerIdChar, 16);
                                    if (nxtMergerNodeIdInHex > nxtPreNodeIdInHex) {
                                        routingTable[k][0] = mergerNode;
                                    }
                                    break;
                                }
                            }
                        }
                    }
                    /**
                     * 1. Following is for checking the second condition ie mergerNodeId lies between successor and localNodeId.
                     * 2. Since we are looking into a circular ring with nibble value range from 0-15,all possible conditions need to be checked.
                     * 3. Like Successor > LocalNodeId or Successor < LocalNodeId  and similarly for all cases of mergerNodeId.
                     * 4. Like mergerNodeId > LocalNodeId or mergerNodeId < LocalNodeId.
                     */
                    if (sucNodeIdInHex >= mergerNodeIdInHex && mergerNodeIdInHex > localNodeIdInHex) {
                        //System.out.println("In successor : Ist Part");
                        if (sucNodeIdInHex == mergerNodeIdInHex) {
                            for (int i = k + 1; i < rt_dimension; i++) {
                                if (mergerNodeidInCharArray[i] != sucNodeIdCharArray[i]) {
                                    String nxtSucIdInChar = Character.toString(routingTable[k][1].getNodeID().charAt(i));
                                    String nxtMergerIdChar = Character.toString(mergerNodeID.charAt(i));
                                    int nxtSucNodeIdInHex = Integer.parseInt(nxtSucIdInChar, 16);
                                    int nxtMergerNodeIdInHex = Integer.parseInt(nxtMergerIdChar, 16);
                                    if (nxtMergerNodeIdInHex < nxtSucNodeIdInHex) {
                                        routingTable[k][1] = mergerNode;
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
                            break;
                        }
                    } else if (sucNodeIdInHex < localNodeIdInHex) {
                        //System.out.println("In successor : 2nd Part");
                        if (sucNodeIdInHex + 16 > mergerNodeIdInHex && mergerNodeIdInHex > localNodeIdInHex) {
                            if (preNodeIdInHex != sucNodeIdInHex) {
                                routingTable[k][2] = routingTable[k][1];
                            }
                            routingTable[k][1] = mergerNode;
                            break;

                        } else if (mergerNodeIdInHex < localNodeIdInHex && sucNodeIdInHex + 16 > mergerNodeIdInHex + 16 && mergerNodeIdInHex + 16 > localNodeIdInHex) {
                            if (preNodeIdInHex != sucNodeIdInHex) {
                                routingTable[k][2] = routingTable[k][1];
                            }
                            routingTable[k][1] = mergerNode;
                            break;
                        } else if (sucNodeIdInHex == mergerNodeIdInHex) {
                            for (int i = k + 1; i < rt_dimension; i++) {
                                if (mergerNodeidInCharArray[i] != sucNodeIdCharArray[i]) {
                                    String nxtSucIdInChar = Character.toString(routingTable[k][1].getNodeID().charAt(i));
                                    String nxtMergerIdChar = Character.toString(mergerNodeID.charAt(i));
                                    int nxtSucNodeIdInHex = Integer.parseInt(nxtSucIdInChar, 16);
                                    int nxtMergerNodeIdInHex = Integer.parseInt(nxtMergerIdChar, 16);
                                    if (nxtMergerNodeIdInHex < nxtSucNodeIdInHex) {
                                        routingTable[k][1] = mergerNode;
                                    }
                                    break;
                                }
                            }
                        }
                    }

                    /**
                     * 1. Following is for checking the Third condition ie mergerNodeId lies between predecessor and successor.
                     * 2. Since we are looking into a circular ring with nibble value range from 0-15,all possible conditions need to be checked.z
                     */
                    if (sucNodeIdInHex < mergerNodeIdInHex && mergerNodeIdInHex < preNodeIdInHex || sucNodeIdInHex < mergerNodeIdInHex && mergerNodeIdInHex < preNodeIdInHex + 16 || sucNodeIdInHex < mergerNodeIdInHex + 16 && mergerNodeIdInHex + 16 < preNodeIdInHex + 16) {
                        //System.out.println("In Middle");
                        if (!routingTable[k][2].getNodeID().isEmpty()) {
                            String existingMidNodeIdChar = Character.toString(routingTable[k][2].getNodeID().charAt(k));
                            int existingMidNodeIdHex = Integer.parseInt(existingMidNodeIdChar, 16);
                            if (Math.abs(((localNodeIdInHex + 8) % 16) - mergerNodeIdInHex) < Math.abs(((localNodeIdInHex + 8) % 16) - existingMidNodeIdHex)) {
                                routingTable[k][2] = mergerNode;
                                break;
                            }
                        } else {
                            routingTable[k][2] = mergerNode;
                            break;
                        }
                    }
                }
                break;
            }
        }
    }

    /**
     * @return B4_Node object
     */
    private B4_Node getBootStrapNodeID() {
        B4_Node bootStrapNode = null;
        try {
            FileReader reader = new FileReader("config.properties");
            Properties properties = new Properties();
            properties.load(reader);
            String bootStrapID = properties.getProperty("BootstrapND");
            String bootStrapIP = properties.getProperty("BootstrapPvtIP");
            String bootStrapPort = properties.getProperty("BootstrapPort");
            String bootStrapAddress = properties.getProperty("BootstrapAddress");
            bootStrapNode = new B4_Node(bootStrapID, bootStrapIP, bootStrapPort, bootStrapAddress);
        } catch (IOException e) {
            System.out.println("Config file not Found or Issue in config file fetching");
            ;
        }
        return bootStrapNode;
    }

    /**
     * @param fileHeading
     * @param routingTable
     * @param neighbourTable <br>This function is used to convert the Routing Table in the form of an array to xml format
     *                       <br>Here XML parsing is used.
     */
    private void localBaseTablesToXML(String fileHeading, B4_Node[][] routingTable, B4_Node[] neighbourTable) {
        String selfNodeId = localNode.getNodeID();
        String selfIPAddress = localNode.getIpAddress();
        String selfPortAddress = localNode.getPortAddress();
        String selfTransport = localNode.getTransport();

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();

            //create root Element
            Element root = doc.createElement(fileHeading);
            doc.appendChild(root);
            root.setAttribute("SELF_NODE_ID", selfNodeId);
            root.setAttribute("SELF_IP_ADDRESS", selfIPAddress);
            root.setAttribute("SELF_PORT_ADDRESS", selfPortAddress);
            root.setAttribute("SELF_TRANSPORT", selfTransport);

            for (int i = 0; i < rt_dimension; i++) {
                for (int j = 0; j < 3; j++) {
                    Element row = doc.createElement("B4_Node");
                    root.appendChild(row);
                    row.setAttribute("INDEX", "[" + i + "]" + "[" + j + "]");

                    Element nodeID = doc.createElement("NODEID");
                    nodeID.appendChild(doc.createTextNode(routingTable[i][j].getNodeID()));
                    row.appendChild(nodeID);

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
                nodeID.appendChild(doc.createTextNode(neighbourTable[i].getNodeID()));
                row1.appendChild(nodeID);

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
            /**
             * For debugging//
             * StreamResult streamResult = new StreamResult(System.out);
             **/
            StreamResult streamResult = new StreamResult(new File(fileHeading + ".xml"));
            transformer.transform(domSource, streamResult);
            System.out.println("Added to file " + fileHeading);
        } catch (ParserConfigurationException | TransformerException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param serviceName
     * @return boolean
     */
    private boolean serviceAccess(String serviceName) {
        boolean access = false;
        FileReader reader;
        try {
            reader = new FileReader("config.properties");
            Properties properties = new Properties();
            properties.load(reader);
            String value = properties.getProperty(serviceName);
            if (value.contentEquals("yes")) access = true;

        } catch (IOException e) {
            System.out.println("Service Not found in Config file\n" + e);
        }
        return access;
    }

    private int getRT_length() {
        int routingTable_length = 0;
        FileReader reader;
        try {
            reader = new FileReader("config.properties");
            Properties properties = new Properties();
            properties.load(reader);
            String value = properties.getProperty("RT_length");
            routingTable_length = Integer.parseInt(value);

        } catch (IOException e) {
            System.out.println("RT_length parameter not found in config file\n" + e);
        }
        return routingTable_length;
    }

    /**
     * @param rtFileName         0     * @param routingTableName
     * @param neighbourTableName
     */
    public void purgeRTEntry(String rtFileName, B4_Node[][] routingTableName, B4_Node[] neighbourTableName) {
        //Two counter arrays were created to keep track of no of failed ping.
        int[][] counter_rtable = new int[rt_dimension][3];
        int[] counter_neighbour = new int[nt_dimension];
        long currentTime = System.currentTimeMillis();
        System.out.println("I am in main Thread");

        // New thread is created
        Thread purgeThread = new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("Thread is started");
                //count will decide the number of times the while loop will run.
                //I have choosen count value four here.It can be changed to any other value depending on the requirment
                int count = 0;
                int dataPurged_RT = 0;
                int dataPurged_Neighbour = 0;
                long sleepingTime =0;
                while (true) {
                    while (!(count >= 4)) {
                        for (int i = 0; i < rt_dimension; i++) {
                            for (int j = 0; j < 3; j++) {
                                String ipAddressBase = routingTableName[i][j].getIpAddress();
                                System.out.println(ipAddressBase);
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
                                    } catch (UnknownHostException e) {
                                        e.printStackTrace();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                                if (counter_rtable[i][j] == 4) {
                                    routingTableName[i][j] = new B4_Node("", "", "", "");
                                    System.out.println("Data is purged");
                                    dataPurged_RT = dataPurged_RT + 1;
                                    counter_rtable[i][j] = 0;
                                }
                            }
                        }
                        for (int k = 0; k < 16; k++) {
                            String ipAddressNeighbour = neighbourTableName[k].getIpAddress();
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
                                } catch (UnknownHostException e) {
                                    e.printStackTrace();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                            if (counter_neighbour[k] == 4) {
                                neighbourTableName[k] = new B4_Node("", "", "", "", -1);
                                System.out.println("Data is purged");
                                dataPurged_Neighbour = dataPurged_Neighbour + 1;
                                counter_neighbour[k] = 0;
                            }
                        }
                        localBaseTablesToXML(rtFileName, routingTableName, neighbourTableName);
                        count = count + 1;
                    }
                    count = 0;
                    System.out.println(dataPurged_Neighbour);
                    System.out.println(dataPurged_RT);
                    if (dataPurged_RT == 0 && dataPurged_Neighbour == 0) {
                        try {
                            sleepingTime = sleepingTime + incrementTime;
                            System.out.println("going for sleeping for " + sleepingTime);
                            Thread.sleep(sleepingTime);
                            dataPurged_Neighbour=0;
                            dataPurged_RT=0;
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } else {
                        try {
                            sleepingTime = sleepTime;
                            System.out.println("going for sleeping for " + sleepingTime);
                            Thread.sleep(sleepingTime);
                            dataPurged_Neighbour=0;
                            dataPurged_RT=0;
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
        purgeThread.start();
    }

    public B4_Node[][] getLocalBaseRoutingTable() {
        return localBaseRoutingTable;
    }

    public B4_Node[] getLocalBaseNeighbourTable() {
        return localBaseNeighbourTable;
    }

    public B4_Node[][] getStorageRoutingTable() {
        return storageRoutingTable;
    }

    public B4_Node[] getStorageNeighbourTable() {
        return storageNeighbourTable;
    }

    private long getIncrementTime () {
        long increment_Time = 0;
        try {
            FileReader reader = new FileReader("config.properties");
            Properties properties = new Properties();
            properties.load(reader);
            String inc_Time = properties.getProperty("Increment_time");
            increment_Time = Long.parseLong(inc_Time);

        } catch (IOException e) {
            System.out.println("Config file not Found or Issue in config file fetching");

        }
        return increment_Time;
    }

    private long getSleepTime () {
        long sleep_Time = 0;
        try {
            FileReader reader = new FileReader("config.properties");
            Properties properties = new Properties();
            properties.load(reader);
            String slp_time = properties.getProperty("Sleep_time");
            sleep_Time = Long.parseLong(slp_time);

        } catch (IOException e) {
            System.out.println("Config file not Found or Issue in config file fetching");

        }
        return sleep_Time;
    }

    private int getNT_length() {
        int neighbourTable_length = 0;
        FileReader reader;
        try {
            reader = new FileReader("config.properties");
            Properties properties = new Properties();
            properties.load(reader);
            String value = properties.getProperty("NT_length");
            neighbourTable_length = Integer.parseInt(value);

        } catch (IOException e) {
            System.out.println("RT_length parameter not found in config file\n" + e);
        }
        return neighbourTable_length;
    }
}
