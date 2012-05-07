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

import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.DefaultSettingsBuilder;
import org.apache.maven.settings.building.DefaultSettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuildingException;
import org.apache.maven.settings.building.SettingsBuildingResult;
import org.apache.maven.settings.io.DefaultSettingsReader;
import org.apache.maven.settings.io.DefaultSettingsWriter;
import org.apache.maven.settings.validation.DefaultSettingsValidator;

import java.io.File;

/**
 * A utility for loading Maven settings.
 *
 * @author Dennis Reedy
 */
public final class SettingsUtil {
    private SettingsUtil() {}

    /**
     * Get the Maven {@code Settings}
     *
     * @return Maven {@code Settings}
     *
     * @throws SettingsBuildingException If there are problems loading the settings.
     */
    public static Settings getSettings() throws SettingsBuildingException {
        DefaultSettingsBuilder defaultSettingsBuilder = new DefaultSettingsBuilder();
        DefaultSettingsBuildingRequest request = new DefaultSettingsBuildingRequest();
        File userSettingsFile = new File(System.getProperty("user.home"), ".m2" + File.separator + "settings.xml");
        request.setUserSettingsFile(userSettingsFile);
        defaultSettingsBuilder.setSettingsWriter(new DefaultSettingsWriter());
        defaultSettingsBuilder.setSettingsReader(new DefaultSettingsReader());
        defaultSettingsBuilder.setSettingsValidator(new DefaultSettingsValidator());
        SettingsBuildingResult build = defaultSettingsBuilder.build(request);
        return build.getEffectiveSettings();
    }

    /**
     * Determine the local repository path, honoring any custom setting in the user's maven settings.xml.
     * Defaults to <code>${user.home}/.m2/repository</code> (which is the maven default).
     *
     * @param settings The Maven Settings to access, must not be {@code null}
     *
     * @return The location of the local repository
     *
     * @throws IllegalArgumentException if the {@code settings} is {@code null}
     */
    public static String getLocalRepositoryLocation(final Settings settings) {
        if(settings==null)
            throw new IllegalArgumentException("settings must not be null");
        String localRepositoryLocation = settings.getLocalRepository();
        if (localRepositoryLocation == null) {
            StringBuilder locationBuilder = new StringBuilder();
            locationBuilder.append(System.getProperty("user.home")).append(File.separator);
            locationBuilder.append(".m2").append(File.separator);
            locationBuilder.append("repository");
            localRepositoryLocation = locationBuilder.toString();
        }
        return localRepositoryLocation;
    }
}
