package com.adam.swing_project.jcompiler.internal_compiler;

import com.adam.swing_project.jcompiler.JCompilerException;
import com.adam.swing_project.library.assertion.Assert;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 项目布局，指定源代码目录，资源文件目录等
 */
public class ProjectLayout {
    private File rootDir;
    private List<File> sourceDirs;
    private List<File> resources;
    private File buildDir;
    private File releaseDir;
    private String releaseFileName;
    private File manifestFile;

    public ProjectLayout(File rootDir) {
        Assert.notNull(rootDir);
        Assert.isTrue(rootDir.exists() && rootDir.isDirectory(), JCompilerException.class, "rootDir invalid");
        this.sourceDirs = new ArrayList<>();
        this.resources = new ArrayList<>();
    }

    /**
     * 默认布局：
     * project      项目根目录
     * --src        源代码目录
     * --resources  资源目录
     * --compile    编译目录
     */
    public static ProjectLayout defaultLayout(File rootDir) {
        ProjectLayout projectLayout = new ProjectLayout(rootDir);
        projectLayout.rootDir = rootDir;
        Assert.isTrue(rootDir != null && rootDir.exists() && rootDir.isDirectory(), JCompilerException.class, "ProjectLayout.rootDir invalid");
        projectLayout.sourceDirs.add(new File(rootDir.getPath() + File.separator + "src"));
        projectLayout.resources.add(new File(rootDir.getPath() + File.separator + "resources"));
        projectLayout.buildDir = new File(rootDir.getPath() + File.separator + "compile");
        projectLayout.releaseDir = new File(rootDir.getPath() + File.separator + "release");
        projectLayout.releaseFileName = rootDir.getAbsoluteFile().getName() + ".jar";
        projectLayout.manifestFile = new File(projectLayout.releaseDir.getPath() + File.separator + "manifest");
        return projectLayout;
    }

    public File getRootDir() {
        return rootDir;
    }

    public List<File> getSourceDirs() {
        return sourceDirs;
    }

    public List<File> getResources() {
        return resources;
    }

    public File getBuildDir() {
        return buildDir;
    }

    public File getReleaseDir() {
        return releaseDir;
    }

    public String getReleaseFileName() {
        return releaseFileName;
    }

    public File getManifestFile() {
        return manifestFile;
    }

    /**
     * 检查必须根目录、源代码目录、输出目录是否已指定
     * @return
     */
    public boolean isValid() {
        boolean rootDirValid = (rootDir != null && rootDir.exists() && rootDir.isDirectory());
        if(!rootDirValid) return false;
        boolean sourceDirsValid = (sourceDirs != null && sourceDirs.size() > 0);
        if(!sourceDirsValid) return false;
        for(File sourceDir: sourceDirs) {
            boolean sourceDirValid = (sourceDir != null && sourceDir.exists() && sourceDir.isDirectory());
            if(!sourceDirValid) return false;
        }
        boolean compileDirValid = (buildDir != null && buildDir.exists() && buildDir.isDirectory());
        if(!compileDirValid) return false;

        //资源目录是可选的
        if(resources != null && resources.size() > 0) {
            for(File resourceDir: resources) {
                boolean resourceDirValid = (resourceDir != null && resourceDir.exists() && resourceDir.isDirectory());
                if(!resourceDirValid) return false;
            }
        }
        //打包目录是可选的
        if(releaseDir != null) {
            boolean releaseDirValid = (releaseDir.exists() && releaseDir.isDirectory());
            if(!releaseDirValid) return false;
        }
        //清单文件是可选的
        if(manifestFile != null) {
            boolean manifestFileValid = (manifestFile.exists() && manifestFile.isFile());
            if(!manifestFileValid)  return false;
        }
        return true;
    }

    public static void main(String[] args) {
        ProjectLayout projectLayout = defaultLayout(new File("D:\\Users\\Adam\\Documents\\Coding\\swing-project\\jcompiler"));
        System.out.println(projectLayout.rootDir.getAbsolutePath());
        System.out.println(projectLayout.isValid());
        System.out.println(projectLayout.releaseFileName);
    }
}
