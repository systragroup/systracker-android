package net.kpktracker.listener;

import android.content.Intent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import net.kpktracker.OSMTracker;
import net.kpktracker.activity.TrackLogger;
import net.kpktracker.db.TrackContentProvider;

/**
 * Manages Systra counter buttons.
 *
 */
public class SystraCounterOnClickListener implements View.OnClickListener {
    private TrackLogger tl;
    private String type;
    private TextView counterTxt = null;
    private ImageButton checkBtn = null;
    private ImageButton plusBtn = null;
    private ImageButton minusBtn = null;
    private int count = 0;
    private long currentTrackId;

    public SystraCounterOnClickListener(TrackLogger trackLogger, String systraType, long trackId){
        tl = trackLogger;
        type = systraType;
        currentTrackId = trackId;
    }

    public void setCounterText(TextView counter) {
        counterTxt = counter;
        // Get counter value back
        count = tl.getCount(type);
        counterTxt.setText(Integer.toString(count));
    }

    public void resetCounter() {
        this.setCounterText(this.counterTxt);
    }

    public void setCheckBtn(ImageButton check) { checkBtn = check; }
    public void setPlusBtn(ImageButton plus) { plusBtn = plus; }
    public void setMinusBtn(ImageButton minus) { minusBtn = minus; }
    public void setPlusAndMinusButtonsEnabled(Boolean isEnabled) {
        if (this.plusBtn != null) {
            this.plusBtn.setEnabled(isEnabled);
        }
        if (this.minusBtn != null) {
            this.minusBtn.setEnabled(isEnabled);
        }
    }
    public void setCheckBtnEnabled(Boolean isEnabled) {
        if (checkBtn != null) {
            checkBtn.setEnabled(isEnabled);
        }
    }

    private void updateLabelAndButtons(boolean enableCheck) {
        if (counterTxt != null) {
            counterTxt.setText(Integer.toString(count));
        }
        setCheckBtnEnabled(enableCheck);
    }

    @Override
    public void onClick(final View v) {
        // let the TrackLogger activity open and control the dialog
        ImageButton button = (ImageButton) v;
        String buttonType = (String) button.getTag();
        switch (buttonType) {
            case "+":
                count += 1;
                updateLabelAndButtons(true);
                break;
            case "-":
                if (count > 0) {
                    count -= 1;
                    updateLabelAndButtons(true);
                }
                break;
            case "c":
                if (type.equals("number_passengers")) {
                    tl.setNbPassengers(count);
                    String keyName = "number_passengers=" + count;
                    Intent intent = new Intent(OSMTracker.INTENT_TRACK_WP);
                    intent.putExtra(TrackContentProvider.Schema.COL_TRACK_ID, currentTrackId);
                    intent.putExtra(OSMTracker.INTENT_KEY_NAME, keyName);
                    v.getContext().sendBroadcast(intent);
                    updateLabelAndButtons(false);
                    // Inform user that the waypoint was tracked
                    Toast.makeText(v.getContext(), type + " : " + count, Toast.LENGTH_SHORT).show();
                }
                break;
        }
        if (checkBtn == null) {
            tl.setCount(type, count);
        }
    }
}
