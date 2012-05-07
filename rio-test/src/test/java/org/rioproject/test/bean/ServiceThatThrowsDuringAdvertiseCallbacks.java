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

package org.rioproject.test.bean;

import org.rioproject.bean.PostUnAdvertise;
import org.rioproject.bean.PreAdvertise;
import org.rioproject.bean.SetParameters;

import java.util.HashMap;
import java.util.Map;

/**
 * 
 * @author Dennis Reedy
 */
public class ServiceThatThrowsDuringAdvertiseCallbacks {
    private final Map<String, Object> parameters = new HashMap<String, Object>();

    @SetParameters
    public void setParams(Map<String, Object> p) {
        parameters.putAll(p);
    }

    @PreAdvertise
    public void preAdv() {
        Boolean throwOnPreAdvertise = (Boolean) parameters.get("throwOnPreAdvertise");
        if(throwOnPreAdvertise)
            throw new RuntimeException("something bad happened");
    }

    @PostUnAdvertise
    public void postUnadvertise() {
        Boolean throwOnPostUnAdvertise = (Boolean) parameters.get("throwOnPostUnAdvertise");
        if(throwOnPostUnAdvertise)
            throw new RuntimeException("something bad happened");
    }
}
