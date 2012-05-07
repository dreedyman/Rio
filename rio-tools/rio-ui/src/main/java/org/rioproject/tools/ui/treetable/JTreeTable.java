/*
 * Copyright 2008 the original author or authors.
 * Copyright 1997, 1998 Sun Microsystems, Inc.
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

import org.rioproject.tools.ui.ColorManager;
import org.rioproject.ui.Util;
import org.rioproject.tools.ui.GraphViewAdapter;
import org.rioproject.jsb.ServiceElementUtil;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.util.EventObject;

/**
 * A table in a tree, based on Milne/Violet work
 *
 * @author Sun Microsystems
 */
public class JTreeTable extends JTable {
    private ColorManager colorManager;
    protected TreeTableCellRenderer tree;
    final GraphViewAdapter graphViewAdapter;

    public JTreeTable(TreeTableModel treeTableModel,
                      ColorManager colorManager,
                      GraphViewAdapter graphViewAdapter) {
        super();
        setModel(treeTableModel);
        this.colorManager = colorManager;
        this.graphViewAdapter = graphViewAdapter;

        // Forces the JTable and JTree to share their row selection models.
        ListToTreeSelectionModelWrapper selectionWrapper = new
            ListToTreeSelectionModelWrapper();
        tree.setSelectionModel(selectionWrapper);
        setSelectionModel(selectionWrapper.getListSelectionModel());

        // Installs the tree editor renderer and editor.
        setDefaultRenderer(TreeTableModel.class, tree);
        setDefaultEditor(TreeTableModel.class, new TreeTableCellEditor());

        // No grid.
        setShowGrid(false);

        // No intercell spacing
        setIntercellSpacing(new Dimension(0, 0));

        // And update the height of the trees row to match that of
        // the table.
        if (tree.getRowHeight() < 1) {
            // Metal looks better like this.
            setRowHeight(20);
        }
    }

    /**
     * Returns2 the tree that is being shared between the model.
     *
     * @return The JTree
     */
    public JTree getTree() {
        return tree;
    }

    /**
     * Overridden to invoke repaint for the particular location if the column
     * contains the tree. This is done as the tree editor does not fill the
     * bounds of the cell, we need the renderer to paint the tree in the
     * background, and then draw the editor over it.
     */
    public boolean editCellAt(int row, int column, EventObject e) {
        boolean retValue = super.editCellAt(row, column, e);
        if (retValue && getColumnClass(column) == TreeTableModel.class) {
            repaint(getCellRect(row, column, false));
        }
        return retValue;
    }

    /**
     * Workaround for BasicTableUI anomaly. Make sure the UI never tries to
     * resize the editor. The UI currently uses different techniques to paint
     * the renderers and editors; overriding setBounds() below is not the right
     * thing to do for an editor. Returning -1 for the editing row in this case,
     * ensures the editor is never painted.
     */
    public int getEditingRow() {
        return (getColumnClass(editingColumn) == TreeTableModel.class) ? -1 :
               editingRow;
    }

    /*
     * Returns the actual row that is editing as <code>getEditingRow</code> will
     * always return -1.
     */
    private int realEditingRow() {
        return editingRow;
    }

    private void setModel(TreeTableModel treeTableModel) {
        if (treeTableModel == null) {
            return;
        }
        // Creates the tree. It will be used as a renderer and editor.
        tree = new TreeTableCellRenderer(treeTableModel);

        // Installs a tableModel representing the visible rows in the tree.
        super.setModel(new TreeTableModelAdapter(treeTableModel, tree));
    }

    /**
     * Overridden to pass the new rowHeight to the tree.
     */
    public void setRowHeight(int rowHeight) {
        super.setRowHeight(rowHeight);
        if (tree != null && tree.getRowHeight() != rowHeight) {
            tree.setRowHeight(getRowHeight());
        }
    }

