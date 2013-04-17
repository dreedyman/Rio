/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.rioproject.tools.ui.browser;

import com.sun.jini.outrigger.AdminIterator;
import com.sun.jini.outrigger.JavaSpaceAdmin;
import net.jini.core.entry.Entry;
import net.jini.core.entry.UnusableEntryException;
import net.jini.core.lease.Lease;
import net.jini.space.JavaSpace05;
import net.jini.space.MatchSet;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

/**
 * A browser utility to browse entries in a specified space.
 *
 * @version 0.2 06/04/98
 */
class SpaceBrowser extends JFrame {
    //private SpaceEntryPanel entryPanel;
    private final static int MINIMUM_WINDOW_WIDTH = 320;
    private Browser browser;

    public SpaceBrowser(Object proxy, Browser browser) {
        super("SpaceBrowser");

        this.browser = browser;
        // init main components
        SpaceEntryPanel entryPanel = new SpaceEntryPanel(proxy);

        // add menu and attr panel
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(new BrowserMenuBar(entryPanel), "North");
        getContentPane().add(entryPanel, "Center");

        validate();
        pack();
        setSize(((getSize().width < MINIMUM_WINDOW_WIDTH) ? MINIMUM_WINDOW_WIDTH : getSize().width),
                getSize().height);

        // center in parent frame
        Rectangle bounds = browser.getBounds();
        Dimension dialogSize = getPreferredSize();
        int xpos = bounds.x + (bounds.width - dialogSize.width) / 2;
        int ypos = bounds.y + (bounds.height - dialogSize.height) / 2;
        setLocation((xpos < 0) ? 0 : xpos,
                    (ypos < 0) ? 0 : ypos);
    }


    class BrowserMenuBar extends JMenuBar {
        private EntryTreePanel entryPanel;

        public BrowserMenuBar(EntryTreePanel entryPanel) {
            this.entryPanel = entryPanel;
            JMenuItem mitem;

            // "File" Menu
            JMenu fileMenu = (JMenu) add(new JMenu("File"));
            mitem = (JMenuItem) fileMenu.add(new JMenuItem("Refresh"));
            mitem.addActionListener(browser.wrap(new ActionListener() {
                public void actionPerformed(ActionEvent ev) {
                    BrowserMenuBar.this.entryPanel.refreshPanel();
                }
            }));
            mitem = (JMenuItem) fileMenu.add(new JMenuItem("Close"));
            mitem.addActionListener(browser.wrap(new ActionListener() {
                public void actionPerformed(ActionEvent ev) {
                    SpaceBrowser.this.setVisible(false);
                }
            }));
        }
    }

    class SpaceEntryPanel extends EntryTreePanel {
        private Object proxy;

        public SpaceEntryPanel(Object proxy) {
            super(false);    // Entries are not editable.
            this.proxy = proxy;

            refreshPanel();
        }

        protected Entry[] getEntryArray() {
            try {
                List acc = new java.util.LinkedList();
                if (proxy instanceof JavaSpace05) {
                    MatchSet set =
                        ((JavaSpace05) proxy).contents(Collections.singleton(null),
                                                       null, Lease.ANY, Integer.MAX_VALUE);
                    Lease lease = set.getLease();
                    if (lease != null) {
                        lease = (Lease) browser.getLeasePreparer().prepareProxy(lease);
                        browser.getLeaseManager().renewUntil(lease, Lease.ANY, null);
                    }
                    try {
                        while (true) {
                            try {
                                Entry e = set.next();
                                if (e == null)
                                    break;
                                acc.add(e);
                            } catch (UnusableEntryException e) {
                                Browser.logger.log(Level.INFO, "unusable entry", e);
                            }
                        }
                    } finally {
                        if (lease != null) {
                            try {
                                browser.getLeaseManager().cancel(lease);
                            } catch (Exception e) {
                            }
                        }
                    }
                } else {
                    AdminIterator iter = ((JavaSpaceAdmin) proxy).contents(null, null, 128);
                    try {
                        while (true) {
                            try {
                                Entry e = iter.next();
                                if (e == null)
                                    break;
                                acc.add(e);
                            } catch (UnusableEntryException e) {
                                Browser.logger.log(Level.INFO, "unusable entry", e);
                            }
                        }
                    } finally {
                        try {
                            iter.close();
                        } catch (Exception e) {
                        }
                    }
                }
                return (Entry[]) acc.toArray(new Entry[acc.size()]);
            } catch (Throwable t) {
                Browser.logger.log(Level.INFO, "obtaining entries failed", t);
            }

            return null;
        }
    }
}

