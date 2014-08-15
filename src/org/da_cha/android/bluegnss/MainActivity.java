/*
 * Copyright 2014, Hiroshi Miura <miurahr@linux.com>
 * Copyright 2014, BlueGnss4OSM Project
 * Copyright (C) 2010, 2011, 2012 BluetoothGPS4Droid Project
 *
 * This file is part of BlueGnss4OSM.
 *
 * This is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with it. If not, see <http://www.gnu.org/licenses/>.
 */

package org.da_cha.android.bluegnss;

import java.text.NumberFormat;

import org.da_cha.android.bluegnss.GnssProviderService;
import org.da_cha.android.bluegnss.GnssStatus;
import org.da_cha.android.bluegnss.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.FragmentActivity;
import android.text.format.Time;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;
import android.widget.Button;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.util.Log;

/**
 * An Activity Class used to start and stop connecting BT GPS/GNSS dongle.
 *
 * @author Hiroshi Miura
 *
 */
public class MainActivity extends Activity {

    private final static String LOG_TAG = "BlueGPS";

    private SharedPreferences sharedPref;
    private boolean conn_state = false;
    private boolean logging_state = false;

    private GnssProviderService mService = null;
    boolean mIsBound;
    private final GnssUpdateReceiver mGnssUpdateReceiver = new GnssUpdateReceiver();

    /** Called when the activity is first created. */
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        setContentView(R.layout.main);

        this.setBluetoothDeviceName();

        restoreMe(savedInstanceState);
        CheckIfServiceIsRunning();

        Button btnStartStop = (Button)findViewById(R.id.btn_start_stop);
        btnStartStop.setOnClickListener(mStartStop);

        Button btnStartLogging = (Button)findViewById(R.id.btn_start_logging);
        btnStartLogging.setOnClickListener(mStartLogging);

