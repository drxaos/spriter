package com.github.drxaos.spriter;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Input manager
 */
public class SpriterJFrameControl implements Control {

    AtomicReference<Double>
            mx = new AtomicReference<>(0d),
            my = new AtomicReference<>(0d);
    AtomicReference<Click> c = new AtomicReference<>();
    AtomicReference<Integer> k = new AtomicReference<>(null);

    Map<Integer, AtomicBoolean> buttons = new HashMap<>();
    Map<Integer, AtomicBoolean> keys = new HashMap<>();

    @Override
    public double getMouseX() {
        return mx.get();
    }

    @Override
    public double getMouseY() {
        return my.get();
    }

    @Override
    public Point getMousePos() {
        return new Point(getMouseX(), getMouseY());
    }

    @Override
    public Click getClick() {
        return c.getAndSet(null);
    }

    @Override
    public Integer getKeyPress() {
        return k.getAndSet(null);
    }

    @Override
    public boolean isButtonDown(int btn) {
        AtomicBoolean b = buttons.get(btn);
        if (b == null) {
            return false;
        }
        return b.get();
    }

    @Override
    public boolean isKeyDown(int key) {
        AtomicBoolean b = keys.get(key);
        if (b == null) {
            return false;
        }
        return b.get();
    }

    @Override
    public boolean isAnyKeyDown(int... keys) {
        for (int key : keys) {
            if (isKeyDown(key)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void dump() {
        String dump = "";
        for (Map.Entry<Integer, AtomicBoolean> entry : buttons.entrySet()) {
            if (entry.getValue().get()) {
                dump += "b" + entry.getKey() + ",";
            }
        }
        for (Map.Entry<Integer, AtomicBoolean> entry : keys.entrySet()) {
            if (entry.getValue().get()) {
                dump += "k" + entry.getKey() + ",";
            }
        }
        System.out.println(dump);
    }
}
