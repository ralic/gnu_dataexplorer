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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;

import osde.OSDE;
import osde.device.DataTypes;
import osde.device.IDevice;
import osde.device.ObjectFactory;
import osde.device.PropertyType;
import osde.device.StatisticsType;
import osde.ui.OpenSerialDataExplorer;
import osde.ui.SWTResourceManager;
import osde.utils.StringHelper;
import osde.utils.TimeLine;

/**
 * @author Winfried Brügmann
 * class record holds data points of one measurement or line or curve
 */
public class Record extends Vector<Integer> {
	final static String	$CLASS_NAME 					= Record.class.getName();
	final static long		serialVersionUID			= 26031957;
	final static Logger	log										= Logger.getLogger(Record.class.getName());
	
	public static final String DELIMITER 			= "|-|"; 		//$NON-NLS-1$
	public static final String END_MARKER 		= "|:-:|"; 	//$NON-NLS-1$

	// this variables are used to make a record selfcontained within compare set
	String							channelConfigKey; 								// used as channelConfigKey
	String							keyName;
	double							timeStep_ms						= 0;				// time base of measurement points
	IDevice							device;
	final int						ordinal;	// ordinal is referencing the source position of the record relative to the initial 
																// device measurement configuration and used to find specific properties

	RecordSet						parent;
	String							name;															// measurement name <Höhe>
	String							unit;															// unit <m>
	String							symbol;														// yymbol <h>
	boolean							isActive;
	boolean							isDisplayable;
	boolean							isVisible							= true;
	StatisticsType			statistics						= null;
	Boolean							triggerIsGreater			= null;
	Integer							triggerLevel					= null;
	Integer							minTriggerTimeSec 		= null;
	class TriggerRange {
		int in  = -1;
		int out = -1;
		public TriggerRange(int newIn, int newOut) { this.in = newIn; this.out = newOut; }
		public TriggerRange(int newIn) { this.in = newIn; }
		/**
		 * @return the in value
		 */
		public int getIn() {
			return this.in;
		}
		/**
		 * @param newIn the in value to set
		 */
		public void setIn(int newIn) {
			this.in = newIn;
		}
		/**
		 * @return the out value
		 */
		public int getOut() {
			return this.out;
		}
		/**
		 * @param newOut the out value to set
		 */
		public void setOut(int newOut) {
			this.out = newOut;
		}
		/**
		 * query if bothe values has been set
		 */
		public boolean isComplete() {
			return this.in >= 0 && this.out > 0 ? true : false;
		}
	}
	TriggerRange				tmpTriggerRange				= null;
	Vector<TriggerRange> triggerRanges				= null;
	List<PropertyType>	properties						= new ArrayList<PropertyType>();	// offset, factor, reduction, ...
	boolean							isPositionLeft				= true;
	Color								color									= OpenSerialDataExplorer.COLOR_BLACK;
	int									lineWidth							= 1;
	int									lineStyle							= new Integer(SWT.LINE_SOLID);
	boolean							isRoundOut						= false;
	boolean							isStartpointZero			= false;
	boolean							isStartEndDefined			= false;
	boolean							isScaleSynced					= false; // indicates if record is part of syncable records and scale sync is requested
	boolean							isSyncPlaceholder			= false;
	DecimalFormat				df;
	int									numberFormat					= 1;													// 0 = 0000, 1 = 000.0, 2 = 00.00
	int									maxValue							= 0;		 										  // max value of the curve
	int									minValue							= 0;													// min value of the curve
	double							maxScaleValue					= this.maxValue;							// overwrite calculated boundaries
	double							minScaleValue					= this.minValue;

	// scope view 
	int									scopeMin							= 0;													// min value of the curve within scope display area
	int									scopeMax							= 0;													// max value of the curve within scope display area

	// statistics trigger
	int									maxValueTriggered			= Integer.MIN_VALUE;		 			// max value of the curve, according a set trigger level if any
	int									minValueTriggered			= Integer.MAX_VALUE;					// min value of the curve, according a set trigger level if any
	int									avgValue							= Integer.MIN_VALUE;		 			// avarage value (avg = sum(xi)/n)
	int									sigmaValue						= Integer.MIN_VALUE;		 			// sigma value of data, according a set trigger level if any
	int									avgValueTriggered			= Integer.MIN_VALUE;		 			// avarage value (avg = sum(xi)/n)
	int									sigmaValueTriggered		= Integer.MIN_VALUE;		 			// sigma value of data, according a set trigger level if any
	
	int 								drawLimit							= Integer.MAX_VALUE;		// above this limit the record will not be drawn (compareSet with different records) 
	int									zoomOffset			= 0;													// number of measurements point until zoom area begins
	int									zoomSize				= 0;													// number of measurements point of the zoom area width
	double							maxZoomScaleValue			= this.maxScaleValue;
	double							minZoomScaleValue			= this.minScaleValue;
	int									numberScaleTicks			= 0;

	// display the record
	double							displayScaleFactorTime;
	double							displayScaleFactorValue;
	double							minDisplayValue;									// min value in device units, correspond to draw area
	double							maxDisplayValue;									// max value in device units, correspond to draw area

	// measurement
	boolean							isMeasurementMode				= false;
	boolean							isDeltaMeasurementMode	= false;
		
	public final static String	NAME									= "_name";							// active means this measurement can be red from device, other wise its calculated //$NON-NLS-1$
	public final static String	UNIT									= "_unit";							// active means this measurement can be red from device, other wise its calculated //$NON-NLS-1$
	public final static String	SYMBOL								= "_symbol";						// active means this measurement can be red from device, other wise its calculated //$NON-NLS-1$
	public final static String	IS_ACTIVE							= "_isActive";					// active means this measurement can be red from device, other wise its calculated //$NON-NLS-1$
	public final static String	IS_DIPLAYABLE					= "_isDisplayable";			// true for all active records, true for passive records when data calculated //$NON-NLS-1$
	public final static String	IS_VISIBLE						= "_isVisible";					// defines if data are displayed  //$NON-NLS-1$
	public final static String	IS_POSITION_LEFT			= "_isPositionLeft";		// defines the side where the axis id displayed  //$NON-NLS-1$
	public final static String	COLOR									= "_color";							// defines which color is used to draw the curve //$NON-NLS-1$
	public final static String	LINE_WITH							= "_lineWidth"; 				//$NON-NLS-1$
	public final static String	LINE_STYLE						= "_lineStyle"; 				//$NON-NLS-1$
	public final static String	IS_ROUND_OUT					= "_isRoundOut";				// defines if axis values are rounded //$NON-NLS-1$
	public final static String	IS_START_POINT_ZERO		= "_isStartpointZero";	// defines if axis value starts at zero //$NON-NLS-1$
	public final static String	IS_START_END_DEFINED	= "_isStartEndDefined";	// defines that explicit end values are defined for axis //$NON-NLS-1$
	public final static String	NUMBER_FORMAT					= "_numberFormat"; 			//$NON-NLS-1$
	public final static String	MAX_VALUE							= "_maxValue"; 					//$NON-NLS-1$
	public final static String	DEFINED_MAX_VALUE			= "_defMaxValue";				// overwritten max value //$NON-NLS-1$
	public final static String	MIN_VALUE							= "_minValue"; 					//$NON-NLS-1$
	public final static String	DEFINED_MIN_VALUE			= "_defMinValue";				// overwritten min value //$NON-NLS-1$
	
	public final static String[] propertyKeys = new String[] { NAME, UNIT, SYMBOL, IS_ACTIVE, IS_DIPLAYABLE, IS_VISIBLE, IS_POSITION_LEFT, COLOR, LINE_WITH, LINE_STYLE, 
			IS_ROUND_OUT, IS_START_POINT_ZERO, IS_START_END_DEFINED, NUMBER_FORMAT, MAX_VALUE, DEFINED_MAX_VALUE, MIN_VALUE, DEFINED_MIN_VALUE	};


