package com.whatscloud.logic.security;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import com.bugsense.trace.BugSenseHandler;
import com.whatscloud.config.debug.Logging;
import com.whatscloud.config.reporting.BugSense;
import com.whatscloud.logic.auth.User;
import com.whatscloud.utils.objects.Singleton;

import java.io.ByteArrayOutputStream;
import java.security.AlgorithmParameters;
import java.security.MessageDigest;
import java.security.spec.KeySpec;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class AES
{
    public static final int KEY_SIZE = 256;
    public static final int ITERATIONS = 10;

    public static final String SALT = "WhatsCloud";
    public static final String HASHING_ALGORITHM = "SHA-256";

    public static SecretKeySpec getAESSecret(Context context) throws Exception
    {
        //--------------------------------
        // Get encryption key
        //--------------------------------

        String key = User.getEncryptionKey(context);

        //--------------------------------
        // Hash it
        //--------------------------------

        key = get256BitHash(key);

        //--------------------------------
        // Generate salt hash
        //--------------------------------

        String salt = get256BitHash(SALT);

        //--------------------------------
        // Use PBKDF2 key derivation
        //--------------------------------

        SecretKeyFactory derivation = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");

        //--------------------------------
        // Supply the key, salt, iterations
        // and key size
        //--------------------------------

        KeySpec spec = new PBEKeySpec(key.toCharArray(), salt.getBytes("UTF-8"), ITERATIONS, KEY_SIZE);

        //--------------------------------
        // Generate a secret
        //--------------------------------

        SecretKey secret = derivation.generateSecret(spec);

        //--------------------------------
        // Prepare it for AES
        //--------------------------------

        SecretKeySpec secretSpec = new SecretKeySpec(secret.getEncoded(), "AES");

        //--------------------------------
        // Return it
        //--------------------------------

        return secretSpec;
    }

    private static byte[] generateRandomIV()
    {
        //--------------------------------
        // Get random generator
        //--------------------------------

        Random random = new Random();

        //--------------------------------
        // Prepare 16-bit iv
        //--------------------------------

        byte[] iv = new byte[16];

        //--------------------------------
        // Get 16 random bytes
        //--------------------------------

        random.nextBytes(iv);

        //--------------------------------
        // Return IV
        //--------------------------------

        return iv;
    }

    public static String encrypt(String message, Context context)
    {
        try
        {
            //--------------------------------
            // Get AES secret
            //--------------------------------

            SecretKeySpec secret = getAESSecret(context);

            //--------------------------------
            // Get AES cipher
            //--------------------------------

            Cipher aes = Cipher.getInstance("AES/CBC/PKCS5Padding");

            //--------------------------------
            // Get generated IV
            //--------------------------------

            byte[] iv = generateRandomIV();

            //--------------------------------
            // Get generated IV spec
            //--------------------------------

            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            //--------------------------------
            // Initialize it with secret
            //--------------------------------

            aes.init(aes.ENCRYPT_MODE, secret, ivSpec);

            //--------------------------------
            // Get AES cipher text
            //--------------------------------

            byte[] cipherText = aes.doFinal(message.getBytes("UTF-8"));

            //--------------------------------
            // Merge IV and cipher into array
            //--------------------------------

            byte[] encrypted = mergeByteArrays(iv, cipherText);

            //--------------------------------
            // Base64 encode the byte array
            //--------------------------------

            return Base64.encodeToString(encrypted, Base64.DEFAULT);
        }
        catch( Exception exc )
        {
            //--------------------------------
            // Send to BugSense
            //--------------------------------

            BugSenseHandler.sendException(exc);

            //--------------------------------
            // Log the error
            //--------------------------------

            Log.e(Logging.TAG_NAME, "Encryption error: " + exc.getMessage());
        }

        //--------------------------------
        // Something went wrong,
        // return nothing
        //--------------------------------

        return "";
    }

    public static byte[] mergeByteArrays(byte[] array1, byte[] array2) throws Exception
    {
        //--------------------------------
        // Create tmp byte stream
        //--------------------------------

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        //--------------------------------
        // Write both arrays
        //--------------------------------

        outputStream.write( array1 );
        outputStream.write( array2 );

        //--------------------------------
        // Merge them
        //--------------------------------

        return outputStream.toByteArray();
    }

    public static byte[] getEncryptionIV(byte[] data)
    {
        //--------------------------------
        // Prepare IV byte array
        //--------------------------------

        byte[] iv = new byte[16];

        //--------------------------------
        // Copy first 16 bytes
        //--------------------------------

        System.arraycopy(data, 0, iv, 0, 16);

        //--------------------------------
        // Return IV
        //--------------------------------

        return iv;
    }

    public static byte[] getEncryptionCipher(byte[] data)
    {
        //--------------------------------
        // Prepare cipher byte array
        //--------------------------------

        byte[] cipher = new byte[data.length - 16];

        //--------------------------------
        // Copy from 17th byte
        //--------------------------------

        System.arraycopy(data, 16, cipher, 0, cipher.length );

        //--------------------------------
        // Return cipher
        //--------------------------------

        return cipher;
    }

    public static String decrypt(String message, Context context)
    {
        try
        {
            //--------------------------------
            // Decode from Base64
            //--------------------------------

            byte[] data = Base64.decode(message.getBytes("UTF-8"), Base64.DEFAULT);

            //--------------------------------
            // Get IV from byte array
            //--------------------------------

            byte[] iv = getEncryptionIV(data);

            //--------------------------------
            // Get cipher from byte array
            //--------------------------------

            byte[] cipherText = getEncryptionCipher(data);

            //--------------------------------
            // Get AES secret
            //--------------------------------

            SecretKeySpec secret = getAESSecret(context);

            //--------------------------------
            // Get AES cipher
            //--------------------------------

            Cipher aes = Cipher.getInstance("AES/CBC/PKCS5Padding");

            //--------------------------------
            // Init cipher with secret & IV
            //--------------------------------

            aes.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(iv));

            //--------------------------------
            // Actually decrypt
            //--------------------------------

            cipherText = aes.doFinal(cipherText);

            //--------------------------------
            // Convert back to string
            //--------------------------------

            String decrypted = new String(cipherText, "UTF-8");

            //--------------------------------
            // Return decrypted string
            //--------------------------------

            return decrypted;
        }
        catch (Exception exc)
        {
            //--------------------------------
            // Send to BugSense
            //--------------------------------

            BugSenseHandler.sendException(exc);

            //--------------------------------
            // Log the error
            //--------------------------------

            Log.e(Logging.TAG_NAME, "Decryption error: " + exc.getMessage());
        }

        //--------------------------------
        // Something went wrong,
        // return nothing
        //--------------------------------

        return "";
    }

    public static String get256BitHash(String input) throws Exception
    {
        //--------------------------------
        // Prepare hashing algorithm
        //--------------------------------

        MessageDigest digest = MessageDigest.getInstance(HASHING_ALGORITHM);

        //--------------------------------
        // Actually hash the string
        //--------------------------------

        byte[] hash = digest.digest(input.getBytes("UTF-8"));

        //--------------------------------
        // Create string buffer
        // to convert it to String
        //--------------------------------

        StringBuffer hexString = new StringBuffer();

        //--------------------------------
        // Traverse byte array
        //--------------------------------

        for (int i = 0; i < hash.length; i++)
        {
            //--------------------------------
            // Convert int to hex
            //--------------------------------

            String hex = Integer.toHexString(0xff & hash[i]);

            //--------------------------------
            // Pad with 0
            //--------------------------------

            if ( hex.length() == 1 )
            {
                hexString.append('0');
            }

            //--------------------------------
            // Append hex to string buffer
            //--------------------------------

            hexString.append(hex);
        }

        //--------------------------------
        // Convert buffer to string
        //--------------------------------

        return hexString.toString();
    }

    public static String getRandomEncryptionKey()
    {
        //--------------------------------
        // Get random generator
        //--------------------------------

        Random random = new Random();

        //--------------------------------
        // Generate random key
        //--------------------------------

         int key = 100000 + random.nextInt(900000);

        //--------------------------------
        // Convert to String
        //--------------------------------

        return key + "";
    }

}
