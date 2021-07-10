package com.adam.swing_project.local_file_transfer;

import javax.swing.*;
import java.awt.*;

/**
 * 局域网文件传输
 */
public class LocalFileTransfer {

    private ServerHook serverHook;
    private ClientHook clientHook;

    public static void main(String[] args) {
        new LocalFileTransfer();
    }

    public LocalFileTransfer() {
        JFrame jFrame = new JFrame("局域网文件传输");
        Container contentPane = jFrame.getContentPane();
        CardLayout contentPaneLayout = new CardLayout();
        contentPane.setLayout(contentPaneLayout);

        GridBagLayout entranceLayout = new GridBagLayout();
        JPanel entrancePanel = new JPanel(entranceLayout);
        JButton entranceButton1 = new JButton("我是客户端")
                , entranceButton2 = new JButton("我是服务端");
        GridBagConstraints entranceButton1Constraint = new GridBagConstraints(0,0,1,1,1,1,GridBagConstraints.CENTER,GridBagConstraints.NONE,new Insets(10,10,10,10),50,50)
                ,entranceButton2Constraint = new GridBagConstraints(1,0,1,1,1,1,GridBagConstraints.CENTER,GridBagConstraints.NONE,new Insets(10,10,10,10),50,50);
        entranceLayout.setConstraints(entranceButton1,entranceButton1Constraint);
        entranceLayout.setConstraints(entranceButton2,entranceButton2Constraint);
        entrancePanel.add(entranceButton1);
        entrancePanel.add(entranceButton2);

        JPanel clientPanel = new JPanel();
        entranceButton1.addActionListener(e -> {
            contentPaneLayout.show(contentPane, "client");
        });
        Box clientBox = Box.createVerticalBox();
        clientPanel.add(clientBox);
        Box clientInputBox = Box.createHorizontalBox();
        clientBox.add(clientInputBox);
        clientInputBox.add(new JLabel("ip:"));
        JTextField clientIpField = new JTextField("127.0.0.1");
        clientIpField.setPreferredSize(new Dimension(120, 20));
        clientInputBox.add(clientIpField);
        clientInputBox.add(Box.createHorizontalStrut(20));
        clientInputBox.add(new JLabel("port:"));
        JTextField clientPortField = new JTextField("8000");
        clientPortField.setPreferredSize(new Dimension(50, 20));
        clientInputBox.add(clientPortField);
        JButton clientConnectButton = new JButton("测试连接")
                , clientReceiveButton = new JButton("接收文件");
        clientBox.add(Box.createVerticalStrut(10));
        JLabel clientConsole = new JLabel("Console", JLabel.CENTER);
        Box clientConsoleBox = Box.createHorizontalBox();
        clientConsoleBox.add(clientConsole);
        clientBox.add(clientConsoleBox);
        clientBox.add(Box.createVerticalStrut(10));
        Box clientButtonBox = Box.createHorizontalBox();
        clientBox.add(clientButtonBox);
        clientButtonBox.add(clientConnectButton);
        clientButtonBox.add(clientReceiveButton);
        clientConnectButton.addActionListener(e -> clientHook.checkAndTest());
        clientReceiveButton.addActionListener(e -> clientHook.checkAndReceive());

        JPanel serverPanel = new JPanel();
        entranceButton2.addActionListener(e -> {
            contentPaneLayout.show(contentPane, "server");
        });
        Box serverBox = Box.createVerticalBox();
        serverPanel.add(serverBox);
        JLabel serverStartupConsole = new JLabel("未启动");
        Box serverStartupConsoleBox = Box.createHorizontalBox();
        serverStartupConsoleBox.add(serverStartupConsole);
        serverBox.add(serverStartupConsoleBox);
        JButton serverStartupButton = new JButton("启动")
                ,serverStopButton = new JButton("停止");
        serverStopButton.setEnabled(false);
        Box serverStartupBox = Box.createHorizontalBox();
        serverBox.add(serverStartupBox);
        serverStartupBox.add(serverStartupButton);
        serverStartupBox.add(serverStopButton);
        serverStartupButton.addActionListener(e -> {
            serverHook.startServerSocket();
        });
        serverStopButton.addActionListener(e -> serverHook.stopServerSocket());
        serverBox.add(Box.createVerticalStrut(20));
        Box serverTransferConsoleBox = Box.createHorizontalBox();
        serverBox.add(serverTransferConsoleBox);
        JLabel serverTransferConsole = new JLabel("等待连接");
        serverTransferConsoleBox.add(serverTransferConsole);
        Box serverFileConsoleBox = Box.createHorizontalBox();
        serverBox.add(serverFileConsoleBox);
        JLabel serverFilePathLabel = new JLabel("路径：")
                , serverFilePath = new JLabel("")
                , serverFileNameLabel = new JLabel("文件名：")
                , serverFileName = new JLabel("");
        serverFileConsoleBox.add(serverFilePathLabel);
        serverFileConsoleBox.add(serverFilePath);
        serverFileConsoleBox.add(Box.createHorizontalStrut(20));
        serverFileConsoleBox.add(serverFileNameLabel);
        serverFileConsoleBox.add(serverFileName);
        Box serverTransferButtonBox = Box.createHorizontalBox();
        serverBox.add(serverTransferButtonBox);
        JButton serverChooseFileButton = new JButton("选择本地文件")
                , serverTransferButton = new JButton("发送");
        serverTransferButton.setEnabled(false);
        serverTransferButtonBox.add(serverChooseFileButton);
        serverTransferButtonBox.add(serverTransferButton);

        contentPane.add("entrance", entrancePanel);
        contentPane.add("client", clientPanel);
        contentPane.add("server", serverPanel);
        contentPaneLayout.show(contentPane, "entrance");

//        jFrame.setSize(300, 200);
        jFrame.pack();
        jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jFrame.setVisible(true);

        serverHook = new ServerHook();
        serverHook.setServerStartupConsole(serverStartupConsole);
        serverHook.setServerTransferConsole(serverTransferConsole);
        serverHook.setServerPanel(serverPanel);
        serverHook.setServerFilePath(serverFilePath);
        serverHook.setServerFileName(serverFileName);
        serverHook.setServerStartupButton(serverStartupButton);
        serverHook.setServerStopButton(serverStopButton);
        clientHook = new ClientHook();
        clientHook.setClientPanel(clientPanel);
        clientHook.setClientIpField(clientIpField);
        clientHook.setClientPortField(clientPortField);
        clientHook.setClientConsole(clientConsole);
    }

}
