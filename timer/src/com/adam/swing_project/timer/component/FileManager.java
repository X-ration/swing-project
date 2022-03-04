package com.adam.swing_project.timer.component;

import com.adam.swing_project.timer.TimerProgram;
import com.adam.swing_project.library.assertion.Assert;
import com.adam.swing_project.timer.option.OptionConstants;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * 文件管理器
 */
public class FileManager {

    private static final FileManager instance = new FileManager();
    private final Map<String, File> resourceTempFileMap = new HashMap<>();
    private final String TEMP_FILE_PREFIX = "swing_project.timer";

    private File appRootDir;

    private FileManager() {
        String rootWorkDir = OptionManager.getInstance().getOptionValueOrDefault(OptionConstants.OPTION_ROOT_WORK_DIR, String.class,
                System.getProperty("user.home") + File.separator + "swing-timer");
        updateAppRootDir(rootWorkDir);
    }

    /**
     * 变更工作目录，此方法目前只能通过选项面板调用
     * @param rootWorkDir
     */
    public void updateAppRootDir(String rootWorkDir) {
        appRootDir = new File(rootWorkDir);
        if(!appRootDir.exists()) {
            appRootDir.mkdir();
        }
    }

    /**
     * 在根目录下新建一个文件夹
     * @param subDirName
     */
    public File requireSubDir(String subDirName) {
        return requireSubDir(appRootDir, subDirName);
    }

    /**
     * 在指定目录下新建一个文件夹
     * @param rootDir
     * @param subDirName
     */
    private File requireSubDir(File rootDir, String subDirName) {
        if(!rootDir.exists()) {
            rootDir.mkdir();
        }
        File subDir = new File(rootDir, subDirName);
        if(!subDir.exists()) {
            subDir.mkdir();
        }
        return subDir;
    }

    /**
     * 从jar包中读取文件，并写入到temp文件夹缓存
     */
    public File readFileForResourcePath(String resourcePath) {
        Assert.notNull(resourcePath);
        File tempFile = resourceTempFileMap.get(resourcePath);
        if(tempFile != null) {
            return tempFile;
        }
        InputStream soundInputStream = TimerProgram.class.getResourceAsStream(resourcePath);
        byte[] buffer = new byte[1024];
        try {
            String[] splits = resourcePath.split("\\.");
            String format = "";
            if(splits != null && splits.length > 0) {
                format = splits[splits.length-1];
            }
            tempFile = File.createTempFile(TEMP_FILE_PREFIX, format);
            FileOutputStream fileOutputStream = new FileOutputStream(tempFile);
            int n = 0;
            while ((n = soundInputStream.read(buffer, 0, buffer.length)) != -1) {
                fileOutputStream.write(buffer, 0, n);
            }
            fileOutputStream.close();
            return tempFile;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static FileManager getInstance() {
        return instance;
    }

    public void cleanTempFiles() {
        for(File tempFile: resourceTempFileMap.values()) {
            tempFile.delete();
        }
    }

}
