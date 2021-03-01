package main;

import org.apache.log4j.Logger;

import java.io.*;
import java.util.Properties;

/**
 * This class is used to get all the data from the configuration file ie config.properties
 */
class ConfigData {
    private static final Logger log = Logger.getLogger(ConfigData.class);
    private static ConfigData config;
    private final NodeCryptography nodeCryptography;
    private final String path = "src/configuration/config.properties";
    private long sleepTime;
    private B4_Node bootStrapNode;
    private FileReader reader;
    private Properties properties;

    private ConfigData() {
        nodeCryptography = NodeCryptography.getInstance();
        try {
            reader = new FileReader(path);
            properties = new Properties();
        } catch (FileNotFoundException e) {
            log.error("Exception Occurred", e);
        }
    }

    public static synchronized ConfigData getInstance() {
        if (config == null) {
            config = new ConfigData();
        }
        return config;
    }

    private int servicesInt(String key) {
        int length = 0;
        try {
            properties.load(reader);
            String value = properties.getProperty(key);
            length = Integer.parseInt(value);

        } catch (IOException e) {
            log.error("RT_length parameter not found in config file\n", e);
        }
        return length;
    }

    private String servicesString(String key) {
        String name = null;
        try {
            properties.load(reader);
            name = properties.getProperty(key);

        } catch (IOException e) {
            log.error("RT_length parameter not found in config file\n", e);
        }
        return name;
    }

    private long servicesLong(String key) {
        long time = 0;
        try {
            properties.load(reader);
            String slp_time = properties.getProperty(key);
            sleepTime = Long.parseLong(slp_time);

        } catch (IOException e) {
            log.error("Config file not Found or Issue in config file fetching\n", e);
        }
        return time;
    }

    private void serviceBootStrap() {
        try {
            properties.load(reader);
            String bootStrapID = properties.getProperty("BootstrapND");
            String bootStrapPub = properties.getProperty("BootstrapPubKey");
            String bootStrapHash = properties.getProperty("BootstrapHashID");
            String bootStrapDigitalSign = properties.getProperty("BootstrapDigitalSign");
            String bootStrapIP = properties.getProperty("BootstrapPvtIP");
            String bootStrapPort = properties.getProperty("BootstrapPort");
            String bootStrapAddress = properties.getProperty("BootstrapAddress");
            bootStrapNode = new B4_Node(new B4_NodeTuple(bootStrapID, nodeCryptography.strToPub(bootStrapPub), bootStrapHash, bootStrapDigitalSign), bootStrapIP, bootStrapPort, bootStrapAddress);
        } catch (IOException e) {
            log.error("Config file not Found or Issue in config file fetching\n", e);
        }
    }

    private boolean serviceAccess(String serviceName) {
        boolean access = false;
        try {
            properties.load(reader);
            String value = properties.getProperty(serviceName);
            if (value.contentEquals("yes")) access = true;

        } catch (IOException e) {
            log.error("Service Not found in Config file\n", e);
        }
        return access;
    }

    long getSleepTime() {
        sleepTime = servicesLong("Sleep_time");
        return sleepTime;
    }

    int getPortAddress() {
        int portAddress = servicesInt("PortAddress");
        return portAddress;
    }

    String getTransportAddress() {
        return servicesString("TransportAddress");
    }

    long getIncrementTime() {
        return servicesLong("Increment_time");
    }

    int getNeighbourTableLength() {
        return servicesInt("NT_length");
    }

    int getRoutingTableLength() {
        return servicesInt("RT_length");
    }

    boolean isLayerAccess(String layerName) {
        return serviceAccess(layerName);
    }

    B4_Node getBootStrapNode() {
        serviceBootStrap();
        return bootStrapNode;
    }

    void addToConfigFile(String layerName) {
        try {
            FileWriter writer = new FileWriter(path, true);
            PrintWriter printWriter = new PrintWriter(writer);
            printWriter.println(layerName + "=yes");
            printWriter.flush();
            printWriter.close();
            log.debug("Config.properties File updated successfully");
        } catch (IOException e) {
            log.error("Exception Occurred", e);
        }
    }

    boolean checkLayerName(String layerName) {
        boolean access = false;
        try {
            properties.load(reader);
            String value = properties.getProperty(layerName);
            if (value.equalsIgnoreCase("yes") || (value.equalsIgnoreCase("no"))) access = true;

        } catch (IOException e) {
            return false;
        }
        return access;
    }
}
