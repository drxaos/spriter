package com.github.drxaos.spriter;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Spriter {

    private IOutput output;
    private Object renderLock = new Object();

    private IRenderer renderer;
    private IFps fps;

    private IScene scene;

    private boolean shutdown = false;

    private Spriter() {
    }

    public void setOutput(IOutput output) {
        if (this.output != null) {
            this.output.setSpriter(null);
        }
        this.output = output;
        output.setSpriter(this);
    }

    public void setRenderer(IRenderer renderer) {
        if (this.renderer != null) {
            this.renderer.setSpriter(null);
        }
        this.renderer = renderer;
        renderer.setSpriter(this);
    }

    public void setFps(IFps fps) {
        if (this.fps != null) {
            this.fps.setSpriter(null);
        }
        this.fps = fps;
        fps.setSpriter(this);
    }

    public void setScene(IScene scene) {
        if (this.scene != null) {
            this.scene.setSpriter(null);
        }
        this.scene = scene;
        scene.setSpriter(this);
    }

    public IOutput getOutput() {
        return output;
    }

    public IFps getFps() {
        return fps;
    }

    public IScene getScene() {
        return scene;
    }

    public static Spriter createDefault(String title) {
        Spriter spriter = createDefault();
        spriter.setTitle(title);
        return spriter;
    }

    public static Spriter createDefault() {
        Spriter spriter = new Spriter();
        IOutput output = new Output();
        IRenderer renderer = new Renderer();
        IFps fps = new Fps();
        IScene scene = new Scene();

        spriter.setOutput(output);
        spriter.setRenderer(renderer);
        spriter.setFps(fps);
        spriter.setScene(scene);

        spriter.new Loop().start();

        return spriter;
    }

    public static Spriter createCustom(
            IOutput output,
            IRenderer renderer,
            IFps fps,
            IScene scene
    ) {
        Spriter spriter = new Spriter();

        spriter.setOutput(output);
        spriter.setRenderer(renderer);
        spriter.setFps(fps);
        spriter.setScene(scene);

        spriter.new Loop().start();

        return spriter;
    }

    /**
     * Get control instance for this Spriter window.
     */
    public synchronized Control getControl() {
        return output.getControl();
    }

    /**
     * Show default system cursor inside canvas.
     * <br/>
     * Default is false.
     */
    public void setShowCursor(boolean show) {
        output.setShowCursor(show);
    }

    /**
     * Show debug info
     */
    public void setDebug(boolean debug) {
        renderer.setDebug(debug);
    }

    /**
     * Set target FPS. Spriter will sleep in sleepAfterFrame().
     */
    public void setTargetFps(int targetFps) {
        fps.setTargetFps(targetFps);
    }

    /**
     * Begin modification of scene.
     */
    public void beginFrame() {
        fps.beginFrame();
    }

    /**
     * End modification of scene. Spriter will render frame after this call.
     */
    public void endFrame() throws InterruptedException {
        synchronized (renderLock) {
            scene.snapshot();
            renderLock.notifyAll();
        }

        fps.sleepAfterFrame();
    }

    /**
     * Get renderer
     */
    public IRenderer getRenderer() {
        return renderer;
    }

    public void setBackgroundColor(Color color) {
        scene.setBackgroundColor(color);
    }

    public void setBorderColor(Color color) {
        scene.setBorderColor(color);
        output.setDefaultColor(color);
    }

    /**
     * Set new viewport width.
     * <br/>
     * Default is 2.0
     */
    public void setViewportWidth(double viewportWidth) {
        scene.setViewportWidth(viewportWidth);
    }

    /**
     * Set new viewport height.
     * <br/>
     * Default is 2.0
     */
    public void setViewportHeight(double viewportHeight) {
        scene.setViewportHeight(viewportHeight);
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
     * Shift viewport along X axis.
     * <br/>
     * Default is 0.0
     */
    public void setViewportShiftX(double shiftX) {
        scene.setViewportShiftX(shiftX);
    }

    /**
     * Shift viewport along Y axis.
     * <br/>
     * Default is 0.0
     */
    public void setViewportShiftY(double shiftY) {
        scene.setViewportShiftY(shiftY);
    }

    /**
     * Rotate viewport.
     * <br/>
     * Default is 0.0
     */
    public void setViewportAngle(double angle) {
        scene.setViewportAngle(angle);
    }

    /**
     * Create new sprite prototype.
     *
     * @param image        Original image of new sprite.
     * @param imageCenterX Distance from left side to center of image.
     * @param imageCenterY Distance from top side to center of image.
     * @return new sprite prototype
     */
    public Proto createProto(BufferedImage image, double imageCenterX, double imageCenterY) {
        Proto proto = new Proto(this, scene, image, imageCenterX, imageCenterY, -1, -1);
        scene.addProto(proto);
        return proto;
    }

    /**
     * Create new animated sprite prototype.
     *
     * @param image        Original image of new sprite.
     * @param imageCenterX Distance from left side to center of image.
     * @param imageCenterY Distance from top side to center of image.
     * @param frameWidth   Single animation frame wifth.
     * @param frameHeight  Single animation frame height.
     * @return new sprite prototype
     */
    public Proto createProto(BufferedImage image, double imageCenterX, double imageCenterY, int frameWidth, int frameHeight) {
        Proto proto = new Proto(this, scene, image, imageCenterX, imageCenterY, frameWidth, frameHeight);
        scene.addProto(proto);
        return proto;
    }

    /**
     * Create new sprite.
     *
     * @param proto        Sprite prototype.
     * @param objectWidth  Rendering width.
     * @param objectHeight Rendering height.
     * @return new sprite
     */
    public Sprite createSprite(Proto proto, double objectWidth, double objectHeight) {
        if (scene.getProtoByIndex(proto.getIndex()) != proto) {
            proto = createProto(proto.getImage(), proto.getAnchorX(), proto.getAnchorY(), proto.getFrameWidth(), proto.getFrameHeight());
        }
        Sprite sprite = new Sprite(scene, proto, objectWidth, objectHeight);
        scene.addSprite(sprite);
        return sprite;
    }

    /**
     * Create new sprite.
     * <br/>
     * Rendering height will be proportional to width.
     *
     * @param proto       Sprite prototype.
     * @param objectWidth Rendering width.
     * @return new sprite
     */
    public Sprite createSprite(Proto proto, double objectWidth) {
        if (scene.getProtoByIndex(proto.getIndex()) != proto) {
            proto = createProto(proto.getImage(), proto.getAnchorX(), proto.getAnchorY(), proto.getFrameWidth(), proto.getFrameHeight());
        }
        Sprite sprite = new Sprite(scene, proto, objectWidth, -1d);
        scene.addSprite(sprite);
        return sprite;
    }

    public Sprite copySprite(Sprite sprite) {
        Sprite inst = new Sprite(sprite);
        Proto proto = sprite.getProto();
        if (scene.getProtoByIndex(proto.getIndex()) != proto) {
            proto = createProto(proto.getImage(), proto.getAnchorX(), proto.getAnchorY(), proto.getFrameWidth(), proto.getFrameHeight());
            sprite.setProto(proto);
        }
        scene.addSprite(inst);
        return inst;
    }

    /**
     * Create new node of sprites.
     *
     * @return new node
     */
    public Sprite createNode() {
        Node node = new Node(scene);
        scene.addSprite(node);
        return node;
    }

    public void shutdown() {
        shutdown = true;
    }

    public void setTitle(String title) {
        output.setTitle(title);
    }

    private class Loop extends Thread {

        Loop() {
            super("Spriter rendering loop");
        }

        public void run() {
            while (true) {
                if (output.isClosing() || shutdown) {
                    break;
                }

                fps.calculateFps();

                do {
                    long currentFrame = fps.getCurrentFrameCounter();

                    Image canvasImage = output.getCanvasImage();
                    Graphics2D canvasGraphics = output.getCanvasGraphics();
                    Image rendered = renderer.render(scene, canvasImage, canvasGraphics);
                    output.setCanvasImage(rendered);

                    try {
                        synchronized (renderLock) {
                            if (fps.getCurrentFrameCounter() == currentFrame) {
                                renderLock.wait();
                            }
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } while (!output.sync());
            }
        }
    }
}