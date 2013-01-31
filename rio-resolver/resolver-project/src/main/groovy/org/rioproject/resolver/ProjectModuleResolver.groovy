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
package org.rioproject.resolver

import org.rioproject.resolver.aether.AetherResolver

import org.sonatype.aether.util.graph.selector.ExclusionDependencySelector
import org.sonatype.aether.util.graph.selector.ScopeDependencySelector
import org.sonatype.aether.util.graph.selector.AndDependencySelector
import org.sonatype.aether.collection.DependencySelector
import org.apache.maven.repository.internal.MavenRepositorySystemSession
import org.sonatype.aether.util.artifact.JavaScopes
import org.rioproject.resolver.aether.ZeroOptionalDependencySelector
import org.rioproject.resolver.aether.TestDependencySelector
import org.rioproject.resolver.aether.TestDependencyFilter

import org.rioproject.resolver.aether.AetherService

import org.rioproject.resolver.aether.ProjectWorkspaceReader

/**
 * Resolves artifacts from within (or among) a project (module).
 */
class ProjectModuleResolver extends AetherResolver {

    def ProjectModuleResolver() {
        service = AetherService.getInstance(new ProjectWorkspaceReader())
        /* We need to modify the DependencySelector the repository system session will use to allow the
         * inclusion of the test scope, and to disallow all optional dependencies. The reason for the latter
         * is required since we will be iterating over the collection of declared dependencies, and resolving each one.
         * As each dependency gets resolved, we will want to ignore that dependency's optional
         * dependencies. Otherwise we would be resolving the dependency's optional dependencies at the wrong level
         */
        Set<DependencySelector> selectors = new LinkedHashSet<DependencySelector>()
        Collections.addAll(selectors,
                           new ScopeDependencySelector("provided"),
                           new TestDependencySelector(),
                           new ZeroOptionalDependencySelector(),
                           new ExclusionDependencySelector())
        DependencySelector depFilter = new AndDependencySelector(selectors)
        ((MavenRepositorySystemSession)aetherService.repositorySystemSession).setDependencySelector(depFilter)
        aetherService.dependencyFilterScope = JavaScopes.TEST
        aetherService.addDependencyFilter(new TestDependencyFilter())
    }
}
