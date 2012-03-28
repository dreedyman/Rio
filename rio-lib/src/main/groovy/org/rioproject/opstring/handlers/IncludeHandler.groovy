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

import org.rioproject.opstring.OpString
import org.rioproject.opstring.OpStringParser
import java.util.logging.Logger
import java.util.logging.Level
import org.rioproject.log.GroovyLogger

/**
 * Handles the parsing for the Include element
 *
 * @author Jerome Bernard
 */
class IncludeHandler implements Handler {
    def OpStringParser parser;
    /** A suitable Logger */
    def logger = new GroovyLogger("org.rioproject.opstring")
    def visited = new ArrayList()
    //def sourceMap = new HashMap()

    public parse(Object element, Object options) {
        def opString = options['opstring']
        def xmlSource = options.source
        //sourceMap.put(xmlSource, opString)
        def loader = options.loader
        def defaultExportJars = options.defaultExportJars
        def defaultGroups = options.defaultGroups
        def loadPath = options.loadPath

        logger.fine "Parsing include $element"
        String opStringRef = element.text()
        boolean resolved = false

        if (xmlSource instanceof File)
            if (new File(xmlSource.parent, opStringRef).exists())
                resolved = true

        def location
        if (resolved) {
            if (xmlSource instanceof File)
                location = new File(xmlSource.parentFile, opStringRef)
        } else {
            if (!(opStringRef.startsWith("http:") || opStringRef.startsWith("file:")) && loadPath) {
                if(loadPath.startsWith("file:")) {
                    location = loadPath.substring(5) + opStringRef
                } else {
                    location = loadPath + opStringRef
                }
                location = new File(location)
            }
        }

        /* RIO-167: detect and avoid potential circular Includes dependency
         * declarations */
        if(!visited.contains(location)) {
            visited.add(location)
            try {
                def includes = parser.parse(location,
                                            loader,
                                            false,
                                            defaultExportJars,
                                            defaultGroups,
                                            loadPath)
                if (opString) {
                    logger.info "Adding opstrings $includes"
                    opString.addOperationalString(includes as OpString[])
                }
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "Failed to include OperationalString : $opStringRef", ex)
            }
        } else {
            logger.warning "Already included ${location}, possible circular _INCLUDES_ dependency."
        }
    }
}