    /**
     * This is overridden to invoke super's implementation, and then, if the
     * receiver is editing a Tree column, the editor's bounds is reset. The
     * reason we have to do this is because JTable doesn't think the table is
     * being edited, as <code>getEditingRow</code> returns -1, and therefore
     * doesn't automatically resize the editor for us.
     */
    public void sizeColumnsToFit(int resizingColumn) {
        super.sizeColumnsToFit(resizingColumn);
        if (getEditingColumn() != -1 && getColumnClass(editingColumn) ==
                                        TreeTableModel.class) {
            Rectangle cellRect = getCellRect(realEditingRow(),
                                             getEditingColumn(), false);
            Component component = getEditorComponent();
            component.setBounds(cellRect);
            component.validate();
        }
    }

    /**
     * Overridden to message super and forward the method to the tree. Since the
     * tree is not actually in the component hierarchy it will never receive
     * this unless we forward it in this manner.
     */
    public void updateUI() {
        super.updateUI();
        if (tree != null) {
            tree.updateUI();
            // Do this so that the editor is referencing the current renderer
            // from the tree. The renderer can potentially change each time
            // laf changes.
            setDefaultEditor(TreeTableModel.class, new TreeTableCellEditor());
        }
        // Use the tree's default foreground and background colors in the
        // table.
        LookAndFeel.installColorsAndFont(this, "Tree.background",
                                         "Tree.foreground", "Tree.font");
    }

    @Override
    public Component prepareRenderer(TableCellRenderer renderer,
                                     int row,
                                     int column) {
        Component component = super.prepareRenderer(renderer, row, column);
        int[] selectedRows = getTree().getSelectionRows();
        if (selectedRows != null) {
            for (int selectedRow : selectedRows) {
                if (row == selectedRow) {
                    component.setForeground(UIManager.getColor(
                        "Table.focusCellForeground"));
                    return component;
                }
            }
        }

        JTree tree = getTree();
        if(tree!=null) {

            TreePath path = tree.getPathForRow(row);
            if(path!=null) {
                DefaultMutableTreeNode node =
                    (DefaultMutableTreeNode)path.getLastPathComponent();
                DefaultMutableTreeNode root =
                    (DefaultMutableTreeNode)tree.getModel().getRoot();
                component.setBackground(Util.getRowColor(root,
                                                         node,
                                                         getTree(),
                                                         getBackground(),
                                                         colorManager.getAltRowColor()));

                component.setForeground(Color.black);
            }
        }
        return component;
    }


