package com.github.drxaos.spriter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.awt.image.VolatileImage;
import java.util.concurrent.atomic.AtomicBoolean;

public class Output extends JFrame implements IOutput {

    private Canvas canvas;
    private BufferStrategy strategy;
    private VolatileImage background;
    private Graphics2D graphics;
    private Graphics2D backgroundGraphics;

    private GraphicsConfiguration config;

    private Control control;

    private AtomicBoolean resized = new AtomicBoolean(false);

    private boolean closing = false;

    private Cursor defaultCursor, blankCursor;

    static {
        System.setProperty("sun.awt.noerasebackground", "true");
    }

    private Spriter spriter;

    @Override
    public boolean isClosing() {
        return closing;
    }

    /**
     * Get control instance for this Spriter window.
     */
    @Override
    public synchronized Control getControl() {
        return control;
    }

    @Override
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
    @Override
    public void setShowCursor(boolean show) {
        canvas.setCursor(show ? defaultCursor : blankCursor);
    }

    public Output() {
        super("Spriter engine");

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
                control.setMousePressed(e.getButton(), true);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                control.setMousePressed(e.getButton(), false);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                Point wp = spriter.getScene().screenToWorld(e.getX(), e.getY(), getCanvasWidth(), getCanvasHeight());
                control.setMouseClicked(wp.getX(), wp.getY(), e.getButton());
            }
        });
        setFocusTraversalKeysEnabled(false);
        canvas.setFocusTraversalKeysEnabled(false);
        canvas.setFocusable(false);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                control.setKeyPressed(e.getKeyCode(), true);
            }

            @Override
            public void keyReleased(KeyEvent e) {
                control.setKeyPressed(e.getKeyCode(), false);
            }
        });
        canvas.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                mouseMoved(e);
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                if (spriter == null || spriter.getScene() == null) {
                    return;
                }
                Point wp = spriter.getScene().screenToWorld(e.getX(), e.getY(), getCanvasWidth(), getCanvasHeight());
                control.setMouseMoved(wp.getX(), wp.getY());
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

    @Override
    public int getCanvasWidth() {
        return canvas.getWidth();
    }

    @Override
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

    @Override
    public Image getCanvasImage() {
        checkResized();
        return background;
    }

    @Override
    public Graphics2D getCanvasGraphics() {
        checkResized();
        return backgroundGraphics;
    }

    @Override
    public void setCanvasImage(Image canvasImage) {
        Graphics2D bg = getBuffer();
        if (bg != null) {
            bg.drawImage(canvasImage, 0, 0, null);
            bg.dispose();
        }
    }

    @Override
    public void setSpriter(Spriter spriter) {
        this.spriter = spriter;
    }

    private class FrameClose extends WindowAdapter {
        @Override
        public void windowClosing(final WindowEvent e) {
            closing = true;
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

    @Override
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

    @Override
    public Image makeOutputImage(final int width, final int height, final boolean alpha) {
        return makeVolatileImage(width, height, alpha);
    }

    @Override
    public void setDefaultColor(Color color) {
        setBackground(color);
    }
}