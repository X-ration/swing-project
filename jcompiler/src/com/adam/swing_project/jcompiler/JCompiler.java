package com.adam.swing_project.jcompiler;

import com.adam.swing_project.jcompiler.ajswing.AJFileChooserButton;
import com.adam.swing_project.jcompiler.ajswing.AJLabel;
import com.adam.swing_project.jcompiler.ajswing.AJScrollPane;
import com.adam.swing_project.jcompiler.internal_compiler.CompileEvent;
import com.adam.swing_project.jcompiler.internal_compiler.CompileEventType;
import com.adam.swing_project.jcompiler.internal_compiler.CompileListener;
import com.adam.swing_project.jcompiler.internal_compiler.InternalCompiler;

import javax.swing.*;
import java.awt.*;
import java.io.File;

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
        projectDirButton.addFileChosenListener(outputDirButton::setCurrentDirectory);

        baseBox.add(Box.createVerticalStrut(20));
        JButton compileButton = new JButton("编译");
        baseBox.add(compileButton);

        baseBox.add(Box.createVerticalStrut(20));
        Box compileConsoleBox = Box.createHorizontalBox();
        JTextArea compileConsole = new JTextArea();
        compileConsole.setEditable(false);
        compileConsole.setVisible(false);
        compileConsole.setLineWrap(true);
        compileConsole.setColumns(50);
        compileConsole.setRows(5);
        compileConsole.setFont(new Font("Tahoma", Font.ITALIC, 10));
        AJScrollPane compileConsolePane = new AJScrollPane(compileConsole, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        compileConsoleBox.add(compileConsolePane);
        compileConsolePane.setWheelScrollingEnabled(true);
        compileConsolePane.setVerticalAutoScroll();
        baseBox.add(compileConsoleBox);
        internalCompiler.addCompileLoggerListener(compileLog -> {
            SwingUtilities.invokeLater(()->{
                compileConsole.append(compileLog);
            });
        });
        internalCompiler.addCompileListener(new CompileListener() {
            @Override
            public void onEvent(CompileEvent compileEvent) {
                compileButton.setEnabled(false);
            }
            @Override
            public boolean filterEvent(CompileEvent compileEvent) {
                return compileEvent.getType() == CompileEventType.STARTED;
            }
        });
        internalCompiler.addCompileListener(new CompileListener() {
            @Override
            public void onEvent(CompileEvent compileEvent) {
                compileButton.setEnabled(true);
            }

            @Override
            public boolean filterEvent(CompileEvent compileEvent) {
                return compileEvent.getType() == CompileEventType.FINISHED;
            }
        });
        compileButton.addActionListener(e -> {
            compileConsole.setVisible(true);
            internalCompiler.setSrcDir(projectDirButton.getFileChosen());
            internalCompiler.setCompileDir(outputDirButton.getFileChosen());
            internalCompiler.compile();
        });

        contentPane.add(baseBox);
        Dimension minDimension = new Dimension(600, 350);
        jFrame.setMinimumSize(minDimension);
        jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jFrame.setVisible(true);
    }
}
