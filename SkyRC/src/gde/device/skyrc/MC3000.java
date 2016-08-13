/**************************************************************************************
  	This file is part of DataExplorer.

    GNU DataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    GNU DataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with DataExplorer.  If not, see <http://www.gnu.org/licenses/>.

    Copyright (c) 2010,2011,2012,2013,2014,2015,2016 Winfried Bruegmann
****************************************************************************************/
package gde.device.skyrc;

import gde.GDE;
import gde.comm.DeviceCommPort;
import gde.config.Settings;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.Record;
import gde.data.RecordSet;
import gde.device.DeviceConfiguration;
import gde.device.IDevice;
import gde.device.MeasurementType;
import gde.exception.ApplicationConfigurationException;
import gde.exception.DataInconsitsentException;
import gde.io.DataParser;
import gde.log.Level;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.utils.StringHelper;
import gde.utils.WaitTimer;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.logging.Logger;

import javax.usb.UsbClaimException;
import javax.usb.UsbException;
import javax.usb.UsbInterface;
import javax.xml.bind.JAXBException;

/**
 * Class to implement SKYRC MC3000 device
 * @author Winfried Brügmann
 */
public class MC3000 extends DeviceConfiguration implements IDevice {
	final static Logger	log	= Logger.getLogger(MC3000.class.getName());

	final DataExplorer	application;
	final Settings			settings;
	final MC3000Dialog	dialog;
	final MC3000UsbPort	usbPort;

	protected String[]	STATUS_MODE;
	protected String[]	USAGE_MODE_LI;
	protected String[]	USAGE_MODE_NI;
	protected String[]	USAGE_MODE_ZN;
	protected String[]	BATTERY_TYPE;
	//firmware 1.05+ power and energy comes direct from device
	//protected int[]			resetEnergy = new int[] {5,5,5,5};
  final static String[]			cellTypeNames							= {"LiIo","LiFe","LiHV","NiMH","NiCd","NiZn","Eneloop","RAM"};
	final static String[]			cycleModeNames						= {"D>C","D>C>D","C>D","C>D>C"};
	final static String[]			operationModeLi						= {"CHARGE","REFRESH","STORAGE","DISCHARGE","CYCLE"};
	final static String[]			operationModeOther				= {"CHARGE","REFRESH","BREAKIN","DISCHARGE","CYCLE"};
	final static String[]			cellModelNames						= {"Lite AAA","Std AAA","Pro/XX AAA","Lite AA","Std AA","Pro/XX AA","Std C","Std D"};
	final static int[]			  cellModelCapacity					= {720, 960, 1080, 1200, 2400, 3000, 3840, 7200};

	protected class SystemSettings {
		byte currentSlotNumber;
		byte slot1programmNumber;
		byte slot2programmNumber;
		byte slot3programmNumber;
		byte slot4programmNumber;
		byte userInterfaceMode;
		byte temperatureUnit;
		byte beepTone;
		boolean isHideLiFe;
		boolean isHideLiIon435;
		boolean isHideEneloop;
		boolean isHideNiZn;
		byte LCDoffTime;
		byte minVoltage;
		byte[] machineId = new byte[16];
		boolean isHideRAM;
		
		public SystemSettings(final byte[] buffer) {
			currentSlotNumber = buffer[2];
			slot1programmNumber = buffer[3];
			slot2programmNumber = buffer[4];
			slot3programmNumber = buffer[5];
			slot4programmNumber = buffer[6];
			userInterfaceMode = buffer[7];
			temperatureUnit = buffer[8];
			beepTone = buffer[9];
			isHideLiFe = buffer[10] == 0x01;
			isHideLiIon435 = buffer[11] == 0x01;
			isHideEneloop = buffer[12] == 0x01;
			isHideNiZn = buffer[13] == 0x01;
			LCDoffTime = buffer[14];
			minVoltage = buffer[15];
			for (int i = 0; i < 15; i++) {
				machineId[i] = buffer[i+16];
			}
			isHideRAM = buffer[32] == 0x01;
			//System.out.println(new String(machineId));
		}

		public String getFirmwareVersion() {
			return String.format("Firmware : %d.%02d", machineId[11], machineId[12]);
		}

		public int getFirmwareVersionAsInt() {
			return Integer.valueOf(String.format("%d%02d", machineId[11], machineId[12])).intValue();
		}
		
		public byte getCurrentSlotNumber() {
			return currentSlotNumber;
		}

		public void setCurrentSlotNumber(byte currentSlotNumber) {
			this.currentSlotNumber = currentSlotNumber;
		}

		public byte getUserInterfaceMode() {
			return userInterfaceMode;
		}

		public void setUserInterfaceMode(byte userInterfaceMode) {
			this.userInterfaceMode = userInterfaceMode;
		}

		public byte getTemperatureUnit() {
			return temperatureUnit;
		}

		public void setTemperatureUnit(byte temperatureUnit) {
			this.temperatureUnit = temperatureUnit;
		}

		public byte getBeepTone() {
			return beepTone;
		}

		public void setBeepTone(byte beepTone) {
			this.beepTone = beepTone;
		}

		public boolean isHideLiFe() {
			return isHideLiFe;
		}

		public void setHideLiFe(boolean isHideLiFe) {
			this.isHideLiFe = isHideLiFe;
		}

		public boolean isHideLiIon435() {
			return isHideLiIon435;
		}

		public void setHideLiIon435(boolean isHideLiIon435) {
			this.isHideLiIon435 = isHideLiIon435;
		}

		public boolean isHideEneloop() {
			return isHideEneloop;
		}

		public void setHideEneloop(boolean isHideEneloop) {
			this.isHideEneloop = isHideEneloop;
		}

		public boolean isHideNiZn() {
			return isHideNiZn;
		}

