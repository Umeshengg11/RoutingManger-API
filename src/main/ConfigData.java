package main;

import org.apache.log4j.Logger;

import java.io.*;
import java.util.Properties;

/**
 * This class is used to get all the data from the configuration file ie config.properties
 */
class ConfigData {
    private static ConfigData config;
    private long sleepTime;
    private B4_Node bootStrapNode;
    private final NodeCryptography nodeCryptography;
    private static final Logger log = Logger.getLogger(ConfigData.class);
    private FileReader reader;
    private Properties properties;

    private ConfigData() {
        nodeCryptography = NodeCryptography.getInstance();
        try {
            reader = new FileReader("src/configuration/config.properties");
            properties = new Properties();
        } catch (FileNotFoundException e) {
           log.error("Exception Occurred",e);
        }
    }

    public static synchronized ConfigData getInstance() {
        if (config == null) {
            config = new ConfigData();
        }
        return config;
    }

    int getNeighbourTableLength() {
        return servicesInt("NT_length");
    }

    int getRoutingTableLength() {
        return servicesInt("RT_length");
    }

    long getSleepTime() {
        sleepTime = servicesLong("Sleep_time");
        return sleepTime;
    }

    long getIncrementTime() {
        return servicesLong("Increment_time");
    }

    B4_Node getBootStrapNode() {
        serviceBootStrap();
        return bootStrapNode;
    }

    boolean isLayerAccess(String layerName) {
        return serviceAccess(layerName);
    }

    private int servicesInt(String key) {
        int length = 0;
        try {
            properties.load(reader);
            String value = properties.getProperty(key);
            length = Integer.parseInt(value);

        } catch (IOException e) {
            log.error("RT_length parameter not found in config file\n",e);
        }
        return length;
    }

    private long servicesLong(String key) {
        long time = 0;
        try {
            properties.load(reader);
            String slp_time = properties.getProperty(key);
            sleepTime = Long.parseLong(slp_time);

        } catch (IOException e) {
            log.error("Config file not Found or Issue in config file fetching\n",e);
        }
        return time;
    }

    private void serviceBootStrap() {
        try {
            properties.load(reader);
            String bootStrapID = properties.getProperty("BootstrapND");
            String bootStrapPub = properties.getProperty("BootstrapPubKey");
            String bootStrapHash = properties.getProperty("BootstrapHashID");
            String bootStrapIP = properties.getProperty("BootstrapPvtIP");
            String bootStrapPort = properties.getProperty("BootstrapPort");
            String bootStrapAddress = properties.getProperty("BootstrapAddress");
            bootStrapNode = new B4_Node(new B4_NodeTuple(bootStrapID,nodeCryptography.strToPub(bootStrapPub),bootStrapHash), bootStrapIP, bootStrapPort, bootStrapAddress);
        } catch (IOException e) {
            log.error("Config file not Found or Issue in config file fetching\n",e);
        }
    }

    private boolean serviceAccess(String serviceName) {
        boolean access = false;
        try {
            properties.load(reader);
            String value = properties.getProperty(serviceName);
            if (value.contentEquals("yes")) access = true;

        } catch (IOException e) {
            log.error("Service Not found in Config file\n",e);
        }
        return access;
    }

    boolean addToConfigFile(String layerName) {
        boolean isAdded = false;
        try {
            FileWriter writer = new FileWriter("src/configuration/config.properties", true);
            PrintWriter printWriter = new PrintWriter(writer);
            printWriter.println(layerName + "=yes");
            printWriter.flush();
            printWriter.close();
            log.debug("Config.properties File updated successfully");
            isAdded = true;
        } catch (IOException e) {
            log.error("Exception Occurred", e);
        }
        return isAdded;
    }
}
