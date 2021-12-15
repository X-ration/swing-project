package com.adam.swing_project.loader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class FatJarResourceURLConnection extends URLConnection {

    private final LoaderLogger logger = LoaderLogger.createLogger(this);
    private InputStream inputStream;
    private final AbstractFatJarLibReader fatJarLibReader;
    private boolean connected;

    public FatJarResourceURLConnection(URL url, AbstractFatJarLibReader fatJarLibReader) {
        super(url);
        this.fatJarLibReader = fatJarLibReader;
        LoaderAssert.isTrue(url.getProtocol().equals("fat-jar"), FatJarClassLoaderException.class, "Unknown protocol!");
        LoaderAssert.notNull(fatJarLibReader);
    }

    @Override
    public void connect() throws IOException {
        if(!connected) {
            inputStream = fatJarLibReader.readResourceAsStream(url.getPath());
            connected = true;
        }
    }

    @Override
    public InputStream getInputStream() throws IOException {
        connect();
        return inputStream;
    }
}
