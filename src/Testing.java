import java.io.File;
import java.io.IOException;

public class Testing {
    public static void main(String[] args) throws IOException, InterruptedException {
        RoutingManager rt = RoutingManager.getInstance();
        String ip = rt.getSystemIP();
        System.out.println(ip);
        System.out.println(rt.getMACAddress());
//        CreateXMLWithEntry makeXML = new CreateXMLWithEntry();
//        rt.addFileToInputBuffer(makeXML.createXML());
//        rt.addFileToInputBuffer(makeXML.createXML());
//        rt.addFileToInputBuffer(makeXML.createXML());
//        File file = rt.fetchFileFromOutputBuffer();
//        InActCommunicationManager in = new InActCommunicationManager();
//        in.returnRTTData(file);


//        B4_Node node = rt.findNextHop("6588DBAA1286821A9B66AEDA0CA7BBA29DEA9C9C", 0);
//        if (node != null) System.out.println("Next hop is " + node.getB4node().getNodeID());
//        else System.out.println("Current Node is the Root Node " + rt.getLocalNode().getB4node().getNodeID());
//
       // rt.getRTTMergerTable("TestRoutingTable.xml");
//       // rt.rt_length();
//
//        rt.purgeRTEntry("BaseRoutingTable",rt.getLocalBaseRoutingTable(),rt.getLocalBaseNeighbourTable());
//        B4_NodeGeneration b4_nodeGeneration = B4_NodeGeneration.getInstance();
//        System.out.println(b4_nodeGeneration.getHashID());
//        System.out.println(b4_nodeGeneration.getPublicKey());
//        System.out.println(b4_nodeGeneration.getNodeID());
        // rt.getRTTMergerTable("TestStorageLayerRT.xml",1);

    }
}
