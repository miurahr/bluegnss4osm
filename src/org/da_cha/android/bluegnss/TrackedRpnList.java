/*
 * Copyright 2014 Hiroshi Miura <miurahr@linux.com>
 * 
 * This file is part of BluetoothGNSS4OSM.
 *
 * BluetoothGNSS4OSM is free software: you can redistribute it and/or modify
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
 *  along with source code. If not, see <http://www.gnu.org/licenses/>.
 */

package org.da_cha.android.bluegnss;

import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.Iterator;

import org.da_cha.android.bluegnss.GnssSystem;

public class TrackedRpnList {

  private ArrayList<Integer> satellitesRpnListInFix = new ArrayList<Integer>();
  private GnssSystem system;

  TrackedRpnList (GnssSystem system) {
    this.system = system;
  }

  public void set(ArrayList<Integer> rpnList){
      satellitesRpnListInFix.clear();
      for (Integer rpn : rpnList){
        satellitesRpnListInFix.add(rpn);
      }
  }

  public void add(int rpn){
      satellitesRpnListInFix.add(rpn);
  }

  public ArrayList<Integer> get() {
      return satellitesRpnListInFix;
  }

  public void clear(){
      this.satellitesRpnListInFix.clear();
  }

  public GnssSystem system(){
      return this.system;
  }

}
