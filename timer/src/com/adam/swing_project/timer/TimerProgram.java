package com.adam.swing_project.timer;

import com.adam.swing_project.timer.ajswing.AJButton;
import com.adam.swing_project.timer.assertion.Assert;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;

public class TimerProgram extends JFrame{
    private TrayIcon trayIcon;
    private boolean isSupportSystemTray;

    public static void main(String[] args) {
        new TimerProgram();
    }

    public TimerProgram() {
        //窗体
        JFrame jFrame = new JFrame("Swing计时器");
        Container contentPane = jFrame.getContentPane();

        //菜单栏
        JMenuBar jMenuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("文件(F)")
                , helpMenu = new JMenu("帮助(H)");
        JMenuItem fileResetTimer = new JMenuItem("设定计时"),
                fileStatisticItem = new JMenuItem("统计数据"),
                helpAboutItem = new JMenuItem("关于(A)");
        jMenuBar.add(fileMenu);
        jMenuBar.add(helpMenu);
        fileMenu.add(fileResetTimer);
        fileMenu.add(fileStatisticItem);
        helpMenu.add(helpAboutItem);
        fileMenu.setMnemonic('F');
        helpMenu.setMnemonic('H');
//        fileResetTimer.setMnemonic('S');
        helpAboutItem.setMnemonic('A');
        String[] hourOptions = new String[24];
        String[] minuteOptions = new String[60];
        for(int i=0;i<hourOptions.length;i++) {
            hourOptions[i] = "" + i;
        }
        for(int i=0;i<minuteOptions.length;i++) {
            minuteOptions[i] = "" + i;
        }
        fileResetTimer.addActionListener(e -> {
            JDialog resetTimerDialog = new JDialog(jFrame, "重设计时", true);
            Container dialogContentPane = resetTimerDialog.getContentPane();

            Box dialogOuterBox = Box.createVerticalBox(),
                    dialogButtonBox = Box.createHorizontalBox();
            JComboBox<String> dialogHourCombo = new JComboBox<>(hourOptions)
                    , dialogMinuteCombo = new JComboBox<>(minuteOptions);
            JButton dialogOkButton = new JButton("确定")
                    , dialogCancelButton = new JButton("取消");
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
                if(isValid && hourInput < 0 || hourInput > 23) {
                    isValid = false;
                }
                if(!isValid) {
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
                if(isValid && minuteInput < 0 || minuteInput > 59) {
                    isValid = false;
                }
                if(!isValid) {
                    dialogMinuteCombo.getEditor().setItem("0");
                }
            });
            dialogOkButton.addActionListener(e1 -> {
                int hour = Integer.parseInt((String)dialogHourCombo.getSelectedItem())
                        , minute = Integer.parseInt((String)dialogMinuteCombo.getSelectedItem());
                if(hour == 0 && minute == 0) {
                    JOptionPane.showMessageDialog(resetTimerDialog, "请检查输入！", "提示", JOptionPane.WARNING_MESSAGE);
                } else {
                    TimerThread.getInstance().resetTime(hour, minute);
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
        });
        fileStatisticItem.addActionListener(e -> {
            Object[] statistic = TimerStatistic.getInstance().getDayStatistic(3);
            Assert.isTrue(statistic.length % 2 == 0, "统计数据非法！");
            StringBuilder sb = new StringBuilder();
            sb.append("计时器统计数据").append(System.lineSeparator()).append(System.lineSeparator());
            for(int i=0;i<statistic.length;i++) {
                String date = (String) statistic[i++];
                sb.append(date).append(' ');
                TimerStatistic.DayStatistic dayStatistic = (TimerStatistic.DayStatistic) statistic[i];
                if(dayStatistic == null) {
                    sb.append("暂无数据");
                } else {
                    sb.append("总时长 ").append(dayStatistic.getTotalStatistic());
                    //今日详细数据
                    if(i==1) {
                        sb.append(System.lineSeparator());
                        for(int j=0;j<date.length();j++) {
                            sb.append(" ");
                        }
                        sb.append(" 自然计时总时长 ").append(dayStatistic.getNaturalStatistic());
                        sb.append(System.lineSeparator());
                        for(int j=0;j<date.length();j++) {
                            sb.append(" ");
                        }
                        sb.append(" 用户中断总时长 ").append(dayStatistic.getUserStoppedStatistic());
                    }
                }
                sb.append(System.lineSeparator());
            }
            JOptionPane.showMessageDialog(jFrame, sb.toString(), "统计数据", JOptionPane.INFORMATION_MESSAGE);
        });
        helpAboutItem.addActionListener(e -> {
            String aboutMessage = "计时器 v1.0" + System.lineSeparator() +
                    System.lineSeparator() +
                    "图标来源：https://icons8.com";
            JOptionPane.showMessageDialog(jFrame, aboutMessage, "关于计时器", JOptionPane.INFORMATION_MESSAGE);
        });

        //工作区域
        JLabel countingLabel = new JLabel("00:00:00")
                , timerLabel = new JLabel("");
        countingLabel.setHorizontalAlignment(JLabel.CENTER);
        countingLabel.setFont(new Font("Consolas", Font.BOLD, 60));
        timerLabel.setHorizontalAlignment(JLabel.CENTER);
        timerLabel.setFont(new Font("Consolas", Font.PLAIN, 24));
        JPanel timerButtonPanel = new JPanel();
        final int TIMER_MAIN_BUTTON_STATUS_START = 0
                , TIMER_MAIN_BUTTON_STATUS_PAUSE = 1
                , TIMER_MAIN_BUTTON_STATUS_STOP_PLAY = 2;
//        JButton startPauseButton = new JButton(IconManager.play24()),
//                stopButton = new JButton(IconManager.stop24());
        AJButton timerMainButton = new AJButton(3, 0);
        JButton stopButton = new JButton(IconManager.stop24());
        timerMainButton.setToolTipText("开始/暂停计时");
        stopButton.setToolTipText("停止计时");
        timerMainButton.setEnabled(false);
        stopButton.setEnabled(false);
        timerButtonPanel.add(timerMainButton);
        timerButtonPanel.add(stopButton);

//        timerMainButton.addActionListener(e -> {
//            TimerThread.getInstance().startTimer();
//        });
        timerMainButton.bind(TIMER_MAIN_BUTTON_STATUS_START, ajButton -> {
            ajButton.setIcon(IconManager.play24());
            if(ajButton.getCurrentStatus() != ajButton.getLastStatus()) {
                ajButton.setEnabled(true);
            }
            }, e -> TimerThread.getInstance().startTimer());
        timerMainButton.bind(TIMER_MAIN_BUTTON_STATUS_PAUSE, ajButton ->
                ajButton.setIcon(IconManager.pause24()), e -> TimerThread.getInstance().pauseTimer());
        timerMainButton.bind(TIMER_MAIN_BUTTON_STATUS_STOP_PLAY, ajButton ->
                ajButton.setIcon(IconManager.noRecord24()), e -> AudioController.getInstance().stopPlay());
        stopButton.addActionListener(e -> TimerThread.getInstance().stopTimer());



        contentPane.add(timerLabel, BorderLayout.NORTH);
        contentPane.add(countingLabel, BorderLayout.CENTER);
        contentPane.add(timerButtonPanel, BorderLayout.SOUTH);
        jFrame.setJMenuBar(jMenuBar);

        jFrame.setSize(400, 300);
        jFrame.setMinimumSize(new Dimension(400, 300));
        jFrame.setVisible(true);
        jFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                System.out.println("CLosing");
                int result;
                do {
                    result = JOptionPane.showConfirmDialog(jFrame, "您点击了关闭按钮。" + System.lineSeparator() + "是否收起到系统托盘？（程序仍然在后台运行）", "提示", JOptionPane.YES_NO_OPTION,
                            JOptionPane.WARNING_MESSAGE, null);
                    if (result == JOptionPane.YES_OPTION) {
                        jFrame.setVisible(false);
                        pushMessageToTrayIcon("计时器在后台运行", "可通过系统托盘图标右键-显示主窗口恢复", TrayIcon.MessageType.INFO);
                    } else if (result == JOptionPane.NO_OPTION) {
                        AudioController.getInstance().terminate();
                        TimerThread.getInstance().terminate();
                        System.exit(0);
                    } else if (result == JOptionPane.CLOSED_OPTION) {
                        //do nothing
                    }
                } while (result == JOptionPane.CLOSED_OPTION);
            }
        });

