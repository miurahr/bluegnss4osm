/*
 * Copyright 2014 Hiroshi Miura <miurahr@linux.com>
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

package org.broeuschmeul.android.gps.nmea.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Iterator;
import java.util.ListIterator;

public class gnssStatus {

	private long fixTimestamp;
  private long startTimestamp;
  private long firstFixTimestamp=0;
  private float PDOP;
  private float HDOP;
  private float VDOP;
  private double latitude;
  private double longitude;
  private double altitude;
  private double height;
  private double speed;
  private float angle;
  private int nbsat;
  private int numSatellites;
  private boolean active;
  /* 1: nofix, 2: 2D fix, 3: 3D fix */
  private int fixMode; 
  /* 
       Fix quality: 0 = invalid
                    1 = GPS fix (SPS)
                    2 = DGPS fix
                    3 = PPS fix
                    4 = Real Time Kinematic
                    5 = double RTK
                    6 = estimated (dead reckoning) (2.3 feature)
                    7 = Manual input mode
                    8 = Simulation mode
   */
  private int quality;
  // for NMEA 0183 version 3.00 active the Mode indicator field is added
  // Mode indicator, (A=autonomous, D=differential, E=Estimated, N=not valid, S=Simulator )
  private String mode;

  private HashMap<Integer, gnssSatellite> gnssSatellitesList = new HashMap<Integer, gnssSatellite>();

  // satallite list
  // accessor
  public void addSatellite(gnssSatellite sat){
    gnssSatellitesList.put(sat.getRpn(), sat);
  }
  // clear list at all
  public void clearSatellitesList(){
    gnssSatellitesList.clear();
  }
  public void clearSatellitesList(ArrayList<Integer> activeList){
    ArrayList<Integer> currentRpnList = new ArrayList<Integer>();
    for (Integer rpn: gnssSatellitesList.keySet()){
      currentRpnList.add(rpn);
    }
    for (Integer i : currentRpnList){
      if (!activeList.contains(i)) {
        removeSatellite(i);
      }
    }
  }
  // clear sat data, return cleared satellite
  public gnssSatellite removeSatellite(int rpn){
    return gnssSatellitesList.remove(rpn);
  }
  public gnssSatellite getSatellite(int rpn){
    return gnssSatellitesList.get(rpn);
  }

  public void setTrackedSatellites(ArrayList<Integer> rpnList){
    for (Integer rpn  : gnssSatellitesList.keySet()){
      if (rpnList.contains(rpn)){
        gnssSatellitesList.get(rpn).usedinFix();
      } else {
        gnssSatellitesList.get(rpn).unusedinFix();
      }
    }
  }
  // timestamps
  public void clearTTFF(){
    this.firstFixTimestamp=0;
  }
  public long getTTFF(){
    if (this.firstFixTimestamp == 0 || this.startTimestamp == 0 ||
        this.firstFixTimestamp < this.startTimestamp) {
      // invalid status
      return 0;
    } else {
      return this.firstFixTimestamp - this.startTimestamp;
    }
  }
  public long getFixTimestamp(){
    return this.fixTimestamp;
  }
  public void setFixTimestamp(long timestamp){
    this.fixTimestamp = timestamp;
    if (this.firstFixTimestamp == 0){
      this.firstFixTimestamp = timestamp;
    }
  }
  public void setStartTimestamp(long timestamp){
    this.startTimestamp = timestamp;
  }
  // clear all
  public void clear(){
    clearTTFF();
    clearSatellitesList();
    startTimestamp = 0;
	  fixTimestamp = 0;
    PDOP = 0f;
    HDOP = 0f;
    VDOP = 0f;
    latitude  = 0d;
    longitude = 0d;
    altitude  = 0d;
    height    = 0d;
    speed     = 0d;
    angle     = 0f;
    nbsat = 0;
    numSatellites = 0;
    active = false;
    fixMode = 0;
    quality = 0;
    mode = "N";
  }
  // accessors
  // 
  // Lat/Lon/Alt
  public double getLatitude(){
    return this.latitude;
  }
  public void setLatitude(double lat){
    this.latitude = lat;
  }
  public double getLongitude(){
    return this.longitude;
  }
  public void setLongitude(double lon){
    this.longitude = lon;
  }
  public double getAltitude(){
    return this.altitude;
  }
  public void setAltitude(double alt){ this.altitude = alt;
  }
  // DOP
  public double getPDOP(){
    return this.PDOP;
  }
  public void setPDOP(float dop){
    this.PDOP = dop;
  }
  public double getHDOP(){
    return this.HDOP;
  }
  public void setHDOP(float dop){
    this.HDOP = dop;
  }
  public double getVDOP(){
    return this.VDOP;
  }
  public void setVDOP(float dop){
    this.VDOP = dop;
  }
  // others
  public double getHeight(){
    return this.height;
  }
  public void setHeight(double height){
    this.height = height;
  }
  public double getSpeed(){
    return this.speed;
  }
  public void setSpeed(double speed){
    this.speed = speed;
  }
  public double getAngle(){
    return this.angle;
  }
  public void setAngle(float angle){
    this.angle = angle;
  }
  public int getQuality(){
    return this.quality;
  }
  public void setQuality(int q){
    this.quality = q;
  }
  public int getNbSat(){
    return this.nbsat;
  }
  public void setNbSat(int sat){
    this.nbsat = sat;
  }
  public String getMode(){
    return this.mode;
  }
  public void setMode(String mode){
    this.mode = mode;
  }
  public int getNumSatellites(){
    return this.numSatellites;
  }
  public void setNumSatellites(int num){
    this.numSatellites = num;
  }

}
