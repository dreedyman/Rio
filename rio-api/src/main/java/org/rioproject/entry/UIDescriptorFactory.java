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
package org.rioproject.entry;

import net.jini.lookup.entry.UIDescriptor;
import net.jini.lookup.ui.MainUI;
import net.jini.lookup.ui.attribute.UIFactoryTypes;
import net.jini.lookup.ui.factory.JComponentFactory;
import net.jini.lookup.ui.factory.JDialogFactory;
import net.jini.lookup.ui.factory.JFrameFactory;
import net.jini.lookup.ui.factory.JWindowFactory;
import org.rioproject.serviceui.UIComponentFactory;
import org.rioproject.serviceui.UIDialogFactory;
import org.rioproject.serviceui.UIFrameFactory;
import org.rioproject.serviceui.UIWindowFactory;

import java.io.IOException;
import java.net.URL;
import java.rmi.MarshalledObject;
import java.util.Collections;

/**
 * A helper utility that creates a UIDescriptor as part of the ServiceUI project.
 *
 * @author Dennis Reedy
 */
public class UIDescriptorFactory {

   /**
    * Get a UIDescriptor for a JComponent
    *
    * @param artifact The artifact
    * @param className The classname
    * @return A UIDescriptor
    * @throws IOException if the MarshalledObject cannot be created
    */
    public static UIDescriptor getJComponentDesc(String artifact, String className) throws IOException {
        if(artifact == null)
            throw new IllegalArgumentException("artifact is null");
        return (getJComponentDesc(artifact, new String[]{""}, className));
    }

    /**
     * Get a UIDescriptor for a JComponent
     *
     * @param codebase The codebase
     * @param jarName The jar name
     * @param className The classname
     * @return A UIDescriptor
     * @throws IOException if the MarshalledObject cannot be created
     */
    public static UIDescriptor getJComponentDesc(String codebase, String jarName, String className) throws IOException {
        if(jarName == null)
            throw new IllegalArgumentException("jarName is null");
        return (getJComponentDesc(codebase, new String[]{jarName}, className));
    }

    /**
     * Get a UIDescriptor for a JComponent
     *
     * @param codebase The codebase
     * @param jars The jars to use
     * @param className The classname
     * @return A UIDescriptor
     * @throws IOException if the MarshalledObject cannot be created
     */
    public static UIDescriptor getJComponentDesc(String codebase, String[] jars, String className) throws IOException {
        if(codebase == null)
            throw new IllegalArgumentException("codebase is null");
        if(jars == null)
            throw new IllegalArgumentException("jars are null");
        if(className == null)
            throw new IllegalArgumentException("className is null");
        return getUIDescriptor(MainUI.ROLE, JComponentFactory.TYPE_NAME, codebase, jars, className);
    }

    /**
     * Get a UIDescriptor for a JFrame
     *
     * @param codebase The codebase
     * @param jarName The jar name
     * @param className The classname
     *
     * @return A UIDescriptor
     * @throws IOException if the MarshalledObject cannot be created
     */
    public static UIDescriptor getJFrameDesc(String codebase,
                                             String jarName,
                                             String className)
        throws IOException {
        if(codebase == null)
            throw new IllegalArgumentException("codebase is null");
        if(jarName == null)
            throw new IllegalArgumentException("jarName is null");
        if(className == null)
            throw new IllegalArgumentException("className is null");
        return getUIDescriptor(MainUI.ROLE, JFrameFactory.TYPE_NAME, codebase, new String[]{jarName}, className);
    }

    /**
     * Get a UIDescriptor for a JFrame
     *
     * @param codebase The codebase
     * @param jars The jars to use
     * @param className The classname
     * @return A UIDescriptor
     * @throws IOException if the MarshalledObject cannot be created
     */
    public static UIDescriptor getJFrameDesc(String codebase, String[] jars, String className)
    throws IOException {
        if(codebase==null)
            throw new IllegalArgumentException("codebase is null");
        if(jars==null)
            throw new IllegalArgumentException("jars are null");
        if(className==null)
            throw new IllegalArgumentException("className is null");
        return getUIDescriptor(MainUI.ROLE, JFrameFactory.TYPE_NAME, codebase, jars, className);
    }
    
