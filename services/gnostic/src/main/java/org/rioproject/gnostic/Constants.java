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

/**
 * Constants related to Gnostic configuration.
 *
 * @author Jerome Bernard
 * @author Dennis Reedy
 */
public interface Constants {
    static final String PROVISION_EVENTS_STREAM = "provision-events-stream";
    static final String CALCULABLES_STREAM = "calculables-stream";
    static final String SCALING_RULE = "ScalingRuleHandler.drl";
    static final String[] BUILT_IN_RULES = {SCALING_RULE};
}
