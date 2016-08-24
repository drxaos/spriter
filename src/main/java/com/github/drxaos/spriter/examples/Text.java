package com.github.drxaos.spriter.examples;

import com.github.drxaos.spriter.Spriter;

import javax.imageio.ImageIO;
import java.awt.event.KeyEvent;

public class Text {

    public static void main(String[] args) throws Exception {
        Spriter spriter = new Spriter("Text");

        Spriter.Sprite grass = spriter.createSpriteProto(ImageIO.read(Animation.class.getResource("/grass.jpg")), 0, 0);
        boolean fx = false, fy = false;
        for (double x = -1.1; x < 1; x += grass.getWidth() - 0.03) {
            fx = !fx;
            for (double y = -1.1; y < 1; y += grass.getHeight() - 0.03) {
                fy = !fy;
                grass.createGhost().setPos(x, y).setWidthProportional(0.8).setFlipX(fx).setFlipY(fy).setVisible(true);
            }
        }

        Spriter.Font font = spriter.createFont(ImageIO.read(Animation.class.getResource("/font1.png")), 0, 0, 90, 100,
                "?bcdef\nghijkl\nmnopqr\nstuvwx\nyz1234\n567890\n!a%$(&\n@+-).,\n=#_*").setWidthProportional(0.25);
        double x = -1, y = -1;
        font.getChar('t').setPos(x, y).setVisible(true);
        x += font.getWidth();
        font.getChar('y').setPos(x, y).setVisible(true);
        x += font.getWidth();
        font.getChar('p').setPos(x, y).setVisible(true);
        x += font.getWidth();
        font.getChar('e').setPos(x, y).setVisible(true);
        x += font.getWidth();

        Spriter.Control control = spriter.getControl();

        while (true) {
            Integer key = control.getKeyPress();
            if (key != null) {
                char c = (char) (int) key;
                if (!Character.isISOControl(c)) {
                    if (control.isKeyDown(KeyEvent.VK_SHIFT)) {
                        switch (c) {
                            case '-':
                                c = '_';
                                break;
                            case '=':
                                c = '+';
                                break;
                            case '1':
                                c = '!';
                                break;
                            case '2':
                                c = '@';
                                break;
                            case '3':
                                c = '#';
                                break;
                            case '4':
                                c = '$';
                                break;
                            case '5':
                                c = '%';
                                break;
                            case '6':
                                c = '^';
                                break;
                            case '7':
                                c = '&';
                                break;
                            case '8':
                                c = '*';
                                break;
                            case '9':
                                c = '(';
                                break;
                            case '0':
                                c = ')';
                                break;
                        }
                    }
                    font.getChar(Character.toLowerCase(c)).setPos(x, y).setVisible(true);
                    x += font.getWidth();
                    if (x >= 0.99) {
                        x = -1;
                        y += font.getHeight();
                    }
                }
            }
            Thread.sleep(30);
        }
    }

}