    /**
     * Get a UIDescriptor
     *
     * @param role The role
     * @param typeName The type
     * @param codebase The codebase
     * @param jars The jars to use
     * @param className The classname
     * @return A UIDescriptor
     
     * @throws IOException if the MarshalledObject cannot be created
     */
    public static UIDescriptor getUIDescriptor(String role,
                                               String typeName,
                                               String codebase,
                                               String[] jars,
                                               String className) throws IOException {
        if(role == null)
            throw new IllegalArgumentException("role is null");
        if(typeName == null)
            throw new IllegalArgumentException("typeName is null");
        if(codebase == null)
            throw new IllegalArgumentException("codebase is null");
        if(jars == null)
            throw new IllegalArgumentException("jars are null");
        if(className == null)
            throw new IllegalArgumentException("className is null");
        UIDescriptor desc = new UIDescriptor();
        desc.role = role;
        UIFactoryTypes types  ;
        MarshalledObject factory;
        URL[] urls = new URL[jars.length];
        
        for(int i = 0; i < urls.length; i++) {
            urls[i] = new URL(codebase+(codebase.endsWith("/")?"":"/")+jars[i]);
        }
        
        if(typeName.equals(JComponentFactory.TYPE_NAME)) {
            types = new UIFactoryTypes( Collections.singleton(JComponentFactory.TYPE_NAME));
            factory = new MarshalledObject<UIComponentFactory>(new UIComponentFactory(urls, className));
            desc.toolkit = JComponentFactory.TOOLKIT;
        } else if(typeName.equals(JDialogFactory.TYPE_NAME)) {
            types = new UIFactoryTypes(
                Collections.singleton(JDialogFactory.TYPE_NAME));
            factory = new MarshalledObject<UIDialogFactory>(new UIDialogFactory(urls, className));
            desc.toolkit = JDialogFactory.TOOLKIT;
        } else if(typeName.equals(JFrameFactory.TYPE_NAME)) {
            types = new UIFactoryTypes(
                Collections.singleton(JFrameFactory.TYPE_NAME));
            factory = new MarshalledObject<UIFrameFactory>(new UIFrameFactory(urls, className));
            desc.toolkit = JFrameFactory.TOOLKIT;
        } else if(typeName.equals(JWindowFactory.TYPE_NAME)) {
            types = new UIFactoryTypes(
                Collections.singleton(JWindowFactory.TYPE_NAME));
            factory = new MarshalledObject<UIWindowFactory>(new UIWindowFactory(urls, className));
            desc.toolkit = JWindowFactory.TOOLKIT;
        } else {
            throw new IllegalArgumentException("unknown typeName "+typeName);
        }
        desc.attributes = Collections.singleton(types);
        desc.factory = factory;
        return (desc);
    }
    
    /**
     * Get a UIDescriptor for a JComponentFactory
     * 
     * @param role The role
     * @param factory A JComponentFactory
     * 
     * @return A UIDescriptor
     * 
     * @throws IOException if the MarshalledObject cannot be created
     */
    public static UIDescriptor getUIDescriptor(String role, JComponentFactory factory) throws IOException {
        if(role==null)
            throw new IllegalArgumentException("role is null");
        if(factory==null)
            throw new IllegalArgumentException("factory is null");
        UIDescriptor desc = new UIDescriptor();
        desc.role = role;
        desc.toolkit = JComponentFactory.TOOLKIT;
        desc.attributes = Collections.singleton(new UIFactoryTypes(Collections.singleton(JComponentFactory.TYPE_NAME)));
        desc.factory = new MarshalledObject<JComponentFactory>(factory);
        return (desc);
    }

    /**
     * Get a UIDescriptor for a JDialogFactory
     * 
     * @param role The role
     * @param factory A JDialogFactory
     * 
     * @return A UIDescriptor
     * 
     * @throws IOException if the MarshalledObject cannot be created
     */
    public static UIDescriptor getUIDescriptor(String role, JDialogFactory factory) throws IOException {
        if(role==null)
            throw new IllegalArgumentException("role is null");
        if(factory==null)
            throw new IllegalArgumentException("factory is null");
        UIDescriptor desc = new UIDescriptor();
        desc.role = role;
        desc.toolkit = JDialogFactory.TOOLKIT;
        desc.attributes = Collections.singleton(new UIFactoryTypes(Collections.singleton(JDialogFactory.TYPE_NAME)));
        desc.factory = new MarshalledObject<JDialogFactory>(factory);
        return (desc);
    }

    /**
     * Get a UIDescriptor for a JFrameFactory
     * @param role The role
     * @param factory A JFrameFactory
     * 
     * @return A UIDescriptor
     * 
     * @throws IOException if the MarshalledObject cannot be created
     */
    public static UIDescriptor getUIDescriptor(String role, JFrameFactory factory) throws IOException {
        if(role==null)
            throw new IllegalArgumentException("role is null");
        if(factory==null)
            throw new IllegalArgumentException("factory is null");
        UIDescriptor desc = new UIDescriptor();
        desc.role = role;
        desc.toolkit = JFrameFactory.TOOLKIT;
        desc.attributes = Collections.singleton(new UIFactoryTypes(Collections.singleton(JFrameFactory.TYPE_NAME)));
        desc.factory = new MarshalledObject<JFrameFactory>(factory);
        return (desc);
    }

    public static UIDescriptor getUIDescriptor(String role, JWindowFactory factory) throws IOException {
        if(role==null)
            throw new IllegalArgumentException("role is null");
        if(factory==null)
            throw new IllegalArgumentException("factory is null");
        UIDescriptor desc = new UIDescriptor();
        desc.role = role;
        desc.toolkit = JWindowFactory.TOOLKIT;
        desc.attributes = Collections.singleton(new UIFactoryTypes(Collections.singleton(JWindowFactory.TYPE_NAME)));
        desc.factory = new MarshalledObject<JWindowFactory>(factory);
        return (desc);
    }
}
