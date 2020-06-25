/*
 * Copyright to the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.rioproject.resolver.aether.util;

import org.eclipse.aether.artifact.Artifact;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * Generates a Maven pom
 *
 * @author Dennis Reedy
 */
public class DefaultPomGenerator {

    public static void writeTo(File pomFile, Artifact artifact) throws IOException {
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(pomFile),
                                                                       StandardCharsets.UTF_8))) {
            writer.write(DefaultPomGenerator.getContents(artifact));
        }
        /*ignore*/
    }
    public static String getContents(Artifact artifact) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<project xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd");
        sb.append("\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n");
        sb.append("    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">");
        sb.append("  <modelVersion>4.0.0</modelVersion>\n");
        sb.append("  <groupId>").append(artifact.getGroupId()).append("</groupId>\n");
        sb.append("  <artifactId>").append(artifact.getArtifactId()).append("</artifactId>\n");
        sb.append("  <version>").append(artifact.getVersion()).append("</version>\n");
        sb.append("</project>\n");
        return sb.toString();
    }

    public static String getGenerationPath(Artifact a) {
        return String.format("%s/%s/%s", a.getGroupId(), a.getArtifactId(), a.getVersion());
    }
}
