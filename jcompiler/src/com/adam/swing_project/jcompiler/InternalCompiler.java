package com.adam.swing_project.jcompiler;

import com.adam.swing_project.jcompiler.assertion.Assert;
import com.adam.swing_project.jcompiler.cmdhelper.CmdHelper;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class InternalCompiler {
    private File srcDir, compileDir;
    private CmdHelper cmdHelper;
    private DefaultCompileLogger compileLogger = new DefaultCompileLogger();
    private static final String COMMAND = "cmd /c javac -sourcepath \"%s\" -d \"%s\" -encoding utf-8 \"%s\"";

    public InternalCompiler() {
        this(null, null);
    }

    public InternalCompiler(File srcDir, File compileDir) {
        this.srcDir = srcDir;
        this.compileDir = compileDir;
        this.cmdHelper = new CmdHelper();
    }

    public void setCompileDir(File compileDir) {
        this.compileDir = compileDir;
    }

    public void setSrcDir(File srcDir) {
        this.srcDir = srcDir;
    }

    public void compile() {
        cmdHelper.startup();
        checkDirs();
        compileLogger.setProjectDir(srcDir);
        compileLogger.logCompile("Compilation started");
        compileDir(srcDir);
        compileLogger.logCompile("Compilation finished!");
        cmdHelper.stop();
    }

    public void compileDir(File rootDir) {
        File[] files = rootDir.listFiles();
        List<File> directories = new ArrayList<>();
        for(File file: files) {
            if(file.isFile() && file.getName().endsWith(".java")) {
                compileLogger.logCompile(file);
                String command = String.format(COMMAND, srcDir.getPath(), compileDir.getPath(), file.getPath());
                cmdHelper.exec(command);
            } else if(file.isDirectory()) {
                directories.add(file);
            }
        }
        for(File dir: directories) {
            compileDir(dir);
        }

    }

    public void addCompileLoggerListener(CompileLoggerListener listener) {
        compileLogger.addCompileLoggerListener(listener);
    }

    public void checkDirs() {
        Assert.notNull(srcDir);
        Assert.notNull(compileDir);
        Assert.isTrue(srcDir.exists() && srcDir.exists(), "srcDir/compileDir无效");
    }

}
