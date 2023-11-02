package net.kpktracker.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.kpktracker.OSMTracker;
import net.kpktracker.R;
import net.kpktracker.activity.TrackLogger;
import net.kpktracker.layout.DisablableTableLayout;
import net.kpktracker.layout.UserDefinedLayout;
import net.kpktracker.listener.PageButtonOnClickListener;
import net.kpktracker.listener.StillImageOnClickListener;
import net.kpktracker.listener.SystraCounterOnClickListener;
import net.kpktracker.listener.SystraNoteOnClickListener;
import net.kpktracker.listener.SystraValidateButtonOnClickListener;
import net.kpktracker.listener.TagButtonOnClickListener;
import net.kpktracker.listener.TextNoteOnClickListener;
import net.kpktracker.listener.VoiceRecOnClickListener;
import net.kpktracker.service.resources.IconResolver;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

/**
 * Reads an user defined layout, using a pull parser,
 * and instantiate corresponding objects (Layouts, Buttons)
 * 
 * @author Nicolas Guillaumin
 * 
 */
public class UserDefinedLayoutReader {

	@SuppressWarnings("unused")
	private static final String TAG = UserDefinedLayoutReader.class.getSimpleName();

	/**
	 * Map containing parsed layouts
	 */
	private HashMap<String, ViewGroup> layouts = new HashMap<String, ViewGroup>();

	/**
	 * Source parser
	 */
	private XmlPullParser parser;

	/**
	 * Context for accessing resources
	 */
	private Context context;

	/**
	 * The user defined Layout
	 */
	private UserDefinedLayout userDefinedLayout;
	
	/**
	 * {@link IconResolver} to retrieve button icons.
	 */
	private IconResolver iconResolver;

	/**
	 * Listener bound to text note buttons
	 */
	private TextNoteOnClickListener textNoteOnClickListener;

	/**
	 * Listener bound to Systra note buttons
	 */
	private SystraNoteOnClickListener systraNoteOnClickListener;
	private SystraValidateButtonOnClickListener systraValidateOnClickListener;
	private ArrayList<SystraCounterOnClickListener> listSystraCounterListeners;
	public ArrayList<SystraCounterOnClickListener> getListSystraCounterListeners() {
		return listSystraCounterListeners;
	}
	private ArrayList<Button> listSystraNoteButtons;
	public ArrayList<Button> getListSystraNoteButtons() { return listSystraNoteButtons; }

	/**
	 * Listener bound to voice record buttons
	 */
	private VoiceRecOnClickListener voiceRecordOnClickListener;
	
	/**
	 * Lister bound to picture buttons
	 */
	private StillImageOnClickListener stillImageOnClickListener;
	
	/**
	 * {@link Resources} to retrieve String resources
	 */
	private Resources resources;

	private TrackLogger trackLogger;

	/** 
	 * representing ScreenOrientation
	 * see {@link Configuration.orientation}
	 */
	private int orientation;
	
	private static final int ICON_POS_AUTO = 0;
	private static final int ICON_POS_TOP = 1;
	private static final int ICON_POS_RIGHT = 2;
	private static final int ICON_POS_BOTTOM = 3;
	private static final int ICON_POS_LEFT = 4;
	
	/**
	 * the icon position for the current layout
	 */
	private int currentLayoutIconPos = UserDefinedLayoutReader.ICON_POS_AUTO;

	/**
	 * Current track id
	 */
	private long currentTrackId;
	
	/**
	 * Constructor
	 * 
	 * @param udl
	 *				User defined layout
	 * @param c
	 *				Context for accessing resources
	 * @param tl
	 *				TrackLogger activity
	 * @param trackId
	 * 			  Current track id
	 * @param input
	 *				Parser for reading layout
	 * @param ir
	 * 			  Icon resolver to use to fetch icons 
	 */
	public UserDefinedLayoutReader(UserDefinedLayout udl, Context c, TrackLogger tl, long trackId, XmlPullParser input, IconResolver ir) {
		parser = input;
		context = c;
		resources = context.getResources();
		userDefinedLayout = udl;
		iconResolver = ir;
		currentTrackId = trackId;
		orientation = resources.getConfiguration().orientation;
		trackLogger = tl;

		// Initialize listeners which will be bound to buttons
		systraNoteOnClickListener = new SystraNoteOnClickListener(tl);
		systraValidateOnClickListener = new SystraValidateButtonOnClickListener(tl);
		textNoteOnClickListener = new TextNoteOnClickListener(tl);
		voiceRecordOnClickListener = new VoiceRecOnClickListener(tl);
		stillImageOnClickListener = new StillImageOnClickListener(tl);
		listSystraCounterListeners = new ArrayList<>();
		listSystraNoteButtons = new ArrayList<>();
	}

