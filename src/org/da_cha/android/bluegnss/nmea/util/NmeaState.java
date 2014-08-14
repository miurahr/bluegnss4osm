/*
 * Copyright 2014, Hiroshi Miura <miurahr@linux.com>
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

package org.da_cha.android.bluegnss.nmea.util;

/**
 * This class is used to keep NMEA sentence status automaton.
 *
 */
public class NmeaState {

	private long timestamp;
	private boolean fixed = false;
  private enum nmeastate {START, RECEIVE, COMPLETE}
  private nmeastate currentStatus = nmeastate.START;
  
  public long getTimestamp(){
    return this.timestamp;
  }
  public boolean canNotify(){
    return (this.currentStatus == nmeastate.COMPLETE);
  }

  public boolean isFixed(){
    return this.fixed;
  }

  public boolean canFixNotify(){
    return (this.fixed && this.currentStatus == nmeastate.COMPLETE);
  }

  public boolean recvGGA(boolean fixed, long time){
    this.fixed = fixed;
    this.timestamp = time;
    nmeastate previousStatus = currentStatus;
    this.currentStatus = nmeastate.RECEIVE;
    return (previousStatus == nmeastate.START);
  }

  public boolean recvRMC(boolean fixed, long time){
    if (this.timestamp != time){
      this.currentStatus = nmeastate.START;
      return false; // invalid
    }
    this.currentStatus = nmeastate.COMPLETE;
    return true;
  }

  public void recvVTG(){
    this.currentStatus = nmeastate.START;
    this.timestamp = 0;
    this.fixed = false;
  }

  public boolean recvGLL(long time){
    return (this.timestamp == time && this.currentStatus == nmeastate.RECEIVE);
  }
  public boolean recvGNS(long time){
    return (this.timestamp == time && this.currentStatus == nmeastate.RECEIVE);
  }
  public boolean recvGSA(){
    return (this.currentStatus == nmeastate.RECEIVE);
  }
  public boolean recvGSV(){
    return (this.currentStatus == nmeastate.RECEIVE);
  }
}
