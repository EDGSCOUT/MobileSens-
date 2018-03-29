package wsi.mobilesens;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONException;
import org.json.JSONObject;

import com.nevin.dmeo.jnimac.jni.MacAddr;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Browser;
import android.provider.ContactsContract;
import android.telephony.TelephonyManager;
import android.util.Log;
import wsi.mobilesens.sensors.EventLogger;
import wsi.mobilesens.sensors.NetLogger;
import wsi.mobilesens.util.DataBaseAdaptor;
import wsi.mobilesens.util.MobileSensWakeLock;
import wsi.mobilesens.util.Status;
import wsi.mobilesens.util.Uploader;
import wsi.survey.Main;
import wsi.survey.MobileApplication;
/**

SystemSens:  http://systemsens.cens.ucla.edu/service/viz/login/?next=/service/media/SystemSens
Source code:  https://github.com/falaki/SystemSens
Server code:   falaki@cs.ucla.edu

 */

public class MobileSens extends Service implements SensorEventListener {

	private static final String TAG = "MobileSens";

	/** Action string for recording polling sensors */
	public static final String POLLSENSORS_ACTION = "PollSENSORS";

	public static String IMEI="";
	/** Types of messages used by this service */
	private static final int UPLOAD_START_MSG = 2;
	private static final int UPLOAD_END_MSG = 3;
	private static final int PROC_MSG = 5;
	private static final int WIFISCAN_MSG = 6;
	private static final int EVENTLOG_MSG = 7;
	private static final int NETLOG_MSG = 8;

	/** String names of JSON records */
	private static final String ACTIVITYLOG_TYPE = "activitylog";
	private static final String APK_TYPE = "apklog";
	private static final String CALL_TYPE = "calllog";
	private static final String CAMERA_BUTTON_TYPE = "camerabuttonlog";
	private static final String CONFIGUATION_CHANGED_TYPE = "configuationlog";
	private static final String CONTACT_ADD_TYPE = "contactlog";
	private static final String DATE_CHANGED_TYPE = "datechangedlog";
	private static final String GPS_TYPE = "gpslog";									 // gps
	private static final String HEADSET_TYPE = "headssetlog";
	private static final String INPUT_METHOD_CHANGED_TYPE = "inputmethodchangedlog";
	private static final String LOCALE_CHANGED_TYPE = "localechangedlog";
	private static final String NET_TYPE = "netlog";
	private static final String POWER_CONNECTED_TYPE = "powerconnectedlog";
	private static final String POWER_TYPE = "powerlog";
	private static final String SCREEN_TYPE = "screenlog";
	private static final String SERVICELOG_TYPE = "servicelog";
	private static final String SMS_TYPE = "smslog"; 									// sms
	private static final String URLINFO_TYPE = "urllog"; 									// url
	private static final String WALLPAPER_CHANGED_TYPE = "wallpaperchangedlog";

	private static final String confDir_initialContacts = "/sdcard/initialcontacts.conf";
	private static final String confDir_contactsNum = "/sdcard/contactsnum.conf";
	private static final String confDir_configInfo = "/sdcard/configinfo.conf";

	
	private static final String ACCELERATE_TYPE = "accelerateinfo";
	private static final String ORIENTATION_TYPE = "ori3entationinfo";
	
	
	/** Intervals used for timers in seconds */
	private long POLLING_INTERVAL;

	/** Unites of time */
	private static final int ONE_SECOND = 1000;
	private static final int HALF_MINUTE = 30 * ONE_SECOND;
	private static final int ONE_MINUTE = 60 * ONE_SECOND;

	private static final long DEFAULT_POLLING_INTERVAL = 10 * ONE_MINUTE;
	private static final long DEFAULT_WIFISCAN_INTERVAL = 10 * ONE_MINUTE;

	private static final int MIN_GPS_TIME = 30 * ONE_MINUTE;
	private static final int MIN_GPS_DIST = 500;
	private static final int ONE_HOUR = 60 * ONE_MINUTE;
	private static final int TEN_MINUTE = 10 *ONE_MINUTE;
	private static final int THREE_MINUE = 3 * ONE_MINUTE;
	/** Unique identifier for the notification */
	private static final int NOTIFICATION_ID = 0;

	/** Power manager object used to acquire a partial wakeLock */
	private PowerManager mPM;
	private PowerManager.WakeLock mWL;
	
	private NotificationManager mNM;
	private TelephonyManager mTelManager;
	private WifiManager mWifi;
	
	private LocationManager locationManager;
	private LocationProvider locationProvider;

	/** Database adaptor object */
	private DataBaseAdaptor mDbAdaptor;
	private Uploader mUploader;
	public static String MAC_ADDRESS=null;

	private EventLogger mEventLogger;
	private NetLogger mNetLogger;

	private static boolean mIsPlugged = false;
	private static boolean controlPlugged = false;
	private boolean mIsUploading;

	
	JSONObject gpsjson = new JSONObject();
	JSONObject acceleratejson = new JSONObject();
	JSONObject orientationjson = new JSONObject();

	
	private SensorManager sensorManager;
	private Sensor accelerometer;
	private Sensor orientationmeter;
	
    private int mRate = SensorManager.SENSOR_DELAY_NORMAL;  
    private int mCycle = 2000; //milliseconds  
    private int mEventCycle = 2000; //milliseconds  

    private float mAccuracyX = 90;  
    private float mAccuracyY = 45;  
    private float mAccuracyZ = 45;  

    private long lastUpdateOrientation = -1;  
    private long lastEventOrientation = -1;  

    private float x = -999f;  
    private float y = -999f;  
    private float z = -999f;
	
	private float gravity[] = new float[3];
	private float linear_acceleration[] = new float[3];
	private long lastUpdateAccelrate = -1;
	private long lastEventAccelerate = -1;
	private float accelerateAccuracy = 2;
	
	String gpsProvider=null;
	private static int OPEN_GPS = 0;
	private static int CLOSE_GPS = 1;
	
	
	
	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		
		Log.i(TAG, "onStart()");
		
