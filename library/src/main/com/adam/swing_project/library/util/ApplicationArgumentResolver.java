package com.adam.swing_project.library.util;

import com.adam.swing_project.library.assertion.Assert;
import com.adam.swing_project.library.logger.ConsoleLogger;
import com.adam.swing_project.library.logger.Logger;
import com.adam.swing_project.library.logger.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class ApplicationArgumentResolver {

    private final Logger logger = LoggerFactory.getLogger(this);
    private final Map<String, String> resolvedMap = new HashMap<>();
    public static final String ARGUMENT_RESOLVER_LOG_LEVEL = "argumentResolverLogLevel";

    public ApplicationArgumentResolver() {}

    public ApplicationArgumentResolver(String[] args) {
        resolveArgs(args);
    }

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
                    Assert.isTrue(++i != args.length, "Uncomplete option '" + optionName + "'");
                    String optionValue = args[i];
                    registerOption(optionName, optionValue);
                }
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
        logger.logDebug("Registered option '" + key + "=" + value + "'");
        resolvedMap.put(key, value);
    }

}
