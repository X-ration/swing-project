package com.adam.swing_project.timer.frontend;

import com.adam.swing_project.timer.component.FontManager;
import com.adam.swing_project.timer.component.IconManager;
import com.adam.swing_project.timer.helper.TimerStatistic;
import com.adam.swing_project.timer.ajswing.AJStatusButton;
import com.adam.swing_project.timer.ajswing.AJStatusButtonBinaryStatus;
import com.adam.swing_project.timer.thread.AudioThread;
import com.adam.swing_project.timer.component.FileManager;
import com.adam.swing_project.timer.helper.Logger;
import com.adam.swing_project.timer.thread.ThreadManager;
import com.adam.swing_project.timer.core.Timer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * 计时器面板前端组件，为了支持多计时器计时独立成类
 * 将单个计时器涉及的JLabel、JButton等封装在一起
 */
public class TimerPanel extends JPanel {

    private final JLabel countingLabel, infoLabel;
    private final AJStatusButton timerMainButton, stopButton, editButton;
    private final JFrame parentJFrame;

    private final Timer timer;
    private final AudioThread audioThread;

    public enum TimerMainButtonStatus {
        INITIAL, START, PAUSE, STOP_PLAY
    }

    public TimerPanel(JFrame jFrame) {
        this.timer = new Timer();
        this.countingLabel = new JLabel();
        this.infoLabel = new JLabel();
        this.timerMainButton = new AJStatusButton(TimerMainButtonStatus.class, TimerMainButtonStatus.INITIAL);
        this.stopButton = new AJStatusButton(AJStatusButtonBinaryStatus.class, AJStatusButtonBinaryStatus.CLOSED);
        this.editButton = new AJStatusButton(AJStatusButtonBinaryStatus.class, AJStatusButtonBinaryStatus.OPEN);
        this.audioThread = ThreadManager.getInstance().getAudioThread();
        this.parentJFrame = jFrame;

        syncCountingLabel();
        syncInfoLabel();
        timerMainButton.bind(TimerMainButtonStatus.INITIAL, ajStatusButton -> {
            ajStatusButton.setEnabled(false);
            ajStatusButton.setIcon(IconManager.play24());
        }, e -> {});
        timerMainButton.bind(TimerMainButtonStatus.START, ajStatusButton -> {
            ajStatusButton.setEnabled(true);
            ajStatusButton.setIcon(IconManager.play24());
        }, e -> timer.startTimer());
        timerMainButton.bind(TimerMainButtonStatus.PAUSE, ajStatusButton -> {
            ajStatusButton.setEnabled(true);
            ajStatusButton.setIcon(IconManager.pause24());
        }, e -> timer.pauseTimer());
        timerMainButton.bind(TimerMainButtonStatus.STOP_PLAY, ajStatusButton -> {
            ajStatusButton.setEnabled(true);
            ajStatusButton.setIcon(IconManager.noRecord24());
        }, e -> {
            audioThread.stopPlay();
            timerMainButton.changeStatus(TimerMainButtonStatus.START);
        });
        stopButton.setIcon(IconManager.stop24());
        stopButton.bind(AJStatusButtonBinaryStatus.CLOSED, ajStatusButton -> ajStatusButton.setEnabled(false), e -> {});
        stopButton.bind(AJStatusButtonBinaryStatus.OPEN, ajStatusButton -> ajStatusButton.setEnabled(true), e -> timer.stopTimer());
        editButton.setIcon(IconManager.edit24());
        editButton.bind(AJStatusButtonBinaryStatus.OPEN, ajStatusButton -> ajStatusButton.setEnabled(true), e -> showResetTimeDialog());
        editButton.bind(AJStatusButtonBinaryStatus.CLOSED, ajStatusButton -> ajStatusButton.setEnabled(false), e -> {});
        timer.registerTimerListener(new Timer.TimerListener() {
            @Override
            public void timerStarted() {
                syncInfoLabel();
                syncCountingLabel();
                timerMainButton.changeStatus(TimerMainButtonStatus.PAUSE);
                stopButton.changeStatus(AJStatusButtonBinaryStatus.OPEN);
                editButton.changeStatus(AJStatusButtonBinaryStatus.CLOSED);
            }

            @Override
            public void timerPaused() {
                timerMainButton.changeStatus(TimerMainButtonStatus.START);
            }

            @Override
            public void timerStopped() {
                syncInfoLabel();
                syncCountingLabel();
                timerMainButton.changeStatus(TimerMainButtonStatus.STOP_PLAY);
                stopButton.changeStatus(AJStatusButtonBinaryStatus.CLOSED);
                editButton.changeStatus(AJStatusButtonBinaryStatus.OPEN);
                audioThread.chooseSoundFile("/Listen.wav");
                int statHour = timer.getResetTime().getHour(), statMinute = timer.getResetTime().getMinute();
                TimerStatistic.getInstance().recordNaturalCounting(statHour, statMinute);
            }

            @Override
            public void timerStoppedByUser() {
                syncInfoLabel();
                syncCountingLabel();
                timerMainButton.changeStatus(TimerMainButtonStatus.START);
                stopButton.changeStatus(AJStatusButtonBinaryStatus.CLOSED);
                editButton.changeStatus(AJStatusButtonBinaryStatus.OPEN);
                int statHour = timer.getResetTime().getHour(), statMinute = timer.getResetTime().getMinute(),
                        statSecond = timer.getResetTime().getSecond();
                TimerStatistic.getInstance().recordUserStoppedCounting(statHour, statMinute, statSecond);
            }

            @Override
            public void timerUpdated() {
                syncInfoLabel();
                syncCountingLabel();
            }

            @Override
            public void timerReset() {
                timerMainButton.changeStatus(TimerMainButtonStatus.START);
                syncInfoLabel();
            }
        });

        infoLabel.setFont(FontManager.getByNameStyleSize("Consolas", Font.PLAIN, 24));
        infoLabel.setHorizontalAlignment(JLabel.CENTER);
        countingLabel.setFont(FontManager.getByNameStyleSize("Consolas", Font.BOLD, 60));
        countingLabel.setHorizontalAlignment(JLabel.CENTER);
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

        GridBagLayout timerPanelLayout =  new GridBagLayout();
        setLayout(timerPanelLayout);
        GridBagConstraints timerPanelConstraints = new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.CENTER,
                GridBagConstraints.BOTH, new Insets(0,0,0,0), 0, 0);
        add(infoLabel, timerPanelConstraints);
        timerPanelConstraints = new GridBagConstraints(0,2,1,1,1,1,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0,0,0,0),0,0);
        add(countingLabel, timerPanelConstraints);
        timerPanelConstraints = new GridBagConstraints(0,4,1,1,1,1,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0,0,0,0),0,0);
        add(southPanel, timerPanelConstraints);
        setBorder(BorderFactory.createEtchedBorder());
    }

    public static void main(String[] args) {
        Logger.setGlobalLogLevel(Logger.LogLevel.INFO);
        JFrame jFrame = new JFrame();
        Container contentPane = jFrame.getContentPane();
        GridBagLayout gridBagLayout = new GridBagLayout();
        contentPane.setLayout(gridBagLayout);

        GridBagConstraints gridBagConstraints = new GridBagConstraints(0,0,1,1,1,1,
                GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(5,5,5,5), 0, 0);
        contentPane.add(new TimerPanel(jFrame), gridBagConstraints);
        gridBagConstraints = new GridBagConstraints(1,0,1,1,1,1,
                GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(5,5,5,5), 0, 0);
        contentPane.add(new TimerPanel(jFrame), gridBagConstraints);
        gridBagConstraints = new GridBagConstraints(0,1,1,1,1,1,
                GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(5,5,5,5), 0, 0);
        contentPane.add(new TimerPanel(jFrame), gridBagConstraints);
        gridBagConstraints = new GridBagConstraints(1,1,1,1,1,1,
                GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(5,5,5,5), 0, 0);
        contentPane.add(new TimerPanel(jFrame), gridBagConstraints);

        jFrame.pack();
        jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jFrame.setVisible(true);

        ThreadManager.getInstance().initThreads();
        jFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                ThreadManager.getInstance().destroyThreads();
                FileManager.getInstance().cleanTempFiles();
            }
        });
    }

    public Timer getTimer() {
        return timer;
    }

    //内部方法
    private void syncCountingLabel() {
        Timer.Time countingTime = timer.getCountingTime();
        countingLabel.setText(wrapTimeHourToSecond(countingTime));
    }
    private void syncInfoLabel() {
        StringBuilder sb = new StringBuilder();
        Timer.Time resetTime = timer.getResetTime()
                , targetTime = timer.getTargetTime()
                , startTime = timer.getStartTime();
        sb.append(wrapTimeHourToMinute(resetTime));
        if(startTime != null && targetTime != null) {
            sb.append(" (").append(wrapTimeHourToMinute(startTime))
                    .append("~").append(wrapTimeHourToMinute(targetTime)).append(")");
        }
        infoLabel.setText(sb.toString());
    }

    //独立方法
    private String wrapTimeHourToSecond(Timer.Time time) {
        return wrapTimeHourToSecond(time.getHour(),time.getMinute(),time.getSecond());
    }
    private String wrapTimeHourToMinute(Timer.Time time) {
        return wrapTimeHourToMinute(time.getHour(), time.getMinute());
    }
    private String wrapTimeHourToSecond(int hour, int minute, int second) {
        StringBuilder sb = new StringBuilder();
        sb.append(hour < 10 ? "0" : "").append(hour).append(":")
                .append(minute < 10 ? "0" : "").append(minute).append(":")
                .append(second < 10 ? "0" : "").append(second);
        return sb.toString();
    }
    private String wrapTimeHourToMinute(int hour, int minute) {
        StringBuilder sb = new StringBuilder();
        sb.append(hour < 10 ? "0" : "").append(hour).append(":")
                .append(minute < 10 ? "0" : "").append(minute);
        return sb.toString();
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

        Box dialogOuterBox = Box.createVerticalBox(),
                dialogButtonBox = Box.createHorizontalBox();
        JComboBox<String> dialogHourCombo = new JComboBox<>(hourOptions), dialogMinuteCombo = new JComboBox<>(minuteOptions);
        JButton dialogOkButton = new JButton("确定"), dialogCancelButton = new JButton("取消");
        dialogHourCombo.setEditable(true);
        dialogMinuteCombo.setEditable(true);
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
            if (hour == 0 && minute == 0) {
                JOptionPane.showMessageDialog(resetTimerDialog, "请检查输入！", "提示", JOptionPane.WARNING_MESSAGE);
            } else {
                this.timer.resetTimer(hour, minute);
                resetTimerDialog.dispose();
                resetTimerDialog.setVisible(false);
            }
        });
        dialogCancelButton.addActionListener(e1 -> {
            resetTimerDialog.dispose();
            resetTimerDialog.setVisible(false);
        });

        dialogOuterBox.add(dialogHourCombo);
        dialogOuterBox.add(dialogMinuteCombo);
        dialogOuterBox.add(dialogButtonBox);
        dialogButtonBox.add(dialogOkButton);
        dialogButtonBox.add(dialogCancelButton);
        dialogContentPane.add(dialogOuterBox);

        resetTimerDialog.setSize(300, 300);
        resetTimerDialog.setVisible(true);
    }

}
