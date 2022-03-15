package com.adam.swing_project.timer.component;

import com.adam.swing_project.library.util.ApplicationArgumentResolver;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class RootConfigStorage {

    private final File rootDir = new File(System.getProperty("user.home") + File.separator + "swing-timer");
    private File rootFile;
    private final Map<String, String> rootConfigMap = new HashMap<>();
    private static final RootConfigStorage instance = new RootConfigStorage();

    private RootConfigStorage() {
    }

    public void init(ApplicationArgumentResolver argumentResolver) {
        String rootFileName = "root_config";
        String env = argumentResolver.getOptionValue("env");
        if(env != null) {
            rootFileName += ("-" + env);
        }
        rootFile = new File(rootDir, rootFileName);
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

    public String getRootConfigOrPutDefault(String rootConfigKey, String defaultValue) {
        String value = getRootConfig(rootConfigKey);
        if(value == null) {
            value = defaultValue;
            updateRootConfig(rootConfigKey, value);
        }
        return value;
    }

    public String updateRootConfig(String rootConfigKey, String rootConfigValue) {
        String oldValue = rootConfigMap.get(rootConfigKey);
        if(oldValue != null && oldValue.equals(rootConfigValue)) {
            return oldValue;
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
