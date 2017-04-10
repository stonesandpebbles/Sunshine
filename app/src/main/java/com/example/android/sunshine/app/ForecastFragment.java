package com.example.android.sunshine.app;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.AppBarLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.example.android.sunshine.app.data.WeatherContract;
import com.example.android.sunshine.app.sync.SunshineSyncAdapter;

/**
 * Created by Abhijeet on 13-02-2016.
 */
public class ForecastFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, SharedPreferences.OnSharedPreferenceChangeListener{
    public static final String LOG_TAG = ForecastFragment.class.getSimpleName();
    private static final String[] FORECAST_COLUMNS = {
            // In this case the id needs to be fully qualified with a table name, since
            // the content provider joins the location & weather tables in the background
            // (both have an _id column)
            // On the one hand, that's annoying.  On the other, you can search the weather table
            // using the location set by the user, which is only in the Location table.
            // So the convenience is worth it.
            WeatherContract.WeatherEntry.TABLE_NAME + "." + WeatherContract.WeatherEntry._ID,
            WeatherContract.WeatherEntry.COLUMN_DATE,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING,
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.LocationEntry.COLUMN_COORD_LAT,
            WeatherContract.LocationEntry.COLUMN_COORD_LONG
    };

    // These indices are tied to FORECAST_COLUMNS.  If FORECAST_COLUMNS changes, these
    // must change.
    static final int COL_WEATHER_ID = 0;
    static final int COL_WEATHER_DATE = 1;
    static final int COL_WEATHER_DESC = 2;
    static final int COL_WEATHER_MAX_TEMP = 3;
    static final int COL_WEATHER_MIN_TEMP = 4;
    static final int COL_LOCATION_SETTING = 5;
    static final int COL_WEATHER_CONDITION_ID = 6;
    static final int COL_COORD_LAT = 7;
    static final int COL_COORD_LONG = 8;
    private static final String SELECTED_KEY = "position";
    private RecyclerView viewWeatherData;

