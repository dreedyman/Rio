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
package org.rioproject.version;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Dennis Reedy
 */
public class VersionTest {

    @Test
    public void testIsRange() throws Exception {
        Version v = new Version("1,2");
        Assert.assertTrue(v.isRange());
        Version v1 = new Version("1.2");
        Assert.assertFalse(v1.isRange());
    }

    @Test
    public void testGetVersion() throws Exception {
        Version v = new Version("1,2");
        Assert.assertNotNull(v.getVersion());
    }

    @Test
    public void testGetStartRange() throws Exception {
        Version v = new Version("1,2");
        Assert.assertNotNull(v.getStartRange());
        Assert.assertEquals("1", v.getStartRange());
        Version v1 = new Version("1.2");
        Assert.assertNotNull(v1.getStartRange());
    }

    @Test
    public void testGetEndRange() throws Exception {
        Version v = new Version("1,2");
        Assert.assertNotNull(v.getEndRange());
        Assert.assertEquals("2", v.getEndRange());
        Version v1 = new Version("1.2");
        Assert.assertNotNull(v1.getEndRange());
    }

    @Test
    public void testMinorVersionSupport() throws Exception {
        Version v = new Version("1.1*");
        Assert.assertTrue(v.minorVersionSupport());
        Version v1 = new Version("1.2");
        Assert.assertFalse(v1.minorVersionSupport());
    }

    @Test
    public void testMajorVersionSupport() throws Exception {
        Version v = new Version("1+");
        Assert.assertTrue(v.majorVersionSupport());
        Version v1 = new Version("1.2");
        Assert.assertFalse(v1.majorVersionSupport());
    }

    @Test
    public void testEquals() throws Exception {
        Version v = new Version("1.2");
        Version v1 = new Version("1.2");
        Assert.assertTrue(v.equals(v1));
    }
}
