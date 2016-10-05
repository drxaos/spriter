package com.github.drxaos.spriter;

public class Fps implements IFps {

    private int fps = 0;
    private long fpsCounterStart = 0;
    private long currentFrameStart = 0;
    private int targetFps = 40;
    private int dynamicSleep = 1000 / targetFps;
    private int fpsCounter = 0;
    private Spriter spriter;

    @Override
    public void setTargetFps(int targetFps) {
        this.targetFps = targetFps;
    }

    @Override
    public void calculateFps() {
        if (System.currentTimeMillis() - fpsCounterStart > 1000) {
            fps = fpsCounter;
            fpsCounter = 0;
            if (fps < targetFps && dynamicSleep > 0) {
                if (fps > targetFps * 1.2) {
                    dynamicSleep = 1000 / targetFps;
                } else {
                    dynamicSleep--;
                }
            }
            if (fps > targetFps && dynamicSleep < 1000) {
                if (fps < targetFps * 0.8) {
                    dynamicSleep = 1000 / targetFps;
                } else {
                    dynamicSleep++;
                }
            }
            fpsCounterStart = System.currentTimeMillis();
        }
    }

    public int getFps() {
        return fps;
    }

    @Override
    public void beginFrame() {
        currentFrameStart = System.currentTimeMillis();
    }

    @Override
    public void sleepAfterFrame() throws InterruptedException {
        fpsCounter++;
        int sleep = (int) (dynamicSleep - (System.currentTimeMillis() - currentFrameStart));
        if (sleep > 0) {
            Thread.sleep(sleep);
        }
    }

    @Override
    public int getCurrentFrameCounter() {
        return fpsCounter;
    }

    @Override
    public void setSpriter(Spriter spriter) {
        this.spriter = spriter;
    }
}
