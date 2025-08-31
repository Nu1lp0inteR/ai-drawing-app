package com.aidrawing.backend.event;

import com.aidrawing.backend.entity.Drawing;
import org.springframework.context.ApplicationEvent;

public class DrawingCompleteEvent extends ApplicationEvent {

    private final Drawing drawing;

    public DrawingCompleteEvent(Object source, Drawing drawing) {
        super(source);
        this.drawing = drawing;
    }

    public Drawing getDrawing() {
        return drawing;
    }
}