package com.quest.mobileocr;



import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;

import com.abbyy.mobile.ocr4.AssetDataSource;
import com.abbyy.mobile.ocr4.DataSource;
import com.abbyy.mobile.ocr4.Engine;
import com.abbyy.mobile.ocr4.FileLicense;
import com.abbyy.mobile.ocr4.License;
import com.abbyy.mobile.ocr4.RecognitionConfiguration;
import com.abbyy.mobile.ocr4.RecognitionManager;
import com.abbyy.mobile.ocr4.RecognitionManager.RecognitionCallback;
import com.abbyy.mobile.ocr4.RecognitionManager.RotationType;
import com.abbyy.mobile.ocr4.layout.MocrPrebuiltLayoutInfo;
import com.quest.mobileocr.RecognitionContext.RecognitionTarget;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

//Include the abbyy libraries or other files

public class AbbyyPlugin implements RecognitionCallback {

	private Uri _imageUri;
	private static final String _licenseFile = "license";
	private static final String _applicationID = "Android_ID";

	private static final String _patternsFileExtension = ".mp3";
	private static final String _dictionariesFileExtension = ".mp3";
	private static final String _keywordsFileExtension = ".mp3";

    private static String FILE_SUBDIRECTORY = "Q_MOBILE_OCR";

    private static File fileDirectory;

    private MainActivity activity;


     public AbbyyPlugin(MainActivity activity){
        this.activity = activity;
    	initDevice();
        initStorage();
      }

    public File getFileStorage(){
        return fileDirectory;
    }

    private void initStorage() {
        fileDirectory = new File(Environment.getExternalStorageDirectory(), FILE_SUBDIRECTORY);
        if (!fileDirectory.exists()) fileDirectory.mkdirs();
    }

	//TODO: Initialization
	private void initDevice(){
		try {
			Context context = activity.getApplicationContext();
			final DataSource assetDataSource = new AssetDataSource( activity.getAssets()  );
			final List<DataSource> dataSources = new ArrayList<DataSource>();
			dataSources.add( assetDataSource );

			Engine.loadNativeLibrary();
			FileLicense license = new FileLicense( 
				assetDataSource,
				this._licenseFile,
				this._applicationID );
			Engine.DataFilesExtensions ext = new Engine.DataFilesExtensions( 
				this._patternsFileExtension,
				this._dictionariesFileExtension,
				this._keywordsFileExtension );
			Engine.createInstance( dataSources, license, ext);

			RecognitionContext.createInstance(context);

		} catch( final License.BadLicenseException e ) {
		} catch(Exception e) {
            e.printStackTrace();
			System.err.println("AbbyOCRPlugin.initDevice();; Error: "+e.getMessage());
		}
	}

	private Uri saveImage( String base64img) {
		//Build path for image
		String path = Environment.getExternalStorageDirectory().toString();
		File file  = null;
		try {
			//Create the file & stream object
			file = new File(path, "test.jpg");
			FileOutputStream imageOutFile = new FileOutputStream(file);

			//Convert base64 into bytes
			byte[] imageByteArray = Base64.decode(base64img, Base64.DEFAULT);
	        System.err.println("imageByteArray.length: " + imageByteArray.length);
	        // Write a image byte array into file system
	        imageOutFile.write(imageByteArray);
	        imageOutFile.close();

	       /* MediaStore.Images.Media.insertImage( 
	        	cordova.getActivity().getContentResolver(),
	        	file.getAbsolutePath(),
	        	file.getName(),
	        	file.getName()
	        );*/
	    } catch (IOException ioexception) {

	    }
        return Uri.parse(file.getAbsolutePath());
	}

    public String recogniseText(String uri){
        try {
			//Get the image uri argument from the json.
			_imageUri = Uri.parse(uri);
			if (_imageUri == null) {
				throw new IllegalArgumentException("Missing image uri");
			} else {
				System.err.println("uri is not null");
			}
            //Change to synchronous method
            startRecognition();

            return startRecognition();
        } catch(Exception e) {
            System.err.println("Exception: "+e.getMessage());
            return "Error: performOCR();;"+e.getMessage();
        }
    }

	public String recogniseText(Bitmap image){
		try {
//			//Get the image uri argument from the json.
//			_imageUri = Uri.parse(uri);
//			if (_imageUri == null) {
//				throw new IllegalArgumentException("Missing image uri");
//			} else {
//				System.err.println("uri is not null");
//			}

			//Change to synchronous method
			startRecognition(image);

			return startRecognition(image);
		} catch(Exception e) {
			System.err.println("Exception: "+e.getMessage());
			return "Error: performOCR();;"+e.getMessage();
		}
	}

