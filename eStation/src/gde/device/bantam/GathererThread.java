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
package osde.device.bantam;

import java.util.HashMap;
import java.util.logging.Logger;

import osde.data.Channel;
import osde.data.Channels;
import osde.data.Record;
import osde.data.RecordSet;
import osde.exception.ApplicationConfigurationException;
import osde.exception.DataInconsitsentException;
import osde.exception.SerialPortException;
import osde.exception.TimeOutException;
import osde.log.Level;
import osde.messages.Messages;
import osde.ui.OpenSerialDataExplorer;
import osde.utils.TimeLine;

/**
 * Thread implementation to gather data from eStation device
 * @author Winfied Brügmann
 */
public class GathererThread extends Thread {
	final static String				$CLASS_NAME									= GathererThread.class.getName();
	final static Logger				log													= Logger.getLogger(GathererThread.class.getName());
	final static int					TIME_STEP_DEFAULT						= 1250;
	final static int 					FILTER_TIME_DELTA_MS 				= 800; // definition of the tolerated time delta in msec 
	final static int					WAIT_TIME_RETRYS						= 36;


	final OpenSerialDataExplorer application;
	final EStationSerialPort	serialPort;
	final eStation						device;
	final EStationDialog			dialog;
	final Channels						channels;
	final Channel							channel;
	final int									channelNumber;
	
	String										recordSetKey								= Messages.getString(osde.messages.MessageIds.OSDE_MSGT0272);
	boolean										isPortOpenedByLiveGatherer	= false;
	boolean										isGatheredRecordSetVisible	= true;
	int 											numberBatteryCells 					= 0; 
	int												retryCounter								= GathererThread.WAIT_TIME_RETRYS;	// 36 * 5 sec timeout = 180 sec
	boolean										isCollectDataStopped				= false;

	/**
	 * data gatherer thread definition 
	 * @throws SerialPortException 
	 * @throws ApplicationConfigurationException 
	 * @throws Exception 
	 */
	public GathererThread(OpenSerialDataExplorer currentApplication, eStation useDevice, EStationSerialPort useSerialPort, int channelConfigNumber, EStationDialog useDialog)
			throws ApplicationConfigurationException, SerialPortException {
		this.application = currentApplication;
		this.device = useDevice;
		this.dialog = useDialog;
		this.serialPort = useSerialPort;
		this.channels = Channels.getInstance();
		this.channelNumber = channelConfigNumber;
		this.channel = this.channels.get(this.channelNumber);

		if (!this.serialPort.isConnected()) {
			this.serialPort.open();
			this.isPortOpenedByLiveGatherer = true;
		}
		this.setPriority(Thread.MAX_PRIORITY);
	}

