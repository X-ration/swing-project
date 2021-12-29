package com.adam.swing_project.loader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

public class NestedJarLibReader extends AbstractFatJarLibReader{

    private NestInputStreamVendor rootInputStreamVendor;
    private String[] classEntryNames, nestedLibEntryNames, fileEntryNames;

    interface NestInputStreamVendor {
        InputStream getStream() throws IOException;
    }

    public NestedJarLibReader(NestInputStreamVendor vendor, String rootName, AbstractFatJarLibReader parent) throws IOException{
        super(parent);
        this.rootInputStreamVendor = vendor;
        this.rootName = rootName;
        resolveFatJarProperties();
        scan();
    }

    @Override
    protected byte[] readClass(String className) throws IOException {
        String expectedEntryName = className.replaceAll("\\.", "/") + ".class";
        JarEntry jarEntry;
        for(String classEntryName: classEntryNames) {
            if(expectedEntryName.equals(classEntryName)) {
                JarInputStream jarInputStream = getJarInputStream();
                while((jarEntry = jarInputStream.getNextJarEntry()) != null) {
                    if(jarEntry.getName().equals(expectedEntryName)) {
                        byte[] classBytes = jarInputStream.readAllBytes();
                        jarInputStream.close();
                        logger.logDebug("Found class '" + className + "' bytes from " + debugGetReaderPath());
                        return classBytes;
                    }
                }
            }
        }
        JarInputStream jarInputStream = getJarInputStream();
        while((jarEntry = jarInputStream.getNextJarEntry()) != null) {
            if(containsNestedEntry(jarEntry.getName())) {
                byte[] nestedLibBytes = jarInputStream.readAllBytes();
                AbstractFatJarLibReader reader = getCachedEntryReader(jarEntry.getName());
                if(reader == null) {
                    reader = new NestedJarLibReader
                            (()->new ByteArrayInputStream(nestedLibBytes), jarEntry.getName(), this);
                    cacheEntryReader(jarEntry.getName(), reader);
                }
                byte[] classBytes = reader.readClass(className);
                if(classBytes != null) {
                    jarInputStream.close();
                    return classBytes;
                }
            }
        }
        jarInputStream.close();
        return null;
    }

    @Override
    protected InputStream readResourceAsStream(String resourceName) throws IOException {
        JarEntry jarEntry;
        //first find current
        //manifest is not a 'entry', finding manifest requires special proceeding
        if(resourceName.equalsIgnoreCase(JarFile.MANIFEST_NAME)) {
            JarInputStream jarInputStream = getJarInputStream();
            Manifest manifest = jarInputStream.getManifest();
            if(manifest != null) {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                manifest.write(bos);
                logger.logDebug("Found manifest input stream from " + debugGetReaderPath());
                return new ByteArrayInputStream(bos.toByteArray());
            }
        } else {
            for (String fileEntryName : fileEntryNames) {
                if (fileEntryName.equals(resourceName)) {
                    JarInputStream jarInputStream = getJarInputStream();
                    while ((jarEntry = jarInputStream.getNextJarEntry()) != null) {
                        if (jarEntry.getName().equals(fileEntryName)) {
                            logger.logDebug("Found resource '" + resourceName + "' input stream from " + debugGetReaderPath());
                            return jarInputStream;
                        }
                    }
                }
            }
        }
        //next find root path
        if(fatJarEnabled && fatJarAppRootPath != null) {
            JarInputStream jarInputStream = getJarInputStream();
            while ((jarEntry = jarInputStream.getNextJarEntry()) != null) {
                if(jarEntry.getName().equals(fatJarAppRootPath)) {
                    AbstractFatJarLibReader reader = getCachedReader(fatJarLibDir + "/" + jarEntry.getName(), jarInputStream);
                    InputStream inputStream = reader.readResourceAsStream(resourceName);
                    if (inputStream != null) {
                        jarInputStream.close();
                        return inputStream;
                    }
                }
            }
            jarInputStream.close();
        }

        //last find all nested
        JarInputStream jarInputStream = getJarInputStream();
        while((jarEntry = jarInputStream.getNextJarEntry()) != null) {
            if(containsNestedEntry(jarEntry.getName())) {
                AbstractFatJarLibReader reader = getCachedReader(jarEntry.getName(), jarInputStream);
                InputStream inputStream = reader.readResourceAsStream(resourceName);
                if(inputStream != null) {
                    jarInputStream.close();
                    return inputStream;
                }
            }
        }
        jarInputStream.close();
        return null;
    }
    private AbstractFatJarLibReader getCachedReader(String jarEntryName, JarInputStream jarInputStream) throws IOException {
        AbstractFatJarLibReader reader = getCachedEntryReader(jarEntryName);
        if(reader == null) {
            byte[] nestedLibBytes = jarInputStream.readAllBytes();
            reader = new NestedJarLibReader
                    (()->new ByteArrayInputStream(nestedLibBytes), jarEntryName, this);
            cacheEntryReader(jarEntryName, reader);
        }
        return reader;
    }

    private boolean containsNestedEntry(String entryName) {
        for(String nestedLibEntryName: nestedLibEntryNames) {
            if(nestedLibEntryName.equals(entryName))
                return true;
        }
        return false;
    }

    @Override
    protected void scan() throws IOException {
        JarInputStream jarInputStream = getJarInputStream();
        JarEntry jarEntry;
        LinkedList<String> classEntryNames = new LinkedList<>(),
                nestedLibEntryNames = new LinkedList<>(),
                fileEntryNames = new LinkedList<>();
        while((jarEntry = jarInputStream.getNextJarEntry()) != null) {
            if(!jarEntry.isDirectory() && jarEntry.getName().endsWith(".class")) {
                classEntryNames.add(jarEntry.getName());
            }
            if(fatJarEnabled && !jarEntry.isDirectory() && jarEntry.getName().startsWith(fatJarLibDir) && jarEntry.getName().endsWith(".jar")) {
                nestedLibEntryNames.add(jarEntry.getName());
            }
            if(!jarEntry.isDirectory()) {
                fileEntryNames.add(jarEntry.getName());
            }
        }
        this.classEntryNames = classEntryNames.toArray(new String[classEntryNames.size()]);
        this.nestedLibEntryNames = nestedLibEntryNames.toArray(new String[nestedLibEntryNames.size()]);
        this.fileEntryNames = fileEntryNames.toArray(new String[fileEntryNames.size()]);
        logger.logDebug("Found " + this.classEntryNames.length + " class entries for '" + rootName + "'");
        if(this.nestedLibEntryNames.length > 0) {
            logger.logDebug("Found nested lib entries: " + Arrays.toString(this.nestedLibEntryNames) + " for '" + rootName + "'");
        } else if(fatJarEnabled){
            logger.logDebug("Found no nested lib entries for '" + rootName + "'");
        }
        jarInputStream.close();
    }

    @Override
    protected Manifest readManifest() throws IOException {
        JarInputStream jarInputStream = getJarInputStream();
        Manifest manifest = jarInputStream.getManifest();
        jarInputStream.close();
        return manifest;
    }

    @Override
    protected void close() throws IOException {
        super.close();
    }

    private JarInputStream getJarInputStream() throws IOException {
        return new JarInputStream(rootInputStreamVendor.getStream());
    }
}
