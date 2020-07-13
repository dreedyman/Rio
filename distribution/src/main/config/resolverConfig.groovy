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
import org.rioproject.RioVersion

/*
 * This file is used to configure the org.rioproject.resolver.Resolver, defining
 * the resolver jar file name as well as the remote Maven and flatDir repositories
 * that the resolver will use. The order in which these repositories are listed is the
 * order in which they will be searched.
 *
 * If you have configured repositories into your ~/.m2/settings.xml, those repositories
 * will be appended to this repositories listed below.
 *
 * Add or remove entries as needed.
 */
resolver {
    jar = "${rioHome()}/lib/resolver/resolver-aether-${RioVersion.VERSION}.jar"

    repositories {
        remote = ["bintray": "https://dl.bintray.com/dreedyman/Rio",
                  "rio"    : "http://www.rio-project.org/maven2",
                  "central": "https://repo1.maven.org/maven2"]

        flatDirs = [new File(rioHome(), "lib-dl"),
                    new File(rioHome(), "config/poms"),
                    new File(rioHome(), "lib")]
    }
}

static def rioHome() {
    return System.getProperty("rio.home")
}