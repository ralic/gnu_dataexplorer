/**************************************************************************************
  	This file is part of GNU DataExplorer.

    GNU DataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    GNU DataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with GNU DataExplorer.  If not, see <http://www.gnu.org/licenses/>.
    
    Copyright (c) 2013 Winfried Bruegmann
****************************************************************************************/
package gde.device.renschler;

import gde.GDE;
import gde.comm.DeviceCommPort;
import gde.data.Record;
import gde.data.RecordSet;
import gde.device.DeviceConfiguration;
import gde.device.PropertyType;
import gde.io.FileHandler;
import gde.log.Level;
import gde.messages.Messages;
import gde.ui.dialog.IgcExportDialog;
import gde.utils.CalculationThread;
import gde.utils.FileUtils;
import gde.utils.LinearRegression;
import gde.utils.QuasiLinearRegression;

import java.io.FileNotFoundException;

import javax.xml.bind.JAXBException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

/**
 * Picolario2 device main implementation class
 * @author Winfried Brügmann
 */
public class Picolario2 extends Picolario {

	/**
	 * @param iniFile
	 * @throws JAXBException 
	 * @throws FileNotFoundException 
	 */
	public Picolario2(String iniFile) throws FileNotFoundException, JAXBException {
		super(iniFile);
		this.dialog = new Picolario2Dialog(this.application.getShell(), this);
		if (this.application.getMenuToolBar() != null) {
			this.configureSerialPortMenu(DeviceCommPort.ICON_SET_IMPORT_CLOSE, Messages.getString(MessageIds.GDE_MSGT1253), Messages.getString(MessageIds.GDE_MSGT1253));
			updateFileMenu(this.application.getMenuBar().getExportMenu());
			updateFileImportMenu(this.application.getMenuBar().getImportMenu());
		}
	}
	

	/**
	 * constructor using existing device configuration
	 * @param deviceConfig device configuration
	 */
	public Picolario2(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
		this.dialog = new Picolario2Dialog(this.application.getShell(), this);
		if (this.application.getMenuToolBar() != null) {
			this.configureSerialPortMenu(DeviceCommPort.ICON_SET_IMPORT_CLOSE, Messages.getString(MessageIds.GDE_MSGT1253), Messages.getString(MessageIds.GDE_MSGT1253));
			updateFileMenu(this.application.getMenuBar().getExportMenu());
			updateFileImportMenu(this.application.getMenuBar().getImportMenu());
		}
	}

	/**
	 * function to prepare a data table row of record set while translating available measurement values
	 * @return pointer to filled data table row with formated values
	 */
	@Override
	public String[] prepareDataTableRow(RecordSet recordSet, String[] dataTableRow, int rowIndex) {
		try {
			for (int j = 0; j < recordSet.size(); j++) {
				Record record = recordSet.get(j);
				double offset = record.getOffset(); // != 0 if curve has an defined offset
				double reduction = record.getReduction();
				double factor = record.getFactor(); // != 1 if a unit translation is required
				
				// 0=Höhe, 1=Druck, 2=SpannungRx, 3=Steigung, 4=Spannung, 5=Strom, 6=Drehzahl 7=Temperatur, 8=Speed, 9=Höhe GPS
				switch (j) { 
				case 0: //Höhe/Height
					PropertyType property = record.getProperty(Picolario.DO_SUBTRACT_FIRST);
					boolean subtractFirst = property != null ? Boolean.valueOf(property.getValue()).booleanValue() : false;
					property = record.getProperty(Picolario.DO_SUBTRACT_LAST);
					boolean subtractLast = property != null ? Boolean.valueOf(property.getValue()).booleanValue() : false;
					
					if (subtractFirst) {
						reduction = record.getFirst()/1000.0;
					}
					else if (subtractLast) {
						reduction = record.getLast()/1000.0;
					}
					else {
						reduction = 0;
					}
					break;
				case 3: //Steigung/Slope
					factor = recordSet.get(3).getFactor(); // 1=height
					break;
				}
				
				dataTableRow[j + 1] = record.getDecimalFormat().format((offset + ((record.realGet(rowIndex) / 1000.0) - reduction) * factor));
			}
		}
		catch (RuntimeException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}
		return dataTableRow;		
	}

