package com.github.drxaos.spriter.examples;

import com.github.drxaos.spriter.Proto;
import com.github.drxaos.spriter.Sprite;
import com.github.drxaos.spriter.Spriter;
import com.github.drxaos.spriter.Utils;

import java.awt.image.BufferedImage;
import java.io.IOException;

public class Animation {

    public static BufferedImage loadImage(String name) throws IOException {
        BufferedImage image = Utils.loadImageFromResource(name);
        return Utils.scaleImage(image, image.getWidth() * 10, image.getHeight() * 10, true);
    }

    public static void main(String[] args) throws IOException, InterruptedException {

        Spriter spriter = Spriter.createDefault("Animation");
        Proto characterProto = spriter.createProto(loadImage("/sprite-steps.png"), 50 / 2 * 10, 72 * 10, 50 * 10, 72 * 10);

        Sprite characters[] = new Sprite[10];
        for (int i = 0; i < characters.length; i++) {
            double a = Math.PI / 5 * i;
            characters[i] = characterProto.newInstance(0.2)
                    .setPos(Math.sin(a) * 0.5, -Math.cos(a) * 0.5)
                    .setAngle(a)
                    .setVisible(true);
        }

        int frame = -1, x = 0;
        while (true) {
            spriter.beginFrame();

            frame++;
            characters[x].setFrame(frame % 10);
            if (frame == 10) {
                frame = -1;
                x = (int) (Math.random() * 10);
            }

            spriter.endFrame();
        }
    }

}
