package wsi.survey;

import wsi.mobilesens.R;
import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;

public class AboutActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.about);
		TextView versionTextView=(TextView)findViewById(R.id.id_version);		
		PackageManager pm = getPackageManager();
		try{
			PackageInfo pInfo=pm.getPackageInfo(getPackageName(), PackageManager.GET_CONFIGURATIONS);			
			String nowVersion = pInfo.versionName;
			versionTextView.setText(nowVersion);
		}
		catch (Exception e) {
			// TODO: handle exception
			versionTextView.setText("error");
		}
	}
	
	
}
