/*
 * Copyright 2014, Hiroshi Miura <miurahr@linux.com>
 * 
 * This file is part of BlueGnss4OSm.
 *
 * This is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * It is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with it. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 * To build this, you'll need to copy a few files from the Android source
 * code into this project:
 *
 * frameworks/base/location/java/android/location/IGpsStatusListener.aidl
 * frameworks/base/location/java/android/location/ILocationListener.aidl
 * frameworks/base/location/java/android/location/ILocationManager.aidl
 * frameworks/base/location/java/android/location/Address.aidl
 *
 * Place those files in an "android.location" namespace in your project in
 * eclipse, and the Android plugin should generate the correct .java files
 * that will make this thing run.
 */

package org.da_cha.android.bluegnss.provider;

import android.app.Service;
import android.content.Context;
import android.location.IGpsStatusListener;
import android.location.LocationManager;
import android.location.GpsStatus;
import android.os.RemoteException;

import java.lang.reflect.*;

import android.util.Log;

/**
 * This class is used to provide Mock GPS Status provider for clients.
 *
 */
public class MockGpsStatusProvider {

    /**
    * Tag used for log messages
    */
    private static final String LOG_TAG = "BlueGNSS";

    private boolean mockGpsEnabled;

    private LocationManager mLocationManager;
    private IGpsStatusListener mGpsListener;
    private GpsStatus mGpsStatus;

    public MockGpsStatusProvider(Service callingService) {
        this.mLocationManager = (LocationManager)callingService.getSystemService(Context.LOCATION_SERVICE);
    }

    /**
     * Enables the Mock GPS Status Provider used for the bluetooth GPS.
     */
    public void enableMockGpsStatusProvider(){
        try {
            if (! mockGpsEnabled){
                Class c = Class.forName(mLocationManager.getClass().getName());
                Field f = c.getDeclaredField("mGpsStatus");
                f.setAccessible(true);
                mGpsStatus = (GpsStatus) f.get(mGpsStatus);

                Class gc = Class.forName(mGpsStatus.getClass().getName());
                Method m = gc.getDeclaredMethod("setStatus", int.class, int[].class, float[].class, float[].class, float[].class, int.class, int.class, int.class );
                m.setAccessible(true);
            }
        } catch (SecurityException e){
            Log.d(LOG_TAG, "Error while enabling Mock Gps Status Provider", e);
            Log.d(LOG_TAG, "Exception: " + e.getMessage());
            disableMockGpsStatusProvider();
            return;
        } catch (NoSuchFieldException e) {
            Log.d(LOG_TAG, "Error while enabling Mock Gps Status Provider", e);
	        //throw new RuntimeException(e);
            Log.d(LOG_TAG, "Exception: " + e.getMessage());
            return;
        } catch (ClassNotFoundException e){
            Log.d(LOG_TAG, "Error while enabling Mock Gps Status Provider", e);
            Log.d(LOG_TAG, "Exception: " + e.getMessage());
            return;
        } catch (IllegalAccessException e){
            Log.d(LOG_TAG, "Error while enabling Mock Gps Status Provider", e);
            Log.d(LOG_TAG, "Exception: " + e.getMessage());
            return;
       }
        mockGpsEnabled = true;
    }

    public void disableMockGpsStatusProvider(){
        try {
            if (mockGpsEnabled){
                mockGpsEnabled = false;
                Log.d(LOG_TAG, "removed mock GPS status");
            } else {
                Log.d(LOG_TAG, "Mock Gps provider already disabled.");
            }
        } catch (SecurityException e){
            Log.e(LOG_TAG, "Error while enabling Mock Gps Status Provider", e);
        } finally {
            mockGpsEnabled = false;
        }
    }

    /**
     * @return the mockGpsEnabled
     */
    public boolean isMockGpsEnabled() {
        return mockGpsEnabled;
    }
}
// vim: tabstop=4 expandtab shiftwidth=4 softtabstop=4
