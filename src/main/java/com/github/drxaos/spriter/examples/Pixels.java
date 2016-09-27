package com.github.drxaos.spriter.examples;

import com.github.drxaos.spriter.Spriter;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.*;
import java.io.IOException;

public class Pixels {

    static final int L_TILES = 100;
    static final int L_PENGUIN = 200;

    public static BufferedImage loadImage(String name) throws IOException {
        BufferedImage image = ImageIO.read(Animation.class.getResource(name));
        BufferedImage aimage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        aimage.getGraphics().drawImage(image, 0, 0, null);
        int[] bufferbyte = ((DataBufferInt) aimage.getRaster().getDataBuffer()).getData();
        for (int i = 0; i < bufferbyte.length; i++) {
            if (bufferbyte[i] == 0xFFFF00FF) {
                bufferbyte[i] = 0;
            }
        }
        return aimage;
    }

    public static void main(String[] args) throws IOException, InterruptedException {

        Spriter spriter = new Spriter("Pixels");
        spriter.getRenderer().setAntialiasing(false);
        spriter.getRenderer().setBilinearInterpolation(false);

        spriter.setSmoothScaling(false);
        spriter.setBackgroundColor(Color.decode("#D0F4F7"));
        spriter.setViewportShiftY(-0.3);
        BufferedImage tilesImage = loadImage("/penguin.png");
        BufferedImage iglooImage = tilesImage.getSubimage(110, 0, 88, 44);
        Spriter.Sprite character = spriter.createSprite(tilesImage, 11, 21, 22, 22, 0.2).setLayer(L_PENGUIN);
        Spriter.Sprite tile = character.createGhost().setFrame(0).setFrameRow(1).setY(0.2).setVisible(false).setLayer(L_TILES);
        for (double x = -1; x <= 1; x += 0.2) {
            tile.clone().setX(x).setVisible(true);
        }
        tile.clone().setPos(-0.3, -0.0).setFrame(1).setVisible(true);
        tile.clone().setPos(-0.3, -0.2).setFrame(2).setVisible(true);

        Spriter.Sprite igloo = spriter.createSprite(iglooImage, 11, 43, 0.8).setLayer(L_TILES);

        int f = 0, st = 1;
        double x = 0;
        while (true) {
            f++;
            x += 0.01 * st;
            if (x > 0.9) {
                st = -1;
            }
            if (x < -0.9) {
                st = 1;
            }
            character.setFrame(((f / 5) % 4) + 1).setX(x).setFlipX(st < 0);
            Thread.sleep(30);
        }
    }

}
