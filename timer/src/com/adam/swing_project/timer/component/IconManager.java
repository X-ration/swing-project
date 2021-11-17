package com.adam.swing_project.timer.component;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * Created by Adam.
 * 2021-10-27 21:28
 * Modified 1
 */
 //todo 或许有内存问题
public class IconManager {

    public static ImageIcon timer24() {
        return getByFileNameOriginalSize("icons8-time-limit-24.png");
    }

    public static ImageIcon play24() {
        return getByFileNameOriginalSize("icons8-play-24.png");
    }

    public static ImageIcon stop24() {
        return getByFileNameOriginalSize("icons8-stop-24.png");
    }

    public static ImageIcon pause24() {
        return getByFileNameOriginalSize("icons8-pause-24.png");
    }

    public static ImageIcon noRecord24() {
        return getByFileNameOriginalSize("icons8-no-record-24.png");
    }

    public static ImageIcon edit24() {
        return getByFileNameOriginalSize("icons8-edit-24.png");
    }

    public static ImageIcon trash24() {
        return getByFileNameAndSquareSize("icons8-trash-96.png", 24);
    }

    /**
     * 指定宽度、高度缩放图片
     * @param fileName
     * @param width
     * @param height
     * @return
     */
    private static ImageIcon getByFileNameAndRectangleSize(String fileName, int width, int height) {
        ImageIcon imageIcon = null;
        try {
            /*BufferedImage srcImage = ImageIO.read(IconManager.class.getResourceAsStream("/" + fileName));
            double newW = width * 1.0 / srcImage.getWidth(), newH = height * 1.0 / srcImage.getHeight();
            AffineTransformOp affineTransformOp = new AffineTransformOp(AffineTransform.getScaleInstance(newW, newH), null);
            Image Itemp = affineTransformOp.filter(srcImage, null);
            imageIcon = new ImageIcon(Itemp);
            */
            BufferedImage bufferedImage = ImageIO.read(IconManager.class.getResourceAsStream("/" + fileName))
                    , newImage = new BufferedImage(width, height, bufferedImage.getType());
            Graphics graphics = newImage.getGraphics();
            graphics.drawImage(bufferedImage, 0, 0, width, height, null);
            graphics.dispose();
            imageIcon = new ImageIcon(newImage);
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    public static void main(String[] args) {
        ImageIcon imageIcon = edit24();
        System.out.println(imageIcon);
    }

}
