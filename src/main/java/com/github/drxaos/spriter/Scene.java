package com.github.drxaos.spriter;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class Scene implements IScene {

    private final NodeProto NODE_PROTO = new NodeProto(this);

    private Spriter spriter;

    final ArrayList<Proto> protos = new ArrayList<>();
    final ArrayList<Sprite> sprites = new ArrayList<>();
    final ArrayList<Sprite> removed = new ArrayList<>();
    Color bgColor = Color.WHITE;
    Color borderColor = Color.BLACK;

    private double
            viewportWidth = 2d,
            viewportHeight = 2d,
            viewportShiftX = 0d,
            viewportShiftY = 0d,
            viewportShiftA = 0d;

    private GarbageCollector gc = new GarbageCollector();

    /**
     * Set new viewport width.
     * <br/>
     * Default is 2.0
     */
    @Override
    public void setViewportWidth(double viewportWidth) {
        this.viewportWidth = viewportWidth;
    }

    /**
     * Set new viewport height.
     * <br/>
     * Default is 2.0
     */
    @Override
    public void setViewportHeight(double viewportHeight) {
        this.viewportHeight = viewportHeight;
    }

    /**
     * Shift viewport along X axis.
     * <br/>
     * Default is 0.0
     */
    @Override
    public void setViewportShiftX(double shiftX) {
        this.viewportShiftX = shiftX;
    }

    /**
     * Shift viewport along Y axis.
     * <br/>
     * Default is 0.0
     */
    @Override
    public void setViewportShiftY(double shiftY) {
        this.viewportShiftY = shiftY;
    }

    /**
     * Shift viewport.
     * <br/>
     * Default is 0.0, 0.0
     */
    @Override
    public void setViewportShift(double shiftX, double shiftY) {
        setViewportShiftX(shiftX);
        setViewportShiftY(shiftY);
    }

    /**
     * Rotate viewport.
     * <br/>
     * Default is 0.0
     */
    @Override
    public void setViewportAngle(double angle) {
        this.viewportShiftA = angle;
    }


    @Override
    public Sprite getSpriteByIndex(int index) {
        return sprites.get(index);
    }

    @Override
    public Proto getProtoByIndex(int index) {
        if (index == -1) {
            return NODE_PROTO;
        }
        return protos.get(index);
    }

    @Override
    public void setBackgroundColor(Color color) {
        if (color != null) {
            bgColor = color;
        }
    }

    @Override
    public void setBorderColor(Color color) {
        if (color != null) {
            borderColor = color;
        }
    }

    @Override
    public void snapshot() {
        gc.endFrame();
        synchronized (sprites) {
            for (Sprite sprite : sprites) {
                if (sprite.snapshotGetRemove()) {
                    // skip
                } else if (sprite.isDirty()) {
                    sprite.snapshot();
                }
            }
        }
    }

    @Override
    public void addProto(Proto proto) {
        synchronized (protos) {
            proto.setIndex(protos.size());
            protos.add(proto);
        }
    }

    @Override
    public void addSprite(Sprite sprite) {
        synchronized (sprites) {
            sprite.setIndex(sprites.size());
            sprites.add(sprite);
        }
    }

    @Override
    public Point screenToWorld(int screenX, int screenY, int canvasWidth, int canvasHeight) {
        double vpWidth = viewportWidth;
        double vpHeight = viewportHeight;

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

    @Override
    public Point worldToScreen(int worldX, int worldY, int canvasWidth, int canvasHeight) {
        double vpWidth = viewportWidth;
        double vpHeight = viewportHeight;

        double ws = canvasWidth / vpWidth;
        double hs = canvasHeight / vpHeight;
        double size = ws > hs ? hs : ws;

        double screenX = (canvasWidth / 2) + worldX * size;
        double screenY = (canvasHeight / 2) + worldY * size;

        return new Point(screenX, screenY);
    }

    @Override
    public void setSpriter(Spriter spriter) {
        this.spriter = spriter;
    }

    @Override
    public double getViewportWidth() {
        return viewportWidth;
    }

    @Override
    public double getViewportHeight() {
        return viewportHeight;
    }

    @Override
    public double getViewportShiftX() {
        return viewportShiftX;
    }

    @Override
    public double getViewportShiftY() {
        return viewportShiftY;
    }

    @Override
    public Point getViewportShift() {
        return new Point(getViewportShiftX(), getViewportShiftY());
    }

    @Override
    public Color getBgColor() {
        return bgColor;
    }

    @Override
    public Color getBorderColor() {
        return borderColor;
    }

    @Override
    public double getViewportShiftA() {
        return viewportShiftA;
    }

    @Override
    public Proto createProto(BufferedImage image, double imageCenterX, double imageCenterY) {
        return spriter.createProto(image, imageCenterX, imageCenterY);
    }

    @Override
    public Proto createProto(BufferedImage image, double imageCenterX, double imageCenterY, int frameWidth, int frameHeight) {
        return spriter.createProto(image, imageCenterX, imageCenterY, frameWidth, frameHeight);
    }

    @Override
    public Sprite createSprite(Proto proto, double objectWidth, double objectHeight) {
        return spriter.createSprite(proto, objectWidth, objectHeight);
    }

    @Override
    public Sprite createSprite(Proto proto, double objectWidth) {
        return spriter.createSprite(proto, objectWidth);
    }

    @Override
    public Sprite copySprite(Sprite sprite) {
        return spriter.copySprite(sprite);
    }

    @Override
    public Collection<Sprite> getSprites() {
        return Collections.unmodifiableCollection(sprites);
    }

    public Collection<Proto> getProtos() {
        return Collections.unmodifiableCollection(protos);
    }

    @Override
    public void remove(Sprite sprite) {
        removed.add(sprite);
    }

    @Override
    public NodeProto getNodeProto() {
        return NODE_PROTO;
    }

    /**
     * Auto garbage collection every second.
     */
    public void setAutoGC(boolean autoGC) {
        gc.setAutoGC(autoGC);
    }

    /**
     * Show collected objects count.
     */
    public void setDebugGC(boolean debugGC) {
        gc.setDebugGC(debugGC);
    }

    public class GarbageCollector {

        private long last = 0l;
        private boolean autoGC = true;
        private boolean debugGC = false;

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

        public void endFrame() {
            if (!autoGC) {
                return;
            }
            long now = System.currentTimeMillis();
            if (now - last > 1000) {
                last = now;
                garbageCollect();
            }
        }

        public int garbageCollect() {
            int collected = 0;
            synchronized (Scene.this) {
                ArrayList<Sprite> marked = new ArrayList<>();
                while (removed.size() > 0) {
                    for (Sprite current : removed) {
                        int currentIndex = current.getIndex();

                        if (current.isRemoved()) {
                            Sprite last = sprites.remove(sprites.size() - 1);
                            for (Sprite sprite : sprites) {
                                if (sprite.getParentId() == currentIndex) {
                                    sprite.remove();
                                    marked.add(sprite);
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
                    removed.clear();
                    removed.addAll(marked);
                    marked.clear();
                }
                removed.clear();

                if (debugGC) {
                    System.err.println("Collected: " + collected + " (left: " + sprites.size() + ")");
                }
            }
            System.gc();
            return collected;
        }
    }

}
