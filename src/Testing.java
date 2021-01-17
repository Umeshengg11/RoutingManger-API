import java.io.File;
import java.io.IOException;

public class Testing {
    public static void main(String[] args) throws IOException, InterruptedException {
        RoutingManagerBuffer buffer = RoutingManagerBuffer.getInstance();
        CreateXMLWithEntry makeXML = new CreateXMLWithEntry();
        buffer.addFileToBuffer(makeXML.createXML());


        RoutingManager rt = RoutingManager.getInstance();
        File file = buffer.fetchFileFromBuffer();

      rt.mergeRoutingTable(file, 0);
       rt.mergeNeighbourTable(file, 0);
//       rt.mergeRoutingTable("TestStorageLayerRT.xml", 1);
//       rt.mergeNeighbourTable("TestStorageLayerRT.xml", 1);
//        B4_Node node = rt.findNextHop("6588DBAA1286821A9B66AEDA0CA7BBA29DEA9C9C", 0);
//        if (node != null) System.out.println("Next hop is " + node.getB4node().getNodeID());
//        else System.out.println("Current Node is the Root Node " + rt.getLocalNode().getB4node().getNodeID());
//
//        rt.getRTTMergerTable("TestRoutingTable.xml");
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
