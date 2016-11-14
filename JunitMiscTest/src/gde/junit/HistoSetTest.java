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
    
    Copyright (c) 2008,2009,2010,2011,2012,2013,2014,2015,2016 Winfried Bruegmann
									2016 Thomas Eickert
****************************************************************************************/
package gde.junit;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.naming.OperationNotSupportedException;

import gde.GDE;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.HistoRecordSet;
import gde.data.RecordSet;
import gde.device.DeviceConfiguration;
import gde.device.IDevice;
import gde.device.graupner.HoTTbinHistoReader;
import gde.exception.DataInconsitsentException;
import gde.exception.DataTypeException;
import gde.exception.NotSupportedFileFormatException;
import gde.io.HistoOsdReaderWriter;
import gde.io.OsdReaderWriter;
import gde.utils.FileUtils;

public class HistoSetTest extends TestSuperClass { // TODO for junit tests in general it may be better to choose another directory structure: http://stackoverflow.com/a/2388285
	private final static String	$CLASS_NAME	= HistoSetTest.class.getName();
	private final static Logger	log					= Logger.getLogger($CLASS_NAME);

	private static final double	DELTA				= 1e-13;
	private static int					count				= 0;

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	public void setUp() throws Exception {
		super.setUp();
		log.setLevel(Level.INFO);
		log.setUseParentHandlers(true);
	}

	private List<String> getProblemFileNames() {
		ArrayList<String> problemFileNames = new ArrayList<String>();
		//		problemFileNames.add("2016-02-12_T-Rex 250_.osd"); // header has java.io.UTFDataFormatException: malformed input around byte 8
		//		problemFileNames.add("2015-05-14_T-Rex 250 Kanaele.osd"); // RecordSet #1 consists of 86 records whereas the recordSetDataPointer value of the next recordSet allows 85 records only
		return problemFileNames;
	}

