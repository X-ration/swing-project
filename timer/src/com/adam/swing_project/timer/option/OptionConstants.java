package com.adam.swing_project.timer.option;

public class OptionConstants {

    public static final String OPTION_ROOT_WORK_DIR = "root.workDir";
    public static final String OPTION_ROOT_LOG_FILE_ENABLED = "root.log.fileEnabled";
    public static final String OPTION_ROOT_LOG_DEBUG_ENABLED = "root.log.debugEnabled";

    public static final String OPTION_GENERAL_STAT_DEFAULT = "general.stat.default";


    public enum StatDefaultMethod {
        DISABLED, STAT_BY_START_DAY, STAT_BY_END_DAY
    }
}
