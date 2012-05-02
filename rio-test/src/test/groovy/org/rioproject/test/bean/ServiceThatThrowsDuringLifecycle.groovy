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
package org.rioproject.test.bean

import org.rioproject.bean.Initialized
import org.rioproject.bean.Started
import org.rioproject.bean.PreDestroy
import org.rioproject.bean.SetConfiguration
import net.jini.config.Configuration
import org.rioproject.bean.SetParameters
import org.rioproject.bean.CreateProxy
import org.rioproject.bean.PreAdvertise

/**
 * Test that if lifecycle methods throw exceptions (checked or unchecked) the service creation fails.
 *
 * @author Dennis Reedy
 */
class ServiceThatThrowsDuringLifecycle {

    @Initialized
    public void initialized() {
        throw new RuntimeException("foo")
    }

    @Started
    public void started() {
        throw new RuntimeException("foo")
    }

    @PreDestroy
    public void destroyed() {
        throw new RuntimeException("foo")
    }

    @SetConfiguration
    public void setDaConfiguration(Configuration config) {
        throw new RuntimeException("foo")
    }

    @SetParameters
    public void parms(Map<String, ?> p) {
        throw new RuntimeException("foo")
    }

    @CreateProxy
    def makeOne(def o) {
        throw new RuntimeException("foo")
    }

    @PreAdvertise
    def preAdv() {
        throw new RuntimeException("foo")
    }
}
