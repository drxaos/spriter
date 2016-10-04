package com.github.drxaos.spriter;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Input manager
 */
public class Control {

    private AtomicReference<Double>
            mx = new AtomicReference<>(0d),
            my = new AtomicReference<>(0d);
    private AtomicReference<Click> c = new AtomicReference<>();
    private AtomicReference<Integer> k = new AtomicReference<>(null);

    private Map<Integer, AtomicBoolean> buttons = new HashMap<>();
    private Map<Integer, AtomicBoolean> keys = new HashMap<>();

    /**
     * Get current mouse X coordinate.
     */
    public double getMouseX() {
        return mx.get();
    }

    /**
     * Get current mouse Y coordinate.
     */
    public double getMouseY() {
        return my.get();
    }

    /**
     * Get current mouse coordinates.
     */
    public Point getMousePos() {
        return new Point(getMouseX(), getMouseY());
    }

    /**
     * Get last mouse click coordinates and button.
     */
    public Click getClick() {
        return c.getAndSet(null);
    }

    /**
     * Get last pressed key.
     */
    public Integer getKeyPress() {
        return k.getAndSet(null);
    }

    /**
     * Check if mouse button is pressed now.
     * <br/>
     * Example:
     * <pre>
     * control.isButtonDown(MouseEvent.BUTTON1)
     * </pre>
     * <br/>
     * See also {@link java.awt.event.MouseEvent}
     */
    public boolean isButtonDown(int btn) {
        AtomicBoolean b = buttons.get(btn);
        if (b == null) {
            return false;
        }
        return b.get();
    }

    /**
     * Check if keyboard key is pressed now.
     * <br/>
     * Example:
     * <pre>
     * control.isKeyDown(KeyEvent.VK_UP)
     * </pre>
     * <br/>
     * See also {@link java.awt.event.KeyEvent}
     */
    public boolean isKeyDown(int key) {
        AtomicBoolean b = keys.get(key);
        if (b == null) {
            return false;
        }
        return b.get();
    }

    /**
     * Check if any of keyboard keys from list is pressed now.
     * <br/>
     * Example:
     * <pre>
     * control.isAnyKeyDown(KeyEvent.VK_UP, KeyEvent.VK_W, KeyEvent.VK_NUMPAD8)
     * </pre>
     * <br/>
     * See also {@link java.awt.event.KeyEvent}
     */
    public boolean isAnyKeyDown(int... keys) {
        for (int key : keys) {
            if (isKeyDown(key)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Dump input state to console.
     */
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
