/*
 * Copyright 2008 the original author or authors.
 * Copyright 2005 Sun Microsystems, Inc.
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

import java.util.EventListener;

/**
 * Register for SLAPolicyEvent notification by providing a SLAPolicyEventListener 
 * implementation
 *
 * @author Dennis Reedy
 */
public interface SLAPolicyEventListener extends EventListener {
    /**
     * Notification of a SLAPolicyEvent
     *
     * @param event The SLAPolicyEvent   
     */
    void policyAction(SLAPolicyEvent event);
}



