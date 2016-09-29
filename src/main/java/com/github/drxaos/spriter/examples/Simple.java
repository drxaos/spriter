package com.github.drxaos.spriter.examples;

import com.github.drxaos.spriter.Spriter;

import javax.imageio.ImageIO;

public class Simple {
    public static void main(String[] args) throws Exception {
        Spriter spriter = new Spriter("Simple");

        Spriter.Sprite sprite = spriter.createProto(
                ImageIO.read(Animation.class.getResource("/point.png")),
                256 / 2, 256 / 2   // sprite center
        ).newInstance(0.2); // object size

        Spriter.Control control = spriter.getControl();

        while (true) {
            sprite.setPos(control.getMousePos());
            spriter.render();
            Thread.sleep(30);
        }
    }
}
