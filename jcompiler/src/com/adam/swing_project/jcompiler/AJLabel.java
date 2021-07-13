package com.adam.swing_project.jcompiler;

import javax.swing.*;

/**
 * 支持设置默认文字
 */
public class AJLabel extends JLabel {
    private final String defaultText;
    public AJLabel(String defaultText) {
        super(defaultText);
        this.defaultText = defaultText;
    }

    @Override
    public void setText(String text) {
        if(text == null || text.trim().equals("")) {
            super.setText(defaultText);
        } else {
            super.setText(text);
        }
    }
}
