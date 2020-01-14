package nl.knoppel.cios.android.textfilewidget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
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
	protected static final int FILE_CHOICE = 1;
	private int widgetId;
	private SharedPreferences sharedPreferences;
	private Editor editor;
	private String fileSelectKey;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		widgetId = getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
		this.getPreferenceManager().setSharedPreferencesName(WIDGET_PREFS_PREFIX + widgetId);
		addPreferencesFromResource(R.xml.preferences);
		this.sharedPreferences = getPreferenceManager().getSharedPreferences();
		this.editor = sharedPreferences.edit();
		
		Preference fileSelectPreference = (Preference) findPreference("filename");
		this.fileSelectKey = fileSelectPreference.getKey();
		String path = sharedPreferences.getString(fileSelectKey, "No file selected");
		fileSelectPreference.setSummary(path);

		String lastDir = Environment.getExternalStorageDirectory().getPath();
		if (path != null && !path.equals("")) {
			int lastsSlash = path.lastIndexOf("/");
			if (lastsSlash > 0) {
				lastDir = path.substring(0,lastsSlash);
			}
		}
		createFileSelector(lastDir);

	}

	private void createFileSelector(final String path) {
		Preference fileSelectPreference = (Preference) findPreference("filename");
		fileSelectPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			public boolean onPreferenceClick(Preference preference) {

				Intent intent = new Intent(getBaseContext(), FileDialog.class);
				intent.putExtra(FileDialog.START_PATH, path);
				startActivityForResult(intent, FILE_CHOICE);

//				Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
//				intent.setDataAndType(Uri.fromFile(new File("/sdcard")), "*/*");
//				intent.addCategory(Intent.CATEGORY_OPENABLE);
//				intent = Intent.createChooser(intent, "Choose a file");
//				startActivityForResult(intent, FILE_CHOICE);

				return true;
			}

		});
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case FILE_CHOICE:
			switch (resultCode) {
			case RESULT_CANCELED:
				break;
			default:
				String path = data.getStringExtra(FileDialog.RESULT_PATH);
				
				Preference fileSelectPreference = (Preference) findPreference("filename");
				Log.i(TextwidgetProvider.TAG, "Got path: "+path);
				editor.putString(fileSelectKey, path).commit();
				
				Log.d(TextwidgetProvider.TAG, "Stored: "+path);
				String newStoredPath = sharedPreferences.getString(fileSelectKey, "No file selected");
				fileSelectPreference.setSummary(newStoredPath);
				Log.d(TextwidgetProvider.TAG, "Updated to: "+newStoredPath);
				break;
			}

			break;

		default:
			break;
		}
	}

	public void onReceive() {
	}

	public void onPause() {
		Context context = this.getApplicationContext();
		Intent settingsIntent = new Intent(context, TextwidgetProvider.class);
		settingsIntent.setAction(TextwidgetProvider.UPDATE);
		context.sendBroadcast(settingsIntent);
		super.onPause();

	}

}
