package com.github.drxaos.spriter;

import java.util.LinkedHashSet;
import java.util.function.Consumer;

public class Group {

    private final LinkedHashSet<Sprite> elements = new LinkedHashSet<>();

    public Group(Sprite... elements) {
        for (Sprite sprite : elements) {
            this.elements.add(sprite);
        }
    }

    public LinkedHashSet<Sprite> elements() {
        return elements;
    }

    public void eachSprite(Consumer<Sprite> action) {
        elements.forEach(action);
    }


    /**
     * Set coordinates of sprite center.
     * <br/>
     * Default is 0.0, 0.0
     */
    public Group setPos(double x, double y) {
        setX(x);
        setY(y);
        return this;
    }

    /**
     * Set coordinates of sprite center.
     * <br/>
     * Default is 0.0, 0.0
     */
    public Group setPos(Point point) {
        setX(point.getX());
        setY(point.getY());
        return this;
    }

    /**
     * Set angle of sprite.
     * <br/>
     * Default is 0.0
     */
    public Group setAngle(double a) {
        eachSprite((s) -> s.setAngle(a));
        return this;
    }

    /**
     * Set new width and proportional height to sprite.
     */
    public Group setWidthProportional(double w) {
        eachSprite((s) -> s.setWidthProportional(w));
        return this;
    }

    /**
     * Set new height and proportional width to sprite.
     */
    public Group setHeightProportional(double h) {
        eachSprite((s) -> s.setHeightProportional(h));
        return this;
    }

    /**
     * Set new width of sprite.
     */
    public Group setWidth(double w) {
        eachSprite((s) -> s.setWidth(w));
        return this;
    }

    /**
     * Set new height of sprite.
     */
    public Group setHeight(double h) {
        eachSprite((s) -> s.setHeight(h));
        return this;
    }

    /**
     * Set new width and height of sprite.
     */
    public Group setSquareSide(double wh) {
        setWidth(wh);
        setHeight(wh);
        return this;
    }

    /**
     * Set current frame of animated sprite.
     */
    public Group setFrame(int n) {
        eachSprite((s) -> s.setFrame(n));
        return this;
    }

    /**
     * Set current frame of animated sprite.
     */
    public Group setFrame(int n, int row) {
        setFrame(n);
        setFrameRow(row);
        return this;
    }

    /**
     * Set current frame row of animated sprite.
     */
    public Group setFrameRow(int row) {
        eachSprite((s) -> s.setFrameRow(row));
        return this;
    }

    /**
     * Remove sprite from scene.
     */
    public void remove() {
        eachSprite(Sprite::remove);
        setParent(null);
    }

    /**
     * Set X coordinate of sprite center.
     * <br/>
     * Default is 0.0
     */
    public Group setX(double x) {
        eachSprite((s) -> s.setX(x));
        return this;
    }

    /**
     * Set Y coordinate of sprite center.
     * <br/>
     * Default is 0.0
     */
    public Group setY(double y) {
        eachSprite((s) -> s.setY(y));
        return this;
    }

    /**
     * Set sprite z position
     * <br/>
     * Default is 0
     */
    public Group setZ(double z) {
        eachSprite((s) -> s.setZ(z));
        return this;
    }

    /**
     * Set visibility of sprite.
     * <br/>
     * Default is true
     */
    public Group setVisible(boolean visible) {
        eachSprite((s) -> s.setVisible(visible));
        return this;
    }

    /**
     * Make sprite to move with viewport.
     */
    public Group setHud(boolean hud) {
        eachSprite((s) -> s.setHud(hud));
        return this;
    }

    /**
     * Make sprite flipped right to left.
     * <br/>
     * Default is false
     */
    public Group setFlipX(boolean flipx) {
        eachSprite((s) -> s.setFlipX(flipx));
        return this;
    }

    /**
     * Make sprite flipped top to bottom.
     * <br/>
     * Default is false
     */
    public Group setFlipY(boolean flipy) {
        eachSprite((s) -> s.setFlipY(flipy));
        return this;
    }

    /**
     * Set opacity of sprite from 0 to 1
     * <br/>
     * Default is 1
     */
    public Group setAlpha(double alpha) {
        eachSprite((s) -> s.setAlpha(alpha));
        return this;
    }

    /**
     * Set parent for this sprite.
     * Parent adds to this sprite it's coordinates and angle.
     */
    public Group setParent(Sprite parent) {
        eachSprite((s) -> s.setParent(parent));
        return this;
    }

}
