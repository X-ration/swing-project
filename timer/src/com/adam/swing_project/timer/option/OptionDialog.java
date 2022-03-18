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
import java.util.*;
import java.util.List;

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
        void addTrackedOptionComponent(JComponent jComponent, String identifier, TrackedComponentProcessor trackedComponentProcessor) {
            TrackedOptionComponent trackedOptionComponent = new TrackedOptionComponent(jComponent, identifier, trackedComponentProcessor);
            trackedOptionComponents.add(trackedOptionComponent);
        }
        void addTrackedOptionComponent(JComponent jComponent, String identifier, TrackedComponentAction trackedComponentAction) {
            addTrackedOptionComponent(jComponent, identifier, null, trackedComponentAction);
        }
        void addTrackedOptionComponent(JComponent jComponent, String identifier, TrackedComponentProcessor trackedComponentProcessor, TrackedComponentAction trackedComponentAction) {
            TrackedOptionComponent trackedOptionComponent = new TrackedOptionComponent(jComponent, identifier, trackedComponentProcessor);
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
            if(jComponent instanceof JRadioButton) {
                return ((JRadioButton)jComponent).isSelected();
            }
            if(jComponent instanceof JCheckBox) {
                return ((JCheckBox)jComponent).isSelected();
            }
            return null;
        }

        @Override
        public boolean valueChanged(Object initialValue, JComponent jComponent) {
            Object currentValue = getValue(jComponent);
            return !((currentValue == initialValue) || (currentValue != null && currentValue.equals(initialValue)));
        }

    }

    private static class DefaultTrackedComponentProcessorForRoot implements TrackedComponentProcessor {
        static TrackedComponentProcessor INSTANCE = new DefaultTrackedComponentProcessorForRoot();

        @Override
        public Object getValue(JComponent jComponent) {
            return String.valueOf(DefaultTrackedComponentProcessor.INSTANCE.getValue(jComponent));
        }

        @Override
        public boolean valueChanged(Object initialValue, JComponent jComponent) {
            return DefaultTrackedComponentProcessor.INSTANCE.valueChanged(initialValue, jComponent);
        }
    }

    /**
     * 单选按钮的定制处理器：一个单选按钮组中只有一个按钮可被选中，只认可其中由未选中到选中的按钮触发事件。
     */
    private static class SingleChosenRadioButtonProcessor<T> implements TrackedComponentProcessor {

        private final Map<JRadioButton, T> radioValueMap;

        SingleChosenRadioButtonProcessor(Map<JRadioButton, T> radioValueMap) {
            this.radioValueMap = radioValueMap;
        }

        @Override
        public Object getValue(JComponent jComponent) {
            return radioValueMap.get(((JRadioButton) jComponent));
        }

        @Override
        public boolean valueChanged(Object initialValue, JComponent jComponent) {
            return ((JRadioButton)jComponent).isSelected();
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
        OptionConstants.StatDefaultMethod generalStatDefault = OptionManager.getInstance().getOptionValueOrPutDefault(
                OptionConstants.OPTION_GENERAL_STAT_DEFAULT, OptionConstants.StatDefaultMethod.class,
                OptionConstants.StatDefaultMethod.STAT_BY_START_DAY);
        Map<JRadioButton, OptionConstants.StatDefaultMethod> statDefaultRadioMap = new HashMap<>();
        statDefaultRadioMap.put(statDisableRadio, OptionConstants.StatDefaultMethod.DISABLED);
        statDefaultRadioMap.put(statStartDayRadio, OptionConstants.StatDefaultMethod.STAT_BY_START_DAY);
        statDefaultRadioMap.put(statEndDayRadio, OptionConstants.StatDefaultMethod.STAT_BY_END_DAY);
        switch (generalStatDefault) {
            case DISABLED:
                statDisableRadio.setSelected(true);
                break;
            case STAT_BY_START_DAY:
                statStartDayRadio.setSelected(true);
                break;
            case STAT_BY_END_DAY:
                statEndDayRadio.setSelected(true);
                break;
        }
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
        optionDialogItem.addTrackedOptionComponent(workingDirField, OptionConstants.OPTION_ROOT_WORK_DIR, (jComponent, currentValue) -> {
            String workingDir = (String) currentValue;
            FileManager.getInstance().updateAppRootDir(workingDir);
            ApplicationManager.getInstance().updateSnapshotDir();
        });
        optionDialogItem.addTrackedOptionComponent(statDisableRadio, OptionConstants.OPTION_GENERAL_STAT_DEFAULT,
                new SingleChosenRadioButtonProcessor<>(statDefaultRadioMap));
        optionDialogItem.addTrackedOptionComponent(statStartDayRadio, OptionConstants.OPTION_GENERAL_STAT_DEFAULT,
                new SingleChosenRadioButtonProcessor<>(statDefaultRadioMap));
        optionDialogItem.addTrackedOptionComponent(statEndDayRadio, OptionConstants.OPTION_GENERAL_STAT_DEFAULT,
                new SingleChosenRadioButtonProcessor<>(statDefaultRadioMap));
        itemList.add(optionDialogItem);

        JPanel advancedPane = new JPanel(new GridBagLayout());
        JPanel advancedLogComp = new JPanel(new BorderLayout()), advancedLogInnerComp = new JPanel(new GridBagLayout());
        advancedLogComp.setBorder(BorderFactory.createTitledBorder("日志选项"));
        JCheckBox advancedLogFileEnabled = new JCheckBox("输出日志文件(重启生效)"),
                advancedLogDebugEnabled = new JCheckBox("启用调试级别");
        boolean logFileEnabled = Boolean.parseBoolean(OptionManager.getInstance().getOptionValueOrPutDefault(OptionConstants.OPTION_ROOT_LOG_FILE_ENABLED, String.class, "true"));
        boolean logDebugEnabled = Boolean.parseBoolean(OptionManager.getInstance().getOptionValueOrPutDefault(OptionConstants.OPTION_ROOT_LOG_DEBUG_ENABLED, String.class, "false"));
        advancedLogFileEnabled.setSelected(logFileEnabled);
        advancedLogDebugEnabled.setSelected(logDebugEnabled);
        gridBagConstraints = new GridBagConstraints(0,0,1,1,0,0,
                GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(0,0,0,0),0,0);
        advancedLogInnerComp.add(advancedLogFileEnabled, gridBagConstraints);
        gridBagConstraints = new GridBagConstraints(0,1,1,1,0,0,
                GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(0,0,0,0),0,0);
        advancedLogInnerComp.add(advancedLogDebugEnabled, gridBagConstraints);
        advancedLogComp.add(advancedLogInnerComp, BorderLayout.WEST);
        gridBagConstraints = new GridBagConstraints(0,0,1,1,1,0,
                GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(0,0,0,0),0,0);
        advancedPane.add(advancedLogComp, gridBagConstraints);
        optionDialogItem = new OptionDialogItem("高级", advancedPane);
        optionDialogItem.addTrackedOptionComponent(advancedLogFileEnabled, OptionConstants.OPTION_ROOT_LOG_FILE_ENABLED, DefaultTrackedComponentProcessorForRoot.INSTANCE);
        optionDialogItem.addTrackedOptionComponent(advancedLogDebugEnabled, OptionConstants.OPTION_ROOT_LOG_DEBUG_ENABLED, DefaultTrackedComponentProcessorForRoot.INSTANCE, new TrackedComponentAction() {
            @Override
            public void onValueChange(JComponent jComponent, Object currentValue) {
                boolean logDebugEnabled = Boolean.parseBoolean((String) currentValue);
                if(logDebugEnabled) {
                    LoggerFactory.setupGlobalLevel(Logger.LogLevel.DEBUG);
                } else {
                    LoggerFactory.setupGlobalLevel(Logger.LogLevel.INFO);
                }
            }
        });
        itemList.add(optionDialogItem);
        return itemList;
    }

}
