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
package org.rioproject.gnostic.service;

import org.junit.Assert;
import org.rioproject.gnostic.Gnostic;
import org.rioproject.resolver.maven2.Repository;
import org.rioproject.impl.util.FileUtils;
import org.rioproject.sla.RuleMap;

import java.io.*;
import java.rmi.RemoteException;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

/**
 * Helper to create and install an artifact for testing
 */
public class Util {

    public static Double getJVMVersion() {
        String sVersion = System.getProperty("java.version");
        String[] parts = sVersion.split("\\.");
        return Double.parseDouble(String.format("%s.%s", parts[0], parts[1]));
    }

    public static File createJar(String name) {
        File gnosticTestDir = getGnosticTestDirectory();
        if (!gnosticTestDir.exists())
            if (gnosticTestDir.mkdirs())
                System.out.println("Created " + gnosticTestDir.getPath());

        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        JarOutputStream target = null;
        File jar = new File(gnosticTestDir, name + ".jar");
        try {
            target = new JarOutputStream(new FileOutputStream(jar), manifest);
            addToJar(new File("target" + File.separator + "test-classes"), target);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (target != null)
                    target.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return jar;
    }

    private static void addToJar(File source, JarOutputStream jarOutput) throws IOException {
        BufferedInputStream in = null;
        try {
            if (source.isDirectory()) {
                String name = source.getPath().replace("\\", "/");
                if (!name.isEmpty()) {
                    if (!name.endsWith("/"))
                        name += "/";
                    JarEntry entry = new JarEntry(name);
                    entry.setTime(source.lastModified());
                    jarOutput.putNextEntry(entry);
                    jarOutput.closeEntry();
                }
                for (File nestedFile : source.listFiles())
                    addToJar(nestedFile, jarOutput);
                return;
            }
            String name = source.getPath().replace("\\", "/").substring("target/test-classes/".length());
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

    public static void writePom(String name) {
        File gnosticTestDir = getGnosticTestDirectory();
        try {
            File pom = new File(gnosticTestDir, name + ".pom");
            BufferedWriter out = new BufferedWriter(new FileWriter(pom));
            out.write(getPom());
            out.close();
        } catch (IOException e) {
        }
    }

    private static String getPom() {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>").append("\n");
        sb.append("<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd\">").append("\n");
        sb.append("<modelVersion>4.0.0</modelVersion>").append("\n");
        sb.append("<groupId>org.rioproject.gnostic.service</groupId>").append("\n");
        sb.append("<artifactId>test</artifactId>").append("\n");
        sb.append("<version>1.0</version>").append("\n");
        sb.append("</project>").append("\n");
        return sb.toString();

    }

    public static void removeInstallation() {
        File gnosticTestDir = getGnosticTestDirectory();
        FileUtils.remove(gnosticTestDir.getParentFile());
    }


    public static void waitForRule(Gnostic g, String rule) {
        Throwable thrown = null;
        long t0 = System.currentTimeMillis();
        try {
            while (!hasRule(g.get(), rule)) {
                sleep(500);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
            thrown = e;
        }
        Assert.assertNull(thrown);
        System.out.println("Rule loaded in " + (System.currentTimeMillis() - t0) + " millis");
    }

    private static boolean hasRule(List<RuleMap> ruleMaps, String rule) {
        boolean hasRule = false;
        for (RuleMap ruleMap : ruleMaps) {
            System.out.println("rule: " + ruleMap.getRuleDefinition().getResource());
            if (ruleMap.getRuleDefinition().getResource().contains(rule)) {
                hasRule = true;
                break;
            }
        }
        return hasRule;
    }

    public static void sleep(long l) {
        try {
            Thread.sleep(l);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static File getGnosticTestDirectory() {
        File m2Repo = Repository.getLocalRepository();
        return new File(m2Repo, "org/rioproject/gnostic/service/test/1.0/");
    }
}
