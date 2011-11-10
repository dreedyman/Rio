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
package org.rioproject.fdh;

import net.jini.core.lookup.ServiceID;
import org.rioproject.associations.AssociationDescriptor;
import org.rioproject.opstring.ClassBundle;
import org.rioproject.opstring.ServiceElement;
import org.rioproject.loader.ClassBundleLoader;

import java.util.List;

/**
 * The FaultDetectionHandlerFactory class provides static methods to 
 * create FaultDetectionHandler instances
 *
 * @author Dennis Reedy
 */
public class FaultDetectionHandlerFactory {
    
    /**
     * Get a FaultDetectionHandler from the ServiceElement
     * 
     * @param sElem The ServiceElement
     * @param cl The delegation ClassLoader used to load the 
     * FaultDetectionHandler, or the current thread's context class 
     * loader if cl is null. 
     * @return A FaultDetectionHandler created and
     * initialized. A new FaultDetectionHandler will be created each time.
     * If there are problems creating the FaultDetectionHandler a null will
     * be returned
     *
     * @throws Exception If there are errors creating the FaultDetectionHandler
     */
    public static FaultDetectionHandler<ServiceID> getFaultDetectionHandler(ServiceElement sElem, ClassLoader cl)
    throws Exception {
        return (getFaultDetectionHandler(sElem.getFaultDetectionHandlerBundle(), cl));
    }
    
   
    /**
     * Get a FaultDetectionHandler from the AssociationDescriptor
     * 
     * @param aDesc The AssociationDescriptor
     * @param cl The delegation ClassLoader used to load the 
     * FaultDetectionHandler, or the current thread's context class 
     * loader if cl is null. 
     * @return A FaultDetectionHandler created and
     * initialized. A new FaultDetectionHandler will be created each time.
     * If there are problems creating the FaultDetectionHandler a null will
     * be returned
     *
     * @throws Exception If there are errors creating the FaultDetectionHandler
     */
    public static FaultDetectionHandler<ServiceID> getFaultDetectionHandler(AssociationDescriptor aDesc, ClassLoader cl)
    throws Exception {
        return (getFaultDetectionHandler(aDesc.getFaultDetectionHandlerBundle(), cl));
    }
    
    /**
     * Load the FaultDetectionHandler from a ClassBundle
     *   
     * @param fdhBundle The ClassBundle
     * @param cl The delegation ClassLoader used to load the 
     * FaultDetectionHandler, or the current thread's context class 
     * loader if cl is null. 
     * @return A FaultDetectionHandler created and
     * initialized. A new FaultDetectionHandler will be created each time. If
     * the fdhBundle is null, an
     * {@link AdminFaultDetectionHandler} will be
     * created. If there are problems creating the FaultDetectionHandler a
     * null will be returned
     *
     * @throws Exception If there are errors creating the FaultDetectionHandler
     */
    @SuppressWarnings("unchecked")
    public static FaultDetectionHandler<ServiceID> getFaultDetectionHandler(ClassBundle fdhBundle, ClassLoader cl)
    throws Exception {
        Class fdhClass;
        if(fdhBundle==null) {
            fdhBundle = new ClassBundle("org.rioproject.fdh.AdminFaultDetectionHandler");
        }
        if(cl==null)
            fdhClass = ClassBundleLoader.loadClass(fdhBundle);
        else
            fdhClass = ClassBundleLoader.loadClass(cl, fdhBundle);
        FaultDetectionHandler fdh = (FaultDetectionHandler)fdhClass.newInstance();
        fdhBundle.runKnownMethods(fdh);
        return (fdh);
    }

    /**
     * Get the ClassBundle for a FaultDetectionHandler declaration
     *
     * @param fdhClassName The class name of the FaultDetectionHandler
     * @param configuration Configuration arguments
     * @return A ClassBundle created based on the input arguments
     */
    public static ClassBundle getClassBundle(String fdhClassName, String configuration) {
        ClassBundle bundle = new ClassBundle(fdhClassName);
        bundle.addMethod("setConfiguration",
                         new Object[] {new String[]{configuration}});
        return(bundle);
    }

    /**
     * Get the ClassBundle for a FaultDetectionHandler declaration
     *
     * @param fdhClassName The class name of the FaultDetectionHandler
     * @param configArgs Configuration arguments
     * @return A ClassBundle created based on the input arguments
     */
    public static ClassBundle
        getClassBundle(String fdhClassName, String[] configArgs) {
        ClassBundle bundle = new ClassBundle(fdhClassName);
        bundle.addMethod("setConfiguration",
                         new Object[] {configArgs});
        return(bundle);
    }

    /**
     * Get the ClassBundle for a FaultDetectionHandler declaration
     *
     * @param fdhClassName The class name of the FaultDetectionHandler
     * @param configArgs List of configuration arguments
     * @return A ClassBundle created based on the input arguments
     */
    public static ClassBundle
        getClassBundle(String fdhClassName, List<String> configArgs) {
        String[] args = configArgs.toArray(new String[configArgs.size()]);
        ClassBundle bundle = new ClassBundle(fdhClassName);
        bundle.addMethod("setConfiguration", new Object[] {args});
        return(bundle);
    }

}
