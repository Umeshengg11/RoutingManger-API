package com.ehelpy.brihaspati4.routingManagerAPI;

import org.apache.log4j.Logger;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class Utility {
    private static final Logger log = Logger.getLogger(Utility.class);
    /**
     * This method is used to convert the String format of public key to original Public Key format.
     *
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
     *
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

}
