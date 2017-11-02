package com.github.drxaos.spriter;

public class Node extends Sprite {
    public Node(IScene scene) {
        super(scene, scene.getNodeProto(), 1, 1);
    }
}
