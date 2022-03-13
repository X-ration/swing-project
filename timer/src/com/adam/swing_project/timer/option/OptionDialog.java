package com.adam.swing_project.timer.option;

import com.adam.swing_project.library.logger.Logger;
import com.adam.swing_project.library.logger.LoggerFactory;
import com.adam.swing_project.timer.component.ApplicationManager;
import com.adam.swing_project.timer.component.FileManager;
import com.adam.swing_project.timer.component.OptionManager;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class OptionDialog extends JDialog {
    private class OptionDialogItem {
        String indexName;
        Component component;
        Queue<TrackedOptionComponent> trackedOptionComponents = new LinkedList<>();
        OptionDialogItem(String indexName, Component component) {
            this.indexName = indexName;
            this.component = component;
        }
        void addTrackedOptionComponent(JComponent jComponent, String identifier) {
            trackedOptionComponents.add(new TrackedOptionComponent(jComponent, identifier));
        }
        void addTrackedOptionComponent(JComponent jComponent, String identifier, TrackedComponentAction trackedComponentAction) {
            TrackedOptionComponent trackedOptionComponent = new TrackedOptionComponent(jComponent, identifier);
            trackedOptionComponent.addTrackedComponentAction(trackedComponentAction);
            trackedOptionComponents.add(trackedOptionComponent);
        }
    }

    private class TrackedOptionComponent {
        JComponent jComponent;
        String optionIdentifier;
        Object initialValue;
        TrackedComponentProcessor processor;
        List<TrackedComponentAction> trackedComponentActions = new LinkedList<>();

        TrackedOptionComponent(JComponent jComponent, String optionIdentifier, TrackedComponentProcessor processor) {
            this.jComponent = jComponent;
            this.optionIdentifier = optionIdentifier;
            this.processor = (processor == null) ? DefaultTrackedComponentProcessor.INSTANCE : processor;
            this.initialValue = this.processor.getValue(jComponent);
        }

        TrackedOptionComponent(JComponent jComponent, String optionIdentifier) {
            this(jComponent, optionIdentifier, null);
        }

        boolean valueChanged() {
            return processor.valueChanged(initialValue, jComponent);
        }

        void onValueChange() {
            Object currentValue = processor.getValue(jComponent);
            OptionManager.getInstance().putOption(optionIdentifier, currentValue);
            if(!trackedComponentActions.isEmpty()) {
                for(TrackedComponentAction trackedComponentAction: trackedComponentActions) {
                    trackedComponentAction.onValueChange(jComponent, currentValue);
                }
            }
        }

        void addTrackedComponentAction(TrackedComponentAction trackedComponentAction) {
            trackedComponentActions.add(trackedComponentAction);
        }
    }

    /**
     * 针对JComponent组件获取其值、判断值改变的接口方法
     */
    private interface TrackedComponentProcessor {
        /**
         * 获得组件的最新值
         * @param jComponent
         * @return
         */
        Object getValue(JComponent jComponent);

        /**
         * 判断组件值是否发生改变
         * @param initialValue
         * @param jComponent
         * @return
         */
        boolean valueChanged(Object initialValue, JComponent jComponent);
    }

    private static class DefaultTrackedComponentProcessor implements TrackedComponentProcessor {
        static TrackedComponentProcessor INSTANCE = new DefaultTrackedComponentProcessor();

        @Override
        public Object getValue(JComponent jComponent) {
            if(jComponent instanceof JTextComponent) {
                return ((JTextComponent) jComponent).getText();
            }
            return null;
        }

        @Override
        public boolean valueChanged(Object initialValue, JComponent jComponent) {
            Object currentValue = getValue(jComponent);
            return !((currentValue == initialValue) || (currentValue != null && currentValue.equals(initialValue)));
        }

    }

    private interface TrackedComponentAction {
        void onValueChange(JComponent jComponent, Object currentValue);
    }

    private final Logger logger = LoggerFactory.getLogger(this);

    public OptionDialog(JFrame jFrame) {
        super(jFrame, "选项面板");
        setModal(true);

        CardLayout rightPaneLayout = new CardLayout();
        JPanel rightPane = new JPanel(rightPaneLayout);
        List<OptionDialogItem> optionDialogItems = collectDialogItems();
        String[] leftIndexArray = new String[optionDialogItems.size()];
        for(int i=0;i<optionDialogItems.size();i++) {
            OptionDialogItem item = optionDialogItems.get(i);
            leftIndexArray[i] = item.indexName;
            JPanel outerWrap = new JPanel(new BorderLayout()), innerWrap = new JPanel(new BorderLayout());
            outerWrap.add(item.component, BorderLayout.NORTH);
//            innerWrap.add(item.component, BorderLayout.WEST);
            rightPane.add(item.indexName, outerWrap);
        }
        JList<String> leftIndexPane = new JList<>(leftIndexArray);
        leftIndexPane.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JSplitPane rootPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, leftIndexPane,rightPane);
        leftIndexPane.addListSelectionListener(e -> {
            int selected = leftIndexPane.getSelectedIndex();
            if(selected != -1) {
                rightPaneLayout.show(rightPane, leftIndexPane.getSelectedValue());
            }
        });
        leftIndexPane.setSelectedIndex(0);
        rootPane.setDividerSize(5);
        rootPane.setDividerLocation(100);
        JPanel bottomButtonPanel = new JPanel(new BorderLayout()), bottomButtonInnerPanel = new JPanel(new GridBagLayout());
        JButton confirmButton = new JButton("确认"), cancelButton = new JButton("取消");
        ActionListener applyOptionsAction = e -> {
            for(OptionDialogItem optionDialogItem: optionDialogItems) {
                Queue<TrackedOptionComponent> trackedOptionComponents = optionDialogItem.trackedOptionComponents;
                while(!trackedOptionComponents.isEmpty()) {
                    TrackedOptionComponent trackedOptionComponent = trackedOptionComponents.poll();
                    if(trackedOptionComponent.valueChanged()) {
                        logger.logDebug("TrackedOptionComponent value change " + trackedOptionComponent.optionIdentifier);
                        trackedOptionComponent.onValueChange();
                    }
                }
            }
        };
        confirmButton.addActionListener(applyOptionsAction);
        confirmButton.addActionListener(e -> dispose());
        cancelButton.addActionListener(e -> dispose());
        GridBagConstraints gridBagConstraints = new GridBagConstraints(0,0,1,1,0,0,
                GridBagConstraints.CENTER,GridBagConstraints.NONE,new Insets(0,0,0,0),0,0);
        bottomButtonInnerPanel.add(confirmButton, gridBagConstraints);
        gridBagConstraints.gridx = 1;
        bottomButtonInnerPanel.add(cancelButton, gridBagConstraints);
        bottomButtonPanel.add(bottomButtonInnerPanel, BorderLayout.EAST);

        add(rootPane, BorderLayout.CENTER);
        add(bottomButtonPanel, BorderLayout.SOUTH);
        setSize(600,400);
        setLocationRelativeTo(jFrame);
    }

    private List<OptionDialogItem> collectDialogItems() {
        List<OptionDialogItem> itemList = new LinkedList<>();
        OptionDialogItem optionDialogItem;
        GridBagConstraints gridBagConstraints;

        JPanel generalPane = new JPanel(new GridBagLayout());
        JPanel workingDirComp = new JPanel(new GridBagLayout()), statComp = new JPanel(new BorderLayout());
        workingDirComp.setBorder(BorderFactory.createTitledBorder("工作目录"));
        statComp.setBorder(BorderFactory.createTitledBorder("默认统计行为"));
        JTextField workingDirField = new JTextField(30);
        workingDirField.setEditable(false);
        workingDirField.setText(OptionManager.getInstance().getOptionValue(OptionConstants.OPTION_ROOT_WORK_DIR, String.class));
        JButton workingDirButton = new JButton("浏览");
        workingDirButton.addActionListener(e -> {
            JFileChooser jFileChooser = new JFileChooser();
            jFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            jFileChooser.setDialogTitle("选择工作目录");
            int result = jFileChooser.showOpenDialog(generalPane);
            if(result == JFileChooser.APPROVE_OPTION) {
                File file = jFileChooser.getSelectedFile();
                workingDirField.setText(file.getAbsolutePath());
            }
        });
        gridBagConstraints = new GridBagConstraints(0,0,1,1,1,0,
                GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(0,0,0,0),0,0);
        workingDirComp.add(workingDirField, gridBagConstraints);
        gridBagConstraints = new GridBagConstraints(1,0,1,1,0,0,
                GridBagConstraints.CENTER,GridBagConstraints.NONE,new Insets(0,0,0,0),0,0);
        workingDirComp.add(workingDirButton, gridBagConstraints);
        gridBagConstraints = new GridBagConstraints(0,0,1,1,1,0,
                GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(0,0,0,0),0,0);
        generalPane.add(workingDirComp, gridBagConstraints);
        JPanel statInnerComp = new JPanel(new GridBagLayout());
        JRadioButton statDisableRadio = new JRadioButton("关闭统计功能"),
                statStartDayRadio = new JRadioButton("计时时长计入计时开始日"),
                statEndDayRadio = new JRadioButton("计时时长计入计时结束日");
        ButtonGroup statButtonGroup = new ButtonGroup();
        statButtonGroup.add(statDisableRadio);
        statButtonGroup.add(statStartDayRadio);
        statButtonGroup.add(statEndDayRadio);
        gridBagConstraints = new GridBagConstraints(0,0,1,1,0,0,
                GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(0,0,0,0),0,0);
        statInnerComp.add(statDisableRadio, gridBagConstraints);
        gridBagConstraints = new GridBagConstraints(0,1,1,1,0,0,
                GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(0,0,0,0),0,0);
        statInnerComp.add(statStartDayRadio, gridBagConstraints);
        gridBagConstraints = new GridBagConstraints(0,2,1,1,0,0,
                GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(0,0,0,0),0,0);
        statInnerComp.add(statEndDayRadio, gridBagConstraints);
        statComp.add(statInnerComp, BorderLayout.WEST);
        gridBagConstraints = new GridBagConstraints(0, 1, 1, 1, 1, 0,
                GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0,0,0,0),0,0);
        generalPane.add(statComp, gridBagConstraints);
        optionDialogItem = new OptionDialogItem("常规", generalPane);
        optionDialogItem.addTrackedOptionComponent(workingDirField, OptionConstants.OPTION_ROOT_WORK_DIR, new TrackedComponentAction() {
            @Override
            public void onValueChange(JComponent jComponent, Object currentValue) {
                String workingDir = (String) currentValue;
                FileManager.getInstance().updateAppRootDir(workingDir);
                ApplicationManager.getInstance().updateSnapshotDir();
            }
        });
        itemList.add(optionDialogItem);

        Box advancedPane = Box.createVerticalBox();
        JCheckBox advancedDebug = new JCheckBox("输出日志");
        advancedDebug.setEnabled(false);
        advancedDebug.setSelected(true);
        advancedPane.add(advancedDebug);
        optionDialogItem = new OptionDialogItem("高级", advancedPane);
        itemList.add(optionDialogItem);
        return itemList;
    }

}
