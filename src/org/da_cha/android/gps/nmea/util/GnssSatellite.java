/*
 * Copyright 2014 Hiroshi Miura <miurahr@linux.com>
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

package org.da_cha.android.gps.nmea.util;


public class GnssSatellite {
  public float azimuth;
  public float elevation;
  public float snr;

  // cannot change rpn
  private int rpn;

  private enum SatelliteSystem {GPS, GLONASS, GALILEO, QZSS, IRNASS, BEIDOU}
  private SatelliteSystem system;

  /*
   * Satellite identification
   *
   * System   System ID   Satellite ID        Signal ID      Signal/Channel
   * GPS       1 (GP)        1-99                0            All Signals
   *                         1-32 reserve GPS    1            L1 C/A
   *                         33-64 reserve SBAS  2            L1 P(Y)
   *                         66-99 undefined     3            L1 M
   *                                             4            L2 P(Y)
   *                                             5            L2C-M
   *                                             6            L5-I
   *                                             7            L5-Q
   *                                             9-F          Reserved
   *
   * GLONASS  2 (GL)         1-99                0            All Signals
   *                         1-32 undefined      1            G1 C/A
   *                         32-64 for SBAS      2            G1 P
   *                         65-99 for GLONASS   3            G2 C/A
   *                                             4            GLONASS (M) G2 P
   *                                             5-F          Reserved
   *
   * GALILEO  3 (GA)         1-99                0            All signals
   *                         1-36 for Galileo    1            E5a
   *                         37-64 for SBAS      2            E5b
   *                         65-99 undefined     3            E5 a+b
   *                                             4            E6-A
   *                                             5            E6-BC
   *                                             6            L1-A
   *                                             7            L1-BC
   *                                             8-F          Reserved
   *
   * BEIDOU  unknown
   * 
   * QZSS     ? (QZ)         193                 unknown      unknown
   *
   */

  public GnssSatellite(String systemid, int rpn){
    this.rpn = rpn;
    setSystem(systemid);
  }

  private void setSystem(String systemid){
    if (systemid.equals("GP")){
      this.system = SatelliteSystem.GPS;
    } else if (systemid.equals("GL")){
      this.system = SatelliteSystem.GLONASS;
    } else if (systemid.equals("QZ")){
      this.system = SatelliteSystem.QZSS;
    } else if (systemid.equals("GA")){
      this.system = SatelliteSystem.GALILEO;
    } else {
      // unknown satellite
    }
  }

  public int getRpn(){
    return this.rpn;
  }
  public boolean isRpn(int n){
    return (n == this.rpn);
  }

  // object that has same RPN are equals for satellite.
  @Override
  public int hashCode(){
    return this.rpn;
  }

  @Override
  public boolean equals(Object o){
    if (o == this){
      return true;
    }
    if (! (o instanceof GnssSatellite)) {
      return false;
    }
    return (((GnssSatellite)o).getRpn() == this.rpn);
  }

  public void setStatus(float azimuth, float elevation, float snr){
    this.azimuth = azimuth;
    this.elevation = elevation;
    this.snr = snr;
  }

  public String getSystemPrefix(){
    switch(this.system){
      case GPS:
        return "GP";
      case GLONASS:
        return "GL";
      case QZSS:
        return "QZ";
      case GALILEO:
        return "GA";
    }
    return null;
  }

  public float getElevation(){
    return this.elevation;
  }
  public float getAzimuth(){
    return this.azimuth;
  }
  public float getSnr(){
    return this.snr;
  }

  public boolean isQzss(){
    return (this.system == SatelliteSystem.QZSS);
  }

  public boolean isGlonass(){
    return (this.system == SatelliteSystem.GLONASS);
  }

  public boolean isGalileo(){
    return (this.system == SatelliteSystem.GALILEO);
  }

  public boolean isGps(){
    return (this.system == SatelliteSystem.GPS);
  }
}
