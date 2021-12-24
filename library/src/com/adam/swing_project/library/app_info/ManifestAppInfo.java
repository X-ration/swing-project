package com.adam.swing_project.library.app_info;

import java.io.IOException;
import java.io.InputStream;
import java.util.jar.Manifest;

public class ManifestAppInfo {

    private static final String APP_INFO_VERSION = "App-Info-Version";
    private static final String APP_INFO_NAME = "App-Info-Name";
    private final String appVersion;
    private final String appName;

    public ManifestAppInfo() throws IOException {
        Manifest manifest = readManifest();
        String version = manifest.getMainAttributes().getValue(APP_INFO_VERSION);
        if(version == null || version.equals("")) {
            version = "x.x.x";
        }
        String appName = manifest.getMainAttributes().getValue(APP_INFO_NAME);
        if(appName == null || appName.equals("")) {
            appName = "Unnamed app";
        }
        this.appVersion = version;
        this.appName = appName;
    }

    public String getAppName() {
        return appName;
    }

    public String getAppVersion() {
        return appVersion;
    }

    private Manifest readManifest() throws IOException {
        InputStream inputStream = ManifestAppInfo.class.getResourceAsStream("/META-INF/MANIFEST.MF");
        Manifest manifest = new Manifest(inputStream);
        inputStream.close();
        return manifest;
    }

}
