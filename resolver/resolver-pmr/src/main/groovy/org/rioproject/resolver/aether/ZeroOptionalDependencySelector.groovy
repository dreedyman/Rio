package org.rioproject.resolver.aether

import org.sonatype.aether.collection.DependencySelector
import org.sonatype.aether.graph.Dependency
import org.sonatype.aether.collection.DependencyCollectionContext

/**
 * A dependency selector that excludes all optional dependencies
 */
class ZeroOptionalDependencySelector implements DependencySelector {

    boolean selectDependency(Dependency dependency) {
        return !dependency.isOptional()
    }

    DependencySelector deriveChildSelector(DependencyCollectionContext context) {
        return this
    }
}
