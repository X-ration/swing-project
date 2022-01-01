package com.adam.swing_project.timer.app_info;

import com.adam.swing_project.library.app_info.ManifestAppInfo;
import com.adam.swing_project.library.util.ApplicationArgumentResolver;

import java.io.IOException;

public class TimerAppInfo extends ManifestAppInfo {

    private final String env;
    private String titleString;

    public TimerAppInfo(ApplicationArgumentResolver argumentResolver) throws IOException {
        super();
        this.env = argumentResolver.getOptionValue("env");
    }

    public String getEnv() {
        return env;
    }

    public void setTitleString(String titleString) {
        this.titleString = titleString;
    }

    public String getTitleString() {
        return titleString;
    }
}
