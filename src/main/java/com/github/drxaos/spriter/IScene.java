package com.github.drxaos.spriter;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Collection;

public interface IScene {
    void setViewportWidth(double viewportWidth);

    void setViewportHeight(double viewportHeight);

    void setViewportShiftX(double shiftX);

    void setViewportShiftY(double shiftY);

    void setViewportShift(double shiftX, double shiftY);

    void setViewportAngle(double angle);

    Sprite getSpriteByIndex(int index);

    Proto getProtoByIndex(int index);

    void setBackgroundColor(Color color);

    void setBorderColor(Color color);

    void snapshot();

    void addProto(Proto proto);

    void addSprite(Sprite sprite);

    Point screenToWorld(int screenX, int screenY, int canvasWidth, int canvasHeight);

    Point worldToScreen(int worldX, int worldY, int canvasWidth, int canvasHeight);

    void setSpriter(Spriter spriter);

    double getViewportWidth();

    double getViewportHeight();

    double getViewportShiftX();

    double getViewportShiftY();

    Color getBgColor();

    Color getBorderColor();

    double getViewportShiftA();

    Proto createProto(BufferedImage image, double imageCenterX, double imageCenterY);

    Proto createProto(BufferedImage image, double imageCenterX, double imageCenterY, int frameWidth, int frameHeight);

    Sprite createSprite(Proto proto, double objectWidth, double objectHeight);

    Sprite createSprite(Proto proto, double objectWidth);

    Sprite copySprite(Sprite sprite);

    Collection<Sprite> getSprites();

    Collection<Proto> getProtos();

    void remove(Sprite sprite);
}
