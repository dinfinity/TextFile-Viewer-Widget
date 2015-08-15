/*package nl.knoppel.cios.android.textfilewidget;

import java.util.HashMap;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService.RemoteViewsFactory;

public class ListViewFactory implements RemoteViewsFactory {

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
*/