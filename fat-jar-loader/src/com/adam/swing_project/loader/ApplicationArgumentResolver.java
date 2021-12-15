package com.adam.swing_project.loader;

import java.util.HashMap;
import java.util.Map;

public class ApplicationArgumentResolver {

    private final LoaderLogger logger = LoaderLogger.createLogger(this);
    private final Map<String, String> resolvedMap = new HashMap<>();
    public static final String ARGUMENT_RESOLVER_LOG_LEVEL = "argumentResolverLogLevel";

    /**
     * 约定两种格式：
     * 其一： -<option-name>=<option-value>
     * 其二： -<option-name> <option-value>
     * @param args
     */
    public void resolveArgs(String[] args) {
        for(int i=0;i<args.length;i++) {
            String arg = args[i];
            if(arg.startsWith("-")) {
                int assignIndex = arg.indexOf('=');
                if(assignIndex != -1) {
                    String optionName = arg.substring(1, assignIndex),
                            optionValue = arg.substring(assignIndex + 1);
                    registerOption(optionName, optionValue);
                } else {
                    String optionName = arg.substring(1);
                    LoaderAssert.isTrue(++i != args.length, "Uncomplete option '" + optionName + "'");
                    String optionValue = args[i];
                    registerOption(optionName, optionValue);
                }
            }
        }
        LoaderLogger.LogLevel logLevel;
        if(containsOption(ARGUMENT_RESOLVER_LOG_LEVEL)) {
            String optionValue = getOptionValue(ARGUMENT_RESOLVER_LOG_LEVEL);
            try {
                logLevel = LoaderLogger.LogLevel.valueOf(optionValue);
            } catch (IllegalArgumentException e) {
                logger.logWarning("Incorrect option value '" + optionValue + "' for '" + ARGUMENT_RESOLVER_LOG_LEVEL + "'");
                return;
            }
            logger.setLogLevel(logLevel);
            if(logger.debugEnabled()) {
                StringBuilder stringBuilder = new StringBuilder("Resolved arguments: ");
                for(Map.Entry<String, String> entry: resolvedMap.entrySet()) {
                    stringBuilder.append(entry.getKey()).append('=').append(entry.getValue()).append(' ');
                }
                logger.logDebug(stringBuilder.toString());
            }
        }
    }

    public String getOptionValue(String optionName) {
        return resolvedMap.get(optionName);
    }

    public boolean containsOption(String optionName) {
        return resolvedMap.containsKey(optionName);
    }

    private void registerOption(String key, String value) {
        resolvedMap.put(key, value);
    }

}
