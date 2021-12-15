package com.adam.swing_project.loader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

public class URLDemo {
    public static void main(String[] args) {
        try {
            URLStreamHandler handler = new URLStreamHandler() {
                @Override
                protected URLConnection openConnection(URL u) throws IOException {
                    System.out.println(u.getPath());
                    System.out.println(u.getProtocol());
                    return new URLConnection(u) {
                        private InputStream inputStream;
                        @Override
                        public void connect() throws IOException {
                            inputStream = new URL("file:\\D:\\Users\\Adam\\Documents\\1.txt").openStream();
                        }

                        @Override
                        public InputStream getInputStream() throws IOException {
                            connect();
                            return inputStream;
                        }
                    };
                }
            };
            URL url = new URL(null, "fat-jar:sony.wav", handler);
            InputStream inputStream = url.openStream();
            byte[] bytes = inputStream.readAllBytes();
            System.out.println(new String(bytes));
            inputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
