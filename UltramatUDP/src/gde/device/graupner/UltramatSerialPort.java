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
    
    Copyright (c) 2008,2009,2010,2011 Winfried Bruegmann
****************************************************************************************/
package gde.device.graupner;

import gde.GDE;
import gde.comm.DeviceCommPort;
import gde.comm.DeviceSerialPortImpl;
import gde.device.DeviceConfiguration;
import gde.exception.SerialPortException;
import gde.exception.TimeOutException;
import gde.log.Level;
import gde.ui.DataExplorer;
import gde.utils.Checksum;
import gde.utils.StringHelper;

import java.io.IOException;
import java.util.Vector;
import java.util.logging.Logger;

/**
 * eStation serial port implementation
 * @author Winfried Brügmann
 */
public class UltramatSerialPort extends DeviceCommPort {
	final static String	$CLASS_NAME										= UltramatSerialPort.class.getName();
	final static Logger	log														= Logger.getLogger(UltramatSerialPort.$CLASS_NAME);

	final static byte[]	RESET_CONFIG									= new byte[] { DeviceSerialPortImpl.FF, 0x41, 0x37, 0x30, 0x30, 0x30, 0x30, 0x44, 0x38, DeviceSerialPortImpl.CR };	//1000 1111
	final static byte[]	RESET													= new byte[] { DeviceSerialPortImpl.FF, 0x43, 0x30, 0x30, 0x30, 0x30, 0x30, 0x44, 0x33, DeviceSerialPortImpl.CR };	//1000 1111
	final static byte[]	READ_MEMORY_NAME							= new byte[] { DeviceSerialPortImpl.FF, '8', '0', '0', '0', '0', '0', '0', '0', DeviceSerialPortImpl.CR };					//1000 0000
	final static byte[]	WRITE_MEMORY_NAME							= new byte[] { DeviceSerialPortImpl.FF, '0', '0' };																																//0000 0000
	final static byte[]	READ_MEMORY_SETUP							= new byte[] { DeviceSerialPortImpl.FF, '8', '1', '0', '0', '0', '0', '0', '0', DeviceSerialPortImpl.CR };					//1000 0001
	final static byte[]	WRITE_MEMORY_SETUP						= new byte[] { DeviceSerialPortImpl.FF, '0', '1' };																																//0000 0001
	final static byte[]	READ_MEMORY_STEP_CHARGE_SETUP	= new byte[] { DeviceSerialPortImpl.FF, '8', '2', '0', '0', '0', '0', '0', '0', DeviceSerialPortImpl.CR };					//1000 0010
	final static byte[]	WRITE_STEP_CHARGE_SETUP				= new byte[] { DeviceSerialPortImpl.FF, '0', '2' };																																//0000 0010
	final static byte[]	READ_MEMORY_CYCLE_DATA				= new byte[] { DeviceSerialPortImpl.FF, '8', '3', '0', '0', '0', '0', '0', '0', DeviceSerialPortImpl.CR };					//1000 0011
	final static byte[]	WRITE_CYCLE_DATA							= new byte[] { DeviceSerialPortImpl.FF, '0', '3' };																																//0000 0011
	final static byte[]	READ_MEMORY_TRACE_DATA				= new byte[] { DeviceSerialPortImpl.FF, '8', '4', '0', '0', '0', '0', '0', '0', DeviceSerialPortImpl.CR };					//1000 0100
	final static byte[]	WRITE_TRACE_DATA							= new byte[] { DeviceSerialPortImpl.FF, '0', '4' };																																//0000 0100
	final static byte[]	READ_TIRE_HEATER							= new byte[] { DeviceSerialPortImpl.FF, '8', '5', '0', '0', '0', '0', '0', '0', DeviceSerialPortImpl.CR };					//1000 0101
	final static byte[]	WRITE_TIRE_HEATER							= new byte[] { DeviceSerialPortImpl.FF, '0', '5' };																																//0000 0101
	final static byte[]	READ_MOTOR_RUN								= new byte[] { DeviceSerialPortImpl.FF, '8', '6', '0', '0', '0', '0', '0', '0', DeviceSerialPortImpl.CR };					//1000 0110
	final static byte[]	WRITE_MOTOR_RUN								= new byte[] { DeviceSerialPortImpl.FF, '0', '6' };																																//0000 0110
	final static byte[]	READ_CHANNEL_SETUP						= new byte[] { DeviceSerialPortImpl.FF, '8', '7', '0', '0', '0', '0', '0', '0', DeviceSerialPortImpl.CR };					//1000 0111
	final static byte[]	WRITE_CHANNEL_SETUP						= new byte[] { DeviceSerialPortImpl.FF, '0', '7' };																																//0000 0111
	final static byte[]	READ_DEVICE_IDENTIFIER_NAME		= new byte[] { DeviceSerialPortImpl.FF, '8', '8', '0', '0', '0', '0', '0', '0', DeviceSerialPortImpl.CR };					//1000 1000
	final static byte[]	WRITE_DEVICE_IDENTIFIER_NAME	= new byte[] { DeviceSerialPortImpl.FF, '0', '8' };																																//0000 1000
	final static byte[]	READ_GRAPHICS_DATA						= new byte[] { DeviceSerialPortImpl.FF, '8', '9', '0', '0', '0', '0', '0', '0', DeviceSerialPortImpl.CR };					//1000 1001
	final static byte[]	WRITE_GRAPHICS_DATA						= new byte[] { DeviceSerialPortImpl.FF, '0', '9' };																																//0000 1001