	public void onPrebuiltWordsInfoReady( final MocrPrebuiltLayoutInfo layoutInfo ) {
		//Do nothing for now
	}

	public boolean onRecognitionProgress( int percentage, int warningCode ) {
		return false;
	}

	public void onRotationTypeDetected( final RotationType rotationType ) {
		//Do nothing for now
	}

    private String startRecognition(Bitmap image){
        //Recognition context - get from cordova
        Context context = activity.getApplicationContext();

        //Recognition configs
        final RecognitionConfiguration recognitionConfiguration = new RecognitionConfiguration();

        //Image processing options
        int imageProcessingOptions = RecognitionConfiguration.ImageProcessingOptions.PROHIBIT_VERTICAL_CJK_TEXT;
        imageProcessingOptions |= RecognitionConfiguration.ImageProcessingOptions.DETECT_PAGE_ORIENTATION;

        //Set the img proc options of the config
        recognitionConfiguration.setImageProcessingOptions( imageProcessingOptions );

        //Set recognition mode - FULL vs FAST
        recognitionConfiguration.setRecognitionMode( RecognitionConfiguration.RecognitionMode.FULL );
        recognitionConfiguration.setRecognitionLanguages( RecognitionContext
                .getRecognitionLanguages( RecognitionTarget.TEXT ) );


        // Get a recognition manager from the Engine.
        final RecognitionManager recognitionManager =
                Engine.getInstance().getRecognitionManager( recognitionConfiguration );


        if (image != null) {
            System.err.println("Image is retrieved");
        }

        // Reset the stored rotation type value
        RecognitionContext.setRotationType( RotationType.NO_ROTATION );

        String result = null;
        try {
            Object resultObj = recognitionManager.recognizeText( image, this );
            if (resultObj == null) {
                throw new NullPointerException("resultObj is null");
            }

            result = resultObj.toString();


        } catch( final Throwable exception ) {
            //Will need error handling- remove loggin for now
            Log.w( "AbbyyPlugin", "Failed to recognize image", exception );

            //Return the exception
            result = "!#error!#";
        } finally {
            try {
                //Will need to close.
                recognitionManager.close();
            } catch( final IOException e ) {

            }
            //Return the result
            return result;
        }
    }

	private String startRecognition() {
		//Recognition context - get from cordova
		Context context = activity.getApplicationContext();

		//Recognition configs
		final RecognitionConfiguration recognitionConfiguration = new RecognitionConfiguration();

		//Image processing options
		int imageProcessingOptions = RecognitionConfiguration.ImageProcessingOptions.PROHIBIT_VERTICAL_CJK_TEXT;
		imageProcessingOptions |= RecognitionConfiguration.ImageProcessingOptions.DETECT_PAGE_ORIENTATION;

		//Set the img proc options of the config
		recognitionConfiguration.setImageProcessingOptions( imageProcessingOptions );

		//Set recognition mode - FULL vs FAST
		recognitionConfiguration.setRecognitionMode( RecognitionConfiguration.RecognitionMode.FULL );
		recognitionConfiguration.setRecognitionLanguages( RecognitionContext
				.getRecognitionLanguages( RecognitionTarget.TEXT ) );


		// Get a recognition manager from the Engine.
		final RecognitionManager recognitionManager =
				Engine.getInstance().getRecognitionManager( recognitionConfiguration );

		// Get bitmap of image from recognition context.
		final Bitmap image = RecognitionContext.getImage( this._imageUri );

		if (image != null) {
			System.err.println("Image is retrieved");
		}

		// Reset the stored rotation type value
		RecognitionContext.setRotationType( RotationType.NO_ROTATION );

		String result = null;
		try {
			Object resultObj = recognitionManager.recognizeText( image, this );
			if (resultObj == null) {
				throw new NullPointerException("resultObj is null");
			}

			result = resultObj.toString();


		} catch( final Throwable exception ) {
			//Will need error handling- remove loggin for now
			Log.w( "AbbyyPlugin", "Failed to recognize image", exception );

			//Return the exception
			result = "!#error!#";
		} finally {
			try {
				//Will need to close.
				recognitionManager.close();
			} catch( final IOException e ) {

			}
			//Return the result
			return result;
		}
	}

}