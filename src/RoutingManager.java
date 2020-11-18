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
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RoutingManager {
    private static RoutingManager routingManager;
    private final B4_Node[][] localBaseRoutingTable;
    private final B4_Node[] localBaseNeighbourTable;
    private final B4_Node[][] storageRoutingTable;
    private final B4_Node[] storageNeighbourTable;
    private B4_Node[] mergerNeighbourTable;
    private B4_Node[][] mergerRoutingTable;
    private B4_Node localNode;

    private RoutingManager() {
        setLocalNode();
        localBaseRoutingTable = new B4_Node[40][3];
        localBaseNeighbourTable = new B4_Node[16];
        storageRoutingTable = new B4_Node[40][30];
        storageNeighbourTable = new B4_Node[16];
        init("BaseRoutingTable", localBaseRoutingTable, localBaseNeighbourTable);
        boolean access = serviceAccess("StorageAccess");
        if (access) init("StorageRoutingTable", storageRoutingTable, storageNeighbourTable);
    }

    private void init(String rtFileName, B4_Node[][] routingTable, B4_Node[] neighbourTable) {
        boolean rtExists;
        File rtFile = new File(rtFileName + ".xml");
        rtExists = rtFile.exists();

        if (!rtExists) {

            /* Routing Table */
            for (int i = 0; i < 40; i++) {
                for (int j = 0; j < 3; j++) {
                    routingTable[i][j] = new B4_Node("", "", "", "");
                }
            }
            B4_Node bootStrapNode = getBootStrapNodeID();
            mergerRT(bootStrapNode, routingTable);

            /* Neighbour Table */
            for (int i = 0; i < 16; i++) {
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

    public static synchronized RoutingManager getInstance() {
        if (routingManager == null) {
            routingManager = new RoutingManager();
        }
        return routingManager;
    }

    private void setLocalNode() {
        localNode = new B4_Node("4589ABAA1234ABC1234591111ABCDFE123456789", "192.168.0.105", "6666", "TCP");
    }

    public B4_Node getLocalNode() {
        return localNode;
    }

    public void mergeRoutingTable(String mergerTableDataFile, int layerID) {

        B4_Node[][] routingTableLayer = null;
        if (layerID == 0) {
            routingTableLayer = localBaseRoutingTable;
        } else if (layerID == 1) {
            routingTableLayer = storageRoutingTable;
        }
        B4_Node selfNodeOfMergerTable = getSelfNodeOfMergerTable(mergerTableDataFile);
        mergerRoutingTable = getMergerRoutingTable(mergerTableDataFile);
        /**
         This function (ie nibbleComparison()) perform nibble wise comparison between the mergerNodeID (from the mergerRoutingTable, here now it is selfNodeIdOfMergerTable) and localNodeID.
         For more info about the function refer nibbleComparison() function.
         */

        /**
         * 1. Obtain the merger Routing Table (ie the routing table which needs to be merged with the localBaseRoutingTable
         * 2. Obtain the localNodeID.
         * 3. Get first nodeId from the mergerTable and compare it with the localNode Id and obtain the first character which mismatches.
         * 4. This will give the column in localBaseRoutingTable which needs to be updated.
         * 5. nibbleComparison() is used for nibble wise comparison
         */
        mergerRT(selfNodeOfMergerTable, routingTableLayer);
        for (int i = 0; i < 40; i++) {
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

    public void mergeNeighbourTable(String mergerTableDataFile, int layerID) {
        B4_Node[] neighbourTable = null;
        if (layerID == 0) {
            neighbourTable = localBaseNeighbourTable;
        } else if (layerID == 1) {
            neighbourTable = storageNeighbourTable;
        }

        boolean rttFileExists;
        mergerNeighbourTable = new B4_Node[16];
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
            for (int i = 0; i < 16; i++) {
                assert selfMergerNode != null;
                if (selfMergerNode.getRtt() == -1) break;
                assert neighbourTable != null;
                if (neighbourTable[i].getRtt() == -1) {
                    neighbourTable[i] = selfMergerNode;
                    break;
                } else if (neighbourTable[i].getRtt() <= selfMergerNode.getRtt()) {
                    continue;
                } else {
                    for (int j = 15; j >= i + 1; j--) {
                        neighbourTable[j] = neighbourTable[j - 1];
                    }
                    neighbourTable[i] = selfMergerNode;
                }
            }
            for (int i = 0; i < 16; i++) {
                for (int j = 0; j < 16; j++) {
                    if (mergerNeighbourTable[i].getRtt() == -1) break;
                    assert neighbourTable != null;
                    if (neighbourTable[j].getRtt() == -1) {
                        neighbourTable[j] = mergerNeighbourTable[i];
                        break;
                    } else if (mergerNeighbourTable[i].getRtt() >= neighbourTable[j].getRtt()) {
                        continue;
                    } else {
                        for (int k = 15; k >= j + 1; k--) {
                            neighbourTable[k] = neighbourTable[k - 1];
                        }
                        neighbourTable[j] = mergerNeighbourTable[i];
                        break;
                    }
                }
            }
            for (int i = 0; i < 16; i++) {
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


            for (int i = 0; i < 16; i++) {
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
             * This will give value of k (i.e column at which we start looking for next hop).
             * IF this column is not empty check predecessor successor and middle row one by one based on the logic defined to get the next hop.
             */
        } else {
            for (int k = 0; k < 40; k++) {
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
                            if (k != 39) {
                                for (int i = k + 1; i < 40; i++) {
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
                                            if (i != 39) continue;
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
                                        if (i == 39) return null;
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

    private B4_Node[][] getMergerRoutingTable(String mergerFile) {
        B4_RoutingTables mergerTable = new B4_RoutingTables(mergerFile);
        return mergerTable.getRoutingTable();
    }

    private B4_Node[] getMergerNeighbourTable(String mergerFile) {
        B4_RoutingTables mergerTable = new B4_RoutingTables(mergerFile);
        return mergerTable.getNeighbourTable();
    }

    private B4_Node getSelfNodeOfMergerTable(String mergerFile) {
        B4_RoutingTables mergerTable = new B4_RoutingTables(mergerFile);
        return mergerTable.getSelfNodeID();
    }

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
        for (int k = 0; k < 40; k++) {
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
                            for (int i = k + 1; i < 40; i++) {
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
                            for (int i = k + 1; i < 40; i++) {
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
                            for (int i = k + 1; i < 40; i++) {
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
                            for (int i = k + 1; i < 40; i++) {
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
            System.out.println("Config file not Found or Issue in config file fetching");;
        }
        return bootStrapNode;
    }

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

            for (int i = 0; i < 40; i++) {
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
            for (int i = 0; i < 16; i++) {
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
            e.printStackTrace();
        }
        return access;
    }


}
