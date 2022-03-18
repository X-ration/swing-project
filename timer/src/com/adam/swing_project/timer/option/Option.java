package com.adam.swing_project.timer.option;

public class Option<T> {

    private String identifier;
    private T value;

    public Option(String identifier, T value) {
        this.identifier = identifier;
        this.value = value;
    }

    public String getIdentifier() {
        return identifier;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }
}
