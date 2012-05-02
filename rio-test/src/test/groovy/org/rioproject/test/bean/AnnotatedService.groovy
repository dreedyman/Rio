package org.rioproject.test.bean

import org.rioproject.bean.Initialized
import org.rioproject.bean.Started
import org.rioproject.bean.PreDestroy
import net.jini.config.Configuration
import org.rioproject.bean.SetConfiguration
import org.rioproject.bean.SetServiceBeanContext
import org.rioproject.core.jsb.ServiceBeanContext
import org.rioproject.bean.PreAdvertise
import org.rioproject.bean.PostUnAdvertise
import org.rioproject.bean.SetParameters
import org.rioproject.bean.SetServiceBean
import org.rioproject.core.jsb.ServiceBean

/**
 * Test bean annotations
 */
public class AnnotatedService {
    boolean initializedInvoked = false
    boolean startedInvoked = false
    boolean destroyedInvoked = false
    boolean configInvoked = false
    boolean contextInvoked = false
    boolean preAdvertisedInvoked = false
    boolean postUnAdvertisedInvoked = false
    boolean parametersInvoked = false
    boolean serviceBeanInvoked = false

    @Initialized
    public void initialized() {
        initializedInvoked = true
    }

    @Started
    public void started() {
        startedInvoked = true
    }

    @PreDestroy
    public void destroyed() {
        destroyedInvoked = true
    }

    @SetConfiguration
    public void config(Configuration config) {
        configInvoked = true
    }

    @SetServiceBeanContext
    public void context(ServiceBeanContext context) {
        contextInvoked = context!=null
    }

    @SetParameters
    public void parameters(Map<String, Object> parameters) {
        parametersInvoked = parameters!=null
    }

    @SetServiceBean
    public void serviceBean(ServiceBean bean) {
        serviceBeanInvoked = bean!=null
    }

    @PreAdvertise
    public void preAdvertise() {
        preAdvertisedInvoked = true
    }

    @PostUnAdvertise
    public void postUnAdvertise() {
        postUnAdvertisedInvoked = true
    }


}
