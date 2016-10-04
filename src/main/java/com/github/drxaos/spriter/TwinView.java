package com.github.drxaos.spriter;

import java.awt.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

class TwinView extends Renderer {

    Spriter spriter;
    Renderer rendererLeft, rendererRight;
    Image left, right;
    Graphics2D graphicsLeft, graphicsRight;
    ExecutorService executor = Executors.newFixedThreadPool(1);

    public TwinView(Spriter spriter) {
        this.spriter = spriter;
        this.rendererLeft = spriter.createRenderer();
        this.rendererRight = spriter.createRenderer();
    }

    @Override
    public Image render(Scene scene, Image img, Graphics2D g, int width, int height) {
        int w = width / 2 - 10;
        int h = height - 10;

        if (left == null || left.getWidth(null) != w || left.getHeight(null) != h) {
            left = spriter.makeOutputImage(w, h, true);
            graphicsLeft = (Graphics2D) left.getGraphics();
        }
        if (right == null || right.getWidth(null) != w || right.getHeight(null) != h) {
            right = spriter.makeOutputImage(w, h, true);
            graphicsRight = (Graphics2D) right.getGraphics();
        }

        Future<Image> leftFuture = executor.submit(() -> {
            rendererLeft.setViewportShift(player2_x, player2_y);
            return rendererLeft.chain(scene, this.left, graphicsLeft, w, h);
        });

        Future<Image> rightFuture = executor.submit(() -> {
            rendererRight.setViewportShift(player1_x, player1_y);
            return rendererRight.chain(scene, this.right, graphicsRight, w, h);
        });

        g.setColor(Color.BLACK);
        g.setBackground(Color.BLACK);
        g.fillRect(0, 0, width, height);

        try {
            g.drawImage(leftFuture.get(), 5, 5, null);
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            System.exit(0);
        }

        try {
            g.drawImage(rightFuture.get(), w + 15, 5, null);
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            System.exit(0);
        }

        return img;
    }

    @Override
    public void render(Scene scene) {

    }

    @Override
    public void setDebug(boolean debug) {
        rendererLeft.setDebug(debug);
        rendererRight.setDebug(debug);
    }
}