package com.video.lxf.audiosocketdemo;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.AutomaticGainControl;
import android.media.audiofx.NoiseSuppressor;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.RequiresApi;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.LinkedList;
import java.util.concurrent.ArrayBlockingQueue;

public class AudioRecordThread implements Runnable {
    private static final String TAG = "AudioRecordThread";
    private boolean flag = true;
    private DatagramSocket mSocket;
    byte[] buffer;
    AudioRecord audioRec;
    private static final int PORT = 6000;
    private ArrayBlockingQueue<byte[]> queue;
    private Object mLock;
    private Handler mHandler;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public AudioRecordThread(Handler handler) throws SocketException {
        // TODO Auto-generated constructor stub
        mHandler = handler;
        mSocket = new DatagramSocket();
        mLock = new Object();
        initAudio();
    }

    protected LinkedList<byte[]> mRecordQueue;
    int minBufferSize;
    private static AcousticEchoCanceler aec;
    private static AutomaticGainControl agc;
    private static NoiseSuppressor nc;
    private long mStartTime, mStopTime;
    private double mVolume;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private void initAudio() {
        //播放的采样频率 和录制的采样频率一样
        int sampleRate = 44100;

        //和录制的一样的
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;

        //录音用输入单声道  播放用输出单声道
        int channelConfig = AudioFormat.CHANNEL_IN_MONO;


        minBufferSize = AudioRecord.getMinBufferSize(
                sampleRate,
                channelConfig, AudioFormat.ENCODING_PCM_16BIT);
        System.out.println("****RecordMinBufferSize = " + minBufferSize);
        audioRec = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                minBufferSize);
        buffer = new byte[minBufferSize];

        if (audioRec == null) {
            return;
        }
        //声学回声消除器 AcousticEchoCanceler 消除了从远程捕捉到音频信号上的信号的作用
        if (AcousticEchoCanceler.isAvailable()) {
            aec = AcousticEchoCanceler.create(audioRec.getAudioSessionId());
            if (aec != null) {
                aec.setEnabled(true);
            }
        }

        //自动增益控制 AutomaticGainControl 自动恢复正常捕获的信号输出
        if (AutomaticGainControl.isAvailable()) {
            agc = AutomaticGainControl.create(audioRec.getAudioSessionId());
            if (agc != null) {
                agc.setEnabled(true);
            }
        }

        //噪声抑制器 NoiseSuppressor 可以消除被捕获信号的背景噪音
        if (NoiseSuppressor.isAvailable()) {
            nc = NoiseSuppressor.create(audioRec.getAudioSessionId());
            if (nc != null) {
                nc.setEnabled(true);
            }
        }
        mRecordQueue = new LinkedList<byte[]>();
    }

//    private File mAudioFile;
//    private FileOutputStream fileOutputStream;

    @Override
    public void run() {
        if (mSocket == null)
            return;
        try {
            mStartTime = System.currentTimeMillis();
            audioRec.startRecording();
            while (flag) {
                try {
                    byte[] bytes_pkg = buffer.clone();
                    if (mRecordQueue.size() >= 2) {
                        int length = audioRec.read(buffer, 0, minBufferSize);
                        //获取音量大小
                        mVolume = getAudioColum(buffer);
                        System.out.println(TAG + "= " + mVolume);
                        Message message = mHandler.obtainMessage();
                        message.arg1 = (int) mVolume;
                        mHandler.sendMessage(message);

                        DatagramPacket writePacket;
                        InetAddress inet = InetAddress.getByName(inetAddressName);
                        writePacket = new DatagramPacket(buffer, length, inet, 6660);
                        writePacket.setLength(length);
                        System.out.println("AudioRTwritePacket = " + writePacket.getData().toString());

                        mSocket.send(writePacket);
                    }
                    mRecordQueue.add(bytes_pkg);
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            audioRec.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private double getAudioColum(byte[] buffer) {

        double sumVolume = 0.0;

        double avgVolume = 0.0;

        double volume = 0.0;

        for (int i = 0; i < buffer.length; i += 2) {

            int v1 = buffer[i] & 0xFF;

            int v2 = buffer[i + 1] & 0xFF;

            int temp = v1 + (v2 << 8);// 小端

            if (temp >= 0x8000) {

                temp = 0xffff - temp;

            }

            sumVolume += Math.abs(temp);

        }

        avgVolume = sumVolume / buffer.length / 2;

        volume = Math.log10(1 + avgVolume) * 10;

        return volume;
    }

    public void close() {
        flag = false;
        if (mSocket != null) {
            mSocket.close();
        }
    }

    private String inetAddressName = "";

    public void setInetAddressName(String inetAddressName) {
        this.inetAddressName = inetAddressName;
    }

    public void setFlag(boolean flag) {
        this.flag = flag;
        mStopTime = System.currentTimeMillis();

    }

    public boolean isFlag() {
        return flag;
    }

    public long getmStartTime() {
        return mStartTime;
    }

    public long getmStopTime() {
        return mStopTime;
    }
}