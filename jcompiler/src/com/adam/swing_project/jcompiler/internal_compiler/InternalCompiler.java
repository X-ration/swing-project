package com.adam.swing_project.jcompiler.internal_compiler;

import com.adam.swing_project.jcompiler.assertion.Assert;
import com.adam.swing_project.jcompiler.shellexecutor.cmdhelper.CmdHelper;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class InternalCompiler {
    private File srcDir, compileDir;
    private CmdHelper cmdHelper;
    private DefaultCompileLogger compileLogger = new DefaultCompileLogger();
    private List<CompileListener> compileListeners;
    private static final String COMMAND = "javac -sourcepath \"%s\" -d \"%s\" -encoding utf-8 \"%s\"";
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    public InternalCompiler() {
        this(null, null);
    }

    public InternalCompiler(File srcDir, File compileDir) {
        this.srcDir = srcDir;
        this.compileDir = compileDir;
        this.cmdHelper = new CmdHelper();
        this.compileListeners = new ArrayList<>();
    }

    public void setCompileDir(File compileDir) {
        this.compileDir = compileDir;
    }

    public void addCompileListener(CompileListener compileListener) {
        Assert.notNull(compileListener);
        this.compileListeners.add(compileListener);
    }

    public void setSrcDir(File srcDir) {
        this.srcDir = srcDir;
    }

    public void compile() {
        InternalCompiler lock = this;
        checkDirs();
        executorService.submit(() -> {
            publishEvent(new CompileEvent(CompileEventType.STARTED));
            synchronized (lock) {
                cmdHelper.startup();
                compileLogger.setProjectDir(srcDir);
                compileLogger.logCompile("Compilation started");
                compileDir(srcDir);
                compileLogger.logCompile("Compilation finished!");
                cmdHelper.stop();
            }
            publishEvent(new CompileEvent(CompileEventType.FINISHED));
        });
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

    public void publishEvent(CompileEvent compileEvent) {
        for(CompileListener compileListener: compileListeners) {
            if(compileListener.filterEvent(compileEvent)) {
                compileListener.onEvent(compileEvent);
            }
        }
    }

}