	/**
	 * function to translate measured value from a device to values represented (((value - reduction) * factor) + offset - firstLastAdaption)
	 * @return double with the adapted value
	 */
	@Override
	public double translateValue(Record record, double value) {
		final String $METHOD_NAME = "translateValue()";
		log.log(Level.FINEST, String.format("input value for %s - %f", record.getName(), value)); //$NON-NLS-1$

		String recordKey = "?"; //$NON-NLS-1$
		double newValue = 0.0;
		try {
			// 0=Höhe, 1=Druck, 2=SpannungRx, 3=Steigung, 4=Spannung, 5=Strom, 6=Drehzahl 7=Temperatur, 8=Speed, 9=Höhe GPS
			recordKey = record.getName();
			double offset = record.getOffset(); // != 0 if curve has an defined offset
			double reduction = record.getReduction();
			double factor = record.getFactor(); // != 1 if a unit translation is required

			switch (record.getOrdinal()) {
			case 0: // 0=height calculation need special procedure
				PropertyType property = record.getProperty(Picolario.DO_SUBTRACT_FIRST);
				boolean subtractFirst = property != null ? Boolean.valueOf(property.getValue()).booleanValue() : false;
				property = record.getProperty(Picolario.DO_SUBTRACT_LAST);
				boolean subtractLast = property != null ? Boolean.valueOf(property.getValue()).booleanValue() : false;

				try {
					if (subtractFirst) {
						reduction = record.getFirst() / 1000.0;
					}
					else if (subtractLast) {
						reduction = record.getLast() / 1000.0;
					}
				}
				catch (Throwable e) {
					log.log(Level.SEVERE, record.getParent().getName() + " " + record.getName() + " " + e.getMessage() + " " + $CLASS_NAME + "." + $METHOD_NAME);
				}
				break;

			case 3: // 3=slope calculation needs height factor for calculation
				factor = this.getMeasurementFactor(record.getParent().getChannelConfigNumber(), 0); // 0=height
				break;
			}

			newValue = offset + (value - reduction) * factor;
		}
		catch (RuntimeException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}

		log.log(Level.FINER, String.format("value calculated for %s - inValue %f - outValue %f", recordKey, value, newValue)); //$NON-NLS-1$
		return newValue;
	}

	/**
	 * function to translate measured value from a device to values represented (((value - offset + firstLastAdaption)/factor) + reduction)
	 * @return double with the adapted value
	 */
	@Override
	public double reverseTranslateValue(Record record, double value) {
		log.log(Level.FINEST, String.format("input value for %s - %f", record.getName(), value)); //$NON-NLS-1$
		final String $METHOD_NAME = "reverseTranslateValue()";

		// 0=Höhe, 1=Druck, 2=SpannungRx, 3=Steigung, 4=Spannung, 5=Strom, 6=Drehzahl 7=Temperatur, 8=Speed, 9=Höhe GPS
		String recordKey = record.getName();
		double offset = record.getOffset(); // != 0 if curve has an defined offset
		double reduction = record.getReduction();
		double factor = record.getFactor(); // != 1 if a unit translation is required

		switch (record.getOrdinal()) {
		case 0: // 0=height calculation need special procedure
			PropertyType property = record.getProperty(Picolario.DO_SUBTRACT_FIRST);
			boolean subtractFirst = property != null ? Boolean.valueOf(property.getValue()).booleanValue() : false;
			property = record.getProperty(Picolario.DO_SUBTRACT_LAST);
			boolean subtractLast = property != null ? Boolean.valueOf(property.getValue()).booleanValue() : false;

			try {
				if (subtractFirst) {
					reduction = record.getFirst() / 1000.0;
				}
				else if (subtractLast) {
					reduction = record.getLast() / 1000.0;
				}
			}
			catch (Throwable e) {
				log.log(Level.SEVERE, record.getParent().getName() + " " + record.getName() + " " + e.getMessage() + " " + $CLASS_NAME + "." + $METHOD_NAME);
			}
			break;
		
		case 3: // 3=slope calculation needs height factor for calculation
			factor = this.getMeasurementFactor(record.getParent().getChannelConfigNumber(), 0); // 1=height
			break;
		}
		double newValue = (value - offset) / factor + reduction;

		log.log(Level.FINER, String.format("new value calculated for %s - inValue %f - outValue %f", recordKey, value, newValue)); //$NON-NLS-1$
		return newValue;
	}

