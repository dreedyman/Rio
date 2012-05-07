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
package org.rioproject.sla;

import java.io.Serializable;
import java.util.*;

/**
 * Provides the association for rule resources to services.
 */
public class RuleMap implements Serializable {
    private RuleDefinition ruleDefinition;
    private final List<ServiceDefinition> services =
        new ArrayList<ServiceDefinition>();

    public void addRuleMapping(RuleDefinition rule, List<ServiceDefinition> services) {
        if(rule==null)
            throw new IllegalArgumentException("rule cannot be null");
        if(services==null)
            throw new IllegalArgumentException("services cannot be null");
        if(services.isEmpty())
            throw new IllegalArgumentException("services cannot be empty");
        this.ruleDefinition = rule;
        this.services.addAll(services);
    }

    public RuleDefinition getRuleDefinition() {
        return ruleDefinition;
    }

    public List<ServiceDefinition> getServiceDefinitions() {
        return Collections.unmodifiableList(services);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        RuleMap ruleMap = (RuleMap) o;

        return !(ruleDefinition != null? !ruleDefinition.equals(ruleMap.ruleDefinition):
                 ruleMap.ruleDefinition != null) &&
               !(services != null? !services.equals(ruleMap.services):
                 ruleMap.services !=null);

    }

    @Override
    public int hashCode() {
        int result = ruleDefinition != null ? ruleDefinition.hashCode() : 0;
        result = 31 * result + (services != null ? services.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("RuleMap: ");
        sb.append("ruleDefinition=").append(ruleDefinition);
        sb.append(", services=").append(services);
        return sb.toString();
    }

    public static class RuleDefinition implements Serializable {
        private String resource;
        private String ruleClassPath;

        public RuleDefinition(String resource) {
            this.resource = resource;
        }

        public RuleDefinition(String resource, String ruleClassPath) {
            this.resource = resource;
            this.ruleClassPath = ruleClassPath;
        }

        public String getResource() {
            return resource;
        }

        public String getRuleClassPath() {
            return ruleClassPath;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;

            RuleDefinition that = (RuleDefinition) o;
            return !(ruleClassPath != null? !ruleClassPath.equals(that.ruleClassPath):
                     that.ruleClassPath != null) &&
                   !(resource != null? !resource.equals(that.resource):
                     that.resource != null);

        }

        @Override
        public int hashCode() {
            int result = resource != null ? resource.hashCode() : 0;
            result = 31 * result + (ruleClassPath != null ? ruleClassPath.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append("RuleDefinition: ");
            sb.append("resource=").append(resource);
            sb.append(", ruleClassPath=").append(ruleClassPath);
            return sb.toString();
        }
    }

    public static class ServiceDefinition implements Serializable {
        private String serviceName;
        private String opStringName;
        private final List<String> watches = new ArrayList<String>();

        public ServiceDefinition(String serviceName) {
            this.serviceName = serviceName;
        }

        public ServiceDefinition(String serviceName, String opStringName) {
            this.serviceName = serviceName;
            this.opStringName = opStringName;
        }

        public String getServiceName() {
            return serviceName;
        }

        public String getOpStringName() {
            return opStringName;
        }

        public void addWatches(String ... watch) {
            if(watch!=null)
                watches.addAll(Arrays.asList(watch));
        }

        public List<String> getWatches() {
            return Collections.unmodifiableList(watches);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;

            ServiceDefinition that = (ServiceDefinition) o;

            return !(opStringName != null? !opStringName.equals(that.opStringName):
                     that.opStringName != null) &&
                   !(serviceName != null ? !serviceName.equals(that.serviceName):
                     that.serviceName !=null) &&
                   !(watches != null? !watches.equals(that.watches):
                     that.watches != null);
        }

        @Override
        public int hashCode() {
            int result = serviceName != null ? serviceName.hashCode() : 0;
            result = 31 * result +
                     (opStringName != null ? opStringName.hashCode() : 0);
            result = 31 * result + (watches != null ? watches.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append("ServiceDefinition: ");
            sb.append("serviceName=").append(serviceName);
            sb.append(", opStringName=").append(opStringName);
            sb.append(", watches=").append(watches);
            return sb.toString();
        }
    }
}
