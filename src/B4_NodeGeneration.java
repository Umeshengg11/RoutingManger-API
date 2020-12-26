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
    private String HashID;

    public String getNodeID() {
        return NodeID;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public String getHashID() {
        return HashID;
    }

    public B4_NodeGeneration() {
        NodeCryptography nodeCryptography = NodeCryptography.getInstance();
        publicKey = nodeCryptography.getPublicKey();
        System.out.println(publicKey);
    }
    //get public key and convert it into 60 bit node id
    // hash id signed using private key
    //next data required is public key itself.

    public cd cdvoid getNodeId(){

    }



}