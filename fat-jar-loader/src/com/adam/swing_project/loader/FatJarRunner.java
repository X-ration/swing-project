package com.adam.swing_project.loader;

import java.lang.reflect.Method;

public class FatJarRunner {

    public static void main(String[] args) {
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
