package com.adam.swing_project.jcompiler.internal_compiler;

import com.adam.swing_project.jcompiler.assertion.Assert;
import com.adam.swing_project.jcompiler.shellexecutor.CommandInput;
import com.adam.swing_project.jcompiler.shellexecutor.CommandOutput;
import com.adam.swing_project.jcompiler.shellexecutor.ShellExecutor;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class InternalCompiler {
    private File rootDir;
    private ProjectLayout projectLayout;
    private ShellExecutor shellExecutor;
    private DefaultCompileLogger compileLogger = new DefaultCompileLogger();
    private List<CompileListener> compileListeners;
    private static final String COMPILE_COMMAND = "javac -sourcepath \"%s\" -d \"%s\" -encoding utf-8 \"%s\""
            , XCOPY_COMMAND = "xcopy /FYS \"%s\" \"%s\""
            , JAR_COMMAND = "jar --create --file \"%s\" --manifest \"%s\" -C \"%s\" .";
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    public InternalCompiler() {
        this(null);
    }

    public InternalCompiler(File rootDir) {
        this.rootDir = rootDir;
        this.shellExecutor = ShellExecutor.systemShellExecutor();
        this.compileListeners = new ArrayList<>();
    }

    public InternalCompiler(File rootDir, ProjectLayout projectLayout, ShellExecutor shellExecutor) {
        this.rootDir = rootDir;
        this.projectLayout = projectLayout;
        this.shellExecutor = shellExecutor;
    }

    public void addCompileListener(CompileListener compileListener) {
        Assert.notNull(compileListener);
        this.compileListeners.add(compileListener);
    }

    public void setRootDir(File rootDir) {
        this.rootDir = rootDir;
    }

    public void compile() {
        InternalCompiler lock = this;
        checkDirs();
        executorService.submit(() -> {
            publishEvent(new CompileEvent(CompileEventType.STARTED));
            synchronized (lock) {
                compileLogger.setProjectDir(rootDir);
                compileLogger.logCompileWithLineSeparator("Compilation started");
                List<CommandInput<String>> commandInputs = collectAllInput();
                if(commandInputs == null) {
                    compileLogger.logCompileWithLineSeparator("compilation aborted, no source files found");
                } else {
                    shellExecutor.reset();
                    shellExecutor.submitAsync(commandInputs);
                    while(!shellExecutor.finished()) {
                        List<CommandOutput> commandOutputs = shellExecutor.getResults();
                        for(CommandOutput commandOutput: commandOutputs) {
//                            compileLogger.logCompile("Compiling " + (String)commandOutput.getSourceInput().getTargetObject() + "..." + (commandOutput.isSuccess() ? "Success" : "Failed"));
                            compileLogger.logCompileWithLineSeparator(commandOutput.getSourceInput().getTargetObject().toString());
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

    private List<CommandInput<String>> collectAllInput() {
        List<CommandInput<String>> mergedList = collectCompileInput();
        mergedList.addAll(collectResourceInput());
        CommandInput<String> packageInput = collectPackageInput();
        mergedList.add(packageInput);
        return mergedList;
    }

    private CommandInput<String> collectPackageInput() {
        String releaseJarPath = projectLayout.getReleaseDir().getPath() + File.separator + projectLayout.getReleaseFileName();
        String command = String.format(JAR_COMMAND, releaseJarPath, projectLayout.getManifestFile().getPath(), projectLayout.getBuildDir().getPath());
        CommandInput<String> commandInput = new CommandInput<>(command, command, "packaging into " + releaseJarPath);
        return commandInput;
    }

    private List<CommandInput<String>> collectResourceInput() {
        List<CommandInput<String>> resourceInputs = new ArrayList<>();
        for(File dir: projectLayout.getResources()) {
            if(dir.exists() && dir.isDirectory()) {
                String command = String.format(XCOPY_COMMAND, dir.getPath(), projectLayout.getBuildDir().getPath());
                String relativePath = dir.getPath().substring(projectLayout.getRootDir().getPath().length() + 1);
                String msg = "copying resources in " + relativePath + "...";
                CommandInput<String> commandInput = new CommandInput<>(command, command, msg);
                resourceInputs.add(commandInput);
            }
        }
        return resourceInputs;
    }

    private List<CommandInput<String>> collectCompileInput() {
        List<CommandInput<String>> mergedList = new ArrayList<>();
        for(File dir: projectLayout.getSourceDirs()) {
            if(dir.exists() && dir.isDirectory()) {
                mergedList.addAll(collectCompileInput(dir, dir));
            }
        }
        return mergedList;
    }

    private List<CommandInput<String>> collectCompileInput(File curDir, File sourceDir) {
        List<CommandInput<String>> resultList = null;
        File[] files = curDir.listFiles();
        List<File> directories = null;
        if(files == null) return null;
        for(File file: files) {
            if(file.isFile() && file.getName().endsWith(".java")) {
                if(resultList == null) {
                    resultList = new ArrayList<>();
                }
                String command = String.format(COMPILE_COMMAND, sourceDir, projectLayout.getBuildDir().getPath(), file.getPath());
                String relativePath = file.getPath().substring(projectLayout.getRootDir().getPath().length() + 1);
                String msg = "Compiling " + relativePath + "...";
                resultList.add(new CommandInput<>(command, command, msg));
            } else if(file.isDirectory()) {
                if(directories == null) {
                    directories = new ArrayList<>();
                }
                directories.add(file);
            }
        }
        if(directories != null) {
            for(File dir: directories) {
                List<CommandInput<String>> collected = collectCompileInput(dir, sourceDir);
                if(resultList == null) {
                    resultList = collected;
                } else {
                    resultList.addAll(collected);
                }
            }
        }
        return resultList;
    }

    public void addCompileLoggerListener(CompileLoggerListener listener) {
        compileLogger.addCompileLoggerListener(listener);
    }

    public void checkDirs() {
        Assert.notNull(rootDir);
        Assert.isTrue(rootDir.exists() && rootDir.exists(), "rootDir invalid");
        if(projectLayout == null) {
            projectLayout = ProjectLayout.defaultLayout(rootDir);
        }
        Assert.isTrue(projectLayout.isValid(), "project layout invalid");
    }

    public void publishEvent(CompileEvent compileEvent) {
        for(CompileListener compileListener: compileListeners) {
            if(compileListener.filterEvent(compileEvent)) {
                compileListener.onEvent(compileEvent);
            }
        }
    }

}
