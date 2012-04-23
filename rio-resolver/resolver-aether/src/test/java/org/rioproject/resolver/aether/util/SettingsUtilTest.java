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
package org.rioproject.resolver.aether.util;

import junit.framework.Assert;
import org.apache.maven.settings.Settings;
import org.junit.Test;

/**
 * Test SettingsUtil
 *
 * @author Dennis Reedy
 */
public class SettingsUtilTest {
    @Test
    public void testGetSettings() throws Exception {
        Settings settings = SettingsUtil.getSettings();
        Assert.assertNotNull(settings);
    }

    @Test
    public void testGetLocalRepositoryLocation() throws Exception {
        Settings settings = SettingsUtil.getSettings();
        String localRepositoryLocation = SettingsUtil.getLocalRepositoryLocation(settings);
        Assert.assertNotNull(localRepositoryLocation);
    }
}
