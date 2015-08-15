package nl.knoppel.cios.android.textfilewidget;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.FileObserver;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;


/**
 * Handles the updating of the widgets
 * @author Dual Infinity
 *
 */
public class TextwidgetProviderUpdateService extends RemoteViewsService {

	
	public class ListViewFactory implements RemoteViewsService.RemoteViewsFactory {

		private Context mContext;
		private int mAppWidgetId;
		private HashMap<Integer, String> textContents;

		public ListViewFactory(Context context, Intent intent) {
		        mContext = context;
		        mAppWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,AppWidgetManager.INVALID_APPWIDGET_ID);
		    }

		
		@Override
		public int getCount() {
			return 1;
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		@Override
		public RemoteViews getLoadingView() {
			 Log.e(TextwidgetProvider.TAG, "LOADINK");		
		    RemoteViews rv = new RemoteViews(mContext.getPackageName(), R.layout.text_view);
		    rv.setTextViewText(R.id.textView, "LOADING");

		    // Return the remote views object.
		    return rv;
		}

		@Override
		public RemoteViews getViewAt(int position) {
			 Log.e(TextwidgetProvider.TAG, "GETVIEWAT");
		    // Construct a remote views item based on the app widget item XML file, 
		    // and set the text based on the position.
		    RemoteViews rv = new RemoteViews(mContext.getPackageName(), R.layout.text_view);
		    rv.setTextViewText(R.id.textView, "TEST");

		    // Return the remote views object.
		    return rv;
		}
		
		@Override
		public int getViewTypeCount() {
			// TODO Auto-generated method stub
			return 1;
		}

		@Override
		public boolean hasStableIds() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void onCreate() {
			// TODO Auto-generated method stub

		}

		@Override
		public void onDataSetChanged() {
		}

		@Override
		public void onDestroy() {
		}

		public void setTextContents(HashMap<Integer, String> textContents) {
			Log.e(TextwidgetProvider.TAG, "SETTEXT");
			this.textContents = textContents;
		}

	}
	
	
	private HashMap<Integer, FileObserver> observers = new HashMap<Integer, FileObserver>();
	private HashMap<Integer, String> paths = new HashMap<Integer, String>();
	private HashMap<Integer, String> textContents = new HashMap<Integer, String>();
	
	private static final int MAX_LINES = 100;
	static int[] appWidgetIds;
	public static final String TAG = "Textfile_Widget";

	@Override
	public void onStart(Intent intent, int startId) {
		doUpdate(this);
	}

	/**
	 * Starts the update for the registered widgets
	 * @param context
	 */
	public void doUpdate(Context context) {
		RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.main);

