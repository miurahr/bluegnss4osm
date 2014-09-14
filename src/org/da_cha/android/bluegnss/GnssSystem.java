/*
 * Copyright 2014 Hiroshi Miura <miurahr@linux.com>
 * 
 * This file is part of BluetoothGPS4OSM.
 *
 * It is free software: you can redistribute it and/or modify
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
 *  along with BluetoothGPS4OSm. If not, see <http://www.gnu.org/licenses/>.
 */

package org.da_cha.android.bluegnss;

 
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
   * BEIDOU   4 (BD)         1-99                unknown      unknown
   *                         1-36 for Beidou/Compass
   * 
   * QZSS     5 (QZ)         193                 unknown      unknown
   *
   */


public enum GnssSystem {
    GPS     (1, 0),
    GLONASS (2, 100),
    GALILEO (3, 200),
    BEIDOU  (4, 300),
    QZSS    (5, 400),
    SBAS    (6, 600),
    UNKNOWN (99,1000);

    private final int id;
    private final int offset;

    GnssSystem (int systemid, int offset) {
      this.id = systemid;
      this.offset = offset;
    }

    public int systemid() {return id;}
    public int offset()   {return offset;}

    public int index(int sat) {
      return offset + sat;
    }

    public int satelliteid(int index) {
      return index - offset;
    }

    public String prefix(int len){
      if (len == 2){
        switch(this){
          case GPS:
            return "GP";
          case GLONASS:
            return "GL";
          case QZSS:
            return "QZ";
          case GALILEO:
            return "GA";
          case BEIDOU:
            return "BD";
          case SBAS:
            return "SB";
          default:
            return "UN";
        }
      } else if (len == 1){
        switch(this){
          case GPS:
            return "G";
          case GLONASS:
            return "L";
          case QZSS:
            return "Q";
          case GALILEO:
            return "E";
          case BEIDOU:
            return "B";
          case SBAS:
            return "S";
          default:
            return "U";
        }
      } else {
        return "";
      }
    }
}