    /**
     * A TreeCellRenderer that displays a JTree.
     */
    public class TreeTableCellRenderer extends JTree implements
                                                     TableCellRenderer {
        /**
         * Last table/tree row asked to renderer.
         */
        protected int visibleRow;
        /**
         * Border to draw around the tree, if this is non-null, it will be
         * painted.
         */
        protected Border highlightBorder;

        public TreeTableCellRenderer(TreeModel model) {
            super(model);
            this.setCellRenderer(new JTreeTableCellRenderer());
        }

        /**
         * updateUI is overridden to set the colors of the Tree's renderer to
         * match that of the table.
         */
        public void updateUI() {
            super.updateUI();
            // Make the tree's cell renderer use the table's cell selection
            // colors.
            TreeCellRenderer tcr = getCellRenderer();
            if (tcr instanceof DefaultTreeCellRenderer) {
                DefaultTreeCellRenderer dtcr = ((DefaultTreeCellRenderer) tcr);
                // For 1.1 uncomment this, 1.2 has a bug that will cause an
                // exception to be thrown if the border selection color is
                // null.
                // dtcr.setBorderSelectionColor(null);
                dtcr.setTextSelectionColor(UIManager.getColor
                    ("Table.selectionForeground"));
                dtcr.setBackgroundSelectionColor(UIManager.getColor
                    ("Table.selectionBackground"));
            }
        }

        /**
         * Sets the row height of the tree, and forwards the row height to the
         * table.
         */
        public void setRowHeight(int rowHeight) {
            if (rowHeight > 0) {
                super.setRowHeight(rowHeight);
                if (JTreeTable.this.getRowHeight() != rowHeight) {
                    JTreeTable.this.setRowHeight(getRowHeight());
                }
            }
        }

        /**
         * This is overridden to set the height to match that of the JTable.
         */
        public void setBounds(int x, int y, int w, int h) {
            super.setBounds(x, 0, w, JTreeTable.this.getHeight());
        }

        /**
         * Sublcassed to translate the graphics such that the last visible row
         * will be drawn at 0,0.
         */
        public void paint(Graphics g) {
            g.translate(0, -visibleRow * getRowHeight());
            super.paint(g);
            // Draw the Table border if we have focus.
            if (highlightBorder != null) {
                highlightBorder.paintBorder(this,
                                            g,
                                            0,
                                            visibleRow *
                                            getRowHeight(),
                                            getWidth(),
                                            getRowHeight());
            }
        }

        /**
         * TreeCellRenderer method. Overridden to update the visible row.
         */
        public Component getTableCellRendererComponent(JTable table,
                                                       Object value,
                                                       boolean isSelected,
                                                       boolean hasFocus,
                                                       int row,
                                                       int column) {
            Color background;
            Color foreground;
            
            if (isSelected) {
                background = table.getSelectionBackground();
                foreground = table.getSelectionForeground();
            } else {
                background = table.getBackground();
                foreground = table.getForeground();

            }
            highlightBorder = null;
            if (realEditingRow() == row && getEditingColumn() == column) {
                background = UIManager.getColor("Table.focusCellBackground");
                foreground = UIManager.getColor("Table.focusCellForeground");
            } else if (hasFocus) {
                highlightBorder = UIManager.getBorder("Table.focusCellHighlightBorder");
                //if (isCellEditable(row, column)) {
                //    background = UIManager.getColor("Table.focusCellBackground");
                //    foreground = UIManager.getColor("Table.focusCellForeground");
                //}
            }

            visibleRow = row;
            setBackground(background);                        

            TreeCellRenderer tcr = getCellRenderer();
            if (tcr instanceof DefaultTreeCellRenderer) {
                JTree t = getTree();
                DefaultTreeCellRenderer dtcr = ((DefaultTreeCellRenderer) tcr);
                if (isSelected) {
                    dtcr.setTextSelectionColor(foreground);
                    dtcr.setBackgroundSelectionColor(background);
                } else {
                    dtcr.setTextNonSelectionColor(foreground);
                    TreePath tPath = t.getPathForRow(row);
                    if(tPath!=null) {
                        DefaultMutableTreeNode node =
                            (DefaultMutableTreeNode)tPath.getLastPathComponent();
                        Color color =
                            Util.getRowColor((DefaultMutableTreeNode) t.getModel().getRoot(),
                                             node,
                                             t,
                                             getBackground(),
                                             colorManager.getAltRowColor());
                        dtcr.setBackgroundNonSelectionColor(color);
                    }
                }
            }
            return this;
        }

        /*
         * Returns the appropriate background color for the given row.
         */
        protected Color colorForRow(int row) {
            return (row % 2 == 0) ? Color.LIGHT_GRAY : getBackground();
        }
    }

    /**
     * An editor that can be used to edit the tree column. This extends
     * DefaultCellEditor and uses a JTextField (actually, TreeTableTextField) to
     * perform the actual editing. 
     */
    public class TreeTableCellEditor extends DefaultCellEditor {
        public TreeTableCellEditor() {
            super(new TreeTableTextField());
        }

