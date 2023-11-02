package net.kpktracker.view;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.WindowManager.LayoutParams;
import android.widget.EditText;

import net.kpktracker.R;
import net.kpktracker.activity.TrackLogger;

import java.util.UUID;

public class SystraNoteDialog extends AlertDialog {

	/**
	 * bundle key for text of input field
	 */
	private static final String KEY_INPUT_TEXT = "INPUT_TEXT";

	/**
	 * bundle key for waypoint uuid
	 */
	private static final String KEY_WAYPOINT_UUID = "WAYPOINT_UUID";

	/**
	 * bundle key for waypoints track id
	 */
	private static final String KEY_WAYPOINT_TRACKID = "WAYPOINT_TRACKID";

	/**
	 * the input box displayed in the dialog
	 */
	EditText input;

	/**
	 * Unique identifier of the waypoint this dialog working on
	 */
	private String wayPointUuid = null;

	/**
	 * Id of the track the dialog will add this waypoint to
	 */
	private long wayPointTrackId;

	private Context context;

	public SystraNoteDialog(Context context, long trackId) {
		super(context);

		this.context = context;
		this.wayPointTrackId = trackId;

		// Text edit control for user input
		input = new EditText(context);

		// default settings
		//this.setTitle(R.string.gpsstatus_record_textnote);
		this.setCancelable(true);
		this.setView(input);

		this.setButton(context.getResources().getString(android.R.string.ok),  new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// Track waypoint with user input text
				String text = input.getText().toString();
				String systraKey = getSystraKey();
				String defaultLabel = getDefaultLabel();
				((TrackLogger) SystraNoteDialog.this.context).setSystraKeyValue(systraKey, text);
				// Update text on button
				if (!text.equals("")) {
					((TrackLogger) SystraNoteDialog.this.context).getSystraNoteBtn().setText(text);
				} else {
					((TrackLogger) SystraNoteDialog.this.context).getSystraNoteBtn().setText(defaultLabel);
				}
				((TrackLogger) SystraNoteDialog.this.context).clearSystraNoteBtn();
			}
		});

		this.setButton2(context.getResources().getString(android.R.string.cancel),  new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// cancel the dialog
				((TrackLogger) SystraNoteDialog.this.context).clearSystraNoteBtn();
				dialog.cancel();	
			}
		});
	}

	private String getSystraKey() {
		String[] systraKeys = ((TrackLogger) SystraNoteDialog.this.context).getSystraKey().split("\\|");
		String systraKey = "";
		for (int i = 0; i < systraKeys.length; i++) {
			if (i < (systraKeys.length - 1)) {
				if (systraKey.length() != 0) {
					systraKey += "|";
				}
				systraKey += systraKeys[i];
			}
		}
		return systraKey;
	}

	private String getDefaultLabel() {
		String[] systraKeys = ((TrackLogger) SystraNoteDialog.this.context).getSystraKey().split("\\|");
		String defaultLabel = ((TrackLogger) SystraNoteDialog.this.context).getResources().getString(R.string.gpsstatus_record_textnote);
		if (systraKeys.length != 0) {
			defaultLabel = systraKeys[systraKeys.length - 1];
		}
		return defaultLabel;
	}

	/**
	 * @link android.app.Dialog#onStart()
	 */
	@Override
	protected void onStart() {
		if (wayPointUuid == null) {
			// there is no UUID set for the waypoint we're working on
			// so we need to generate a UUID and track this point
			wayPointUuid = UUID.randomUUID().toString();
		}
		getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
		// Set text value if any
		String systraKey = getSystraKey();
		String inputText = ((TrackLogger) this.context).getSystraKeyValue(systraKey);
		if (inputText != null) {
			input.setText(inputText);
			input.setSelection(input.getText().length());
		}
		super.onStart();
	}

	/**
	 * resets values of this dialog
	 * such as the input fields text and the waypoints uuid
	 */
	public void resetValues(){
		wayPointUuid = null;
		input.setText("");
	}

	/**
	 * restoring values from the savedInstaceState 
	 */
	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		String text = savedInstanceState.getString(KEY_INPUT_TEXT);
		if (text != null) {
			input.setText(text);
		}
		wayPointUuid = savedInstanceState.getString(KEY_WAYPOINT_UUID);
		wayPointTrackId = savedInstanceState.getLong(KEY_WAYPOINT_TRACKID);
		super.onRestoreInstanceState(savedInstanceState);
	}

	/**
	 * save values to bundle that we'll need later
	 */
	@Override
	public Bundle onSaveInstanceState() {
		Bundle extras = super.onSaveInstanceState();
		extras.putString(KEY_INPUT_TEXT, input.getText().toString());
		extras.putLong(KEY_WAYPOINT_TRACKID, wayPointTrackId);
		extras.putString(KEY_WAYPOINT_UUID, wayPointUuid);
		return extras;
	}
}
