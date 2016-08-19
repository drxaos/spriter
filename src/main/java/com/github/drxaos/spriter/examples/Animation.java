package com.github.drxaos.spriter.examples;

import com.github.drxaos.spriter.Spriter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class Animation {

    public static BufferedImage loadImage(String name) throws IOException {
        return ImageIO.read(Animation.class.getResource(name));
    }

    public static void main(String[] args) throws IOException, InterruptedException {

        Spriter spriter = new Spriter("Animation");
        Spriter.Sprite characterProto = spriter.createSprite(loadImage("/sprite-steps.png"), 50 / 2, 72, 50, 72, 0.2).setVisible(false);

        Spriter.Sprite characters[] = new Spriter.Sprite[10];
        for (int i = 0; i < characters.length; i++) {
            double a = Math.PI / 5 * i;
            characters[i] = characterProto.createGhost()
                    .setPos(Math.sin(a) * 0.5, -Math.cos(a) * 0.5)
                    .setAngle(a)
                    .setVisible(true);
        }

        int frame = -1, x = 0;
        while (true) {
            frame++;
            characters[x].setFrame(frame % 10);
            if (frame == 10) {
                frame = -1;
                x = (int) (Math.random() * 10);
            }
            Thread.sleep(100);
        }
    }

}
