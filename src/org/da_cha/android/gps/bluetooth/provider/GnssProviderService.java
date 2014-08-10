/*
 * Copyright 2014, Hiroshi Miura <miurahr@linux.com>
 * Copyright (C) 2010, 2011, 2012 Herbert von Broeuschmeul
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

/**
 * 
 */
package org.da_cha.android.gps.bluetooth.provider;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.GpsStatus.NmeaListener;
import android.location.GpsStatus.Listener;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Binder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import org.da_cha.android.gps.bluetooth.provider.R;
import org.da_cha.android.gps.nmea.util.NmeaParser;
import org.da_cha.android.gps.nmea.util.GnssStatus;
import org.da_cha.android.gps.bluetooth.provider.BlueGnssMainActivity;
import org.da_cha.android.gps.bluetooth.provider.BluetoothGnssManager;
import org.da_cha.android.gps.bluetooth.provider.MockLocationProvider;

/**
 * A Service used to replace Android internal GPS with a bluetooth GPS and/or write GPS NMEA data in a File.
 * 
 * @author Herbert von Broeuschmeul
 *
 */
public class GnssProviderService extends Service implements NmeaListener, Listener {

	public static final String ACTION_START_TRACK_RECORDING = "org.da_cha.android.gps.bluetooth.tracker.intent.action.START_TRACK_RECORDING";
	public static final String ACTION_STOP_TRACK_RECORDING = "org.da_cha.android.gps.bluetooth.tracker.intent.action.STOP_TRACK_RECORDING";
	public static final String ACTION_START_GPS_PROVIDER = "org.da_cha.android.gps.bluetooth.provider.intent.action.START_GPS_PROVIDER";
	public static final String ACTION_STOP_GPS_PROVIDER = "org.da_cha.android.gps.bluetooth.provider.intent.action.STOP_GPS_PROVIDER";
	public static final String ACTION_CONFIGURE_SIRF_GPS = "org.da-cha.android.gps.bluetooth.provider.intent.action.CONFIGURE_SIRF_GPS";
	public static final String PREF_GPS_LOCATION_PROVIDER = "gpsLocationProviderKey";
	public static final String PREF_FORCE_ENABLE_PROVIDER = "forceEnableProvider";
	public static final String PREF_CONNECTION_RETRIES = "connectionRetries";
	public static final String PREF_TRACK_FILE_DIR = "trackFileDirectory";
	public static final String PREF_TRACK_FILE_PREFIX = "trackFilePrefix";
	public static final String PREF_BLUETOOTH_DEVICE = "bluetoothDevice";
	public static final String PREF_ABOUT = "about";
	public static final String NOTIFY_UPDATE = "org.da_cha.android.gps.bluetooth.provider.intent.notify.UPDATE";

	public static final int MSG_REGISTER_CLIENT   = 1;
	public static final int MSG_UNREGISTER_CLIENT = 2;
	public static final int MSG_DISCONNECTED      = 3;
	public static final int MSG_UPDATED           = 4;
	public static final int MSG_CONNECTED         = 5;


	/**
	 * Tag used for log messages
	 */
	private static final String LOG_TAG = "BlueGPS";

	public static final String PREF_SIRF_GPS = "sirfGps";
	public static final String PREF_SIRF_ENABLE_GGA = "enableGGA";
	public static final String PREF_SIRF_ENABLE_RMC = "enableRMC";
	public static final String PREF_SIRF_ENABLE_GLL = "enableGLL";
	public static final String PREF_SIRF_ENABLE_VTG = "enableVTG";
	public static final String PREF_SIRF_ENABLE_GSA = "enableGSA";
	public static final String PREF_SIRF_ENABLE_GSV = "enableGSV";
	public static final String PREF_SIRF_ENABLE_ZDA = "enableZDA";
	public static final String PREF_SIRF_ENABLE_SBAS = "enableSBAS";
	public static final String PREF_SIRF_ENABLE_NMEA = "enableNMEA";
	public static final String PREF_SIRF_ENABLE_STATIC_NAVIGATION = "enableStaticNavigation";

	private BluetoothGnssManager gpsManager = null;
	private MockLocationProvider gpsMockProvider = null;
	private PrintWriter writer;
	private File trackFile;
	private boolean preludeWritten = false;
	private Toast toast;
	private static boolean isRunning = false;
	private NmeaParser nmeaParser;

	private static PowerManager.WakeLock wl;