		if (getAppWidgetIds() != null) {
			for (int appWidgetId : getAppWidgetIds()) {
				updateWidget(context, views, appWidgetId);
			}
		}
	}
	
	/**
	 * Updates the widget with the correct file contents. Also observes the file for modifications
	 * @param context
	 * @param views
	 * @param appWidgetId
	 */
	public void updateWidget(final Context context, final RemoteViews views, final int appWidgetId) {
		Log.i(TAG, "Updating widget: " + appWidgetId);
		AppWidgetManager manager = AppWidgetManager.getInstance(this);
		SharedPreferences preferences = context.getSharedPreferences(TextwidgetSettingsActivity.WIDGET_PREFS_PREFIX + appWidgetId, Context.MODE_PRIVATE);
		
		String path = preferences.getString("filename", "");
		final File file = new File(path);
		String readText = "";
		
		// =-- Start file observer if necessary
		FileObserver previousObserver = observers.get(appWidgetId);
		String previousPath = paths.get(appWidgetId);
		Log.d(TAG, "Paths: " + previousPath + " - "+path);
		boolean notObservingYet = previousObserver == null; 
		
		if (path != null && (notObservingYet || previousPath == null || !previousPath.equals(path))) {
			final Handler handler = new Handler();
			if (previousObserver != null) {
				previousObserver.stopWatching();
				this.observers.remove(appWidgetId);
//				TextwidgetProvider.observers.remove(appWidgetId);
				Log.i(TAG, "Removed observer for widget: " + appWidgetId);
			}
			
			FileObserver observer = new FileObserver(path, FileObserver.MODIFY) {

				@Override
				public void onEvent(final int event, final String path) {
					handler.post(new Runnable() {

						@Override
						/**
						 * Update the widget if the file has changed
						 */
						public void run() {
							Log.i(TAG, "File modified, initiating update for widget - "+appWidgetId);
							updateWidget(context, views, appWidgetId);
							
							boolean notifyDropbox = false;
							if (notifyDropbox) {
								notifyDropbox(context, file);
							}

						}

						/**
						 *	Meant to notify the dropbox package of changes to the file
						 */
						private void notifyDropbox(final Context context, final File file) {
							Intent dbIntent = new Intent(android.content.Intent.ACTION_SEND);
							dbIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
							dbIntent.setType("*/*");
							dbIntent.setPackage("com.dropbox.android");
							dbIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							context.startActivity(dbIntent);
						}
					});

				}
			};
			Log.i(TAG, "Observing modification for path: " + path);
			observer.startWatching();
			this.observers.put(appWidgetId, observer);
//			TextwidgetProvider.observers.put(appWidgetId, observer);
			this.paths.put(appWidgetId, path);
		}

		if (file == null || file.isDirectory()) {
			textContents.put(appWidgetId, "Erroneous input file.");
			Log.d(TAG, "Erroneous input file.");
		} else {
		
			try {
				BufferedReader reader = new BufferedReader(new FileReader(file));
				String line = reader.readLine();
				int linesRead = 1;
				while (line != null && linesRead < MAX_LINES) {
					readText += line + "\r\n";
					line = reader.readLine();
					linesRead++;
					textContents.put(linesRead, line);
				}
				this.paths.put(appWidgetId, path);
				reader.close();

			} catch (FileNotFoundException e) {
				textContents.put(appWidgetId, "File not found.");
				Log.d(TAG, e.toString());
			} catch (IOException e) {
				textContents.put(appWidgetId, "Error opening file.");
				Log.d(TAG, e.toString());
			} catch (Exception e) {
				textContents.put(appWidgetId, "Unknown error: " + e.getMessage());
				Log.d(TAG, e.toString());
			}
		}
		
		Log.d(TAG, "File successfully read.");
		
		//=-- Link this service to remoteviews and the listview
//		int randomNumber = (int)(Math.random()*10000);
//		Intent thisIntent = new Intent(context,TextwidgetProviderUpdateService.class);
//		thisIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
//		thisIntent.putExtra("random", randomNumber);
//		thisIntent.setData(Uri.parse(thisIntent.toUri(Intent.URI_INTENT_SCHEME)));
//		views.setRemoteAdapter(R.id.listView1, thisIntent);
		
		//=-- Set content text
		views.setTextViewText(R.id.textContainer, readText);
		
		//=-- Set title
		views.setTextViewText(R.id.titleContainer, file.getName());

		//=-- Attach edit button listener
		Intent viewIntent = new Intent(context, TextwidgetProvider.class);
		viewIntent.setAction(TextwidgetProvider.CLICK_EDIT);
		viewIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(context, appWidgetId, viewIntent, 0);
		views.setOnClickPendingIntent(R.id.ImageButton02, pendingIntent);

		//=-- Attach settings button listener
		Intent settingsIntent = new Intent(context, TextwidgetProvider.class);
		settingsIntent.setAction(TextwidgetProvider.CLICK_SETTINGS);
		settingsIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		PendingIntent settingsPendingIntent = PendingIntent.getBroadcast(context, appWidgetId, settingsIntent, 0);
		views.setOnClickPendingIntent(R.id.ImageButton01, settingsPendingIntent);

		//=-- Do Android widget update
		Log.d(TAG, "Calling Android update.");
		manager.updateAppWidget(appWidgetId, views);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public RemoteViewsFactory onGetViewFactory(Intent intent) {
		Log.d(TAG, "ONGETVIEW.");
		ListViewFactory factory = new ListViewFactory(this.getApplicationContext(), intent);
//		factory.setTextContents(textContents);
		return factory;
	}

	public static int[] getAppWidgetIds() {
		return appWidgetIds;
	}

	public static void setAppWidgetIds(int[] appWidgetIds) {
		TextwidgetProviderUpdateService.appWidgetIds = appWidgetIds;
	}

}