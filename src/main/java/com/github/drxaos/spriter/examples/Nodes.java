package com.github.drxaos.spriter.examples;

import com.github.drxaos.spriter.*;

public class Nodes {
    public static void main(String[] args) throws Exception {
        Spriter spriter = Spriter.createDefault("Nodes");
        spriter.setViewportHeight(10);
        spriter.setViewportWidth(10);
        spriter.setShowCursor(true);

        Proto ufoProto = spriter.createProto(Utils.loadImageFromResource("/ufo.png"), 45, 45);
        Proto meteorProto = spriter.createProto(Utils.loadImageFromResource("/meteor.png"), 45, 45);

        Sprite leftNode = spriter.createNode().setZ(0);
        Sprite rightNode = spriter.createNode().setZ(1);

        for (int i = 0; i < 30; i++) {
            ufoProto.newInstance(1).setPos(Math.random() * 10 - 5, Math.random() * 10 - 5).setVisible(true).setParent(leftNode);
        }
        for (int i = 0; i < 30; i++) {
            meteorProto.newInstance(1).setPos(Math.random() * 10 - 5, Math.random() * 10 - 5).setVisible(true).setParent(rightNode);
        }

        Control control = spriter.getControl();

        boolean right = true;
        double yShift = 0;
        while (true) {
            spriter.beginFrame();

            Point mousePos = control.getMousePos();
            if (mousePos.getX() > 0) {
                if (!right) {
                    yShift = rightNode.getY() - mousePos.getY();
                    right = true;
                }
                leftNode.setZ(0);
                rightNode.setZ(1);
                rightNode.setY(mousePos.getY() + yShift);
            } else {
                if (right) {
                    yShift = leftNode.getY() - mousePos.getY();
                    right = false;
                }
                rightNode.setZ(0);
                leftNode.setZ(1);
                leftNode.setY(mousePos.getY() + yShift);
            }

            spriter.endFrame();
            Thread.sleep(25);
        }
    }
}
