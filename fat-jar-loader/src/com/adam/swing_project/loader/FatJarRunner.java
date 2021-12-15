package com.adam.swing_project.loader;

import java.lang.reflect.Method;

public class FatJarRunner {

    public static final String LOADER_LOG_LEVEL = "loaderLogLevel";
    private static final LoaderLogger logger = LoaderLogger.createLogger(FatJarRunner.class);

    public static void main(String[] args) {
        ApplicationArgumentResolver argumentResolver = new ApplicationArgumentResolver();
        argumentResolver.resolveArgs(args);

        LoaderLogger.LogLevel logLevel = LoaderLogger.LogLevel.INFO;
        if(argumentResolver.containsOption(LOADER_LOG_LEVEL)) {
            String optionValue = argumentResolver.getOptionValue(LOADER_LOG_LEVEL);
            try {
                logLevel = LoaderLogger.LogLevel.valueOf(optionValue);
            } catch (IllegalArgumentException e) {
                logger.logWarning("Incorrect option value '" + optionValue + "' for '" + LOADER_LOG_LEVEL + "'");
            }
        }
        LoaderLogger.setGlobalLogLevel(logLevel);

        FatJarClassLoader fatJarClassLoader = new FatJarClassLoader();
        fatJarClassLoader.init();
        AbstractFatJarLibReader rootReader = fatJarClassLoader.getFatJarLibReader();
        String runClassName = rootReader.getFatJarRunClassName();
        LoaderAssert.isTrue(runClassName != null && !runClassName.equals(""), "Unable to resolve run class");
        try {
            Class<?> clazz = fatJarClassLoader.loadClass(runClassName);
            Method method = clazz.getMethod("main", String[].class);
            method.invoke(null, (Object) args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
