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

public class SpriterWindow extends JFrame implements Runnable {

    private boolean isRunning = true;

    private Thread thread;
    private Canvas canvas;
    private BufferStrategy strategy;
    private BufferedImage background;
    private Graphics2D backgroundGraphics;
    private Graphics2D graphics;

    private GraphicsConfiguration config;

    private SpriterControl control;

    ArrayList<Sprite> sprites = new ArrayList<Sprite>();

    public synchronized SpriterControl getControl() {
        return control;
    }

    // create a hardware accelerated image
    public final BufferedImage create(final int width, final int height, final boolean alpha) {
        return config.createCompatibleImage(width, height, alpha ? Transparency.TRANSLUCENT : Transparency.OPAQUE);
    }

    // Setup
    public SpriterWindow(String title) {
        super(title);

        addWindowListener(new FrameClose());
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(800, 800);
        setLayout(new BorderLayout());

        addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                background = create(canvas.getWidth(), canvas.getHeight(), true);
                backgroundGraphics = (Graphics2D) background.getGraphics();
            }
        });

        BufferedImage cursorImg = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Cursor blankCursor = Toolkit.getDefaultToolkit().createCustomCursor(cursorImg, new Point(0, 0), "blank cursor");
        getContentPane().setCursor(blankCursor);

        config = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();

        control = new SpriterControl(this);

        // Canvas
        canvas = new Canvas(config);
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

    // Screen and buffer stuff

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

    SpriterPoint screenToWorld(int x, int y) {
        int width = canvas.getWidth();
        int height = canvas.getHeight();

        double size, brdx = 0, brdy = 0;
        if (width > height) {
            size = height;
            brdx = (0d + width - height) / 2;
        } else {
            size = width;
            brdy = (0d + height - width) / 2;
        }

        double wx = (0d + x - brdx) / (size / 2) - 1;
        double wy = (0d + y - brdy) / (size / 2) - 1;

        if (wx > 1) {
            wx = 1;
        }
        if (wx < -1) {
            wx = -1;
        }
        if (wy > 1) {
            wy = 1;
        }
        if (wy < -1) {
            wy = -1;
        }

        return new SpriterPoint(wx, wy);
    }

    private TreeMap<Integer, ArrayList<Sprite>> layers = new TreeMap<>();

    public void render(Graphics2D g, int width, int height) {
        g.setColor(Color.WHITE);
        g.setBackground(Color.WHITE);
        g.fillRect(0, 0, width, height);

        double size, brdx = 0, brdy = 0;
        if (width > height) {
            size = height;
            brdx = (0d + width - height) / 2;
        } else {
            size = width;
            brdy = (0d + height - width) / 2;
        }

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
        for (Integer l : layers.keySet()) {
            ArrayList<Sprite> list = layers.get(l);
            for (Sprite sprite : list) {
                if (!sprite.visible.get()) {
                    continue;
                }

                int ix = (int) (size / 2 * (1 + sprite.x.get() + sprite.dx.get()) + brdx);
                int iy = (int) (size / 2 * (1 + sprite.y.get() + sprite.dy.get()) + brdy);
                int iw = (int) (size / 2 * sprite.w.get());
                int ih = (int) (size / 2 * sprite.h.get());

                if (sprite.a.get() != 0) {
                    AffineTransform trans = new AffineTransform();
                    trans.translate(ix, iy);
                    trans.translate(-sprite.dx.get() * size / 2, -sprite.dy.get() * size / 2);
                    trans.rotate(sprite.a.get());
                    trans.translate(sprite.dx.get() * size / 2, sprite.dy.get() * size / 2);
                    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g.drawImage(sprite.getTransformed(iw, ih), trans, null);
                } else {
                    g.drawImage(sprite.getTransformed(iw, ih), ix, iy, null);
                }
            }
        }

        g.setColor(Color.BLACK);
        g.setBackground(Color.BLACK);
        if (brdx > 0) {
            g.fillRect(0, 0, (int) brdx, height);
            g.fillRect((int) (width - brdx), 0, width, height);
        }
        if (brdy > 0) {
            g.fillRect(0, 0, width, (int) (brdy));
            g.fillRect(0, (int) (height - brdy), width, height);
        }
    }

    public void addSprite(Sprite sprite) {
        sprites.add(sprite);
    }
}