		public void setHideRAM(boolean isHideRAM) {
			this.isHideRAM = isHideRAM;
		}

		public boolean isHideRAM() {
			return isHideRAM;
		}

		public void setHideNiZn(boolean isHideNiZn) {
			this.isHideNiZn = isHideNiZn;
		}

		public byte getLCDoffTime() {
			return LCDoffTime;
		}

		public void setLCDoffTime(byte lCDoffTime) {
			LCDoffTime = lCDoffTime;
		}

		public byte getMinVoltage() {
			return minVoltage;
		}

		public void setMinVoltage(byte minVoltage) {
			this.minVoltage = minVoltage;
		}

		public byte getSlot1programmNumber() {
			return (byte) (slot1programmNumber+1);
		}

		public byte getSlot2programmNumber() {
			return (byte) (slot2programmNumber+1);
		}

		public byte getSlot3programmNumber() {
			return (byte) (slot3programmNumber+1);
		}

		public byte getSlot4programmNumber() {
			return (byte) (slot4programmNumber+1);
		}
	}
	SystemSettings systemSettings;

	protected class SlotSettings {
		byte[] slotBuffer = new byte[64];
		byte slotNumber;
		byte busyTag;
		byte batteryType;
		byte operatinoMode;
		byte[] capacity;
		byte[] chargeCurrent;
		byte[] dischargeCurrent;
		byte[] dischargeEndVoltage;
		byte[] chargeEndVoltage;
		byte[] dischargeEndCurrent;
		byte[] chargeEndCurrent;
		byte numberCycle;
		byte chargeRestingTime;
		byte cycleMode;
		byte endDeltaVoltage; //Ni cells only
		byte trickleCurrent;
		byte cutTemperature;
		byte[] cutTime;
		byte[] restartVoltage;
		byte temeratureUnit;
	
		public SlotSettings(final byte[] buffer) {
			System.arraycopy(buffer, 0, this.slotBuffer, 0, buffer.length);
			this.slotNumber = buffer[1];
			this.busyTag = buffer[2];
			this.batteryType = buffer[3];
			this.operatinoMode = buffer[4];
			this.capacity = new byte[] {buffer[5], buffer[6]};
			this.chargeCurrent = new byte[] {buffer[7], buffer[8]};
			this.dischargeCurrent = new byte[] {buffer[9], buffer[10]};
			this.dischargeEndVoltage = new byte[] {buffer[11], buffer[12]};
			this.chargeEndVoltage = new byte[] {buffer[13], buffer[14]};
			this.dischargeEndCurrent = new byte[] {buffer[15], buffer[16]};
			this.chargeEndCurrent = new byte[] {buffer[17], buffer[18]};
			this.numberCycle = buffer[19];
			this.chargeRestingTime = buffer[20];
			this.cycleMode = buffer[21];
			this.endDeltaVoltage = buffer[22];
			this.trickleCurrent = buffer[23];
			this.restartVoltage = new byte[] {buffer[24], buffer[25]};
			this.cutTemperature = buffer[26];
			this.cutTime = new byte[] {buffer[27], buffer[28]};
			this.temeratureUnit = buffer[29];
			log.log(Level.OFF, this.toString());
		}
		
		public boolean isBusy() {
			return this.busyTag == 0x01;
		}
		
		@Override
		public String toString() {
			return String.format("slotNumber=%02d busy=%b batteryType=%02d operatinoMode=%02d capacity=%04d chargeCurrent=%04d dischargeCurrent=%04d dischargeEndVoltage=%04d chargeEndVoltage=%04d dischargeEndCurrent=%04d chargeEndCurrent=%04d numberCycle=%02d chargeRestingTime=%02d cycleMode=%d endDeltaVoltage=%02d trickleCurrent=%02d cutTemperature=%02d cutTime=%03d restartVoltage=%03d temeratureUnit=%d",
					this.slotNumber, this.busyTag == 0x01, this.batteryType, this.operatinoMode, getCapacity(), getChargeCurrent(), getDischargeCurrent(), getDischargeEndCurrent(), getChargeEndVoltage(), getDischargeEndCurrent(), getChargeEndCurrent(), this.numberCycle, this.chargeRestingTime, this.cycleMode, this.endDeltaVoltage, this.trickleCurrent, this.cutTemperature, getCutTime(), getRestartVoltage(), this.temeratureUnit);
		}
		
		public String toString4View() {
			int i = 0;
		for (; this.batteryType == 6 && i < cellModelCapacity.length; i++) {
			if (this.getCapacity() == cellModelCapacity[i])
				break;
		}
			return String.format("%s - %s - %04d mAh (%s)",
					MC3000.cellTypeNames[this.batteryType], 
					this.batteryType < 3 ? MC3000.operationModeLi[this.operatinoMode] : MC3000.operationModeOther[this.operatinoMode],
					getCapacity(),
					this.batteryType == 6 && i < MC3000.cellModelNames.length ? MC3000.cellModelNames[i] : GDE.STRING_DASH );
		}
		
		public byte[] getBuffer() {
			return this.slotBuffer;
		}
		
