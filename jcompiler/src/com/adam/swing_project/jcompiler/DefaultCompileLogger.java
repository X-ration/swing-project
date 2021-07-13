package com.adam.swing_project.jcompiler;

import com.adam.swing_project.jcompiler.assertion.Assert;

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
        String text = "Compiling " + currentFilePath.substring(projectPath.length());
        return text;
    }
}
