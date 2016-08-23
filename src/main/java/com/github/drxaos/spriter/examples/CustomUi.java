package com.github.drxaos.spriter.examples;

import com.github.drxaos.spriter.Spriter;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;


public class CustomUi {
    public static final int L_POINT = 50;
    public static final int L_FLY = 100;
    public static final int L_HUD_CURSOR = 1999;

    static class StatusBar extends JLabel {
        public StatusBar() {
            super();
            super.setPreferredSize(new Dimension(100, 22));
            setMessage("Ready");
            setBorder(BorderFactory.createEtchedBorder());
        }

        public void setMessage(String message) {
            setText(" " + message);
        }
    }

    public static BufferedImage loadImage(String name) throws IOException {
        return ImageIO.read(Animation.class.getResource(name));
    }

    static Spriter.Sprite flyProto;

    static class Fly {
        Spriter.Sprite fSprite;

        double x, y, a, v, z, zf, vf;
        int f, b;
        boolean c;

        public Fly(double x, double y) {
            this.x = x;
            this.y = y;
            fSprite = flyProto.clone().setWidthProportional(0.07).setVisible(true);
            zf = Math.random() * 100;
            vf = Math.random() * 100;
            c = Math.random() > 0.5;
        }

        public void fly(Spriter.Point p) {
            f++;
            fSprite.setFrame((f / 5) % 2);

            double angle = Math.atan2(p.getY() - y, p.getX() - x) - a;
            while (angle < Math.PI) {
                angle += Math.PI * 2;
            }
            while (angle > Math.PI) {
                angle -= Math.PI * 2;
            }
            double distance = Math.sqrt((p.getX() - x) * (p.getX() - x) + (p.getY() - y) * (p.getY() - y));
            if (Math.random() * 100 > 98) {
                b = (int) (Math.random() * 5 + 5);
                c = Math.random() > 0.5;
            }
            if (c) {
                a += Math.sin(Math.abs(distance) + 1) / 10 + Math.random() * 0.01;
            } else {
                a -= Math.sin(Math.abs(distance) + 1) / 10 + Math.random() * 0.01;
            }
            if (Math.abs(angle) > Math.PI / 3 && f % 30 < distance * 100) {
                if (b <= 0) {
                    a += angle / 8;
                } else {
                    a -= angle / 8;
                    b--;
                }
            }

            vf += 0.02 + Math.random() * 0.01;
            v = Math.sin(0.1 * vf) * 0.005 + 0.01;
            x += Math.cos(a) * v;
            y += Math.sin(a) * v;
            zf += 0.02 + Math.random() * 0.01;
            z = (Math.cos(zf) + 2) / 50 + 0.01;

            fSprite.setPos(x, y).setWidthProportional(z);
        }

        public void remove() {
            fSprite.remove();
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {

        Spriter spriter = new Spriter("Custom UI");
        spriter.setMinimumSize(new Dimension(400, 400));

        StatusBar statusBar = new StatusBar();
        spriter.getContentPane().add(statusBar, BorderLayout.SOUTH);

        JButton jb1 = new JButton("Add fly");
        JButton jb3 = new JButton("Add 10 flies");
        JButton jb2 = new JButton("Remove fly");
        JButton jb4 = new JButton("Clear all");
        JButton jb5 = new JButton("Smooth is ON");
        JButton jb6 = new JButton("Exit");
        {
            JPanel outerPanel = new JPanel(new BorderLayout());
            outerPanel.setBorder(
                    BorderFactory.createCompoundBorder(
                            BorderFactory.createRaisedSoftBevelBorder(),
                            BorderFactory.createEmptyBorder(10, 10, 10, 10)
                    )
            );
            spriter.getContentPane().add(outerPanel, BorderLayout.EAST);

            JPanel panel = new JPanel(new GridLayout(0, 1, 10, 10));
            panel.add(jb1);
            panel.add(jb3);
            panel.add(jb2);
            panel.add(jb4);
            outerPanel.add(panel, BorderLayout.NORTH);

            JPanel panel2 = new JPanel(new GridLayout(0, 1, 10, 10));
            panel2.add(jb5);
            panel2.add(jb6);
            outerPanel.add(panel2, BorderLayout.SOUTH);
        }

        flyProto = spriter.createSpriteProto(loadImage("/fly.png"), 17, 23, 40, 36).setLayer(L_FLY);

        Spriter.Sprite cursor = spriter.createSprite(loadImage("/cur1.png"), 7, 7, 0.08).setLayer(L_HUD_CURSOR);
        Spriter.Sprite point = spriter.createSprite(loadImage("/point.png"), 256 / 2, 256 / 2, 0.025).setLayer(L_POINT).setSquareSide(0).setVisible(true);

        Spriter.Control control = spriter.getControl();

        ArrayList<Fly> flies = new ArrayList<>();
        flies.add(new Fly(0, 0));
        flies.add(new Fly(0, 0));
        flies.add(new Fly(0, 0));

        AtomicBoolean add = new AtomicBoolean(false);
        AtomicBoolean add10 = new AtomicBoolean(false);
        AtomicBoolean remove = new AtomicBoolean(false);
        AtomicBoolean clear = new AtomicBoolean(false);
        AtomicBoolean smooth = new AtomicBoolean(true);
        jb1.addActionListener(e -> {
            add.set(true);
        });
        jb2.addActionListener(e -> {
            remove.set(true);
        });
        jb3.addActionListener(e -> {
            add10.set(true);
        });
        jb4.addActionListener(e -> {
            clear.set(true);
        });
        jb5.addActionListener(e -> {
            boolean s = !smooth.get();
            smooth.set(s);
            spriter.setSmoothScaling(s);
            jb5.setText(s ? "Smooth is ON" : "Smooth is OFF");
        });
        jb6.addActionListener(e -> {
            System.exit(0);
        });

        int pointSize = 0;
        Spriter.Point m = new Spriter.Point(0, 0);
        while (true) {
            Spriter.Click click = control.getClick();
            if (click != null) {
                pointSize = 0;
                point.setVisible(true).setPos(click);
                m = click;
            }
            if (pointSize < 8) {
                pointSize++;
            }
            point.setSquareSide(0.01 * pointSize);
            statusBar.setMessage("X = " + m.getX() + ", Y = " + m.getY() + ", Flies: " + flies.size());

            cursor.setPos(control.getMousePos());

            if (add.getAndSet(false)) {
                flies.add(new Fly(m.getX(), m.getY()));
            }
            if (add10.getAndSet(false)) {
                for (int i = 0; i < 10; i++) {
                    flies.add(new Fly(m.getX(), m.getY()));
                }
            }
            if (remove.getAndSet(false) && flies.size() > 0) {
                Fly fly = flies.remove(0);
                fly.remove();
            }
            if (clear.getAndSet(false)) {
                for (Fly fly : flies) {
                    fly.remove();
                }
                flies.clear();
            }

            for (Fly fly : flies) {
                fly.fly(m);
            }

            Thread.sleep(20);
        }
    }
}