		handleStart(intent);
	}
	
	
	
	private void handleStart(Intent intent) {
		if (intent != null) {
			String action = intent.getAction();
			if (action != null)
				if (action.equals(POLLSENSORS_ACTION)) {
					pollingSensors();
				}
		}
	}


	@Override
	public void onCreate() {
		super.onCreate();
		Log.i(TAG, "onCreate() start...");
		
		//开启前台Service
      	Log.i(TAG, "startForeground()");
      				
      	Notification notification = new Notification(R.drawable.icon, "MobileSens",System.currentTimeMillis());  
      	Intent notificationIntent = new Intent(this, Main.class);  
      	PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);  
      	notification.setLatestEventInfo(this, "MobileSens","正在运行", pendingIntent);  
      	startForeground(1, notification);  
      	//开启前台Service
		
		initial();

 		mTelManager = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
 		IMEI = mTelManager.getDeviceId();

 		if(MAC_ADDRESS==null){
			 MAC_ADDRESS=MacAddr.getMacAddress(MobileApplication.mContext);
			 
			 //未联网获取mac地址失败  使用imei代替
			 if(MAC_ADDRESS.equals("0"))
				 MAC_ADDRESS = IMEI;
		 }
		POLLING_INTERVAL = DEFAULT_POLLING_INTERVAL;

		
		mIsUploading = false;
		mDbAdaptor = new DataBaseAdaptor(this);
		mUploader = new Uploader(mDbAdaptor);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {		// android 2.2
			mEventLogger = new EventLogger();
//			mNetLogger = new NetLogger(this);
		}

		IntentFilter apkfilter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED); 		// add
		apkfilter.addAction(Intent.ACTION_PACKAGE_CHANGED); 					// changed
		apkfilter.addAction(Intent.ACTION_PACKAGE_DATA_CLEARED);				// data_cleared
		apkfilter.addAction(Intent.ACTION_PACKAGE_INSTALL); 					// install
		apkfilter.addAction(Intent.ACTION_PACKAGE_REMOVED); 					// removed
		apkfilter.addAction(Intent.ACTION_PACKAGE_REPLACED); 					// replaced
		apkfilter.addAction(Intent.ACTION_PACKAGE_RESTARTED); 					// restarted
		apkfilter.addDataScheme("package");
		registerReceiver(APK_Receiveinfo, apkfilter);


		registerReceiver(BATTERY_Receiverinfo, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
		registerReceiver(CAMERA_BUTTON_Receiveinfo, new IntentFilter(Intent.ACTION_CAMERA_BUTTON));
		
		registerReceiver(CONFIGURATION_CHANGED_Receiveinfo, new IntentFilter(Intent.ACTION_CONFIGURATION_CHANGED));
		registerReceiver(DATE_AND_TIME_CHANGED_Receiverinfo, new IntentFilter(Intent.ACTION_DATE_CHANGED));
		registerReceiver(mScreenInfoReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));
		registerReceiver(mScreenInfoReceiver, new IntentFilter(Intent.ACTION_SCREEN_ON));
		registerReceiver(HEADSET_PLUG_Receiveinfo, new IntentFilter(Intent.ACTION_HEADSET_PLUG));
		registerReceiver(NEW_OUTGOING_CALL_Receiveinfo, new IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED));
		registerReceiver(INPUT_METHOD_CHANGED_Receiveinfo, new IntentFilter(Intent.ACTION_INPUT_METHOD_CHANGED));

		registerReceiver(LOCALE_CHANGED_Receiverinfo, new IntentFilter(Intent.ACTION_LOCALE_CHANGED));
		registerReceiver(NEW_OUTGOING_CALL_Receiveinfo, new IntentFilter(Intent.ACTION_NEW_OUTGOING_CALL));
		
		registerReceiver(POWER_Receiveinfo, new IntentFilter(Intent.ACTION_POWER_CONNECTED));
		registerReceiver(POWER_Receiveinfo, new IntentFilter(Intent.ACTION_POWER_DISCONNECTED));
		registerReceiver(SHUTDOWN_Receiverinfo, new IntentFilter(Intent.ACTION_SHUTDOWN));
		
