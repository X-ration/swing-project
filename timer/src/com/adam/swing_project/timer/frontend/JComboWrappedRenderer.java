package com.adam.swing_project.timer.frontend;

import javax.swing.*;
import java.awt.*;

public class JComboWrappedRenderer implements ListCellRenderer{

    private final ListCellRenderer listCellRenderer;
    private final CustomRender customRender;

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        JLabel jLabel = (JLabel) listCellRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        customRender.render(jLabel);
        return jLabel;
    }

    public interface CustomRender {
        void render(JLabel jLabel);
    }

    public JComboWrappedRenderer(JComboBox jComboBox, CustomRender customRender) {
        this.listCellRenderer = jComboBox.getRenderer();
        this.customRender = customRender;
    }

}
