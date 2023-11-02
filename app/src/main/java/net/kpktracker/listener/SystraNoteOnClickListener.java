package net.kpktracker.listener;

import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import net.kpktracker.activity.TrackLogger;

/**
 * Manages Systra note button.
 *
 */
public class SystraNoteOnClickListener implements OnClickListener {

	private TrackLogger tl;

	public SystraNoteOnClickListener(TrackLogger trackLogger){
		tl = trackLogger;
	}

	@Override
	public void onClick(final View v) {
		// let the TrackLogger activity open and control the dialog
		Button button = (Button) v;
		String buttonType = (String) button.getTag();
		if (buttonType == null) {
			buttonType = "???";
		}
		tl.setSystraKey(buttonType);
		tl.setSystraNoteBtn(button);
		tl.showDialog(TrackLogger.SYSTRA_TEXT_NOTE);
	}
}
