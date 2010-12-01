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
package org.rioproject.resolver.maven2.filters

import org.rioproject.resolver.Dependency
import java.util.logging.Logger
import java.util.logging.Level;

/**
 * Exclude platform classes, test, provided, system and optional,
 * and "dl" classifiers
 */
class ExclusionFilter implements DependencyFilter {
    Logger logger = Logger.getLogger(ExclusionFilter.class.getName())
    private boolean allowTest = false

    def ExclusionFilter() {
    }

    def ExclusionFilter(allowTest) {
        this.allowTest = allowTest;
        if(logger.isLoggable(Level.FINEST))
            logger.finest "Including test scoped artifacts: $allowTest"
    }

    public boolean include(Dependency dep) {
        if(dep.excluded || dep.optional) {
            if(logger.isLoggable(Level.FINEST))
                logger.finest "Exclude ${dep.getGAV()}, "+
                              "it is declared as \"excluded\" or \"optional\""
            return false
        }
        if(!allowTest && dep.scope.equals("test")) {
            if(logger.isLoggable(Level.FINEST))
                logger.finest "Exclude ${dep.getGAV()}, declared with of ${dep.scope}"
            return false
        }
        if(dep.scope.equals("test") || dep.scope.equals("provided") || dep.scope.equals("system")) {
            if(logger.isLoggable(Level.FINEST))
                logger.finest "Exclude ${dep.getGAV()}, declared with of ${dep.scope}"
            return false
        }
        if(dep.groupId.equals("org.rioproject") && 
           (dep.artifactId.equals("rio") || dep.artifactId.equals("rio-test"))) {
            if(logger.isLoggable(Level.FINEST))
                logger.finest "Exclude ${dep.getGAV()}"
            return false
        }
        if(dep.groupId.equals("net.jini") ||
           dep.groupId.equals("org.codehaus.groovy") ||
           dep.groupId.equals("cglib")) {
            if(logger.isLoggable(Level.FINEST))
                logger.finest "Exclude ${dep.getGAV()}, it is in the platform"
            return false;
        }
        if(dep.classifier && dep.classifier=="dl") {
            if(logger.isLoggable(Level.FINEST))
                logger.finest "Exclude ${dep.getGAV()} with classifier of \"dl\""
            return false;
        }
        return true
    }
}
