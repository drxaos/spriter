package com.github.drxaos.spriter.examples;

import com.github.drxaos.spriter.Spriter;
import com.sun.javafx.application.PlatformImpl;
import javafx.application.Application;
import javafx.embed.swing.JFXPanel;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class Tiles {
    public static final int L_COIN = 200;
    public static final int L_PLAYER = 300;
    public static final int L_WATER = 400;
    public static final int L_BOX = 450;
    public static final int L_TILE = 500;

    public static BufferedImage loadImage(String name) throws IOException {
        return ImageIO.read(Tiles.class.getResource(name));
    }

    static Spriter.Sprite tileProto, playerProto;

    static class Coin {
        Spriter.Sprite sprite;
        double x, y;
        int p, f;

        public Coin(Spriter.Sprite sprite, double x, double y) {
            this.sprite = sprite;
            this.x = x;
            this.y = y;
        }

        public void animate() {
            f++;
            sprite.setY(y + Math.sin(0.1 * f) * 0.1);
            if (f % 100 == 0) {
                sprite.setFrame(1);
            } else if (f % 100 == 5) {
                sprite.setFrame(2);
            } else if (f % 100 > 10) {
                sprite.setFrame(0);
            }
            ;
        }
    }

    static class Water {
        Spriter.Sprite sprite0, sprite1, sprite2;
        double x, y;
        int p, f;

        public Water(Spriter.Sprite sprite, double x, double y) {
            this.sprite1 = sprite;
            this.sprite2 = sprite.clone().setX(x + 1);
            this.sprite0 = sprite.clone().setX(x - 1);
            this.x = x;
            this.y = y;
        }

        public void animate() {
            f++;
            double shiftX = Math.sin(0.05 * f) * 0.5;
            sprite0.setX(x - 1 + shiftX);
            sprite1.setX(x + shiftX);
            sprite2.setX(x + 1 + shiftX);
        }
    }

    static class Player {
        Spriter.Sprite sprite;
        double x, y, vx, vy;
        int state;
        int f;

        public Player(Spriter.Sprite sprite, double x, double y) {
            this.sprite = sprite;
            this.x = x;
            this.y = y;
        }

        public void go(int go) {
            if (go == 0) {
                if (vx > 0.001) {
                    vx -= 0.005;
                } else if (vx < -0.001) {
                    vx += 0.005;
                } else {
                    state = 0;
                }
            } else if (go == 1 && vx < 0.03) {
                vx += 0.005;
                state = 1;
            } else if (go == -1 && vx > -0.03) {
                vx -= 0.005;
                state = -1;
            }

            x += vx;
            sprite.setX(x);
        }

        public void animate() {
            f++;
            if (state == 1 || state == -1) {
                int frame = (f / 3) % 4;
                if ((f / 3) % 8 >= 4) {
                    frame = 3 - frame;
                }
                sprite.setFrame(frame).setFrameRow(1).setFlipX(state == -1);
            }
            if (state == 0) {
                sprite.setFrame(0).setFrameRow(0);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        SwingUtilities.invokeLater(() -> {
            new JFXPanel();
            latch.countDown();
        });
        latch.await();

        final URL resource = Tiles.class.getResource("/mushroom.mp3");
        final Media media = new Media(resource.toString());
        final MediaPlayer mediaPlayer = new MediaPlayer(media);

        Spriter spriter = new Spriter("Tiles");
        spriter.setViewportWidth(9.75);
        spriter.setViewportHeight(9.75);
        spriter.setViewportShiftX(5 - 0.5);
        spriter.setViewportShiftY(5 - 0.5);

        spriter.setBackgroundColor(Color.decode("#D0F4F7"));

        BufferedImage tilesSpriteSheet = loadImage("/tiles.png");
        BufferedImage playerSpriteSheet = loadImage("/alien.png");

        tileProto = spriter.createSpriteProto(tilesSpriteSheet, 35, 35, 70, 70).setSquareSide(1).setLayer(L_TILE);
        playerProto = spriter.createSpriteProto(playerSpriteSheet, 72 / 2, 97, 72, 97).setWidthProportional(0.75).setLayer(L_PLAYER);

        HashMap<Spriter.Point, Spriter.Sprite> bricks = new HashMap<>();
        HashMap<Spriter.Point, Coin> coins = new HashMap<>();
        HashMap<Spriter.Point, Water> water = new HashMap<>();
        Player player = null;

        String level = new String(Files.readAllBytes(Paths.get(Tiles.class.getResource("/level.txt").toURI())));
        int ty = -1;
        for (String row : level.split("\n")) {
            ty++;
            int tx = -1;
            for (char c : row.toCharArray()) {
                tx++;

                if (c == '@') {
                    Spriter.Sprite pl = playerProto.createGhost().setFrame(0).setFrameRow(0).setPos(tx, ty + 0.5).setVisible(true);
                    player = new Player(pl, tx, ty);
                    continue;
                }

                int fx = -1, fy = -1, l = L_TILE;
                switch (c) {
                    case 'm':
                        fx = 0;
                        fy = 1;
                        break;
                    case 'b':
                        fx = 9;
                        fy = 1;
                        break;
                    case 'd':
                        fx = 7;
                        fy = 1;
                        break;
                    case 'T':
                        fx = 8;
                        fy = 1;
                        break;
                    case '~':
                        fx = 0;
                        fy = 2;
                        l = L_WATER;
                        break;
                    case 'x':
                        fx = 2;
                        fy = 1;
                        l = L_BOX;
                        break;
                    case '<':
                        fx = 3;
                        fy = 1;
                        break;
                    case '>':
                        fx = 4;
                        fy = 1;
                        break;
                    case 'A':
                        fx = 5;
                        fy = 1;
                        break;
                    case 'a':
                        fx = 6;
                        fy = 1;
                        break;
                    case 'c':
                        fx = 0;
                        fy = 3;
                        l = L_COIN;
                        break;
                    case '#':
                        fx = 0;
                        fy = 0;
                        break;
                }
                if (fx >= 0 && fy >= 0) {
                    Spriter.Point p = new Spriter.Point(tx, ty);
                    Spriter.Sprite tile = tileProto.createGhost().setFrame(fx).setFrameRow(fy).setPos(p).setVisible(true).setLayer(l);
                    if (c == '#') {
                        bricks.put(p, tile);
                    }
                    if (c == 'c') {
                        coins.put(p, new Coin(tile, tx, ty));
                    }
                    if (c == '~') {
                        water.put(p, new Water(tile, tx, ty));
                    }
                }
            }
        }

        for (Map.Entry<Spriter.Point, Spriter.Sprite> entry : bricks.entrySet()) {
            double x = entry.getKey().getX();
            double y = entry.getKey().getY();
            int fx = 0;
            boolean left = bricks.containsKey(new Spriter.Point(x - 1, y));
            boolean up = bricks.containsKey(new Spriter.Point(x, y - 1));
            boolean down = bricks.containsKey(new Spriter.Point(x, y + 1));
            boolean right = bricks.containsKey(new Spriter.Point(x + 1, y));
            if (left && right || up && down) {
                fx = 1;
            } else if (down && !left && !right) {
                fx = 2;
            } else if (up && !left && !right) {
                fx = 3;
            } else if (right && !up && !down) {
                fx = 4;
            } else if (left && !up && !down) {
                fx = 5;
            } else if (right && down) {
                fx = 6;
            } else if (left && down) {
                fx = 7;
            } else if (left && up) {
                fx = 8;
            } else if (right && up) {
                fx = 9;
            }
            entry.getValue().setFrame(fx);
        }

        spriter.setViewportShiftY(ty - 5 + 0.5);

        mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
        mediaPlayer.setAutoPlay(true);
        mediaPlayer.play();

        Spriter.Control control = spriter.getControl();

        while (true) {
            for (Water w : water.values()) {
                w.animate();
            }
            for (Coin c : coins.values()) {
                c.animate();
            }
            if (player != null) {
                if (control.isKeyDown(KeyEvent.VK_RIGHT)) {
                    player.go(1);
                } else if (control.isKeyDown(KeyEvent.VK_LEFT)) {
                    player.go(-1);
                } else {
                    player.go(0);
                }
                player.animate();
            }
            Thread.sleep(30);
        }
    }
}
