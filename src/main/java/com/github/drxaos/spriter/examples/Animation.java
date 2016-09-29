package com.github.drxaos.spriter.examples;

import com.github.drxaos.spriter.Spriter;
import com.github.drxaos.spriter.SpriterUtils;

import java.awt.image.BufferedImage;
import java.io.IOException;

public class Animation {

    public static BufferedImage loadImage(String name) throws IOException {
        BufferedImage image = SpriterUtils.loadImageFromResource(name);
        return SpriterUtils.scaleImage(image, image.getWidth() * 10, image.getHeight() * 10, true);
    }

    public static void main(String[] args) throws IOException, InterruptedException {

        Spriter spriter = new Spriter("Animation");
        Spriter.Proto characterProto = spriter.createProto(loadImage("/sprite-steps.png"), 50 / 2 * 10, 72 * 10, 50 * 10, 72 * 10);

        Spriter.Sprite characters[] = new Spriter.Sprite[10];
        for (int i = 0; i < characters.length; i++) {
            double a = Math.PI / 5 * i;
            characters[i] = characterProto.newInstance(0.2)
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
            spriter.render();
            Thread.sleep(100);
        }
    }

}