		public byte[] getBuffer(final byte newSlotNumber, final int firmwareAsNumber) {
			byte[] reducedBuffer = new byte[64];
			if (firmwareAsNumber <= 111) {
				reducedBuffer[0] = 0x0F;
				reducedBuffer[1] = 0x1D;
				reducedBuffer[2] = 0x11;
				reducedBuffer[3] = 0x00;
				reducedBuffer[4] = newSlotNumber;
				reducedBuffer[5] = this.batteryType;
				reducedBuffer[6] = (byte) (this.getCapacity()/100);
				reducedBuffer[7] = this.operatinoMode;
				System.arraycopy(this.slotBuffer,  7, reducedBuffer,  8, 17);//charge current to trickle current
				reducedBuffer[25] = this.cutTemperature;
				System.arraycopy(this.slotBuffer, 27, reducedBuffer, 26,  2);//cut time
				System.arraycopy(this.slotBuffer, 24, reducedBuffer, 28,  2);//restart voltage
				reducedBuffer[30] = MC3000UsbPort.calculateCheckSum(reducedBuffer);
				reducedBuffer[31] = (byte) 0xFF;
				reducedBuffer[32] = (byte) 0xFF;
				log.log(Level.OFF, StringHelper.byte2Hex2CharString(reducedBuffer, 64));
				log.log(Level.OFF, String.format("slotNumber=%02d batteryType=%02d operatinoMode=%02d capacity=%04d chargeCurrent=%04d dischargeCurrent=%04d dischargeEndVoltage=%04d chargeEndVoltage=%04d dischargeEndCurrent=%04d chargeEndCurrent=%04d numberCycle=%02d chargeRestingTime=%02d cycleMode=%d endDeltaVoltage=%02d trickleCurrent=%02d cutTemperature=%02d cutTime=%03d restartVoltage=%04d",
						reducedBuffer[4], reducedBuffer[5], this.operatinoMode, reducedBuffer[6]*100, getChargeCurrent(), getDischargeCurrent(), getDischargeEndCurrent(), getChargeEndVoltage(), getDischargeEndCurrent(), getChargeEndCurrent(), this.numberCycle, this.chargeRestingTime, this.cycleMode, this.endDeltaVoltage, this.trickleCurrent, this.cutTemperature, DataParser.parse2Short(reducedBuffer[27], reducedBuffer[26]), DataParser.parse2Short(reducedBuffer[29], reducedBuffer[28])));
			}
			return reducedBuffer;
		}
		
		public void setSlotNumber(final byte newSlotNumber) {
			this.slotNumber = newSlotNumber;
			this.slotBuffer[1] = newSlotNumber;
			this.setCheckSum(MC3000UsbPort.calculateCheckSum(this.slotBuffer));
		}
		
		public int getSlotNumber() {
			return this.slotNumber;
		}

		public byte getBatteryType() {
			return batteryType;
		}

		public byte getOperatinoMode() {
			return operatinoMode;
		}

		public short getCapacity() {
			return DataParser.parse2Short(this.capacity[1], this.capacity[0]);
		}

		public short getChargeCurrent() {
			return DataParser.parse2Short(this.chargeCurrent[1], this.chargeCurrent[0]);
		}

		public short getDischargeCurrent() {
			return DataParser.parse2Short(this.dischargeCurrent[1], this.dischargeCurrent[0]);
		}

		public short getDischargeEndVoltage() {
			return DataParser.parse2Short(this.dischargeEndVoltage[1], this.dischargeEndVoltage[0]);
		}

		public short getChargeEndVoltage() {
			return DataParser.parse2Short(this.chargeEndVoltage[1], this.chargeEndVoltage[0]);
		}

		public short getDischargeEndCurrent() {
			return DataParser.parse2Short(this.dischargeEndCurrent[1], this.dischargeEndCurrent[0]);
		}

		public short getChargeEndCurrent() {
			return DataParser.parse2Short(this.chargeEndCurrent[1], this.chargeEndCurrent[0]);
		}

		public byte getNumberCycle() {
			return numberCycle;
		}

		public byte getChargeRestingTime() {
			return chargeRestingTime;
		}

		public byte getCycleMode() {
			return cycleMode;
		}

		public byte getEndDeltaVoltage() {
			return endDeltaVoltage;
		}

		public byte getTrickleCurrent() {
			return trickleCurrent;
		}

		public byte getCutTemperature() {
			return cutTemperature;
		}

		public short getCutTime() {
			return DataParser.parse2Short(this.cutTime[1], this.cutTime[0]);
		}

		public short getRestartVoltage() {
			return DataParser.parse2Short(this.restartVoltage[1], this.restartVoltage[0]);
		}

		public void setCheckSum(final byte newChecksum) {
			this.slotBuffer[this.slotBuffer.length - 1] = newChecksum;
		}
	}

	/**
	 * constructor using properties file
	 * @throws JAXBException 
	 * @throws FileNotFoundException 
	 */
	public MC3000(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.skyrc.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$
		this.STATUS_MODE = new String[] { Messages.getString(MessageIds.GDE_MSGT3600), Messages.getString(MessageIds.GDE_MSGT3601), Messages.getString(MessageIds.GDE_MSGT3602),
				Messages.getString(MessageIds.GDE_MSGT3603), Messages.getString(MessageIds.GDE_MSGT3604), Messages.getString(MessageIds.GDE_MSGT3605) };
		this.USAGE_MODE_LI = new String[] { Messages.getString(MessageIds.GDE_MSGT3610), Messages.getString(MessageIds.GDE_MSGT3611), Messages.getString(MessageIds.GDE_MSGT3612),
				Messages.getString(MessageIds.GDE_MSGT3613), Messages.getString(MessageIds.GDE_MSGT3614) };
		this.USAGE_MODE_NI = new String[] { Messages.getString(MessageIds.GDE_MSGT3620), Messages.getString(MessageIds.GDE_MSGT3621), Messages.getString(MessageIds.GDE_MSGT3622),
				Messages.getString(MessageIds.GDE_MSGT3623), Messages.getString(MessageIds.GDE_MSGT3624) };
		this.USAGE_MODE_ZN = new String[] { Messages.getString(MessageIds.GDE_MSGT3620), Messages.getString(MessageIds.GDE_MSGT3621), Messages.getString(MessageIds.GDE_MSGT3623), 
				Messages.getString(MessageIds.GDE_MSGT3624), Messages.getString(MessageIds.GDE_MSGT3624) };
		this.BATTERY_TYPE = new String[] { Messages.getString(MessageIds.GDE_MSGT3640), Messages.getString(MessageIds.GDE_MSGT3641), Messages.getString(MessageIds.GDE_MSGT3642), 
				Messages.getString(MessageIds.GDE_MSGT3643), Messages.getString(MessageIds.GDE_MSGT3644), Messages.getString(MessageIds.GDE_MSGT3645),
				Messages.getString(MessageIds.GDE_MSGT3646), Messages.getString(MessageIds.GDE_MSGT3647) };

		this.application = DataExplorer.getInstance();
		this.settings = Settings.getInstance();
		this.usbPort = new MC3000UsbPort(this, this.application);
		this.dialog = new MC3000Dialog(this.application.getShell(), this);
		if (this.application.getMenuToolBar() != null) {
			this.configureSerialPortMenu(DeviceCommPort.ICON_SET_START_STOP, GDE.STRING_EMPTY, GDE.STRING_EMPTY);
		}
	}

