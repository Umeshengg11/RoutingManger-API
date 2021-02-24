package main;

import org.apache.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.x509.X509V3CertificateGenerator;

import javax.security.auth.x500.X500Principal;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;

/**
 * This class deals with all the cryptographic work associated with the Node like
 * generating public and private key , generation of keystore, generation of certificate for the node
 * putting private key and certificate in the keystore, retrieval of private key from the keystore
 */
class NodeCryptography {
    private PublicKey publicKey;
    private PrivateKey privateKey;
    private KeyStore keyStore;
    private static NodeCryptography nodeCryptography;
    private static final Logger log = Logger.getLogger(NodeCryptography.class);
    private final String filePath = "src/configuration/NodeDetails.txt";

     private NodeCryptography() {
        Provider provider = new BouncyCastleProvider();
        Security.addProvider(provider);
         keyStoreCreation();
         File nodeFile = new File(filePath);
         boolean nodeDetailsExists = nodeFile.exists();
         if (!nodeDetailsExists){
             keyPairGeneration();
             generateCertificate();
             savePrivateKeyToKeyStore();
         } else {

         }
    }

    public static synchronized NodeCryptography getInstance() {
        if (nodeCryptography == null) {
            nodeCryptography = new NodeCryptography();
        }
        return nodeCryptography;
    }

    PublicKey getPublicKey() {
        return publicKey;
    }

    private void keyPairGeneration() {
        try {
            String ALGORITHM = "RSA";
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(ALGORITHM, "BC");
            keyPairGenerator.initialize(1024, new SecureRandom());
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            privateKey = keyPair.getPrivate();
            publicKey = keyPair.getPublic();
            log.debug("Key Pair Generated");
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            log.error("Exception Occurred",e);
        }
    }

    private void keyStoreCreation() {
        try {
           keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            char[] keyStorePassword = "123@abc".toCharArray();
            keyStore.load(null, null);
            FileOutputStream fos = new FileOutputStream("KeyStore.ks");
            keyStore.store(fos, keyStorePassword);
            fos.close();
            log.debug("Key Store Created");
        } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | IOException e) {
            log.error("Exception Occurred",e);
        }
    }

    private void savePrivateKeyToKeyStore() {
        char[] keyPassword = "123@abc".toCharArray();
        KeyStore.ProtectionParameter protectionParameter = new KeyStore.PasswordProtection(keyPassword);
        X509Certificate certificate = generateCertificate();
        java.security.cert.Certificate[] certChain = new Certificate[1];
        certChain[0] = certificate;
        KeyStore.PrivateKeyEntry privateKeyEntry = new KeyStore.PrivateKeyEntry(privateKey, certChain);
        try {
            keyStore.setEntry("Private Key", privateKeyEntry, protectionParameter);
            log.debug("Private key stored to KeyStore");
        } catch (KeyStoreException e) {
            log.error("Exception Occurred",e);
        }
    }

    @SuppressWarnings("deprecation")
    private X509Certificate generateCertificate() {
        // build a certificate generator
        X509V3CertificateGenerator certGen = new X509V3CertificateGenerator();
        String CERTIFICATE_DN = "CN = cn , O = o, L =L ,ST = i1, C = c";
        X500Principal dnName = new X500Principal(CERTIFICATE_DN);

        // add some options
        certGen.setSerialNumber(BigInteger.valueOf(System.currentTimeMillis()));
        certGen.setSubjectDN(dnName);
        certGen.setIssuerDN(dnName);
        // Set not before Yesterday
        certGen.setNotBefore(new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000));
        // Set not after 2 years
        certGen.setNotAfter(new Date(System.currentTimeMillis() + 2L * 365 * 24 * 60 * 60 * 1000));
        certGen.setPublicKey(publicKey);
        certGen.setSignatureAlgorithm("SHA256WithRSAEncryption");
        //certGen.addExtension(X509Extensions.ExtendedKeyUsage, true,new ExtendedKeyUsage(KeyPurposeId.id_kp_timeStamping));

        // Finally, sign the certificate with the private key
        X509Certificate cert = null;
        try {
            cert = certGen.generate(privateKey, "BC");
            log.debug("Certificate Generated");
        } catch (CertificateEncodingException | InvalidKeyException | NoSuchAlgorithmException | SignatureException | NoSuchProviderException e) {
            log.error("Exception Occurred",e);
        }
        return cert;
    }

    KeyStore getKeyStore() {
        return keyStore;
    }

    PublicKey strToPub(String str){
        PublicKey publicKey = null;
        //converting string to byte initially and then back to public key
        byte[] bytePub1 = Base64.getDecoder().decode(str);
        if (str.equals("")){
            return null;
        }
        KeyFactory factory;
        try {
            factory = KeyFactory.getInstance("RSA");
            publicKey = factory.generatePublic(new X509EncodedKeySpec(bytePub1));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            log.error("Exception Occurred",e);
        }
        return publicKey;
    }

    String pubToStr(PublicKey key){
        String strPub;
        //converting public key to byte[] and then convert it in to string
        if(key==null){
            strPub = "";
            return  strPub;
        }
        byte[] bytePub = key.getEncoded();
        strPub = Base64.getEncoder().encodeToString(bytePub);
        return strPub;
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
}
