package com.github.drxaos.spriter;

public class GarbageCollector {

    private boolean shouldGC = false;
    private boolean autoGC = true;
    private boolean debugGC = false;

    /**
     * Auto garbage collection every second.
     */
    void setAutoGC(boolean autoGC) {
        this.autoGC = autoGC;
    }

    /**
     * Show collected objects count.
     */
    void setDebugGC(boolean debugGC) {
        this.debugGC = debugGC;
    }

    void endFrame(Scene scene) {
        if (shouldGC) {
            shouldGC = false;
            garbageCollect(scene);
        }
    }

    void triggerAuto() {
        shouldGC = autoGC;
    }

    public int garbageCollect(Scene scene) {
        int collected = 0;
        synchronized (scene.sprites) {
            int marked = 1;
            while (marked > 0) {
                marked = 0;
                for (int currentIndex = scene.sprites.size() - 1; currentIndex >= 0; currentIndex--) {
                    Sprite current = scene.sprites.get(currentIndex);

                    if (current.isRemoved()) {
                        Sprite last = scene.sprites.remove(scene.sprites.size() - 1);
                        for (Sprite sprite : scene.sprites) {
                            if (sprite.getParentId() == currentIndex) {
                                sprite.remove();
                                marked++;
                            } else if (sprite.getParentId() == last.getIndex()) {
                                sprite.setParentId(currentIndex);
                            }
                        }
                        if (last != current) {
                            scene.sprites.set(currentIndex, last);
                            last.setIndex(currentIndex);
                        }

                        collected++;
                    }
                }
            }
            if (debugGC) {
                System.err.println("Collected: " + collected + " (left: " + scene.sprites.size() + ")");
            }
        }
        System.gc();
        return collected;
    }


}
