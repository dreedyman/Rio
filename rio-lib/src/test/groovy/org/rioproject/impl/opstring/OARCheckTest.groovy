/*
 * Copyright to the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.rioproject.impl.opstring

import org.junit.After
import org.junit.Before
import org.junit.Test
import org.rioproject.opstring.OperationalString
import org.rioproject.test.utils.JarUtil

import java.util.jar.Attributes
import java.util.jar.Manifest

import static org.junit.Assert.*

/**
 * Check OAR construction.
 */
class OARCheckTest /*extends GroovyTestCase */{
    File oarFile

    @Before
    void init() {
        if(oarFile!=null) {
            if (oarFile.delete())
                println "Removed ${oarFile.path}"
        }
        oarFile = null
    }
    @After
    void cleanup() {
        if(oarFile!=null) {
            if(oarFile.delete())
                println "Removed ${oarFile.path}"
        }
    }

    @Test
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

    @Test
    void testBadOARCreateFromManifest2() {
        Throwable t = null
        Manifest manifest = new Manifest()
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0")
        oarFile = createOAR(manifest)
        try {
            new OAR(oarFile)
        } catch(OARException e) {
            t = e
        }
        assertTrue t!=null
    }

    @Test
    void testOARCreateFromManifest() {
        Throwable t = null
        Manifest manifest = new Manifest()
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

    @Test
    void testOARCreateFromURL() {
        Throwable t = null
        Manifest manifest = new Manifest()
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0")
        manifest.getMainAttributes().putValue(OAR.OAR_NAME, "test")
        manifest.getMainAttributes().putValue(OAR.OAR_VERSION, "1.0")
        manifest.getMainAttributes().putValue(OAR.OAR_OPSTRING, "test.groovy")
        manifest.getMainAttributes().putValue(OAR.OAR_ACTIVATION, OAR.MANUAL)
        oarFile = createOAR(manifest, "from-url.oar")
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
        assertEquals OAR.MANUAL, oar.activationType
        assertTrue oar.repositories.size()==2
    }

    @Test
    void testOARCreateFromJarURL() {
        Throwable t = null
        Manifest manifest = new Manifest()
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0")
        manifest.getMainAttributes().putValue(OAR.OAR_NAME, "test")
        manifest.getMainAttributes().putValue(OAR.OAR_VERSION, "1.0")
        manifest.getMainAttributes().putValue(OAR.OAR_OPSTRING, "test.groovy")
        manifest.getMainAttributes().putValue(OAR.OAR_ACTIVATION, OAR.AUTOMATIC)
        oarFile = createOAR(manifest, "from-jar-url.oar")
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
        assertTrue oar.repositories.size()==2
    }

    @Test
    void testOARCreateFromURLAndLoadOpStrings() {
        File target =  getOpString("rules.groovy")
        assertTrue target.exists()

        String oarOpString = 'opstrings'+File.separator+target.name
        Throwable t = null
        Manifest manifest = new Manifest()
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0")
        manifest.getMainAttributes().putValue(OAR.OAR_NAME, "test")
        manifest.getMainAttributes().putValue(OAR.OAR_VERSION, "1.0")
        manifest.getMainAttributes().putValue(OAR.OAR_OPSTRING, oarOpString)
        manifest.getMainAttributes().putValue(OAR.OAR_ACTIVATION, OAR.AUTOMATIC)
        oarFile = createOAR(manifest)
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
        assertEquals oarOpString, oar.opStringName
        assertEquals OAR.AUTOMATIC, oar.activationType
        OperationalString[] opstrings = oar.loadOperationalStrings()
        assertTrue "Should have 1 opstring ", opstrings.length==1
        opstrings[0].name.equals "Gnostic"
        assertTrue oar.repositories.size()==2
    }

    @Test
    void testOARCreateFromJarURLAndLoadOpStrings() {
        File target =  getOpString("rules.groovy")
        assertTrue target.exists()

        String oarOpString = 'opstrings'+File.separator+target.name
        Manifest manifest = new Manifest()
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0")
        manifest.getMainAttributes().putValue(OAR.OAR_NAME, "test")
        manifest.getMainAttributes().putValue(OAR.OAR_VERSION, "1.0")
        manifest.getMainAttributes().putValue(OAR.OAR_OPSTRING, oarOpString)
        manifest.getMainAttributes().putValue(OAR.OAR_ACTIVATION, OAR.AUTOMATIC)
        oarFile = createOAR(manifest)
        OAR oar =  new OAR(new URL("jar:${oarFile.toURI().toURL()}!/"))
        assertNotNull oar
        assertEquals "test", oar.name
        assertEquals "1.0", oar.version
        assertEquals oarOpString, oar.opStringName
        assertEquals OAR.AUTOMATIC, oar.activationType
        OperationalString[] opstrings = oar.loadOperationalStrings()
        assertTrue "Should have 1 opstring ", opstrings.length==1
        opstrings[0].name.equals "Gnostic"
        assertTrue oar.repositories.size() == 2
    }

    def createOAR(Manifest manifest) {
        return createOAR(manifest, String.format("test-%s.oar", Math.random()))
    }

    def createOAR(Manifest manifest, String name) {
        String sep = File.separator
        String userDir = System.getProperty('user.dir')
        File target = new File("${userDir}${sep}build${sep}oar-test")
        File jar = new File(target, name)
        if(jar.exists()) {
            if(jar.delete())
                System.out.println("Deleted "+jar.getPath())
        }
        File repositories = new File("${userDir}/src/test/resources/repositories.xml")
        return JarUtil.createJar(new File("${userDir}/build/resources/test"),
                target,
                name,
                manifest,
                null)
    }

    private File getOpString(String name) {
        String sep = File.separator
        return new File("${System.getProperty('user.dir')}${sep}build${sep}resources${sep}test${sep}opstrings", name)
    }

}
