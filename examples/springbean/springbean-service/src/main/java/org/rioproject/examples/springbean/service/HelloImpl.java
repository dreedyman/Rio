/*
 * Copyright to the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.rioproject.examples.springbean.service;

import org.rioproject.examples.springbean.Hello;

import java.rmi.RemoteException;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Policy;
import java.util.Enumeration;

public class HelloImpl implements Hello {
    int visitorNumber = 1;

    public HelloImpl() {
        System.out.println("Policy: "+ Policy.getPolicy().getClass().getName());
        CodeSource cs = HelloImpl.class.getProtectionDomain().getCodeSource();
        PermissionCollection pCollection = Policy.getPolicy().getPermissions(cs);
        Enumeration<Permission> elements = pCollection.elements();
        System.out.println("Permissions");
        while (elements.hasMoreElements()) {
            System.out.println("\t"+elements.nextElement());
        }
    }

    public String hello(String message) {
        System.out.println("Client says hello : "+message);
        return("Hello visitor : "+visitorNumber++);
    }    
}
