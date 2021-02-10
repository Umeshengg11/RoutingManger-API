package main;

import java.io.IOException;

public class Testing {
    public static void main(String[] args) throws IOException, InterruptedException {
        RoutingManager rt = RoutingManager.getInstance();
        CreateXMLWithEntry xm = new CreateXMLWithEntry();
        rt.addFileToInputBuffer(xm.createXML());
        rt.addFileToInputBuffer(xm.createXML());
        rt.addFileToInputBuffer(xm.createXML());
        InActCommunicationManager in = new InActCommunicationManager();
        in.returnRTTData(rt.fetchFileFromOutputBuffer());
        in.returnRTTData(rt.fetchFileFromOutputBuffer());
        in.returnRTTData(rt.fetchFileFromOutputBuffer());
        String ip = rt.getSystemIP();
        System.out.println(ip);
        System.out.println(rt.getMACAddress());
    }
}

