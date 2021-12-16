package com.adam.swing_project.loader;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.security.CodeSource;
import java.security.ProtectionDomain;

/**
 * 支持Fat-jar的类加载器
 */
public class FatJarClassLoader extends ClassLoader {

    private final LoaderLogger logger = LoaderLogger.createLogger(this);

    private File rootFile;
    private AbstractFatJarLibReader fatJarLibReader;
    private String fatJarRunClassName;

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
            this.fatJarRunClassName = fatJarLibReader.getFatJarRunClassName();
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
            return defineClass(name, classBytes, 0, classBytes.length);
        } catch (IOException e) {
            e.printStackTrace();
            throw new ClassNotFoundException(name);
        }
    }

    protected Class<?> directLoadClass(String name) throws ClassNotFoundException {
        return findClass(name);
    }

    @Override
    protected URL findResource(String name) {
        URL url = super.findResource(name);
        if(url == null) {
            try {
                url = new URL(null, "fat-jar:" + name, new URLStreamHandler() {
                    @Override
                    protected URLConnection openConnection(URL u) throws IOException {
                        return new FatJarResourceURLConnection(u, fatJarLibReader);
                    }
                });
            } catch (Exception e) {
                FatJarClassLoaderException ne = new FatJarClassLoaderException("Reading resource '" + name + "' error");
                ne.initCause(e);
                throw ne;
            }
        }
        return url;
    }

    public String getFatJarRunClassName() {
        return fatJarRunClassName;
    }

    public static void main(String[] args) {
        FatJarClassLoader fatJarClassLoader = new FatJarClassLoader();
        fatJarClassLoader.init();
        URL url = FatJarClassLoader.class.getResource("/META-INF/MANIFEST.MF");
        System.out.println(url.getProtocol());
        System.out.println(url.getPath());
        try {
            Class clazz = fatJarClassLoader.loadClass("com.adam.swing_project.jcompiler.JCompiler");
            System.out.println("Loaded class '" + clazz.getName() + "' by " + clazz.getClassLoader());
            Method method = clazz.getMethod("main", String[].class);
            method.invoke(null, (Object) new String[0]);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
