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
    
    Copyright (c) 2011 Winfried Bruegmann
****************************************************************************************/
package gde.utils;

import java.util.logging.Logger;

import gde.data.Record;
import gde.data.RecordSet;
import gde.device.IDevice;
import gde.device.MeasurementPropertyTypes;
import gde.device.PropertyType;
import gde.log.Level;

/**
 * helper class to calculate miscellaneous values based on GPS coordinates
 */
public class GPSHelper {
	private static Logger log = Logger.getLogger(GPSHelper.class.getName());

	/**
	 * calculate values of relative altitude, trip length, distance from start point, azimuth and climb/slope
	 * @param device
	 * @param recordSet
	 * @param recordOrdinalLatitude - input, will be checked for reasonable data
	 * @param recordOrdinalLongitude - input, will be checked for reasonable data
	 * @param recordOrdinalAltutude - input, might contain zero values, which result in zero values for relative altitude and climb/slope values
	 * @param recordOrdinalAltitudeRelative - output, depends on input altitude
	 * @param recordOrdinalClimb - output, depends on input altitude
	 * @param recordOrdinalTripLength - output, depends on input latitude, longitude and altitude
	 * @param recordOrdinalDistance - output, depends on input latitude, longitude and altitude
	 * @param recordOrdinalAzimuth - output, depends on input latitude, longitude (will be smoothed to make somehow interpretable)
	 * @param recordOrdinalDirectionStart - output, depends on input latitude, longitude 
	 */
	public static void calculateValues(IDevice device, RecordSet recordSet, int recordOrdinalLatitude, int recordOrdinalLongitude, int recordOrdinalAltutude, int recordOrdinalAltitudeRelative,
			int recordOrdinalClimb, int recordOrdinalTripLength, int recordOrdinalDistance, int recordOrdinalAzimuth, int recordOrdinalDirectionStart) {
		final double rad = Math.PI / 180;
		double lastTripLength = 0;
		int startAltitude = 0;

		try {
			//input records
			Record recordLatitude = recordSet.get(recordOrdinalLatitude);
			Record recordLongitude = recordSet.get(recordOrdinalLongitude);
			Record recordAlitude = recordSet.get(recordOrdinalAltutude);
			int recordSize = recordLatitude.realSize();

			if (recordSize >= 3 && recordLatitude.hasReasonableData() && recordLongitude.hasReasonableData()) {
				//output records
				Record recordAlitudeRelative = recordSet.get(recordOrdinalAltitudeRelative);
				Record recordClimb = recordSet.get(recordOrdinalClimb);
				Record recordTripLength = recordSet.get(recordOrdinalTripLength);
				Record recordDistance = recordSet.get(recordOrdinalDistance);
				Record recordAzimuth = recordSet.get(recordOrdinalAzimuth);
				Record recordDirection = recordSet.get(recordOrdinalDirectionStart);
				recordAlitudeRelative.clear();
				recordClimb.clear();
				recordTripLength.clear();
				recordDistance.clear();
				recordAzimuth.clear();
				recordDirection.clear();

				startAltitude = recordAlitude.get(0); //set initial altitude to enable absolute altitude calculation 
				int lastLongitude = recordLongitude.get(0);
				int startLongitude = recordLongitude.get(0);
				double phi_start_rad = device.translateValue(recordLatitude, recordLatitude.get(0) / 1000.0) * rad;
				double lambda_start = device.translateValue(recordLongitude, lastLongitude / 1000.0);

				double phi_A_rad = phi_start_rad;
				double lambda_A = lambda_start;
				double[] azimuths = new double[3];
				
				int indexMovement = 0;

				recordAlitudeRelative.add(0);
				recordTripLength.add(0);
				recordDistance.add(0);
				recordDirection.add(0);
				for (int i = 1; i < recordSize; ++i) {
					recordAlitudeRelative.add((recordAlitude.get(i) - startAltitude));

					double phi_B_rad = device.translateValue(recordLatitude, recordLatitude.get(i) / 1000.0) * rad;
					double lambda_B = device.translateValue(recordLongitude, recordLongitude.get(i) / 1000.0);

					double prod_start = (Math.sin(phi_start_rad) * Math.sin(phi_B_rad)) + (Math.cos(phi_start_rad) * Math.cos(phi_B_rad) * Math.cos((lambda_B - lambda_start) * rad));
					prod_start = prod_start > 1.0 ? 1.0 : prod_start < -1.0 ? -1.0 : prod_start;

					double zeta_start_rad = Math.acos(prod_start);
					zeta_start_rad = zeta_start_rad <= 0.0 ? 0.0 : zeta_start_rad >= Math.PI ? Math.PI : zeta_start_rad;
					double zeta_start = zeta_start_rad / rad;

					double prod = (Math.sin(phi_A_rad) * Math.sin(phi_B_rad)) + (Math.cos(phi_A_rad) * Math.cos(phi_B_rad) * Math.cos((lambda_B - lambda_A) * rad));
					prod = prod > 1.0 ? 1.0 : prod < -1.0 ? -1.0 : prod;

					double zeta_rad = Math.acos(prod);
					zeta_rad = zeta_rad <= 0.0 ? 0.0 : zeta_rad >= Math.PI ? Math.PI : zeta_rad;
					double zeta = zeta_rad / rad;

					double powDeltaHeight = Math.pow((recordAlitude.get(i - 1) - recordAlitude.get(i)) / 1000.0, 2);
					double powOrthodrome = Math.pow((zeta * (40041000.0 / 360.0)), 2);
					double deltaTrip = Math.sqrt(powOrthodrome + powDeltaHeight);
					recordTripLength.add((int) (lastTripLength + deltaTrip));//[km}];

					powDeltaHeight = Math.pow(recordAlitudeRelative.get(i) / 1000.0, 2);
					powOrthodrome = Math.pow(((zeta_start * 40041000 / 360)), 2);
					recordDistance.add((int) (Math.sqrt(powOrthodrome + powDeltaHeight) * 1000.0)); //[km}];

					double prod_alpha_start = zeta_start <= 0.0 ? -1.0 : zeta_start >= Math.PI ? Math.PI : (Math.sin(phi_B_rad) - (Math.sin(phi_start_rad) * Math.cos(zeta_start_rad)))
							/ (Math.cos(phi_start_rad) * Math.sin(zeta_start_rad));
					double alpha_start = Math.acos(prod_alpha_start < -1.0 ? -1.0 : prod_alpha_start > 1.0 ? 1.0 : prod_alpha_start) / rad;
					alpha_start = startLongitude > recordLongitude.get(i) ? 360.0 - alpha_start : alpha_start;
					recordDirection.add((int) (alpha_start * 1000.0));

					double prod_alpha = zeta_rad <= 0.0 ? -1.0 : zeta_rad >= Math.PI ? Math.PI : (Math.sin(phi_B_rad) - (Math.sin(phi_A_rad) * Math.cos(zeta_rad))) / (Math.cos(phi_A_rad) * Math.sin(zeta_rad));
					double alpha = Math.acos(prod_alpha < -1.0 ? -1.0 : prod_alpha > 1.0 ? 1.0 : prod_alpha) / rad;
					alpha = lastLongitude > recordLongitude.get(i) ? 360.0 - alpha : alpha;

					if (recordSize >= 30) {
						//slight smoothing
						if (i > 3) {
							azimuths[(i - 1) % 3] = alpha;
							recordAzimuth.add((int) ((azimuths[0] + azimuths[1] + azimuths[2]) * 333.333));
						}
						else {
							azimuths[(i - 1) % 3] = alpha;
							recordAzimuth.add((int) (alpha * 1000.0));
						}
					}
					else
						recordAzimuth.add((int) (alpha * 1000.0));

					//make more insensitive for azimuth dither around 0/360 
					int deltaLongitude = Math.abs(lastLongitude - recordLongitude.get(i));
					int deltaDistance = Math.abs(recordDistance.get(i - 1) - recordDistance.get(i));
					if (i != 1 && ((deltaLongitude <= 2 && deltaTrip < 0.5) || deltaDistance < 5)) {
						if (i > 3) {
							recordAzimuth.set(i - 1, ((recordAzimuth.get(i - 1) + recordAzimuth.get(i - 2) + recordAzimuth.get(i - 3)) / 3));
						}
						else
							recordAzimuth.set(i - 1, recordAzimuth.get(i - 1));
					}
					else
						lastLongitude = recordLongitude.get(i);

					if (log.isLoggable(Level.FINER))
						log.log(Level.FINER, String.format("deltaLongitude = %d; deltaDistance = %d; Kurswinkel = %7.3f; %7.3f", deltaLongitude, deltaDistance, alpha, (recordAzimuth.get(i - 1) / 1000.0)));

					phi_A_rad = phi_B_rad;
					lambda_A = lambda_B;

					lastTripLength = lastTripLength + deltaTrip;
					
					if (indexMovement == 0 && recordDistance.get(i) > 1500) 
						indexMovement = i;
				}
				recordAzimuth.add(recordAzimuth.getLast());
				
				int azimuth = recordAzimuth.get(indexMovement);
				int direction = recordDirection.get(indexMovement);
				for (int i = 0; i < indexMovement; i++) {
					recordAzimuth.set(i, azimuth);
					recordDirection.set(i, direction);
				}

				if (recordAlitude.hasReasonableData()) {
					recordClimb.setDisplayable(false);
					recordClimb.clear();
					PropertyType property = recordClimb.getProperty(MeasurementPropertyTypes.REGRESSION_INTERVAL_SEC.value());
					int regressionInterval = (int) (property != null ? Double.parseDouble(property.getValue().trim()) : 3);
					//this.calculationThread = new QuasiLinearRegression(recordSet, recordAlitude.getName(), recordClimb.getName(), regressionInterval);
					new LinearRegression(recordSet, recordAlitude.getName(), recordClimb.getName(), regressionInterval).start();
				}
			}
		}
		catch (RuntimeException e) {
			log.log(Level.WARNING, e.getMessage(), e);
		}
	}

}
