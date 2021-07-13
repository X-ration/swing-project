package com.adam.swing_project.jcompiler;

import javax.swing.*;
import java.awt.*;

public class JCompiler {
    public static void main(String[] args) {
        JFrame jFrame = new JFrame("JCompiler");
        Container contentPane = jFrame.getContentPane();
        Box baseBox = Box.createVerticalBox();

        final String EMPTY_LABEL = "<empty>";
        InternalCompiler internalCompiler = new InternalCompiler();

        JLabel projectDirLabel = new AJLabel("项目路径")
                , projectDir = new AJLabel(EMPTY_LABEL);
        baseBox.add(projectDirLabel);
        baseBox.add(projectDir);
        AJFileChooserButton projectDirButton = new AJFileChooserButton("选择项目路径", JFileChooser.DIRECTORIES_ONLY);
        baseBox.add(projectDirButton);
        projectDirButton.addFileChosenListener(file -> projectDir.setText(file.getPath()));

        baseBox.add(Box.createVerticalStrut(20));
        JLabel outputDirLabel = new AJLabel("编译路径")
                , outputDir = new AJLabel(EMPTY_LABEL);
        baseBox.add(outputDirLabel);
        baseBox.add(outputDir);
        AJFileChooserButton outputDirButton = new AJFileChooserButton("选择编译路径", JFileChooser.DIRECTORIES_ONLY);
        baseBox.add(outputDirButton);
        outputDirButton.addFileChosenListener(file -> outputDir.setText(file.getPath()));

        baseBox.add(Box.createVerticalStrut(20));
        JButton compileButton = new JButton("编译");
        baseBox.add(compileButton);

        baseBox.add(Box.createVerticalStrut(20));
        Box compileConsoleBox = Box.createHorizontalBox();
        JTextArea compileConsole = new JTextArea();
        JScrollPane compileConsolePane = new JScrollPane(compileConsole);
        compileConsoleBox.add(compileConsolePane);
        compileConsole.setEditable(false);
        compileConsole.setLineWrap(true);
        compileConsole.setColumns(50);
        compileConsolePane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        compileConsolePane.setWheelScrollingEnabled(true);
        baseBox.add(compileConsoleBox);
        internalCompiler.addCompileLoggerListener(compileLog -> {
//            compileConsole.append(compileLog);
//            compileConsole.append(System.lineSeparator());
            compileConsole.insert(System.lineSeparator(),0);
            compileConsole.insert(compileLog,0);
            JScrollBar compileConsoleScrollBar = compileConsolePane.getVerticalScrollBar();
            compileConsole.paintImmediately(compileConsole.getBounds());
            //todo 实时滚动还有点问题
//            compileConsoleScrollBar.setValue(compileConsoleScrollBar.getMaximum());
//            compileConsoleScrollBar.paintImmediately(compileConsoleScrollBar.getBounds());
        });
        compileButton.addActionListener(e -> {
            compileConsole.setVisible(true);
            internalCompiler.setSrcDir(projectDirButton.getFileChosen());
            internalCompiler.setCompileDir(outputDirButton.getFileChosen());
            internalCompiler.compile();
        });


        contentPane.add(baseBox);
        Dimension minDimension = new Dimension(600, 400);
        jFrame.setMinimumSize(minDimension);
        jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jFrame.setVisible(true);
    }
}
