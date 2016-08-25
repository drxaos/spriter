package com.github.drxaos.spriter.examples;

import com.github.drxaos.spriter.Spriter;

import javax.imageio.ImageIO;

public class Test {
    public static void main(String[] args) throws Exception {
        new Spriter("Test")
                .createSprite(ImageIO.read(Animation.class.getResource("/100.jpg")), 50, 50, 0.2).setPos(0.1, 0)
                .createGhost().setPos(-0.1, 0);
    }
}
