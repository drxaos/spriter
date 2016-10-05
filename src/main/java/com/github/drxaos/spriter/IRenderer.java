package com.github.drxaos.spriter;

import java.awt.*;

public interface IRenderer {
    void setAntialiasing(boolean antialiasing);

    void setBilinearInterpolation(boolean bilinearInterpolation);

    Image render(IScene scene, Image dst, Graphics2D dstGraphics);

    void setDebug(boolean debug);

    void setSpriter(Spriter spriter);

    Image makeOutputImage(int w, int h, boolean alpha);
}
