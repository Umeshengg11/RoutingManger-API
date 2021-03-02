package TestingModule;;
import main.B4_NodeTuple;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

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
import java.security.*;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Random;

public class CreateRoutingTablesForTesting {
    public static File createXML(String fileName) {
        String selfNodeId = "7589ABAA1234ABA1234591111ABCDFE123456789";
        PublicKey selfPublicKey = keyPairGeneration();
        String selfHashID = "7589ABAA1234ABA1234591111ABCDFE1234567897589ABAA1234ABA1234591111ABCDFE123456789";
        String selfNodeIP = "192.168.10.5";
        String selfNodePort = "777";
        String selfNodeTransport = "UDP";
        float rtt = 1012;
        B4_NodeSimulator[][] testRoutingtable = new B4_NodeSimulator[40][3];
        B4_NodeSimulator[] testNeighbourTable = new B4_NodeSimulator[16];
        String testNodeID;
        PublicKey testPublicKey;
        String testHashID;
        String testNodeIP;
        int testNodePort = 6666;
        String testTransport = "TCP";
        for (int i = 0; i < 40; i++) {
            for (int j = 0; j < 3; j++) {
                testNodeID = getNodeID();
                testPublicKey = keyPairGeneration();
                testHashID = getNodeID()+""+getNodeID()+""+getNodeID();
                testNodeIP = getIPaddress();
                testRoutingtable[i][j] = new B4_NodeSimulator(new B4_NodeTuple(testNodeID,testPublicKey,testHashID), testNodeIP, (testNodePort + i + j) + "", testTransport);
            }
        }

        for (int i = 0; i < 16; i++) {
            testNodeID = getNodeID();
            testNodeIP = getIPaddress();
            testPublicKey = keyPairGeneration();
            testHashID = getNodeID()+""+getNodeID()+""+getNodeID();
            testNeighbourTable[i] = new B4_NodeSimulator(new B4_NodeTuple(testNodeID,testPublicKey,testHashID), testNodeIP, (testNodePort + i) + "", (testTransport), (rtt + i));
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();

            //create root Element
            Element root = doc.createElement("Routingtable");
            doc.appendChild(root);
            root.setAttribute("SELF_NODE_ID", selfNodeId);
            root.setAttribute("SELF_PUBLIC_KEY", pubToStr(selfPublicKey));
            root.setAttribute("SELF_HASH_ID",selfHashID);
            root.setAttribute("SELF_IP_ADDRESS", selfNodeIP);
            root.setAttribute("SELF_PORT_ADDRESS", selfNodePort);
            root.setAttribute("SELF_TRANSPORT", selfNodeTransport);

            for (int i = 0; i < 40; i++) {
                for (int j = 0; j < 3; j++) {
                    Element row = doc.createElement("NODE");
                    root.appendChild(row);
                    row.setAttribute("INDEX", "[" + i + "]" + "[" + j + "]");

                    Element nodeID = doc.createElement("NODEID");
                    nodeID.appendChild(doc.createTextNode(testRoutingtable[i][j].getB4node().getNodeID()));
                    row.appendChild(nodeID);

                    Element publicKey = doc.createElement("PUBLICKEY");
                    publicKey.appendChild(doc.createTextNode(pubToStr(testRoutingtable[i][j].getB4node().getPublicKey())));
                    row.appendChild(publicKey);

                    Element hashID = doc.createElement("HASHID");
                    hashID.appendChild(doc.createTextNode(testRoutingtable[i][j].getB4node().getHashID()));
                    row.appendChild(hashID);

                    Element nodeIP = doc.createElement("NODEIP");
                    nodeIP.appendChild(doc.createTextNode(testRoutingtable[i][j].getIpAddress()));
                    row.appendChild(nodeIP);

                    Element nodePort = doc.createElement("NODEPORT");
                    nodePort.appendChild(doc.createTextNode(testRoutingtable[i][j].getPortAddress()));
                    row.appendChild(nodePort);

                    Element nodeTransport = doc.createElement("NODETRANSPORT");
                    nodeTransport.appendChild(doc.createTextNode(testRoutingtable[i][j].getTransport()));
                    row.appendChild(nodeTransport);
                }
            }

            for (int i = 0; i < 16; i++) {
                Element row1 = doc.createElement("NEIGHBOUR");
                root.appendChild(row1);
                row1.setAttribute("INDEX", "[" + i + "]");

                Element nodeID = doc.createElement("NODEID");
                nodeID.appendChild(doc.createTextNode(testNeighbourTable[i].getB4node().getNodeID()));
                row1.appendChild(nodeID);

                Element publicKey = doc.createElement("PUBLICKEY");
                publicKey.appendChild(doc.createTextNode(pubToStr(testNeighbourTable[i].getB4node().getPublicKey())));
                row1.appendChild(publicKey);

                Element hashID = doc.createElement("HASHID");
                hashID.appendChild(doc.createTextNode(testNeighbourTable[i].getB4node().getHashID()));
                row1.appendChild(hashID);

                Element nodeIP = doc.createElement("NODEIP");
                nodeIP.appendChild(doc.createTextNode(testNeighbourTable[i].getIpAddress()));
                row1.appendChild(nodeIP);

                Element nodePort = doc.createElement("NODEPORT");
                nodePort.appendChild(doc.createTextNode(testNeighbourTable[i].getPortAddress()));
                row1.appendChild(nodePort);

                Element nodeTransport = doc.createElement("NODETRANSPORT");
                nodeTransport.appendChild(doc.createTextNode(testNeighbourTable[i].getTransport()));
                row1.appendChild(nodeTransport);

                Element nodeRTT = doc.createElement("NODERTT");
                nodeRTT.appendChild(doc.createTextNode(String.valueOf(testNeighbourTable[i].getRtt())));
                row1.appendChild(nodeRTT);


            }
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource domSource = new DOMSource(doc);
            /**
             * For debugging//
             * StreamResult streamResult = new StreamResult(System.out);
             **/

            StreamResult streamResult = new StreamResult(new File(fileName+".xml"));
            transformer.transform(domSource, streamResult);
        } catch (ParserConfigurationException | TransformerConfigurationException e) {
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();
        }

        File file1 = new File(fileName+".xml");
        return file1;

    }

