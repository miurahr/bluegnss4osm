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

package org.da_cha.android.gps.bluetooth.provider;

import java.util.Iterator;
import java.text.DecimalFormat;

import org.da_cha.android.gps.bluetooth.provider.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
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
public class BluetoothGpsMainActivity extends Activity {

    private final static String LOG_TAG = "BlueGPS";

    private SharedPreferences sharedPref;
    private boolean conn_state = false;
    private boolean logging_state = false;

    private Messenger mService = null;
    boolean mIsBound;
    final Messenger mMessenger = new Messenger(new IncomingHandler());

    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case BluetoothGpsProviderService.MSG_UPDATED:
                //loc = (String) msg.obj;
                break;
            case BluetoothGpsProviderService.MSG_DISCONNECTED:
                stopProviderService();
                break;
            default:
                super.handleMessage(msg);
            }
        }
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
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
        if (BluetoothGpsProviderService.isRunning()) {
            doBindService();
        }
    }
    private void doBindService() {
        bindService(new Intent(this, BluetoothGpsProviderService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }
    private void doUnbindService() {
        if (mIsBound) {
            if (mService != null) {
                try {
                    Message msg = Message.obtain(null, BluetoothGpsProviderService.MSG_UNREGISTER_CLIENT);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                } catch (RemoteException e) {
                    // There is nothing special we need to do if the service has crashed.
                }
            }
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
    private boolean checkBluetoothDevice() {
        String deviceAddress = sharedPref.getString(BluetoothGpsProviderService.PREF_BLUETOOTH_DEVICE, null);
        return (deviceAddress == null)?false:true;
    }

    /*
     * Start/Stop service and logging button.
     */
    private void stopProviderService() {
        doUnbindService();
        // stop service
        Intent i = new Intent(BluetoothGpsProviderService.ACTION_STOP_GPS_PROVIDER);
        i.setClass(BluetoothGpsMainActivity.this, BluetoothGpsProviderService.class);
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
        Intent i = new Intent(BluetoothGpsProviderService.ACTION_START_GPS_PROVIDER);
        i.setClass(BluetoothGpsMainActivity.this, BluetoothGpsProviderService.class);
        startService(i);
        // wait 1000ms.
        try{
            Thread.sleep(2000);
        }catch(InterruptedException e){}
        // Bound service.
        doBindService();
        conn_state = true;
        // button -> "Stop"
        Button btnStartStop = (Button)findViewById(R.id.btn_start_stop);
        btnStartStop.setText(R.string.main_stop);
        // Logging button enabled
        Button btnStartLogging = (Button)findViewById(R.id.btn_start_logging);
        btnStartLogging.setEnabled(true);
        if (BluetoothGpsProviderService.isRunning()) {
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
            startActivity(new Intent(this, BluetoothGpsActivity.class));
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
            mService = new Messenger(service);
            try {
                Message msg = Message.obtain(null, BluetoothGpsProviderService.MSG_REGISTER_CLIENT);
                msg.replyTo = mMessenger;
                mService.send(msg);
            } catch (RemoteException e) {
                // In this case the service has crashed before we could even do anything with it
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            mService = null;
        }
    };

}

// vim: tabstop=8 expandtab shiftwidth=4 softtabstop=4
