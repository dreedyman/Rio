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
package org.rioproject.tools.harvest

import net.jini.core.entry.Entry
import net.jini.core.lookup.ServiceID
import net.jini.discovery.DiscoveryManagement
import net.jini.export.Exporter
import net.jini.id.Uuid
import net.jini.id.UuidFactory
import net.jini.jeri.BasicILFactory
import net.jini.jeri.BasicJeriExporter
import net.jini.jeri.tcp.TcpServerEndpoint
import net.jini.lease.LeaseRenewalManager
import net.jini.lookup.JoinManager
import net.jini.lookup.entry.Name
import org.rioproject.config.Constants
import org.rioproject.deploy.ProvisionManager
import org.rioproject.net.HostUtil
import org.slf4j.LoggerFactory

import java.rmi.Remote

/**
 * Provides an implementation of  {@link Harvester}
 */
class HarvesterBean implements Harvester {
    JoinManager joiner
    ProvisionManager monitor
    final List<HarvesterSession> agentsHandled = new ArrayList<HarvesterSession>()
    def logger = LoggerFactory.getLogger(HarvesterBean.class.getName())
    String harvestDir

    def HarvesterBean(DiscoveryManagement dMgr) {
        advertise(export(), dMgr)
    }

    def export() {
        String address = HostUtil.getHostAddressFromProperty(Constants.RMI_HOST_ADDRESS)
        System.setProperty("java.rmi.server.codebase", "")
        Exporter exporter =
            new BasicJeriExporter(TcpServerEndpoint.getInstance(address,0),
                                  new BasicILFactory(),
                                  false,
                                  true)
        return exporter.export(this)
    }

    def advertise(Remote remoteRef, DiscoveryManagement dMgr) {
        Uuid uuid = UuidFactory.generate()
        ServiceID serviceID = new ServiceID(uuid.getMostSignificantBits(),
                                            uuid.getLeastSignificantBits());
        def name = [new Name("Harvester")]
        joiner = new JoinManager(remoteRef,
                                 name as Entry[],
                                 serviceID,
                                 dMgr,
                                 new LeaseRenewalManager())

    }

    def unadvertise() {
        joiner.terminate()
    }

    HarvesterSession connect() {
        String address = HostUtil.getHostAddressFromProperty(Constants.RMI_HOST_ADDRESS)
        ServerSocket server = new ServerSocket(0, 50, InetAddress.getByName(address))
        HarvesterSession hSession =
        new HarvesterSession(server.localPort,
                             InetAddress.localHost.hostName)
        Thread.start {
            File parent
            if (harvestDir) {
                if (harvestDir.startsWith(File.separator))
                    parent = new File(harvestDir)
                else
                    parent = new File(System.getProperty("user.dir"), harvestDir)
            } else {
                parent = new File(System.getProperty("user.dir"), "logs")
            }

            if (!parent.exists())
                parent.mkdirs()

            logger.info "Harvesting to directory ${parent.absolutePath}"
            handleConnect(server, hSession, parent)
            server.close()
            synchronized (agentsHandled) {
                agentsHandled.add(hSession)
            }
        }
        return hSession
    }

    List<File> handleConnect(ServerSocket server, HarvesterSession hSession, File parent) {
        List<File> harvested = new ArrayList<File>()
        Socket socket = server.accept()
        SocketAddress sockAddr = socket.remoteSocketAddress
        if (logger.isInfoEnabled())
            logger.info "Connect from HarvesterAgent " +
                        "[$sockAddr.hostName, $sockAddr.port]"
        def line
        File file
        PrintWriter writer = null
        def reader = new BufferedReader(new InputStreamReader(socket.inputStream))
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("filename:")) {
                String filename = line.substring(9)
                //println "===> filename: arg [${filename}]"
                int ndx = filename.lastIndexOf("/")
                if (ndx > 0) {
                    String parentDir = filename.substring(0, ndx)
                    //println "===> parentDir = ${parentDir}"
                    File p = new File(parent, parentDir)
                    if (!p.exists())
                        p.mkdirs()
                }
                file = new File(parent, filename)
                if (!file.exists()) {
                    file.createNewFile()
                }
                harvested.add(file)
                writer = file.newPrintWriter()
                //println "===> Using file ${file.path}"
            } else {
                if (writer) {
                    writer.write("${line}\n")
                    writer.flush()
                }
            }
        }
        return harvested
    }

    int getAgentsHandledCount() {
        int count
        synchronized (agentsHandled) {
            count = agentsHandled.size()
        }
        return count
    }
}