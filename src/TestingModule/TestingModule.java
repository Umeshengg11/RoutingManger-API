package TestingModule;

import main.RoutingManager;

import java.io.IOException;

public class TestingModule {
    public static void main(String[] args) throws IOException, InterruptedException {
        RoutingManager rt = RoutingManager.getInstance();
        rt.createNewLayer("VoipRoutingTable");
        rt.createNewLayer("OverCastingLayer");
        CreateRoutingTablesForTesting xm = new CreateRoutingTablesForTesting();
        rt.addFileToInputBuffer(xm.createXML("0BaseRoutingTable"));
        rt.addFileToInputBuffer(xm.createXML("1StorageRoutingTable"));
        rt.addFileToInputBuffer(xm.createXML("3VoipRoutingLayer"));
        CommunicationManagerSimulator in = new CommunicationManagerSimulator();
        in.returnRTTData(rt.fetchFileFromOutputBuffer());
        in.returnRTTData(rt.fetchFileFromOutputBuffer());
        in.returnRTTData(rt.fetchFileFromOutputBuffer());
//        String ip = rt.getSystemIP();
//        System.out.println(ip);
//        System.out.println(rt.getMACAddress());


    }
}

