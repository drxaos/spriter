package com.github.drxaos.spriter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class SpriterWindow extends JFrame implements Runnable {

    boolean isRunning = true;

    private Thread thread;
    private Canvas canvas;
    private BufferStrategy strategy;
    private BufferedImage background;
    private Graphics2D backgroundGraphics;
    private Graphics2D graphics;

    GraphicsConfiguration config;

    SpriterControl control;

    ArrayList<Sprite> sprites = new ArrayList<Sprite>();

    AtomicBoolean resized = new AtomicBoolean(false);

    AtomicReference<Double>
            viewportWidth = new AtomicReference<>(2d),
            viewportHeight = new AtomicReference<>(2d);

    static {
        System.setProperty("sun.awt.noerasebackground", "true");
    }

    public synchronized SpriterControl getControl() {
        return control;
    }

    public final BufferedImage create(final int width, final int height, final boolean alpha) {
        return config.createCompatibleImage(width, height, alpha ? Transparency.TRANSLUCENT : Transparency.OPAQUE);
    }

    public SpriterWindow(String title) {
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
        Cursor blankCursor = Toolkit.getDefaultToolkit().createCustomCursor(cursorImg, new Point(0, 0), "blank cursor");
        getContentPane().setCursor(blankCursor);

        config = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();

        control = new SpriterControl(this);

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
                SpriterPoint wp = screenToWorld(e.getX(), e.getY());
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
                SpriterPoint wp = screenToWorld(e.getX(), e.getY());
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

    SpriterPoint screenToWorld(int screenX, int screenY) {
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

        return new SpriterPoint(worldX, worldY);
    }

    SpriterPoint worldToScreen(int worldX, int worldY) {
        double width = canvas.getWidth();
        double height = canvas.getHeight();
        double vpWidth = viewportWidth.get();
        double vpHeight = viewportHeight.get();

        double ws = width / vpWidth;
        double hs = height / vpHeight;
        double size = ws > hs ? hs : ws;

        double screenX = (width / 2) + worldX * size;
        double screenY = (height / 2) + worldY * size;

        return new SpriterPoint(screenX, screenY);
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

    public void setViewportWidth(double viewportWidth) {
        this.viewportWidth.set(viewportWidth);
    }

    public void setViewportHeight(double viewportHeight) {
        this.viewportHeight.set(viewportHeight);
    }

    public void addSprite(Sprite sprite) {
        sprites.add(sprite);
    }
}