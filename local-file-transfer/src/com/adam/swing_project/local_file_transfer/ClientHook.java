package com.adam.swing_project.local_file_transfer;

import javax.swing.*;
import java.io.*;
import java.net.Socket;

public class ClientHook {

    @HookCompleteCheck(HookCompleteCheck.CHECK_CONSTANT.REQUIRED)
    private JPanel clientPanel;
    @HookCompleteCheck(HookCompleteCheck.CHECK_CONSTANT.REQUIRED)
    private JTextField clientIpField, clientPortField;
    @HookCompleteCheck(HookCompleteCheck.CHECK_CONSTANT.REQUIRED)
    private JLabel clientConsole, clientDirectory;
    @HookCompleteCheck(HookCompleteCheck.CHECK_CONSTANT.REQUIRED)
    private JButton clientConnectButton, clientReceiveButton, clientDirectoryButton;
    @HookCompleteCheck(HookCompleteCheck.CHECK_CONSTANT.REQUIRED)
    private JFileChooser clientDirectoryChooser;
    private HookCompleteChecker hookCompleteChecker;
    private String iClientDirectory = System.getProperty("user.home");

    public ClientHook() {
        this.hookCompleteChecker = new HookCompleteChecker(this);
    }

    public void setClientDirectoryChooser(JFileChooser clientDirectoryChooser) {
        this.clientDirectoryChooser = clientDirectoryChooser;
    }

    public void setClientConnectButton(JButton clientConnectButton) {
        this.clientConnectButton = clientConnectButton;
    }

    public void setClientDirectory(JLabel clientDirectory) {
        this.clientDirectory = clientDirectory;
    }

    public void setClientDirectoryButton(JButton clientDirectoryButton) {
        this.clientDirectoryButton = clientDirectoryButton;
    }

    public void setClientReceiveButton(JButton clientReceiveButton) {
        this.clientReceiveButton = clientReceiveButton;
    }

    public void setClientPanel(JPanel clientPanel) {
        this.clientPanel = clientPanel;
    }

    public void setClientIpField(JTextField clientIpField) {
        this.clientIpField = clientIpField;
    }

    public void setClientPortField(JTextField clientPortField) {
        this.clientPortField = clientPortField;
    }

    public void setClientConsole(JLabel clientConsole) {
        this.clientConsole = clientConsole;
    }

    public void checkAndReceive() {
        this.hookCompleteChecker.checkComplete();
        String ipField = clientIpField.getText();
        String portField = clientPortField.getText();
        int port = Integer.parseInt(portField);
        Runnable runnable = () -> {
            try {
                clientConnectButton.setEnabled(false);
                clientReceiveButton.setEnabled(false);
                updateConsole("准备从"+ipField+":"+portField+"接收文件...");
                Socket socket = new Socket(ipField, port);
                updateConsole("已连接到"+ipField+":"+portField+",等待接收...");
                Writer writer = new OutputStreamWriter(socket.getOutputStream());
                writer.write("RECEIVE");
                writer.flush();
                BufferedInputStream inputStream = new BufferedInputStream(socket.getInputStream());
                byte[] buffer = new byte[1024];
                int len = inputStream.read(buffer, 0, 256);
                if (len != -1) {
                    String fileName = new String(buffer).trim();
                    System.out.println(fileName);
                    File file = new File(iClientDirectory +File.separator+ fileName);
                    if (!file.exists()) {
                        file.getParentFile().mkdirs();
                        file.createNewFile();
                    }
                    FileOutputStream fileOutputStream = new FileOutputStream(file);
                    int off = 256;
                    while ((len = inputStream.read(buffer)) != -1) {
                        fileOutputStream.write(buffer, 0, len);
                        off += 1024;
                    }
                    fileOutputStream.close();
                    inputStream.close();
                    writer.close();
                    socket.close();
                    updateConsole("成功接收文件：" + fileName);
                } else {
                    updateConsole("接收失败,没有获取到文件名");
                }
            } catch (IOException e) {
                e.printStackTrace();
                updateConsole("连接已断开");
            } finally {
                clientConnectButton.setEnabled(true);
                clientReceiveButton.setEnabled(true);
            }
        };
        new Thread(runnable).start();
    }

    public void checkAndTest() {
        this.hookCompleteChecker.checkComplete();
        String ipField = clientIpField.getText();
        String portField = clientPortField.getText();
        int port = Integer.parseInt(portField);
        updateConsole("测试连接到"+ipField+":"+portField+"...");
        try {
            Socket socket = new Socket(ipField, port);
            Writer writer = new OutputStreamWriter(socket.getOutputStream());
            writer.write("PING");
            writer.flush();
            Reader reader = new InputStreamReader(socket.getInputStream());
            char[] buffer = new char[32];
            int len = reader.read(buffer);
            if(len!=-1) {
                String reply = new String(buffer).trim();
                if("PONG".equals(reply)) {
                    updateConsole("测试连接成功");
                } else {
                    updateConsole("测试连接失败");
                }
            } else {
            }
            socket.close();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
            updateConsole("测试连接失败");
        }
    }

    public void changeDirectory() {
        int result = clientDirectoryChooser.showOpenDialog(clientPanel);
        if(result == JFileChooser.APPROVE_OPTION) {
            File directoryChosen = clientDirectoryChooser.getSelectedFile();
            if(directoryChosen.exists()) {
                iClientDirectory = directoryChosen.getPath();
                updateClientDirectory(iClientDirectory);
            }
        }
    }

    public void updateConsole(String text) {
        clientConsole.setText(text);
    }

    public void updateClientDirectory(String directory) {
        clientDirectory.setText(directory);
    }

}
