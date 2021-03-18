package TestingModule;

import main.B4_NodeTuple;

/**
 * This is simulation class used for testing purpose.
 * This class hold all the information about the node - Node ID, Node IP, Node Port,Node Transport and rtt.
 * The routingTable and neighbourTable will be created using the object of this class and NodeTuple class.
 * Two constructor of this class is created - one without rtt value and other with rtt value.
 */
class B4_NodeSimulator {
    private final B4_NodeTuple b4node;
    private final String ipAddress;
    private final String portAddress;
    private final String transport;
    private float rtt;

    /**
     * @param b4node -Its a main.resources.B4_NodeTuple Object
     * @param ipAddress - IpAddress of the node
     * @param portAddress - PortAddress of the node
     * @param transport - TransportAddress of the node
     */
    B4_NodeSimulator(B4_NodeTuple b4node, String ipAddress, String portAddress, String transport) {
        this.b4node = b4node;
        this.ipAddress = ipAddress;
        this.portAddress = portAddress;
        this.transport = transport;
    }

    /**
     * @param b4node -Its a main.resources.B4_NodeTuple Object
     * @param ipAddress- IpAddress of the node
     * @param portAddress - PortAddress of the node
     * @param transport - TransportAddress of the node
     * @param rtt - rtt value of the particular node form the selfNode
     */
    B4_NodeSimulator(B4_NodeTuple b4node, String ipAddress, String portAddress, String transport, float rtt) {
        this.b4node = b4node;
        this.ipAddress = ipAddress;
        this.portAddress = portAddress;
        this.transport = transport;
        this.rtt = rtt;
    }

    float getRtt() {
        return rtt;
    }

    B4_NodeTuple getB4node() {
        return b4node;
    }

    String getIpAddress() {
        return ipAddress;
    }

    String getPortAddress() {
        return portAddress;
    }

    String getTransport() {
        return transport;
    }
}
