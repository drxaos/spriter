package com.github.drxaos.spriter.examples;

import com.github.drxaos.spriter.Proto;
import com.github.drxaos.spriter.Sprite;
import com.github.drxaos.spriter.Spriter;
import com.github.drxaos.spriter.Utils;

import java.io.IOException;

public class Animation2 {

    public static void main(String[] args) throws IOException, InterruptedException {

        Spriter spriter = Spriter.createDefault("Animation 2");

        Proto fireProto = spriter.createProto(Utils.loadImageFromResource("/fire.jpg"), 320 / 2, 320 / 2, 320, 320);
        Sprite fire = fireProto.newInstance(2, 2);

        int frame = 0;
        while (true) {
            spriter.beginFrame();

            frame++;
            fire.setFrame(frame % 6, frame % 36 / 6);

            spriter.endFrame();
        }
    }

}
