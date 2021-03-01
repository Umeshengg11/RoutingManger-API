package main;

import org.apache.log4j.Logger;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class B4_Layer {
    private static final Logger log = Logger.getLogger(B4_Layer.class);
    private FileWriter writer;
    private PrintWriter printWriter;
    private FileReader reader;
    private final String filePath = "src/configuration/LayerDetails.txt";

    public B4_Layer() {
        File layerFile = new File(filePath);
        boolean layerDetailsExists = layerFile.exists();
        if (!layerDetailsExists) {
            try {
                writer = new FileWriter(filePath);
                printWriter = new PrintWriter(writer);
                printWriter.println("#  Layering Details  #");
                printWriter.println("..................................");
                printWriter.println("Heightens=3");
                printWriter.println("0=BaseRoutingTable");
                printWriter.println("1=StorageRoutingTable");
                printWriter.println("2=MessageRoutingTable");
                printWriter.println("3=VoipRoutingTable");
                printWriter.flush();
                printWriter.close();
                log.debug("LayerDetails File created successfully");
            } catch (IOException e) {
                log.error("Exception Occurred", e);
            }
        }
    }

    public boolean addToLayeringDetailsFile(String name) {
        boolean isCreated = false;
        try {
            String layerName;
            int layerId = fetchMaxLayerID();
            writer = new FileWriter(filePath, true);
            printWriter = new PrintWriter(writer);
            for (int i = 0; i <= layerId; i++) {
                    reader = new FileReader(filePath);
                    Properties properties = new Properties();
                    properties.load(reader);
                    layerName = properties.getProperty(""+i+"");
                    if(layerName.equalsIgnoreCase(name)){
                        break;
                    }
                    if(i==layerId){
                        printWriter.println((layerId + 1) + "=" + name);
                        printWriter.flush();
                        printWriter.close();
                        isCreated=true;
                    }
            }
        } catch (IOException e) {
            log.error("Exception Occurred", e);
        }
        return isCreated;
    }

    int fetchMaxLayerID() {
        FileReader reader;
        int layerIDMax = 0;
        try {
            reader = new FileReader(filePath);
            Properties properties = new Properties();
            properties.load(reader);
            layerIDMax = Integer.parseInt(properties.getProperty("Heightens"));
            return layerIDMax;
        } catch (IOException e) {
            log.error("Exception Occurred", e);
        }
        return layerIDMax;
    }

    boolean amendLayerFile() {
        boolean isAmended = false;
        List<String> lines = new ArrayList<>();
        String line;
        int num = fetchMaxLayerID();
        try {
            File f1 = new File(filePath);
            FileReader fr = new FileReader(f1);
            BufferedReader br = new BufferedReader(fr);
            while ((line = br.readLine()) != null) {
                if (line.contains("Heightens"))
                    line = "Heightens=" + (num + 1);
                lines.add(line);
            }
            fr.close();
            br.close();

            writer = new FileWriter(f1);
            printWriter = new PrintWriter(writer);
            for (String s : lines)
                printWriter.println(s);
            printWriter.flush();
            printWriter.close();
            isAmended = true;
        } catch (Exception e) {
            log.error("Exception Occurred", e);
        }
        return isAmended;
    }

    String getLayerName(int layerID){
        String layerName = null;
        try {
            reader = new FileReader(filePath);
            Properties properties = new Properties();
            properties.load(reader);
            layerName = properties.getProperty(""+layerID+"");
        } catch (IOException e) {
            log.error("Exception Occurred", e);
        }
    return layerName;
    }

}
