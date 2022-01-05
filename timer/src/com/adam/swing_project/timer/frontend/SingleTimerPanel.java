package com.adam.swing_project.timer.frontend;

import com.adam.swing_project.library.ajswing.AJStatusButton;
import com.adam.swing_project.library.ajswing.AJStatusButtonBinaryStatus;
import com.adam.swing_project.library.datetime.Time;
import com.adam.swing_project.library.logger.Logger;
import com.adam.swing_project.library.timer.Timer;
import com.adam.swing_project.library.util.DateTimeUtil;
import com.adam.swing_project.timer.app_info.TimerAppInfo;
import com.adam.swing_project.timer.component.*;
import com.adam.swing_project.timer.thread.AudioThread;
import com.adam.swing_project.timer.thread.ThreadManager;

import javax.swing.*;
import java.awt.*;

/**
 * 计时器面板前端组件，适配新的Timer类
 */
public class SingleTimerPanel extends JPanel {

    private final JLabel countingLabel, infoLabel, nameLabel;
    private final AJStatusButton timerMainButton, stopButton, editButton, deleteButton;
    private final JFrame parentJFrame;

    private final Timer timer;
    private final AudioThread audioThread;
    private final Logger logger = Logger.createLogger(this);

    public enum TimerMainButtonStatus {
        INITIAL, START, PAUSE, STOP_PLAY
    }

    public SingleTimerPanel(JFrame jFrame) {
        this(jFrame, new Timer("计时器"));
    }

