/*
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

package org.broeuschmeul.android.gps.nmea.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.location.Criteria;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.TextUtils;
import android.text.TextUtils.SimpleStringSplitter;
import android.util.Log;

import org.broeuschmeul.android.gps.bluetooth.provider.BluetoothGpsMockProvider;

/**
 * This class is used to parse NMEA sentences an generate the Android Locations when there is a new GPS FIX.
 * It manage also the Mock Location Provider (enable/disable/fix & status notification)
 * and can compute the the checksum of a NMEA sentence.
 * 
 * @author Herbert von Broeuschmeul
 *
 */
public class NmeaParser {
	/**
	 * Tag used for log messages
	 */
	private static final String LOG_TAG = "BlueGPS";

	private String fixTime = null;
	private long fixTimestamp;
	private long firstFixTimestamp;
	private boolean fixed = false;

	private boolean hasGGA = false;
	private boolean hasRMC = false;
	private float precision = 10f;
	private Location fix = null;

	private final int GPS_NONE      = 0;
	private final int GPS_FIXED     = 3;
	private final int GPS_NOTIFY    = 4;
	private int currentGpsStatus = GPS_NONE;
  private boolean gpsFixNotified = false;

	private BluetoothGpsMockProvider mockProvider;

	private gnssStatus gnssStatus;
	private ArrayList<Integer> activeSatellites = new ArrayList<Integer>();
	private ArrayList<Integer> prnList = new ArrayList<Integer>();

	public NmeaParser(){
		this(5f);
	}

	public NmeaParser(float precision){
		this.precision = precision;
		this.gnssStatus = new gnssStatus();
	}

	public void setGpsMockProvider(BluetoothGpsMockProvider mockProvider){
		this.mockProvider = mockProvider;
	}

	public gnssStatus getGnssStatus(){
		return this.gnssStatus;
	}

