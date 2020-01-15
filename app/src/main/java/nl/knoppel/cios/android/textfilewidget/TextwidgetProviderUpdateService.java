package nl.knoppel.cios.android.textfilewidget;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.provider.OpenableColumns;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;


/**
 * Handles the updating of the widgets
 *
 * @author Dual Infinity
 */
public class TextwidgetProviderUpdateService extends RemoteViewsService {


    public class ListViewFactory implements RemoteViewsService.RemoteViewsFactory {

        private Context mContext;
        private int mAppWidgetId;
        private HashMap<Integer, String> textContents;

        public ListViewFactory(Context context, Intent intent) {
            mContext = context;
            mAppWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
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
            Log.d(TextwidgetProvider.TAG, "Loading");
            RemoteViews rv = new RemoteViews(mContext.getPackageName(), R.layout.loading_view);
            rv.setTextViewText(R.id.loadingView, "LOADING");

            // Return the remote views object.
            return rv;
        }

        @Override
        public RemoteViews getViewAt(int position) {
            Log.d(TextwidgetProvider.TAG, "ListViewFactory getViewAt");
            // Construct a remote views item based on the app widget item XML file,
            // and set the text based on the position.
            RemoteViews rv = new RemoteViews(mContext.getPackageName(), R.layout.loading_view);

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
            Log.d(TextwidgetProvider.TAG, "ListViewFactory created");
        }

        public void setTextContents(HashMap<Integer, String> textContents) {
            Log.d(TextwidgetProvider.TAG, "ListViewFactory setText");
            this.textContents = textContents;
        }

    }


    private HashMap<Integer, ContentObserver> observers = new HashMap<>();
    private HashMap<Integer, String> uris = new HashMap<>();
    private HashMap<Integer, String> textContents = new HashMap<>();

    private static final int MAX_LINES = 1000;
    static int[] appWidgetIds;
    public static final String TAG = "Textfile_Widget";

    @Override
    public void onStart(Intent intent, int startId) {
        doUpdate(this);
    }

    /**
     * Called by the system to notify a Service that it is no longer used and is being removed.  The
     * service should clean up any resources it holds (threads, registered
     * receivers, etc) at this point.  Upon return, there will be no more calls
     * in to this Service object and it is effectively dead.  Do not call this method directly.
     */
    public void onDestroy() {
        Log.d(TextwidgetProvider.TAG, "Service onDestroy");
        ContentResolver contentResolver = getContentResolver();
        for (ContentObserver observer: observers.values()) {
            contentResolver.unregisterContentObserver(observer);
        }
    }