	/**
	 * this constructor will create an vector to hold data points in case the initial capacity is > 0
	 * @param newDevice
	 * @param newOrdinal
	 * @param newName
	 * @param newUnit
	 * @param newSymbol
	 * @param isActiveValue
	 * @param newStatistic
	 * @param newProperties (offset, factor, color, lineType, ...)
	 * @param initialCapacity
	 */
	public Record(IDevice newDevice, int newOrdinal, String newName, String newSymbol, String newUnit, boolean isActiveValue, StatisticsType newStatistic, List<PropertyType> newProperties, int initialCapacity) {
		super(initialCapacity);
		log.log(Level.FINE, newName + " Record(IDevice, int, String, String, String, boolean, StatisticsType, List<PropertyType>, int)"); //$NON-NLS-1$
		this.device = newDevice;
		this.ordinal = newOrdinal;
		this.name = newName;
		this.symbol = newSymbol;
		this.unit = newUnit;
		this.isActive = isActiveValue;
		this.isDisplayable = isActiveValue ? true : false;
		this.statistics = newStatistic;
		this.triggerIsGreater = (newStatistic != null && newStatistic.getTrigger() != null) ? newStatistic.getTrigger().isGreater() : null;
		this.triggerLevel = (newStatistic != null && newStatistic.getTrigger() != null) ? newStatistic.getTrigger().getLevel() : null;
		this.minTriggerTimeSec = (newStatistic != null && newStatistic.getTrigger() != null) ? newStatistic.getTrigger().getMinTimeSec() : null;
		this.initializeProperties(this, newProperties);
		this.df = new DecimalFormat("0.0"); //$NON-NLS-1$
		this.numberFormat = 1;
		
		// special keys for compare set record are handled with put method
		//this.channelConfigKey;
		//this.keyName;
	}

	/**
	 * copy constructor
	 */
	private Record(Record record) {
		super(record);
		log.log(Level.FINE, record.name + " Record(Record)"); //$NON-NLS-1$
		this.ordinal = record.ordinal;
		this.name = record.name;
		this.symbol = record.symbol;
		this.unit = record.unit;
		this.timeStep_ms = record.timeStep_ms;
		this.isActive = record.isActive;
		this.isDisplayable = record.isDisplayable;
		this.statistics = record.statistics;
		this.triggerIsGreater = record.triggerIsGreater;
		this.triggerLevel = record.triggerLevel;
		this.minTriggerTimeSec = record.minTriggerTimeSec;
		this.initializeProperties(record, record.properties);
		this.maxValue = record.maxValue;
		this.minValue = record.minValue;
		this.df = (DecimalFormat) record.df.clone();
		this.numberFormat = record.numberFormat;
		this.isVisible = record.isVisible;
		this.isPositionLeft = record.isPositionLeft;
		this.color = new Color(record.color.getDevice(), record.color.getRGB());
		this.lineWidth = record.lineWidth;
		this.lineStyle = record.lineStyle;
		this.isRoundOut = record.isRoundOut;
		this.isStartpointZero = record.isStartpointZero;
		this.isStartEndDefined = record.isStartEndDefined;
		this.maxScaleValue = record.maxScaleValue;
		this.minScaleValue = record.minScaleValue;
		this.zoomOffset = 0;
		this.zoomSize = record.realSize();
		// handle special keys for compare set record
		this.channelConfigKey = record.channelConfigKey;
		this.keyName = record.keyName;
		this.timeStep_ms = record.timeStep_ms;
		this.device = record.device; // reference to device	
	}

	/**
	 * overwritten clone method used to compare curves
	 */
	public Record clone() {
		return new Record(this);
	}

	/**
	 * clone method used to move records to other configuration, where measurement signature does not match the source
	 */
	public Record clone(String newName) {
		Record newRecord = new Record(this);
		newRecord.name = newName;
		return newRecord;
	}

	/**
	 * copy constructor
	 */
	private Record(Record record, int dataIndex, boolean isFromBegin) {
		//super(record); // vector
		log.log(Level.FINE, record.name + " Record(Record, int, boolean)"); //$NON-NLS-1$
		this.parent = record.parent;
		this.parent.setZoomMode(false);
		this.ordinal = record.ordinal;
		this.name = record.name;
		this.symbol = record.symbol;
		this.unit = record.unit;
		this.timeStep_ms = record.timeStep_ms;
		this.isActive = record.isActive;
		this.isDisplayable = record.isDisplayable;
		this.statistics = record.statistics;
		this.triggerIsGreater = record.triggerIsGreater;
		this.triggerLevel = record.triggerLevel;
		this.minTriggerTimeSec = record.minTriggerTimeSec;
		this.initializeProperties(record, record.properties);
		this.maxValue = 0;
		this.minValue = 0;
		this.clear();
		this.trimToSize();
		if (isFromBegin) {
			for (int i = dataIndex; i < record.realSize(); i++) {
				this.add(record.get(i).intValue());
			}
		}
		else {
			for (int i = 0; i < dataIndex; i++) {
				this.add(record.get(i).intValue());
			}
		}
		
		this.df = (DecimalFormat) record.df.clone();
		this.numberFormat = record.numberFormat;
		this.isVisible = record.isVisible;
		this.isPositionLeft = record.isPositionLeft;
		this.color = new Color(record.color.getDevice(), record.color.getRGB());
		this.lineWidth = record.lineWidth;
		this.lineStyle = record.lineStyle;
		this.isRoundOut = record.isRoundOut;
		this.isStartpointZero = record.isStartpointZero;
		this.maxScaleValue = this.maxValue;
		this.minScaleValue = this.minValue;
		this.zoomOffset = 0;
		this.zoomSize = record.realSize();
		// handle special keys for compare set record
		this.channelConfigKey = record.channelConfigKey;
		this.keyName = record.keyName;
		this.device = record.device; // reference to device
	}

	/**
	 * clone method re-writes data points of all records of this record set
	 * - if isFromBegin == true, the given index is the index where the record starts after this operation
	 * - if isFromBegin == false, the given index represents the last data point index of the records.
	 * @param dataIndex
	 * @param isFromBegin
	 * @return new dreated record
	 */
	public Record clone(int dataIndex, boolean isFromBegin) {
		return new Record(this, dataIndex, isFromBegin);
	}

	/**
	 * initialize properties, at least all record will have as default a factor, an offset and a reduction property
	 * @param recordRef
	 * @param newProperties
	 */
	private void initializeProperties(Record recordRef, List<PropertyType> newProperties) {
		this.properties = this.properties != null ? this.properties : new ArrayList<PropertyType>();	// offset, factor, reduction, ...
		for (PropertyType property : newProperties) {
			log.log(Level.FINER, recordRef.name + " - " + property.getName() + " = " + property.getValue()); //$NON-NLS-1$ //$NON-NLS-2$
			this.properties.add(property.clone());
		}
		// initialize factor, offset, reduction if not exist only
		//if (!this.properties.contains(IDevice.FACTOR)) this.properties.add(this.createProperty(IDevice.FACTOR, DataTypes.DOUBLE, recordRef.getFactor())); 
		//if (!this.properties.contains(IDevice.OFFSET)) this.properties.add(this.createProperty(IDevice.OFFSET, DataTypes.DOUBLE, recordRef.getOffset()));
		//if (!this.properties.contains(IDevice.REDUCTION)) this.properties.add(this.createProperty(IDevice.REDUCTION, DataTypes.DOUBLE, recordRef.getReduction()));
	}

	/**
	 * Method to initialize color and scale position defaults
	 * @param recordOrdinal
	 */
	void setColorDefaultsAndPosition(int recordOrdinal) {
		// set color defaults
		switch (recordOrdinal) {
		case 0: // erste Kurve
			this.setColor(SWTResourceManager.getColor(0, 0, 255)); //(SWT.COLOR_BLUE));
			break;
		case 1: // zweite Kurve
			this.setColor(SWTResourceManager.getColor(0, 128, 0)); //SWT.COLOR_DARK_GREEN));
			break;
		case 2: // dritte Kurve
			this.setColor(SWTResourceManager.getColor(128, 0, 0)); //(SWT.COLOR_DARK_RED));
			break;
		case 3: // vierte Kurve
			this.setColor(SWTResourceManager.getColor(255, 0, 255)); //(SWT.COLOR_MAGENTA));
			break;
		case 4: // fünfte Kurve
			this.setColor(SWTResourceManager.getColor(64, 0, 64)); //(SWT.COLOR_CYAN));
			break;
		case 5: // erste Kurve
			this.setColor(SWTResourceManager.getColor(0, 128, 128)); //(SWT.COLOR_DARK_YELLOW));
			break;
		case 6: // erste Kurve
			this.setColor(SWTResourceManager.getColor(128, 128, 0));
			break;
		case 7: // erste Kurve
			this.setColor(SWTResourceManager.getColor(128, 0, 128));
			break;
		case 8: // erste Kurve
			this.setColor(SWTResourceManager.getColor(0, 128, 255));
			break;
		case 9: // erste Kurve
			this.setColor(SWTResourceManager.getColor(128, 255, 0));
			break;
		case 10: // erste Kurve
			this.setColor(SWTResourceManager.getColor(255, 0, 128));
			break;
		case 11: // erste Kurve
			this.setColor(SWTResourceManager.getColor(0, 64, 128));
			break;
		case 12: // erste Kurve
			this.setColor(SWTResourceManager.getColor(64, 128, 0));
			break;
		case 13: // erste Kurve
			this.setColor(SWTResourceManager.getColor(128, 0, 64));
			break;
		case 14: // erste Kurve
			this.setColor(SWTResourceManager.getColor(128, 64, 0));
			break;
		case 15: // erste Kurve
			this.setColor(SWTResourceManager.getColor(0, 128, 64));
			break;
		default:
			this.setColor(SWTResourceManager.getColor(128, 255, 128)); //(SWT.COLOR_GREEN));
			break;
		}
		// set position defaults
		if (recordOrdinal % 2 == 0) {
			this.setPositionLeft(true); //position left
		}
		else {
			this.setPositionLeft(false); // position right
		}
	}
	
