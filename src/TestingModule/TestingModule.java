package TestingModule;

import main.RoutingManager;

import java.io.IOException;

public class TestingModule {
    public static void main(String[] args) throws IOException, InterruptedException {
        RoutingManager rt = RoutingManager.getInstance();
        rt.createNewLayer("VoipRoutingTable");
        rt.createNewLayer("OverCastingLayer");
        CreateRoutingTablesForTesting xm = new CreateRoutingTablesForTesting();
        rt.addFileToInputBuffer(xm.createXML());
        rt.addFileToInputBuffer(xm.createXML());
        rt.addFileToInputBuffer(xm.createXML());
        CommunicationManagerSimulator in = new CommunicationManagerSimulator();
        in.returnRTTData(rt.fetchFileFromOutputBuffer());
        in.returnRTTData(rt.fetchFileFromOutputBuffer());
        in.returnRTTData(rt.fetchFileFromOutputBuffer());
//        String ip = rt.getSystemIP();
//        System.out.println(ip);
//        System.out.println(rt.getMACAddress());


    }
}

