import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

/**
 * This class is used to get all the data from the configuration file ie config.properties
 */
class ConfigData {
    private static ConfigData config;
    private long sleepTime;
    private B4_Node bootStrapNode;
    private final NodeCryptography nodeCryptography;

    private ConfigData() {
        nodeCryptography = NodeCryptography.getInstance();
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
        FileReader reader;
        try {
            reader = new FileReader("config.properties");
            Properties properties = new Properties();
            properties.load(reader);
            String value = properties.getProperty(key);
            length = Integer.parseInt(value);

        } catch (IOException e) {
            System.out.println("RT_length parameter not found in config file\n" + e);
        }
        return length;
    }

    private long servicesLong(String key) {
        long time = 0;
        try {
            FileReader reader = new FileReader("config.properties");
            Properties properties = new Properties();
            properties.load(reader);
            String slp_time = properties.getProperty(key);
            sleepTime = Long.parseLong(slp_time);

        } catch (IOException e) {
            System.out.println("Config file not Found or Issue in config file fetching");
        }
        return time;
    }

    private void serviceBootStrap() {
        try {
            FileReader reader = new FileReader("config.properties");
            Properties properties = new Properties();
            properties.load(reader);
            String bootStrapID = properties.getProperty("BootstrapND");
            String bootStrapPub = properties.getProperty("BootstrapPubKey");
            String bootStrapHash = properties.getProperty("BootstrapHashID");
            String bootStrapIP = properties.getProperty("BootstrapPvtIP");
            String bootStrapPort = properties.getProperty("BootstrapPort");
            String bootStrapAddress = properties.getProperty("BootstrapAddress");
            bootStrapNode = new B4_Node(new B4_NodeTuple(bootStrapID,nodeCryptography.strToPub(bootStrapPub),bootStrapHash), bootStrapIP, bootStrapPort, bootStrapAddress);
        } catch (IOException e) {
            System.out.println("Config file not Found or Issue in config file fetching");
        }
    }

    private boolean serviceAccess(String serviceName) {
        boolean access = false;
        FileReader reader;
        try {
            reader = new FileReader("config.properties");
            Properties properties = new Properties();
            properties.load(reader);
            String value = properties.getProperty(serviceName);
            if (value.contentEquals("yes")) access = true;

        } catch (IOException e) {
            System.out.println("Service Not found in Config file\n" + e);
        }
        return access;
    }
}
