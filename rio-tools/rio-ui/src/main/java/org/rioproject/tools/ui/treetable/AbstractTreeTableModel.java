/*
 * Copyright 2008 the original author or authors.
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
package org.rioproject.tools.ui.treetable;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

/**
 * Provides a base upon which to create a tree table
 */
public abstract class AbstractTreeTableModel extends DefaultTreeModel
    implements TreeTableModel {
    protected DefaultMutableTreeNode root;

    public AbstractTreeTableModel(DefaultMutableTreeNode root) {
        super(root);
        this.root = root;
    }

    public DefaultMutableTreeNode getRoot() {
        return root;
    }
}