	/**
	 * Parses an XML layout
	 * 
	 * @return An HashMap of {@link ViewGroup} with layout name as key.
	 * @throws XmlPullParserException
	 * @throws IOException
	 */
	public HashMap<String, ViewGroup> parseLayout() throws XmlPullParserException, IOException {
		int eventType = parser.getEventType();
		while (eventType != XmlPullParser.END_DOCUMENT) {
			switch (eventType) {
			case XmlPullParser.START_TAG:
				String tagName = parser.getName();
				if (XmlSchema.TAG_LAYOUT.equals(tagName)) {
					// <layout> tag has been encountered. Inflate this layout
					inflateLayout();
				}
				break;
			case XmlPullParser.END_TAG:
				break;
			}
			eventType = parser.next();
		}
		return layouts;
	}

	/**
	 * Inflates a <layout> into a {@link TableLayout}
	 * 
	 * @throws IOException
	 * @throws XmlPullParserException
	 */
	private void inflateLayout() throws IOException, XmlPullParserException {
		String layoutName = parser.getAttributeValue(null, XmlSchema.ATTR_NAME);
		String layoutIconPosValue = parser.getAttributeValue(null, XmlSchema.ATTR_ICONPOS);

		// find out the correct icon position for this layout
		if (XmlSchema.ATTR_VAL_ICONPOS_TOP.equals(layoutIconPosValue)) {
			// TOP position
			this.currentLayoutIconPos = UserDefinedLayoutReader.ICON_POS_TOP;
		} else if (XmlSchema.ATTR_VAL_ICONPOS_RIGHT.equals(layoutIconPosValue)){
			// RIGHT position
			this.currentLayoutIconPos = UserDefinedLayoutReader.ICON_POS_RIGHT;
		} else if (XmlSchema.ATTR_VAL_ICONPOS_BOTTOM.equals(layoutIconPosValue)){
			// BOTTOM position
			this.currentLayoutIconPos = UserDefinedLayoutReader.ICON_POS_BOTTOM;
		} else if (XmlSchema.ATTR_VAL_ICONPOS_LEFT.equals(layoutIconPosValue)){
			// LEFT position
			this.currentLayoutIconPos = UserDefinedLayoutReader.ICON_POS_LEFT;
		} else {
			// if no or an undefined value is given for the current layout
			// AUTO position depending on screen orientation
			this.currentLayoutIconPos = UserDefinedLayoutReader.ICON_POS_AUTO;
		}

		// Create a new table layout and set default parameters
		DisablableTableLayout tblLayout = new DisablableTableLayout(context);
		tblLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,
				LinearLayout.LayoutParams.FILL_PARENT, 1));

		String currentTagName = null;
		while (!XmlSchema.TAG_LAYOUT.equals(currentTagName)) {
			int eventType = parser.next();
			switch (eventType) {
			case XmlPullParser.START_TAG:
				String name = parser.getName();
				if (XmlSchema.TAG_ROW.equals(name)) {
					// <row> tag has been encountered, inflates it
					inflateRow(tblLayout);
				}
				break;
			case XmlPullParser.END_TAG:
				currentTagName = parser.getName();
				break;
			}
		}
		// Add the new inflated layout to the list
		layouts.put(layoutName, tblLayout);
	}

	/**
	 * Inflates a <row> into a {@link TableRow}
	 * 
	 * @param layout
	 *				{@link TableLayout} to rattach the row to
	 * @throws XmlPullParserException
	 * @throws IOException
	 */
	private void inflateRow(TableLayout layout) throws XmlPullParserException, IOException {
		TableRow tblRow = new TableRow(layout.getContext());
		TableLayout.LayoutParams tableRowParams = new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT,
				TableLayout.LayoutParams.MATCH_PARENT, 1);

		int leftMargin = 5;
		int topMargin = 5;
		int rightMargin = 5;
		int bottomMargin = 5;

		tableRowParams.setMargins(leftMargin, topMargin, rightMargin, bottomMargin);
		tblRow.setLayoutParams(tableRowParams);
		tblRow.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER);

		String currentTagName = null;
		// int eventType = parser.next();
		while (!XmlSchema.TAG_ROW.equals(currentTagName)) {
			int eventType = parser.next();
			switch (eventType) {
			case XmlPullParser.START_TAG:
				String name = parser.getName();
				if (XmlSchema.TAG_BUTTON.equals(name)) {
					// <button> tag has been encountered, inflates it.
					inflateButton(tblRow);
				}
				break;
			case XmlPullParser.END_TAG:
				currentTagName = parser.getName();
				break;
			}

		}
		// Add the inflated table row to the current layout
		layout.addView(tblRow);
	}

	/**
	 * Inflates a <button>
	 * 
	 * @param row
	 *				The table row to attach the button to
	 */
	public void inflateButton(TableRow row) {
		Button button = new Button(row.getContext());
		button.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.FILL_PARENT,
				TableRow.LayoutParams.FILL_PARENT, 1));

		// TODO: Use kind of ButtonFactory here

		String buttonType = parser.getAttributeValue(null, XmlSchema.ATTR_TYPE);
		Drawable buttonIcon = null;
		if (XmlSchema.ATTR_VAL_PAGE.equals(buttonType)) {
			// Page button
			button.setText(findLabel(parser.getAttributeValue(null, XmlSchema.ATTR_LABEL), resources));
			buttonIcon = iconResolver.getIcon(parser.getAttributeValue(null, XmlSchema.ATTR_ICON));
			button.setOnClickListener(new PageButtonOnClickListener(userDefinedLayout, parser.getAttributeValue(null,
					XmlSchema.ATTR_TARGETLAYOUT)));
		} else if (XmlSchema.ATTR_VAL_TAG.equals(buttonType)) {
			// Standard tag button
			button.setText(findLabel(parser.getAttributeValue(null, XmlSchema.ATTR_LABEL), resources));			
			buttonIcon = iconResolver.getIcon(parser.getAttributeValue(null, XmlSchema.ATTR_ICON));
			button.setOnClickListener(new TagButtonOnClickListener(currentTrackId));
		} else if (XmlSchema.ATTR_VAL_VOICEREC.equals(buttonType)) {
			// Voice record button
			String label = findLabel(parser.getAttributeValue(null, XmlSchema.ATTR_LABEL), resources);
			if (label == null) {
				label = resources.getString(R.string.gpsstatus_record_voicerec);
			}
			button.setText(label);
			buttonIcon = resources.getDrawable(R.drawable.voice_32x32);
			button.setOnClickListener(voiceRecordOnClickListener);
		} else if (XmlSchema.ATTR_VAL_TEXTNOTE.equals(buttonType)) {
			// Text note button
			button.setText(resources.getString(R.string.gpsstatus_record_textnote));
			buttonIcon = resources.getDrawable(R.drawable.text_32x32);
			button.setOnClickListener(textNoteOnClickListener);
		} else if (XmlSchema.ATTR_VAL_SYSTRA_BUTTON_VALIDATE.equals(buttonType)) {
			String label = findLabel(parser.getAttributeValue(null, XmlSchema.ATTR_LABEL), resources);
			String backgroundColor = findLabel(parser.getAttributeValue(null, XmlSchema.ATTR_VAL_SYSTRA_BACKGROUND_COLOR), resources);
			if (label == null) {
				label = "OK";
			}
			button.setText(label);
			buttonIcon = resources.getDrawable(R.drawable.check);
			String sytraType = parser.getAttributeValue(null, XmlSchema.ATTR_VAL_SYSTRATYPE);
			button.setTag(sytraType);
			// TODO: button background color
			//Drawable buttonDrawable = button.getBackground();
			//buttonDrawable = DrawableCompat.wrap(buttonDrawable);
			//DrawableCompat.setTint(buttonDrawable, Color.RED);
			//button.setBackground(buttonDrawable);
			button.setOnClickListener(systraValidateOnClickListener);
		} else if (XmlSchema.ATTR_VAL_SYSTRACOUNTER.equals(buttonType)) {
			// Systra counter buttons
			Pattern colorPattern = Pattern.compile("#([0-9a-f]{3}|[0-9a-f]{6}|[0-9a-f]{8})");
			String label = findLabel(parser.getAttributeValue(null, XmlSchema.ATTR_LABEL), resources);
			buttonIcon = iconResolver.getIcon(parser.getAttributeValue(null, XmlSchema.ATTR_ICON));
			String backgroundColor = findLabel(parser.getAttributeValue(null, XmlSchema.ATTR_VAL_SYSTRA_BACKGROUND_COLOR), resources);
			String to_validate = findLabel(parser.getAttributeValue(null, XmlSchema.ATTR_VAL_SYSTRA_IS_TO_VALIDATE), resources);
			String systraType = parser.getAttributeValue(null, XmlSchema.ATTR_VAL_SYSTRATYPE);
			SystraCounterOnClickListener listener = new SystraCounterOnClickListener(this.trackLogger, systraType, currentTrackId);
			listSystraCounterListeners.add(listener);
			// Row background color
			if (backgroundColor != null) {
				Matcher m = colorPattern.matcher(backgroundColor);
				if (m.matches()) {
					row.setBackgroundColor(Color.parseColor(backgroundColor));
				}
			}
			LinearLayout layoutPrincipal = new LinearLayout(row.getContext());
			layoutPrincipal.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT));
			layoutPrincipal.setOrientation(LinearLayout.HORIZONTAL);

			LinearLayout layoutLeft = new LinearLayout(row.getContext());
			layoutLeft.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1));
			layoutLeft.setOrientation(LinearLayout.VERTICAL);
			layoutLeft.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER);

			LinearLayout layoutRight = new LinearLayout(row.getContext());
			layoutRight.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1));
			layoutRight.setOrientation(LinearLayout.VERTICAL);
			layoutRight.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER);

			LinearLayout layoutCounter = new LinearLayout(row.getContext());
			layoutCounter.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));
			layoutCounter.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER);

			LinearLayout layoutButtons = new LinearLayout(row.getContext());
			layoutButtons.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));
			layoutButtons.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.RIGHT);
			layoutButtons.setOrientation(LinearLayout.HORIZONTAL);
			// Layout left
			// Label
			TextView counterLabel = new TextView(row.getContext());
			counterLabel.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
					LinearLayout.LayoutParams.MATCH_PARENT, 1));
			counterLabel.setTypeface(counterLabel.getTypeface(), Typeface.BOLD);
			counterLabel.setText(label);
			counterLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
			layoutLeft.addView(counterLabel);
			// Image
			if (buttonIcon != null) {
				ImageView imageView = new ImageView(row.getContext());
				imageView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT, 1));
				imageView.setImageDrawable(buttonIcon);
				imageView.setScaleType(ImageView.ScaleType.FIT_START);
				layoutLeft.addView(imageView);
			}
			// Layout right
			// Counter
			TextView counterCount = new TextView(row.getContext());
			counterCount.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
					LinearLayout.LayoutParams.WRAP_CONTENT));
			counterCount.setTypeface(counterLabel.getTypeface(), Typeface.BOLD);
			counterCount.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
			layoutCounter.addView(counterCount);
			layoutRight.addView(layoutCounter);
			listener.setCounterText(counterCount);
			// Buttons "+" & "-" & check
			Boolean plusMinusEnabled = to_validate == null;
			ImageButton button_minus = new ImageButton(row.getContext());
			button_minus.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
					LinearLayout.LayoutParams.MATCH_PARENT, 1));
			button_minus.setImageDrawable(resources.getDrawable(R.drawable.minus));
			button_minus.setTag("-");
			button_minus.setOnClickListener(listener);
			button_minus.setEnabled(plusMinusEnabled);
			listener.setMinusBtn(button_minus);
			ImageButton button_plus = new ImageButton(row.getContext());
			button_plus.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
					LinearLayout.LayoutParams.MATCH_PARENT, 1));
			button_plus.setImageDrawable(resources.getDrawable(R.drawable.plus));
			button_plus.setTag("+");
			button_plus.setOnClickListener(listener);
			button_plus.setEnabled(plusMinusEnabled);
			listener.setPlusBtn(button_plus);
			layoutButtons.addView(button_plus);
			layoutButtons.addView(button_minus);
			if (to_validate != null) {
				ImageButton button_check = new ImageButton(row.getContext());
				button_check.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
						LinearLayout.LayoutParams.MATCH_PARENT, 1));
				button_check.setImageDrawable(resources.getDrawable(R.drawable.check));
				button_check.setTag("c");
				button_check.setOnClickListener(listener);
				button_check.setEnabled(false);
				listener.setCheckBtn(button_check);
				layoutButtons.addView(button_check);
			}
			layoutRight.addView(layoutButtons);
			layoutPrincipal.addView(layoutLeft);
			layoutPrincipal.addView(layoutRight);
			row.addView(layoutPrincipal);
		} else if (XmlSchema.ATTR_VAL_SYSTRANOTE.equals(buttonType)) {
			// Systra note button
			String label = findLabel(parser.getAttributeValue(null, XmlSchema.ATTR_LABEL), resources);
			if (label == null) {
				label = resources.getString(R.string.gpsstatus_record_textnote);
			}
			button.setText(label);
			buttonIcon = iconResolver.getIcon(parser.getAttributeValue(null, XmlSchema.ATTR_ICON));
			if (buttonIcon == null) {
				buttonIcon = resources.getDrawable(R.drawable.text_32x32);
			}
			String sytraType = parser.getAttributeValue(null, XmlSchema.ATTR_VAL_SYSTRATYPE);
			button.setTag(sytraType + "|" + label);
			button.setOnClickListener(systraNoteOnClickListener);
			listSystraNoteButtons.add(button);
		} else if (XmlSchema.ATTR_VAL_PICTURE.equals(buttonType)) {
			// Picture button
			button.setText(resources.getString(R.string.gpsstatus_record_stillimage));
			buttonIcon = resources.getDrawable(R.drawable.camera_32x32);
			button.setOnClickListener(stillImageOnClickListener);
		}

		// Where to draw the button's icon (depending on the current layout)
		switch (this.currentLayoutIconPos) {
		case UserDefinedLayoutReader.ICON_POS_TOP:
			// TOP position
			button.setCompoundDrawablesWithIntrinsicBounds(null, buttonIcon, null, null);
			break;
		case UserDefinedLayoutReader.ICON_POS_RIGHT:
			// RIGHT position
			button.setCompoundDrawablesWithIntrinsicBounds(null, null, buttonIcon, null);
			break;
		case UserDefinedLayoutReader.ICON_POS_BOTTOM:
			// BOTTOM position
			button.setCompoundDrawablesWithIntrinsicBounds(null, null, null, buttonIcon);
			break;
		case UserDefinedLayoutReader.ICON_POS_LEFT:
			// LEFT position
			button.setCompoundDrawablesWithIntrinsicBounds(buttonIcon, null, null, null);
			break;
		case UserDefinedLayoutReader.ICON_POS_AUTO:
		default:
			// if no or an undefined value is given for the current layout
			// AUTO position depending on screen orientation
			if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
				// in landscape mode draw icon to the LEFT
				button.setCompoundDrawablesWithIntrinsicBounds(buttonIcon, null, null,null);
			} else {
				// in portrait mode draw icon to the TOP
				button.setCompoundDrawablesWithIntrinsicBounds(null, buttonIcon, null, null);
			}
			break;
		}
		if (!XmlSchema.ATTR_VAL_SYSTRACOUNTER.equals(buttonType)) {
			row.addView(button);
		}
	}
	
	/**
	 * Finds a label if it's a reference to an internal resource (@string/label) 
	 * @param text Resource reference or plain label
	 * @param r {@link Resources} to lookup from
	 * @return Plain label, or corresponding text extracted from {@link Resources}
	 */
	private String findLabel(String text, Resources r) {
		if (text != null) {
			if (text.startsWith("@")) {
				// Check if it's a resource identifier
				int resId = resources.getIdentifier(text.replace("@", ""), null, OSMTracker.PACKAGE_NAME);
				if (resId != 0) {
					return resources.getString(resId);
				}
			}
		}
		return text;
	}

	/**
	 * XML Schema
	 */
	private static final class XmlSchema {
		public static final String TAG_LAYOUT = "layout";
		public static final String TAG_ROW = "row";
		public static final String TAG_BUTTON = "button";

		public static final String ATTR_NAME = "name";
		public static final String ATTR_TYPE = "type";
		public static final String ATTR_LABEL = "label";
		public static final String ATTR_TARGETLAYOUT = "targetlayout";
		public static final String ATTR_ICON = "icon";
		public static final String ATTR_ICONPOS = "iconpos";

		public static final String ATTR_VAL_TAG = "tag";
		public static final String ATTR_VAL_PAGE = "page";
		public static final String ATTR_VAL_VOICEREC = "voicerec";
		public static final String ATTR_VAL_TEXTNOTE = "textnote";
		public static final String ATTR_VAL_SYSTRANOTE = "systranote";
		public static final String ATTR_VAL_SYSTRATYPE = "systratype";
		public static final String ATTR_VAL_SYSTRACOUNTER = "systracounter";
		public static final String ATTR_VAL_SYSTRA_BACKGROUND_COLOR = "background_color";
		public static final String ATTR_VAL_SYSTRA_IS_TO_VALIDATE = "is_to_validate";
		public static final String ATTR_VAL_SYSTRA_BUTTON_VALIDATE = "systravalidate";
		public static final String ATTR_VAL_PICTURE = "picture";

		public static final String ATTR_VAL_ICONPOS_TOP = "top";
		public static final String ATTR_VAL_ICONPOS_RIGHT = "right";
		public static final String ATTR_VAL_ICONPOS_BOTTOM = "bottom";
		public static final String ATTR_VAL_ICONPOS_LEFT = "left";
	}
}
