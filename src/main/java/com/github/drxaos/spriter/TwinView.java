package com.github.drxaos.spriter;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class TwinView implements IRenderer {

    Image background;
    Graphics2D backgroundGraphics;

    Spriter spriter;
    IRenderer rendererLeft, rendererRight;
    Image left, right;
    Graphics2D graphicsLeft, graphicsRight;
    Viewport vpLeft, vpRight;
    ExecutorService executor = Executors.newFixedThreadPool(1);
    double vpLeftX, vpLeftY, vpRightX, vpRightY;

    public class Viewport implements IScene {
        IScene scene;

        private double
                viewportShiftX = 0d,
                viewportShiftY = 0d,
                viewportShiftA = 0d;

        public Viewport(IScene scene) {
            this.scene = scene;
        }

        @Override
        public void setViewportWidth(double viewportWidth) {
            scene.setViewportWidth(viewportWidth);
        }

        @Override
        public void setViewportHeight(double viewportHeight) {
            scene.setViewportHeight(viewportHeight);
        }

        @Override
        public void setViewportShiftX(double shiftX) {
            viewportShiftX = shiftX;
        }

        @Override
        public void setViewportShiftY(double shiftY) {
            viewportShiftY = shiftY;
        }

        @Override
        public void setViewportShift(double shiftX, double shiftY) {
            setViewportShiftX(shiftX);
            setViewportShiftY(shiftY);
        }

        @Override
        public void setViewportAngle(double angle) {
            viewportShiftA = angle;
        }

        @Override
        public Sprite getSpriteByIndex(int index) {
            return scene.getSpriteByIndex(index);
        }

        @Override
        public Proto getProtoByIndex(int index) {
            return scene.getProtoByIndex(index);
        }

        @Override
        public void setBackgroundColor(Color color) {
            scene.setBackgroundColor(color);
        }

        @Override
        public void setBorderColor(Color color) {
            scene.setBorderColor(color);
        }

        @Override
        public void snapshot() {
            scene.snapshot();
        }

        @Override
        public void addProto(Proto proto) {
            scene.addProto(proto);
        }

        @Override
        public void addSprite(Sprite sprite) {
            scene.addSprite(sprite);
        }

        @Override
        public Point screenToWorld(int screenX, int screenY, int canvasWidth, int canvasHeight) {
            return scene.screenToWorld(screenX, screenY, canvasWidth, canvasHeight);
        }

        @Override
        public Point worldToScreen(int worldX, int worldY, int canvasWidth, int canvasHeight) {
            return scene.worldToScreen(worldX, worldY, canvasWidth, canvasHeight);
        }

        @Override
        public void setSpriter(Spriter spriter) {
            scene.setSpriter(spriter);
        }

        @Override
        public double getViewportWidth() {
            return scene.getViewportWidth();
        }

        @Override
        public double getViewportHeight() {
            return scene.getViewportHeight();
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
        public Color getBgColor() {
            return scene.getBgColor();
        }

        @Override
        public Color getBorderColor() {
            return scene.getBorderColor();
        }

        @Override
        public double getViewportShiftA() {
            return viewportShiftA;
        }

        @Override
        public Proto createProto(BufferedImage image, double imageCenterX, double imageCenterY) {
            return scene.createProto(image, imageCenterX, imageCenterY);
        }

        @Override
        public Proto createProto(BufferedImage image, double imageCenterX, double imageCenterY, int frameWidth, int frameHeight) {
            return scene.createProto(image, imageCenterX, imageCenterY, frameWidth, frameHeight);
        }

        @Override
        public Sprite createSprite(Proto proto, double objectWidth, double objectHeight) {
            return scene.createSprite(proto, objectWidth, objectHeight);
        }

        @Override
        public Sprite createSprite(Proto proto, double objectWidth) {
            return scene.createSprite(proto, objectWidth);
        }

        @Override
        public Sprite copySprite(Sprite sprite) {
            return scene.copySprite(sprite);
        }

        @Override
        public Collection<Sprite> getSprites() {
            return scene.getSprites();
        }

        @Override
        public Collection<Proto> getProtos() {
            return scene.getProtos();
        }

        @Override
        public void remove(Sprite sprite) {
            scene.remove(sprite);
        }
    }

    public TwinView(Spriter spriter) {
        this.spriter = spriter;
        this.rendererLeft = new Renderer();
        this.rendererLeft.setSpriter(spriter);
        this.rendererRight = new Renderer();
        this.rendererRight.setSpriter(spriter);
    }

    public void render(IScene scene, Graphics2D g, int width, int height) {
        int w = width / 2 - 10;
        int h = height - 10;

        if (left == null || left.getWidth(null) != w || left.getHeight(null) != h) {
            left = spriter.getRenderer().makeOutputImage(w, h, true);
            graphicsLeft = (Graphics2D) left.getGraphics();
            vpLeft = new Viewport(scene);
        }
        if (right == null || right.getWidth(null) != w || right.getHeight(null) != h) {
            right = spriter.getRenderer().makeOutputImage(w, h, true);
            graphicsRight = (Graphics2D) right.getGraphics();
            vpRight = new Viewport(scene);
        }

        Future<Image> leftFuture = executor.submit(() -> {
            vpLeft.setViewportShift(vpLeftX, vpLeftY);
            return rendererLeft.render(vpLeft, left, graphicsLeft);
        });

        Future<Image> rightFuture = executor.submit(() -> {
            vpRight.setViewportShift(vpRightX, vpRightY);
            return rendererRight.render(vpRight, right, graphicsRight);
        });

        g.setColor(Color.BLACK);
        g.setBackground(Color.BLACK);
        g.fillRect(0, 0, width, height);

        try {
            g.drawImage(leftFuture.get(), 5, 5, null);
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            System.exit(0);
        }

        try {
            g.drawImage(rightFuture.get(), w + 15, 5, null);
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            System.exit(0);
        }
    }

    public void setVpLeft(double vpLeftX, double vpLeftY) {
        this.vpLeftX = vpLeftX;
        this.vpLeftY = vpLeftY;
    }

    public void setVpRight(double vpRightX, double vpRightY) {
        this.vpRightX = vpRightX;
        this.vpRightY = vpRightY;
    }

    @Override
    public void setAntialiasing(boolean antialiasing) {
        rendererLeft.setAntialiasing(antialiasing);
        rendererRight.setAntialiasing(antialiasing);
    }

    @Override
    public void setBilinearInterpolation(boolean bilinearInterpolation) {
        rendererLeft.setBilinearInterpolation(bilinearInterpolation);
        rendererRight.setBilinearInterpolation(bilinearInterpolation);
    }

    @Override
    public Image render(IScene scene, Image dst, Graphics2D dstGraphics) {
        background = dst;
        backgroundGraphics = dstGraphics;
        render(scene, backgroundGraphics, background.getWidth(null), background.getHeight(null));
        return background;
    }

    @Override
    public void setDebug(boolean debug) {
        rendererLeft.setDebug(debug);
        rendererRight.setDebug(debug);
    }

    @Override
    public void setSpriter(Spriter spriter) {
        rendererLeft.setSpriter(spriter);
        rendererRight.setSpriter(spriter);
    }

    @Override
    public Image makeOutputImage(int w, int h, boolean alpha) {
        return rendererLeft.makeOutputImage(w, h, alpha);
    }
}