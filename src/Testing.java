import java.io.IOException;

public class Testing {
    public static void main(String[] args) throws IOException, InterruptedException {
        RoutingManager rt = RoutingManager.getInstance();

        rt.mergeRoutingTable("TestRoutingTable.xml", 0);
        rt.mergeNeighbourTable("TestRoutingTable.xml", 0);
        rt.mergeRoutingTable("TestStorageLayerRT.xml", 1);
        rt.mergeNeighbourTable("TestStorageLayerRT.xml", 1);
//
//        B4_Node node = rt.findNextHop("6588DBAA1286821A9B66AEDA0CA7BBA29DEA9C9C", 1);
//        if (node != null) System.out.println("Next hop is " + node.getNodeID());
//        else System.out.println("Current Node is the Root Node " + rt.getLocalNode().getNodeID());

       //rt.getRTTMergerTable("TestRoutingTable.xml");


    }
}

