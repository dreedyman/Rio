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
package org.rioproject.core.provision;

import org.rioproject.util.PropertyHelper;

import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * The StagedData class defines the attributes needed to download and stage
 * data (and artifact typically software or data)
 *
 * @author Dennis Reedy
 */
public class StagedData implements Serializable {
    static final long serialVersionUID = 1L;
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
     * Optional target file name. If not set the target file name will be the
     * same as the source file name
     */
    //private String targetFileName;

    /**
     * Create a StagedData instance
     *
     * @param location The location to download from, including the source file
     * @param installRoot The directory for installation
     * @param unarchive Whether to unarchive the download
     */
    public StagedData(String location,
                      String installRoot,
                      boolean unarchive) {
        this(location, installRoot, unarchive, true, true, null);
    }

    /**
     * Create a StagedData instance
     *
     * @param location The location to download from, including the source file
     * @param installRoot The directory for installation
     * @param unarchive Whether to unarchive the download
     * @param removeOnDestroy True if the artifact should be removed on
     * ServiceBean termination, false if not
     */
    public StagedData(String location,
                      String installRoot,
                      boolean unarchive,
                      boolean removeOnDestroy) {
        this(location, installRoot, unarchive, removeOnDestroy, true, null);
    }

    /**
     * Create a StagedData instance
     *
     * @param location The location to download from, including the source file
     * @param installRoot The directory for installation
     * @param unarchive Whether to unarchive the download
     * @param removeOnDestroy True if the artifact should be removed on
     * ServiceBean termination, false if not
     * @param overwrite True to overwrite a file at the target download
     * location with the same name
     */
    public StagedData(String location,
                      String installRoot,
                      boolean unarchive,
                      boolean removeOnDestroy,
                      boolean overwrite) {
        this(location, installRoot, unarchive, removeOnDestroy, overwrite, null);
    }

    /**
     * Create a StagedData instance
     *
     * @param location The location to download from, including the source file
     * @param installRoot The directory for installation
     * @param unarchive Whether to unarchive the download
     * @param removeOnDestroy True if the artifact should be removed on
     * ServiceBean termination, false if not
     * @param overwrite True to overwrite a file at the target download
     * location with the same name
     * @param perms Optional permissions to set on the staged data. The
     * permissions need to be in the form of what the <tt>chmod</tt> command
     * uses
     */
    public StagedData(String location,
                      String installRoot,
                      boolean unarchive,
                      boolean removeOnDestroy,
                      boolean overwrite,
                      String perms) {
        if (location == null)
            throw new IllegalArgumentException("location is null");
        if (installRoot == null)
            throw new IllegalArgumentException("installRoot is null");
        this.location = location;
        this.installRoot = installRoot;
        this.unarchive = unarchive;
        this.removeOnDestroy = removeOnDestroy;
        this.overwrite = overwrite;
        this.perms = perms;
    }

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
            if(location.indexOf(PropertyHelper.PARSETIME[0])!=-1 ||
               location.indexOf(PropertyHelper.RUNTIME[0])!=-1) {
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
