package com.github.drxaos.spriter.examples;

import com.github.drxaos.spriter.Proto;
import com.github.drxaos.spriter.Sprite;
import com.github.drxaos.spriter.Spriter;
import com.github.drxaos.spriter.Utils;

import java.awt.image.BufferedImage;
import java.io.IOException;

public class Puppet {
    public static void main(String[] args) throws IOException, InterruptedException {

        Spriter spriter = Spriter.createDefault("Puppet");
        spriter.setViewportWidth(2000);
        spriter.setViewportHeight(2000);

        BufferedImage sheet = Utils.loadImageFromResource("/lumberjack.png");
        Proto shieldProto = spriter.createProto(sheet.getSubimage(20, 9, 339, 994), 244, 704);
        Proto axeProto = spriter.createProto(sheet.getSubimage(380, 24, 276, 667), 63, 393);
        Proto bodyProto = spriter.createProto(sheet.getSubimage(717, 12, 276, 430), 124, 191);
        Proto headProto = spriter.createProto(sheet.getSubimage(1058, 15, 209, 387), 78, 340);
        Proto neckProto = spriter.createProto(sheet.getSubimage(1101, 421, 61, 159), 16, 146);
        Proto leftLegProto = spriter.createProto(sheet.getSubimage(816, 482, 51, 395), 21, 16);
        Proto rightLegProto = spriter.createProto(sheet.getSubimage(887, 533, 65, 341), 17, 13);
        Proto leftFootProto = spriter.createProto(sheet.getSubimage(624, 744, 173, 91), 155, 20);
        Proto rightFootProto = spriter.createProto(sheet.getSubimage(986, 642, 207, 93), 20, 22);
        Proto leftArmProto = spriter.createProto(sheet.getSubimage(394, 891, 225, 74), 209, 35);
        Proto rightArmProto = spriter.createProto(sheet.getSubimage(894, 905, 349, 61), 11, 28);

        Sprite body = bodyProto.newInstance(bodyProto.getImageWidth());
        body.setZ(1);

        Sprite neck = neckProto.newInstance(neckProto.getImageWidth());
        neck.setParent(body);
        neck.setPos(126 - 124, 20 - 191);

        Sprite head = headProto.newInstance(headProto.getImageWidth());
        head.setParent(neck);
        head.setPos(31 - 16, 13 - 146);

        Sprite leftArm = leftArmProto.newInstance(leftArmProto.getImageWidth());
        leftArm.setParent(body);
        leftArm.setPos(30 - 124, 87 - 191);

        Sprite rightArm = rightArmProto.newInstance(rightArmProto.getImageWidth());
        rightArm.setParent(body);
        rightArm.setPos(189 - 124, 49 - 191);

        Sprite shield = shieldProto.newInstance(shieldProto.getImageWidth());
        shield.setParent(leftArm);
        shield.setPos(20 - 209, 38 - 35);
        shield.setAngle(-0.9);
        shield.setZ(2);

        Sprite axe = axeProto.newInstance(axeProto.getImageWidth());
        axe.setParent(rightArm);
        axe.setPos(305 - 11, 29 - 28);
        axe.setAngle(0.9);
        axe.setZ(3);

        Sprite leftLeg = leftLegProto.newInstance(leftLegProto.getImageWidth());
        leftLeg.setParent(body);
        leftLeg.setPos(106 - 124, 310 - 191);

        Sprite rightLeg = rightLegProto.newInstance(rightLegProto.getImageWidth());
        rightLeg.setParent(body);
        rightLeg.setPos(177 - 124, 329 - 191);

        Sprite leftFoot = leftFootProto.newInstance(leftFootProto.getImageWidth());
        leftFoot.setParent(leftLeg);
        leftFoot.setPos(29 - 21, 372 - 16);

        Sprite rightFoot = rightFootProto.newInstance(rightFootProto.getImageWidth());
        rightFoot.setParent(rightLeg);
        rightFoot.setPos(43 - 17, 317 - 13);

        double b = 0;
        double la = 0;
        double ra = 0;
        double l = 0;
        while (true) {
            spriter.beginFrame();

            b += 0.04;
            body.setAngle(Math.sin(b) * 0.1);

            double neckAngle = neck.getAngle() + body.getAngle();
            if (neckAngle > 0.002) {
                neck.setAngle(neck.getAngle() - 0.002);
            }
            if (neckAngle < -0.002) {
                neck.setAngle(neck.getAngle() + 0.002);
            }

            la += 0.1;
            leftArm.setAngle(0.7 + Math.sin(la) * 0.2);

            ra += 0.07;
            rightArm.setAngle(-0.5 + Math.sin(ra) * 1);

            l += 0.09;
            leftLeg.setAngle(Math.sin(l) * 0.2 + 0.1);
            rightLeg.setAngle(Math.cos(l) * -0.2 - 0.1);

            spriter.endFrame();
        }

    }
}
