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
package org.rioproject.examples.springbean.service;

import org.rioproject.examples.springbean.Hello;

import java.rmi.RemoteException;

public class HelloImpl implements Hello {
    int visitorNumber = 1;

    public String hello(String message) throws RemoteException {
        System.out.println("Client says hello : "+message);
        return("Hello visitor : "+visitorNumber++);
    }    
}
