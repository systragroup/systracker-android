package net.kpktracker.util;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.content.SharedPreferences.Editor;

import net.kpktracker.OSMTracker;
import net.kpktracker.R;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;


@RunWith(PowerMockRunner.class)
@PrepareForTest(PreferenceManager.class)

public class ThemeValidatorTest {
    Context mockContext;
    SharedPreferences mockPrefs;
    Resources mockRes;
    Editor mockEditor;

    /** Setup all the mocks(classes) that are used  when calling the
     * ThemeValidator class with a selected theme
    * @param theme
    */
    public void setupMocks(String theme) {

        mockPrefs = mock(SharedPreferences.class);
        when(mockPrefs.getString(OSMTracker.Preferences.KEY_UI_THEME,
                OSMTracker.Preferences.VAL_UI_THEME))
                .thenReturn(theme);


        String[] themes = {	"net.systracker:style/DefaultTheme",
		"net.systracker:style/DarkTheme",
		"net.systracker:style/LightTheme",
		"net.systracker:style/HighContrast"};

        mockRes = mock(Resources.class);
        when(mockRes.getStringArray(R.array.prefs_theme_values))
                .thenReturn(themes);


        mockContext = mock(Context.class);

        mockStatic(PreferenceManager.class);

        when(PreferenceManager.getDefaultSharedPreferences(mockContext)).thenReturn(mockPrefs);

        mockEditor=mock(SharedPreferences.Editor.class);

        when(mockPrefs.edit())
                .thenReturn(mockEditor);

    }

    @Test
    public void validateDefaultTheme(){
        setupMocks("net.systracker:style/DefaultTheme");
        String result =ThemeValidator.getValidTheme(mockPrefs, mockRes);
        String expected = "net.systracker:style/DefaultTheme";
        assertEquals(result, expected);
    }

    /*Use a theme that is not included on the theme values array and also
     *  verify methods of the mocked editor so that the preferences are saved.*/
    @Test
    public void validateWrongTheme(){

        setupMocks("net.systracker:style/YellowTheme");
        String result =ThemeValidator.getValidTheme(mockPrefs, mockRes);
        String expected = "net.systracker:style/DefaultTheme";
        assertEquals(result, expected);
        verify(mockPrefs,atLeastOnce()).edit();
        verify(mockEditor,atLeastOnce()).putString(OSMTracker.Preferences.KEY_UI_THEME, OSMTracker.Preferences.VAL_UI_THEME);
        verify(mockEditor).commit();
    }
}