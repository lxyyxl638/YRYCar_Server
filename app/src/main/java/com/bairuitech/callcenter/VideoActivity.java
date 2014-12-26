package com.bairuitech.callcenter;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.media.Image;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bairuitech.anychat.*;
import com.bairuitech.bluetooth.AcceptThread;
import com.bairuitech.bluetooth.ConnectThread;
import com.bairuitech.bluetooth.ConnectedThread;
import com.bairuitech.bussinesscenter.BussinessCenter;

import com.bairuitech.util.*;
import com.bairuitech.callcenter.R;
import com.facepp.error.FaceppParseException;
import com.facepp.http.HttpRequests;
import com.facepp.http.PostParameters;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

public class VideoActivity extends Activity implements AnyChatBaseEvent,
		OnClickListener, OnTouchListener, AnyChatVideoCallEvent, AnyChatUserInfoEvent {
	private SurfaceView mSurfaceSelf;
	private SurfaceView mSurfaceRemote;
	private ProgressBar mProgressSelf;
	private ProgressBar mProgressRemote;
	private ImageView mImgSwitch;
	private TextView mTxtTime;
	private Button mBtnEndSession;
	private Dialog dialog;



	private AnyChatCoreSDK anychat;
	private Handler mHandler;
	private Timer mTimerCheckAv;
	private Timer mTimerShowVideoTime;
	private TimerTask mTimerTask;
	private ConfigEntity configEntity;

	boolean bSelfVideoOpened = false;
	boolean bOtherVideoOpened = false;
	boolean bVideoViewLoaded = false;
	public static final int MSG_CHECKAV = 1;
	public static final int MSG_TIMEUPDATE = 2;
	public static final int PROGRESSBAR_HEIGHT = 5;

	int dwTargetUserId;
	int videoIndex = 0;
	int videocallSeconds = 0;

    private boolean isRunning = false;
    private ServerServer server;

    //蓝牙部分
    int REQUEST_ENABLE_BT = 1;
    ListView list;
    Button scanBtn;
    BluetoothAdapter mBluetoothAdapter;
    ArrayAdapter<String> mArrayAdapter;
    ArrayList<BluetoothDevice> bluetoothDevices = new ArrayList<BluetoothDevice>();

    /*线程成员*/
    AcceptThread serverThread = null;
    ConnectThread clientThread = null;
    ConnectedThread connectedThread = null;

    /*控件*/
    TextView bluetoothStateTextView;
    TextView serverStateTextView;
    TextView receOrderTextView;
    TextView sendOrderTextView;
    ImageView photoImageView;
    Bitmap img;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		initSdk();
		dwTargetUserId = BussinessCenter.sessionItem
				.getPeerUserItem(BussinessCenter.selfUserId);
		initView();

        /*启动服务器线程*/
        if (!isRunning) {
            server = new ServerServer();
            new Thread(server).start();
            server.setMyActivity(VideoActivity.this);
        }

		anychat.EnterRoom(BussinessCenter.sessionItem.roomId, "");
		mHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				super.handleMessage(msg);
				switch (msg.what) {
				case MSG_CHECKAV:
					CheckVideoStatus();
					updateVolume();
					break;
				case MSG_TIMEUPDATE:
					mTxtTime.setText(BaseMethod
							.getTimeShowString(videocallSeconds++));
					break;
				}

			}
		};
		initTimerCheckAv();
		initTimerShowTime();

        //启动蓝牙
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            System.out.println("没有蓝牙设备");
        }

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        list = (ListView) this.findViewById(R.id.list);
        mArrayAdapter = new ArrayAdapter<String>(this,R.layout.item);
        scanBtn = (Button) this.findViewById(R.id.scanBtn);
        scanBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pairDevices task = new pairDevices();
                task.execute();
            }
        });

        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                BluetoothDevice device = bluetoothDevices.get(position);
                System.out.println(device.getName()+" "+device.getAddress());
                clientThread = new ConnectThread(device,mBluetoothAdapter,VideoActivity.this);
                clientThread.start();
            }
        });

        /*打开服务器端线程*/
        serverThread = new AcceptThread(mBluetoothAdapter,VideoActivity.this);
        serverThread.start();
    }

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
	}

	@Override
	protected void onRestart() {
		// TODO Auto-generated method stub
		super.onRestart();
		// 如果是采用Java视频显示，则需要绑定用户
		if (AnyChatCoreSDK
				.GetSDKOptionInt(AnyChatDefine.BRAC_SO_VIDEOSHOW_DRIVERCTRL) == AnyChatDefine.VIDEOSHOW_DRIVER_JAVA) {
			videoIndex = anychat.mVideoHelper.bindVideo(mSurfaceRemote
					.getHolder());
			anychat.mVideoHelper.SetVideoUser(videoIndex, dwTargetUserId);
		}

	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		BussinessCenter.mContext = this;
	}

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
	

	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		anychat.UserCameraControl(-1, 0);
		anychat.UserSpeakControl(-1, 0);
		anychat.UserSpeakControl(dwTargetUserId, 0);
		anychat.UserCameraControl(dwTargetUserId, 0);
		mTimerCheckAv.cancel();
		mTimerShowVideoTime.cancel();
		if (dialog != null && dialog.isShowing())
			dialog.dismiss();
		anychat.LeaveRoom(-1);
		

	}

	private void initSdk() {
		if (anychat == null)
			anychat = new AnyChatCoreSDK();
		anychat.SetBaseEvent(this);
		anychat.SetVideoCallEvent(this);
		anychat.SetUserInfoEvent(this);
		anychat.mSensorHelper.InitSensor(this);
		// 初始化Camera上下文句柄
		AnyChatCoreSDK.mCameraHelper.SetContext(this);
        AnyChatCoreSDK.mCameraHelper.setActivity(VideoActivity.this);
	}

	private void initTimerShowTime() {
		if (mTimerShowVideoTime == null)
			mTimerShowVideoTime = new Timer();
		mTimerTask = new TimerTask() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				mHandler.sendEmptyMessage(MSG_TIMEUPDATE);
			}
		};
		mTimerShowVideoTime.schedule(mTimerTask, 100, 1000);
	}

	private void initTimerCheckAv() {
		if (mTimerCheckAv == null)
			mTimerCheckAv = new Timer();
		mTimerTask = new TimerTask() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				mHandler.sendEmptyMessage(MSG_CHECKAV);
			}
		};
		mTimerCheckAv.schedule(mTimerTask, 1000, 100);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			dialog = DialogFactory.getDialog(DialogFactory.DIALOGID_ENDCALL,
					dwTargetUserId, this);
			dialog.show();
		}

		return super.onKeyDown(keyCode, event);
	}

	private void initView() {
		this.setContentView(R.layout.video_activity);
        /*设置控件*/
        bluetoothStateTextView = (TextView) findViewById(R.id.bluetoothStateTextView);
        serverStateTextView = (TextView) findViewById(R.id.serverStateTextView);
        receOrderTextView = (TextView) findViewById(R.id.receOrderTextView);
        sendOrderTextView = (TextView) findViewById(R.id.sendOrderTextView);

        /*视频部分*/
		mSurfaceSelf = (SurfaceView) findViewById(R.id.surface_local);
		mSurfaceRemote = (SurfaceView) findViewById(R.id.surface_remote);
		mProgressSelf = (ProgressBar) findViewById(R.id.progress_local);
		mProgressRemote = (ProgressBar) findViewById(R.id.progress_remote);
		mImgSwitch = (ImageView) findViewById(R.id.img_switch);
		mTxtTime = (TextView) findViewById(R.id.txt_time);
		mBtnEndSession = (Button) findViewById(R.id.btn_endsession);
		mBtnEndSession.setOnClickListener(this);
		mImgSwitch.setOnClickListener(this);
		mSurfaceRemote.setTag(dwTargetUserId);
		configEntity = ConfigService.LoadConfig(this);
		if (configEntity.videoOverlay != 0) {
			mSurfaceSelf.getHolder().setType(
					SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		}
		mSurfaceSelf.setZOrderOnTop(true);
		// 如果是采用Java视频采集，则设置Surface的CallBack
		if (AnyChatCoreSDK.GetSDKOptionInt(AnyChatDefine.BRAC_SO_LOCALVIDEO_CAPDRIVER) == AnyChatDefine.VIDEOCAP_DRIVER_JAVA) {
			mSurfaceSelf.getHolder().addCallback(AnyChatCoreSDK.mCameraHelper);
			Log.i("ANYCHAT", "VIDEOCAPTRUE---" + "JAVA");
		}

		// 如果是采用Java视频显示，则设置Surface的CallBack
		if (AnyChatCoreSDK.GetSDKOptionInt(AnyChatDefine.BRAC_SO_VIDEOSHOW_DRIVERCTRL) == AnyChatDefine.VIDEOSHOW_DRIVER_JAVA) {
			videoIndex = anychat.mVideoHelper.bindVideo(mSurfaceRemote.getHolder());
			anychat.mVideoHelper.SetVideoUser(videoIndex, dwTargetUserId);
			Log.i("ANYCHAT", "VIDEOSHOW---" + "JAVA");
		}

		final View layoutLocal = (View) findViewById(R.id.frame_local_area);
		// 得到xml布局中视频区域的大小
		layoutLocal.getViewTreeObserver().addOnGlobalLayoutListener(
				new OnGlobalLayoutListener() {

					@Override
					public void onGlobalLayout() {
						// TODO Auto-generated method stub
						if (!bVideoViewLoaded) {
							bVideoViewLoaded = true;
						}
					}
				});
		// 判断是否显示本地摄像头切换图标
		if (AnyChatCoreSDK
				.GetSDKOptionInt(AnyChatDefine.BRAC_SO_LOCALVIDEO_CAPDRIVER) == AnyChatDefine.VIDEOCAP_DRIVER_JAVA) {
			if (AnyChatCoreSDK.mCameraHelper.GetCameraNumber() > 1) {
				mImgSwitch.setVisibility(View.VISIBLE);
				// 默认打开前置摄像头
				AnyChatCoreSDK.mCameraHelper.SelectVideoCapture(AnyChatCoreSDK.mCameraHelper.CAMERA_FACING_FRONT);
			}
		} else {
			String[] strVideoCaptures = anychat.EnumVideoCapture();
			if (strVideoCaptures != null && strVideoCaptures.length > 1) {
				mImgSwitch.setVisibility(View.VISIBLE);
				// 默认打开前置摄像头
				for (int i = 0; i < strVideoCaptures.length; i++) {
					String strDevices = strVideoCaptures[i];
					if (strDevices.indexOf("Front") >= 0) {
						anychat.SelectVideoCapture(strDevices);
						break;
					}
				}
			}
		}
		// 根据屏幕方向改变本地surfaceview的宽高比
		if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
			adjustLocalVideo(true);
		} else if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
			adjustLocalVideo(false);
		}

	}

	/***
	 * 调整本地视频区域宽度为界面宽度大小的1/4。竖屏时，本地预览的surfaceview的宽高比例为分辨率高宽比例;横屏时，
	 * 本地预览的surfaceview的宽高比例为分辨率宽高比例
	 * 
	 *
	 * 
	 */
	public void adjustLocalVideo(boolean bLandScape) {
		float width;
		float height = 0;
		DisplayMetrics dMetrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dMetrics);
		width = (float) dMetrics.widthPixels / 4;
		LinearLayout layoutLocal = (LinearLayout) this
				.findViewById(R.id.frame_local_area);
		FrameLayout.LayoutParams layoutParams = (android.widget.FrameLayout.LayoutParams) layoutLocal
				.getLayoutParams();
		if (bLandScape) {

			if (AnyChatCoreSDK.GetSDKOptionInt(AnyChatDefine.BRAC_SO_LOCALVIDEO_WIDTHCTRL) != 0)
				height = width * AnyChatCoreSDK.GetSDKOptionInt(AnyChatDefine.BRAC_SO_LOCALVIDEO_HEIGHTCTRL)
						/ AnyChatCoreSDK.GetSDKOptionInt(AnyChatDefine.BRAC_SO_LOCALVIDEO_WIDTHCTRL)
						+ PROGRESSBAR_HEIGHT;
			else
				height = (float) 3 / 4 * width + PROGRESSBAR_HEIGHT;
		} else {

			if (AnyChatCoreSDK.GetSDKOptionInt(AnyChatDefine.BRAC_SO_LOCALVIDEO_HEIGHTCTRL) != 0)
				height = width * AnyChatCoreSDK.GetSDKOptionInt(AnyChatDefine.BRAC_SO_LOCALVIDEO_WIDTHCTRL)
						/ AnyChatCoreSDK.GetSDKOptionInt(AnyChatDefine.BRAC_SO_LOCALVIDEO_HEIGHTCTRL)
						+ PROGRESSBAR_HEIGHT;
			else
				height = (float) 4 / 3 * width + PROGRESSBAR_HEIGHT;
		}
		layoutParams.width = (int) width;
		layoutParams.height = (int) height;
		layoutLocal.setLayoutParams(layoutParams);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
			adjustLocalVideo(true);
			AnyChatCoreSDK.mCameraHelper.setCameraDisplayOrientation();
		} else {
			adjustLocalVideo(false);
			AnyChatCoreSDK.mCameraHelper.setCameraDisplayOrientation();
		}

	}

	// 判断视频是否已打开
	private void CheckVideoStatus() {
		if (!bOtherVideoOpened) {
			if (anychat.GetCameraState(dwTargetUserId) == 2
					&& anychat.GetUserVideoWidth(dwTargetUserId) != 0) {
				SurfaceHolder holder = mSurfaceRemote.getHolder();
				// 如果是采用内核视频显示（非Java驱动），则需要设置Surface的参数
				if (AnyChatCoreSDK.GetSDKOptionInt(AnyChatDefine.BRAC_SO_VIDEOSHOW_DRIVERCTRL) != AnyChatDefine.VIDEOSHOW_DRIVER_JAVA) {
					holder.setFormat(PixelFormat.RGB_565);
					holder.setFixedSize(anychat.GetUserVideoWidth(-1), anychat.GetUserVideoHeight(-1));
				}
				Surface s = holder.getSurface();
				if (AnyChatCoreSDK.GetSDKOptionInt(AnyChatDefine.BRAC_SO_VIDEOSHOW_DRIVERCTRL) == AnyChatDefine.VIDEOSHOW_DRIVER_JAVA) {
					anychat.mVideoHelper.SetVideoUser(videoIndex, dwTargetUserId);
				} else
					anychat.SetVideoPos(dwTargetUserId, s, 0, 0, 0, 0);
				bOtherVideoOpened = true;
			}
		}
		if (!bSelfVideoOpened) {
			if (anychat.GetCameraState(-1) == 2 && anychat.GetUserVideoWidth(-1) != 0) {
				SurfaceHolder holder = mSurfaceSelf.getHolder();
				if (AnyChatCoreSDK.GetSDKOptionInt(AnyChatDefine.BRAC_SO_VIDEOSHOW_DRIVERCTRL) != AnyChatDefine.VIDEOSHOW_DRIVER_JAVA) {
					holder.setFormat(PixelFormat.RGB_565);
					holder.setFixedSize(anychat.GetUserVideoWidth(-1), anychat.GetUserVideoHeight(-1));
				}
				Surface s = holder.getSurface();
				anychat.SetVideoPos(-1, s, 0, 0, 0, 0);
				bSelfVideoOpened = true;
			}
		}
	}

	private void updateVolume() {
		mProgressSelf.setProgress(anychat.GetUserSpeakVolume(-1));
		mProgressRemote.setProgress(anychat.GetUserSpeakVolume(dwTargetUserId));
	}

	@Override
	public void OnAnyChatConnectMessage(boolean bSuccess) {
		// TODO Auto-generated method stub
		if (dialog != null
				&& dialog.isShowing()
				&& DialogFactory.getCurrentDialogId() == DialogFactory.DIALOGID_RESUME) {
			dialog.dismiss();
		}
	}

	@Override
	public void OnAnyChatLoginMessage(int dwUserId, int dwErrorCode) {
		// TODO Auto-generated method stub
		if (dwErrorCode == 0) {
			BussinessCenter.selfUserId = dwUserId;
			BussinessCenter.selfUserName = anychat.GetUserName(dwUserId);
		}
	}

	@Override
	public void OnAnyChatEnterRoomMessage(int dwRoomId, int dwErrorCode) {
		// TODO Auto-generated method stub
		if (dwErrorCode == 0) {
			anychat.UserCameraControl(-1, 1);
			anychat.UserSpeakControl(-1, 1);
			bSelfVideoOpened = false;
		}
	}

	@Override
	public void OnAnyChatOnlineUserMessage(int dwUserNum, int dwRoomId) {
		// TODO Auto-generated method stub
		anychat.UserCameraControl(dwTargetUserId, 1);
		anychat.UserSpeakControl(dwTargetUserId, 1);
		bOtherVideoOpened = false;
	}

	@Override
	public void OnAnyChatUserAtRoomMessage(int dwUserId, boolean bEnter) {
		// TODO Auto-generated method stub
		anychat.UserCameraControl(dwTargetUserId, 1);
		anychat.UserSpeakControl(dwTargetUserId, 1);
		bOtherVideoOpened = false;

	}

	@Override
	public void OnAnyChatLinkCloseMessage(int dwErrorCode) {
		// TODO Auto-generated method stub
		anychat.UserCameraControl(-1, 0);
		anychat.UserSpeakControl(-1, 0);
		anychat.UserSpeakControl(dwTargetUserId, 0);
		anychat.UserCameraControl(dwTargetUserId, 0);
		if (dwErrorCode == 0) {
			if (dialog != null && dialog.isShowing())
				dialog.dismiss();
			BaseMethod.showToast(this.getString(R.string.session_end), this);
			dialog = DialogFactory.getDialog(DialogFactory.DIALOG_NETCLOSE,
					DialogFactory.DIALOG_NETCLOSE, this);
			dialog.show();
		} else {
			BaseMethod.showToast(this.getString(R.string.str_serverlink_close),
					this);
			Intent intent = new Intent();
			intent.putExtra("INTENT", BaseConst.AGAIGN_LOGIN);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			intent.setClass(this, LoginActivity.class);
			this.startActivity(intent);
			this.finish();
		}
		Log.i("ANYCHAT", "OnAnyChatLinkCloseMessage:" + dwErrorCode);
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		if (v == mBtnEndSession) {
			dialog = DialogFactory.getDialog(DialogFactory.DIALOGID_ENDCALL,
					dwTargetUserId, this);
			dialog.show();
		}
		if (v == mImgSwitch) {
			// 如果是采用Java视频采集，则在Java层进行摄像头切换
			if (AnyChatCoreSDK
					.GetSDKOptionInt(AnyChatDefine.BRAC_SO_LOCALVIDEO_CAPDRIVER) == AnyChatDefine.VIDEOCAP_DRIVER_JAVA) {
				AnyChatCoreSDK.mCameraHelper.SwitchCamera();
				return;
			}
			String strVideoCaptures[] = anychat.EnumVideoCapture();
			String temp = anychat.GetCurVideoCapture();
			for (int i = 0; i < strVideoCaptures.length; i++) {
				if (!temp.equals(strVideoCaptures[i])) {
					anychat.UserCameraControl(-1, 0);
					anychat.SelectVideoCapture(strVideoCaptures[i]);
					anychat.UserCameraControl(-1, 1);
					bSelfVideoOpened = false;
					break;
				}
			}
		}
	}

	@Override
	public void OnAnyChatVideoCallEvent(int dwEventType, int dwUserId,
			int dwErrorCode, int dwFlags, int dwParam, String userStr) {
		// TODO Auto-generated method stub
		this.finish();
	}

	@Override
	public void OnAnyChatUserInfoUpdate(int dwUserId, int dwType) {
		// TODO Auto-generated method stub
		if (dwUserId == 0 && dwType == 0) {
			BussinessCenter.getBussinessCenter().getOnlineFriendDatas();
		}
	}

	@Override
	public void OnAnyChatFriendStatus(int dwUserId, int dwStatus) {
		// TODO Auto-generated method stub
		BussinessCenter.getBussinessCenter().onUserOnlineStatusNotify(dwUserId, dwStatus);
	}
	

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		// TODO Auto-generated method stub
		return false;
	}

	public void setIsRunning(boolean runningState)
    {
        this.isRunning = runningState;
    }


    class pairDevices extends AsyncTask<Void,Void,ArrayAdapter<String>>
    {

        @Override
        protected ArrayAdapter<String> doInBackground(Void... params) {
            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
// If there are paired devices
            if (pairedDevices.size() > 0) {
                // Loop through paired devices
                for (BluetoothDevice device : pairedDevices) {
                    // Add the name and address to an array adapter to show in a ListView
                    mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                    bluetoothDevices.add(device);
                }
            }

            return mArrayAdapter;
        }

        @Override
        protected void onPostExecute(ArrayAdapter<String> mArrayAdapter) {
            list.setAdapter(mArrayAdapter);
        }
    }

    public void setBluetoothState(final String state)
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                bluetoothStateTextView.setText(state);
            }
        });
    }

    public void setServerState(final String state)
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                serverStateTextView.setText(state);
            }
        });
    }

    public void setReceOrder(final String receOrder)
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                receOrderTextView.setText(receOrder);
            }
        });
        System.out.println(receOrder);
        if (receOrder.equals("face"))
        {
            AnyChatCoreSDK.mCameraHelper.capture();
        }else if (connectedThread != null) {
            System.out.println("向蓝牙发送" + receOrder);
            connectedThread.write(receOrder);
        }
    }

    public void setSendOrder(final String sendOrder)
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                sendOrderTextView.setText(sendOrder);
            }
        });
    }


    public void setImage(final Bitmap image)
    {
        photoImageView = (ImageView) this.findViewById(R.id.photoImageView);

        Matrix matrix = new Matrix();
        matrix.reset();
        matrix.setRotate(270);
        img = Bitmap.createBitmap(image,0,0,image.getWidth(),image.getHeight(),matrix,true);
        photoImageView.setImageBitmap(img);

        FaceppDetect faceppDetect = new FaceppDetect();

        faceppDetect.setDetectCallback(new DetectCallback() {

            public void detectResult(JSONObject rst) {
                //use the red paint
                Paint paint = new Paint();
                paint.setColor(Color.RED);
                paint.setStrokeWidth(Math.max(img.getWidth(), img.getHeight()) / 100f);

                //create a new canvas
                Bitmap bitmap = Bitmap.createBitmap(img.getWidth(), img.getHeight(), img.getConfig());
                Canvas canvas = new Canvas(bitmap);
                canvas.drawBitmap(img, new Matrix(), null);

                try {
                    //find out all faces
                    final int count = rst.getJSONArray("face").length();
                    for (int i = 0; i < count; ++i) {
                        float x, y, w, h;
                        //get the center point
                        x = (float)rst.getJSONArray("face").getJSONObject(i)
                                .getJSONObject("position").getJSONObject("center").getDouble("x");
                        y = (float)rst.getJSONArray("face").getJSONObject(i)
                                .getJSONObject("position").getJSONObject("center").getDouble("y");

                        //get face size
                        w = (float)rst.getJSONArray("face").getJSONObject(i)
                                .getJSONObject("position").getDouble("width");
                        h = (float)rst.getJSONArray("face").getJSONObject(i)
                                .getJSONObject("position").getDouble("height");
                        System.out.println("笑容是：" + rst.getJSONArray("face").getJSONObject(i).getJSONObject("attribute").getJSONObject("smiling").getDouble("value"));

                        //change percent value to the real size
                        x = x / 100 * img.getWidth();
                        w = w / 100 * img.getWidth() * 0.7f;
                        y = y / 100 * img.getHeight();
                        h = h / 100 * img.getHeight() * 0.7f;

                        //draw the box to mark it out
                        canvas.drawLine(x - w, y - h, x - w, y + h, paint);
                        canvas.drawLine(x - w, y - h, x + w, y - h, paint);
                        canvas.drawLine(x + w, y + h, x - w, y + h, paint);
                        canvas.drawLine(x + w, y + h, x + w, y - h, paint);
                    }

                    //save new image
                    img = bitmap;

                    VideoActivity.this.runOnUiThread(new Runnable() {

                        public void run() {
                            //show the image
                            photoImageView.setImageBitmap(img);
                            System.out.println("检测到人脸啦");
                        }
                    });

                } catch (JSONException e) {
                    e.printStackTrace();
                   System.out.println("跪了");
                }

            }
        });
        faceppDetect.detect(img);

    }





    private class FaceppDetect {
        DetectCallback callback = null;

        public void setDetectCallback(DetectCallback detectCallback) {
            callback = detectCallback;
        }

        public void detect(final Bitmap image) {

            new Thread(new Runnable() {

                public void run() {
                    HttpRequests httpRequests = new HttpRequests("4480afa9b8b364e30ba03819f3e9eff5", "Pz9VFT8AP3g_Pz8_dz84cRY_bz8_Pz8M", true, false);
                    //Log.v(TAG, "image size : " + img.getWidth() + " " + img.getHeight());

                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    float scale = Math.min(1, Math.min(600f / img.getWidth(), 600f / img.getHeight()));
                    Matrix matrix = new Matrix();
                    matrix.postScale(scale, scale);

                    Bitmap imgSmall = Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, false);
                    //Log.v(TAG, "imgSmall size : " + imgSmall.getWidth() + " " + imgSmall.getHeight());

                    imgSmall.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                    byte[] array = stream.toByteArray();

                    try {
                        //detect
                        JSONObject result = httpRequests.detectionDetect(new PostParameters().setImg(array));
                        System.out.println(result);
                        //finished , then call the callback function
                        if (callback != null) {
                            callback.detectResult(result);
                        }
                    } catch (FaceppParseException e) {
                        e.printStackTrace();
                    }

                }
            }).start();
        }
    }

    interface DetectCallback {
        void detectResult(JSONObject rst);
    }

    public void manageBluetoothConnectedSocket(BluetoothSocket socket)
    {
        connectedThread = new ConnectedThread(socket,VideoActivity.this);
        connectedThread.start();
    }
}
