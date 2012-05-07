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
package org.rioproject.gnostic;

import org.rioproject.opstring.OperationalStringManager;
import org.rioproject.opstring.ServiceElement;
import org.rioproject.sla.SLA;
import org.rioproject.watch.WatchDataSource;

import java.util.HashMap;
import java.util.Map;

/**
 * Provides context for service information.
 */
public class ServiceHandle {
    ServiceElement elem;
    OperationalStringManager opMgr;
    Map<String, WatchDataSource> watchMap = new HashMap<String, WatchDataSource>();
    Map<String, SLA> slaMap = new HashMap<String, SLA>();

    ServiceElement getElem() {
        return elem;
    }

    void setElem(ServiceElement elem) {
        this.elem = elem;
    }

    OperationalStringManager getOpMgr() {
        return opMgr;
    }

    void setOpMgr(OperationalStringManager opMgr) {
        this.opMgr = opMgr;
    }

    void addToWatchMap(String id, WatchDataSource wds) {
        watchMap.put(id, wds);
    }

    Map<String, WatchDataSource> getWatchMap() {
        return watchMap;
    }

    void addToSLAMap(String id, SLA sla) {
        slaMap.put(id, sla);
    }

    Map<String, SLA> getSLAMap() {
        return slaMap;
    }

}