//		registerReceiver(SMS_Receiveinfo, new IntentFilter("android.provider.Telephony.SMS_RECEIVED"));							// sms receiver
		registerReceiver(DATE_AND_TIME_CHANGED_Receiverinfo, new IntentFilter(Intent.ACTION_TIME_CHANGED));
		registerReceiver(WALLPAPER_CHANGED_Receiverinfo, new IntentFilter(Intent.ACTION_WALLPAPER_CHANGED));
		registerReceiver(WIFI_Receiverinfo, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

		ObserverSMS observerSMS = new ObserverSMS(new Handler(), this);										
		this.getContentResolver().registerContentObserver(Uri.parse("content://sms/"), true,  observerSMS);									// sms

		ObserverContacts observercontacts = new ObserverContacts(new Handler(), this);										
		this.getContentResolver().registerContentObserver(android.provider.ContactsContract.AUTHORITY_URI, true, observercontacts);			// contacts

		ObserverURL observerurl = new ObserverURL(new Handler(), this);
		this.getContentResolver().registerContentObserver(android.provider.Browser.BOOKMARKS_URI, true, observerurl);						// url

		
		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		accelerometer = sensorManager.getDefaultSensor( Sensor.TYPE_ACCELEROMETER);
		sensorManager.registerListener( this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
		orientationmeter = sensorManager.getDefaultSensor( Sensor.TYPE_ORIENTATION);
		sensorManager.registerListener( this, orientationmeter, SensorManager.SENSOR_DELAY_NORMAL);
		
		openGPS();
        Timer timer = new Timer();  
        TimerTask openTask = new TimerTask(){  
	        public void run() {  
	        	Message msg = new Message();
	        	msg.what = OPEN_GPS;
	            mHandler.sendMessage(msg);
	        }  
        };
        TimerTask closeTask = new TimerTask() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				Message msg = new Message();
	        	msg.what = CLOSE_GPS;
	        	mHandler.sendMessage(msg);
			}
		};
        
        
        timer.schedule(openTask, 0, ONE_HOUR);
        timer.schedule(closeTask, HALF_MINUTE, ONE_HOUR);

		Intent alarmIntent = new Intent(MobileSens.this, MobileSensAlarmReceiver.class);
		PendingIntent sender = PendingIntent.getBroadcast(MobileSens.this, 0, alarmIntent, 0);
		long firstTime = SystemClock.elapsedRealtime() + POLLING_INTERVAL;
		AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
		am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, firstTime, POLLING_INTERVAL, sender);

		mPM = (PowerManager) getSystemService(Context.POWER_SERVICE);
		mWL = mPM.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MobileSenseUpload");
		mWL.setReferenceCounted(false);

		mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		

		Log.i(TAG, "onCreate() done!");
		
	}
	
	private void openGPS(){	
		String serviceName = Context.LOCATION_SERVICE;
        locationManager = (LocationManager) this.getSystemService(serviceName);
        // 查找到服务信息
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE); // 高精度
        criteria.setAltitudeRequired(true);
        criteria.setBearingRequired(true);
        criteria.setCostAllowed(true);
        criteria.setPowerRequirement(Criteria.POWER_LOW); // 低功耗
        if(locationManager!=null){
        	gpsProvider = locationManager.getBestProvider(criteria, true); // 获取GPS信息
        	
        	 if(gpsProvider!=null){
             	Location location = locationManager.getLastKnownLocation(gpsProvider); // 通过GPS获取位置
             	if(location!=null){
             		JSONObject gpsjson = new JSONObject();

             		Log.i("position", "position has changed!");
             		double latitude = location.getLatitude();
             		double longitude = location.getLongitude();
             		double altitude = location.getAltitude();
             		float bearing = location.getBearing(); 		
             		try {
             			Log.i("latitude", Double.toString(latitude));
             			gpsjson.put("latitude", Double.toString(latitude));
             			gpsjson.put("longitude", Double.toString(longitude));
             			gpsjson.put("altitude", Double.toString(altitude));
             			gpsjson.put("bearing", Float.toString(bearing));
             		} catch (Exception e) {
             			Log.e("gpsListener", "a error has happen");
             		}
             		Log.e(MobileSens.class.getSimpleName(), "service get gps location!");
             		mDbAdaptor.createEntry(gpsjson, GPS_TYPE);
             	}
             	locationManager.requestLocationUpdates(gpsProvider,  MIN_GPS_TIME, MIN_GPS_DIST,
	                gpsListenser);   
             }  
        }
	}
	
	private void closeGPS(){
		if(locationManager!=null){
			locationManager.removeUpdates(gpsListenser);
		}
	}

	/** Clean up because we are going down. */
	@Override
	public void onDestroy() {
		Log.i(TAG, "onDestroy() start...!");

		// Log a message indicating killing SystemSens
		JSONObject sysJson = new JSONObject();
		try {
			sysJson.put("state", "killed");
		} catch (JSONException e) {
			Log.e(TAG, "Exception", e);
		}

        //mDbAdaptor.createEntry( sysJson, SYSTEMSENS_TYPE);

		// Clear the message handler's pending messages
		mHandler.removeMessages(EVENTLOG_MSG);
		mHandler.removeMessages(NETLOG_MSG);
		mHandler.removeMessages(PROC_MSG);

		// Unregister event updates
		// Unregister location updates
		// mLocManager.removeUpdates(mLocationListener);
		// Stop further WiFi scanning
		// Close the database adaptor
		//Obsolete. DB is not kept open continously mDbAdaptor.close();
		 
		mNM.cancel(NOTIFICATION_ID);

		Log.i(TAG, "onDestroy() done!");
		
	}

	/** Class for clients to access. */
	public class LocalBinder extends Binder {
		MobileSens getService() {
			return MobileSens.this;
		}
	}

	private final IBinder mLocalBinder = new LocalBinder();

	@Override
	public IBinder onBind(Intent intent) {
		Log.i(TAG, "onBind()");
		return mLocalBinder;
	}

	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			Log.i(TAG, "handleMessage()");

			if (msg.what == UPLOAD_START_MSG) {
				mIsUploading = true;
				Log.i(TAG, "mIsUploading = true");
			}else if (msg.what == UPLOAD_END_MSG) {
				mIsUploading = false;
				Log.i(TAG, "mIsUploading = false");
			}

			if (msg.what == WIFISCAN_MSG) {
				mWifi.startScan();

				msg = obtainMessage(WIFISCAN_MSG);
				long nextTime = SystemClock.uptimeMillis() + DEFAULT_WIFISCAN_INTERVAL;
				mHandler.sendMessageAtTime(msg, nextTime);
			}
			if(msg.what==OPEN_GPS){
				openGPS();
			}else if(msg.what==CLOSE_GPS){
				closeGPS();
			}
		}
	};

	/**
	 * Spawns a worker thread to "try" to upload the contents of the
	 * database. Before starting the thread, checks if a worker thread is
	 * already trying to upload. If so, returns. Otherwise a new thread is
	 * spawned and tasked with the upload job.
	 * 
	 */
	private void upload() {
		Log.i(TAG, "upload() start...");
		
		if (!mIsUploading) {
			Thread uploaderThread = new Thread() {
				public void run() {
					mWL.acquire();
					
					mHandler.sendMessageAtTime(mHandler.obtainMessage(UPLOAD_START_MSG), SystemClock.uptimeMillis());
					mUploader.tryUpload();
					mHandler.sendMessageAtTime(mHandler.obtainMessage(UPLOAD_END_MSG), SystemClock.uptimeMillis());
					
					mWL.release();
				}
			};

			uploaderThread.start();

		} else {
			Log.i(TAG, "upload()  in progress ...");
		}
		
		Log.i(TAG, "upload() done!");
	}

	public static boolean isPlugged() {
		return mIsPlugged;
	}
	public static boolean isControlPlugged() {
		return controlPlugged;
	}

	public static void setControlPlugged(boolean controlPlugged) {
		MobileSens.controlPlugged = controlPlugged;
	}
	private void pollingSensors() {
		Log.i(TAG, "pollingSensors() start...");
		Log.i("mIsPlugged ","1111"+Boolean.toString(mIsPlugged));
		if (mIsPlugged && (!mIsUploading))
			upload();

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {

			// Get network information
//			mDbAdaptor.createEntry(mNetLogger.getAppNetUsage(), NET_TYPE);
			
			mEventLogger.update();
			mDbAdaptor.createEntry(mEventLogger.getActivityEvents(), ACTIVITYLOG_TYPE);
			mDbAdaptor.createEntry(mEventLogger.getServiceEvents(), SERVICELOG_TYPE);
		}

		mDbAdaptor.flushDb();

		MobileSensWakeLock.releaseCpuLock();
		Log.i(TAG, "pollingSensors() done!");
	}

	/** BroadcastReceiver for sms receiver */
	/*
	private BroadcastReceiver SMS_Receiveinfo = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.i(TAG, "sms BroadcastReceiver()");
			
			Bundle bundle = intent.getExtras();
//			int smsID = intent.getIntExtra("smsID", -1);
			JSONObject smsjson = new JSONObject();
			if (bundle != null) {
				Object[] pdus = (Object[]) bundle.get("pdus");
				SmsMessage[] messages = new SmsMessage[pdus.length];
				for (int i = 0; i < pdus.length; i++){
					messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
					
					String msg = messages[i].getMessageBody();
					String address = messages[i].getOriginatingAddress();
					String name = getContacter(address);
					try {
						smsjson.put("type", "receive");
						smsjson.put("address", address);
//						smsjson.put("msg", msg);
						smsjson.put("name", name);
					} catch (JSONException e) {
						Log.e(TAG, "Exception", e);
					}
				}
			}
			Log.i("showSMS", smsjson.toString());
			
			mDbAdaptor.createEntry(smsjson, SMS_TYPE);
		}
	};*/

	/** Broadcast receive for app install info  */
	private BroadcastReceiver APK_Receiveinfo = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			JSONObject apkInfojson = new JSONObject();
			if (Intent.ACTION_PACKAGE_ADDED.equals(intent.getAction())) {
				Uri uri = intent.getData();
				String apkName = uri != null ? uri.getSchemeSpecificPart() : null;

				List<PackageInfo> packages = getPackageManager().getInstalledPackages(0);
				for (int i = 0; i < packages.size(); i++) {
					PackageInfo packageInfo = packages.get(i);
					AppInfo tmpInfo = new AppInfo();
					tmpInfo.appName = packageInfo.applicationInfo.loadLabel(getPackageManager()).toString();
					tmpInfo.packageName = packageInfo.packageName;
					if (tmpInfo.packageName.endsWith(apkName)) {
						try {
							apkInfojson.put("type", "install");
							apkInfojson.put("packageName", tmpInfo.packageName);
							apkInfojson.put("appName", tmpInfo.appName);

						} catch (JSONException e) {
							Log.e("intent", "a error happen");
						}
						break;
					}
				}
			} else if (Intent.ACTION_PACKAGE_CHANGED.equals(intent.getAction())) {
				Uri uri = intent.getData();
				String apkName = uri != null ? uri.getSchemeSpecificPart() : null;
				String componentsname = intent.getStringExtra(Intent.EXTRA_CHANGED_COMPONENT_NAME);
				try {
					apkInfojson.put("type", "changed");
					apkInfojson.put("packageName", apkName);
					apkInfojson.put("componentsname", componentsname);
				} catch (JSONException e) {
					Log.e("intent", "a error happen");
				}

			} else if (Intent.ACTION_PACKAGE_DATA_CLEARED.endsWith(intent.getAction())) {
				Uri uri = intent.getData();
				String apkName = uri != null ? uri.getSchemeSpecificPart() : null;
				try {
					apkInfojson.put("type", "datacleared");
					apkInfojson.put("packageName", apkName);
					apkInfojson.put("appName", null);
				} catch (JSONException e) {
					Log.e("intent", "a error happen");
				}

			} else if (Intent.ACTION_PACKAGE_REMOVED.equals(intent.getAction())) {
				Uri uri = intent.getData();
				String apkName = uri != null ? uri.getSchemeSpecificPart() : null;
				try {
					apkInfojson.put("type", "removed");
					apkInfojson.put("packageName", apkName);
					apkInfojson.put("appName", null);
				} catch (JSONException e) {
					Log.e("intent", "a error happen");
				}

			} else if (Intent.ACTION_PACKAGE_REPLACED.equals(intent.getAction())) {
				Uri uri = intent.getData();
				String apkName = uri != null ? uri.getSchemeSpecificPart() : null;
				try {
					apkInfojson.put("type", "replaced");
					apkInfojson.put("packageName", apkName);
					apkInfojson.put("appName", null);
				} catch (JSONException e) {
					Log.e("intent", "a error happen");
				}

			} else if (Intent.ACTION_PACKAGE_RESTARTED.equals(intent.getAction())) {
				Uri uri = intent.getData();
				String apkName = uri != null ? uri.getSchemeSpecificPart() : null;
				try {
					apkInfojson.put("type", "restarted");
					apkInfojson.put("packageName", apkName);
					apkInfojson.put("appName", null);
				} catch (JSONException e) {
					Log.e("intent", "a error happen");
				}

			}
			mDbAdaptor.createEntry(apkInfojson, APK_TYPE);
		}

	};
	
	
	/** Broadcast receiver for Battery information updates. start load data */
	private BroadcastReceiver BATTERY_Receiverinfo = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
				
				int plugType = intent.getIntExtra("plugged", 0);
				if (plugType > 0) {
					mIsPlugged = true;
					controlPlugged = true;
					Status.setPlug(true);
					Log.i(TAG, "Phone is plugged.  Starting upload.");
					upload();
				} else {
					if (mIsPlugged) {
						mIsPlugged = false;
						Status.setPlug(false);
					}
				}
			}
		}
	};
	


	private BroadcastReceiver CAMERA_BUTTON_Receiveinfo = new BroadcastReceiver() {
		JSONObject camerabuttonjson = new JSONObject();
		String camerabuttonInfo = "";

		@Override
		public void onReceive(Context context, Intent intent) {
			camerabuttonInfo = "press";
			try {
				camerabuttonjson.put("state", camerabuttonInfo);
			} catch (JSONException e) {
				Log.e("CAMERA_BUTTON_INFO", "detect camera button info is error!");
			}
			mDbAdaptor.createEntry(camerabuttonjson, CAMERA_BUTTON_TYPE);
		}
	};
	
	private BroadcastReceiver CONFIGURATION_CHANGED_Receiveinfo = new BroadcastReceiver() {
		JSONObject configuationjson = new JSONObject();

		@Override
		public void onReceive(Context context, Intent intent) {
			String configpath = "/sdcard/configinfo.txt";
			Configuration config = getResources().getConfiguration();
			float fontScale = config.fontScale; // 1
			int hardKeyboardHidden = config.keyboardHidden; // 2
			int keyboard = config.keyboard; // 3
			int navigation = config.navigation; // 4
			int navigationHidden = config.navigationHidden; // 5
			int screenlayout = config.screenLayout; // 6
			int touchscreen = config.touchscreen; // 7
			Boolean changeflag = false;
			String str = "";
			str = readNnum(configpath, 0);
			if (Float.toString(fontScale).equals(str)) {
				changeflag = true;
				try {
					configuationjson.put("fontScale", Float.toString(fontScale));
				} catch (JSONException e) {
					Log.e("CONFIGUATION_CHANGED_INFO", "detect configuation info has error!");
				}
			}
			str = readNnum(configpath, 1);
			if (Integer.toString(hardKeyboardHidden).equals(str)) {
				changeflag = true;
				try {
					configuationjson.put("hardKeyboardHidden", Integer.toString(hardKeyboardHidden));
				} catch (JSONException e) {
					Log.e("CONFIGUATION_CHANGED_INFO", "detect configuation info has error!");
				}
			}
			str = readNnum(configpath, 2);
			if (Integer.toString(keyboard).equals(str)) {
				changeflag = true;
				try {
					configuationjson.put("keyboard", Integer.toString(keyboard));

				} catch (JSONException e) {
					Log.e("CONFIGUATION_CHANGED_INFO", "detect configuation info has error!");
				}
			}
			str = readNnum(configpath, 3);
			if (Integer.toString(touchscreen).equals(str)) {
				changeflag = true;
				try {
					configuationjson.put("touchscreen", Integer.toString(touchscreen));
				} catch (JSONException e) {
					Log.e("CONFIGUATION_CHANGED_INFO", "detect configuation info has error!");
				}
			}
			str = readNnum(configpath, 4);
			if (Integer.toString(navigation).equals(str)) {
				changeflag = true;
				try {
					configuationjson.put("navigation", Integer.toString(navigation));
				} catch (JSONException e) {
					Log.e("CONFIGUATION_CHANGED_INFO", "detect configuation info has error!");
				}
			}
			str = readNnum(configpath, 5);
			if (Integer.toString(navigationHidden).equals(str)) {
				changeflag = true;
				try {
					configuationjson.put("navigationHidden", Integer.toString(navigationHidden));
				} catch (JSONException e) {
					Log.e("CONFIGUATION_CHANGED_INFO", "detect configuation info has error!");
				}
			}
			str = readNnum(configpath, 6);
			if (Integer.toString(screenlayout).equals(str)) {
				changeflag = true;
				try {
					configuationjson.put("screenlayout", Integer.toString(screenlayout));
				} catch (JSONException e) {
					Log.e("CONFIGUATION_CHANGED_INFO", "detect configuation info has error!");
				}
			}

			if (changeflag) {
				writeconfiginfo(configpath);
				mDbAdaptor.createEntry(configuationjson, CONFIGUATION_CHANGED_TYPE);
			}
		}

	};
	/*
	 * Broadcast date change
	 */
	BroadcastReceiver DATE_AND_TIME_CHANGED_Receiverinfo = new BroadcastReceiver() {
		JSONObject datechangedjson = new JSONObject();

		@Override
		public void onReceive(Context arg0, Intent arg1) {
			try {
				datechangedjson.put("datechangedInfo", "the system date has changed!");
				Log.i("datechangedInfo", "the system data has changed!");
			} catch (JSONException e) {
				Log.e("DATA_CHANGED", "detect system date changed info has error!");
			}
			mDbAdaptor.createEntry(datechangedjson, DATE_CHANGED_TYPE);
		}

	};
	private BroadcastReceiver HEADSET_PLUG_Receiveinfo = new BroadcastReceiver() {
		JSONObject headsetjson = new JSONObject();

		@Override
		public void onReceive(Context context, Intent intent) {
			int state = intent.getIntExtra("state", 0);
			String name = intent.getStringExtra("name");
			int microphone = intent.getIntExtra("microphone", 0);
			try {
				headsetjson.put("state", Integer.toString(state));
				headsetjson.put("name", name);
				headsetjson.put("microphone", Integer.toString(microphone));
			} catch (JSONException e) {
				Log.e("headjson", "put has a error");
			}

			mDbAdaptor.createEntry(headsetjson, HEADSET_TYPE);
		}
	};

	private BroadcastReceiver INPUT_METHOD_CHANGED_Receiveinfo = new BroadcastReceiver() {
		JSONObject inputmethodchangedjson = new JSONObject();
		String inputmethodchangedInfo = "";

		@Override
		public void onReceive(Context context, Intent intent) {
			inputmethodchangedInfo = "changed";
			try {
				inputmethodchangedjson.put("inputmethodchangedInfo", inputmethodchangedInfo);
			} catch (JSONException e) {
				Log.e("INPUT_METHOD_CHANGED", "detect input method change has a error!");
			}
			Log.i("INPUT_METHOD_CHANGED", "happen");
			mDbAdaptor.createEntry(inputmethodchangedjson, INPUT_METHOD_CHANGED_TYPE);
		}
	};

	/**
	 * Broadcast receiver for WiFi scan results. An object of this class has
	 * been passed to the system through registerRceiver.
	 * 
	 */

	/*
	 * 
	 * LOCALE_CHANGED_Receiverinfo
	 */
	BroadcastReceiver LOCALE_CHANGED_Receiverinfo = new BroadcastReceiver() {
		JSONObject localechangedjson = new JSONObject();
		String language = "";

		@Override
		public void onReceive(Context arg0, Intent arg1) {
			language = Locale.getDefault().getDisplayLanguage() + Locale.getDefault().getDisplayCountry();
			try {
				localechangedjson.put("language", language);
			} catch (JSONException e) {
				Log.e("LOCALE_CHANGED", "detect the system locale changed info has a error");
			}
			mDbAdaptor.createEntry(localechangedjson, LOCALE_CHANGED_TYPE);
		}

	};

	private BroadcastReceiver NEW_OUTGOING_CALL_Receiveinfo = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			JSONObject callinfojson = new JSONObject();
			String calldirection = "";
			String contactername = "";
			String state = "";
			String outingnumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
