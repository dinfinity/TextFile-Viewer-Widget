package nl.knoppel.cios.android.textfilewidget;

import java.io.File;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Vibrator;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.core.app.JobIntentService;

public class TextwidgetProvider extends AppWidgetProvider {
	public static final String TAG = "Textfile_Widget";
	public static final String CLICK_EDIT = "nl.knoppel.cios.android.textwidget.TextwidgetProvider.CLICK_EDIT";
	public static final String CLICK_SETTINGS = "nl.knoppel.cios.android.textwidget.TextwidgetProvider.CLICK_SETTINGS";
	public static final String CLICK_REFRESH = "nl.knoppel.cios.android.textwidget.TextwidgetProvider.CLICK_REFRESH";
	public static final String UPDATE = "nl.knoppel.cios.android.textwidget.TextwidgetProvider.UPDATE";

	public void onEnabled(Context context) {
		Log.d(TextwidgetProvider.TAG, "OnEnabled");

		//TODO Perhaps add click intents here as well
//		RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.main);
//		attachButtonListeners(context, currentAppWidgetId, rv);
	}


	/**
	 * @link https://developer.android.com/guide/topics/appwidgets#java
	 * @param context
	 * @param appWidgetManager
	 * @param appWidgetIds
	 */
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		Log.d(TextwidgetProvider.TAG, "OnUpdate");

		super.onUpdate(context, appWidgetManager, appWidgetIds);

		for (int i = 0; i < appWidgetIds.length; ++i) {
			int currentAppWidgetId = appWidgetIds[i];

//			Intent intent = new Intent(context, TextwidgetRemoteViewsService.class);
//			intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, currentAppWidgetId);
//			intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));

			RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.main);
			attachButtonListeners(context, currentAppWidgetId, rv);

			//			rv.setRemoteAdapter(R.id.textContainer, intent);
//			rv.setEmptyView(R.id.textContainer, R.id.loadingView);

			appWidgetManager.updateAppWidget(currentAppWidgetId, rv);

			startUpdate(context, currentAppWidgetId);
		}
		super.onUpdate(context, appWidgetManager, appWidgetIds);

	}

	/**
	 *
	 * @param context
	 * @param currentAppWidgetId
	 * @param rv
	 */
	public static void attachButtonListeners(Context context, int currentAppWidgetId, RemoteViews rv) {
		//=-- Attach edit button listener
		Intent viewIntent = new Intent(context, TextwidgetProvider.class);
		viewIntent.setAction(TextwidgetProvider.CLICK_EDIT);
		viewIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, currentAppWidgetId);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(context, currentAppWidgetId, viewIntent, PendingIntent.FLAG_CANCEL_CURRENT);
		rv.setOnClickPendingIntent(R.id.editButton, pendingIntent);

		//=-- Attach refreshbutton listener
		Intent refreshIntent = new Intent(context, TextwidgetProvider.class);
		refreshIntent.setAction(TextwidgetProvider.CLICK_REFRESH);
		refreshIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, currentAppWidgetId);
		PendingIntent refreshPendingIntent = PendingIntent.getBroadcast(context, currentAppWidgetId, refreshIntent, PendingIntent.FLAG_CANCEL_CURRENT);
		rv.setOnClickPendingIntent(R.id.refreshButton, refreshPendingIntent);

		//=-- Attach settings button listener
		Intent settingsIntent = new Intent(context, TextwidgetProvider.class);
		settingsIntent.setAction(TextwidgetProvider.CLICK_SETTINGS);
		settingsIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, currentAppWidgetId);
		PendingIntent settingsPendingIntent = PendingIntent.getBroadcast(context, currentAppWidgetId, settingsIntent, PendingIntent.FLAG_CANCEL_CURRENT);
		rv.setOnClickPendingIntent(R.id.settingsButton, settingsPendingIntent);
	}

	/**
	 *
	 * @param context
	 */
	private void startUpdate(Context context, int appWidgetId) {
		int JOB_ID = 88;
		Intent intent = new Intent(context, TextwidgetProvider.class);
		intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		JobIntentService.enqueueWork(context, TextwidgetUpdateService.class, JOB_ID, intent);
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(TextwidgetProvider.TAG, "OnReceive");

		super.onReceive(context, intent);
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
			startUpdate(context, appWidgetId);
			
		} else if (action.equals(UPDATE)) {

			startUpdate(context,appWidgetId);

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
		Log.d(TextwidgetProvider.TAG, "StartSettings");
		Intent settingsIntent = new Intent(context, TextwidgetSettingsActivity.class);
		settingsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		settingsIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		context.startActivity(settingsIntent);
	}
}