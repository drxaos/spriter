package com.github.drxaos.spriter;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;

/**
 * Sprite
 */
public class Sprite {

    public final static int PROTO = 0;
    public final static int X = 1;
    public final static int Y = 2;
    public final static int ANGLE = 3;
    public final static int WIDTH = 4;
    public final static int HEIGHT = 5;
    public final static int DX = 6;
    public final static int DY = 7;
    public final static int Z = 8;
    public final static int FRAME_X = 9;
    public final static int FRAME_Y = 10;
    public final static int FLAGS = 11;
    public final static int FLAGS_DIRTY = 0b1;
    public final static int FLAGS_VISIBLE = 0b10;
    public final static int FLAGS_REMOVE = 0b100;
    public final static int FLAGS_HUD = 0b1000;
    public final static int FLAGS_FLIP_X = 0b10000;
    public final static int FLAGS_FLIP_Y = 0b100000;
    public final static int ALPHA = 12;
    public final static int PARENT = 13;

    int index;
    long[] active = new long[16];
    long[] snapshot = new long[16];

    private transient Spriter spriter;
    private transient Scene scene;

    private Proto snapshotCachedProto;

    public Sprite(Spriter spriter, Scene scene, Proto proto, double objectWidth, double objectHeight) {
        this.spriter = spriter;
        this.scene = scene;
        setProto(proto);

        if (objectHeight < 0) {
            objectHeight = objectWidth * proto.getFrmH() / proto.getFrmW();
        }

        setX(0d);
        setY(0d);
        setAngle(0d);
        setWidth(objectWidth);
        setHeight(objectHeight);
        setDx(-(objectWidth * proto.getImgCX() / proto.getFrmW()));
        setDy(-(objectHeight * proto.getImgCY() / proto.getFrmH()));
        setZ(0d);
        setFrame(0);
        setFrameRow(0);
        setVisible(true);
        setParent(null);
        unsetRemove();
        setHud(false);
        setFlipX(false);
        setFlipY(false);
        setAlpha(1d);
        dirty();
    }

    public Sprite(Sprite sprite) {
        this.spriter = sprite.spriter;
        this.scene = sprite.scene;
        System.arraycopy(sprite.active, 0, active, 0, active.length);
        System.arraycopy(sprite.snapshot, 0, snapshot, 0, snapshot.length);
    }

    int getIndex() {
        return index;
    }

    void setIndex(int index) {
        dirty();
        this.index = index;
    }

    void unsetRemove() {
        dirty();
        active[FLAGS] &= ~FLAGS_REMOVE;
    }

    void unsetDirty() {
        active[FLAGS] &= ~FLAGS_DIRTY;
    }

    void dirty() {
        active[FLAGS] |= FLAGS_DIRTY;
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
        dirty();
        active[ANGLE] = Double.doubleToRawLongBits(a);
        return this;
    }

    /**
     * Set new width and proportional height to sprite.
     */
    public Sprite setWidthProportional(double w) {
        dirty();
        active[WIDTH] = Double.doubleToRawLongBits(w);
        active[DX] = Double.doubleToRawLongBits(-(w * getProto().getImgCX() / getProto().getFrmW()));
        setHeight(w * getProto().getFrmH() / getProto().getFrmW());
        return this;
    }

    /**
     * Set new height and proportional width to sprite.
     */
    public Sprite setHeightProportional(double h) {
        dirty();
        active[HEIGHT] = Double.doubleToRawLongBits(h);
        active[DY] = Double.doubleToRawLongBits(-(h * getProto().getImgCY() / getProto().getFrmH()));
        setWidth(h * getProto().getFrmW() / getProto().getFrmH());
        return this;
    }

    /**
     * Get current object width.
     */
    public double getWidth() {
        return Double.longBitsToDouble(active[WIDTH]);
    }

    /**
     * Set new width of sprite.
     */
    public Sprite setWidth(double w) {
        dirty();
        active[WIDTH] = Double.doubleToRawLongBits(w);
        active[DX] = Double.doubleToRawLongBits(-(w * getProto().getImgCX() / getProto().getFrmW()));
        return this;
    }

