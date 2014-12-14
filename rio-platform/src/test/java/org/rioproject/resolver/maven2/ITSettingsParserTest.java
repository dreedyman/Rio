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
package org.rioproject.resolver.maven2;

import org.junit.Assert;
import org.junit.Test;
import org.rioproject.resolver.RemoteRepository;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Test the maven settings parser
 */
public class ITSettingsParserTest {

    @Test
    public void parserTest() throws IOException {
        File settingsFile = new File("src/test/conf/mirrored-settings.xml");
        SettingsParser parser = new SettingsParser();
        Settings settings = parser.parse(settingsFile);
        List<RemoteRepository> rrs = settings.getRemoteRepositories();
        Assert.assertEquals(9, rrs.size());
        Assert.assertEquals(5, countMirrored(rrs));
        printHeader(rrs, settingsFile);
        int i = 1;
        for (RemoteRepository rc : settings.getRemoteRepositories()) {
            if (rc.getId().equals("fraz")) {
                Assert.assertEquals("http://external:8081/nexus/content/groups/public", rc.getUrl());
                Assert.assertFalse("Releases should be false, got (" + rc.supportsReleases() + ")",
                                   rc.supportsReleases());
                Assert.assertTrue(rc.supportsSnapshots());
            }
            if (rc.getId().equals("bar")) {
                Assert.assertEquals("file://HARRY.POTTER", rc.getUrl());
                Assert.assertTrue(rc.supportsReleases());
                Assert.assertFalse(rc.supportsSnapshots());
            }
            if (rc.getId().equals("local-file-system-repo")) {
                Assert.assertEquals("file://local.file.system", rc.getUrl());
            }
            if (rc.getId().equals("localhost-repo")) {
                Assert.assertEquals("http://localhost:80801/repo", rc.getUrl());
            }
            if (rc.getId().equals("west")) {
                Assert.assertEquals("http://voldemort:8088/nexus/content/groups/public", rc.getUrl());
            }
            if (rc.getId().equals("east") || rc.getId().equals("north")) {
                Assert.assertEquals("http://blutarsky:8088/nexus/content/groups/public", rc.getUrl());
            }
            if (rc.getId().equals("jane")) {
                Assert.assertEquals("http://NOT.MIRRORED.COM", rc.getUrl());
            }

            i = print(rc, i, rrs.size());
        }
        parser.parse(getMavenSettingsFile());
    }

    @Test
    public void parserTest2() throws IOException {
        File settingsFile = new File("src/test/conf/jrsettings.xml");
        SettingsParser parser = new SettingsParser();
        Settings settings = parser.parse(settingsFile);
        List<RemoteRepository> rrs = settings.getRemoteRepositories();
        Assert.assertEquals(4, rrs.size());
        Assert.assertEquals(0, countMirrored(rrs));
        printHeader(rrs, settingsFile);
        int i = 1;
        for (RemoteRepository rc : settings.getRemoteRepositories()) {
            if (rc.getId().equals("internal")) {
                Assert.assertEquals("http://rds.dev:8180/archiva/repository/internal/", rc.getUrl());
                Assert.assertTrue(rc.supportsReleases());
                Assert.assertFalse(rc.supportsSnapshots());
            }
            if (rc.getId().equals("snapshots")) {
                Assert.assertEquals("http://rds.dev:8180/archiva/repository/snapshots/", rc.getUrl());
                Assert.assertFalse(rc.supportsReleases());
                Assert.assertTrue(rc.supportsSnapshots());
            }
            if (rc.getId().equals("real_repository")) {
                Assert.assertEquals("http://build01.dev.real.com:9999/repository/", rc.getUrl());
                Assert.assertTrue(rc.supportsReleases());
                Assert.assertTrue(rc.supportsSnapshots());
            }

            i = print(rc, i, rrs.size());
        }
        parser.parse(getMavenSettingsFile());
    }

    @Test
    public void parserTest3() throws IOException {
        File settingsFile = new File(System.getProperty("user.home"), ".m2/settings.xml");
        SettingsParser parser = new SettingsParser();
        Settings settings = parser.parse(settingsFile);
        List<RemoteRepository> rrs = settings.getRemoteRepositories();
        //Assert.assertEquals(4, rrs.size());
        //Assert.assertEquals(0, countMirrored(rrs));
        printHeader(rrs, settingsFile);
        int i = 1;
        for (RemoteRepository rc : settings.getRemoteRepositories()) {
            /*if(rc.getId().equals("internal")) {
                Assert.assertEquals("http://rds.dev:8180/archiva/repository/internal/", rc.getUrl());
                Assert.assertTrue(rc.supportsReleases());
                Assert.assertFalse(rc.supportsSnapshots());
            }
            if(rc.getId().equals("snapshots")) {
                Assert.assertEquals("http://rds.dev:8180/archiva/repository/snapshots/", rc.getUrl());
                Assert.assertFalse(rc.supportsReleases());
                Assert.assertTrue(rc.supportsSnapshots());
            }
            if(rc.getId().equals("real_repository")) {
                Assert.assertEquals("http://build01.dev.real.com:9999/repository/", rc.getUrl());
                Assert.assertTrue(rc.supportsReleases());
                Assert.assertTrue(rc.supportsSnapshots());
            }*/

            i = print(rc, i, rrs.size());
        }
        parser.parse(getMavenSettingsFile());
    }

    private void printHeader(List<RemoteRepository> rrs, File settings) {
        StringBuilder sb = new StringBuilder();
        sb.append("Settings file (").append(settings.getPath()).append("), ")
            .append("Repositories (").append(rrs.size()).append("), ")
            .append("Mirrored (").append(countMirrored(rrs)).append(")");
        int length = sb.length();
        sb.append("\n");
        for (int i = 0; i < length; i++)
            sb.append("-");
        System.out.println("\n" + sb.toString());
    }

    private int print(RemoteRepository r, int i, int total) {
        System.out.println("(" + i + ") id:        " + r.getId());
        System.out.println("    url:       " + r.getUrl());
        System.out.println("    releases:  " + r.supportsReleases());
        System.out.println("    snapshots: " + r.supportsSnapshots());
        i++;
        if (i <= total)
            System.out.println("");
        return i;
    }

    private int countMirrored(List<RemoteRepository> rrs) {
        int mirrored = 0;
        for (RemoteRepository rr : rrs) {
            if (rr.isMirrored())
                mirrored++;
        }
        return mirrored;
    }

    /**
     * Gets the user's maven settings file (global or in home directory
     *
     * @throws IOException If no readable maven settings file exists
     */
    private File getMavenSettingsFile() throws IOException {
        // If maven home specified, first try that, otherwise try user home dir
        String m2Home = System.getenv().get("M2_HOME");
        File globalConfig = m2Home != null ? new File(m2Home, "conf/settings.xml") : new File("does not exist");
        File userConfig = new File(new File(System.getProperty("user.home")), ".m2/settings.xml");
        if (userConfig.exists() && userConfig.canRead()) {
            return userConfig;
        } else if (globalConfig.exists() && globalConfig.canRead()) {
            return globalConfig;
        } else {
            throw new IOException("Unable to find a usable Maven settings file (global or in user home directory)");
        }
    }
}
