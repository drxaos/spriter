package com.github.drxaos.spriter;

public class State {
    Double[] active = new Double[100];
    Double[] snapshot = new Double[100];

    public void snapshot() {
        if (snapshot.length != active.length) {
            snapshot = new Double[active.length];
        }
        System.arraycopy(active, 0, snapshot, 0, active.length);
    }
}
