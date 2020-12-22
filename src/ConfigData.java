import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

public class ConfigData {
   private int neighbourTableLength;
   private int routingTableLength;
   private long sleepTime;
   private long incrementTime;
   private B4_Node bootStrapNode;
   private static ConfigData config;

    private ConfigData(){};

    public static synchronized ConfigData getInstance() {
        if (config == null) {
            config = new ConfigData();
        }
        return config;
    }

    public int getNeighbourTableLength() {
        neighbourTableLength = servicesInt("NT_length");
        return neighbourTableLength;
    }

    public int getRoutingTableLength() {
        routingTableLength = servicesInt("RT_length");
        return routingTableLength;
    }

    public long getSleepTime() {
        sleepTime = servicesLong("Sleep_time");
        return sleepTime;
    }

    public long getIncrementTime() {
        incrementTime = servicesLong("Increment_time");
        return incrementTime;
    }

    public B4_Node getBootStrapNode() {
        serviceBootStrap();
        return bootStrapNode;
    }

    private int servicesInt(String key) {
        int length =0;
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

    private long servicesLong (String key) {
        long time =0;
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
            String bootStrapIP = properties.getProperty("BootstrapPvtIP");
            String bootStrapPort = properties.getProperty("BootstrapPort");
            String bootStrapAddress = properties.getProperty("BootstrapAddress");
            bootStrapNode = new B4_Node(bootStrapID, bootStrapIP, bootStrapPort, bootStrapAddress);
        } catch (IOException e) {
            System.out.println("Config file not Found or Issue in config file fetching");
        }
    }
}
