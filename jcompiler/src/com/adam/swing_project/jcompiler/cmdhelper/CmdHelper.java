package com.adam.swing_project.jcompiler.cmdhelper;

import com.adam.swing_project.jcompiler.assertion.Assert;
import com.adam.swing_project.jcompiler.iohelper.CharIOHelper;
import com.adam.swing_project.jcompiler.iohelper.IOHelperException;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.Iterator;

/**
 * Cmd命令行交互接口
 */
public class CmdHelper {

    private Process cmdProcess;
    private BufferedReader stdOutReader, stdErrReader;
    private BufferedWriter stdInWriter;
    private String charset, bound, cmdName;
    private boolean isStarted;
    private CmdHelperLogger cmdHelperLogger;
    private CharIOHelper charIOHelper;

    private int lastCommandLen;

    public static void main(String[] args) {
        CmdHelper cmdHelper = new CmdHelper(new DefaultCmdHelperLogger());
        cmdHelper.startup();
        cmdHelper.exec("dir");
        cmdHelper.stop();
    }

    public CmdHelper() {
        this(null, null, null, new DefaultCmdHelperLogger(), null);
    }

    public CmdHelper(CmdHelperLogger cmdHelperLogger) {
        this(null, null, null, cmdHelperLogger, null);
    }

    public CmdHelper(String charset, String bound, String cmdName, CmdHelperLogger cmdHelperLogger, CharIOHelper charIOHelper) {
        this.charset = getCharsetOrDefault(charset);
        this.bound = getBoundOrDefault(bound);
        this.cmdName = getCmdNameOrDefault(cmdName);
        this.cmdHelperLogger = cmdHelperLogger;
        this.charIOHelper = charIOHelper;
        this.lastCommandLen = 0;
    }

    public void exec(Iterator<String> commands) {
        Assert.notNull(commands);
        while(commands.hasNext()) {
            exec(commands.next());
        }
    }

    public void exec(String command) {
        requireStarted();
        Assert.notNull(command, CmdHelperException.class, "CmdHelper require exec command not null");
        if(cmdProcess.isAlive() && command.length() > 0) {
            try {
                writeStdIn(command);
                readStdOutAndStdErr();
            } catch (IOHelperException e) {
                stop();
                CmdHelperException newE = new CmdHelperException("cmdProcess IO exception");
                newE.initCause(e);
                throw newE;
            }
        } else {
            stop();
            throw new CmdHelperException("cmdProcess is dead");
        }
    }

    public void requireStarted() {
        Assert.isTrue(isStarted, CmdHelperException.class, "Method requires CmdHelper started");
    }

    public void startup() {
        if(!isStarted) {
            try {
                System.out.print(System.lineSeparator() + "CmdHelper started at " + new Date() + System.lineSeparator());
                this.cmdProcess = Runtime.getRuntime().exec(cmdName);
                this.stdOutReader = new BufferedReader(new InputStreamReader(cmdProcess.getInputStream(), this.charset));
                this.stdErrReader = new BufferedReader(new InputStreamReader(cmdProcess.getErrorStream(), this.charset));
                this.stdInWriter = new BufferedWriter(new OutputStreamWriter(cmdProcess.getOutputStream(), this.charset));
                this.charIOHelper = new CharIOHelper(stdOutReader, stdErrReader, stdInWriter);
                isStarted = true;
                //读取命令行启动时的输出
                readStdOutAndStdErr();
            } catch (IOException e) {
                e.printStackTrace();
                reset();
                throw new CmdHelperException("CmdHelper startup failed");
            }
        }
    }

    public void stop() {
        if(isStarted) {
            try {
                this.stdOutReader.close();
            } catch (IOException e) {
            }
            try {
                this.stdErrReader.close();
            } catch (IOException e) {
            }
            try {
                this.stdInWriter.close();
            } catch (IOException e) {
            }
            this.cmdProcess.destroy();
            isStarted = false;
            reset();
            System.out.print(System.lineSeparator() + "CmdHelper terminated at " + new Date() + System.lineSeparator());
        }
    }

    public void reset() {
        this.cmdProcess = null;
        this.stdOutReader = null;
        this.stdErrReader = null;
        this.stdInWriter = null;
        this.charIOHelper = null;
        this.isStarted = false;
    }

    public void readStdOutAndStdErr() throws IOHelperException {
        boolean isOver = false;
        String lastLine = null;
        String bufString;
        while(!isOver && ((bufString = charIOHelper.readStdOut()) != null)) {
            String printString = bufString;
            if(bufString.endsWith(this.bound)) {
                int index = bufString.lastIndexOf(System.lineSeparator());
                if(index != -1) {
                    lastLine = bufString.substring(index);
                    printString = bufString.substring(0, index);
                    isOver = true;
                }
            }
            if(lastCommandLen != 0) {
                printString = printString.substring(lastCommandLen + 2);
                lastCommandLen = 0;
            }
            cmdHelperLogger.logStdOut(printString);

        }

        while((bufString = charIOHelper.readStdErr()) != null) {
            cmdHelperLogger.logStdErr(bufString);
        }

        if(!cmdProcess.isAlive()) {
            cmdHelperLogger.logStdErr("process is dead");
            stop();
            throw new CmdHelperException("Cmd process is dead");
        }

        cmdHelperLogger.logStdOut(lastLine);
    }

    public void writeStdIn(String text) throws IOHelperException {
        Assert.isTrue(text != null);
        if(text.length() > 0) {
            this.lastCommandLen = text.length();
            String command = text + System.lineSeparator();
            cmdHelperLogger.logStdIn(command);
            charIOHelper.writeStdIn(command);
        }
    }

    private String getCharsetOrDefault(String charset) {
        if(charset != null) {
            return charset;
        } else {
            String osName = System.getProperty("os.name");
            if (osName != null) {
                if (osName.startsWith("Windows")) {
                    charset = "GBK";
                } else if(osName.startsWith("Linux")) {
                    charset = "UTF-8";
                }
            }
            if(charset == null) {
                charset = Charset.defaultCharset().name();
            }
            return charset;
        }
    }

    private String getBoundOrDefault(String bound) {
        if(bound != null) {
            return bound;
        } else {
            String osName = System.getProperty("os.name");
            if (osName != null) {
                if (osName.startsWith("Windows")) {
                    bound = ">";
                } else if(osName.startsWith("Linux")) {
                    bound = "$";
                }
            }
            return bound;
        }
    }

    private String getCmdNameOrDefault(String cmdName) {
        if(cmdName != null) {
            return cmdName;
        } else {
            String osName = System.getProperty("os.name");
            if(osName != null) {
                if(osName.startsWith("Windows")) {
                    cmdName = "cmd";
                } else if(osName.startsWith("Linux")) {
                    cmdName = "bash";
                }
            }
            return cmdName;
        }
    }

}
