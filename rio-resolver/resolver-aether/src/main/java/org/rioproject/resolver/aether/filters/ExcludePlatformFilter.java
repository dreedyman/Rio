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
package org.rioproject.resolver.aether.filters;

import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.graph.DependencyFilter;
import org.sonatype.aether.graph.DependencyNode;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 * Exclude platform classes and "dl" classifiers
 */
public class ExcludePlatformFilter implements DependencyFilter {
    private final Collection<String> excludes = new HashSet<String>();

    public ExcludePlatformFilter() {
        String prunePlatformProperty = System.getProperty("org.rioproject.resolver.prune.platform");
        boolean prunePlatform = prunePlatformProperty == null || prunePlatformProperty.equals("true");
        if (prunePlatform) {
            excludes.add("org.rioproject:rio");
            excludes.add("org.rioproject:rio-lib");
            excludes.add("org.rioproject:rio-platform");
            excludes.add("org.codehaus.groovy:groovy-all");
            excludes.add("net.jini:jsk-platform");
            excludes.add("org.slf4j:slf4j-api");
        }
    }

    @Override
    public boolean accept(DependencyNode node, List<DependencyNode> parents) {
        Dependency dependency = node.getDependency();

        if (dependency == null) {
            return true;
        }

        String id = getGroupIDAndArtifactID(dependency);
        if (excludes.contains(id)) {
            return false;
        }

        for (String parentID : getParentIDs(parents)) {
            if (excludes.contains(parentID)) {
                return false;
            }
        }
        return true;
    }

    private Collection<String> getParentIDs(List<DependencyNode> parents) {
        Collection<String> ids = new HashSet<String>();
        for (DependencyNode node : parents) {
            ids.add(getGroupIDAndArtifactID(node.getDependency()));
        }
        return ids;
    }

    private String getGroupIDAndArtifactID(Dependency dependency) {
        return dependency.getArtifact().getGroupId() + ':' + dependency.getArtifact().getArtifactId();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || !getClass().equals(obj.getClass())) {
            return false;
        }

        ExcludePlatformFilter that = (ExcludePlatformFilter) obj;
        return this.excludes.equals(that.excludes);
    }

    @Override
    public int hashCode() {
        int hash = 17;
        hash = hash * 31 + excludes.hashCode();
        return hash;
    }
}
