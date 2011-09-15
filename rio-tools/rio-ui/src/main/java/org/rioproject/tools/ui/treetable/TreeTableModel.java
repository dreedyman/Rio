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

import javax.swing.tree.TreeModel;

/**
 * TreeTableModel is the model used by a JTreeTable. It extends TreeModel
 * to add methods for getting information about the set of columns each
 * node in the TreeTableModel may have. Each column, like a column in
 * a TableModel, has a name and a type associated with it. Each node in
 * the TreeTableModel can return a value for each of the columns and
 * set that value if isCellEditable() returns true.
 *
 * @author Philip Milne
 * @author Scott Violet
 */
public interface TreeTableModel extends TreeModel {
    /**
     * Get the column count
     *
     * @return The number of available columns.
     */
    int getColumnCount();

    /**
     * Get the column name
     *
     * @param column The column
     *
     * @return The name for column number <code>column</code>.
     */
    String getColumnName(int column);

    /**
     * @return the type for column number <code>column</code>.
     *
     * @param column The column
     */
    Class getColumnClass(int column);

    /**
     * The value at the column
     *
     * @return The value to be displayed for node <code>node</code>,
     * at column number <code>column</code>.
     *
     * @param node The node
     * @param column The column
     */
    Object getValueAt(Object node, int column);

    /**
     * Indicates whether the the value for node <code>node</code>,
     * at column number <code>column</code> is editable.
     *
     * @param node The node
     * @param column The column
     *
     * @return true if the cell is editable
     */
    boolean isCellEditable(Object node, int column);

    /**
     * Sets the value for node <code>node</code>,
     * at column number <code>column</code>.
     *
     * @param aValue The value to set
     * @param row The row
     */
    void setValueAt(Object aValue, int row);
}
