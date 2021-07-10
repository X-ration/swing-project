package com.adam.swing_project.local_file_transfer;

import javax.swing.*;
import java.io.*;
import java.net.Socket;

public class ClientHook {

    private JPanel clientPanel;
    private JTextField clientIpField, clientPortField;
    private JLabel clientConsole;

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
        String ipField = clientIpField.getText();
        String portField = clientPortField.getText();
        int port = Integer.parseInt(portField);
        updateConsole("准备从"+ipField+":"+portField+"接收文件...");
        try {
            Socket socket = new Socket(ipField, port);
            Writer writer = new OutputStreamWriter(socket.getOutputStream());
            writer.write("RECEIVE");
            writer.flush();
            BufferedInputStream inputStream = new BufferedInputStream(socket.getInputStream());
            byte[] buffer = new byte[1024];
            int len = inputStream.read(buffer,0,256);
            if(len!=-1) {
                String fileName = new String(buffer).trim();
                System.out.println(fileName);
                File file = new File("C:\\LFT\\"+fileName);
                if(!file.exists()) {
                    file.getParentFile().mkdirs();
                    file.createNewFile();
                }
                FileOutputStream fileOutputStream = new FileOutputStream(new File("C:\\LFT\\"+fileName));
                int off = 256;
                while((len = inputStream.read(buffer))!=-1) {
                    fileOutputStream.write(buffer, 0, len);
                    off+=1024;
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
        }
    }

    public void checkAndTest() {
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

    public void updateConsole(String text) {
        clientConsole.setText(text);
    }

}
