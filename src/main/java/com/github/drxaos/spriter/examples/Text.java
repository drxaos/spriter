package com.github.drxaos.spriter.examples;

import com.github.drxaos.spriter.Spriter;

import javax.imageio.ImageIO;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;

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
                "?bcdef\nghijkl\nmnopqr\nstuvwx\nyz1234\n567890\n!a%$(&\n@+-).,\n=#_* ").setWidthProportional(0.25);
        final AtomicInteger x = new AtomicInteger(0);
        final AtomicInteger y = new AtomicInteger(0);
        final ArrayList<Spriter.Sprite> text = new ArrayList<>();
        Function<Character, Spriter.Sprite> type = (c) -> {
            text.add(font.getChar(c).setPos(x.getAndIncrement() * font.getWidth() - 1, y.get() * font.getHeight() - 1).setVisible(true));
            if (x.get() >= 8) {
                x.set(0);
                y.incrementAndGet();
            }
            return null;
        };
        Runnable backspace = () -> {
            if (text.size() > 0) {
                text.remove(text.size() - 1).remove();
                int xVal = x.decrementAndGet();
                if (xVal < 0) {
                    x.set(7);
                    y.decrementAndGet();
                }
            }
        };
        type.apply('t');
        type.apply('y');
        type.apply('p');
        type.apply('e');

        Spriter.Control control = spriter.getControl();

        while (true) {
            keypress:
            {
                Integer key = control.getKeyPress();
                if (key != null) {
                    if (key == KeyEvent.VK_BACK_SPACE) {
                        backspace.run();
                        break keypress;
                    }
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
                        type.apply(Character.toLowerCase(c));
                    }
                }
            }
            Thread.sleep(30);
        }
    }

}
