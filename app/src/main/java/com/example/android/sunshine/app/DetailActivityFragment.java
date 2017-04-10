package com.example.android.sunshine.app;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.ShareActionProvider;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.android.sunshine.app.data.WeatherContract;

/**
 * A placeholder fragment containing a simple view.
 */
public class DetailActivityFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>{
    String mForecastStr = null;



    private final String LOG_TAG = DetailActivityFragment.class.getSimpleName();
    private final String FORECAST_SHARE_HASHTAG = "#FirstApp";
    static final String DETAIL_TRANSITION_ANIMATION = "DTA";
    private final int LOADER_ID = 678910;

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
            WeatherContract.WeatherEntry.COLUMN_HUMIDITY,
            WeatherContract.WeatherEntry.COLUMN_PRESSURE,
            WeatherContract.WeatherEntry.COLUMN_WIND_SPEED,
            WeatherContract.WeatherEntry.COLUMN_DEGREES,
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID
    };

    // These indices are tied to FORECAST_COLUMNS.  If FORECAST_COLUMNS changes, these
    // must change.
    public static final int COL_WEATHER_ID = 0;
    public static final int COL_WEATHER_DATE = 1;
    public static final int COL_WEATHER_DESC = 2;
    public static final int COL_WEATHER_MAX_TEMP = 3;
    public static final int COL_WEATHER_MIN_TEMP = 4;
    public static final int COL_WEATHER_HUMIDITY = 5;
    public static final int COL_WEATHER_PRESSURE = 6;
    public static final int COL_WEATHER_WIND_SPEED = 7;
    public static final int COL_WEATHER_DEGREES = 8;
    public static final int COL_WEATHER_CONDITION_ID = 9;
    static final String DETAIL_URI = "URI";
    private ImageView mIconView;
    private TextView mDateView;
    private TextView mDescriptionView;
    private TextView mHighTempView;
    private TextView mLowTempView;
    private TextView mHumidityView;
    private TextView mHumidityLabelView;
    private TextView mWindView;
    private TextView mWindLabelView;
    private TextView mPressureView;
    private TextView mPressureLabelView;
    private Uri mUri;
    private boolean mTransitionAnimation;
    public DetailActivityFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Add this line in order for this fragment to handle menu events.
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Bundle arguments = getArguments();
        if (arguments != null) {
            mUri = arguments.getParcelable(DetailActivityFragment.DETAIL_URI);
            mTransitionAnimation = arguments.getBoolean(DetailActivityFragment.DETAIL_TRANSITION_ANIMATION, false);
        }
        View rootView = inflater.inflate(R.layout.fragment_detail_start, container, false);
        mIconView = (ImageView) rootView.findViewById(R.id.detail_icon);
        mDateView = (TextView) rootView.findViewById(R.id.detail_date_textview);
        mDescriptionView = (TextView) rootView.findViewById(R.id.detail_forecast_textview);
        mHighTempView = (TextView) rootView.findViewById(R.id.detail_high_textview);
        mLowTempView = (TextView) rootView.findViewById(R.id.detail_low_textview);
        mHumidityView = (TextView) rootView.findViewById(R.id.detail_humidity_textview);
        mHumidityLabelView = (TextView) rootView.findViewById(R.id.detail_humidity_label_textview);
        mWindView = (TextView) rootView.findViewById(R.id.detail_wind_textview);
        mWindLabelView = (TextView) rootView.findViewById(R.id.detail_wind_label_textview);
        mPressureView = (TextView) rootView.findViewById(R.id.detail_pressure_textview);
        mPressureLabelView = (TextView) rootView.findViewById(R.id.detail_pressure_label_textview);

        return rootView;
    }

    public Intent createShareForecastIntent() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, mForecastStr + FORECAST_SHARE_HASHTAG);
        return shareIntent;
    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if ( getActivity() instanceof DetailActivity ) {
            // Inflate the menu; this adds items to the action bar if it is present
            inflater.inflate(R.menu.detailfragment, menu);
            MenuItem share = menu.findItem(R.id.menu_item_share);
            finishCreatingMenu(menu);
        }
    }

    private void finishCreatingMenu(Menu menu) {
        // Retrieve the share menu item
        MenuItem menuItem = menu.findItem(R.id.menu_item_share);
        menuItem.setIntent(createShareForecastIntent());
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getLoaderManager().initLoader(LOADER_ID, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Log.v(LOG_TAG, "In onCreateLoader");
    if(null != mUri) {
        return new CursorLoader(getActivity(), mUri,
                FORECAST_COLUMNS,
                null,
                null,
                null);
    }
        ViewParent vp = getView().getParent();
        if ( vp instanceof CardView ) {
            ((View)vp).setVisibility(View.INVISIBLE);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        Log.v(LOG_TAG, "In onLoadFinished");
        if (data != null && data.moveToFirst()) {
            ViewParent vp = getView().getParent();
            if ( vp instanceof CardView) {
                ((View)vp).setVisibility(View.INVISIBLE);
            }
            // Read weather condition ID from cursor
            int weatherId = data.getInt(COL_WEATHER_CONDITION_ID);
            if ( Utility.usingLocalGraphics(getActivity()) ) {
                // Use placeholder Image
                mIconView.setImageResource(Utility.getArtResourceForWeatherCondition(weatherId));
            }
            else {
                Glide.with(this).load(Utility.getArtUrlForWeatherCondition(getActivity(), weatherId))
                        .error(Utility.getArtResourceForWeatherCondition(weatherId)).crossFade().into(mIconView);
            }
            // Read date from cursor and update views for day of week and date
            long date = data.getLong(COL_WEATHER_DATE);
            String dateText = Utility.getFullFriendlyDayString(getActivity(),date);
            mDateView.setText(dateText);

            // Get description from weather condition ID
            String description = Utility.getStringForWeatherCondition(getActivity(), weatherId);
            mDescriptionView.setText(description);
            mDescriptionView.setContentDescription(getString(R.string.a11y_forecast, description));

            // For accessibility, add a content description to the icon field. Because the ImageView
            // is independently focusable, it's better to have a description of the image. Using
            // null is appropriate when the image is purely decorative or when the image already
            // has text describing it in the same UI component.
            mIconView.setContentDescription(getString(R.string.a11y_forecast_icon, description));

            // Read high temperature from cursor and update view
            boolean isMetric = Utility.isMetric(getActivity());

            double high = data.getDouble(COL_WEATHER_MAX_TEMP);
            String highString = Utility.formatTemperature(getActivity(), high);
            mHighTempView.setText(highString);
            mHighTempView.setContentDescription(getString(R.string.a11y_high_temp, highString));

            // Read low temperature from cursor and update view
            double low = data.getDouble(COL_WEATHER_MIN_TEMP);
            String lowString = Utility.formatTemperature(getActivity(), low);
            mLowTempView.setText(lowString);
            mLowTempView.setContentDescription(getString(R.string.a11y_low_temp, lowString));

            // Read humidity from cursor and update view
            float humidity = data.getFloat(COL_WEATHER_HUMIDITY);
            mHumidityView.setText(getActivity().getString(R.string.format_humidity, humidity));
            mHumidityView.setContentDescription(getString(R.string.a11y_humidity, mHumidityView.getText()));
            mHumidityLabelView.setContentDescription(mHumidityView.getContentDescription());

            // Read wind speed and direction from cursor and update view
            float windSpeedStr = data.getFloat(COL_WEATHER_WIND_SPEED);
            float windDirStr = data.getFloat(COL_WEATHER_DEGREES);
            mWindView.setText(Utility.getFormattedWind(getActivity(), windSpeedStr, windDirStr));
            mWindView.setContentDescription(getString(R.string.a11y_wind, mWindView.getText()));
            mWindLabelView.setContentDescription(mWindView.getContentDescription());

            // Read pressure from cursor and update view
            float pressure = data.getFloat(COL_WEATHER_PRESSURE);
            mPressureView.setText(getString(R.string.format_pressure, pressure));
            mPressureView.setContentDescription(getString(R.string.a11y_pressure, mPressureView.getText()));
            mPressureLabelView.setContentDescription(mPressureView.getContentDescription());

            // We still need this for the share intent
            mForecastStr = String.format("%s - %s - %s/%s", dateText, description, high, low);

        }
        AppCompatActivity activity = (AppCompatActivity)getActivity();
        Toolbar toolbarView = (Toolbar) getView().findViewById(R.id.toolbar);

        // We need to start the enter transition after the data has loaded
        if (mTransitionAnimation) {
            activity.supportStartPostponedEnterTransition();

            if ( null != toolbarView ) {
                activity.setSupportActionBar(toolbarView);

                activity.getSupportActionBar().setDisplayShowTitleEnabled(false);
                activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
        } else {
            if ( null != toolbarView ) {
                Menu menu = toolbarView.getMenu();
                if ( null != menu ) menu.clear();
                toolbarView.inflateMenu(R.menu.detailfragment);
                finishCreatingMenu(toolbarView.getMenu());
            }
        }
    }


    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    public void onLocationChanged(String newLocation){
        // replace the uri, since the location has changed
        Uri uri = mUri;
        if (null != uri) {
            long date = WeatherContract.WeatherEntry.getDateFromUri(uri);
            Uri updatedUri = WeatherContract.WeatherEntry.buildWeatherLocationWithDate(newLocation, date);
            mUri = updatedUri;
            getLoaderManager().restartLoader(LOADER_ID, null, this);
        }
    }
}
