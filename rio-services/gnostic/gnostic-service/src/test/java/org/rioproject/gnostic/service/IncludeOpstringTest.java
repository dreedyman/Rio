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
package org.rioproject.gnostic.service;

import junit.framework.Assert;
import org.junit.Test;
import org.rioproject.impl.opstring.GroovyDSLOpStringParser;
import org.rioproject.impl.opstring.OpString;

import java.io.File;
import java.util.List;

/**
 * Test that the gnostic operational string gets included
 *
 * @author Dennis Reedy
 */
public class IncludeOpstringTest {
    @Test
    public void load1()  {
        String baseDir = System.getProperty("user.dir");
        File file = new File(baseDir, "../src/main/opstring/gnostic.groovy");
        GroovyDSLOpStringParser parser = new GroovyDSLOpStringParser();
        List<OpString> opStrings = parser.parse(file, null, null, null);
        Assert.assertEquals(1, opStrings.size());
    }

    @Test
    public void load2()  {
        String baseDir = System.getProperty("user.dir");
        File file = new File(baseDir, "src/test/opstring/testOpString.groovy");
        GroovyDSLOpStringParser parser = new GroovyDSLOpStringParser();
        List<OpString> opStrings = parser.parse(file, null, null, null);
    }
}
