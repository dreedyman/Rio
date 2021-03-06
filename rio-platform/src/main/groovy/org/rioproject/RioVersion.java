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
package org.rioproject;

import org.rioproject.util.RioManifest;
import java.net.URL;

/**
 * Version and build number for the current release of Rio
 *
 * @author Dennis Reedy
 */
public final class RioVersion {
    private static final RioManifest MANIFEST = loadManifest();
    public static final String VERSION = MANIFEST.getRioVersion();

    private RioVersion() {}

    /**
     * Get the build number.
     *
     * @return The build number for the current release of Rio.
     */
    public static String getBuildNumber() {
        return MANIFEST.getRioBuild();
    }

    private static RioManifest loadManifest() {
        URL implUrl = RioVersion.class.getProtectionDomain().getCodeSource().getLocation();
        return new RioManifest(implUrl);
    }
}
