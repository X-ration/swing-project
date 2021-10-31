package com.adam.swing_project.timer.ajswing;

import com.adam.swing_project.timer.assertion.Assert;

import javax.swing.*;
import java.awt.event.ActionListener;

/**
 * 一按钮多种功能
 */
public class AJButton extends JButton {

    /**
     * 状态数量，不同状态对应不同的功能
     */
    private int totalStatus = 0, currentStatus = 0, lastStatus = 0;
    private final AJButtonSettingFunction[] buttonSettingFunctionArray;
    private final ActionListener[] actionListenerArray;

    public interface AJButtonSettingFunction {
        void setAJButton(AJButton ajButton);
    }

    public AJButton(int totalStatus, int initialStatus) {
        super();
        Assert.isTrue(totalStatus > 0, "状态种类>0!");
        Assert.isTrue(initialStatus >= 0 && initialStatus < totalStatus, "初始状态有误！");
        this.totalStatus = totalStatus;
        this.currentStatus = initialStatus;
        this.lastStatus = initialStatus;
        this.buttonSettingFunctionArray = new AJButtonSettingFunction[totalStatus];
        this.actionListenerArray = new ActionListener[totalStatus];
    }

    public int getCurrentStatus() {
        return currentStatus;
    }

    public int getLastStatus() {
        return lastStatus;
    }

    public void bind(int status, AJButtonSettingFunction settingFunction, ActionListener actionListener) {
        Assert.isTrue(status >= 0 && status < totalStatus, "非法状态" + status + "超出范围" + totalStatus);
        buttonSettingFunctionArray[status] = settingFunction;
        actionListenerArray[status] = actionListener;
        if(status == currentStatus) {
            settingFunction.setAJButton(this);
            this.addActionListener(actionListener);
        }
    }

    public void changeStatus(int newStatus) {
        Assert.isTrue(newStatus >= 0 && newStatus < totalStatus, "非法状态" + newStatus + "超出范围" + totalStatus);
        if(newStatus == currentStatus)
            return;

        this.removeActionListener(actionListenerArray[currentStatus]);
        buttonSettingFunctionArray[newStatus].setAJButton(this);
        this.addActionListener(actionListenerArray[newStatus]);
        this.lastStatus = this.currentStatus;
        this.currentStatus = newStatus;
    }

}
