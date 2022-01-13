package com.adam.swing_project.library.logger;

import com.adam.swing_project.library.datetime.Date;
import com.adam.swing_project.library.util.DateTimeUtil;

import java.io.File;

public class RollingFileLogger extends AsyncFileLogger{

    private interface RollingFileMapper {
        File mapToRollingFile(File file);
    }

    public enum RollingFileMode {
        BY_DAY(file -> {
            Date date = DateTimeUtil.getCurrentDate();
            String dateString = DateTimeUtil.wrapDateYearToDay(date);
            String originalFilePath = file.getPath();
            int lastDot = originalFilePath.lastIndexOf('.');
            String newFilePath;
            if(lastDot != -1) {
                newFilePath = originalFilePath.substring(0, lastDot) + '-' + dateString + originalFilePath.substring(lastDot);
            } else {
                newFilePath = originalFilePath + '-' + dateString;
            }
            return new File(newFilePath);
        })
        ;
        private RollingFileMapper mapper;
        RollingFileMode(RollingFileMapper mapper) {
            this.mapper = mapper;
        }
    }

    private final RollingFileMapper rollingFileMapper;

    private RollingFileLogger(Object object, File logFile, RollingFileMode mode) {
        super(object, logFile);
        this.rollingFileMapper = mode.mapper;
    }

    public static RollingFileLogger createLogger(Object object, File logFile, RollingFileMode mode) {
        return new RollingFileLogger(object, logFile, mode);
    }

    @Override
    protected void doLog(String msg) {
        FileLog fileLog = new FileLog(msg, rollingFileMapper.mapToRollingFile(logFile));
        enqueueLog(fileLog);
    }
}