        /**
         * Overridden to determine an offset that tree would place the editor
         * at. The offset is determined from the <code>getRowBounds</code> JTree
         * method, and additionally from the icon DefaultTreeCellRenderer will
         * use. <p>The offset is then set on the TreeTableTextField component
         * created in the constructor, and returned.
         */
        public Component getTableCellEditorComponent(JTable table,
                                                     Object value,
                                                     boolean isSelected,
                                                     int r, int c) {
            Component component = super.getTableCellEditorComponent
                (table, value, isSelected, r, c);
            JTree t = getTree();
            boolean rv = t.isRootVisible();
            int offsetRow = rv ? r : r - 1;
            Rectangle bounds = t.getRowBounds(offsetRow);
            int offset = (bounds==null?0:bounds.x);
            TreeCellRenderer tcr = t.getCellRenderer();
            if (tcr instanceof DefaultTreeCellRenderer) {
                if(t.getPathForRow(offsetRow)==null)
                    return component;
                Object node = t.getPathForRow(offsetRow).getLastPathComponent();
                Icon icon;
                if (t.getModel().isLeaf(node))
                    icon = ((DefaultTreeCellRenderer) tcr).getLeafIcon();
                else if (tree.isExpanded(offsetRow))
                    icon = ((DefaultTreeCellRenderer) tcr).getOpenIcon();
                else
                    icon = ((DefaultTreeCellRenderer) tcr).getClosedIcon();
                if (icon != null) {
                    offset += ((DefaultTreeCellRenderer) tcr).getIconTextGap() +
                              icon.getIconWidth();
                }
            }
            ((TreeTableTextField) getComponent()).offset = offset;
            return component;
        }

        /**
         * This is overridden to forward the event to the tree.
         */
        public boolean isCellEditable(EventObject e) {
            if (e instanceof MouseEvent) {
                MouseEvent me = (MouseEvent) e;
                // If the modifiers are not 0 (or the left mouse button),
                // tree may try and toggle the selection, and table
                // will then try and toggle, resulting in the
                // selection remaining the same. To avoid this, we
                // only dispatch when the modifiers are 0 (or the left mouse
                // button).
                if (me.getModifiers() == 0 ||
                    me.getModifiers() == InputEvent.BUTTON1_MASK) {
                    for (int counter = getColumnCount() - 1; counter >= 0;
                         counter--) {
                        if (getColumnClass(counter) == TreeTableModel.class) {
                            MouseEvent newME = new MouseEvent
                                (JTreeTable.this.tree,
                                 me.getID(),
                                 me.getWhen(),
                                 me.getModifiers(),
                                 me.getX() -
                                 getCellRect(0, counter, true).x,
                                 me.getY(),
                                 me.getClickCount(),
                                 me.isPopupTrigger());
                            JTreeTable.this.tree.dispatchEvent(newME);
                            break;
                        }
                    }
                }
                //if (me.getClickCount() >= 3) {
                //    return true;
                //}
                return false;
            }
            //if (e == null) {
            //    return true;
            //}
            return false;
        }
    }

    /**
     * Component used by TreeTableCellEditor. The only thing this does is to
     * override the <code>setBounds</code> method, and to ALWAYS make the x
     * location be <code>offset</code>.
     */
    static class TreeTableTextField extends JTextField {
        public int offset;

        public void setBounds(int x, int y, int w, int h) {
            //public void reshape(int x, int y, int w, int h) {
            int newX = Math.max(x, offset);
            //super.reshape(newX, y, w - (newX - x), h);
            super.setBounds(newX, y, w - (newX - x), h);
        }

        
    }

    class JTreeTableCellRenderer extends DefaultTreeCellRenderer {
        private ImageIcon forkedIcon;
        private ImageIcon warningIcon;

        JTreeTableCellRenderer() {
            forkedIcon = Util.getImageIcon(
                "org/rioproject/tools/ui/images/forkedService.gif");
            warningIcon = Util.getImageIcon(
                "org/rioproject/tools/ui/images/warning.png");
        }

