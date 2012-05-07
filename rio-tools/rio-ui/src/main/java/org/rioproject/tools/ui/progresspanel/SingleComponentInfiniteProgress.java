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
package org.rioproject.tools.ui.progresspanel;

import javax.swing.*;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.*;
import java.awt.image.BufferedImage;

/**
 * A InfiniteProgressPanel-like component, but more efficient. This is the
 * preferred class to use unless you need the total control over the appearance
 * that InfiniteProgressPanel gives you.
 *
 * <p>An infinite progress panel
 * displays a rotating figure and a message to notice the user of a long,
 * duration unknown task. The shape and the text are drawn upon a white veil
 * which alpha level (or shield value) lets the underlying component shine
 * through. This panel is meant to be used as a <i>glass pane</i> in the window
 * performing the long operation.
 *
 * <p>Calling setVisible(true) makes
 * the component visible and starts the animation. Calling setVisible(false)
 * halts the animation and makes the component invisible. Once you've started
 * the animation all the mouse events are intercepted by this panel, preventing
 * them from being forwared to the underlying components.
 *
 * <p>The panel
 * can be controlled by the <code>setVisible()</code>, method.
 *
 * <p>This version of the infinite progress panel does not display any fade
 * in/out when
 * the animation is started/stopped.
 *
 */
