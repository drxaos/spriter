package com.github.drxaos.spriter.examples;

import com.github.drxaos.spriter.Sprite;
import com.github.drxaos.spriter.Spriter;
import com.github.drxaos.spriter.Utils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.IOException;

public class Pixels {

    static final int L_TILES = 100;
    static final int L_PENGUIN = 200;

    public static BufferedImage loadImage(String name) throws IOException {
        BufferedImage image = Utils.loadImageFromResource(name);
        BufferedImage aimage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        aimage.getGraphics().drawImage(image, 0, 0, null);
        int[] bufferbyte = ((DataBufferInt) aimage.getRaster().getDataBuffer()).getData();
        for (int i = 0; i < bufferbyte.length; i++) {
            if (bufferbyte[i] == 0xFFFF00FF) {
                bufferbyte[i] = 0;
            }
        }
        BufferedImage scaled = Utils.scaleImage(aimage, aimage.getWidth() * 10, aimage.getHeight() * 10, false);
        return scaled;
    }

    public static void main(String[] args) throws IOException, InterruptedException {

        Spriter spriter = Spriter.createDefault("Pixels");
        spriter.getRenderer().setAntialiasing(false);
        spriter.getRenderer().setBilinearInterpolation(false);

        spriter.setBackgroundColor(Color.decode("#D0F4F7"));
        spriter.setViewportShiftY(-0.3);
        BufferedImage tilesImage = loadImage("/penguin.png");
        BufferedImage iglooImage = tilesImage.getSubimage(1100, 0, 880, 440);
        Sprite character = spriter.createProto(tilesImage, 110, 210, 220, 220).newInstance(0.2).setZ(L_PENGUIN);
        Sprite tile = character.newInstance().setFrame(0).setFrameRow(1).setY(0.2).setVisible(false).setZ(L_TILES);
        for (double x = -1; x <= 1; x += 0.2) {
            tile.newInstance().setX(x).setVisible(true);
        }
        tile.newInstance().setPos(-0.3, -0.0).setFrame(1).setVisible(true);
        tile.newInstance().setPos(-0.3, -0.2).setFrame(2).setVisible(true);

        Sprite igloo = spriter.createProto(iglooImage, 110, 430).newInstance(0.8).setZ(L_TILES);

        int f = 0, st = 1;
        double x = 0;
        while (true) {
            spriter.beginFrame();

            f++;
            x += 0.01 * st;
            if (x > 0.9) {
                st = -1;
            }
            if (x < -0.9) {
                st = 1;
            }
            character.setFrame(((f / 5) % 4) + 1).setX(x).setFlipX(st < 0);

            spriter.endFrame();
        }
    }

}
