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
package org.rioproject.impl.system.measurable.disk;

import org.rioproject.impl.system.measurable.MeasurableMonitor;
import org.rioproject.system.measurable.disk.DiskSpaceUtilization;
import org.rioproject.watch.ThresholdValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.FileStore;
import java.nio.file.Files;

/**
 * The <code>DiskSpaceMonitor</code> monitors disk space usage. This class
 * uses {@link java.nio.file.FileStore} to obtain it's results.
 *
 * @author Dennis Reedy
 */
public class DiskSpaceMonitor implements MeasurableMonitor<DiskSpaceUtilization> {
    private static Logger logger = LoggerFactory.getLogger("org.rioproject.system.measurable.disk");
    private String id;
    private ThresholdValues tVals;
    private String fileSystem = File.separator;

    public void setID(String id) {
        this.id = id;
    }

    public void setThresholdValues(ThresholdValues tVals) {
        this.tVals = tVals;
    }

    public void setFileSystemToMonitor(String fileSystem) {
        this.fileSystem = fileSystem;
    }
    
    public DiskSpaceUtilization getMeasuredResource() {
        double used;
        double available;
        DiskSpaceUtilization diskSpaceUtilization;
        try {
            FileStore fileStore = Files.getFileStore(new File(fileSystem).toPath());
            double total = fileStore.getTotalSpace();
            available = fileStore.getUsableSpace();
            used = total - available;
            double u = used / total;
            BigDecimal bd = new BigDecimal(u).setScale(2, RoundingMode.HALF_EVEN);
            double utilization = bd.doubleValue();
            diskSpaceUtilization =  new DiskSpaceUtilization(id, used, available, total, utilization, tVals);
        } catch (IOException e) {
            logger.error("Failed getting disk space utilization", e);
            diskSpaceUtilization = new DiskSpaceUtilization(id, 0, tVals);
        }
        return diskSpaceUtilization;
    }

    public void terminate() {
        // do nothing
        ;
    }
}
