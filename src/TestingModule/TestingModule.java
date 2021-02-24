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

        Thread th = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true){
                    in.returnRTTData(rt.fetchFileFromOutputBuffer(),rt);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }
        });th.start();
        System.out.println(".................................");
        System.out.println(".................................");
        System.out.println(".................................");
        System.out.println(".................................");
        System.out.println(rt.verifySignature("B9ED3B104383EED455A022C68BCE327F1C4F1C30E4CD857D39D316E10255E9CB4B36D684B19D62767569A16572B5D2FCECB8B828555C217C0F4E1993CCD63BE98B4BAB5966E4372856A2D93F39E240FA7C6DDFCB95AF1CCA551D1A9E7810790E89C82FCBF5A915A18998E32123CB5650387B8D2C966D25600A19B67435327234"));


    }
}

