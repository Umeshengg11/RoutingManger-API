import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.x509.X509V3CertificateGenerator;

import javax.security.auth.x500.X500Principal;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;

public class NodeCryptography {
    private PublicKey publicKey;
    private PrivateKey privateKey;
    private KeyStore keyStore;
    private final String CERTIFICATE_DN = "CN = cn , O = o, L =L ,ST = i1, C = c";
    private final String ALGORITHM = "RSA";
    private static NodeCryptography nodeCryptography;

    private NodeCryptography() {
        Provider provider = new BouncyCastleProvider();
        Security.addProvider(provider);
        keyPairGeneration();
        keyStoreCreation();
        generateCertificate();
        savePrivateKeyToKeyStore();
    }

    public static synchronized NodeCryptography getInstance() {
        if (nodeCryptography == null) {
            nodeCryptography = new NodeCryptography();
        }
        return nodeCryptography;
    }

    public  PublicKey getPublicKey() {
        return publicKey;
    }

    private void keyPairGeneration() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(ALGORITHM, "BC");
            keyPairGenerator.initialize(1024, new SecureRandom());
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            privateKey = keyPair.getPrivate();
            publicKey = keyPair.getPublic();
            //System.out.println("Key Pair Generated");
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            e.printStackTrace();
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
        } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
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
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("deprecation")
    private X509Certificate generateCertificate() {
        // build a certificate generator
        X509V3CertificateGenerator certGen = new X509V3CertificateGenerator();
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
        } catch (CertificateEncodingException | InvalidKeyException | NoSuchAlgorithmException | SignatureException | NoSuchProviderException e) {
            e.printStackTrace();
        }
        return cert;
    }

    public  KeyStore getKeyStore() {
        return keyStore;
    }
}