	final static int		SIZE_MEMORY_SETUP							= 28;
	final static int		SIZE_MEMORY_STEP_CHARGE_SETUP	= 20;
	final static int		SIZE_MEMORY_TRACE							= 6;
	final static int		SIZE_MEMORY_CYCLE							= 121;
	static int					SIZE_CHANNEL_1_SETUP					= 16;
	final static int		SIZE_CHANNEL_2_SETUP					= 4;
	final static int		SIZE_TIRE_HEATER_SETUP				= 8;
	final static int		SIZE_MOTOR_RUN_SETUP					= 17;
	
	final static int    xferErrorLimit								= 15;

	boolean							isInSync											= false;
	boolean							isDataMissmatchWarningWritten	= false;
	int									dataCheckSumOffset						= 0;
	boolean							isCmdMissmatchWarningWritten	= false;
	int									cmdCheckSumOffset							= 0;

	/**
	 * constructor of default implementation
	 * @param currentDeviceConfig - required by super class to initialize the serial communication port
	 * @param currentApplication - may be used to reflect serial receive,transmit on/off status or overall status by progress bar 
	 */
	public UltramatSerialPort(Ultramat currentDevice, DataExplorer currentApplication) {
		super(currentDevice, currentApplication);
		switch (currentDevice.getDeviceTypeIdentifier()) {
		case UltraDuoPlus45:
			UltramatSerialPort.SIZE_CHANNEL_1_SETUP = 19; // additional supply input 2 voltage, current
			break;

		case UltraDuoPlus60:
		default:
			UltramatSerialPort.SIZE_CHANNEL_1_SETUP = 16;
			break;
		}
	}

	/**
	 * constructor for testing purpose
	 * @param currentDeviceConfig - required by super class to initialize the serial communication port
	 * @param currentApplication - may be used to reflect serial receive,transmit on/off status or overall status by progress bar 
	 */
	public UltramatSerialPort(DeviceConfiguration deviceConfiguration) {
		super(deviceConfiguration);
	}

