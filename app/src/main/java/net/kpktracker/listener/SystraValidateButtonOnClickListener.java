package net.kpktracker.listener;

import net.kpktracker.activity.TrackLogger;

import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

/**
 * Listener for page-type buttons. Provokes a navigation
 * to the target page.
 *
 */
public class SystraValidateButtonOnClickListener implements OnClickListener {

	private TrackLogger tl;

	public SystraValidateButtonOnClickListener(TrackLogger trackLogger){
		tl = trackLogger;
	}

	@Override
	public void onClick(View v) {
		Button button = (Button) v;
		String buttonType = (String) button.getTag();
		tl.systraValidatePage(buttonType);
	}
}
