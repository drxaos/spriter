package com.github.drxaos.spriter.examples;

import com.github.drxaos.spriter.Spriter;

import javax.imageio.ImageIO;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

public class Composition {
    public static final int L_TWHEEL = 500;
    public static final int L_TBODY = 501;
    public static final int L_TTURRET = 502;
    public static final int L_TBULLET = 503;
    public static final int L_HUD_CURSOR = 1999;

    public static BufferedImage loadImage(String name) throws IOException {
        return ImageIO.read(Composition.class.getResource(name));
    }

    static Spriter.Sprite turretProto;
    static Spriter.Sprite bulletProto;
    static Spriter.Sprite bodyProto;
    static Spriter.Sprite wheelFLProto;
    static Spriter.Sprite wheelFRProto;
    static Spriter.Sprite wheelBLProto;
    static Spriter.Sprite wheelBRProto;

    static class Bullet {
        Spriter.Sprite sBullet;
        double x, y, a, v;

        public Bullet(double x, double y, double a, double v) {
            this.x = x;
            this.y = y;
            this.a = a;
            this.v = v;
            sBullet = bulletProto.clone().setPos(x, y).setAngle(a).setWidthProportional(0.035).setVisible(true);
        }

        public boolean move() {
            x += Math.cos(a) * v;
            y += Math.sin(a) * v;
            sBullet.setPos(x, y);
            return x > -2 && x < 2 && y > -2 && y < 2;
        }

        public void destroy() {
            sBullet.remove();
        }
    }

    static class Tank {
        Spriter.Sprite sWheelFR, sWheelFL, sWheelBR, sWheelBL,
                sTurret, sBody;

        double wheelsAngle = 0;
        double turretAngle = 0;
        double x = 0, y = 0, a = 0;

        public Tank() {
            sBody = bodyProto.clone().setPos(0, 0).setWidthProportional(0.2).setVisible(true);
            sWheelFL = wheelFLProto.clone().setParent(sBody).setPos(-0.085, -0.08).setWidthProportional(0.06).setVisible(true);
            sWheelFR = wheelFRProto.clone().setParent(sBody).setPos(0.085, -0.08).setWidthProportional(0.06).setVisible(true);
            sWheelBL = wheelBLProto.clone().setParent(sBody).setPos(-0.085, 0.08).setWidthProportional(0.06).setVisible(true);
            sWheelBR = wheelBRProto.clone().setParent(sBody).setPos(0.085, 0.08).setWidthProportional(0.06).setVisible(true);
            sTurret = turretProto.clone().setParent(sBody).setPos(0, 0.04).setWidthProportional(0.1).setVisible(true);
        }

        public void moveForward(double l) {
            a += l * wheelsAngle * 12;
            x += Math.sin(a) * l;
            y -= Math.cos(a) * l;
            a -= l * wheelsAngle * 4;
            sBody.setPos(x, y).setAngle(a);
        }

        public void moveTurretTo(Spriter.Point target) {
            double angle = getAngle(target);
            double ad = turretAngle - angle - Math.PI / 2;
            while (ad < Math.PI) {
                ad += Math.PI * 2;
            }
            while (ad > Math.PI) {
                ad -= Math.PI * 2;
            }
            if (ad < -0.05) {
                turretAngle += 0.05;
            } else if (ad > 0.05) {
                turretAngle -= 0.05;
            } else {
                turretAngle -= ad;
            }

            if (turretAngle - a > Math.PI / 4) {
                turretAngle = a + Math.PI / 4;
            }
            if (turretAngle - a < -Math.PI / 4) {
                turretAngle = a - Math.PI / 4;
            }
            sTurret.setAngle(turretAngle - a);
        }

        public void setWheelsAngle(double a) {
            sWheelFL.setAngle(a);
            sWheelFR.setAngle(a);
            wheelsAngle = a;
        }

        public double getAngle(Spriter.Point target) {
            return Math.atan2(target.getY() - y, target.getX() - x);
        }

