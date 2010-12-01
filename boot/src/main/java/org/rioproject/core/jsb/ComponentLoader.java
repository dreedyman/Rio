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
package org.rioproject.core.jsb;

/**
 * The ComponentLoader provides a mechanism for the ServiceBean to load classes
 * and resources (such as native libraries) making them (and the resources it
 * uses) available to all services
 *
 * @author Dennis Reedy
 */
public interface ComponentLoader {
    /**
     * Creates an Object which has been added to the ComponentLoader using the
     * addSystemComponent method, or is in the classpath of the ComponentLoader.
     * The Object is loaded from the registered code source.
     * 
     * @param name The name of the class. The resulting class will be loaded
     * and a new instance of the class created using a zero-arg contructor.
     * @throws IllegalAccessException - If the Class or its nullary constructor
     * is not accessible.
     * @throws InstantiationException - If this Class represents an abstract
     * class, an interface, an array class, a primitive type, or void; or if the
     * class has no nullary constructor; or if the instantiation fails for some
     * other reason.
     * @throws ClassNotFoundException - If the class cannot be located by the
     * class loader or it has not been registered
     *
     * @return The instantiated Object
     */
    Object load(String name) throws ClassNotFoundException,
        IllegalAccessException, InstantiationException;

    /**
     * Registers a class name, and the code source which is used as the search
     * path to load the class.
     * 
     * @param name The name of the class
     * @param urls Codebase for the class identified by the name parameter
     */
    void addComponent(String name, java.net.URL[] urls);

    /**
     * Test whether a named component (Class) exists.
     * 
     * @param name The component name
     * @return true If the requested component can be located, false
     * otherwise.
     */
    boolean testComponentExistence(String name);

    /**
     * Test whether a named resource exists. A resource is some data (images,
     * audio, text, etc) that can be accessed by class code in a way that is
     * independent of the location of the code.
     * <p>
     * The name of a resource is a '<tt>/</tt> '-separated path name that
     * identifies the resource.
     * 
     * @param name The resource name
     * @return true If the requested resource can be located, false otherwise.
     */
    boolean testResourceExistence(String name);
}
