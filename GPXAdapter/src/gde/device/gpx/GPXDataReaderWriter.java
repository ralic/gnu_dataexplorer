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
    
    Copyright (c) 2008,2009,2010,2011,2012,2013 Winfried Bruegmann
****************************************************************************************/
package gde.device.gpx;

import gde.GDE;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.RecordSet;
import gde.device.IDevice;
import gde.exception.DataInconsitsentException;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.menu.MenuToolBar;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Class to read and write geo points exchange format data 
 * @author Winfried Brügmann
 */
public class GPXDataReaderWriter {
	static Logger							log					= Logger.getLogger(GPXDataReaderWriter.class.getName());

	static String							lineSep			= GDE.LINE_SEPARATOR;
	static DecimalFormat			df3					= new DecimalFormat("0.000");														//$NON-NLS-1$
	static StringBuffer				sb;

	final static DataExplorer	application	= DataExplorer.getInstance();
	final static Channels			channels		= Channels.getInstance();

	/**
	 * read GPS exchange format track point and extension data
	 * @param filePath
	 * @param device
	 * @param recordNameExtend
	 * @param channelConfigNumber
	 * @return
	 */
	public static RecordSet read(String filePath, IDevice device, String recordNameExtend, Integer channelConfigNumber) {
		String sThreadId = String.format("%06d", Thread.currentThread().getId()); //$NON-NLS-1$
		Channel activeChannel = null;
		int lineNumber = 0;
		String recordSetNameExtend = device.getRecordSetStemName();
		RecordSet channelRecordSet = null;
		MenuToolBar menuToolBar = GPXDataReaderWriter.application.getMenuToolBar();
		if (menuToolBar != null) {
			GPXDataReaderWriter.application.setProgress(0, sThreadId);
			GPXDataReaderWriter.application.setStatusMessage(Messages.getString(gde.device.gpx.MessageIds.GDE_MSGT1776) + filePath);
		}

		try {
			if (channelConfigNumber == null)
				activeChannel = GPXDataReaderWriter.channels.getActiveChannel();
			else
				activeChannel = GPXDataReaderWriter.channels.get(channelConfigNumber);
			channelConfigNumber = GPXDataReaderWriter.channels.getActiveChannelNumber();

			if (activeChannel != null) {
				if (GPXDataReaderWriter.log.isLoggable(Level.FINE))
					GPXDataReaderWriter.log.log(Level.FINE, device.getChannelCount() + " - data for channel = " + channelConfigNumber);

				String recordSetName = (activeChannel.size() + 1) + recordSetNameExtend;
				recordSetName = recordNameExtend.length() > 2 ? recordSetName + GDE.STRING_BLANK_LEFT_BRACKET + recordNameExtend + GDE.STRING_RIGHT_BRACKET : recordSetName;

				if (menuToolBar != null) GPXDataReaderWriter.application.setProgress(30, sThreadId);

				parseInputXML(filePath, device, activeChannel, recordSetName);
				channelRecordSet = activeChannel.get(recordSetName);
				
				if (menuToolBar != null) GPXDataReaderWriter.application.setProgress(100, sThreadId);

				if (menuToolBar != null) {
					Channels.getInstance().switchChannel(activeChannel.getName());
					activeChannel.switchRecordSet(recordSetName);
					device.updateVisibilityStatus(channelRecordSet, true);

					menuToolBar.updateChannelSelector();
					menuToolBar.updateRecordSetSelectCombo();
				}
			}
		}
		catch (FileNotFoundException e) {
			GPXDataReaderWriter.log.log(Level.WARNING, e.getMessage(), e);
			GPXDataReaderWriter.application.openMessageDialog(e.getMessage());
		}
		catch (IOException e) {
			GPXDataReaderWriter.log.log(Level.WARNING, e.getMessage(), e);
			GPXDataReaderWriter.application.openMessageDialog(e.getMessage());
		}
		catch (Exception e) {
			GPXDataReaderWriter.log.log(Level.WARNING, e.getMessage(), e);
			// check if previous records are available and needs to be displayed
			if (activeChannel != null && activeChannel.size() > 0) {
				String recordSetName = activeChannel.getFirstRecordSetName();
				activeChannel.setActiveRecordSet(recordSetName);
				device.updateVisibilityStatus(activeChannel.get(recordSetName), true);
				activeChannel.get(recordSetName).checkAllDisplayable(); // raw import needs calculation of passive records
				if (GPXDataReaderWriter.application.getStatusBar() != null) activeChannel.switchRecordSet(recordSetName);
			}
			// now display the error message
			String msg = filePath + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGE0045, new Object[] { e.getMessage(), lineNumber });
			GPXDataReaderWriter.log.log(Level.WARNING, msg, e);
			GPXDataReaderWriter.application.openMessageDialog(msg);
		}
		finally {
			if (GPXDataReaderWriter.application.getStatusBar() != null) {
				GPXDataReaderWriter.application.setStatusMessage(GDE.STRING_EMPTY);
			}
		}

