package com.ehelpy.brihaspati4.routingManagerAPI;

import org.apache.log4j.Logger;

import java.io.*;
import java.util.Properties;

/**
 * This class is used to fetch all the data from the configuration file i.e config.properties
 */
class ConfigData {
    private static final Logger log = Logger.getLogger(ConfigData.class);
    private static ConfigData config;
    private static Utility utility;
    private final String path = "configuration/config.properties";
    private final String path1 = "configuration";
    private final String path2 = "LogFiles";
    private B4_Node bootStrapNode;
    private FileReader reader;
    private Properties properties;

    /**
     * This is the default constructor for this class
     * This is made private so that it cannot be accessed directly from any where.
     */
    private ConfigData() {
        File file = new File(path1);
        boolean isDirExist = file.exists();
        if (!isDirExist) {
            boolean isDirCreated = createConfigDir(path1);
            if (!isDirCreated) log.error("Configuration Directory is not created");
            else log.info("Configuration directory is created");
        }
        File logfile = new File(path2);
        boolean isDirLogExist = logfile.exists();
        if (!isDirLogExist) {
            boolean isDirLogCreated = createConfigDir(path2);
            if (!isDirLogCreated) log.error("LogFile Directory is not created");
            else log.info("LogFile directory is created");
        }
        utility = new Utility();
        try {
            boolean configFileExist;
            File configFile = new File(path);
            configFileExist = configFile.exists();
            if (configFileExist) {
                properties = new Properties();
            } else {
                generateDefaultConfigFile();
            }
            reader = new FileReader(path);
        } catch (FileNotFoundException e) {
            log.error("FileNotFoundException", e);
        }
    }

    /**
     * @return - object of ConfigData
     * This is to ensure same instance of object is provided whenever this class is accessed.
     * Hence it is made singleton.
     */
    static synchronized ConfigData getInstance() {
        if (config == null) {
            config = new ConfigData();
        }
        return config;
    }

    private int servicesInt(String key) throws IOException {
        int length;
        properties.load(reader);
        String value = properties.getProperty(key);
        length = Integer.parseInt(value);
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
            time = Long.parseLong(slp_time);

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
            String bootStrapIP = properties.getProperty("BootstrapPvtIP");
            String bootStrapPort = properties.getProperty("BootstrapPort");
            String bootStrapAddress = properties.getProperty("BootstrapAddress");
            bootStrapNode = new B4_Node(new B4_NodeTuple(bootStrapID, utility.strToPub(bootStrapPub), bootStrapHash), bootStrapIP, bootStrapPort, bootStrapAddress);
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
        return servicesLong("Sleep_time");
    }

    int getPortAddress() {
        int portAddress = 0;
        try {
            portAddress = servicesInt("PortAddress");
        } catch (IOException e) {
            log.error("PortAddress parameter not found in config file\n", e);
        }
        return portAddress;
    }

    String getTransportAddress() {
        return servicesString("TransportAddress");
    }

    long getIncrementTime() {
        return servicesLong("Increment_time");
    }

    int getNeighbourTableLength() {
        int ntLength = 0;
        try {
            ntLength = servicesInt("NT_length");
        } catch (IOException e) {
            log.error("NeighbourTable Length parameter not found in config file\n", e);
        }
        return ntLength;
    }

    int getRoutingTableLength() {
        int rtLength = 0;
        try {
            rtLength = servicesInt("RT_length");
        } catch (IOException e) {
            log.error("RT Length parameter not found in config file\n", e);
        }
        return rtLength;
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
        boolean access =false;
        try {
            properties.load(reader);
            String value = properties.getProperty(layerName);
            if (value==null) return false;
            else if (value.equalsIgnoreCase("yes") || (value.equalsIgnoreCase("no"))) access = true;
        } catch (IOException e) {
            log.error("Exception Occurred", e);
            return false;
        }
        return access;
    }

    String getValue(String fileName) {
        String value = null;
        try {
            properties.load(reader);
            value = properties.getProperty(fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return value;
    }

    int getPurgeLoopCount() {
        int purgeCount = 0;
        try {
            purgeCount = servicesInt("PurgeLoopCount");
        } catch (IOException e) {
            log.error("Purge Loop Count parameter not found in config file\n", e);
        }
        return purgeCount;
    }

    void generateDefaultConfigFile() {
        properties = new Properties();
        properties.setProperty("BootstrapND", "ED38EE69F98BDF529CC05E34A19D04647A487B71");
        properties.setProperty("BootstrapPubKey", "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDJtY6SHzQ1fIpQxflwvbQpNfOS9ul0XJBHq42vFafTaVNZaTi9f1YZfybvGY67zPw2jmrW/jx89HnyMfzld/nSDzS8xRltjzIqxck+Xg5n0aWTTyt0zv/uO6pVDiecgJ9j46YsevJ/zf85hC3fL/jl6nhLdV0zaHyZGcCms/J1JQIDAQAB");
        properties.setProperty("BootstrapHashID", "E5B9ABAA1234ABA1234591111ABCDFE1234567897589ABAA1234ABA1234591111ABCDFE123456789");
        properties.setProperty("LayerDetailsPath", "configuration/LayerDetails.txt");
        properties.setProperty("NodeDetailsPath", "configuration/NodeDetails.txt");
        properties.setProperty("PurgeLoopCount", "4");
        properties.setProperty("BootstrapPvtIP", "172.20.160.56");
        properties.setProperty("BootstrapPort", "1022");
        properties.setProperty("BootstrapAddress", "TCP");
        properties.setProperty("PortAddress", "1024");
        properties.setProperty("TransportAddress", "TCP");
        properties.setProperty("RT_length", "40");
        properties.setProperty("NT_length", "16");
        properties.setProperty("Increment_time", "30000");
        properties.setProperty("Sleep_time", "30000");
        properties.setProperty("BaseRoutingTable", "yes");
        properties.setProperty("StorageRoutingTable", "yes");
        properties.setProperty("Email", "umesh@iitk.ac.in");
        properties.setProperty("OrgUnit", "IITK");
        properties.setProperty("Organisation", "IITK");
        properties.setProperty("City", "Kanpur");
        properties.setProperty("State", "UP");
        properties.setProperty("Country", "India");
        try {
            FileOutputStream outputStream = new FileOutputStream(path);
            properties.store(outputStream, "RoutingManager Configuration File");
            log.info("Default configuration file is created");
        } catch (IOException e) {
            log.error("Configuration file cannot be created, check error log", e);
        }
    }

    private boolean createConfigDir(String location) {
        boolean isDirCreated;
        File file = new File(location);
        isDirCreated = file.mkdir();
        return isDirCreated;
    }

    public boolean addLastLogOutToConfigFile(String logoutTime) {
        boolean isAdded = false;
        FileWriter writer;
        PrintWriter printWriter;
        try {
            properties.load(reader);
            if (properties.getProperty("LastLogoutTime") != null) {
                writer = new FileWriter(path);
                printWriter = new PrintWriter(writer);
                properties.remove("LastLogoutTime");
                properties.setProperty("LastLogoutTime",logoutTime);
                properties.store(printWriter, "RoutingManager Configuration File");
            } else {
                writer = new FileWriter(path, true);
                printWriter = new PrintWriter(writer);
                printWriter.println("LastLogoutTime=" + logoutTime);
            }
            printWriter.flush();
            printWriter.close();
            isAdded = true;
            log.debug("LastLogoutTime added to config.properties");
        } catch (IOException e) {
            log.error("Exception Occurred", e);
        }
        return isAdded;
    }
}
