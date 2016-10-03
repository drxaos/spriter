package com.github.drxaos.spriter;

import java.awt.*;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

public class Scene {

    final ArrayList<Proto> protos = new ArrayList<>();
    final ArrayList<Sprite> sprites = new ArrayList<>();
    AtomicReference<Color> bgColor = new AtomicReference<>(Color.WHITE);
    AtomicReference<Color> borderColor = new AtomicReference<>(Color.BLACK);


    public Sprite getSpriteByIndex(int index) {
        return sprites.get(index);
    }

    public Proto getProtoByIndex(int index) {
        return protos.get(index);
    }

    public void setBackgroundColor(Color color) {
        if (color != null) {
            bgColor.set(color);
        }
    }

    public void setBorderColor(Color color) {
        if (color != null) {
            borderColor.set(color);
        }
    }

    public void snapshot() {
        synchronized (sprites) {
            for (Sprite sprite : sprites) {
                if (sprite.snapshotGetRemove()) {
                    // skip
                } else if (sprite.isDirty()) {
                    sprite.snapshot();
                }
            }
        }
    }

    public void addProto(Proto proto) {
        synchronized (protos) {
            proto.setIndex(protos.size());
            protos.add(proto);
        }
    }

    public void addSprite(Sprite sprite) {
        synchronized (sprites) {
            sprite.setIndex(sprites.size());
            sprites.add(sprite);
        }
    }
}
