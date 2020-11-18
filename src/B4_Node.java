/**
 * Created by S/L Umesh Nair
 * This class hold all the information about a node like its Node ID, Node IP, Node Port,Node Transport
 * The routingTable created will be array of this class (ie Nodes)
 */
public class B4_Node {
    private final String nodeID;
    private final String ipAddress;
    private final String portAddress;
    private final String transport;
    private float rtt;

    public B4_Node(String nodeID, String ipAddress, String portAddress, String transport) {
        this.nodeID = nodeID;
        this.ipAddress = ipAddress;
        this.portAddress = portAddress;
        this.transport = transport;
    }

    public B4_Node(String nodeID, String ipAddress, String portAddress, String transport, float rtt) {
        this.nodeID = nodeID;
        this.ipAddress = ipAddress;
        this.portAddress = portAddress;
        this.transport = transport;
        this.rtt = rtt;
    }

    public float getRtt() {
        return rtt;
    }

    public String getNodeID() {
        return nodeID;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getPortAddress() {
        return portAddress;
    }

    public String getTransport() {
        return transport;
    }
}
