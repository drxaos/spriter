package com.github.drxaos.spriter.examples;

import com.github.drxaos.spriter.Spriter;
import com.github.drxaos.spriter.SpriterUtils;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.concurrent.TimeUnit;


/**
 * Based on https://github.com/drxaos-edu/spacerace
 */
public class Multiplayer {

    final static int
            LAYER_BG = 0,
            LAYER_STAR = LAYER_BG + 50,
            LAYER_OBJECTS = 500,
            LAYER_SHIP = LAYER_OBJECTS,
            LAYER_SHIP_TAIL = LAYER_SHIP - 100,
            LAYER_WALL = LAYER_OBJECTS,
            LAYER_UFO = LAYER_WALL + 50;

    static double
            player1_a, player1_x, player1_y, player1_vx, player1_vy,
            player2_a, player2_x, player2_y, player2_vx, player2_vy;

    static Spriter.Sprite
            player_green,
            player_green_tail,
            player_red,
            player_red_tail;

    static double[]
            wall_x = new double[10000],
            wall_y = new double[10000];

    static double[]
            ufo_x = new double[10000],
            ufo_y = new double[10000],
            ufo_vx = new double[10000],
            ufo_vy = new double[10000];

    static Spriter.Sprite[] ufo = new Spriter.Sprite[10000];

    static class TwinView extends Spriter.PainterChain {

        Spriter spriter;
        Spriter.PainterChain renderer;
        BufferedImage left, right;
        Graphics2D lg, rg;

        public TwinView(Spriter spriter, Spriter.PainterChain renderer) {
            this.spriter = spriter;
            this.renderer = renderer;
        }

        @Override
        public BufferedImage render(BufferedImage img, Graphics2D g, int width, int height) {
            int w = width / 2 - 10;
            int h = height - 10;

            if (left == null || left.getWidth() != w || left.getHeight() != h) {
                left = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                lg = left.createGraphics();
            }
            if (right == null || right.getWidth() != w || right.getHeight() != h) {
                right = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                rg = right.createGraphics();
            }

            spriter.setViewportShift(player2_x, player2_y);
            left = renderer.chain(left, lg, w, h);

            spriter.setViewportShift(player1_x, player1_y);
            right = renderer.chain(right, rg, w, h);

            g.drawImage(left, 5, 5, null);
            g.drawImage(right, w + 15, 5, null);
            return img;
        }

        @Override
        public void render(Graphics2D g, int width, int height) {
            // none
        }
    }