    public SingleTimerPanel(JFrame jFrame, Timer timer) {
        this.timer = timer;
        this.countingLabel = new JLabel();
        this.infoLabel = new JLabel();
        this.nameLabel = new JLabel();
        TimerMainButtonStatus timerMainButtonInitialStatus = TimerMainButtonStatus.INITIAL;
        AJStatusButtonBinaryStatus stopButtonInitialStatus = AJStatusButtonBinaryStatus.CLOSED,
                editButtonInitialStatus = AJStatusButtonBinaryStatus.OPEN,
                deleteButtonInitialStatus = AJStatusButtonBinaryStatus.OPEN;
        this.audioThread = ThreadManager.getInstance().getAudioThread();

        this.timerMainButton = new AJStatusButton(TimerMainButtonStatus.class, timerMainButtonInitialStatus);
        this.stopButton = new AJStatusButton(AJStatusButtonBinaryStatus.class, stopButtonInitialStatus);
        this.editButton = new AJStatusButton(AJStatusButtonBinaryStatus.class, editButtonInitialStatus);
        this.deleteButton = new AJStatusButton(AJStatusButtonBinaryStatus.class, deleteButtonInitialStatus);
        this.parentJFrame = jFrame;

        syncCountingLabel();
        syncInfoLabelGroup();
        timerMainButton.bind(TimerMainButtonStatus.INITIAL, ajStatusButton -> {
            ajStatusButton.setEnabled(false);
            ajStatusButton.setIcon(IconManager.play24());
        }, e -> {});
        timerMainButton.bind(TimerMainButtonStatus.START, ajStatusButton -> {
            ajStatusButton.setEnabled(true);
            ajStatusButton.setIcon(IconManager.play24());
        }, e -> timer.start());
        timerMainButton.bind(TimerMainButtonStatus.PAUSE, ajStatusButton -> {
            ajStatusButton.setEnabled(true);
            ajStatusButton.setIcon(IconManager.pause24());
        }, e -> timer.pause());
        timerMainButton.bind(TimerMainButtonStatus.STOP_PLAY, ajStatusButton -> {
            ajStatusButton.setEnabled(true);
            ajStatusButton.setIcon(IconManager.noRecord24());
        }, e -> {
            audioThread.stopPlay();
            timerMainButton.changeStatus(TimerMainButtonStatus.START);
        });
        stopButton.setIcon(IconManager.stop24());
        stopButton.bind(AJStatusButtonBinaryStatus.CLOSED, ajStatusButton -> ajStatusButton.setEnabled(false), e -> {});
        stopButton.bind(AJStatusButtonBinaryStatus.OPEN, ajStatusButton -> ajStatusButton.setEnabled(true), e -> timer.stop());
        editButton.setIcon(IconManager.edit24());
        editButton.bind(AJStatusButtonBinaryStatus.OPEN, ajStatusButton -> ajStatusButton.setEnabled(true), e -> showResetTimeDialog());
        editButton.bind(AJStatusButtonBinaryStatus.CLOSED, ajStatusButton -> ajStatusButton.setEnabled(false), e -> {});
        deleteButton.bind(AJStatusButtonBinaryStatus.OPEN, ajStatusButton -> {
            ajStatusButton.setIcon(IconManager.trash24());
            ajStatusButton.setEnabled(true);
        }, e -> {
            TimerPanel timerPanel = ApplicationManager.getInstance().getProgramGlobalObject(TimerPanel.class);
            timerPanel.removeSingleTimerPanel(this);
            timer.terminate();
        });
        deleteButton.bind(AJStatusButtonBinaryStatus.CLOSED, ajStatusButton -> ajStatusButton.setEnabled(false), e -> {});

        timer.addStateChangeListener(((oldStatus, newStatus) -> {
            switch (newStatus) {
                case READY:
                    timerMainButton.changeStatus(TimerMainButtonStatus.START);
                    stopButton.changeStatus(AJStatusButtonBinaryStatus.CLOSED);
                    editButton.changeStatus(AJStatusButtonBinaryStatus.OPEN);
                    deleteButton.changeStatus(AJStatusButtonBinaryStatus.OPEN);
                    syncInfoLabelGroup();
                    break;
                case INITIALIZED:
                    timerMainButton.changeStatus(TimerMainButtonStatus.INITIAL);
                    stopButton.changeStatus(AJStatusButtonBinaryStatus.CLOSED);
                    editButton.changeStatus(AJStatusButtonBinaryStatus.OPEN);
                    deleteButton.changeStatus(AJStatusButtonBinaryStatus.OPEN);
                    syncInfoLabelGroup();
                    break;
                case RUNNING:
                    timerMainButton.changeStatus(TimerMainButtonStatus.PAUSE);
                    stopButton.changeStatus(AJStatusButtonBinaryStatus.OPEN);
                    editButton.changeStatus(AJStatusButtonBinaryStatus.CLOSED);
                    deleteButton.changeStatus(AJStatusButtonBinaryStatus.CLOSED);
                    break;
                case PAUSED:
                    timerMainButton.changeStatus(TimerMainButtonStatus.START);
                    break;
                case STOPPED:
                    timerMainButton.changeStatus(TimerMainButtonStatus.START);
                    stopButton.changeStatus(AJStatusButtonBinaryStatus.CLOSED);
                    editButton.changeStatus(AJStatusButtonBinaryStatus.OPEN);
                    deleteButton.changeStatus(AJStatusButtonBinaryStatus.OPEN);
                    break;
                case TIME_UP:
                    timerMainButton.changeStatus(TimerMainButtonStatus.STOP_PLAY);
                    stopButton.changeStatus(AJStatusButtonBinaryStatus.CLOSED);
                    editButton.changeStatus(AJStatusButtonBinaryStatus.CLOSED);
                    deleteButton.changeStatus(AJStatusButtonBinaryStatus.OPEN);
                    audioThread.clearListener();
                    audioThread.registerListener(audioThread.new AudioControllerListener() {
                        @Override
                        public void playStopped() {
                            timer.timeUpClear();
                        }
                        @Override
                        public void playPaused() {
                        }
                    });
                    audioThread.chooseSoundFile("/audio/Listen.wav");
                    TimerAppInfo timerAppInfo = ApplicationManager.getInstance().getProgramGlobalObject(TimerAppInfo.class);
                    String trayMessageTitle = timerAppInfo.getTitleString(), trayMessageBody = timer.getTimerName() + " (" + DateTimeUtil.wrapTimeHourToSecond(timer.getResetTime()) + ") 时间到啦！";
                    TrayIconManager.getInstance().pushMessageToTrayIcon(trayMessageTitle, trayMessageBody, TrayIcon.MessageType.INFO);
                    break;
            }
        }));
        timer.addCountingListener(this::syncCountingLabel);

        timer.fireStateChanged();
        timer.fireCountingUpdated();

        infoLabel.setFont(FontManager.getByNameStyleSize("Consolas", Font.PLAIN, 24));
        infoLabel.setHorizontalAlignment(JLabel.CENTER);
        countingLabel.setFont(FontManager.getByNameStyleSize("Consolas", Font.BOLD, 60));
        countingLabel.setHorizontalAlignment(JLabel.CENTER);
        nameLabel.setHorizontalAlignment(JLabel.CENTER);
        nameLabel.setFont(FontManager.getByNameStyleSize("宋体", Font.BOLD, 20));
        JPanel southPanel = new JPanel();
        GridBagLayout southLayout = new GridBagLayout();
        southPanel.setLayout(southLayout);
        GridBagConstraints southConstraints = new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.CENTER,
                GridBagConstraints.NONE, new Insets(0,0,0,0), 0, 0);
        southPanel.add(timerMainButton, southConstraints);
        southConstraints = new GridBagConstraints(1, 0, 1, 1, 0, 0, GridBagConstraints.CENTER,
                GridBagConstraints.NONE, new Insets(0,0,0,0), 0, 0);
        southPanel.add(stopButton, southConstraints);
        southConstraints = new GridBagConstraints(2, 0, 1, 1, 0, 0, GridBagConstraints.CENTER,
                GridBagConstraints.NONE, new Insets(0,0,0,0), 0, 0);
        southPanel.add(editButton, southConstraints);
        southConstraints = new GridBagConstraints(3, 0, 1, 1, 0, 0, GridBagConstraints.CENTER,
                GridBagConstraints.NONE, new Insets(0,0,0,0),0, 0);
        southPanel.add(deleteButton, southConstraints);