	/**
	 * method to gather data from device, implementation is individual for device
	 * @return byte array containing gathered data - this can individual specified per device
	 * @throws IOException
	 */
	public synchronized byte[] getData(boolean checkBeginEndSignature) throws Exception {
		final String $METHOD_NAME = "getData"; //$NON-NLS-1$
		byte[] data = new byte[Math.abs(this.device.getDataBlockSize())];
		byte[] answer = new byte[] { 0x00 };

		try {

			answer = new byte[data.length];
			answer = this.read(data, 3000);
			// synchronize received data to DeviceSerialPortImpl.FF of sent data 
			while (answer[0] != DeviceSerialPortImpl.FF) {
				this.isInSync = false;
				for (int i = 1; i < answer.length; i++) {
					if (answer[i] == DeviceSerialPortImpl.FF) {
						System.arraycopy(answer, i, data, 0, data.length - i);
						answer = new byte[i];
						answer = this.read(answer, 1000);
						System.arraycopy(answer, 0, data, data.length - i, i);
						this.isInSync = true;
						log.logp(Level.FINE, UltramatSerialPort.$CLASS_NAME, $METHOD_NAME, "----> receive sync finished"); //$NON-NLS-1$
						break; //sync
					}
				}
				if (this.isInSync) break;

				this.addXferError();
				log.logp(Level.WARNING, UltramatSerialPort.$CLASS_NAME, $METHOD_NAME,
						"=====> unable to synchronize received data, number of errors = " + this.getXferErrors()); //$NON-NLS-1$
				if (this.getXferErrors() > xferErrorLimit) throw new SerialPortException("Number of tranfer error exceed the acceptable limit of " + xferErrorLimit);
				this.write(RESET);
				answer = new byte[data.length];
				answer = this.read(answer, 3000);
			}
			log.logp(Level.FINE, UltramatSerialPort.$CLASS_NAME, $METHOD_NAME, StringHelper.convert2CharString(data));

			if (checkBeginEndSignature  && !(data[0] == DeviceSerialPortImpl.FF && data[data.length - 1] == DeviceSerialPortImpl.CR)) {
				this.addXferError();
				log.logp(Level.WARNING, UltramatSerialPort.$CLASS_NAME, $METHOD_NAME,
						"=====> data start or end does not match, number of errors = " + this.getXferErrors()); //$NON-NLS-1$
				if (this.getXferErrors() > xferErrorLimit) throw new SerialPortException("Number of tranfer error exceed the acceptable limit of " + xferErrorLimit);
				this.write(RESET);
				data = getData(true);
			}

			if (checkBeginEndSignature && !isChecksumOK(data)) {
				this.addXferError();
				log.logp(Level.WARNING, UltramatSerialPort.$CLASS_NAME, $METHOD_NAME, "=====> checksum error occured, number of errors = " + this.getXferErrors()); //$NON-NLS-1$
				if (this.getXferErrors() > xferErrorLimit) throw new SerialPortException("Number of tranfer error exceed the acceptable limit of " + xferErrorLimit);
				this.write(RESET);
				data = getData(true);
			}
		}
		catch (Exception e) {
			if (!(e instanceof TimeOutException)) {
				log.logp(Level.SEVERE, UltramatSerialPort.$CLASS_NAME, $METHOD_NAME, e.getMessage(), e);
			}
			throw e;
		}
		return data;
	}

	/**
	 * read the device user name
	 * @return string configured in device
	 * @throws IOException
	 * @throws TimeOutException
	 * @throws SerialPortException 
	 */
	public synchronized String readDeviceUserName() throws IOException, TimeOutException, SerialPortException {
		byte[] answer = this.readConfigData(UltramatSerialPort.READ_DEVICE_IDENTIFIER_NAME, 23, 2);
		return String.format(DeviceSerialPortImpl.FORMAT_16_CHAR, answer[1], answer[2], answer[3], answer[4], answer[5], answer[6], answer[7], answer[8], answer[9], answer[10], answer[11], answer[12],
				answer[13], answer[14], answer[15], answer[16]);
	}

