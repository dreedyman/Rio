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

import org.rioproject.annotation.Started
import org.rioproject.util.PropertyHelper
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * A service that is dispatched to all Cybernodes, and used to collect (harvest)
 * service log files and such
 */
public class HarvesterAgent {
    Harvester harvester
    Map<String, Object> parameters
    static final String MATCH = "match"
    static final String PREFIX = "prefix"
    private static final Logger logger = LoggerFactory.getLogger(HarvesterAgent.class)

    @Started
    def harvest() {
        String hostName = InetAddress.localHost.hostName

        def dirParm = parameters.get("directories")
        if(!dirParm)
            dirParm = "${System.getProperty("rio.log.dir")}"
        logger.info("dirParam: {}", dirParm)
        def dirs = dirParm.tokenize(":")

        def matchOn
        def matchParm = parameters.get(MATCH)
        if(matchParm)
            matchOn = matchParm.tokenize(", ")

        def prefix = parameters.get(PREFIX)
        if(!prefix) {
            prefix = ""
        } else {
            if(!prefix.endsWith(File.separator))
                prefix = "${prefix}${File.separator}"
        }

        Thread.start {
            Map<String, List<File>> fileMap = new HashMap<String, List<File>>()
            for(String sDir : dirs) {
                sDir = PropertyHelper.expandProperties(sDir)
                logger.info("Checking ${sDir} ...")
                List<File> fileList = buildFileList(sDir, matchOn)
                logger.info("${sDir} returned ${fileList.size()} matching files")
                if(fileList.size()>0)
                    fileMap.put(sDir, fileList)
            }

            while(harvester==null) {
                logger.info "Waiting for Harvester to come online..."
                Thread.sleep(1000)
            }

            logger.info "Harvester is online."
            fileMap.each { parent, fileList ->
                logger.info "${parent} -> ${fileList}"
            }
            HarvesterSession session = harvester.connect()
            logger.info("Obtained {}", session)
            Socket socket = new Socket(session.host, session.port)
            logger.info("Connected to HarvesterBean: {}", session)
            socket.withStreams { input, output ->
                def writer = new PrintWriter(output)
                if(fileMap.size()==0) {
                    writer.write("empty\n")
                    writer.flush()
                    logger.info "No files to send, exit immediately"
                    return
                }
                fileMap.each { parent, fileList ->
                    logger.info "Directory ${parent} has ${fileList.size()} files to send"
                    for(File file : fileList) {
                        logger.info "Processing file [${file.absolutePath}]"
                        String filePath = file.absolutePath
                        String baseDirName = filePath.substring(parent.length())
                        if(!baseDirName.startsWith(File.separator))
                            baseDirName = File.separator+baseDirName
                        logger.info "Sending file    [filename:${prefix}${hostName}${baseDirName}]"
                        writer.write("filename:${prefix}${hostName}${baseDirName}\n")
                        writer.flush()
                        file.eachLine { line ->
                            //print "."
                            writer.write("${line}\n")
                            writer.flush()
                        }
                        //println "."
                    }
                }
            }
        }
    }

    def buildFileList(String sDir, def matchList) {
        List<File> list = new ArrayList<File>()
        File dir = new File(sDir)
        if(!(dir.exists() && dir.isDirectory())) {
            logger.info("The [$sDir] directory does not exist or is not a directory")
        }

        for(File f : dir.listFiles()) {
            if(f.isDirectory()) {
                //println("${f.getPath()} is a directory, recurse into it")
                list.addAll(buildFileList(f.absolutePath, matchList))
            } else {
                if(matchList) {
                    for(String match : matchList) {
                        if(match.startsWith("*")) {
                            match = match.substring(1)
                        }
                        //println("Check if [${f.absolutePath}] ends with [${match}]")
                        if(f.name.endsWith(match)) {
                            list.add(f)
                            //println("Added [${f.absolutePath}]")
                            break
                        }
                    }
                } else {
                    list.add(f)
                }
            }
        }

        return list
    }
}