	/**
	 * add a data point to the record data, checks for minimum and maximum to define display range
	 * @param point
	 */
	public boolean add(int point) {
		final String $METHOD_NAME = "add"; //$NON-NLS-1$
		if (super.size() == 0) {
			this.minValue = this.maxValue = point;
		}
		else {
			if 			(point > this.maxValue) this.maxValue = point;
			else if (point < this.minValue) this.minValue = point;
		}	
		log.logp(Level.FINER, $CLASS_NAME, $METHOD_NAME, "adding point = " + point); //$NON-NLS-1$
		log.logp(Level.FINEST, $CLASS_NAME, $METHOD_NAME, this.name + " minValue = " + this.minValue + " maxValue = " + this.maxValue); //$NON-NLS-1$ //$NON-NLS-2$
		return this.add(new Integer(point)); // Vector add method
	}

	public int getOrdinal() {
		return this.ordinal;
	}
	
	public String getName() {
		return this.name;
	}

	public void setName(String newName) {
		if (!this.name.equals(newName)) {
			this.parent.replaceRecordName(this.name, newName);
			this.name = newName;
		}
	}
	
	public String getUnit() {
		return this.unit;
	}

	public void setUnit(String newUnit) {
		this.unit = newUnit;
	}

	public String getSymbol() {
		return this.symbol;
	}

	public void setSymbol(String newSymbol) {
		this.symbol = newSymbol;
	}
	
	/**
	 * get a reference to the record properies (offset, factor, ...)
	 * @return list containing the properties
	 */
	List<PropertyType> getProperties() {
		return this.properties;
	}

	/**
	 * replace the properties to enable channel/configuration switch
	 * @param newProperties
	 */
	public void setProperties(List<PropertyType> newProperties) {
		this.properties = new ArrayList<PropertyType>();
		for (PropertyType property : newProperties) {
			this.properties.add(property.clone());
		}
	}
	
	/**
	 * get property reference using given property type key (IDevice.OFFSET, ...)
	 * @param propertyKey
	 * @return PropertyType
	 */
	public PropertyType getProperty(String propertyKey) {
		PropertyType property = null;
		for (PropertyType propertyType : this.properties) {
			if(propertyType.getName().equals(propertyKey)) {
				property = propertyType;
				break;
			}
		}
		return property;
	}
	
	/**
	 * create a property and return the reference
	 * @param propertyKey
	 * @param type
	 * @return created property with associated propertyKey
	 */
	public PropertyType createProperty(String propertyKey, DataTypes type, Object value) {
		ObjectFactory factory = new ObjectFactory();
		PropertyType newProperty = factory.createPropertyType();
		newProperty.setName(propertyKey);
		newProperty.setType(type);
		newProperty.setValue(OSDE.STRING_EMPTY + value);
		return newProperty;
	}

	public double getFactor() {
		double value = 1.0;
		PropertyType property = this.getProperty(IDevice.FACTOR);
		if (property != null)
			value = new Double(property.getValue()).doubleValue();
		else
			try {
				value = this.getDevice().getMeasurementFactor(this.getChannelConfigKey(), this.ordinal);
			}
			catch (RuntimeException e) {
				//log.log(Level.WARNING, this.name + " use default value for property " + IDevice.FACTOR); // log warning and use default value
			}
		return value;
	}

	public void setFactor(double newValue) {
		PropertyType property = this.getProperty(IDevice.FACTOR);
		if (property != null)
			property.setValue(String.format("%.4f", newValue)); //$NON-NLS-1$
		else
			this.properties.add(this.createProperty(IDevice.FACTOR, DataTypes.DOUBLE, String.format(Locale.ENGLISH, "%.4f", newValue))); //$NON-NLS-1$
	}

	public double getOffset() {
		double value = 0.0;
		PropertyType property = this.getProperty(IDevice.OFFSET);
		if (property != null)
			value = new Double(property.getValue()).doubleValue();
		else
			try {
				value = this.getDevice().getMeasurementOffset(this.getChannelConfigKey(), this.ordinal);
			}
			catch (RuntimeException e) {
				//log.log(Level.WARNING, this.name + " use default value for property " + IDevice.OFFSET); // log warning and use default value
			}
		return value;
	}
	
	public void setOffset(double newValue) {
		PropertyType property = this.getProperty(IDevice.OFFSET);
		if (property != null)
			property.setValue(String.format("%.4f", newValue)); //$NON-NLS-1$
		else
			this.properties.add(this.createProperty(IDevice.OFFSET, DataTypes.DOUBLE, String.format(Locale.ENGLISH, "%.4f", newValue))); //$NON-NLS-1$
	}

	public double getReduction() {
		double value = 0.0;
		PropertyType property = this.getProperty(IDevice.REDUCTION);
		if (property != null)
			value = new Double(property.getValue()).doubleValue();
		else {
			try {
				String strValue = (String)this.getDevice().getMeasurementPropertyValue(this.getChannelConfigKey(), this.ordinal, IDevice.REDUCTION);
				if (strValue != null && strValue.length() > 0) value = new Double(strValue.trim().replace(',', '.')).doubleValue();
			}
			catch (RuntimeException e) {
				//log.log(Level.WARNING, this.name + " use default value for property " + IDevice.REDUCTION); // log warning and use default value
			}
		}
		return value;
	}
	
	public void setReduction(double newValue) {
		PropertyType property = this.getProperty(IDevice.REDUCTION);
		if (property != null)
			property.setValue(String.format("%.4f", newValue)); //$NON-NLS-1$
		else
			this.properties.add(this.createProperty(IDevice.REDUCTION, DataTypes.DOUBLE, String.format(Locale.ENGLISH, "%.4f", newValue))); //$NON-NLS-1$
	}

	public boolean isVisible() {
		return this.isVisible;
	}

	public void setVisible(boolean enabled) {
		this.isVisible = enabled;
	}

	public int getMaxValue() {
		return this.parent.isScopeMode ? this.scopeMax : 
				this.maxValue == this.minValue ? this.maxValue + 100 : 
					this.maxValue;
	}

	public int getMinValue() {
		return this.parent.isScopeMode ? this.scopeMin : 
				this.minValue == this.maxValue ? this.minValue - 100 : 
					this.minValue;
	}

	public int getRealMaxValue() {
		return this.maxValue;
	}

	public int getRealMinValue() {
		return this.minValue;
	}

	public int getMaxValueTriggered() {
		if (this.tmpTriggerRange == null) this.evaluateMinMax();
			return this.maxValueTriggered;
	}

	public int getMinValueTriggered() {
		if (this.tmpTriggerRange == null) this.evaluateMinMax();
			return this.minValueTriggered;
	}

