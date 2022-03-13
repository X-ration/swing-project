package com.adam.swing_project.timer;

import com.adam.swing_project.library.logger.ConsoleLogger;
import com.adam.swing_project.library.logger.Logger;
import com.adam.swing_project.library.logger.LoggerFactory;
import com.adam.swing_project.library.logger.RollingFileLogger;
import com.adam.swing_project.library.util.ApplicationArgumentResolver;
import com.adam.swing_project.timer.app_info.TimerAppInfo;
import com.adam.swing_project.timer.component.ApplicationManager;
import com.adam.swing_project.timer.component.FileManager;
import com.adam.swing_project.timer.component.TrayIconManager;
import com.adam.swing_project.timer.frontend.StatisticDialog;
import com.adam.swing_project.timer.frontend.TimerPanel;
import com.adam.swing_project.timer.option.OptionDialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class TimerProgram extends JFrame{
    private final TrayIconManager trayIconManager;

    public static void main(String[] args) {
        ApplicationArgumentResolver argumentResolver = new ApplicationArgumentResolver(args);
        FileManager.getInstance().init(argumentResolver);
        File logFile = FileManager.getInstance().requireSubFile("swing-timer.log");
        List<Logger> loggerList = new LinkedList<>();
        loggerList.add(ConsoleLogger.createLogger(TimerProgram.class));
        loggerList.add(RollingFileLogger.createLogger(TimerProgram.class, logFile, RollingFileLogger.RollingFileMode.BY_DAY));
        LoggerFactory.setupLoggers(loggerList);
        ApplicationManager.getInstance().registerProgramGlobalObject(argumentResolver);
        TimerAppInfo appInfo;
        try {
            appInfo = new TimerAppInfo(argumentResolver);
            ApplicationManager.getInstance().registerProgramGlobalObject(appInfo);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        new TimerProgram(appInfo);
    }

    public TimerProgram(TimerAppInfo appInfo) {
        final String titleString = appInfo.getAppName() + " " + appInfo.getAppVersion() +
                ((appInfo.getEnv() == null) ?  "" : " [" + appInfo.getEnv() + "]");
        appInfo.setTitleString(titleString);
        //窗体
        JFrame jFrame = new JFrame(titleString);
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
                , optionMenu = new JMenu("选项(O)")
                , helpMenu = new JMenu("帮助(H)");
        JMenuItem fileNewTimerItem = new JMenuItem("新建计时器(N)"),
                fileStatisticItem = new JMenuItem("统计数据(S)"),
                optionItem = new JMenuItem("选项面板"),
                helpAboutItem = new JMenuItem("关于(A)");
        jMenuBar.add(fileMenu);
        jMenuBar.add(optionMenu);
        jMenuBar.add(helpMenu);
        fileMenu.add(fileNewTimerItem);
        fileMenu.add(fileStatisticItem);
        optionMenu.add(optionItem);
        helpMenu.add(helpAboutItem);
        fileMenu.setMnemonic('F');
        optionMenu.setMnemonic('O');
        helpMenu.setMnemonic('H');
        fileNewTimerItem.setMnemonic('N');
        fileStatisticItem.setMnemonic('S');
        helpAboutItem.setMnemonic('A');
        fileNewTimerItem.addActionListener(e -> {
            timerPanel.addSingleTimerPanel();
            jFrame.revalidate();
        });
        fileStatisticItem.addActionListener(e -> showStatisticDialog(jFrame));
        optionItem.addActionListener(e -> (new OptionDialog(jFrame)).setVisible(true));
        helpAboutItem.addActionListener(e -> {
            String aboutMessage = titleString + System.lineSeparator() +
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
        jFrame.setLocation(450, 230);
        jFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
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

    private void showStatisticDialog(JFrame jFrame) {
        StatisticDialog dialog = new StatisticDialog(jFrame);
        dialog.setVisible(true);
    }

}