	// parse NMEA Sentence 
	public String parseNmeaSentence(String gpsSentence) throws SecurityException {
		String nmeaSentence = null;
		Log.v(LOG_TAG, "data: "+System.currentTimeMillis()+" "+gpsSentence);
		long ern = SystemClock.elapsedRealtimeNanos();
		Pattern xx = Pattern.compile("\\$([^*$]*)(?:\\*([0-9A-F][0-9A-F]))?\r\n");
		Matcher m = xx.matcher(gpsSentence);
		if (m.matches()){
			nmeaSentence = m.group(0);
			String sentence = m.group(1);
			String checkSum = m.group(2);
			Log.v(LOG_TAG, "data: "+System.currentTimeMillis()+" "+sentence+" cheksum: "+checkSum +" control: "+String.format("%02X",computeChecksum(sentence)));
			SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(',');
			splitter.setString(sentence);
			String command = splitter.next();
			if (command.equals("GPGGA")){
				/* $GPGGA,123519,4807.038,N,01131.000,E,1,08,0.9,545.4,M,46.9,M,,*47
					
					Where:
					     GGA          Global Positioning System Fix Data
					     123519       Fix taken at 12:35:19 UTC
					     4807.038,N   Latitude 48 deg 07.038' N
					     01131.000,E  Longitude 11 deg 31.000' E
					     1            Fix quality: 0 = invalid
					                               1 = GPS fix (SPS)
					                               2 = DGPS fix
					                               3 = PPS fix
											       4 = Real Time Kinematic
											       5 = Float RTK
					                               6 = estimated (dead reckoning) (2.3 feature)
											       7 = Manual input mode
											       8 = Simulation mode
					     08           Number of satellites being tracked
					     0.9          Horizontal dilution of position
					     545.4,M      Altitude, Meters, above mean sea level
					     46.9,M       Height of geoid (mean sea level) above WGS84
					                      ellipsoid
					     (empty field) time in seconds since last DGPS update
					     (empty field) DGPS station ID number
					     *47          the checksum data, always begins with *
				 */
				// UTC time of fix HHmmss.S
				String time = splitter.next();
				// latitude ddmm.M
				String lat = splitter.next();
				// direction (N/S)
				String latDir = splitter.next();
				// longitude dddmm.M
				String lon = splitter.next();
				// direction (E/W)
				String lonDir = splitter.next();
				/* fix quality: 
				  	0= invalid
					1 = GPS fix (SPS)
					2 = DGPS fix
					3 = PPS fix
					4 = Real Time Kinematic
					5 = Float RTK
					6 = estimated (dead reckoning) (2.3 feature)
					7 = Manual input mode
					8 = Simulation mode
				 */
				String quality = splitter.next();
				// Number of satellites being tracked
				String nbSat = splitter.next();
				// Horizontal dilution of position (float)
				String hdop = splitter.next();
				// Altitude, Meters, above mean sea level
				String alt = splitter.next();
				String altUnit = splitter.next();
				// Height of geoid (mean sea level) above WGS84 ellipsoid
				String geoAlt = splitter.next();
				String geoUnit = splitter.next();
				// time in seconds since last DGPS update
				// DGPS station ID number
				//
				// Update GNSS object status
				if (quality != null && !quality.equals("") && !quality.equals("0") ){
					if (! mockProvider.isMockStatus(LocationProvider.AVAILABLE)){
						long updateTime = parseNmeaTime(time);
						fixTime = null;
						hasGGA = false;
						hasRMC = false;
						fix = null;
						mockProvider.notifyStatusChanged(LocationProvider.AVAILABLE, null, updateTime);
						firstFixTimestamp = updateTime;
						if (!gpsFixNotified) {
							currentGpsStatus = GPS_FIXED;
						}
					}
					if (! time.equals(fixTime)){
						fix = new Location(LocationManager.GPS_PROVIDER);
						fixTime = time;
						fixTimestamp = parseNmeaTime(time);
						fix.setTime(fixTimestamp);
					}
					fix.setElapsedRealtimeNanos(ern);
					gnssStatus.setQuality(Integer.parseInt(quality));
					if (lat != null && !lat.equals("")){
						gnssStatus.setLatitude(parseNmeaLatitude(lat,latDir));
						fix.setLatitude(parseNmeaLatitude(lat,latDir));
					}
					if (lon != null && !lon.equals("")){
						gnssStatus.setLongitude(parseNmeaLongitude(lon,lonDir));
						fix.setLongitude(parseNmeaLongitude(lon,lonDir));
					}
					if (hdop != null && !hdop.equals("")){
						gnssStatus.setHDOP(Float.parseFloat(hdop));
						fix.setAccuracy(Float.parseFloat(hdop)*precision);
					}
					if (alt != null && !alt.equals("")){
						gnssStatus.setAltitude(Double.parseDouble(alt));
						fix.setAltitude(Double.parseDouble(alt));
					}
					if (nbSat != null && !nbSat.equals("")){
						Bundle extras = new Bundle();
						extras.putInt("satellites", Integer.parseInt(nbSat));
						gnssStatus.setNbSat(Integer.parseInt(nbSat));
						fix.setExtras(extras);
					}
					if (geoAlt != null & !geoAlt.equals("")){
						gnssStatus.setHeight(Float.parseFloat(geoAlt));
					}
					hasGGA = true;
					mockProvider.notifyFix(fix);
					fix = null;
				} else if(quality.equals("0")){
					if (!mockProvider.isMockStatus(LocationProvider.TEMPORARILY_UNAVAILABLE)){
						fixTime = null;
						hasGGA = false;
						hasRMC = false;
						fix = null;
						long updateTime = parseNmeaTime(time);
						mockProvider.notifyStatusChanged(LocationProvider.TEMPORARILY_UNAVAILABLE, null, updateTime);
					}
				}
			} else if (command.equals("GPRMC")){
				/* $GPRMC,123519,A,4807.038,N,01131.000,E,022.4,084.4,230394,003.1,W*6A
	
				   Where:
				     RMC          Recommended Minimum sentence C
				     123519       Fix taken at 12:35:19 UTC
				     A            Status A=active or V=Void.
				     4807.038,N   Latitude 48 deg 07.038' N
				     01131.000,E  Longitude 11 deg 31.000' E
				     022.4        Speed over the ground in knots
				     084.4        Track angle in degrees True
				     230394       Date - 23rd of March 1994
				     003.1,W      Magnetic Variation
				     *6A          The checksum data, always begins with *
				*/
				// UTC time of fix HHmmss.S
				String time = splitter.next();
				// fix status (A/V)
				String status = splitter.next();
				// latitude ddmm.M
				String lat = splitter.next();
				// direction (N/S)
				String latDir = splitter.next();
				// longitude dddmm.M
				String lon = splitter.next();
				// direction (E/W)
				String lonDir = splitter.next();
				// Speed over the ground in knots		 
				String speed = splitter.next();
				// Track angle in degrees True
				String bearing = splitter.next();
				// UTC date of fix DDMMYY
				String date = splitter.next();
				// Magnetic Variation ddd.D
				String magn = splitter.next();
				// Magnetic variation direction (E/W)
				String magnDir = splitter.next();
				// for NMEA 0183 version 3.00 active the Mode indicator field is added
				// Mode indicator, (A=autonomous, D=differential, E=Estimated, N=not valid, S=Simulator )
				gnssStatus.setMode(status);
				if (status != null && !status.equals("") && status.equals("A") ){
					if (! mockProvider.isMockStatus(LocationProvider.AVAILABLE)){
						long updateTime = parseNmeaTime(time);
						fixTime = null;
						hasGGA = false;
						hasRMC = false;
						fix = null;
						mockProvider.notifyStatusChanged(LocationProvider.AVAILABLE, null, updateTime);
						firstFixTimestamp = updateTime;
						if (!gpsFixNotified){
							currentGpsStatus = GPS_FIXED;
						}
					}
					if (! time.equals(fixTime)){
						fix = new Location(LocationManager.GPS_PROVIDER);
						fixTime = time;
						fixTimestamp = parseNmeaTime(time);
						fix.setTime(fixTimestamp);
					  gnssStatus.setFixTimestamp(fixTimestamp);
					} 
					fix.setElapsedRealtimeNanos(ern);
					if (lat != null && !lat.equals("")){
						gnssStatus.setLatitude(parseNmeaLatitude(lat,latDir));
						fix.setLatitude(parseNmeaLatitude(lat,latDir));
					}
					if (lon != null && !lon.equals("")){
						gnssStatus.setLongitude(parseNmeaLongitude(lon,lonDir));
						fix.setLongitude(parseNmeaLongitude(lon,lonDir));
					}
				if (speed != null && !speed.equals("")){
						gnssStatus.setSpeed(parseNmeaSpeed(speed, "N"));
						fix.setSpeed(parseNmeaSpeed(speed, "N"));
					}
					if (bearing != null && !bearing.equals("")){
						gnssStatus.setAngle(Float.parseFloat(bearing));
						fix.setBearing(Float.parseFloat(bearing));
					}
					hasRMC = true;
					if (! hasGGA) {
						mockProvider.notifyFix(fix);
						fix = null;
					}
				} else if(status.equals("V")){
					if (! mockProvider.isMockStatus(LocationProvider.TEMPORARILY_UNAVAILABLE)){
						long updateTime = parseNmeaTime(time);
						fixTime = null;
						hasGGA = false;
						hasRMC = false;
						fix = null;
						mockProvider.notifyStatusChanged(LocationProvider.TEMPORARILY_UNAVAILABLE, null, updateTime);
						// currentGpsStatus = GPS_STARTED;
					}
				}
			} else if (command.equals("GPGSA")){
				/*  $GPGSA,A,3,04,05,,09,12,,,24,,,,,2.5,1.3,2.1*39
	
					Where:
					     GSA      Satellite status
					     A        Auto selection of 2D or 3D fix (M = manual) 
					     3        3D fix - values include: 1 = no fix
					                                       2 = 2D fix
					                                       3 = 3D fix
					     04,05... PRNs of satellites used for fix (space for 12) 
					     2.5      PDOP (Position dilution of precision) 
					     1.3      Horizontal dilution of precision (HDOP) 
					     2.1      Vertical dilution of precision (VDOP)
					     *39      the checksum data, always begins with *
				 */
				// mode : A Auto selection of 2D or 3D fix / M = manual
				String mode = splitter.next();
				// fix type  : 1 - no fix / 2 - 2D / 3 - 3D
				String fixType = splitter.next();
				if (! "1".equals(fixType)) {
					prnList.clear();
					String prn;
					for (int i=0 ; i<12  ; i++){
						prn = splitter.next();
						if (prn != null && !prn.equals("")){
							prnList.add(Integer.parseInt(prn));
						}
					}
					gnssStatus.setTrackedSatellites(prnList);
					// Position dilution of precision (float)
					String pdop = splitter.next();
					// Horizontal dilution of precision (float)
					String hdop = splitter.next();
					// Vertical dilution of precision (float)
					String vdop = splitter.next();
					gnssStatus.setPDOP(Float.parseFloat(pdop));
					gnssStatus.setHDOP(Float.parseFloat(hdop));
					gnssStatus.setVDOP(Float.parseFloat(vdop));
				}
			} else if (command.equals("GPGSV")){
				/*   $GPGSV,2,1,08,01,40,083,46,02,17,308,41,12,07,344,39,14,22,228,45*75

				    Where:
				        GSV          Satellites in view
				        2            Number of sentences for full data
				        1            sentence 1 of 2
				        08           Number of satellites in view

				        01           Satellite PRN number
				        40           Elevation, degrees
				        083          Azimuth, degrees
				        46           SNR - higher is better
				                     for up to 4 satellites per sentence
				        *75          the checksum data, always begins with *
				 */
				String totalGsvSentence = splitter.next();
				String currentGsvSentence = splitter.next();
				String satellitesInView = splitter.next();

        Integer numCurrentGsvSentence = Integer.parseInt(currentGsvSentence);
        Integer numTotalGsvSentence   = Integer.parseInt(totalGsvSentence);
        Integer numSatellitesInView   = Integer.parseInt(satellitesInView);

        if (numCurrentGsvSentence.equals("1")){ // first sentence
					activeSatellites.clear();
				}
				gnssStatus.setNumSatellites(numSatellitesInView);

        int numRecord = 4;
				if (numCurrentGsvSentence == numTotalGsvSentence){ // last sentence
          numRecord = numSatellitesInView % 4;
          if (numRecord == 0){
            numRecord = 4;
          }
        }
				for (int i =0; i < numRecord; i++){
					String prn = splitter.next();
          if (prn != null && !prn.equals("")){
            String elevation = splitter.next();
            String azimuth = splitter.next();
            String snr = splitter.next();
            gnssSatellite sat = new gnssSatellite(Integer.parseInt(prn));
            sat.setStatus(parseNmeaFloat(elevation),parseNmeaFloat(azimuth),parseNmeaFloat(snr));
            gnssStatus.addSatellite(sat);
            activeSatellites.add(Integer.parseInt(prn));
          } else {
            break;
          }
				}
				if (numCurrentGsvSentence == numTotalGsvSentence){ // last sentence
		      currentGpsStatus = GPS_NOTIFY;
					gnssStatus.clearSatellitesList(activeSatellites);
				}
			} else if (command.equals("GPVTG")){
				/*  $GPVTG,054.7,T,034.4,M,005.5,N,010.2,K*48
					
					where:
					        VTG          Track made good and ground speed
					        054.7,T      True track made good (degrees)
					        034.4,M      Magnetic track made good
					        005.5,N      Ground speed, knots
					        010.2,K      Ground speed, Kilometers per hour
					        *48          Checksum
				 */
				// Track angle in degrees True
				String bearing = splitter.next();
				// T
				splitter.next();
				// Magnetic track made good
				String magn = splitter.next();
				// M
				splitter.next();
				// Speed over the ground in knots		 
				String speedKnots = splitter.next();
				// N
				splitter.next();
				// Speed over the ground in Kilometers per hour		 
				String speedKm = splitter.next();
				// K
				splitter.next();
				// for NMEA 0183 version 3.00 active the Mode indicator field is added
				// Mode indicator, (A=autonomous, D=differential, E=Estimated, N=not valid, S=Simulator )
			} else if (command.equals("GPGLL")){
				/*  $GPGLL,4916.45,N,12311.12,W,225444,A,*1D
					
					Where:
					     GLL          Geographic position, Latitude and Longitude
					     4916.46,N    Latitude 49 deg. 16.45 min. North
					     12311.12,W   Longitude 123 deg. 11.12 min. West
					     225444       Fix taken at 22:54:44 UTC
					     A            Data Active or V (void)
					     *iD          checksum data
				 */
				// latitude ddmm.M
				String lat = splitter.next();
				// direction (N/S)
				String latDir = splitter.next();
				// longitude dddmm.M
				String lon = splitter.next();
				// direction (E/W)
				String lonDir = splitter.next();
				// UTC time of fix HHmmss.S
				String time = splitter.next();
				// fix status (A/V)
				String status = splitter.next();
				// for NMEA 0183 version 3.00 active the Mode indicator field is added
				// Mode indicator, (A=autonomous, D=differential, E=Estimated, N=not valid, S=Simulator )
			}
		}
		return nmeaSentence;
	}

