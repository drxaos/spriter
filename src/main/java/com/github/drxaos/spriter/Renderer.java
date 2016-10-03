package com.github.drxaos.spriter;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Scene renderer
 */
public class Renderer extends RenderChain {

    private Spriter spriter;
    private int rps;
    private long rpsCounterStart = 0;
    private AtomicInteger rpsCounter = new AtomicInteger(0);

    private AtomicReference<Double>
            viewportWidth = new AtomicReference<>(2d),
            viewportHeight = new AtomicReference<>(2d),
            viewportShiftX = new AtomicReference<>(0d),
            viewportShiftY = new AtomicReference<>(0d),
            viewportShiftA = new AtomicReference<>(0d);

    private AtomicBoolean bilinearInterpolation = new AtomicBoolean(true);
    private AtomicBoolean antialiasing = new AtomicBoolean(true);

    Renderer(Spriter spriter) {
        this.spriter = spriter;
    }

    Renderer(Renderer proto) {
        this.spriter = proto.spriter;
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

    private TreeMap<Double, ArrayList<Sprite>> layers = new TreeMap<>();


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

    public Point screenToWorld(int screenX, int screenY, int canvasWidth, int canvasHeight) {
        double vpWidth = viewportWidth.get();
        double vpHeight = viewportHeight.get();

        double ws = canvasWidth / vpWidth;
        double hs = canvasHeight / vpHeight;
        double size = ws > hs ? hs : ws;

        double worldX = (screenX - canvasWidth / 2) / size;
        double worldY = (screenY - canvasHeight / 2) / size;

        worldX = worldX > vpWidth / 2 ? vpWidth / 2 : worldX;
        worldY = worldY > vpHeight / 2 ? vpHeight / 2 : worldY;
        worldX = worldX < -vpWidth / 2 ? -vpWidth / 2 : worldX;
        worldY = worldY < -vpHeight / 2 ? -vpHeight / 2 : worldY;

        return new Point(worldX, worldY);
    }

    public Point worldToScreen(int worldX, int worldY, int canvasWidth, int canvasHeight) {
        double vpWidth = viewportWidth.get();
        double vpHeight = viewportHeight.get();

        double ws = canvasWidth / vpWidth;
        double hs = canvasHeight / vpHeight;
        double size = ws > hs ? hs : ws;

        double screenX = (canvasWidth / 2) + worldX * size;
        double screenY = (canvasHeight / 2) + worldY * size;

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

    public void render(Scene scene, Graphics2D g, int width, int height) {
        if (System.currentTimeMillis() - rpsCounterStart > 1000) {
            rps = rpsCounter.getAndSet(0);
            rpsCounterStart = System.currentTimeMillis();
        }

        rpsCounter.incrementAndGet();

        g.setColor(scene.bgColor.get());
        g.setBackground(scene.bgColor.get());
        g.fillRect(0, 0, width, height);

        fillLayers(scene);

        double vpWidth = viewportWidth.get();
        double vpHeight = viewportHeight.get();
        double ws = width / vpWidth;
        double hs = height / vpHeight;
        double size = ws > hs ? hs : ws;

        int drawCounter = 0;

        for (Double l : layers.keySet()) {
            ArrayList<Sprite> list = layers.get(l);

            nextSprite:
            for (Sprite sprite : list) {
                if (!sprite.snapshotGetVisible() || sprite.snapshotGetRemove()) {
                    continue;
                }

                double px, py, pa;

                if (sprite.snapshotGetHud()) {
                    px = py = pa = 0;
                } else {
                    // rotating vector
                    pa = -viewportShiftA.get();

                    double oldX = -viewportShiftX.get();
                    double oldY = -viewportShiftY.get();
                    double rotX = oldX * Math.cos(pa) - oldY * Math.sin(pa);
                    double rotY = oldX * Math.sin(pa) + oldY * Math.cos(pa);

                    px = rotX;
                    py = rotY;
                }

                Sprite parent = sprite.snapshotGetParent();
                while (parent != null) {
                    if (!parent.snapshotGetVisible() || parent.snapshotGetRemove()) {
                        continue nextSprite;
                    }

                    // rotating vector
                    double oldX = parent.snapshotGetX();
                    double oldY = parent.snapshotGetY();
                    double rotX = oldX * Math.cos(pa) - oldY * Math.sin(pa);
                    double rotY = oldX * Math.sin(pa) + oldY * Math.cos(pa);

                    px += rotX;
                    py += rotY;
                    pa += parent.snapshotGetAngle();

                    parent = parent.snapshotGetParent();
                }

                // rotating vector
                double oldX = sprite.snapshotGetX();
                double oldY = sprite.snapshotGetY();
                double rotX = oldX * Math.cos(pa) - oldY * Math.sin(pa);
                double rotY = oldX * Math.sin(pa) + oldY * Math.cos(pa);

                double ix = size * (px + rotX + sprite.snapshotGetDx()) + 0.5d * width;
                double iy = size * (py + rotY + sprite.snapshotGetDy()) + 0.5d * height;
                double iw = size * sprite.snapshotGetWidth();
                double ih = size * sprite.snapshotGetHeight();

                if (iw < 1 || ih < 1 ||
                        ix + iw + ih < 0 || iy + iw + ih < 0 ||
                        ix - iw - ih > width || iy - iw - ih > height) {
                    continue;
                }

                drawCounter++;

                draw(g, size, sprite, pa, ix, iy, iw, ih);
            }
        }

        drawBorders(scene, g, width, height, vpWidth, vpHeight, size);
        drawDebug(g, drawCounter);
    }

    private void drawDebug(Graphics2D g, int drawCounter) {
        if (debug.get()) {
            g.setColor(Color.WHITE);
            g.drawString("FPS: " + spriter.getFps(), 0, 20);
            g.setColor(Color.BLACK);
            g.drawString("FPS: " + spriter.getFps(), 1, 21);
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

    private void drawBorders(Scene scene, Graphics2D g, int width, int height, double vpWidth, double vpHeight, double size) {
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1));
        Color color = scene.borderColor.get();
        g.setColor(color);
        g.setBackground(color);
        double brdx = 0.5d * width - 0.5d * vpWidth * size;
        double brdy = 0.5d * height - 0.5d * vpHeight * size;
        g.fillRect(0, 0, (int) brdx, height);
        g.fillRect((int) (width - brdx), 0, width, height);
        g.fillRect(0, 0, width, (int) (brdy));
        g.fillRect(0, (int) (height - brdy), width, height);
    }

    private void draw(Graphics2D g, double size, Sprite sprite, double pa, double ix, double iy, double iw, double ih) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, antialiasing.get() ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, bilinearInterpolation.get() ? RenderingHints.VALUE_INTERPOLATION_BILINEAR : RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) sprite.snapshotGetAlpha()));

        AffineTransform trans = new AffineTransform();
        trans.translate(ix, iy);

        trans.translate(-sprite.snapshotGetDx() * size, -sprite.snapshotGetDy() * size);
        trans.rotate(pa + sprite.snapshotGetAngle());
        trans.scale(1d * iw / sprite.snapshotGetProto().getFrmW(), 1d * ih / sprite.snapshotGetProto().getFrmH());
        trans.translate(sprite.snapshotGetDx() * size / (1d * iw / sprite.snapshotGetProto().getFrmW()), sprite.snapshotGetDy() * size / (1d * ih / sprite.snapshotGetProto().getFrmH()));

        trans.translate(sprite.snapshotGetWidth() / 2 * size / (1d * iw / sprite.snapshotGetProto().getFrmW()), sprite.snapshotGetHeight() / 2 * size / (1d * ih / sprite.snapshotGetProto().getFrmH()));
        trans.scale(sprite.snapshotGetFlipX() ? -1 : 1, sprite.snapshotGetFlipY() ? -1 : 1);
        trans.translate(-sprite.snapshotGetWidth() / 2 * size / (1d * iw / sprite.snapshotGetProto().getFrmW()), -sprite.snapshotGetHeight() / 2 * size / (1d * ih / sprite.snapshotGetProto().getFrmH()));

        g.drawRenderedImage(sprite.snapshotGetFrame(), trans);
    }

    private void fillLayers(Scene scene) {
        // clear layers
        for (ArrayList<Sprite> list : layers.values()) {
            list.clear();
        }

        // fill layers
        synchronized (scene.sprites) {
            for (Iterator<Sprite> iterator = scene.sprites.iterator(); iterator.hasNext(); ) {
                Sprite sprite = iterator.next();
                if (sprite.snapshotGetRemove()) {
                    // TODO garbage collection
                    //iterator.remove();
                } else {
                    Double l = sprite.snapshotGetZ();
                    ArrayList<Sprite> list = layers.get(l);
                    if (list == null) {
                        list = new ArrayList<>();
                        layers.put(l, list);
                    }
                    list.add(sprite);
                }
            }
        }
    }
}
