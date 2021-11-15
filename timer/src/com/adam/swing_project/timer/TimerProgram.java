package com.adam.swing_project.timer;

import com.adam.swing_project.timer.assertion.Assert;
import com.adam.swing_project.timer.component.ApplicationManager;
import com.adam.swing_project.timer.component.ConfigManager;
import com.adam.swing_project.timer.component.TrayIconManager;
import com.adam.swing_project.timer.frontend.TimerPanel;
import com.adam.swing_project.timer.helper.TimerStatistic;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class TimerProgram extends JFrame{
    private final TrayIconManager trayIconManager;

    public static void main(String[] args) {
        ConfigManager.getInstance().loadStartupArgs(args);
        new TimerProgram();
    }

    public TimerProgram() {
        //窗体
        JFrame jFrame = new JFrame("Swing计时器");
        Container contentPane = jFrame.getContentPane();
        TimerPanel timerPanel = new TimerPanel(jFrame);
        JScrollPane jScrollPane = new JScrollPane(timerPanel);
        contentPane.add(jScrollPane, BorderLayout.CENTER);

        //托盘
        trayIconManager = TrayIconManager.getInstance();
        trayIconManager.setjFrame(jFrame);
        trayIconManager.addTrayIconIfSupported();

        //菜单栏
        JMenuBar jMenuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("文件(F)")
                , helpMenu = new JMenu("帮助(H)");
        JMenuItem fileNewTimerItem = new JMenuItem("新建计时器(N)"),
                fileStatisticItem = new JMenuItem("统计数据(S)"),
                helpAboutItem = new JMenuItem("关于(A)");
        jMenuBar.add(fileMenu);
        jMenuBar.add(helpMenu);
        fileMenu.add(fileNewTimerItem);
        fileMenu.add(fileStatisticItem);
        helpMenu.add(helpAboutItem);
        fileMenu.setMnemonic('F');
        helpMenu.setMnemonic('H');
        fileNewTimerItem.setMnemonic('N');
        fileStatisticItem.setMnemonic('S');
        helpAboutItem.setMnemonic('A');
        fileNewTimerItem.addActionListener(e -> {
            timerPanel.addSingleTimerPanel();
            jFrame.revalidate();
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
            String aboutMessage = "计时器 v1.2.1-dev" + System.lineSeparator() +
                    System.lineSeparator() +
                    "图标来源：https://icons8.com";
            JOptionPane.showMessageDialog(jFrame, aboutMessage, "关于计时器", JOptionPane.INFORMATION_MESSAGE);
        });
        jFrame.setJMenuBar(jMenuBar);

        ApplicationManager.getInstance().registerProgramGlobalObject(timerPanel);
        ApplicationManager.getInstance().init();
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
                    //后台运行
                    if (result == JOptionPane.YES_OPTION) {
                        jFrame.setVisible(false);
                        trayIconManager.pushMessageToTrayIcon("计时器在后台运行", "可通过系统托盘图标右键-显示主窗口恢复", TrayIcon.MessageType.INFO);
                    }
                    //结束程序
                    else if (result == JOptionPane.NO_OPTION) {
                        System.exit(0);
                    } else if (result == JOptionPane.CLOSED_OPTION) {
                        //do nothing
                    }
                } while (result == JOptionPane.CLOSED_OPTION);
            }
        });
    }

}