	private ArrayList<Messenger> mClients = new ArrayList<Messenger>();
    private Messenger mServiceMessenger;

	@Override
	public void onCreate() {
		super.onCreate();
		toast = Toast.makeText(getApplicationContext(), "NMEA track recording... on", Toast.LENGTH_SHORT);
		isRunning = true;
        createNewWakeLock();
        mServiceMessenger = new Messenger(new IncomingHandler());
	}

    @SuppressWarnings("deprecation")
    private void createNewWakeLock(){
		PowerManager pm = (PowerManager)getApplicationContext().getSystemService(Context.POWER_SERVICE);
        // SCREEN_DIM_WAKE_LOCK is deprecated but no better way to specify from service.
		wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, LOG_TAG);
    }

	/* (non-Javadoc)
	 * @see android.app.Service#onStartCommand(android.content.Intent, int, int)
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		String deviceAddress = sharedPreferences.getString(PREF_BLUETOOTH_DEVICE, null);
		int maxConRetries = Integer.parseInt(sharedPreferences.getString(PREF_CONNECTION_RETRIES, this.getString(R.string.defaultConnectionRetries)));
		Log.d(LOG_TAG, "prefs device addr: "+deviceAddress);
		if (ACTION_START_GPS_PROVIDER.equals(intent.getAction())){
			if (gpsManager == null){
				if (BluetoothAdapter.checkBluetoothAddress(deviceAddress)){
				/*
				 * Instanciate btgps manager, mock provider and nmea parser
				 */
					gpsManager = new BluetoothGnssManager(this, deviceAddress, maxConRetries);
					gpsMockProvider = new MockLocationProvider(this);
					nmeaParser = new NmeaParser(10f);
					// register
					gpsManager.setGpsMockProvider(gpsMockProvider);
					gpsManager.setNMEAParser(nmeaParser);
					nmeaParser.setGpsMockProvider(gpsMockProvider);

					// now ready to enable it.
					boolean enabled = gpsManager.enable();
