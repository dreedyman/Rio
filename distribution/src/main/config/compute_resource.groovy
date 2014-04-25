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

/*
 * Configuration for a ComputeResource
 */

import net.jini.config.Configuration
import org.rioproject.config.Component
import org.rioproject.impl.system.OperatingSystemType
import org.rioproject.impl.system.measurable.MeasurableMonitor
import org.rioproject.impl.system.measurable.memory.MemInfoMonitor
import org.rioproject.impl.system.measurable.memory.SystemMemoryMonitor
import org.rioproject.impl.system.measurable.memory.pool.MemoryPool
import org.rioproject.watch.ThresholdValues

import java.lang.management.ManagementFactory
import java.lang.management.MemoryPoolMXBean
import java.lang.management.MemoryType

/*
 * Declare ComputeResource properties
 */
@Component('org.rioproject.system')
class ComputeResourceConfig {

    /* The reportInterval property controls how often the compute resource
     * will inform registered ResourceCapabilityChangeListeners with an update to
     * the @link ResourceCapability.
     * */
    long getReportInterval() {
        return 60000
    }

    /*
     * Defines the upper limit of the mean of all depletion oriented resources.
     * We choose the number of processors as the upper limit because CPU
     * utilization can be 100% for each processor.
     *
     * If this threshold is crossed, an SLAThresholdEvent is sent by a Cybernode.
     * Additionally services may declare a utilization SLA as part of their
     * service requirements. If this value is breached, a service may choose to
     * relocate to a different host, or simply not be allocated to this machine.
     */
    double getSystemThreshold() {
        return Runtime.getRuntime().availableProcessors()
    }
}

/*
 * Base class for all measurable capabilities. We declare the reportRate
 * property, sampleSize and thresholdValues here. Components below simply
 * extend this class if they need to override these properties.
 */
class BasicMeasurable {
    /* Report every 5 seconds */
    long getReportRate() {
        return 5000
    }

    /* Include a single metric in the set of samples used to produce a result */
    int getSampleSize() {
        return 1
    }

    /* Maintain a collection size of 50 calculables */
    int getCollectionSize() {
        return 50
    }

    /*
     * Low threshold of 0, high threshold of 1 (100%)
     */
    ThresholdValues getThresholdValues() {
        return new ThresholdValues(0.0, 1.0)
    }
}

/*
 * Configuration for the physical machine CPU measurable capability. This
 * configuration overrides methods in the BasicMeasurable class to customize
 * the setting for threshold values.
 */
@Component('org.rioproject.system.measurable.cpu')
class MeasurableCPU extends BasicMeasurable {
    /*
     * High threshold is the number of CPUs on the system
     */
    @Override
    ThresholdValues getThresholdValues() {
        int numCPUs = Runtime.getRuntime().availableProcessors()
        return new ThresholdValues(0.0, numCPUs);
    }

}

/*
 * Configuration for the JVM's CPU measurable capability. This configuration
 * overrides methods in the BasicMeasurable class to customize the setting for
 * threshold values.
 */
@Component('org.rioproject.system.measurable.cpu.jvm')
class MeasurableJVMCPU extends BasicMeasurable {
    /*
     * High threshold for JVM CPU utilization is the number of CPUs on the system
     */
    @Override
    ThresholdValues getThresholdValues() {
        int numCPUs = Runtime.getRuntime().availableProcessors()
        return new ThresholdValues(0.0, numCPUs);
    }
}

/*
 * Configuration for the Memory measurable capability. This
 * configuration overrides methods in the BasicMeasurable class to customize
 * the setting for threshold values.
 */
@Component('org.rioproject.system.measurable.memory')
class MeasurableMemory extends BasicMeasurable {
    /*
     * Memory utilization should be capped at 95%
     */
    @Override
    ThresholdValues getThresholdValues() {
        return new ThresholdValues(0.0, 0.80);
    }
}

/*
 * Configuration for the SystemMemory measurable capability. It just extends
 * BasicMeasurable, allowing it to have the same default values.
 */
@Component('org.rioproject.system.measurable.systemMemory')
class MeasurableSystemMemory extends BasicMeasurable {

    MeasurableMonitor getMonitor() {
        if(OperatingSystemType.isLinux())
            return new MemInfoMonitor()
        else
            return new SystemMemoryMonitor();
    }

    /*
     * Memory utilization should be capped at 99%
     */
    @Override
    ThresholdValues getThresholdValues() {
        return new ThresholdValues(0.0, 0.99);
    }
}

/*
 * Configuration for the DiskSpace measurable capability. It just extends
 * BasicMeasurable, allowing it to have the same default values.
 */
@Component('org.rioproject.system.measurable.disk')
class MeasurableDiskSpace extends BasicMeasurable {    }

@Component('org.rioproject.system.memory.pool')
class MemoryPools extends BasicMeasurable {
    MemoryPool[] getMemoryPools(Configuration config) {
        def memoryPools = []
        for(MemoryPoolMXBean mBean : ManagementFactory.getMemoryPoolMXBeans()) {
            if(mBean.name.contains("Perm Gen"))
                memoryPools << new MemoryPool(mBean.name, config, new ThresholdValues(0.0, 0.80))
            if(mBean.getType()==MemoryType.HEAP && mBean.isUsageThresholdSupported()) {
                memoryPools << new MemoryPool(mBean.name, config, new ThresholdValues(0.0, 0.80))
            }
        }
        return memoryPools as MemoryPool[]
    }

}