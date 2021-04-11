package TestingModule;

import main.B4_Node;
import main.RoutingManager;

import java.io.IOException;

/**
 * This is the main Test class for this API.
 * All the function are tested from this class.
 */
public class TestingModule {
    public static void main(String[] args) throws IOException, InterruptedException {
        RoutingManager rt = RoutingManager.getInstance();
        rt.createNewLayer("VoipRoutingTable");
        rt.createNewLayer("OverCastingLayer");
        CreateRoutingTablesForTesting xm = new CreateRoutingTablesForTesting();
        rt.addFileToInputBuffer(CreateRoutingTablesForTesting.createXML("BaseRoutingTable",0));
        rt.addFileToInputBuffer(CreateRoutingTablesForTesting.createXML("StorageRoutingTable",1));
        rt.addFileToInputBuffer(CreateRoutingTablesForTesting.createXML("VoipRoutingLayer",3));
        CommunicationManagerSimulator in = new CommunicationManagerSimulator();
        Thread th = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true){
                    in.returnRTTData(rt.getFileFromOutputBuffer(),rt);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }
        });th.start();
        B4_Node b4_node = rt.getLocalNode();



        rt.dateTimeCheck();

//rt.purgeRTEntry("BaseRoutingTable",rt.getLocalBaseRoutingTable(),rt.getLocalBaseNeighbourTable());

    }
}

