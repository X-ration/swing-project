package com.adam.swing_project.jcompiler.ajswing;

import com.adam.swing_project.jcompiler.assertion.Assert;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;

/**
 * 实现自动滚动的ScrollPane组件
 */
public class AJScrollPane extends JScrollPane {

    public AJScrollPane(Component component) {
        super(component);
    }

    public AJScrollPane(Component component, int vsbPolicy, int hsbPolicy) {
        super(component, vsbPolicy, hsbPolicy);
    }

    /**
     * 设置在水平方向自动滚动
     */
    public void setHorizontalAutoScroll() {
        getHorizontalScrollBar().getModel().addChangeListener(new ScrollBarChangeListener(getHorizontalScrollBar()));
    }

    /**
     * 设置在垂直方向自动滚动
     */
    public void setVerticalAutoScroll() {
        getVerticalScrollBar().getModel().addChangeListener(new ScrollBarChangeListener(getVerticalScrollBar()));
    }

    class ScrollBarChangeListener implements ChangeListener {
        private JScrollBar target;
        private int lastMax;

        ScrollBarChangeListener(JScrollBar target) {
            Assert.notNull(target);
            this.target = target;
            this.lastMax = target.getMaximum();
        }


        @Override
        public void stateChanged(ChangeEvent e) {
            int curMax = ((DefaultBoundedRangeModel)e.getSource()).getMaximum();
            if(curMax != lastMax) {
                lastMax = curMax;
                target.setValue(target.getMaximum());
            }
        }
    }

}
