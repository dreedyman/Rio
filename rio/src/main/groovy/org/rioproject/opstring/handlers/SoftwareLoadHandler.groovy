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
package org.rioproject.opstring.handlers

import org.rioproject.core.provision.StagedData
import org.rioproject.core.provision.StagedSoftware
import org.rioproject.core.provision.StagedSoftware.PostInstallAttributes
import org.rioproject.opstring.OpStringParser
import java.util.logging.Logger

/**
 * Handles the parsing for StagedSoftware elements
 *
 * @author Jerome Bernard
 */
class SoftwareLoadHandler implements Handler {
    def OpStringParser parser
    /** A suitable Logger */
    def logger = Logger.getLogger("org.rioproject.opstring")
    
    public parse(Object element, Object options) {
        logger.fine "Parsing SoftwareLoad $element"

        def opString = options['opstring']
        def global = options['global']
        def sDescriptor = options['serviceDescriptor']

        def StagedSoftware softwareDownload
        element.Download.each { download ->
            StagedData a = parser.parseElement(download,
                                               global,
                                               sDescriptor,
                                               opString)
            softwareDownload = new StagedSoftware(a.location,
                                                  a.installRoot,
                                                  a.unarchive(),
                                                  a.removeOnDestroy())

            def useAsClassPathResource = element.'@ClasspathResource'.equals("yes")
            softwareDownload.setUseAsClasspathResource(useAsClassPathResource);
        }
        element.PostInstall.each {
            def execDescriptor
            def stagedData
            it.Download.each {
                stagedData = parser.parseElement(it,
                                                 global,
                                                 sDescriptor,
                                                 opString)
            }
            it.Exec.each {
                execDescriptor = parser.parseElement(it, global, sDescriptor, opString)
            }
            def postInstall = new PostInstallAttributes(execDescriptor, stagedData)
            if (softwareDownload)
                softwareDownload.postInstallAttributes = postInstall
        }
        return softwareDownload
    }
}
