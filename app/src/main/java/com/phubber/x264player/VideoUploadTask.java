package com.phubber.x264player;

import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.TextureView;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class VideoUploadTask implements
		PreviewCallback, Callback{

	private final String TAG = "VideoUploadTask";
	private SurfaceView mSurfaceView;
	private TextureView mTextureView;
	private SurfaceHolder mHolder;
	private Camera mCamera;
	private int mWidth,mHeight;
	private int mCameraId;
	private int mFps;
	private boolean mIsStarted = false;
	private DataThread mDataThread;

	public VideoUploadTask(SurfaceView view,
			int cameraId,int width,int height,int fps) {
		mSurfaceView = view;
		mHolder = mSurfaceView.getHolder();
		mCameraId = cameraId;
		mWidth = width;
		mHeight = height;
		mFps = fps;
	}
	public VideoUploadTask(TextureView textureView,
			int cameraId,int width,int height,int fps) {
		mTextureView = textureView;
		mCameraId = cameraId;
		mWidth = width;
		mHeight = height;
		mFps = fps;
	}

	private boolean startCamera() {
		stopCamera();
		mCamera = Camera.open(mCameraId);
		Parameters params = mCamera.getParameters();
		List<Size> sizes = params.getSupportedPreviewSizes();
		for (Size size : sizes) {
			if (size.width == mWidth && size.height == mHeight) {
				params.setPreviewSize(mWidth, mHeight);
				break;
			}
		}

		mWidth = params.getPreviewSize().width;
		mHeight = params.getPreviewSize().height;

		List<Integer> formats = params.getSupportedPreviewFormats();
		for (int i = 0; i < formats.size(); i++) {
			if (formats.get(i).equals(ImageFormat.YV12)) {
				params.setPreviewFormat(ImageFormat.YV12);
				break;
			}
		}
		params.setFocusMode(Parameters.FOCUS_MODE_AUTO);
		params.setRecordingHint(true);

		try {
			mCamera.setParameters(params);
			if (mSurfaceView != null) {
				mCamera.setPreviewDisplay(mHolder);
				mHolder.addCallback(this);
			} else if (mTextureView != null) {
				mCamera.setPreviewTexture(mTextureView.getSurfaceTexture());
			}

		} catch (IOException e) {
			mCamera.release();
			mCamera = null;
			Log.e(TAG,"setParameters error",e);
			return false;
		}

		mCamera.addCallbackBuffer(new byte[mWidth * mHeight * 3 / 2]);
//		mCamera.addCallbackBuffer(new byte[mWidth * mHeight * 3 / 2]);
		mCamera.setPreviewCallbackWithBuffer(this);
		mCamera.startPreview();
		mDataThread = new DataThread();
		mDataThread.start();
		return true;
	}

	public synchronized boolean start() {
		mIsStarted = false;

		if (!startCamera()) {
			mIsStarted = false;
			return false;
		}
		MainActivity.x264_encoder_init(mWidth,mHeight);
		mIsStarted = true;
		return mIsStarted;
	}

	private void stopCamera() {
		if (mCamera != null) {
			mCamera.stopPreview();
			try {
				if (mTextureView != null)
					mCamera.setPreviewTexture(null);
				else if (mHolder != null)
					mCamera.setPreviewDisplay(null);
			} catch (Throwable e) {
				Log.e(TAG,"stopCamera failed",e);
			}
			mCamera.cancelAutoFocus();
			mCamera.release();
			mCamera = null;
		}
		MainActivity.x264_encoder_release();
	}

	public synchronized void stop() {
		try {
			DataThread tmp = mDataThread;
			mDataThread = null;
			if (tmp != null)
			{
				tmp.release();
			}
			stopCamera();

		} catch (Throwable e) {
			Log.e(TAG,"stop failed",e);
		}
		mIsStarted = false;
	}

	@Override
	public void onPreviewFrame(byte[] data, Camera camera) {
		if (mDataThread != null && mIsStarted)
			mDataThread.addData(data);
		camera.addCallbackBuffer(data);
	}

	public void release() {
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		if (mHolder != holder) {
			mHolder.removeCallback(this);
			mHolder = holder;
			mHolder.addCallback(this);
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		if (mHolder != holder) {
			mHolder.removeCallback(this);
			mHolder = holder;
			mHolder.addCallback(this);
		}
		synchronized (this) {
			startCamera();
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		mHolder.removeCallback(this);
		stop();
		release();
	}

	class DataThread extends Thread {
		private ArrayList<byte[]> mBuffers = new ArrayList<byte[]>();
		private boolean mIsRunning = true;
		FileOutputStream mFos = null;

		public DataThread() {
			mIsRunning = true;
			setName("CameraDataThread");
			try{
//				mFos = new FileOutputStream("/sdcard/welsenc/test/yuv/record_1280x720.yuv");
			}catch(Throwable e)
			{
				Log.e(TAG, "new DataThread instance error", e);
			}
			
		}

		public boolean addData(byte[] data) {
			try{
			if(mFos != null)
				mFos.write(data);
			}catch(Throwable e)
			{
				Log.e(TAG,"addData error",e);
			}
			if (mBuffers.size() < 10) {
				synchronized (mBuffers) {
					mBuffers.add(data);
					mBuffers.notify();
					return true;
				}
			}
			return false;
		}

		public void release() {
			synchronized (mBuffers) {
				mIsRunning = false;
				mBuffers.notify();
			}
			try{
			if(mFos != null)
				mFos.close();
			}catch(Throwable e)
			{
				e.printStackTrace();
			}
			System.gc();
		}
		byte[] dataOut;
		public void run() {
			FileOutputStream fos = null;
			FileOutputStream yuv_fos = null;
			try {
				fos = new FileOutputStream("/mnt/sdcard/" + mWidth + "x" + mHeight + ".264");
				yuv_fos = new FileOutputStream("/mnt/sdcard/" + mWidth + "x" + mHeight + ".yuv");
			}catch (Throwable e)
			{
				e.printStackTrace();
			}
			while (mIsRunning) {
				if (mBuffers.isEmpty()) {
					synchronized (mBuffers) {
						try {
							mBuffers.wait();
						} catch (InterruptedException e) {
							Log.e(TAG,"wait error",e);
						}
					}
				}

				if (!mBuffers.isEmpty()) {
					byte[] data = mBuffers.remove(0);
					if(dataOut == null)
						dataOut = new byte[mWidth*mHeight*3/2];
					int size = MainActivity.x264_encoder_encode(data,dataOut);
					Log.d(TAG,"encode size="+size);
					if(size > 0)
					{
						try {
							yuv_fos.write(data,0,data.length);
							fos.write(dataOut, 0, size);
						}catch(Throwable e)
						{
							e.printStackTrace();;
						}
					}
				}
			}
			if(fos != null)
			{
				try {
					yuv_fos.close();
					fos.close();
				}catch (Throwable e)
				{
					e.printStackTrace();
				}
			}
		}
	}

}
