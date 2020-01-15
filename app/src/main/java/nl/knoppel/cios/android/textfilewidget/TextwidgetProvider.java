package nl.knoppel.cios.android.textfilewidget;

import java.io.File;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Vibrator;
import android.util.Log;

public class TextwidgetProvider extends AppWidgetProvider {
	public static final String TAG = "Textfile_Widget";
	public static final String CLICK_EDIT = "nl.knoppel.cios.android.textwidget.TextwidgetProvider.CLICK_EDIT";
	public static final String CLICK_SETTINGS = "nl.knoppel.cios.android.textwidget.TextwidgetProvider.CLICK_SETTINGS";
	public static final String CLICK_REFRESH = "nl.knoppel.cios.android.textwidget.TextwidgetProvider.CLICK_REFRESH";
	public static final String UPDATE = "nl.knoppel.cios.android.textwidget.TextwidgetProvider.UPDATE";

//	private static HashMap<Integer, FileObserver> observers = new HashMap<Integer, FileObserver>();
//	private static HashMap<Integer, String> fileData = new HashMap<Integer, String>();

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		TextwidgetProviderUpdateService.appWidgetIds = appWidgetIds;
		startUpdate(context);
		super.onUpdate(context, appWidgetManager, appWidgetIds);
	}

	public void startUpdate(Context context) {
		context.startService(new Intent(context, TextwidgetProviderUpdateService.class));
	}

	@Override
	public void onReceive(Context context, Intent intent) {
//		RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.main);

		String action = intent.getAction();
		Log.d(TAG, "Received intent with action: "+ action);
		int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);

		if (action.equals(CLICK_EDIT)) {
			Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
			v.vibrate(100);
			
			SharedPreferences preferences = context.getSharedPreferences(TextwidgetSettingsActivity.WIDGET_PREFS_PREFIX + appWidgetId, Context.MODE_PRIVATE);
			String uri = preferences.getString(TextwidgetSettingsActivity.PREF_FILE_URI, "");

			startEditor(context, uri);
			
		} else if (action.equals(CLICK_SETTINGS)) {
			
			Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
			v.vibrate(100);

			startSettings(context, intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1));
			
		} else if (action.equals(CLICK_REFRESH)) {
			startUpdate(context);
			
		} else if (action.equals(UPDATE)) {

			startUpdate(context);

		} else {
			super.onReceive(context, intent);
		}
	}

	@Override
	public void onDeleted(Context context, int[] appWidgetIds) {
		for (int appWidgetId : appWidgetIds) {
			cleanupWidget(context, appWidgetId);
		}
	}

	/**
	 * Removes things associated with the specified widgetId
	 * @param context
	 * @param appWidgetId
	 */
	private void cleanupWidget(Context context, int appWidgetId) {
		// SharedPreferences preferences =
		// context.getSharedPreferences(TextwidgetSettingsActivity.WIDGET_PREFS_PREFIX
		// + appWidgetId, Context.MODE_PRIVATE);
		// preferences.edit().clear().commit();

		String path = context.getApplicationInfo().dataDir + "/shared_prefs/" + TextwidgetSettingsActivity.WIDGET_PREFS_PREFIX + appWidgetId + ".xml";
		File settingsFile = new File(path);
		settingsFile.delete();

	}

	/**
	 * Starts the text editor
	 * @param context
	 * @param uriString
	 */
	private void startEditor(Context context, String uriString) {
		Uri uri = Uri.parse(uriString);
		Intent textEditorIntent = new Intent(Intent.ACTION_EDIT);
		textEditorIntent.setDataAndType(uri, "text/plain");
		textEditorIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		textEditorIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		textEditorIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
		context.startActivity(textEditorIntent);
	}

	/**
	 * Starts the preferences activity
	 * @param context
	 * @param appWidgetId
	 */
	private void startSettings(Context context, int appWidgetId) {
		Intent settingsIntent = new Intent(context, TextwidgetSettingsActivity.class);
		settingsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		settingsIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		context.startActivity(settingsIntent);
	}
}