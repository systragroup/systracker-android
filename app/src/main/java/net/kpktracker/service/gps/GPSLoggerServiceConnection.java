package net.kpktracker.service.gps;

import net.kpktracker.OSMTracker;
import net.kpktracker.R;
import net.kpktracker.activity.TrackLogger;
import net.kpktracker.layout.GpsStatusRecord;
import net.kpktracker.db.TrackContentProvider;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

/**
 * Handles the bind to the GPS Logger service
 * 
 * @author Nicolas Guillaumin
 *
 */
public class GPSLoggerServiceConnection implements ServiceConnection {

	/**
	 * Reference to TrackLogger activity
	 */
	private TrackLogger activity;
	
	public GPSLoggerServiceConnection(TrackLogger tl) {
		activity = tl;
	}
	
	@Override
	public void onServiceDisconnected(ComponentName name) {
		activity.setEnabledActionButtons(false);
		activity.setGpsLogger(null);
	}

	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {
		
		activity.setGpsLogger( ((GPSLogger.GPSLoggerBinder) service).getService());

		// Update record status regarding of current tracking state
		GpsStatusRecord gpsStatusRecord = (GpsStatusRecord) activity.findViewById(R.id.gpsStatus);
		if (gpsStatusRecord != null) {
			gpsStatusRecord.manageRecordingIndicator(activity.getGpsLogger().isTracking());
		}
		
		// If not already tracking, start tracking
		if (!activity.getGpsLogger().isTracking()) {
			activity.setEnabledActionButtons(false);
			Intent intent = new Intent(OSMTracker.INTENT_START_TRACKING);
			intent.putExtra(TrackContentProvider.Schema.COL_TRACK_ID, activity.getCurrentTrackId());
			activity.sendBroadcast(intent);
		}
	}

}
