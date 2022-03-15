package com.adam.swing_project.timer.component;

import com.adam.swing_project.library.logger.Logger;
import com.adam.swing_project.library.logger.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Created by Adam.
 * 2021-10-27 21:28
 * Modified 1
 */
 //todo 或许有内存问题
public class IconManager {

    private static class IconCacheKey {
        String iconName;
        boolean isOriginal;
        int requiredWidth, requiredHeight;

        IconCacheKey(String iconName) {
            this.iconName = iconName;
            this.isOriginal = true;
        }

        IconCacheKey(String iconName, int requiredWidth, int requiredHeight) {
            this.iconName = iconName;
            this.isOriginal = false;
            this.requiredWidth = requiredWidth;
            this.requiredHeight = requiredHeight;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            IconCacheKey that = (IconCacheKey) o;
            return isOriginal == that.isOriginal && requiredWidth == that.requiredWidth && requiredHeight == that.requiredHeight && Objects.equals(iconName, that.iconName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(iconName, isOriginal, requiredWidth, requiredHeight);
        }
    }

    private static final Map<IconCacheKey, ImageIcon> ICON_CACHE = new HashMap<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(IconManager.class);

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
        IconCacheKey iconCacheKey = new IconCacheKey(fileName, width, height);
        ImageIcon imageIcon = ICON_CACHE.get(iconCacheKey);
        if(imageIcon == null) {
            try {
                BufferedImage bufferedImage = ImageIO.read(IconManager.class.getResourceAsStream("/icon/" + fileName)), newImage = new BufferedImage(width, height, bufferedImage.getType());
                Graphics graphics = newImage.getGraphics();
                graphics.drawImage(bufferedImage, 0, 0, width, height, null);
                graphics.dispose();
                imageIcon = new ImageIcon(newImage);
                ICON_CACHE.put(iconCacheKey, imageIcon);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            LOGGER.logDebug("icon cache hit " + fileName + " " + width + "*" + height);
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
        IconCacheKey iconCacheKey = new IconCacheKey(fileName);
        ImageIcon imageIcon = ICON_CACHE.get(iconCacheKey);
        if(imageIcon == null) {
            imageIcon = new ImageIcon(IconManager.class.getResource("/icon/" + fileName));
            ICON_CACHE.put(iconCacheKey, imageIcon);
        } else {
            LOGGER.logDebug("icon cache hit " + fileName + " original");
        }
        return imageIcon;
    }

    public static void main(String[] args) {
        int i=200;
        int j=1000;
        while(i-->0) {
            for(int k=0;k<j;k++) {
                ImageIcon imageIcon = edit24();
            }
        }
        System.out.println("End");
    }

}
