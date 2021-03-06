package com.github.drxaos.spriter;

import java.awt.image.BufferedImage;

/**
 * Sprite prototype
 */
public class Proto {

    private IScene scene;
    private int index = -1;

    private BufferedImage img;
    private int imgW, imgH;
    private int frmW, frmH;
    private double imgCX, imgCY;

    private final transient BufferedImage[][] scaledImg;

    Proto(Spriter spriter, IScene scene, BufferedImage image, double imageCenterX, double imageCenterY, int frameWidth, int frameHeight) {
        this.scene = scene;

        if (image != null) {
            this.img = image;
            this.imgW = img.getWidth();
            this.imgH = img.getHeight();
        } else {
            this.img = null;
            this.imgW = 1;
            this.imgH = 1;
        }
        this.imgCX = imageCenterX;
        this.imgCY = imageCenterY;

        if (frameWidth < 0) {
            frameWidth = imgW;
        }
        if (frameHeight < 0) {
            frameHeight = imgH;
        }

        this.frmW = frameWidth;
        this.frmH = frameHeight;

        scaledImg = new BufferedImage[imgW / frmW + 1][imgH / frmH + 1];
    }

    BufferedImage getFrame(int frameX, int frameY) {
        synchronized (scaledImg) {
            BufferedImage scaledFrame = scaledImg[frameX][frameY];
            if (scaledFrame == null) {
                if (imgW == frmW && imgH == frmH) {
                    scaledFrame = img;
                } else {
                    scaledFrame = img.getSubimage(frameX * frmW, frameY * frmH, frmW, frmH);
                }
                scaledImg[frameX][frameY] = scaledFrame;
            }
            return scaledFrame;
        }
    }

    public IScene getScene() {
        return scene;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public BufferedImage getImage() {
        return img;
    }

    public int getImageWidth() {
        return imgW;
    }

    public int getImageHeight() {
        return imgH;
    }

    public int getFrameWidth() {
        return frmW;
    }

    public int getFrameHeight() {
        return frmH;
    }

    public double getAnchorX() {
        return imgCX;
    }

    public double getAnchorY() {
        return imgCY;
    }


    public Sprite newInstance(double objectWidth) {
        return scene.createSprite(this, objectWidth);
    }

    public Sprite newInstance(double objectWidth, double objectHeight) {
        return scene.createSprite(this, objectWidth, objectHeight);
    }
}
