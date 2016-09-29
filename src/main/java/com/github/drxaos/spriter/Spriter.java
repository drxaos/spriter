package com.github.drxaos.spriter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.awt.image.VolatileImage;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class Spriter extends JFrame implements Runnable {

    boolean shutdown = false;

    private Thread thread;
    private Canvas canvas;
    private BufferStrategy strategy;
    private VolatileImage background;
    private Graphics2D backgroundGraphics;
    private Graphics2D graphics;
    private final Object renderWait = new Object();

    private int fps;
    private long fpsCounterStart = 0;
    private AtomicInteger fpsCounter = new AtomicInteger(0);

    private PainterChain painterChain;
    private Renderer renderer;

    GraphicsConfiguration config;

    Control control;

    ArrayList<Sprite> sprites = new ArrayList<>();

    AtomicBoolean resized = new AtomicBoolean(false);

    AtomicReference<Color> bgColor = new AtomicReference<>(Color.WHITE);
    Cursor defaultCursor, blankCursor;

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

        painterChain = renderer = new Renderer();

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

        thread = new Thread(this);
        thread.start();
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
     * End modification of scene. Spriter will render frame after this call.
     */
    public void render() {
        fpsCounter.incrementAndGet();
        synchronized (renderWait) {
            renderWait.notifyAll();
        }
    }

    /**
     * Set new head of painters chain.
     * Default is instance of Renderer
     */
    public void setPainterChain(PainterChain head) {
        if (head == null) {
            throw new IllegalArgumentException("head painter must not be null");
        }
        this.painterChain = head;
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
        backgroundGraphics = (Graphics2D) background.getGraphics();
        while (true) {
            if (shutdown) {
                break;
            }

            if (System.currentTimeMillis() - fpsCounterStart > 1000) {
                fps = fpsCounter.getAndSet(0);
                fpsCounterStart = System.currentTimeMillis();
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

                painterChain.chain(background, backgroundGraphics, background.getWidth(), background.getHeight());

                bg.drawImage(background, 0, 0, null);
                bg.dispose();

                try {
                    synchronized (renderWait) {
                        renderWait.wait();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } while (!updateScreen());

        }
    }

    Point screenToWorld(int screenX, int screenY) {
        return renderer.screenToWorld(screenX, screenY);
    }

    Point worldToScreen(int worldX, int worldY) {
        return renderer.worldToScreen(worldX, worldY);
    }

    public void setBackgroundColor(Color color) {
        if (color != null) {
            bgColor.set(color);
        }
    }

    /**
     * Add a painter to the end of painters chain
     */
    public void addPostProcessor(PainterChain postProcessor) {
        PainterChain chain = postProcessor;
        while (chain.next != null) {
            chain = chain.next;
        }
        chain.next = postProcessor;
    }

    abstract static public class PainterChain {
        protected PainterChain next;

        public void setNext(PainterChain next) {
            this.next = next;
        }

        public Image chain(Image img, Graphics2D g, int width, int height) {
            Image render = render(img, g, width, height);
            if (next == null) {
                return render;
            }
            if (render != img) {
                g = (Graphics2D) render.getGraphics();
                width = render.getWidth(null);
                height = render.getHeight(null);
            }
            return next.chain(render, g, width, height);
        }

        public Image render(Image img, Graphics2D g, int width, int height) {
            render(g, width, height);
            return img;
        }

        abstract public void render(Graphics2D g, int width, int height);
    }

    public class Renderer extends PainterChain {

        private int rps;
        private long rpsCounterStart = 0;
        private AtomicInteger rpsCounter = new AtomicInteger(0);

        AtomicReference<Double>
                viewportWidth = new AtomicReference<>(2d),
                viewportHeight = new AtomicReference<>(2d),
                viewportShiftX = new AtomicReference<>(0d),
                viewportShiftY = new AtomicReference<>(0d),
                viewportShiftA = new AtomicReference<>(0d);

        AtomicBoolean bilinearInterpolation = new AtomicBoolean(true);
        AtomicBoolean antialiasing = new AtomicBoolean(true);

        public Renderer() {
        }

        public Renderer(Renderer proto) {
            this.bilinearInterpolation = new AtomicBoolean(proto.bilinearInterpolation.get());
            this.antialiasing = new AtomicBoolean(proto.antialiasing.get());
            this.debug = new AtomicBoolean(proto.debug.get());
            this.viewportWidth = new AtomicReference<>(proto.viewportWidth.get());
            this.viewportHeight = new AtomicReference<>(proto.viewportHeight.get());
            this.viewportShiftX = new AtomicReference<>(proto.viewportShiftX.get());
            this.viewportShiftY = new AtomicReference<>(proto.viewportShiftY.get());
            this.viewportShiftA = new AtomicReference<>(proto.viewportShiftA.get());
        }

        private AtomicBoolean debug = new AtomicBoolean(false);

        private TreeMap<Integer, ArrayList<Sprite>> layers = new TreeMap<>();


        /**
         * Set new viewport width.
         * <br/>
         * Default is 2.0
         */
        public void setViewportWidth(double viewportWidth) {
            this.viewportWidth.set(viewportWidth);
        }

        /**
         * Set new viewport height.
         * <br/>
         * Default is 2.0
         */
        public void setViewportHeight(double viewportHeight) {
            this.viewportHeight.set(viewportHeight);
        }

        /**
         * Shift viewport along X axis.
         * <br/>
         * Default is 0.0
         */
        public void setViewportShiftX(double shiftX) {
            this.viewportShiftX.set(shiftX);
        }

        /**
         * Shift viewport along Y axis.
         * <br/>
         * Default is 0.0
         */
        public void setViewportShiftY(double shiftY) {
            this.viewportShiftY.set(shiftY);
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
         * Rotate viewport.
         * <br/>
         * Default is 0.0
         */
        public void setViewportAngle(double angle) {
            this.viewportShiftA.set(angle);
        }

        Point screenToWorld(int screenX, int screenY) {
            double width = canvas.getWidth();
            double height = canvas.getHeight();
            double vpWidth = viewportWidth.get();
            double vpHeight = viewportHeight.get();

            double ws = width / vpWidth;
            double hs = height / vpHeight;
            double size = ws > hs ? hs : ws;

            double worldX = (screenX - width / 2) / size;
            double worldY = (screenY - height / 2) / size;

            worldX = worldX > vpWidth / 2 ? vpWidth / 2 : worldX;
            worldY = worldY > vpHeight / 2 ? vpHeight / 2 : worldY;
            worldX = worldX < -vpWidth / 2 ? -vpWidth / 2 : worldX;
            worldY = worldY < -vpHeight / 2 ? -vpHeight / 2 : worldY;

            return new Point(worldX, worldY);
        }

        Point worldToScreen(int worldX, int worldY) {
            double width = canvas.getWidth();
            double height = canvas.getHeight();
            double vpWidth = viewportWidth.get();
            double vpHeight = viewportHeight.get();

            double ws = width / vpWidth;
            double hs = height / vpHeight;
            double size = ws > hs ? hs : ws;

            double screenX = (width / 2) + worldX * size;
            double screenY = (height / 2) + worldY * size;

            return new Point(screenX, screenY);
        }

        /**
         * Images antialiasing.
         * <br/>
         * Default is true
         */
        public void setAntialiasing(boolean antialiasing) {
            this.antialiasing.set(antialiasing);
        }

        /**
         * Images interpolation.
         * <br/>
         * Default is true
         */
        public void setBilinearInterpolation(boolean bilinearInterpolation) {
            this.bilinearInterpolation.set(bilinearInterpolation);
        }

        /**
         * Show debug info
         */
        public void setDebug(boolean debug) {
            this.debug.set(debug);
        }

        public void render(Graphics2D g, int width, int height) {
            if (System.currentTimeMillis() - rpsCounterStart > 1000) {
                rps = rpsCounter.getAndSet(0);
                rpsCounterStart = System.currentTimeMillis();
            }

            rpsCounter.incrementAndGet();

            g.setColor(bgColor.get());
            g.setBackground(bgColor.get());
            g.fillRect(0, 0, width, height);

            // clear layers
            for (ArrayList<Sprite> list : layers.values()) {
                list.clear();
            }

            // fill layers
            synchronized (sprites) {
                for (Iterator<Sprite> iterator = sprites.iterator(); iterator.hasNext(); ) {
                    Sprite sprite = iterator.next();
                    if (sprite.remove.get()) {
                        iterator.remove();
                    } else {
                        Integer l = sprite.layer.get();
                        ArrayList<Sprite> list = layers.get(l);
                        if (list == null) {
                            list = new ArrayList<>();
                            layers.put(l, list);
                        }
                        list.add(sprite);
                    }
                }
            }

            double vpWidth = viewportWidth.get();
            double vpHeight = viewportHeight.get();
            double ws = width / vpWidth;
            double hs = height / vpHeight;
            double size = ws > hs ? hs : ws;

            int drawCounter = 0;

            for (Integer l : layers.keySet()) {
                ArrayList<Sprite> list = layers.get(l);

                nextSprite:
                for (Sprite sprite : list) {
                    if (!sprite.visible.get()) {
                        continue;
                    }

                    double px = -viewportShiftX.get(),
                            py = -viewportShiftY.get(),
                            pa = -viewportShiftA.get();

                    if (sprite.hud.get()) {
                        px = py = pa = 0;
                    }

                    Sprite parent = sprite.parent.get();
                    while (parent != null) {
                        if (!parent.visible.get()) {
                            continue nextSprite;
                        }

                        // rotating vector
                        double oldX = parent.x.get();
                        double oldY = parent.y.get();
                        double rotX = oldX * Math.cos(pa) - oldY * Math.sin(pa);
                        double rotY = oldX * Math.sin(pa) + oldY * Math.cos(pa);

                        px += rotX;
                        py += rotY;
                        pa += parent.a.get();

                        parent = parent.parent.get();
                    }

                    // rotating vector
                    double oldX = sprite.x.get();
                    double oldY = sprite.y.get();
                    double rotX = oldX * Math.cos(pa) - oldY * Math.sin(pa);
                    double rotY = oldX * Math.sin(pa) + oldY * Math.cos(pa);

                    double ix = size * (px + rotX + sprite.dx.get()) + 0.5d * width;
                    double iy = size * (py + rotY + sprite.dy.get()) + 0.5d * height;
                    double iw = size * sprite.w.get();
                    double ih = size * sprite.h.get();

                    if (iw < 1 || ih < 1 ||
                            ix + iw + ih < 0 || iy + iw + ih < 0 ||
                            ix - iw - ih > width || iy - iw - ih > height) {
                        continue;
                    }

                    drawCounter++;

                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, antialiasing.get() ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);
                    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, bilinearInterpolation.get() ? RenderingHints.VALUE_INTERPOLATION_BILINEAR : RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, sprite.alpha.get().floatValue()));

                    AffineTransform trans = new AffineTransform();
                    trans.translate(ix, iy);

                    trans.translate(-sprite.dx.get() * size, -sprite.dy.get() * size);
                    trans.rotate(pa + sprite.a.get());
                    trans.scale(1d * iw / sprite.proto.frmW, 1d * ih / sprite.proto.frmH);
                    trans.translate(sprite.dx.get() * size / (1d * iw / sprite.proto.frmW), sprite.dy.get() * size / (1d * ih / sprite.proto.frmH));

                    trans.translate(sprite.w.get() / 2 * size / (1d * iw / sprite.proto.frmW), sprite.h.get() / 2 * size / (1d * ih / sprite.proto.frmH));
                    trans.scale(sprite.flipX.get() ? -1 : 1, sprite.flipY.get() ? -1 : 1);
                    trans.translate(-sprite.w.get() / 2 * size / (1d * iw / sprite.proto.frmW), -sprite.h.get() / 2 * size / (1d * ih / sprite.proto.frmH));

                    g.drawRenderedImage(sprite.getFrame(), trans);
                }
            }

            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1));
            g.setColor(Color.BLACK);
            g.setBackground(Color.BLACK);
            double brdx = 0.5d * width - 0.5d * vpWidth * size;
            double brdy = 0.5d * height - 0.5d * vpHeight * size;
            g.fillRect(0, 0, (int) brdx, height);
            g.fillRect((int) (width - brdx), 0, width, height);
            g.fillRect(0, 0, width, (int) (brdy));
            g.fillRect(0, (int) (height - brdy), width, height);

            if (debug.get()) {
                g.setColor(Color.WHITE);
                g.drawString("FPS: " + fps, 0, 20);
                g.setColor(Color.BLACK);
                g.drawString("FPS: " + fps, 1, 21);
                g.setColor(Color.WHITE);
                g.drawString("RPS: " + rps, 0, 40);
                g.setColor(Color.BLACK);
                g.drawString("RPS: " + rps, 1, 41);
                g.setColor(Color.WHITE);
                g.drawString("OBJ: " + drawCounter, 0, 60);
                g.setColor(Color.BLACK);
                g.drawString("OBJ: " + drawCounter, 1, 61);
            }
        }
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
        return new Proto(image, imageCenterX, imageCenterY, -1, -1);
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
        return new Proto(image, imageCenterX, imageCenterY, frameWidth, frameHeight);
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
        Sprite sprite = new Sprite(proto, objectWidth, objectHeight);
        synchronized (sprites) {
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
        Sprite sprite = new Sprite(proto, objectWidth, -1d);
        synchronized (sprites) {
            sprites.add(sprite);
        }
        return sprite;
    }

    public class Proto {
        BufferedImage img;
        private BufferedImage[][] scaledImg;
        int imgW, imgH;
        int frmW, frmH;
        double imgCX, imgCY;

        public Proto(BufferedImage image, double imageCenterX, double imageCenterY, int frameWidth, int frameHeight) {
            this.img = image;
            this.imgW = img.getWidth();
            this.imgH = img.getHeight();
            this.imgCX = imageCenterX;
            this.imgCY = imageCenterY;

            if (frameWidth < 0) {
                frameWidth = imgW;
            }
            if (frameHeight < 0) {
                frameHeight = imgH;
            }

            this.frmW = frameWidth;
            this.frmH = frameHeight;

            scaledImg = new BufferedImage[imgW / frmW + 1][imgH / frmH + 1];
        }

        protected BufferedImage getFrame(int frameX, int frameY) {
            synchronized (scaledImg) {
                BufferedImage scaledFrame = scaledImg[frameX][frameY];
                if (scaledFrame == null) {
                    if (imgW == frmW && imgH == frmH) {
                        scaledFrame = img;
                    } else {
                        scaledFrame = img.getSubimage(frameX * frmW, frameY * frmH, frmW, frmH);
                    }
                    scaledImg[frameX][frameY] = scaledFrame;
                }
                return scaledFrame;
            }
        }

        public Sprite newInstance(double objectWidth) {
            return createSprite(this, objectWidth);
        }

        public Sprite newInstance(double objectWidth, double objectHeight) {
            return createSprite(this, objectWidth, objectHeight);
        }
    }

    public class Sprite {

        Proto proto;

        AtomicReference<Double> x, y, a, w, h, dx, dy;
        AtomicInteger layer;
        AtomicInteger frameX, frameY;
        AtomicBoolean visible;
        AtomicBoolean remove;
        AtomicBoolean hud;
        AtomicBoolean flipX, flipY;
        AtomicReference<Double> alpha;
        AtomicReference<Sprite> parent;

        public Sprite(Proto proto, double objectWidth, double objectHeight) {
            this.proto = proto;

            if (objectHeight < 0) {
                objectHeight = objectWidth * proto.frmH / proto.frmW;
            }

            this.x = new AtomicReference<>(0d);
            this.y = new AtomicReference<>(0d);
            this.a = new AtomicReference<>(0d);
            this.w = new AtomicReference<>(0d);
            this.h = new AtomicReference<>(0d);
            this.dx = new AtomicReference<>(0d);
            this.dy = new AtomicReference<>(0d);
            this.layer = new AtomicInteger(1);
            this.frameX = new AtomicInteger(0);
            this.frameY = new AtomicInteger(0);
            this.visible = new AtomicBoolean(true);
            this.parent = new AtomicReference<>();
            this.remove = new AtomicBoolean(false);
            this.hud = new AtomicBoolean(false);
            this.flipX = new AtomicBoolean(false);
            this.flipY = new AtomicBoolean(false);

            this.alpha = new AtomicReference<>(1d);

            this.w.set(objectWidth);
            this.h.set(objectHeight);
            this.dx.set(-(objectWidth * proto.imgCX / proto.frmW));
            this.dy.set(-(objectHeight * proto.imgCY / proto.frmH));
        }

        public Sprite(Sprite sprite) {
            this.proto = sprite.proto;

            this.x = new AtomicReference<>(sprite.x.get());
            this.y = new AtomicReference<>(sprite.y.get());
            this.a = new AtomicReference<>(sprite.a.get());
            this.w = new AtomicReference<>(sprite.w.get());
            this.h = new AtomicReference<>(sprite.h.get());
            this.dx = new AtomicReference<>(sprite.dx.get());
            this.dy = new AtomicReference<>(sprite.dy.get());
            this.layer = new AtomicInteger(sprite.layer.get());
            this.frameX = new AtomicInteger(sprite.frameX.get());
            this.frameY = new AtomicInteger(sprite.frameY.get());
            this.visible = new AtomicBoolean(sprite.visible.get());
            this.parent = new AtomicReference<>(sprite.parent.get());
            this.remove = new AtomicBoolean(sprite.remove.get());
            this.hud = new AtomicBoolean(sprite.hud.get());
            this.flipX = new AtomicBoolean(sprite.flipX.get());
            this.flipY = new AtomicBoolean(sprite.flipY.get());

            this.alpha = new AtomicReference<>(sprite.alpha.get());
        }

        /**
         * Move sprite to another layer.
         * <br/>
         * Spriter engine draws all layers in ascending order.
         * <br/>
         * Default layer is 1
         */
        public Sprite setLayer(int layer) {
            this.layer.set(layer);
            return this;
        }

        /**
         * Set visibility of sprite.
         * <br/>
         * Default is true
         */
        public Sprite setVisible(boolean visible) {
            this.visible.set(visible);
            return this;
        }

        /**
         * Make sprite flipped right to left.
         * <br/>
         * Default is false
         */
        public Sprite setFlipX(boolean flipx) {
            this.flipX.set(flipx);
            return this;
        }

        /**
         * Make sprite flipped top to bottom.
         * <br/>
         * Default is false
         */
        public Sprite setFlipY(boolean flipy) {
            this.flipY.set(flipy);
            return this;
        }

        /**
         * Set X coordinate of sprite center.
         * <br/>
         * Default is 0.0
         */
        public Sprite setX(double x) {
            this.x.set(x);
            return this;
        }

        /**
         * Set Y coordinate of sprite center.
         * <br/>
         * Default is 0.0
         */
        public Sprite setY(double y) {
            this.y.set(y);
            return this;
        }

        /**
         * Set coordinates of sprite center.
         * <br/>
         * Default is 0.0, 0.0
         */
        public Sprite setPos(double x, double y) {
            setX(x);
            setY(y);
            return this;
        }

        /**
         * Set coordinates of sprite center.
         * <br/>
         * Default is 0.0, 0.0
         */
        public Sprite setPos(Point point) {
            setX(point.getX());
            setY(point.getY());
            return this;
        }

        /**
         * Set angle of sprite.
         * <br/>
         * Default is 0.0
         */
        public Sprite setAngle(double a) {
            this.a.set(a);
            return this;
        }

        /**
         * Set new width of sprite.
         */
        public Sprite setWidth(double w) {
            this.w.set(w);
            this.dx.set(-(w * proto.imgCX / proto.frmW));
            return this;
        }

        /**
         * Set new width and proportional height to sprite.
         */
        public Sprite setWidthProportional(double w) {
            this.w.set(w);
            this.dx.set(-(w * proto.imgCX / proto.frmW));
            setHeight(w * proto.frmH / proto.frmW);
            return this;
        }

        /**
         * Set new height of sprite.
         */
        public Sprite setHeight(double h) {
            this.h.set(h);
            this.dy.set(-(h * proto.imgCY / proto.frmH));
            return this;
        }

        /**
         * Set new height and proportional width to sprite.
         */
        public Sprite setHeightProportional(double h) {
            this.h.set(h);
            this.dy.set(-(h * proto.imgCY / proto.frmH));
            setWidth(h * proto.frmW / proto.frmH);
            return this;
        }

        /**
         * Get current object width.
         */
        public double getWidth() {
            return w.get();
        }

        /**
         * Get current object height.
         */
        public double getHeight() {
            return h.get();
        }

        /**
         * Set new width and height of sprite.
         */
        public Sprite setSquareSide(double wh) {
            setWidth(wh);
            setHeight(wh);
            return this;
        }

        BufferedImage getFrame() {
            return proto.getFrame(frameX.get(), frameY.get());
        }


        /**
         * Set current frame of animated sprite.
         */
        public Sprite setFrame(int n, int row) {
            setFrame(n);
            setFrameRow(row);
            return this;
        }

        /**
         * Set current frame of animated sprite.
         */
        public Sprite setFrame(int n) {
            frameX.set(n);
            return this;
        }

        /**
         * Set current frame row of animated sprite.
         */
        public Sprite setFrameRow(int row) {
            frameY.set(row);
            return this;
        }

        /**
         * Set parent for this sprite.
         * Parent adds to this sprite it's coordinates and angle.
         */
        public Sprite setParent(Sprite parent) {
            this.parent.set(parent);
            return this;
        }

        /**
         * Create new instance of sprite. Image data will be shared between all instances.
         */
        public Sprite newInstance() {
            Sprite inst = new Sprite(this);
            synchronized (sprites) {
                sprites.add(inst);
            }
            return inst;
        }

        /**
         * Remove sprite from scene.
         */
        public void remove() {
            this.remove.set(true);
        }

        /**
         * Make sprite to move with viewport.
         */
        public Sprite setHud(boolean hud) {
            this.hud.set(hud);
            return this;
        }

        public void setAlpha(double alpha) {
            this.alpha.set(alpha);
        }
    }

    public static class Point {
        final double x, y;

        public Point(double x, double y) {
            this.x = x;
            this.y = y;
        }

        /**
         * X coordinate.
         */
        public double getX() {
            return x;
        }

        /**
         * Y coordinate.
         */
        public double getY() {
            return y;
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Point point = (Point) o;

            if (Double.compare(point.x, x) != 0) return false;
            return Double.compare(point.y, y) == 0;

        }

        @Override
        public int hashCode() {
            int result;
            long temp;
            temp = Double.doubleToLongBits(x);
            result = (int) (temp ^ (temp >>> 32));
            temp = Double.doubleToLongBits(y);
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            return result;
        }
    }

    public class Click extends Point {

        final int button;

        public Click(double x, double y, int button) {
            super(x, y);
            this.button = button;
        }

        public Click(Point point, int button) {
            super(point.getX(), point.getY());
            this.button = button;
        }

        /**
         * Mouse button.
         * <br/>
         * See also {@link java.awt.event.MouseEvent}
         */
        public int getButton() {
            return button;
        }
    }

    public class Control {

        AtomicReference<Double>
                mx = new AtomicReference<>(0d),
                my = new AtomicReference<>(0d);
        AtomicReference<Spriter.Click> c = new AtomicReference<>();
        AtomicReference<Integer> k = new AtomicReference<>(null);

        Map<Integer, AtomicBoolean> buttons = new HashMap<>();
        Map<Integer, AtomicBoolean> keys = new HashMap<>();

        /**
         * Get current mouse X coordinate.
         */
        public double getMouseX() {
            return mx.get();
        }

        /**
         * Get current mouse Y coordinate.
         */
        public double getMouseY() {
            return my.get();
        }

        /**
         * Get current mouse coordinates.
         */
        public Point getMousePos() {
            return new Point(getMouseX(), getMouseY());
        }

        /**
         * Get last mouse click coordinates and button.
         */
        public Spriter.Click getClick() {
            return c.getAndSet(null);
        }

        /**
         * Get last pressed key.
         */
        public Integer getKeyPress() {
            return k.getAndSet(null);
        }

        /**
         * Check if mouse button is pressed now.
         * <br/>
         * Example:
         * <pre>
         * control.isButtonDown(MouseEvent.BUTTON1)
         * </pre>
         * <br/>
         * See also {@link java.awt.event.MouseEvent}
         */
        public boolean isButtonDown(int btn) {
            AtomicBoolean b = buttons.get(btn);
            if (b == null) {
                return false;
            }
            return b.get();
        }

        /**
         * Check if keyboard key is pressed now.
         * <br/>
         * Example:
         * <pre>
         * control.isKeyDown(KeyEvent.VK_UP)
         * </pre>
         * <br/>
         * See also {@link java.awt.event.KeyEvent}
         */
        public boolean isKeyDown(int key) {
            AtomicBoolean b = keys.get(key);
            if (b == null) {
                return false;
            }
            return b.get();
        }

        /**
         * Check if any of keyboard keys from list is pressed now.
         * <br/>
         * Example:
         * <pre>
         * control.isAnyKeyDown(KeyEvent.VK_UP, KeyEvent.VK_W, KeyEvent.VK_NUMPAD8)
         * </pre>
         * <br/>
         * See also {@link java.awt.event.KeyEvent}
         */
        public boolean isAnyKeyDown(int... keys) {
            for (int key : keys) {
                if (isKeyDown(key)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Dump input state to console.
         */
        public void dump() {
            String dump = "";
            for (Map.Entry<Integer, AtomicBoolean> entry : buttons.entrySet()) {
                if (entry.getValue().get()) {
                    dump += "b" + entry.getKey() + ",";
                }
            }
            for (Map.Entry<Integer, AtomicBoolean> entry : keys.entrySet()) {
                if (entry.getValue().get()) {
                    dump += "k" + entry.getKey() + ",";
                }
            }
            System.out.println(dump);
        }
    }

}