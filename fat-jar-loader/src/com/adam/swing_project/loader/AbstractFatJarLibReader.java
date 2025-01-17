package com.adam.swing_project.loader;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public abstract class AbstractFatJarLibReader {
    protected final LoaderLogger logger = LoaderLogger.createLogger(this);

    protected String fatJarLibDir, rootName;
    protected final Map<String, AbstractFatJarLibReader> entryReaderIndexMap = new HashMap<>();
    protected AbstractFatJarLibReader parent;
    protected boolean fatJarEnabled;
    protected String fatJarRunClassName;
    protected String fatJarAppRootPath;
    protected static final String MANIFEST_FILE_PATH = "/META-INF/MANIFEST.MF";
    protected static final String FAT_JAR_ENABLED = "Fat-Jar-Enabled";
    protected static final String FAT_JAR_LIBRARY_DIR = "Fat-Jar-Lib-Dir";
    protected static final String FAT_JAR_RUN_CLASS_NAME = "Fat-Jar-Run-Class-Name";
    protected static final String FAT_JAR_APP_ROOT_PATH = "Fat-Jar-App-Root-Path";

    protected AbstractFatJarLibReader(AbstractFatJarLibReader parent) {
        this.parent = parent;
    }

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
        String fatJarEnabled = resolveManifestProperty(attributes, FAT_JAR_ENABLED, false);
        if(fatJarEnabled == null || !fatJarEnabled.equalsIgnoreCase("TRUE")) {
            logger.logDebug("Fat-Jar not enabled for '" + rootName + "'");
            return;
        }
        this.fatJarEnabled = true;
        this.fatJarLibDir = resolveManifestProperty(attributes, FAT_JAR_LIBRARY_DIR, true);
        this.fatJarRunClassName = resolveManifestProperty(attributes, FAT_JAR_RUN_CLASS_NAME, false);
        this.fatJarAppRootPath = resolveManifestProperty(attributes, FAT_JAR_APP_ROOT_PATH, false);
    }

    private String resolveManifestProperty(Attributes attributes, String name, boolean required) {
        String value = attributes.getValue(name);
        LoaderAssert.isTrue(!required || (value != null && !value.equals("")), FatJarLibReaderException.class, "Manifest attribute '" + name + "' required");
        if(value != null && !value.equals("")) {
            if(logger.debugEnabled()) {
                logger.logDebug("Resolved manifest attribute '" + name + "=" + value + "' + for '" + rootName + "'");
            }
            return value;
        } else {
            return null;
        }
    }

    /**
     * 读取给定类名的数据
     * @param className
     * @return
     */
    protected abstract byte[] readClass(String className) throws IOException;

    public String getFatJarRunClassName() {
        return fatJarRunClassName;
    }

    protected abstract InputStream readResourceAsStream(String resourceName) throws IOException;

    /**
     * 扫描所有类文件，嵌套lib
     * @return
     */
    protected abstract void scan() throws IOException;

    protected abstract Manifest readManifest() throws IOException;

    protected void cacheEntryReader(String entryName, AbstractFatJarLibReader reader) {
        entryReaderIndexMap.put(entryName, reader);
    }

    protected AbstractFatJarLibReader getCachedEntryReader(String entryName) {
        return entryReaderIndexMap.get(entryName);
    }

    protected String debugGetReaderPath() {
        return debugGetReaderPathInternal(this);
    }

    private String debugGetReaderPathInternal(AbstractFatJarLibReader reader) {
        if(reader.parent == null) {
            return reader.rootName;
        }
        return debugGetReaderPathInternal(reader.parent) + "/" +
                ((reader instanceof PackagedJarLibReader) ? reader.parent.fatJarLibDir + "/" : "") + reader.rootName;
    }

    protected void close() throws IOException {
        for(Map.Entry<String, AbstractFatJarLibReader> entry: entryReaderIndexMap.entrySet()) {
            entry.getValue().close();
        }
        entryReaderIndexMap.clear();
    }

}
