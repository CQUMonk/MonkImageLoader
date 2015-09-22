package com.cqumonk.monkimageloader.bean;

/**
 * Created by CQUMonk on 2015/9/15.
 */
public class Dir {
    private String dirPath;
    private String firstPicPath;
    private String dirName;
    private int picCount;

    public String getDirPath() {
        return dirPath;
    }

    public void setDirPath(String dirPath) {
        this.dirPath = dirPath;
        int lastSplit=dirPath.lastIndexOf('/');
        this.dirName=dirPath.substring(lastSplit);
    }

    public String getFirstPicPath() {
        return firstPicPath;
    }

    public void setFirstPicPath(String firstPicPath) {
        this.firstPicPath = firstPicPath;
    }

    public String getDirName() {
        return dirName;
    }

    public int getPicCount() {
        return picCount;
    }

    public void setPicCount(int picCount) {
        this.picCount = picCount;
    }
}
