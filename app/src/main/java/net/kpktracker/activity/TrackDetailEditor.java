package net.kpktracker.activity;

import android.app.Activity;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import net.kpktracker.R;
import net.kpktracker.db.DataHelper;
import net.kpktracker.db.TrackContentProvider;
import net.kpktracker.db.model.Track;

import java.util.Date;

/**
 * Base class for activities that edit track details.
 *
 */
public abstract class TrackDetailEditor extends Activity {

	/** Current track ID */
	protected long trackId;

	/** Edit text for the track name */
	protected EditText etName;

	/** Edit text for track description */
	protected EditText etDescription;

	/** Edit text for track tags */
	protected EditText etTags;

	protected EditText etSurveyor;
	protected EditText etRouteName;
	protected EditText etRouteID;
	protected EditText etHeadwayMns;
	protected Spinner spVehicleType;
	protected EditText etFarePrice;
	protected EditText etProject;

	/** Spinner for track visibility */
	protected Spinner spVisibility;
	
	/** Whereas to verify if mandatory fields are filled or not */
	protected boolean fieldsMandatory = false;
	
	protected void onCreate(Bundle savedInstanceState, int viewResId, long trackId) {
		super.onCreate(savedInstanceState);

		this.trackId = trackId;

		setContentView(viewResId);
		setTitle(getTitle() + ": #" + trackId);

		etName = (EditText) findViewById(R.id.trackdetail_item_name);
		etDescription = (EditText) findViewById(R.id.trackdetail_item_description);
		etTags = (EditText) findViewById(R.id.trackdetail_item_tags);
		etSurveyor = (EditText) findViewById(R.id.trackdetail_item_surveyor);
		etRouteName = (EditText) findViewById(R.id.trackdetail_item_route_name);
		etRouteID = (EditText) findViewById(R.id.trackdetail_item_route_id);
		etHeadwayMns = (EditText) findViewById(R.id.trackdetail_item_headway_in_minutes);
		spVehicleType = (Spinner) findViewById(R.id.trackdetail_item_vehicle_type);
		etFarePrice = (EditText) findViewById(R.id.trackdetail_item_fare_price);
		etProject = (EditText) findViewById(R.id.trackdetail_item_project);
		spVisibility = (Spinner) findViewById(R.id.trackdetail_item_osm_visibility);

		ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(this,
				android.R.layout.simple_spinner_item);
		for (Track.OSMVisibility v: Track.OSMVisibility.values()) {
			adapter.add(getResources().getString(v.resId));
		}
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spVisibility.setAdapter(adapter);

		ArrayAdapter<CharSequence> adapterVehicle = new ArrayAdapter<CharSequence>(this,
				android.R.layout.simple_spinner_item);
		for (Track.VehicleType v: Track.VehicleType.values()) {
			adapterVehicle.add(getResources().getString(v.resId));
		}
		adapterVehicle.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spVehicleType.setAdapter(adapterVehicle);
	}
	
	protected void bindTrack(Track t) {
		if (etName.length() == 0) {
			etName.setText(t.getDisplayName());
		}
		etDescription.setText(t.getDescription());
		etTags.setText(t.getCommaSeparatedTags());
		etSurveyor.setText(t.getSurveyor());
		etRouteName.setText(t.getRouteName());
		etRouteID.setText(t.getRouteID());
		etHeadwayMns.setText(t.getHeadwayMns());
		spVehicleType.setSelection(t.getVehicleType().position);
		etFarePrice.setText(t.getFarePrice());
		etProject.setText(t.getProject());
		spVisibility.setSelection(t.getVisibility().position);
	}

	/**
	 * Saves the new information in database
	 * @return false if the save didn't take place, true otherwise.
	 */
	protected boolean save() {
		// Save changes to db (if any), then finish.
		Uri trackUri = ContentUris.withAppendedId(TrackContentProvider.CONTENT_URI_TRACK, trackId);
		ContentValues values = new ContentValues();

		Cursor cursor = getContentResolver().query(trackUri, null, null,
				null, null);

		long startDateLong = 0;
		String tname = "";
		if (cursor != null && cursor.moveToFirst()) {
			startDateLong = cursor.getLong(cursor.getColumnIndex(TrackContentProvider.Schema.COL_START_DATE));
			tname = cursor.getString(cursor.getColumnIndex(TrackContentProvider.Schema.COL_NAME));
			cursor.close();
		}

		// Saved track startDate
		Date startDate = new Date(startDateLong);

		// Save name field, if changed, to db.
		// String class required for equals to work, and for trim().
		String nameToSave = etName.getText().toString().trim();
		if(nameToSave.length() == 0)
			nameToSave = DataHelper.FILENAME_FORMATTER.format(startDate); // Set default track name
        values.put(TrackContentProvider.Schema.COL_NAME, nameToSave);

		// All other values updated even if empty
		values.put(TrackContentProvider.Schema.COL_DESCRIPTION, etDescription.getText().toString().trim());
		values.put(TrackContentProvider.Schema.COL_SURVEYOR, etSurveyor.getText().toString().trim());
		values.put(TrackContentProvider.Schema.COL_ROUTE_NAME, etRouteName.getText().toString().trim());
		values.put(TrackContentProvider.Schema.COL_ROUTE_ID, etRouteID.getText().toString().trim());
		values.put(TrackContentProvider.Schema.COL_HEADWAY_MINUTES, etHeadwayMns.getText().toString().trim());
		values.put(TrackContentProvider.Schema.COL_ROUTE_TYPE, Track.VehicleType.fromPosition(spVehicleType.getSelectedItemPosition()).toString());
		values.put(TrackContentProvider.Schema.COL_FARE_PRICE, etFarePrice.getText().toString().trim());
		values.put(TrackContentProvider.Schema.COL_PROJECT, etProject.getText().toString().trim());
		values.put(TrackContentProvider.Schema.COL_TAGS, etTags.getText().toString().trim());
		values.put(TrackContentProvider.Schema.COL_OSM_VISIBILITY, Track.OSMVisibility.fromPosition(spVisibility.getSelectedItemPosition()).toString());

		getContentResolver().update(trackUri, values, null, null);
		// All done
		return true;
	}
}
