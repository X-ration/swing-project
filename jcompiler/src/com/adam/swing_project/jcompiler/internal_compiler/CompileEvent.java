package com.adam.swing_project.jcompiler.internal_compiler;

public class CompileEvent {
    private CompileEventType type;

    public CompileEvent(CompileEventType type) {
        this.type = type;
    }

    public CompileEventType getType() {
        return type;
    }

    public void setType(CompileEventType type) {
        this.type = type;
    }
}
