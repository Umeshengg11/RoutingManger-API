package TestingModule;


import com.ehelpy.brihaspati4.routingManagerAPI.RoutingManager;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.util.Random;

/**
 * This class is a simulation class used to act as communication manager for testing purpose.
 */
public class CommunicationManagerSimulator {
    private static final Logger log = Logger.getLogger(CommunicationManagerSimulator.class);
    public void returnRTTData(File file, RoutingManager rt) {
        if (!(file == null)) {
            String fileName = file.getName();
            if (fileName.startsWith("0")) {

            } else if (fileName.startsWith("1")) {

            } else if (fileName.startsWith("GetRTT_0")) {
               File file1 = test(0,fileName);
               rt.addFileToInputBuffer(file1);
            } else if (fileName.startsWith("GetRTT_1")) {
                File file1 = test(1,fileName);
                rt.addFileToInputBuffer(file1);
            } else if(fileName.startsWith("GetRTT_3")){
                File file1 = test(3,fileName);
                rt.addFileToInputBuffer(file1);
            }
        }
    }

    public File test(int layerID, String fileName){
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = null;
        String selfNodeID = null;
        try {
            documentBuilder = builderFactory.newDocumentBuilder();
            Document doc = documentBuilder.parse(new File(fileName ));
            doc.getDocumentElement().normalize();
            String rootElement = doc.getDocumentElement().getNodeName();
            selfNodeID = doc.getDocumentElement().getAttribute("SELF_NODE_ID");
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
                    Random rand = new Random();
                    int randInt12 = rand.nextInt(200);
                    String randIntS = Integer.toString(randInt12);
                    element.getElementsByTagName("NODERTT").item(0).setTextContent(randIntS);
                    String nodeRTT = element.getElementsByTagName("NODERTT").item(0).getTextContent();

                }
            }

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource domSource = new DOMSource(doc);
            StreamResult streamResult = new StreamResult(new File("RcvRTT_" + layerID + "_"+ selfNodeID+ ".xml"));
            transformer.transform(domSource, streamResult);
            log.debug("RcvRTT_" + layerID + "_"+ selfNodeID+ ".xml" + "file updated");



        } catch (ParserConfigurationException | SAXException | IOException | TransformerException e) {
            log.error("Exception Occurred",e);
        }
        return new File("RcvRTT_" + layerID + "_"+ selfNodeID+ ".xml");

    }
}