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
package org.rioproject.resources.ui;

import net.jini.core.entry.Entry;
import net.jini.io.UnsupportedConstraintException;
import org.rioproject.core.OperationalStringException;
import org.rioproject.core.OperationalStringManager;
import org.rioproject.entry.ApplianceInfo;
import org.rioproject.monitor.DeployAdmin;
import org.rioproject.monitor.ProvisionMonitor;

import javax.swing.*;
import javax.swing.plaf.basic.BasicToolTipUI;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;

/**
 * General utilities for use with a UI component in Rio
 *
 * @author Dennis Reedy
 */
public class Util {


    public static OperationalStringManager
    getOperationalStringManager(ProvisionMonitor monitor, String opStringName)
        throws RemoteException, OperationalStringException {
		if (monitor == null)
			return null;
        DeployAdmin da = (DeployAdmin) monitor.getAdmin();
        OperationalStringManager mgr =
            da.getOperationalStringManager(opStringName);
        return(mgr);
    }

    /**
     * Get the color for  row in the tree table
     *
     * @param root The tree root
     * @param node The tre node
     * @param tree The tree
     * @param defaultColor The default background color
     * @param altRowColor The background color for alternate rows
     *
     * @return The row color
     */
    public static Color getRowColor(DefaultMutableTreeNode root,
                                    DefaultMutableTreeNode node,
                                    JTree tree,
                                    Color defaultColor,
                                    Color altRowColor) {
        Color color;
        if(node.getAllowsChildren()) {
            int ndx = tree.getModel().getIndexOfChild(root, node);
            color = colorForRow(ndx, defaultColor, altRowColor);
        } else {
            DefaultMutableTreeNode parent =
                (DefaultMutableTreeNode)node.getParent();
            int ndx = tree.getModel().getIndexOfChild(root, parent);
            color = colorForRow(ndx, defaultColor, altRowColor);
        }
        return color;
    }

    private static Color colorForRow(int row,
                                     Color defaultColor,
                                     Color altRowColor) {
        return (row % 2 == 0) ? altRowColor : defaultColor;
    }