    /**
     * Starts the update for the registered widgets
     *
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
     *
     * @param context
     * @param views
     * @param appWidgetId
     */
    public void updateWidget(final Context context, final RemoteViews views, final int appWidgetId) {
        Log.i(TAG, "Updating widget: " + appWidgetId);
        AppWidgetManager manager = AppWidgetManager.getInstance(this);
        SharedPreferences preferences = context.getSharedPreferences(TextwidgetSettingsActivity.WIDGET_PREFS_PREFIX + appWidgetId, Context.MODE_PRIVATE);

        String uriString = preferences.getString(TextwidgetSettingsActivity.PREF_FILE_URI, "");
        Uri uri = Uri.parse(uriString);
        StringBuilder readText = new StringBuilder();

        // =-- Start file observer if necessary
        ContentObserver previousObserver = observers.get(appWidgetId);
        String previousUri = uris.get(appWidgetId);
        Log.d(TAG, "Uris: " + previousUri + " - " + uriString);
        boolean notObservingYet = previousObserver == null;

        if (uri != null && (notObservingYet || previousUri == null || !previousUri.equals(uriString))) {
            final Handler handler = new Handler();
            if (previousObserver != null) {

                getContentResolver().unregisterContentObserver(previousObserver);
                this.observers.remove(appWidgetId);
//				TextwidgetProvider.observers.remove(appWidgetId);
                Log.i(TAG, "Removed observer for widget: " + appWidgetId);
            }

            ContentObserver observer = new ContentObserver(handler) {
                @Override
                public void onChange(boolean selfChange) {
                    handler.post(new Runnable() {

                        @Override
                        /**
                         * Update the widget if the file has changed
                         */
                        public void run() {
                            Log.i(TAG, "File modified, initiating update for widget - " + appWidgetId);
                            updateWidget(context, views, appWidgetId);
                        }
                    });

                }
            };
            Log.i(TAG, "Observing modification for uri: " + uriString);
            try {
                getContentResolver().registerContentObserver(uri, false, observer);
                this.observers.put(appWidgetId, observer);
//			TextwidgetProvider.observers.put(appWidgetId, observer);
                this.uris.put(appWidgetId, uriString);
            } catch (IllegalArgumentException e) {
                uri = null;
            }
        }

        if (uri == null) {
            textContents.put(appWidgetId, "Erroneous input file.");
            Log.d(TAG, "Erroneous input file.");
        } else {

            InputStream inputStream = null;
            InputStreamReader isReader = null;
            BufferedReader reader = null;
            try {
                inputStream = getContentResolver().openInputStream(uri);
                isReader = new InputStreamReader(inputStream);
                reader = new BufferedReader(isReader);
                String line = reader.readLine();
                int linesRead = 1;
                while (line != null && linesRead < MAX_LINES) {
                    readText.append(line).append("\r\n");
                    line = reader.readLine();
                    linesRead++;
                    textContents.put(linesRead, line);
                }
                this.uris.put(appWidgetId, uriString);

            } catch (FileNotFoundException e) {
                textContents.put(appWidgetId, "File not found.");
                Log.d(TAG, e.toString());
            } catch (IOException e) {
                textContents.put(appWidgetId, "Error opening file.");
                Log.d(TAG, e.toString());
            } catch (Exception e) {
                textContents.put(appWidgetId, "Unknown error: " + e.getMessage());
                Log.d(TAG, e.toString());
            } finally {
                try {
                    if (reader != null) {
                        reader.close();
                    }
                    if (isReader != null) {
                        isReader.close();
                    }
                    if (inputStream != null) {
                        inputStream.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
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
        views.setTextViewText(R.id.textContainer, readText.toString());

        //=-- Set title
        if (uri != null) {
            views.setTextViewText(R.id.titleContainer, getFileName(uri));
        }

        //=-- Attach update intent
        Intent updateIntent = new Intent(context, TextwidgetProvider.class);
        updateIntent.setAction(TextwidgetProvider.UPDATE);
        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        PendingIntent updatePendingIntent = PendingIntent.getBroadcast(context, appWidgetId, updateIntent, 0);
        views.setOnClickPendingIntent(R.id.titleContainer, updatePendingIntent);

        //=-- Attach edit button listener
        Intent viewIntent = new Intent(context, TextwidgetProvider.class);
        viewIntent.setAction(TextwidgetProvider.CLICK_EDIT);
        viewIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, appWidgetId, viewIntent, 0);
        views.setOnClickPendingIntent(R.id.editButton, pendingIntent);

        //=-- Attach refreshbutton listener
        Intent refreshIntent = new Intent(context, TextwidgetProvider.class);
        refreshIntent.setAction(TextwidgetProvider.CLICK_REFRESH);
        refreshIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        PendingIntent refreshPendingIntent = PendingIntent.getBroadcast(context, appWidgetId, refreshIntent, 0);
        views.setOnClickPendingIntent(R.id.refreshButton, refreshPendingIntent);

        //=-- Attach settings button listener
        Intent settingsIntent = new Intent(context, TextwidgetProvider.class);
        settingsIntent.setAction(TextwidgetProvider.CLICK_SETTINGS);
        settingsIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        PendingIntent settingsPendingIntent = PendingIntent.getBroadcast(context, appWidgetId, settingsIntent, 0);
        views.setOnClickPendingIntent(R.id.settingsButton, settingsPendingIntent);

        //=-- Do Android widget update
        Log.d(TAG, "Calling Android update.");
        manager.updateAppWidget(appWidgetId, views);
    }

    public String getFileName(Uri uri) {
        String result = null;
        if (uri != null) {
            if (uri.getScheme().equals("content")) {
                Cursor cursor = null;
                try {
                    cursor = getContentResolver().query(uri, null, null, null, null);
                    if (cursor != null && cursor.moveToFirst()) {
                        result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                    }
                } catch (Exception e) {
                    //=-- Best effort
                    Log.e(TextwidgetProvider.TAG, e.getMessage());
                }
                finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            }
            if (result == null) {
                result = uri.getPath();
                int cut = result.lastIndexOf('/');
                if (cut != -1) {
                    result = result.substring(cut + 1);
                }
            }
        }
        return result;
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