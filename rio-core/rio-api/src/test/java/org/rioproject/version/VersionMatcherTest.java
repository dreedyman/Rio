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
package org.rioproject.version;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Dennis Reedy
 */
public class VersionMatcherTest {

    @Test
    public void testExactVersionSupported() throws Exception {
        VersionMatcher versionMatcher = new VersionMatcher();
        Assert.assertTrue(versionMatcher.versionSupported("1.2.7", "1.2.7"));
    }

    @Test
    public void testExactVersionNotSupported() throws Exception {
        VersionMatcher versionMatcher = new VersionMatcher();
        Assert.assertFalse(versionMatcher.versionSupported("1.2", "1.2.7"));
    }

    @Test
    public void testMinorVersionSupported() throws Exception {
        VersionMatcher versionMatcher = new VersionMatcher();
        Assert.assertTrue(versionMatcher.versionSupported("1.2*", "1.2.7"));
        Assert.assertTrue(versionMatcher.versionSupported("1*", "1.2.7"));
        Assert.assertTrue(versionMatcher.versionSupported("1*", "1-M"));
        Assert.assertFalse(versionMatcher.versionSupported("1*", "2"));
    }

    @Test
    public void testMinorVersionSupported2() throws Exception {
        VersionMatcher versionMatcher = new VersionMatcher();
        Assert.assertFalse(versionMatcher.versionSupported("1*", "2"));
    }

    @Test
    public void testMinorVersionNotSupported() throws Exception {
        VersionMatcher versionMatcher = new VersionMatcher();
        Assert.assertFalse(versionMatcher.versionSupported("1.2*", "1.3-7"));
        Assert.assertFalse(versionMatcher.versionSupported("2*", "1.3.7"));
    }

    @Test
    public void testMajorVersionSupported() throws Exception {
        VersionMatcher versionMatcher = new VersionMatcher();
        Assert.assertTrue(versionMatcher.versionSupported("1.2+", "1.3.7"));
        Assert.assertTrue(versionMatcher.versionSupported("2+", "3.7"));
        Assert.assertTrue(versionMatcher.versionSupported("2+", "2"));
        Assert.assertTrue(versionMatcher.versionSupported("2+", "2-SNAPSHOT"));
    }

    @Test
    public void testVersionRanges() {
        VersionMatcher versionMatcher = new VersionMatcher();
        Assert.assertFalse(versionMatcher.versionSupported("3.24.1", "3.23.13.4, 3.24"));
        Assert.assertFalse(versionMatcher.versionSupported("3.23.1", "3.23.13.4, 3.24"));
        Assert.assertTrue(versionMatcher.versionSupported("3.23.1", "3.23, 3.24"));
        Assert.assertTrue(versionMatcher.versionSupported("3.*", "3.23, 4.0"));
        Assert.assertFalse(versionMatcher.versionSupported("3.1*", "3.23, 4.0"));
        Assert.assertTrue(versionMatcher.versionSupported("4+", "3.23, 4.1"));
        String s = "SNAPSHOT";
        String m2 = "M2";
        String m3 = "M3";
        System.out.println("===> 1<2? "+("1".compareToIgnoreCase("2"))+", M2<M3? "+m2.compareToIgnoreCase(m3)+", M3>M2? "+m3.compareToIgnoreCase(m2)+", SNAPSHOT vs M3? "+s.compareToIgnoreCase(m3));
    }

}
