import java.security.PublicKey;

public class B4_NodeTupple {
    private final String nodeID;
    private final PublicKey publicKey;
    private final String hashID;

    public B4_NodeTupple(String nodeID, PublicKey publicKey, String hashID) {
        this.nodeID = nodeID;
        this.publicKey = publicKey;
        this.hashID = hashID;
    }

    public String getNodeID() {
        return nodeID;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public String getHashID() {
        return hashID;
    }

}
