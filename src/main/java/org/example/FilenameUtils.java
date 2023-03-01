package org.example;

import java.nio.file.Path;

public class FilenameUtils {
    public static String getExtension(Path p) {
        String fileName = p.getFileName().toString();
        if(!fileName.contains(".")) {
            return "";
        } else {
            return fileName.substring(fileName.lastIndexOf("."));
        }
    }
}
