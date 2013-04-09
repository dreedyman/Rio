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
package org.rioproject.resolver;

import java.io.Serializable;

/**
 * Representation of a remote repository.
 */
@SuppressWarnings("unused")
public class RemoteRepository implements Serializable {
    static final long serialVersionUID = 1L;
    private String id;
    private String name;
    private String url;
    private boolean snapshots = true;
    private boolean releases = true;
    private String releaseChecksumPolicy = CHECKSUM_POLICY_FAIL;
    private String snapshotChecksumPolicy = CHECKSUM_POLICY_FAIL;
    private String releaseUpdatePolicy = UPDATE_POLICY_DAILY;
    private String snapshotUpdatePolicy = UPDATE_POLICY_DAILY;
    private boolean mirrored;
    /**
     * Verify checksums and fail the resolution if they do not match.
     */
    public static final String CHECKSUM_POLICY_FAIL = "fail";

    /**
     * Verify checksums and warn if they do not match.
     */
    public static final String CHECKSUM_POLICY_WARN = "warn";

    /**
     * Do not verify checksums.
     */
    public static final String CHECKSUM_POLICY_IGNORE = "ignore";
    /**
     * Never update locally cached data.
     */
    public static final String UPDATE_POLICY_NEVER = "never";

    /**
     * Always update locally cached data.
     */
    public static final String UPDATE_POLICY_ALWAYS = "always";

    /**
     * Update locally cached data once a day.
     */
    public static final String UPDATE_POLICY_DAILY = "daily";

    /**
     * Update locally cached data every X minutes as given by "interval:X".
     */
    public static final String UPDATE_POLICY_INTERVAL = "interval";

    public boolean supportsSnapshots() {
        return snapshots;
    }

    public boolean supportsReleases() {
        return releases;
    }

    public boolean isMirrored() {
        return mirrored;
    }

    public void setMirrored(boolean mirrored) {
        this.mirrored = mirrored;
    }

    public void setSnapshots(boolean snapshots) {
        this.snapshots = snapshots;
    }

    public void setReleases(boolean releases) {
        this.releases = releases;
    }

    public String getReleaseUpdatePolicy() {
        return releaseUpdatePolicy;
    }

    public void setReleaseUpdatePolicy(String releaseUpdatePolicy) {
        this.releaseUpdatePolicy = releaseUpdatePolicy;
    }

    public String getSnapshotUpdatePolicy() {
        return snapshotUpdatePolicy;
    }

    public void setSnapshotUpdatePolicy(String snapshotUpdatePolicy) {
        this.snapshotUpdatePolicy = snapshotUpdatePolicy;
    }

    public String getSnapshotChecksumPolicy() {
        return snapshotChecksumPolicy;
    }

    public void setSnapshotChecksumPolicy(String snapshotChecksumPolicy) {
        this.snapshotChecksumPolicy = snapshotChecksumPolicy;
    }

    public String getReleaseChecksumPolicy() {
        return releaseChecksumPolicy;
    }

    public void setReleaseChecksumPolicy(String releaseChecksumPolicy) {
        this.releaseChecksumPolicy = releaseChecksumPolicy;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override public String toString() {
        return "RemoteRepository{" +
               "id='" + id + '\'' +
               ", name='" + name + '\'' +
               ", url='" + url + '\'' +
               ", snapshots=" + snapshots +
               ", releases=" + releases +
               ", releaseChecksumPolicy='" + releaseChecksumPolicy + '\'' +
               ", snapshotChecksumPolicy='" + snapshotChecksumPolicy + '\'' +
               ", releaseUpdatePolicy='" + releaseUpdatePolicy + '\'' +
               ", snapshotUpdatePolicy='" + snapshotUpdatePolicy + '\'' +
               ", mirrored=" + mirrored +
               '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        RemoteRepository that = (RemoteRepository) o;

        if (id != null ? !id.equals(that.id) : that.id != null)
            return false;
        if (url != null ? !url.equals(that.url) : that.url != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (url != null ? url.hashCode() : 0);
        return result;
    }
}
