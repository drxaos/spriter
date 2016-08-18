package com.github.drxaos.spriter;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class Sprite {

    AtomicReference<Double> x, y, a, w, h, dx, dy;

    BufferedImage img;
    BufferedImage scaledImg;
    int imgW, imgH;
    double imgCX, imgCY;
    AtomicInteger layer;
    AtomicBoolean visible;

    public Sprite(BufferedImage image, double imageCenterX, double imageCenterY, double objectWidth, double objectHeight, int layer) {
        this(image, imageCenterX, imageCenterY, objectWidth, objectHeight);
        this.layer.set(layer);
    }

    public Sprite(BufferedImage image, double imageCenterX, double imageCenterY, double objectWidth, double objectHeight) {
        this.img = image;
        this.imgW = img.getWidth();
        this.imgH = img.getHeight();
        this.imgCX = imageCenterX;
        this.imgCY = imageCenterY;

        this.x = new AtomicReference<Double>(0d);
        this.y = new AtomicReference<Double>(0d);
        this.a = new AtomicReference<Double>(0d);
        this.w = new AtomicReference<Double>(0d);
        this.h = new AtomicReference<Double>(0d);
        this.dx = new AtomicReference<Double>(0d);
        this.dy = new AtomicReference<Double>(0d);
        this.layer = new AtomicInteger(1);
        this.visible = new AtomicBoolean(true);

        this.w.set(objectWidth);
        this.h.set(objectHeight);
        this.dx.set(-(objectWidth * imageCenterX / imgW));
        this.dy.set(-(objectHeight * imageCenterY / imgH));
    }

    public void setLayer(int layer) {
        this.layer.set(layer);
    }

    public void setVisible(boolean visible) {
        this.visible.set(visible);
    }

    public void setX(double x) {
        this.x.set(x);
    }

    public void setY(double y) {
        this.y.set(y);
    }

    public void setAngle(double a) {
        this.a.set(a);
    }

    public void setWidth(double w) {
        this.w.set(w);
        this.dx.set(-(w * imgCX / imgW));
    }

    public void setHeight(double h) {
        this.h.set(h);
        this.dy.set(-(h * imgCY / imgH));
    }

    public BufferedImage getScaled(int targetWidth, int targetHeight) {
        if (scaledImg == null || targetWidth != scaledImg.getWidth() || targetHeight != scaledImg.getHeight()) {
            scaledImg = getScaledInstance(img, targetWidth, targetHeight, RenderingHints.VALUE_INTERPOLATION_BILINEAR, true);
        }
        return scaledImg;
    }

    BufferedImage getScaledInstance(BufferedImage img,
                                    int targetWidth,
                                    int targetHeight,
                                    Object hint,
                                    boolean higherQuality) {
        int type = (img.getTransparency() == Transparency.OPAQUE) ?
                BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
        BufferedImage ret = (BufferedImage) img;
        int w, h;
        if (higherQuality) {
            // Use multi-step technique: start with original size, then
            // scale down in multiple passes with drawImage()
            // until the target size is reached
            w = img.getWidth();
            h = img.getHeight();
        } else {
            // Use one-step technique: scale directly from original
            // size to target size with a single drawImage() call
            w = targetWidth;
            h = targetHeight;
        }

        do {
            if (higherQuality && w > targetWidth) {
                w /= 2;
                if (w < targetWidth) {
                    w = targetWidth;
                }
            }

            if (higherQuality && h > targetHeight) {
                h /= 2;
                if (h < targetHeight) {
                    h = targetHeight;
                }
            }

            BufferedImage tmp = new BufferedImage(w, h, type);
            Graphics2D g2 = tmp.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);
            g2.drawImage(ret, 0, 0, w, h, null);
            g2.dispose();

            ret = tmp;
        } while (w != targetWidth || h != targetHeight);

        return ret;
    }
}
