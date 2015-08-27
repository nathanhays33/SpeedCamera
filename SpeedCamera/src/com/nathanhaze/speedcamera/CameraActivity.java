package com.nathanhaze.speedcamera;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.android.gms.ads.*;
import com.nathanhaze.cameraspeed.R;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.Paint.Style;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.Switch;



public class CameraActivity extends Activity implements LocationListener   {

	static ProgressDialog pd;
	volatile  Handler mapHandler = new Handler();
	
    private static Camera mCamera;
    private static CameraPreview mPreview;
    
    static Location location;
    static LocationListener locationListener;
	static LocationManager locationManager;
        
    static Context context;
    
    static GlobalVar gv = new GlobalVar();
    
    static ImageView myImage;
    static File imgFile = null;
    
	private InterstitialAd interstitial;
	
	SharedPreferences sharedPrefs;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_preview);

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
     // Register the listener with the Location Manager to receive location updates
        
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        
        location = getLastKnownLocation();
	    // getting GPS status
        Boolean isGPSEnabled = locationManager
                .isProviderEnabled(LocationManager.GPS_PROVIDER);
        if(!isGPSEnabled){
        	showSettingsAlert();
        }
        mCamera = getCameraInstance();

        context = getApplicationContext();
        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);

        //speedBackground = (LinearLayout)findViewById(R.id.speedBackgound);
        
        myImage = (ImageView) findViewById(R.id.lastPic);
        myImage.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				
				Intent intent = new Intent();
				intent.setAction(Intent.ACTION_VIEW);
				if(imgFile == null){
					 CharSequence text = "This is only an icon, take a photo"; 
                 	 Toast toast = Toast.makeText(context, text, Toast.LENGTH_LONG);
                 	 toast.show();
				}
				else{
				intent.setDataAndType(Uri.parse("file://" +  imgFile.getAbsolutePath()), "image/*");
				startActivity(intent);
				}
				return false;
			}
        });
        
        /*
        // Prepare the Interstitial Ad
        interstitial = new InterstitialAd(CameraActivity.this);
        // Insert the Ad Unit ID
        interstitial.setAdUnitId("ca-app-pub-2377934805759836/7573073366");
 
        // Request for Ads
        AdRequest adRequest = new AdRequest.Builder()
        // Add a test device to show Test Ads
     //    .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
     //    .addTestDevice("5E39C82DA23AB651436D5DA0866A484D")
                .build();
 
        // Load ads into Interstitial Ads
        interstitial.loadAd(adRequest);
 
        // Prepare an Interstitial Ad Listener
        interstitial.setAdListener(new AdListener() {
            public void onAdLoaded() {
                // Call displayInterstitial() function
                displayInterstitial();
            }
        });
 */       
        //set up preference
        try {
            sharedPrefs = PreferenceManager
                    .getDefaultSharedPreferences(this);
        } catch (NullPointerException e) {
            sharedPrefs = null;
        }

        boolean hasFlash = this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
        if(!hasFlash){
        	((Switch)findViewById(R.id.togglebutton)).setVisibility(View.GONE);
        }else{
        	((Switch)findViewById(R.id.togglebutton)).setOnCheckedChangeListener(new OnCheckedChangeListener() {
        	    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        	        Parameters param = mCamera.getParameters(); 
        	        if (isChecked) {
        	            param.setFlashMode(Parameters.FLASH_MODE_ON);
        	        } else {
        	            param.setFlashMode(Parameters.FLASH_MODE_OFF);
        	        }  
        	        mCamera.setParameters(param);

        	    }
        	});
        }

    }
    
    public void onToggleClicked(View view) {
        // Is the toggle on?
  
        /*
        mCamera.getParameters().setGpsLatitude(location.getLatitude());
        mCamera.getParameters().setGpsLongitude(location.getLongitude());
        mCamera.getParameters().setGpsAltitude(location.getAltitude());
        mCamera.getParameters().setGpsTimestamp(location.getTime());
        */
    }
    
    
	/** A safe way to get an instance of the Camera object. */
	public static Camera getCameraInstance(){
	    Camera c = null;
	    try {
	        c = Camera.open(); // attempt to get a Camera instance
	    }
	    catch (Exception e){
	        // Camera is not available (in use or does not exist)
	    }
	    return c; // returns null if camera is unavailable
	}
	
	public void takePhoto(View v){
	   	pd = ProgressDialog.show(this, "Saving" , "");
	    Thread t = new Thread() {
	        public void run() {
	            mCamera.takePicture(null, null, mPicture);
	        }
	      };
	      t.start();
	    //mCamera.startPreview();
	}
	
	private PictureCallback mPicture = new PictureCallback() {

	    @SuppressLint("NewApi")
		@Override
	    public void onPictureTaken(byte[] data, Camera camera) {
	        File pictureFile = getOutputMediaFile(1);

	        if (pictureFile == null){
	            Log.d("ERROR", "Error creating media file, check storage permissions:" );
	        }

	        try {
	            FileOutputStream fos = new FileOutputStream(pictureFile);
	            fos.write(data);
	            fos.close();
	            
	        } catch (FileNotFoundException e) {
	            Log.d("ERROR", "File not found: " + e.getMessage());
	        } catch (IOException e) {
	            Log.d("ERROR", "Error accessing file: " + e.getMessage());
	        }
	        
	        Bitmap myBitmap = null;
	        if (android.os.Build.VERSION.SDK_INT>=android.os.Build.VERSION_CODES.HONEYCOMB) {
	        	 BitmapFactory.Options opt = new BitmapFactory.Options();
	        	 opt.inMutable = true;
	        	 
	        	 
	        	 opt.inJustDecodeBounds = false;
	        	 opt.inPreferredConfig = Config.RGB_565;
	        	 opt.inDither = true;
	        	 
	        	 
	        	 myBitmap = BitmapFactory.decodeFile(pictureFile.getAbsolutePath(), opt);
	        	 myBitmap = timestampItAndSave(myBitmap);
	        }
	        else{
	        	myBitmap = BitmapFactory.decodeFile(pictureFile.getAbsolutePath());
	        	myBitmap = convertToMutable(context, myBitmap);
	        }
	        
	        
	        if(myBitmap ==null){

	        	 myBitmap = BitmapFactory.decodeFile(pictureFile.getAbsolutePath());
	        	 myBitmap = timestampItAndSaveOLD(myBitmap);
	        }
	        
		//	Bitmap myBitmap = BitmapFactory.decodeFile(pictureFile.getAbsolutePath());
		//	Bitmap second = timestampItAndSave(myBitmap);

			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			myBitmap.compress(CompressFormat.JPEG, 97, bos);
			
			byte[] bitmapdata = bos.toByteArray();
			
			//write the bytes in file
			FileOutputStream fos = null;
			try {
				fos = new FileOutputStream(pictureFile);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				fos.write(bitmapdata);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				fos.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	        setImage();

	        setImage();
	        pd.dismiss();
		    mapHandler.post(Success);
	        mCamera.startPreview();
	    }
	};
	
	/** Create a File for saving an image or video */
	private File getOutputMediaFile(int type){

		/*
        Boolean isSDPresent = android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);
        if(!isSDPresent)
        {	
        	int duration = Toast.LENGTH_LONG;

        	Toast toast = Toast.makeText(context, "card not mounted", duration);
        	toast.show();
        	
        	Log.d("ERROR", "Card not mounted");
        }
        */
	    File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "CameraSpeed");
	    
	    if (! mediaStorageDir.exists()){
	        if (! mediaStorageDir.mkdirs()){
	        	
	            Log.d("MyCameraApp", "failed to create directory");
	            return null;
	        }
	    }

	    // Create a media file name
	    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
	    File mediaFile;
	    if (type == 1){
	        mediaFile = new File(mediaStorageDir.getPath() + File.separator +
	        "IMG_"+ timeStamp + ".jpg");
	        imgFile = mediaFile;
	    } else {
	        return null;
	    }

	    scanMedia(mediaFile.getAbsolutePath());
	    return mediaFile;
	}

	/**
	 * Sends a broadcast to have the media scanner scan a file
	 * 
	 * @param path
	 *            the file to scan
	 */
	private  void scanMedia(String path) {
	    File file = new File(path);
	    Uri uri = Uri.fromFile(file);
	    Intent scanFileIntent = new Intent(
	            Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri);
	    sendBroadcast(scanFileIntent);
	}
	
	public static void setImage(){
        if(imgFile !=null){
	        if(imgFile.exists()){
				Bitmap myBitmap = decodeSampleImage(imgFile, 100, 100);
	            myImage.setImageBitmap(myBitmap);
	
	        }
        }
	}
	
	private Bitmap timestampItAndSave(Bitmap toEdit){
		Canvas canvas = new Canvas(toEdit);
	    Paint paint = new Paint();
	    paint.setAntiAlias(true);

	    paint.setColor(Color.WHITE);

	  
	    Typeface tf =Typeface.createFromAsset(getAssets(),
	            "fonts/DIGITALDREAMFATSKEW.ttf");
	    paint.setTypeface(tf);
	    paint.setShadowLayer(2.0f, 1.0f, 1.0f, Color.BLACK);
	    paint.setStyle(Style.FILL);

	    
	   int pictureHeight = toEdit.getHeight();
//	   int pictureWidth = toEdit.getHeight();

	   int fontSize  =Math.round(pictureHeight * (.06f));
	   paint.setTextSize(fontSize);
	  // paint.setAlpha(150);

	    String speedText  =  "";
	    
        if(sharedPrefs.getBoolean("useMetric", false)){
        	speedText = Integer.toString(gv.getSpeedMetric()) + " kph  " +
	    	             Integer.toString(gv.getAltitudeMetric()) +" meters";
        }else{
        	speedText= Integer.toString(gv.getSpeed()) + " mph  "+ 
	    		 Integer.toString(gv.getAltitude()) +" feet";
        }
 	    canvas.drawText(speedText, 30, fontSize +10 , paint);
 	   return toEdit;
	}
	
	/*
	 * Old version, can cause out of memory exception 
	 */
	private Bitmap timestampItAndSaveOLD(Bitmap toEdit){
		
		Bitmap dest = toEdit.copy(Bitmap.Config.ARGB_8888, true);		
	    Canvas canvas = new Canvas(dest);
	    Paint paint = new Paint();
	    paint.setAntiAlias(true);

	    paint.setColor(Color.WHITE);

	  
	    Typeface tf =Typeface.createFromAsset(getAssets(),
	            "fonts/DIGITALDREAMFATSKEW.ttf");
	    paint.setTypeface(tf);
	    paint.setShadowLayer(2.0f, 1.0f, 1.0f, Color.BLACK);
	    paint.setStyle(Style.FILL);

	    
	   int pictureHeight = toEdit.getHeight();
	//   int pictureWidth = toEdit.getHeight();

	   paint.setTextSize(pictureHeight * (.03f));
	  // paint.setAlpha(150);


 	    canvas.drawText(Integer.toString(
 	    		gv.getSpeed()) + " mph (" + Integer.toString(gv.getSpeedMetric()) + " kph) " +
 	    		Integer.toString(gv.getAltitude()) + " feet (" + Integer.toString(gv.getAltitudeMetric()) +" meters)"
 	    		, 10, pictureHeight -200, paint);
	    return dest;
	}
	

	
	 public void showSettingsAlert(){
	        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
	      
	        // Setting Dialog Title
	        alertDialog.setTitle("GPS is settings");
	  
	        // Setting Dialog Message
	        alertDialog.setMessage("GPS is not enabled. Do you want to go to settings menu?");
	  
	        // Setting Icon to Dialog
	        //alertDialog.setIcon(R.drawable.delete);
	  
	        // On pressing Settings button
	        alertDialog.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog,int which) {
	                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
	                startActivity(intent);
	            }
	        });
	  
	        // on pressing cancel button
	        alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog, int which) {
	                dialog.cancel();
	            }
	        });
	  
	        // Showing Alert Message
	        alertDialog.show();
	  }

    @Override
    protected void onPause() {
        super.onPause();
        releaseCamera();              // release the camera immediately on pause event
    }
    
    @Override
    protected void onStart() {
        super.onStart();  
    //    mCamera = getCameraInstance();
        EasyTracker.getInstance(this).activityStart(this);  // Add this method.

    }
    
    
    
    @Override
    protected void onResume() {
        super.onResume();  
        if(mCamera == null){
        mCamera = getCameraInstance();
        
        context = getApplicationContext();
        mPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);
        }
    }
    
    protected void onRestart(){
    	super.onRestart();
   // 	mCamera = getCameraInstance();
    }
    
    @Override
    protected void onStop() {
        super.onStop();
  //      releaseCamera();              // release the camera immediately on pause event
        EasyTracker.getInstance(this).activityStop(this);  // Add this method.
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
     //   releaseCamera();              // release the camera immediately on pause event
    }
    
    private void releaseCamera(){
        if (mCamera != null){
          //  mCamera.release();        // release the camera for other applications
          //  mCamera = null;
            
          //  mCamera.setPreviewCallback(null);
            mPreview.getHolder().removeCallback(mPreview);
            mCamera.release();
            mCamera = null;
            
        }
    }

	@Override
	public void onLocationChanged(Location location) {
		int speed  = (int) (2.23694f * location.getSpeed());
	    int alt = (int)(location.getAltitude() * 3.28084f);
	    
	      gv.setSpeed(speed);	
	      gv.setAltutude(alt);
	      gv.setSpeedMetric((int)(location.getSpeed() * 3.6));
	      gv.setAltutudeMetric((int)location.getAltitude());
	      
	      if(sharedPrefs.getBoolean("useMetric", false)){
		      ((TextView)findViewById(R.id.speed)).setText(gv.getSpeedMetric() + " kph");
		      ((TextView)findViewById(R.id.altitude)).setText(gv.getAltitudeMetric() +" meters");
	      }else{
		      ((TextView)findViewById(R.id.speed)).setText(speed + " mph ");
		      ((TextView)findViewById(R.id.altitude)).setText(alt + " feet ");
	      }

	      
	      Typeface tf =Typeface.createFromAsset(getAssets(),
		            "fonts/DIGITALDREAMFATSKEW.ttf");
	      ((TextView)findViewById(R.id.speed)).setTypeface(tf);
	      ((TextView)findViewById(R.id.altitude)).setTypeface(tf);
	      
	      ((LinearLayout)findViewById(R.id.speedBackgound)).
	               setBackgroundColor(Color.rgb(0 + (speed *3) ,255 - (speed *3),0));
	}

    @Override
    public void onProviderDisabled(String provider) {
   //   Toast.makeText(this, "GPS was Disabled",
    //      Toast.LENGTH_SHORT).show();
    }

	@Override
	public void onProviderEnabled(String provider) {
	    Toast.makeText(this, "Enabled new provider " + provider,
	            Toast.LENGTH_SHORT).show();	
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub
		
	}
	
	private Location getLastKnownLocation() {
	    List<String> providers = locationManager.getProviders(true);
	    Location bestLocation = null;
	    for (String provider : providers) {
	        Location l = locationManager.getLastKnownLocation(provider);
	        if (l == null) {
	            continue;
	        }
	        if (bestLocation == null
	                || l.getAccuracy() < bestLocation.getAccuracy()) {
	            bestLocation = l;
	        }
	    }
	    if (bestLocation == null) {
	        return null;
	    }
	    return bestLocation;
	}
	
	public static Bitmap decodeSampleImage(File f, int width, int height) {
	    try {
	        System.gc(); // First of all free some memory

	        // Decode image size

	        BitmapFactory.Options o = new BitmapFactory.Options();
	        o.inJustDecodeBounds = true;
	        BitmapFactory.decodeStream(new FileInputStream(f), null, o);

	        // The new size we want to scale to

	        final int requiredWidth = width;
	        final int requiredHeight = height;

	        // Find the scale value (as a power of 2)

	        int sampleScaleSize = 1;

	        while (o.outWidth / sampleScaleSize / 2 >= requiredWidth && o.outHeight / sampleScaleSize / 2 >= requiredHeight)
	            sampleScaleSize *= 2;

	        // Decode with inSampleSize

	        BitmapFactory.Options o2 = new BitmapFactory.Options();
	        o2.inSampleSize = sampleScaleSize;

	        return BitmapFactory.decodeStream(new FileInputStream(f), null, o2);
	    } catch (Exception e) {
	      //  Log.d(TAG, e.getMessage()); // We don't want the application to just throw an exception
	    }

	    return null;
	}

	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	public static Bitmap convertToMutable(final Context context, final Bitmap imgIn) {
	    final int width = imgIn.getWidth(), height = imgIn.getHeight();
	    final Config type = imgIn.getConfig();
	    File outputFile = null;
	    final File outputDir = context.getCacheDir();
	    try {
	        outputFile = File.createTempFile(Long.toString(System.currentTimeMillis()), null, outputDir);
	        outputFile.deleteOnExit();
	        final RandomAccessFile randomAccessFile = new RandomAccessFile(outputFile, "rw");
	        final FileChannel channel = randomAccessFile.getChannel();
	        final MappedByteBuffer map = channel.map(MapMode.READ_WRITE, 0, imgIn.getRowBytes() * height);
	        imgIn.copyPixelsToBuffer(map);
	        imgIn.recycle();
	        final Bitmap result = Bitmap.createBitmap(width, height, type);
	        map.position(0);
	        result.copyPixelsFromBuffer(map);
	        channel.close();
	        randomAccessFile.close();
	        outputFile.delete();
	        return result;
	    } catch (final Exception e) {
	    } finally {
	        if (outputFile != null)
	            outputFile.delete();
	    }
	    return null;
	}
	
	
	
	final Runnable Success = new Runnable() {
		   public void run() {
			    	
		   }
	  };
	  
	  public void loadAd(){
		    // Create the interstitial.
		    interstitial = new InterstitialAd(this);
		    interstitial.setAdUnitId("ca-app-pub-2377934805759836/3876184165");

		    // Create ad request.
		    AdRequest adRequest = new AdRequest.Builder().build();

		    // Begin loading your interstitial.
		    interstitial.loadAd(adRequest);
	  }
	  
	  // Invoke displayInterstitial() when you are ready to display an interstitial.
	  public void displayInterstitial() {
	    if (interstitial.isLoaded()) {
	      interstitial.show();
	    }
	  }
	 
	public void settings(View v){
		Intent intent = new Intent (this, CameraSettings.class);
		startActivity(intent);
	}
	
}