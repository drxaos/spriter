package com.github.drxaos.spriter.examples;

import com.github.drxaos.spriter.Spriter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class Groups {

    public static BufferedImage loadImage(String name) throws IOException {
        return ImageIO.read(Groups.class.getResource(name));
    }

    public static void main(String[] args) throws IOException, InterruptedException {

        BufferedImage tankSprite = loadImage("/tank1.png");
        BufferedImage wheelSprite = tankSprite.getSubimage(0, 0, 159, 304);
        BufferedImage bodySprite = tankSprite.getSubimage(162, 0, 648, 584);
        BufferedImage turretSprite = tankSprite.getSubimage(651, 5, 887, 546);

        Spriter spriter = new Spriter("Groups");

        Spriter.Sprite wheelProto = spriter.createSprite(wheelSprite, 160/2, 304/2, 0.05).setVisible(true);
        Spriter.Sprite bodyProto = spriter.createSprite(wheelSprite, 160/2, 304/2, 0.05).setVisible(true);
        Spriter.Sprite turretProto = spriter.createSprite(wheelSprite, 160/2, 304/2, 0.05).setVisible(true);

//        Spriter.Sprite characters[] = new Spriter.Sprite[10];
//        for (int i = 0; i < characters.length; i++) {
//            double a = Math.PI / 5 * i;
//            characters[i] = characterProto.createGhost()
//                    .setPos(Math.sin(a) * 0.5, -Math.cos(a) * 0.5)
//                    .setAngle(a)
//                    .setVisible(true);
//        }
//
//        int frame = -1, x = 0;
//        while (true) {
//            frame++;
//            characters[x].setFrame(frame % 10);
//            if (frame == 10) {
//                frame = -1;
//                x = (int) (Math.random() * 10);
//            }
//            Thread.sleep(100);
//        }
    }

}
