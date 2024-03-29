package com.ehelpy.brihaspati4.routingManagerAPI;

import org.apache.log4j.Logger;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Base64;

/**
 * This class is used to generate NodeID from the public key generated using the NodeCryptography class.
 * It also generate hashID which is generated by signing NodeID with the private key of the current node.
 * hashID generated can be verified using the verifySignature method.This method can access from the main class ie RoutingManager.
 */
class B4_NodeGeneration {
    private static final Logger log = Logger.getLogger(B4_NodeGeneration.class);
    private static B4_NodeGeneration b4_nodeGeneration;
    private String nodeID;
    private PublicKey publicKey;
    private PrivateKey privateKey;
    private NodeCryptography nodeCryptography;
    private byte[] signatureData;
    private String hashID;

    /**
     * It is the default constructor of the class
     * Few functions are initialised/fetched using this constructor are NodeCryptography,publicKey,nodeID, SignatureDate,hashID
     */
    private B4_NodeGeneration() {
    }

    public static synchronized B4_NodeGeneration getInstance() {
        if (b4_nodeGeneration == null) {
            b4_nodeGeneration = new B4_NodeGeneration();
        }
        return b4_nodeGeneration;
    }

    void initiateNodeGenerationProcess() {
        nodeCryptography = NodeCryptography.getInstance();
        publicKey = nodeCryptography.getPublicKey();
        nodeID = generateNodeId();
        signatureData = signNodeIdUsingPvtKey();
        hashID = generateHashID();
    }

    /**
     * @return - It will generate nodeID for the Node.
     * Return type is String
     */
    private String generateNodeId() {
        String node1ID = null;
        StringBuilder publicKeyToString = new StringBuilder();
        for (byte bytes : publicKey.getEncoded()) {
            publicKeyToString.append(String.format("%02x", bytes).toUpperCase());
        }
        byte[] messageByte = publicKeyToString.toString().getBytes(StandardCharsets.UTF_8);
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
            messageDigest.update(messageByte);
            byte[] digest = messageDigest.digest();

            // Converting byte[] to HexString format
            StringBuilder hexString = new StringBuilder();
            for (byte bytes : digest) {
                hexString.append(String.format("%02x", bytes).toUpperCase());
            }
            node1ID = hexString.toString();
            log.info("Node ID is generated Successfully");
            log.info("Node ID -" + node1ID);
        } catch (NoSuchAlgorithmException e) {
            log.error("Exception Occurred", e);
        }
        return node1ID;
    }

    /**
     * This function is used to sign the nodeID using privateKey to generate a digital signature
     *
     * @return - the sign date in the form of byte[]
     */
    private byte[] signNodeIdUsingPvtKey() {
        byte[] data = getNodeID().getBytes(StandardCharsets.UTF_8);
        Signature signature;
        byte[] sigData = null;
        try {
            signature = Signature.getInstance("SHA1WithRSA");
            signature.initSign(nodeCryptography.getFromKeyStore());
            signature.update(data);
            sigData = signature.sign();
            log.info("NodeID is signed using Private Key");
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            log.error("Exception Occurred", e);
        }
        return sigData;
    }

    /**
     * This function will generate hashID from signatureData
     *
     * @return - hashID
     */
    private String generateHashID() {
        byte[] base1 = Base64.getEncoder().encode(signatureData);
        return new String(base1);
    }

    /**
     * This function is used to verify whether the node is generated randomly from the publicKey and is signed by the
     * corresponding private key to generate the hashID.
     *
     * @param hashID    - hashID associated with the node.
     * @param publicKey - public key associated with the node
     * @param nodeID    - nodeID associated with the node
     * @return - boolean value if the signature is verified
     */
    boolean verifySignature(String hashID, PublicKey publicKey, String nodeID) {
        boolean verify = false;
        byte[] baseSign = Base64.getDecoder().decode(hashID);
        byte[] data = nodeID.getBytes(StandardCharsets.UTF_8);
        try {
            Signature signature = Signature.getInstance("SHA1WithRSA");
            signature.initVerify(publicKey);
            signature.update(data);
            verify = signature.verify(baseSign);
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            log.error("Exception Occurred", e);
        }
        return verify;
    }

    /**
     * @return - NodeID associate with the node.
     */
    String getNodeID() {
        return nodeID;
    }

    public void setNodeID(String nodeID) {
        this.nodeID = nodeID;
    }

    /**
     * @return - HashID associated with the node.
     */
    String getHashID() {
        return hashID;
    }

    public void setHashID(String hashID) {
        this.hashID = hashID;
    }

    /**
     * @return - publicKey associated with the node.
     */
    PublicKey getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(PublicKey publicKey) {
        this.publicKey = publicKey;
    }

    void newNodeGenProcess() {
        nodeCryptography = NodeCryptography.getInstance();
        publicKey = nodeCryptography.getPublicKey();
        privateKey = nodeCryptography.getFromKeyStore();
        nodeID = generateNodeId();
        signatureData = signNodeIdUsingPvtKey();
        hashID = generateHashID();
    }

    public void setPrivateKey(PrivateKey privateKey) {
        this.privateKey = privateKey;
    }

    public void setSignatureData(byte[] signatureData) {
        this.signatureData = signatureData;
    }

}