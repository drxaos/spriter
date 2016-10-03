package com.github.drxaos.spriter;

import java.awt.*;

/**
 * Render chain
 */
abstract public class RenderChain {
    protected RenderChain next;

    public void setNext(RenderChain next) {
        this.next = next;
    }

    public Image chain(Scene scene, Image img, Graphics2D g, int width, int height) {
        Image render = render(scene, img, g, width, height);
        if (next == null) {
            return render;
        }
        if (render != img) {
            g = (Graphics2D) render.getGraphics();
            width = render.getWidth(null);
            height = render.getHeight(null);
        }
        return next.chain(scene, render, g, width, height);
    }

    public Image render(Scene scene, Image img, Graphics2D g, int width, int height) {
        render(scene, g, width, height);
        return img;
    }

    abstract public void render(Scene scene, Graphics2D g, int width, int height);
}
