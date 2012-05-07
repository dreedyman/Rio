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
package org.rioproject.tools.ui.serviceui;

import net.jini.core.lookup.ServiceItem;
import org.rioproject.opstring.ServiceElement;
import org.rioproject.tools.ui.Constants;
import org.rioproject.ui.Util;

import javax.swing.*;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyVetoException;
import java.util.HashMap;
import java.util.Map;

/**
 * The DesktopAdminFrame class creates ServiceUIPanels in JInternalFrames
 *
 * @author Dennis Reedy
 */
/* PMD complains that cascade() is called during creation. This is okay */
@SuppressWarnings("PMD.ConstructorCallsOverridableMethod")
public class DesktopAdminFrame extends JFrame {
    private JDesktopPane desktop;
    private JPanel tray;
    private String lastWindowLayout = Constants.ADMIN_FRAME_WINDOW_TILE;
    private JCheckBoxMenuItem autoLayout;
    final Map<String, JInternalFrame> trayItems = new HashMap<String, JInternalFrame>();

    public DesktopAdminFrame(ServiceElement elem,
                             Map<String, ServiceItem> items) throws Exception {
        super("Service UIs for " + elem.getName());
        desktop = new JDesktopPane();
        desktop.putClientProperty("JDesktopPane.dragMode", "outline");
        JPanel content = new JPanel(new BorderLayout());
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEtchedBorder(),
            BorderFactory.createEmptyBorder(2, 4, 4, 2)));
        bottom.add(new JLabel(items.size()+" of "+elem.getPlanned()+" services"),
                   BorderLayout.EAST);
        tray = new JPanel();
        tray.setLayout(new FlowLayout(FlowLayout.LEFT, 8, 0));       
        //JLabel l = new JLabel("<html>&nbsp;</html>");
        //tray.add(l);
        bottom.add(tray, BorderLayout.CENTER);
        content.add(bottom, BorderLayout.SOUTH);
        JToolBar toolBar = new JToolBar(JToolBar.HORIZONTAL);
        toolBar.setFloatable(false);
        toolBar.setRollover(true);
        toolBar.setBorderPainted(false);

        ImageIcon cascadeIcon =
            Util.getImageIcon("org/rioproject/tools/ui/images/cascade.png");
        ImageIcon tileIcon =
            Util.getImageIcon("org/rioproject/tools/ui/images/tile.png");
        JButton cascade = new JButton(cascadeIcon);
        cascade.setToolTipText("Cascade the windows");
        cascade.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                cascade();
            }
        });

        JButton tile = new JButton(tileIcon);
        tile.setToolTipText("Tile the windows");
        tile.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                tile();
            }
        });
        toolBar.add(cascade);
        toolBar.add(tile);
        content.add(toolBar, BorderLayout.NORTH);
        content.add(desktop, BorderLayout.CENTER);
        getContentPane().add(content);
        //setContentPane(desktop);
        setJMenuBar(createJMenuBar());
        FrameListener listener = new FrameListener();

        for(Map.Entry<String, ServiceItem> entry : items.entrySet()) {
            String name = entry.getKey();
            ServiceItem item = entry.getValue();
            AdminInternalFrame iFrame = new AdminInternalFrame(name, item);
            iFrame.addInternalFrameListener(listener);
            desktop.add(iFrame);
            addToTray(name, iFrame);
        }
    }

    private void addToTray(String name, JInternalFrame iFrame) {
        JLabel l = createLabel(name);
        l.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent me) {
                JLabel l = (JLabel)me.getComponent();
                JInternalFrame iFrame = trayItems.get(l.getText());
                if(me.getClickCount()==2) {
                    iFrame.setVisible(false);
                } else {
                    try {
                        iFrame.setIcon(false);
                        iFrame.setVisible(true);
                        iFrame.setSelected(true);
                    } catch (PropertyVetoException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        l.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEtchedBorder(),
            BorderFactory.createEmptyBorder(0, 2, 0, 2)));
        tray.add(l);
        trayItems.put(name, iFrame);
    }

    private JLabel createLabel(String name) {
        final JLabel l = new JLabel(name) {
            //public Color getBackground() {
            //    return(new Color(230, 230, 230));
            //}
            public void paintComponent(final Graphics g) {
                JInternalFrame iFrame = trayItems.get(getText());
                if(iFrame.isVisible()) {
                    drawGradient(g, getSize());
                    setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Color.black),
                        BorderFactory.createEmptyBorder(0, 2, 0, 2)));
                } else {
                    setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Color.gray),
                        BorderFactory.createEmptyBorder(0, 2, 0, 2)));
                }
                super.paintComponent(g);
            }
        };
        l.setDoubleBuffered(true);
        return(l);
    }

    private void drawGradient(Graphics g, Dimension dim) {
        Graphics2D g2 = (Graphics2D)g;
        Color startColor = Color.white;
        Color endColor = Color.gray;
        GradientPaint gradient = new GradientPaint(dim.height,
                                                   dim.width,
                                                   startColor,
                                                   0,
                                                   dim.width,
                                                   endColor);
        g2.setPaint(gradient);
        g2.fillRect(0, 0, dim.width, dim.height);
    }
    
    public String getLastWindowLayout() {
        return lastWindowLayout;
    }

    public boolean getAutoLayout() {
        return autoLayout.isSelected();
    }

    private JMenuBar createJMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);

        JMenu windowMenu = new JMenu("Window");
        menuBar.add(windowMenu);

        JMenuItem cascadeItem = new JMenuItem("Cascade");
        cascadeItem.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent event) {
                cascade();
            }
        });
        windowMenu.add(cascadeItem);

        JMenuItem tileItem = new JMenuItem("Tile");
        tileItem.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent event) {
                tile();
            }
        });
        windowMenu.add(tileItem);

        autoLayout = new JCheckBoxMenuItem("Auto Layout");
        autoLayout.setSelected(true);
        windowMenu.add(autoLayout);
        return menuBar;
    }

    public void cascade() {
        JInternalFrame[] frames = desktop.getAllFrames();
        if (frames.length == 0)
            return;
        Rectangle dBounds = desktop.getBounds();
        int separation = 24;
        int margin = frames.length*separation + separation;
        int width = dBounds.width - margin;
        int height = dBounds.height - margin;
        for (int i = 0; i < frames.length; i++) {
            frames[i].setBounds(//separation + dBounds.x + i*separation,
                                //separation + dBounds.y + i*separation,
                                separation + i*separation,
                                separation + i*separation,
                                width,
                                height);
            frames[i].moveToFront();
        }
        try {
            frames[frames.length-1].setSelected(true);
        } catch (PropertyVetoException e) {
            e.printStackTrace();
        }
        lastWindowLayout = Constants.ADMIN_FRAME_WINDOW_CASCADE;
    }

    public void tile() {
        JInternalFrame[] frames = desktop.getAllFrames();
        Rectangle dBounds = desktop.getBounds();
        if ( frames.length == 0)
            return;
        int cols = (int)Math.sqrt(frames.length);
        int rows = (int)(Math.ceil( ((double)frames.length) / cols));
        int lastRow = frames.length - cols*(rows-1);
        int width, height;

        if (lastRow == 0 ) {
            rows--;
            height = dBounds.height / rows;
        }
        else {
            height = dBounds.height / rows;
            if ( lastRow < cols ) {
                rows--;
                width = dBounds.width / lastRow;
                for (int i = 0; i < lastRow; i++ ) {
                    frames[cols*rows+i].setBounds( i*width, rows*height,
                                                   width, height );
                }
            }
        }

        width = dBounds.width/cols;
        for (int j = 0; j < rows; j++ ) {
            for (int i = 0; i < cols; i++ ) {
                frames[i+j*cols].setBounds( i*width, j*height,
                                            width, height );
            }
        }
        try {
            frames[frames.length-1].setSelected(true);
        } catch (PropertyVetoException e) {
            e.printStackTrace();
        }
        lastWindowLayout = Constants.ADMIN_FRAME_WINDOW_TILE;
    }

    private class FrameListener implements InternalFrameListener {
        final Map<String, JInternalFrame> minimized = new HashMap<String, JInternalFrame>();

        public void internalFrameOpened(InternalFrameEvent event) {
        }

        public void internalFrameClosing(InternalFrameEvent event) {
        }

        public void internalFrameClosed(InternalFrameEvent event) {
        }

        public void internalFrameIconified(InternalFrameEvent event) {
            event.getInternalFrame().setVisible(false);
            tray.repaint();
            autoLayout();
        }

        public void internalFrameDeiconified(InternalFrameEvent event) {
            autoLayout();
        }

        public void internalFrameActivated(InternalFrameEvent event) {
        }

        public void internalFrameDeactivated(InternalFrameEvent event) {
            //autoLayout();
        }

        void autoLayout() {
            if(getAutoLayout()) {
                if(lastWindowLayout.equals(Constants.ADMIN_FRAME_WINDOW_CASCADE))
                    cascade();
                else
                    tile();
            }
        }
    }
}
