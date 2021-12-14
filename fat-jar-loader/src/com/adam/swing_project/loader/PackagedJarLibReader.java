package com.adam.swing_project.loader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

//todo resource, 代码优化
public class PackagedJarLibReader extends AbstractFatJarLibReader{

    private final File rootFile;
    private final JarFile jarFile;
    private String[] nestLibEntryNames, classEntryNames;

    public PackagedJarLibReader(File rootFile) throws IOException {
        this(rootFile, null);
    }

    public PackagedJarLibReader(File rootFile, AbstractFatJarLibReader parent) throws IOException {
        super(parent);
        this.rootFile = rootFile;
        LoaderAssert.isTrue(rootFile != null && rootFile.isFile() && rootFile.exists(), FatJarLibReaderException.class, "Invalid root file");
        this.rootName = rootFile.getName();
        this.jarFile = new JarFile(rootFile);
        resolveFatJarProperties();
        scan();
    }

    @Override
    protected byte[] readClass(String className) throws IOException {
        String expectedEntryName = className.replaceAll("\\.", "/") + ".class";
        for(String classEntryName: classEntryNames) {
            if(expectedEntryName.equals(classEntryName)) {
                JarEntry jarEntry = jarFile.getJarEntry(expectedEntryName);
                InputStream inputStream = jarFile.getInputStream(jarEntry);
                byte[] classBytes = inputStream.readAllBytes();
                inputStream.close();
                logger.logDebug("Found class '" + className + "' bytes from " + debugGetReaderPath());
                return classBytes;
            }
        }
        for(String nestedLibEntryName: nestLibEntryNames) {
            AbstractFatJarLibReader reader = getCachedEntryReader(nestedLibEntryName);
            if(reader == null) {
                reader = new NestedJarLibReader
                        (()->jarFile.getInputStream(jarFile.getJarEntry(nestedLibEntryName)), nestedLibEntryName, this);
                cacheEntryReader(nestedLibEntryName, reader);
            }
            byte[] classBytes = reader.readClass(className);
            if(classBytes != null) {
                return classBytes;
            }
        }
        return null;
    }

    @Override
    protected void scan() throws IOException {
        List<String> nestedLibEntryNames = new LinkedList<>()
                , classEntryNames = new LinkedList<>();
        Enumeration<? extends ZipEntry> enumeration = jarFile.entries();
        while(enumeration.hasMoreElements()) {
            JarEntry jarEntry = (JarEntry) enumeration.nextElement();
            if (fatJarEnabled && !jarEntry.isDirectory() && jarEntry.getName().startsWith(fatJarLibDir) && jarEntry.getName().endsWith(".jar")) {
                nestedLibEntryNames.add(jarEntry.getName());
            } else if(!jarEntry.isDirectory() && jarEntry.getName().endsWith(".class")) {
                classEntryNames.add(jarEntry.getName());
            }
        }
        this.nestLibEntryNames = nestedLibEntryNames.toArray(new String[nestedLibEntryNames.size()]);
        this.classEntryNames = classEntryNames.toArray(new String[classEntryNames.size()]);
        logger.logDebug("Found " + this.classEntryNames.length + " class entries for '" + rootName + "'");
        if(this.nestLibEntryNames.length > 0) {
            logger.logDebug("Found nested lib entries: " + Arrays.toString(this.nestLibEntryNames) + " for '" + rootName + "'");
        } else if(fatJarEnabled) {
            logger.logDebug("Found no nested lib entries for '" + rootName + "'");
        }
    }

    @Override
    protected Manifest readManifest() throws IOException {
        return jarFile.getManifest();
    }

    @Override
    protected void close() throws IOException {
        super.close();
        jarFile.close();
    }
}
