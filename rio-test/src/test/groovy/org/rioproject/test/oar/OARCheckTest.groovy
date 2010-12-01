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
package org.rioproject.test.oar

import java.util.jar.Attributes
import java.util.jar.Manifest
import org.rioproject.core.OperationalString
import org.rioproject.opstring.OAR
import org.rioproject.opstring.OARException
import org.rioproject.test.utils.JarUtil

/**
 * Check OAR construction.
 */
class OARCheckTest extends GroovyTestCase {

    void testBadOARCreateFromManifest() {
        Throwable t = null
        try {
            new OAR((Manifest)null)
        } catch(Exception e) {
            t = e
        }
        assertNotNull t
        assertTrue t instanceof IllegalArgumentException

        try {
            new OAR((File)null)
        } catch(Exception e) {
            t = e
        }
        assertNotNull t
        assertTrue t instanceof IllegalArgumentException
    }

    void testBadOARCreateFromManifest2() {
        Throwable t = null
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        File oarFile = createOAR(manifest)
        try {
            new OAR(oarFile)
        } catch(OARException e) {
            t = e
        }
        assertTrue t!=null
        oarFile.delete()
    }

    void testOARCreateFromManifest() {
        Throwable t = null
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0")
        manifest.getMainAttributes().putValue(OAR.OAR_NAME, "test")
        manifest.getMainAttributes().putValue(OAR.OAR_VERSION, "1.0")
        manifest.getMainAttributes().putValue(OAR.OAR_OPSTRING, "test.groovy")
        manifest.getMainAttributes().putValue(OAR.OAR_ACTIVATION, OAR.AUTOMATIC)
        OAR oar = null
        try {
            oar = new OAR(manifest)
        } catch(OARException e) {
            t = e
        }
        assertTrue t==null
        assertNotNull oar
        assertEquals "test", oar.name
        assertEquals "1.0", oar.version
        assertEquals "test.groovy", oar.opStringName
        assertEquals OAR.AUTOMATIC, oar.activationType
    }

    void testOARCreateFromURL() {
        Throwable t = null
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0")
        manifest.getMainAttributes().putValue(OAR.OAR_NAME, "test")
        manifest.getMainAttributes().putValue(OAR.OAR_VERSION, "1.0")
        manifest.getMainAttributes().putValue(OAR.OAR_OPSTRING, "test.groovy")
        manifest.getMainAttributes().putValue(OAR.OAR_ACTIVATION, OAR.AUTOMATIC)
        File oarFile = createOAR(manifest)
        OAR oar = null
        try {
            oar = new OAR(oarFile.toURI().toURL())
        } catch(OARException e) {
            e.printStackTrace()
            t = e
        }
        assertTrue t==null
        assertNotNull oar
        assertEquals "test", oar.name
        assertEquals "1.0", oar.version
        assertEquals "test.groovy", oar.opStringName
        assertEquals OAR.AUTOMATIC, oar.activationType
    }

    void testOARCreateFromJarURL() {
        Throwable t = null
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0")
        manifest.getMainAttributes().putValue(OAR.OAR_NAME, "test")
        manifest.getMainAttributes().putValue(OAR.OAR_VERSION, "1.0")
        manifest.getMainAttributes().putValue(OAR.OAR_OPSTRING, "test.groovy")
        manifest.getMainAttributes().putValue(OAR.OAR_ACTIVATION, OAR.AUTOMATIC)
        File oarFile = createOAR(manifest)
        OAR oar = null
        try {
            oar = new OAR(new URL("jar:${oarFile.toURI().toURL()}!/"))
        } catch(OARException e) {
            e.printStackTrace()
            t = e
        }
        assertTrue t==null
        assertNotNull oar
        assertEquals "test", oar.name
        assertEquals "1.0", oar.version
        assertEquals "test.groovy", oar.opStringName
        assertEquals OAR.AUTOMATIC, oar.activationType
    }

    void testOARCreateFromURLAndLoadOpStrings() {
        String sep = File.separator
        File target =  new File("${System.getProperty('user.dir')}${sep}target${sep}test-classes", "rules.groovy")
        assertTrue target.exists()

        Throwable t = null
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0")
        manifest.getMainAttributes().putValue(OAR.OAR_NAME, "test")
        manifest.getMainAttributes().putValue(OAR.OAR_VERSION, "1.0")
        manifest.getMainAttributes().putValue(OAR.OAR_OPSTRING, target.name)
        manifest.getMainAttributes().putValue(OAR.OAR_ACTIVATION, OAR.AUTOMATIC)
        File oarFile = createOAR(manifest)
        OAR oar = null
        try {
            oar = new OAR(oarFile.toURI().toURL())
        } catch(OARException e) {
            e.printStackTrace()
            t = e
        }
        assertTrue t==null
        assertNotNull oar
        assertEquals "test", oar.name
        assertEquals "1.0", oar.version
        assertEquals target.name, oar.opStringName
        assertEquals OAR.AUTOMATIC, oar.activationType
        OperationalString[] opstrings = oar.loadOperationalStrings();
        assertTrue "Should have 1 opstring ", opstrings.length==1
        opstrings[0].name.equals "Gnostic"
    }

    void testOARCreateFromJarURLAndLoadOpStrings() {
        String sep = File.separator
        File target =  new File("${System.getProperty('user.dir')}${sep}target${sep}test-classes", "rules.groovy")
        assertTrue target.exists()

        Throwable t = null
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0")
        manifest.getMainAttributes().putValue(OAR.OAR_NAME, "test")
        manifest.getMainAttributes().putValue(OAR.OAR_VERSION, "1.0")
        manifest.getMainAttributes().putValue(OAR.OAR_OPSTRING, target.name)
        manifest.getMainAttributes().putValue(OAR.OAR_ACTIVATION, OAR.AUTOMATIC)
        File oarFile = createOAR(manifest)
        OAR oar = null
        try {
            oar = new OAR(new URL("jar:${oarFile.toURI().toURL()}!/"))
        } catch(OARException e) {
            e.printStackTrace()
            t = e
        }
        assertTrue t==null
        assertNotNull oar
        assertEquals "test", oar.name
        assertEquals "1.0", oar.version
        assertEquals target.name, oar.opStringName
        assertEquals OAR.AUTOMATIC, oar.activationType
        OperationalString[] opstrings = oar.loadOperationalStrings();
        assertTrue "Should have 1 opstring ", opstrings.length==1
        opstrings[0].name.equals "Gnostic"
    }

    def createOAR(Manifest manifest) {
        File jar = new File(new File(System.getProperty('user.dir')), "test.oar")
        if(jar.exists())
            jar.delete()
        return JarUtil.createJar(new File('target/test-classes'),
                                     new File(System.getProperty('user.dir')),
                                     "test.oar",
                                     manifest)
    }

}