	/**
	 * read the outlet channel setup data
	 * @param channelNumber
	 * @return values integer array
	 * @throws IOException
	 * @throws TimeOutException
	 * @throws SerialPortException 
	 */
	public synchronized String readChannelData(int channelNumber) throws IOException, TimeOutException, SerialPortException {
		if (channelNumber == 1) {
			return new String(this.readConfigData(UltramatSerialPort.READ_CHANNEL_SETUP, UltramatSerialPort.SIZE_CHANNEL_1_SETUP * 4 + 7, channelNumber)).substring(1,
					UltramatSerialPort.SIZE_CHANNEL_1_SETUP * 4 + 1);
		}
		else if (channelNumber == 2) {
			return new String(this.readConfigData(UltramatSerialPort.READ_CHANNEL_SETUP, UltramatSerialPort.SIZE_CHANNEL_2_SETUP * 4 + 7, channelNumber)).substring(1,
					UltramatSerialPort.SIZE_CHANNEL_2_SETUP * 4 + 1);
		}
		return GDE.STRING_EMPTY;
	}

	/**
	 * read the name of memory with given number 0 - 39 for memory 01 - 40
	 * @param number
	 * @return
	 * @throws IOException
	 * @throws TimeOutException
	 * @throws SerialPortException 
	 */
	public synchronized String readMemoryName(int number) throws IOException, TimeOutException, SerialPortException {
		byte[] answer = this.readConfigData(UltramatSerialPort.READ_MEMORY_NAME, 23, number);
		return String.format(DeviceSerialPortImpl.FORMAT_16_CHAR, answer[1], answer[2], answer[3], answer[4], answer[5], answer[6], answer[7], answer[8], answer[9], answer[10], answer[11], answer[12],
				answer[13], answer[14], answer[15], answer[16]);
	}

	/**
	 * read the memory setup values for given memory number
	 * @param number
	 * @return
	 * @throws IOException
	 * @throws TimeOutException
	 * @throws SerialPortException 
	 */
	public synchronized String readMemorySetup(int number) throws IOException, TimeOutException, SerialPortException {
		return new String(this.readConfigData(UltramatSerialPort.READ_MEMORY_SETUP, UltramatSerialPort.SIZE_MEMORY_SETUP * 4 + 7, number)).substring(1, UltramatSerialPort.SIZE_MEMORY_SETUP * 4 + 1);
	}

	/**
	 * read the memory trace data for given memory number
	 * @param number
	 * @return
	 * @throws IOException
	 * @throws TimeOutException
	 * @throws SerialPortException 
	 */
	public synchronized String readMemoryTrace(int number) throws IOException, TimeOutException, SerialPortException {
		return new String(this.readConfigData(UltramatSerialPort.READ_MEMORY_TRACE_DATA, UltramatSerialPort.SIZE_MEMORY_TRACE * 4 + 7, number)).substring(1, UltramatSerialPort.SIZE_MEMORY_TRACE * 4 + 1);
	}

	/**
	 * read the memory cycle data for given memory number
	 * @param number
	 * @return
	 * @throws IOException
	 * @throws TimeOutException
	 * @throws SerialPortException 
	 */
	public synchronized String readMemoryCycle(int number) throws IOException, TimeOutException, SerialPortException {
		return new String(this.readConfigData(UltramatSerialPort.READ_MEMORY_CYCLE_DATA, UltramatSerialPort.SIZE_MEMORY_CYCLE * 4 + 7, number)).substring(1, UltramatSerialPort.SIZE_MEMORY_CYCLE * 4 + 1);
	}

	/**
	 * read the memory step charge data for given memory number
	 * @param number
	 * @return
	 * @throws IOException
	 * @throws TimeOutException
	 * @throws SerialPortException 
	 */
	public synchronized String readMemoryStepChargeSetup(int number) throws IOException, TimeOutException, SerialPortException {
		return new String(this.readConfigData(UltramatSerialPort.READ_MEMORY_STEP_CHARGE_SETUP, UltramatSerialPort.SIZE_MEMORY_STEP_CHARGE_SETUP * 4 + 7, number)).substring(1,
				UltramatSerialPort.SIZE_MEMORY_STEP_CHARGE_SETUP * 4 + 1);
	}

