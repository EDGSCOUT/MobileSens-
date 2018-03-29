package wsi.survey;

import java.util.HashSet;

//import org.acra.ReportingInteractionMode;
//import org.acra.annotation.ReportsCrashes;

import android.accounts.Account;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.widget.Toast;


public class MobileApplication extends Application {

	public static SharedPreferences mPref; 
	public static Context mContext;
	@Override
	public void onCreate() {
		super.onCreate();
		mContext = this.getApplicationContext();
		mPref = PreferenceManager.getDefaultSharedPreferences(this);
	}
	
	
	
}
