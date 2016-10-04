package com.github.drxaos.spriter;

import java.util.concurrent.atomic.AtomicInteger;

public class Fps {

    private long fps = 0;
    private long fpsCounterStart = 0;
    private long currentFrameStart = 0;
    private int targetFps = 40;
    private int dynamicSleep = 1000 / targetFps;
    private AtomicInteger fpsCounter = new AtomicInteger(0);

    public void setTargetFps(int targetFps) {
        this.targetFps = targetFps;
    }

    private void calculateFps() {
        if (System.currentTimeMillis() - fpsCounterStart > 1000) {
            fps = fpsCounter.getAndSet(0);
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

    public void beginFrame() {
        currentFrameStart = System.currentTimeMillis();
    }

    public void endFrame() throws InterruptedException {
        fpsCounter.incrementAndGet();
        int sleep = (int) (dynamicSleep - (System.currentTimeMillis() - currentFrameStart));
        if (sleep > 0) {
            Thread.sleep(sleep);
        }
    }

}
