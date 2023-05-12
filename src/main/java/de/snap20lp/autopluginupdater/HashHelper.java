package de.snap20lp.autopluginupdater;

import java.io.File;
import java.math.BigInteger;
import java.nio.file.Files;
import java.security.MessageDigest;

public class HashHelper {

    private static String generateHashFromFile(File file) throws Exception {
        byte[] data = Files.readAllBytes(file.toPath());
        byte[] hash = MessageDigest.getInstance("MD5").digest(data);
        return new BigInteger(1, hash).toString(16);
    }

    public static boolean compareFileHash(File file, File file2) throws Exception {
        return generateHashFromFile(file).equals(generateHashFromFile(file2));
    }

}
