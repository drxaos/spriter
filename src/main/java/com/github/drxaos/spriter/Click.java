package com.github.drxaos.spriter;

/**
 * Clicked point
 */
public class Click extends Point {

    private final int button;

    public Click(double x, double y, int button) {
        super(x, y);
        this.button = button;
    }

    public Click(Point point, int button) {
        super(point.getX(), point.getY());
        this.button = button;
    }

    /**
     * Mouse button.
     * <br/>
     * See also {@link java.awt.event.MouseEvent}
     */
    public int getButton() {
        return button;
    }
}
