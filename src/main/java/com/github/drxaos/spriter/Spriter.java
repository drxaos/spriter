package com.github.drxaos.spriter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.awt.image.VolatileImage;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class Spriter extends JFrame implements Runnable {

    private boolean shutdown = false;

    private Canvas canvas;
    private BufferStrategy strategy;
    private VolatileImage background;
    private Graphics2D graphics;
    private final Object renderLock = new Object();

    private long fps = 0;
    private long fpsCounterStart = 0;
    private long currentFrameStart = 0;
    private float targetFps = 1000f / 40;
    private AtomicInteger fpsCounter = new AtomicInteger(0);

    private boolean shouldGC = false;
    private boolean autoGC = true;
    private boolean debugGC = true;

    private RenderChain renderChain;
    private Renderer renderer;

    private GraphicsConfiguration config;

    private Control control;

    final ArrayList<Proto> protos = new ArrayList<>();
    final ArrayList<Sprite> sprites = new ArrayList<>();

    private AtomicBoolean resized = new AtomicBoolean(false);

    AtomicReference<Color> bgColor = new AtomicReference<>(Color.WHITE);
    AtomicReference<Color> borderColor = new AtomicReference<>(Color.BLACK);
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
     * Show debug info
     */
    public void setDebug(boolean debug) {
        renderer.setDebug(debug);
    }

    /**
     * Set target FPS. Spriter will sleep in endFrame().
     */
    public void setTargetFps(int targetFps) {
        this.targetFps = 1000f / targetFps;
    }

    /**
     * Auto garbage collection every second.
     */
    public void setAutoGC(boolean autoGC) {
        this.autoGC = autoGC;
    }

    /**
     * Show collected objects count.
     */
    public void setDebugGC(boolean debugGC) {
        this.debugGC = debugGC;
    }

    /**
     * Create new Spriter window and start rendering.
     *
     * @param title Title of window
     */
    public Spriter(String title) {
        super(title);

        addWindowListener(new FrameClose());
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(800, 800);
        setLayout(new BorderLayout());
        setIgnoreRepaint(true);

        renderChain = renderer = new Renderer(this);

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
                Point wp = screenToWorld(e.getX(), e.getY());
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
                Point wp = screenToWorld(e.getX(), e.getY());
                control.mx.set(wp.getX());
                control.my.set(wp.getY());
            }
        });

        setVisible(true);

        // Background & Buffer
        background = makeVolatileImage(canvas.getWidth(), canvas.getHeight(), true);
        canvas.createBufferStrategy(2);
        do {
            strategy = canvas.getBufferStrategy();
        }
        while (strategy == null);

        Thread thread = new Thread(this);
        thread.setName("Spriter rendering loop");
        thread.start();
    }

    public Sprite getSpriteByIndex(int index) {
        return sprites.get(index);
    }

    public Proto getProtoByIndex(int index) {
        return protos.get(index);
    }

    private class FrameClose extends WindowAdapter {
        @Override
        public void windowClosing(final WindowEvent e) {
            shutdown = true;
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

    private boolean updateScreen() {
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

    /**
     * Begin modification of scene.
     */
    public void beginFrame() {
        currentFrameStart = System.currentTimeMillis();
    }

    /**
     * End modification of scene. Spriter will render frame after this call.
     */
    public void endFrame() throws InterruptedException {
        synchronized (renderLock) {
            synchronized (sprites) {
                for (Sprite sprite : sprites) {
                    if (sprite.snapshotGetRemove()) {
                        // skip
                    } else if (sprite.isDirty()) {
                        sprite.snapshot();
                    }
                }
            }
            fpsCounter.incrementAndGet();
            if (shouldGC) {
                shouldGC = false;
                garbageCollect();
            }
            renderLock.notifyAll();
        }

        int sleep = (int) (targetFps - (System.currentTimeMillis() - currentFrameStart));
        if (sleep > 0) {
            Thread.sleep(sleep);
        }
    }

    public int garbageCollect() {
        int collected = 0;
        synchronized (sprites) {
            int marked = 1;
            while (marked > 0) {
                marked = 0;
                for (int currentIndex = sprites.size() - 1; currentIndex >= 0; currentIndex--) {
                    Sprite current = sprites.get(currentIndex);

                    if (current.isRemoved()) {
                        Sprite last = sprites.remove(sprites.size() - 1);
                        for (Sprite sprite : sprites) {
                            if (sprite.getParentId() == currentIndex) {
                                sprite.remove();
                                marked++;
                            } else if (sprite.getParentId() == last.getIndex()) {
                                sprite.setParentId(currentIndex);
                            }
                        }
                        if (last != current) {
                            sprites.set(currentIndex, last);
                            last.setIndex(currentIndex);
                        }

                        collected++;
                    }
                }
            }
            if (debugGC) {
                System.err.println("Collected: " + collected + " (left: " + sprites.size() + ")");
            }
        }
        System.gc();
        return collected;
    }

    /**
     * Set new head of painters chain.
     * Default is instance of Renderer
     */
    public void setRenderChain(RenderChain head) {
        if (head == null) {
            throw new IllegalArgumentException("head painter must not be null");
        }
        this.renderChain = head;
    }

    /**
     * Get default renderer
     */
    public Renderer getDefaultRenderer() {
        return renderer;
    }

    /**
     * Get new renderer
     */
    public Renderer createRenderer() {
        return new Renderer(getDefaultRenderer());
    }

    public void run() {
        Graphics2D backgroundGraphics = (Graphics2D) background.getGraphics();
        while (true) {
            if (shutdown) {
                break;
            }

            if (System.currentTimeMillis() - fpsCounterStart > 1000) {
                fps = fpsCounter.getAndSet(0);
                fpsCounterStart = System.currentTimeMillis();
                shouldGC = autoGC;
            }

            if (resized.getAndSet(false)) {
                background = makeVolatileImage(canvas.getWidth(), canvas.getHeight(), true);
                backgroundGraphics = (Graphics2D) background.getGraphics();
            }

            do {
                Graphics2D bg = getBuffer();
                if (bg == null) {
                    continue;
                }

                long currentFrame = fpsCounter.get();

                renderChain.chain(background, backgroundGraphics, background.getWidth(), background.getHeight());
                bg.drawImage(background, 0, 0, null);
                bg.dispose();

                try {
                    synchronized (renderLock) {
                        if (fpsCounter.get() == currentFrame) {
                            renderLock.wait();
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } while (!updateScreen());

        }
    }

    public Point screenToWorld(int screenX, int screenY) {
        return renderer.screenToWorld(screenX, screenY, canvas.getWidth(), canvas.getHeight());
    }

    public Point worldToScreen(int worldX, int worldY) {
        return renderer.worldToScreen(worldX, worldY, canvas.getWidth(), canvas.getHeight());
    }

    public Point screenToWorld(int screenX, int screenY, int canvasWidth, int canvasHeight) {
        return renderer.screenToWorld(screenX, screenY, canvasWidth, canvasHeight);
    }

    public Point worldToScreen(int worldX, int worldY, int canvasWidth, int canvasHeight) {
        return renderer.worldToScreen(worldX, worldY, canvasWidth, canvasHeight);
    }

    public void setBackgroundColor(Color color) {
        if (color != null) {
            bgColor.set(color);
        }
    }

    public void setBorderColor(Color color) {
        if (color != null) {
            borderColor.set(color);
        }
    }

    /**
     * Add a painter to the end of painters chain
     */
    public void addPostProcessor(RenderChain postProcessor) {
        RenderChain chain = postProcessor;
        while (chain.next != null) {
            chain = chain.next;
        }
        chain.next = postProcessor;
    }

    /**
     * Set new viewport width.
     * <br/>
     * Default is 2.0
     */
    public void setViewportWidth(double viewportWidth) {
        renderer.setViewportWidth(viewportWidth);
    }

    /**
     * Set new viewport height.
     * <br/>
     * Default is 2.0
     */
    public void setViewportHeight(double viewportHeight) {
        renderer.setViewportHeight(viewportHeight);
    }

    /**
     * Shift viewport.
     * <br/>
     * Default is 0.0, 0.0
     */
    public void setViewportShift(double shiftX, double shiftY) {
        setViewportShiftX(shiftX);
        setViewportShiftY(shiftY);
    }

    /**
     * Shift viewport along X axis.
     * <br/>
     * Default is 0.0
     */
    public void setViewportShiftX(double shiftX) {
        renderer.setViewportShiftX(shiftX);
    }

    /**
     * Shift viewport along Y axis.
     * <br/>
     * Default is 0.0
     */
    public void setViewportShiftY(double shiftY) {
        renderer.setViewportShiftY(shiftY);
    }

    /**
     * Rotate viewport.
     * <br/>
     * Default is 0.0
     */
    public void setViewportAngle(double angle) {
        renderer.setViewportAngle(angle);
    }

    /**
     * Create new sprite prototype.
     *
     * @param image        Original image of new sprite.
     * @param imageCenterX Distance from left side to center of image.
     * @param imageCenterY Distance from top side to center of image.
     * @return new sprite prototype
     */
    public Proto createProto(BufferedImage image, double imageCenterX, double imageCenterY) {
        Proto proto = new Proto(this, image, imageCenterX, imageCenterY, -1, -1);
        synchronized (protos) {
            proto.setIndex(protos.size());
            protos.add(proto);
        }
        return proto;
    }

    /**
     * Create new animated sprite prototype.
     *
     * @param image        Original image of new sprite.
     * @param imageCenterX Distance from left side to center of image.
     * @param imageCenterY Distance from top side to center of image.
     * @param frameWidth   Single animation frame wifth.
     * @param frameHeight  Single animation frame height.
     * @return new sprite prototype
     */
    public Proto createProto(BufferedImage image, double imageCenterX, double imageCenterY, int frameWidth, int frameHeight) {
        Proto proto = new Proto(this, image, imageCenterX, imageCenterY, frameWidth, frameHeight);
        synchronized (protos) {
            proto.setIndex(protos.size());
            protos.add(proto);
        }
        return proto;
    }

    /**
     * Create new sprite.
     *
     * @param proto        Sprite prototype.
     * @param objectWidth  Rendering width.
     * @param objectHeight Rendering height.
     * @return new sprite
     */
    public Sprite createSprite(Proto proto, double objectWidth, double objectHeight) {
        Sprite sprite = new Sprite(this, proto, objectWidth, objectHeight);
        synchronized (sprites) {
            sprite.setIndex(sprites.size());
            sprites.add(sprite);
        }
        return sprite;
    }

    /**
     * Create new sprite.
     * <br/>
     * Rendering height will be proportional to width.
     *
     * @param proto       Sprite prototype.
     * @param objectWidth Rendering width.
     * @return new sprite
     */
    public Sprite createSprite(Proto proto, double objectWidth) {
        Sprite sprite = new Sprite(this, proto, objectWidth, -1d);
        synchronized (sprites) {
            sprite.setIndex(sprites.size());
            sprites.add(sprite);
        }
        return sprite;
    }

    Sprite copySprite(Sprite sprite) {
        Sprite inst = new Sprite(sprite);
        synchronized (sprites) {
            sprites.add(inst);
        }
        return inst;

    }

    public long getFps() {
        return fps;
    }
}