        if (logging_state) {
            btnStartLogging.setText(R.string.main_logging_stop);
        } else {
            btnStartLogging.setText(R.string.main_logging_start);
        }
        if (conn_state) {
            btnStartLogging.setEnabled(true);
            btnStartStop.setText(R.string.main_stop);
        } else {
            btnStartLogging.setEnabled(false);
            btnStartStop.setText(R.string.main_start);
        }
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("connStatus", Boolean.toString(conn_state));
        outState.putString("loggingStatus", Boolean.toString(logging_state));
    }

    private void restoreMe(Bundle state) {
        if (state!=null) {
            conn_state = Boolean.valueOf(state.getString("connStatus"));
            logging_state = Boolean.valueOf(state.getString("LoggingStatus"));
        }
    }

    /*
     * Service communications
     */
    private void CheckIfServiceIsRunning() {
        if (GnssProviderService.isRunning()) {
            doBindService();
            IntentFilter filter = new IntentFilter(GnssProviderService.NOTIFY_UPDATE);
            registerReceiver(mGnssUpdateReceiver, filter);
        }
    }
    private void doBindService() {
        bindService(new Intent(this, GnssProviderService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }
    private void doUnbindService() {
        if (mIsBound) {
            unbindService(mConnection);
            mIsBound = false;
        }
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onResume()
     */
    @Override
    protected void onResume() {
        super.onResume();
        this.setBluetoothDeviceName();
    }
 
    @Override
    protected void onDestroy() {
        super.onDestroy();
        doUnbindService();
        unregisterReceiver(mGnssUpdateReceiver);
    }

    private void setBluetoothDeviceName() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        String deviceAddress = sharedPref.getString(GnssProviderService.PREF_BLUETOOTH_DEVICE, null);
        TextView txtDeviceName = (TextView)findViewById(R.id.main_bluetooth_device_name);
        if (bluetoothAdapter != null) {
            if (BluetoothAdapter.checkBluetoothAddress(deviceAddress)){
                txtDeviceName.setText(bluetoothAdapter.getRemoteDevice(deviceAddress).getName());
            }
        }
    }
    private boolean checkBluetoothDevice() {
        String deviceAddress = sharedPref.getString(GnssProviderService.PREF_BLUETOOTH_DEVICE, null);
        return (deviceAddress == null)?false:true;
    }

    /*
     * Start/Stop service and logging button.
     */
    private void stopProviderService() {
        doUnbindService();
        unregisterReceiver(mGnssUpdateReceiver);
        // stop service
        Intent i = new Intent(GnssProviderService.ACTION_STOP_GPS_PROVIDER);
        i.setClass(MainActivity.this, GnssProviderService.class);
        startService(i);
        // button -> "Start"
        Button btnStartStop = (Button)findViewById(R.id.btn_start_stop);
        btnStartStop.setText(R.string.main_start);
        // Logging button disabled
        Button btnStartLogging = (Button)findViewById(R.id.btn_start_logging);
        btnStartLogging.setEnabled(false);
        conn_state = false;
    }
    private void startProviderService() {
        // start service
        Intent i = new Intent(GnssProviderService.ACTION_START_GPS_PROVIDER);
        i.setClass(MainActivity.this, GnssProviderService.class);
        startService(i);
        // wait 1000ms.
        try{
            Thread.sleep(2000);
        }catch(InterruptedException e){}
        // Bound service.
        doBindService();
        // register Receiver
        IntentFilter filter = new IntentFilter(GnssProviderService.NOTIFY_UPDATE);
        registerReceiver(mGnssUpdateReceiver, filter);
        conn_state = true;
        // button -> "Stop"
        Button btnStartStop = (Button)findViewById(R.id.btn_start_stop);
        btnStartStop.setText(R.string.main_stop);
        // Logging button enabled
        Button btnStartLogging = (Button)findViewById(R.id.btn_start_logging);
        btnStartLogging.setEnabled(true);
        if (GnssProviderService.isRunning()) {
            Log.d(LOG_TAG, "Cannot detect Service running");
        }
    }

    private OnClickListener mStartStop = new OnClickListener() {
        public void onClick(View v) {
            if (conn_state) {
                Log.d(LOG_TAG, "mStartStop: stop service");
                stopProviderService();
            } else if (checkBluetoothDevice()) {
                Log.d(LOG_TAG, "mStartStop: start service");
                startProviderService();
            } else {
                // do nothing
            }
        }
    };

    private OnClickListener mStartLogging = new OnClickListener() {
        public void onClick(View v) {
            if (logging_state) {
                Intent i = new Intent(GnssProviderService.ACTION_STOP_TRACK_RECORDING);
                i.setClass(MainActivity.this, GnssProviderService.class);
                startService(i);
                Log.d(LOG_TAG, "mStartLogging: stop service");
                Button btnStartLogging = (Button)findViewById(R.id.btn_start_logging);
                btnStartLogging.setText(R.string.main_logging_start);
                logging_state = false;
            } else {
                Intent i = new Intent(GnssProviderService.ACTION_START_TRACK_RECORDING);
                i.setClass(MainActivity.this, GnssProviderService.class);
                startService(i);
                Log.d(LOG_TAG, "mStartLogging: start service");
                Button btnStartLogging = (Button)findViewById(R.id.btn_start_logging);
                btnStartLogging.setText(R.string.main_logging_stop);
                logging_state = true;
            }
        }
    };

    /*
     * Option Menu preparation
     */
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
            startActivity(new Intent(this, SettingActivity.class));
            return true;
        case R.id.menu_about:
            displayAboutDialog();
            return true;
        }
        return false;
    }

    private void displayAboutDialog(){
        View messageView = getLayoutInflater().inflate(R.layout.about, null, false);
        // we need this to enable html links
        TextView textView = (TextView) messageView.findViewById(R.id.about_license);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        // When linking text, force to always use default color. This works
        // around a pressed color state bug.
        int defaultColor = textView.getTextColors().getDefaultColor();
        textView.setTextColor(defaultColor);
        textView = (TextView) messageView.findViewById(R.id.about_sources);
        textView.setTextColor(defaultColor);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.about_title);
        builder.setIcon(R.drawable.gplv3_icon);
        builder.setView(messageView);
        builder.show();
    }

    /*
     * For bind to ProviderService
     */
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = (GnssProviderService)((GnssProviderService.GnssProviderServiceBinder)service).getService();
        }

        public void onServiceDisconnected(ComponentName className) {
            mService = null;
        }
    };
    /*
     * for reciever
     */
     private class GnssUpdateReceiver extends BroadcastReceiver {
        private GnssStatus status;
        @Override
        public void onReceive(Context context, Intent intent) {
      
            Bundle bundle = intent.getExtras();
            String message = bundle.getString("notification");
            if (GnssProviderService.NOTIFY_UPDATE_GPS_STATUS.equals(message)){
               status = mService.getGnssStatus();
               long fix = status.getFixTimestamp();
               if (fix != 0) {
                   update_view(bundle);
               } else {
                   update_time(bundle);
               }
            } else if (GnssProviderService.NOTIFY_DISCONNECT.equals(message)){
               stopProviderService();
            } else {
               Log.e(LOG_TAG, "Unknown message: "+message);
            }
        }
        private void update_time(Bundle bundle){
            // Update date/time on View
            long timestamp = bundle.getLong("timestamp");
            Time sat_time = new Time();
            sat_time.set(timestamp);
            TextView tv = (TextView) findViewById(R.id.main_date_time);
            tv.setText(sat_time.format("%Y-%m-%d %H-%M-%S"));
        } 
        private void update_view(Bundle bundle){
            // Update all information on main screen
            // date/time
            long timestamp = status.getTimestamp();
            Time sat_time = new Time();
            sat_time.set(timestamp);
            TextView tv = (TextView) findViewById(R.id.main_date_time);
            tv.setText(sat_time.format("%Y-%m-%d %H-%M-%S"));
            double latitude = status.getLatitude();
            tv = (TextView) findViewById(R.id.main_lat);
            tv.setText(lonlat_format(latitude));
            double longitude = status.getLongitude();
            tv = (TextView) findViewById(R.id.main_lon);
            tv.setText(lonlat_format(longitude));
            float speed = status.getSpeed();
            tv = (TextView) findViewById(R.id.main_speed);
            tv.setText(len_format(speed));
            double hdop = status.getHDOP();
            tv = (TextView) findViewById(R.id.main_hdop);
            tv.setText(len_format(hdop));
            int numNbSat = status.getNbSat();
            int numSat = status.getNumSatellites();
            tv = (TextView) findViewById(R.id.main_num_satellites);
            tv.setText(Integer.toString(numNbSat)+"/"+Integer.toString(numSat));
        }
        private String lonlat_format(Double lonlat){
            NumberFormat format = NumberFormat.getInstance();
            format.setMaximumFractionDigits(6);
            return format.format(lonlat);
        }
        private String len_format(Double len){
            NumberFormat format = NumberFormat.getInstance();
            format.setMaximumFractionDigits(2);
            return format.format(len);
        }
        private String len_format(Float len){
            NumberFormat format = NumberFormat.getInstance();
            format.setMaximumFractionDigits(2);
            return format.format(len);
        }


    }
}

// vim: tabstop=4 expandtab shiftwidth=4 softtabstop=4
