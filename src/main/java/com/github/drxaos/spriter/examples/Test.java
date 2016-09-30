package com.github.drxaos.spriter.examples;

import com.github.drxaos.spriter.Spriter;

import javax.imageio.ImageIO;

public class Test {
    public static void main(String[] args) throws Exception {
        Spriter spriter = new Spriter("Test");
        spriter.createProto(ImageIO.read(Animation.class.getResource("/100.jpg")), 50, 50).newInstance(0.2).setPos(0.1, 0)
                .newInstance().setPos(-0.1, 0);
        while (true) {
            spriter.beginFrame();
            spriter.endFrame();
        }
    }
}
