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
package org.rioproject.ui;

import net.jini.admin.Administrable;
import net.jini.io.UnsupportedConstraintException;
import org.rioproject.deploy.DeployAdmin;
import org.rioproject.deploy.ProvisionManager;
import org.rioproject.opstring.OperationalStringException;
import org.rioproject.opstring.OperationalStringManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Method;
import java.net.URL;
import java.rmi.RemoteException;

/**
 * General utilities for use with a UI component in Rio
 *
 * @author Dennis Reedy
 */
public class Util {

    public static OperationalStringManager getOperationalStringManager(final ProvisionManager monitor,
                                                                       final String opStringName)
        throws RemoteException, OperationalStringException {
		if (monitor == null)
			return null;
        DeployAdmin da = (DeployAdmin) ((Administrable)monitor).getAdmin();
        OperationalStringManager mgr = da.getOperationalStringManager(opStringName);
        return(mgr);
    }

    /**
     * If the Container has a dispose method, call it
     *
     * @param comp The Container
     */
    public static void dispose(final Container comp) {
        try {
            Method dispose = comp.getClass().getMethod("dispose");
            dispose.invoke(comp);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Get an image as a resource and create an ImageIcon
     *
     * @param location The image icon location
     *
     * @return The corresponding ImageIcon loaded from the location
     */
    public static ImageIcon getImageIcon(final String location) {
        ImageIcon icon = null;
        URL url = Thread.currentThread().getContextClassLoader().getResource(location);
        if (url != null)
            icon = new ImageIcon(url);
        return (icon);
    }

    /**
     * Get an image as a resource and create an ImageIcon
     *
     * @param location The image icon location
     * @param width The scaled width
     * @param height The scaled height
     *
     * @return The corresponding ImageIcon loaded from the location and scaled
     */
    public static ImageIcon getScaledImageIcon(final String location, final int width, final int height) {
        ImageIcon icon = null;
        URL url = Thread.currentThread().getContextClassLoader().getResource(location);
        if (url != null) {
            icon = new ImageIcon(url);
            icon = new ImageIcon(icon.getImage().getScaledInstance(width,
                                                                   height,
                                                                   Image.SCALE_SMOOTH));
        }
        return (icon);
    }

    /**
     * Show an exception in a Dialog
     *
     * @param e The exception
     * @param comp The parent component
     * @param title The title to use
     */
    public static void showError(final Throwable e, final Component comp, final String title) {
        if (e.getCause() != null &&
            e.getCause() instanceof UnsupportedConstraintException) {
            Throwable cause = e.getCause();
            JOptionPane.showMessageDialog(comp,
                                          "<html><font face=monospace><font size=3>" +
                                          "Exception: " +
                                          cause.getClass().getName() +
                                          "<br><br> " +
                                          "You do not have permission to perform " +
                                          "the action" +
                                          "</font></font></html>",
                                          "Action Denied",
                                          JOptionPane.ERROR_MESSAGE);
            return;
        }
        StringBuilder buffer = new StringBuilder();
        Throwable thrown = e;
        if (e.getCause() != null)
            thrown = e.getCause();
        StackTraceElement[] trace = thrown.getStackTrace();
        for (StackTraceElement aTrace : trace)
            buffer.append("&nbsp;&nbsp;&nbsp;&nbsp;at ").append(aTrace).append("<br>");

        showError("<html><font face=monospace><font size=3>" +
                  "Exception : " +
                  thrown.getClass().getName() +
                  " : " +
                  thrown.getLocalizedMessage() +
                  "<br>" +
                  buffer.toString() +
                  "</font></font></html>",
                  comp,
                  title);
    }

    /**
     * Show an error in a Dialog
     *
     * @param text The text to show
     * @param comp The parent component
     * @param title The title to use
     */
    public static void showError(final String text, final Component comp, final String title) {
        JDialog dialog;
        if (comp instanceof Dialog)
            dialog = new JDialog((Dialog) comp);
        else if (comp instanceof Frame)
            dialog = new JDialog((Frame) comp);
        else
            dialog = new JDialog();
        JEditorPane errorPane = new JEditorPane();
        errorPane.setEditable(false);
        errorPane.setContentType("text/html");
        errorPane.setText(text);
        errorPane.setCaretPosition(0);
        errorPane.moveCaretPosition(0);
        JPanel pane = new JPanel();
        pane.setLayout(new BorderLayout(8, 8));
        pane.add(new JScrollPane(errorPane), BorderLayout.CENTER);
        JButton dismiss = new JButton("Close");
        dismiss.addActionListener(new DisposeActionListener(dialog));
        JPanel buttonPane = new JPanel();
        buttonPane.add(dismiss);
        pane.add(buttonPane, BorderLayout.SOUTH);
        dialog.getContentPane().add(pane);
        int width = 650;
        int height = 300;
        dialog.pack();
        dialog.setSize(width, height);
        if(comp==null) {
            Dimension screenSize =
                Toolkit.getDefaultToolkit().getScreenSize();
            int widthLoc = screenSize.width / 2 - width / 2;
            int heightLoc = screenSize.height / 2 - height / 2;
            dialog.setLocation(widthLoc, heightLoc);
        } else {
            dialog.setLocationRelativeTo(comp);
        }
        dialog.setTitle(title);
        dialog.setVisible(true);
    }

    public static class DisposeActionListener implements ActionListener {
        JDialog dialog;

        public DisposeActionListener(final JDialog dialog) {
            this.dialog = dialog;
        }

        public void actionPerformed(final ActionEvent ae) {
            dialog.dispose();
        }
    }
}
