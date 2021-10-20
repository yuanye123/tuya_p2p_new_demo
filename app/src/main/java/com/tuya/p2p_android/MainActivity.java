package com.tuya.p2p_android;


import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.tuya.p2p.C2C_CMD_IO_CTRL_ALBUM_DOWNLOAD_START;
import com.tuya.p2p.DPEvent;
import com.tuya.p2p.Download_Album_Head;
import com.tuya.p2p.Log;
import com.tuya.p2p.P2pJniApi;
import com.tuya.p2p.UpgradeEventCallback;
import com.tuya.p2p_android.utils.FileAssembleUtil;
import com.uuzuche.lib_zxing.activity.CodeUtils;

import org.json.JSONObject;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;

import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity {

    public final static String TAG = MainActivity.class.getSimpleName();

    private String[] requiredPermissions = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE};

    private final int PERMISSION_CODE = 123;

    private ConcurrentHashMap<Integer, FileAssembleUtil> mediaParseHashMap = new ConcurrentHashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.init(this, "/sdcard/p2p", 2);

//        pid: String, uid: String, key: String, path: String,
//                version: String, netcardName: String

        if (!EasyPermissions.hasPermissions(this, requiredPermissions)) {
            EasyPermissions.requestPermissions(this, "需要授予权限以使用设备", PERMISSION_CODE, requiredPermissions);
        } else {
            initSDK();
        }

//        int dpid, byte type, Object value, int time_stamp
//        DPEvent dpEvent = new DPEvent(1, "1".getBytes()[0], new Object(), -1);
//
//        ArrayList<DPEvent> dpEvents = new ArrayList<>();
//        dpEvents.add(dpEvent);
//        P2pJniApi.INSTANCE.sendDP(dpEvents);
        findViewById(R.id.txt_reset).setOnClickListener(v -> {
            P2pJniApi.INSTANCE.unActive();
        });

        File videoDir = new File( "/sdcard/p2p/video");
        if (!videoDir.exists()) {
            videoDir.mkdirs();
        }
        File imgDir = new File( "/sdcard/p2p/video");
        if (!imgDir.exists()) {
            imgDir.mkdirs();
        }


    }

    private void initSDK() {
        P2pJniApi.INSTANCE.setUpgradeEventCallback(new UpgradeEventCallback() {
            @Override
            public void onUpgradeInfo(String s) {

            }

            @Override
            public void onUpgradeDownloadStart() {

            }

            @Override
            public void onUpgradeDownloadUpdate(int i) {

            }

            @Override
            public void upgradeFileDownloadFinished(int i, String s) {

            }
        });

        P2pJniApi.INSTANCE.setP2pCallBack(new P2pJniApi.P2pCallBack() {
            @Override
            public void recvVideData(int sessionId, P2pJniApi.RecvStatus status, byte[] buf, int len, Download_Album_Head downloadAlbumHead, C2C_CMD_IO_CTRL_ALBUM_DOWNLOAD_START downloadExtra) {
                android.util.Log.d(TAG, "channel is " + downloadExtra.filename + Thread.currentThread().getName() + "recvVideData status is " + status + "  download is packsize is " + downloadAlbumHead.packageSize + " fileName is " + downloadAlbumHead.fileName);
                FileAssembleUtil fileAssembleUtil;
                switch (status) {
                    case TY_DATA_TRANSFER_IDLE:
                        break;
                    case TY_DATA_TRANSFER_START:
                        Log.d(TAG, "recvVideData:current sessionId: $sessionId, current channel:${downloadExtra.channel} ,and current reqId is:${downloadAlbumHead.reqId}");
                        //开始接受
                        fileAssembleUtil = FileAssembleUtil.getInstance("/sdcard/p2p/video", "/sdcard/p2p/image");
                        fileAssembleUtil.handlerData(FileAssembleUtil.STATE_START, buf, downloadAlbumHead, downloadExtra.albumName);
                        mediaParseHashMap.put(sessionId,fileAssembleUtil);
                        break;
                    case TY_DATA_TRANSFER_PROCESS:
                        //接收中
                        fileAssembleUtil = mediaParseHashMap.get(sessionId);
                        if (fileAssembleUtil!=null) {
                            fileAssembleUtil.handlerData(FileAssembleUtil.STATE_PROCESS, buf, downloadAlbumHead, downloadExtra.albumName);
                        }
                        break;
                    case TY_DATA_TRANSFER_ONCE :
                        //开始即结束，只有一包数据
                        fileAssembleUtil = FileAssembleUtil.getInstance("/sdcard/p2p/video", "/sdcard/p2p/image");
                        fileAssembleUtil.handlerData(FileAssembleUtil.STATE_ONCE, buf, downloadAlbumHead, downloadExtra.albumName);
                        break;
                    case TY_DATA_TRANSFER_END:
                        //接收完成
                        fileAssembleUtil = mediaParseHashMap.get(sessionId);
                        if (fileAssembleUtil!=null) {
                            fileAssembleUtil.handlerData(FileAssembleUtil.STATE_DONE, buf, downloadAlbumHead, downloadExtra.albumName);
                        }
                        break;
                    case TY_DATA_TRANSFER_CANCEL:

                        fileAssembleUtil = mediaParseHashMap.get(sessionId);
                        if (fileAssembleUtil!=null) {
                            fileAssembleUtil.handlerData(FileAssembleUtil.STATE_CANCEL, buf, downloadAlbumHead, downloadExtra.albumName);
                        }
                        break;
                    default:
                }


            }

            @Override
            public void onDpEvent(DPEvent event) {

            }

            @Override
            public void onMqttMsg(int protocol, JSONObject msgObj) {

            }

            @Override
            public void onNetStauts(int status) {
                android.util.Log.d(TAG, "onNetStauts status is " + status);
                if (status == 7) {
                    P2pJniApi.INSTANCE.initTransP2p();
                    android.util.Log.d(TAG, "get device id is " + P2pJniApi.INSTANCE.getDeviceId());
                }
            }

            @Override
            public void onReset(int type) {
                android.util.Log.d(TAG, "onReset ++++");
//                P2pJniApi.INSTANCE.closeTransP2p();
                Intent mStartActivity = getPackageManager().getLaunchIntentForPackage(getPackageName());
                if (mStartActivity != null) {
                    int mPendingIntentId = 123456;
                    PendingIntent mPendingIntent = PendingIntent.getActivity(MainActivity.this, mPendingIntentId
                            , mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
                    AlarmManager mgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                    mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 2000, mPendingIntent);
                    Runtime.getRuntime().exit(0);
                }
            }

            @Override
            public void onShorturl(String url) {
                android.util.Log.d(TAG, "onShorturl is " + url);
                if(url != null){
                    final String localUrl = (String) com.alibaba.fastjson.JSONObject.parseObject(url).get("shortUrl");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ImageView qrcode = findViewById(R.id.qrcode);
                            qrcode.setVisibility(View.VISIBLE);
                            qrcode.setImageBitmap(CodeUtils.createImage(localUrl, 400, 400, null));
                        }
                    });
                }
            }
        });
        P2pJniApi.INSTANCE.initSdk(BuildConfig.PID, BuildConfig.UUID, BuildConfig.AUTHOR_KEY, "/sdcard/", "1.0.0", "wlan0");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PERMISSION_CODE) {
            if (!EasyPermissions.hasPermissions(this, requiredPermissions)) {
                EasyPermissions.requestPermissions(this, "需要授予权限以使用设备", PERMISSION_CODE, requiredPermissions);
            } else {
                initSDK();
            }
        }
    }
}