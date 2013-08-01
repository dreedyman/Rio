/*
 * Copyright to the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.rioproject.test.utils;

import java.io.*;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

/**
 * Utility to create a JAR.
 */
public class JarUtil {

    public static File createJar(File source,
                                 File target,                                 
                                 String jarName,
                                 Manifest manifest,
                                 File toAdd) throws IOException {
        if (!target.exists())
            if (target.mkdirs())
                System.out.println("Created " + target.getPath());
        JarOutputStream jarOutput = null;
        File jar = new File(target, jarName);
        String strip = replaceSeparators(source.getPath());
        if(!strip.endsWith(File.separator))
            strip = strip+File.separator;
        try {
            jarOutput = new JarOutputStream(new FileOutputStream(jar), manifest);
            if(toAdd!=null)
                addToJar(toAdd, jarOutput, strip);
            addToJar(source, jarOutput, strip);
        } finally {
            if (jarOutput != null)
                jarOutput.close();            
        }
        return jar;
    }

    private static void addToJar(File source,
                                 JarOutputStream jarOutput,
                                 String strip) throws IOException {
        BufferedInputStream in = null;
        try {
            if (source.isDirectory()) {
                String name = replaceSeparators(source.getPath());
                if(name.length()>strip.length()) {
                    name = name.substring(strip.length());
                    if (!name.isEmpty()) {
                        if (!name.endsWith("/"))
                            name += "/";
                        JarEntry entry = new JarEntry(name);
                        entry.setTime(source.lastModified());
                        jarOutput.putNextEntry(entry);
                        jarOutput.closeEntry();
                    }
                }
                for (File nestedFile : source.listFiles())
                    addToJar(nestedFile, jarOutput, strip);
                return;
            }            
            String name = replaceSeparators(source.getPath()).substring(strip.length());
            JarEntry entry = new JarEntry(name);
            entry.setTime(source.lastModified());
            jarOutput.putNextEntry(entry);
            in = new BufferedInputStream(new FileInputStream(source));

            byte[] buffer = new byte[1024];
            while (true) {
                int count = in.read(buffer);
                if (count == -1)
                    break;
                jarOutput.write(buffer, 0, count);
            }
            jarOutput.closeEntry();
        }
        finally {
            if (in != null)
                in.close();
        }
    }

    private static String replaceSeparators(String s) {
        if(System.getProperty("os.name").startsWith("Windows"))
            return s;
        return s.replace("\\", "/");
    }
}
