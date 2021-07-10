package com.adam.swing_project.local_file_transfer;

import javax.swing.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

//todo checker
public class ServerHook {

    private JPanel serverPanel;
    private JLabel serverStartupConsole, serverTransferConsole, serverFilePath, serverFileName;
    private JButton serverStartupButton;

    public void setServerStartupButton(JButton serverStartupButton) {
        this.serverStartupButton = serverStartupButton;
    }

    public void setServerStopButton(JButton serverStopButton) {
        this.serverStopButton = serverStopButton;
    }

    private JButton serverStopButton;
    private HookCompleteChecker hookCompleteChecker;
    private boolean serverStarted;

    public void setServerSocket(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    private ServerSocket serverSocket;

    public ServerHook() {
    }

    public void setServerPanel(JPanel serverPanel) {
        this.serverPanel = serverPanel;
    }

    public void setServerStartupConsole(JLabel serverStartupConsole) {
        this.serverStartupConsole = serverStartupConsole;
    }

    public void setServerTransferConsole(JLabel serverTransferConsole) {
        this.serverTransferConsole = serverTransferConsole;
    }

    public void setServerFilePath(JLabel serverFilePath) {
        this.serverFilePath = serverFilePath;
    }

    public void setServerFileName(JLabel serverFileName) {
        this.serverFileName = serverFileName;
    }

    public void updateServerStartupConsole(String text) {
        serverStartupConsole.setText(text);
    }

    public void updateServerTransferConsole(String text) {
        serverTransferConsole.setText(text);
    }

    public void startServerSocket() {
        if(!serverStarted) {
            Runnable runnable = () -> {
                int port = 8000;
                try (ServerSocket serverSocket = new ServerSocket(port)) {
                    updateServerStartupConsole("已启动在" + port + "端口");
                    serverStarted = true;
                    serverStartupButton.setEnabled(false);
                    serverStopButton.setEnabled(true);
                    setServerSocket(serverSocket);
                    while(true) {
                        try (Socket socket = serverSocket.accept();
                             Reader reader = new InputStreamReader(socket.getInputStream());
                             BufferedOutputStream outputStream = new BufferedOutputStream(socket.getOutputStream())) {
                            char[] buffer = new char[32];
                            int len = reader.read(buffer,0,32);
                            if (len != -1) {
                                String command = new String(buffer).trim();
                                if ("PING".equals(command)) {
                                    outputStream.write("PONG".getBytes());
                                    updateServerTransferConsole("与客户端连接成功");
                                } else if("RECEIVE".equals(command)) {
                                    updateServerTransferConsole("发送文件中...");
                                    String fileName = "637262112909361979.jpg";
                                    FileInputStream fileInputStream= new FileInputStream(new File(".\\local-file-transfer\\src\\com\\adam\\swing_project\\local_file_transfer\\637262112909361979.jpg"));
                                    byte[] bytes = fileName.getBytes();
                                    outputStream.write(bytes);
                                    int leftBlank = 256-bytes.length;
                                    for(int i=0;i<leftBlank;i++) {
                                        outputStream.write(0);
                                    }
                                    byte[] byteBuffer = new byte[1024];
                                    while((len = fileInputStream.read(byteBuffer))!=-1) {
                                        outputStream.write(byteBuffer);
                                    }
                                    updateServerTransferConsole("发送成功");
                                    fileInputStream.close();
                                }
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                } finally {
                    setServerSocket(null);
                    serverStarted = false;
                    serverStartupButton.setEnabled(true);
                    serverStopButton.setEnabled(false);
                    updateServerStartupConsole("已停止");
                }
            };
            new Thread(runnable).start();
        }
    }

    public void stopServerSocket() {
        if(serverStarted) {
            System.out.println("Server主动关闭");
            try {
                serverSocket.close();
            } catch (IOException e) {
//                e.printStackTrace();
            }
        }
    }

}
