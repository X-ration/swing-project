package com.adam.swing_project.jcompiler.iohelper;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

/**
 * 字符流处理便捷工具类
 */
public class CharIOHelper implements IOHelper{

    /**
     * 标准输入和标准错误输入
     */
    private Reader stdOutReader, stdErrReader;
    /**
     * 标准输出
     */
    private Writer stdInWriter;
    /**
     * 缓冲数组
     */
    private char[] buffer;

    public CharIOHelper(Reader stdOutReader, Reader stdErrReader, Writer stdInWriter) {
        this(stdOutReader, stdErrReader, stdInWriter, 1024);
    }

    public CharIOHelper(Reader stdOutReader, Reader stdErrReader, Writer stdInWriter, int bufferSize) {
        this.stdOutReader = stdOutReader;
        this.stdErrReader = stdErrReader;
        this.stdInWriter = stdInWriter;
        this.buffer = new char[bufferSize];
    }

    public String readStdOut() throws IOHelperException{
        try {
            int len;
            if ((len = stdOutReader.read(buffer)) != -1) {
                return new String(buffer, 0, len);
            } else {
                return null;
            }
        } catch (IOException e) {
            IOHelperException newE = new IOHelperException("IOHelperException during reading stdout: " + e.getMessage());
            newE.setStackTrace(e.getStackTrace());
            throw newE;

        }
    }

    public String readStdErr() throws IOHelperException {
        try {
            int len;
            if (stdErrReader.ready() && (len = stdErrReader.read(buffer)) != -1) {
                return new String(buffer, 0, len);
            } else {
                return null;
            }
        } catch (IOException e) {
            IOHelperException newE = new IOHelperException("IOHelperException during reading stderr: " + e.getMessage());
            newE.setStackTrace(e.getStackTrace());
            throw newE;
        }
    }

    public void writeStdIn(String text) throws IOHelperException{
        try {
            stdInWriter.write(text);
            stdInWriter.flush();
        } catch (IOException e) {
            IOHelperException newE = new IOHelperException("IOHelperException during writing stdout: " + e.getMessage());
            newE.setStackTrace(e.getStackTrace());
            throw newE;
        }
    }

}
