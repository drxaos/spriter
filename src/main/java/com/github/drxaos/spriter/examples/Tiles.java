package com.github.drxaos.spriter.examples;

import com.github.drxaos.spriter.Spriter;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class Tiles {
    public static final int L_COIN = 200;
    public static final int L_PLAYER = 300;
    public static final int L_WATER = 400;
    public static final int L_BOX = 450;
    public static final int L_TILE = 500;

    public static BufferedImage loadImage(String name) throws IOException {
        return ImageIO.read(Tiles.class.getResource(name));
    }

    static Spriter.Sprite tileProto;

    public static void main(String[] args) throws Exception {

        Spriter spriter = new Spriter("Tiles");
        spriter.setViewportWidth(9.75);
        spriter.setViewportHeight(9.75);
        spriter.setViewportShiftX(5 - 0.5);
        spriter.setViewportShiftY(5 - 0.5);

        spriter.setBackgroundColor(Color.decode("#D0F4F7"));

        BufferedImage tilesSpriteSheet = loadImage("/tiles.png");

        tileProto = spriter.createSpriteProto(tilesSpriteSheet, 35, 35, 70, 70).setSquareSide(1).setLayer(L_TILE);

        HashMap<Spriter.Point, Spriter.Sprite> bricks = new HashMap<>();

        String level = new String(Files.readAllBytes(Paths.get(Tiles.class.getResource("/level.txt").toURI())));
        int ty = -1;
        for (String row : level.split("\n")) {
            ty++;
            int tx = -1;
            for (char c : row.toCharArray()) {
                tx++;
                int fx = -1, fy = -1, l = L_TILE;
                switch (c) {
                    case 'm':
                        fx = 0;
                        fy = 1;
                        break;
                    case '~':
                        fx = 1;
                        fy = 1;
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
                        fx = 7;
                        fy = 1;
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

        while (true) {
            Thread.sleep(30);
        }
    }
}