    /**
     * Get current object height.
     */
    public double getHeight() {
        return Double.longBitsToDouble(active[HEIGHT]);
    }

    /**
     * Set new height of sprite.
     */
    public Sprite setHeight(double h) {
        dirty();
        active[HEIGHT] = Double.doubleToRawLongBits(h);
        active[DY] = Double.doubleToRawLongBits(-(h * getProto().getImgCY() / getProto().getFrmH()));
        return this;
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
        return getProto().getFrame(getFrameX(), getFrameY());
    }

    /**
     * Set current frame of animated sprite.
     */
    public Sprite setFrame(int n) {
        dirty();
        active[FRAME_X] = n;
        return this;
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
     * Set current frame row of animated sprite.
     */
    public Sprite setFrameRow(int row) {
        dirty();
        active[FRAME_Y] = row;
        return this;
    }

    /**
     * Create new instance of sprite. Image data will be shared between all instances.
     */
    public Sprite newInstance() {
        return spriter.copySprite(this);
    }

    /**
     * Remove sprite from scene.
     */
    public void remove() {
        dirty();
        active[FLAGS] |= FLAGS_REMOVE;
        setParent(null);
    }

    double getX() {
        return Double.longBitsToDouble(active[X]);
    }

    /**
     * Set X coordinate of sprite center.
     * <br/>
     * Default is 0.0
     */
    public Sprite setX(double x) {
        dirty();
        active[X] = Double.doubleToRawLongBits(x);
        return this;
    }

    double getY() {
        return Double.longBitsToDouble(active[Y]);
    }

    /**
     * Set Y coordinate of sprite center.
     * <br/>
     * Default is 0.0
     */
    public Sprite setY(double y) {
        dirty();
        active[Y] = Double.doubleToRawLongBits(y);
        return this;
    }

    double getAngle() {
        return Double.longBitsToDouble(active[ANGLE]);
    }

    double getDx() {
        return Double.longBitsToDouble(active[DX]);
    }

    void setDx(double dx) {
        dirty();
        active[DX] = Double.doubleToRawLongBits(dx);
    }

    double getDy() {
        return Double.longBitsToDouble(active[DY]);
    }

    void setDy(double dy) {
        dirty();
        active[DY] = Double.doubleToRawLongBits(dy);
    }

    double getZ() {
        return Double.longBitsToDouble(active[Z]);
    }

    /**
     * Set sprite z position
     * <br/>
     * Default is 0
     */
    public Sprite setZ(double z) {
        dirty();
        active[Z] = Double.doubleToRawLongBits(z);
        return this;
    }

    int getFrameX() {
        return (int) active[FRAME_X];
    }

    int getFrameY() {
        return (int) active[FRAME_Y];
    }


    boolean getVisible() {
        return (active[FLAGS] & FLAGS_VISIBLE) != 0;
    }

    /**
     * Set visibility of sprite.
     * <br/>
     * Default is true
     */
    public Sprite setVisible(boolean visible) {
        dirty();
        if (visible) {
            active[FLAGS] |= FLAGS_VISIBLE;
        } else {
            active[FLAGS] &= ~FLAGS_VISIBLE;
        }
        return this;
    }

    boolean isRemoved() {
        return (active[FLAGS] & FLAGS_REMOVE) != 0;
    }

    boolean getHud() {
        return (active[FLAGS] & FLAGS_HUD) != 0;
    }

    /**
     * Make sprite to move with viewport.
     */
    public Sprite setHud(boolean hud) {
        dirty();
        if (hud) {
            active[FLAGS] |= FLAGS_HUD;
        } else {
            active[FLAGS] &= ~FLAGS_HUD;
        }
        return this;
    }

    boolean getFlipX() {
        return (active[FLAGS] & FLAGS_FLIP_X) != 0;
    }

    /**
     * Make sprite flipped right to left.
     * <br/>
     * Default is false
     */
    public Sprite setFlipX(boolean flipx) {
        dirty();
        if (flipx) {
            active[FLAGS] |= FLAGS_FLIP_X;
        } else {
            active[FLAGS] &= ~FLAGS_FLIP_X;
        }
        return this;
    }

    boolean getFlipY() {
        return (active[FLAGS] & FLAGS_FLIP_Y) != 0;
    }

    /**
     * Make sprite flipped top to bottom.
     * <br/>
     * Default is false
     */
    public Sprite setFlipY(boolean flipy) {
        dirty();
        if (flipy) {
            active[FLAGS] |= FLAGS_FLIP_Y;
        } else {
            active[FLAGS] &= ~FLAGS_FLIP_Y;
        }
        return this;
    }

    boolean isDirty() {
        return (active[FLAGS] & FLAGS_DIRTY) != 0;
    }

    double getAlpha() {
        return Double.longBitsToDouble(active[ALPHA]);
    }

    /**
     * Set opacity of sprite from 0 to 1
     * <br/>
     * Default is 1
     */
    public Sprite setAlpha(double alpha) {
        dirty();
        active[ALPHA] = Double.doubleToRawLongBits(alpha);
        return this;
    }

    Sprite getParent() {
        if (active[PARENT] < 0) {
            return null;
        }
        return scene.getSpriteByIndex((int) active[PARENT]);
    }

    int getParentId() {
        return (int) active[PARENT];
    }

    public void setParentId(int id) {
        dirty();
        active[PARENT] = id;
    }

    /**
     * Set parent for this sprite.
     * Parent adds to this sprite it's coordinates and angle.
     */
    public Sprite setParent(Sprite parent) {
        dirty();
        if (parent == null) {
            active[PARENT] = -1;
        } else {
            active[PARENT] = parent.getIndex();
        }
        return this;
    }

    public Proto getProto() {
        return scene.getProtoByIndex((int) active[PROTO]);
    }

    void setProto(Proto proto) {
        dirty();
        active[PROTO] = proto.getIndex();
    }

    void snapshot() {
        unsetDirty();
        snapshotCachedProto = null;
        System.arraycopy(active, 0, snapshot, 0, active.length);
    }

    double snapshotGetWidth() {
        return Double.longBitsToDouble(snapshot[WIDTH]);
    }

    double snapshotGetHeight() {
        return Double.longBitsToDouble(snapshot[HEIGHT]);
    }

    double snapshotGetX() {
        return Double.longBitsToDouble(snapshot[X]);
    }

    double snapshotGetY() {
        return Double.longBitsToDouble(snapshot[Y]);
    }

    double snapshotGetAngle() {
        return Double.longBitsToDouble(snapshot[ANGLE]);
    }

    double snapshotGetDx() {
        return Double.longBitsToDouble(snapshot[DX]);
    }

    double snapshotGetDy() {
        return Double.longBitsToDouble(snapshot[DY]);
    }

    double snapshotGetZ() {
        return Double.longBitsToDouble(snapshot[Z]);
    }

    int snapshotGetFrameX() {
        return (int) snapshot[FRAME_X];
    }

    int snapshotGetFrameY() {
        return (int) snapshot[FRAME_Y];
    }

    boolean snapshotGetVisible() {
        return (snapshot[FLAGS] & FLAGS_VISIBLE) != 0;
    }

    boolean snapshotGetRemove() {
        return (snapshot[FLAGS] & FLAGS_REMOVE) != 0;
    }

    boolean snapshotGetHud() {
        return (snapshot[FLAGS] & FLAGS_HUD) != 0;
    }

    boolean snapshotGetFlipX() {
        return (snapshot[FLAGS] & FLAGS_FLIP_X) != 0;
    }

    boolean snapshotGetFlipY() {
        return (snapshot[FLAGS] & FLAGS_FLIP_Y) != 0;
    }

    double snapshotGetAlpha() {
        return Double.longBitsToDouble(snapshot[ALPHA]);
    }

    Sprite snapshotGetParent() {
        if (snapshot[PARENT] < 0) {
            return null;
        }
        return scene.getSpriteByIndex((int) snapshot[PARENT]);
    }

    Proto snapshotGetProto() {
        if (snapshotCachedProto != null) {
            return snapshotCachedProto;
        }
        return snapshotCachedProto = scene.getProtoByIndex((int) snapshot[PROTO]);
    }

    RenderedImage snapshotGetFrame() {
        return snapshotGetProto().getFrame(snapshotGetFrameX(), snapshotGetFrameY());
    }
}
