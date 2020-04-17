package com.video.lxf.audiosocketdemo;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class AudioTrackThread implements Runnable {
    private boolean flag = true;
    private DatagramSocket mSocket;

    byte[] buffer;
    private static final int PORT = 6660;
    AudioTrack audioTrk;
    private long mStartTime, mStopTime;
    int recBufSize;
    public AudioTrackThread() throws SocketException {
        // TODO Auto-generated constructor stub
        mSocket = new DatagramSocket(PORT);
        //配置播放器
        initAudioTracker();
    }


    private void initAudioTracker() {
        //扬声器播放
        int streamType = AudioManager.STREAM_MUSIC;
        //播放的采样频率 和录制的采样频率一样
        int sampleRate = 44100;

        //和录制的一样的
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;

        //流模式
        int mode = AudioTrack.MODE_STREAM;

        //录音用输入单声道  播放用输出单声道
        int channelConfig = AudioFormat.CHANNEL_OUT_MONO;

        int recBufSize = AudioTrack.getMinBufferSize(
                sampleRate,
                channelConfig,
                audioFormat);
        System.out.println("****playRecBufSize = " + recBufSize);
        audioTrk = new AudioTrack(
                streamType,
                sampleRate,
                channelConfig,
                audioFormat,
                recBufSize,
                mode);
        audioTrk.setStereoVolume(AudioTrack.getMaxVolume(),
                AudioTrack.getMaxVolume());
        buffer = new byte[recBufSize];

    }

    @Override
    public void run() {
        if (mSocket == null)
            return;
        //从文件流读数据
        audioTrk.play();
        while (flag) {
            DatagramPacket recevPacket;
            try {
                recevPacket = new DatagramPacket(buffer, 0, buffer.length);
                mSocket.receive(recevPacket);
                audioTrk.write(recevPacket.getData(), 0, recevPacket.getLength());
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        audioTrk.stop();
    }

    public void setFlag(boolean flag) {
        this.flag = flag;
    }

    public void close() {
        flag = false;
        if (mSocket != null) {
            mSocket.close();
        }
    }
}
