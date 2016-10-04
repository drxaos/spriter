package com.github.drxaos.spriter;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.IOException;

public class Utils {

    /**
     * "/img.png" -> "resources/img.png"
     */
    public static BufferedImage loadImageFromResource(String name) throws IOException {
        BufferedImage image = ImageIO.read(Utils.class.getResource(name));
        BufferedImage convertedImage;
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gd = ge.getDefaultScreenDevice();
        GraphicsConfiguration gc = gd.getDefaultConfiguration();
        convertedImage = gc.createCompatibleImage(image.getWidth(),
                image.getHeight(),
                image.getTransparency());
        convertedImage.setAccelerationPriority(1f);
        Graphics2D g2d = convertedImage.createGraphics();
        g2d.drawImage(image, 0, 0, image.getWidth(), image.getHeight(), null);
        g2d.dispose();
        return convertedImage;
    }

    /**
     * ARGB
     */
    public static BufferedImage createColorImage(Color color) {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_4BYTE_ABGR);
        image.setRGB(0, 0, color.getRGB());
        return image;
    }

    /**
     * 0xFFFF00FF (purple) -> 0 (transparent)
     */
    public static BufferedImage replaceColor(BufferedImage image, int searchARGB, int replaceARGB) {
        BufferedImage aimage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        aimage.getGraphics().drawImage(image, 0, 0, null);
        int[] bufferbyte = ((DataBufferInt) aimage.getRaster().getDataBuffer()).getData();
        for (int i = 0; i < bufferbyte.length; i++) {
            if (bufferbyte[i] == searchARGB) {
                bufferbyte[i] = replaceARGB;
            }
        }
        return aimage;
    }

    public static BufferedImage scaleImage(BufferedImage img, int targetWidth, int targetHeight, boolean smoothScaling) {

        int type = (img.getTransparency() == Transparency.OPAQUE) ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
        BufferedImage ret = img;
        int w = img.getWidth();
        int h = img.getHeight();
        boolean scaleDownW = w >= targetWidth;
        boolean scaleDownH = h >= targetHeight;

        do {
            if (scaleDownW && w > targetWidth) {
                w /= 2;
                if (w < targetWidth) {
                    w = targetWidth;
                }
            }

            if (scaleDownH && h > targetHeight) {
                h /= 2;
                if (h < targetHeight) {
                    h = targetHeight;
                }
            }

            if (!scaleDownW && w < targetWidth) {
                w *= 2;
                if (w > targetWidth) {
                    w = targetWidth;
                }
            }

            if (!scaleDownH && h < targetHeight) {
                h *= 2;
                if (h > targetHeight) {
                    h = targetHeight;
                }
            }

            BufferedImage tmp = new BufferedImage(w, h, type);
            Graphics2D g2 = tmp.createGraphics();
            if (smoothScaling) {
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            } else {
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            }
            g2.drawImage(ret, 0, 0, w, h, null);
            g2.dispose();

            ret = tmp;
        } while (w != targetWidth || h != targetHeight);

        return ret;
    }
}
