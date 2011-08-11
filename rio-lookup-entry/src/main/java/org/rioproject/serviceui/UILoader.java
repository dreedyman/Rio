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
package org.rioproject.serviceui;

import net.jini.core.entry.Entry;
import net.jini.core.lookup.ServiceItem;
import net.jini.lookup.entry.UIDescriptor;
import net.jini.lookup.ui.attribute.UIFactoryTypes;
import net.jini.lookup.ui.factory.JComponentFactory;

import javax.swing.*;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

/**
 * The UILoader class is a helper for use with the ServiceUI
 *
 * @author Dennis Reedy
 */
public class UILoader {
    /**
     * This method returns an array JComponents from a UIDescriptor that contain
     * an JComponentFactory for a given role
     *
     * @param role The role to use
     * @param serviceItem The ServiceItem to use
     *
     * @return An array of JComponents
     */
    @SuppressWarnings("unchecked")
    public static JComponent[] loadUI(String role, ServiceItem serviceItem) {
        try {
            int attrCount = serviceItem.attributeSets.length;
            Entry[] attr = serviceItem.attributeSets;
            List<UIDescriptor> list = new ArrayList<UIDescriptor>();
            for(int i = 0; i < attrCount; ++i) {
                if(attr[i] instanceof UIDescriptor) {                    
                    UIDescriptor desc = (UIDescriptor)attr[i];
                    if(desc.attributes == null) {
                        continue;
                    }
                    if(!desc.role.equals(role)) {
                        continue;
                    }
                    for (Object attribute : desc.attributes) {
                        if (attribute instanceof UIFactoryTypes) {
                            UIFactoryTypes factoryTypes =
                                (UIFactoryTypes) attribute;
                            boolean found;
                            found = factoryTypes.isAssignableTo(
                                JComponentFactory.class);
                            if (found) {
                                // Should also look through required packages
                                // here
                                list.add(desc);
                            }
                        }
                    }
                }
            }
            List factoryList = new ArrayList();
            for (UIDescriptor desc : list) {
                Object uiFactory;
                try {
                    uiFactory =
                        desc.getUIFactory(serviceItem.service
                            .getClass().getClassLoader());
                    if (uiFactory instanceof JComponentFactory)
                        factoryList.add(uiFactory);
                } catch (ClassNotFoundException e) {
                    System.out.println(
                        "Class not found. Could not unmarshall " +
                        "the UI factory.");
                    e.printStackTrace();
                }
            }
            JComponent[] jComponents = new JComponent[factoryList.size()];
            for(int i = 0; i < factoryList.size(); i++) {
                JComponentFactory factory =
                    (JComponentFactory)factoryList.get(i);
                jComponents[i] = factory.getJComponent(serviceItem);
            }
            return (jComponents);
        } catch(MalformedURLException e) {
            e.printStackTrace();
        } catch(IOException e) {
            e.printStackTrace();
        }
        return (new JComponent[0]);
    }

    /**
     * This method returns an array of UI Factories that have been loaded from
     * each <code>UIDescriptor</code> found. This method will return all
     * <code>UIDescriptor</code> entries regardless of their role or factory
     * type
     *
     * @param serviceItem The ServiceItem to load
     * @return An array of loaded ui objects
     */
    @SuppressWarnings("unchecked")
    public static Object[] loadUI(ServiceItem serviceItem) {
        try {
            int attrCount = serviceItem.attributeSets.length;
            Entry[] attr = serviceItem.attributeSets;
            List<UIDescriptor> list = new ArrayList<UIDescriptor>();
            for(int i = 0; i < attrCount; ++i) {                
                if(attr[i] instanceof UIDescriptor) {
                    UIDescriptor desc = (UIDescriptor)attr[i];
                    if(desc.attributes == null) {
                        continue;
                    }
                    for (Object attribute : desc.attributes) {
                        if (attribute instanceof UIFactoryTypes) {
                            // Should also look through required packages here
                            list.add(desc);
                        }
                    }
                }
            }
            List factoryList = new ArrayList();
            for (UIDescriptor desc : list) {
                Object uiFactory;
                try {
                    uiFactory = desc.getUIFactory(serviceItem.service
                        .getClass().getClassLoader());
                    factoryList.add(uiFactory);
                } catch (ClassNotFoundException cnfe) {
                    System.out.println(
                        "Class not found. Could not unmarshall " +
                        "the UI factory.");
                    cnfe.printStackTrace();
                }
            }
            return (factoryList.toArray());
        } catch(MalformedURLException e) {
            e.printStackTrace();
        } catch(IOException e) {
            e.printStackTrace();
        }
        return (new Object[0]);
    }
}