	public int getGpsStatusChange(){
		if (currentGpsStatus == GPS_NOTIFY){
      currentGpsStatus = GPS_NONE;
			return GpsStatus.GPS_EVENT_SATELLITE_STATUS;
		} else if (currentGpsStatus == GPS_FIXED && !gpsFixNotified){
      gpsFixNotified = true;
			return GpsStatus.GPS_EVENT_FIRST_FIX;
		}
		return 0;
	}

	public double parseNmeaLatitude(String lat,String orientation){
		double latitude = 0.0;
		if (lat != null && orientation != null && !lat.equals("") && !orientation.equals("")){
			double temp1 = Double.parseDouble(lat);
			double temp2 = Math.floor(temp1/100); 
			double temp3 = (temp1/100 - temp2)/0.6;
			if (orientation.equals("S")){
				latitude = -(temp2+temp3);
			} else if (orientation.equals("N")){
				latitude = (temp2+temp3);
			}
		}
		return latitude;
	}
	public double parseNmeaLongitude(String lon,String orientation){
		double longitude = 0.0;
		if (lon != null && orientation != null && !lon.equals("") && !orientation.equals("")){
			double temp1 = Double.parseDouble(lon);
			double temp2 = Math.floor(temp1/100); 
			double temp3 = (temp1/100 - temp2)/0.6;
			if (orientation.equals("W")){
				longitude = -(temp2+temp3);
			} else if (orientation.equals("E")){
				longitude = (temp2+temp3);
			}
		}
		return longitude;
	}
	public float parseNmeaSpeed(String speed,String metric){
		float meterSpeed = 0.0f;
		if (speed != null && metric != null && !speed.equals("") && !metric.equals("")){
			float temp1 = Float.parseFloat(speed)/3.6f;
			if (metric.equals("K")){
				meterSpeed = temp1;
			} else if (metric.equals("N")){
				meterSpeed = temp1*1.852f;
			}
		}
		return meterSpeed;
	}
	public long parseNmeaTime(String time){
		long timestamp = 0;
		SimpleDateFormat fmt = new SimpleDateFormat("HHmmss.SSS");
		fmt.setTimeZone(TimeZone.getTimeZone("GMT"));
		try {
			if (time != null && time != null){
				long now = System.currentTimeMillis();
				long today = now - (now %86400000L);
				long temp1;
				// sometime we don't have millisecond in the time string, so we have to reformat it 
				temp1 = fmt.parse(String.format((Locale)null,"%010.3f", Double.parseDouble(time))).getTime();
				long temp2 = today+temp1;
				// if we're around midnight we could have a problem...
				if (temp2 - now > 43200000L) {
					timestamp  = temp2 - 86400000L;
				} else if (now - temp2 > 43200000L){
					timestamp  = temp2 + 86400000L;
				} else {
					timestamp  = temp2;
				}
			}
		} catch (ParseException e) {
			Log.e(LOG_TAG, "Error while parsing NMEA time", e);
		}
		return timestamp;
	}
	public byte computeChecksum(String s){
		byte checksum = 0;
		for (char c : s.toCharArray()){
			checksum ^= (byte)c;			
		}
		return checksum;
	}
  private float parseNmeaFloat(String s){
    if (s!=null &&!s.equals("")){
      return Float.parseFloat(s);
    } else {
      return Float.NaN;
    }
  }
  private int parseNmeaInt(String s){
    if (s!=null &&!s.equals("")){
      return Integer.parseInt(s);
    } else {
      return 0;
    }
  }
}
