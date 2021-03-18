package main;

import org.apache.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import javax.security.auth.x500.X500Principal;
import java.io.File;
import java.io.FileInputStream;
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
    private static final Logger log = Logger.getLogger(NodeCryptography.class);
    private static PublicKey publicKey;
    private static PrivateKey privateKey;
    private static KeyStore keyStore;
    private static NodeCryptography nodeCryptography;
    private final char[] keyPassword = "123@abc".toCharArray();

    /**
     * This is the default constructor for this class.
     * This is made private so that it cannot be accessed from outside the class.
     * All the initialization is done in the constructor.
     * BountyCastle is used as the service provider.
     * BountyCastle jar file is added to the class path.
     * nodeDetailsFile is checked whether is available from the previous login to take all the relevant parameters from
     * there and if it is not available it is created and all the parameters are added to the file.
     *
     */
    private NodeCryptography() {
        Provider provider = new BouncyCastleProvider();
        Security.addProvider(provider);
        try {
            keyStore = KeyStore.getInstance("JCEKS");
            String filePath = "KeyStore.ks";
            File nodeFile = new File(filePath);
            boolean nodeDetailsExists = nodeFile.exists();
            System.out.println(nodeDetailsExists);
            if (!nodeDetailsExists) {
                keyStore.load(null, null);
                keyPairGeneration();
                saveToKeyStore();
                saveKeyStore();
            } else {
                loadKeyStore();
                KeyStore.ProtectionParameter protectionParameter = new KeyStore.PasswordProtection(keyPassword);
                KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry) keyStore.getEntry("Private Key", protectionParameter);
                privateKey = privateKeyEntry.getPrivateKey();
                Certificate certificate = keyStore.getCertificate("Certificate");
                publicKey = certificate.getPublicKey();
            }
        } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | IOException | UnrecoverableEntryException e) {
            e.printStackTrace();
        }
    }

    /**
     * @return - Object of NodeCryptography.
     * Singleton object is created so that the same instance of the object is provided when this class is called.
     */
    public static synchronized NodeCryptography getInstance() {
        if (nodeCryptography == null) {
            nodeCryptography = new NodeCryptography();
        }
        return nodeCryptography;
    }

    /**
     * This method is used to generate keyPair and associated public and private key from it.
     */
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
            log.error("Exception Occurred", e);
        }
    }

    /**
     * This method will load keystore.
     */
    private void loadKeyStore() {
        try {
            FileInputStream fis = new FileInputStream("KeyStore.ks");
            keyStore.load(fis, keyPassword);
            fis.close();
            log.debug("Key Store Created");
        } catch (CertificateException | NoSuchAlgorithmException | IOException e) {
            log.error("Exception Occurred", e);
        }
    }

    /**
     * This method will save the keystore to the location defined.
     */
    private void saveKeyStore() {
        FileOutputStream fos;
        try {
            fos = new FileOutputStream("KeyStore.ks");
            keyStore.store(fos, keyPassword);
            fos.flush();
            fos.close();
        } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
            e.printStackTrace();
        }
    }

    /**
     * This method is used to save private key and self signed certificate to the keystore.
     */
    private void saveToKeyStore() {
        KeyStore.ProtectionParameter protectionParameter = new KeyStore.PasswordProtection(keyPassword);
        X509Certificate certificate = generateCertificate();
        java.security.cert.Certificate[] certChain = new Certificate[1];
        certChain[0] = certificate;
        KeyStore.PrivateKeyEntry privateKeyEntry = new KeyStore.PrivateKeyEntry(privateKey, certChain);
        try {
            keyStore.setEntry("Private Key", privateKeyEntry, protectionParameter);
            keyStore.setCertificateEntry("Certificate", certChain[0]);
            log.debug("Private key stored to KeyStore");
        } catch (KeyStoreException e) {
            log.error("Exception Occurred", e);
        }
    }

    /**
     * This method is used to convert the String format of public key to original Public Key format.
     * @param str - The string format of public key is given as argument
     * @return - Public Key
     */
    PublicKey strToPub(String str) {
        PublicKey publicKey = null;
        //converting string to byte initially and then back to public key
        byte[] bytePub1 = Base64.getDecoder().decode(str);
        if (str.equals("")) {
            return null;
        }
        KeyFactory factory;
        try {
            factory = KeyFactory.getInstance("RSA");
            publicKey = factory.generatePublic(new X509EncodedKeySpec(bytePub1));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            log.error("Exception Occurred", e);
        }
        return publicKey;
    }

    /**
     * This method is used to convert Public key to String format.
     * @param key - Public key is given as argument.
     * @return - String format of public key.
     */
    String pubToStr(PublicKey key) {
        String strPub;
        //converting public key to byte[] and then convert it in to string
        if (key == null) {
            strPub = "";
            return strPub;
        }
        byte[] bytePub = key.getEncoded();
        strPub = Base64.getEncoder().encodeToString(bytePub);
        return strPub;
    }

    /**
     * This method will fetch Private key from the keystore and return it.
     * @return - Private Key
     */
     PrivateKey getFromKeyStore() {
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

    /**
     * @return - Self signed Certificate.
     */
    @SuppressWarnings("deprecation")
    private static X509Certificate generateCertificate() {
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
            log.error("Exception Occurred", e);
        }
        return cert;
    }

    /**
     * @return - Public Key.
     */
    PublicKey getPublicKey() {
        return publicKey;
    }

    /**
     * @return - KeyStore.
     */
    KeyStore getKeyStore() {
        return keyStore;
    }
}
