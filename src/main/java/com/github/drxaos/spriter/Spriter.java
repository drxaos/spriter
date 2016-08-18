package com.github.drxaos.spriter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
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

    GraphicsConfiguration config;

    Control control;

    ArrayList<Sprite> sprites = new ArrayList<Sprite>();

    AtomicBoolean resized = new AtomicBoolean(false);

    AtomicReference<Double>
            viewportWidth = new AtomicReference<>(2d),
            viewportHeight = new AtomicReference<>(2d);

    static {
        System.setProperty("sun.awt.noerasebackground", "true");
    }

    public synchronized Control getControl() {
        return control;
    }

    final BufferedImage create(final int width, final int height, final boolean alpha) {
        return config.createCompatibleImage(width, height, alpha ? Transparency.TRANSLUCENT : Transparency.OPAQUE);
    }

    public Spriter(String title) {
        super(title);

        addWindowListener(new FrameClose());
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(800, 800);
        setLayout(new BorderLayout());
        setIgnoreRepaint(true);

        addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                resized.set(true);
            }
        });

        BufferedImage cursorImg = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Cursor blankCursor = Toolkit.getDefaultToolkit().createCustomCursor(cursorImg, new java.awt.Point(0, 0), "blank cursor");
        getContentPane().setCursor(blankCursor);

        config = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();

        control = new Control();

        // Canvas
        canvas = new Canvas(config);
        canvas.setIgnoreRepaint(true);
        add(canvas, BorderLayout.CENTER);

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
                control.c.set(wp);
            }
        });
        canvas.setFocusTraversalKeysEnabled(false);
        canvas.setFocusable(false);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                AtomicBoolean b = control.keys.get(e.getKeyCode());
                if (b == null) {
                    b = new AtomicBoolean();
                    control.buttons.put(e.getKeyCode(), b);
                }
                b.set(true);
            }

            @Override
            public void keyReleased(KeyEvent e) {
                AtomicBoolean b = control.keys.get(e.getKeyCode());
                if (b == null) {
                    b = new AtomicBoolean();
                    control.buttons.put(e.getKeyCode(), b);
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

        for (ArrayList<Sprite> list : layers.values()) {
            list.clear();
        }
        for (Sprite sprite : sprites) {
            Integer l = sprite.layer.get();
            ArrayList<Sprite> list = layers.get(l);
            if (list == null) {
                list = new ArrayList<>();
                layers.put(l, list);
            }
            list.add(sprite);
        }

        double vpWidth = viewportWidth.get();
        double vpHeight = viewportHeight.get();
        double ws = width / vpWidth;
        double hs = height / vpHeight;
        double size = ws > hs ? hs : ws;

        for (Integer l : layers.keySet()) {
            ArrayList<Sprite> list = layers.get(l);
            for (Sprite sprite : list) {
                if (!sprite.visible.get()) {
                    continue;
                }

                int ix = (int) (size * (sprite.x.get() + sprite.dx.get()) + width / 2);
                int iy = (int) (size * (sprite.y.get() + sprite.dy.get()) + height / 2);
                int iw = (int) (size * sprite.w.get());
                int ih = (int) (size * sprite.h.get());

                if (iw < 1 || ih < 1) {
                    continue;
                }

                if (sprite.a.get() != 0) {
                    AffineTransform trans = new AffineTransform();
                    trans.translate(ix, iy);
                    trans.translate(-sprite.dx.get() * size, -sprite.dy.get() * size);
                    trans.rotate(sprite.a.get());
                    trans.translate(sprite.dx.get() * size, sprite.dy.get() * size);
                    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g.drawImage(sprite.getScaled(iw, ih), trans, null);
                } else {
                    g.drawImage(sprite.getScaled(iw, ih), ix, iy, null);
                }
            }
        }

        g.setColor(Color.BLACK);
        g.setBackground(Color.BLACK);
        double brdx = width / 2 - vpWidth / 2 * size;
        double brdy = height / 2 - vpHeight / 2 * size;
        g.fillRect(0, 0, (int) brdx, height);
        g.fillRect((int) (width - brdx), 0, width, height);
        g.fillRect(0, 0, width, (int) (brdy));
        g.fillRect(0, (int) (height - brdy), width, height);
    }

    static BufferedImage getScaledInstance(BufferedImage img,
                                           int targetWidth,
                                           int targetHeight,
                                           Object hint) {
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
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);
            g2.drawImage(ret, 0, 0, w, h, null);
            g2.dispose();

            ret = tmp;
        } while (w != targetWidth || h != targetHeight);

        return ret;
    }

    public void setViewportWidth(double viewportWidth) {
        this.viewportWidth.set(viewportWidth);
    }

    public void setViewportHeight(double viewportHeight) {
        this.viewportHeight.set(viewportHeight);
    }

    public Sprite createSprite(BufferedImage image, double imageCenterX, double imageCenterY, double objectWidth, double objectHeight) {
        Sprite sprite = new Sprite(image, imageCenterX, imageCenterY, objectWidth, objectHeight);
        sprites.add(sprite);
        return sprite;
    }

    public Sprite createSprite(BufferedImage image, double imageCenterX, double imageCenterY, double objectWidth) {
        Sprite sprite = new Sprite(image, imageCenterX, imageCenterY, objectWidth, -1d);
        sprites.add(sprite);
        return sprite;
    }

    public class Sprite {

        AtomicReference<Double> x, y, a, w, h, dx, dy;

        BufferedImage img;
        private BufferedImage scaledImg;
        int imgW, imgH;
        double imgCX, imgCY;
        AtomicInteger layer;
        AtomicBoolean visible;

        Sprite() {
            imgW = 0;
            imgH = 0;
        }

        public Sprite(BufferedImage image, double imageCenterX, double imageCenterY, double objectWidth, double objectHeight) {
            this.img = image;
            this.imgW = img.getWidth();
            this.imgH = img.getHeight();
            this.imgCX = imageCenterX;
            this.imgCY = imageCenterY;

            if (objectHeight < 0) {
                objectHeight = objectWidth * imgH / imgW;
            }

            this.x = new AtomicReference<Double>(0d);
            this.y = new AtomicReference<Double>(0d);
            this.a = new AtomicReference<Double>(0d);
            this.w = new AtomicReference<Double>(0d);
            this.h = new AtomicReference<Double>(0d);
            this.dx = new AtomicReference<Double>(0d);
            this.dy = new AtomicReference<Double>(0d);
            this.layer = new AtomicInteger(1);
            this.visible = new AtomicBoolean(true);

            this.w.set(objectWidth);
            this.h.set(objectHeight);
            this.dx.set(-(objectWidth * imageCenterX / imgW));
            this.dy.set(-(objectHeight * imageCenterY / imgH));
        }

        public Sprite setLayer(int layer) {
            this.layer.set(layer);
            return this;
        }

        public Sprite setVisible(boolean visible) {
            this.visible.set(visible);
            return this;
        }

        public Sprite setX(double x) {
            this.x.set(x);
            return this;
        }

        public Sprite setY(double y) {
            this.y.set(y);
            return this;
        }

        public Sprite setPos(double x, double y) {
            setX(x);
            setY(y);
            return this;
        }

        public Sprite setAngle(double a) {
            this.a.set(a);
            return this;
        }

        public Sprite setWidth(double w) {
            this.w.set(w);
            this.dx.set(-(w * imgCX / imgW));
            return this;
        }

        public Sprite setHeight(double h) {
            this.h.set(h);
            this.dy.set(-(h * imgCY / imgH));
            return this;
        }

        public Sprite setSide(double wh) {
            setWidth(wh);
            setHeight(wh);
            return this;
        }

        BufferedImage getScaled(int targetWidth, int targetHeight) {
            if (scaledImg == null || targetWidth != scaledImg.getWidth() || targetHeight != scaledImg.getHeight()) {
                scaledImg = getScaledInstance(img, targetWidth, targetHeight, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            }
            return scaledImg;
        }

        public Sprite createGhost() {
            SpriteGhost ghost = new SpriteGhost(this);
            sprites.add(ghost);
            return ghost;
        }

        public Sprite clone() {
            Sprite sprite = new Sprite(img, imgCX, imgCY, w.get(), h.get());
            sprites.add(sprite);
            return sprite;
        }
    }

    public class SpriteGhost extends Sprite {
        Sprite sprite;

        public SpriteGhost(Sprite sprite) {
            this.sprite = sprite;

            img = sprite.img;
            imgW = sprite.imgW;
            imgH = sprite.imgH;
            imgCX = sprite.imgCX;
            imgCY = sprite.imgCY;

            this.x = new AtomicReference<Double>(sprite.x.get());
            this.y = new AtomicReference<Double>(sprite.y.get());
            this.a = new AtomicReference<Double>(sprite.a.get());

            this.w = sprite.w;
            this.h = sprite.h;
            this.dx = sprite.dx;
            this.dy = sprite.dy;
            this.layer = new AtomicInteger(sprite.layer.get());
            this.visible = new AtomicBoolean(sprite.visible.get());
        }

        @Override
        public BufferedImage getScaled(int targetWidth, int targetHeight) {
            return sprite.getScaled(targetWidth, targetHeight);
        }
    }


    public class Point {
        final double x, y;

        public Point(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }
    }

    public class Control {

        AtomicReference<Double>
                mx = new AtomicReference<Double>(0d),
                my = new AtomicReference<Double>(0d);
        AtomicReference<Spriter.Point> c = new AtomicReference<>();

        Map<Integer, AtomicBoolean> buttons = new HashMap<>();
        Map<Integer, AtomicBoolean> keys = new HashMap<>();

        public double getMouseX() {
            return mx.get();
        }

        public double getMouseY() {
            return my.get();
        }

        public Spriter.Point getClick() {
            return c.getAndSet(null);
        }

        public boolean isButtonDown(int btn) {
            AtomicBoolean b = buttons.get(btn);
            if (b == null) {
                return false;
            }
            return b.get();
        }

        public boolean isKeyDown(int key) {
            AtomicBoolean b = keys.get(key);
            if (b == null) {
                return false;
            }
            return b.get();
        }

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