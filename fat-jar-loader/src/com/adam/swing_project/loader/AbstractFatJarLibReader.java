package com.adam.swing_project.loader;

import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public abstract class AbstractFatJarLibReader {
    protected final LoaderLogger logger = LoaderLogger.createLogger(this);

    protected String fatJarLibDir, rootName;
    protected boolean fatJarEnabled;
    protected static final String MANIFEST_FILE_PATH = "/META-INF/MANIFEST.MF";
    protected static final String FAT_JAR_ENABLED = "Fat-Jar-Enabled";
    protected static final String FAT_JAR_LIBRARY_DIR = "Fat-Jar-Lib-Dir";

    protected void resolveFatJarProperties() {
        Manifest manifest = null;
        try {
            manifest = readManifest();
            LoaderAssert.notNull(manifest, FatJarLibReaderException.class, "Null manifest");
        } catch (IOException e) {
            e.printStackTrace();
            FatJarLibReaderException ne = new FatJarLibReaderException("Reading manifest error");
            ne.initCause(e);
            throw ne;
        }
        Attributes attributes = manifest.getMainAttributes();
        String fatJarEnabled = attributes.getValue(FAT_JAR_ENABLED);
        if(fatJarEnabled == null || !fatJarEnabled.equalsIgnoreCase("TRUE")) {
            logger.logInfo("Fat-Jar not enabled for '" + rootName + "'");
            return;
        }
        this.fatJarEnabled = true;
        String fatJarLibDir = attributes.getValue(FAT_JAR_LIBRARY_DIR);
        if(fatJarLibDir == null || fatJarLibDir.equals("")) {
            throw new FatJarLibReaderException(FAT_JAR_LIBRARY_DIR + " required");
        }
        this.fatJarLibDir = fatJarLibDir;
        logger.logInfo("Resolved " + FAT_JAR_LIBRARY_DIR + "=" + fatJarLibDir + " for '" + rootName + "'");
    }

    /**
     * 读取给定类名的数据
     * @param className
     * @return
     */
    protected abstract byte[] readClass(String className) throws IOException;

    /**
     * 扫描所有类文件，嵌套lib
     * @return
     */
    protected abstract void scan() throws IOException;

    protected abstract Manifest readManifest() throws IOException;

    protected abstract void close() throws IOException;

}