	/**
	 * evaluate min and max value within range according trigger configuration
	 * while building vector of trigger range definitions as pre-requisite of avg and sigma calculation
	 */
	@SuppressWarnings("unchecked") // clone triggerRanges to be able to modify by time filter
	synchronized void evaluateMinMax() {
		if (this.triggerRanges == null && this.isDisplayable && this.triggerIsGreater != null && this.triggerLevel != null) {
			int deviceTriggerlevel = new Double(this.device.reverseTranslateValue(this, this.triggerLevel / 1000.0) * 1000).intValue();
			for (int i = 0; i < this.realSize(); ++i) {
				int point = this.realGet(i);
				if (this.triggerIsGreater) { // point value must above trigger level
					if (point > deviceTriggerlevel) {
						if (this.tmpTriggerRange == null) {
							if (this.triggerRanges == null) {
								this.triggerRanges = new Vector<TriggerRange>();
								this.minValueTriggered = this.maxValueTriggered = point;
							}
							this.tmpTriggerRange = new TriggerRange(i);
						}
						else {
							if (point > this.maxValueTriggered) this.maxValueTriggered = point;
							if (point < this.minValueTriggered) this.minValueTriggered = point;
						}
					}
					else {
						if (this.triggerRanges != null && this.tmpTriggerRange != null) {
							this.tmpTriggerRange.setOut(i);
							this.triggerRanges.add(this.tmpTriggerRange);
							this.tmpTriggerRange = null;
						}
					}
				}
				else { // point value must below trigger level
					if (point < deviceTriggerlevel) {
						if (this.tmpTriggerRange == null) {
							if (this.triggerRanges == null) {
								this.triggerRanges = new Vector<TriggerRange>();
								this.minValueTriggered = this.maxValueTriggered = point;
							}
							this.tmpTriggerRange = new TriggerRange(i);
						}
						else {
							if (point > this.maxValueTriggered) this.maxValueTriggered = point;
							if (point < this.minValueTriggered) this.minValueTriggered = point;
						}
					}
					else {
						if (this.triggerRanges != null) {
							this.tmpTriggerRange.setOut(i);
							this.triggerRanges.add(this.tmpTriggerRange);
							this.tmpTriggerRange = null;
						}
					}
				}
			}
		}
		if (log.isLoggable(Level.FINE)) {
			if (this.triggerRanges != null) {
				for (TriggerRange range : this.triggerRanges) {
					log.log(Level.FINE, this.name + " trigger range = " + range.in + "(" + TimeLine.getFomatedTime(range.in*this.timeStep_ms) + "), " + range.out + "(" + TimeLine.getFomatedTime(range.out*this.timeStep_ms) + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
				}
			}
			else
				log.log(Level.FINE, this.name + " triggerRanges = null"); //$NON-NLS-1$
		}
		if (this.triggerRanges != null) {
			// evaluate trigger ranges to meet minTimeSec requirement 
			int countDelta = new Double(this.minTriggerTimeSec / (this.timeStep_ms / 1000.0)).intValue();
			for (TriggerRange range : (Vector<TriggerRange>) this.triggerRanges.clone()) {
				if ((range.out - range.in) < countDelta) this.triggerRanges.remove(range);
			}
		}
		if (log.isLoggable(Level.FINE)) {
			if (this.triggerRanges != null) {
				for (TriggerRange range : this.triggerRanges) {
					log.log(Level.FINE, this.name + " trigger range = " + range.in + "(" + TimeLine.getFomatedTime(range.in*this.timeStep_ms) + "), " + range.out + "(" + TimeLine.getFomatedTime(range.out*this.timeStep_ms) + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
				}
			}
			else
				log.log(Level.FINE, this.name + " triggerRanges = null"); //$NON-NLS-1$
		}
		log.log(Level.FINER, this.name + " minTriggered = " + this.minValueTriggered + " maxTriggered = " + this.maxValueTriggered); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * get/calcualte max value by referenced triggered other measurement
	 * @param referencedMeasurementOrdinal
	 * @return maximum value according trigger specification of referenced measurement
	 */
	public int getMaxValueTriggered(int referencedMeasurementOrdinal) {
		if (this.tmpTriggerRange == null || this.triggerRanges == null)  {
			this.triggerRanges = this.parent.getRecord(this.parent.getRecordNames()[referencedMeasurementOrdinal]).getTriggerRanges();
		}
		if (this.maxValueTriggered == Integer.MIN_VALUE )this.setMinMaxValueTriggered();
		return this.maxValueTriggered;
	}

	/**
	 * get/calcualte max value by referenced triggered other measurement
	 * @param referencedMeasurementOrdinal
	 * @return minimum value according trigger specification of referenced measurement
	 */
	public int getMinValueTriggered(int referencedMeasurementOrdinal) {
		if (this.tmpTriggerRange == null || this.triggerRanges == null)  {
			this.triggerRanges = this.parent.getRecord(this.parent.getRecordNames()[referencedMeasurementOrdinal]).getTriggerRanges();
		}
		if (this.minValueTriggered == Integer.MAX_VALUE )this.setMinMaxValueTriggered();
		return this.minValueTriggered;
	}
	
	void setMinMaxValueTriggered() {
		if (this.triggerRanges != null) {
			for (TriggerRange range : this.triggerRanges) {
				for (int i = range.in; i < range.out; i++) {
					int point = this.realGet(i);
					if (point > this.maxValueTriggered) this.maxValueTriggered = point;
					if (point < this.minValueTriggered) this.minValueTriggered = point;
				}
			}
		}
	}
	
	/**
	 * overwrites size method for zoom mode and not zoomed compare window
	 */
	public int size() {
		int tmpSize = super.size();
		
		if (this.parent.isZoomMode)
			tmpSize = this.zoomSize;
		else if (this.parent.isScopeMode)
			tmpSize = this.parent.scopeModeSize;
		
		return tmpSize;
	}
	
	/**
	 * time calculation needs always the real size of the record
	 * @return real vector size 
	 */
	public int realSize() {
		return super.size();
	}
	
	public Integer getFirst() {
		return super.get(0);
	}
	
	public Integer getLast() {
		return super.get(super.size()-1);
	}

	/**
	 * overwrites vector get(int index) to enable zoom
	 * @param index
	 */
	public Integer get(int index) {
		int size = super.size();
		if(this.parent.isZoomMode) {
			index = index + this.zoomOffset;
			index = index > (size-1) ? (size-1) : index;
			index = index < 0 ? 0 : index;
		}
		else if(this.parent.isScopeMode) {
			index = index + this.parent.scopeModeOffset;
			index = index > (size-1) ? (size-1) : index;
			index = index < 0 ? 0 : index;
		}
		else {
			index = index > (size-1) ? (size-1) : index;
			index = index < 0 ? 0 : index;
		}
		return size != 0 ? super.get(index) : 0;
	}

	/**
	 * 
	 * @param index
	 */
	public Integer realGet(int index) {
		return super.size() != 0 ? super.get(index) : 0;
	}

	/**
	 * overwrites vector elementAt(int index) to enable zoom
	 * @param index
	 */
	public Integer elementAt(int index) {
		Integer value;
		if(this.parent.isZoomMode || this.parent.isScopeMode)
			value = super.elementAt(index + this.zoomOffset);
		else
			value = super.elementAt(index);
		
		return value;
	}
	
	public boolean isPositionLeft() {
		return this.isPositionLeft;
	}

	public void setPositionLeft(boolean enabled) {
		this.isPositionLeft = enabled;
	}

	public Color getColor() {
		return this.color;
	}

	public void setColor(Color newColor) {
		this.color = newColor;
	}

	public boolean isRoundOut() {
		return this.parent.isZoomMode ? false : this.isRoundOut;
	}

	public void setRoundOut(boolean enabled) {
		this.isRoundOut = enabled;
	}

	public boolean isStartpointZero() {
		return this.parent.isZoomMode ? false : this.isStartpointZero;
	}

	public void setStartpointZero(boolean enabled) {
		this.isStartpointZero = enabled;
	}

	public boolean isStartEndDefined() {
		return this.parent.isZoomMode ? true : this.isStartEndDefined;
	}

	/**
	 * sets the min-max values as displayed 4.0 - 200.5
	 * @param enabled
	 * @param newMinScaleValue
	 * @param newMaxScaleValue
	 */
	public void setStartEndDefined(boolean enabled, double newMinScaleValue, double newMaxScaleValue) {
		this.isStartEndDefined = enabled;
		if (enabled) {
			this.maxScaleValue = this.maxDisplayValue = newMaxScaleValue;
			this.minScaleValue = this.minDisplayValue = newMinScaleValue;
		}
		else {
			if (this.channelConfigKey == null || this.channelConfigKey.length() < 1)
				this.channelConfigKey = this.parent.getChannelConfigName();
			this.maxScaleValue = this.parent.getDevice().translateValue(this, this.maxValue/1000.0);
			this.minScaleValue = this.parent.getDevice().translateValue(this, this.minValue/1000.0);
		}
	}

	public void setMinScaleValue(double newMinScaleValue) {
		if (this.parent.isZoomMode)
			this.minZoomScaleValue = newMinScaleValue;
		else
			this.minScaleValue = newMinScaleValue;
	}

	public void setMaxScaleValue(double newMaxScaleValue) {
		if (this.parent.isZoomMode)
			this.maxZoomScaleValue = newMaxScaleValue;
		else
			this.maxScaleValue = newMaxScaleValue;
	}

	public int getLineWidth() {
		return this.lineWidth;
	}

	public void setLineWidth(int newLineWidth) {
		this.lineWidth = newLineWidth;
	}

	public int getLineStyle() {
		return this.lineStyle;
	}

	public void setLineStyle(int newLineStyle) {
		this.lineStyle = newLineStyle;
	}

	public int getNumberFormat() {
		return this.numberFormat;
	}

	public void setNumberFormat(int newNumberFormat) {
		this.numberFormat = newNumberFormat;
		switch (newNumberFormat) {
		case -1:
			double delta = this.maxScaleValue - this.minScaleValue;
			if (this.maxScaleValue <= 100) {
				if (delta <= 0.1)
					this.df.applyPattern("0.000"); //$NON-NLS-1$
				else if (delta <= 1)
					this.df.applyPattern("0.00"); //$NON-NLS-1$
				else if (delta <= 100)
					this.df.applyPattern("0.0"); //$NON-NLS-1$
				else
					this.df.applyPattern("0"); //$NON-NLS-1$
			}
			else if (this.maxScaleValue <= 500) {
				if (delta <= 0.1)
					this.df.applyPattern("0.00"); //$NON-NLS-1$
				else if (delta <= 1)
					this.df.applyPattern("0.0"); //$NON-NLS-1$
				else
					this.df.applyPattern("0"); //$NON-NLS-1$
			}
			else {
				if (delta <= 5)
					this.df.applyPattern("0.0"); //$NON-NLS-1$
				else
					this.df.applyPattern("0"); //$NON-NLS-1$
			}
			break;
		case 0:
			this.df.applyPattern("0"); //$NON-NLS-1$
			break;
		case 1:
			this.df.applyPattern("0.0"); //$NON-NLS-1$
			break;
		case 2:
		default:
			this.df.applyPattern("0.00"); //$NON-NLS-1$
			break;
		case 3:
			this.df.applyPattern("0.000"); //$NON-NLS-1$
			break;
		}
		RecordSet compareSet = OpenSerialDataExplorer.getInstance().getCompareSet();
		if (compareSet != null && compareSet.size() > 0) {
			this.parent.syncScaleNumberFormat(this.keyName, this.numberFormat);
		}
	}

	/**
	 * @return the parent
	 */
	public RecordSet getParent() {
		return this.parent;
	}

	/**
	 * @param currentParent the parent to set
	 */
	public void setParent(RecordSet currentParent) {
		if (this.channelConfigKey == null || this.channelConfigKey.length() < 1)
			this.channelConfigKey = currentParent.getChannelConfigName();
		this.parent = currentParent;
	}

	/**
	 * @return the isDisplayable
	 */
	public boolean isDisplayable() {
		return this.isDisplayable;
	}

	/**
	 * @param enabled the isDisplayable to set
	 */
	public void setDisplayable(boolean enabled) {
		this.isDisplayable = enabled;
	}

	/**
	 * @return the isActive
	 */
	public boolean isActive() {
		return this.isActive;
	}

	/**
	 * set isActive value
	 */
	public void setActive(boolean newValue) {
		this.isActive = newValue;
	}

	/**
	 * @return the maxScaleValue
	 */
	public double getMaxScaleValue() {
		return this.parent.isZoomMode ? this.maxZoomScaleValue : this.maxScaleValue;
	}

	/**
	 * @return the minScaleValue
	 */
	public double getMinScaleValue() {
		return this.parent.isZoomMode ? this.minZoomScaleValue : this.minScaleValue;
	}

	/** 
	 * qurey time step in milli seconds, this property is hold local to be independent (compare window)
	 * @return time step in ms
	 */
	public double getTimeStep_ms() {
		return this.timeStep_ms == 0 ? this.parent.getTimeStep_ms() : this.timeStep_ms;
	}

	/**
	 * set the time step in milli seconds, this property is hold local to be independent
	 * @param timeStep_ms the timeStep_ms to set
	 */
	void setTimeStep_ms(double newTimeStep_ms) {
		this.timeStep_ms = newTimeStep_ms;
	}

	public DecimalFormat getDecimalFormat() {
		if(this.numberFormat == -1) this.setNumberFormat(-1); // update the number format to actual delta
		return this.df;
	}

	/**
	 * @return the keyName
	 */
	public String getKeyName() {
		return this.keyName;
	}

	/**
	 * @param newKeyName the keyName to set
	 */
	public void setKeyName(String newKeyName) {
		this.keyName = newKeyName;
	}

	/**
	 * get the device to calculate or retrieve measurement properties, this property is hold local to be independent
	 * @return the device
	 */
	public IDevice getDevice() {
		if (this.device == null)
			this.device = this.parent.getDevice();
		
		return this.device;
	}

	/**
	 * set the device as fallback for data point calculation, this property is hold local to be independent
	 * @param device the device to set
	 */
	void setDevice(IDevice useDevice) {
		this.device = useDevice;
	}
	
	/**
	 * method to query time and value for display at a given index
	 * @param index
	 * @param scaledIndex (may differ from index if display width << number of points)
	 * @param xDisplayOffset
	 * @param yDisplayOffset
	 * @return point time, value
	 */
	public Point getDisplayPoint(int index, int scaledIndex, int xDisplayOffset, int yDisplayOffset) {
		Point returnPoint = new Point(0,0);
		returnPoint.x = new Double((xDisplayOffset + (this.timeStep_ms * index) * this.displayScaleFactorTime)).intValue();
		returnPoint.y = new Double(yDisplayOffset - ((this.get(scaledIndex) / 1000.0) - this.minDisplayValue) * this.displayScaleFactorValue).intValue();
		return returnPoint;
	}

	/**
	 * query data value (not translated in device units) from a display position point 
	 * @param xPos
	 * @param drawAreaBounds
	 * @return displays yPos in pixel
	 */
	public int getDisplayPointDataValue(int xPos, Rectangle drawAreaBounds) {
		int scaledIndex = this.size() * xPos / drawAreaBounds.width;
		scaledIndex = this.zoomOffset + scaledIndex >= this.realSize() ? this.realSize() - this.zoomOffset -1 : scaledIndex;
		log.log(Level.FINER, "scaledIndex = " + scaledIndex); //$NON-NLS-1$
		int pointY = new Double(drawAreaBounds.height - ((this.get(scaledIndex) / 1000.0) - this.minDisplayValue) * this.displayScaleFactorValue).intValue();
		pointY = pointY < 0 ? 0 : pointY;
		pointY = pointY >= drawAreaBounds.height ? drawAreaBounds.height : pointY;
		log.log(Level.FINER, "pointY = " + pointY); //$NON-NLS-1$
		return pointY;
	}
	
	/**
	 * get the value corresponding the display point (needs translate)
	 * @param yPos
	 * @param drawAreaBounds
	 * @return formated value
	 */
	public String getDisplayPointValueString(int yPos, Rectangle drawAreaBounds) {
		String displayPointValue;
		if(this.parent.isZoomMode)
			displayPointValue = this.df.format(new Double(this.minZoomScaleValue +  ((this.maxZoomScaleValue - this.minZoomScaleValue) * (drawAreaBounds.height-yPos) / drawAreaBounds.height)));
		else
			displayPointValue = this.df.format(new Double(this.minScaleValue +  ((this.maxScaleValue - this.minScaleValue) * (drawAreaBounds.height-yPos) / drawAreaBounds.height)));
		
		return displayPointValue;
	}

	/**
	 * get the value corresponding the display point (needs translate)
	 * @param yPos
	 * @param drawAreaBounds
	 * @return formated value
	 */
	public double getDisplayPointValue(int yPos, Rectangle drawAreaBounds) {
		double value;
		if(this.parent.isZoomMode || this.parent.isScopeMode)
			value = this.minZoomScaleValue + ((this.maxZoomScaleValue - this.minZoomScaleValue) * yPos) / drawAreaBounds.height;
		else
			value = this.minScaleValue + ((this.maxScaleValue - this.minScaleValue) * yPos) / drawAreaBounds.height;
		
		return value;
	}

	/**
	 * get the value corresponding the display point (needs translate)
	 * @param deltaPos
	 * @param drawAreaBounds
	 * @return formated value
	 */
	public String getDisplayDeltaValue(int deltaPos, Rectangle drawAreaBounds) {
		String textValue;
		if(this.parent.isZoomMode || this.parent.isScopeMode)
			textValue = this.df.format(new Double((this.maxZoomScaleValue - this.minZoomScaleValue) * deltaPos / drawAreaBounds.height));
		else
			textValue = this.df.format(new Double((this.maxScaleValue - this.minScaleValue) * deltaPos / drawAreaBounds.height));
	
		return textValue;
	}
	
	/**
	 * get the slope value of two given points, unit depends on device configuration
	 * @param points describing the time difference (x) as well as the measurement difference (y)
	 * @param drawAreaBounds
	 * @return string of value
	 */
	public String getSlopeValue(Point points, Rectangle drawAreaBounds) {
		log.log(Level.FINE, OSDE.STRING_EMPTY + points.toString());
		double measureDelta;
		if(this.parent.isZoomMode)
			measureDelta = (this.maxZoomScaleValue - this.minZoomScaleValue) * points.y / drawAreaBounds.height;
		else
			measureDelta = (this.maxScaleValue - this.minScaleValue) * points.y / drawAreaBounds.height;
		double timeDelta = 1.0 * points.x * this.size() / (drawAreaBounds.width-1) * this.getTimeStep_ms() / 1000; //sec
		log.log(Level.FINE, "measureDelta = " + measureDelta + " timeDelta = " + timeDelta); //$NON-NLS-1$ //$NON-NLS-2$
		return new DecimalFormat("0.0").format(measureDelta / timeDelta); //$NON-NLS-1$
	}

	/**
	 * set the zoom bounds from display as record point offset and size, min/max scale values
	 * @param zoomBounds - where the start point offset is x,y and the area is width, height
	 */
	public void setZoomBounds(Rectangle zoomBounds) {
		int size = super.size();
		this.zoomOffset = this.getPointIndexFromDisplayPoint(zoomBounds.x) + this.zoomOffset;
		this.zoomSize = this.getPointIndexFromDisplayPoint(zoomBounds.width);
		this.zoomSize = this.zoomOffset + this.zoomSize > size ? size - this.zoomOffset : this.zoomSize;
		this.minZoomScaleValue = this.getDisplayPointValue(zoomBounds.y, this.parent.drawAreaBounds);
		this.maxZoomScaleValue = this.getDisplayPointValue(zoomBounds.height + zoomBounds.y, this.parent.drawAreaBounds);
	}

	/**
	 * set the zoom size to record set to enable to display only the last size point
	 * recordZoomOffset must be calculated each graphics refresh
	 * @param newZoomSize number of points shown
	 */
	public void setZoomSize(int newZoomSize) {
		this.zoomSize = newZoomSize;
		this.parent.isScopeMode = true;
	}
	
	/**
	 * query actual recordZoomOffset
	 * @return recordZoomOffset
	 */
	public int getZoomOffset() {
		return this.zoomOffset;
	}
	
	/**
	 * set a new recordZoomOffset
	 * @param newRecordZoomOffset
	 */
	public void setZoomOffset(int newRecordZoomOffset) {
		this.zoomOffset = newRecordZoomOffset;
	}

	public int getZoomSize() {
		return this.zoomSize;
	}

	/**
	* get the formatted time at given position
	* @param xPos of the display point
	* @return string of time value in simple date format HH:ss:mm:SSS
	*/
	public String getDisplayPointTime(int xPos) {
		return TimeLine.getFomatedTimeWithUnit(new Double((this.getPointIndexFromDisplayPoint(xPos) + this.zoomOffset) * this.timeStep_ms).intValue());
	}

	/**
	 * calculate index in data vector from given display point
	 * @param xPos
	 * @return position integer value
	 */
	public int getPointIndexFromDisplayPoint(int xPos) {
		return new Double(1.0 * xPos * this.zoomSize / this.parent.getDrawAreaBounds().width).intValue();
	}
	
	/**
	 * @return the displayScaleFactorTime
	 */
	public double getDisplayScaleFactorTime() {
		return this.displayScaleFactorTime;
	}

	/**
	 * @param newDisplayScaleFactorTime the displayScaleFactorTime to set
	 */
	public void setDisplayScaleFactorTime(double newDisplayScaleFactorTime) {
		this.displayScaleFactorTime = newDisplayScaleFactorTime;
		log.log(Level.FINER, String.format(Locale.ENGLISH, "displayScaleFactorTime = %.3f", newDisplayScaleFactorTime)); //$NON-NLS-1$
	}

	/**
	 * @return the displayScaleFactorValue
	 */
	public double getDisplayScaleFactorValue() {
		return this.displayScaleFactorValue;
	}

	/**
	 * @param drawAreaHeight - used to calculate the displayScaleFactorValue to set
	 */
	public void setDisplayScaleFactorValue(int drawAreaHeight) {
		this.displayScaleFactorValue = (1.0 * drawAreaHeight) / (this.maxDisplayValue - this.minDisplayValue);
		log.log(Level.FINER, String.format(Locale.ENGLISH, "displayScaleFactorValue = %.3f (this.maxDisplayValue - this.minDisplayValue) = %.3f", this.displayScaleFactorValue, (this.maxDisplayValue - this.minDisplayValue))); //$NON-NLS-1$

	}

	/**
	 * @param newMinDisplayValue the minDisplayValue to set
	 */
	public void setMinDisplayValue(double newMinDisplayValue) {
		this.minDisplayValue = newMinDisplayValue;
	}

	/**
	 * @param newMaxDisplayValue the maxDisplayValue to set
	 */
	public void setMaxDisplayValue(double newMaxDisplayValue) {
		this.maxDisplayValue = newMaxDisplayValue;
	}

	/**
	 * @return the minDisplayValue
	 */
	public double getMinDisplayValue() {
		return this.minDisplayValue;
	}

	/**
	 * @return the maxDisplayValue
	 */
	public double getMaxDisplayValue() {
		return this.maxDisplayValue;
	}

	/**
	 * @return the formated minDisplayValue as it is displayed in graphics window
	 */
	public String getFormatedMinDisplayValue() {
		return this.df.format(this.minDisplayValue);
	}

	/**
	 * @return the formated maxDisplayValue as it is displayed in graphics window
	 */
	public String getFormatedMaxDisplayValue() {
		return this.df.format(this.maxDisplayValue);
	}

	/**
	 * set min and max scale values for zoomed mode
	 * @param newMinZoomScaleValue
	 * @param newMaxZoomScaleValue
	 */
	public void setMinMaxZoomScaleValues(double newMinZoomScaleValue, double newMaxZoomScaleValue) {
		this.minZoomScaleValue				= newMinZoomScaleValue;
		this.maxZoomScaleValue				= newMaxZoomScaleValue;
		log.log(Level.FINER, this.name + " - minScaleValue/minZoomScaleValue = " + this.minScaleValue + "/"  + newMinZoomScaleValue + " : maxScaleValue/maxZoomScaleValue = " + this.maxScaleValue + "/"  + newMaxZoomScaleValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}

	/**
	 * @return the isMeasurementMode
	 */
	public boolean isMeasurementMode() {
		return this.isMeasurementMode;
	}

	/**
	 * @param enabled the isMeasurementMode to set
	 */
	public void setMeasurementMode(boolean enabled) {
		this.isMeasurementMode = enabled;
	}

	/**
	 * @return the isDeltaMeasurementMode
	 */
	public boolean isDeltaMeasurementMode() {
		return this.isDeltaMeasurementMode;
	}

	/**
	 * @param enabled the isDeltaMeasurementMode to set
	 */
	public void setDeltaMeasurementMode(boolean enabled) {
		this.isDeltaMeasurementMode = enabled;
	}

	/**
	 * @return the parentName
	 */
	public String getChannelConfigKey() {
		if (this.channelConfigKey == null || this.channelConfigKey.length() < 1)
			this.channelConfigKey = this.parent.getChannelConfigName();

		return this.channelConfigKey;
	}

	/**
	 * @param newChannelConfigKey the channelConfigKey to set
	 */
	public void setChannelConfigKey(String newChannelConfigKey) {
		this.channelConfigKey = newChannelConfigKey;
	}

	/**
	 * reset the min-max-values to enable new settings after re-calculation
	 */
	public void resetMinMax() {
		this.maxValue = 0;
		this.minValue = 0;
		log.log(Level.FINER, this.name);
	}

	/**
	 * set new the min-max-values after external re-calculation
	 */
	public void setMinMax(int newMin, int newMax) {
		this.maxValue = newMax;
		this.minValue = newMin;
	}
	
	/**
	 * reset all variables to enable recalcualation of statistics
	 */
	public void resetStatiticCalculationBase() {
		this.maxValueTriggered = Integer.MIN_VALUE;
		this.minValueTriggered = Integer.MAX_VALUE;
		this.avgValue = Integer.MIN_VALUE;
		this.sigmaValue = Integer.MIN_VALUE;
		this.avgValueTriggered = Integer.MIN_VALUE;
		this.sigmaValueTriggered = Integer.MIN_VALUE;
		this.triggerRanges = null;
		this.tmpTriggerRange = null;
		log.log(Level.FINER, this.name);
	}
	
	/**
	 * get all record properties in serialized form
	 * @return serializedRecordProperties
	 */
	public String getSerializeProperties() {
		StringBuilder sb = new StringBuilder();
		sb.append(NAME).append(OSDE.STRING_EQUAL).append(this.name).append(DELIMITER);
		sb.append(UNIT).append(OSDE.STRING_EQUAL).append(this.unit).append(DELIMITER);
		sb.append(SYMBOL).append(OSDE.STRING_EQUAL).append(this.symbol).append(DELIMITER);
		sb.append(IS_ACTIVE).append(OSDE.STRING_EQUAL).append(this.isActive).append(DELIMITER);
		sb.append(IS_DIPLAYABLE).append(OSDE.STRING_EQUAL).append(this.isDisplayable).append(DELIMITER);
		sb.append(IS_VISIBLE).append(OSDE.STRING_EQUAL).append(this.isVisible).append(DELIMITER);
		sb.append(MAX_VALUE).append(OSDE.STRING_EQUAL).append(this.maxValue).append(DELIMITER);
		sb.append(MIN_VALUE).append(OSDE.STRING_EQUAL).append(this.minValue).append(DELIMITER);
		for (PropertyType property : this.properties) {
			log.log(Level.FINE, this.name + " - " + property.getName() + " = " + property.getValue()); //$NON-NLS-1$ //$NON-NLS-2$
			sb.append(property.getName()).append(OSDE.STRING_UNDER_BAR).append(property.getType()).append(OSDE.STRING_EQUAL).append(property.getValue()).append(DELIMITER);
		}
		sb.append(DEFINED_MAX_VALUE).append(OSDE.STRING_EQUAL).append(this.maxScaleValue).append(DELIMITER);
		sb.append(DEFINED_MIN_VALUE).append(OSDE.STRING_EQUAL).append(this.minScaleValue).append(DELIMITER);
		sb.append(IS_POSITION_LEFT).append(OSDE.STRING_EQUAL).append(this.isPositionLeft).append(DELIMITER);
		sb.append(COLOR).append(OSDE.STRING_EQUAL).append(this.color.getRed()).append(OSDE.STRING_COMMA).append(this.color.getGreen()).append(OSDE.STRING_COMMA).append(this.color.getBlue()).append(DELIMITER);
		sb.append(LINE_WITH).append(OSDE.STRING_EQUAL).append(this.lineWidth).append(DELIMITER);
		sb.append(LINE_STYLE).append(OSDE.STRING_EQUAL).append(this.lineStyle).append(DELIMITER);
		sb.append(IS_ROUND_OUT).append(OSDE.STRING_EQUAL).append(this.isRoundOut).append(DELIMITER);
		sb.append(IS_START_POINT_ZERO).append(OSDE.STRING_EQUAL).append(this.isStartpointZero).append(DELIMITER);
		sb.append(IS_START_END_DEFINED).append(OSDE.STRING_EQUAL).append(this.isStartEndDefined).append(DELIMITER);
		sb.append(NUMBER_FORMAT).append(OSDE.STRING_EQUAL).append(this.numberFormat).append(DELIMITER);
		return sb.substring(0, sb.lastIndexOf(Record.DELIMITER)) + Record.END_MARKER;
	}
	
	/**
	 * set all record properties by given serialized form
	 * @param serializedRecordProperties
	 */
	public void setSerializedProperties(String serializedRecordProperties) {
		HashMap<String, String> recordProps = StringHelper.splitString(serializedRecordProperties, DELIMITER, Record.propertyKeys);
		String tmpValue = null;
				
		tmpValue = recordProps.get(UNIT);
		if (tmpValue!=null && tmpValue.length() > 0) this.unit =  tmpValue.trim();
		tmpValue = recordProps.get(SYMBOL);
		if (tmpValue!=null && tmpValue.length() > 0) this.symbol =  tmpValue.trim();
		tmpValue = recordProps.get(IS_ACTIVE);
		if (tmpValue!=null && tmpValue.length() > 0) this.isActive =  new Boolean(tmpValue.trim()).booleanValue();
		tmpValue = recordProps.get(IS_DIPLAYABLE);
		if (tmpValue!=null && tmpValue.length() > 0) this.isDisplayable =  new Boolean(tmpValue.trim()).booleanValue();
		tmpValue = recordProps.get(IS_VISIBLE);
		if (tmpValue!=null && tmpValue.length() > 0) this.isVisible =  new Boolean(tmpValue.trim()).booleanValue();
		tmpValue = recordProps.get(IS_POSITION_LEFT);
		if (tmpValue!=null && tmpValue.length() > 0) this.isPositionLeft =  new Boolean(tmpValue.trim()).booleanValue();
		tmpValue = recordProps.get(IS_DIPLAYABLE);
		if (tmpValue!=null && tmpValue.length() > 0) this.isDisplayable =  new Boolean(tmpValue.trim()).booleanValue();
		tmpValue = recordProps.get(COLOR);
		if (tmpValue!=null && tmpValue.length() > 5) this.color = SWTResourceManager.getColor(new Integer(tmpValue.split(OSDE.STRING_COMMA)[0]), new Integer(tmpValue.split(OSDE.STRING_COMMA)[1]), new Integer(tmpValue.split(OSDE.STRING_COMMA)[2]));
		tmpValue = recordProps.get(LINE_WITH);
		if (tmpValue!=null && tmpValue.length() > 0) this.lineWidth =  new Integer(tmpValue.trim()).intValue();
		tmpValue = recordProps.get(LINE_STYLE);
		if (tmpValue!=null && tmpValue.length() > 0) this.lineStyle =  new Integer(tmpValue.trim()).intValue();
		tmpValue = recordProps.get(IS_ROUND_OUT);
		if (tmpValue!=null && tmpValue.length() > 0) this.isRoundOut =  new Boolean(tmpValue.trim()).booleanValue();
		tmpValue = recordProps.get(IS_START_POINT_ZERO);
		if (tmpValue!=null && tmpValue.length() > 0) this.isStartpointZero =  new Boolean(tmpValue.trim()).booleanValue();
		tmpValue = recordProps.get(IS_START_END_DEFINED);
		if (tmpValue!=null && tmpValue.length() > 0) this.isStartEndDefined =  new Boolean(tmpValue.trim()).booleanValue();
		tmpValue = recordProps.get(NUMBER_FORMAT);
		if (tmpValue!=null && tmpValue.length() > 0) this.setNumberFormat(new Integer(tmpValue.trim()).intValue());
		tmpValue = recordProps.get(MAX_VALUE);
		if (tmpValue!=null && tmpValue.length() > 0) this.maxValue =  new Integer(tmpValue.trim()).intValue();
		tmpValue = recordProps.get(MIN_VALUE);
		if (tmpValue!=null && tmpValue.length() > 0) this.minValue =  new Integer(tmpValue.trim()).intValue();
		tmpValue = recordProps.get(DEFINED_MAX_VALUE);
		if (tmpValue!=null && tmpValue.length() > 0) this.maxScaleValue =  new Double(tmpValue.trim()).doubleValue();
		tmpValue = recordProps.get(DEFINED_MIN_VALUE);
		if (tmpValue!=null && tmpValue.length() > 0) this.minScaleValue =  new Double(tmpValue.trim()).doubleValue();

		tmpValue =  recordProps.get(NAME);
		if (tmpValue!=null && tmpValue.length() > 0 && !this.name.equalsIgnoreCase(tmpValue)) {
			this.setName(tmpValue.trim()); // might replace the record set key as well
		}
}
	
	/**
	 * set the device specific properties for this record
	 * @param serializedProperties
	 */
	public void setSerializedDeviceSpecificProperties(String serializedProperties) {
		HashMap<String, String> recordDeviceProps = StringHelper.splitString(serializedProperties, DELIMITER, this.getDevice().getUsedPropertyKeys());
		StringBuilder sb = new StringBuilder();
		if (log.isLoggable(Level.FINE)) sb.append(this.name).append(OSDE.STRING_MESSAGE_CONCAT);
		
		// each record loaded from a file gets new properties instead of using the default initialized in constructor
		this.properties = new ArrayList<PropertyType>(); // offset, factor, reduction, ...
		
		Iterator<String> iterator = recordDeviceProps.keySet().iterator();
		if (iterator.hasNext()) {
			while (iterator.hasNext()) {
				String propName = iterator.next();
				String prop = recordDeviceProps.get(propName);
				PropertyType tmpProperty = new ObjectFactory().createPropertyType();
				tmpProperty.setName(propName);
				String type = prop.split(OSDE.STRING_EQUAL)[0].substring(1);
				if (type != null && type.length() > 3) tmpProperty.setType(DataTypes.fromValue(type));
				String value = prop.split(OSDE.STRING_EQUAL)[1];
				if (value != null && value.length() > 0) tmpProperty.setValue(value.trim());
				this.properties.add(tmpProperty);
				if (log.isLoggable(Level.FINE)) sb.append(propName).append(" = ").append(value);
			}
		}
		log.log(Level.FINE, sb.toString());
	}
	
	/**
	 * set data unsaved with a given reason
	 * @param reason
	 */
	public void setUnsaved(String reason) {
		this.parent.setUnsaved(reason);
	}

	/**
	 * calls parent getRecordNames for all records of normal record sets, 
	 * if record set isCompareSet it returns the recordSetNames of the source record set 
	 * @return the getRecordSetNames
	 */
	public String[] getRecordSetNames() {
		return this.getParent().getRecordNames();
	}

	/**
	 * @return the numberScaleTicks
	 */
	public int getNumberScaleTicks() {
		return this.numberScaleTicks;
	}

	/**
	 * @param newNumberScaleTicks the numberScaleTicks to set
	 */
	public void setNumberScaleTicks(int newNumberScaleTicks) {
		this.numberScaleTicks = newNumberScaleTicks;
	}

	/**
	 * @return the avgValue
	 */
	public int getAvgValue() {
		this.setAvgValue();
		return this.avgValue;
	}
	
	/**
	 * get/calcualte avg value by configuraed trigger
	 * @return average value according trigger specification
	 */
	public int getAvgValueTriggered() {
		if (this.triggerRanges == null)  {
			this.evaluateMinMax();
		}
		this.setAvgValueTriggered();
		return this.avgValueTriggered;
	}
	
	/**
	 * get/calcualte avg value by referenced triggered other measurement
	 * @param referencedMeasurementOrdinal
	 * @return average value according trigger specification of referenced measurement
	 */
	public int getAvgValueTriggered(int referencedMeasurementOrdinal) {
		if (this.triggerRanges == null)  {
			this.triggerRanges = this.parent.getRecord(this.parent.getRecordNames()[referencedMeasurementOrdinal]).getTriggerRanges();
		}
		this.setAvgValueTriggered();
		return this.avgValueTriggered;
	}
	
	/**
	 * calculates the avgValue
	 */
	public synchronized void setAvgValue() {
		if (super.size() >= 2) {
			long sum = 0;
			for (Integer xi : this) {
				sum += xi;
			}
			this.avgValue = new Long(sum / this.realSize()).intValue();
		}
	}
	
	/**
	 * calculates the avgValue using trigger ranges
	 */
	public synchronized void setAvgValueTriggered() {
		long sum = 0;
		int numPoints = 0;
		StringBuilder sb = new StringBuilder();
		if (this.triggerRanges != null) {
			for (TriggerRange range : this.triggerRanges) {
				for (int i = range.in; i < range.out; i++) {
					sum += this.get(i);
					if (log.isLoggable(Level.FINER)) sb.append(this.realGet(i) / 1000.0).append(", "); //$NON-NLS-1$
					numPoints++;
				}
				if (log.isLoggable(Level.FINER)) sb.append("\n"); //$NON-NLS-1$
			}
			log.log(Level.FINER, sb.toString());
			this.avgValueTriggered = numPoints > 0 ? new Long(sum / numPoints).intValue() : 0 ;
		}
	}

	/**
	 * @return the sigmaValue
	 */
	public int getSigmaValue() {
		this.setSigmaValue();
		return this.sigmaValue;
	}
	
	/**
	 * get/calcualte avg value by trigger configuration
	 * @return sigma value according trigger specification
	 */
	public int getSigmaValueTriggered() {
		if (this.triggerRanges == null)  {
			this.evaluateMinMax();
		}
		if (this.sigmaValueTriggered == Integer.MIN_VALUE) this.setSigmaValueTriggered();
		return this.sigmaValueTriggered;
	}
	
	/**
	 * get/calcualte avg value by referenced triggered other measurement
	 * @param referencedMeasurementOrdinal
	 * @return sigma value according trigger specification of referenced measurement
	 */
	public int getSigmaValueTriggered(int referencedMeasurementOrdinal) {
		if (this.triggerRanges == null)  {
			this.triggerRanges = this.parent.getRecord(this.parent.getRecordNames()[referencedMeasurementOrdinal]).getTriggerRanges();
		}
		if (this.sigmaValueTriggered == Integer.MIN_VALUE) this.setSigmaValueTriggered();
		return this.sigmaValueTriggered;
	}

	/**
	 * calculates the sigmaValue 
	 */
	public synchronized void setSigmaValue() {
		if (super.size() >= 2) {
			double average = this.getAvgValue() / 1000.0;
			double sumPoweredValues = 0;
			for (Integer xi : this) {
				sumPoweredValues += Math.pow(xi / 1000.0 - average, 2);
			}
			this.sigmaValue = new Double(Math.sqrt(sumPoweredValues / (this.realSize() - 1)) * 1000).intValue();
		}
	}

	/**
	 * calculates the sigmaValue using trigger ranges
	 */
	public synchronized void setSigmaValueTriggered() {
		double average = this.getAvgValueTriggered()/1000.0;
		double sumPoweredDeviations = 0;
		int numPoints = 0;
		if (this.triggerRanges != null) {
			for (TriggerRange range : this.triggerRanges) {
				for (int i = range.in; i < range.out; i++) {
					sumPoweredDeviations += Math.pow(this.realGet(i)/1000.0 - average, 2);
					numPoints++;
				}
			}
			this.sigmaValueTriggered = new Double(Math.sqrt(sumPoweredDeviations/(numPoints-1))*1000).intValue();
		}
	}
	
	/**
	 * get/calcualte sum of values by configured trigger
	 * @return sum value according trigger range specification of referenced measurement
	 */
	public int getSumTriggeredRange() {
		if (this.triggerRanges == null)  {
			this.evaluateMinMax();
		}
		return this.calculateSum();
	}
	
	/**
	 * get/calcualte sum of values by configuraed trigger
	 * @param referencedMeasurementOrdinal
	 * @return sum value according trigger range specification of referenced measurement
	 */
	public int getSumTriggeredRange(int referencedMeasurementOrdinal) {
		if (this.triggerRanges == null)  {
			this.triggerRanges = this.parent.getRecord(this.parent.getRecordNames()[referencedMeasurementOrdinal]).getTriggerRanges();
		}
		return this.calculateSum();
	}
	
	/**
	 * calculate sum of min/max delta of each trigger range
	 */
	int calculateSum() {
		int sum = 0;
		int min=0, max=0;
		if (this.triggerRanges != null) {
			for (TriggerRange range : this.triggerRanges) {
				for (int i = range.in; i < range.out; i++) {
					if (i == range.in)
						min = max = this.realGet(i);
					else {
						int point = this.realGet(i);
						if (point > max) max = point;
						if (point < min) min = point;
					}
				}
				sum += this.device.translateValue(this, (max - min));
			}
		}
		return sum;
	}
	
	/**
	 * get/calcualte sum of time by configured trigger
	 * @return sum value according trigger range specification of referenced measurement
	 */
	public String getTimeSumTriggeredRange() {
		if (this.triggerRanges == null)  {
			this.evaluateMinMax();
		}
		return this.calculateTimeSum();
	}
	
	/**
	 * get/calcualte sum of time by configuraed trigger
	 * @param referencedMeasurementOrdinal
	 * @return sum value according trigger range specification of referenced measurement
	 */
	public String getTimeSumTriggeredRange(int referencedMeasurementOrdinal) {
		if (this.triggerRanges == null)  {
			this.triggerRanges = this.parent.getRecord(this.parent.getRecordNames()[referencedMeasurementOrdinal]).getTriggerRanges();
		}
		return this.calculateTimeSum();
	}
	
	/**
	 * calculate sum of min/max delta of each trigger range
	 */
	String calculateTimeSum() {
		double sum = 0;
		if (this.triggerRanges != null) {
			for (TriggerRange range : this.triggerRanges) {
				sum += (range.out - range.in) * this.getTimeStep_ms();
			}
		}
		return TimeLine.getFomatedTimeWithUnit(sum);
	}
	
	/**
	 * @return the triggerRanges
	 */
	public Vector<TriggerRange> getTriggerRanges() {
		this.evaluateMinMax();
		return this.triggerRanges;
	}

	/**
	 * query if the record display scale is synced with an other record
	 * @return the isScaleSynced
	 */
	public boolean isScaleSynced() {
		return this.isScaleSynced;
	}

	/**
	 * set isScaleSynced to true if the to be displaed scale is in sync with other record
	 * @param enabled the isScaleSynced value to set
	 */
	public void setScaleSynced(boolean enabled) {
		this.isScaleSynced = enabled;
	}

	/**
	 * @return the isSyncPlaceholder
	 */
	public boolean isSyncPlaceholder() {
		return this.isSyncPlaceholder;
	}

	/**
	 * @param enable the value of isSyncPlaceholder to set
	 */
	public void setSyncPlaceholder(boolean enable) {
		this.isSyncPlaceholder = enable;
	}

	/**
	 * set min max values for scope view (recordSet.setScopeMode())
	 * @param newScopeMin the scopeMin to set
	 * @param newScopeMax the scopeMin to set
	 */
	public void setScopeMinMax(int newScopeMin, int newScopeMax) {
		this.scopeMin = newScopeMin;
		this.scopeMax = newScopeMax;
	}
	
	/**
	 * get the curve index until the curve will be drawn (for compare set with different time length curves)
	 * @return the drawLimit
	 */
	public int getDrawLimit() {
		return this.parent.isZoomMode ? this.drawLimit - this.zoomOffset : drawLimit;
	}

	/**
	 * set the curve index until the curve will be drawn
	 * @param drawLimit the drawLimit to set
	 */
	public void setDrawLimit(int drawLimit) {
		this.drawLimit = drawLimit;
	}
}

