package com.ehelpy.brihaspati4.routingManagerAPI;

import java.security.PublicKey;

/**
 * This class will is used to create an Object which contains tuple(Three parameters)- NodeID,PublicKey and HashID
 * This class is used to create another object called B4_Node.
 */
public class B4_NodeTuple {
    private final String nodeID;
    private final PublicKey publicKey;
    private final String hashID;

    /**
     * @param nodeID - nodeID is taken as the argument.
     * @param publicKey - public is taken as the argument.
     * @param hashID - hashID is taken as the argument.
     * This is a constructor of B4_NodeTuple.This constructor override the default constructor.
     */
    public B4_NodeTuple(String nodeID, PublicKey publicKey,String hashID) {
        this.nodeID = nodeID;
        this.publicKey = publicKey;
        this.hashID=hashID;
    }

    /**
     * @return - nodeID associated with the current node.
     */
    public String getNodeID() {
        return nodeID;
    }

    /**
     * @return - publicKey associated with the current node.
     */
    public PublicKey getPublicKey() {
        return publicKey;
    }

    /**
     * @return - hashID associated with the current node.
     */
    public String getHashID(){return hashID;}

}
