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
package org.rioproject.resolver.aether.util;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.sonatype.aether.graph.DependencyFilter;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.graph.DependencyVisitor;

/**
 * A dependency visitor that dumps the graph to the console.
 */
public class ConsoleDependencyGraphDumper implements DependencyVisitor {
    private PrintStream out;
    private String currentIndent = "";
    private DependencyFilter filter;

    public ConsoleDependencyGraphDumper() {
        this(null);
    }

    public ConsoleDependencyGraphDumper(PrintStream out) {
        this.out = (out != null) ? out : System.out;
    }

    public void setFilter(DependencyFilter filter) {
        this.filter = filter;
    }

    public boolean visitEnter(DependencyNode node) {
        if(filter!=null) {
            if(!filter.accept(node, new ArrayList<DependencyNode>()))
                return false;
        }
        out.println(currentIndent + node);
        if (currentIndent.length() <= 0) {
            currentIndent = "+- ";
        } else {
            currentIndent = "|  " + currentIndent;
        }
        return true;
    }

    public boolean visitLeave(DependencyNode node) {
        if(filter!=null) {
            if(!filter.accept(node, new ArrayList<DependencyNode>()))
                return false;
        }
        currentIndent = currentIndent.substring(3, currentIndent.length());
        return true;
    }

}
