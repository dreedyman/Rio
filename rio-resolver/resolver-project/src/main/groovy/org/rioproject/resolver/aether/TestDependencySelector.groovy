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
package org.rioproject.resolver.aether

import org.sonatype.aether.collection.DependencySelector
import org.sonatype.aether.graph.Dependency
import org.sonatype.aether.collection.DependencyCollectionContext

/**
 * A dependency selector that excludes test dependencies which occur beyond level one of the dependency graph.
 */
class TestDependencySelector implements DependencySelector {
    private final int depth

    TestDependencySelector() {
        depth = 0
    }

    TestDependencySelector(int depth) {
        this.depth = depth
    }

    boolean selectDependency(Dependency dependency) {
        boolean select = true
        if(dependency.scope == "test" && depth > 1)
            select = false
        return select
    }

    DependencySelector deriveChildSelector(DependencyCollectionContext context) {
        if (depth >= 2) {
            return this
        }

        return new TestDependencySelector(depth + 1)
    }

    boolean equals(o) {
        if (this.is(o))
            return true

        if (getClass() != o.class)
            return false

        TestDependencySelector that = (TestDependencySelector) o

        if (depth != that.depth)
            return false

        return true
    }

    int hashCode() {
        int hash = getClass().hashCode()
        hash = hash * 31 + depth
        return hash
    }
}
