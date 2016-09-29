package com.github.drxaos.spriter.examples;

import com.github.drxaos.spriter.Spriter;

import javax.imageio.ImageIO;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class Fighter {

    public static final int LAYER_BG = 0;
    public static final int LAYER_GROUND = 400;
    public static final int LAYER_AIR = 500;
    public static final int LAYER_TOP = 900;
    public static final int LAYER_HUD = 1000;

    public static BufferedImage loadImage(String name) throws IOException {
        return ImageIO.read(Fighter.class.getResource(name));
    }

    public static void main(String[] args) throws IOException, InterruptedException {

        Spriter spriter = new Spriter("Fighter");
        Spriter.Sprite cur1 = spriter.createProto(loadImage("/cur1.png"), 7, 7).newInstance(0.08).setLayer(LAYER_HUD);
        Spriter.Sprite cur2 = spriter.createProto(loadImage("/cur2.png"), 7, 7).newInstance(0.08).setLayer(LAYER_HUD);
        Spriter.Sprite fighter = spriter.createProto(loadImage("/fighter-01.png").getSubimage(3, 3, 713, 705), 354, 420).newInstance(0.25).setLayer(LAYER_AIR);
        Spriter.Sprite target1 = spriter.createProto(loadImage("/target.png"), 125, 125).newInstance(0.1).setLayer(LAYER_TOP);
        Spriter.Sprite target2 = target1.newInstance();
        Spriter.Sprite point = spriter.createProto(loadImage("/point.png"), 256 / 2, 256 / 2).newInstance(1).setLayer(LAYER_GROUND);
        Spriter.Control control = spriter.getControl();

        point.setVisible(false);
        int pointSize = 0;

        double x = 0, y = 0, a = 0;
        while (true) {
            cur1.setPos(control.getMousePos()).setVisible(!control.isButtonDown(MouseEvent.BUTTON1));
            cur2.setPos(control.getMousePos()).setVisible(control.isButtonDown(MouseEvent.BUTTON1));

            Spriter.Click click = control.getClick();
            if (click != null) {
                pointSize = 0;
                point.setVisible(true).setPos(click);
            }
            if (pointSize < 8) {
                pointSize++;
            }
            point.setSquareSide(0.01 * pointSize);

            a += 0.01;
            x = Math.cos(a);
            y = Math.sin(a);

            fighter.setAngle(a + Math.PI);
            fighter.setPos(x, y);

            target1.setX(x);
            target2.setY(y);

            spriter.render();
            Thread.sleep(40);
        }
    }

}