        //单独线程相关
        TimerThread.getInstance().registerListener(new TimerThread.TimerListener() {
            @Override
            public void timerStarted() {
                timerMainButton.changeStatus(TIMER_MAIN_BUTTON_STATUS_PAUSE);
                stopButton.setEnabled(true);
                StringBuilder sb = new StringBuilder();
                int hour = TimerThread.getInstance().getCountingHour()
                        , minute = TimerThread.getInstance().getCountingMinute()
                        , second = TimerThread.getInstance().getCountingSecond();
                sb.append(hour < 10 ? "0" : "").append(hour).append(":");
                sb.append(minute < 10 ? "0" : "").append(minute).append(":");
                sb.append(second < 10 ? "0" : "").append(second);
                String timerText = sb.toString();
                countingLabel.setText(timerText);
            }

            @Override
            public void timerPaused() {
                timerMainButton.changeStatus(TIMER_MAIN_BUTTON_STATUS_START);
            }

            @Override
            public void timerStopped() {
                timerMainButton.changeStatus(TIMER_MAIN_BUTTON_STATUS_STOP_PLAY);
                stopButton.setEnabled(false);
                countingLabel.setText("00:00:00");
                if(AudioController.getInstance().getSoundFile() == null) {
                    InputStream soundInputStream = TimerProgram.class.getResourceAsStream("/Listen.wav");
                    byte[] buffer = new byte[1024];
                    try {
                        File soundFile = File.createTempFile("Listen", ".wav");
                        FileOutputStream fileOutputStream = new FileOutputStream(soundFile);
                        int n = 0;
                        while((n = soundInputStream.read(buffer, 0, buffer.length)) != -1) {
                            fileOutputStream.write(buffer, 0, n);
                        }
                        fileOutputStream.close();
                        AudioController.getInstance().chooseSoundFile(soundFile);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    AudioController.getInstance().startPlay();
                }
                pushMessageToTrayIcon("计时器", "时间到啦！", TrayIcon.MessageType.INFO);

                jFrame.setVisible(true);
            }


            @Override
            public void timerStoppedByUser() {
                timerMainButton.changeStatus(TIMER_MAIN_BUTTON_STATUS_START);
                stopButton.setEnabled(false);
                countingLabel.setText("00:00:00");
            }

            @Override
            public void timerUpdated(int hour, int minute, int second) {
                StringBuilder sb = new StringBuilder();
                sb.append(hour < 10 ? "0" : "").append(hour).append(":");
                sb.append(minute < 10 ? "0" : "").append(minute).append(":");
                sb.append(second < 10 ? "0" : "").append(second);
                String countingText = sb.toString();
                countingLabel.setText(countingText);
            }

            @Override
            public void timerReset(int hour, int minute) {
                StringBuilder sb = new StringBuilder();
                sb.append(hour < 10 ? "0" : "").append(hour).append(":");
                sb.append(minute < 10 ? "0" : "").append(minute);
                String timerText = sb.toString();
                timerLabel.setText(timerText);
                countingLabel.setText(timerText + ":00");
                timerMainButton.setEnabled(true);
            }
        });
        AudioController.getInstance().registerListener(new AudioController.AudioControllerListener() {
            @Override
            public void playStopped() {
                timerMainButton.changeStatus(TIMER_MAIN_BUTTON_STATUS_START);
            }

            @Override
            public void playPaused() {
            }
        });
        AudioController.getInstance().start();
        TimerThread.getInstance().start();

        //托盘
        isSupportSystemTray = SystemTray.isSupported();
        if(isSupportSystemTray) {
            Font f = new Font("宋体", Font.PLAIN, 12);
            UIManager.put("Label.font",f);
            UIManager.put("Label.foreground",Color.black);
            UIManager.put("Button.font",f);
            UIManager.put("Menu.font",f);
            UIManager.put("MenuItem.font",f);
            UIManager.put("List.font",f);
            UIManager.put("CheckBox.font",f);
            UIManager.put("RadioButton.font",f);
            UIManager.put("ComboBox.font",f);
            UIManager.put("TextArea.font",f);
            UIManager.put("EditorPane.font",f);
            UIManager.put("ScrollPane.font",f);
            UIManager.put("ToolTip.font",f);
            UIManager.put("TextField.font",f);
            UIManager.put("TableHeader.font",f);
            UIManager.put("Table.font",f);
//            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
//            String script[] = ge.getAvailableFontFamilyNames();
//            for(String s:script){
//                System.out.print(s+",");
//            }

            PopupMenu trayPopupMenu = new PopupMenu();
            trayIcon = new TrayIcon(IconManager.timer24().getImage());
            SystemTray systemTray = SystemTray.getSystemTray();

            MenuItem showMainItem = new MenuItem("显示主窗口")
                    , exitItem = new MenuItem("退出");
            trayPopupMenu.add(showMainItem);
            trayPopupMenu.add(exitItem);
            showMainItem.addActionListener(e -> jFrame.setVisible(true));
            exitItem.addActionListener(e -> System.exit(0));
            trayIcon.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    //双击回到主界面
                    if(e.getClickCount() == 2) {
                        jFrame.setVisible(true);
                    }
                }
            });

            trayIcon.setPopupMenu(trayPopupMenu);
            try {
                systemTray.add(trayIcon);
            } catch (AWTException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("系统不支持托盘！");
        }
    }

    public void pushMessageToTrayIcon(String caption, String text, TrayIcon.MessageType type) {
        if(isSupportSystemTray) {
            trayIcon.displayMessage(caption, text, type);
        }
    }

}
