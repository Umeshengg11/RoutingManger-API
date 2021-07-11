package com.ehelpy.brihaspati4.routingManagerAPI;

/**
 * Developed by S/L Umesh Nair
 * This class hold all the information about the node - Node ID, Node IP, Node Port,Node Transport and RTT.
 * The routingTable and neighbourTable will be created using the object of this class and NodeTuple class.
 * Two constructor of this class is created - one without rtt value and other with rtt value.
 */
public class B4_Node {
    private final B4_NodeTuple b4node;
    private final String ipAddress;
    private final String portAddress;
    private final String transport;
    private float rtt;

    /**
     * @param b4node -Its a B4_NodeTuple Object
     * @param ipAddress - IpAddress of the node
     * @param portAddress - PortAddress of the node
     * @param transport - TransportAddress of the node
     */
    public B4_Node(B4_NodeTuple b4node, String ipAddress, String portAddress, String transport) {
        this.b4node = b4node;
        this.ipAddress = ipAddress;
        this.portAddress = portAddress;
        this.transport = transport;
    }

    /**
     * @param b4node -Its a B4_NodeTuple Object
     * @param ipAddress- IpAddress of the node
     * @param portAddress - PortAddress of the node
     * @param transport - TransportAddress of the node
     * @param rtt - rtt value of the particular node form the selfNode
     */
    public B4_Node(B4_NodeTuple b4node, String ipAddress, String portAddress, String transport, float rtt) {
        this.b4node = b4node;
        this.ipAddress = ipAddress;
        this.portAddress = portAddress;
        this.transport = transport;
        this.rtt = rtt;
    }

    /**
     * @return - rtt value associated with the node
     */
   public float getRtt() {
        return rtt;
    }

    /**
     * @return - Object of B4_NodeTuple
     */
   public B4_NodeTuple getB4node() {
        return b4node;
    }

    /**
     * @return - IP address associated with the node
     */
   public String getIpAddress() {
        return ipAddress;
    }

    /**
     * @return - PortAddress associated with the node
     */
   public String getPortAddress() {
        return portAddress;
    }

    /**
     * @return - Transport address associated with the node
     */
   public String getTransport() {
        return transport;
    }
}
