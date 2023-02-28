package org.example;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FileDigester {
    public static byte[] digestFile(Path p) {
        byte[] buffer = new byte[4096]; //Seems Reasonable?

        try {
            MessageDigest md = MessageDigest.getInstance("SHA3-256");
            InputStream is = Files.newInputStream(p);
            int length = 0;
            while( (length = is.read(buffer)) != -1) {
                md.digest(buffer, 0, length);
            }
            return md.digest();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (DigestException e) {
            e.printStackTrace();
        }
        return new byte[]{};
    }
}
