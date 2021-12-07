package com.adam.swing_project.jcompiler.internal_compiler;

import com.adam.swing_project.library.assertion.Assert;

import java.io.File;

public class DefaultCompileLogger extends CompileLogger{
    private File projectDir;

    public void setProjectDir(File projectDir) {
        Assert.notNull(projectDir);
        this.projectDir = projectDir;
    }

    @Override
    protected String convertToLog(File file) {
        String projectPath = projectDir.getPath();
        String currentFilePath = file.getPath();
        String text = ("Compiling " + currentFilePath.substring(projectPath.length() + 1));
        return text;
    }
}
