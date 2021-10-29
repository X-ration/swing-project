package com.adam.swing_project.timer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class Timer extends JFrame{
    private TrayIcon trayIcon;
    private boolean isSupportSystemTray;

    public static void main(String[] args) {
        new Timer();
    }

    public Timer() {
        //窗体
        JFrame jFrame = new JFrame("Swing计时器");
        Container contentPane = jFrame.getContentPane();

        contentPane.add(new JButton("dflas"));

        jFrame.setSize(400, 300);
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
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            String script[] = ge.getAvailableFontFamilyNames();
            for(String s:script){
                System.out.print(s+",");
            }

            PopupMenu trayPopupMenu = new PopupMenu();
            trayIcon = new TrayIcon(IconManager.timer24().getImage());
            SystemTray systemTray = SystemTray.getSystemTray();

            MenuItem showMainItem = new MenuItem("显示主窗口")
                    , exitItem = new MenuItem("退出");
            trayPopupMenu.add(showMainItem);
            trayPopupMenu.add(exitItem);
            showMainItem.addActionListener(e -> jFrame.setVisible(true));
            exitItem.addActionListener(e -> System.exit(0));

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