	/**
	 * read configuration data according given type and expected data size for a given index (channel, memory number, ...)
	 * @param type
	 * @param expectedDataSize
	 * @param index
	 * @return byte array containing the requested configuration data
	 * @throws IOException
	 * @throws TimeOutException
	 * @throws SerialPortException 
	 */
	public synchronized byte[] readConfigData(byte[] type, int expectedDataSize, int index) throws IOException, TimeOutException, SerialPortException {
		final String $METHOD_NAME = "readConfigData"; //$NON-NLS-1$
		log.logp(Level.FINEST, UltramatSerialPort.$CLASS_NAME, $METHOD_NAME, "entry"); //$NON-NLS-1$
		byte[] readBuffer = new byte[expectedDataSize];

		if (this.isConnected()) {
			byte[] writeBuffer = type;
			byte[] num = String.format("%02X", index).getBytes(); //$NON-NLS-1$
			System.arraycopy(num, 0, writeBuffer, 3, 2);
			byte[] checkSum = getChecksum(writeBuffer);
			System.arraycopy(checkSum, 0, writeBuffer, 5, 4);
			log.logp(Level.FINE, UltramatSerialPort.$CLASS_NAME, $METHOD_NAME, "write = " + StringHelper.convert2CharString(writeBuffer)); //$NON-NLS-1$

			this.write(writeBuffer);
			byte[] answer = this.read(readBuffer, 3000);
			while (answer[0] != DeviceSerialPortImpl.FF) {
				this.isInSync = false;
				for (int i = 1; i < answer.length; i++) {
					if (answer[i] == DeviceSerialPortImpl.FF) {
						System.arraycopy(answer, i, readBuffer, 0, readBuffer.length - i);
						answer = new byte[i];
						answer = this.read(answer, 1000);
						System.arraycopy(answer, 0, readBuffer, readBuffer.length - i, i);
						this.isInSync = true;
						log.logp(Level.FINE, UltramatSerialPort.$CLASS_NAME, $METHOD_NAME, "----> receive sync finished"); //$NON-NLS-1$
						break; //sync
					}
				}
				if (this.isInSync) break;

				this.addXferError();
				log.logp(Level.WARNING, UltramatSerialPort.$CLASS_NAME, $METHOD_NAME,
						"=====> unable to synchronize received data, number of errors = " + this.getXferErrors()); //$NON-NLS-1$
				if (this.getXferErrors() > xferErrorLimit) throw new SerialPortException("Number of tranfer error exceed the acceptable limit of " + xferErrorLimit);
				this.write(RESET);
				answer = new byte[expectedDataSize];
				answer = this.read(answer, 3000);
			}
			if (!(readBuffer[0] == DeviceSerialPortImpl.FF && readBuffer[readBuffer.length - 1] == DeviceSerialPortImpl.ACK && isCommandChecksumOK(readBuffer))) {
				this.addXferError();
				log.logp(Level.WARNING, UltramatSerialPort.$CLASS_NAME, $METHOD_NAME,
						"=====> data start or end does not match, number of errors = " + this.getXferErrors()); //$NON-NLS-1$
				if (this.getXferErrors() > xferErrorLimit) throw new SerialPortException("Number of tranfer error exceed the acceptable limit of " + xferErrorLimit);
				this.write(RESET);
				readBuffer = readConfigData(type, expectedDataSize, index);
			}
			log.logp(Level.FINE, UltramatSerialPort.$CLASS_NAME, $METHOD_NAME, "readBuffer = " + StringHelper.convert2CharString(readBuffer)); //$NON-NLS-1$
		}
		return readBuffer;
	}

