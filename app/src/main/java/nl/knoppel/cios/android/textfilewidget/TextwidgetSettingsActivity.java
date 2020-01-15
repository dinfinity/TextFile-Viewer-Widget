package nl.knoppel.cios.android.textfilewidget;

import android.appwidget.AppWidgetManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.text.Html.TagHandler;
import android.util.Log;
import android.widget.Toast;

import com.lamerman.FileDialog;

public class TextwidgetSettingsActivity extends PreferenceActivity {

    public static final String WIDGET_PREFS_PREFIX = "textWidget_";
    public static final String PREF_FILE_PATH = "filePath";
    public static final String PREF_FILE_URI = "fileUri";
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

        Preference fileSelectPreference = (Preference) findPreference(PREF_FILE_URI);
        String path = sharedPreferences.getString(PREF_FILE_URI, "No file selected");
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
        if (filePathPreference != null) {
            filePathPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {

                public boolean onPreferenceClick(Preference preference) {

                    Intent intent = new Intent(getBaseContext(), FileDialog.class);
                    intent.putExtra(FileDialog.START_PATH, path);
                    startActivityForResult(intent, FILE_CHOICE_PATH);

                    return true;
                }
            });
        }

        Preference fileUriPreference = (Preference) findPreference(TextwidgetSettingsActivity.PREF_FILE_URI);
        fileUriPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.setType("text/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
//                intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                intent = Intent.createChooser(intent, "Choose a file");
                startActivityForResult(intent, FILE_CHOICE_URI);

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
                    if (uri != null) {
                        Log.i(TextwidgetProvider.TAG, "Got uri: " + uri.toString());

                        int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
                        ContentResolver resolver = getContentResolver();
                        resolver.takePersistableUriPermission(uri, takeFlags);

                        updateUri(uri.toString());
                    }
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

    private void updateUri(String uri) {
        Preference fileSelectPreference = (Preference) findPreference(PREF_FILE_URI);
        editor.putString(PREF_FILE_URI, uri).commit();

        Log.d(TextwidgetProvider.TAG, "Stored: " + uri);
        String newStoredUri = sharedPreferences.getString(PREF_FILE_URI, "No file selected");
        fileSelectPreference.setSummary(newStoredUri);
    }

    public void onPause() {
        Context context = this.getApplicationContext();
        Intent settingsIntent = new Intent(context, TextwidgetProvider.class);
        settingsIntent.setAction(TextwidgetProvider.UPDATE);
        context.sendBroadcast(settingsIntent);
        super.onPause();
    }

}
