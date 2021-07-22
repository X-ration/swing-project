package com.adam.swing_project.jcompiler.internal_compiler;

import com.adam.swing_project.jcompiler.JCompilerException;

import java.io.File;

public class ShellCommandGenerator {

    private static final String COMPILE_COMMAND = "javac -sourcepath \"%s\" -d \"%s\" -encoding utf-8 \"%s\""
            , WIN_XCOPY_COMMAND = "xcopy /FYS \"%s\" \"%s\""
            , LINUX_CP_COMMAND = "cp -r %s" + File.separator + "* \"%s\""
            , JAR_COMMAND = "jar --create --file \"%s\" --manifest \"%s\" -C \"%s\" .";

    public static String compileCommand(String sourcePath, String compilePath, String filePath) {
        return String.format(COMPILE_COMMAND, sourcePath, compilePath, filePath);
    }

    public static String copyCommand(String srcDir, String destDir) {
        String osName = System.getProperty("os.name");
        if(osName.startsWith("Windows")) {
            return String.format(WIN_XCOPY_COMMAND, srcDir, destDir);
        } else if(osName.startsWith("Linux")) {
            return String.format(LINUX_CP_COMMAND, srcDir, destDir);
        } else {
            throw new JCompilerException("unknown system");
        }
    }

    public static String jarCommand(String jarFilePath, String manifestPath, String buildPath) {
        return String.format(JAR_COMMAND, jarFilePath, manifestPath, buildPath);
    }
}
