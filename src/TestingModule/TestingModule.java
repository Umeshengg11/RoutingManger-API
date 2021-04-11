package TestingModule;

import main.B4_Node;
import main.RoutingManager;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.cert.X509Certificate;

/**
 * This is the main Test class for this API.
 * All the function are tested from this class.
 */
public class TestingModule {
    public static void main(String[] args) throws IOException, InterruptedException {
        RoutingManager rt = RoutingManager.getInstance();
        rt.createNewLayer("MessageRoutingTable");
//        CreateRoutingTablesForTesting xm = new CreateRoutingTablesForTesting();
//        rt.addFileToInputBuffer(CreateRoutingTablesForTesting.createXML("BaseRoutingTable",0));
//        CommunicationManagerSimulator in = new CommunicationManagerSimulator();
//        Thread th = new Thread(new Runnable() {
//            @Override
//            public void run() {
//                while (true){
//                    in.returnRTTData(rt.getFileFromOutputBuffer(),rt);
//                    try {
//                        Thread.sleep(1000);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
//
//            }
//        });th.start();
////        B4_Node b4_node = rt.getLocalNode();
////        rt.dateTimeCheck();
//        try {
//            X509Certificate certificate = (X509Certificate) rt.getKeystore().getCertificate("Certificate");
//            System.out.println("Old date "+certificate.getNotAfter());
//            rt.renewSelfSignedCertificate();
//            X509Certificate certificate1 = (X509Certificate) rt.getKeystore().getCertificate("Certificate");
//            System.out.println("New Date "+certificate1.getNotAfter());
//        } catch (KeyStoreException e) {
//            e.printStackTrace();
//        }
        System.out.println("The NodeID is verified as - "+rt.verifySignature(rt.getHashID(),rt.getPublicKey(),rt.getNodeID()));
//rt.purgeRTEntry("BaseRoutingTable",rt.getLocalBaseRoutingTable(),rt.getLocalBaseNeighbourTable());

    }
}

