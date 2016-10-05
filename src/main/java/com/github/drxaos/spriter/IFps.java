package com.github.drxaos.spriter;

public interface IFps {
    void setTargetFps(int targetFps);

    void calculateFps();

    int getFps();

    void beginFrame();

    void sleepAfterFrame() throws InterruptedException;

    int getCurrentFrameCounter();

    void setSpriter(Spriter spriter);
}
