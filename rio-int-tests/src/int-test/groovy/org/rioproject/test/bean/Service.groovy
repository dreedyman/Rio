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
package org.rioproject.test.bean

import net.jini.config.Configuration
import org.rioproject.annotation.*

import javax.annotation.PreDestroy

class Service implements ServiceInterface {
    boolean initializedInvoked = false
    boolean startedInvoked = false
    boolean destroyedInvoked = false
    String something
    String something2
    Map<String, ?> parms = null
    ServiceInterfaceProxy bogusProxy
    List<String> order = new LinkedList<>()

    @Initialized
    void initialized() {
        initializedInvoked = true
        order.add("initialized")
    }

    @PreAdvertise
    void preAdvertise() {
        order.add("pre-advertise")
    }

    @Started
    void started() {
        startedInvoked = true
        order.add("started")
    }

    void setService(ServiceInterface service) {
        order.add("inject-service")
    }

    @PreDestroy
    void destroyed() {
        destroyedInvoked = true
    }

    @SetConfiguration
    void setDaConfiguration(Configuration config) {
        if(bogusProxy!=null)
            throw new IllegalStateException("configuration set after createProxy")        
        something = config.getEntry('simple', 'something', String.class, "1")
        something2 = config.getEntry('simple2', 'something', String.class, "2")
    }

    String getSomething() {
        return something
    }

    @SetParameters
    void parms(Map<String, ?> p) {
        parms = p
    }

    @CreateProxy
    ServiceInterface makeOne(ServiceInterface o) {
        if(something==null) {
            throw new IllegalStateException("createProxy called before configuration, something is null")
        }
        if(initializedInvoked) {
            throw new IllegalStateException("createProxy called before initializedInvoked")
        }
        if(parms==null) {
            throw new IllegalStateException("createProxy called before @SetParameters")
        }
        bogusProxy = new ServiceInterfaceProxy()
        return bogusProxy
    }
}

class ServiceInterfaceProxy implements ServiceInterface, Serializable {

}