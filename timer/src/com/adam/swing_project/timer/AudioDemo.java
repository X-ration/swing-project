package com.adam.swing_project.timer;

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;

public class AudioDemo {
    private static Object startLock = new Object();
    private static JButton button = new JButton("Play");
    private static class PlayThread extends Thread{

        private AudioInputStream audioInputStream = null;
        private SourceDataLine dataLine = null;
        private boolean isFinished = false;
        private boolean interrupted = false;

        private void beforePlay() {
            try {
                audioInputStream = AudioSystem.getAudioInputStream(AudioDemo.class.getResource("/WAVSony Ericsson.wav"));
                AudioFormat format = audioInputStream.getFormat();
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
                dataLine = (SourceDataLine) AudioSystem.getLine(info);
                dataLine.open();
                dataLine.start();
            } catch (UnsupportedAudioFileException e) {
                e.printStackTrace();
            } catch (LineUnavailableException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void afterPlay() {
            dataLine.drain();
            dataLine.close();
        }

        private void play() {
            try {
                byte[] buffer = new byte[1024];
                int n = audioInputStream.read(buffer, 0, buffer.length);
                if (n >= 0) {
                    dataLine.write(buffer, 0, n);
                } else {
                    isFinished = true;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        private void pause() {
            dataLine.stop();
        }

        @Override
        public void interrupt() {
            super.interrupt();
            this.interrupted = true;
        }

        @Override
        public void run() {

            while(!interrupted) {
                synchronized (startLock) {
                    try {
                        startLock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        break;
                    }
                }
                beforePlay();
                while (!isFinished) {
                    if(interrupted) {
                        System.out.println("interrupted");
                        break;
                    }
                    play();
                }
                afterPlay();
                button.setEnabled(true);
                isFinished = false;
            }
            System.out.println("PlayThread terminated");
        }
    }
    public static void main(String[] args) {
        JFrame jFrame = new JFrame("wav播放器");
        Container contentPane = jFrame.getContentPane();

        Thread playThread = new PlayThread();
        playThread.start();
        button.addActionListener(e -> {
            button.setEnabled(false);
            synchronized (startLock) {
                startLock.notify();
            }
        });

        JButton interrupt = new JButton("Interrupt");
        interrupt.addActionListener(e -> playThread.interrupt());

        contentPane.add(button, BorderLayout.CENTER);
        contentPane.add(interrupt, BorderLayout.SOUTH);
        jFrame.setSize(300,100);
        jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jFrame.setVisible(true);
        jFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    playThread.interrupt();
                    System.out.println("interrupting");
                    playThread.join();
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        });
    }
}
