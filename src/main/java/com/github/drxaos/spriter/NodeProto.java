package com.github.drxaos.spriter;

import java.awt.image.BufferedImage;

/**
 * Node prototype
 */
public class NodeProto extends Proto {

    public NodeProto(IScene scene) {
        super(null, scene, null, 0, 0, 1, 1);
    }

    BufferedImage getFrame(int frameX, int frameY) {
        return null;
    }

    public BufferedImage getImage() {
        return null;
    }

    @Override
    public int getIndex() {
        return -1;
    }

    @Override
    public void setIndex(int index) {
    }

    @Override
    public int getImageWidth() {
        return 1;
    }

    @Override
    public int getImageHeight() {
        return 1;
    }

    @Override
    public int getFrameWidth() {
        return 1;
    }

    @Override
    public int getFrameHeight() {
        return 1;
    }

    @Override
    public double getAnchorX() {
        return 0;
    }

    @Override
    public double getAnchorY() {
        return 0;
    }

    @Override
    public Sprite newInstance(double objectWidth) {
        return null;
    }

    @Override
    public Sprite newInstance(double objectWidth, double objectHeight) {
        return null;
    }
}
