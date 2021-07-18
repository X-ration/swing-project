package com.adam.swing_project.jcompiler.cmdhelper;

import com.adam.swing_project.jcompiler.assertion.Assert;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class BashCommandExecutor extends CommandExecutor implements AsyncCommandExecutor, SyncCommandExecutor{

    private String charset;
    private String identifierPrefix;
    private int batchSize, readIndex;
    private volatile int processingIndex;
    private volatile boolean terminated = true;
    private final Object readingLock = new Object();
    private List<CommandInput> commandInputs;
    private List<CommandOutput> commandOutputs;

    public BashCommandExecutor() {
        this("UTF-8", "Executing");
    }

    public BashCommandExecutor(String charset, String identifierPrefix) {
        this.charset = charset;
        this.identifierPrefix = identifierPrefix;
    }

    public static void main(String[] args) {
        BashCommandExecutor bashCommandExecutor = new BashCommandExecutor();
        List<CommandInput> commandInputs = Arrays.asList(
                new CommandInput("pwd", "pwd"),
                new CommandInput("ls tmp", "ls tmp"),
                new CommandInput("whoami", "whoami"),
                new CommandInput("sleep 1s", "sleep")
        );
        List<CommandInput> realInputs = new ArrayList<>(commandInputs);
        for(int i=0;i<5;i++) {
            realInputs.addAll(commandInputs);
        }
        bashCommandExecutor.submit(realInputs, 5);
        int i=0;
        try {
            while (!bashCommandExecutor.finished()) {
                List<CommandOutput> commandOutputs = bashCommandExecutor.getResults();
                System.out.println("getResult" + i++ +"[" + commandOutputs.size() + "]:");
                for (CommandOutput output : commandOutputs) {
                    System.out.println(output.isSuccess() + " " + output.getMsg());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        bashCommandExecutor.reset();

        List<CommandOutput> commandOutputs = bashCommandExecutor.exec(realInputs);
        System.out.println("exec getResult[" + commandOutputs.size()+"]");
        for (CommandOutput output : commandOutputs) {
            System.out.println(output.isSuccess() + " " + output.getMsg());
        }
        bashCommandExecutor.cleanup();
    }

    @Override
    public void submit(List<CommandInput> inputList, int batchSize) {
        Assert.notNull(inputList);
        Assert.isTrue(inputList.size()>0, "CommandInput list must not empty");
        Assert.isTrue(batchSize > 0, "batchSize must >0");

        if(inputList.size() > 0) {
            List<String> commands = new ArrayList<>();
            for(CommandInput input: inputList) {
                commands.add("echo '" + identifierPrefix + " " + input.getIdentifier() + "'");
                commands.add(input.getCommand());
            }

            String scriptPath = "tmp/command.sh";
            File script;
            try {
                script = createFile(scriptPath);
                writeLinesToFile(script, commands);
            } catch (IOException e) {
                e.printStackTrace();
                throw new CommandExecutorException("create temporary file failed");
            }

            this.commandInputs = inputList;
            this.commandOutputs = new ArrayList<>();
            this.readIndex = 0;
            this.processingIndex = 0;
            this.batchSize = batchSize;
            this.terminated = false;

            executorService.submit(()->{
                Process cmdProcess = null;
                BufferedReader bufferedReader = null;
                try {
                    cmdProcess = new ProcessBuilder()
                            .redirectInput(ProcessBuilder.Redirect.PIPE)
                            .redirectOutput(ProcessBuilder.Redirect.PIPE)
                            .redirectError(ProcessBuilder.Redirect.PIPE)
                            .redirectErrorStream(true)
                            .command("bash", "-il", scriptPath)
                            .start();
                    System.out.println("["+Thread.currentThread().getName()+"]" + "bash started with pid " + cmdProcess.pid());
                    bufferedReader = new BufferedReader(new InputStreamReader(cmdProcess.getInputStream(), charset));
                    String line;
                    processingIndex = -1;
                    CommandOutput lastOutput = null;
                    while((line=bufferedReader.readLine())!=null) {
                        synchronized (readingLock) {
                            System.out.println("["+Thread.currentThread().getName()+"]" + line);
                            if(line.startsWith(identifierPrefix)) {
                                processingIndex++;
                                String requiredIdentifier = identifierPrefix + " " + commandInputs.get(processingIndex).getIdentifier();
                                Assert.isTrue(line.equals(requiredIdentifier), CommandExecutorException.class, "identifier mismatched, consider change identifier prefix. line: '" + line + "' required: '" + requiredIdentifier + "'");
                                if(lastOutput != null) {
                                    lastOutput.joinMsg();
                                }
                                lastOutput = new CommandOutput();
                                lastOutput.setSourceInput(commandInputs.get(processingIndex));
                                lastOutput.setSuccess(true);
                                commandOutputs.add(lastOutput);
                                readingLock.notify();
                            } else {
                                lastOutput.setSuccess(false);
                                lastOutput.appendMsg(line);
                                lastOutput.appendMsg(System.lineSeparator());
                            }
                        }
                    }
                    if(lastOutput != null) {
                        lastOutput.joinMsg();
                    }
                    processingIndex++;
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if(bufferedReader!=null) {
                        try {
                            bufferedReader.close();
                        } catch (IOException e) {}
                    }
                    if(cmdProcess!=null) {
                        cmdProcess.destroy();
                    }
                    deleteFile(script);
                    terminated = true;
                    synchronized (readingLock) {
                        readingLock.notify();
                    }
                    System.out.println("["+Thread.currentThread().getName()+"]" + "bash stopped.");
                }
            });

        }
    }

    private void waitIfNecessary(int readEndIndex) {
        if(!terminated) {
            synchronized (readingLock) {
                while (!terminated && processingIndex < readEndIndex) {
                    try {
                        readingLock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        throw new CommandExecutorException("bash lock exception");
                    }
                }
            }
        }
    }

    @Override
    public List<CommandOutput> getResults() {
        Assert.notNull(commandOutputs);
        Assert.isTrue(!finished(), CommandExecutorException.class, "read already complete");
        int readEndIndex = Math.min(readIndex + batchSize, commandInputs.size());
        waitIfNecessary(readEndIndex);

        List<CommandOutput> resultList = new ArrayList<>();
        while(readIndex<readEndIndex) {
            Assert.isTrue(readIndex < commandOutputs.size(), CommandExecutorException.class, "read index exceeds limit, bash probably already stopped");
            resultList.add(commandOutputs.get(readIndex++));
        }
        return resultList;
    }
    @Override
    public boolean finished() {
        return readIndex >= commandInputs.size();
    }

    private File createFile(String filePath) throws IOException {
        File file = new File(filePath);
        if(!file.exists()) {
            file.getParentFile().mkdirs();
            file.createNewFile();
        }
        return file;
    }

    private void writeLinesToFile(File file, List<String> lines) throws IOException {
        FileWriter fileWriter = new FileWriter(file);
        for(String line: lines) {
            fileWriter.write(line);
            fileWriter.write(System.lineSeparator());
        }
        fileWriter.close();
    }

    private void deleteFile(File file) {
        file.delete();
    }

    @Override
    public CommandOutput exec(CommandInput commandInput) {
        return exec(Collections.singletonList(commandInput)).get(0);
    }

    @Override
    public List<CommandOutput> exec(List<CommandInput> commandInputs) {
        submit(commandInputs);
        while(!finished()) {
            getResults();
        }
        return commandOutputs;
    }

    @Override
    public void reset() {
        super.reset();
        readIndex = 0;
        processingIndex = 0;
        terminated = true;
        commandInputs = null;
        commandOutputs = null;
    }
}
