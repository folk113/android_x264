package com.phubber.x264player;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    @Override
    public void onClick(View v) {
        switch(v.getId())
        {
            case R.id.btn_start:
                if(mVideoUploadTask ==  null)
                {
                    mVideoUploadTask = new VideoUploadTask(mSurfaceView,0,640,480,30);
                    mVideoUploadTask.start();
                }

                break;
            case R.id.btn_stop:
                if(mVideoUploadTask != null)
                {
                    mVideoUploadTask.stop();
                    mVideoUploadTask.release();
                    mVideoUploadTask = null;
                }
                break;
            default:break;
        }
    }

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("x264-encoder");
    }
    private SurfaceView mSurfaceView;
    private VideoUploadTask mVideoUploadTask;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Example of a call to a native method
        TextView tv = findViewById(R.id.sample_text);
        findViewById(R.id.btn_start).setOnClickListener(this);
        findViewById(R.id.btn_stop).setOnClickListener(this);
        mSurfaceView = findViewById(R.id.surfaceView);
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public static native int x264_encoder_init(int width,int height);
    public static native int x264_encoder_encode(byte[] dataIn,byte[] dataOut);
    public static native void x264_encoder_release();
}
