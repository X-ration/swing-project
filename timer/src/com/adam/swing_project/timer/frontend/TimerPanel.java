package com.adam.swing_project.timer.frontend;

import com.adam.swing_project.library.logger.Logger;
import com.adam.swing_project.library.timer.newcode.Timer;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedList;
import java.util.List;

/**
 * 计时器面板，组装多个SingleTimerPanel实例。
 */
public class TimerPanel extends JPanel {

    private final JFrame jFrame;
    private int timerPanelCount = 0;
    private List<NewSingleTimerPanel> singleTimerPanelList = new LinkedList<>();
    private final Logger logger = Logger.createLogger(this);

    public TimerPanel(JFrame jFrame) {
        this.jFrame = jFrame;
        setLayout(new GridBagLayout());
    }

    public void addSingleTimerPanel() {
        addSingleTimerPanel(null);
    }

    public void addSingleTimerPanel(Timer timer) {
        NewSingleTimerPanel singleTimerPanel;
        if(timer == null) {
            singleTimerPanel = new NewSingleTimerPanel(jFrame);
        }  else {
            singleTimerPanel = new NewSingleTimerPanel(jFrame, timer);
        }
        singleTimerPanelList.add(singleTimerPanel);
        addSingleTimerPanelInternal(singleTimerPanel);
        timerPanelCount++;
    }

    private void addSingleTimerPanelInternal(NewSingleTimerPanel singleTimerPanel) {
        //每行摆3个计时器
        int gridx = timerPanelCount % 3, gridy = timerPanelCount / 3;
        logger.logDebug("add single timer panel [" + gridx + "," + gridy + "]");
        GridBagConstraints gridBagConstraints = new GridBagConstraints(gridx, gridy, 1, 1, 1, 1,
                GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(10,10,10,10), 0, 0);
        add(singleTimerPanel, gridBagConstraints);
    }

    public void removeSingleTimerPanel(NewSingleTimerPanel singleTimerPanel) {
        remove(singleTimerPanel);
        singleTimerPanelList.remove(singleTimerPanel);
        timerPanelCount--;
        for (NewSingleTimerPanel singleTimerPanel1: singleTimerPanelList) {
            remove(singleTimerPanel1);
            timerPanelCount--;
        }
        for(NewSingleTimerPanel singleTimerPanel1: singleTimerPanelList) {
            addSingleTimerPanelInternal(singleTimerPanel1);
            timerPanelCount++;
        }
        //TimerPanel被包裹在JScrollPane中，所以要获取父容器重绘
        getParent().revalidate();
        getParent().repaint();
    }

}
