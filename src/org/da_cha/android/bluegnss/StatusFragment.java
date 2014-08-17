/*
 * Copyright 2014, Hiroshi Miura <miurahr@linux.com>
 * Copyright 2014, BlueGnss4OSM Project
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

import java.util.ArrayList;

import org.da_cha.android.bluegnss.GnssProviderService;
import org.da_cha.android.bluegnss.GnssStatus;
import org.da_cha.android.bluegnss.view.GnssStatusView;
import org.da_cha.android.bluegnss.R;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.util.Log;


public class StatusFragment extends Fragment {

    private final static String LOG_TAG = "BlueGNSS";

    private GnssProviderService mService = null;
    private boolean mIsBound = false;
    private boolean mIsRegistered = false;
    private GnssUpdateReceiver mGnssUpdateReceiver;

    private GnssStatusView mGnssStatusView;
    private View myView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        myView = inflater.inflate(R.layout.status_fragment, container, false);
        mGnssUpdateReceiver = new GnssUpdateReceiver();
        doRegisterReceiver();
        CheckIfServiceIsRunning();
        return myView;
    }

    /*
     * Service communications
     */
    private void CheckIfServiceIsRunning() {
        Log.d(LOG_TAG, "StatusFragment: called CheckIfServiceIsRunning()");
        if (GnssProviderService.isRunning()) {
            doBindService();
        } else {
            mIsBound = false;
        }
    }
    private void doRegisterReceiver(){
        if (!mIsRegistered) {
            IntentFilter filter = new IntentFilter(GnssProviderService.NOTIFY_UPDATE);
            getActivity().registerReceiver(mGnssUpdateReceiver, filter);
            mIsRegistered = true;
        }
    }
    private void doUnregisterReceiver(){
        if (mIsRegistered) {
            getActivity().unregisterReceiver(mGnssUpdateReceiver);
            mIsRegistered = false;
        }
    }
    private void doBindService() {
        if (!mIsBound){
            Context appContext = getActivity().getApplicationContext();
            getActivity().bindService(new Intent(appContext, GnssProviderService.class), mConnection, Context.BIND_AUTO_CREATE);
            mIsBound = true;
        }
    }
    private void doUnbindService() {
        if (mIsBound) {
            getActivity().unbindService(mConnection);
            mIsBound = false;
        }
    }
    @Override
    public void onActivityCreated(Bundle bundle){
        mGnssStatusView = (GnssStatusView) myView.findViewById(R.id.GnssStatusView);
        super.onActivityCreated(bundle);
    }
    @Override
    public void onResume() {
        super.onResume();
        CheckIfServiceIsRunning();
    }
    @Override
    public void onPause() {
        doUnbindService();
        super.onPause();
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
            Log.d(LOG_TAG, "update satellite list");
            status = mService.getGnssStatus();
            ArrayList<GnssSatellite> satList = status.getSatellitesList();
            mGnssStatusView.setSatelliteList(satList);
        }
    }
}
// vim: tabstop=4 expandtab shiftwidth=4 softtabstop=4
