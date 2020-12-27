import java.nio.charset.StandardCharsets;
import java.security.*;

public class B4_NodeGeneration {
    private String nodeID;
    private final PublicKey publicKey;
    private String hashID;
    private final NodeCryptography nodeCryptography;
    private byte[] signatureData;
    private static B4_NodeGeneration b4_nodeGeneration;

    private B4_NodeGeneration() {
        nodeCryptography = NodeCryptography.getInstance();
        publicKey = nodeCryptography.getPublicKey();
        generateNodeId();
        signNodeId();
    }

    public static synchronized B4_NodeGeneration getInstance() {
        if (b4_nodeGeneration == null) {
            b4_nodeGeneration = new B4_NodeGeneration();
        }
        return b4_nodeGeneration;
    }

    private void generateNodeId() {
        StringBuilder publicKeyToString = new StringBuilder();
        for (byte bytes : publicKey.getEncoded()) {
            publicKeyToString.append(String.format("%02x", bytes).toUpperCase());
        }
        //System.out.print(publicKeyToString);
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
            //System.out.println(hexString);
            nodeID = hexString.toString();
            //System.out.println("nodeID"+nodeID);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    private void signNodeId() {
        StringBuilder signData = new StringBuilder();
        byte[] data = getNodeID().getBytes(StandardCharsets.UTF_8);
        try {
            Signature signature = Signature.getInstance("SHA1WithRSA");
            signature.initSign(getFromKeyStore());
            signature.update(data);
            signatureData = signature.sign();

            for (byte bytes : signatureData) {
                signData.append(String.format("%02x", bytes).toUpperCase());
            }
            hashID = signData.toString();

        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            e.printStackTrace();
        }
    }

    private boolean verifySignature() {
        boolean verify = false;
        byte[] data = getNodeID().getBytes(StandardCharsets.UTF_8);
        try {
            Signature signature = Signature.getInstance("SHA1WithRSA");
            signature.initVerify(publicKey);
            signature.update(data);
            verify = signature.verify(signatureData);
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            e.printStackTrace();
        }
        return verify;
    }

    private PrivateKey getFromKeyStore() {
        char[] keyPassword = "123@abc".toCharArray();
        KeyStore.ProtectionParameter protectionParameter = new KeyStore.PasswordProtection(keyPassword);
        KeyStore.PrivateKeyEntry privateKeyEntry = null;
        try {
            privateKeyEntry = (KeyStore.PrivateKeyEntry) nodeCryptography.getKeyStore().getEntry("Private Key", protectionParameter);
        } catch (NoSuchAlgorithmException | KeyStoreException | UnrecoverableEntryException e) {
            e.printStackTrace();
        }
        assert privateKeyEntry != null;
        return privateKeyEntry.getPrivateKey();
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