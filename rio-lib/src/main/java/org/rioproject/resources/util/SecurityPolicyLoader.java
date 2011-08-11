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
package org.rioproject.resources.util;

import java.net.URL;
import java.security.AllPermission;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;
import java.rmi.RMISecurityManager;

/**
 * Utility class to load a security policy as a resource.
 *
 * @author Dennis Reedy
 */
public class SecurityPolicyLoader {
    /**
     * Load and set the java.security.policy property
     * 
     * @param clazz The Class that will be used to get the policy as a resource
     * @param policyName The name of the policy file to load
     */
    public static void load(Class clazz, String policyName) {
        Policy.setPolicy(
            new Policy() {
                public PermissionCollection getPermissions(CodeSource codesource) {
                    Permissions perms = new Permissions();
                    perms.add(new AllPermission());
                    return(perms);
                }
                public void refresh() {
                }

            });
        if(System.getSecurityManager() != null) {            
            return;

        } else {
            System.setSecurityManager(new RMISecurityManager());
        }
        try {
            URL policy = clazz.getResource(policyName);
            if(policy == null) {
                System.err.println("Warning: can't find ["+clazz.getPackage()+"."+policyName+"] resource");
                // not a fatal error, but a SecurityException is probable.
            } else {
                System.setProperty("java.security.policy", policy.toString());
            }            
            System.getSecurityManager().checkPermission(
                new RuntimePermission("getClassLoader"));
        } catch(SecurityException uh_oh) {
            uh_oh.printStackTrace();
            System.err.println("(aborting)");
            System.exit(1);
        }
    }
}