//			String outgoingcall = "Intent.ACTION_NEW_OUTGOING_CALL";
//			String incomingcall = "TelephonyManager.ACTION_PHONE_STATE_CHANGED";

			if (Intent.ACTION_NEW_OUTGOING_CALL.equals(intent.getAction())) {

				calldirection = "outging";
				contactername = getPeople(outingnumber);
				try {
					callinfojson.put("calldirection", calldirection);
					callinfojson.put("number", outingnumber);
					callinfojson.put("name", contactername);
					callinfojson.put("state", "");
				} catch (JSONException e) {
					Log.e("CALL_INFO", "receive call info has a error");
				}
				Log.i("Outgoingcallreceiver", "a new outgoing call");
			} else if (TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(intent.getAction())) {
				Bundle bundle = intent.getExtras();
				Log.i("Incomingcallreceiver", "a new incoming call");
				if (bundle == null)
					return;
				state = bundle.getString(TelephonyManager.EXTRA_STATE);
				if (state.equalsIgnoreCase(TelephonyManager.EXTRA_STATE_RINGING)) {
					calldirection = "incoming";
					String incomingnumber = bundle.getString(TelephonyManager.EXTRA_INCOMING_NUMBER);
					contactername = getPeople(incomingnumber);
					try {
						callinfojson.put("calldirection", calldirection);
						callinfojson.put("number", incomingnumber);
						callinfojson.put("name", contactername);
						callinfojson.put("state", state);
					} catch (JSONException e) {
						Log.e("CALL_INFO", "receive incoming call info has a error!");
					}

				} else {
					try {
						callinfojson.put("calldirection", "");
						callinfojson.put("number", "");
						callinfojson.put("name", "");
						callinfojson.put("state", state);
					} catch (JSONException e) {
						Log.e("CALL_INFO", "receive incoming call info has a error!");
					}
				}
			}
			mDbAdaptor.createEntry(callinfojson, CALL_TYPE);
		}
	};
	private BroadcastReceiver POWER_Receiveinfo = new BroadcastReceiver() {
		JSONObject powerjson = new JSONObject();
		String powerInfo = "";

		@Override
		public void onReceive(Context arg0, Intent arg1) {
			if (Intent.ACTION_POWER_CONNECTED.equals(arg1.getAction())) {
				powerInfo = "connected";

			} else if (Intent.ACTION_POWER_DISCONNECTED.equals(arg1.getAction())) {
				powerInfo = "disconnected";
			}
			try {
				powerjson.put("state", powerInfo);
			} catch (JSONException e) {
				Log.e("POWER ERROR", "detect power connection info is error");
			}
			mDbAdaptor.createEntry(powerjson, POWER_CONNECTED_TYPE);
		}
	};
	
	/** SHUTDOWN_Receiverinfo */
	BroadcastReceiver SHUTDOWN_Receiverinfo = new BroadcastReceiver() {
		JSONObject shutdownjson = new JSONObject();

		@Override
		public void onReceive(Context arg0, Intent arg1) {
			try {
				shutdownjson.put("state", "power off");
			} catch (JSONException e) {
				Log.e("SHUTDOWN", "detect system shutdown info has error");
			}
			mDbAdaptor.createEntry(shutdownjson, POWER_TYPE);
		}
	};

	/**
	 * Broadcast receiver for screen updates. An object of this class has
	 * been passed to the system through registerReceiver.
	 * 
	 */
	private BroadcastReceiver mScreenInfoReceiver = new BroadcastReceiver() {
		/**
		 * Method called whenever the intent is received. Gets the
		 * following information regarding the screen: o ON o OFF
		 * 
		 * When the screen turns on polling sensors are logged at a
		 * higher frequency.
		 * 
		 */
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			String status = "";
			if (action.equals(Intent.ACTION_SCREEN_OFF)) {
				status = "OFF";
//				upload();
			} else if (action.equals(Intent.ACTION_SCREEN_ON)) {
				status = "ON";
			}

			JSONObject screenjson = new JSONObject();
			try {
				screenjson.put("status", status);
			} catch (JSONException e) {
				Log.e(TAG, "Exception", e);
			}

			Log.i(TAG, "Screen " + screenjson.toString());

			mDbAdaptor.createEntry(screenjson, SCREEN_TYPE);
		}
	};
	
	private BroadcastReceiver WIFI_Receiverinfo = new BroadcastReceiver() {
//		JSONObject wifijson = new JSONObject();

		@Override
		public void onReceive(Context context, Intent intent) {
//			String action = intent.getAction();
//			if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
//				WifiInfo mWifiInfo = mWifi.getConnectionInfo();
//				String ssid = mWifiInfo.getSSID();
//				try {
//					wifijson.put("ssid", ssid);
//				} catch (JSONException e) {
//					Log.e("wifiinfo", "has a error");
//				}
//				mDbAdaptor.createEntry(wifijson, WIFISCAN_TYPE);
//			}
		}
	};
	
	/* * wall paper change */
	BroadcastReceiver WALLPAPER_CHANGED_Receiverinfo = new BroadcastReceiver() {
		JSONObject wallpaperchangedjson = new JSONObject();

		@Override
		public void onReceive(Context arg0, Intent arg1) {
			try {
				wallpaperchangedjson.put("wallpaperchangedInfo", "the wallpaper has changed");
				Log.i("wallpaperchangedInfo", "the wallpaper has changed");
			} catch (JSONException e) {
				Log.e("WALLPAPER_CHANGED_INFO", "detect the wallpaper changed has a error!");
			}
			mDbAdaptor.createEntry(wallpaperchangedjson, WALLPAPER_CHANGED_TYPE);
		}
	};

	public class ObserverSMS extends ContentObserver {
		private Context mcontext;

		public ObserverSMS(Handler handler, Context context) {
			super(handler);
			this.mcontext = context;
		}

		@Override
		public void onChange(boolean selfChange) {
			super.onChange(selfChange);
			
			Log.i(TAG, "sms ContentObserver()");
			
			//监听发送信息
			Cursor cursor = this.mcontext.getContentResolver().query(Uri.parse("content://sms/"),
					null,   
                   null,   
                    null,   
                    "date desc limit 1");
			cursor.moveToFirst(); 
			if (cursor!=null) {
				/*
				 * 
					type 的值对应的意思:
					java中常量名                          值                      解释
					MESSAGE_TYPE_INBOX           1                      收件箱(收到的信息)
					MESSAGE_TYPE_SENTBOX       2                      已发送(已经发送成功的信息)
					MESSAGE_TYPE_DRAFT           3                      草稿箱(保存为草稿的信息)
					MESSAGE_TYPE_OUTBOX        4                      发件箱(正在发送中的信息)
					MESSAGE_TYPE_FAILED          5                      发件箱(发送失败的信息)
					MESSAGE_TYPE_QUEUED        6                      发件箱(正在等待被发送的信息)
				 */
				int type = cursor.getInt(cursor.getColumnIndex("type"));
				String typee=null;
				if(type==1){
					typee="receive";
				}else if(type==2){
					typee="send";
				}
				if(typee!=null){
					Log.e("ReceiveSendSMS type",typee);
					String address = cursor.getString(cursor.getColumnIndex("address"));
					Log.e("sms send address", address);
					String msg = cursor.getString(cursor.getColumnIndex("body"));
					String name = getPeople(address);
					Log.i("ReceiveSendSMS", address + ":" + msg);
					
					JSONObject smsSendjson = new JSONObject();
					try {
						smsSendjson.put("type", typee);
						smsSendjson.put("address", address);
	//					smsSendjson.put("msg", msg);
						smsSendjson.put("name", name);
					} catch (JSONException e) {
						Log.e(TAG, "Exception", e);
					}
					Log.i("ReceiveSendSMS", smsSendjson.toString());
					Log.i("showSMS", smsSendjson.toString());
					
					mDbAdaptor.createEntry(smsSendjson, SMS_TYPE);
				}
			}
		
		}
	};

	public class ObserverURL extends ContentObserver {

		private static final String TAG = "Observer URL";
		private Context mcontext;
		private JSONObject urljson = new JSONObject();
		private String bookmark = new String();
		private String title = new String();
		private String urlinfo = new String();
		private String urlvisits = new String();

		public ObserverURL(Handler handler, Context context) {
			super(handler);
			this.mcontext = context;
		}

		// @SuppressWarnings("null")
		@Override
		public void onChange(boolean selfChange) {

			super.onChange(selfChange);
			Cursor urlCursor = this.mcontext
					.getContentResolver()
					.query(Browser.BOOKMARKS_URI,
							Browser.HISTORY_PROJECTION,
							null,
							null,
							Browser.BookmarkColumns.DATE);
			urlCursor.moveToLast();
			try {
				bookmark = urlCursor.getString(Browser.HISTORY_PROJECTION_BOOKMARK_INDEX);
				title = urlCursor.getString(Browser.HISTORY_PROJECTION_TITLE_INDEX);
				urlinfo = urlCursor.getString(Browser.HISTORY_PROJECTION_URL_INDEX);
				urlvisits = urlCursor.getString(Browser.HISTORY_PROJECTION_VISITS_INDEX);

			} catch (Exception e) {
				Log.e(TAG, "Exception", e);
			}

			try {
				urljson.put("bookmark", bookmark);
				urljson.put("title", title);
				urljson.put("urlinfo", urlinfo);
				urljson.put("urlvisits", urlvisits);

			} catch (JSONException e) {
				Log.e(TAG, "Exception", e);
			}
			 mDbAdaptor.createEntry(urljson, URLINFO_TYPE);
		}

	};

	public class ObserverContacts extends ContentObserver {
		
		JSONObject contactsjson = new JSONObject();
		JSONObject numberjson = new JSONObject();
		JSONObject emailjson = new JSONObject();
		private int count = 0;

		public ObserverContacts(Handler handler, Context context) {
			super(handler);
		}

		@Override
		public void onChange(boolean selfChange) {
			super.onChange(selfChange);
			
			String id = "";
			String name = "";
			String number = "";
			int numtype;
			int contentnum = 0;
			ContentResolver cr = getContentResolver();
			Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI,
								null, 
								null, 
								null, 
								"_ID desc");
			contentnum = cur.getCount();
			String num = readsd(confDir_contactsNum);
			writesd(Integer.toString(contentnum), confDir_contactsNum);
			int lastnum = Integer.parseInt(num);
			if (lastnum > contentnum) {
				try {
					contactsjson.put("name", "");
					contactsjson.put("number", null);
					contactsjson.put("state", "delete");
				} catch (JSONException e) {
					Log.e("CONTACTSJSON", "has a error!");
				}
				mDbAdaptor.createEntry(contactsjson, CONTACT_ADD_TYPE);
			} else if (lastnum < contentnum) {
				cur.moveToFirst();
				id = cur.getString(cur.getColumnIndex(ContactsContract.Contacts._ID));
				name = cur.getString(cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
				if (Integer.parseInt(cur.getString(cur.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0) {
					Cursor pCur = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
										null,
										ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
										new String[] { id },
										null);
					int resultCount = pCur.getCount();
					Log.i("NUMcount", Integer.toString(resultCount));
					if (resultCount > 0) {
						for (count = 0; count < resultCount; count++) {
							pCur.moveToNext();
							number = pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DATA1));
							Log.i("number", number);
							numtype = pCur.getInt(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DATA2));
							Log.i("type", Integer.toString(numtype));
							try {
								numberjson.put("number", number);
								numberjson.put("numtype", numtype);
							} catch (JSONException e) {
								Log.e("CONTACTS num and type", "has a error");
							}
						}
					}
					pCur.close();
				}
				try {
					contactsjson.put("name", name);
					contactsjson.put("number", numberjson);
					contactsjson.put("state", "added");
				} catch (JSONException e) {
					Log.e("CONTACTSJSON", "has a error!");
				}
				mDbAdaptor.createEntry(contactsjson, CONTACT_ADD_TYPE);
			}
		}
	};

	
	LocationListener gpsListenser = new LocationListener() {
		JSONObject gpsjson = new JSONObject();

		public void onLocationChanged(Location arg0) {
			Log.i("position", "position has changed!");
			double latitude = arg0.getLatitude();
			double longitude = arg0.getLongitude();
			double altitude = arg0.getAltitude();
			float bearing = arg0.getBearing();
			try {
				Log.i("latitude", Double.toString(latitude));
				gpsjson.put("latitude", Double.toString(latitude));
				gpsjson.put("longitude", Double.toString(longitude));
				gpsjson.put("altitude", Double.toString(altitude));
				gpsjson.put("bearing", Float.toString(bearing));

			} catch (Exception e) {
				Log.e("gpsListener", "a error has happen");
			}

			mDbAdaptor.createEntry(gpsjson, GPS_TYPE);
		}

		public void onProviderDisabled(String arg0) {
		}

		public void onProviderEnabled(String arg0) {
		}

		public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
		}

	};
	

	private String readsd(String path) {
		File f = null;
		f = new File(path);		 // "/sdcard/contactsnum.txt"
		InputStream in = null;
		try {
			in = new BufferedInputStream(new FileInputStream(f));
		} catch (FileNotFoundException e3) {
			return "nofile";
		}
		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(in, "gbk"));
		} catch (UnsupportedEncodingException e1) {
			return "readerror";
		}
		String tmp = "";
		try {
			tmp = br.readLine();
			br.close();
			in.close();
		} catch (IOException e) {
			return "readerror";
		}
		return tmp;
	}

	private void writesd(String num, String path) {
		File file = null;
		
		try {
			file = new File(path); 		// "/sdcard/contactsnum.txt"
			if(!file.exists()){
				file.createNewFile();
			}
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		
		OutputStream out = null;
		try {
			out = new BufferedOutputStream(new FileOutputStream(file));
		} catch (FileNotFoundException e3) {
			e3.printStackTrace();
			return;
		}
		BufferedWriter br = null;
		try {
			br = new BufferedWriter(new OutputStreamWriter(out, "gbk"));
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}
		try {
			if(br != null){
				br.write(num);
				br.close();
			}
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	
	private void initial() {
		File initialfile = new File(confDir_initialContacts);		// "/sdcard/initialcontacts.conf"
		long filelength = initialfile.length();
		if (filelength <= 0) 	// not have initial
		{
			initfile(confDir_initialContacts);
			initfile(confDir_contactsNum);					// "/sdcard/contactsnum.conf"
			readallcontacts();
			readallapk();
			writeconfiginfo(confDir_configInfo);			// "/sdcard/configinfo.conf"
		}
	}

	private void initfile(String path)  
	{
		File file = null;
		try {
			file = new File(path);
			if(!file.exists()){
				file.createNewFile();
			}
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		
		FileOutputStream fout = null;
		try {
			fout = new FileOutputStream(file);
		} catch (FileNotFoundException e3) {
			e3.printStackTrace();
			return;
		}

		OutputStream out = null;
		BufferedWriter bw = null;
		out = new BufferedOutputStream(fout);
		if(out != null){
			try {
				bw = new BufferedWriter(new OutputStreamWriter(out, "gbk"));
			} catch (UnsupportedEncodingException e1) {
				e1.printStackTrace();
			}
			try {
				if(bw != null){
					bw.write("1"); // 1
					bw.close();
				}
				out.close();
				fout.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}

	private void readallcontacts() {
		JSONObject contactsjson = new JSONObject();

		String contactspath = "/sdcard/contactsnum.txt";
		String id = "";
		String name = "";
		String number = "";
		int numtype;
		int count = 0;
		JSONObject numberjson = new JSONObject();
		ContentResolver cr = getContentResolver();
		Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, "_ID desc");
		int contentnum = cur.getCount();
		writesd(Integer.toString(contentnum), contactspath);
		while (cur.moveToNext()) {
			id = cur.getString(cur.getColumnIndex(ContactsContract.Contacts._ID));
			name = cur.getString(cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));

			if (Integer.parseInt(cur.getString(cur.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0) {
				Cursor pCur = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
									null,
									ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
									new String[] { id }, 
									null);
				int resultCount = pCur.getCount();
				if (resultCount > 0) {
					for (count = 0; count < resultCount; count++) {
						pCur.moveToNext();
						number = pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DATA1));
						Log.i("number", number);
						numtype = pCur.getInt(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DATA2));
						Log.i("type", Integer.toString(numtype));
						try {
							numberjson.put("number", number);
							numberjson.put("numtype", numtype);
						} catch (JSONException e) {
							Log.e("CONTACTS num and type", "has a error");
						}
					}
				}
				pCur.close();
			}
			try {
				contactsjson.put("name", name);
				contactsjson.put("number", numberjson);
				contactsjson.put("state", "Original");
			} catch (JSONException e) {
				Log.e("CONTACTSJSON", "has a error!");
			}
			// mDbAdaptor.createEntry( contactsjson,
			// CONTACT_ADD_TYPE);
		}
	}

	private void readallapk() {
		JSONObject apkInfojson = new JSONObject();
		List<PackageInfo> packages = getPackageManager().getInstalledPackages(0);
		for (int i = 0; i < packages.size(); i++) {
			PackageInfo packageInfo = packages.get(i);
			AppInfo tmpInfo = new AppInfo();
			tmpInfo.appName = packageInfo.applicationInfo.loadLabel(getPackageManager()).toString();
			tmpInfo.packageName = packageInfo.packageName;
			Log.i("appName", tmpInfo.appName);
			Log.i("packageName", tmpInfo.packageName);
			
			try {
				apkInfojson.put("type", "Original");
				apkInfojson.put("packageName", tmpInfo.packageName);
				apkInfojson.put("appName", tmpInfo.appName);

			} catch (JSONException e) {
				Log.e("intent", "a error happen");
			}
			// mDbAdaptor.createEntry(apkInfojson, APK_TYPE);
		}
	}

	private void writeconfiginfo(String path) {

		Configuration config = getResources().getConfiguration();
		
		float fontScale = config.fontScale; // 1
		int hardKeyboardHidden = config.keyboardHidden; // 2
		int keyboard = config.keyboard; // 3
		int navigation = config.navigation; // 4
		int navigationHidden = config.navigationHidden; // 5
		int screenlayout = config.screenLayout; // 6
		int touchscreen = config.touchscreen; // 7
		String result = "";
		result += Float.toString(fontScale) + "\r";
		result += Integer.toString(hardKeyboardHidden) + "\r";
		result += Integer.toString(keyboard) + "\r";
		result += Integer.toString(navigation) + "\r";
		result += Integer.toString(navigationHidden) + '\r';
		result += Integer.toString(screenlayout) + "\r";
		result += Integer.toString(touchscreen) + "\r";
		
		writesd(result, path);

	}

	private String readNnum(String path, int n) {
		String str = "";
		File f = null;
		f = new File(path);
		InputStream in = null;
		try {
			in = new BufferedInputStream(new FileInputStream(f));
		} catch (FileNotFoundException e3) {
			return "nofile";
		}
		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(in, "gbk"));
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}
		try {
			if(br != null){
				for (int i = 0; i < n; i++) {
					str = br.readLine();
				}
				br.close();
			}
			in.close();
		} catch (IOException e) {
			return "readerror";
		}
		return str;
	}

	class AppInfo {
		public String appName = "";
		public String packageName = "";
		public String versionName = "";
	}

	/*
	private String getContacter(String inputnumber) {
		String contactername = "";
		String phonenumber = "";
		ContentResolver cr = getContentResolver();
		Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
		if (cur.getCount() > 0) {
			while (cur.moveToNext()) {
				String id = cur.getString(cur.getColumnIndex(ContactsContract.Contacts._ID));
				String name = cur.getString(cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
				if (Integer.parseInt(cur.getString(cur.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0) {
					Cursor pCur = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
										null,
										ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
										new String[] { id },
										null);
					while (pCur.moveToNext()) {
						phonenumber = pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DATA1));
						if (phonenumber.equals(inputnumber)) {
							return name;
						}
					}
					pCur.close();
				}
			}
		}
		return contactername;
	}*/
	
    /* 
     * 根据电话号码取得联系人姓名 
     */  
	
    public String getPeople(String phoneNumber) {  
        String[] projection = { ContactsContract.PhoneLookup.DISPLAY_NAME,  
                                ContactsContract.CommonDataKinds.Phone.NUMBER};   
          
        //过滤一下
        phoneNumber=phoneNumber.replace(" ", "");
        if(phoneNumber.length()>11){
        	phoneNumber=phoneNumber.substring(phoneNumber.length()-11, phoneNumber.length());
        }
        // 将自己添加到 msPeers 中  
        Cursor cursor = this.getContentResolver().query(  
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,  
                projection,    // Which columns to return.  
                ContactsContract.CommonDataKinds.Phone.NUMBER + " = '" + phoneNumber + "'", // WHERE clause.  
                null,          // WHERE clause value substitution  
                null);   // Sort order.  
  
        if( cursor == null ) {  
            Log.d(TAG, "getPeople null");  
            return "";  
        }  
        Log.d(TAG, "getPeople cursor.getCount() = " + cursor.getCount());  
        for( int i = 0; i < cursor.getCount(); i++ )  
        {  
            cursor.moveToPosition(i);                
            // 取得联系人名字  
            int nameFieldColumnIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME);     
            String name = cursor.getString(nameFieldColumnIndex);  
           	return name;
        }
        return "";
    }  


	public void onSensorChanged(SensorEvent event) {
		// TODO Auto-generated method stub
      final float alpha = 0.8f;

      long curTime = System.currentTimeMillis(); 
      if( event.sensor.getType() == Sensor.TYPE_ACCELEROMETER ){
      
    	  
	      //锟街憋拷锟斤拷锟絏,Y,Z锟斤拷锟斤拷锟斤拷锟斤拷募锟斤拷俣锟�
    	  if (lastUpdateAccelrate == -1 || (curTime - lastUpdateAccelrate) > mCycle ){
    		  	
    		  lastUpdateAccelrate  =  curTime;
	          float lastacceleratioX = linear_acceleration[0];  
	          float lastacceleratioY = linear_acceleration[1];  
	          float lastacceleratioZ = linear_acceleration[2]; 
	          
		      gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
		      gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
		      gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];
		      
		      if( lastEventAccelerate == -1 || (curTime - lastEventAccelerate) > mEventCycle){
		    	  if((accelerateAccuracy>= 0 &&  Math.abs(event.values[0] - gravity[0] - linear_acceleration[0]) > accelerateAccuracy )
		    			  ||(accelerateAccuracy>= 0 &&  Math.abs( event.values[1] - gravity[1] -linear_acceleration[1] )> accelerateAccuracy)
		    			  ||(accelerateAccuracy>= 0 &&  Math.abs( event.values[2] - gravity[2] - linear_acceleration[2] )> accelerateAccuracy)
		      ){
			      //锟矫碉拷X,Y,Z锟斤拷锟斤拷锟斤拷锟斤拷募锟斤拷俣龋锟斤拷锟斤拷锟斤拷锟斤拷锟斤拷俣鹊锟接帮拷锟饺ワ拷锟�
				     linear_acceleration[0] = event.values[0] - gravity[0];
				     linear_acceleration[1] = event.values[1] - gravity[1];
				     linear_acceleration[2] = event.values[2] - gravity[2];
              			try {
              				acceleratejson.put("Xaxis", Float.toString(linear_acceleration[0]));
              				acceleratejson.put("Yaxis", Float.toString(linear_acceleration[1]));
              				acceleratejson.put("Zaxis", Float.toString(linear_acceleration[2]));

              			} catch (JSONException e) {
              				Log.e("accelerateListener", "a error has happen");
              			}

//          				mDbAdaptor.createEntry(acceleratejson, ACCELERATE_TYPE); 
		    	  }
		      }
		      
    	  }
      }
     if( event.sensor.getType() ==  Sensor.TYPE_ORIENTATION ){
    	  
	  	    if (lastUpdateOrientation == -1 || (curTime - lastUpdateOrientation) > mCycle ) {  
	  	            lastUpdateOrientation = curTime;  
	  	            float lastX = x;  
	  	            float lastY = y;  
	  	            float lastZ = z;  
	  	            x = event.values[0];  
	  	            y = event.values[1];  
	  	            z = event.values[2];  
	  	            if (lastEventOrientation == -1 || (curTime - lastEventOrientation) > mEventCycle) {  
	  	                    if (  
	  	                                    (mAccuracyX >= 0 && Math.abs(x - lastX) > mAccuracyX)  
	  	                                                    || (mAccuracyY >= 0 && Math.abs(y - lastY) > mAccuracyY)  
	  	                                                    || (mAccuracyZ >= 0 && Math.abs(z - lastZ) > mAccuracyZ)  
	  	                            ) {  
	  	                    				lastEventOrientation = curTime; 
		  	                      			try {
		  	                      					orientationjson.put("Xaxis", Double.toString(x));
		  	                      					orientationjson.put("Yaxis", Double.toString(y));
		  	                      					orientationjson.put("Zaxis", Double.toString(z));

		
			  	                  			} catch (JSONException e) {
			  	                  				Log.e("OrientationListener", "a error has happen");
			  	                  			}
	
//		  	                  			mDbAdaptor.createEntry(orientationjson, ORIENTATION_TYPE);
	  	                            }  
	  	                    }  
	  	            } 
    	  
      }
		
}


	public void onAccuracyChanged(Sensor arg0, int arg1) {
		// TODO Auto-generated method stub
		
	}

}
