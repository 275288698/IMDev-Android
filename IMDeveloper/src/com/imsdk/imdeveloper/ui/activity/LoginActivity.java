package com.imsdk.imdeveloper.ui.activity;

import imsdk.data.IMMyself;
import imsdk.data.IMMyself.OnActionListener;
import imsdk.data.IMMyself.OnAutoLoginListener;
import imsdk.data.IMSDK;
import imsdk.data.mainphoto.IMSDKMainPhoto;
import android.app.Activity;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.imsdk.imdeveloper.R;
import com.imsdk.imdeveloper.app.IMApplication;
import com.imsdk.imdeveloper.app.IMConfiguration;
import com.imsdk.imdeveloper.ui.a1common.UICommon;
import com.imsdk.imdeveloper.ui.view.TipsToast;
import com.imsdk.imdeveloper.util.LoadingDialog;

public class LoginActivity extends Activity implements OnClickListener {
	public static LoginActivity sSingleton;

	private EditText mUserNameEditText; // 帐号编辑框
	private EditText mPasswordEditText; // 密码编辑框

	private ImageView mImageView;
	private Button mLoginBtn;
	private Button mRegisterBtn;

	private LoadingDialog mDialog;

	private long mExitTime;

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if ((System.currentTimeMillis() - mExitTime) > 2000) {
				UICommon.showTips(R.drawable.tips_smile, "再按一次返回桌面");
				mExitTime = System.currentTimeMillis();
			} else {
				finish();
			}

			return true;
		}

		return super.onKeyDown(keyCode, event);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		sSingleton = this;

		// 使得音量键控制媒体声音
		setVolumeControlStream(AudioManager.STREAM_MUSIC);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_login);

		mUserNameEditText = (EditText) findViewById(R.id.login_user_name_edittext);
		mPasswordEditText = (EditText) findViewById(R.id.login_password_edittext);

		mLoginBtn = (Button) findViewById(R.id.login_login_btn);
		mLoginBtn.setOnClickListener(this);

		mRegisterBtn = (Button) findViewById(R.id.login_register_btn);
		mRegisterBtn.setOnClickListener(this);

		mImageView = (ImageView) findViewById(R.id.login_imageview);

		IMMyself.init(getApplicationContext(), IMConfiguration.sAppKey,
				new OnAutoLoginListener() {
					@Override
					public void onAutoLoginBegan() {
						Uri uri = IMSDKMainPhoto.getLocalUri(IMMyself.getCustomUserID());

						if (uri != null) {
							IMApplication.sImageLoader.displayImage(uri.toString(),
									mImageView, IMApplication.sDisplayImageOptions);
						}

						mLoginBtn.setEnabled(false);

						mUserNameEditText.setText(IMMyself.getCustomUserID());
						mPasswordEditText.setText(IMMyself.getPassword());

						mDialog = new LoadingDialog(LoginActivity.this, "正在登录...");
						mDialog.setCancelable(false);
						mDialog.show();
					}

					@Override
					public void onAutoLoginSuccess() {
						UICommon.showTips(R.drawable.tips_smile, "登录成功");
						updateStatus(SUCCESS);
					}

					@Override
					public void onAutoLoginFailure(boolean conflict) {
						if (conflict) {
							UICommon.showTips(R.drawable.tips_error, "登录冲突");
						} else {
							UICommon.showTips(R.drawable.tips_error, "登录失败");
						}

						updateStatus(FAILURE);
					}
				});

		mUserNameEditText.addTextChangedListener(mTextWatcher);

		String customUserID = getIntent().getStringExtra("CustomUserID");

		if (customUserID != null) {
			mUserNameEditText.setText(customUserID);
		}
	}

	private final static int SUCCESS = 0;
	private final static int FAILURE = -1;

	private void updateStatus(int status) {
		mLoginBtn.setEnabled(true);

		switch (status) {
		case SUCCESS: {
			mDialog.dismiss();
			UICommon.showTips(R.drawable.tips_smile, "登录成功");

			Intent intent = new Intent(LoginActivity.this, MainActivity.class);

			intent.putExtra("userName", mUserNameEditText.getText().toString());
			startActivity(intent);
			LoginActivity.this.finish();
		}
			break;
		case FAILURE:
			mDialog.dismiss();
			mLoginBtn.setEnabled(true);
			break;
		default:
			throw new IllegalStateException();
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.login_login_btn:
			mDialog = new LoadingDialog(this, "正在登录...");
			mDialog.setCancelable(true);
			mDialog.show();
			login();
			break;
		case R.id.login_register_btn: {
			Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);

			startActivityForResult(intent, 1);
		}
			break;
		default:
			break;
		}
	}

	private void login() {
		boolean result = IMMyself.setCustomUserID(mUserNameEditText.getText()
				.toString());

		if (!result) {
			UICommon.showTips(R.drawable.tips_warning, IMSDK.getLastError());
			mDialog.dismiss();
			return;
		}

		result = IMMyself.setPassword(mPasswordEditText.getText().toString());

		if (!result) {
			showTips(R.drawable.tips_warning, IMSDK.getLastError());
			mDialog.dismiss();
			return;
		}

		IMMyself.login(false, 5, new OnActionListener() {
			@Override
			public void onSuccess() {
				UICommon.showTips(R.drawable.tips_smile, "登录成功");
				updateStatus(SUCCESS);
			}

			@Override
			public void onFailure(String error) {
				if (error.equals("Timeout")) {
					error = "登录超时";
				} else if (error.equals("Wrong Password")) {
					error = "密码错误";
				}

				updateStatus(FAILURE);
				UICommon.showTips(R.drawable.tips_error, error);
			}
		});
	}

	private static TipsToast mTipsToast;

	private void showTips(int iconResId, String tips) {
		if (mTipsToast != null) {
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
				mTipsToast.cancel();
			}
		} else {
			mTipsToast = TipsToast.makeText(getApplication().getBaseContext(), tips,
					TipsToast.LENGTH_SHORT);
		}

		mTipsToast.show();
		mTipsToast.setIcon(iconResId);
		mTipsToast.setText(tips);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (data == null) {
			return;
		}

		switch (requestCode) {
		case 1:
			if (resultCode == RESULT_OK) {
				String username = data.getStringExtra("username");
				String password = data.getStringExtra("password");

				if (null == username || "".equals(username) || "null".equals(username)
						|| null == password || "".equals(password)
						|| "null".equals(password)) {
					return;
				} else {
					mUserNameEditText.setText(username);
					mPasswordEditText.setText(password);
					mLoginBtn.setEnabled(false);
					mDialog = new LoadingDialog(this, "正在登录...");
					mDialog.show();
					login();
				}
			}

			break;
		}
	}

	private TextWatcher mTextWatcher = new TextWatcher() {
		public void afterTextChanged(Editable s) {
		}

		public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		}

		public void onTextChanged(CharSequence s, int start, int before, int count) {
			Uri uri = IMSDKMainPhoto.getLocalUri(s.toString());

			if (uri != null) {
				IMApplication.sImageLoader.displayImage(uri.toString(), mImageView,
						IMApplication.sDisplayImageOptions);
			} else {
				mImageView.setImageResource(R.drawable.ic_launcher);
			}
		}
	};
}