	/**
	 * constructor using existing device configuration
	 * @param deviceConfig device configuration
	 */
	public MC3000(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
		// initializing the resource bundle for this device
		Messages.setDeviceResourceBundle("gde.device.skyrc.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$
		this.STATUS_MODE = new String[] { Messages.getString(MessageIds.GDE_MSGT3600), Messages.getString(MessageIds.GDE_MSGT3601), Messages.getString(MessageIds.GDE_MSGT3602),
				Messages.getString(MessageIds.GDE_MSGT3603), Messages.getString(MessageIds.GDE_MSGT3604), Messages.getString(MessageIds.GDE_MSGT3605) };
		this.USAGE_MODE_LI = new String[] { Messages.getString(MessageIds.GDE_MSGT3610), Messages.getString(MessageIds.GDE_MSGT3611), Messages.getString(MessageIds.GDE_MSGT3612),
				Messages.getString(MessageIds.GDE_MSGT3613), Messages.getString(MessageIds.GDE_MSGT3614) };
		this.USAGE_MODE_NI = new String[] { Messages.getString(MessageIds.GDE_MSGT3620), Messages.getString(MessageIds.GDE_MSGT3621), Messages.getString(MessageIds.GDE_MSGT3622),
				Messages.getString(MessageIds.GDE_MSGT3623), Messages.getString(MessageIds.GDE_MSGT3624) };
		this.USAGE_MODE_ZN = new String[] { Messages.getString(MessageIds.GDE_MSGT3620), Messages.getString(MessageIds.GDE_MSGT3621), Messages.getString(MessageIds.GDE_MSGT3623), 
				Messages.getString(MessageIds.GDE_MSGT3624), Messages.getString(MessageIds.GDE_MSGT3624) };
		this.BATTERY_TYPE = new String[] { Messages.getString(MessageIds.GDE_MSGT3640), Messages.getString(MessageIds.GDE_MSGT3641), Messages.getString(MessageIds.GDE_MSGT3642), 
				Messages.getString(MessageIds.GDE_MSGT3643), Messages.getString(MessageIds.GDE_MSGT3644), Messages.getString(MessageIds.GDE_MSGT3645),
				Messages.getString(MessageIds.GDE_MSGT3646), Messages.getString(MessageIds.GDE_MSGT3647) };

		this.application = DataExplorer.getInstance();
		this.settings = Settings.getInstance();
		this.usbPort = new MC3000UsbPort(this, this.application);
		this.dialog = new MC3000Dialog(this.application.getShell(), this);
		if (this.application.getMenuToolBar() != null) {
			this.configureSerialPortMenu(DeviceCommPort.ICON_SET_START_STOP, GDE.STRING_EMPTY, GDE.STRING_EMPTY);
		}
	}

	/**
	 * @return the dialog
	 */
	@Override
	public MC3000Dialog getDialog() {
		return this.dialog;
	}

	/**
	 * load the mapping exist between lov file configuration keys and gde keys
	 * @param lov2osdMap reference to the map where the key mapping has to be put
	 * @return lov2osdMap same reference as input parameter
	 */
	@Override
	public HashMap<String, String> getLovKeyMappings(HashMap<String, String> lov2osdMap) {
		// ...
		return lov2osdMap;
	}

	/**
	 * convert record LogView config data to gde config keys into records section
	 * @param header reference to header data, contain all key value pairs
	 * @param lov2osdMap reference to the map where the key mapping
	 * @param channelNumber 
	 * @return converted configuration data
	 */
	@Override
	public String getConvertedRecordConfigurations(HashMap<String, String> header, HashMap<String, String> lov2osdMap, int channelNumber) {
		// ...
		return ""; //$NON-NLS-1$
	}

	/**
	 * get LogView data bytes size, as far as known modulo 16 and depends on the bytes received from device 
	 */
	@Override
	public int getLovDataByteSize() {
		return 86; //sometimes first 4 bytes give the length of data + 4 bytes for number
	}

	/**
	 * check and update visibility status of all records according the available device configuration
	 * this function must have only implementation code if the device implementation supports different configurations
	 * where some curves are hided for better overview 
	 * example: if device supports voltage, current and height and no sensors are connected to voltage and current
	 * it makes less sense to display voltage and current curves, if only height has measurement data
	 * at least an update of the graphics window should be included at the end of this method
	 */
	@Override
	public void updateVisibilityStatus(RecordSet recordSet, boolean includeReasonableDataCheck) {
		int channelConfigNumber = recordSet.getChannelConfigNumber();
		int displayableCounter = 0;
		boolean configChanged = this.isChangePropery();
		Record record;
		MeasurementType measurement;
		// 0=voltageReceiver, 1=voltage, 2=current, 3=capacity, 4=power, 5=energy, 6=votagePerCell, 7=revolutionSpeed, 8=efficiency, 9=height, 10=slope, 11=a1Value, 12=a2Value, 13=a3Value
		String[] measurementNames = this.getMeasurementNames(channelConfigNumber);
		// check if measurements isActive == false and set to isDisplayable == false
		for (int i = 0; i < recordSet.size(); ++i) {
			// since actual record names can differ from device configuration measurement names, match by ordinal
			record = recordSet.get(i);
			measurement = this.getMeasurement(channelConfigNumber, i);
			if (MC3000.log.isLoggable(java.util.logging.Level.FINE)) MC3000.log.log(java.util.logging.Level.FINE, record.getName() + " = " + measurementNames[i]); //$NON-NLS-1$

			// update active state and displayable state if configuration switched with other names
			if (record.isActive() != measurement.isActive()) {
				record.setActive(measurement.isActive());
				record.setVisible(measurement.isActive());
				record.setDisplayable(measurement.isActive());
				if (MC3000.log.isLoggable(java.util.logging.Level.FINE)) MC3000.log.log(java.util.logging.Level.FINE, "switch " + record.getName() + " to " + measurement.isActive()); //$NON-NLS-1$ //$NON-NLS-2$
			}
			if (includeReasonableDataCheck) {
				record.setDisplayable(record.hasReasonableData() && measurement.isActive());
				if (MC3000.log.isLoggable(java.util.logging.Level.FINE)) MC3000.log.log(java.util.logging.Level.FINE, record.getName() + " hasReasonableData " + record.hasReasonableData()); //$NON-NLS-1$ 
			}

			if (record.isActive() && record.isDisplayable()) {
				if (MC3000.log.isLoggable(java.util.logging.Level.FINE)) MC3000.log.log(java.util.logging.Level.FINE, "add to displayable counter: " + record.getName()); //$NON-NLS-1$
				++displayableCounter;
			}
		}
		if (MC3000.log.isLoggable(java.util.logging.Level.FINE)) MC3000.log.log(java.util.logging.Level.FINE, "displayableCounter = " + displayableCounter); //$NON-NLS-1$
		recordSet.setConfiguredDisplayable(displayableCounter);
		this.setChangePropery(configChanged); //reset configuration change indicator to previous value, do not vote automatic configuration change at all
	}

	/**
	 * function to calculate values for inactive records, data not readable from device
	 * if calculation is done during data gathering this can be a loop switching all records to displayable
	 * for calculation which requires more effort or is time consuming it can call a background thread, 
	 * target is to make sure all data point not coming from device directly are available and can be displayed 
	 */
	@Override
	public void makeInActiveDisplayable(RecordSet recordSet) {
		//add implementation where data point are calculated
		//do not forget to make record displayable -> record.setDisplayable(true);

		//for the moment there are no calculations necessary
		//String[] recordNames = recordSet.getRecordNames();
		//for (int i=0; i<recordNames.length; ++i) {
		//	MeasurementType measurement = this.getMeasurement(recordSet.getChannelConfigNumber(), i);
		//	if (measurement.isCalculation()) {
		//		log.log(Level.FINE, "do calculation for " + recordNames[i]); //$NON-NLS-1$
		//	}
		//}
	}

	/**
	 * @return the device communication port
	 */
	@Override
	public MC3000UsbPort getCommunicationPort() {
		return this.usbPort;
	}

	/**
	 * query for all the property keys this device has in use
	 * - the property keys are used to filter serialized properties form OSD data file
	 * @return [offset, factor, reduction, number_cells, prop_n100W, ...]
	 */
	@Override
	public String[] getUsedPropertyKeys() {
		return new String[] { IDevice.OFFSET, IDevice.FACTOR, IDevice.REDUCTION };
	}

	/**
	 * method toggle open close serial port or start/stop gathering data from device
	 * if the device does not use serial port communication this place could be used for other device related actions which makes sense here
	 * as example a file selection dialog could be opened to import serialized ASCII data 
	 */
	@Override
	public void open_closeCommPort() {
		if (this.usbPort != null) {
			if (!this.usbPort.isConnected()) {
				try {
					Channel activChannel = Channels.getInstance().getActiveChannel();
					if (activChannel != null) {
						this.getDialog().dataGatherThread = new GathererThread(this.application, this, this.usbPort, activChannel.getNumber(), this.getDialog());
						try {
							if (this.getDialog().dataGatherThread != null && this.usbPort.isConnected()) {
								this.systemSettings = new MC3000.SystemSettings(this.usbPort.getSystemSettings(this.getDialog().dataGatherThread.getUsbInterface()));
								WaitTimer.delay(100);
								this.usbPort.startProcessing(this.getDialog().dataGatherThread.getUsbInterface());
								WaitTimer.delay(100);
								this.getDialog().dataGatherThread.start();
							}
						}
						catch (Throwable e) {
							MC3000.log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
							if (this.getDialog().dataGatherThread != null) 
								this.usbPort.stopProcessing(this.getDialog().dataGatherThread.getUsbInterface());
						}
					}
				}
				catch (UsbClaimException e) {
					MC3000.log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
					this.application.openMessageDialog(this.dialog.getDialogShell(),
							Messages.getString(gde.messages.MessageIds.GDE_MSGE0051, new Object[] { e.getClass().getSimpleName() + GDE.STRING_BLANK_COLON_BLANK + e.getMessage() }));
					try {
						if (this.usbPort != null && this.usbPort.isConnected()) this.usbPort.closeUsbPort(null);
					}
					catch (UsbException ex) {
						MC3000.log.log(java.util.logging.Level.SEVERE, ex.getMessage(), ex);
					}
				}
				catch (UsbException e) {
					MC3000.log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
					if (this.getDialog().dataGatherThread != null) 
						this.usbPort.stopProcessing(this.getDialog().dataGatherThread.getUsbInterface());
					this.application.openMessageDialog(this.dialog.getDialogShell(),
							Messages.getString(gde.messages.MessageIds.GDE_MSGE0051, new Object[] { e.getClass().getSimpleName() + GDE.STRING_BLANK_COLON_BLANK + e.getMessage() }));
					try {
						if (this.usbPort != null && this.usbPort.isConnected()) this.usbPort.closeUsbPort(null);
					}
					catch (UsbException ex) {
						MC3000.log.log(java.util.logging.Level.SEVERE, ex.getMessage(), ex);
					}
				}
				catch (ApplicationConfigurationException e) {
					MC3000.log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
					if (this.getDialog().dataGatherThread != null)
						this.usbPort.stopProcessing(this.getDialog().dataGatherThread.getUsbInterface());
					this.application.openMessageDialog(this.dialog.getDialogShell(), Messages.getString(gde.messages.MessageIds.GDE_MSGE0010));
					this.application.getDeviceSelectionDialog().open();
				}
				catch (Throwable e) {
					MC3000.log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
					if (this.getDialog().dataGatherThread != null)
						this.usbPort.stopProcessing(this.getDialog().dataGatherThread.getUsbInterface());
				}
			}
			else {
				if (this.getDialog().dataGatherThread != null) {
					this.usbPort.stopProcessing(this.getDialog().dataGatherThread.getUsbInterface());
					this.getDialog().dataGatherThread.stopDataGatheringThread(false, null);
				}
				//if (this.getDialog().boundsComposite != null && !this.getDialog().isDisposed()) this.getDialog().boundsComposite.redraw();
				try {
					WaitTimer.delay(1000);
					if (this.usbPort != null && this.usbPort.isConnected()) this.usbPort.closeUsbPort(null);
				}
				catch (UsbException e) {
					MC3000.log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
				}
			}
		}
	}

	/**
	 * add record data size points from LogView data stream to each measurement, if measurement is calculation 0 will be added
	 * adaption from LogView stream data format into the device data buffer format is required
	 * do not forget to call makeInActiveDisplayable afterwards to calculate the missing data
	 * this method is more usable for real logger, where data can be stored and converted in one block
	 * @param recordSet
	 * @param dataBuffer
	 * @param recordDataSize
	 * @param doUpdateProgressBar
	 * @throws DataInconsitsentException 
	 */
	@Override
	public synchronized void addConvertedLovDataBufferAsRawDataPoints(RecordSet recordSet, byte[] dataBuffer, int recordDataSize, boolean doUpdateProgressBar) throws DataInconsitsentException {
		//not implemented
	}

	/**
	 * convert the device bytes into raw values, no calculation will take place here, see translateValue reverseTranslateValue
	 * inactive or to be calculated data point are filled with 0 and needs to be handles after words
	 * @param points pointer to integer array to be filled with converted data
	 * @param dataBuffer byte arrax with the data to be converted
	 */
	@Override
	public int[] convertDataBytes(int[] points, byte[] dataBuffer) {
		int chargeCorrection = (this.getProcessingMode(dataBuffer) == 3  
				|| ((this.getProcessingMode(dataBuffer) == 1 || this.getProcessingMode(dataBuffer) == 2 || this.getProcessingMode(dataBuffer) == 4) && this.getProcessingStatus(dataBuffer) == 2)) 
				? -1 : 1;
		//0=Voltage 1=Current 2=Capacity 3=power 4=Energy 5=Temperature 6=Resistence
		points[0] = DataParser.parse2Short(dataBuffer[9], dataBuffer[8]) * 1000;
		points[1] = DataParser.parse2Short(dataBuffer[11], dataBuffer[10]) * 1000 * chargeCorrection;
		points[2] = DataParser.parse2Short(dataBuffer[13], dataBuffer[12]) * 1000;
		if (this.systemSettings != null && this.systemSettings.getFirmwareVersionAsInt() <= 105) {
			points[3] = Double.valueOf(points[0] / 1000.0 * points[1] / 1000.0 * chargeCorrection).intValue(); // power U*I [W]
			switch (dataBuffer[1]) {
			case 0: //add up energy
				points[4] += Double.valueOf((points[0] / 1000.0 * points[1] / 1000.0 * chargeCorrection)/3600.0 + 0.5).intValue();
				break;
			case 1: // reset energy
				points[4] = 0;
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "reset Energy");
				break;
			default: // keep energy untouched
			case -1: // keep energy untouched
				points[4] = points[4];
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "untouche Energy");
				break;
			}
		} else {//firmware 1.05+ Energy and power comes direct from the device			
			points[3] = DataParser.parse2Short(dataBuffer[23], dataBuffer[22]) * 1000;
			points[4] = DataParser.parse2Short(dataBuffer[21], dataBuffer[20]) * 1000;
		}
		if (this.systemSettings != null && this.systemSettings.getFirmwareVersionAsInt() >= 111) {
			points[2] += dataBuffer[24] * 100; //capacity decimal
		}
		points[5] = DataParser.parse2Short(dataBuffer[15], dataBuffer[14]) * 1000;
		points[6] = DataParser.parse2Short(dataBuffer[17], dataBuffer[16]) * 1000;
		points[7] = DataParser.parse2Short(dataBuffer[19], dataBuffer[18]) * 1000;

