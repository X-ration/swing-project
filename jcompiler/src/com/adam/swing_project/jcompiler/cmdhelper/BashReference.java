package com.adam.swing_project.jcompiler.cmdhelper;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class BashReference {
    public static void main(String[] args) {
        System.out.println(Charset.defaultCharset().name());
        Process cmdProcess = null;

        try {
            List<String> commands = Arrays.asList("echo 'executing pwd'", "pwd",  "echo 'executing javac'", "javac Main.java", "echo 'executing fake'", "fake", "echo 'executing ll'", "ll");
            String path = "tmp/command.sh";
            File tempScript = new File(path);
            if(!tempScript.exists()) {
                tempScript.getParentFile().mkdirs();
                tempScript.createNewFile();
            }
            FileWriter fileWriter = new FileWriter(tempScript);
            for(int i=0;i<10;i++) {
                for (String command : commands) {
                    fileWriter.write(command);
                    fileWriter.write(System.lineSeparator());
                }
            }
            fileWriter.close();

//            Process cmdProcess = Runtime.getRuntime().exec("bash");
            cmdProcess = new ProcessBuilder()
                    .redirectInput(ProcessBuilder.Redirect.PIPE)
                    .redirectOutput(ProcessBuilder.Redirect.PIPE)
                    .redirectError(ProcessBuilder.Redirect.PIPE)
                    .command("bash", "-il", path)
                    .redirectErrorStream(true)
                    .start();
            System.out.println("PID:" + cmdProcess.pid());
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(cmdProcess.getInputStream(), "UTF-8"))
                    , bufferedErrorReader = new BufferedReader(new InputStreamReader(cmdProcess.getErrorStream(), "UTF-8"));
            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(cmdProcess.getOutputStream(), "UTF-8"));
//            cmdProcess.waitFor();

            int i=0;
            String line;
            System.out.println("Read " + (++i));
            while((line=bufferedReader.readLine()) != null) {
                System.out.println(line.length()+line);
            }
            while((line=bufferedErrorReader.readLine()) != null) {
                System.err.println(line);
            }

            bufferedReader.close();
            bufferedErrorReader.close();
            bufferedWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(cmdProcess != null) {
                cmdProcess.destroy();
            }
        }
    }
}
