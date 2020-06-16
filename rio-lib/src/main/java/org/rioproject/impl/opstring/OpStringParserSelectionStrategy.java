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
package org.rioproject.impl.opstring;

import org.rioproject.impl.opstring.OpStringParser;

/**
 * @author Jerome Bernard
 */
public interface OpStringParserSelectionStrategy {

    /**
     * Selects an {@link org.rioproject.impl.opstring.OpStringParser} based on
     * the source.
     *
     * @param source either a local {@link java.io.File} or a {@link java.net.URL}
     * to the OpString deployment descriptor.
     *
     * @return the {@link org.rioproject.impl.opstring.OpStringParser} which
     * should be used for the source
     */
    OpStringParser findParser(Object source);

}
