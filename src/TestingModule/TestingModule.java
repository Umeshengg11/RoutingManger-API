package TestingModule;

import main.RoutingManager;

import java.io.IOException;

/**
 * This is the main Test class for this API.
 * All the function are tested from this class.
 */
public class TestingModule {
    public static void main(String[] args) throws IOException, InterruptedException {
        RoutingManager rt = RoutingManager.getInstance();
//        rt.createNewLayer("VoipRoutingTable");
//        rt.createNewLayer("OverCastingLayer");
//        CreateRoutingTablesForTesting xm = new CreateRoutingTablesForTesting();
//        rt.addFileToInputBuffer(xm.createXML("0BaseRoutingTable"));
//        rt.addFileToInputBuffer(xm.createXML("1StorageRoutingTable"));
//        rt.addFileToInputBuffer(xm.createXML("3VoipRoutingLayer"));
//        CommunicationManagerSimulator in = new CommunicationManagerSimulator();
//
//
//        Thread th = new Thread(new Runnable() {
//            @Override
//            public void run() {
//                while (true){
//                    in.returnRTTData(rt.fetchFileFromOutputBuffer(),rt);
//                    try {
//                        Thread.sleep(1000);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
//
//            }
//        });th.start();

//rt.purgeRTEntry("BaseRoutingTable",rt.getLocalBaseRoutingTable(),rt.getLocalBaseNeighbourTable());

    }
}

