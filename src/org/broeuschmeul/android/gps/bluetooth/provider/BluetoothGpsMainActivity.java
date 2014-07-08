/*
 * Copyright (C) 2014 Hiroshi Miura
 * Copyright (C) 2010, 2011, 2012 BluetoothGPS4Droid Project
 *
 * This file is part of BluetoothGPS4Droid.
 *
 * BluetoothGPS4Droid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * BluetoothGPS4Droid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with BluetoothGPS4Droid. If not, see <http://www.gnu.org/licenses/>.
 */

package org.broeuschmeul.android.gps.bluetooth.provider;

import java.util.Iterator;
import java.text.DecimalFormat;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.text.format.Time;
import android.text.method.LinkMovementMethod;

import android.widget.TextView;
import android.widget.Button;

import android.view.View;
import android.view.View.OnClickListener;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import android.location.Location;
import android.location.LocationManager;
import android.location.GpsStatus;
import android.location.GpsStatus.Listener;
import android.location.GpsSatellite;

import android.util.Log;

/**
 * An Activity Class used to start and stop connecting BT GPS/GNSS dongle.
 *
 * @author Hiroshi Miura
 *
 */
public class BluetoothGpsMainActivity extends Activity {

    private static final String LOG_TAG = "BlueGPS";
    private LocationManager locationManager = null;
    private SharedPreferences sharedPref;
    private GpsStatus.Listener gpsListener;
    private boolean conn_state = false;
    private boolean logging_state = false;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        gpsListener = new BluetoothGpsStatusListener();
        locationManager.addGpsStatusListener(gpsListener);

        setContentView(R.layout.main);

        this.setBluetoothDeviceName();

        Button button = (Button)findViewById(R.id.btn_start_stop);
        button.setOnClickListener(mStartStop);

