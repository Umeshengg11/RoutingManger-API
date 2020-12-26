import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.x509.X509V3CertificateGenerator;

import javax.security.auth.x500.X500Principal;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.*;
import java.security.cert.Certificate;
import java.util.Date;

public class B4_NodeGeneration {
    private String NodeID;
    private static PublicKey publicKey;
    private static PrivateKey privateKey;
    private String HashID;
    private static KeyStore keyStore;
    private static KeyPair keyPair;

    public String getNodeID() {
        return NodeID;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public String getHashID() {
        return HashID;
    }

    private static void keyPairGeneration() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(1048);
            keyPair = keyPairGenerator.generateKeyPair();
            privateKey = keyPair.getPrivate();
            publicKey = keyPair.getPublic();

            for (byte bytes : publicKey.getEncoded()) {
                String pub = String.format("%02x", bytes);
                System.out.print(pub);
            }
            System.out.println();

            for (byte bytes : privateKey.getEncoded()) {
                String pri = String.format("%02x", bytes);
                System.out.print(pri);
            }
            System.out.println("Key Pair Generated");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    private static void keyStoreCreation() {
        try {
            keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            char[] keyStorePassword = "123@abc".toCharArray();
            keyStore.load(null, keyStorePassword);
            FileOutputStream fos = new FileOutputStream("KeyStoreFile");
            keyStore.store(fos, keyStorePassword);
            fos.close();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void setToKeyStore() {
        char[] keyPassword = "123@abc".toCharArray();
        KeyStore.ProtectionParameter protectionParameter = new KeyStore.PasswordProtection(keyPassword);
        X509Certificate certificate = generateCertificate(keyPair);
        Certificate[] certChain = new Certificate[1];
        certChain[0] = certificate;
        KeyStore.PrivateKeyEntry privateKeyEntry = new KeyStore.PrivateKeyEntry(privateKey,certChain);

        try {
            keyStore.setEntry("Private Key",privateKeyEntry,protectionParameter);
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }
    }

    private static X509Certificate generateCertificate(KeyPair keyPair) {
    Provider provider = new BouncyCastleProvider();
    Security.addProvider(provider);
        // generate a key pair
        KeyPairGenerator keyPairGenerator = null;
        try {
            keyPairGenerator = KeyPairGenerator.getInstance("RSA", "BC");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        }
        keyPairGenerator.initialize(4096, new SecureRandom());
        KeyPair keyPair1 = keyPairGenerator.generateKeyPair();

        // build a certificate generator
        X509V3CertificateGenerator certGen = new X509V3CertificateGenerator();
        X500Principal dnName = new X500Principal("cn=example");

        // add some options
        certGen.setSerialNumber(BigInteger.valueOf(System.currentTimeMillis()));
        certGen.setSubjectDN(new X509Name("dc=name"));
        certGen.setIssuerDN(dnName); // use the same
        // yesterday
        certGen.setNotBefore(new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000));
        // in 2 years
        certGen.setNotAfter(new Date(System.currentTimeMillis() + 2 * 365 * 24 * 60 * 60 * 1000));
        certGen.setPublicKey(keyPair.getPublic());
        certGen.setSignatureAlgorithm("SHA256WithRSAEncryption");
        certGen.addExtension(X509Extensions.ExtendedKeyUsage, true,
                new ExtendedKeyUsage(KeyPurposeId.id_kp_timeStamping));

        // finally, sign the certificate with the private key of the same KeyPair
        X509Certificate cert = null;
        try {
            cert = certGen.generate(keyPair.getPrivate(), "BC");
        } catch (CertificateEncodingException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (SignatureException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
        return cert;
    }

    private static PrivateKey getFromKeyStore() {
        char[] keyPassword = "123@abc".toCharArray();
        KeyStore.ProtectionParameter protectionParameter = new KeyStore.PasswordProtection(keyPassword);
        KeyStore.PrivateKeyEntry privateKeyEntry = null;
        try {
            privateKeyEntry = (KeyStore.PrivateKeyEntry) keyStore.getEntry("Private Key",protectionParameter);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnrecoverableEntryException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }
        PrivateKey pvtKey = privateKeyEntry.getPrivateKey();
     return pvtKey;
    }

    public static void main(String[] args) {
        PrivateKey priKey;
        keyPairGeneration();
        keyStoreCreation();
        setToKeyStore();
        priKey = getFromKeyStore();

        for (byte bytes : priKey.getEncoded()) {
            String pri = String.format("%02x", bytes);
            System.out.print(pri);
        }
    }
}