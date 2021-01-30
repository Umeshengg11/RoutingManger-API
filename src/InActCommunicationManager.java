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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InActCommunicationManager {

    public void returnRTTData(File file) {
        RoutingManager rt = RoutingManager.getInstance();
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
            }
        }
    }

    public File test(int layerID, String fileName){
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = null;
        try {
            documentBuilder = builderFactory.newDocumentBuilder();
            Document doc = documentBuilder.parse(new File(fileName ));
            doc.getDocumentElement().normalize();
            String rootElement = doc.getDocumentElement().getNodeName();
            System.out.println(rootElement);
            NodeList nodeList1 = doc.getElementsByTagName("NEIGHBOUR");
            for (int i = 0; i < nodeList1.getLength(); i++) {
                Node node = nodeList1.item(i);

                if (node.getNodeType() == node.ELEMENT_NODE) {
                    Element element = (Element) node;
                    String index = node.getAttributes().getNamedItem("INDEX").getNodeValue();

                    //Get value of all sub-Elements
                    String nodeID = element.getElementsByTagName("NODEID").item(0).getTextContent();
                    String nodePub = element.getElementsByTagName("PUBLICKEY").item(0).getTextContent();
                    String nodeHash = element.getElementsByTagName("HASHID").item(0).getTextContent();
                    String nodeIP = element.getElementsByTagName("NODEIP").item(0).getTextContent();
                    String nodePort = element.getElementsByTagName("NODEPORT").item(0).getTextContent();
                    String nodeTransport = element.getElementsByTagName("NODETRANSPORT").item(0).getTextContent();

                    element.getElementsByTagName("NODERTT").item(0).setTextContent("10");
                    String nodeRTT = element.getElementsByTagName("NODERTT").item(0).getTextContent();
                    System.out.println("Node RTT is " + nodeRTT);
                }
            }

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource domSource = new DOMSource(doc);
            StreamResult streamResult = new StreamResult(new File("RcvRTT_" + layerID + "_" + ".xml"));
            transformer.transform(domSource, streamResult);
            System.out.println("RcvRTT_" + layerID + "_" + ".xml" + " file updated");


        } catch (ParserConfigurationException | SAXException | IOException | TransformerException e) {
            e.printStackTrace();
        }
        File file1 = new File("RcvRTT_" + layerID + "_" + ".xml");
        return file1;

    }
}