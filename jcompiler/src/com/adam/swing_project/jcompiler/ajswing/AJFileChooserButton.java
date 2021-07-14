package com.adam.swing_project.jcompiler.ajswing;

import com.adam.swing_project.jcompiler.assertion.Assert;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 复合JButton、JFileChooser的文件选择按钮。
 */
public class AJFileChooserButton extends JButton {

    private final JFileChooser jFileChooser = new JFileChooser();
    private File fileChosen;
    private String filePathChosen;
    private List<AJFileChosenListener> fileChosenListeners;

    public AJFileChooserButton() {
        this(null, JFileChooser.FILES_ONLY);
    }

    public AJFileChooserButton(String text) {
        this(text, JFileChooser.FILES_ONLY);
    }

    public AJFileChooserButton(String text, int fileSelectionMode) {
        super(text);
        jFileChooser.setFileSelectionMode(fileSelectionMode);
        this.fileChosenListeners = new ArrayList<>();
        addActionListener(e -> {
            int result = jFileChooser.showOpenDialog(this);
            if(result == JFileChooser.APPROVE_OPTION) {
                File file = jFileChooser.getSelectedFile();
                if(file.exists()) {
                    this.fileChosen = file;
                    this.filePathChosen = fileChosen.getPath();
                    triggerFileChosenListeners(file);
                }
            }
        });
    }

    public File getFileChosen() {
        return fileChosen;
    }

    public String getFilePathChosen() {
        return filePathChosen;
    }

    public void addFileChosenListener(AJFileChosenListener listener) {
        Assert.notNull(listener, IllegalArgumentException.class, "AJFileChosenListener不能为Null");
        this.fileChosenListeners.add(listener);
    }

    private void triggerFileChosenListeners(File file) {
        for(AJFileChosenListener listener: fileChosenListeners) {
            listener.fileChosen(file);
        }
    }

    public void setCurrentDirectory(File file) {
        Assert.notNull(file);
        Assert.isTrue(file.isDirectory(), IllegalArgumentException.class, "param 'file' is not a directory");
        this.jFileChooser.setCurrentDirectory(file);
    }
}
