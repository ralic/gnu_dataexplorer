/**************************************************************************************
  	This file is part of OpenSerialdataExplorer.

    OpenSerialdataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OpenSerialdataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with OpenSerialdataExplorer.  If not, see <http://www.gnu.org/licenses/>.
****************************************************************************************/
package osde.device.renschler;

import gnu.io.NoSuchPortException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import osde.data.Channels;
import osde.data.Record;
import osde.data.RecordSet;
import osde.device.DeviceConfiguration;
import osde.device.IDevice;
import osde.device.MeasurementType;
import osde.ui.OpenSerialDataExplorer;
import osde.ui.tab.GraphicsWindow;
import osde.utils.CalculationThread;
import osde.utils.QuasiLinearRegression;

/**
 * Picolariolog device main implementaion class
 * @author Winfried Brügmann
 */
public class Picolario extends DeviceConfiguration implements IDevice {
	private Logger									log										= Logger.getLogger(this.getClass().getName());

	private final OpenSerialDataExplorer application;
	private final PicolarioDialog	dialog;
	private final PicolarioSerialPort serialPort;
	private final Channels channels;
	private CalculationThread				calculationThread;

	/**
	 * @param iniFile
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws NoSuchPortException 
	 * @throws SAXException 
	 * @throws ParserConfigurationException 
	 */
	public Picolario(String iniFile) throws FileNotFoundException, IOException, NoSuchPortException, ParserConfigurationException, SAXException {
		super(iniFile);
		this.application = OpenSerialDataExplorer.getInstance();
		this.serialPort = new PicolarioSerialPort(this, this.application.getStatusBar());
		this.dialog = new PicolarioDialog(this.application.getShell(), this);
		this.channels = Channels.getInstance();
	}

	/**
	 * constructor using existing device configuration
	 * @param deviceConfig device configuration
	 * @throws NoSuchPortException 
	 */
	public Picolario(DeviceConfiguration deviceConfig) throws NoSuchPortException {
		super(deviceConfig);
		this.application = OpenSerialDataExplorer.getInstance();
		this.serialPort = new PicolarioSerialPort(this, this.application.getStatusBar());
		this.dialog = new PicolarioDialog(this.application.getShell(), this);
		this.channels = Channels.getInstance();
	}

	/**
	 * function to translate measured values from a device to values represented
	 * @return double[] where value[0] is the min value and value[1] the max value
	 */
	public double[] translateValues(String recordKey, double minValue, double maxValue) {
		double[] newValues = new double[2];

		newValues[0] = translateValue(recordKey, minValue);
		newValues[1] = translateValue(recordKey, maxValue);

		return newValues;
	}

	/**
	 * function to translate measured value from a device to values represented
	 * @return double with the adapted value
	 */
	public double translateValue(String recordKey, double value) {
		double newValue = 0.0;
		String[] measurements = this.getMeasurementNames(); // 0=Spannung, 1=Höhe, 2=Steigung
		if(log.isLoggable(Level.FINEST)) log.finest(String.format("input value for %s - %f", recordKey, value));
		if (recordKey.startsWith(measurements[0])) {		// 0=Spannung
			// calculate voltage U = 2.5 + (byte3 - 45) * 0.0532
			newValue = 2.5 + (value - 45.0) * 0.0532;
		}
		else if (recordKey.startsWith(measurements[1])) {	// 1=Höhe
			int firstValue = 0; // != 0 if first value must subtracted
			double offset = 0; // != 0 if curve has an defined offset
			double factor = 1.0; // != 1 if a unit translation is required
			// prepare the data for adding to record set
			switch (dialog.getHeightUnitSelection()) { // Feet 1, Meter 0
			case 0: // Meter , Feet is default
				factor = 1852.0 / 6076.0;
				break;
			case 1: // Feet /Fuß
				factor = 1.0;
				break;
			}
			RecordSet recordSet = application.isRecordSetVisible(GraphicsWindow.TYPE_NORMAL) ? channels.getActiveChannel().getActiveRecordSet() : application.getCompareSet();
			if (dialog.isDoSubtractFirst()) {
				firstValue = recordSet.getRecord(recordKey).getFirst().intValue() / 1000;
			}
			else if (dialog.isDoSubtractLast()) {
				Record record = recordSet.getRecord(recordKey);
				firstValue = record.getLast().intValue() / 1000;
			}
			else if (dialog.isDoReduceHeight()) {
				offset = dialog.getHeightOffsetValue();
			}
			if (log.isLoggable(Level.FINER)) log.finer("value = " + value + " firstValue = " + firstValue + " factor = " + factor + " offset = " + offset);
			newValue = (value - firstValue) * factor - offset;
		}
		else if (recordKey.startsWith(measurements[2])) {		// 2=Steigung
			double factor = 1.0; // != 1 if a unit translation is required
			switch (dialog.getHeightUnitSelection()) { // Feet 1, Meter 0
			case 0: // Meter , Feet is default
				factor = 1852.0 / 6076.0;
				break;
			case 1: // Feet /Fuß
				factor = 1.0;
				break;
			}
			newValue = value * factor;
		}
		if(log.isLoggable(Level.FINEST)) log.finest(String.format("value calculated for %s - inValue %f - outValue %f", recordKey, value, newValue));
		return newValue;
	}

