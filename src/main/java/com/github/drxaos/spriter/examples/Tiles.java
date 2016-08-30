package com.github.drxaos.spriter.examples;

import com.github.drxaos.spriter.Spriter;
import javafx.embed.swing.JFXPanel;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class Tiles {
    public static final int L_DOOR = 100;
    public static final int L_SIGN = 100;
    public static final int L_COIN = 200;
    public static final int L_PLAYER = 300;
    public static final int L_WATER = 400;
    public static final int L_BOX = 450;
    public static final int L_TILE = 500;
    public static final int L_HUD = 1000;

    public static final int L_SHADOW = 2000;
    public static final int L_WIN = 2100;

    public static BufferedImage loadImage(String name) throws IOException {
        return ImageIO.read(Tiles.class.getResource(name));
    }

    static Spriter.Sprite tileProto, playerProto, numbersProto, hudProto;

    static class Coin {
        Spriter.Sprite sprite;
        double x, y;
        int p, f;
        boolean taken = false;

        public Coin(Spriter.Sprite sprite, double x, double y) {
            this.sprite = sprite;
            this.x = x;
            this.y = y;
        }

        public boolean take() {
            if (taken) {
                return false;
            }
            sprite.setVisible(false);
            taken = true;
            return true;
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

    static class Key {
        Spriter.Sprite sprite;
        double x, y;
        boolean taken = false;

        public Key(Spriter.Sprite sprite, double x, double y) {
            this.sprite = sprite;
            this.x = x;
            this.y = y;
        }

        public boolean take() {
            if (taken) {
                return false;
            }
            sprite.setVisible(false);
            taken = true;
            return true;
        }

        public boolean isTaken() {
            return taken;
        }
    }

    static class Door {
        Spriter.Sprite sprite, spriteTop;
        double x, y;

        public Door(Spriter.Sprite sprite, Spriter.Sprite spriteTop, double x, double y) {
            this.sprite = sprite;
            this.spriteTop = spriteTop;
            this.x = x;
            this.y = y;
        }

        public void open() {
            sprite.setFrame(5).setFrameRow(3);
            spriteTop.setFrame(6).setFrameRow(3);
        }
    }

    static class Brick {
        public static final int GROUND = 0;
        public static final int ROCK = 1;
        public static final int BOX = 2;
        public static final int WALL = 3;

        Spriter.Sprite sprite;
        int type;
        double x, y;

        public Brick(Spriter.Sprite sprite, double x, double y, int type) {
            this.sprite = sprite;
            this.x = x;
            this.y = y;
            this.type = type;
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
        double x, y, vx, vy, sx, sy;
        int state, statey;
        int f;

        public Player(Spriter.Sprite sprite, double x, double y) {
            this.sprite = sprite;
            this.x = sx = x;
            this.y = sy = y;
            sprite.setPos(x, y);
        }

        public void reset() {
            x = sx;
            y = sy;
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
            } else if (go == 1 && vx < 0.05) {
                vx += 0.005;
                state = 1;
            } else if (go == -1 && vx > -0.05) {
                vx -= 0.005;
                state = -1;
            }

            x += vx;
            sprite.setX(x);
        }

        public void fly(int fly, double ground) {
            if (fly == 0 && vy >= -0.01) {
                statey = 0;
                vy = 0;
                if (y - ground > 0.06) {
                    y -= 0.02;
                } else {
                    y = ground;
                }
            } else if (fly == 0 && vy < -0.01) {
                statey = 1;
                vy += 0.005;
            } else {
                if (vy < 0.15) {
                    vy += 0.005;
                }
                statey = 1;
            }
            y += vy;
            sprite.setY(y);
        }

        public void jump(double v) {
            vy = v;
        }

        public void animate() {
            f++;
            if (statey == 1) {
                sprite.setFrame(1).setFrameRow(0);
                if (state == 1 || state == -1) {
                    sprite.setFlipX(state == -1);
                }
            } else {
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

        public void exit(double x, double y) {
            sprite.setPos(this.x = x, this.y = y + 0.5).setFrame(3).setFrameRow(0);
        }
    }

    public static String convertStreamToString(InputStream is) throws IOException {
        if (is != null) {
            Writer writer = new StringWriter();

            char[] buffer = new char[1024];
            try {
                Reader reader = new BufferedReader(
                        new InputStreamReader(is, "UTF-8"));
                int n;
                while ((n = reader.read(buffer)) != -1) {
                    writer.write(buffer, 0, n);
                }
            } finally {
                is.close();
            }
            return writer.toString();
        } else {
            return "";
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

        URL jumpSound = Tiles.class.getResource("/jump.wav");
        AudioInputStream jumpSoundStream = AudioSystem.getAudioInputStream(jumpSound);
        Clip jumpClip = AudioSystem.getClip();
        jumpClip.open(jumpSoundStream);

        URL coinSound = Tiles.class.getResource("/coin.wav");
        AudioInputStream coinSoundStream = AudioSystem.getAudioInputStream(coinSound);
        Clip coinClip = AudioSystem.getClip();
        coinClip.open(coinSoundStream);

        URL winSound = Tiles.class.getResource("/win.wav");
        AudioInputStream winSoundStream = AudioSystem.getAudioInputStream(winSound);
        Clip winClip = AudioSystem.getClip();
        winClip.open(winSoundStream);

        Spriter spriter = new Spriter("Tiles");
        spriter.setBackgroundColor(Color.decode("#D0F4F7"));
        spriter.pause();

        spriter.setViewportWidth(9.75);
        spriter.setViewportHeight(9.75);
        spriter.setViewportShiftX(5 - 0.5);
        spriter.setViewportShiftY(5 - 0.5);

        BufferedImage tilesSpriteSheet = loadImage("/tiles.png");
        BufferedImage playerSpriteSheet = loadImage("/alien.png");
        BufferedImage numbersSpriteSheet = loadImage("/numbers.png");
        BufferedImage hudSpriteSheet = loadImage("/hud.png");

        tileProto = spriter.createSpriteProto(tilesSpriteSheet, 35, 35, 70, 70).setSquareSide(1).setLayer(L_TILE);
        playerProto = spriter.createSpriteProto(playerSpriteSheet, 72 / 2, 97, 72, 97).setWidthProportional(0.75).setLayer(L_PLAYER);
        numbersProto = spriter.createSpriteProto(numbersSpriteSheet, 15, 20, 30, 40).setWidthProportional(0.5).setLayer(L_HUD).setHud(true);
        hudProto = spriter.createSpriteProto(hudSpriteSheet, 25, 25, 50, 50).setWidthProportional(0.7).setLayer(L_HUD).setHud(true);

        hudProto.createGhost().setPos(-4.3, -4.3).setVisible(true).setFrame(0).setFrameRow(0);
        numbersProto.createGhost().setPos(-3.75, -4.3).setVisible(true).setFrame(10).setFrameRow(0);
        Spriter.Sprite n1 = numbersProto.createGhost().setPos(-3.25, -4.3).setVisible(true).setFrame(0).setFrameRow(0);
        Spriter.Sprite n2 = numbersProto.createGhost().setPos(-2.7, -4.3).setVisible(true).setFrame(0).setFrameRow(0);
        hudProto.createGhost().setPos(4.3, -4.3).setVisible(true).setFrame(1).setFrameRow(0);
        hudProto.createGhost().setPos(4.0, -4.3).setVisible(true).setFrame(1).setFrameRow(0);
        hudProto.createGhost().setPos(3.7, -4.3).setVisible(true).setFrame(1).setFrameRow(0);
        Spriter.Sprite hudKey = hudProto.createGhost().setPos(2.6, -4.3).setVisible(true).setFrame(1).setFrameRow(4);

        HashMap<Spriter.Point, Brick> bricks = new HashMap<>();
        HashMap<Spriter.Point, Coin> coins = new HashMap<>();
        HashMap<Spriter.Point, Water> water = new HashMap<>();
        Key key = null;
        Spriter.Sprite doorTop = null;
        Door door = null;
        Player player = null;

        String level = convertStreamToString(Tiles.class.getResource("/level.txt").openStream()).trim();
        int ty = -1;
        int tx = -1;
        for (String row : level.split("\n")) {
            ty++;
            tx = -1;
            for (char c : row.toCharArray()) {
                tx++;

                if (c == '@') {
                    Spriter.Sprite pl = playerProto.createGhost().setFrame(0).setFrameRow(0).setPos(tx, ty + 0.5).setVisible(true);
                    player = new Player(pl, tx, ty + 0.5);
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
                        l = L_SIGN;
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
                        l = L_DOOR;
                        break;
                    case 'a':
                        fx = 6;
                        fy = 1;
                        l = L_DOOR;
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
                    case 'k':
                        fx = 1;
                        fy = 1;
                        break;
                }
                if (fx >= 0 && fy >= 0) {
                    Spriter.Point p = new Spriter.Point(tx, ty);
                    Spriter.Sprite tile = tileProto.createGhost().setFrame(fx).setFrameRow(fy).setPos(p).setVisible(true).setLayer(l);
                    if (c == '#') {
                        bricks.put(p, new Brick(tile, tx, ty, Brick.ROCK));
                    }
                    if (c == '>' || c == '<') {
                        bricks.put(p, new Brick(tile, tx, ty, Brick.WALL));
                    }
                    if (c == 'b' || c == 'd' || c == 'm') {
                        bricks.put(p, new Brick(tile, tx, ty, Brick.GROUND));
                    }
                    if (c == 'x') {
                        bricks.put(p, new Brick(tile, tx, ty, Brick.BOX));
                    }
                    if (c == 'c') {
                        coins.put(p, new Coin(tile, tx, ty));
                    }
                    if (c == '~') {
                        water.put(p, new Water(tile, tx, ty));
                    }
                    if (c == 'k') {
                        key = new Key(tile, tx, ty);
                    }
                    if (c == 'A') {
                        door = new Door(tile, doorTop, tx, ty);
                    }
                    if (c == 'a') {
                        doorTop = tile;
                    }
                }
            }
        }

        for (Map.Entry<Spriter.Point, Brick> entry : bricks.entrySet()) {
            if (entry.getValue().type != Brick.ROCK) {
                continue;
            }
            double x = entry.getKey().getX();
            double y = entry.getKey().getY();
            int fx = 0;
            Brick bleft = bricks.get(new Spriter.Point(x - 1, y));
            Brick bup = bricks.get(new Spriter.Point(x, y - 1));
            Brick bdown = bricks.get(new Spriter.Point(x, y + 1));
            Brick bright = bricks.get(new Spriter.Point(x + 1, y));
            boolean left = bleft != null && bleft.type == Brick.ROCK;
            boolean up = bup != null && bup.type == Brick.ROCK;
            boolean down = bdown != null && bdown.type == Brick.ROCK;
            boolean right = bright != null && bright.type == Brick.ROCK;
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
            entry.getValue().sprite.setFrame(fx);
        }

        mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
        mediaPlayer.setAutoPlay(true);
        mediaPlayer.play();

        Spriter.Control control = spriter.getControl();

        int coinsCount = 0;

        spriter.unpause();

        double vx, vy;

        while (true) {
            spriter.beginFrame();

            for (Water w : water.values()) {
                w.animate();
            }
            for (Coin c : coins.values()) {
                c.animate();
            }
            if (player != null) {

                double pl = player.x - player.sprite.getWidth() / 2 + 0.25;
                double pr = player.x + player.sprite.getWidth() / 2 - 0.25;
                double plf = player.x - player.sprite.getWidth() / 2 + 0.3;
                double prf = player.x + player.sprite.getWidth() / 2 - 0.3;
                double plh = player.x - player.sprite.getWidth() / 2 + 0.5;
                double prh = player.x + player.sprite.getWidth() / 2 - 0.5;
                double pt = player.y - player.sprite.getHeight() + 0.05;
                double pb = player.y;
                double pct = player.y - player.sprite.getHeight() + 0.2;
                double pcb = player.y - 0.3;
                double pm = player.y - player.sprite.getHeight() / 3;
                double pml = player.x - player.sprite.getWidth() / 2 + 0.15;
                double pmr = player.x + player.sprite.getWidth() / 2 - 0.15;

                Brick dl = bricks.get(new Spriter.Point(Math.round(pl), Math.round(pb)));
                Brick dr = bricks.get(new Spriter.Point(Math.round(pr), Math.round(pb)));
                Brick ul = bricks.get(new Spriter.Point(Math.round(pl), Math.round(pt)));
                Brick ur = bricks.get(new Spriter.Point(Math.round(pr), Math.round(pt)));
                Brick ulh = bricks.get(new Spriter.Point(Math.round(plh), Math.round(pt)));
                Brick urh = bricks.get(new Spriter.Point(Math.round(prh), Math.round(pt)));
                Brick dlf = bricks.get(new Spriter.Point(Math.round(plf), Math.round(pb)));
                Brick drf = bricks.get(new Spriter.Point(Math.round(prf), Math.round(pb)));
                Brick ml = bricks.get(new Spriter.Point(Math.round(pml), Math.round(pm)));
                Brick mr = bricks.get(new Spriter.Point(Math.round(pmr), Math.round(pm)));
                Coin cul = coins.get(new Spriter.Point(Math.round(pml), Math.round(pct)));
                Coin cur = coins.get(new Spriter.Point(Math.round(pmr), Math.round(pct)));
                Coin cdl = coins.get(new Spriter.Point(Math.round(pml), Math.round(pcb)));
                Coin cdr = coins.get(new Spriter.Point(Math.round(pmr), Math.round(pcb)));
                boolean stands = false;
                if (dl == null && dr == null) {
                    player.fly(1, -1);
                } else if (dl != null && dlf == null) {
                    player.x += 0.01;
                    player.fly(0, player.y + 0.01);
                } else if (dr != null && drf == null) {
                    player.x -= 0.01;
                    player.fly(0, player.y + 0.01);
                } else {
                    player.fly(0, Math.round(pb) - 0.5);
                    stands = true;
                }

                if (player.vy < 0) {
                    if (player.vx < -0.03) {
                        if (urh != null || ul != null) {
                            player.vy = 0;
                        }
                    } else if (player.vx > 0.03) {
                        if (ulh != null || ur != null) {
                            player.vy = 0;
                        }
                    } else {
                        if (ul != null || ur != null) {
                            player.vy = 0;
                        }
                    }
                }

                if (ml != null) {
                    player.x = ml.x + 0.5 + player.sprite.getWidth() / 2 - 0.15;
                }
                if (mr != null) {
                    player.x = mr.x - 0.5 - player.sprite.getWidth() / 2 + 0.15;
                }
                if (control.isKeyDown(KeyEvent.VK_RIGHT) && mr == null) {
                    player.go(1);
                } else if (control.isKeyDown(KeyEvent.VK_LEFT) && ml == null) {
                    player.go(-1);
                } else {
                    player.go(0);
                }

                Integer press = control.getKeyPress();
                if (stands && press != null && press == KeyEvent.VK_UP) {
                    player.jump(-0.095);
                    jumpClip.stop();
                    jumpClip.setMicrosecondPosition(0);
                    jumpClip.flush();
                    jumpClip.start();
                }
                if (!key.isTaken() && (key.x == Math.round(pml) || key.x == Math.round(pmr)) && (key.y == Math.round(pct) || key.y == Math.round(pct))) {
                    key.take();
                    hudKey.setFrame(0);
                    coinClip.stop();
                    coinClip.setMicrosecondPosition(0);
                    coinClip.flush();
                    coinClip.start();
                }

                if (player.y > ty + 5) {
                    player.reset();
                }

                if (cul != null && cul.take() ||
                        cur != null && cur.take() ||
                        cdl != null && cdl.take() ||
                        cdr != null && cdr.take()) {
                    coinsCount++;
                    n1.setFrame(coinsCount / 10);
                    n2.setFrame(coinsCount % 10);
                    coinClip.stop();
                    coinClip.setMicrosecondPosition(0);
                    coinClip.flush();
                    coinClip.start();
                }

                player.animate();

                vx = (Math.min(Math.max(5 - 0.5, player.x), tx - 5 + 0.5));
                vy = (Math.min(Math.max(5 - 0.5, player.y), ty - 5 + 0.5));
                spriter.setViewportShiftX(vx);
                spriter.setViewportShiftY(vy);

                if (key.isTaken() && Math.sqrt(Math.pow(player.x - door.x, 2) + Math.pow(player.y - 0.5 - door.y, 2)) < 0.3) {
                    player.exit(door.x, door.y);
                    door.open();
                    winClip.stop();
                    winClip.setMicrosecondPosition(0);
                    winClip.flush();
                    winClip.start();
                    break;
                }
            }

            spriter.endFrame();
            Thread.sleep(30);
        }
        spriter.endFrame();

        BufferedImage shadowImage = new BufferedImage(1, 1, BufferedImage.TYPE_4BYTE_ABGR);
        shadowImage.setRGB(0, 0, 120 << 24);
        Spriter.Sprite shadow = spriter.createSprite(shadowImage, 0.5, 0.5, 10, 10).setLayer(L_SHADOW).setHud(true);
        Spriter.Sprite win = spriter.createSprite(loadImage("/win.png"), 314 / 2, 139 / 2, 4).setLayer(L_WIN).setHud(true);

        int f = 0;
        while (true) {
            spriter.beginFrame();
            win.setWidthProportional(4 + Math.sin(0.2 * f++));
            spriter.endFrame();
            Thread.sleep(60);
        }
    }
}