	public void testReadBinOsdFiles() {
		System.out
				.println(String.format("Max Memory=%,11d   Total Memory=%,11d   Free Memory=%,11d", Runtime.getRuntime().maxMemory(), Runtime.getRuntime().totalMemory(), Runtime.getRuntime().freeMemory()));
		List<String> problemFileNames = getProblemFileNames();
		TreeMap<String, Exception> failures = new TreeMap<String, Exception>();

		this.setDataPath();
		String fileRootDir = this.dataPath.getAbsolutePath();
		// >>> take one of these optional data sources for the test <<<
		//		Path dirPath = FileSystems.getDefault().getPath(fileRootDir, "_Thomas", "DataExplorer");  // use with empty datafilepath in DataExplorer.properties
		//		Path dirPath = FileSystems.getDefault().getPath(fileRootDir, "_Winfried", "DataExplorer"); // use with empty datafilepath in DataExplorer.properties
		Path dirPath = FileSystems.getDefault().getPath(fileRootDir); // takes all Files from datafilepath in DataExplorer.properties or from DataFilesTestSamples if empty datafilepath

		FileUtils.checkDirectoryAndCreate(dirPath.toString());
		try {
			List<File> files = FileUtils.getFileListing(dirPath.toFile(), 11);

			for (File file : files) {
				if (!problemFileNames.contains(file.getName())) {
					if (file.getName().endsWith(GDE.FILE_ENDING_DOT_BIN)) {
						HistoSetTest.count++;
						int maxTime_sec = 0;
						String fileDeviceName = "HoTTAdapter";
						IDevice device = setDevice(fileDeviceName);
						setupDataChannels(device);
						for (Entry<Integer, Channel> channelEntry : this.channels.entrySet()) {
							this.channels.setActiveChannelNumber(channelEntry.getKey());
							try {
								RecordSet recordSet = HistoRecordSet.createRecordSet(file.toPath().getFileName().toString(), device, this.channels.getActiveChannelNumber(), true, true);
								HoTTbinHistoReader.read(recordSet, file.getAbsolutePath());
								maxTime_sec = recordSet.getMaxTime_ms() / 1000 > maxTime_sec ? (int) recordSet.getMaxTime_ms() / 1000 : maxTime_sec;
								System.out.println(String.format("binFile processed      channel=%d  MaxTime_sec=%,9d  Bytes=%,11d %s", this.channels.getActiveChannelNumber(), (int) recordSet.getMaxTime_ms() / 1000,
										file.length(), file.toPath().toAbsolutePath().toString()));
							}
							catch (Exception e) {
								e.printStackTrace();
								failures.put(file.getAbsolutePath(), e);
							}
						}
						if (maxTime_sec < 60 && !failures.containsKey(file.getAbsolutePath())) {
							System.out.println(String.format("WARNING: binFile too small  MaxTime_sec= %,6d  Bytes=%,11d %s", maxTime_sec, file.length(), file.getAbsolutePath()));
							failures.put(file.getAbsolutePath(),
									new OperationNotSupportedException(device.getName() + String.format(" WARNING: binFile too small  MaxTime_sec= %,6d  Bytes=%,11d", maxTime_sec, file.length())));
						}

					}
					else if (file.getName().endsWith(GDE.FILE_ENDING_DOT_OSD)) {
						HistoSetTest.count++;
						HashMap<String, String> header = null;
						try {
							header = HistoOsdReaderWriter.getHeader(file.getAbsolutePath());
						}
						catch (Exception e) {
							System.out.println(file.getAbsolutePath());
							e.printStackTrace();
							failures.put(file.getAbsolutePath(), e);
						}
						if (header != null) {
							IDevice device = setDevice(header.get(GDE.DEVICE_NAME));
							if (device == null) {
								failures.put(file.getAbsolutePath(), new OperationNotSupportedException(">" + header.get(GDE.DEVICE_NAME) + "<  device error: probably missing device XML"));
							}
							else {
								int maxTime_sec = 0;
								setupDataChannels(device);
								for (Entry<Integer, Channel> channelEntry : this.channels.entrySet()) {
									this.channels.setActiveChannelNumber(channelEntry.getKey());
									try {
										List<HistoRecordSet> recordSets = new ArrayList<>();
										HistoOsdReaderWriter.readHisto(recordSets, file.toPath());
										if (recordSets.size() > 0) {
											maxTime_sec = recordSets.get(recordSets.size() - 1).getMaxTime_ms() / 1000 > maxTime_sec ? (int) recordSets.get(recordSets.size() - 1).getMaxTime_ms() / 1000 : maxTime_sec;
											System.out.println(String.format("osdFile processed %3d  channel=%d  MaxTime_sec=%,9d  Bytes=%,11d %s", recordSets.size(), this.channels.getActiveChannelNumber(),
													(int) recordSets.get(recordSets.size() - 1).getMaxTime_ms() / 1000, file.length(), file.toPath().toAbsolutePath().toString()));
										}
										else {
											System.out.println(String.format("osdFile w/o recordsets channel=%d                         Bytes=%,11d %s", this.channels.getActiveChannelNumber(), file.length(),
													file.toPath().toAbsolutePath().toString()));
										}
									}
									catch (Exception e) {
										System.out.println(file.getAbsolutePath());
										e.printStackTrace();
										failures.put(file.getAbsolutePath(), e);
									}
								}
								if (maxTime_sec <= 0 && !failures.containsKey(file.getAbsolutePath())) {
									failures.put(file.getAbsolutePath(), new OperationNotSupportedException("WARNING: maxTime_sec = 0"));
								}
							}
						}
					}
				}
			}
		}
		catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail(e.toString());
		}

		System.out.println(String.format("%,11d files processed from  %s", HistoSetTest.count, dirPath.toString()));
		StringBuilder sb = new StringBuilder();
		for (Entry<String, Exception> failure : failures.entrySet()) {
			sb.append(failure).append("\n");
		}
//		System.out.println(sb);
		if (failures.size() > 0) fail(sb.toString());
	}

	/**
	 * @param fileDeviceName
	 * @return
	 */
	private IDevice setDevice(String fileDeviceName) {
		if (this.legacyDeviceNames.get(fileDeviceName) != null) fileDeviceName = this.legacyDeviceNames.get(fileDeviceName);
		if (fileDeviceName.toLowerCase().contains("hottviewer") || fileDeviceName.toLowerCase().contains("mpu")) return null;
		DeviceConfiguration deviceConfig = this.deviceConfigurations.get(fileDeviceName);
		if (deviceConfig == null) return null;
		IDevice device = this.getInstanceOfDevice(deviceConfig);
		this.application.setActiveDeviceWoutUI(device);
		return device;
	}

}
