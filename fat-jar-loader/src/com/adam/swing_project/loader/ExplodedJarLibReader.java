package com.adam.swing_project.loader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.Manifest;

public class ExplodedJarLibReader extends AbstractFatJarLibReader {

    private File rootFile;
    private String[] libFileNames;

    public ExplodedJarLibReader(File rootFile) {
        this.rootFile = rootFile;
        LoaderAssert.isTrue(rootFile != null && rootFile.isDirectory() && rootFile.exists(), FatJarLibReaderException.class, "Invalid root file");
        this.rootName = rootFile.getName();
        resolveFatJarProperties();
        scan();
    }

    @Override
    protected byte[] readClass(String className) throws IOException {
        for(String fatJarFileName: libFileNames) {
            File fatJarLibDirectory = new File(rootFile, fatJarLibDir);
            LoaderAssert.isTrue(fatJarLibDirectory.exists() && fatJarLibDirectory.isDirectory(), FatJarLibReaderException.class,
                    "Fat jar lib '" + fatJarLibDir +"' is not a valid directory");
            File fatJarFile = new File(fatJarLibDirectory, fatJarFileName);
            LoaderAssert.isTrue(fatJarFile.exists(), FatJarLibReaderException.class,
                    "Fat jar '" + fatJarFile +"' does not exist");
            if(fatJarFile.isFile() && fatJarFile.getName().endsWith(".jar")) {
                byte[] classBytes = new PackagedJarLibReader(fatJarFile).readClass(className);
                if (classBytes != null) {
                    return classBytes;
                }
            }
        }
        return null;
    }

    @Override
    protected void scan() {
        File fatJarLibDirectory = new File(rootFile, fatJarLibDir);
        LoaderAssert.isTrue(fatJarLibDirectory.exists() && fatJarLibDirectory.isDirectory(), FatJarLibReaderException.class,
                "Fat jar lib '" + fatJarLibDir +"' is not a valid directory");
        File[] fatJarLibDirFiles = fatJarLibDirectory.listFiles();
        List<String> fatJars = new LinkedList<>();
        for(File file: fatJarLibDirFiles) {
            if(file.isFile() && file.getName().endsWith(".jar")) {
                fatJars.add(file.getName());
            }
        }
        this.libFileNames = fatJars.toArray(new String[fatJars.size()]);
        logger.logDebug("Found lib files: " + Arrays.toString(this.libFileNames));
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
    protected void close() throws IOException{
    }
}
