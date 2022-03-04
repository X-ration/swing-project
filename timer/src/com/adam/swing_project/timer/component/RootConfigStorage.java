package com.adam.swing_project.timer.component;

import com.adam.swing_project.library.assertion.Assert;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class RootConfigStorage {

    private final File rootDir = new File(System.getProperty("user.home") + File.separator + "swing-timer");
    private final File rootFile = new File(rootDir, "root_config");
    private final Map<String, String> rootConfigMap = new HashMap<>();
    private static final RootConfigStorage instance = new RootConfigStorage();

    private RootConfigStorage() {
        try {
            if (!rootDir.exists()) {
                rootDir.mkdirs();
            }
            if (!rootFile.exists()) {
                rootFile.createNewFile();
            }
            readRootConfigFromFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static RootConfigStorage getInstance() {
        return instance;
    }

    public String getRootConfig(String rootConfigKey) {
        return rootConfigMap.get(rootConfigKey);
    }

    public String updateRootConfig(String rootConfigKey, String rootConfigValue) {
        String oldValue = rootConfigMap.get(rootConfigKey);
        if(oldValue != null && oldValue.equals(rootConfigValue)) {
            return null;
        }
        rootConfigMap.put(rootConfigKey, rootConfigValue);
        try {
            writeRootConfigToFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return oldValue;
    }

    private void readRootConfigFromFile() throws IOException {
        FileReader fileReader = new FileReader(rootFile);
        StringBuilder sb = new StringBuilder();
        char[] buffer = new char[1024];
        int n;
        while((n = fileReader.read(buffer)) != -1) {
            sb.append(buffer, 0, n);
        }
        fileReader.close();
        String[] kvs = sb.toString().split(System.lineSeparator());
        for(int i=0;i<kvs.length;i++) {
            String[] kv = kvs[i].split("=");
            if(kv.length == 2) {
                rootConfigMap.put(kv[0], kv[1]);
            }
        }
    }

    private void writeRootConfigToFile() throws IOException{
        StringBuilder sb = new StringBuilder();
        for(Map.Entry<String,String> entry: rootConfigMap.entrySet()) {
            sb.append(entry.getKey()).append('=').append(entry.getValue()).append(System.lineSeparator());
        }
        FileWriter fileWriter = new FileWriter(rootFile);
        fileWriter.write(sb.toString());
        fileWriter.close();
    }
}
