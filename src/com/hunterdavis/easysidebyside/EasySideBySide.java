package com.hunterdavis.easysidebyside;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.ads.AdRequest;
import com.google.ads.AdView;

public class EasySideBySide extends Activity {

	Uri selectedImageUri1 = null;
	Uri selectedImageUri2 = null;
	public Bitmap scaled1 = null;
	public Bitmap scaled2 = null;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		// grab a view to the left and right images and load 1 and 2 png
		ImageView imgView1 = (ImageView) findViewById(R.id.ImageView01);
		ImageView imgView2 = (ImageView) findViewById(R.id.ImageView02);
		imgView1.setImageResource(R.drawable.one);
		imgView2.setImageResource(R.drawable.two);

		// photo on click listener
		imgView1.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent();
				intent.setType("image/*");
				intent.setAction(Intent.ACTION_GET_CONTENT);
				startActivityForResult(
						Intent.createChooser(intent, "Select Picture"), 2);

			}

		});

		// photo on click listener
		imgView2.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent();
				intent.setType("image/*");
				intent.setAction(Intent.ACTION_GET_CONTENT);
				startActivityForResult(
						Intent.createChooser(intent, "Select Picture"), 3);

			}

		});

		// Create an anonymous implementation of OnClickListener
		OnClickListener saveButtonListner = new OnClickListener() {
			public void onClick(View v) {
				// do something when the button is clicked
				if ((scaled1 != null) && (scaled2 != null)) {
					saveDoubleImage(v.getContext());
				}
				else
				{
					Toast.makeText(v.getContext(), "Please Select 2 Images ",
							Toast.LENGTH_LONG).show();
				}

			}
		};

		Button saveButton = (Button) findViewById(R.id.saveButton);
		saveButton.setOnClickListener(saveButtonListner);

		// Look up the AdView as a resource and load a request.
		AdView adView = (AdView) this.findViewById(R.id.adView);
		adView.loadAd(new AdRequest());

	}

	public Boolean saveDoubleImage(Context context) {
		int rgbSize = 2 * 300 * 400;
		int[] rgbValues = new int[rgbSize];
		for (int i = 0; i < rgbSize; i++) {
			rgbValues[i] = calculatePixelValue(i, 300, 400);
		}

		// create a width*height bitmap
		BitmapFactory.Options staticOptions = new BitmapFactory.Options();
		// staticOptions.inSampleSize = 2;
		Bitmap staticBitmap = Bitmap.createBitmap(rgbValues, 600, 400,
				Bitmap.Config.RGB_565);

		// now save out the file holmes!
		OutputStream outStream = null;
		String newFileName = null;
		String extStorageDirectory = Environment.getExternalStorageDirectory()
				.toString();

		String[] projection = { MediaStore.Images.ImageColumns.DISPLAY_NAME /* col1 */};
		Cursor c = context.getContentResolver().query(selectedImageUri1,
				projection, null, null, null);
		if (c != null && c.moveToFirst()) {
			String oldFileName = c.getString(0);
			int dotpos = oldFileName.lastIndexOf(".");
			if (dotpos > -1) {
				newFileName = oldFileName.substring(0, dotpos) + "-";
			}
		}

		c = context.getContentResolver().query(selectedImageUri2, projection,
				null, null, null);
		if (c != null && c.moveToFirst()) {
			String oldFileName = c.getString(0);
			int dotpos = oldFileName.lastIndexOf(".");
			if (dotpos > -1) {
				newFileName += oldFileName.substring(0, dotpos) + ".png";
			}
		}

		if (newFileName != null) {
			File file = new File(extStorageDirectory, newFileName);
			try {
				outStream = new FileOutputStream(file);
				staticBitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream);
				try {
					outStream.flush();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return false;
				}
				try {
					outStream.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return false;
				}

				Toast.makeText(context, "Saved " + newFileName,
						Toast.LENGTH_LONG).show();
				new SingleMediaScanner(context, file);

			} catch (FileNotFoundException e) {
				// do something if errors out?
				return false;
			}
		}
		return true;

	}

	public int calculatePixelValue(int location, int width, int height) {
		// get our x and y location
		int xLocation = (int) location % (width * 2);
		int yLocation = (int) Math.floor(location / (width * 2));
		if (xLocation >= width) {
			int xAdjustedLocation = xLocation - width;
			//Log.v("scaling", "x=" + xAdjustedLocation +", y="+yLocation);
			return scaled2.getPixel(xAdjustedLocation, yLocation);
		}

		//Log.v("scaling", "x=" + xLocation +", y="+yLocation);
		return scaled1.getPixel(xLocation, yLocation);

	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK) {
			if (requestCode == 2) {
				selectedImageUri1 = data.getData();

				// grab a handle to the image
				ImageView imgPreView = (ImageView) findViewById(R.id.ImageView01);
				scaled1 = scaleURIAndDisplay(getBaseContext(),
						selectedImageUri1, imgPreView);

			} else if (requestCode == 3) {
				selectedImageUri2 = data.getData();

				// grab a handle to the image
				ImageView imgPreView = (ImageView) findViewById(R.id.ImageView02);
				scaled2 = scaleURIAndDisplay(getBaseContext(),
						selectedImageUri2, imgPreView);

			}

		}
	}

	public Bitmap scaleURIAndDisplay(Context context, Uri uri, ImageView imgview) {
		InputStream photoStream;
		try {
			photoStream = context.getContentResolver().openInputStream(uri);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inSampleSize = 2;
		Bitmap photoBitmap;

		photoBitmap = BitmapFactory.decodeStream(photoStream, null, options);
		imgview.setImageBitmap(photoBitmap);
		if (photoBitmap == null) {
			return null;
		}

		Bitmap scaled = Bitmap.createScaledBitmap(photoBitmap, 300, 400, true);
		photoBitmap.recycle();
		imgview.setImageBitmap(scaled);
		return scaled;
	}

}