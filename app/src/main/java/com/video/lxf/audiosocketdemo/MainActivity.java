package com.video.lxf.audiosocketdemo;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    @Bind(R.id.tv_receive_ip)
    EditText tvReceiveIp;
    @Bind(R.id.btn_receive)
    Button btnReceive;
    @Bind(R.id.llTop)
    LinearLayout llTop;
    @Bind(R.id.ivSpeak)
    TextView ivSpeak;
    @Bind(R.id.relativeLayout)
    RelativeLayout relativeLayout;
    @Bind(R.id.iv1)
    ImageView iv1;
    @Bind(R.id.iv2)
    ImageView iv2;
    @Bind(R.id.iv3)
    ImageView iv3;
    @Bind(R.id.iv4)
    ImageView iv4;
    @Bind(R.id.iv5)
    ImageView iv5;

    List<ImageView> imageViewList;
    private Random random;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        requestpermission();
    }

    private final int REQUEST_AUDIO = 666;

    private void requestpermission() {
        //SD卡读写权限
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            //权限已授权，功能操作
            init();

        } else {
            //未授权，提起权限申请
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                Toast.makeText(this, "没有权限", Toast.LENGTH_SHORT).show();
                init();

            } else {
                //申请权限
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.RECORD_AUDIO
                }, REQUEST_AUDIO);
            }

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        //判断请求码，确定当前申请的权限
        if (requestCode == REQUEST_AUDIO) {
            //判断权限是否申请通过
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //授权成功
                init();
            } else {
                //授权失败
                Toast.makeText(this, "没有权限", Toast.LENGTH_SHORT).show();
                init();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void init() {
        imageViewList = new ArrayList<>();
        imageViewList.add(iv1);
        imageViewList.add(iv2);
        imageViewList.add(iv3);
        imageViewList.add(iv4);
        imageViewList.add(iv5);

        random = new Random(System.currentTimeMillis());

        ivSpeak.setOnTouchListener(new View.OnTouchListener() {
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        //按下按钮开始录制
                        ivSpeak.setText("正在说话");
                        //显示录音提示
                        relativeLayout.setVisibility(View.VISIBLE);
                        try {
                            if (audioRecordThread == null) {
                                audioRecordThread = new AudioRecordThread(handler);
                            }
                            audioRecordThread.setInetAddressName(tvReceiveIp.getText().toString());
                            audioRecordThread.setFlag(true);
                            new Thread(audioRecordThread).start();
                        } catch (SocketException e) {
                            e.printStackTrace();
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        //松开按钮结束录制
                        ivSpeak.setText("按住说话");
                        relativeLayout.setVisibility(View.GONE);
                        audioRecordThread.setFlag(false);
                        break;
                }
                return true;
            }

        });
    }

    int volume;
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            volume = msg.arg1;
            getRecordVolume(volume);
        }
    };

    private static final int MAXAMPLITUDE = 40;
    private static final int MAXLEVEL = 5;

    public void getRecordVolume(int volume) {
        final int level = volume / (MAXAMPLITUDE / MAXLEVEL);
        //把音量规划到五个等级
        //把等级显示到UI上面
        refreshVolume(level);
    }

    private void refreshVolume(int level) {
        for (int i = 0; i < 5; i++) {
            imageViewList.get(i).setVisibility(i < level ? View.VISIBLE : View.GONE);
        }
    }


    AudioTrackThread audioTrackThread = null;
    AudioRecordThread audioRecordThread = null;

    @OnClick({R.id.btn_receive})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.btn_receive:
                if (btnReceive.getText().toString().equals("开始接收")) {
                    btnReceive.setText("停止接收");
                    try {
                        if (audioTrackThread == null) {
                            audioTrackThread = new AudioTrackThread();
                        }
                        new Thread(audioTrackThread).start();

                    } catch (SocketException e) {
                        e.printStackTrace();
                    }
                } else {
                    btnReceive.setText("开始接收");
                    audioTrackThread.setFlag(false);
                }
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        audioRecordThread.close();
        audioTrackThread.close();
    }
}
