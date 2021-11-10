package com.adam.swing_project.timer.frontend;

import com.adam.swing_project.timer.component.TrayIconManager;

import javax.swing.*;
import java.awt.*;

/**
 * 计时器面板，组装多个SingleTimerPanel实例。
 */
public class TimerPanel extends JPanel {

    private final JFrame jFrame;
    private int timerPanelCount = 0;

    public TimerPanel(JFrame jFrame) {
        this.jFrame = jFrame;
        setLayout(new GridBagLayout());
    }

    public void addSingleTimerPanel() {
        SingleTimerPanel singleTimerPanel = new SingleTimerPanel(jFrame);
        //每行摆3个计时器
        int gridx = timerPanelCount % 3, gridy = timerPanelCount / 3;
        GridBagConstraints gridBagConstraints = new GridBagConstraints(gridx, gridy, 1, 1, 1, 1,
                GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(10,10,10,10), 0, 0);
        add(singleTimerPanel, gridBagConstraints);
        singleTimerPanel.getTimer().registerTimerListener(singleTimerPanel.getTimer().new TimeAdapter() {
            @Override
            public void timerStopped() {
                TrayIconManager.getInstance().pushMessageToTrayIcon("计时器", "时间到啦！", TrayIcon.MessageType.INFO);
            }
        });
        timerPanelCount++;
    }

}