	/**
	 * function to calculate values for inactive and to be calculated records
	 */
	@Override
	public void makeInActiveDisplayable(RecordSet recordSet) {
		// since there are measurement point every 10 seconds during capturing only and the calculation will take place directly switch all to displayable
		if (recordSet.isRaw() && recordSet.isRecalculation()) {
			// 0=Höhe, 1=Druck, 2=SpannungRx, 3=Steigung, 4=Spannung, 5=Strom, 6=Drehzahl 7=Temperatur, 8=Speed, 9=Höhe GPS
			// calculate the values required		
			Record slopeRecord = recordSet.get(3);//3=Steigrate
			slopeRecord.setDisplayable(false);
			PropertyType property = slopeRecord.getProperty(CalculationThread.REGRESSION_INTERVAL_SEC);
			int regressionInterval = property != null ? new Integer(property.getValue()) : 10;
			property = slopeRecord.getProperty(CalculationThread.REGRESSION_TYPE);
			if (property == null || property.getValue().equals(CalculationThread.REGRESSION_TYPE_CURVE))
				this.calculationThread = new QuasiLinearRegression(recordSet, recordSet.get(0).getName(), slopeRecord.getName(), regressionInterval);
			else
				this.calculationThread = new LinearRegression(recordSet, recordSet.get(0).getName(), slopeRecord.getName(), regressionInterval);

			try {
				this.calculationThread.start();
			}
			catch (RuntimeException e) {
				log.log(Level.WARNING, e.getMessage(), e);
			}
		}
	}

