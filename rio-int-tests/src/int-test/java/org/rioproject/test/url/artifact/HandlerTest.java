/*
 * Copyright 2011 the original author or authors
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
package org.rioproject.test.url.artifact;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.rioproject.url.ProtocolRegistryService;
import org.rioproject.url.artifact.Handler;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

public class HandlerTest {
    @BeforeClass
    public static void setupURLStreamHandlerFactory() {
        ProtocolRegistryService.create().register("artifact", new Handler());
    }

    @Test
    @Ignore
    public void testURL() throws IOException {
        URL u = new URL("artifact:org.apache.maven.surefire/surefire-junit4/2.6");
        URLConnection connect = u.openConnection();
        Assert.assertNotNull(connect);
    }

    @Test
    @Ignore
    public void testURLWithRepositories() throws IOException {
        URL u = new URL("artifact:org.rioproject/boot/4.0;https://repo.repsy.io/mvn/dreedy/maven");
        URLConnection connect = u.openConnection();
        Assert.assertNotNull(connect);
    }

}
