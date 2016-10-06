package com.github.drxaos.spriter.examples;

import com.github.drxaos.spriter.Proto;
import com.github.drxaos.spriter.Sprite;
import com.github.drxaos.spriter.Spriter;

import javax.imageio.ImageIO;

public class Test {
    public static void main(String[] args) throws Exception {
        Spriter spriter = Spriter.createDefault("Test");
        spriter.setViewportAngle(-Math.PI / 10);
        spriter.setViewportWidth(1000);
        spriter.setViewportHeight(1000);

        Proto proto = spriter.createProto(ImageIO.read(Animation.class.getResource("/100.jpg")), 50, 50);
        Sprite sprite1 = proto.newInstance(100,50).setPos(200, -100).setAngle(Math.PI /2);
        Sprite sprite2 = proto.newInstance(100,50).setPos(100, 0).setParent(sprite1);
        Sprite sprite3 = proto.newInstance(100,50).setPos(100, 0).setAngle(Math.PI).setParent(sprite2);

        while (true) {
            spriter.beginFrame();
            spriter.endFrame();
        }
    }
}
