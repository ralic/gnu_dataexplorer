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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import gde.config.Settings;
import gde.utils.GpsCoordinate;

/**
 * @author Thomas Eickert
 * takes a list of GPS coordinates and provides the clusters assigned to the locations.
 * uses the maximum cluster distance setting.
 */
public class GpsCluster extends ArrayList<GpsCoordinate> {
	private static final long								serialVersionUID	= 5239035277133565077L;
	private final static String							$CLASS_NAME				= GpsCluster.class.getName();
	private final static Logger							log								= Logger.getLogger($CLASS_NAME);

	private GpsCoordinate										reference;
	private Map<GpsCoordinate, GpsCluster>	assignedClusters	= new HashMap<>();

	/**
	 * @author Thomas Eickert
	 * performs great circle based distance clustering.
	 * uses the GPS cluster reference coordinate.
	 * provides both clustered and remaining GPS coordinates lists.
	 */
	class DistanceProcessor implements Consumer<GpsCoordinate> {
		private final Settings									settings						= Settings.getInstance();

		private GpsCluster											clusteredItems			= new GpsCluster();
		private Map<GpsCoordinate, GpsCluster>	assignedClusters		= new HashMap<>();
		private GpsCluster											relicts							= new GpsCluster();
		private List<Double>										relictSqDistances		= new ArrayList<>();
		private double													relictSqDistanceSum	= 0.;

		public void accept(GpsCoordinate newGpsCoordinate) {
			if (GpsCluster.this.getReference() == newGpsCoordinate) { // == due to equals override
				// a distance to myself is not useful
				log.log(Level.FINEST, "add myself to the empty clusteredItems list", this.clusteredItems.size()); //$NON-NLS-1$
				this.clusteredItems.add(newGpsCoordinate);
				this.assignedClusters.put(newGpsCoordinate, this.clusteredItems);
			}
			else {
				final double distance = GpsCluster.this.getReference().getDistance(newGpsCoordinate);
				if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, String.format("ClusterRadius=%f  distance=%f  to %s", this.settings.getGpsLocationRadius(), distance, newGpsCoordinate)); //$NON-NLS-1$
				if (distance <= this.settings.getGpsLocationRadius()) {
					this.clusteredItems.add(newGpsCoordinate);
					this.assignedClusters.put(newGpsCoordinate, this.clusteredItems);
				}
				else {
					this.relicts.add(newGpsCoordinate);
					this.relictSqDistances.add(distance * distance);
					this.relictSqDistanceSum += distance * distance;
				}
			}
		}

		public void combine(DistanceProcessor other) {
			this.clusteredItems.addAll(other.clusteredItems);
			this.assignedClusters.putAll(other.assignedClusters);
			this.relicts.addAll(other.relicts);
			this.relictSqDistances.addAll(other.relictSqDistances);
			this.relictSqDistanceSum += other.relictSqDistanceSum;
		}

