package com.github.drxaos.spriter;

public interface Control {

    /**
     * Get current mouse X coordinate.
     */
    double getMouseX();

    /**
     * Get current mouse Y coordinate.
     */
    double getMouseY();

    /**
     * Get current mouse coordinates.
     */
    Point getMousePos();

    /**
     * Get last mouse click coordinates and button.
     */
    Click getClick();

    /**
     * Get last pressed key.
     */
    Integer getKeyPress();

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
    boolean isButtonDown(int btn);

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
    boolean isKeyDown(int key);

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
    boolean isAnyKeyDown(int... keys);

    /**
     * Dump input state to console.
     */
    void dump();
}
