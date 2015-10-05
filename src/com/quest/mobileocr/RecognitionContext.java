// Copyright (c) ABBYY (BIT Software), 1993 - 2012. All rights reserved.
// Автор: Rozumyanskiy Michael

package com.quest.mobileocr;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;

import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.CancellationException;

import com.abbyy.mobile.ocr4.Engine;
import com.abbyy.mobile.ocr4.RecognitionConfiguration;
import com.abbyy.mobile.ocr4.RecognitionLanguage;
import com.abbyy.mobile.ocr4.RecognitionManager;
import com.abbyy.mobile.ocr4.RecognitionManager.RotationType;

import com.quest.mobileocr.utils.ImageUtils;
import com.quest.mobileocr.utils.ImageUtils.ImageLoader;
import com.quest.mobileocr.utils.PreferenceUtils;

public class RecognitionContext {
	private static final String TAG = "RecognitionContext";

	private static RecognitionContext _instance;

	private final Context _context;
	private final ImageUtils.ImageLoader _imageLoader;

	private Uri _imageUri;
	private Bitmap _image;

	private RecognitionTarget _recognitionTarget;
	private Object _recognitionResult;
	private RotationType _rotationType = RotationType.NO_ROTATION;

	private Set<RecognitionLanguage> _languagesAvailableForOcr;
	private Set<RecognitionLanguage> _languagesAvailableForBcr;

	RecognitionContext( final Context context ) {
		_context = context;
		_imageLoader = new ImageLoader( context );
	}

	public static Set<RecognitionLanguage> getLanguagesAvailableForOcr() {
		return getInstance().getLanguagesAvailableForOcrInternal();
	}

	public static Set<RecognitionLanguage> getLanguagesAvailableForBcr() {
		return getInstance().getLanguagesAvailableForBcrInternal();
	}
	
	public static boolean shouldDetectPageOrientation() {
		return true;
	}

	public static boolean shouldPrebuildWordsInfo() {
		return false;
	}

	public static boolean shouldBuildWordsInfo() {
		return false;
	}

	public static RecognitionConfiguration.RecognitionMode getRecognitionMode() {
		return RecognitionConfiguration.RecognitionMode.FAST;
	}

	private Set<RecognitionLanguage> getLanguagesAvailableForOcrInternal() {
		if( _languagesAvailableForOcr == null ) {
			_languagesAvailableForOcr = Engine.getInstance().getLanguagesAvailableForOcr();
		}
		return _languagesAvailableForOcr;
	}

	private Set<RecognitionLanguage> getLanguagesAvailableForBcrInternal() {
		if( _languagesAvailableForBcr == null ) {
			_languagesAvailableForBcr = Engine.getInstance().getLanguagesAvailableForBcr();
		}
		return _languagesAvailableForBcr;
	}

	/**
	 * @return the image
	 */
	public static Bitmap getImage( final Uri imageUri ) {
		Log.v( RecognitionContext.TAG, "getImage(" + imageUri + ")" );
		return getInstance().getImageInternal( imageUri );
	}

	/**
	 * @param image
	 *            the image to set
	 */
	public static void setImage( final Uri imageUri, final Bitmap image ) {
		Log.v( RecognitionContext.TAG, "setImage(" + imageUri + ")" );
		getInstance().setImageInternal( imageUri, image );
	}

	public static void cancelGetImage() {
		Log.v( RecognitionContext.TAG, "cancelGetImage()" );
		getInstance()._imageLoader.cancelLoadImage();
	}

	public static Set<RecognitionLanguage> getRecognitionLanguages( final RecognitionTarget recognitionTarget ) {
		final Context context = getInstance()._context;
		return PreferenceUtils.getRecognitionLanguages( context, "" );
	}

	/**
	 * Set a recognition target.
	 * 
	 * @param recognitionTarget
	 *            The recognition target to set.
	 */
	public static void setRecognitionTarget( final RecognitionTarget recognitionTarget ) {
		getInstance()._recognitionTarget = recognitionTarget;
	}

	/**
	 * Get a recognition target previously set by {@link #(com.quest.mobileocr.RecognitionContext.RecognitionTarget)} method.
	 * 
	 * @return The recognition target.
	 */
	public static RecognitionTarget getRecognitionTarget() {
		return getInstance()._recognitionTarget;
	}

	public static void setRotationType( final RotationType rotationType ) {
		getInstance()._rotationType = rotationType;
	}

	public static void setRecognitionResult( final Object result ) {
		getInstance()._recognitionResult = result;
	}

	public static Object getRecognitionResult() {
		return getInstance()._recognitionResult;
	}

	public static RotationType getRotationType() {
		return getInstance()._rotationType;
	}

	public static void cleanupResult() {
		getInstance()._recognitionResult = null;
	}

	public static void cleanupImage() {
		getInstance().cleanupImageInternal();
	}

	static RecognitionContext createInstance( final Context context ) {
		if( RecognitionContext._instance == null ) {
			RecognitionContext._instance = new RecognitionContext( context );
		}
		return RecognitionContext._instance;
	}

	static RecognitionContext getInstance() {
		if( RecognitionContext._instance == null ) {
			throw new NullPointerException( "RecognitionContext instance is null" );
		}
		return RecognitionContext._instance;
	}

	static void destroyInstance() {
		if( RecognitionContext._instance != null ) {
			cleanupImage();
			RecognitionContext._instance = null;
		}
	}

	/**
	 * @return the image
	 */
	private synchronized Bitmap getImageInternal( final Uri imageUri ) {
		if( imageUri == null ) {
			throw new NullPointerException( "imageUri is null" );
		}

		Bitmap image = null;
		if( imageUri.equals( _imageUri ) ) {
			image = _image;
		}
		if( image == null ) {
			Log.i( RecognitionContext.TAG, "Image is null. Need to load image." );
			try {
				image = _imageLoader.loadImage( imageUri );
				if (image == null) {
					Log.i( this.TAG, "Image was not loaded." );
				}
				setImage( imageUri, image );
			} catch( final ImageUtils.ImageLoaderException exception ) {
				//Remove for now
				Log.w( this.TAG, "Failed to load image: " + imageUri, exception );
			} catch( final CancellationException exception ) {
				//Remove for now
				Log.i( this.TAG, "Image loading cancelled: " + imageUri, exception );
			}
		}
		return image;
	}

	/**
	 * @param image
	 *            the image to set
	 */
	private synchronized void setImageInternal( final Uri imageUri, final Bitmap image ) {
		if( imageUri == null ) {
			throw new NullPointerException( "imageUri is null" );
		} else if( image == null ) {
			throw new NullPointerException( "image is null" );
		}

		Log.i( RecognitionContext.TAG, "Cleanup before setting new image." );
		cleanupImage();
		_imageUri = imageUri;
		_image = image;
	}

	private void cleanupImageInternal() {
		Log.v( RecognitionContext.TAG, "cleanup()" );
		_imageUri = null;
		if( _image != null ) {
			_image.recycle();
			_image = null;
			// There's a bug in the Android OS that leads to OutOfMemoryError
			// when working with Bitmaps:
			// http://code.google.com/p/android/issues/detail?id=8488
			// Thus we have to release all references to our image and initiate
			// the garbage collection manually.
			System.gc();
		}
	}

	public static enum RecognitionTarget {
		TEXT,
		BUSINESS_CARD,
		BARCODE
	}

}
