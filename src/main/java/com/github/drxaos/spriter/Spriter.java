package com.github.drxaos.spriter;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Spriter {

    private Output output;

    private Renderer renderer;
    private Fps fps;

    private Scene scene;
    private GarbageCollector gc;
    private MainLoop mainLoop;

    private Spriter() {
    }

    public void setOutput(Output output) {
        this.output = output;
    }

    public void setRenderer(Renderer renderer) {
        this.renderer = renderer;
    }

    public void setFps(Fps fps) {
        this.fps = fps;
    }

    public void setScene(Scene scene) {
        this.scene = scene;
    }

    public void setGc(GarbageCollector gc) {
        this.gc = gc;
    }

    public void setMainLoop(MainLoop mainLoop) {
        this.mainLoop = mainLoop;
    }

    public static Spriter createDefault() {
        Spriter spriter = new Spriter();
        Output output = new Output();
        Renderer renderer = new Renderer();
        Fps fps = new Fps();
        Scene scene = new Scene();
        GarbageCollector garbageCollector = new GarbageCollector();
        MainLoop mainLoop = new MainLoop();

        spriter.setOutput(output);
        spriter.setRenderer(renderer);
        spriter.setFps(fps);
        spriter.setScene(scene);
        spriter.setGc(garbageCollector);
        spriter.setMainLoop(mainLoop);

        mainLoop.start();

        return spriter;
    }

    public static Spriter createCustom(
            Output output,
            Renderer renderer,
            Fps fps,
            Scene scene,
            GarbageCollector garbageCollector,
            MainLoop mainLoop
    ) {
        Spriter spriter = new Spriter();

        spriter.setOutput(output);
        spriter.setRenderer(renderer);
        spriter.setFps(fps);
        spriter.setScene(scene);
        spriter.setGc(garbageCollector);
        spriter.setMainLoop(mainLoop);

        mainLoop.start();

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
     * Set target FPS. Spriter will sleep in endFrame().
     */
    public void setTargetFps(int targetFps) {
        fps.setTargetFps(targetFps);
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
            gc.endFrame(scene);
            scene.snapshot();
            renderLock.notifyAll();
        }

        fps.endFrame();
    }

    /**
     * Get renderer
     */
    public Renderer getRenderer() {
        return renderer;
    }

    public void setBackgroundColor(Color color) {
        scene.setBackgroundColor(color);
    }

    public void setBorderColor(Color color) {
        scene.setBorderColor(color);
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
        Sprite sprite = new Sprite(this, scene, proto, objectWidth, objectHeight);
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
        Sprite sprite = new Sprite(this, scene, proto, objectWidth, -1d);
        scene.addSprite(sprite);
        return sprite;
    }

    Sprite copySprite(Sprite sprite) {
        Sprite inst = new Sprite(sprite);
        scene.addSprite(inst);
        return inst;

    }

    public void shutdown() {
        mainLoop.shutdown();
    }

    public void gc() {
        gc.garbageCollect(scene);
    }


}