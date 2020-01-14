package nl.knoppel.cios.android.textfilewidget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.provider.MediaStore;
import android.util.Log;

import com.lamerman.FileDialog;

import java.io.IOException;

public class TextwidgetSettingsActivity extends PreferenceActivity {

    public static final String WIDGET_PREFS_PREFIX = "textWidget_";
    public static final String PREF_FILE_PATH = "filePath";
    protected static final int FILE_CHOICE_PATH = 1;
    protected static final int FILE_CHOICE_URI = 2;
    private int widgetId;
    private SharedPreferences sharedPreferences;
    private Editor editor;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        widgetId = getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
        this.getPreferenceManager().setSharedPreferencesName(WIDGET_PREFS_PREFIX + widgetId);
        addPreferencesFromResource(R.xml.preferences);
        this.sharedPreferences = getPreferenceManager().getSharedPreferences();
        this.editor = sharedPreferences.edit();

        Preference fileSelectPreference = (Preference) findPreference(PREF_FILE_PATH);
        String path = sharedPreferences.getString(PREF_FILE_PATH, "No file selected");
        fileSelectPreference.setSummary(path);

        String lastDir = Environment.getExternalStorageDirectory().getPath();
        if (path != null && !path.equals("")) {
            int lastsSlash = path.lastIndexOf("/");
            if (lastsSlash > 0) {
                lastDir = path.substring(0, lastsSlash);
            }
        }
        createFileSelector(lastDir);

    }

    private void createFileSelector(final String path) {
        Preference filePathPreference = (Preference) findPreference(TextwidgetSettingsActivity.PREF_FILE_PATH);
        filePathPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            public boolean onPreferenceClick(Preference preference) {

                boolean useFileSelector = true;
                if (useFileSelector) {
                    Intent intent = new Intent(getBaseContext(), FileDialog.class);
                    intent.putExtra(FileDialog.START_PATH, path);
                    startActivityForResult(intent, FILE_CHOICE_PATH);
                } else {
                    // TODO [High] We need a FileObserver for actual file change observe actions elsewhere, but we need to work with URIs in all other circumstances
                    // See other branch
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("text/*");
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent = Intent.createChooser(intent, "Choose a file");
                    startActivityForResult(intent, FILE_CHOICE_URI);
                }

                return true;
            }
        });
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case FILE_CHOICE_PATH:
                if (resultCode != RESULT_CANCELED) {
                    String path = data.getStringExtra(FileDialog.RESULT_PATH);
                    updatePath(path);
                }

                break;
            case FILE_CHOICE_URI:
                if (resultCode != RESULT_CANCELED) {
                    Uri uri = data.getData();
                    assert uri != null;
                    Log.i(TextwidgetProvider.TAG, "Got uri: " + uri.toString());

                    updateUri(uri);
                }

                break;
        }
    }

    private void updatePath(String path) {
        Preference fileSelectPreference = (Preference) findPreference(PREF_FILE_PATH);
        Log.i(TextwidgetProvider.TAG, "Got path: " + path);
        editor.putString(PREF_FILE_PATH, path).commit();

        Log.d(TextwidgetProvider.TAG, "Stored: " + path);
        String newStoredPath = sharedPreferences.getString(PREF_FILE_PATH, "No file selected");
        fileSelectPreference.setSummary(newStoredPath);
    }

    private void updateUri(Uri uri) {
        String path = getRealPathFromURI(uri);
        updatePath(path);
    }

    /**
     * Tries to convert an uri to a file path, using the content resolver of the context.
     * TODO [High] Deprecated
     *
     * @deprecated Probably doesn't work anymore in Android 10
     *
     * @param uri
     * @return
     */
    private String getRealPathFromURI(Uri uri) {
        String[] proj = { MediaStore.Images.Media.DATA };
        Cursor cursor = getContentResolver().query(uri, proj, null, null, null);

        String path = uri.getPath();

        if (cursor != null) {
            boolean hasResults = cursor.moveToFirst();
            if (hasResults) {
                int index = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
                path = cursor.getString(index);
            } else {
                Log.i(TextwidgetProvider.TAG, "No data found for contentUri: " + uri.toString());
            }
            cursor.close();
        }

        if (path == null) {
            Log.i(TextwidgetProvider.TAG, "Path returned by realPathFromUri null for contentUri: " + uri.toString());
        }

        return path;
    }

    public void onPause() {
        Context context = this.getApplicationContext();
        Intent settingsIntent = new Intent(context, TextwidgetProvider.class);
        settingsIntent.setAction(TextwidgetProvider.UPDATE);
        context.sendBroadcast(settingsIntent);
        super.onPause();

    }

}
