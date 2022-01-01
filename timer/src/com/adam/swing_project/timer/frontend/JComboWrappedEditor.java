package com.adam.swing_project.timer.frontend;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class JComboWrappedEditor implements ComboBoxEditor {

    public interface CustomRender {
        void render(JTextField jTextField);
    }

    private final ComboBoxEditor comboBoxEditor;
    private final CustomRender customRender;

    public JComboWrappedEditor(JComboBox jComboBox, CustomRender customRender) {
        this.comboBoxEditor = jComboBox.getEditor();
        this.customRender = customRender;
    }

    @Override
    public Component getEditorComponent() {
        JTextField jTextField = (JTextField) comboBoxEditor.getEditorComponent();
        customRender.render(jTextField);
        return jTextField;
    }

    @Override
    public void setItem(Object anObject) {
        comboBoxEditor.setItem(anObject);
    }

    @Override
    public Object getItem() {
        return comboBoxEditor.getItem();
    }

    @Override
    public void selectAll() {
        comboBoxEditor.selectAll();
    }

    @Override
    public void addActionListener(ActionListener l) {
        comboBoxEditor.addActionListener(l);
    }

    @Override
    public void removeActionListener(ActionListener l) {
        comboBoxEditor.removeActionListener(l);
    }
}
