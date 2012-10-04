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
package org.rioproject.deploy;

import java.io.Serializable;
import java.net.URL;
import java.util.Date;

/**
 * The DownloadRecord stores attributes related to a download
 *
 * @author Dennis Reedy
 */
public class DownloadRecord implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * The location of the artifact
     */
    private URL location;
    /**
     * The canonical path in the file system where the file was downloaded
     */
    private String path;
    /**
     * The name of the downloaded file
     */
    private String name;
    /**
     * Whether or not to unarchive the downloaded software
     */
    private boolean unarchived;
    /**
     * When the artifact was downloaded
     */
    private Date downloadDate;
    /**
     * The size of the downloaded artifact
     */
    private int downloadedSize;
    /**
     * The size of the extracted artifact
     */
    private int extractedSize;
    /**
     * Where the file was extracted to
     */
    private String extractedPath;
    /**
     * The time (in millis) to perform the artifact
     */
    private long downloadTime;
    /**
     * The time (in millis) to perform the unarchive (extraction)
     */
    private long unarchiveTime;
    /**
     * Whether the parent directory was created
     */
    private boolean createdParentDirectory;

    /**
     * Create a DownloadRecord instance
     *
     * @param location The location of the artifact
     * @param path The canonical path on the file system where the file was
     * downloaded
     * @param name The name of the file
     * @param downloadDate When the artifact was downloaded
     * @param downloadedSize The size of the downloaded artifact
     * @param extractedSize The size of the extracted artifact
     * @param extractedPath Where the file was extracted to (null if not extracted)
     * @param unarchived Whether or not the downloaded artifact was unarchived
     * (extracted)
     * @param downloadTime The amount of time (in milliseconds) it took to
     * perform the download
     * @param unarchiveTime The amount of time (in milliseconds) it took to
     * perform the unarchive (extraction)
     */
    public DownloadRecord(URL location,
                          String path,
                          String name,
                          Date downloadDate,
                          int downloadedSize,
                          int extractedSize,
                          String extractedPath,
                          boolean unarchived,
                          long downloadTime,
                          long unarchiveTime) {
        if (location == null)
            throw new IllegalArgumentException("location is null");
        if (path == null)
            throw new IllegalArgumentException("path is null");
        if (name == null)
            throw new IllegalArgumentException("name is null");
        this.location = location;
        this.path = path;
        this.name = name;
        if (downloadDate != null)
            this.downloadDate = new Date(downloadDate.getTime());
        this.downloadedSize = downloadedSize;
        this.extractedSize = extractedSize;
        this.extractedPath = extractedPath;
        this.unarchived = unarchived;
        this.downloadTime = downloadTime;
        this.unarchiveTime = unarchiveTime;
    }

    /**
     * Get the location
     *
     * @return The originating location downloaded artifact
     */
    public URL getLocation() {
        return (location);
    }

    /**
     * Get the size of the downloaded artifact.
     *
     * @return The size of the downloaded artifact
     */
    public int getDownloadedSize() {
        return (downloadedSize);
    }

    /**
     * Get the size of the extracted artifact.
     *
     * @return The size of the extracted artifact. If the artifact has not been
     *         extracted this value will be zero
     */
    public int getExtractedSize() {
        return (extractedSize);
    }

    /**
     * Get the path to the downloaded file
     *
     * @return The canonical path in the file system where the file was
     *         downloaded
     */
    public String getPath() {
        return (path);
    }

    /**
     * Get the name of the downloaded file
     *
     * @return The name of the downloaded file
     */
    public String getName() {
        return (name);
    }

    /**
     * Get whether the downloaded artifact was unarchived
     *
     * @return Return true if the artifact was unarchived, false if not
     */
    public boolean unarchived() {
        return (unarchived);
    }

    /**
     * Get where the file was extracted to
     *
     * @return Where the file was extracted to. Will be null if the file was
     * not extracted
     */
    public String getExtractedPath() {
        return extractedPath;
    }

    /**
     * Get the date of the dowload
     *
     * @return The Date the artifact was downloaded.
     */
    public Date getDate() {
        Date d = null;
        if (downloadDate != null)
            d = new Date(downloadDate.getTime());
        return (d);
    }

    /**
     * Get the amount of time (in millis) it took to perform the download
     *
     * @return The amount of time (in millis) it took to perform the download
     */
    public long getDownloadTime() {
        return (downloadTime);
    }

    /**
     * Get the amount of time (in millis) it took to perform the unarchive
     * (extraction)
     *
     * @return The amount of time (in millis) it took to perform the unarchive
     *         (extraction)
     */
    public long getUnarchiveTime() {
        return (unarchiveTime);
    }

    /**
     * Get whether the parent directory was created
     *
     * @return true if the parent directory was created when downloading the
     * artifact
     */
    public boolean createdParentDirectory() {
        return createdParentDirectory;
    }

    /**
     * Set whether the parent directory was created
     *
     * @param createdParentDirectory True if the parent directory was created
     * when downloading theartifact
     */
    public void setCreatedParentDirectory(boolean createdParentDirectory) {
        this.createdParentDirectory = createdParentDirectory;
    }

    public String toString() {
        return ("Downloaded From : "
                + getLocation().toExternalForm()
                + "\n"
                + "Downloaded To   : "
                + getPath()
                + "\n"
                + "Downloaded File : "
                + getName()
                + "\n"
                + "Downloaded Size : "
                + getDownloadedSize()
                + "\n"
                + "Extracted  Size : "
                + getExtractedSize()
                + "\n"
                + "Extracted  To : "
                + getExtractedPath()
                + "\n"
                + "Download   Time : "
                + getDownloadTime()
                + "\n"
                + "Unarchive  Time : "
                + getUnarchiveTime()
                + "\n"
                + "Date downloaded : " + getDate());
    }
}
