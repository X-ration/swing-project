package com.adam.swing_project.loader;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.Arrays;

/**
 * 支持Fat-jar的类加载器
 */
public class FatJarClassLoader extends ClassLoader {

    private final LoaderLogger logger = LoaderLogger.createLogger(this);

    private File rootFile;
    private AbstractFatJarLibReader fatJarLibReader;

    public void init() {
        try {
            ProtectionDomain protectionDomain = FatJarClassLoader.class.getProtectionDomain();
            CodeSource codeSource = protectionDomain.getCodeSource();
            URI location = codeSource.getLocation().toURI();
            String path = location.getSchemeSpecificPart();
            this.rootFile = new File(path);

            if(rootFile.isDirectory()) {
                this.fatJarLibReader = new ExplodedJarLibReader(rootFile);
            } else {
                this.fatJarLibReader = new PackagedJarLibReader(rootFile);
            }

            Thread cleanThread = new Thread(()->{
                try {
                    fatJarLibReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            cleanThread.setName("FatJarClassLoaderCleanThread");
            Runtime.getRuntime().addShutdownHook(cleanThread);
        } catch (Exception e) {
            e.printStackTrace();
            FatJarClassLoaderException ne = new FatJarClassLoaderException("Error loading class");
            ne.initCause(e);
            throw ne;
        }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        try {
            byte[] classBytes = fatJarLibReader.readClass(name);
            if(classBytes == null) {
                throw new ClassNotFoundException(name);
            }
            Class clazz = defineClass(name, classBytes, 0, classBytes.length);
//            logger.logDebug("Found class '" + clazz.getName() + "' by " + this);
            return clazz;
        } catch (IOException e) {
            e.printStackTrace();
            throw new ClassNotFoundException(name);
        }
    }

    public static void main(String[] args) {
        FatJarClassLoader fatJarClassLoader = new FatJarClassLoader();
        fatJarClassLoader.init();
        try {
            Class clazz = fatJarClassLoader.loadClass("com.adam.swing_project.local_file_transfer.LocalFileTransfer");
            System.out.println("Loaded class '" + clazz.getName() + "' by " + clazz.getClassLoader());
            Method method = clazz.getMethod("main", String[].class);
            method.invoke(null, (Object) new String[0]);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
