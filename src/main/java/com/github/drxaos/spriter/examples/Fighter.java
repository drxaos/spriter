package com.github.drxaos.spriter.examples;

import com.github.drxaos.spriter.Sprite;
import com.github.drxaos.spriter.SpriterControl;
import com.github.drxaos.spriter.SpriterPoint;
import com.github.drxaos.spriter.SpriterWindow;

import javax.imageio.ImageIO;
import java.awt.event.MouseEvent;
import java.io.IOException;

public class Fighter {

    public static final int LAYER_BG = 0;
    public static final int LAYER_OBJ = 500;
    public static final int LAYER_HL = 900;
    public static final int LAYER_HUD = 1000;

    public static void main(String[] args) throws IOException, InterruptedException {

        SpriterWindow window = new SpriterWindow("Test");
        Sprite cur1 = new Sprite(ImageIO.read(Fighter.class.getResource("/cur1.png")), 7, 7, 0.256 * 0.3, 0.256 * 0.3, LAYER_HUD);
        Sprite cur2 = new Sprite(ImageIO.read(Fighter.class.getResource("/cur2.png")), 7, 7, 0.256 * 0.3, 0.256 * 0.3, LAYER_HUD);
        Sprite fighter = new Sprite(ImageIO.read(Fighter.class.getResource("/fighter-01.png")), 720 / 2, 713 / 2, 0.720 * 0.3, 0.713 * 0.3, LAYER_OBJ);
        Sprite target1 = new Sprite(ImageIO.read(Fighter.class.getResource("/target.png")), 125, 125, 0.1, 0.1, LAYER_OBJ);
        Sprite target2 = new Sprite(ImageIO.read(Fighter.class.getResource("/target.png")), 125, 125, 0.1, 0.1, LAYER_OBJ);
        Sprite point = new Sprite(ImageIO.read(Fighter.class.getResource("/point.png")), 1280 / 2, 1280 / 2, 0.1280, 0.1280, LAYER_OBJ);
        window.addSprite(cur1);
        window.addSprite(cur2);
        window.addSprite(point);
        window.addSprite(fighter);
        window.addSprite(target1);
        window.addSprite(target2);
        SpriterControl control = window.getControl();

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

            SpriterPoint click = control.getClick();
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
