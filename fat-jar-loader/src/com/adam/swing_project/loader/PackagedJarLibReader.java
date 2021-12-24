package com.adam.swing_project.loader;

import java.io.*;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

public class PackagedJarLibReader extends AbstractFatJarLibReader{

    private final File rootFile;
    private final JarFile jarFile;
    private String[] nestLibEntryNames, classEntryNames, fileEntryNames;

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
    protected InputStream readResourceAsStream(String resourceName) throws IOException {
        if(parent != null) {
            if(resourceName.equalsIgnoreCase(JarFile.MANIFEST_NAME)) {
                Manifest manifest = jarFile.getManifest();
                if(manifest != null) {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    manifest.write(bos);
                    logger.logDebug("Found manifest input stream from " + debugGetReaderPath());
                    return new ByteArrayInputStream(bos.toByteArray());
                }
            } else {
                InputStream inputStream = readResource(resourceName);
                if (inputStream != null) {
                    return inputStream;
                }
            }
        }
        return readResourceNested(resourceName);
    }

    private InputStream readResource(String resourceName) throws IOException {
        for(String fileEntryName: fileEntryNames) {
            if(fileEntryName.equals(resourceName)) {
                JarEntry jarEntry = jarFile.getJarEntry(fileEntryName);
                InputStream inputStream = jarFile.getInputStream(jarEntry);
                logger.logDebug("Found resource '" + resourceName + "' input stream from " + debugGetReaderPath());
                return inputStream;
            }
        }
        return null;
    }

    private InputStream readResourceNested(String resourceName) throws IOException {
        if(fatJarEnabled && fatJarAppRootPath != null) {
            AbstractFatJarLibReader reader = getCachedReader(fatJarLibDir + "/" + fatJarAppRootPath);
            InputStream inputStream = reader.readResourceAsStream(resourceName);
            if(inputStream != null) {
                return inputStream;
            }
        }
        for(String nestedLibEntryName: nestLibEntryNames) {
            AbstractFatJarLibReader reader = getCachedReader(nestedLibEntryName);
            InputStream inputStream = reader.readResourceAsStream(resourceName);
            if(inputStream != null) {
                return inputStream;
            }
        }
        return null;
    }

    private AbstractFatJarLibReader getCachedReader(String nestedLibEntryName) throws IOException {
        AbstractFatJarLibReader reader = getCachedEntryReader(nestedLibEntryName);
        if(reader == null) {
            reader = new NestedJarLibReader
                    (()->jarFile.getInputStream(jarFile.getJarEntry(nestedLibEntryName)), nestedLibEntryName, this);
            cacheEntryReader(nestedLibEntryName, reader);
        }
        return reader;
    }

    @Override
    protected void scan() throws IOException {
        List<String> nestedLibEntryNames = new LinkedList<>()
                , classEntryNames = new LinkedList<>()
                , fileEntryNames = new LinkedList<>();
        Enumeration<? extends ZipEntry> enumeration = jarFile.entries();
        while(enumeration.hasMoreElements()) {
            JarEntry jarEntry = (JarEntry) enumeration.nextElement();
            if (fatJarEnabled && !jarEntry.isDirectory() && jarEntry.getName().startsWith(fatJarLibDir) && jarEntry.getName().endsWith(".jar")) {
                nestedLibEntryNames.add(jarEntry.getName());
            } else if(!jarEntry.isDirectory() && jarEntry.getName().endsWith(".class")) {
                classEntryNames.add(jarEntry.getName());
            }
            if(!jarEntry.isDirectory()) {
                fileEntryNames.add(jarEntry.getName());
            }
        }
        this.nestLibEntryNames = nestedLibEntryNames.toArray(new String[nestedLibEntryNames.size()]);
        this.classEntryNames = classEntryNames.toArray(new String[classEntryNames.size()]);
        this.fileEntryNames = fileEntryNames.toArray(new String[fileEntryNames.size()]);
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
