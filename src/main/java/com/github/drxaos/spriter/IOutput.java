package com.github.drxaos.spriter;

import java.awt.*;
import java.awt.image.VolatileImage;

public interface IOutput {
    boolean isClosing();

    Control getControl();

    VolatileImage makeVolatileImage(int width, int height, boolean alpha);

    void setShowCursor(boolean show);

    int getCanvasWidth();

    int getCanvasHeight();

    boolean sync();

    Image getCanvasImage();

    Graphics2D getCanvasGraphics();

    void setCanvasImage(Image canvasImage);

    void setSpriter(Spriter spriter);

    boolean updateScreen();

    Image makeOutputImage(int width, int height, boolean alpha);

    void setTitle(String title);

    void setDefaultColor(Color color);
}
