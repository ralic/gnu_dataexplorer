/**************************************************************************************
  	This file is part of GNU DataExplorer.

    GNU DataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    DataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with GNU DataExplorer.  If not, see <http://www.gnu.org/licenses/>.
    
    Copyright (c) 2017 Thomas Eickert
****************************************************************************************/

package gde.histoinventory;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.logging.Logger;

import gde.GDE;
import gde.log.Level;

/**
 * represents a latitude and longitude
 */
public class GpsCoordinate implements Comparable<GpsCoordinate> {
	private final static String	$CLASS_NAME		= GpsCoordinate.class.getName();
	private final static Logger	log						= Logger.getLogger($CLASS_NAME);

	public static double				EARTH_RADIUS	= 6371.000785;											// in km
	public static double				GPS_ACCURACY	= .01;															// 10 m

	private double							latitude;
	private double							longitude;
	private DecimalFormat				format				= new DecimalFormat("##.#######", new DecimalFormatSymbols(Locale.US));	// 7 digits  //$NON-NLS-1$

	public GpsCoordinate() {

	}

	/**
	 * @param latitude 
	 * @param longitude
	 */
	public GpsCoordinate(double latitude, double longitude) {
		this.latitude = latitude;
		this.longitude = longitude;
	}

	/**
	 * @param angularCoordinate is the trigonometric representation. e.g. P075221848P065891377P015957524P098718577 for { 48.782917, 9.182243 }
	 */
	public GpsCoordinate(String angularCoordinate) {
		StringBuilder sb = new StringBuilder(angularCoordinate.replace('P', '+').replace('M', '-')).insert(2, '.').insert(13, '.').insert(24, '.').insert(35, '.');
		this.latitude = Math.toDegrees(Math.atan2(Double.valueOf(sb.substring(0, 11)), Double.valueOf(sb.substring(11, 22))));
		this.longitude = Math.toDegrees(Math.atan2(Double.valueOf(sb.substring(22, 33)), Double.valueOf(sb.substring(33, 44))));
	}

	/**
	 * @return the trigonometric representation. e.g. P075221848P065891377P015957524P098718577 for { 48.782917, 9.182243 }
	 */
	public String toAngularCoordinate() {
		String angularCoordinate = String.format(Locale.US, "%+10.8f%+10.8f%+10.8f%+10.8f", Math.sin(Math.toRadians(this.latitude)), Math.cos(Math.toRadians(this.latitude)), //$NON-NLS-1$
				Math.sin(Math.toRadians(this.longitude)), Math.cos(Math.toRadians(this.longitude)));
		angularCoordinate = angularCoordinate.replace(".", "").replace('+', 'P').replace('-', 'M'); //$NON-NLS-1$ //$NON-NLS-2$
		if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, angularCoordinate, this);
		return angularCoordinate;
	}

	public double getLatitude() {
		return this.latitude;
	}

	public double getLongitude() {
		return this.longitude;
	}

	public String getLatitudeAsString() {
		return this.format.format(this.latitude);
	}

	public String getLongitudeAsString() {
		return this.format.format(this.longitude);
	}

	@Override
	public String toString() {
		return this.format.format(this.latitude) + GDE.STRING_COMMA + this.format.format(this.longitude); 
	}

	/**
	 * determine if one GPS coordinate is the same as another.
	 * the distance must be less than the GPS accuracy (currently 10 m).
	 * @param o the object to compare to
	 * @return true if the GPS coordinates do not differ more than the GPS accuracy, false if they are not
	 */
	@Override
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		}
		if (!(o instanceof GpsCoordinate)) {
			return false;
		}
		GpsCoordinate c = (GpsCoordinate) o;
		//		// compare exactly
		//		String me = this.getLatitudeAsString() + this.getLongitudeAsString();
		//		String you = c.getLatitudeAsString() + c.getLongitudeAsString();
		//		return me.equals(you);

		return getDistance(c) <= GPS_ACCURACY;
	}

	/**
	 * @return the hashcode from the angular representation
	 */
	@Override
	public int hashCode() {
		return 31 * toAngularCoordinate().hashCode();
	}

	/**
	 * @param yourCoordinate 
	 * @return as negative, 0, or positive if the this object is less than, equal to, or greater than yourCoordinate
	 */
	@Override
	public int compareTo(GpsCoordinate yourCoordinate) {
		if (this.equals(yourCoordinate))
			return 0;
		else
			return (this.toAngularCoordinate()).compareTo(yourCoordinate.toAngularCoordinate());
	}

	/**
	 * based on the haversine formula.
	 * particularly well-conditioned for numerical computation even at small distances.
	 * http://www.movable-type.co.uk/scripts/latlong.html 
	 * http://wikivisually.com/lang-de/wiki/Gro%C3%9Fkreis
	 * @param yourCoordinate
	 * @return the great circle distance in km
	 */
	public double getDistance(GpsCoordinate yourCoordinate) {
		double phi1 = Math.toRadians(this.latitude);
		double phi2 = Math.toRadians(yourCoordinate.getLatitude());
		double dPhi = Math.toRadians(yourCoordinate.getLatitude() - this.latitude);
		double dLambda = Math.toRadians(yourCoordinate.getLongitude() - this.longitude);

		double sqOppositeLeg = Math.sin(dPhi / 2.) * Math.sin(dPhi / 2.) //
				+ Math.cos(phi1) * Math.cos(phi2) * Math.sin(dLambda / 2.) * Math.sin(dLambda / 2.);
		double zeta = 2. * Math.atan2(Math.sqrt(sqOppositeLeg), Math.sqrt(1. - sqOppositeLeg));
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, String.format("distance=%f  %s  %s", EARTH_RADIUS * zeta, this, yourCoordinate)); //$NON-NLS-1$
		return EARTH_RADIUS * zeta;
	}

}
