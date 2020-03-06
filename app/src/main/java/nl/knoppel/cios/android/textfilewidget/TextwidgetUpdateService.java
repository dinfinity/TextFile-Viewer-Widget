package nl.knoppel.cios.android.textfilewidget;

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
import android.os.HandlerThread;
import android.provider.OpenableColumns;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.core.app.JobIntentService;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

/**
 * Handles the updating of the widgets
 *
 * @author Dual Infinity
 */
public class TextwidgetUpdateService extends JobIntentService {

    private HashMap<Integer, ContentObserver> observers = new HashMap<>();
    private HashMap<Integer, String> uris = new HashMap<>();

    private static final int MAX_LINES = 1000;
    public static final String TAG = "Textfile_Widget";

    @Override
    protected void onHandleWork(Intent intent) {
        int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
        Log.i(TAG, "[UpdateService] OnHandleWork for widget: " + appWidgetId);
        updateWidget(this, appWidgetId);
    }

    /**
     * Called by the system to notify a Service that it is no longer used and is being removed.  The
     * service should clean up any resources it holds (threads, registered
     * receivers, etc) at this point.  Upon return, there will be no more calls
     * in to this Service object and it is effectively dead.  Do not call this method directly.
     */
    public void onDestroy() {
        Log.d(TextwidgetProvider.TAG, "[UpdateService] Service onDestroy");
        ContentResolver contentResolver = getContentResolver();
        for (ContentObserver observer: observers.values()) {
            contentResolver.unregisterContentObserver(observer);
        }
    }

    /**
     * Updates the widget with the correct file contents. Also observes the file for modifications
     *
     * @param context
     * @param appWidgetId
     */
    public void updateWidget(final Context context, final int appWidgetId) {
        final RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.main);
        TextwidgetProvider.attachButtonListeners(context, appWidgetId, views);

        Log.i(TAG, "[UpdateService] Updating widget: " + appWidgetId);
        AppWidgetManager manager = AppWidgetManager.getInstance(this);
        SharedPreferences preferences = context.getSharedPreferences(TextwidgetSettingsActivity.WIDGET_PREFS_PREFIX + appWidgetId, Context.MODE_PRIVATE);

        String uriString = preferences.getString(TextwidgetSettingsActivity.PREF_FILE_URI, "");
        Log.d(TAG, "[UpdateService] Stored uri: " + uriString);
        String previousUriString = uris.get(appWidgetId);
        Log.d(TAG, "[UpdateService] Previous uri: " + previousUriString);
        Uri uri = Uri.parse(uriString);

        // =-- Start file observer if necessary
//        uri = addFileObserverToUri(appWidgetId, context, previousUriString, uri);

        //=-- Read text from uri
        StringBuilder readText = readTextFromUri(appWidgetId, uri);

        //=-- Set content text
        views.setTextViewText(R.id.textContainer, readText.toString());

        //=-- Set title
        if (uri != null) {
            views.setTextViewText(R.id.titleContainer, getFileName(uri));
        }

        //=-- Do Android widget update
        Log.d(TAG, "[UpdateService] Calling Android update.");
        manager.updateAppWidget(appWidgetId, views);
    }

    /**
     *
     * @param appWidgetId
     * @param context
     * @param previousUriString
     * @param uri
     * @return
     */
    private Uri addFileObserverToUri(final int appWidgetId, final Context context, String previousUriString, Uri uri) {
        ContentObserver previousObserver = observers.get(appWidgetId);
        String uriString = uri.toString();
        boolean notObservingYet = previousObserver == null;

        if (uri != null && (notObservingYet || previousUriString == null || !previousUriString.equals(uriString))) {
            // creates and starts a new thread set up as a looper
            HandlerThread thread = new HandlerThread("MyHandlerThread");
            thread.start();

            // creates the handler using the passed looper
            final Handler handler = new Handler(thread.getLooper());

            if (previousObserver != null) {

                getContentResolver().unregisterContentObserver(previousObserver);
                this.observers.remove(appWidgetId);
//				TextwidgetProvider.observers.remove(appWidgetId);
                Log.i(TAG, "[UpdateService] Removed observer for widget: " + appWidgetId);
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
                            Log.i(TAG, "[UpdateService] File modified, initiating update for widget - " + appWidgetId);
                            updateWidget(context, appWidgetId);
                        }
                    });

                }
            };
            Log.i(TAG, "[UpdateService] Observing modification for uri: " + uriString);
            try {
                getContentResolver().registerContentObserver(uri, false, observer);
                this.observers.put(appWidgetId, observer);
//			TextwidgetProvider.observers.put(appWidgetId, observer);
                this.uris.put(appWidgetId, uriString);
            } catch (IllegalArgumentException e) {
                uri = null;
            }
        }
        return uri;
    }

    /**
     *
     * @param appWidgetId
     * @param uri
     * @return
     */
    private StringBuilder readTextFromUri(int appWidgetId, Uri uri) {
        StringBuilder readText = new StringBuilder();
        if (uri == null) {
            readText.append("Erroneous input file.");
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
                }
                Log.d(TAG, "[UpdateService] File successfully read.");
                this.uris.put(appWidgetId, uri.toString());

            } catch (FileNotFoundException e) {
                readText.append("File not found.");
                Log.d(TAG, e.toString());
            } catch (IOException e) {
                readText.append("Error opening file.");
                Log.d(TAG, e.toString());
            } catch (Exception e) {
                readText.append("Unknown error: ").append(e.getMessage());
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
        return readText;
    }

    /**
     *
     * @param uri
     * @return
     */
    private String getFileName(Uri uri) {
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
}