        public Component getTreeCellRendererComponent(JTree tree,
                                                      Object value,
                                                      boolean sel,
                                                      boolean expanded,
                                                      boolean leaf,
                                                      int row,
                                                      boolean hasFocus) {

            super.getTreeCellRendererComponent(tree,
                                               value,
                                               sel,
                                               expanded,
                                               leaf,
                                               row,
                                               hasFocus);
            if (leaf && value instanceof ServiceNode) {
                ServiceNode node = (ServiceNode)value;
                if(graphViewAdapter.getServiceItem(node.getServiceElement(),
                                                   node.getUuid())==null) {
                    //setIcon(warningIcon);
                    setToolTipText("Active, no ServiceItem\n"+
                                        ServiceElementUtil.formatDiscoverySettings(
                    node.getServiceElement().getServiceBeanConfig()));

                } else if(((ServiceNode)value).isForked()) {
                    setIcon(forkedIcon);
                    this.setToolTipText("");
                }
            }

            return this;
        }       
    }

    /**
     * ListToTreeSelectionModelWrapper extends DefaultTreeSelectionModel to
     * listen for changes in the ListSelectionModel it maintains. Once a change
     * in the ListSelectionModel happens, the paths are updated in the
     * DefaultTreeSelectionModel.
     */
    class ListToTreeSelectionModelWrapper extends DefaultTreeSelectionModel {
        /**
         * Set to true when we are updating the ListSelectionModel.
         */
        protected boolean updatingListSelectionModel;

        public ListToTreeSelectionModelWrapper() {
            super();
            getListSelectionModel().addListSelectionListener(createListSelectionListener());
        }

        /**
         * Returns the list selection model. ListToTreeSelectionModelWrapper
         * listens for changes to this model and updates the selected paths
         * accordingly.
         *
         * @return The ListSelectionModel
         */
        ListSelectionModel getListSelectionModel() {
            return listSelectionModel;
        }

        /**
         * This is overridden to set <code>updatingListSelectionModel</code> and
         * message super. This is the only place DefaultTreeSelectionModel
         * alters the ListSelectionModel.
         */
        public void resetRowSelection() {
            if (!updatingListSelectionModel) {
                updatingListSelectionModel = true;
                try {
                    super.resetRowSelection();
                }
                finally {
                    updatingListSelectionModel = false;
                }
            }
            // Notice how we don't message super if
            // updatingListSelectionModel is true. If
            // updatingListSelectionModel is true, it implies the
            // ListSelectionModel has already been updated and the
            // paths are the only thing that needs to be updated.
        }

        /**
         * Creates and returns an instance of ListSelectionHandler.
         *
         * @return ListSelectionListener
         */
        private ListSelectionListener createListSelectionListener() {
            return new ListSelectionHandler();
        }

        /**
         * If <code>updatingListSelectionModel</code> is false, this will reset
         * the selected paths from the selected rows in the list selection
         * model.
         */
        protected void updateSelectedPathsFromSelectedRows() {
            if (!updatingListSelectionModel) {
                updatingListSelectionModel = true;
                try {
                    // This is way expensive, ListSelectionModel needs an
                    // enumerator for iterating.
                    int min = listSelectionModel.getMinSelectionIndex();
                    int max = listSelectionModel.getMaxSelectionIndex();

                    clearSelection();
                    if (min != -1 && max != -1) {
                        for (int counter = min; counter <= max; counter++) {
                            if (listSelectionModel.isSelectedIndex(counter)) {
                                TreePath selPath = tree.getPathForRow
                                    (counter);

                                if (selPath != null) {
                                    addSelectionPath(selPath);
                                }
                            }
                        }
                    }
                }
                finally {
                    updatingListSelectionModel = false;
                }
            }
        }

        /**
         * Class responsible for calling updateSelectedPathsFromSelectedRows
         * when the selection of the list changse.
         */
        class ListSelectionHandler implements ListSelectionListener {
            public void valueChanged(ListSelectionEvent e) {
                updateSelectedPathsFromSelectedRows();
            }
        }
    }
}
