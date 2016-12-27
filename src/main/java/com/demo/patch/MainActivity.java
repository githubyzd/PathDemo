package com.demo.patch;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.cundong.utils.ApkUtils;
import com.cundong.utils.PatchUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {
    static {
        System.loadLibrary("ApkPatchLibrary");//加载so库
    }

    private final String NEW_APK_PATH = Environment.getExternalStorageDirectory() + File.separator + "patchdemo.apk";
    private final String PATCH_PATH = Environment.getExternalStorageDirectory() + File.separator + "patchdemo.patch";
    private final String URL = "http://192.168.126.1:8080/patchdemo.patch";
    private final int SUCCESS = 5;
    private final int FAIL = 6;
    FileOutputStream fos = null;
    HttpURLConnection conn = null;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case SUCCESS:
                    //弹出Toast,提示合并成功,安装apk
                    Toast.makeText(MainActivity.this, "合并成功", Toast.LENGTH_SHORT).show();
                    ApkUtils.installApk(MainActivity.this, NEW_APK_PATH);
                    break;
                default:
                    Toast.makeText(MainActivity.this, "合并失败", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TextView textView = (TextView) findViewById(R.id.tv);
        textView.setText("已经更新到最新版本了" + ApkUtils.getVersionName(this) + "    " + ApkUtils.getVersionCode(this));
        new Thread(new Runnable() {
            @Override
            public void run() {
                loadApk();
            }
        }).start();
        findViewById(R.id.bt).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateApk();
            }
        });
    }

    private void updateApk() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                //获取已经安装过的旧的apk的路径
                String oldApkSource = ApkUtils.getSourceApkPath(MainActivity.this, getPackageName());
                //合并旧的apk和补丁成为新的apk
                int patchResult = PatchUtils.patch(oldApkSource, NEW_APK_PATH, PATCH_PATH);
                int what;
                if (patchResult == 0) {
                    what = SUCCESS;
                } else {
                    what = FAIL;
                }
                handler.obtainMessage(what).sendToTarget();
            }
        }).start();
    }

    private void loadApk() {
        try {
            File file = new File(PATCH_PATH);
            if (file.exists()) {
                file.delete();
            }
            URL url = new URL(URL);
            conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(5000);
            if(conn.getResponseCode()==HttpURLConnection.HTTP_OK){
                InputStream is=conn.getInputStream();
                fos=new FileOutputStream(file);
                byte[] b=new byte[1024];
                int len=0;
                while((len=is.read(b))!=-1){  //先读到内存
                    fos.write(b, 0, len);
                }
                fos.flush();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this,"下载成功了",Toast.LENGTH_LONG).show();
                    }
                });
            }
        } catch (final Exception e) {
            e.printStackTrace();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this,e.toString(),Toast.LENGTH_LONG).show();
                }
            });
        }
    }
}
