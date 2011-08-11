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

import org.rioproject.bean.Started
import org.rioproject.util.PropertyHelper

/**
 * A service that is dispatched to all Cybernodes, and used to collect (harvest)
 * service log files and such
 */
public class HarvesterAgent {
    Harvester harvester
    Map<String, Object> parameters
    static final String MATCH = "match"
    static final String PREFIX = "prefix"

    @Started
    def harvest() {
        String hostName = InetAddress.localHost.hostName

        def dirParm = parameters.get("directories")
        if(!dirParm)
            dirParm = ["${System.getProperty("java.io.tmpdir")}/service_logs"]
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
                println("===> Checking ${sDir} ...")
                List<File> fileList = buildFileList(sDir, matchOn)
                println("===> ${sDir} returned ${fileList.size()} matching files")
                if(fileList.size()>0)
                    fileMap.put(sDir, fileList)
            }

            while(harvester==null) {
                println "Waiting for Harvester to come online..."
                Thread.sleep(1000)
            }

            println "Harvester is online."
            fileMap.each { parent, fileList ->
                println "${parent} -> ${fileList}"
                println "---"
            }
            HarvesterSession session = harvester.connect()
            Socket socket = new Socket(session.host, session.port)
            socket.withStreams { input, output ->
                def writer = new PrintWriter(output)
                if(fileMap.size()==0) {
                    writer.write("empty\n")
                    writer.flush()
                    println "No files to send, exit immediately"
                    return
                }
                fileMap.each { parent, fileList ->
                    println "===> Directory ${parent} has ${fileList.size()} files to send"
                    for(File file : fileList) {
                        println "===> Processing file [${file.absolutePath}]"
                        String filePath = file.absolutePath
                        String baseDirName = filePath.substring(parent.length())
                        if(!baseDirName.startsWith(File.separator))
                            baseDirName = File.separator+baseDirName
                        println "     Sending file    [filename:${prefix}${hostName}${baseDirName}]"
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
            println("The [$sDir] directory does not exist or is not a directory")
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