    private ForecastAdapter adapterForWeatherData;
    private static final int LOADER_ID = 12345;
    private int mPosition = RecyclerView.NO_POSITION;
    private boolean mUseTodayLayout, mAutoSelectView;
    private int mChoiceMode;
    private boolean mHoldForTransition;
    private long mInitialSelectedDate = -1;

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String locationPref = Utility.getPreferredLocation(getActivity());
        String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATE + " ASC";
        Uri weatherLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(locationPref, System.currentTimeMillis());
        return new CursorLoader(getActivity(), weatherLocationUri,
                FORECAST_COLUMNS,
                null,
                null,
                sortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        adapterForWeatherData.swapCursor(data);
        if (mPosition != RecyclerView.NO_POSITION) {
            viewWeatherData.smoothScrollToPosition(mPosition);
        }
        updateEmptyView();
        if ( data.getCount() == 0 ) {
            getActivity().supportStartPostponedEnterTransition();
        } else {
            viewWeatherData.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    // Since we know we're going to get items, we keep the listener around until
                    // we see Children.
                    if (viewWeatherData.getChildCount() > 0) {
                        viewWeatherData.getViewTreeObserver().removeOnPreDrawListener(this);
                        int itemPosition = adapterForWeatherData.getSelectedItemPosition();
                        if ( RecyclerView.NO_POSITION == itemPosition ) itemPosition = 0;
                        RecyclerView.ViewHolder vh = viewWeatherData.findViewHolderForAdapterPosition(itemPosition);
                        if ( null != vh && mAutoSelectView ) {
                            adapterForWeatherData.selectView( vh );
                        }
                        if ( mHoldForTransition ) {
                            getActivity().supportStartPostponedEnterTransition();
                        }
                        return true;
                    }
                    return false;
                }
            });
        }

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        adapterForWeatherData.swapCursor(null);
    }

    /**
     * Called when the fragment is visible to the user and actively running.
     * This is generally
     * tied to {Activity#onResume() Activity.onResume} of the containing
     * Activity's lifecycle.
     */
    @Override
    public void onResume() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        sp.registerOnSharedPreferenceChangeListener(this);
        super.onResume();
    }

    /**
     * Called when the Fragment is no longer resumed.  This is generally
     * tied to { Activity.onPause} of the containing
     * Activity's lifecycle.
     */
    @Override
    public void onPause() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        sp.unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    public ForecastFragment() {
    }


    @Override
    public void onInflate(Activity activity, AttributeSet attrs, Bundle savedInstanceState) {
        super.onInflate(activity, attrs, savedInstanceState);
        TypedArray a = activity.obtainStyledAttributes(attrs, R.styleable.ForecastFragment,
                +                0, 0);
        mChoiceMode = a.getInt(R.styleable.ForecastFragment_android_choiceMode, AbsListView.CHOICE_MODE_NONE);
        mAutoSelectView = a.getBoolean(R.styleable.ForecastFragment_autoSelectView, false);
        mHoldForTransition = a.getBoolean(R.styleable.ForecastFragment_sharedElementTransitions, false);
        a.recycle();
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Add this line in order for this fragment to handle menu events.
        setHasOptionsMenu(true);
        //updateWeather();

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        // We hold for transition here just in-case the activity
        // needs to be re-created. In a standard return transition,
        // this doesn't actually make a difference.
        if ( mHoldForTransition ) {
            getActivity().supportPostponeEnterTransition();
        }
        getLoaderManager().initLoader(LOADER_ID, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    /**
     * Called to ask the fragment to save its current dynamic state, so it
     * can later be reconstructed in a new instance of its process is
     * restarted.  If a new instance of the fragment later needs to be
     * created, the data you place in the Bundle here will be available
     * in the Bundle given to {@link #onCreate(Bundle)},
     * {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}, and
     * {@link #onActivityCreated(Bundle)}.
     * <p/>
     * <p>This corresponds to
     * Activity.onSaveInstanceState(Bundle)} and most of the discussion there
     * applies here as well.  Note however: <em>this method may be called
     * at any time before {@link #onDestroy()}</em>.  There are many situations
     * where a fragment may be mostly torn down (such as when placed on the
     * back stack with no UI showing), but its state will not be saved until
     * its owning activity actually needs to save its state.
     *
     * @param outState Bundle in which to place your saved state.
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        if(mPosition != RecyclerView.NO_POSITION){
            outState.putInt(SELECTED_KEY, mPosition);
        }
        adapterForWeatherData.onSaveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        viewWeatherData = (RecyclerView) rootView.findViewById(R.id.recyclerview_forecast);

        View emptyView = rootView.findViewById(R.id.recyclerview_forecast_empty);
        viewWeatherData.setLayoutManager(new LinearLayoutManager(getActivity()));

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        viewWeatherData.setHasFixedSize(true);
        // The ForecastAdapter will take data from a source and
        // use it to populate the RecyclerView it's attached to.

        adapterForWeatherData = new ForecastAdapter(getActivity(), new ForecastAdapter.ForecastAdapterOnClickHandler() {
            @Override
            public void onClick(Long date, ForecastAdapter.ForecastAdapterViewHolder vh) {
                String locationSetting = Utility.getPreferredLocation(getActivity());
                ((Callback) getActivity()).
                        onItemSelected(WeatherContract.WeatherEntry.buildWeatherLocationWithDate(
                                                                        locationSetting, date), vh);
                mPosition = vh.getAdapterPosition();
            }
        }, emptyView, mChoiceMode);


        viewWeatherData.setAdapter(adapterForWeatherData);
        final View parallaxView = rootView.findViewById(R.id.parallax_bar);
        if (null != parallaxView) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                viewWeatherData.addOnScrollListener(new RecyclerView.OnScrollListener() {
                    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
                    @Override
                    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                        super.onScrolled(recyclerView, dx, dy);
                        int max = parallaxView.getHeight();
                        if (dy > 0) {
                            parallaxView.setTranslationY(Math.max(-max, parallaxView.getTranslationY() - dy / 2));
                        } else {
                            parallaxView.setTranslationY(Math.min(0, parallaxView.getTranslationY() - dy / 2));
                        }
                    }
                });
            }
        }

        final AppBarLayout appbarView = (AppBarLayout)rootView.findViewById(R.id.appbar);
        if (null != appbarView) {
            ViewCompat.setElevation(appbarView, 0);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                viewWeatherData.addOnScrollListener(new RecyclerView.OnScrollListener() {
                    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                    @Override
                    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                        if (0 == viewWeatherData.computeVerticalScrollOffset()) {
                            appbarView.setElevation(0);
                        } else {
                            appbarView.setElevation(appbarView.getTargetElevation());
                        }
                    }
                });
            }
        }



/*        viewWeatherData.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Cursor cursor = (Cursor) parent.getItemAtPosition(position);
                if (cursor != null) {
                    String locationSetting = Utility.getPreferredLocation(getActivity());
                    ((Callback) getActivity()).onItemSelected(WeatherContract.WeatherEntry.
                            buildWeatherLocationWithDate(locationSetting, cursor.getLong(COL_WEATHER_DATE)));
                }
                mPosition = position;
            }
        });*/
        // If there's instance state, mine it for useful information.
        // The end-goal here is that the user never knows that turning their device sideway
        // does crazy lifecycle related things.  It should feel like some stuff stretched out,
        // or magically appeared to take advantage of room, but data or place in the app was never
        // actually *lost*.
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(SELECTED_KEY)) {
                // The recyclerview probably hasn't even been populated yet.  Actually perform the
                // swapout in onLoadFinished.
                mPosition = savedInstanceState.getInt(SELECTED_KEY);
            }
            adapterForWeatherData.onRestoreInstanceState(savedInstanceState);
        }
        adapterForWeatherData.setUseTodayLayout(mUseTodayLayout);
        return rootView;
    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