    public static String getNodeID() {

        String hex1 = "";

        for (int i = 0; i < 60; i++) {
            Random random = new Random();
            int randInt = random.nextInt(15);
            String[] hex = new String[60];
            hex[i] = Integer.toHexString(randInt).toUpperCase();
            hex1 = hex1.concat(hex[i]);

        }
        return hex1;
    }

    public static String getIPaddress() {
        Random rand = new Random();
        int randInt12 = rand.nextInt(100);
        String randIntS = Integer.toString(randInt12);
        String ipAddressRandom = "192.168.2.".concat(randIntS);

        return ipAddressRandom;

    }

    private static PublicKey keyPairGeneration() {
        PublicKey publicKey = null;
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(1024, new SecureRandom());
            KeyPair keyPair = keyPairGenerator.generateKeyPair();

            publicKey = keyPair.getPublic();
            //System.out.println(publicKey);

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return publicKey;
    }

    private static String pubToStr(PublicKey key){
        String strPub=null;
        PublicKey publicKey1 = null;
        //converting public key to byte[] and then convert it in to string
        byte[] bytePub = key.getEncoded();
        strPub = Base64.getEncoder().encodeToString(bytePub);
        //System.out.println("String format is   "+strPub);
        return strPub;
    }

    private static PublicKey strToPub(String str){
        PublicKey publicKey = null;
        //converting string to byte initially and then back to public key
        byte[] bytePub1 = Base64.getDecoder().decode(str);

        KeyFactory factory = null;
        try {
            factory = KeyFactory.getInstance("RSA");
            publicKey = (RSAPublicKey)factory.generatePublic(new X509EncodedKeySpec(bytePub1));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
        }
        return publicKey;
    }
}
