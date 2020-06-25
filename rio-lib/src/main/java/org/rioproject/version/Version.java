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

/**
 * Represents a version.
 *
 * @author Dennis Reedy
 */
public class Version {
    private final String version;

    public Version(final String version) {
        this.version = version;
    }

    public boolean isRange() {
        return version.contains(",");
    }

    public String getVersion() {
        if(minorVersionSupport() || majorVersionSupport()) {
            return version.substring(0, version.length()-1).trim();
        }
        return version.trim();
    }

    public String getStartRange() {
        String[] parts = version.split(",", 2);
        return parts[0].trim();
    }

    public String getEndRange() {
        if(!isRange())
            return getVersion();
        String[] parts = version.split(",", 2);
        return parts[1].trim();
    }

    public boolean minorVersionSupport() {
        return version.endsWith("*");
    }

    public boolean majorVersionSupport() {
        return  version.endsWith("+");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Version version1 = (Version) o;
        return version.equals(version1.version);

    }

    @Override
    public int hashCode() {
        return version.hashCode();
    }
}