//					Bundle extras = intent.getExtras();

					if (enabled) {
						wl.acquire();
						boolean force = sharedPreferences.getBoolean(PREF_FORCE_ENABLE_PROVIDER, false);
						gpsMockProvider.enableMockLocationProvider(force);
					    gpsManager.addGpsStatusListener(this);
						Intent myIntent = new Intent(this, BlueGnssMainActivity.class);
						PendingIntent myPendingIntent = PendingIntent.getActivity(this, 0, myIntent, PendingIntent.FLAG_CANCEL_CURRENT);
						Context appContext = getApplicationContext();
						Notification notification = new Notification.Builder(appContext)
                            .setSmallIcon(R.drawable.ic_stat_notify)
                            .setContentText(this.getString(R.string.foreground_gps_provider_started_notification))
                            .setWhen(System.currentTimeMillis())
                            .setContentTitle(this.getString(R.string.foreground_service_started_notification_title))
                            .setContentIntent(myPendingIntent)
                            .build();
						startForeground(R.string.foreground_gps_provider_started_notification, notification);
						if (sharedPreferences.getBoolean(PREF_SIRF_GPS, false)){
							enableSirfConfig(sharedPreferences);
						}					
						toast.setText(this.getString(R.string.msg_gps_provider_started));
						toast.show();	
					} else {
						sendGpsDisconnected();
						stopSelf();
					}
				} else {
					sendGpsDisconnected();
					stopSelf();
				}
			} else {
				toast.setText(this.getString(R.string.msg_gps_provider_already_started));
				toast.show();
			}
		} else if (ACTION_START_TRACK_RECORDING.equals(intent.getAction())){
			if (trackFile == null){
				if (gpsManager != null){
					beginTrack();
					gpsManager.addNmeaListener(this);
					toast.setText(this.getString(R.string.msg_nmea_recording_started));
					toast.show();
				} else {
					endTrack();
				}
			} else {
				toast.setText(this.getString(R.string.msg_nmea_recording_already_started));
				toast.show();
			}
		} else if (ACTION_STOP_TRACK_RECORDING.equals(intent.getAction())){
			if (gpsManager != null){
				gpsManager.removeNmeaListener(this);
				endTrack();
				toast.setText(this.getString(R.string.msg_nmea_recording_stopped));
				toast.show();
			}
		} else if (ACTION_STOP_GPS_PROVIDER.equals(intent.getAction())){
			wl.release();
            gpsManager.removeGpsStatusListener(this);
			stopSelf();
		} else if (ACTION_CONFIGURE_SIRF_GPS.equals(intent.getAction())){
			if (gpsManager != null){
				Bundle extras = intent.getExtras();
				enableSirfConfig(extras);
			}
		}
		return Service.START_STICKY;
	}

	private void enableSirfConfig(Bundle extras){
		if (extras.containsKey(PREF_SIRF_ENABLE_GGA)){
			enableNmeaGGA(extras.getBoolean(PREF_SIRF_ENABLE_GGA, true));
		}
		if (extras.containsKey(PREF_SIRF_ENABLE_RMC)){
			enableNmeaRMC(extras.getBoolean(PREF_SIRF_ENABLE_RMC, true));
		}
		if (extras.containsKey(PREF_SIRF_ENABLE_GLL)){
			enableNmeaGLL(extras.getBoolean(PREF_SIRF_ENABLE_GLL, false));
		}
		if (extras.containsKey(PREF_SIRF_ENABLE_VTG)){
			enableNmeaVTG(extras.getBoolean(PREF_SIRF_ENABLE_VTG, false));
		}
		if (extras.containsKey(PREF_SIRF_ENABLE_GSA)){
			enableNmeaGSA(extras.getBoolean(PREF_SIRF_ENABLE_GSA, false));
		}
		if (extras.containsKey(PREF_SIRF_ENABLE_GSV)){
			enableNmeaGSV(extras.getBoolean(PREF_SIRF_ENABLE_GSV, false));
		}
		if (extras.containsKey(PREF_SIRF_ENABLE_ZDA)){
			enableNmeaZDA(extras.getBoolean(PREF_SIRF_ENABLE_ZDA, false));
		}
		if (extras.containsKey(PREF_SIRF_ENABLE_STATIC_NAVIGATION)){
			enableStaticNavigation(extras.getBoolean(PREF_SIRF_ENABLE_STATIC_NAVIGATION, false));
		} else if (extras.containsKey(PREF_SIRF_ENABLE_NMEA)){
			enableNMEA(extras.getBoolean(PREF_SIRF_ENABLE_NMEA, true));
		}
		if (extras.containsKey(PREF_SIRF_ENABLE_SBAS)){
			enableSBAS(extras.getBoolean(PREF_SIRF_ENABLE_SBAS, true));
		}
	}

	private void enableSirfConfig(SharedPreferences extras){
		if (extras.contains(PREF_SIRF_ENABLE_GLL)){
			enableNmeaGLL(extras.getBoolean(PREF_SIRF_ENABLE_GLL, false));
		}
		if (extras.contains(PREF_SIRF_ENABLE_VTG)){
			enableNmeaVTG(extras.getBoolean(PREF_SIRF_ENABLE_VTG, false));
		}
		if (extras.contains(PREF_SIRF_ENABLE_GSA)){
			enableNmeaGSA(extras.getBoolean(PREF_SIRF_ENABLE_GSA, false));
		}
		if (extras.contains(PREF_SIRF_ENABLE_GSV)){
			enableNmeaGSV(extras.getBoolean(PREF_SIRF_ENABLE_GSV, false));
		}
		if (extras.contains(PREF_SIRF_ENABLE_ZDA)){
			enableNmeaZDA(extras.getBoolean(PREF_SIRF_ENABLE_ZDA, false));
		}
		if (extras.contains(PREF_SIRF_ENABLE_STATIC_NAVIGATION)){
			enableStaticNavigation(extras.getBoolean(PREF_SIRF_ENABLE_STATIC_NAVIGATION, false));
		} else if (extras.contains(PREF_SIRF_ENABLE_NMEA)){
			enableNMEA(extras.getBoolean(PREF_SIRF_ENABLE_NMEA, true));
		}
		if (extras.contains(PREF_SIRF_ENABLE_SBAS)){
			enableSBAS(extras.getBoolean(PREF_SIRF_ENABLE_SBAS, true));
		}
		gpsManager.sendNmeaCommand(this.getString(R.string.sirf_nmea_gga_on));
		gpsManager.sendNmeaCommand(this.getString(R.string.sirf_nmea_rmc_on));
		if (extras.contains(PREF_SIRF_ENABLE_GGA)){
			enableNmeaGGA(extras.getBoolean(PREF_SIRF_ENABLE_GGA, true));
		}
		if (extras.contains(PREF_SIRF_ENABLE_RMC)){
			enableNmeaRMC(extras.getBoolean(PREF_SIRF_ENABLE_RMC, true));
		}
	}

	private void enableNmeaGGA(boolean enable){
		if (gpsManager != null){
			if (enable){
				gpsManager.sendNmeaCommand(getString(R.string.sirf_nmea_gga_on));
			} else {
				gpsManager.sendNmeaCommand(getString(R.string.sirf_nmea_gga_off));
			}
		}
	}

	private void enableNmeaRMC(boolean enable){
		if (gpsManager != null){
			if (enable){
				gpsManager.sendNmeaCommand(getString(R.string.sirf_nmea_rmc_on));
			} else {
				gpsManager.sendNmeaCommand(getString(R.string.sirf_nmea_rmc_off));
			}
		}
	}

	private void enableNmeaGLL(boolean enable){
		if (gpsManager != null){
			if (enable){
				gpsManager.sendNmeaCommand(getString(R.string.sirf_nmea_gll_on));
			} else {
				gpsManager.sendNmeaCommand(getString(R.string.sirf_nmea_gll_off));
			}
		}
	}

	private void enableNmeaVTG(boolean enable){
		if (gpsManager != null){
			if (enable){
				gpsManager.sendNmeaCommand(getString(R.string.sirf_nmea_vtg_on));
			} else {
				gpsManager.sendNmeaCommand(getString(R.string.sirf_nmea_vtg_off));
			}
		}
	}

	private void enableNmeaGSA(boolean enable){
		if (gpsManager != null){
			if (enable){
				gpsManager.sendNmeaCommand(getString(R.string.sirf_nmea_gsa_on));
			} else {
				gpsManager.sendNmeaCommand(getString(R.string.sirf_nmea_gsa_off));
			}
		}
	}

	private void enableNmeaGSV(boolean enable){
		if (gpsManager != null){
			if (enable){
				gpsManager.sendNmeaCommand(getString(R.string.sirf_nmea_gsv_on));
			} else {
				gpsManager.sendNmeaCommand(getString(R.string.sirf_nmea_gsv_off));
			}
		}
	}

	private void enableNmeaZDA(boolean enable){
		if (gpsManager != null){
			if (enable){
				gpsManager.sendNmeaCommand(getString(R.string.sirf_nmea_zda_on));
			} else {
				gpsManager.sendNmeaCommand(getString(R.string.sirf_nmea_zda_off));
			}
		}
	}

	private void enableSBAS(boolean enable){
		if (gpsManager != null){
			if (enable){
				gpsManager.sendNmeaCommand(getString(R.string.sirf_nmea_sbas_on));
			} else {
				gpsManager.sendNmeaCommand(getString(R.string.sirf_nmea_sbas_off));
			}
		}
	}

	private void enableNMEA(boolean enable){
		if (gpsManager != null){
			if (enable){
				SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
				int gll = (sharedPreferences.getBoolean(PREF_SIRF_ENABLE_GLL, false)) ? 1 : 0 ;
				int vtg = (sharedPreferences.getBoolean(PREF_SIRF_ENABLE_VTG, false)) ? 1 : 0 ;
				int gsa = (sharedPreferences.getBoolean(PREF_SIRF_ENABLE_GSA, false)) ? 5 : 0 ;
				int gsv = (sharedPreferences.getBoolean(PREF_SIRF_ENABLE_GSV, false)) ? 5 : 0 ;
				int zda = (sharedPreferences.getBoolean(PREF_SIRF_ENABLE_ZDA, false)) ? 1 : 0 ;
				int mss = 0;
				int epe = 0;
				int gga = 1;
				int rmc = 1;
				String command = getString(R.string.sirf_bin_to_nmea_38400_alt, gga, gll, gsa, gsv, rmc, vtg, mss, epe, zda);
				gpsManager.sendSirfCommand(command);
			} else {
				gpsManager.sendNmeaCommand(getString(R.string.sirf_nmea_to_binary));
			}
		}
	}

	private void enableStaticNavigation(boolean enable){
		if (gpsManager != null){
			SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
			boolean isInNmeaMode = sharedPreferences.getBoolean(PREF_SIRF_ENABLE_NMEA, true);
			if (isInNmeaMode){
				enableNMEA(false);
			}
			if (enable){
				gpsManager.sendSirfCommand(getString(R.string.sirf_bin_static_nav_on));
			} else {
				gpsManager.sendSirfCommand(getString(R.string.sirf_bin_static_nav_off));
			}
			if (isInNmeaMode){
				enableNMEA(true);
			}
		}
	}

	@Override
	public void onDestroy() {
		BluetoothGnssManager manager = gpsManager;
		gpsManager  = null;
		if (manager != null){
			if (manager.getDisableReason() != 0){
				toast.setText(getString(R.string.msg_gps_provider_stopped_by_problem, getString(manager.getDisableReason())));
				toast.show();
			} else {
				toast.setText(R.string.msg_gps_provider_stopped);
				toast.show();
			}
			manager.removeNmeaListener(this);
			gpsMockProvider.disableMockLocationProvider();
			manager.disable();
		}
		endTrack();
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
    isRunning = false;
		super.onDestroy();
	}

	private void beginTrack(){
		SimpleDateFormat fmt = new SimpleDateFormat("_yyyy-MM-dd_HH-mm-ss'.nmea'");
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		String trackDirName = sharedPreferences.getString(PREF_TRACK_FILE_DIR, this.getString(R.string.defaultTrackFileDirectory));
		String trackFilePrefix = sharedPreferences.getString(PREF_TRACK_FILE_PREFIX, this.getString(R.string.defaultTrackFilePrefix));
		trackFile = new File(trackDirName,trackFilePrefix+fmt.format(new Date()));
		Log.d(LOG_TAG, "Writing the prelude of the NMEA file: "+trackFile.getAbsolutePath());
		File trackDir = trackFile.getParentFile();
		try {
			if ((! trackDir.mkdirs()) && (! trackDir.isDirectory())){
				Log.e(LOG_TAG, "Error while creating parent dir of NMEA file: "+trackDir.getAbsolutePath());
			}
			writer = new PrintWriter(new BufferedWriter(new FileWriter(trackFile)));
			preludeWritten = true;
		} catch (IOException e) {
			Log.e(LOG_TAG, "Error while writing the prelude of the NMEA file: "+trackFile.getAbsolutePath(), e);
			// there was an error while writing the prelude of the NMEA file, stopping the service...
			stopSelf();
		}
	}
	private void endTrack(){
		if (trackFile != null && writer != null){
			Log.d(LOG_TAG, "Ending the NMEA file: "+trackFile.getAbsolutePath());
			preludeWritten = false;
			writer.close();
			trackFile = null;
		}
	}
	private void addNMEAString(String data){
		if (! preludeWritten){
			beginTrack();
		}
		Log.v(LOG_TAG, "Adding data in the NMEA file: "+ data);
		if (trackFile != null && writer != null){
			writer.print(data);
		}
	}
    public static boolean isRunning()
    {
        return isRunning;
    }

    /* (non-Javadoc)
     * @see android.app.Service#onBind(android.content.Intent)
     */
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(LOG_TAG, "trying access IBinder");
        return mServiceMessenger.getBinder();
    }

  /* XXX
   * keep for note
  private final IBinder mBinder = new LocalBinder();
  public class LocalBinder extends Binder {
        GnssProviderService getService() {
            // Return this instance of LocalService so clients can call public methods
            return GnssProviderService.this;
        }
  }
  */

    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MSG_REGISTER_CLIENT:
                mClients.add(msg.replyTo);
                break;
            case MSG_UNREGISTER_CLIENT:
                mClients.remove(msg.replyTo);
                break;
            default:
                super.handleMessage(msg);
            }
        }
    }

    private void sendGpsDisconnected() {
        for (int i=mClients.size()-1; i>=0; i--) {
            try {
                mClients.get(i).send(Message.obtain(null, MSG_DISCONNECTED, 0, 0));
            } catch (RemoteException e) {
                // The client is dead.
                mClients.remove(i);
            }
        }
    }
    private void sendGpsUpdate(String data) {
        for (int i=mClients.size()-1; i>=0; i--) {
            try {
                Message msg = Message.obtain(null, MSG_UPDATED, data);
                mClients.get(i).send(msg);
            } catch (RemoteException e) {
                // The client is dead.
                mClients.remove(i);
            }
        }
    }

    @Override
    public void onGpsStatusChanged(int event){
        if (nmeaParser != null){
            GnssStatus gnssStatus = nmeaParser.getGnssStatus();
            long timestamp = gnssStatus.getFixTimestamp();
            sendGpsUpdate(Long.toString(timestamp));
        }
    }

	@Override
	public void onNmeaReceived(long timestamp, String data) {
		addNMEAString(data);
	}
}

// vim: tabstop=4 expandtab shiftwidth=4 softtabstop=4
