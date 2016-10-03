package com.github.drxaos.spriter;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class Spriter implements Runnable {

    private SpriterJFrameOutput output;

    private AtomicBoolean shutdown = new AtomicBoolean(false);

    private Graphics2D graphics;
    private final Object renderLock = new Object();

    private long fps = 0;
    private long fpsCounterStart = 0;
    private long currentFrameStart = 0;
    private int targetFps = 40;
    private int dynamicSleep = 1000 / targetFps;
    private AtomicInteger fpsCounter = new AtomicInteger(0);

    private boolean shouldGC = false;
    private boolean autoGC = true;
    private boolean debugGC = false;

    private RenderChain renderChain;
    private Renderer renderer;

    private GraphicsConfiguration config;

    final ArrayList<Proto> protos = new ArrayList<>();
    final ArrayList<Sprite> sprites = new ArrayList<>();

    AtomicReference<Color> bgColor = new AtomicReference<>(Color.WHITE);
    AtomicReference<Color> borderColor = new AtomicReference<>(Color.BLACK);

    /**
     * Get control instance for this Spriter window.
     */
    public synchronized Control getControl() {
        return output.getControl();
    }

    public Image makeOutputImage(final int width, final int height, final boolean alpha) {
        return output.makeOutputImage(width, height, alpha);
    }

    /**
     * Show default system cursor inside canvas.
     * <br/>
     * Default is false.
     */
    public void setShowCursor(boolean show) {
        output.setShowCursor(show);
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
        this.targetFps = targetFps;
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
        output = new SpriterJFrameOutput(title);
        output.setSpriter(this);

        renderChain = renderer = new Renderer(this);
        currentFrameStart = System.currentTimeMillis();

        Thread thread = new Thread(this);
        thread.setName("Spriter rendering loop");
        thread.start();
    }

    public Spriter(SpriterJFrameOutput output) {
        this.output = output;
        output.setSpriter(this);

        renderChain = renderer = new Renderer(this);
        currentFrameStart = System.currentTimeMillis();

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
            if (shouldGC) {
                shouldGC = false;
                garbageCollect();
            }
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
            renderLock.notifyAll();
        }

        int sleep = (int) (dynamicSleep - (System.currentTimeMillis() - currentFrameStart));
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
        while (true) {
            if (shutdown.get()) {
                break;
            }

            if (System.currentTimeMillis() - fpsCounterStart > 1000) {
                fps = fpsCounter.getAndSet(0);
                if (fps < targetFps && dynamicSleep > 0) {
                    if (fps > targetFps * 1.2) {
                        dynamicSleep = 1000 / targetFps;
                    } else {
                        dynamicSleep--;
                    }
                }
                if (fps > targetFps && dynamicSleep < 1000) {
                    if (fps < targetFps * 0.8) {
                        dynamicSleep = 1000 / targetFps;
                    } else {
                        dynamicSleep++;
                    }
                }
                fpsCounterStart = System.currentTimeMillis();
                shouldGC = autoGC;
            }

            Image background = output.getCanvasImage();
            Graphics2D backgroundGraphics = output.getCanvasGraphics();

            do {
                long currentFrame = fpsCounter.get();

                renderChain.chain(background, backgroundGraphics, background.getWidth(null), background.getHeight(null));

                output.setCanvasImage(background);

                try {
                    synchronized (renderLock) {
                        if (fpsCounter.get() == currentFrame) {
                            renderLock.wait();
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } while (!output.updateScreen());

        }
    }

    public Point screenToWorld(int screenX, int screenY) {
        return renderer.screenToWorld(screenX, screenY, output.getCanvasWidth(), output.getCanvasHeight());
    }

    public Point worldToScreen(int worldX, int worldY) {
        return renderer.worldToScreen(worldX, worldY, output.getCanvasWidth(), output.getCanvasHeight());
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

    public void shutdown() {
        this.shutdown.set(true);
    }
}