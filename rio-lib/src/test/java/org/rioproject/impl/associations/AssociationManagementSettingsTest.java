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
package org.rioproject.impl.associations;

import org.junit.Assert;
import org.junit.Test;
import org.rioproject.associations.AssociationDescriptor;
import org.rioproject.impl.associations.strategy.Utilization;
import org.rioproject.impl.opstring.GroovyDSLOpStringParser;
import org.rioproject.impl.opstring.OpString;
import org.rioproject.impl.opstring.OpStringParser;
import org.rioproject.opstring.*;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Tests associations with management settings.
 */
public class AssociationManagementSettingsTest {
    @Test
    public void testAssociationManagementParsing() {
        File file = new File("src/test/resources/opstrings/association_management.groovy");
        OpStringParser dslParser = new GroovyDSLOpStringParser();
        List<OpString> opstrings = dslParser.parse(file, null, null, null);
        Assert.assertEquals("There should be one and only one opstring", 1, opstrings.size());
        OpString opstring = opstrings.get(0);
        ServiceElement serviceElement = opstring.getServices()[0];
        Assert.assertEquals(15, serviceElement.getAssociationDescriptors()[0].getServiceDiscoveryTimeout());
        Assert.assertEquals(TimeUnit.SECONDS, serviceElement.getAssociationDescriptors()[0].getServiceDiscoveryTimeUnits());
        Assert.assertEquals("net.foo.space.SomeProxy", serviceElement.getAssociationDescriptors()[0].getProxyClass());
        Assert.assertEquals(Utilization.class.getName(), serviceElement.getAssociationDescriptors()[0].getServiceSelectionStrategy());

        for(AssociationDescriptor associationDescriptor : serviceElement.getAssociationDescriptors()) {
            Assert.assertNotNull(associationDescriptor.getGroups());
            Assert.assertEquals(1, associationDescriptor.getGroups().length);
            Assert.assertEquals("rio", associationDescriptor.getGroups()[0]);
        }
    }
}