        button = (Button)findViewById(R.id.btn_start_logging);
        button.setEnabled(false);
        button.setOnClickListener(mStartLogging);
    }
    
    /* (non-Javadoc)
	   * @see android.app.Activity#onResume()
     */
    @Override
    protected void onResume() {
        this.setBluetoothDeviceName();
        super.onResume();
    }
 
    @Override
    protected void onDestroy() {
        locationManager.removeGpsStatusListener(gpsListener);
    }

    private void setBluetoothDeviceName() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        String deviceAddress = sharedPref.getString(BluetoothGpsProviderService.PREF_BLUETOOTH_DEVICE, null);
        TextView txtDeviceName = (TextView)findViewById(R.id.main_bluetooth_device_name);
        if (bluetoothAdapter != null) {
            if (BluetoothAdapter.checkBluetoothAddress(deviceAddress)){
                txtDeviceName.setText(bluetoothAdapter.getRemoteDevice(deviceAddress).getName());
            }
        }
    }

    private OnClickListener mStartStop = new OnClickListener() {
        public void onClick(View v) {
            if (conn_state) {
                // stop service
                Intent i = new Intent(BluetoothGpsProviderService.ACTION_STOP_GPS_PROVIDER);
                i.setClass(BluetoothGpsMainActivity.this, BluetoothGpsProviderService.class);
                startService(i);
                Log.d(LOG_TAG, "mStartStop: stop service");
                Button btnStartStop = (Button)findViewById(R.id.btn_start_stop);
                btnStartStop.setText(R.string.main_start);
                Button btnStartLogging = (Button)findViewById(R.id.btn_start_logging);
                btnStartLogging.setEnabled(false);
                conn_state = false;
            } else {
                // start service
                Intent i = new Intent(BluetoothGpsProviderService.ACTION_START_GPS_PROVIDER);
                i.setClass(BluetoothGpsMainActivity.this, BluetoothGpsProviderService.class);
                startService(i);
                Log.d(LOG_TAG, "mStartStop: start service");
                Button btnStartStop = (Button)findViewById(R.id.btn_start_stop);
                btnStartStop.setText(R.string.main_stop);
                Button btnStartLogging = (Button)findViewById(R.id.btn_start_logging);
                btnStartLogging.setEnabled(true);
                conn_state = true;
            }
        }
    };

    private OnClickListener mStartLogging = new OnClickListener() {
        public void onClick(View v) {
            if (logging_state) {
                Intent i = new Intent(BluetoothGpsProviderService.ACTION_STOP_TRACK_RECORDING);
                i.setClass(BluetoothGpsMainActivity.this, BluetoothGpsProviderService.class);
                startService(i);
                Log.d(LOG_TAG, "mStartLogging: stop service");
                Button btnStartLogging = (Button)findViewById(R.id.btn_start_logging);
                btnStartLogging.setText(R.string.main_logging_start);
                logging_state = false;
            } else {
                Intent i = new Intent(BluetoothGpsProviderService.ACTION_START_TRACK_RECORDING);
                i.setClass(BluetoothGpsMainActivity.this, BluetoothGpsProviderService.class);
                startService(i);
                Log.d(LOG_TAG, "mStartLogging: start service");
                Button btnStartLogging = (Button)findViewById(R.id.btn_start_logging);
                btnStartLogging.setText(R.string.main_logging_stop);
                logging_state = true;
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.optionsmenu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
        case R.id.menu_pref:
            startActivity(new Intent(this, BluetoothGpsPreferenceActivity.class));
            return true;
        }
        return false;
    }

  private class BluetoothGpsStatusListener implements GpsStatus.Listener {

    //private static final String LOG_TAG = "BlueGPS";

    private static final int BLUEGPS_NA=0;
    private static final int BLUEGPS_STARTED=1;
    private static final int BLUEGPS_FIXED=2;
    private static final int BLUEGPS_STOPPED=3;

    private int bStatus;
    private GpsStatus gpsStatus = null;
    //private LocationManager locationManager;
    
    public void BluetoothGpsStatusListener() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    }

    private void setNumSatellites(int sat) {
        Log.d(LOG_TAG, "BluetoothGpsStatusListener: NumSatellites: " + Integer.toString(sat));
    }

    private void setTTFF(int ttff) {
        Log.d(LOG_TAG, "BluetoothGpsStatusListener: TTFF: " + Integer.toString(ttff) + " msec");
    }

    private void updateSatellites(){
        Log.d(LOG_TAG, "BluetoothGpsStatusListener: enter updateSatellites()");
        // assert status
        if (bStatus == BLUEGPS_STOPPED) {
            return;
        }
        if (gpsStatus == null) {
            return;
        }

        //TextView tv = (TextView) findViewById(R.id.status_Gpsinfo);

        int num_satellites = gpsStatus.getMaxSatellites();
        setNumSatellites(num_satellites);
        Iterable<GpsSatellite> satellites = gpsStatus.getSatellites();
        Iterator<GpsSatellite> sat = satellites.iterator();
        int i=0;
        String strGpsStats = "";
        while (sat.hasNext()) {
            GpsSatellite satellite = sat.next();
            strGpsStats+= (i++) + ": " + satellite.getPrn() + "," + satellite.usedInFix() + "," + satellite.getSnr() + "," + satellite.getAzimuth() + "," + satellite.getElevation()+ "\n\n";
            }
        Log.v(LOG_TAG, strGpsStats);
        //tv.setText(strGpsStats);

        Location loc = locationManager.getLastKnownLocation(null);
        if (loc != null) {
            if (bStatus == BLUEGPS_STARTED) {
                // time
                Time sat_time = new Time();
                sat_time.set(loc.getTime());
                TextView tv = (TextView) findViewById(R.id.main_date_time);
                tv.setText(sat_time.format("%Y-%m-%d %H-%M-%S"));
            } else if (bStatus == BLUEGPS_FIXED) {
                // lat
                double lat = loc.getLatitude();
                TextView tv = (TextView) findViewById(R.id.main_lat);
                tv.setText(Double.toString(lat));
                // lon
                double lon = loc.getLongitude();
                tv = (TextView) findViewById(R.id.main_lon);
                tv.setText(Double.toString(lon));
                // time
                Time sat_time = new Time();
                sat_time.set(loc.getTime());
                tv = (TextView) findViewById(R.id.main_date_time);
                tv.setText(sat_time.format("%Y-%m-%d %H-%M-%S"));
                // format float
                DecimalFormat dec = new DecimalFormat("#.0");
                // dcop(accuracy)
                tv = (TextView) findViewById(R.id.main_dcop);
                float acc = loc.getAccuracy();
                tv.setText(dec.format(acc) + " m");
                // speed,   nnn.n km/h
                tv = (TextView) findViewById(R.id.main_speed);
                float speed = loc.getSpeed();
                tv.setText(dec.format(speed/1000) + " km/h");
            }
        } else {
            Log.d(LOG_TAG, "BluetoothGpsStatusListener: BUG: loc is null");
        }
         
        Log.d(LOG_TAG, "BluetoothGpsStatusListener: exit updateSatellites()");
    }

    @Override
    public void onGpsStatusChanged(int event){
        gpsStatus = locationManager.getGpsStatus(gpsStatus);
        switch(event) {
            case GpsStatus.GPS_EVENT_STARTED:
                bStatus = BLUEGPS_STARTED;
                Log.d(LOG_TAG, "BluetoothGpsStatusListener: GPS_EVENT_STARTED.");
                updateSatellites();
                return;
            case GpsStatus.GPS_EVENT_STOPPED:
                bStatus = BLUEGPS_STOPPED;
                Log.d(LOG_TAG, "BluetoothGpsStatusListener: GPS_EVENT_STOPPED.");
                return;
            case GpsStatus.GPS_EVENT_FIRST_FIX:
                bStatus = BLUEGPS_FIXED;
                Log.d(LOG_TAG, "BluetoothGpsStatusListener: GPS_EVENT_FIXED.");
                int ttff = gpsStatus.getTimeToFirstFix ();
                setTTFF(ttff);
                updateSatellites();
                return;
            case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                updateSatellites();
                return;
        }
    }
  }

}
