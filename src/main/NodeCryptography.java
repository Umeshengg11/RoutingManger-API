package main;

import org.apache.log4j.Logger;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.x509.X509V3CertificateGenerator;

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
import java.util.Calendar;
import java.util.Hashtable;
import java.util.Vector;

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
    private static final char[] keyPassword = "123@abc".toCharArray();

    /**
     * This is the default constructor for this class.
     * This is made private so that it cannot be accessed from outside the class.
     * All the initialization is done in the constructor.
     * BountyCastle is used as the service provider.
     * BountyCastle jar file is added to the class path.
     * nodeDetailsFile is checked whether is available from the previous login to take all the relevant parameters from
     * there and if it is not available it is created and all the parameters are added to the file.
     */
    private NodeCryptography() {
        Provider provider = new BouncyCastleProvider();
        Security.addProvider(provider);
        try {
            keyStore = KeyStore.getInstance("JCEKS");
            String filePath = "KeyStore.ks";
            File keyStoreFile = new File(filePath);
            boolean keystoreExist = keyStoreFile.exists();
            if (!keystoreExist) {
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
            log.info("Exception Occurred",e);
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
     * @return - Self signed Certificate.
     */
    @SuppressWarnings("deprecation")
    private X509Certificate createSelfSignedCert(String eMail, String orgUnit, String organisation, String city, String state, String country) {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        try {
            X509V3CertificateGenerator x500Name = new X509V3CertificateGenerator();
            Vector<ASN1ObjectIdentifier> order = new Vector<>();
            Hashtable<ASN1ObjectIdentifier, String> attributeMap = new Hashtable<>();
            if (eMail != null) {
                attributeMap.put(X509Name.CN, eMail);
                order.add(X509Name.CN);
            }
            if (orgUnit != null) {
                attributeMap.put(X509Name.OU, orgUnit);
                order.add(X509Name.OU);
            }
            if (organisation != null) {
                attributeMap.put(X509Name.O, organisation);
                order.add(X509Name.O);
            }
            if (city != null) {
                attributeMap.put(X509Name.L, city);
                order.add(X509Name.L);
            }
            if (state != null) {
                attributeMap.put(X509Name.ST, state);
                order.add(X509Name.ST);
            }
            if (country != null) {
                attributeMap.put(X509Name.C, country);
                order.add(X509Name.C);
            }
            X509Name issuerDN = new X509Name(order, attributeMap);
            Calendar calendar = Calendar.getInstance();
            x500Name.setNotBefore(calendar.getTime());
            calendar.add(Calendar.YEAR, 1);
            x500Name.setNotAfter(calendar.getTime());
            x500Name.setSerialNumber(BigInteger.valueOf(System.currentTimeMillis()));
            x500Name.setSignatureAlgorithm("SHA256WithRSAEncryption");
            x500Name.setIssuerDN(issuerDN);
            x500Name.setSubjectDN(issuerDN);
            x500Name.setPublicKey(publicKey);
            X509Certificate[] chain = new X509Certificate[1];
            chain[0] = x500Name.generate(privateKey, "BC");
            log.debug("Certificate Generated");
            return chain[0];
        } catch (CertificateEncodingException | NoSuchProviderException | NoSuchAlgorithmException | SignatureException | InvalidKeyException e) {
            log.error("Exception Occurred", e);
        }
        return null;
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
            log.debug("Key Store Loaded");
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
            log.error("Exception Occurred", e);
        }
    }

    /**
     * This method is used to save private key and self signed certificate to the keystore.
     */
    private void saveToKeyStore() {
        ConfigData configData = ConfigData.getInstance();
        String eMail = configData.getValue("Email");
        String orgUnit = configData.getValue("OrgUnit");
        String organisation = configData.getValue("Organisation");
        String city = configData.getValue("City");
        String state = configData.getValue("State");
        String country = configData.getValue("Country");
        KeyStore.ProtectionParameter protectionParameter = new KeyStore.PasswordProtection(keyPassword);
        X509Certificate certificate = createSelfSignedCert(eMail, orgUnit, organisation, city, state, country);
        X509Certificate[] certChain = new X509Certificate[1];
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
     * This method will fetch Private key from the keystore and return it.
     *
     * @return - Private Key
     */
    PrivateKey getFromKeyStore() {
        KeyStore.ProtectionParameter protectionParameter = new KeyStore.PasswordProtection(keyPassword);
        KeyStore.PrivateKeyEntry privateKeyEntry = null;
        try {
            privateKeyEntry = (KeyStore.PrivateKeyEntry) nodeCryptography.getKeyStore().getEntry("Private Key", protectionParameter);
        } catch (NoSuchAlgorithmException | KeyStoreException | UnrecoverableEntryException e) {
            log.error("Exception Occurred", e);
        }
        assert privateKeyEntry != null;
        return privateKeyEntry.getPrivateKey();
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

     boolean updateCertificate(){
        boolean isUpdated = false;
        try {
            String filePath = "KeyStore.ks";
            File keyStoreFile = new File(filePath);
            boolean isDeleted = keyStoreFile.delete();
            keyStore = KeyStore.getInstance("JCEKS");
            keyStore.load(null, null);
            saveToKeyStore();
            saveKeyStore();
            loadKeyStore();
            KeyStore.ProtectionParameter protectionParameter = new KeyStore.PasswordProtection(keyPassword);
            KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry) keyStore.getEntry("Private Key", protectionParameter);
            privateKey = privateKeyEntry.getPrivateKey();
            Certificate certificate = keyStore.getCertificate("Certificate");
            publicKey = certificate.getPublicKey();
            isUpdated = true;
        } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | IOException | UnrecoverableEntryException e) {
            log.error("Exception Occurred", e);
        }
        return isUpdated;
    }

}
