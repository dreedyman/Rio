/*
 * Copyright 2010 to the original author or authors.
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
package org.rioproject.resolver;

import java.io.*;
import java.util.List;

/**
 * Utilities test cases use in the resolver module
 */
public class Utils {
    public static void writeLocalM2RepoSettings() {
        StringBuilder sb = new StringBuilder();
        sb.append("<settings xmlns=\"http://maven.apache.org/SETTINGS/1.0.0\"").append("\n");
        sb.append("    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"").append("\n");
        sb.append("    xsi:schemaLocation=\"http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd\">").append("\n");
		sb.append("    <localRepository>target/test-repo/</localRepository> ").append("\n");
        sb.append("    <profiles>").append("\n");
        sb.append("        <profile>").append("\n");
        sb.append("            <id>p1</id>").append("\n");
        sb.append("            <activation>").append("\n");
        sb.append("                <activeByDefault>true</activeByDefault>").append("\n");
        sb.append("            </activation>").append("\n");
        sb.append("            <repositories>").append("\n");
        sb.append("                <repository>").append("\n");
        sb.append("                    <id>rio</id>").append("\n");
        sb.append("                    <url>http://www.rio-project.org/maven2</url>").append("\n");
        sb.append("                    <releases>").append("\n");
        sb.append("                        <enabled>true</enabled>").append("\n");
        sb.append("                    </releases>").append("\n");
        sb.append("                    <snapshots>").append("\n");
        sb.append("                        <enabled>true</enabled>").append("\n");
        sb.append("                    </snapshots>").append("\n");
        sb.append("                </repository>").append("\n");
        sb.append("                <repository>").append("\n");
        sb.append("                    <id>jboss</id>").append("\n");
        sb.append("                    <url>http://repository.jboss.org/nexus/content/groups/public-jboss/</url>").append("\n");
        sb.append("                    <releases>").append("\n");
        sb.append("                        <enabled>true</enabled>").append("\n");
        sb.append("                    </releases>").append("\n");
        sb.append("                    <snapshots>").append("\n");
        sb.append("                        <enabled>true</enabled>").append("\n");
        sb.append("                    </snapshots>").append("\n");
        sb.append("                </repository>").append("\n");
        sb.append("            </repositories>").append("\n");
        sb.append("        </profile>").append("\n");
        sb.append("    </profiles>").append("\n");
        sb.append("    <activeProfiles>").append("\n");
        sb.append("        <activeProfile>p1</activeProfile>").append("\n");
        sb.append("    </activeProfiles>").append("\n");
		sb.append("</settings>").append("\n");
		File localM2RepoSettingsFile = getM2Settings();
        Writer output;
        try {
            output = new BufferedWriter(new FileWriter(localM2RepoSettingsFile));
            output.write(sb.toString());
            close(output);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getMirroredURL() {
        StringBuilder builder = new StringBuilder();
        //builder.append("file://").append(System.getProperty("user.dir")).append(File.separator).append("target");
        builder.append("http://repo1.maven.org/maven2");
        return builder.toString();
    }
    public static void writeLocalM2RepoSettingsWithMirror() {
        StringBuilder sb = new StringBuilder();
        sb.append("<settings xmlns=\"http://maven.apache.org/SETTINGS/1.0.0\"").append("\n");
        sb.append("    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"").append("\n");
        sb.append("    xsi:schemaLocation=\"http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd\">").append("\n");
        sb.append("    <localRepository>target/test-repo/</localRepository> ").append("\n");
        sb.append("    <mirrors>").append("\n");
        sb.append("        <mirror>").append("\n");
        sb.append("            <id>all</id>").append("\n");
        sb.append("            <url>").append(getMirroredURL()).append("</url>").append("\n");
        sb.append("            <mirrorOf>*</mirrorOf>").append("\n");
        sb.append("        </mirror>").append("\n");
        sb.append("    </mirrors>").append("\n");
        sb.append("    <profiles>").append("\n");
        sb.append("        <profile>").append("\n");
        sb.append("            <id>p1</id>").append("\n");
        sb.append("            <activation>").append("\n");
        sb.append("                <activeByDefault>true</activeByDefault>").append("\n");
        sb.append("            </activation>").append("\n");
        sb.append("            <repositories>").append("\n");
        sb.append("                <repository>").append("\n");
        sb.append("                    <id>rio</id>").append("\n");
        sb.append("                    <url>http://www.rio-project.org/maven2</url>").append("\n");
        sb.append("                    <releases>").append("\n");
        sb.append("                        <enabled>true</enabled>").append("\n");
        sb.append("                    </releases>").append("\n");
        sb.append("                    <snapshots>").append("\n");
        sb.append("                        <enabled>true</enabled>").append("\n");
        sb.append("                    </snapshots>").append("\n");
        sb.append("                </repository>").append("\n");
        sb.append("                <repository>").append("\n");
        sb.append("                    <id>jboss</id>").append("\n");
        sb.append("                    <url>http://repository.jboss.org/nexus/content/groups/public-jboss/</url>").append("\n");
        sb.append("                    <releases>").append("\n");
        sb.append("                        <enabled>true</enabled>").append("\n");
        sb.append("                    </releases>").append("\n");
        sb.append("                    <snapshots>").append("\n");
        sb.append("                        <enabled>true</enabled>").append("\n");
        sb.append("                    </snapshots>").append("\n");
        sb.append("                </repository>").append("\n");
        sb.append("            </repositories>").append("\n");
        sb.append("        </profile>").append("\n");
        sb.append("    </profiles>").append("\n");
        sb.append("    <activeProfiles>").append("\n");
        sb.append("        <activeProfile>p1</activeProfile>").append("\n");
        sb.append("    </activeProfiles>").append("\n");
        sb.append("</settings>").append("\n");
        File localM2RepoSettingsFile = getM2Settings();
        Writer output;
        try {
            output = new BufferedWriter(new FileWriter(localM2RepoSettingsFile));
            output.write(sb.toString());
            close(output);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static File getM2Settings() {
        return new File(System.getProperty("user.home"), ".m2/settings.xml");
    }

    public static File saveM2Settings() throws IOException {
        File settings = getM2Settings();
        File saveOrigSettings = null;
        if(settings.exists()) {
            saveOrigSettings = new File(System.getProperty("user.home"), ".m2/settings.xml.sav");
            FileUtils.copy(settings, saveOrigSettings);
        }
        return saveOrigSettings;
    }

    public static void close(Closeable c) {
        try {
            if(c!=null)
                c.close();
        } catch (IOException e) {
        }
    }

    public static String formatClassPath(String[] classPath) {
        StringBuilder sb = new StringBuilder();
        for(String s : classPath) {
            if(sb.length()>0)
                sb.append("\n");
            int ndx = s.lastIndexOf(File.separator);
            sb.append(s.substring(ndx+1));
        }
        return sb.toString();
    }

    public static String flatten(List<String> l) {
        StringBuilder sb = new StringBuilder();
        for(String s : l) {
            if(sb.length()>0)
                sb.append(",");
            sb.append(s);
        }
        return sb.toString();
    }

        /**
     * Search and Replace
     *
     * @param str The string to replace tokens for
     * @param pattern The pattern to look for
     * @param replace The replacement for the pattern
     *
     * @return A string with tokens replaced
     */
    public static String replace(String str, String pattern, String replace) {
        int s = 0;
        int e;
        StringBuilder result = new StringBuilder();
        while((e = str.indexOf(pattern, s)) >= 0) {
            result.append(str.substring(s, e));
            result.append(replace);
            s = e+pattern.length();
        }
        result.append(str.substring(s));
        return result.toString();
    }
}
