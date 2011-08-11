package org.rioproject.gnostic.test;

import org.rioproject.core.jsb.ServiceBeanContext;
import org.rioproject.watch.CounterWatch;

/**
 * Test service implementation
 */
public class ExecutionNodeServiceImpl implements ExecutionNodeService {
    private CounterWatch kSessionCounter;
    private Long instanceID;

    public void setServiceBeanContext(ServiceBeanContext context) {
        instanceID = context.getServiceBeanConfig().getInstanceID();
        //loadWatch = new GaugeWatch("load");
        kSessionCounter = new CounterWatch("kSessionCounter");
        // context.getWatchRegistry().register(loadWatch);
        context.getWatchRegistry().register(kSessionCounter);
        instanceID = context.getServiceBeanConfig().getInstanceID();
    }


    public void incrementSessionCounter() {
        double last = kSessionCounter.getLastCalculableValue();
        kSessionCounter.increment();        
    }
}
