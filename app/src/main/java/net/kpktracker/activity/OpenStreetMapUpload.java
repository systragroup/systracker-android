package net.kpktracker.activity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import net.kpktracker.OSMTracker;
import net.kpktracker.R;
import net.kpktracker.db.TrackContentProvider;
import net.kpktracker.db.model.Track;
import net.kpktracker.gpx.ExportToTempFileTask;
import net.kpktracker.gpx.SystraExportTask;
import net.kpktracker.osm.OpenStreetMapConstants;
import net.kpktracker.osm.RetrieveAccessTokenTask;
import net.kpktracker.osm.RetrieveRequestTokenTask;
import net.kpktracker.osm.UploadToOpenStreetMapTask;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import oauth.signpost.OAuth;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthProvider;

/**
 * <p>Uploads a track on OSM using the API and
 * OAuth authentication.</p>
 * 
 * <p>This activity may be called twice during a single
 * upload cycle: First to start the upload, then a second
 * time when the user has authenticated using the browser.</p>
 *
 * @author Nicolas Guillaumin
 */
public class OpenStreetMapUpload extends TrackDetailEditor {

	private static final String TAG = OpenStreetMapUpload.class.getSimpleName();

	/** URL that the browser will call once the user is authenticated */
	private static final String OAUTH_CALLBACK_URL = "osmtracker://osm-upload/oath-completed/?"+ TrackContentProvider.Schema.COL_TRACK_ID+"=";

	private static final CommonsHttpOAuthProvider oAuthProvider = new CommonsHttpOAuthProvider(
			OpenStreetMapConstants.OAuth.Urls.REQUEST_TOKEN_URL,
			OpenStreetMapConstants.OAuth.Urls.ACCESS_TOKEN_URL,
			OpenStreetMapConstants.OAuth.Urls.AUTHORIZE_TOKEN_URL);
	private static final CommonsHttpOAuthConsumer oAuthConsumer = new CommonsHttpOAuthConsumer(
			OpenStreetMapConstants.OAuth.CONSUMER_KEY,
			OpenStreetMapConstants.OAuth.CONSUMER_SECRET);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState, R.layout.osm_upload, getTrackId());
		fieldsMandatory = true;

		final Button btnOk = (Button) findViewById(R.id.osm_upload_btn_ok);
		btnOk.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (save()) {
					startUpload();
				}
			}
		});

		final Button btnCancel = (Button) findViewById(R.id.osm_upload_btn_cancel);
		btnCancel.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();				
			}
		});
		
		// Do not show soft keyboard by default
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
	}
	
	/**
	 * Gets the track ID we were called with, either from the
	 * intent extras if we were started by OSMTracker, or in the
	 * URI if we are returning from the browser.
	 * @return
	 */
	private long getTrackId() {
		if (getIntent().getExtras() != null && getIntent().getExtras().containsKey(TrackContentProvider.Schema.COL_TRACK_ID)) {
			return getIntent().getExtras().getLong(TrackContentProvider.Schema.COL_TRACK_ID);
		} else if (getIntent().getData().toString().startsWith(OAUTH_CALLBACK_URL)) {
			return Long.parseLong(getIntent().getData().getQueryParameter(TrackContentProvider.Schema.COL_TRACK_ID));
		} else {
			throw new IllegalArgumentException("Missing Track ID");
		}
	}
	
	/**
	 * Will be called as well when we come back from the browser
	 * after user authentication.
	 */
	@Override
	protected void onResume() {
		super.onResume();
		
		Cursor cursor = managedQuery(
				ContentUris.withAppendedId(TrackContentProvider.CONTENT_URI_TRACK, trackId),
				null, null, null, null);
			
			if (! cursor.moveToFirst())	{
				// This shouldn't occur, it's here just in case.
				// So, don't make each language translate/localize it.
				Toast.makeText(this, "Track ID not found.", Toast.LENGTH_SHORT).show();
				finish();
				return;  // <--- Early return ---
			}

		bindTrack(Track.build(trackId, cursor, getContentResolver(), false));
		
		Uri uri = getIntent().getData();
		Log.d(TAG, "URI: " + uri);
		if (uri != null && uri.toString().startsWith(OAUTH_CALLBACK_URL)) {
			// User is returning from authentication
			String verifier = uri.getQueryParameter(OAuth.OAUTH_VERIFIER);
			new RetrieveAccessTokenTask(this, oAuthProvider, oAuthConsumer, verifier).execute();
		}
	}

	/**
	 * Either starts uploading directly if we are authenticated against OpenStreetMap,
	 * or ask the user to authenticate via the browser.
	 */
	private void startUpload() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		if (prefs.contains(OSMTracker.Preferences.KEY_OSM_OAUTH_TOKEN)
				&& prefs.contains(OSMTracker.Preferences.KEY_OSM_OAUTH_SECRET)) {
			// Re-use saved token
			oAuthConsumer.setTokenWithSecret(
					prefs.getString(OSMTracker.Preferences.KEY_OSM_OAUTH_TOKEN, ""),
					prefs.getString(OSMTracker.Preferences.KEY_OSM_OAUTH_SECRET, ""));
			uploadToOsm();
		} else {
			// Open browser and request token
			new RetrieveRequestTokenTask(this, oAuthProvider, oAuthConsumer, OAUTH_CALLBACK_URL+trackId).execute();
		}
	}

	/**
	 * Exports track on disk then upload to OSM.
	 */
	public void uploadToOsm() {
		ExecutorService es = Executors.newSingleThreadExecutor();
		SystraExportTask task = new SystraExportTask(this, trackId);
		final Future<String> future = es.submit(task);
		try {
			es.shutdown();
			String result = future.get();
			if (result != null) {
				Log.e(TAG, "future error: " + result);
				new AlertDialog.Builder(this)
						.setTitle(android.R.string.dialog_alert_title)
						.setMessage(this.getResources()
								.getString(R.string.trackmgr_export_error)
								.replace("{0}", result))
						.setIcon(android.R.drawable.ic_dialog_alert)
						.setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.dismiss();
							}
						})
						.show();
			} else {
				Log.d(TAG, "Export finished");
				Toast.makeText(this, R.string.various_export_finished, Toast.LENGTH_SHORT).show();
			}
		} catch (Exception ex) {
			Log.e(TAG, "Exception: " + ex.getMessage());
			new AlertDialog.Builder(this)
					.setTitle(android.R.string.dialog_alert_title)
					.setMessage(this.getResources()
							.getString(R.string.trackmgr_export_error)
							.replace("{0}", ex.getMessage()))
					.setIcon(android.R.drawable.ic_dialog_alert)
					.setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
					})
					.show();
		} finally {
			es.shutdown();
		}
		new UploadToOpenStreetMapTask(OpenStreetMapUpload.this, trackId, oAuthConsumer, task.getTmpFile(),
				task.getFilename(), etDescription.getText().toString(), etTags.getText().toString(),
				Track.OSMVisibility.fromPosition(OpenStreetMapUpload.this.spVisibility.getSelectedItemPosition()))
				.execute();
	}
}
