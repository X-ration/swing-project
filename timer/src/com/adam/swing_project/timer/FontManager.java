package com.adam.swing_project.timer;

import java.awt.*;

public class FontManager {

    public static Font getByNameStyleSize(String fontName, int fontStyle, int fontSize) {
        return new Font(fontName, fontStyle, fontSize);
    }

}
