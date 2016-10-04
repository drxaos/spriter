package com.github.drxaos.spriter;

import java.util.concurrent.atomic.AtomicBoolean;

public class MainLoop extends Thread {
    private AtomicBoolean shutdown = new AtomicBoolean(false);
    private final Object renderLock = new Object();

    public void shutdown() {
        shutdown.set(true);
    }

    public void run() {
        while (true) {
            if (shutdown.get()) {
                break;
            }

            calculateFps();

            do {
                long currentFrame = fpsCounter.get();

                renderer.render(scene);

                try {
                    synchronized (renderLock) {
                        if (fpsCounter.get() == currentFrame) {
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
