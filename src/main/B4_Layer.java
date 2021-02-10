package main;

import org.apache.log4j.Logger;

import java.io.*;
import java.util.Properties;

public class B4_Layer {
    private int layerID;
    private int layerName;
    private static final Logger log = Logger.getLogger(B4_Layer.class);
    FileWriter writer;

    public B4_Layer() {
        File layerFile = new File("src/configuration/LayerDetails.txt");
        boolean layerDetailsExists = layerFile.exists();

        if (!layerDetailsExists) {
            try {
                writer = new FileWriter("src/configuration/LayerDetails.txt");
                PrintWriter printWriter = new PrintWriter(writer);
                printWriter.println("#  Layering Details  #");
                printWriter.println("..................................");
                printWriter.println("Heightens=3");
                printWriter.println("BaseRoutingTable=0");
                printWriter.println("StorageRoutingTable=1");
                printWriter.println("MessageRoutingTable=2");
                printWriter.println("VoipRoutingTable=3");
                printWriter.flush();
                printWriter.close();
                log.debug("LayerDetail File created successfully");
            } catch (IOException e) {
                log.error("Exception Occurred", e);
            }
        }
    }

    public void addToLayeringDetails(String name) {
        try {
            writer = new FileWriter("src/configuration/LayerDetails.txt", true);
            PrintWriter printWriter = new PrintWriter(writer);
            printWriter.println(name+"="+(fetchMaxLayerID()+1));
            printWriter.flush();
            printWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int fetchMaxLayerID() {
        FileReader reader = null;
        int layerIDMax = 0;
        try {
            reader = new FileReader("src/configuration/LayerDetails.txt");
            Properties properties = new Properties();
            properties.load(reader);
            layerIDMax = Integer.parseInt(properties.getProperty("Heightens"));
            return layerIDMax;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return layerIDMax;
    }


    public static void main(String[] args) {
        B4_Layer b4_layer = new B4_Layer();
        b4_layer.addToLayeringDetails("UmeshLayer");
        System.out.println(b4_layer.fetchMaxLayerID());
    }
}
