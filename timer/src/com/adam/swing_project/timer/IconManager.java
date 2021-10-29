package com.adam.swing_project.timer;

import javax.swing.*;
import java.awt.*;

/**
 * Created by Adam.
 * 2021-10-27 21:28
 * Modified 1
 */
public class IconManager {

    public static ImageIcon timer24() {
        return getByFileNameOriginalSize("icons8-time-limit-24.png");
    }

    /**
     * 指定宽度、高度缩放图片
     * @param fileName
     * @param width
     * @param height
     * @return
     */
    private static ImageIcon getByFileNameAndRectangleSize(String fileName, int width, int height) {
        Image srcImage = Toolkit.getDefaultToolkit().getImage(IconManager.class.getResource("/"+fileName));
        Image newImage = srcImage.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        ImageIcon imageIcon = new ImageIcon(newImage);
        return imageIcon;
    }

    /**
     * 指定正方形边长缩放图片
     * @param fileName
     * @param length
     * @return
     */
    private static ImageIcon getByFileNameAndSquareSize(String fileName, int length) {
        return getByFileNameAndRectangleSize(fileName, length, length);
    }

    /**
     * 按图片本身大小展示
     * @param fileName
     * @return
     */
    private static ImageIcon getByFileNameOriginalSize(String fileName) {
        return new ImageIcon(IconManager.class.getResource("/"+fileName));
    }

}
