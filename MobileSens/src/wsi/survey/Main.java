package wsi.survey;


import java.util.List;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import wsi.mobilesens.MobileSens;
import wsi.mobilesens.R;
import wsi.survey.result.GConstant;
import wsi.survey.util.UpdateManager;

public class Main extends Activity {
	private final String TAG = "Main";
	private ImageView ivWelcome;
	private RelativeLayout layout_main;
	private TelephonyManager mTelManager;
	private static boolean isFirstLoad = true;		// 是否是第一次加载
	private static boolean isStartActivity = false;		// 是否是开启新Activity
	public static final String NEW_LIFEFORM_DETECTED =  "com.android.broadcast.NEW_LIFEFORM"; 
	UpdateManager mUpdateManager;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		Log.i(TAG, "onCreate()");
		if(!isServiceRunning(MobileApplication.mContext, MobileSens.class.getName())){
			Log.e(Main.class.getSimpleName(), "service not running");
			Intent it = new Intent(NEW_LIFEFORM_DETECTED);           
	        sendBroadcast(it); 
		}		
		isFirstLoad = true;		
		initResource();		
		animPlay();
		
	}

	/** 初始化控件 */
	private void initResource(){
		ivWelcome = (ImageView)findViewById(R.id.ivWelcome);
		layout_main = (RelativeLayout)findViewById(R.id.layout_main);
		mTelManager = (TelephonyManager)this.getSystemService(Context.TELEPHONY_SERVICE);
		GConstant.IMEI = mTelManager.getDeviceId();
		
		// 获取物理屏幕
		DisplayMetrics dm = new DisplayMetrics();
		dm = getResources().getDisplayMetrics();
		int screenWidth = dm.widthPixels;
		int screenHeight = dm.heightPixels;
		GConstant.adjustFontSize(screenWidth, screenHeight);
		
		Log.i(TAG, "dm.density = " + dm.density + "; screenWidth = " + screenWidth + "; screenHeight = " + screenHeight);
	}
	
	/** 播放开机动画 */
	private void animPlay(){
		Animation anim = AnimationUtils.loadAnimation(Main.this, R.anim.anim_main_welcome);
		anim.setFillEnabled(true); 
		anim.setFillAfter(true);  
		ivWelcome.startAnimation(anim);		
		Message msg = Message.obtain();
		mHandler.sendMessageDelayed(msg, 3000);
	}
	
	private Handler mHandler = new Handler(){
		@Override
		public void handleMessage(Message msg){
			ivWelcome.setVisibility(View.INVISIBLE);
			layout_main.setBackgroundResource(R.drawable.bg_main);
			checkForUpdate();
			
		}
	};
	
	
	@Override
	protected  void onStart(){
		super.onStart();
		Log.i(TAG, "onStart()");
	}
	
	public static int MENU_RESET = Menu.FIRST;
	public static int MENU_UPLOAD = Menu.FIRST + 1;
	public static int MENU_ABOUT = Menu.FIRST + 2;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		menu.add(0, MENU_ABOUT, 1, "关于");
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // TODO Auto-generated method stub
        if(item.getItemId() == MENU_ABOUT){
        	Intent intent=new Intent(Main.this,AboutActivity.class);
        	startActivity(intent);
        }
        return true;
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.i(TAG, "onResume()");
		isStartActivity = false;
		
		if(!isFirstLoad){
			Log.i(TAG, "isFirstLoad = " + isFirstLoad);
			
		} else {
			Log.i(TAG, "isFirstLoad = " + isFirstLoad);
		}		
	}

	@Override
	protected  void onPause() {
		super.onPause();
		Log.i(TAG, "onPause()");
		if(!isStartActivity){
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		Log.i(TAG, "onStop()");
	}
	
	@Override
	protected void onDestroy(){
		super.onDestroy();
		Log.i(TAG, "onDestroy()");
		Upload.apkVersion=null;
		Upload.apkDownLoadUrl=null;
	}
	
	private void checkForUpdate() {
		PageTask task = new PageTask(this);
		task.execute();
	}

	class PageTask extends AsyncTask<String, Integer, String> {
		// 可变长的输入参数，与AsyncTask.exucute()对应
		ProgressDialog progressDialog;

		public PageTask(Context context) {
			progressDialog = new ProgressDialog(context);
		}

		@Override
		protected String doInBackground(String... params) {
			Upload.doPostAll();
			return "";
		}

		@Override
		protected void onCancelled() {
			super.onCancelled();
		}

		@Override
		protected void onPostExecute(String result) {
			// 此处的result参数是来自doInBackground的返回值
			if (progressDialog != null) {
				progressDialog.dismiss();
			}
			if(Upload.apkVersion!=null){
				PackageManager pm = getPackageManager();
				try{
					PackageInfo pInfo=pm.getPackageInfo(getPackageName(), PackageManager.GET_CONFIGURATIONS);
					String versionName = pInfo.versionName;					
					String nowVersion = versionName;
					String newVersion = Upload.apkVersion;
					if(newVersion.compareTo(nowVersion)>0){
				        mUpdateManager = new UpdateManager(Main.this);
				        mUpdateManager.checkUpdateInfo();
					}
				}catch (Exception e) {
					// TODO: handle exception
				}
			}else{
//				Toast.makeText(Main.this, "已是最新版", Toast.LENGTH_LONG).show();
			}
		
			
		}

		@Override
		protected void onPreExecute() {
//			progressDialog = ProgressDialog.show(Main.this, "正在检查更新……",
//					"请等待", true, false);
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			// 更新进度
		}

	}
	
	/**
     * 用来判断服务是否后台运行
     * @param context
     * @param className 判断的服务名字
     * @return true 在运行 false 不在运行
     */
    public static boolean isServiceRunning(Context mContext,String className) {
        boolean IsRunning = false;
        ActivityManager activityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE); 
        List<ActivityManager.RunningServiceInfo> serviceList   = activityManager.getRunningServices(30);
       if (!(serviceList.size()>0)) {
            return false;
        }
        for (int i=0; i<serviceList.size(); i++) {
            if (serviceList.get(i).service.getClassName().equals(className) == true) {
                IsRunning = true;
                break;
            }
        }
        return IsRunning ;
    }
}