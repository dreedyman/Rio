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
package org.rioproject.boot;

import java.io.InterruptedIOException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.DatagramPacket;

/**
 * Class which can be used to determine whether multicast is available. Credit
 * goes to Alexander V. Konstantinou
 */
public class MulticastStatus {
    /**
     * Determines if a multicast socket can be created and a Request
     * annnouncement can be sent. Note that it is still possible that multicast
     * does not work outside the host.
     *
     * @param timeout The timeout to use hen checking the status
     *
     * @throws IOException If there are errors interfacing ith the network
     */
    public static void checkMulticast(final int timeout)
        throws java.io.IOException {
        if(timeout < 0)
            throw new IllegalArgumentException("Invalid timeout = " + timeout);
        /** Use Jini's multicast group */
        final InetAddress group = net.jini.discovery.Constants.getRequestAddress();
        /** Use a different port from Jini discovery */
        final short port = net.jini.discovery.Constants.discoveryPort + 1;
        final long endMillis = System.currentTimeMillis() + timeout;
        final MulticastSocket msocket = new MulticastSocket(port);
        msocket.setTimeToLive(1); // XXX - could it be 0 ?
        msocket.joinGroup(group);
        msocket.setSoTimeout(timeout);
        final String message = MulticastStatus.class.getName()
                               + ".ping("
                               + System.currentTimeMillis()
                               + ")";
        final byte[] messageBytes = message.getBytes();
        DatagramPacket packet = new DatagramPacket(messageBytes,
                                                   messageBytes.length,
                                                   group,
                                                   port);
        final Object lock = new Object();
        final Boolean[] received = new Boolean[]{Boolean.FALSE};
        /**
         * Receive thread waits for msocket SOTIMEOUT to receive a packet. The
         * exception catching code is needed to handle Java multicast exceptions
         * caused by bug 4190513.
         */
        Thread receiveThread = new Thread("MulticastStatus") {
            public void run() {
                byte[] buf = new byte[1024];
                DatagramPacket recv = new DatagramPacket(buf, buf.length);
                do {
                    try {
                        msocket.receive(recv);
                        synchronized(lock) {
                            received[0] = Boolean.TRUE;
                        }
                        return;
                    } catch(InterruptedIOException e) {
                        return;
                    } catch(Throwable e) {
                        long remainingMillis = endMillis
                                               - System.currentTimeMillis();
                        if(remainingMillis <= 0) {
                            return;
                        } else {
                            try {
                                msocket.setSoTimeout((int)remainingMillis);
                            } catch(SocketException e2) {
                                return;
                            }
                        }
                    }
                } while (true);
            }
        };
        receiveThread.start();
        // Start sending packets every 500 msec until the timeout expires
        // or a packet is received
        do {
            msocket.send(packet);
            synchronized(lock) {
                if(received[0]) {
                    msocket.leaveGroup(group);
                    msocket.close();
                    return;
                }
            }
            try {
                Thread.sleep(500);
            } catch(InterruptedException e) {
                e.printStackTrace();
            }
        } while (System.currentTimeMillis() < endMillis);
        msocket.leaveGroup(group);
        msocket.close();
        throw new IOException("Multicast packets were not received in the "+
                              "alloted time");
    }

    public static void main(String[] args) {
        try {
            checkMulticast(1000 * 5);
            System.out.println("Multicast check succesful");
        } catch(IOException ioe) {
            ioe.printStackTrace();
        }
    }
}