//        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_refresh) {
//            updateWeather();
//            return true;
        if (id == R.id.action_map) {
            openPreferredLocationInMap();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void updateWeather(){
        Utility.resetLocationStatus(getActivity());
        SunshineSyncAdapter.syncImmediately(getActivity());
    }

    private void openPreferredLocationInMap(){
        //SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        if ( null != adapterForWeatherData ) {
            Cursor c = adapterForWeatherData.getCursor();
            if(null != c) {
                c.moveToPosition(0);
                String posLat = c.getString(COL_COORD_LAT);
                String posLong = c.getString(COL_COORD_LONG);
                Uri geoLocation = Uri.parse("geo:" + posLat + "," + posLong);
                Intent viewOnMap = new Intent(Intent.ACTION_VIEW);
                viewOnMap.setData(geoLocation);
                if (viewOnMap.resolveActivity(getActivity().getPackageManager()) != null) {
                    startActivity(viewOnMap);
                } else {
                    Log.d(LOG_TAG, "Couldn't call" + geoLocation.toString() + ", no receiving apps installed!");
                }
            }
        }
    }

    public void onLocationChanged(){
        //updateWeather();
        //getLoaderManager().restartLoader(LOADER_ID, null, this);
    }

    public void setUseTodayLayout(boolean useTodayLayout) {
        mUseTodayLayout = useTodayLayout;
        if (adapterForWeatherData != null) {
            adapterForWeatherData.setUseTodayLayout(mUseTodayLayout);
        }
    }


    /*
        Updates the empty list view with contextually relevant information that the user can
        use to determine why they aren't seeing weather.
     */
    private void updateEmptyView() {
        if ( adapterForWeatherData.getItemCount() == 0 ) {
            TextView tv = (TextView) getView().findViewById(R.id.recyclerview_forecast_empty);
            if ( null != tv ) {
                // if cursor is empty, why? do we have an invalid location
                int message = R.string.empty_forecast_list;
                @SunshineSyncAdapter.LocationStatus int location = Utility.getLocationStatus(getActivity());
                switch (location) {
                    case SunshineSyncAdapter.LOCATION_STATUS_SERVER_DOWN:
                        message = R.string.empty_forecast_list_server_down;
                        break;
                    case SunshineSyncAdapter.LOCATION_STATUS_SERVER_INVALID:
                        message = R.string.empty_forecast_list_server_error;
                        break;
                    case SunshineSyncAdapter.LOCATION_STATUS_INVALID:
                        message = R.string.empty_forecast_list_invalid_location;
                    default:
                       if (!Utility.isNetworkAvailable(getActivity()) ) {
                            message = R.string.empty_forecast_list_no_network;
                       }
                }
                tv.setText(message);
            }
        }
    }

    /**
     * Called when a shared preference is changed, added, or removed. This
     * may be called even if a preference is set to its existing value.
     * <p/>
     * <p>This callback will be run on your main thread.
     *
     * @param sharedPreferences The {@link SharedPreferences} that received
     *                          the change.
     * @param key               The key of the preference that was changed, added, or
     */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if ( key.equals(getString(R.string.pref_location_status_key)) ) {
            Utility.resetLocationStatus(getActivity());
            SunshineSyncAdapter.syncImmediately(getActivity());
            updateEmptyView();
        }
        if ( key.equals(getString(R.string.pref_art_pack_key)) ) {
            getActivity().getContentResolver().notifyChange(WeatherContract.WeatherEntry.CONTENT_URI, null);
        }
    }

    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface Callback {
        /**
         * DetailFragmentCallback for when an item has been selected.
         */
        public void onItemSelected(Uri dateUri, ForecastAdapter.ForecastAdapterViewHolder vh);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (null != viewWeatherData) {
            viewWeatherData.clearOnScrollListeners();
        }
    }

    public void setInitialSelectedDate(long initialSelectedDate) {
        mInitialSelectedDate = initialSelectedDate;
    }
}