	/**
	 * write configuration data according given type and expected data size for a given index (channel, memory number, ...)
	 * @param type
	 * @param configData
	 * @param index
	 * @throws IOException if writing configuration data failed
	 * @throws TimeOutException
	 */
	public synchronized void writeConfigData(byte[] type, byte[] configData, int index) throws IOException, TimeOutException {
		final String $METHOD_NAME = "writeConfigData"; //$NON-NLS-1$
		log.logp(Level.FINEST, UltramatSerialPort.$CLASS_NAME, $METHOD_NAME, "entry"); //$NON-NLS-1$
		byte[] writeBuffer = new byte[configData.length + 10]; //0x0C, type, index, data..., checksum, 0x0D

		if (this.isConnected()) {
			System.arraycopy(type, 0, writeBuffer, 0, 3);
			writeBuffer[writeBuffer.length - 1] = DeviceSerialPortImpl.CR;
			byte[] num = String.format("%02X", index).getBytes(); //$NON-NLS-1$
			System.arraycopy(num, 0, writeBuffer, 3, 2);
			System.arraycopy(configData, 0, writeBuffer, 5, configData.length);
			byte[] checkSum = getChecksum(writeBuffer);
			System.arraycopy(checkSum, 0, writeBuffer, writeBuffer.length - 5, 4);
			log.logp(Level.FINE, UltramatSerialPort.$CLASS_NAME, $METHOD_NAME, "answer = " + StringHelper.convert2CharString(writeBuffer)); //$NON-NLS-1$

			this.write(writeBuffer);
			byte[] answer = this.read(new byte[1], 3000);
			log.logp(Level.FINE, UltramatSerialPort.$CLASS_NAME, $METHOD_NAME, "answer = " + StringHelper.convert2CharString(answer)); //$NON-NLS-1$
			if ((answer[0] == DeviceSerialPortImpl.NAK)) {
				log.log(Level.WARNING, "Writing UltraDuoPlus configuration type (" + new String(type) + ") data failed!"); //$NON-NLS-1$ //$NON-NLS-2$
				throw new IOException("Writing UltraDuoPlus configuration type (" + new String(type) + ") data failed!"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	}

	/**
	 * check check sum of data buffer
	 * @param buffer
	 * @return true/false
	 */
	private synchronized boolean isChecksumOK(byte[] buffer) {
		final String $METHOD_NAME = "isChecksumOK"; //$NON-NLS-1$
		boolean isOK = false;
		int length = buffer.length;
		int check_sum = Checksum.ADD(buffer, 1, length - 6);
		int buffer_check_sum = Integer.parseInt(
				String.format(DeviceSerialPortImpl.FORMAT_4_CHAR, (char) buffer[length - 5], (char) buffer[length - 4], (char) buffer[length - 3], (char) buffer[length - 2]), 16);
		if (check_sum == buffer_check_sum || check_sum == (buffer_check_sum - this.dataCheckSumOffset))
			isOK = true;
		else {
			//some devices has a constant checksum offset by firmware error, calculate this offset first time the mismatch occurs and tolerate the calculated offset afterwards
			if (!this.isDataMissmatchWarningWritten) {
				log.logp(Level.WARNING, UltramatSerialPort.$CLASS_NAME, $METHOD_NAME, "check sum missmatch detected, calculates check_sum = " + check_sum + "; delta to data contained delta = "
						+ (buffer_check_sum - check_sum));
				log.logp(Level.WARNING, UltramatSerialPort.$CLASS_NAME, $METHOD_NAME, StringHelper.convert2CharString(buffer));
				this.isDataMissmatchWarningWritten = true;
				this.dataCheckSumOffset = buffer_check_sum - check_sum;
				isOK = true;
			}
		}
		return isOK;
	}

	/**
	 * check check sum of data buffer
	 * @param buffer
	 * @return true/false
	 */
	private synchronized boolean isCommandChecksumOK(byte[] buffer) {
		final String $METHOD_NAME = "isChecksumOK"; //$NON-NLS-1$
		boolean isOK = false;
		int length = buffer.length;
		int check_sum = Checksum.ADD(buffer, 1, length - 7);
		int buffer_check_sum = Integer.parseInt(
				String.format(DeviceSerialPortImpl.FORMAT_4_CHAR, (char) buffer[length - 6], (char) buffer[length - 5], (char) buffer[length - 4], (char) buffer[length - 3]), 16);
		if (check_sum == buffer_check_sum || check_sum == (buffer_check_sum - this.cmdCheckSumOffset))
			isOK = true;
		else {
			//some devices has a constant checksum offset by firmware error, calculate this offset first time the mismatch occurs and tolerate the calculated offset afterwards
			if (!this.isDataMissmatchWarningWritten) {
				log.logp(Level.WARNING, UltramatSerialPort.$CLASS_NAME, $METHOD_NAME, "check sum missmatch detected, calculates check_sum = " + check_sum + "; delta to data contained delta = "
						+ (buffer_check_sum - check_sum));
				log.logp(Level.WARNING, UltramatSerialPort.$CLASS_NAME, $METHOD_NAME, StringHelper.convert2CharString(buffer));
				this.isDataMissmatchWarningWritten = true;
				this.dataCheckSumOffset = buffer_check_sum - check_sum;
				isOK = true;
			}
		}
		return isOK;
	}

	/**
	 * calculate the checksum of the buffer and translate it to required format
	 * @param buffer
	 * @return
	 */
	private synchronized byte[] getChecksum(byte[] buffer) {
		final String $METHOD_NAME = "getChecksum"; //$NON-NLS-1$
		String check_sum = String.format("%04X", Checksum.ADD(buffer, 1, buffer.length - 6)); //$NON-NLS-1$
		log.logp(Level.FINER, UltramatSerialPort.$CLASS_NAME, $METHOD_NAME, "Check_sum char[]= " + check_sum); //$NON-NLS-1$
		return check_sum.getBytes();
	}
	
	/**
	 * query memory cycle data, time stamp, capacity, voltage, resistance for charge and discharge
	 * @param memoryNumber
	 * @return
	 * @throws TimeOutException 
	 * @throws IOException 
	 * @throws SerialPortException 
	 */
	public synchronized Vector<byte[]> readMemoryCycleData(int memoryNumber) throws SerialPortException, IOException, TimeOutException {
		Vector<byte[]> result = new Vector<byte[]>();
		String memoryCycleData = readMemoryCycle(memoryNumber);
		for (int i = 0; i < 11; i++) {
			int startIndex = i*(11*4);
			int endIndex = (i+1)*(11*4);
			result.add(memoryCycleData.substring(startIndex, endIndex).getBytes());
		}
		return result;
	}
	
	/**
	 * query memory cycle data, time stamp, capacity, voltage, resistance for charge and discharge
	 * @param memoryNumber
	 * @param cyclesData
	 * @throws TimeOutException 
	 * @throws IOException 
	 * @throws SerialPortException 
	 */
	public synchronized void writeMemoryCycleData(int memoryNumber, Vector<byte[]> cyclesData) throws SerialPortException, IOException, TimeOutException {
		StringBuilder sb = new StringBuilder();
		for (byte[] bytes : cyclesData) {
			System.out.print(new String(bytes));
			sb.append(new String(bytes));
		}
		System.out.println();
		//clear config data
		//sb = new StringBuilder().append("000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000");
		//05 FlugAkku 3200 A original data
		//sb = new StringBuilder().append("00030026000B00040011050141C2040C00213F880000000B0004000B00040018017341D905910000000000000009002D000B0005000709BF417E03B50000000000000008002B000C00050009084341DC04B4000B0000000000000032000F00050012001A41F0000000000000000000090015000C0005001D0A2E4194044E00000000000000100004000B000600040ADF41960566000F0022001D00120034000B0006000C07D0418004740000000000000003000E000E0006000D094241E206A30000000000000000000000000000000000000000000000000000000000210088000000000000000000000000000000000000");
		//sb = new StringBuilder().append("00030026000B00040011050141C2040C00213F88000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000");
		//05 FlugAkku 3200 A corrected data
		//sb = new StringBuilder().append("00030026000B00040011050141C2040C00213F880000000B0004000B00040018017341D905910000000000000009002D000B0005000709BF417E03B500000000000000130013000b0005000a084341DC04B4000B0000000000000000000000000001001A41F0000000000000000000130028000b0005001e0A2E4194044E00000000000000100004000B000600040ADF41960566000F0022001D00120034000B0006000C07D041800474000000000000000d0022000b00060010094241E206A30000000000000000000000000000000000000000000000000000000000210088000000000000000000000000000000000000");
		this.writeConfigData(UltramatSerialPort.WRITE_CYCLE_DATA, sb.toString().getBytes(), memoryNumber);
	}
}
