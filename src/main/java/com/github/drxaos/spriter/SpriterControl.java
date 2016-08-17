package com.github.drxaos.spriter;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class SpriterControl {

    AtomicReference<Double>
            mx = new AtomicReference<Double>(0d),
            my = new AtomicReference<Double>(0d);
    AtomicReference<SpriterPoint> c = new AtomicReference<>();

    Map<Integer, AtomicBoolean> buttons = new HashMap<>();
    Map<Integer, AtomicBoolean> keys = new HashMap<>();

    SpriterWindow window;

    SpriterControl(SpriterWindow window) {
        this.window = window;
    }

    public double getMouseX() {
        return mx.get();
    }

    public double getMouseY() {
        return my.get();
    }

    public SpriterPoint getClick() {
        return c.getAndSet(null);
    }

    public boolean isButtonDown(int btn) {
        AtomicBoolean b = buttons.get(btn);
        if (b == null) {
            return false;
        }
        return b.get();
    }

    public boolean isKeyDown(int key) {
        AtomicBoolean b = keys.get(key);
        if (b == null) {
            return false;
        }
        return b.get();
    }
}
