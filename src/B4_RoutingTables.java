import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class B4_RoutingTables {
    private B4_Node[][] routingTable;
    private final String mergerFile;
    private B4_Node selfNodeID;
    private B4_Node[] neighbourTable;
    private final int rt_dimension;

    public B4_RoutingTables(String mergerRTFile) {
        rt_dimension=getRT_length();
        this.mergerFile = mergerRTFile;
        routingTable = new B4_Node[rt_dimension][3];
        neighbourTable = new B4_Node[16];
        String selfNodeID;
        String selfIPAddress;
        String selfPortAddress;
        String selfTransport;
        B4_Node selfMergerNode = null;
        DocumentBuilderFactory builderFactory;
        DocumentBuilder documentBuilder;
        Document doc;

        try {
            //Get Document builder
            builderFactory = DocumentBuilderFactory.newInstance();
            documentBuilder = builderFactory.newDocumentBuilder();

            //Load the input XML document,parse it and return an instance of Document class
            doc = documentBuilder.parse(new File(mergerFile));
            doc.getDocumentElement().normalize();
            //String rootElement = doc.getDocumentElement().getNodeName();

            NodeList nodeList = doc.getElementsByTagName("NODE");
            for (int i = 0; i < nodeList.getLength(); i++) {
                // System.out.println(nodeList.getLength());
                Node node = nodeList.item(i);
                //System.out.println(node.getNodeName());

                if (node.getNodeType() == node.ELEMENT_NODE) {
                    Element element = (Element) node;

                    //Get the value of ID attribute
                    String index = node.getAttributes().getNamedItem("INDEX").getNodeValue();
                    //System.out.println(index);

                    //Get value of all sub-Elements
                    String nodeID = element.getElementsByTagName("NODEID").item(0).getTextContent();
                    String nodeIP = element.getElementsByTagName("NODEIP").item(0).getTextContent();
                    String nodePort = element.getElementsByTagName("NODEPORT").item(0).getTextContent();
                    String nodeTransport = element.getElementsByTagName("NODETRANSPORT").item(0).getTextContent();

                    // System.out.println(nodeID + "    " + nodeIP + "   " + nodePort + "  " + nodeTransport);
                    Pattern pattern = Pattern.compile("\\[([^]]+)\\]");
                    Matcher matcher = pattern.matcher(index);
                    matcher.find();
                    int index1 = Integer.parseInt(matcher.group(1));
                    matcher.find();
                    int index2 = Integer.parseInt(matcher.group(1));
                    //System.out.println(index1 + " " + index2);
                    routingTable[index1][index2] = new B4_Node(nodeID, nodeIP, nodePort, nodeTransport);
                }
            }
            doc.getDocumentElement().normalize();
            selfNodeID = doc.getDocumentElement().getAttribute("SELF_NODE_ID");
            selfIPAddress = doc.getDocumentElement().getAttribute("SELF_IP_ADDRESS");
            selfPortAddress = doc.getDocumentElement().getAttribute("SELF_PORT_ADDRESS");
            selfTransport = doc.getDocumentElement().getAttribute("SELF_TRANSPORT");
            selfMergerNode = new B4_Node(selfNodeID, selfIPAddress, selfPortAddress, selfTransport);


            NodeList nodeList1 = doc.getElementsByTagName("NEIGHBOUR");
            for (int i = 0; i < nodeList1.getLength(); i++) {
                // System.out.println(nodeList.getLength());
                Node node = nodeList1.item(i);
                //System.out.println(node.getNodeName());

                if (node.getNodeType() == node.ELEMENT_NODE) {
                    Element element = (Element) node;

                    //Get the value of ID attribute
                    String index = node.getAttributes().getNamedItem("INDEX").getNodeValue();
                    //System.out.println(index);

                    //Get value of all sub-Elements
                    String nodeID = element.getElementsByTagName("NODEID").item(0).getTextContent();
                    String nodeIP = element.getElementsByTagName("NODEIP").item(0).getTextContent();
                    String nodePort = element.getElementsByTagName("NODEPORT").item(0).getTextContent();
                    String nodeTransport = element.getElementsByTagName("NODETRANSPORT").item(0).getTextContent();
                    String nodeRTT = element.getElementsByTagName("NODERTT").item(0).getTextContent();

                    // System.out.println(nodeID + "    " + nodeIP + "   " + nodePort + "  " + nodeTransport+""+nodeRTT);
                    Pattern pattern = Pattern.compile("\\[([^]]+)\\]");
                    Matcher matcher = pattern.matcher(index);
                    matcher.find();
                    int index1 = Integer.parseInt(matcher.group(1));
                    //System.out.println(index1);
                    neighbourTable[index1] = new B4_Node(nodeID, nodeIP, nodePort, nodeTransport, Float.parseFloat(nodeRTT));
                }
            }

        } catch (ParserConfigurationException | IOException | SAXException e) {
            e.printStackTrace();
        }


        setRoutingTable(routingTable);
        setSelfNodeID(selfMergerNode);
        setNeighbourTable(neighbourTable);
    }

    public B4_Node[][] getRoutingTable() {
        return routingTable;
    }

    public void setRoutingTable(B4_Node[][] routingTable) {
        this.routingTable = routingTable;
    }

    public B4_Node getSelfNodeID() {
        return selfNodeID;
    }

    public void setSelfNodeID(B4_Node selfNodeID) {
        this.selfNodeID = selfNodeID;
    }

    public B4_Node[] getNeighbourTable() {
        return neighbourTable;
    }

    public void setNeighbourTable(B4_Node[] neighbourTable) {
        this.neighbourTable = neighbourTable;
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
            System.out.println("RT_length parameter not found in config file\n"+e);
        }
        return routingTable_length;
    }
}
