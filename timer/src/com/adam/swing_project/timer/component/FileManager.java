package com.adam.swing_project.timer.component;

import com.adam.swing_project.library.util.ApplicationArgumentResolver;
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
    }

    public void init(ApplicationArgumentResolver  argumentResolver) {
//        String rootWorkDir = OptionManager.getInstance().getOptionValueOrDefault(OptionConstants.OPTION_ROOT_WORK_DIR, String.class,
//                System.getProperty("user.home") + File.separator + "swing-timer");
        String rootWorkDir = RootConfigStorage.getInstance().getRootConfig(OptionConstants.OPTION_ROOT_WORK_DIR);
        if(rootWorkDir == null) {
            rootWorkDir = System.getProperty("user.home") + File.separator + "swing-timer";
            RootConfigStorage.getInstance().updateRootConfig(OptionConstants.OPTION_ROOT_WORK_DIR, rootWorkDir);
        }
        updateAppRootDir(rootWorkDir, argumentResolver);
    }

    /**
     * 变更根目录，此方法目前只能通过选项面板调用
     * 根目录默认与工作目录(root.workDir)相同；如果命令行指定了env参数则为工作目录下env-{env}子目录。
     * @param rootWorkDir
     */
    public void updateAppRootDir(String rootWorkDir) {
        ApplicationArgumentResolver argumentResolver = ApplicationManager.getInstance().getProgramGlobalObject(ApplicationArgumentResolver.class);
        updateAppRootDir(rootWorkDir, argumentResolver);
    }
    public void updateAppRootDir(String rootWorkDir, ApplicationArgumentResolver argumentResolver) {
        updateAppRootDir(new File(rootWorkDir));
        String env = argumentResolver.getOptionValue("env");
        if(env != null) {
            String subDirName =  ("env-" + env);
            File subDir = requireSubDir(subDirName);
            updateAppRootDir(subDir);
        }
    }

    private void updateAppRootDir(File appRootDir) {
        this.appRootDir = appRootDir;
        if(!this.appRootDir.exists()) {
            this.appRootDir.mkdir();
        }
    }

    /**
     * 在根目录下新建一个文件夹
     * @param subDirName
     */
    public File requireSubDir(String subDirName) {
        return requireSubDir(appRootDir, subDirName);
    }

    public File getAppRootDir() {
        return appRootDir;
    }

    /**
     * 在根目录下新建一个文件
     * @param subFileName
     * @return
     */
    public File requireSubFile(String subFileName) {
        File subFile = new File(appRootDir, subFileName);
        if(!subFile.exists()) {
            try {
                subFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return subFile;
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
