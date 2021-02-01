/**
 * Created by S/L Umesh Nair
 * This class hold all the information about the node - Node ID, Node IP, Node Port,Node Transport and rtt.
 * The routingTable and neighbourTable will be created using the object of this class and NodeTuple class.
 * Two constructor of this class is created - one without rtt value and other with rtt value.
 */
public class B4_Node {
    private final B4_NodeTuple b4node;
    private final String ipAddress;
    private final String portAddress;
    private final String transport;
    private float rtt;

    public B4_Node(B4_NodeTuple b4node, String ipAddress, String portAddress, String transport) {
        this.b4node = b4node;
        this.ipAddress = ipAddress;
        this.portAddress = portAddress;
        this.transport = transport;
    }

    public B4_Node(B4_NodeTuple b4node, String ipAddress, String portAddress, String transport, float rtt) {
        this.b4node = b4node;
        this.ipAddress = ipAddress;
        this.portAddress = portAddress;
        this.transport = transport;
        this.rtt = rtt;
    }

    public float getRtt() {
        return rtt;
    }

    public B4_NodeTuple getB4node() {
        return b4node;
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
