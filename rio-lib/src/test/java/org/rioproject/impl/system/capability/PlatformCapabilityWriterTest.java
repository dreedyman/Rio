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
package org.rioproject.impl.system.capability;

import org.junit.Assert;
import org.junit.Test;
import org.rioproject.system.capability.PlatformCapability;

import java.io.File;

/**
 * @author Dennis Reedy
 */
public class PlatformCapabilityWriterTest {

    @Test
    public void testWrite() throws Exception {
        String cwd = System.getProperty("user.dir");
        File target = new File(cwd, "build");
        File platform = new File(target, "platform-file");
        platform.mkdirs();
        PlatformCapability pCap = new PlatformCapability();
        pCap.define(PlatformCapability.NAME, "Foo");
        pCap.define(PlatformCapability.VERSION, "1.0");
        pCap.setClassPath(new String[]{cwd+File.separator+"build"+File.separator+"classes"+File.separator});
        String fileName = PlatformCapabilityWriter.write(pCap, platform.getPath());
        Assert.assertNotNull(fileName);
        PlatformLoader platformLoader = new PlatformLoader();
        PlatformCapabilityConfig[] pCapConfigs = platformLoader.parsePlatform(platform.getPath());
        Assert.assertNotNull(pCapConfigs);
        Assert.assertEquals(1, pCapConfigs.length);

    }
}