	@Override
	public void run() {
		final String $METHOD_NAME = "run"; //$NON-NLS-1$
		RecordSet recordSet = null;
		int[] points = new int[this.device.getMeasurementNames(this.channelNumber).length];
		int waitTime_ms = 0; // dry time
		boolean isProgrammExecuting = false;
		boolean isConfigUpdated = false;
		HashMap<String, String> configData = new HashMap<String, String>();
		long startCycleTime = 0;
		long tmpCycleTime = 0;
		//long sumCycleTime = 0;
		//long deltaTime = 0;
		long measurementCount = 0;
		boolean isCycleMode = false;
		int dryTimeCycleCount = 0; // number of discharge/charge cycle NiXx cells only 
		//double newTimeStep_ms = 0;
		//StringBuilder sb = new StringBuilder();
		byte[] dataBuffer = null;

		this.isCollectDataStopped = false;
		log.logp(Level.FINE, GathererThread.$CLASS_NAME, $METHOD_NAME, "====> entry initial time step ms = " + this.device.getTimeStep_ms()); //$NON-NLS-1$

		int posCells = this.device.getName().endsWith("BC6") || this.device.getName().endsWith("P6") || this.device.getName().endsWith("P60") ? 6 : 8; //$NON-NLS-1$

		while (!this.isCollectDataStopped) {
			try {
				// get data from device
				dataBuffer = this.serialPort.getData();

				// check if device is ready for data capturing, discharge or charge allowed only
				// else wait for 180 seconds max. for actions
				String processName = this.device.USAGE_MODE[this.device.getProcessingMode(dataBuffer)];
				this.dialog.updateGlobalConfigData(this.device.getConfigurationValues(configData, dataBuffer));
				isProgrammExecuting = this.device.isProcessing(dataBuffer);
				isCycleMode = this.device.isCycleMode(dataBuffer);
				if (isCycleMode && measurementCount < 5) dryTimeCycleCount = this.device.getNumberOfCycle(dataBuffer) > 0 ? this.device.getNumberOfCycle(dataBuffer) * 2 - 1 : 0;
				log.logp(Level.FINER, GathererThread.$CLASS_NAME, $METHOD_NAME,
						"processing mode = " + processName + " isCycleMode = " + isCycleMode + " dryTimeCycleCount = " + dryTimeCycleCount); //$NON-NLS-1$	//$NON-NLS-2$ //$NON-NLS-3$

				if (isProgrammExecuting && (processName.equals(this.device.USAGE_MODE[1]) || processName.equals(this.device.USAGE_MODE[2]))) {
					//if (processName.equals(this.device.USAGE_MODE[1]) || processName.equals(this.device.USAGE_MODE[2])) { // 1=discharge; 2=charge -> eStation active
					// check state change waiting to discharge to charge
					// check if a record set matching for re-use is available and prepare a new if required
					if (this.channel.size() == 0 || recordSet == null || !this.recordSetKey.endsWith(" " + processName)) { //$NON-NLS-1$
						this.application.setStatusMessage(""); //$NON-NLS-1$
						isConfigUpdated = false;
						setRetryCounter(GathererThread.WAIT_TIME_RETRYS); // 36 * receive timeout sec timeout = 180 sec
						waitTime_ms = new Integer(configData.get(eStation.CONFIG_WAIT_TIME)).intValue() * 60000;
						log.logp(Level.FINE, GathererThread.$CLASS_NAME, $METHOD_NAME, "waitTime_ms = " + waitTime_ms); //$NON-NLS-1$
						// record set does not exist or is outdated, build a new name and create
						this.recordSetKey = this.channel.getNextRecordSetNumber() + ") [" + configData.get(eStation.CONFIG_BATTERY_TYPE) + "] " + processName; //$NON-NLS-1$ //$NON-NLS-2$
						this.channel.put(this.recordSetKey, RecordSet.createRecordSet(this.recordSetKey, this.application.getActiveDevice(), channel.getNumber(), true, false));
						this.channel.applyTemplateBasics(this.recordSetKey);
						log.logp(Level.FINE, GathererThread.$CLASS_NAME, $METHOD_NAME, this.recordSetKey + " created for channel " + this.channel.getName()); //$NON-NLS-1$
						if (this.channel.getActiveRecordSet() == null) this.channel.setActiveRecordSet(this.recordSetKey);
						recordSet = this.channel.get(this.recordSetKey);
						recordSet.setAllDisplayable();
						//recordSet.addTimeStep_ms(0.0);
						this.channel.applyTemplate(this.recordSetKey, false);
						// switch the active record set if the current record set is child of active channel
						// for eStation its always the case since we have only one channel
						if (this.channel.getName().equals(this.channels.getActiveChannel().getName())) {
							this.channels.getActiveChannel().switchRecordSet(this.recordSetKey);
						}
						measurementCount = 0;
						startCycleTime = 0;
					}

					// prepare the data for adding to record set
					this.isGatheredRecordSetVisible = this.recordSetKey.equals(this.channels.getActiveChannel().getActiveRecordSet().getName());
					tmpCycleTime = System.nanoTime()/1000000;
					if (measurementCount++ == 0) {
						startCycleTime = tmpCycleTime;
					}
					recordSet.addPoints(this.device.convertDataBytes(points, dataBuffer), (tmpCycleTime - startCycleTime));
					log.logp(Level.TIME, GathererThread.$CLASS_NAME, $METHOD_NAME, "time = " + TimeLine.getFomatedTimeWithUnit(tmpCycleTime - startCycleTime)); //$NON-NLS-1$
					
					this.numberBatteryCells = 0; //this.device.getNumberOfLithiumXCells(dataBuffer);
					String[] recordKeys = recordSet.getRecordNames();
					for (int i = posCells; i < recordSet.size(); i++) {
						Record record = recordSet.get(recordKeys[i]);
						if (record.getRealMinValue() != 0 && record.getRealMaxValue() != 0) {
							this.numberBatteryCells++;
							log.logp(Level.FINER, GathererThread.$CLASS_NAME, $METHOD_NAME, "record = " + record.getName() + " " + record.getRealMinValue() + " " + record.getRealMaxValue()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						}
					}

					if (recordSet.size() > 0 && recordSet.isChildOfActiveChannel() && recordSet.equals(this.channels.getActiveChannel().getActiveRecordSet())) {
						GathererThread.this.application.updateGraphicsWindow();
						GathererThread.this.application.updateStatisticsData();
						GathererThread.this.application.updateDataTable(this.recordSetKey, false);
						GathererThread.this.application.updateDigitalWindowChilds();
						GathererThread.this.application.updateAnalogWindowChilds();
						GathererThread.this.application.updateCellVoltageChilds();
					}
					
					//switch off single cell voltage lines if no cell voltages is available
					for (int i = posCells + this.numberBatteryCells; !isConfigUpdated && i < points.length; i++) {
						recordSet.get(recordKeys[i]).setActive(false);
						recordSet.get(recordKeys[i]).setDisplayable(false);
						recordSet.get(recordKeys[i]).setVisible(false);
					}
					isConfigUpdated = true;
					//OsdReaderWriter.write("E:\\Temp\\not.osd", this.channel, 1);
				}
				else { // no eStation program is executing, wait for 180 seconds max. for actions
					this.application.setStatusMessage(Messages.getString(MessageIds.OSDE_MSGI1400));
					log.logp(Level.FINE, GathererThread.$CLASS_NAME, $METHOD_NAME, "wait for eStation activation"); //$NON-NLS-1$

					if (recordSet != null && recordSet.getRecordDataSize(true) > 5) { // record set has data points, save data and wait
						finalizeRecordSet(false);
						isProgrammExecuting = false;
						recordSet = null;
						setRetryCounter(GathererThread.WAIT_TIME_RETRYS); // 36 * receive timeout sec timeout = 180 sec
						this.application.openMessageDialogAsync(this.dialog.getDialogShell(), Messages.getString(MessageIds.OSDE_MSGT1408));
					}
					else if (0 == (setRetryCounter(getRetryCounter() - 1))) {
						log.log(Level.FINE, "eStation activation timeout"); //$NON-NLS-1$
						this.application.openMessageDialogAsync(this.dialog.getDialogShell(), Messages.getString(MessageIds.OSDE_MSGW1400));
						stopDataGatheringThread(false, null);
					}
				}
			}
			catch (DataInconsitsentException e) {
				String message = Messages.getString(osde.messages.MessageIds.OSDE_MSGE0036, new Object[] {this.getClass().getSimpleName(), $METHOD_NAME}); 
				cleanup(message);
			}
			catch (Throwable e) {
				// this case will be reached while NiXx Akku discharge/charge/discharge cycle
				if (e instanceof TimeOutException && isCycleMode && dryTimeCycleCount > 0) {
					try {
						finalizeRecordSet(false);
						log.logp(Level.FINE, GathererThread.$CLASS_NAME, $METHOD_NAME, "(dry time) waiting..."); //$NON-NLS-1$
						this.application.setStatusMessage(Messages.getString(MessageIds.OSDE_MSGI1401));
						recordSet = null;
						--dryTimeCycleCount;
						Thread.sleep(waitTime_ms);
					}
					catch (InterruptedException e1) {
						// ignore
					}
				}
				// this case will be reached while eStation program is started, checked and the check not asap committed, stop pressed
				else if (e instanceof TimeOutException && !isProgrammExecuting) {
					this.application.setStatusMessage(Messages.getString(MessageIds.OSDE_MSGI1400));
					log.logp(Level.FINE, GathererThread.$CLASS_NAME, $METHOD_NAME, "wait for eStation activation ..."); //$NON-NLS-1$
					if (0 == (setRetryCounter(getRetryCounter() - 1))) {
						log.log(Level.FINE, "eStation activation timeout"); //$NON-NLS-1$
						this.application.openMessageDialogAsync(this.dialog.getDialogShell(), Messages.getString(MessageIds.OSDE_MSGW1400));
						stopDataGatheringThread(false, null);
					}
				}
				// program end or unexpected exception occurred, stop data gathering to enable save data by user
				else {
					log.log(Level.FINE, "eStation program end detected"); //$NON-NLS-1$
					stopDataGatheringThread(true, e);
				}
			}
		}
		this.application.setStatusMessage(""); //$NON-NLS-1$
		log.logp(Level.FINE, GathererThread.$CLASS_NAME, $METHOD_NAME, "======> exit"); //$NON-NLS-1$
	}

	/**
	 * stop the data gathering and check if reasonable data in record set to finalize or clear
	 * @param enableEndMessage
	 * @param throwable
	 */
	void stopDataGatheringThread(boolean enableEndMessage, Throwable throwable) {
		final String $METHOD_NAME = "stopDataGatheringThread"; //$NON-NLS-1$

		if (throwable != null) {
			log.logp(Level.WARNING, $CLASS_NAME, $METHOD_NAME, throwable.getMessage(), throwable);
		}
		
		this.isCollectDataStopped = true;

		if (this.serialPort != null && this.serialPort.getXferErrors() > 0) {
			log.log(Level.WARNING, "During complete data transfer " + this.serialPort.getXferErrors() + " number of errors occured!"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (this.serialPort != null && this.serialPort.isConnected() && this.isPortOpenedByLiveGatherer == true && this.serialPort.isConnected()) {
			this.serialPort.close();
		}

		RecordSet recordSet = this.channel.get(this.recordSetKey);
		if (recordSet != null && recordSet.getRecordDataSize(true) > 5) { // some other exception while program execution, record set has data points
			finalizeRecordSet(false);
			if (enableEndMessage) 
				this.application.openMessageDialog(this.dialog.getDialogShell(), Messages.getString(MessageIds.OSDE_MSGT1409));
		}
		else {
			if (throwable != null) {
				cleanup(Messages.getString(osde.messages.MessageIds.OSDE_MSGE0022, new Object[] { throwable.getClass().getSimpleName(), throwable.getMessage() })
						+ Messages.getString(MessageIds.OSDE_MSGT1408));
			}
			else {
				if (enableEndMessage)
					cleanup(Messages.getString(osde.messages.MessageIds.OSDE_MSGE0026)	+ Messages.getString(MessageIds.OSDE_MSGT1408));
			}
		}
	}

	/**
	 * close port, set isDisplayable according channel configuration and calculate slope
	 */
	void finalizeRecordSet(boolean doClosePort) {
		if (doClosePort && this.isPortOpenedByLiveGatherer && this.serialPort.isConnected()) this.serialPort.close();

		RecordSet tmpRecordSet = this.channel.get(this.recordSetKey);
		if (tmpRecordSet != null) {
			this.device.updateVisibilityStatus(tmpRecordSet);
			this.device.makeInActiveDisplayable(tmpRecordSet);
			this.application.updateStatisticsData();
			this.application.updateDataTable(this.recordSetKey, false);
			
			this.device.setAverageTimeStep_ms(tmpRecordSet.getAverageTimeStep_ms());
			log.log(Level.TIME, "set average time step msec = " + this.device.getAverageTimeStep_ms());
		}
	}

	/**
	 * cleanup all allocated resources and display the message
	 * @param this.recordSetKey
	 * @param message
	 * @param e
	 */
	void cleanup(final String message) {
		if (this.channel.get(this.recordSetKey) != null) {
			this.channel.get(this.recordSetKey).clear();
			this.channel.remove(this.recordSetKey);
			if (Thread.currentThread().getId() == this.application.getThreadId()) {
				this.application.getMenuToolBar().updateRecordSetSelectCombo();
				this.application.updateStatisticsData();
				this.application.updateDataTable(this.recordSetKey, true);
				this.application.openMessageDialog(GathererThread.this.dialog.getDialogShell(), message);
				this.device.getDialog().resetButtons();
			}
			else {
				final String useRecordSetKey = this.recordSetKey;
				OpenSerialDataExplorer.display.asyncExec(new Runnable() {
					public void run() {
						GathererThread.this.application.getMenuToolBar().updateRecordSetSelectCombo();
						GathererThread.this.application.updateStatisticsData();
						GathererThread.this.application.updateDataTable(useRecordSetKey, true);
						GathererThread.this.application.openMessageDialog(GathererThread.this.dialog.getDialogShell(), message);
						GathererThread.this.device.getDialog().resetButtons();
					}
				});
			}
		}
		else
			this.application.openMessageDialog(this.dialog.getDialogShell(), message);
	}

	/**
	 * @param enabled the isCollectDataStopped to set
	 */
	void setCollectDataStopped(boolean enabled) {
		this.isCollectDataStopped = enabled;
	}

	/**
	 * @return the isCollectDataStopped
	 */
	boolean isCollectDataStopped() {
		return this.isCollectDataStopped;
	}

	/**
	 * @return the retryCounter
	 */
	int getRetryCounter() {
		return this.retryCounter;
	}

	/**
	 * @param newRetryCounter the retryCounter to set
	 */
	int setRetryCounter(int newRetryCounter) {
		return this.retryCounter = newRetryCounter;
	}
}
