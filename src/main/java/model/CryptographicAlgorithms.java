package model;

import javax.crypto.*;
import java.security.*;

public class CryptographicAlgorithms
{
    public static KeyPair generateKeyPair() throws NoSuchAlgorithmException
    {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");

        kpg.initialize(2048);

        return kpg.generateKeyPair();
    }

    public static Key unwrapKey(byte[] wrappedKey, PrivateKey key)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException
    {
        Cipher cipher = Cipher.getInstance("RSA");

        cipher.init(Cipher.UNWRAP_MODE, key);

        return cipher.unwrap(wrappedKey, "RSA", Cipher.SECRET_KEY);
    }

    public static byte[] encrypt(byte[] data, Key key)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException
    {
        Cipher cipher = Cipher.getInstance("AES");

        cipher.init(Cipher.ENCRYPT_MODE, key);

        return cipher.doFinal(data);
    }
}
