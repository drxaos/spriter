package com.github.drxaos.spriter.examples;

import com.github.drxaos.spriter.Spriter;

import javax.imageio.ImageIO;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class Composition {
    public static final int L_TWHEEL = 500;
    public static final int L_TBODY = 501;
    public static final int L_TTURRET = 502;

    public static BufferedImage loadImage(String name) throws IOException {
        return ImageIO.read(Composition.class.getResource(name));
    }

    static Spriter.Sprite wheelProto, turretProto, bodyProto;

    static class Tank {
        Spriter.Sprite sWheelFR, sWheelFL, sWheelBR, sWheelBL,
                sTurret, sBody;

        double wheelsAngle = 0;
        double turretAngle = 0;
        double x = 0, y = 0, a = 0;

        public Tank() {
            sBody = bodyProto.clone().setPos(0, 0).setWidthProportional(0.2).setVisible(true);
            sWheelFL = wheelProto.clone().setParent(sBody).setPos(-0.085, -0.08).setWidthProportional(0.06).setVisible(true);
            sWheelFR = wheelProto.clone().setParent(sBody).setPos(0.085, -0.08).setWidthProportional(0.06).setVisible(true);
            sWheelBL = wheelProto.clone().setParent(sBody).setPos(-0.085, 0.08).setWidthProportional(0.06).setVisible(true);
            sWheelBR = wheelProto.clone().setParent(sBody).setPos(0.085, 0.08).setWidthProportional(0.06).setVisible(true);
            sTurret = turretProto.clone().setParent(sBody).setPos(0, 0.04).setWidthProportional(0.1).setVisible(true);
        }

        public void moveForward(double l) {
            a += l * wheelsAngle * 15;
            x += Math.sin(a) * l;
            y -= Math.cos(a) * l;
            sBody.setPos(x, y).setAngle(a);
        }

        public void setTurretAngle(double a) {
            sTurret.setAngle(a);
            turretAngle = a;
        }

        public void setWheelsAngle(double a) {
            sWheelFL.setAngle(a);
            sWheelFR.setAngle(a);
            wheelsAngle = a;
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {

        BufferedImage tankSprite = loadImage("/tank1.png");
        BufferedImage wheelSprite = tankSprite.getSubimage(0, 0, 160, 304);
        BufferedImage bodySprite = tankSprite.getSubimage(162, 0, 486, 584);
        BufferedImage turretSprite = tankSprite.getSubimage(651, 5, 236, 541);

        Spriter spriter = new Spriter("Composition");

        wheelProto = spriter.createSpriteProto(wheelSprite, 160 / 2, 304 / 2).setLayer(L_TWHEEL);
        bodyProto = spriter.createSpriteProto(bodySprite, 486 / 2, 584 / 2).setLayer(L_TBODY);
        turretProto = spriter.createSpriteProto(turretSprite, 236 / 2, 541 - 100).setLayer(L_TTURRET);

        Spriter.Control control = spriter.getControl();

        Tank tank = new Tank();

        double t = 0;
        double w = 0;
        double a = 0;
        while (true) {
            t += 0.1;
            tank.setTurretAngle(Math.PI / 4 * Math.sin(t));

            if (control.isKeyDown(KeyEvent.VK_LEFT) && w > -Math.PI / 2) {
                w -= 0.25;
            } else if (control.isKeyDown(KeyEvent.VK_RIGHT) && w < Math.PI / 2) {
                w += 0.25;
            } else if (w < -0.05) {
                w += 0.25;
            } else if (w > 0.05) {
                w -= 0.25;
            }
            tank.setWheelsAngle(Math.PI / 6 * Math.sin(w));

            if (control.isKeyDown(KeyEvent.VK_UP) && a < 0.02) {
                a += 0.004;
            } else if (a > 0) {
                a -= 0.004;
            }
            tank.moveForward(a);

            Thread.sleep(50);
        }
    }

}
