package TestingModule;

import com.ehelpy.brihaspati4.routingManagerAPI.RoutingManager;
import java.io.File;
import java.io.IOException;

/**
 * This is the main Test class for this API.
 * All the function are tested from this class.
 */
public class TestingModule {
    public static void main(String[] args) throws IOException, InterruptedException {
        RoutingManager rt = RoutingManager.getInstance();


        /* IndexingManager file Check for root node */
//        File file = new File("Table0_RootNodeCheck.xml");
//        rt.addFileToInputBuffer(file);

        /*Generate Routing Table file */
        File file1 = rt.getRoutingTableXMLFile("BaseRT", "BaseRT", rt.getRoutingTable(0));

        /*Adding file to InputBuffer*/
        rt.addFileToInputBuffer(file1);

        /*Generate Neighbour Table File*/
        rt.getNeighbourTableXMLFile("BaseNT", "BaseNT", rt.getNeighbourTable(0));

        /*Creating New Layer*/
        rt.createNewLayer("MessageRoutingTable");

        /*Creating routing and differential table for merging*/
        CreateRoutingTablesForTesting xm = new CreateRoutingTablesForTesting();
        rt.addFileToInputBuffer(xm.createXML("BaseRoutingTable", 0));
        rt.addFileToInputBuffer(xm.simulatedDiffTable("DiffR", 0));

        /*Work done by simulated communication manger and generating RcvRTT file*/
        CommunicationManagerSimulator in = new CommunicationManagerSimulator();
        Thread th = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    File file = rt.getFileFromOutputBuffer();
                    if (file != null) {
                        System.out.println(file.getName());
                        if (file.getName().startsWith("GetRTT"))
                            in.returnRTTData(file, rt);
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }
        });
        th.start();


//        B4_Node b4_node = rt.getLocalNode();

       // rt.dateTimeCheck();


//        System.out.println(rt.getNodeID());
//        System.out.println(rt.getHashID());
//
//
//        rt.generateNewNodeID();
//        System.out.println(rt.getNodeID());
//        System.out.println(rt.getHashID());
//
//
//        for (int i = 0; i <16 ; i++) {
//          B4_Node b4_Node[]= rt.getNeighbourTable(0);
//          b4_Node[i].getB4node().getNodeID();
//        }
//
//        System.out.println(rt.getSystemIP());

//
//        System.out.println("The NodeID is verified as - "+rt.verifySignature(rt.getHashID(),rt.getPublicKey(),rt.getNodeID()));
////rt.purgeRTEntry("BaseRoutingTable",rt.getLocalBaseRoutingTable(),rt.getLocalBaseNeighbourTable());
//       B4_Node b4_node = rt.findNextHop("FD2051C7A9CD59A1BE822F699267C42DE64C0904",0);
//        System.out.println(b4_node );

    }
}

