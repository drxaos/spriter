package com.github.drxaos.spriter;

import java.awt.*;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

public class Scene {

    final ArrayList<Proto> protos = new ArrayList<>();
    final ArrayList<Sprite> sprites = new ArrayList<>();
    AtomicReference<Color> bgColor = new AtomicReference<>(Color.WHITE);
    AtomicReference<Color> borderColor = new AtomicReference<>(Color.BLACK);

    private AtomicReference<Double>
            viewportWidth = new AtomicReference<>(2d),
            viewportHeight = new AtomicReference<>(2d),
            viewportShiftX = new AtomicReference<>(0d),
            viewportShiftY = new AtomicReference<>(0d),
            viewportShiftA = new AtomicReference<>(0d);


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


    public Sprite getSpriteByIndex(int index) {
        return sprites.get(index);
    }

    public Proto getProtoByIndex(int index) {
        return protos.get(index);
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

    public void snapshot() {
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

    public void addProto(Proto proto) {
        synchronized (protos) {
            proto.setIndex(protos.size());
            protos.add(proto);
        }
    }

    public void addSprite(Sprite sprite) {
        synchronized (sprites) {
            sprite.setIndex(sprites.size());
            sprites.add(sprite);
        }
    }
}
