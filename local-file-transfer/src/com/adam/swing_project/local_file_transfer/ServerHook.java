package com.adam.swing_project.local_file_transfer;

import javax.swing.*;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerHook {

    @HookCompleteCheck(HookCompleteCheck.CHECK_CONSTANT.REQUIRED)
    private JPanel serverPanel;
    @HookCompleteCheck(HookCompleteCheck.CHECK_CONSTANT.REQUIRED)
    private JLabel serverStartupConsole, serverTransferConsole, serverFilePath, serverFileName;
    @HookCompleteCheck(HookCompleteCheck.CHECK_CONSTANT.REQUIRED)
    private JButton serverStartupButton, serverStopButton, serverTransferButton, serverChooseFileButton;
    @HookCompleteCheck(HookCompleteCheck.CHECK_CONSTANT.REQUIRED)
    private JFileChooser serverFileChooser;

    @HookCompleteCheck(HookCompleteCheck.CHECK_CONSTANT.IGNORED)
    private HookCompleteChecker hookCompleteChecker;
    private boolean serverStarted;
    private int localPort;
    private ServerSocket serverSocket;
    private File fileChosen;
    private final Object fileSendLock = new Object();

    public static void main(String[] args) {
        ServerHook serverHook = new ServerHook();
        serverHook.setServerPanel(new JPanel());
        serverHook.setServerStartupConsole(new JLabel());
        serverHook.setServerStartupButton(new JButton());
        serverHook.setServerStopButton(new JButton());
        serverHook.hookCompleteChecker.checkComplete();
    }

    public void setServerFileChooser(JFileChooser serverFileChooser) {
        this.serverFileChooser = serverFileChooser;
    }

    public void setServerChooseFileButton(JButton serverChooseFileButton) {
        this.serverChooseFileButton = serverChooseFileButton;
    }

    public void setServerTransferButton(JButton serverTransferButton) {
        this.serverTransferButton = serverTransferButton;
    }

    public void setServerStartupButton(JButton serverStartupButton) {
        this.serverStartupButton = serverStartupButton;
    }

    public void setServerStopButton(JButton serverStopButton) {
        this.serverStopButton = serverStopButton;
    }

    public void setServerSocket(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    public ServerHook() {
        this.hookCompleteChecker = new HookCompleteChecker(this);
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

    public void updateServerFilePath(String path) {
        serverFilePath.setText(path);
    }

    public void updateServerFileName(String name) {
        serverFileName.setText(name);
    }

    public void startServerSocket() {
        hookCompleteChecker.checkComplete();
        if(!serverStarted) {
            Runnable runnable = () -> {
                localPort = 8000;
                try (ServerSocket serverSocket = new ServerSocket(localPort)) {
                    updateServerStartupConsole("已启动在" + localPort + "端口");
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
                            InetAddress remoteInetAddress = socket.getInetAddress();
                            int remotePort = socket.getPort();
                            String clientAddressText = remoteInetAddress.getHostAddress() + ":" + remotePort;
                            if (len != -1) {
                                String command = new String(buffer).trim();
                                if ("PING".equals(command)) {
                                    outputStream.write("PONG".getBytes());
                                    updateServerTransferConsole("客户端"+clientAddressText+"测试连接成功");
                                } else if("RECEIVE".equals(command)) {
                                    serverChooseFileButton.setEnabled(true);
                                    updateServerTransferConsole("客户端"+clientAddressText+"已准备好接收文件...");
                                    updateServerStartupConsole("已启动在"+localPort+"端口，已连接到客户端"+clientAddressText);
                                    File localFile;
                                    while(true) {
                                        synchronized (fileSendLock) {
                                            fileSendLock.wait();
                                        }
                                        if(fileChosen.exists()) {
                                            localFile = fileChosen;
                                            break;
                                        }
                                        updateServerTransferConsole("文件无效，请重新选择文件");
                                    }
                                    updateServerTransferConsole("发送文件中...");
                                    String fileName = localFile.getName();
                                    FileInputStream fileInputStream = new FileInputStream(localFile);

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
                                    updateServerTransferConsole("文件成功发送到"+clientAddressText);
                                    updateServerStartupConsole("已启动在"+localPort+"端口");
                                    fileInputStream.close();
                                    serverTransferButton.setEnabled(false);
                                    serverChooseFileButton.setEnabled(false);
                                }
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
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
                    serverTransferButton.setEnabled(false);
                    serverChooseFileButton.setEnabled(false);
                    updateServerStartupConsole("已停止");
                    updateServerTransferConsole("与客户端断开连接");
                }
            };
            new Thread(runnable).start();
        }
    }

    public void stopServerSocket() {
        hookCompleteChecker.checkComplete();
        if(serverStarted) {
            System.out.println("Server主动关闭");
            try {
                serverSocket.close();
            } catch (IOException e) {
//                e.printStackTrace();
            }
        }
    }

    public void chooseFile() {
        int result = serverFileChooser.showOpenDialog(serverPanel);
        if(result == JFileChooser.APPROVE_OPTION) {
            fileChosen = serverFileChooser.getSelectedFile();
            if(fileChosen.exists()) {
                String fileName = fileChosen.getName();
                String filePath = fileChosen.getParent();
                updateServerFileName(fileName);
                updateServerFilePath(filePath);
                serverTransferButton.setEnabled(true);
            }
        }
    }

    public void sendFile() {
        synchronized (fileSendLock) {
            fileSendLock.notify();
        }
    }

}
