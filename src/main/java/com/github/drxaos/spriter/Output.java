package com.github.drxaos.spriter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.awt.image.VolatileImage;
import java.util.concurrent.atomic.AtomicBoolean;

public class Output extends JFrame {

    private Spriter spriter;

    private Canvas canvas;
    private BufferStrategy strategy;
    private VolatileImage background;
    private Graphics2D graphics;
    private Graphics2D backgroundGraphics;

    private GraphicsConfiguration config;

    private Control control;

    private AtomicBoolean resized = new AtomicBoolean(false);

    private Cursor defaultCursor, blankCursor;

    static {
        System.setProperty("sun.awt.noerasebackground", "true");
    }

    /**
     * Get control instance for this Spriter window.
     */
    public synchronized Control getControl() {
        return control;
    }

    public VolatileImage makeVolatileImage(final int width, final int height, final boolean alpha) {
        VolatileImage compatibleImage = config.createCompatibleVolatileImage(width, height, alpha ? Transparency.TRANSLUCENT : Transparency.OPAQUE);
        compatibleImage.setAccelerationPriority(1);
        return compatibleImage;
    }


    /**
     * Show default system cursor inside canvas.
     * <br/>
     * Default is false.
     */
    public void setShowCursor(boolean show) {
        canvas.setCursor(show ? defaultCursor : blankCursor);
    }

    /**
     * Create new Spriter window and start rendering.
     *
     * @param title Title of window
     */
    public Output(String title, Spriter spriter) {
        super(title);
        this.spriter = spriter;

        addWindowListener(new FrameClose());
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(800, 800);
        setLayout(new BorderLayout());
        setIgnoreRepaint(true);

        config = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();

        control = new Control();

        // Canvas
        canvas = new Canvas(config);
        canvas.setIgnoreRepaint(true);
        add(canvas, BorderLayout.CENTER);

        BufferedImage cursorImg = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        defaultCursor = canvas.getCursor();
        blankCursor = Toolkit.getDefaultToolkit().createCustomCursor(cursorImg, new java.awt.Point(0, 0), "blank cursor");
        canvas.setCursor(blankCursor);

        addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                resized.set(true);
            }
        });

        canvas.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                resized.set(true);
            }
        });

        canvas.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                AtomicBoolean b = control.buttons.get(e.getButton());
                if (b == null) {
                    b = new AtomicBoolean();
                    control.buttons.put(e.getButton(), b);
                }
                b.set(true);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                AtomicBoolean b = control.buttons.get(e.getButton());
                if (b == null) {
                    b = new AtomicBoolean();
                    control.buttons.put(e.getButton(), b);
                }
                b.set(false);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                com.github.drxaos.spriter.Point wp = spriter.screenToWorld(e.getX(), e.getY());
                control.c.set(new Click(wp, e.getButton()));
            }
        });
        setFocusTraversalKeysEnabled(false);
        canvas.setFocusTraversalKeysEnabled(false);
        canvas.setFocusable(false);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                AtomicBoolean b = control.keys.get(e.getKeyCode());
                if (b == null) {
                    b = new AtomicBoolean();
                    control.keys.put(e.getKeyCode(), b);
                }
                b.set(true);
                control.k.set(e.getKeyCode());
            }

            @Override
            public void keyReleased(KeyEvent e) {
                AtomicBoolean b = control.keys.get(e.getKeyCode());
                if (b == null) {
                    b = new AtomicBoolean();
                    control.keys.put(e.getKeyCode(), b);
                }
                b.set(false);
            }
        });
        canvas.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                mouseMoved(e);
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                com.github.drxaos.spriter.Point wp = spriter.screenToWorld(e.getX(), e.getY());
                control.mx.set(wp.getX());
                control.my.set(wp.getY());
            }
        });

        setVisible(true);

        // Background & Buffer
        background = makeVolatileImage(canvas.getWidth(), canvas.getHeight(), true);
        backgroundGraphics = (Graphics2D) background.getGraphics();

        canvas.createBufferStrategy(2);
        do {
            strategy = canvas.getBufferStrategy();
        }
        while (strategy == null);
    }

    public int getCanvasWidth() {
        return canvas.getWidth();
    }

    public int getCanvasHeight() {
        return canvas.getHeight();
    }

    @Override
    public boolean sync() {
        return updateScreen();
    }

    private void checkResized() {
        if (resized.getAndSet(false)) {
            background = makeVolatileImage(canvas.getWidth(), canvas.getHeight(), true);
            backgroundGraphics = (Graphics2D) background.getGraphics();
        }
    }

    public Image getCanvasImage() {
        checkResized();
        return background;
    }

    public Graphics2D getCanvasGraphics() {
        checkResized();
        return backgroundGraphics;
    }

    public void setCanvasImage(Image canvasImage) {
        Graphics2D bg = getBuffer();
        if (bg != null) {
            bg.drawImage(canvasImage, 0, 0, null);
            bg.dispose();
        }
    }

    private class FrameClose extends WindowAdapter {
        @Override
        public void windowClosing(final WindowEvent e) {
            spriter.shutdown();
        }
    }

    private Graphics2D getBuffer() {
        if (graphics == null) {
            try {
                graphics = (Graphics2D) strategy.getDrawGraphics();
            } catch (IllegalStateException e) {
                return null;
            }
        }
        return graphics;
    }

    public boolean updateScreen() {
        graphics.dispose();
        graphics = null;
        try {
            strategy.show();
            Toolkit.getDefaultToolkit().sync();
            return (!strategy.contentsLost());

        } catch (NullPointerException e) {
            return true;

        } catch (IllegalStateException e) {
            return true;
        }
    }

    public Image makeOutputImage(final int width, final int height, final boolean alpha) {
        return makeVolatileImage(width, height, alpha);
    }
}