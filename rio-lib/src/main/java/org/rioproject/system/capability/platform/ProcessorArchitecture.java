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
package org.rioproject.system.capability.platform;

import org.rioproject.deploy.SystemComponent;
import org.rioproject.system.capability.PlatformCapability;

import java.util.Map;

/**
 * The <code>ProcessorArchitecture</code> object provides a definition of the
 * processor architecture
 *
 * @author Dennis Reedy
 */
public class ProcessorArchitecture extends PlatformCapability {
    static final long serialVersionUID = 1L;
    static final String DEFAULT_DESCRIPTION = "Processor Architecture";
    /** number of processors */
    public final static String AVAILABLE = "Available";
    /** cpu type */
    public final static String ARCHITECTURE = "Architecture";
    public static final String ID = "Processor";

    /** 
     * Create a ProcessorArchitecture with the default description 
     */
    public ProcessorArchitecture() {
        this(DEFAULT_DESCRIPTION);
    }

    /** 
     * Create a ProcessorArchitecture with a description
     *
     * @param description The description
     */
    public ProcessorArchitecture(String description) {
        this.description = description;
        define(NAME, "Processor");
        define(ARCHITECTURE, System.getProperty("os.arch"));
        define(AVAILABLE, Integer.toString(Runtime.getRuntime().availableProcessors()));
    }

    /**
     * Override supports to ensure that processor requirements are supported
     */
    public boolean supports(SystemComponent requirement) {
        boolean supports = hasBasicSupport(requirement.getName(),
                                           requirement.getClassName());                
        if(supports) {
            Map<String, Object> attributes = requirement.getAttributes();
            for (Map.Entry<String, Object> entry : attributes.entrySet()) {
                String key = entry.getKey();
                if (capabilities.containsKey(key)) {
                    String myMapping = (String) capabilities.get(key);
                    String theirMapping = (String) entry.getValue();
                    if (key.equals(AVAILABLE)) {
                        // verify number of processors
                        supports = checkNumberOfProcessors(myMapping,
                                                           theirMapping);
                        if (!supports)
                            break;
                    } else {
                        supports = matches(theirMapping, myMapping);
                        if (!supports)
                            break;
                    }
                } else {
                    supports = false;
                    break;
                }
            }
        }
        return(supports);
    }

    /*
     * Check that the number of processors requested is enough
     */
    private boolean checkNumberOfProcessors(String myNumberOfProcessors, 
                                            String requestedNumberOfProcessors) {
        try {
            int myNProcessors = Integer.parseInt(myNumberOfProcessors);
            int requestNProcessors = Integer.parseInt(requestedNumberOfProcessors);
            return myNProcessors >= requestNProcessors;
        } catch (NumberFormatException nfe) {
            return false;
        }

    }
}
