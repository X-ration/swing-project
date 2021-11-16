package com.adam.swing_project.timer.component;

import java.util.HashMap;
import java.util.Map;

public class ConfigManager {

    private static final ConfigManager instance = new ConfigManager();
    private final Map<String, String> configMap = new HashMap<>();

    public static ConfigManager getInstance() {
        return instance;
    }

    public void addConfig(String key, String value) {
        configMap.put(key, value);
    }

    public String getConfig(String key) {
        return configMap.get(key);
    }

    public void loadStartupArgs(String[] args) {
        for(String arg: args) {
            String[] array = arg.split("=");
            if(array.length == 2) {
                configMap.put(array[0], array[1]);
            }
        }
    }

    public static void main(String[] args) {
        StringBuilder sb = new StringBuilder();
        sb.append("Command line args:");
        for(String arg: args) {
            sb.append(arg).append(",");
        }
        sb.deleteCharAt(sb.length()-1);
        System.out.println(sb.toString());
        ConfigManager.getInstance().loadStartupArgs(args);
        System.out.println(ConfigManager.getInstance().configMap);
    }

}
