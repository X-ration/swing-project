package com.adam.swing_project.timer.component;

import com.adam.swing_project.library.logger.Logger;
import com.adam.swing_project.library.logger.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * 托盘图标管理器
 */
public class TrayIconManager {

    private static final TrayIconManager instance = new TrayIconManager();
    private JFrame jFrame;
    private TrayIcon trayIcon;
    private final boolean isSupportSystemTray = SystemTray.isSupported();
    private final Logger logger = LoggerFactory.getLogger(this);

    public static TrayIconManager getInstance() {
        return instance;
    }

    public void setjFrame(JFrame jFrame) {
        this.jFrame = jFrame;
    }

    public boolean isSupportSystemTray() {
        return isSupportSystemTray;
    }

    public void addTrayIconIfSupported() {
        if(isSupportSystemTray) {
//            Font f = new Font("宋体", Font.PLAIN, 12);
//            UIManager.put("Label.font",f);
//            UIManager.put("Label.foreground",Color.black);
//            UIManager.put("Button.font",f);
//            UIManager.put("Menu.font",f);
//            UIManager.put("MenuItem.font",f);
//            UIManager.put("List.font",f);
//            UIManager.put("CheckBox.font",f);
//            UIManager.put("RadioButton.font",f);
//            UIManager.put("ComboBox.font",f);
//            UIManager.put("TextArea.font",f);
//            UIManager.put("EditorPane.font",f);
//            UIManager.put("ScrollPane.font",f);
//            UIManager.put("ToolTip.font",f);
//            UIManager.put("TextField.font",f);
//            UIManager.put("TableHeader.font",f);
//            UIManager.put("Table.font",f);
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
            logger.logWarning("系统不支持托盘！");
        }
    }

    public void pushMessageToTrayIcon(String caption, String text, TrayIcon.MessageType type) {
        if(isSupportSystemTray) {
            trayIcon.displayMessage(caption, text, type);
        } else {
            logger.logWarning("系统不支持托盘！");
        }
    }
}
