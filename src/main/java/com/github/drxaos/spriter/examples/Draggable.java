package com.github.drxaos.spriter.examples;

import com.github.drxaos.spriter.*;

import java.awt.event.MouseEvent;
import java.io.IOException;

public class Draggable {

    public static class DraggableSurface {
        Spriter spriter;
        Control control;

        Sprite cursor;
        Proto dragProto, handProto;
        Point dragPoint;
        double vpx, vpy;

        public DraggableSurface(Spriter spriter, Sprite cursor) throws IOException {
            this.spriter = spriter;
            control = spriter.getControl();

            this.cursor = cursor;
            handProto = spriter.createProto(Utils.loadImageFromResource("/hand.png"), 12, 20);
            dragProto = spriter.createProto(Utils.loadImageFromResource("/drag.png"), 12, 20);
        }

        public void handle() {
            if (control.isButtonDown(MouseEvent.BUTTON1)) {
                cursor.replaceProto(dragProto);

                if (dragPoint == null) {
                    dragPoint = control.getMousePos();
                    vpx = spriter.getScene().getViewportShiftX();
                    vpy = spriter.getScene().getViewportShiftY();
                }

                if (dragPoint != null) {
                    double dx = dragPoint.getX() - control.getMouseX();
                    double dy = dragPoint.getY() - control.getMouseY();
                    spriter.getScene().setViewportShiftX(vpx + dx);
                    spriter.getScene().setViewportShiftY(vpy + dy);
                }

            } else {
                cursor.replaceProto(handProto);
                dragPoint = null;
            }
        }
    }


    public static void main(String[] args) throws InterruptedException, IOException {

        Spriter spriter = Spriter.createDefault("Draggable");
        Proto pointProto = spriter.createProto(Utils.loadImageFromResource("/point.png"), 128, 128);
        Sprite point = pointProto.newInstance(128);

        Proto cursorProto = spriter.createProto(Utils.loadImageFromResource("/cursor.png"), 1, 1);
        Sprite cursor = cursorProto.newInstance(25);

        Control control = spriter.getControl();

        DraggableSurface draggableSurface = new DraggableSurface(spriter, cursor);

        while (true) {
            spriter.beginFrame();

            spriter.getScene().setViewportWidth(spriter.getOutput().getCanvasWidth());
            spriter.getScene().setViewportHeight(spriter.getOutput().getCanvasHeight());

            draggableSurface.handle();
            cursor.setPos(control.getMousePos().plus(spriter.getScene().getViewportShift()));

            spriter.endFrame();
        }


    }


}
