package com.adam.swing_project.timer;

import com.adam.swing_project.timer.assertion.Assert;
import com.adam.swing_project.timer.component.FileManager;
import com.adam.swing_project.timer.component.IconManager;
import com.adam.swing_project.timer.thread.ThreadManager;
import com.adam.swing_project.timer.frontend.TimerPanel;
import com.adam.swing_project.timer.helper.TimerStatistic;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class TimerProgram extends JFrame{
    private TrayIcon trayIcon;
    private boolean isSupportSystemTray;
    private int timerPanelCount = 0;

    public static void main(String[] args) {
        new TimerProgram();
    }

    public TimerProgram() {
        //窗体
        JFrame jFrame = new JFrame("Swing计时器");
        Container contentPane = jFrame.getContentPane();
        JPanel mainContentPanel = new JPanel();
        GridBagLayout mainContentPanelLayout = new GridBagLayout();
        mainContentPanel.setLayout(mainContentPanelLayout);
        JScrollPane jScrollPane = new JScrollPane(mainContentPanel);
        contentPane.add(jScrollPane, BorderLayout.CENTER);

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
            TimerPanel timerPanel = new TimerPanel(jFrame);
            //每行摆3个计时器
            int gridx = timerPanelCount % 3, gridy = timerPanelCount / 3;
            GridBagConstraints gridBagConstraints = new GridBagConstraints(gridx, gridy, 1, 1, 1, 1,
                    GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(10,10,10,10), 0, 0);
            mainContentPanel.add(timerPanel, gridBagConstraints);
            timerPanel.getTimer().registerTimerListener(timerPanel.getTimer().new TimeAdapter() {
                @Override
                public void timerStopped() {
                    pushMessageToTrayIcon("计时器", "时间到啦！", TrayIcon.MessageType.INFO);
                }
            });
            timerPanelCount++;
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
            String aboutMessage = "计时器 v1.0" + System.lineSeparator() +
                    System.lineSeparator() +
                    "图标来源：https://icons8.com";
            JOptionPane.showMessageDialog(jFrame, aboutMessage, "关于计时器", JOptionPane.INFORMATION_MESSAGE);
        });
        jFrame.setJMenuBar(jMenuBar);

        ThreadManager.getInstance().initThreads();
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
                        pushMessageToTrayIcon("计时器在后台运行", "可通过系统托盘图标右键-显示主窗口恢复", TrayIcon.MessageType.INFO);
                    }
                    //结束程序
                    else if (result == JOptionPane.NO_OPTION) {
                        ThreadManager.getInstance().destroyThreads();
                        FileManager.getInstance().cleanTempFiles();
                        System.exit(0);
                    } else if (result == JOptionPane.CLOSED_OPTION) {
                        //do nothing
                    }
                } while (result == JOptionPane.CLOSED_OPTION);
            }
        });

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
