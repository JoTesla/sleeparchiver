package com.pavelfatin.sleeparchiver.util;

import java.io.*;
import java.util.jar.*;

public class UnpackJar {
    public static void main(String[] args) throws Exception {
        String jarPath = "native/SleepTracker3/SleepTracker.jar";
        String outputDir = "native/SleepTracker3/extracted";

        new File(outputDir).mkdirs();

        try (JarFile jar = new JarFile(jarPath)) {
            java.util.Enumeration<JarEntry> entries = jar.entries();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();

                if (name.toLowerCase().contains("device") ||
                    name.toLowerCase().contains("serial") ||
                    name.toLowerCase().contains("port") ||
                    name.toLowerCase().contains("protocol")) {
                    System.out.println("Found: " + name);
                }

                File file = new File(outputDir, name);
                if (entry.isDirectory()) {
                    file.mkdirs();
                } else {
                    file.getParentFile().mkdirs();
                    try (InputStream in = jar.getInputStream(entry);
                         FileOutputStream out = new FileOutputStream(file)) {
                        byte[] buffer = new byte[8192];
                        int len;
                        while ((len = in.read(buffer)) > 0) {
                            out.write(buffer, 0, len);
                        }
                    }
                }
            }
        }

        System.out.println("\nExtracted to: " + outputDir);
    }
}
