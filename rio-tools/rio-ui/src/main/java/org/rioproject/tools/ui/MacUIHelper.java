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
package org.rioproject.tools.ui;

import org.rioproject.tools.ui.cybernodeutilization.CybernodeUtilizationPanel;
import org.rioproject.tools.ui.prefs.PreferencesDialog;

import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Utility for using swing with the Mac
 *
 * @author Dennis Reedy
 */
public class MacUIHelper {
    /**
     * Static method to set properties for the client     
     */
    public static void setSystemProperties() {
        /* Set property for execution on a Mac client */
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        System.setProperty("com.apple.macos.smallTabs", "true");
        System.setProperty("com.apple.mrj.application.growbox.intrudes",
                           "false");
        System.setProperty("apple.awt.brushMetalLook", "true");
    }

    /**
     * Determine if the client is MacOS
     *
     * @return true if the operating system is Mac OS
     */
    public static boolean isMacOS() {
        if (System.getProperty("mrj.version") != null)
            return (true);
        return (false);
    }

    /**
     * Create the ui handler for the Mac OSX client
     *
     * @param frame - The JFrame of the client
     * @param graphView The GraphView for preference handling
     * @param cup The CybernodeUtilizationPanel
     */
    public static void setUIHandler(Main frame,
                                    GraphView graphView,
                                    CybernodeUtilizationPanel cup) {
        if (!isMacOS())
            return;
        try {
            Class appClass = MacUIHelper.class.getClassLoader().loadClass(
                "com.apple.eawt.Application");
            Class adapterClass = MacUIHelper.class.getClassLoader().loadClass(
                "com.apple.eawt.ApplicationListener");
            Object app = appClass.newInstance();
            Method addApplicationListener =
                appClass.getMethod("addApplicationListener", adapterClass);

            Method addPreferencesMenuItem =
                appClass.getMethod("addPreferencesMenuItem");

            Method setEnabledPreferencesMenu =
                appClass.getMethod("setEnabledPreferencesMenu", boolean.class);

            InvocationHandler handler = new UIHandler(frame, graphView, cup);

            Object macAppProxy =
                Proxy.newProxyInstance(MacUIHelper.class.getClassLoader(),
                                       new Class[]{adapterClass},
                                       handler);

            addApplicationListener.invoke(app, macAppProxy);
            addPreferencesMenuItem.invoke(app);
            setEnabledPreferencesMenu.invoke(app, true);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /**
     * InvocationHandler for About and Exit handing
     */
    static class UIHandler implements InvocationHandler {
        Main frame;
        GraphView graphView;
        CybernodeUtilizationPanel cup;

        public UIHandler(Main frame,
                         GraphView graphView,
                         CybernodeUtilizationPanel cup) {

            this.frame = frame;
            this.graphView = graphView;
            this.cup = cup;
        }

        /**
         * Reflection-based invocation
         */
        public Object invoke(Object target, Method method, Object[] args)
            throws Throwable {

            setHandled(args[0]);
            if (method.getName().equals("handleAbout")) {
                new RioAboutBox(frame);
            }

            if (method.getName().equals("handleQuit")) {
                WindowEvent event =
                    new WindowEvent(new Window(frame),
                                    WindowEvent.WINDOW_CLOSING);
                WindowListener[] wListeners = frame.getWindowListeners();
                for (WindowListener wListener : wListeners) {
                    wListener.windowClosing(event);
                }
            }
             if (method.getName().equals("handlePreferences")) {
                 PreferencesDialog prefs = new PreferencesDialog(frame,
                                                                 graphView,
                                                                 cup);
                 prefs.setVisible(true);
             }

            return (null);
        }

        private void setHandled(Object arg) {
            try {
                Method handled = arg.getClass().getMethod("setHandled",
                                                          boolean.class);
                handled.invoke(arg, Boolean.TRUE);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }
}
