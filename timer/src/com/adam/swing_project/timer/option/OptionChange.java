package com.adam.swing_project.timer.option;

public class OptionChange<T> {

    private String identifier;
    private T oldValue, newValue;

    public OptionChange(String identifier, T oldValue, T newValue) {
        this.identifier = identifier;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public String getIdentifier() {
        return identifier;
    }

    public T getOldValue() {
        return oldValue;
    }

    public T getNewValue() {
        return newValue;
    }
}
