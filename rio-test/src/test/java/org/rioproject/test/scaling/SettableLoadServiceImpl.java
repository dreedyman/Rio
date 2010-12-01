/*
 * Copyright 2009 the original author or authors.
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
package org.rioproject.test.scaling;

import org.rioproject.core.jsb.ServiceBeanContext;
import org.rioproject.jsb.ServiceBeanAdapter;
import org.rioproject.watch.GaugeWatch;


/**
 * Implements a service for which a client can change the load
 * programmatically.
 */
public class SettableLoadServiceImpl extends ServiceBeanAdapter
    implements SettableLoadService {

    /**
     * The load watch.
     */
    private GaugeWatch loadWatch;

    /**
     * Is overridden to setup the load watch.
     */
    public void initialize(ServiceBeanContext context) throws Exception {
        super.initialize(context);
        loadWatch = new GaugeWatch("load");
        getWatchRegistry().register(loadWatch);
        loadWatch.addValue(0);
    }

    /**
     * Sets the service load.
     *
     * @param load the load.
     */
    public void setLoad(double load) {
        try {
            double last = loadWatch.getLastCalculableValue();
            loadWatch.addValue(load);
            boolean verified = loadWatch.getLastCalculableValue() == load;
            if (!verified)
                System.err.println(
                    "---> was [" + loadWatch.getLastCalculableValue() +
                    "], SET FAILED [" + load + "] " +
                    "breached=" +
                    loadWatch.getThresholdManager().getThresholdCrossed());
            else
                System.err.println(
                    "---> was [" + last + "], added [" + load + "] " +
                    "breached=" +
                    loadWatch.getThresholdManager().getThresholdCrossed());
        } catch (Throwable t) {

        }
    }
}
