package com.github.drxaos.spriter;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

public class SpriteGroup implements Set<Sprite> {

    private LinkedHashSet<Sprite> sprites = new LinkedHashSet<>();

    @Override
    public Iterator<Sprite> iterator() {
        return sprites.iterator();
    }

    @Override
    public int size() {
        return sprites.size();
    }

    @Override
    public boolean isEmpty() {
        return sprites.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return sprites.contains(o);
    }

    @Override
    public boolean add(Sprite sprite) {
        return sprites.add(sprite);
    }

    @Override
    public boolean remove(Object o) {
        return sprites.remove(o);
    }

    @Override
    public void clear() {
        sprites.clear();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return sprites.removeAll(c);
    }

    @Override
    public Object[] toArray() {
        return sprites.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return sprites.toArray(a);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return sprites.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends Sprite> c) {
        return sprites.addAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return sprites.retainAll(c);
    }


}
