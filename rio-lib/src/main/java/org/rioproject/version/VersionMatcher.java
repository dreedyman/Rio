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

import java.io.Serializable;

/**
 * The {@code VersionMatcher} is a utility that determines whether a required version can be met by a published version.
 * 
 * </br>
 * Version matching is done as follows: &nbsp; <br>
 * <br>
 * <table cellpadding="2" cellspacing="2" border="1"
 * style="text-align: left; width: 100%;">
 * <tbody>
 * <tr>
 * <th style="vertical-align: top;">Requirement<br>
 * </th>
 * <th style="vertical-align: top;">Support Criteria<br>
 * </th>
 * </tr>
 * <tr>
 * <td style="vertical-align: top;">1.2.7<br>
 * </td>
 * <td style="vertical-align: top;">Specifies an exact version<br>
 * </td>
 * </tr>
 * <tr>
 * <td style="vertical-align: top;">2*<br>
 * </td>
 * <td style="vertical-align: top;">Supported for all minor versions of 2 <br>
 * </td>
 * </tr>
 * <tr>
 * <td style="vertical-align: top;">3.4*<br>
 * </td>
 * <td style="vertical-align: top;">Supported for all minor versions of 3.4, including 3.4<br>
 * </td>
 * </tr>
 * <tr>
 * <td style="vertical-align: top;">1.2+<br>
 * </td>
 * <td style="vertical-align: top;">Supported for version 1.2 or above</td>
 * </tr>
 * </tbody>
 * </table>
 * </br>
 *
 * <p>
 * Version requirements are expected to be a "." or "-" separated String of integers. Character values are ignored.
 * For example a version declaration of "2.0-M3" will be processed as "2.0-03"
 * </p>
 * @author Dennis Reedy
 */
public class VersionMatcher implements Serializable {
    private static final long serialVersionUID = 1l;
    
    /**
     * Determine if versions are supported
     *
     * @param requiredVersion The required version, specified as a string. Must not be {@code null}.
     * @param publishedVersion  The published version value. Must not be {@code null}.
     *
     * @return {@code true} if, and only if, the request version can be supported
     *
     * @throws IllegalArgumentException if either of the parameters are {@code null}.
     */
    public boolean versionSupported(final String requiredVersion, final String publishedVersion) {
        if(requiredVersion == null)
            throw new IllegalArgumentException("requiredVersion is null");
        if(publishedVersion == null)
            throw new IllegalArgumentException("publishedVersion is null");
        boolean supported;
        Version versionRequired = new Version(requiredVersion);
        Version versionBeingTested = new Version(publishedVersion);
        if(versionRequired.minorVersionSupport() || versionRequired.majorVersionSupport()) {
            boolean minorVersionSupport = versionRequired.minorVersionSupport();
            int[] required;
            int[] configured;
            boolean isRange = versionBeingTested.isRange();
            try {
                required = toIntArray(versionRequired.getVersion().split("\\D"));
                configured = toIntArray(versionBeingTested.getVersion().split("\\D"));
            } catch(NumberFormatException e) {
                return (false);
            }
            if(required.length == 0)
                return (true);

            if(minorVersionSupport) {
                supported = checkMinorVersion(required, configured);
            } else {
                supported = checkMajorVersion(required, configured);
            }
            if(!supported && isRange) {
                try {
                    configured = toIntArray(versionBeingTested.getEndRange().split("\\D"));
                } catch(NumberFormatException e) {
                    return (false);
                }
                if(minorVersionSupport) {
                    supported = checkMinorVersion(required, configured);
                } else {
                    supported = checkMajorVersion(required, configured);
                }
            }
        } else {
            if(versionBeingTested.isRange()) {
                supported = withinRange(versionBeingTested, versionRequired);
            } else {
                supported = versionBeingTested.equals(versionRequired);
            }
        }
        return supported;
    }

    private boolean withinRange(Version publishedVersion, Version requiredVersion) {
        int[] startRange = toIntArray(publishedVersion.getStartRange().split("\\D"));
        int[] endRange = toIntArray(publishedVersion.getEndRange().split("\\D"));
        int[] version = toIntArray(requiredVersion.getVersion().split("\\D"));
        boolean startRangePassed = false;
        boolean same = false;
        for(int i=0;;i++) {
            if(i>=startRange.length) {
                startRangePassed = true;
                break;
            }
            if(i>=version.length) {
                startRangePassed = !same;
                break;
            }
            if(version[i]>startRange[i]) {
                startRangePassed = true;
                break;
            }
            if(version[i]<startRange[i]) {
                break;
            }
            same = version[i]==startRange[i];
        }
        boolean endRangePassed = false;
        if(startRangePassed) {
            same = false;
            for(int i=0;;i++) {
                if(i>=endRange.length) {
                    endRangePassed = !(i<version.length && same);
                    break;
                }
                if(version[i]<endRange[i]) {
                    endRangePassed = true;
                    break;
                }
                if(version[i]>endRange[i]) {
                    break;
                }
                same = version[i]==endRange[i];
            }
        }
        return startRangePassed&&endRangePassed;
    }

    private boolean checkMinorVersion(int[] required, int[] configured) {
        boolean supported = true;
        for(int i = 0; i < required.length; i++) {
            if(configured.length >= (i+1)) {
                if(configured[i] != required[i]) {
                    supported = false;
                    break;
                }
            }
        }
        return supported;
    }

    private boolean checkMajorVersion(int[] required, int[] configured) {
        boolean supported = true;
        for(int i = 0; i < required.length; i++) {
            if(configured.length >= (i+1)) {
                if(configured[i] > required[i]) {
                    break;
                }
                if(configured[i] == required[i]) {
                    continue;
                }
                if(configured[i] < required[i]) {
                    supported = false;
                    break;
                }
            }
        }
        return supported;
    }

    /**
     * Convert a String array into an int array
     * @param a String array
     *
     * @return An int array
     *
     * @throws NumberFormatException if the value in the array is not a number
     */
    private int[] toIntArray(final String[] a) throws NumberFormatException {
        int[] array = new int[a.length];
        for(int i = 0; i < array.length; i++) {
            if(a[i].length() == 0) {
                array[i] = Integer.parseInt("0");
            } else {
                array[i] = Integer.parseInt(a[i]);
            }
        }
        return (array);
    }
}