        GridBagLayout timerPanelLayout =  new GridBagLayout();
        setLayout(timerPanelLayout);
        GridBagConstraints timerPanelConstraints;
        timerPanelConstraints = new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.CENTER,
                GridBagConstraints.BOTH, new Insets(0,0,0,0), 0, 0);
        add(nameLabel, timerPanelConstraints);
        timerPanelConstraints = new GridBagConstraints(0, 1, 1, 1, 1, 1, GridBagConstraints.CENTER,
                GridBagConstraints.BOTH, new Insets(0,0,0,0), 0, 0);
        add(infoLabel, timerPanelConstraints);
        timerPanelConstraints = new GridBagConstraints(0,2,1,1,1,1,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0,0,0,0),0,0);
        add(countingLabel, timerPanelConstraints);
        timerPanelConstraints = new GridBagConstraints(0,3,1,1,1,1,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0,0,0,0),0,0);
        add(southPanel, timerPanelConstraints);
        setBorder(BorderFactory.createEtchedBorder());
    }

    public Timer getTimer() {
        return timer;
    }

    //内部方法
    private void syncCountingLabel(Time countingTime) {
        countingLabel.setText(DateTimeUtil.wrapTimeHourToSecond(countingTime));
    }
    private void syncCountingLabel() {
        syncCountingLabel(timer.getCountingTime());
    }
    private void syncInfoLabelGroup() {
        StringBuilder sb = new StringBuilder();
        Time resetTime = timer.getResetTime();
        sb.append(DateTimeUtil.wrapTimeHourToMinute(resetTime));
        infoLabel.setText(sb.toString());
        nameLabel.setText(timer.getTimerName());
    }

    private void showResetTimeDialog() {
        String[] hourOptions = new String[24];
        String[] minuteOptions = new String[60];
        for(int i=0;i<hourOptions.length;i++) {
            hourOptions[i] = "" + i;
        }
        for(int i=0;i<minuteOptions.length;i++) {
            minuteOptions[i] = "" + i;
        }

        JDialog resetTimerDialog = new JDialog(parentJFrame, "重设计时", true);
        Container dialogContentPane = resetTimerDialog.getContentPane();
        dialogContentPane.setLayout(new BorderLayout());

        JComboBox<String> dialogHourCombo = new JComboBox<>(hourOptions), dialogMinuteCombo = new JComboBox<>(minuteOptions);
        JButton dialogOkButton = new JButton("确定"), dialogCancelButton = new JButton("取消");
        JLabel dialogHourLabel = new JLabel("小时"), dialogMinuteLabel = new JLabel("分钟");
        JTextField dialogNameField = new JTextField(timer.getTimerName(), 20);
        dialogNameField.setHorizontalAlignment(JTextField.CENTER);
        dialogNameField.setFont(FontManager.getByNameStyleSize("宋体", Font.BOLD, 20));
        dialogHourCombo.setEditable(true);
        dialogMinuteCombo.setEditable(true);
        dialogHourCombo.setRenderer(new JComboWrappedRenderer(dialogHourCombo, jLabel -> jLabel.setHorizontalAlignment(JLabel.CENTER)));
        dialogMinuteCombo.setRenderer(new JComboWrappedRenderer(dialogMinuteCombo, jLabel -> jLabel.setHorizontalAlignment(JLabel.CENTER)));
        dialogHourCombo.setEditor(new JComboWrappedEditor(dialogHourCombo, jTextField -> jTextField.setHorizontalAlignment(JTextField.CENTER)));
        dialogMinuteCombo.setEditor(new JComboWrappedEditor(dialogMinuteCombo, jTextField -> jTextField.setHorizontalAlignment(JTextField.CENTER)));
        dialogHourCombo.addActionListener(e1 -> {
            String input = (String) dialogHourCombo.getSelectedItem();
            boolean isValid = true;
            int hourInput = 0;
            try {
                hourInput = Integer.parseInt(input);
            } catch (NumberFormatException e2) {
                isValid = false;
            }
            if (isValid && hourInput < 0 || hourInput > 23) {
                isValid = false;
            }
            if (!isValid) {
                dialogHourCombo.getEditor().setItem("0");
            }
        });
        dialogMinuteCombo.addActionListener(e1 -> {
            String input = (String) dialogMinuteCombo.getSelectedItem();
            boolean isValid = true;
            int minuteInput = 0;
            try {
                minuteInput = Integer.parseInt(input);
            } catch (NumberFormatException e2) {
                isValid = false;
            }
            if (isValid && minuteInput < 0 || minuteInput > 59) {
                isValid = false;
            }
            if (!isValid) {
                dialogMinuteCombo.getEditor().setItem("0");
            }
        });
        dialogOkButton.addActionListener(e1 -> {
            int hour = Integer.parseInt((String) dialogHourCombo.getSelectedItem()), minute = Integer.parseInt((String) dialogMinuteCombo.getSelectedItem());
//            if (hour == 0 && minute == 0) {
//                JOptionPane.showMessageDialog(resetTimerDialog, "请检查输入！", "提示", JOptionPane.WARNING_MESSAGE);
//                return;
//            }
            String timerName = dialogNameField.getText();
            if(timerName == null || timerName.equals("")) {
                JOptionPane.showMessageDialog(resetTimerDialog, "请检查输入！", "提示", JOptionPane.WARNING_MESSAGE);
                return;
            }
            this.timer.reset(new Time(hour, minute, 0));
            this.timer.setTimerName(timerName);
            syncInfoLabelGroup();
            resetTimerDialog.dispose();
            resetTimerDialog.setVisible(false);
        });
        dialogCancelButton.addActionListener(e1 -> {
            resetTimerDialog.dispose();
            resetTimerDialog.setVisible(false);
        });

        JPanel editPanel = new JPanel(), buttonPanel = new JPanel();
        GridBagLayout editPanelLayout = new GridBagLayout(), buttonPanelLayout = new GridBagLayout();
        editPanel.setLayout(editPanelLayout);
        buttonPanel.setLayout(buttonPanelLayout);
        GridBagConstraints gridBagConstraints;
        gridBagConstraints = new GridBagConstraints(0,0,6,1,0,0,
                GridBagConstraints.CENTER,GridBagConstraints.NONE,new Insets(5,0,5,0),0,0);
        editPanel.add(dialogNameField, gridBagConstraints);
        gridBagConstraints = new GridBagConstraints(0,1,2,1,0,0,
                GridBagConstraints.CENTER,GridBagConstraints.NONE,new Insets(0,0,0,0),0,0);
        editPanel.add(dialogHourCombo, gridBagConstraints);
        gridBagConstraints = new GridBagConstraints(2,1,1,1,0,0,
                GridBagConstraints.CENTER,GridBagConstraints.NONE,new Insets(0,5,0,5),0,0);
        editPanel.add(dialogHourLabel, gridBagConstraints);
        gridBagConstraints = new GridBagConstraints(3,1,2,1,0,0,
                GridBagConstraints.CENTER,GridBagConstraints.NONE,new Insets(0,0,0,0),0,0);
        editPanel.add(dialogMinuteCombo, gridBagConstraints);
        gridBagConstraints = new GridBagConstraints(5,1,1,1,0,0,
                GridBagConstraints.CENTER,GridBagConstraints.NONE,new Insets(0,5,0,5),0,0);
        editPanel.add(dialogMinuteLabel, gridBagConstraints);
        gridBagConstraints = new GridBagConstraints(0,0,1,1,0,0,
                GridBagConstraints.CENTER,GridBagConstraints.NONE,new Insets(5,0,5,0),0,0);
        buttonPanel.add(dialogOkButton, gridBagConstraints);
        gridBagConstraints = new GridBagConstraints(1,0,1,1,0,0,
                GridBagConstraints.CENTER,GridBagConstraints.NONE,new Insets(5,0,5,0),0,0);
        buttonPanel.add(dialogCancelButton,gridBagConstraints);

        dialogContentPane.add(editPanel, BorderLayout.CENTER);
        dialogContentPane.add(buttonPanel, BorderLayout.SOUTH);

        resetTimerDialog.pack();
        resetTimerDialog.setVisible(true);
    }

}
