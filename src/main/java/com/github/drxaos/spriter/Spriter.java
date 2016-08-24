package com.github.drxaos.spriter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class Spriter extends JFrame implements Runnable {

    boolean isRunning = true;

    private Thread thread;
    private Canvas canvas;
    private BufferStrategy strategy;
    private BufferedImage background;
    private Graphics2D backgroundGraphics;
    private Graphics2D graphics;
    private boolean smoothScaling = true;

    GraphicsConfiguration config;

    Control control;

    ArrayList<Sprite> sprites = new ArrayList<>();
    Map<Integer, AtomicBoolean> ignoredLayers = new HashMap<>();

    AtomicBoolean resized = new AtomicBoolean(false);

    AtomicReference<Double>
            viewportWidth = new AtomicReference<>(2d),
            viewportHeight = new AtomicReference<>(2d),
            viewportShiftX = new AtomicReference<>(0d),
            viewportShiftY = new AtomicReference<>(0d),
            viewportShiftA = new AtomicReference<>(0d);

    static {
        System.setProperty("sun.awt.noerasebackground", "true");
    }

    /**
     * Get control instance for this Spriter window.
     */
    public synchronized Control getControl() {
        return control;
    }

    final BufferedImage create(final int width, final int height, final boolean alpha) {
        return config.createCompatibleImage(width, height, alpha ? Transparency.TRANSLUCENT : Transparency.OPAQUE);
    }

    Cursor defaultCursor, blankCursor;

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
    public Spriter(String title) {
        super(title);

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
        background = create(canvas.getWidth(), canvas.getHeight(), true);
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
            isRunning = false;
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
     * Reenable disabled layer.
     */
    public void enableLayer(int layer) {
        AtomicBoolean b = ignoredLayers.get(layer);
        if (b == null) {
            b = new AtomicBoolean(false);
            ignoredLayers.put(layer, b);
        } else {
            b.set(false);
        }
    }

    /**
     * Disable layer. Sprites contained by this layer won't be rendered.
     */
    public void disableLayer(int layer) {
        AtomicBoolean b = ignoredLayers.get(layer);
        if (b == null) {
            b = new AtomicBoolean(true);
            ignoredLayers.put(layer, b);
        } else {
            b.set(true);
        }
    }

    public void run() {
        backgroundGraphics = (Graphics2D) background.getGraphics();
        long fpsWait = (long) (1.0 / 60 * 1000);
        main:
        while (true) {
            if (isRunning) {
                long renderStart = System.nanoTime();

                if (resized.getAndSet(false)) {
                    background = create(canvas.getWidth(), canvas.getHeight(), true);
                    backgroundGraphics = (Graphics2D) background.getGraphics();
                }

                // Update Graphics
                do {
                    Graphics2D bg = getBuffer();
                    if (!isRunning) {
                        break main;
                    }
                    render(backgroundGraphics, background.getWidth(), background.getHeight());
                    // thingy
                    bg.drawImage(background, 0, 0, null);
                    bg.dispose();
                } while (!updateScreen());

                // Better do some FPS limiting here
                long renderTime = (System.nanoTime() - renderStart) / 1000000;
                try {
                    Thread.sleep(Math.max(0, fpsWait - renderTime));
                } catch (InterruptedException e) {
                    Thread.interrupted();
                    break;
                }
            }
        }
    }

    /**
     * Use bilinear interpolation.
     * <br/>
     * Default is true.
     */
    public void setSmoothScaling(boolean smoothScaling) {
        this.smoothScaling = smoothScaling;
        synchronized (sprites) {
            for (Sprite sprite : sprites) {
                for (int x = 0; x < sprite.scaledImg.length; x++) {
                    for (int y = 0; y < sprite.scaledImg[x].length; y++) {
                        sprite.scaledImg[x][y] = null;
                    }
                }
            }
        }
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

    private TreeMap<Integer, ArrayList<Sprite>> layers = new TreeMap<>();

    void render(Graphics2D g, int width, int height) {
        g.setColor(Color.WHITE);
        g.setBackground(Color.WHITE);
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

        // remove ignored layers
        for (Map.Entry<Integer, AtomicBoolean> entry : ignoredLayers.entrySet()) {
            if (entry.getValue().get()) {
                layers.get(entry.getKey()).clear();
            }
        }

        double vpWidth = viewportWidth.get();
        double vpHeight = viewportHeight.get();
        double ws = width / vpWidth;
        double hs = height / vpHeight;
        double size = ws > hs ? hs : ws;

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

                int ix = (int) Math.round(size * (px + rotX + sprite.dx.get()) + 0.5d * width);
                int iy = (int) Math.round(size * (py + rotY + sprite.dy.get()) + 0.5d * height);
                int iw = (int) Math.round(size * sprite.w.get());
                int ih = (int) Math.round(size * sprite.h.get());

                if (iw < 1 || ih < 1) {
                    continue;
                }

                if (pa + sprite.a.get() != 0) {
                    AffineTransform trans = new AffineTransform();
                    trans.translate(ix, iy);

                    trans.translate(-sprite.dx.get() * size, -sprite.dy.get() * size);
                    trans.rotate(pa + sprite.a.get());
                    trans.translate(sprite.dx.get() * size, sprite.dy.get() * size);

                    trans.translate(sprite.w.get() / 2 * size, sprite.h.get() / 2 * size);
                    trans.scale(sprite.flipX.get() ? -1 : 1, sprite.flipY.get() ? -1 : 1);
                    trans.translate(-sprite.w.get() / 2 * size, -sprite.h.get() / 2 * size);

                    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g.drawImage(sprite.getScaled(iw, ih), trans, null);
                } else {
                    BufferedImage scaled = sprite.getScaled(iw, ih);
                    int rx = ix;
                    int ry = iy;
                    int rw = scaled.getWidth();
                    int rh = scaled.getHeight();
                    if (sprite.flipX.get()) {
                        rx += rw;
                        rw *= -1;
                    }
                    if (sprite.flipY.get()) {
                        ry += rh;
                        rh *= -1;
                    }
                    g.drawImage(scaled, rx, ry, rw, rh, null);
                }
            }
        }

        g.setColor(Color.BLACK);
        g.setBackground(Color.BLACK);
        double brdx = 0.5d * width - 0.5d * vpWidth * size;
        double brdy = 0.5d * height - 0.5d * vpHeight * size;
        g.fillRect(0, 0, (int) brdx, height);
        g.fillRect((int) (width - brdx), 0, width, height);
        g.fillRect(0, 0, width, (int) (brdy));
        g.fillRect(0, (int) (height - brdy), width, height);
    }

    BufferedImage getScaledInstance(BufferedImage img,
                                    int targetWidth,
                                    int targetHeight) {
        int type = (img.getTransparency() == Transparency.OPAQUE) ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
        BufferedImage ret = img;
        int w = img.getWidth();
        int h = img.getHeight();
        boolean scaleDownW = w >= targetWidth;
        boolean scaleDownH = h >= targetHeight;

        do {
            if (scaleDownW && w > targetWidth) {
                w /= 2;
                if (w < targetWidth) {
                    w = targetWidth;
                }
            }

            if (scaleDownH && h > targetHeight) {
                h /= 2;
                if (h < targetHeight) {
                    h = targetHeight;
                }
            }

            if (!scaleDownW && w < targetWidth) {
                w *= 2;
                if (w > targetWidth) {
                    w = targetWidth;
                }
            }

            if (!scaleDownH && h < targetHeight) {
                h *= 2;
                if (h > targetHeight) {
                    h = targetHeight;
                }
            }

            BufferedImage tmp = new BufferedImage(w, h, type);
            Graphics2D g2 = tmp.createGraphics();
            if (smoothScaling) {
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            } else {
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            }
            g2.drawImage(ret, 0, 0, w, h, null);
            g2.dispose();

            ret = tmp;
        } while (w != targetWidth || h != targetHeight);

        return ret;
    }

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
     * Rotate viewport.
     * <br/>
     * Default is 0.0
     */
    public void setViewportAngle(double angle) {
        this.viewportShiftA.set(angle);
    }

    /**
     * Create new sprite prototype. It's invisible and has zero width and height.
     *
     * @param image        Original image of new sprite.
     * @param imageCenterX Distance from left side to center of image.
     * @param imageCenterY Distance from top side to center of image.
     * @return new sprite prototype
     */
    public Sprite createSpriteProto(BufferedImage image, double imageCenterX, double imageCenterY) {
        Sprite sprite = new Sprite(image, imageCenterX, imageCenterY, -1, -1, 0, 0).setVisible(false);
        synchronized (sprites) {
            sprites.add(sprite);
        }
        return sprite;
    }

    /**
     * Create new sprite prototype. It's invisible and has zero width and height.
     *
     * @param image        Original image of new sprite.
     * @param imageCenterX Distance from left side to center of image.
     * @param imageCenterY Distance from top side to center of image.
     * @param frameWidth   Single animation frame wifth.
     * @param frameHeight  Single animation frame height.
     * @return new sprite prototype
     */
    public Sprite createSpriteProto(BufferedImage image, double imageCenterX, double imageCenterY, int frameWidth, int frameHeight) {
        Sprite sprite = new Sprite(image, imageCenterX, imageCenterY, frameWidth, frameHeight, 0, 0).setVisible(false);
        synchronized (sprites) {
            sprites.add(sprite);
        }
        return sprite;
    }

    /**
     * Create new font sprite prototype. It's invisible and has zero width and height.
     *
     * @param image        Original image of new sprite.
     * @param imageCenterX Distance from left side to center of image.
     * @param imageCenterY Distance from top side to center of image.
     * @param frameWidth   Single animation frame wifth.
     * @param frameHeight  Single animation frame height.
     * @param symbols      Array of symbols values in font. Example "abc\ndef\ghi".
     * @return new sprite prototype
     */
    public Font createFont(BufferedImage image, double imageCenterX, double imageCenterY, int frameWidth, int frameHeight, String symbols) {
        Font sprite = new Font(image, imageCenterX, imageCenterY, frameWidth, frameHeight, 0, 0, symbols).setVisible(false);
        synchronized (sprites) {
            sprites.add(sprite);
        }
        return sprite;
    }

    /**
     * Create new sprite.
     *
     * @param image        Original image of new sprite.
     * @param imageCenterX Distance from left side to center of image.
     * @param imageCenterY Distance from top side to center of image.
     * @param objectWidth  Rendering width.
     * @param objectHeight Rendering height.
     * @return new sprite
     */
    public Sprite createSprite(BufferedImage image, double imageCenterX, double imageCenterY, double objectWidth, double objectHeight) {
        Sprite sprite = new Sprite(image, imageCenterX, imageCenterY, -1, -1, objectWidth, objectHeight);
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
     * @param image        Original image of new sprite.
     * @param imageCenterX Distance from left side to center of image.
     * @param imageCenterY Distance from top side to center of image.
     * @param objectWidth  Rendering width.
     * @return new sprite
     */
    public Sprite createSprite(BufferedImage image, double imageCenterX, double imageCenterY, double objectWidth) {
        Sprite sprite = new Sprite(image, imageCenterX, imageCenterY, -1, -1, objectWidth, -1d);
        synchronized (sprites) {
            sprites.add(sprite);
        }
        return sprite;
    }

    /**
     * Create new animated sprite.
     *
     * @param image        Original image of new sprite.
     * @param imageCenterX Distance from left side to center of image.
     * @param imageCenterY Distance from top side to center of image.
     * @param frameWidth   Single animation frame wifth.
     * @param frameHeight  Single animation frame height.
     * @param objectWidth  Rendering width.
     * @param objectHeight Rendering height.
     * @return new sprite
     */
    public Sprite createSprite(BufferedImage image, double imageCenterX, double imageCenterY, int frameWidth, int frameHeight, double objectWidth, double objectHeight) {
        Sprite sprite = new Sprite(image, imageCenterX, imageCenterY, frameWidth, frameHeight, objectWidth, objectHeight);
        synchronized (sprites) {
            sprites.add(sprite);
        }
        return sprite;
    }

    /**
     * Create new animated sprite.
     * <br/>
     * Rendering height will be proportional to width.
     *
     * @param image        Original image of new sprite.
     * @param imageCenterX Distance from left side to center of image.
     * @param imageCenterY Distance from top side to center of image.
     * @param frameWidth   Single animation frame wifth.
     * @param frameHeight  Single animation frame height.
     * @param objectWidth  Rendering width.
     * @return new sprite
     */
    public Sprite createSprite(BufferedImage image, double imageCenterX, double imageCenterY, int frameWidth, int frameHeight, double objectWidth) {
        Sprite sprite = new Sprite(image, imageCenterX, imageCenterY, frameWidth, frameHeight, objectWidth, -1d);
        synchronized (sprites) {
            sprites.add(sprite);
        }
        return sprite;
    }

    public class Sprite {

        AtomicReference<Double> x, y, a, w, h, dx, dy;

        BufferedImage img;
        private BufferedImage[][] scaledImg;
        int imgW, imgH;
        int frmW, frmH;
        double imgCX, imgCY;
        AtomicInteger layer;
        AtomicInteger frameX, frameY;
        AtomicBoolean visible;
        AtomicBoolean remove;
        AtomicBoolean flipX, flipY;

        AtomicReference<Sprite> parent;

        Sprite() {
            imgW = 0;
            imgH = 0;
        }

        public Sprite(BufferedImage image, double imageCenterX, double imageCenterY, int frameWidth, int frameHeight, double objectWidth, double objectHeight) {
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

            if (objectHeight < 0) {
                objectHeight = objectWidth * frmH / frmW;
            }

            scaledImg = new BufferedImage[imgW / frmW + 1][imgH / frmH + 1];

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
            this.flipX = new AtomicBoolean(false);
            this.flipY = new AtomicBoolean(false);

            this.w.set(objectWidth);
            this.h.set(objectHeight);
            this.dx.set(-(objectWidth * imageCenterX / frmW));
            this.dy.set(-(objectHeight * imageCenterY / frmH));
        }

        public Sprite(Sprite sprite) {
            this.img = sprite.img;
            this.imgW = sprite.imgW;
            this.imgH = sprite.imgH;
            this.imgCX = sprite.imgCX;
            this.imgCY = sprite.imgCY;
            this.frmW = sprite.frmW;
            this.frmH = sprite.frmH;

            scaledImg = new BufferedImage[imgW / frmW + 1][imgH / frmH + 1];

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
            this.parent = new AtomicReference<>();
            this.remove = new AtomicBoolean(false);
            this.flipX = new AtomicBoolean(false);
            this.flipY = new AtomicBoolean(false);
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
            this.dx.set(-(w * imgCX / frmW));
            return this;
        }

        /**
         * Set new width and proportional height to sprite.
         */
        public Sprite setWidthProportional(double w) {
            this.w.set(w);
            this.dx.set(-(w * imgCX / frmW));
            setHeight(w * frmH / frmW);
            return this;
        }

        /**
         * Set new height of sprite.
         */
        public Sprite setHeight(double h) {
            this.h.set(h);
            this.dy.set(-(h * imgCY / frmH));
            return this;
        }

        /**
         * Set new height and proportional width to sprite.
         */
        public Sprite setHeightProportional(double h) {
            this.h.set(h);
            this.dy.set(-(h * imgCY / frmH));
            setWidth(h * frmW / frmH);
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

        BufferedImage getScaled(int targetWidth, int targetHeight) {
            return getScaled(targetWidth, targetHeight, frameX.get(), frameY.get());
        }

        protected BufferedImage getScaled(int targetWidth, int targetHeight, int frameX, int frameY) {
            BufferedImage scaledFrame = scaledImg[frameX][frameY];
            if (scaledFrame == null || targetWidth != scaledFrame.getWidth() || targetHeight != scaledFrame.getHeight()) {
                scaledFrame = getScaledInstance(img.getSubimage(frameX * frmW, frameY * frmH, frmW, frmH),
                        targetWidth, targetHeight);
                scaledImg[frameX][frameY] = scaledFrame;
            }
            return scaledFrame;
        }

        /**
         * Set current frame of animated frame.
         */
        public Sprite setFrame(int n) {
            frameX.set(n);
            return this;
        }

        /**
         * Set current frame row of animated frame.
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
         * Create a ghost linked to sprite. All properties will be copied from sprite.
         * <br/>
         * Ghost have it's own position, angle, layer, animation frame and visibility.
         */
        public Sprite createGhost() {
            SpriteGhost ghost = new SpriteGhost(this);
            synchronized (sprites) {
                sprites.add(ghost);
            }
            return ghost;
        }

        /**
         * Create a copy of sprite.
         */
        public Sprite clone() {
            Sprite sprite = new Sprite(this);
            synchronized (sprites) {
                sprites.add(sprite);
            }
            return sprite;
        }

        /**
         * Remove sprite from scene.
         */
        public void remove() {
            this.remove.set(true);
        }
    }

    public class SpriteGhost extends Sprite {
        Sprite sprite;

        public SpriteGhost(Sprite sprite) {
            this.sprite = sprite;

            img = null;
            imgW = sprite.imgW;
            imgH = sprite.imgH;
            frmH = sprite.frmH;
            frmW = sprite.frmW;
            imgCX = sprite.imgCX;
            imgCY = sprite.imgCY;

            this.x = new AtomicReference<>(sprite.x.get());
            this.y = new AtomicReference<>(sprite.y.get());
            this.a = new AtomicReference<>(sprite.a.get());

            this.w = sprite.w;
            this.h = sprite.h;
            this.dx = sprite.dx;
            this.dy = sprite.dy;
            this.layer = new AtomicInteger(sprite.layer.get());
            this.frameX = new AtomicInteger(sprite.frameX.get());
            this.frameY = new AtomicInteger(sprite.frameY.get());
            this.visible = new AtomicBoolean(sprite.visible.get());
            this.parent = new AtomicReference<>();
            this.remove = new AtomicBoolean(false);
            this.flipX = new AtomicBoolean(sprite.flipX.get());
            this.flipY = new AtomicBoolean(sprite.flipY.get());
        }

        @Override
        BufferedImage getScaled(int targetWidth, int targetHeight) {
            return sprite.getScaled(targetWidth, targetHeight, frameX.get(), frameY.get());
        }
    }

    public class Font extends Sprite {
        Map<Character, Point> symbolsMap;

        public Font() {
        }

        public Font(BufferedImage image, double imageCenterX, double imageCenterY, int frameWidth, int frameHeight, double objectWidth, double objectHeight, String symbols) {
            super(image, imageCenterX, imageCenterY, frameWidth, frameHeight, objectWidth, objectHeight);

            symbolsMap = new HashMap<>();
            int x = 0, y = 0;
            for (char c : symbols.toCharArray()) {
                if (c == '\n') {
                    x = 0;
                    y++;
                } else {
                    symbolsMap.put(c, new Point(x, y));
                    x++;
                }
            }
        }

        public Font(Font font) {
            super(font);
            this.symbolsMap = font.symbolsMap;
        }

        public Sprite getChar(char c) {
            Point point = symbolsMap.get(c);
            if (point == null) {
                point = new Point(0, 0);
            }
            return clone().setFrame((int) point.getX()).setFrameRow((int) point.getY());
        }

        @Override
        public Font setAngle(double a) {
            super.setAngle(a);
            return this;
        }

        @Override
        public Font setFlipX(boolean flipx) {
            super.setFlipX(flipx);
            return this;
        }

        @Override
        public Font setFlipY(boolean flipy) {
            super.setFlipY(flipy);
            return this;
        }

        @Override
        public Font setWidth(double w) {
            super.setWidth(w);
            return this;
        }

        @Override
        public Font setWidthProportional(double w) {
            super.setWidthProportional(w);
            return this;
        }

        @Override
        public Font setHeight(double h) {
            super.setHeight(h);
            return this;
        }

        @Override
        public Font setHeightProportional(double h) {
            super.setHeightProportional(h);
            return this;
        }

        @Override
        public Font setSquareSide(double wh) {
            super.setSquareSide(wh);
            return this;
        }

        @Override
        public Font setVisible(boolean visible) {
            super.setVisible(visible);
            return this;
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