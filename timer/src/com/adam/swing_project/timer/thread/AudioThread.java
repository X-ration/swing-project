package com.adam.swing_project.timer.thread;

import com.adam.swing_project.library.assertion.Assert;
import com.adam.swing_project.library.assertion.AssertException;
import com.adam.swing_project.timer.component.FileManager;
import com.adam.swing_project.library.logger.Logger;

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;

/**
 * 控制音频播放的主类
 * main方法是一个简单的播放器软件实现（支持wav格式）
 */
public class AudioThread extends Thread {

    private final Object lock = new Object()
            , playLock = new Object();
    private AudioControllerStatus status = AudioControllerStatus.STOPPED;
    private final Logger logger = Logger.createLogger(this);

    private File soundFile;
    private AudioInputStream audioInputStream;
    private SourceDataLine sourceDataLine;
    private byte[] buffer = new byte[1024];

    private java.util.List<AudioControllerListener> listenerList = new ArrayList<>();

    private enum AudioControllerStatus {
        STOPPED,
        PLAYING,
        PAUSED,
        TERMINATING
    }

    public interface AudioControllerListener {
        void playStopped();
        void playPaused();
    }

    AudioThread() {
    }

    /**
     * 线程run方法
     */
    @Override
    public void run() {
        logger.logInfo("AudioController started.");
        try {
            while (status != AudioControllerStatus.TERMINATING) {
                synchronized (lock) {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                if (status == AudioControllerStatus.TERMINATING)
                    break;
                while (status == AudioControllerStatus.PLAYING) {
                    playOnce();
                }
                synchronized (playLock) {
                    if (status == AudioControllerStatus.STOPPED) {
                        playStopped();
                    } else if (status == AudioControllerStatus.PAUSED) {
                        playPaused();
                    }
                    playLock.notify();
                }
            }
        } catch (Exception e) {
            logger.logException(e);
        }
        logger.logInfo("AudioController terminated.");
    }

    private void wakeup() {
        synchronized (lock) {
            lock.notify();
        }
    }

    public File getSoundFile() {
        return soundFile;
    }

    /**
     * 指定音频文件并播放，外部调用
     * 该方法要求从外部加载文件，不推荐使用
     * @param soundFile
     */
    @Deprecated
    public void chooseSoundFile(File soundFile) {
        Assert.isTrue(soundFile.isFile() && soundFile.exists(), "音频文件不存在！");
        this.soundFile = soundFile;
        startPlay();
    }

    /**
     * 指定音频文件资源路径，并播放，外部调用
     * @param resourcePath
     */
    public void chooseSoundFile(String resourcePath) {
        Assert.notNull(resourcePath);
        File soundFile = FileManager.getInstance().readFileForResourcePath(resourcePath);
        if(soundFile != null && this.soundFile != soundFile) {
            chooseSoundFile(soundFile);
        }
    }



    private void beforePlay() {
        try {
            audioInputStream = AudioSystem.getAudioInputStream(soundFile);
            AudioFormat format = audioInputStream.getFormat();
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            sourceDataLine = (SourceDataLine) AudioSystem.getLine(info);
            sourceDataLine.open();
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            e.printStackTrace();
            System.err.println("播放音频文件失败" + soundFile.getPath());
            this.soundFile = null;
            audioInputStream = null;
            sourceDataLine = null;
        }
    }

    /**
     * 开始播放，外部调用
     */
    public void startPlay() {
        if(status == AudioControllerStatus.PAUSED || status == AudioControllerStatus.STOPPED) {
            if(status == AudioControllerStatus.STOPPED && soundFile != null) {
                beforePlay();
            }
            sourceDataLine.start();
            status = AudioControllerStatus.PLAYING;
            wakeup();
        }
    }

    private void playOnce() {
        Assert.isTrue(status == AudioControllerStatus.PLAYING && audioInputStream!=null && sourceDataLine != null);
        try {
            int n = audioInputStream.read(buffer, 0, buffer.length);
            if(n >= 0) {
                sourceDataLine.write(buffer, 0, n);
            } else {
                status = AudioControllerStatus.STOPPED;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 暂停播放，外部调用
     */
    public void pausePlay() {
        if(status == AudioControllerStatus.PLAYING) {
            synchronized (playLock) {
                status = AudioControllerStatus.PAUSED;
                try {
                    playLock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 停止播放，外部调用
     */
    public void stopPlay() {
        if(status == AudioControllerStatus.PLAYING || status == AudioControllerStatus.PAUSED) {
            synchronized (playLock) {
                status = AudioControllerStatus.STOPPED;
                try {
                    playLock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 终止控制器线程,外部调用
     */
    public void terminate() {
        stopPlay();
        status = AudioControllerStatus.TERMINATING;
        wakeup();
    }

    public void registerListener(AudioControllerListener listener) {
        listenerList.add(listener);
    }

    private void playPaused() {
        sourceDataLine.stop();
        for(AudioControllerListener listener: listenerList) {
            listener.playPaused();
        }
    }

    private void playStopped() {
        sourceDataLine.stop();
        sourceDataLine.drain();
        sourceDataLine.close();
        sourceDataLine = null;
        audioInputStream = null;
        for(AudioControllerListener listener: listenerList) {
            listener.playStopped();
        }
    }

    public static void main(String[] args) {
        JFrame jFrame = new JFrame("播放器测试");
        Container contentPane = jFrame.getContentPane();
        contentPane.setLayout(new GridLayout(3,1));

        AudioThread audioThread = ThreadManager.getInstance().getAudioThread();
        audioThread.start();

        JRadioButton tone1 = new JRadioButton("Listen.wav")
                , tone2 = new JRadioButton("Tone.wav")
                , tone3 = new JRadioButton("Sony.wav")
                , tone4 = new JRadioButton("Invalid file")
                , tone5 = new JRadioButton("MP3Sony Ericsson.mp3");
        tone1.setSelected(true);
        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(tone1);
        buttonGroup.add(tone2);
        buttonGroup.add(tone3);
        buttonGroup.add(tone4);
        buttonGroup.add(tone5);
        JButton play = new JButton("Play"),
                pause = new JButton("Pause"),
                stop = new JButton("Stop"),
                choose = new JButton("Choose");
        JPanel upPanel = new JPanel(),
                downPanel = new JPanel();
        upPanel.setLayout(new GridLayout(3,1));
        downPanel.setLayout(new GridLayout(1,3));
        pause.setEnabled(false);
        stop.setEnabled(false);
        choose.addActionListener(e -> {
            try {
                Enumeration<AbstractButton> enumeration = buttonGroup.getElements();
                AbstractButton selected = null;
                while(enumeration.hasMoreElements()) {
                    selected = enumeration.nextElement();
                    if(buttonGroup.isSelected(selected.getModel())) {
                        break;
                    }
                }
                audioThread.stopPlay();
                URL fileURL = AudioThread.class.getResource("/" + selected.getText());
                Assert.notNull(fileURL, AssertException.class, "文件不存在" + selected.getText());
                File soundFile = new File(fileURL.toURI());
                audioThread.chooseSoundFile(soundFile);
                play.setEnabled(false);
                pause.setEnabled(true);
                stop.setEnabled(true);
            } catch (URISyntaxException e1) {
                e1.printStackTrace();
            }
        });
        play.addActionListener(e -> {
            audioThread.startPlay();
            play.setEnabled(false);
            pause.setEnabled(true);
            stop.setEnabled(true);
        });
        pause.addActionListener(e -> {
            audioThread.pausePlay();
            pause.setEnabled(false);
            play.setEnabled(true);
        });
        stop.addActionListener(e -> {
            stop.setEnabled(false);
            play.setEnabled(true);
            pause.setEnabled(false);
            audioThread.stopPlay();
        });

        audioThread.registerListener(new AudioControllerListener() {
            @Override
            public void playStopped() {
                play.setEnabled(true);
                pause.setEnabled(false);
                stop.setEnabled(false);
            }

            @Override
            public void playPaused() {
                play.setEnabled(true);
                pause.setEnabled(false);
                stop.setEnabled(true);
            }

        });

        upPanel.add(tone1);
        upPanel.add(tone2);
        upPanel.add(tone3);
        upPanel.add(tone4);
        upPanel.add(tone5);
        downPanel.add(play);
        downPanel.add(pause);
        downPanel.add(stop);
        contentPane.add(upPanel);
        contentPane.add(choose);
        contentPane.add(downPanel);

        jFrame.setSize(300,200);
        jFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        jFrame.setVisible(true);
        jFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                audioThread.terminate();
            }
        });
    }
}
