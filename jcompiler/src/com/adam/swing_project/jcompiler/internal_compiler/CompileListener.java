package com.adam.swing_project.jcompiler.internal_compiler;

public interface CompileListener {

    void onEvent(CompileEvent compileEvent);

    default boolean filterEvent(CompileEvent compileEvent) {
        return true;
    }

}