		/**
		 * @return the new random reference point for the next clustering step or null if the residuum does not contain elements
		 */
		public GpsCoordinate getResiduumReference() {
			GpsCoordinate residuumReference = null;
			// select an arbitrary GPS coordinate with probability Square(Distance) 
			final double arbitrarySqDistance = Math.random() * this.relictSqDistanceSum;
			double cumulativeSqDistance = 0.;
			for (int i = 0; i < this.relictSqDistances.size(); i++) {
				cumulativeSqDistance += this.relictSqDistances.get(i);
				if (arbitrarySqDistance <= cumulativeSqDistance) {
					residuumReference = this.relicts.get(i);
					break;
				}
			}
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("relictsSize=%d  new reference=%s", this.relicts.size(), residuumReference)); //$NON-NLS-1$
			return residuumReference;
		}

		/**
		 * @return the items which have not been assigned to the new cluster
		 */
		public GpsCluster getResiduumItems() {
			return this.relicts;
		}

		/**
		 * @return the clusteredItems
		 */
		public GpsCluster getClusteredItems() {
			return this.clusteredItems;
		}

		/**
		 * @return the list of assigned GPS coordinates and the cluster which each individual is assigned to
		 */
		public Map<GpsCoordinate, GpsCluster> getAssignedClusters() {
			return this.assignedClusters;
		}
	}

	public GpsCluster() {
	}

	/**
	 * calculate the unweighted average of the GPS coordinates in this cluster.
	 * the algorithm averages the cartesian vectors which supports the lat / lon boundaries correctly.
	 * @return the average of all GPS coordinates
	 */
	public GpsCoordinate getCenter() {
		double xAvg = 0, yAvg = 0, zAvg = 0;
		for (int i = 0; i < this.size(); i++) {
			GpsCoordinate gpsCoordinate = this.get(i);
			double phi = Math.toRadians(gpsCoordinate.getLatitude());
			double lambda = Math.toRadians(gpsCoordinate.getLongitude());
			xAvg += (Math.cos(phi) * Math.cos(lambda) - xAvg) / (i + 1);
			yAvg += (Math.cos(phi) * Math.sin(lambda) - yAvg) / (i + 1);
			zAvg += (Math.sin(phi) - zAvg) / (i + 1);
			log.log(Level.FINER, "coordinate=", gpsCoordinate); //$NON-NLS-1$
		}
		final GpsCoordinate result;
		if (Math.abs(xAvg) < 1.e-11 && Math.abs(yAvg) < 1.e-11 && Math.abs(zAvg) < 1.e-11)
			result = new GpsCoordinate(51.477778, 0.); // center of the earth replaced by Greenwich
		else {
			double hyp = Math.sqrt(xAvg * xAvg + yAvg * yAvg);
			result = new GpsCoordinate(Math.toDegrees(Math.atan2(zAvg, hyp)), Math.toDegrees(Math.atan2(yAvg, xAvg)));
		}
		log.log(Level.FINER, "result=", result); //$NON-NLS-1$
		return result;
	}

	/**
	 * calculate the unweighted average of the GPS coordinates of all clusters.
	 * the algorithm averages the cartesian vectors which supports the lat / lon boundaries correctly.
	 * @return the average of all clusters.
	 */
	public GpsCoordinate getClustersCenter() {
		double xAvg = 0, yAvg = 0, zAvg = 0;
		List<GpsCluster> clusters = getClusters();
		for (int i = 0; i < clusters.size(); i++) {
			GpsCoordinate gpsCoordinate = clusters.get(i).getCenter();
			double phi = Math.toRadians(gpsCoordinate.getLatitude());
			double lambda = Math.toRadians(gpsCoordinate.getLongitude());
			xAvg += (Math.cos(phi) * Math.cos(lambda) - xAvg) / (i + 1);
			yAvg += (Math.cos(phi) * Math.sin(lambda) - yAvg) / (i + 1);
			zAvg += (Math.sin(phi) - zAvg) / (i + 1);
			log.log(Level.FINER, "coordinate=", gpsCoordinate); //$NON-NLS-1$
		}
		final GpsCoordinate result;
		if (Math.abs(xAvg) < 1.e-11 && Math.abs(yAvg) < 1.e-11 && Math.abs(zAvg) < 1.e-11)
			result = new GpsCoordinate(51.477778, 0.); // center of the earth replaced by Greenwich
		else {
			double hyp = Math.sqrt(xAvg * xAvg + yAvg * yAvg);
			result = new GpsCoordinate(Math.toDegrees(Math.atan2(zAvg, hyp)), Math.toDegrees(Math.atan2(yAvg, xAvg)));
		}
		log.log(Level.FINER, "result=", result); //$NON-NLS-1$
		return result;
	}

	/**
	 * determine clusters from the GPS coordinates collected up to now.
	 */
	public void setClusters() {
		this.assignedClusters.clear();
		GpsCluster wip = new GpsCluster(); // use work in process object in order not to remove items from this GpsCluster
		for (GpsCoordinate cc : this) {
			if (cc != null) wip.add(cc);
		}
		if (wip.size() > 0) {
			this.reference = wip.get(wip.size() / 2); // 'random' start location
			while (wip.size() > 0) {
				// start analyzing the GPS coordinate list data 
				DistanceProcessor distanceProcessor = wip.parallelStream().collect(DistanceProcessor::new, DistanceProcessor::accept, DistanceProcessor::combine);
				// add the newly assigned coordinates including their cluster assignment to the total assignment list
				this.assignedClusters.putAll(distanceProcessor.getAssignedClusters());
				// take the remaining GPS coordinate list for the next iteration and take also the optimized reference coordinate
				wip = distanceProcessor.getResiduumItems();
				this.setReference(distanceProcessor.getResiduumReference()); // the distance processor accesses this reference in the next iteration step
				if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "number of clusters : ", this.getClusters().size() + "  new Cluster members : " + this.assignedClusters.size()); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	}

	/**
	 * @return the reference GPS coordinate used for the last cluster operation (should be null)
	 */
	public GpsCoordinate getReference() {
		return this.reference;
	}

	/**
	 * @param reference the reference to set
	 */
	private void setReference(GpsCoordinate reference) {
		this.reference = reference;
	}

	/**
	 * @return the full list of coordinates and the cluster which each individual is assigned to
	 */
	public Map<GpsCoordinate, GpsCluster> getAssignedClusters() {
		return this.assignedClusters;
	}

	/**
	 * @return the list of all clusters identified
	 */
	public List<GpsCluster> getClusters() {
		return this.assignedClusters.values().parallelStream().distinct().collect(Collectors.toList());
	}

}
