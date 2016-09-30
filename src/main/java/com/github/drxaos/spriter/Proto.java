package com.github.drxaos.spriter;

import java.awt.image.BufferedImage;

/**
 * Sprite prototype
 */
public class Proto {
    private int index;

    private transient Spriter spriter;

    private BufferedImage img;
    private int imgW, imgH;
    private int frmW, frmH;
    private double imgCX, imgCY;

    private final transient BufferedImage[][] scaledImg;

    Proto(Spriter spriter, BufferedImage image, double imageCenterX, double imageCenterY, int frameWidth, int frameHeight) {
        this.spriter = spriter;

        this.img = image;
        this.imgW = img.getWidth();
        this.imgH = img.getHeight();
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

    int getIndex() {
        return index;
    }

    void setIndex(int index) {
        this.index = index;
    }

    BufferedImage getImg() {
        return img;
    }

    int getImgW() {
        return imgW;
    }

    int getImgH() {
        return imgH;
    }

    int getFrmW() {
        return frmW;
    }

    int getFrmH() {
        return frmH;
    }

    double getImgCX() {
        return imgCX;
    }

    double getImgCY() {
        return imgCY;
    }


    public Sprite newInstance(double objectWidth) {
        return spriter.createSprite(this, objectWidth);
    }

    public Sprite newInstance(double objectWidth, double objectHeight) {
        return spriter.createSprite(this, objectWidth, objectHeight);
    }
}
