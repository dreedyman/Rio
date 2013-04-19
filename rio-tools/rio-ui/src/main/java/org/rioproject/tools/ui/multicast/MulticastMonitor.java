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
package org.rioproject.tools.ui.multicast;

import net.jini.discovery.Constants;
import net.jini.discovery.IncomingMulticastAnnouncement;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Monitor lookup service multicast announcements.
 *
 * @author Dennis Reedy
 */
public class MulticastMonitor extends JPanel {
    private final JEditorPane area = new JEditorPane();
    private static JDialog dialog;

    private MulticastMonitor() {
        super(new BorderLayout(8,8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JPanel buttons = new JPanel();
        JButton clear = new JButton("Clear");
        area.setEditable(false);
        final AnnouncementListener announcementListener = new AnnouncementListener();
        clear.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    area.getDocument().remove(0, area.getDocument().getLength());
                } catch (BadLocationException e1) {
                    e1.printStackTrace();
                }
            }
        });
        JButton dismiss = new JButton("Close");
        dismiss.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                announcementListener.keepRunning.set(false);
                dialog.dispose();
            }
        });
        buttons.add(clear);
        buttons.add(dismiss);
        JLabel description = new JLabel();
        description.setText("<html><body>This utility waits for multicast announcements from Lookup Services.</body></html>");
        add(description, BorderLayout.NORTH);
        add(new JScrollPane(area), BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);

        new Thread(announcementListener).start();
    }

    /**
     * Get the dialog for the MulticastMonitor
     *
     * @param frame The parent frame
     *
     * @return The JDialog which will encapsulate the MulticastMonitor
     */
    public static JDialog getDialog(JFrame frame) {
        dialog = new JDialog(frame, "Multicast Monitor", true);
        MulticastMonitor multicastMonitor = new MulticastMonitor();
        dialog.getContentPane().add(multicastMonitor);
        int width = 450;
        int height = 340;
        dialog.pack();
        dialog.setSize(width, height);
        dialog.setModal(false);
        dialog.setLocationRelativeTo(frame);
        return (dialog);
    }

    class AnnouncementListener implements Runnable {
        InetAddress announcementAddress;
        MulticastSocket multicastSocket;
        AtomicBoolean keepRunning = new AtomicBoolean(true);
        DateFormat format = new SimpleDateFormat("HH:mm:ss");

        AnnouncementListener() {
            super();
        }

        void terminate() {
            keepRunning.set(false);
            if(multicastSocket != null)
                multicastSocket.close();
        }

        public void run() {
            try {
                byte[] packet = new byte[2048];
                announcementAddress = Constants.getAnnouncementAddress();
                multicastSocket = new MulticastSocket(Constants.discoveryPort);
                multicastSocket.joinGroup(announcementAddress);
                DatagramPacket annPacket;
                while (keepRunning.get()) {
                    IncomingMulticastAnnouncement ima;
                    annPacket = new DatagramPacket(packet, packet.length);
                    multicastSocket.receive(annPacket);
                    Date when;
                    try {
                        ima = new IncomingMulticastAnnouncement(annPacket);
                        when = new Date(System.currentTimeMillis());
                    } catch(IOException ioe) {
                        System.err.println("Unrecognized packet type in the AnnouncementListener");
                        continue;
                    }
                    StringBuilder groups = new StringBuilder();
                    for(String group : ima.getGroups()) {
                        if(groups.length()>0)
                            groups.append(", ");
                        groups.append(group);
                    }
                    Document document = area.getDocument();
                    document.insertString(document.getLength(),
                                          String.format("%s %s:%s [%s]\n",
                                                        format.format(when),
                                                        ima.getLocator().getHost(),
                                                        ima.getLocator().getPort(),
                                                        groups.toString()),
                                          null);
                }
            } catch(Exception e) {
                System.err.println("AnnouncementListener: "+ e.getClass().getName()+ ": "+e.getMessage()+", exiting");
                terminate();
            }
        }
    }
}

