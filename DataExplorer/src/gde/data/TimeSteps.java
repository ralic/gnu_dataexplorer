/**************************************************************************************
  	This file is part of OpenSerialDataExplorer.

    OpenSerialDataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OpenSerialDataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with OpenSerialDataExplorer.  If not, see <http://www.gnu.org/licenses/>.
****************************************************************************************/
package osde.data;

import java.util.Vector;
import java.util.logging.Logger;

/**
 * TimeSteps class handles all the time steps of a record set or a record of part of compare set
 * @author Winfried Brügmann
 */
public class TimeSteps extends Vector<Integer> {
	final static String	$CLASS_NAME				= RecordSet.class.getName();
	final static long		serialVersionUID	= 26031957;
	final static Logger	log								= Logger.getLogger(RecordSet.class.getName());

	final boolean				isConstant;							// true if the time step is constant and the consumed time is a number of measurement points * timeStep_ms
	
	/**
	 * Constructs a new TimeSteps class, a give time step greater than 0 signals that the time step is constant between measurement points
	 * This class should hide time step calculations for constant or individual time steps of a device.
	 * @param newTimeStep_ms
	 */
	public TimeSteps(double newTimeStep_ms) {
		super(1, 1);
		if (this.isConstant = newTimeStep_ms > 0) 
			this.add(newTimeStep_ms);
	}
	
	/**
	 * copy constructor
	 */
	public TimeSteps(TimeSteps toBeClonedTimeSteps) {
  	super(toBeClonedTimeSteps);
  	this.isConstant = toBeClonedTimeSteps.isConstant;
	}
	
	public synchronized TimeSteps clone() {
  	return new TimeSteps(this);
  }

 	/**
	 * query the delta time in msec between two index positions
	 * @param indexStart
	 * @param indexEnd
	 * @return delta time in msec
	 */
	public double getDeltaTime(int indexStart, int indexEnd) {
		return (this.get(indexEnd) - this.get(indexStart)) / 10.0;
	}

	/**
	 * query time at an index position
	 * @param index
	 * @return
	 */
	public double getTime_ms(int index) {
		return this.isConstant ? (index == 0 ? 0.0 : this.get(0)/10.0*index) : (index < 0 ? this.firstElement() : index > elementCount ? this.lastElement()/10.0 : this.get(index)/10.0);
	}

	/**
	 * add a new time step
	 * @param value
	 * @return
	 */
	public synchronized boolean add(double value) {
		return super.add((int) (value * 10));
	}
	
	/**
	 * @return the const. time step in msec
	 */
	public double getTimeStep_ms() {
		return this.isConstant ? this.getTime_ms(1) : this.lastElement()/(elementCount-1);
	}

	/**
	 * @return the isConstant true if time step is a constant value between measurement points
	 */
	public boolean isConstant() {
		return isConstant;
	}
	
	/**
	 * @return the maximum time relative to the displayable area 
	 */
	public double getMaxTime_ms() {
		double maxTime = 0.0;
		if (isConstant) {
			maxTime = this.get(0)*elementCount/10.0;
		}
		else {
			maxTime = this.lastElement()/10.0;
		}
		//return isConstant ? this.get(0)*this.parent.realSize()/10.0 : this.lastElement()/10.0;
		return maxTime;
	}
	
	/**
	 * Find the indexes in this time vector where the given time value is placed
	 * In case of the given time in in between two available measurement points both bounding indexes are returned, 
	 * only in case where the given time matches an existing entry both indexes are equal.
	 * In cases where the returned indexes are not equal the related point x/y has to be interpolated.
	 * @param time_ms
	 * @return
	 */
	public int[] findBoundingIndexes(double time_ms) {
		//log.log(Level.INFO, "time_ms = " + time_ms);
		int index1 = 0, index2 = 0;
		if (isConstant) {
			double position = time_ms / (this.get(0)/10.0);
			index1 = (int) position;
			index2 = (position - index1) < 0.0001 ? index1 : index1 + 1;
		}
		else {
			int value = Double.valueOf(time_ms * 10.0).intValue();
			for (; index2 < elementCount; index2++) {
				if (value == get(index2)) {
					index1 = index2;
					break;
				}
				else if (value <= get(index2)) {
					index1 = index2 - 1;
					break;
				}
			}
		}
		//log.log(Level.INFO, index1 + " - " + index2);
		return new int[] {index1, index2};
	}

	/**
	 * find the index closest to given time in msec
	 * @param time_ms
	 * @return
	 */
	public int findBestIndex(double time_ms) {
		int index = 0;
		double position = 0.0;
		if (isConstant) {
			position = time_ms / (this.get(0)/10.0);
			index = (int) position;
			index = (position - index) > 0.5 ? index : index + 1;
			index = index <= elementCount-1 ? index : elementCount-1;
		}
		else {
			index = (int) (time_ms / this.lastElement() / elementCount / 10);
			int value = Double.valueOf(time_ms * 10.0).intValue();
			for (; index < elementCount; index++) {
				if (value <= this.get(index))
					break;
			}
			if (index+1 <= elementCount-1 && value > (this.get(index+1) + this.get(index))/2) 
				index = index + 1;
		}
			
		return index;
	}
}
