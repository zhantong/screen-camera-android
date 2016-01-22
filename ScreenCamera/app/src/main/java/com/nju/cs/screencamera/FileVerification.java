package com.nju.cs.screencamera;

import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;

/**
 * Created by zhantong on 16/1/22.
 */
public class FileVerification {
    /**
     * Get the md5 value of the filepath specified file
     * @param filePath The filepath of the file
     * @return The md5 value
     */
    public static String fileToMD5(String filePath) {
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(filePath); // Create an FileInputStream instance according to the filepath
            byte[] buffer = new byte[1024]; // The buffer to read the file
            MessageDigest digest = MessageDigest.getInstance("MD5"); // Get a MD5 instance
            int numRead = 0; // Record how many bytes have been read
            while (numRead != -1) {
                numRead = inputStream.read(buffer);
                if (numRead > 0)
                    digest.update(buffer, 0, numRead); // Update the digest
            }
            byte [] md5Bytes = digest.digest(); // Complete the hash computing
            return convertHashToString(md5Bytes); // Call the function to convert to hex digits
        } catch (Exception e) {
            return null;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close(); // Close the InputStream
                } catch (Exception e) { }
            }
        }
    }

    /**
     * Get the sha1 value of the filepath specified file
     * @param filePath The filepath of the file
     * @return The sha1 value
     */
    public static String fileToSHA1(String filePath) {
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(filePath); // Create an FileInputStream instance according to the filepath
            byte[] buffer = new byte[1024]; // The buffer to read the file
            MessageDigest digest = MessageDigest.getInstance("SHA-1"); // Get a SHA-1 instance
            int numRead = 0; // Record how many bytes have been read
            while (numRead != -1) {
                numRead = inputStream.read(buffer);
                if (numRead > 0)
                    digest.update(buffer, 0, numRead); // Update the digest
            }
            byte [] sha1Bytes = digest.digest(); // Complete the hash computing
            return convertHashToString(sha1Bytes); // Call the function to convert to hex digits
        } catch (Exception e) {
            return null;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close(); // Close the InputStream
                } catch (Exception e) { }
            }
        }
    }

    /**
     * Convert the hash bytes to hex digits string
     * @param hashBytes
     * @return The converted hex digits string
     */
    private static String convertHashToString(byte[] hashBytes) {
        String returnVal = "";
        for (int i = 0; i < hashBytes.length; i++) {
            returnVal += Integer.toString(( hashBytes[i] & 0xff) + 0x100, 16).substring(1);
        }
        return returnVal.toLowerCase();
    }
}