		return points;
	}

	/**
	 * add record data size points from file stream to each measurement
	 * it is possible to add only none calculation records if makeInActiveDisplayable calculates the rest
	 * do not forget to call makeInActiveDisplayable afterwards to calculate the missing data
	 * since this is a long term operation the progress bar should be updated to signal business to user 
	 * @param recordSet
	 * @param dataBuffer
	 * @param recordDataSize
	 * @param doUpdateProgressBar
	 * @throws DataInconsitsentException 
	 */
	@Override
	public void addDataBufferAsRawDataPoints(RecordSet recordSet, byte[] dataBuffer, int recordDataSize, boolean doUpdateProgressBar) throws DataInconsitsentException {
		int dataBufferSize = GDE.SIZE_BYTES_INTEGER * recordSet.getNoneCalculationRecordNames().length;
		byte[] convertBuffer = new byte[dataBufferSize];
		int[] points = new int[recordSet.size()];
		String sThreadId = String.format("%06d", Thread.currentThread().getId()); //$NON-NLS-1$
		int progressCycle = 0;
		if (doUpdateProgressBar) this.application.setProgress(progressCycle, sThreadId);

		for (int i = 0; i < recordDataSize; i++) {
			MC3000.log.log(java.util.logging.Level.FINER, i + " i*dataBufferSize+timeStampBufferSize = " + i * dataBufferSize); //$NON-NLS-1$
			System.arraycopy(dataBuffer, i * dataBufferSize, convertBuffer, 0, dataBufferSize);

			for (int j = 0, k = 0; j < points.length; j++, k += 4) {
				//0=Voltage 1=Current 2=Capacity 3=Power 4=Energy 5=Teperature 6=Resistance 7=SysTemperature
				points[j] = (((convertBuffer[k] & 0xff) << 24) + ((convertBuffer[k + 1] & 0xff) << 16) + ((convertBuffer[k + 2] & 0xff) << 8) + ((convertBuffer[k + 3] & 0xff) << 0));
			}
			recordSet.addPoints(points);

			if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle * 2500) / recordDataSize), sThreadId);
		}
		if (doUpdateProgressBar) this.application.setProgress(100, sThreadId);
		recordSet.syncScaleOfSyncableRecords();
	}

	/**
	 * function to prepare a data table row of record set while translating available measurement values
	 * @return pointer to filled data table row with formated values
	 */
	@Override
	public String[] prepareDataTableRow(RecordSet recordSet, String[] dataTableRow, int rowIndex) {
		try {
			int index = 0;
			for (final Record record : recordSet.getVisibleAndDisplayableRecordsForTable()) {
				double offset = record.getOffset(); // != 0 if curve has an defined offset
				double reduction = record.getReduction();
				double factor = record.getFactor(); // != 1 if a unit translation is required
				dataTableRow[index + 1] = record.getDecimalFormat().format((offset + ((record.realGet(rowIndex) / 1000.0) - reduction) * factor));
				++index;
			}
		}
		catch (RuntimeException e) {
			MC3000.log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
		}
		return dataTableRow;
	}

	/**
	 * function to translate measured values from a device to values represented
	 * this function should be over written by device and measurement specific algorithm
	 * @return double of device dependent value
	 */
	@Override
	public double translateValue(Record record, double value) {
		double factor = record.getFactor(); // != 1 if a unit translation is required
		double offset = record.getOffset(); // != 0 if a unit translation is required
		double reduction = record.getReduction(); // != 0 if a unit translation is required

		double newValue = (value - reduction) * factor + offset;

		MC3000.log.log(java.util.logging.Level.FINE, "for " + record.getName() + " in value = " + value + " out value = " + newValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return newValue;
	}

	/**
	 * function to reverse translate measured values from a device to values represented
	 * this function should be over written by device and measurement specific algorithm
	 * @return double of device dependent value
	 */
	@Override
	public double reverseTranslateValue(Record record, double value) {
		double factor = record.getFactor(); // != 1 if a unit translation is required
		double offset = record.getOffset(); // != 0 if a unit translation is required
		double reduction = record.getReduction(); // != 0 if a unit translation is required

		double newValue = (value - offset) / factor + reduction;

		MC3000.log.log(java.util.logging.Level.FINE, "for " + record.getName() + " in value = " + value + " out value = " + newValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return newValue;
	}

	/**
	 * query device for specific smoothing index
	 * 0 do nothing at all
	 * 1 current drops just a single peak
	 * 2 current drop more or equal than 2 measurements 
	 */
	@Override
	public int getCurrentSmoothIndex() {
		return 1;
	}

	/**
	 * check if one of the outlet channels are in processing mode
	 * STATUS:     0=standby 1=charge 2=discharge 3=resting 4=finish 0x80--0xff：error code
	 * @param outletNum
	 * @param dataBuffer
	 * @return true if channel 1 is active 
	 */
	public boolean isProcessing(final int outletNum, final byte[] dataBuffer) {
		if (MC3000.log.isLoggable(java.util.logging.Level.FINE)) MC3000.log.log(java.util.logging.Level.FINE, "isProcessing = " + dataBuffer[5]);

		//firmware 1.05+ power and energy comes direct from device
//		if (this.resetEnergy[outletNum-1] != dataBuffer[5] || dataBuffer[5] > 2)
//			if (dataBuffer[5] > 2)
//				dataBuffer[1] = -1; //keep energy
//			else
//				dataBuffer[1] = 1;  //reset energy
//		else
//			dataBuffer[1] = 0;	//add up energy
//		
//		this.resetEnergy[outletNum-1] = dataBuffer[5];
		
		if (this.settings.isReduceChargeDischarge()) 
			return dataBuffer[5] > 0 && dataBuffer[5] < 3;
		return dataBuffer[5] > 0;
	}

	/**
	 * STATUS:     0=standby 1=charge 2=discharge 3=resting 4=finish 0x80--0xff：error code
	 * @param dataBuffer
	 * @return
	 */
	public int getProcessingStatus(final byte[] dataBuffer) {
		return dataBuffer[5];
	}

	/**
	 * STATUS:     0=standby 1=charge 2=discharge 3=resting 4=finish 0x80--0xff：error code
	 * @param dataBuffer
	 * @return
	 */
	public boolean isProcessingStatusStandByOrFinished(final byte[] dataBuffer) {
		return dataBuffer[5]==0 || dataBuffer[5]==4;
	}

	/**
	 * STATUS:     0=standby 1=charge 2=discharge 3=resting 4=finish 0x80--0xff：error code
	 * @param dataBuffer
	 * @return
	 */
	public String getProcessingStatusName(final byte[] dataBuffer) {
		return this.settings.isContinuousRecordSet() ? Messages.getString(MessageIds.GDE_MSGT3606) : this.STATUS_MODE[dataBuffer[5]];
	}

	/**
	 * battery type:  0:LiIon 1:LiFe 2:LiHV 3:NiMH 4:NiCd 5:NiZn 6:Eneloop 7:RAM
	 * @param dataBuffer
	 * @return
	 */
	public String getProcessingBatteryType(final byte[] dataBuffer) {
		return this.BATTERY_TYPE[dataBuffer[2]];
	}

	/**
	 * query the processing mode, main modes are charge/discharge, make sure the data buffer contains at index 15,16 the processing modes
	 * Mode LI battery： 	0=CHARGE 1=REFRESH 2=STORAGE 3=DISCHARGE 4=CYCLE
	 * Mode Ni battery:		0=CHARGE 1=REFRESH 2=PAUSE     3=DISCHARGE 4=CYCLE
	 * Mode Zn battery:		0=CHARGE 1=REFRESH 2=DISCHARGE 3=CYCLE
	 * Mode RAM battery:	0=CHARGE 1=REFRESH 2=DISCHARGE 3=CYCLE
	 * @param dataBuffer 
	 * @return
	 */
	public int getProcessingMode(final byte[] dataBuffer) {
		return dataBuffer[3];
	}

	/**
	 * query battery type
	 * battery type:  0:LiIon 1:LiFe 2:LiHV 3:NiMH 4:NiCd 5:NiZn 6:Eneloop 7:RAM
	 * @param dataBuffer
	 * @return
	 */
	public int getBatteryType(byte[] dataBuffer) {
		return dataBuffer[2];
	}

	/**
	 * query the processing type name
	 * @param dataBuffer
	 * @return string of mode
	 */
	public String getProcessingTypeName(byte[] dataBuffer) {
		String processTypeName = GDE.STRING_EMPTY;
		//Mode LI battery： 		0=CHARGE 1=REFRESH 2=STORAGE 	 3=DISCHARGE 4=CYCLE
		//Mode Ni battery:		0=CHARGE 1=REFRESH 2=PAUSE     3=DISCHARGE 4=CYCLE
		//Mode Zn battery:		0=CHARGE 1=REFRESH 2=DISCHARGE 3=CYCLE
		//Mode RAM battery:		0=CHARGE 1=REFRESH 2=DISCHARGE 3=CYCLE

		// battery type:     0:LiIon       1:LiFe        2:LiHV        3:NiMH        4:NiCd        5:NiZn        6:Eneloop
		switch (this.getBatteryType(dataBuffer)) {
		case 0: //LiIon
		case 1: //LiFe
		case 2: //LiHv
			processTypeName = this.USAGE_MODE_LI[this.getProcessingMode(dataBuffer)];
			break;
		case 3: //NiMH
		case 4: //NiCD
		case 6: //Eneloop
			processTypeName = this.USAGE_MODE_NI[this.getProcessingMode(dataBuffer)];
			break;
		case 5: //NiZn
		case 7: //RAM
			processTypeName = this.USAGE_MODE_ZN[this.getProcessingMode(dataBuffer)];
			break;
		}
		return this.settings.isContinuousRecordSet() ? Messages.getString(MessageIds.GDE_MSGT3606) : processTypeName;
	}

	/**
	 * query the cycle number of the given outlet channel
	 * @param outletNum
	 * @param dataBuffer
	 * @return
	 */
	public int getCycleNumber(int outletNum, byte[] dataBuffer) {
		return dataBuffer[4];
	}

	/**
	 * set the measurement ordinal of the values displayed in cell voltage window underneath the cell voltage bars
	 * set value of -1 to suppress this measurement
	 */
	@Override
	public int[] getCellVoltageOrdinals() {
		//0=Voltage 1=Current 2=Capacity 3=power 4=Energy 5=Temperature 6=Resistance

		// Combi
		//0=Voltage 1=Voltage 2=Voltage 3=Voltage 4=Current 5=Current 6=Current 7=Current 8=Capacity 9=Capacity 10=Capacity 11=Capacity
		//12=Temperature 13=Temperature 14=Temperature 15=Temperature 16=Resistance 17=Resistance 18=Resistance 19=Resistance
		switch (Channels.getInstance().getActiveChannelNumber()) {
		case 1:
		case 2:
		case 3:
		case 4:
			return new int[] { 0, 2 };
		case 5:
		default:
			return new int[] { -1, -1 };
		}
	}

	/**
	 * @return string containing firmware : major.minor
	 */
	public String getFirmwareString() {
		return this.systemSettings.getFirmwareVersion();
	}
	
	/**
	 * @param slotNumber
	 * @param usbInterface
	 * @return slot related program/memory number
	 */
	public byte getBatteryMemoryNumber(final int slotNumber, UsbInterface usbInterface) {
		try {
			this.systemSettings = new SystemSettings(this.usbPort.getSystemSettings(usbInterface));
		}
		catch (Exception e) {
			return 0x00;
		}
		switch (slotNumber) {
		case 1:
			return this.systemSettings.getSlot1programmNumber();
		case 2:
			return this.systemSettings.getSlot2programmNumber();
		case 3:
			return this.systemSettings.getSlot3programmNumber();
		case 4:
			return this.systemSettings.getSlot4programmNumber();
		default:
			return 0x00;
		}
	}
}
