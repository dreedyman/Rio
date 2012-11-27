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
package org.rioproject.tools.ui;

import net.jini.config.Configuration;
import net.jini.config.ConfigurationException;
import net.jini.core.lease.LeaseDeniedException;
import net.jini.discovery.DiscoveryManagement;
import org.rioproject.eventcollector.api.EventCollector;
import org.rioproject.eventcollector.api.UnknownEventCollectorRegistration;
import org.rioproject.tools.ui.cybernodeutilization.CybernodeUtilizationPanel;
import org.rioproject.tools.ui.servicenotification.RemoteEventTable;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.io.IOException;
import java.rmi.server.ExportException;
import java.util.Properties;

/**
 * Container for utilities
 */
public class UtilitiesPanel extends JPanel {
    private final RemoteEventTable remoteEventTable;

    public UtilitiesPanel(CybernodeUtilizationPanel cup,
                          Configuration config,
                          Properties props) throws ExportException, ConfigurationException {
        super(new BorderLayout());
        remoteEventTable = new RemoteEventTable(config, props);

        JTabbedPane tabs = new JTabbedPane();
        tabs.add("Utilization", cup);
        JLabel label = makeTabLabel("Service Notifications",
                                    remoteEventTable);
        tabs.addTab(null, remoteEventTable);
        tabs.setTabComponentAt(1, label);
        add(tabs, BorderLayout.CENTER);
    }

    Properties getOptions() {
        Properties props = new Properties();
        props.put(Constants.USE_EVENT_COLLECTOR, Boolean.toString(remoteEventTable.getUseEventCollector()));
        props.put(Constants.EVENTS_DIVIDER, Integer.toString(remoteEventTable.getDividerLocation()));
        return props;
    }

    void setDiscoveryManagement(DiscoveryManagement dMgr) throws Exception {
        remoteEventTable.setDiscoveryManagement(dMgr);
    }

    private JLabel makeTabLabel(String name, NotificationUtility... comps) {
        NotificationNode nn = new NotificationNode(comps);
        for(NotificationUtility nu : comps)
            nu.subscribe(nn);
        JLabel l = new TabLabel(name, nn);
        nn.setLabel(l);
        return l;
    }

    void addEventCollector(EventCollector eventCollector) throws IOException, LeaseDeniedException, UnknownEventCollectorRegistration {
        remoteEventTable.addEventCollector(eventCollector);
    }

    void removeEventCollector(EventCollector eventCollector) {
        remoteEventTable.removeEventCollector(eventCollector);
    }

    void stopNotifications() {
        remoteEventTable.terminate();
    }

    class TabLabel extends JLabel {
        private NotificationNode node;
        Dimension originalSize;

        TabLabel(String s, NotificationNode node) {
            super(s);
            this.node = node;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if(node.getCount()>0) {
                String countString = Integer.toString(node.getCount());
                g.setColor(new Color(227, 6, 19));

                int y = g.getFontMetrics().getAscent();
                int startX = g.getFontMetrics().stringWidth(getText());
                int countW = g.getFontMetrics().stringWidth(countString)+8;

                Shape shape = new RoundRectangle2D.Double(startX+4,
                                                          2,
                                                          countW,
                                                          (MacUIHelper.isMacOS()?getHeight()-4:getHeight()-2),
                                                          16,
                                                          16);
                Graphics2D g2d = (Graphics2D)g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                     RenderingHints.VALUE_ANTIALIAS_ON );
                g2d.fill(shape);
                g.setColor(Color.white);
                Font currentFont = g.getFont();
                g.setFont(Constants.NOTIFY_COUNT_FONT);
                g.drawString(countString, startX+8, y+1);
                g.setFont(currentFont);
            }
        }

        @Override
        public Dimension getPreferredSize() {
            if(node.getCount()>0) {
                FontMetrics fm = getGraphics().getFontMetrics(getFont());
                String countString = Integer.toString(node.getCount());
                int countLength = fm.stringWidth(countString)+12;
                int textLength = fm.stringWidth(getText())+countLength;
                if(originalSize==null)
                    originalSize = super.getPreferredSize();
                Dimension d = super.getPreferredSize();
                d.width = textLength;
                return d;
            } else {
                if(originalSize==null)
                    originalSize = super.getPreferredSize();
                return originalSize;
            }
        }
    }

    class NotificationNode implements NotificationUtilityListener {
        JLabel label;
        NotificationUtility[] utility;

        NotificationNode(NotificationUtility... utility) {
            this.utility = utility;
        }

        void setLabel(JLabel label) {
            this.label = label;
        }

        int getCount() {
            int count = 0;
            for(NotificationUtility nu : utility) {
                count += nu.getTotalItemCount();
            }
            return count;
        }

        public void notify(NotificationUtility nu) {
            //getCount();            
            if(label!=null)
                label.repaint();
        }
    }
}
