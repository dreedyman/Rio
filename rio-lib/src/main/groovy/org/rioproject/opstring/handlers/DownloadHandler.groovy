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
import java.util.logging.Logger

/**
 * Handles the parsing for Download elements
 *
 * @author Jerome Bernard
 */
class DownloadHandler implements Handler {
    /** A suitable Logger */
    def logger = Logger.getLogger("org.rioproject.opstring")

    public parse(Object element, Object options) {
        logger.fine "Parsing download from $element"

        def parent = options['parent']
        
        def source = element.'@Source'
        def installRoot = element.'@InstallRoot'
        def unarchive = element.'@Unarchive'
        def location = element.Location.text()

        boolean remove = true
        String sRemoveOnDestroy = parent.'@RemoveOnDestroy'
        if (sRemoveOnDestroy)
            remove = (sRemoveOnDestroy == "yes")
        String sRemoveOnCompletion = parent.'@RemoveOnCompletion'
        if (sRemoveOnCompletion)
            remove = (sRemoveOnCompletion == "yes")

        return new StagedData("$location$source", installRoot, unarchive == "yes", remove)
    }

}
