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
import org.rioproject.loader.ClassBundleLoader;
import org.rioproject.opstring.ClassBundle;
import org.rioproject.opstring.ServiceElement;

/**
 * The FaultDetectionHandlerFactory class provides static methods to create FaultDetectionHandler instances
 *
 * @author Dennis Reedy
 */
public final class FaultDetectionHandlerFactory {

    private FaultDetectionHandlerFactory(){}
    
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
    public static FaultDetectionHandler<ServiceID> getFaultDetectionHandler(final ServiceElement sElem,
                                                                            final ClassLoader cl) throws Exception {
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
    public static FaultDetectionHandler<ServiceID> getFaultDetectionHandler(final AssociationDescriptor aDesc,
                                                                            final ClassLoader cl) throws Exception {
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
    public static FaultDetectionHandler<ServiceID> getFaultDetectionHandler(final ClassBundle fdhBundle,
                                                                            final ClassLoader cl) throws Exception {
        Class fdhClass;
        ClassBundle theFdhBundle = getClassBundle(fdhBundle);
        if(cl==null)
            fdhClass = ClassBundleLoader.loadClass(theFdhBundle);
        else
            fdhClass = ClassBundleLoader.loadClass(cl, theFdhBundle);
        FaultDetectionHandler fdh = (FaultDetectionHandler)fdhClass.newInstance();
        return (fdh);
    }

    private static ClassBundle getClassBundle(ClassBundle cb) {
        if(cb==null)
            return new ClassBundle(AdminFaultDetectionHandler.class.getName());
        return cb;
    }

}