    public static void main(String[] args) throws Exception {

        Spriter spriter = new Spriter("Multiplayer");
        spriter.setViewportWidth(15);
        spriter.setViewportHeight(15);

        spriter.setBackgroundColor(Color.BLACK);
        spriter.beginFrame();
        Spriter.Sprite loading = spriter.createSprite(SpriterUtils.loadImageFromResource("/loading.png"), 367 / 2, 62 / 2, 5);
        spriter.endFrame();
        spriter.pause();

        loading.setVisible(false);

        BufferedImage background_image = SpriterUtils.loadImageFromResource("/background.jpg");
        Spriter.Sprite background = spriter.createSpriteProto(background_image, 512, 512).setWidth(25).setHeight(25).setLayer(LAYER_BG);
        for (int x = 0; x <= 100; x += 25) {
            for (int y = 0; y <= 100; y += 25) {
                background.createGhost().setPos(x, y).setVisible(true);
            }
        }

        BufferedImage player_green_image = SpriterUtils.loadImageFromResource("/player-green.png");
        BufferedImage player_red_image = SpriterUtils.loadImageFromResource("/player-red.png");
        BufferedImage tail_image = SpriterUtils.loadImageFromResource("/tail.png");
        BufferedImage ufo_image = SpriterUtils.loadImageFromResource("/ufo.png");
        BufferedImage star_image = SpriterUtils.loadImageFromResource("/star.png");
        BufferedImage meteor_image = SpriterUtils.loadImageFromResource("/meteor.png");
        BufferedImage map_image = SpriterUtils.loadImageFromResource("/map.png");

        Spriter.Sprite ufoPrototype = spriter.createSpriteProto(ufo_image, 45, 45).setWidth(1).setHeight(1).setLayer(LAYER_UFO);
        Spriter.Sprite wallPrototype = spriter.createSpriteProto(meteor_image, 50, 50).setWidth(1).setHeight(1).setLayer(LAYER_WALL);
        Spriter.Sprite starPrototype = spriter.createSpriteProto(star_image, 50, 50).setWidth(0.5).setHeight(0.5).setLayer(LAYER_STAR);
        Spriter.Sprite trg = spriter.createSprite(SpriterUtils.loadImageFromResource("/point.png"), 256 / 2, 256 / 2, 0.5);

        player_green = spriter.createSprite(player_green_image, 40, 50, 1).setLayer(LAYER_SHIP);
        player_red = spriter.createSprite(player_red_image, 40, 50, 1).setLayer(LAYER_SHIP);

        Spriter.Sprite tailPrototype = spriter.createSpriteProto(tail_image, 41, 8).setWidth(0.4).setHeight(0.2).setX(-0.2).setLayer(LAYER_SHIP_TAIL);
        player_green_tail = tailPrototype.clone().setParent(player_green).setVisible(true);
        player_red_tail = tailPrototype.clone().setParent(player_red).setVisible(true);

        int wall_counter = 0;
        int ufo_counter = 0;
        for (int y = 0; y < 100; y++) {
            for (int x = 0; x < 100; x++) {
                int[] pixel = new int[4];
                map_image.getData().getPixel(x, y, pixel);
                int type = (pixel[0] & 1) + ((pixel[1] & 1) << 1) + ((pixel[2] & 1) << 2);
                switch (type) {
                    case (0):
                        wallPrototype.createGhost().setPos(x, y).setVisible(true);
                        wall_x[wall_counter] = x;
                        wall_y[wall_counter] = y;
                        wall_counter++;
                        break;
                    case (1):
                        player_red.setPos(x, y);
                        player2_a = -Math.PI / 2;
                        player2_x = x;
                        player2_y = y;
                        player2_vx = 0;
                        player2_vy = 0;
                        break;
                    case (2):
                        player_green.setPos(x, y).setAngle(-Math.PI / 2);
                        player1_a = -Math.PI / 2;
                        player1_x = x;
                        player1_y = y;
                        player1_vx = 0;
                        player1_vy = 0;
                        break;
                    case (3):
                        starPrototype.clone().setPos(x, y).setWidthProportional(Math.random() * 0.4 + 0.4).setVisible(true);
                        break;
                    case (4):
                        ufo[ufo_counter] = ufoPrototype.createGhost().setPos(x, y).setVisible(true);
                        ufo_x[ufo_counter] = x;
                        ufo_y[ufo_counter] = y;
                        ufo_vx[ufo_counter] = 0;
                        ufo_vy[ufo_counter] = 0;
                        ufo_counter++;
                        break;
                    case (5):
                        break;
                    case (6):
                        break;
                    case (7):
                        break;
                }
            }
        }

        TwinView twinView = new TwinView(spriter, spriter.getPainterChainHead());
        spriter.setPainterChainHead(twinView);

        spriter.setDebug(true);

        Spriter.Control control = spriter.getControl();

        spriter.unpause();

        while (true) {
            spriter.beginFrame();

            if (control.isKeyDown(KeyEvent.VK_LEFT)) {
                player1_a -= 0.06;
                player_green.setAngle(player1_a);
            }
            if (control.isKeyDown(KeyEvent.VK_RIGHT)) {
                player1_a += 0.06;
                player_green.setAngle(player1_a);
            }
            if (control.isKeyDown(KeyEvent.VK_UP)) {
                player1_vx += Math.cos(player1_a) * 0.005;
                player1_vy += Math.sin(player1_a) * 0.005;
                player_green_tail.setVisible(true);
            } else {
                player_green_tail.setVisible(false);
            }

            player1_x += player1_vx;
            player1_y += player1_vy;
            player_green.setPos(player1_x, player1_y);


            if (control.isKeyDown(KeyEvent.VK_A)) {
                player2_a -= 0.06;
                player_red.setAngle(player2_a);
            }
            if (control.isKeyDown(KeyEvent.VK_D)) {
                player2_a += 0.06;
                player_red.setAngle(player2_a);
            }
            if (control.isKeyDown(KeyEvent.VK_W)) {
                player2_vx += Math.cos(player2_a) * 0.005;
                player2_vy += Math.sin(player2_a) * 0.005;
                player_red_tail.setVisible(true);
            } else {
                player_red_tail.setVisible(false);
            }

            player2_x += player2_vx;
            player2_y += player2_vy;
            player_red.setPos(player2_x, player2_y);


            for (int i = 0; i < wall_counter; i++) {
                double deltaX = wall_x[i] - player1_x;
                double deltaY = wall_y[i] - player1_y;
                double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
                if (distance <= 1) {
                    double dx = wall_x[i] - player1_x;
                    double dy = wall_y[i] - player1_y;
                    double angle = Math.atan2(dy, dx);
                    double targetX = player1_x + Math.cos(angle);
                    double targetY = player1_y + Math.sin(angle);
                    double ax = (targetX - wall_x[i]);
                    double ay = (targetY - wall_y[i]);
                    player1_vx -= ax;
                    player1_vy -= ay;
                    player1_vx *= 0.7;
                    player1_vy *= 0.7;
                }
            }

            for (int i = 0; i < ufo_counter; i++) {
                double deltaX = ufo_x[i] - player1_x;
                double deltaY = ufo_y[i] - player1_y;
                double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
                if (distance <= 1) {
                    double dx = ufo_x[i] - player1_x;
                    double dy = ufo_y[i] - player1_y;
                    double angle = Math.atan2(dy, dx);
                    double targetX = player1_x + Math.cos(angle);
                    double targetY = player1_y + Math.sin(angle);
                    double ax = (targetX - ufo_x[i]);
                    double ay = (targetY - ufo_y[i]);
                    player1_vx -= ax;
                    player1_vy -= ay;
                    player1_vx *= 0.7;
                    player1_vy *= 0.7;
                    ufo_vx[i] += ax;
                    ufo_vy[i] += ay;
                }
            }

            for (int i = 0; i < wall_counter; i++) {
                double deltaX = wall_x[i] - player2_x;
                double deltaY = wall_y[i] - player2_y;
                double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
                if (distance <= 1) {
                    double dx = wall_x[i] - player2_x;
                    double dy = wall_y[i] - player2_y;
                    double angle = Math.atan2(dy, dx);
                    double targetX = player2_x + Math.cos(angle);
                    double targetY = player2_y + Math.sin(angle);
                    double ax = (targetX - wall_x[i]);
                    double ay = (targetY - wall_y[i]);
                    player2_vx -= ax;
                    player2_vy -= ay;
                    player2_vx *= 0.7;
                    player2_vy *= 0.7;
                }
            }

            for (int i = 0; i < ufo_counter; i++) {
                double deltaX = ufo_x[i] - player2_x;
                double deltaY = ufo_y[i] - player2_y;
                double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
                if (distance <= 1) {
                    double dx = ufo_x[i] - player2_x;
                    double dy = ufo_y[i] - player2_y;
                    double angle = Math.atan2(dy, dx);
                    double targetX = player2_x + Math.cos(angle);
                    double targetY = player2_y + Math.sin(angle);
                    double ax = (targetX - ufo_x[i]);
                    double ay = (targetY - ufo_y[i]);
                    player2_vx -= ax;
                    player2_vy -= ay;
                    player2_vx *= 0.7;
                    player2_vy *= 0.7;
                    ufo_vx[i] += ax;
                    ufo_vy[i] += ay;
                }
            }

            double deltaX = player1_x - player2_x;
            double deltaY = player1_y - player2_y;
            double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
            if (distance <= 1) {
                double dx = player1_x - player2_x;
                double dy = player1_y - player2_y;
                double angle = Math.atan2(dy, dx);
                double targetX = player2_x + Math.cos(angle);
                double targetY = player2_y + Math.sin(angle);
                double ax = (targetX - player1_x);
                double ay = (targetY - player1_y);
                player2_vx -= ax;
                player2_vy -= ay;
                player1_vx += ax;
                player1_vy += ay;
                player2_vx *= 0.7;
                player2_vy *= 0.7;
                player1_vx *= 0.7;
                player1_vy *= 0.7;
            }

            for (int i = 0; i < ufo_counter; i++) {
                ufo_x[i] += ufo_vx[i];
                ufo_y[i] += ufo_vy[i];
                ufo_vx[i] *= 0.9;
                ufo_vy[i] *= 0.9;
                ufo[i].setPos(ufo_x[i], ufo_y[i]);
            }

            spriter.endFrame();

            TimeUnit.MILLISECONDS.sleep(25);
        }
    }
}