        public Bullet fire() {
            double angle = turretAngle - Math.PI / 2 + (Math.random() - 0.5) * Math.PI / 25;
            return new Bullet(x + Math.cos(angle) * 0.09, y + Math.sin(angle) * 0.09, angle, 0.12 + Math.random() * 0.04);
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {

        Spriter spriter = new Spriter("Composition");

        BufferedImage tankSpriteSheet = loadImage("/tank1.png");
        BufferedImage wheelFLSprite = tankSpriteSheet.getSubimage(0, 0, 161, 304);
        BufferedImage wheelFRSprite = tankSpriteSheet.getSubimage(169, 0, 163, 304);
        BufferedImage wheelBLSprite = tankSpriteSheet.getSubimage(0, 305, 162, 306);
        BufferedImage wheelBRSprite = tankSpriteSheet.getSubimage(169, 307, 163, 306);

        wheelFLProto = spriter.createSpriteProto(wheelFLSprite, 80, 152).setLayer(L_TWHEEL);
        wheelFRProto = spriter.createSpriteProto(wheelFRSprite, 80, 152).setLayer(L_TWHEEL);
        wheelBLProto = spriter.createSpriteProto(wheelBLSprite, 80, 152).setLayer(L_TWHEEL);
        wheelBRProto = spriter.createSpriteProto(wheelBRSprite, 80, 152).setLayer(L_TWHEEL);

        bodyProto = spriter.createSpriteProto(tankSpriteSheet.getSubimage(344, 23, 486, 579), 486 / 2, 579 / 2).setLayer(L_TBODY);
        turretProto = spriter.createSpriteProto(tankSpriteSheet.getSubimage(834, 31, 234, 533), 234 / 2, 437).setLayer(L_TTURRET);
        bulletProto = spriter.createSpriteProto(tankSpriteSheet.getSubimage(937, 590, 48, 24), 22, 12).setLayer(L_TBULLET);

        Spriter.Sprite cursor = spriter.createSprite(loadImage("/point.png"), 256 / 2, 256 / 2, 0.05).setLayer(L_HUD_CURSOR);

        Spriter.Control control = spriter.getControl();

        Tank tank = new Tank();

        double t = 0;
        double w = 0;
        double a = 0;
        double b = 0;
        ArrayList<Bullet> bullets = new ArrayList<>();
        while (true) {
            Spriter.Point m = control.getMousePos();
            cursor.setPos(m);

            tank.moveTurretTo(m);

            if (control.isAnyKeyDown(KeyEvent.VK_LEFT, KeyEvent.VK_A, KeyEvent.VK_NUMPAD4) && w > -Math.PI / 2) {
                w -= 0.25;
            } else if (control.isAnyKeyDown(KeyEvent.VK_RIGHT, KeyEvent.VK_D, KeyEvent.VK_NUMPAD6) && w < Math.PI / 2) {
                w += 0.25;
            } else if (w < -0.05) {
                w += 0.25;
            } else if (w > 0.05) {
                w -= 0.25;
            }
            tank.setWheelsAngle(Math.PI / 8 * Math.sin(w));

            if (control.isAnyKeyDown(KeyEvent.VK_UP, KeyEvent.VK_W, KeyEvent.VK_NUMPAD8) && a < 0.02) {
                a += 0.002;
            } else if (a > 0.0005) {
                a -= 0.002;
            }
            if (control.isAnyKeyDown(KeyEvent.VK_DOWN, KeyEvent.VK_S, KeyEvent.VK_NUMPAD5) && a > -0.02) {
                a -= 0.002;
            } else if (a < -0.0005) {
                a += 0.002;
            }
            tank.moveForward(a);

            if (control.isButtonDown(MouseEvent.BUTTON1) && b <= 0) {
                bullets.add(tank.fire());
                b = 3;
            }
            if (b > 0) {
                b--;
            }
            for (Iterator<Bullet> iterator = bullets.iterator(); iterator.hasNext(); ) {
                Bullet bullet = iterator.next();
                if (!bullet.move()) {
                    iterator.remove();
                    bullet.destroy();
                }
            }

            Thread.sleep(50);
        }
    }

}
