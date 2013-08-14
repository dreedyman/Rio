package org.rioproject.resolver.aether

import org.eclipse.aether.collection.DependencySelector
import org.eclipse.aether.graph.Dependency
import org.eclipse.aether.collection.DependencyCollectionContext

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