	/**
	 * function to translate measured value from a device to values represented
	 * @return double with the adapted value
	 */
	public double reverseTranslateValue(String recordKey, double value) {
		if(log.isLoggable(Level.FINEST)) log.finest(String.format("input value for %s - %f", recordKey, value));
		double newValue = 0;
		String[] measurements = this.getMeasurementNames(); // 0=Spannung, 1=Höhe, 2=Steigung
		if (recordKey.startsWith(measurements[0])) { // 0=Spannung
			// calculate voltage U = 2.5 + (value - 45) * 0.0532
			newValue = (value - 2.5) / 0.0532 + 45.0;
		}
		else if (recordKey.startsWith(measurements[1])) {  	// 1=Höhe
			int firstValue = 0; // != 0 if first value must subtracted
			double offset = 0; // != 0 if curve has an defined offset
			double factor = 1.0; // != 1 if a unit translation is required
			// prepare the data for adding to record set
			switch (dialog.getHeightUnitSelection()) { // Feet 1, Meter 0
			case 0: // Meter , Feet is default
				if (log.isLoggable(Level.FINER)) log.finer("heightUnitSelection = " + dialog.getHeightUnitSelection());
				factor = 1852.0 / 6076.0;
				break;
			case 1: // Feet /Fuß
				if (log.isLoggable(Level.FINER)) log.finer("heightUnitSelection = " + dialog.getHeightUnitSelection());
				factor = 1.0;
				break;
			}
			RecordSet recordSet = application.isRecordSetVisible(GraphicsWindow.TYPE_NORMAL) ? channels.getActiveChannel().getActiveRecordSet() : application.getCompareSet();
			if (dialog.isDoSubtractFirst()) {
				firstValue = recordSet.getRecord(recordKey).getFirst().intValue() / 1000;
			}
			else if (dialog.isDoSubtractLast()) {
				Record record = recordSet.getRecord(recordKey);
				firstValue = record.getLast().intValue() / 1000;
			}
			else if (dialog.isDoReduceHeight()) {
				offset = dialog.getHeightOffsetValue();
			}
			if (log.isLoggable(Level.FINER)) log.finer("value = " + value + " offset = " + offset + " factor = " + factor + " firstValue = " + firstValue);
			newValue = (value + offset) / factor + firstValue;
		}
		else if (recordKey.startsWith(measurements[2])) {		// 2=Steigung
			double factor = 1.0; // != 1 if a unit translation is required
			switch (dialog.getHeightUnitSelection()) { // Feet 1, Meter 0
			case 0: // Meter , Feet is default
				factor = 1852.0 / 6076.0;
				break;
			case 1: // Feet /Fuß
				factor = 1.0;
				break;
			}
			newValue = value / factor;
		}
		if(log.isLoggable(Level.FINEST)) log.finest(String.format("new value calculated for %s - inValue %f - outValue %f", recordKey, value, newValue));
		return newValue;
	}

	/**
	 * @return the dataUnit
	 */
	public String getDataUnit(String recordKey) {
		String unit = "";
		recordKey = recordKey.split("_")[0];
		MeasurementType measurement = this.getMeasurementDefinition(recordKey);
		String[] measurements = this.getMeasurementNames(); // 0=Spannung, 1=Höhe, 2=Steigung
		//channel.get("Messgröße1");
		if (recordKey.startsWith(measurements[0])) {		// 0=Spannung
			unit = (String) measurement.getUnit();
		}
		else if (recordKey.startsWith(measurements[1])) {	// 1=Höhe
			unit = dialog.getHeightDataUnit();
		}
		else if (recordKey.startsWith(measurements[2])) {	// 2=Steigung
			unit = dialog.getHeightDataUnit() + "/" +  measurement.getUnit().split("/")[1];
		}
		return unit;
	}

	/**
	 * function to calculate values for inactive and to be calculated records
	 */
	public void makeInActiveDisplayable(RecordSet recordSet) {
		// since there are measurement point every 10 seconds during capturing only and the calculation will take place directly switch all to displayable
		if (recordSet.isFromFile() && recordSet.isRaw()) {
			for (String recordKey : recordSet.getRecordNames()) {
				if (!recordSet.get(recordKey).isDisplayable()) {
					// calculate the values required				
					String[] measurements = this.getMeasurementNames(); // 0=Spannung, 1=Höhe, 2=Steigrate
					calculationThread = new QuasiLinearRegression(recordSet, measurements[1], measurements[2]);
					calculationThread.start();
				}
			}
		}
	}

	/**
	 * @return the dialog
	 */
	public PicolarioDialog getDialog() {
		return dialog;
	}

	/**
	 * @return the serialPort
	 */
	public PicolarioSerialPort getSerialPort() {
		return serialPort;
	}
}
