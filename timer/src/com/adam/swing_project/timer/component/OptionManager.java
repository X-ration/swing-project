package com.adam.swing_project.timer.component;

import com.adam.swing_project.library.assertion.Assert;
import com.adam.swing_project.library.logger.Logger;
import com.adam.swing_project.library.logger.LoggerFactory;
import com.adam.swing_project.timer.option.Option;
import com.adam.swing_project.timer.option.OptionChange;
import com.adam.swing_project.timer.option.OptionChangeListener;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class OptionManager {

    private static final OptionManager instance = new OptionManager();
    private final Map<String, Option<?>> optionMap = new HashMap<>();
    private final Logger logger = LoggerFactory.getLogger(OptionManager.class);
    private final Map<String, List<OptionChangeListener>> listenerMap = new HashMap<>();

    private OptionManager() {}

    public static OptionManager getInstance() {
        return instance;
    }

    public <T> T putOption(String identifier, T optionValue) {
        Assert.notNull(identifier, "identifier is null");
        Assert.notNull(optionValue, "option value is null");
        if(identifier.startsWith("root.")) {
            Assert.isTrue(optionValue instanceof String, "invalid root option");
            String oldValue = RootConfigStorage.getInstance().updateRootConfig(identifier, optionValue.toString());
            if(oldValue == null || !oldValue.equals(optionValue)) {
                logger.logInfo("Option '" + identifier + "' change: '" + oldValue + "' -> '" + optionValue + "'");
                triggerListener(identifier, oldValue, optionValue);
                return (T) oldValue;
            } else {
                return null;
            }
        }
        T oldValue = (T) optionMap.get(identifier);
        if(oldValue == null || !oldValue.equals(optionValue)) {
            Option<?> option = new Option<>(identifier, optionValue);
            optionMap.put(identifier, option);
            logger.logInfo("Option '" + identifier + "' change: '" + oldValue + "' -> '" + optionValue + "'");
            triggerListener(identifier, oldValue, optionValue);
            return oldValue;
        } else {
            return null;
        }
    }

    private <T> void triggerListener(String identifier, T oldValue, T newValue) {
        List<OptionChangeListener> listeners = listenerMap.get(identifier);
        if(listeners != null) {
            for(OptionChangeListener listener: listeners) {
                listener.onChange(new OptionChange<>(identifier, oldValue, newValue));
            }
        }
    }

    public <T> T getOptionValue(String identifier, Class<T> optionClass) {
        Assert.notNull(identifier, "identifier is null");
        if(identifier.startsWith("root.")) {
            String rootConfigValue = RootConfigStorage.getInstance().getRootConfig(identifier);
            if(rootConfigValue != null) {
                return (T) rootConfigValue;
            } else {
                return null;
            }
        }
        Option<?> option = optionMap.get(identifier);
        if(option != null) {
            return (T) option.getValue();
        }
        return null;
    }

    public <T> T getOptionValueOrDefault(String identifier, Class<T> optionClass, T defaultValue) {
        T optionValue = getOptionValue(identifier, optionClass);
        if(optionValue == null) {
            putOption(identifier, defaultValue);
            optionValue = getOptionValue(identifier, optionClass);
        }
        return optionValue;
    }

    public void bindOptionChangeListener(String identifier, OptionChangeListener listener) {
        Assert.notNull(listener);
        listenerMap.putIfAbsent(identifier, new LinkedList<>());
        List<OptionChangeListener> listeners = listenerMap.get(identifier);
        listeners.add(listener);
    }
}
