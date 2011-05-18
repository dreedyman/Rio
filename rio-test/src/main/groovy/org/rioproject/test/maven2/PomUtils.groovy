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
package org.rioproject.test.maven2

/**
 * Utility for working with pom modules
 */
class PomUtils {

    def static URL getParentPomFromProject(File pomFile,
                                           String parentGroupId,
                                           String parentArtifactId) {
        URL parentPomURL = null
        XmlSlurper parser = getXmlSlurper()
        def pom = parser.parseText(pomFile.text)

        if(pom.parent.size()>0) {
            if(pomFile.parentFile!=null) {
                File parentDir = pomFile.parentFile.parentFile
                File parentPom = new File(parentDir, "pom.xml")
                if(!parentPom.exists())
                    return null
                String gid = pom.parent.groupId
                String aid = pom.parent.artifactId
                parentPomURL = getParentPomFromProject(parentPom, gid, aid)
            }

        } else {
            String gid = pom.groupId
            String aid = pom.artifactId
            if(parentGroupId.equals(gid) && parentArtifactId.equals(aid)) {
                parentPomURL = pomFile.toURI().toURL()
            }
        }
        return parentPomURL
    }

    def static Map<String, File> getProjectModuleMap(File pomFile, String version) {
        Map<String, File> map = new HashMap<String, File>()
        File dir = pomFile.parentFile
        XmlSlurper parser = new XmlSlurper()
        def pom = parser.parseText(pomFile.text)
        if (version == null)
            version = pom.version
        map.put(getPomGAV(pomFile, version), new File(dir, "target"))
        pom.modules.module.each {module ->
            String mName = module.toString()
            File moduleDir = new File(dir, mName)
            File modulePom = new File(moduleDir, "pom.xml")
            if (modulePom.exists()) {
                map.put(getPomGAV(modulePom, version), new File(moduleDir, "target"))
                map.putAll(getProjectModuleMap(modulePom, version))
            }
        }
        return map
    }

    def static String getPomGAV(File pomFile, String parentVersion) {
        XmlSlurper parser = new XmlSlurper()
        def pom = parser.parseText(pomFile.text)
        String g = pom.groupId
        String a = pom.artifactId
        String v = pom.version
        if (v.length() == 0 || v.equals('${pom.version}') || v.equals('${project.version}')) {
            if (parentVersion.length() == 0)
                v = '${pom.version}'
            else
                v = parentVersion
        }
        return "$g:$a:$v"
    }


    def static getXmlSlurper() {
        XmlSlurper parser = new XmlSlurper()
        parser.setFeature("http://xml.org/sax/features/namespaces", false)
        return parser
    }

    private String checkVersion(String gav, String parentVersion) {
        if (gav.indexOf('${pom.version}') != -1)
            gav = gav.replace('${pom.version}', parentVersion)
        return gav
    }
}
