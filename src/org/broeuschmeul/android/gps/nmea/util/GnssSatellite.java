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

package org.broeuschmeul.android.gps.nmea.util;

import java.util.ArrayList;
import java.util.List;

public class GnssSatellite {
  public float azimuth;
  public float elevation;
  public float snr;

  // cannot change rpn
  private int rpn;

  public GnssSatellite(int rpn){
    this.rpn = rpn;
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

}
