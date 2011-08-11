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
import org.rioproject.opstring.ParsedService

/**
 * Handles the parsing for the Data element
 *
 * @author Jerome Bernard
 */
class DataHandler implements Handler {
    public parse(Object element, Object options) {
        def ParsedService sDescriptor = options['serviceDescriptor']

        def removeOnDestroy = element.'@RemoveOnDestroy'
        def overwrite = element.'@Overwrite'
        def unarchive = element.'@Unarchive'
        def perms = element.'@Perms'
        def fileName = element.FileName.text()
        def target = element.Target.text()
        def source = element.Source.text()
        def targetFileName = element.TargetFileName.text()
        targetFileName = (targetFileName?targetFileName:fileName)
        
        //println "*******************"
        //println " ["+sDescriptor.name+"] TARGET FILE NAME = "+targetFileName+", FILE NAME = "+fileName+", ELEMENT = "+element
        //println " ["+sDescriptor.name+"] ELEMENT = "+element
        //println "*******************"
        if (fileName && !source.endsWith("/"))
            source = source + "/"

        sDescriptor.stagedData = new StagedData("$source$fileName",
                                                target,
                                                unarchive == "yes",
                                                removeOnDestroy == "yes",
                                                overwrite == "yes",
                                                perms ? perms : null)
        return sDescriptor.stagedData
    }
}
