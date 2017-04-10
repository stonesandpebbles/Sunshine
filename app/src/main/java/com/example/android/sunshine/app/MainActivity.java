package com.example.android.sunshine.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.example.android.sunshine.app.data.WeatherContract;
import com.example.android.sunshine.app.sync.SunshineSyncAdapter;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;



public class MainActivity extends AppCompatActivity implements ForecastFragment.Callback{

    private static final String LOG_TAG = MainActivity.class.getSimpleName();


    private final String FORECASTFRAGMENT_TAG = "FFTAG";
    private static final String DETAILFRAGMENT_TAG = "DFTAG";
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private String mLocation;
    private boolean mTwoPane;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLocation = Utility.getPreferredLocation(this);
        Uri contentUri = getIntent() != null ? getIntent().getData() : null;
        setContentView(R.layout.activity_main);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);
        // myToolbar.setLogo(R.drawable.ic_logo);
        // myToolbar.setTitle("");
        setSupportActionBar(myToolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        if (findViewById(R.id.weather_detail_container) != null) {
            // The detail container view will be present only in the large-screen layouts
            // (res/layout-sw600dp). If this view is present, then the activity should be
            // in two-pane mode.
            mTwoPane = true;
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            if (savedInstanceState == null) {
                DetailActivityFragment fragment = new DetailActivityFragment();
                if (contentUri != null) {
                    Bundle args = new Bundle();
                    args.putParcelable(DetailActivityFragment.DETAIL_URI, contentUri);
                    fragment.setArguments(args);
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.weather_detail_container, fragment, DETAILFRAGMENT_TAG)
                            .commit();
                }
            }
            } else {
                mTwoPane = false;
                //getSupportActionBar().setElevation(of);
            }
            ForecastFragment forecastFragment = ((ForecastFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.fragment_forecast));
            forecastFragment.setUseTodayLayout(!mTwoPane);
            if (contentUri != null) {
                forecastFragment.setInitialSelectedDate(
                        WeatherContract.WeatherEntry.getDateFromUri(contentUri));
            }
            SunshineSyncAdapter.initializeSyncAdapter(this);
            if (!checkPlayServices()) {
                // This is where we could either prompt a user that they should install
                // the latest version of Google Play Services, or add an error snackbar
                // that some features won't be available.
            }


        }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }


    @Override
    public void onResume() {
        super.onResume();
        String newPref = Utility.getPreferredLocation(this);
        if(newPref != null && !mLocation.equalsIgnoreCase(newPref)){
            ForecastFragment ff = (ForecastFragment)getSupportFragmentManager().findFragmentById(R.id.fragment_forecast);
            if(ff != null)ff.onLocationChanged();
            DetailActivityFragment df = (DetailActivityFragment)getSupportFragmentManager().findFragmentByTag(DETAILFRAGMENT_TAG);
            if ( null != df ) {
                df.onLocationChanged(newPref);
            }
            mLocation = newPref;
        }
    }

    /**
     * DetailFragmentCallback for when an item has been selected.
     *
     * @param contentUri
     */
    @Override
    public void onItemSelected(Uri contentUri, ForecastAdapter.ForecastAdapterViewHolder vh) {
        if (mTwoPane) {
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            Bundle args = new Bundle();
            args.putParcelable(DetailActivityFragment.DETAIL_URI, contentUri);

            DetailActivityFragment fragment = new DetailActivityFragment();
            fragment.setArguments(args);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.weather_detail_container, fragment, DETAILFRAGMENT_TAG)
                    .commit();
        } else {
            Intent intent = new Intent(this, DetailActivity.class)
                    .setData(contentUri);
            ActivityOptionsCompat activityOptions =
                    ActivityOptionsCompat.makeSceneTransitionAnimation(this,  new Pair<View, String>(vh.mIconView, getString(R.string.detail_icon_transition_name)));
            ActivityCompat.startActivity(this, intent, activityOptions.toBundle());
        }
    }


    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.i(LOG_TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }
}
