package com.nathanhaze.speedcamera;

import com.google.analytics.tracking.android.EasyTracker;
import com.nathanhaze.cameraspeed.R;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.CompoundButton;
import android.widget.Switch;

public class CameraSettings extends Activity {

	SharedPreferences sharedPrefs;
	SharedPreferences.Editor editor;
	    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);
		
		 //set up preference
        try {
            sharedPrefs = PreferenceManager
                    .getDefaultSharedPreferences(this);
        } catch (NullPointerException e) {
            sharedPrefs = null;
        }
        if(sharedPrefs !=null) {
            editor = sharedPrefs.edit();
        }

        ((Switch)findViewById(R.id.use_metric)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                editor.putBoolean("useMetric", isChecked);
                editor.commit();
            }
        });

        if(sharedPrefs.getBoolean("useMetric", false)){
            ((Switch)findViewById(R.id.use_metric)).setChecked(true);
        }
        
	}

	
    protected void onStop() {
        super.onStop();
        EasyTracker.getInstance(this).activityStop(this);  // Add this method.
    }

    @Override
    protected void onStart(){
        super.onStart();
        EasyTracker.getInstance(this).activityStart(this);  // Add this method.
    }
}
