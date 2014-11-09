package com.bairuitech.callcenter;

import com.bairuitech.anychat.AnyChatBaseEvent;
import com.bairuitech.anychat.AnyChatCoreSDK;
import com.bairuitech.bussinesscenter.BussinessCenter;

import com.bairuitech.util.*;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import com.bairuitech.callcenter.R;
public class LoginActivity extends Activity implements AnyChatBaseEvent,
        OnClickListener {
    private Button configBtn;
    private Button loginBtn;
//    private CheckBox mCheckRemember;
    private ConfigEntity configEntity;
    private EditText mEditAccount;
    private ProgressDialog mProgressLogin;
    private Dialog dialog;
    private AnyChatCoreSDK anychat;
    private boolean bNeedRelease = false;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initSdk();
        intParams();
        initView();
        initLoginProgress();

    }

    protected void intParams() {
        configEntity = ConfigService.LoadConfig(this);
        BussinessCenter.getBussinessCenter();
    }

    @Override
    protected void onRestart() {
        // TODO Auto-generated method stub
        super.onRestart();
    }


    @Override
    protected void onNewIntent(Intent intent) {
        // TODO Auto-generated method stub
        super.onNewIntent(intent);
        int tag=intent.getIntExtra("INTENT", BaseConst.AGAIGN_LOGIN);
        if(tag==BaseConst.AGAIGN_LOGIN)
        {
            if(anychat!=null)
            {
                anychat.Logout();
                anychat.SetBaseEvent(this);
            }
        }
        else if(tag==BaseConst.APP_EXIT)
        {
            this.finish();
        }
    }

    @Override
    protected void onStop() {
        // TODO Auto-generated method stub
        super.onStop();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // TODO Auto-generated method stub

        return super.onKeyDown(keyCode, event);
    }

    private void initSdk() {
        if (anychat == null) {
            anychat = new AnyChatCoreSDK();
            anychat.SetBaseEvent(this);
            anychat.InitSDK(android.os.Build.VERSION.SDK_INT, 0);
            bNeedRelease = true;
        }
    }

    private void initView() {
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        this.setContentView(R.layout.login);
        mEditAccount = (EditText) findViewById(R.id.editAccount);
//        mCheckRemember = (CheckBox) findViewById(R.id.check_issavepass);
//        mCheckRemember.setTextColor(Color.BLACK);
        loginBtn = (Button) findViewById(R.id.loginBtn);
        loginBtn.setOnClickListener(this);
        configBtn = (Button) findViewById(R.id.settingBtn);
        configBtn.setOnClickListener(this);
//        if (configEntity.IsSaveNameAndPw) {
//            mCheckRemember.setChecked(true);
//            if (configEntity.name != null)
//                mEditAccount.setText(configEntity.name);
//        } else
//            mCheckRemember.setChecked(false);
    }


    private void initLoginProgress() {
        mProgressLogin = new ProgressDialog(this);
        mProgressLogin.setOnDismissListener(new OnDismissListener() {

            @Override
            public void onDismiss(DialogInterface dialog) {
                // TODO Auto-generated method stub
                loginBtn.setClickable(true);
            }
        });
        mProgressLogin.setMessage(this.getString(R.string.login_progress));
    }

    private void Login() {
        String strUserName = mEditAccount.getEditableText().toString();
//        if (mCheckRemember.isChecked()) {
//            configEntity.IsSaveNameAndPw = true;
//            configEntity.name = strUserName;
//        } else {
//            configEntity.IsSaveNameAndPw = false;
//        }
        ConfigService.SaveConfig(this, configEntity);
        if (mEditAccount.getText().length() == 0) {
            BaseMethod.showToast(this.getString(R.string.str_account_input_hint), this);
            return;
        }

        this.anychat.Connect(configEntity.ip, configEntity.port);
        this.anychat.Login(strUserName, "123");
        loginBtn.setClickable(false);
        mProgressLogin.show();
    }

    protected void onDestroy() {
        super.onDestroy();
        if (bNeedRelease) {
            anychat.Logout();
            anychat.Release();
            android.os.Process.killProcess(android.os.Process.myPid());
        }
        BussinessCenter.getBussinessCenter().realseData();
    }

    protected void onResume() {
        super.onResume();
    }

    @Override
    public void OnAnyChatConnectMessage(boolean bSuccess) {
        if (!bSuccess) {
            BaseMethod.showToast(getString(R.string.server_connect_error), this);
            mProgressLogin.dismiss();
        } else {
        }
    }

    @Override
    public void onAttachedToWindow() {
        // TODO Auto-generated method stub

    }

    @Override
    public void OnAnyChatEnterRoomMessage(int dwRoomId, int dwErrorCode) {
        // TODO Auto-generated method stub

    }

    @Override
    public void OnAnyChatLinkCloseMessage(int dwErrorCode) {
        // Toast.makeText(this, "���ӹرգ�error��" + dwErrorCode,
        // Toast.LENGTH_SHORT)
        // .show();
    }

    @Override
    public void OnAnyChatLoginMessage(int dwUserId, int dwErrorCode) {
        if (dwErrorCode == 0) {
            BussinessCenter.selfUserId = dwUserId;
            BussinessCenter.selfUserName=mEditAccount.getText()
                    .toString();
            Intent intent=new Intent();
            intent.setClass(this, HallActivity.class);
            this.startActivity(intent);
        } else if (dwErrorCode == 200) {
            BaseMethod.showToast(
                    getString(R.string.str_lggin_failed), this);
        }
        mProgressLogin.dismiss();
    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
    }

    @Override
    public void OnAnyChatOnlineUserMessage(int dwUserNum, int dwRoomId) {
        // TODO Auto-generated method stub

    }

    @Override
    public void OnAnyChatUserAtRoomMessage(int dwUserId, boolean bEnter) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub
        switch (v.getId()) {
            case R.id.loginBtn:
                Login();
                break;
            case R.id.settingBtn:
                dialog = DialogFactory.getDialog(DialogFactory.DIALOGID_CONFIG, configEntity,this);
                dialog.show();
                break;
            default:
                break;
        }
    }

}