		return channelRecordSet;
	}

	public static void parseInputXML(final String localUnixFullQualifiedPath, final IDevice device, final Channel activeChannel, final String recordSetName) throws ParserConfigurationException, SAXException, IOException {

		SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setNamespaceAware(true);
		factory.setValidating(true);
		SAXParser saxParser = factory.newSAXParser();
		saxParser.setProperty("http://java.sun.com/xml/jaxp/properties/schemaLanguage", "http://www.w3.org/2001/XMLSchema");
		saxParser.setProperty("http://java.sun.com/xml/jaxp/properties/schemaSource", "file:/resource/gpx.xsd");

		DefaultHandler handler = new DefaultHandler() {
			boolean										isDescription						= false;
			boolean										isDescription2					= false;
			boolean										isElevation							= false;
			boolean										isTime									= false, isDateSet = false;
			boolean										isNumSatelites					= false;
			Boolean										isExtensionFirstCalled	= null;
			boolean										isExtension							= false;
			final int[]								date										= new int[3];
			final int[]								time										= new int[3];
			long											timeStamp								= 0, startTimeStamp = 0;
			final Map<String, String>	tmpPoints								= new LinkedHashMap<String, String>();
			final Vector<String>			extensionNames					= new Vector<String>();
			String										extensionName						= GDE.STRING_EMPTY;
			int[]											points									= new int[device.getMeasurementNames(activeChannel.getNumber()).length];
			int												pointsIndex							= 0;
			RecordSet									activeRecordSet;
			String 										recordSetDescription 		= GDE.STRING_EMPTY;

			@Override
			public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {

				if (GPXDataReaderWriter.log.isLoggable(Level.FINE)) GPXDataReaderWriter.log.log(Level.FINE, "Start Element :" + qName);
				if (qName != null && qName.length() > 1) {
					if (qName.equalsIgnoreCase("text"))
						this.isDescription = true; //<text>MikroKopter</text> 
					else if (qName.equalsIgnoreCase("desc"))
						this.isDescription2 = true;//<desc>FC HW:2.1 SW:0.88e + NC HW:2.0 SW:0.28i</desc>

					// <trkpt lat="+41.0334244" lon="-73.5230532">	
					else if (qName.equalsIgnoreCase("trkpt")) {
						if (attributes.getLength() == 2) {
							this.tmpPoints.put("lat", attributes.getValue("lat")); //lat="+41.0334244"
							this.tmpPoints.put("lon", attributes.getValue("lon")); //lon="-73.5230532"
						}
					}
					else if (qName.equalsIgnoreCase("ele"))
						this.isElevation = true;//<ele>12.863</ele>
					else if (qName.equalsIgnoreCase("time"))
						this.isTime = true;//<time>2012-04-19T15:37:33Z</time>
					else if (qName.equalsIgnoreCase("sat"))
						this.isNumSatelites = true;//<sat>10</sat>

					//<extensions>
					else if (qName.equalsIgnoreCase("extensions")) {
						this.isExtension = true;
						if (this.isExtensionFirstCalled == null) this.isExtensionFirstCalled = true;
					}
					else if (this.isExtension) {
						this.extensionName = qName;
					}

				}

				//				<extensions>
				//				<Altimeter>252,' '</Altimeter>
				//				<Variometer>89</Variometer>
				//				<Course>297</Course>
				//				<GroundSpeed>175</GroundSpeed>
				//				<VerticalSpeed>508</VerticalSpeed>
				//				<FlightTime>3</FlightTime>
				//				<Voltage>15.8</Voltage>
				//				<Current>68.9</Current>
				//				<Capacity>76</Capacity>
				//				<RCQuality>197</RCQuality>
				//				<RCRSSI>0</RCRSSI>
				//				<Compass>094,095</Compass>
				//				<NickAngle>006</NickAngle>
				//				<RollAngle>000</RollAngle>
				//				<MagnetField>102</MagnetField>
				//				<MagnetInclination>64,-4</MagnetInclination>
				//				<MotorCurrent>24,91,143,97,157,88,0,0,0,0,0,0</MotorCurrent>
				//				<BL_Temperature>25,27,20,27,26,24,0,0,0,0,0,0</BL_Temperature>
				//				<AvaiableMotorPower>255</AvaiableMotorPower>
				//				<FC_I2C_ErrorCounter>000</FC_I2C_ErrorCounter>
				//				<AnalogInputs>21,12,24,760</AnalogInputs>
				//				<NCFlag>0x82</NCFlag>
				//				<Servo>153,128,0</Servo>
				//				<WP>----,0,13,0</WP>
				//				<FCFlags2>0xc3,0x18</FCFlags2>
				//				<ErrorCode>000</ErrorCode>
				//				<TargetBearing>090</TargetBearing>
				//				<TargetDistance>12</TargetDistance>
				//				<RCSticks>0,0,0,30,1,127,1,153,1,1,1,1</RCSticks>
				//				<GPSSticks>-77,-14,0,'D'</GPSSticks>
				//				</extensions>

			}

			@Override
			public void endElement(String uri, String localName, String qName) throws SAXException {
				if (GPXDataReaderWriter.log.isLoggable(Level.FINE)) GPXDataReaderWriter.log.log(Level.FINE, "End Element :" + qName);
				if (qName.equalsIgnoreCase("trkpt")) {
					this.pointsIndex = 0;
					this.points[this.pointsIndex++] = (int) (Double.valueOf(this.tmpPoints.get("lat").replace("+", "").trim()) * 1000000);
					this.points[this.pointsIndex++] = (int) (Double.valueOf(this.tmpPoints.get("lon").replace("+", "").trim()) * 1000000);
					this.points[this.pointsIndex++] = Integer.valueOf(this.tmpPoints.get("ele").replace(".", "").trim());
					this.points[this.pointsIndex++] = Integer.valueOf(this.tmpPoints.get("sat").trim()) * 1000;

					if (this.isExtensionFirstCalled != null) {
						if (this.isExtensionFirstCalled) {
							
							int measurementSize = device.getNumberOfMeasurements(activeChannel.getNumber());
							if (GPXDataReaderWriter.log.isLoggable(Level.FINE)) GPXDataReaderWriter.log.log(Level.FINE, "measurementSize = " + measurementSize); //$NON-NLS-1$

							for (String tmpExtensionName : this.extensionNames) {
								String[] values = this.tmpPoints.get(tmpExtensionName).split(GDE.STRING_COMMA);
								for (int i = 0; i < values.length && (this.points.length - this.pointsIndex) > 0; i++) {
									//System.out.println(i + " values.lenght " + this.pointsIndex + " - " + (this.points.length - this.pointsIndex));
									String newRecordName = tmpExtensionName + (values.length > 1 ? GDE.STRING_BLANK + (i + 1) : GDE.STRING_EMPTY);
									//System.out.print(activeRecordSet.getRecordNames()[this.pointsIndex] + " -> ");
									device.getMeasurement(activeChannel.getNumber(), this.pointsIndex).setName(newRecordName);
									//System.out.println(activeRecordSet.getRecordNames()[this.pointsIndex]);
									try {
										this.points[this.pointsIndex++] = Integer.valueOf(values[i].trim()) * 1000;
									}
									catch (NumberFormatException e) {
										// ignore and keep existing value
									}
								}
							}
							for (int i = pointsIndex; i < this.points.length; i++) {
								device.getMeasurement(activeChannel.getNumber(), i).setName(i+"?????");
							}

							//create the recordSet 
							activeChannel.put(recordSetName, RecordSet.createRecordSet(recordSetName, GPXDataReaderWriter.application.getActiveDevice(), activeChannel.getNumber(), true, false));
							if (GPXDataReaderWriter.log.isLoggable(Level.FINE))
								GPXDataReaderWriter.log.log(Level.FINE, recordSetName + " created for channel " + activeChannel.getName()); //$NON-NLS-1$
							activeChannel.setActiveRecordSet(recordSetName);
							activeRecordSet = activeChannel.get(recordSetName);
							activeChannel.applyTemplate(recordSetName, false);
							
							this.isExtensionFirstCalled = false;
						}
						else if (!this.isExtensionFirstCalled) {
							for (String tmpExtensionName : this.extensionNames) {
								String[] values = this.tmpPoints.get(tmpExtensionName).split(GDE.STRING_COMMA);
								for (int i = 0; i < values.length && (this.points.length - this.pointsIndex) > 0; i++) {
									//System.out.println(i + " values.lenght " + this.pointsIndex + " - " + (this.points.length - this.pointsIndex));
									try {
										this.points[this.pointsIndex++] = Integer.valueOf(values[i].trim()) * 1000;
									}
									catch (NumberFormatException e) {
										// ignore and keep existing value
									}
								}
							}
						}
					}
					try {
						if (this.startTimeStamp == 0) this.startTimeStamp = this.timeStamp;
						if (GPXDataReaderWriter.log.isLoggable(Level.FINER)) GPXDataReaderWriter.log.log(Level.FINER, "" + (this.timeStamp - this.startTimeStamp) * 1.0);
						//System.out.println(StringHelper.intArrayToString(this.points));
						activeRecordSet.addPoints(this.points, (this.timeStamp - this.startTimeStamp) * 1.0);
					}
					catch (DataInconsitsentException e) {
						e.printStackTrace();
					}
				}
			}

			@Override
			public void characters(char ch[], int start, int length) throws SAXException {
				String values = new String(ch, start, length);
				if (!values.contains("\n") && !values.contains("\r")) {
					if (GPXDataReaderWriter.log.isLoggable(Level.FINE)) GPXDataReaderWriter.log.log(Level.FINE, values);
					if (this.isDescription) {
						recordSetDescription = GDE.LINE_SEPARATOR + new String(ch, start, length);
						this.isDescription = false;
					}
					else if (this.isDescription2) {
						recordSetDescription = recordSetDescription.length() > 1 
							? recordSetDescription + GDE.STRING_COLON + GDE.STRING_BLANK + new String(ch, start, length)
							: GDE.LINE_SEPARATOR + new String(ch, start, length);
						this.isDescription2 = false;
					}
					else if (this.isElevation) {
						this.tmpPoints.put("ele", new String(ch, start, length)); //<ele>12.863</ele>
						this.isElevation = false;
					}
					else if (this.isTime) {
						String dateTime = new String(ch, start, length);//<time>2012-04-19T15:37:33Z</time>
						if (!this.isDateSet) {
							String strDate = dateTime.split("T")[0];
							this.date[0] = Integer.parseInt(strDate.substring(0, 4));
							this.date[1] = Integer.parseInt(strDate.substring(5, 7));
							this.date[2] = Integer.parseInt(strDate.substring(8, 10));
						}
						String strValueTime = dateTime.split("T|Z")[1];
						this.time[0] = Integer.parseInt(strValueTime.substring(0, 2));
						this.time[1] = Integer.parseInt(strValueTime.substring(3, 5));
						this.time[2] = Integer.parseInt(strValueTime.substring(6, 8));
						GregorianCalendar calendar = new GregorianCalendar(this.date[0], this.date[1] - 1, this.date[2], this.time[0], this.time[1], this.time[2]);
						this.timeStamp = calendar.getTimeInMillis() + (strValueTime.contains(GDE.STRING_DOT) ? Integer.parseInt(strValueTime.substring(strValueTime.indexOf(GDE.STRING_DOT) + 1)) : 0);
						if (!this.isDateSet && this.isExtensionFirstCalled != null) {
							String description = activeRecordSet.getRecordSetDescription();
							activeRecordSet.setRecordSetDescription(description.substring(0, description.indexOf(GDE.STRING_COLON) + 2) + dateTime.split("T")[0] + GDE.STRING_COMMA + GDE.STRING_BLANK
									+ dateTime.split("T|Z")[1] + recordSetDescription);
							this.isDateSet = true;
						}
						this.isTime = false;
					}
					else if (this.isNumSatelites) {
						this.tmpPoints.put("sat", new String(ch, start, length)); //<sat>10</sat>
						this.isNumSatelites = false;
					}
					else if (this.isExtension && this.extensionName.length() > 3) {
						if (isExtensionFirstCalled != null && isExtensionFirstCalled) 
							this.extensionNames.add(this.extensionName);
						this.tmpPoints.put(this.extensionName, new String(ch, start, length)); //<MotorCurrent>24,91,143,97,157,88,0,0,0,0,0,0</MotorCurrent>
					}
				}
			}
		};
		saxParser.parse(localUnixFullQualifiedPath, handler);

		return;
	}

}