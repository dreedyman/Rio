/*
 * Copyright 2008 the original author or authors.
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
package org.rioproject.deploy;

import org.rioproject.util.PropertyHelper;

import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * The StagedData class defines the attributes needed to download and stage
 * data (an artifact typically software or data)
 *
 * @author Dennis Reedy
 */
public class StagedData implements Serializable {
    static final long serialVersionUID = 2L;
    /**
     * The location to download from
     */
    private String location;
    /**
     * The URL for the location, lazily created
     */
    private URL locationURL;
    /**
     * The directory for installation. If the directory name does not begin with
     * a '/', the installRoot directory is a relative directory, and will be
     * appended to the directory determined by the downloading entity
     */
    private String installRoot;
    /**
     * Whether to unarchive the download
     */
    private boolean unarchive;
    /**
     * Whether to remove the downloaded artifact when the ServiceBean terminates
     */
    private boolean removeOnDestroy = true;
    /**
     * Whether to overwrite a file at the target download location with the
     * same name
     */
    private boolean overwrite;
    /**
     * Optional permissions to set on the staged data.
     */
    private String perms;

    /**
     * Get the download size
     *
     * @return The size of the download
     *
     * @throws java.io.IOException If there are errors accessing the location
     */
    public int getDownloadSize() throws IOException {
        getLocationURL();
        return (locationURL.openConnection().getContentLength());
    }

    /**
     * Get the download location
     *
     * @return The location of the download
     *
     * @throws MalformedURLException if the source locatiion <tt>URL</tt>
     * cannot be created
     */
    public URL getLocationURL() throws MalformedURLException {
        if(locationURL==null) {
            if(location.contains(PropertyHelper.PARSETIME[0]) ||
               location.contains(PropertyHelper.RUNTIME[0])) {
                location = PropertyHelper.expandProperties(location, PropertyHelper.PARSETIME);
                location = PropertyHelper.expandProperties(location, PropertyHelper.RUNTIME);
            }
            locationURL = new URL(location);
        }
        return (locationURL);
    }

    /**
     * Get the download location
     *
     * @return The location of the download
     */
    public String getLocation() {
        return (location);
    }

    public void setLocation(String location) {
        if (location == null)
            throw new IllegalArgumentException("location is null");
        this.location = location;
    }

    /**
     * Get the installation root
     *
     * @return The directory for installation. If the directory name does not
     *         begin with a '/', the installRoot directory is a relative
     *         directory, and will be appended to the directory determined by
     *         the downloading entity
     */
    public String getInstallRoot() {
        return (installRoot);
    }

    public void setInstallRoot(String installRoot) {
        if (installRoot == null)
            throw new IllegalArgumentException("installRoot is null");
        this.installRoot = installRoot;
    }

    public void setUnarchive(boolean unarchive) {
        this.unarchive = unarchive;
    }

    public void setRemoveOnDestroy(boolean removeOnDestroy) {
        this.removeOnDestroy = removeOnDestroy;
    }

    public void setOverwrite(boolean overwrite) {
        this.overwrite = overwrite;
    }

    public void setPerms(String perms) {
        this.perms = perms;
    }

    /**
     * Get whether to unarchive the download
     *
     * @return True if the download should be unarchived, false if not
     */
    public boolean unarchive() {
        return (unarchive);
    }

    /**
     * Get whether to remove the artifact on ServiceBean termination
     *
     * @return True if the artifact should be removed on ServiceBean
     *         termination, false if not
     */
    public boolean removeOnDestroy() {
        return (removeOnDestroy);
    }

    /**
     * Get whether to overwrite a file
     *
     * @return Whether to overwrite a file at the target download location
     * with the same name
     */
    public boolean overwrite() {
        return overwrite;
    }

    /**
     * File permissions to set on the downloaded data. If the downloaded data
     * is extracted, this permission string will be applied recursively to the
     * extracted directory structure
     *
     * @return Permissions to set on the staged data. The
     * permissions need to be in the form of what the <tt>chmod</tt> command
     * uses
     */
    public String getPerms() {
        return perms;
    }


    public String toString() {
        return "StagedData{" +
               "location=" + location +
               ", installRoot='" + installRoot + '\'' +
               ", unarchive=" + unarchive +
               ", removeOnDestroy=" + removeOnDestroy +
               ", overwrite=" + overwrite +
               ", perms='" + perms + '\'' +
               '}';
    }
}
