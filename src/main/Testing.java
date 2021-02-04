package main;

import org.apache.log4j.PropertyConfigurator;

import java.io.IOException;

public class Testing {
    public static void main(String[] args) throws IOException, InterruptedException {
       // PropertyConfigurator.configure("log4j.properties");
        RoutingManager rt = RoutingManager.getInstance();
        String ip = rt.getSystemIP();
        System.out.println(ip);
        System.out.println(rt.getMACAddress());


//        main.resources.CreateXMLWithEntry makeXML = new main.resources.CreateXMLWithEntry();
//        rt.addFileToInputBuffer(makeXML.createXML());
//        rt.addFileToInputBuffer(makeXML.createXML());
//        rt.addFileToInputBuffer(makeXML.createXML());
//        File file = rt.fetchFileFromOutputBuffer();
//        main.resources.InActCommunicationManager in = new main.resources.InActCommunicationManager();
//        in.returnRTTData(file);


//        main.resources.B4_Node node = rt.findNextHop("6588DBAA1286821A9B66AEDA0CA7BBA29DEA9C9C", 0);
//        if (node != null) System.out.println("Next hop is " + node.getB4node().getNodeID());
//        else System.out.println("Current Node is the Root Node " + rt.getLocalNode().getB4node().getNodeID());
//
       // rt.getRTTMergerTable("TestRoutingTable.xml");
//       // rt.rt_length();
//
//        rt.purgeRTEntry("BaseRoutingTable",rt.getLocalBaseRoutingTable(),rt.getLocalBaseNeighbourTable());
//        main.resources.B4_NodeGeneration b4_nodeGeneration = main.resources.B4_NodeGeneration.getInstance();
//        System.out.println(b4_nodeGeneration.getHashID());
//        System.out.println(b4_nodeGeneration.getPublicKey());
//        System.out.println(b4_nodeGeneration.getNodeID());
        // rt.getRTTMergerTable("TestStorageLayerRT.xml",1);

    }
}
