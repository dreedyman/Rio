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
package org.rioproject.ui;

import javax.swing.*;
import java.awt.*;
import java.io.Serializable;

/**
 * A utility that allows glass pane type effects to be applied to portions of
 * your UI instead of the entire UI.
 *
 * <p>This class is part of the rio-api module because it will be downloaded with service ui
 * classes. If we were to put it into the rio-lib module, the rio-lib module would have to
 * be declared as part of the codebase (dependencies) of the watch-ui module.
 */
@SuppressWarnings("PMD")
public class GlassPaneContainer extends JPanel {
    private JComponent glassPane;

    public GlassPaneContainer() {
        setLayout(new Layout());
        setGlassPane(createGlassPane());
    }

    public GlassPaneContainer(final Component singleComponent) {
        this();
        add(singleComponent);
    }

    public void setGlassPane(final JComponent glass) {
        if (glass == null) {
            setGlassPane(createGlassPane());
            return;
        }

        final JComponent old = this.glassPane;

        final boolean visible;
        if (this.glassPane != null && this.glassPane.getParent() == this) {
            this.remove(this.glassPane);
            visible = this.glassPane.isVisible();
        } else {
            visible = false;
        }

        glass.setVisible(visible);
        this.glassPane = glass;
        this.add(this.glassPane);

        firePropertyChange("glassPane", old, glass);

        if (visible) {
            repaint();
        }
    }

    public JComponent getGlassPane() {
        return glassPane;
    }


    public static GlassPaneContainer findGlassPaneContainerFor(Component c) {
        while (c != null && !(c instanceof GlassPaneContainer)) {
            c = c.getParent();
        }

        return (GlassPaneContainer) c;
    }

    @Override
    protected void addImpl(Component comp, Object constraints, int index) {
        if (comp == getGlassPane()) {
            super.addImpl(comp, constraints, 0);
        } else {
            if (index == 0) {
                index = 1;
            }
            super.addImpl(comp, constraints, index);
        }
    }


    @Override
    public boolean isOptimizedDrawingEnabled() {
        return !getGlassPane().isVisible() && super.isOptimizedDrawingEnabled();
    }

    protected JPanel createGlassPane() {
        final JPanel ret = new JPanel();
        ret.setName(getName() + ".glassPane");
        ret.setVisible(false);
        ret.setOpaque(false);
        return ret;
    }

    protected class Layout implements LayoutManager2, Serializable {
        public Dimension maximumLayoutSize(final Container target) {
            assert target == GlassPaneContainer.this;
            final int componentCount = getComponentCount();
            final JComponent glassPane = getGlassPane();
            final Dimension max = new Dimension();
            for (int i = 0; i < componentCount; i++) {
                final Component c = getComponent(i);
                if (c != glassPane) {
                    final Dimension cMax = c.getMaximumSize();
                    max.setSize(Math.max(max.width, cMax.width), Math.max(max.height, cMax.height));
                }
            }
            return max;
        }

        public Dimension preferredLayoutSize(final Container target) {
            assert target == GlassPaneContainer.this;
            final int componentCount = getComponentCount();
            final JComponent glassPane = getGlassPane();
            final Dimension pref = new Dimension();
            for (int i = 0; i < componentCount; i++) {
                final Component c = getComponent(i);
                if (c != glassPane) {
                    final Dimension cPref = c.getPreferredSize();
                    pref.setSize(Math.max(pref.width, cPref.width), Math.max(pref.height, cPref.height));
                }
            }
            return pref;
        }

        public Dimension minimumLayoutSize(final Container target) {
            assert target == GlassPaneContainer.this;
            final int componentCount = getComponentCount();
            final JComponent glassPane = getGlassPane();
            final Dimension min = new Dimension();
            for (int i = 0; i < componentCount; i++) {
                final Component c = getComponent(i);
                if (c != glassPane) {
                    final Dimension cMin = c.getMinimumSize();
                    min.setSize(Math.max(min.width, cMin.width), Math.max(min.height, cMin.height));
                }
            }
            return min;
        }

        public void layoutContainer(final Container target) {
            assert target == GlassPaneContainer.this;

            final Rectangle bounds = getBounds();
            final Insets insets = getInsets();
            final int width = bounds.width - insets.right - insets.left;
            final int height = bounds.height - insets.bottom - insets.top;

            final int componentCount = getComponentCount();

            for (int i = 0; i < componentCount; i++) {
                final Component c = getComponent(i);
                c.setBounds(0, 0, width, height);
            }
        }

        public float getLayoutAlignmentX(Container target) {
            return 0f;
        }

        public float getLayoutAlignmentY(Container target) {
            return 0f;
        }

        public void addLayoutComponent(Component comp, Object constraints) {
        }

        public void invalidateLayout(Container target) {
        }

        public void addLayoutComponent(String name, Component comp) {
        }

        public void removeLayoutComponent(Component comp) {
        }
    }
}
