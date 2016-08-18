package com.github.drxaos.spriter.examples;

import com.github.drxaos.spriter.Spriter;

import javax.imageio.ImageIO;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class Fighter {

    public static final int LAYER_BG = 0;
    public static final int LAYER_OBJ = 500;
    public static final int LAYER_HL = 900;
    public static final int LAYER_HUD = 1000;

    public static BufferedImage loadImage(String name) throws IOException {
        return ImageIO.read(Fighter.class.getResource(name));
    }

    public static void main(String[] args) throws IOException, InterruptedException {

        Spriter spriter = new Spriter("Test");
        Spriter.Sprite cur1 = spriter.createSprite(loadImage("/cur1.png"), 7, 7, 0.08).setLayer(LAYER_HUD);
        Spriter.Sprite cur2 = spriter.createSprite(loadImage("/cur2.png"), 7, 7, 0.08).setLayer(LAYER_HUD);
        Spriter.Sprite fighter = spriter.createSprite(loadImage("/fighter-01.png"), 720 / 2, 713 / 2, 0.25).setLayer(LAYER_OBJ);
        Spriter.Sprite target1 = spriter.createSprite(loadImage("/target.png"), 125, 125, 0.1).setLayer(LAYER_OBJ);
        Spriter.Sprite target2 = target1.createGhost();
        Spriter.Sprite point = spriter.createSprite(loadImage("/point.png"), 1280 / 2, 1280 / 2, 1).setLayer(LAYER_OBJ);
        Spriter.Control control = spriter.getControl();

        point.setVisible(false);
        int pointSize = 0;

        double x = 0, y = 0, a = 0;
        while (true) {
            cur1.setX(control.getMouseX());
            cur1.setY(control.getMouseY());
            cur2.setX(control.getMouseX());
            cur2.setY(control.getMouseY());
            cur1.setVisible(!control.isButtonDown(MouseEvent.BUTTON1));
            cur2.setVisible(control.isButtonDown(MouseEvent.BUTTON1));

            Spriter.Point click = control.getClick();
            if (click != null) {
                pointSize = 0;
                point.setVisible(true);
                point.setX(click.getX());
                point.setY(click.getY());
            }
            if (pointSize < 5) {
                pointSize++;
            }
            point.setWidth(0.01 * pointSize);
            point.setHeight(0.01 * pointSize);

            a += 0.01;
            x = Math.cos(a);
            y = Math.sin(a);

            fighter.setAngle(a + Math.PI);
            fighter.setX(x);
            fighter.setY(y);

            target1.setX(x);
            target2.setY(y);

            Thread.sleep(40);
        }
    }

}
