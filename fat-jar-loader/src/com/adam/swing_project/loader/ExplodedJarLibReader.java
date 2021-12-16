package com.adam.swing_project.loader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.Manifest;

public class ExplodedJarLibReader extends AbstractFatJarLibReader {

    private File rootFile;
    private String[] libFileNames, classFilePaths;

    public ExplodedJarLibReader(File rootFile) {
        super(null);
        this.rootFile = rootFile;
        LoaderAssert.isTrue(rootFile != null && rootFile.isDirectory() && rootFile.exists(), FatJarLibReaderException.class, "Invalid root file");
        this.rootName = rootFile.getName();
        resolveFatJarProperties();
        scan();
    }

    @Override
    protected byte[] readClass(String className) throws IOException {
        String expectedClassFilePath = rootFile.getPath() + File.separator + className.replaceAll("\\.", File.separator.equals("\\") ? "\\\\" : ("\\" + File.separator)) + ".class";
        for(String classFilePath: classFilePaths) {
            if(classFilePath.equals(expectedClassFilePath)) {
                File classFile = new File(classFilePath);
                FileInputStream fileInputStream = new FileInputStream(classFile);
                byte[] classBytes = fileInputStream.readAllBytes();
                fileInputStream.close();
                logger.logDebug("Found class '" + className + "' bytes from " + debugGetReaderPath());
                return classBytes;
            }
        }
        for(String fatJarFileName: libFileNames) {
            AbstractFatJarLibReader reader = getCachedReader(fatJarFileName);
            byte[] classBytes = reader.readClass(className);
            if (classBytes != null) {
                return classBytes;
            }
        }
        return null;
    }

    private AbstractFatJarLibReader getCachedReader(String fatJarFileName) throws IOException {
        File fatJarLibDirectory = new File(rootFile, fatJarLibDir);
        LoaderAssert.isTrue(fatJarLibDirectory.exists() && fatJarLibDirectory.isDirectory(), FatJarLibReaderException.class,
                "Fat jar lib '" + fatJarLibDir +"' is not a valid directory");
        File fatJarFile = new File(fatJarLibDirectory, fatJarFileName);
        LoaderAssert.isTrue(fatJarFile.isFile() && fatJarFile.exists(), FatJarLibReaderException.class,
                "Fat jar '" + fatJarFile +"' invalid");
        AbstractFatJarLibReader reader = getCachedEntryReader(fatJarFileName);
        if(reader == null) {
            reader = new PackagedJarLibReader(fatJarFile, this);
            cacheEntryReader(fatJarFileName, reader);
        }
        return reader;
    }

    @Override
    protected InputStream readResourceAsStream(String resourceName) throws IOException {
        for(String fatJarFileName: libFileNames) {
            AbstractFatJarLibReader reader = getCachedReader(fatJarFileName);
            InputStream inputStream = reader.readResourceAsStream(resourceName);
            if(inputStream != null) {
                return inputStream;
            }
        }
        return null;
    }

    @Override
    protected void scan() {
        List<String> libFileNames = new LinkedList<>(), classFilePaths = new LinkedList<>();

        List<File> allFiles = recursiveListFile(rootFile);
        for(File file:allFiles) {
            if(file.isFile() && file.getName().endsWith(".class")) {
                classFilePaths.add(file.getPath());
            }
        }
        if(fatJarEnabled) {
            File fatJarLibDirectory = new File(rootFile, fatJarLibDir);
            LoaderAssert.isTrue(fatJarLibDirectory.exists() && fatJarLibDirectory.isDirectory(), FatJarLibReaderException.class,
                    "Fat jar lib '" + fatJarLibDir + "' is not a valid directory");
            List<File> fatJarLibDirFiles = recursiveListFile(fatJarLibDirectory);
            for (File file : fatJarLibDirFiles) {
                if (file.isFile() && file.getName().endsWith(".jar")) {
                    libFileNames.add(file.getName());
                }
            }
        }
        this.libFileNames = libFileNames.toArray(new String[libFileNames.size()]);
        this.classFilePaths = classFilePaths.toArray(new String[classFilePaths.size()]);
        logger.logDebug("Found " + this.classFilePaths.length + " class files for '" + rootName + "'");
        if(this.libFileNames.length > 0) {
            logger.logDebug("Found lib files: " + Arrays.toString(this.libFileNames) + " for '" + rootName + "'");
        } else {
            logger.logDebug("Found no lib files for '" + rootName + "'");
        }
    }

    private List<File> recursiveListFile(File rootDirectory) {
        LinkedList<File> fileLinkedList = new LinkedList<>();
        File[] files = rootDirectory.listFiles();
        for(File file: files) {
            if(file.isFile()) {
                fileLinkedList.add(file);
            } else if(file.isDirectory()) {
                fileLinkedList.addAll(recursiveListFile(file));
            }
        }
        return fileLinkedList;
    }

    @Override
    protected Manifest readManifest() throws IOException {
        File manifestFile = new File(rootFile, MANIFEST_FILE_PATH);
        LoaderAssert.isTrue(manifestFile.exists() && manifestFile.isFile(), FatJarLibReaderException.class, "Manifest file does not exist");
        FileInputStream fileInputStream = new FileInputStream(manifestFile);
        Manifest manifest = new Manifest(fileInputStream);
        fileInputStream.close();
        return manifest;
    }

    @Override
    protected void close() throws IOException {
        super.close();
    }
}