	/**
	 * update the file menu by adding two new entries to export KML/GPX files
	 * @param exportMenue
	 */
	public void updateFileMenu(Menu exportMenue) {
		MenuItem convertKMZ3DRelativeItem;
		MenuItem convertKMZ3DAbsoluteItem;
		MenuItem convertIGCItem;

		if (exportMenue.getItem(exportMenue.getItemCount() - 1).getText().equals(Messages.getString(gde.messages.MessageIds.GDE_MSGT0018))) {
			new MenuItem(exportMenue, SWT.SEPARATOR);

			convertKMZ3DRelativeItem = new MenuItem(exportMenue, SWT.PUSH);
			convertKMZ3DRelativeItem.setText(Messages.getString(MessageIds.GDE_MSGT1254));
			convertKMZ3DRelativeItem.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					log.log(java.util.logging.Level.FINEST, "convertKLM3DRelativeItem action performed! " + e); //$NON-NLS-1$
					export2KMZ3D(DeviceConfiguration.HEIGHT_RELATIVE);
				}
			});

			convertKMZ3DAbsoluteItem = new MenuItem(exportMenue, SWT.PUSH);
			convertKMZ3DAbsoluteItem.setText(Messages.getString(MessageIds.GDE_MSGT1255));
			convertKMZ3DAbsoluteItem.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					log.log(java.util.logging.Level.FINEST, "convertKLM3DAbsoluteItem action performed! " + e); //$NON-NLS-1$
					export2KMZ3D(DeviceConfiguration.HEIGHT_ABSOLUTE);
				}
			});

			convertKMZ3DAbsoluteItem = new MenuItem(exportMenue, SWT.PUSH);
			convertKMZ3DAbsoluteItem.setText(Messages.getString(MessageIds.GDE_MSGT1256));
			convertKMZ3DAbsoluteItem.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					log.log(java.util.logging.Level.FINEST, "convertKLM3DAbsoluteItem action performed! " + e); //$NON-NLS-1$
					export2KMZ3D(DeviceConfiguration.HEIGHT_CLAMPTOGROUND);
				}
			});

			new MenuItem(exportMenue, SWT.SEPARATOR);

			convertIGCItem = new MenuItem(exportMenue, SWT.PUSH);
			convertIGCItem.setText(Messages.getString(gde.messages.MessageIds.GDE_MSGT0611));
			convertIGCItem.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					log.log(java.util.logging.Level.FINEST, "convertIGCItem action performed! " + e); //$NON-NLS-1$
					//GPS 		0=latitude 1=longitude 2=altitudeAbs 3=numSatelites 4=PDOP 5=HDOP 6=VDOP 7=velocity;
					//SMGPS 	8=altitudeRel 9=climb 10=voltageRx 11=distanceTotal 12=distanceStart 13=directionStart 14=glideRatio;
					//Unilog 15=voltageUniLog 16=currentUniLog 17=powerUniLog 18=revolutionUniLog 19=voltageRxUniLog 20=heightUniLog 21=a1UniLog 22=a2UniLog 23=a3UniLog;
					//M-LINK 24=valAdd00 25=valAdd01 26=valAdd02 27=valAdd03 28=valAdd04 29=valAdd05 30=valAdd06 31=valAdd07 32=valAdd08 33=valAdd09 34=valAdd10 35=valAdd11 36=valAdd12 37=valAdd13 38=valAdd14;
					new IgcExportDialog().open(1, 0, 2);
				}
			});
		}
	}

	/**
	 * exports the actual displayed data set to KML file format
	 * @param type DeviceConfiguration.HEIGHT_RELATIVE | DeviceConfiguration.HEIGHT_ABSOLUTE | DeviceConfiguration.HEIGHT_CLAMPTOGROUND
	 */
	public void export2KMZ3D(int type) {
		//GPS 		0=latitude 1=longitude 2=altitudeAbs 3=numSatelites 4=PDOP 5=HDOP 6=VDOP 7=velocity;
		//SMGPS 	8=altitudeRel 9=climb 10=voltageRx 11=distanceTotal 12=distanceStart 13=directionStart 14=glideRatio;
		//Unilog 15=voltageUniLog 16=currentUniLog 17=powerUniLog 18=revolutionUniLog 19=voltageRxUniLog 20=heightUniLog 21=a1UniLog 22=a2UniLog 23=a3UniLog;
		//M-LINK 24=valAdd00 25=valAdd01 26=valAdd02 27=valAdd03 28=valAdd04 29=valAdd05 30=valAdd06 31=valAdd07 32=valAdd08 33=valAdd09 34=valAdd10 35=valAdd11 36=valAdd12 37=valAdd13 38=valAdd14;
		new FileHandler().exportFileKMZ(Messages.getString(MessageIds.GDE_MSGT1252), 1, 0, 2, 7, 9, 11, -1, type == DeviceConfiguration.HEIGHT_RELATIVE, type == DeviceConfiguration.HEIGHT_CLAMPTOGROUND);
	}

	/**
	 * update the file import menu by adding new entry to import device specific files
	 * @param importMenue
	 */
	public void updateFileImportMenu(Menu importMenue) {
		MenuItem importDeviceLogItem;

		if (importMenue.getItem(importMenue.getItemCount() - 1).getText().equals(Messages.getString(gde.messages.MessageIds.GDE_MSGT0018))) {
			new MenuItem(importMenue, SWT.SEPARATOR);

			importDeviceLogItem = new MenuItem(importMenue, SWT.PUSH);
			importDeviceLogItem.setText(Messages.getString(MessageIds.GDE_MSGT1257, GDE.MOD1));
			importDeviceLogItem.setAccelerator(SWT.MOD1 + Messages.getAcceleratorChar(MessageIds.GDE_MSGT1257));
			importDeviceLogItem.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					log.log(java.util.logging.Level.FINEST, "importDeviceLogItem action performed! " + e); //$NON-NLS-1$
					open_closeCommPort();
				}
			});
		}
	}

	/**
	 * @return the measurement ordinal where velocity limits as well as the colors are specified (GPS-velocity)
	 */
	@Override
	public Integer getGPS2KMZMeasurementOrdinal() {
		//GPGGA	0=latitude 1=longitude  2=altitudeAbs 3=numSatelites
		return -1;
	}

	/**
	 * method toggle open close serial port or start/stop gathering data from device
	 * if the device does not use serial port communication this place could be used for other device related actions which makes sense here
	 * as example a file selection dialog could be opened to import serialized ASCII data 
	 */
	@Override
	public void open_closeCommPort() {
		final FileDialog fd = FileUtils.getImportDirectoryFileDialog(this, Messages.getString(MessageIds.GDE_MSGT1251));

		Thread reader = new Thread("reader") { //$NON-NLS-1$
			@Override
			public void run() {
				try {
					Picolario2.this.application.setPortConnected(true);
					for (String tmpFileName : fd.getFileNames()) {
						String selectedImportFile = fd.getFilterPath() + GDE.FILE_SEPARATOR_UNIX + tmpFileName;
						Picolario2.log.log(java.util.logging.Level.FINE, "selectedImportFile = " + selectedImportFile); //$NON-NLS-1$

						if (fd.getFileName().length() > 4) {
							try {
								Integer channelConfigNumber = Picolario2.this.dialog != null && !Picolario2.this.dialog.isDisposed() ? ((Picolario2Dialog)Picolario2.this.dialog).getTabFolderSelectionIndex() + 1 : null;
								String recordNameExtend = selectedImportFile.substring(selectedImportFile.lastIndexOf(GDE.STRING_DOT) - 4, selectedImportFile.lastIndexOf(GDE.STRING_DOT));
								Picolario2LogReader.read(selectedImportFile, Picolario2.this, recordNameExtend, channelConfigNumber);
							}
							catch (Throwable e) {
								Picolario2.log.log(java.util.logging.Level.WARNING, e.getMessage(), e);
							}
						}
					}
				}
				finally {
					Picolario2.this.application.setPortConnected(false);
				}
			}
		};
		reader.start();
	}
}
