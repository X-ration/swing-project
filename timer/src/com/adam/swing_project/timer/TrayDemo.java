package com.adam.swing_project.timer;

import com.adam.swing_project.timer.component.IconManager;

import javax.swing.*;
import java.awt.*;

public class TrayDemo {

    public static void main(String[] args) {
        System.out.println(SystemTray.isSupported() ? "支持":"不支持");
        final PopupMenu popup = new PopupMenu();
        final TrayIcon trayIcon =
                new TrayIcon((IconManager.play24().getImage()));
        final SystemTray tray = SystemTray.getSystemTray();
        Font f = new Font("KaiTi", Font.PLAIN, 12);

        // Create a pop-up menu components
        MenuItem aboutItem = new MenuItem("About");
        CheckboxMenuItem cb1 = new CheckboxMenuItem("Set auto size");
        CheckboxMenuItem cb2 = new CheckboxMenuItem("Set tooltip");
        Menu displayMenu = new Menu("Display");
        MenuItem errorItem = new MenuItem("Error");
        MenuItem warningItem = new MenuItem("Warning");
        MenuItem infoItem = new MenuItem("Info");
        MenuItem noneItem = new MenuItem("None");
        MenuItem exitItem = new MenuItem("Exit");

        //Add components to pop-up menu
        popup.add(aboutItem);
        popup.addSeparator();
        popup.add(cb1);
        popup.add(cb2);
        popup.addSeparator();
        popup.add(displayMenu);
        displayMenu.add(errorItem);
        displayMenu.add(warningItem);
        displayMenu.add(infoItem);
        displayMenu.add(noneItem);
        popup.add(exitItem);
        MenuItem labelItem = new MenuItem("任务栏图标文字ABCD1234");
        labelItem.setFont(f);
        UIManager.put("MenuItem.font",f);
        popup.add(labelItem);
        trayIcon.setToolTip("任务栏图标Demo");

        trayIcon.setPopupMenu(popup);

        try {
            tray.add(trayIcon);
        } catch (AWTException e) {
            System.out.println("TrayIcon could not be added.");
        }
        trayIcon.displayMessage("Caption", "text", TrayIcon.MessageType.INFO);
    }

}
