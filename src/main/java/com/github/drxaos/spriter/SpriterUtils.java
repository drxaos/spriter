package com.github.drxaos.spriter;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.IOException;

public class SpriterUtils {

    /**
     * "/img.png" -> "resources/img.png"
     */
    public static BufferedImage loadImageFromResource(String name) throws IOException {
        return ImageIO.read(SpriterUtils.class.getResource(name));
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


}
