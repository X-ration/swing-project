package com.adam.swing_project.jcompiler.internal_compiler;

import com.adam.swing_project.jcompiler.assertion.Assert;
import com.adam.swing_project.jcompiler.shellexecutor.CommandInput;
import com.adam.swing_project.jcompiler.shellexecutor.CommandOutput;
import com.adam.swing_project.jcompiler.shellexecutor.ShellExecutor;
import com.adam.swing_project.jcompiler.shellexecutor.cmdhelper.CmdHelper;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class InternalCompiler {
    private File srcDir, compileDir;
    private ShellExecutor shellExecutor;
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
        this.shellExecutor = ShellExecutor.systemShellExecutor();
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
                compileLogger.setProjectDir(srcDir);
                compileLogger.logCompileWithLineSeparator("Compilation started");
//                compileDir(srcDir);
                List<CommandInput<String>> commandInputs = collectCompileInput(srcDir);
                if(commandInputs == null) {
                    compileLogger.logCompileWithLineSeparator("compilation aborted, no source files found");
                } else {
                    shellExecutor.reset();
                    shellExecutor.submitAsync(commandInputs);
                    while(!shellExecutor.finished()) {
                        List<CommandOutput> commandOutputs = shellExecutor.getResults();
                        for(CommandOutput commandOutput: commandOutputs) {
//                            compileLogger.logCompile("Compiling " + (String)commandOutput.getSourceInput().getTargetObject() + "..." + (commandOutput.isSuccess() ? "Success" : "Failed"));
                            compileLogger.logCompileWithLineSeparator("Compiling " + commandOutput.getSourceInput().getTargetObject() + "...");
                            if(commandOutput.getMsg() != null && !commandOutput.getMsg().equals("")) {
                                compileLogger.logCompileWithLineSeparator(commandOutput.getMsg());
                            }
                        }
                    }
                    compileLogger.logCompileWithLineSeparator("Compilation finished!");
                }
            }
            publishEvent(new CompileEvent(CompileEventType.FINISHED));
        });
    }

    public List<CommandInput<String>> collectCompileInput(File rootDir) {
        List<CommandInput<String>> resultList = null;
        File[] files = rootDir.listFiles();
        List<File> directories = null;
        for(File file: files) {
            if(file.isFile() && file.getName().endsWith(".java")) {
                if(resultList == null) {
                    resultList = new ArrayList<>();
                }
                String command = String.format(COMMAND, srcDir.getPath(), compileDir.getPath(), file.getPath());
                String relativePath = file.getPath().substring(srcDir.getPath().length() + 1);
                resultList.add(new CommandInput<>(command, command, relativePath));
            } else if(file.isDirectory()) {
                if(directories == null) {
                    directories = new ArrayList<>();
                }
                directories.add(file);
            }
        }
        if(directories != null) {
            for(File dir: directories) {
                List<CommandInput<String>> collected = collectCompileInput(dir);
                if(resultList == null) {
                    resultList = collected;
                } else {
                    resultList.addAll(collected);
                }
            }
        }
        return resultList;
    }

//    public void compileDir(File rootDir) {
//        File[] files = rootDir.listFiles();
//        List<File> directories = new ArrayList<>();
//        for(File file: files) {
//            if(file.isFile() && file.getName().endsWith(".java")) {
//                String command = String.format(COMMAND, srcDir.getPath(), compileDir.getPath(), file.getPath());
//                cmdHelper.exec(command);
//            } else if(file.isDirectory()) {
//                directories.add(file);
//            }
//        }
//        for(File dir: directories) {
//            compileDir(dir);
//        }
//
//    }

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