    /**
     * If the Container has a dispose method, call it
     *
     * @param comp The Container
     */
    public static void dispose(Container comp) {
        try {
            Method dispose = comp.getClass().getMethod("dispose");
            dispose.invoke(comp);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Save properties to a file stored in ${user.home}/.rio
     *
     * @param props The Properties object to save, must not be null
     * @param filename The file name to save the Proeprties to
     *
     * @throws IOException If there are exceptions accessing the file system
     */
    public static void saveProperties(Properties props, String filename)
        throws IOException {
        if (props == null)
            throw new NullPointerException("props is null");

        File rioHomeDir = new File(System.getProperty("user.home") +
                                    File.separator +
                                    ".rio");
        if (!rioHomeDir.exists())
            rioHomeDir.mkdir();
        File propFile = new File(rioHomeDir, filename);
        props.store(new FileOutputStream(propFile), null);
    }

    /**
     * Load properties from ${user.home}/.rio
     *
     * @param filename The name of the properties file to load
     *
     * @return A Properties object loaed from the system
     * @throws IOException If there are exceptions accessing the file system
     */
    public static Properties loadProperties(String filename) throws IOException {
        File rioHomeDir = new File(System.getProperty("user.home") +
                                    File.separator +
                                    ".rio");
        Properties props = new Properties();
        if (!rioHomeDir.exists())
            return (props);
        File propFile = new File(rioHomeDir, filename);
        props.load(new FileInputStream(propFile));
        return (props);
    }

    /**
     * Get an image as a resource and create an ImageIcon
     *
     * @param location The image icon location
     *
     * @return The corresponding ImageIcon loaded from the location
     */
    public static ImageIcon getImageIcon(String location) {
        ImageIcon icon = null;
        URL url = Util.class.getClassLoader().getResource(location);
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
    public static ImageIcon getScaledImageIcon(String location, int width, int height) {
        ImageIcon icon = null;
        URL url = Util.class.getClassLoader().getResource(location);
        if (url != null) {
            icon = new ImageIcon(url);
            icon = new ImageIcon(icon.getImage().getScaledInstance(width,
                                                                   height,
                                                                   Image.SCALE_SMOOTH));
        }
        return (icon);
    }

    /**
     * Helper to get the ApplianceInfo Entry
     *
     * @param attrs - Array of Entry objects
     * @return ApplianceInfo
     */
    public static ApplianceInfo getApplianceInfo(Entry[] attrs) {
        for (Entry attr : attrs) {
            if (attr instanceof ApplianceInfo) {
                return (ApplianceInfo) attr;
            }
        }
        return (null);
    }

    /**
     * Show an exception in a Dialog
     *
     * @param e The exception
     * @param comp The parent component
     * @param title The title to use
     */
    public static void showError(Throwable e, Component comp, String title) {
        if (e.getCause() != null &&
            e.getCause() instanceof UnsupportedConstraintException) {
            e = e.getCause();
            JOptionPane.showMessageDialog(null,
                                          "<html>" +
                                          "Exception: <font color=red>" +
                                          e.getClass().getName() +
                                          "</font><br><br> " +
                                          "You do not have permission to perform " +
                                          "the action</html>",
                                          "Action Denied",
                                          JOptionPane.OK_OPTION);
            return;
        }
        StringBuffer buffer = new StringBuffer();
        if (e.getCause() != null)
            e = e.getCause();
        StackTraceElement[] trace = e.getStackTrace();
        for (StackTraceElement aTrace : trace)
            buffer.append("at ").append(aTrace).append("<br>");

        showError("<html>Exception : <font color=red>" +
                  e.getClass().getName() + "</font>" +
                  " : " +
                  "<font color=blue>" +
                  e.getLocalizedMessage() + "</font>" +
                  "<br>" +
                  buffer.toString() +
                  "</html>",
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
    public static void showError(String text, Component comp, String title) {
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

        public DisposeActionListener(JDialog dialog) {
            this.dialog = dialog;
        }

        public void actionPerformed(ActionEvent ae) {
            dialog.dispose();
        }
    }

    public static class MultiLineToolTip extends JToolTip {
        public MultiLineToolTip() {
            setUI(new MultiLineToolTipUI());
        }
    }

    public static class MultiLineToolTipUI extends BasicToolTipUI {
        private String[] strs;

        public void paint(Graphics g, JComponent c) {
            FontMetrics metrics = g.getFontMetrics();
            Dimension size = c.getSize();
            g.setColor(c.getBackground());
            g.fillRect(0, 0, size.width, size.height);
            g.setColor(c.getForeground());
            if (strs != null) {
                for (int i = 0; i < strs.length; i++) {
                    g.drawString(strs[i], 3, (metrics.getHeight()) * (i + 1));
                }
            }
        }

        public Dimension getPreferredSize(JComponent c) {
            FontMetrics metrics = c.getFontMetrics(c.getFont());
            String tipText = ((JToolTip) c).getTipText();
            if (tipText == null) {
                tipText = "";
            }
            BufferedReader br = new BufferedReader(new StringReader(tipText));
            String line;
            int maxWidth = 0;
            Vector<String> v = new Vector<String>();
            try {
                while ((line = br.readLine()) != null) {
                    int width = SwingUtilities.computeStringWidth(metrics,
                                                                  line);
                    maxWidth = (maxWidth < width) ? width : maxWidth;
                    v.addElement(line);
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            int lines = v.size();
            if (lines < 1) {
                strs = null;
                lines = 1;
            } else {
                strs = new String[lines];
                int i = 0;
                for (Enumeration e = v.elements(); e.hasMoreElements(); i++) {
                    strs[i] = (String) e.nextElement();
                }
            }
            int height = metrics.getHeight() * lines;
            return new Dimension(maxWidth + 6, height + 4);
        }
    }
}
