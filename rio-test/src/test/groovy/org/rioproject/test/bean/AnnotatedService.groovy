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
    List<String> order = new LinkedList<>()

    @Initialized
    public void initialized() {
        initializedInvoked = true
        order.add("initialized")
    }

    @Started
    public void started() {
        startedInvoked = true
        order.add("started")
    }

    @PreDestroy
    public void destroyed() {
        destroyedInvoked = true
    }

    @SetConfiguration
    public void config(Configuration config) {
        configInvoked = true
        order.add("set-config")
    }

    @SetServiceBeanContext
    public void context(ServiceBeanContext context) {
        contextInvoked = context!=null
        order.add("set-context")
    }

    @SetParameters
    public void parameters(Map<String, Object> parameters) {
        parametersInvoked = parameters!=null
        order.add("set-parameters")
    }

    @SetServiceBean
    public void serviceBean(ServiceBean bean) {
        serviceBeanInvoked = bean!=null
        order.add("set-service-bean")
    }

    @PreAdvertise
    public void preAdvertise() {
        preAdvertisedInvoked = true
        order.add("pre-advertise")
    }

    @PostUnAdvertise
    public void postUnAdvertise() {
        postUnAdvertisedInvoked = true
        order.add("post-unadvertise")
    }


}
