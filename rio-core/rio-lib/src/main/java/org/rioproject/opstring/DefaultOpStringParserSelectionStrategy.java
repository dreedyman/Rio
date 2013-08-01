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
package org.rioproject.opstring;

import java.net.URL;
import java.io.File;

/**
 * {@link OpStringParserSelectionStrategy} selecting either the XML or Groovy parser.
 *
 * @author Dennis Reedy
 * @author Jerome Bernard
 */
public class DefaultOpStringParserSelectionStrategy implements OpStringParserSelectionStrategy {

    public OpStringParser findParser(Object source) {
        OpStringParser parser;
        String filename;

        // handle either local filename or URL as source
        if (source instanceof URL || source instanceof File) {
            filename = source.toString();
        } else {
            throw new UnsupportedOperationException("There is no support for "
                                                    + source.getClass().getName() + " source");
        }

        /* Determine we are using Groovy */
        if (filename.endsWith(".groovy"))
            parser = new GroovyDSLOpStringParser();
        else
            throw new UnsupportedOperationException("There is no support for " + filename +  " format");

        return parser;
    }
}