public class SingleComponentInfiniteProgress extends JComponent
    implements ActionListener {
    private static final double UNSCALED_BAR_SIZE = 45d;

    public static final int DEFAULT_NUMBER_OF_BARS = 12;
    public static final int DEFAULT_FPS = 10;
    public static final double NO_AUTOMATIC_RESIZING = -1d;
    public static final double NO_MAX_BAR_SIZE = -1d;

    private int numBars;
    private double dScale = 1.2d;
    private double resizeRatio = NO_AUTOMATIC_RESIZING;
    private double maxBarSize = 64d;
    private double minBarSize = 4;
    private boolean useBackBuffer;
    private String text;

    private MouseAdapter mouseAdapter = new MouseAdapter() {
    };
    private MouseMotionAdapter mouseMotionAdapter = new MouseMotionAdapter() {
    };
    private KeyAdapter keyAdapter = new KeyAdapter() {
    };
    private ComponentAdapter componentAdapter = new ComponentAdapter() {
        public void componentResized(final ComponentEvent e) {
            if (useBackBuffer) {
                setOpaque(false);
                imageBuf = null;
                iterate = 3;
            }
        }
    };

    private BufferedImage imageBuf = null;
    private Area[] bars;
    private Rectangle barsBounds = null;
    private Rectangle barsScreenBounds = null;
    private AffineTransform centerAndScaleTransform = null;
    private Timer timer;
    private Color[] colors = null;
    private int colorOffset = 0;
    private boolean tempHide = false;

    public SingleComponentInfiniteProgress() {
        this(true);
    }

    public SingleComponentInfiniteProgress(boolean i_bUseBackBuffer) {
        this(i_bUseBackBuffer, DEFAULT_NUMBER_OF_BARS);
    }

    public SingleComponentInfiniteProgress(int numBars) {
        this(true, numBars, DEFAULT_FPS, NO_AUTOMATIC_RESIZING, NO_MAX_BAR_SIZE);
    }

    public SingleComponentInfiniteProgress(boolean i_bUseBackBuffer,
                                           int numBars) {
        this(i_bUseBackBuffer,
             numBars,
             DEFAULT_FPS,
             NO_AUTOMATIC_RESIZING,
             NO_MAX_BAR_SIZE);
    }

    public SingleComponentInfiniteProgress(boolean i_bUseBackBuffer,
                                           int numBars,
                                           int fps,
                                           double resizeRatio,
                                           double maxBarSize) {
        this.useBackBuffer = i_bUseBackBuffer;
        this.numBars = numBars;
        this.resizeRatio = resizeRatio;
        this.maxBarSize = maxBarSize;

        this.timer = new Timer(1000 / fps, this);

        colors = new Color[numBars * 2];
        // build bars
        bars = buildTicker(numBars);
        // calculate bars bounding rectangle
        barsBounds = new Rectangle();
        for (Area bar : bars) {
            barsBounds = barsBounds.union(bar.getBounds());
        }
        // create colors
        for (int i = 0; i < bars.length; i++) {
            int channel = 224 - 128 / (i + 1);
            colors[i] = new Color(channel, channel, channel);
            colors[numBars + i] = colors[i];
        }
        // set cursor
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        // set opaque
        setOpaque(useBackBuffer);
    }

    /**
     * Show/Hide the pane, starting and stopping the animation as you go
     */
    public void setVisible(boolean i_bIsVisible) {
        final boolean old = isVisible();
        setOpaque(false);
        // capture
        if (i_bIsVisible) {
            if (useBackBuffer) {
                // add window resize listener
                Window w = SwingUtilities.getWindowAncestor(this);
                if (w != null) {
                    w.addComponentListener(componentAdapter);
                } else {
                    addAncestorListener(new AncestorListener() {
                        public void ancestorAdded(AncestorEvent event) {
                            Window w = SwingUtilities.getWindowAncestor(
                                SingleComponentInfiniteProgress.this);
                            if (w != null) {
                                w.addComponentListener(componentAdapter);
                            }
                        }

                        public void ancestorRemoved(AncestorEvent event) {
                        }

                        public void ancestorMoved(AncestorEvent event) {
                        }
                    });
                }
                iterate = 3;
            }
            // capture events
            addMouseListener(mouseAdapter);
            addMouseMotionListener(mouseMotionAdapter);
            addKeyListener(keyAdapter);
            timer.start();
        } else {
            // stop anim
            timer.stop();
            /// free back buffer
            imageBuf = null;
            // stop capturing events
            removeMouseListener(mouseAdapter);
            removeMouseMotionListener(mouseMotionAdapter);
            removeKeyListener(keyAdapter);
            // remove window resize listener
            Window oWindow = SwingUtilities.getWindowAncestor(this);
            if (oWindow != null) {
                oWindow.removeComponentListener(componentAdapter);
            }
        }
        super.setVisible(i_bIsVisible);
        firePropertyChange("running", old, i_bIsVisible);
    }

    /**
     * Recalc bars based on changes in size
     */
    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        recalcSize();
    }

    @Override
    public void setSize(int i, int i1) {
        super.setSize(i, i1);
        recalcSize();
    }

    private void recalcSize() {
        calcBarsForBounds(getWidth(), getHeight(), true);
        // Now, see if the text fits...
        final double maxY = getTextMaxY(getText(),
                                        getFont(),
                                        (Graphics2D) getGraphics(),
                                        barsScreenBounds.getMaxY());
        final int bottom = getY() + getHeight();
        if (maxY >= bottom) {
            final int neededSpace = (int) Math.ceil(maxY) - bottom;
            calcBarsForBounds(getWidth(),
                              Math.max(1,
                                       getHeight() - neededSpace),
                              false);
        }

    }

    /**
     * paint background dimed and bars over top
     */
    protected void paintComponent(Graphics g) {
        if (!tempHide) {
            paintBars(g, true);
        }
    }

    public void start() {
        setVisible(true);
    }

    public void stop() {
        setVisible(false);
    }

    public void setText(String text) {
        final String old = this.text;
        this.text = text;
        repaint();
        firePropertyChange("text", old, text);
    }

    public JComponent getComponent() {
        return this;
    }

    //
    // METHODS FROM INTERFACE ActionListener
    //

    int iterate;  //we draw use transparency to draw a number of iterations before making a snapshot

    /**
     * Called to animate the rotation of the bar's colors
     */
    public void actionPerformed(ActionEvent e) {
        // rotate colors
        if (colorOffset == (numBars - 1)) {
            colorOffset = 0;
        } else {
            colorOffset++;
        }
        // repaint
        if (barsScreenBounds != null) {
            repaint(barsScreenBounds);
        } else {
            repaint();
        }
        if (useBackBuffer && imageBuf == null) {
            if (iterate < 0) {
                try {
                    makeSnapshot();
                    setOpaque(true);
                } catch (AWTException e1) {
                    e1.printStackTrace();
                }
            } else {
                iterate--;
            }
        }
    }

    public String getText() {
        return text;
    }

    public double getResizeRatio() {
        return resizeRatio;
    }

    public void setResizeRatio(final double resizeRatio) {
        final double old = this.resizeRatio;
        this.resizeRatio = resizeRatio;
        setBounds(getBounds());
        repaint();
        firePropertyChange("resizeRatio", old, resizeRatio);
    }

    public double getMaxBarSize() {
        return maxBarSize;
    }

    public void setMaxBarSize(final double maxBarSize) {
        final double old = this.maxBarSize;
        this.maxBarSize = maxBarSize;
        setBounds(getBounds());
        repaint();
        firePropertyChange("maxBarSize", old, maxBarSize);
    }

    public int getNumBars() {
        return numBars;
    }

    public boolean getUseBackBuffer() {
        return useBackBuffer;
    }

    public boolean isRunning() {
        return isVisible();
    }

    private void makeSnapshot() throws AWTException {
        final Rectangle bounds = getBounds();
        final Point upperLeft = new Point(bounds.x, bounds.y);
        SwingUtilities.convertPointToScreen(upperLeft, this);
        final Rectangle screenRect = new Rectangle(upperLeft.x,
                                                   upperLeft.y,
                                                   bounds.width,
                                                   bounds.height);
        Insets insets = getInsets();
        screenRect.x += insets.left;
        screenRect.y += insets.top;
        screenRect.width -= insets.left + insets.right;
        screenRect.height -= insets.top + insets.bottom;
        // capture window contents
        imageBuf = new Robot().createScreenCapture(screenRect);
        //no need to fade because we are allready using an image that is showing through
    }

    protected void calcBarsForBounds(final int width,
                                     final int height,
                                     final boolean honorMinBarSize) {
        // update centering transform
        centerAndScaleTransform = new AffineTransform();
        centerAndScaleTransform.translate((double) width / 2d,
                                          (double) height / 2d);

        double scale = dScale;

        if (resizeRatio != NO_AUTOMATIC_RESIZING) {
            final int minSpace = Math.min(width, height);
            scale = (minSpace * resizeRatio) / UNSCALED_BAR_SIZE;
            if (maxBarSize != NO_MAX_BAR_SIZE &&
                (UNSCALED_BAR_SIZE * scale) >= maxBarSize) {
                scale = maxBarSize / UNSCALED_BAR_SIZE;
            }
            if (honorMinBarSize && (UNSCALED_BAR_SIZE * scale < minBarSize)) {
                scale = minBarSize / UNSCALED_BAR_SIZE;
            }
        }

        centerAndScaleTransform.scale(scale, scale);

        calcNewBarsBounds();
    }

    private void calcNewBarsBounds() {
        if (barsBounds != null) {
            Area oBounds = new Area(barsBounds);
            oBounds.transform(centerAndScaleTransform);
            barsScreenBounds = oBounds.getBounds();
        }
    }

    protected double paintBars(final Graphics g,
                               final boolean paintBackground) {
        Rectangle oClip = g.getClipBounds();
        if (paintBackground) {
            if (imageBuf != null) {
                // draw background image
                g.drawImage(imageBuf,
                            oClip.x,
                            oClip.y,
                            oClip.x + oClip.width,
                            oClip.y + oClip.height,
                            oClip.x,
                            oClip.y,
                            oClip.x + oClip.width,
                            oClip.y + oClip.height,
                            null);
                g.drawImage(imageBuf, 0, 0, null);
            } else {
                g.setColor(new Color(255, 255, 255, 180));
                g.fillRect(oClip.x, oClip.y, oClip.width, oClip.height);
            }
        }
        // move to center
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
        g2.transform(centerAndScaleTransform);
        // draw ticker
        for (int i = 0; i < bars.length; i++) {
            g2.setColor(colors[i + colorOffset]);
            g2.fill(bars[i]);
        }

        // NOTE: we pass in g and not g2 so that the transformation is not
        // applied.

        // NOTE: this will not contain the size of the sub components, since the
        // paintSubComponents(...) method does not provide this information, and
        // I don't feel like patching the adapter since I don't use it right
        // now. :)

        return drawTextAt(text,
                          getFont(),
                          (Graphics2D) g,
                          getWidth(),
                          barsScreenBounds.getMaxY(),
                          getForeground());
    }

    /**
     * Draw text in a Graphics2D.
     *
     * @param text the text to draw
     * @param font the font to use
     * @param g2 the graphics context to draw in
     * @param width the width of the parent, so it can be centered
     * @param y the height at which to draw
     * @param foreGround the foreground color to draw in
     * @return the y value that is the y param + the text height.
     */
    public static double drawTextAt(String text, Font font, Graphics2D g2,
                                    int width, final double y, Color foreGround) {
        final TextLayout layout = getTextLayout(text, font, g2);
        if (layout != null) {
            Rectangle2D bounds = layout.getBounds();
            g2.setColor(foreGround);
            float textX = (float) (width - bounds.getWidth()) / 2;
            float horizontal = (float) (y + layout.getLeading() + 2 * layout.getAscent());
            layout.draw(g2, textX, horizontal);
            return y + bounds.getHeight();
        } else {
            return 0d;
        }
    }

    public static double getTextMaxY(final String text, final Font font,
                                     final Graphics2D g2, final double y) {
        final TextLayout layout = getTextLayout(text, font, g2);
        if (layout != null) {
            return y + layout.getLeading() + (2 * layout.getAscent()) +
                   layout.getBounds().getHeight();
        } else {
            return 0d;
        }
    }

    private static TextLayout getTextLayout(final String text, final Font font,
                                            final Graphics2D g2) {
        if (text != null && text.length() > 0) {
            FontRenderContext context = g2.getFontRenderContext();
            return new TextLayout(text, font, context);
        } else {
            return null;
        }
    }


    /*
     * Builds the circular shape and returns the result as an array of
     * <code>Area</code>. Each <code>Area</code> is one of the bars composing the
     * shape.
     */
    private static Area[] buildTicker(int i_iBarCount) {
        Area[] ticker = new Area[i_iBarCount];
        Point2D.Double center = new Point2D.Double(0, 0);
        double fixedAngle = 2.0 * Math.PI / ((double) i_iBarCount);

        for (double i = 0.0; i < (double) i_iBarCount; i++) {
            Area primitive = buildPrimitive();

            AffineTransform toCenter = AffineTransform.getTranslateInstance(
                center.getX(), center.getY());
            AffineTransform toBorder = AffineTransform.getTranslateInstance(
                UNSCALED_BAR_SIZE,
                -6.0);
            AffineTransform toCircle = AffineTransform.getRotateInstance(
                -i * fixedAngle, center.getX(), center.getY());

            AffineTransform toWheel = new AffineTransform();
            toWheel.concatenate(toCenter);
            toWheel.concatenate(toBorder);

            primitive.transform(toWheel);
            primitive.transform(toCircle);

            ticker[(int) i] = primitive;
        }

        return ticker;
    }

    /*
     * Builds a bar.
     */
    private static Area buildPrimitive() {
        Rectangle2D.Double body = new Rectangle2D.Double(6, 0, 30, 12);
        Ellipse2D.Double head = new Ellipse2D.Double(0, 0, 12, 12);
        Ellipse2D.Double tail = new Ellipse2D.Double(30, 0, 12, 12);

        Area tick = new Area(body);
        tick.add(new Area(head));
        tick.add(new Area(tail));

        return tick;
    }
}
