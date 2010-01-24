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
package osde.device.wb;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import osde.data.Channels;
import osde.device.IDevice;
import osde.log.Level;
import osde.messages.Messages;
import osde.ui.MeasurementControl;
import osde.ui.OpenSerialDataExplorer;

/**
 * This class represents a tab item of a universal record visualization control
 * @author Winfried Brügmann
 */
public class CSV2SerialAdapterDialogTabItem extends CTabItem {
	final static Logger						log											= Logger.getLogger(CSV2SerialAdapterDialogTabItem.class.getName());

	Composite											measurementComposite;
	Button												measurement;
	Button												inputFileButton;
	Composite											buttonComposite;
	Label													measurementUnitLabel;
	Label													measurementSymbolLabel;
	Label													tabItemLabel;
	Composite											mainTabComposite;

	boolean												isVisibilityChanged			= false;

	final CTabFolder							parent;
	final IDevice									device;																																						// get device specific things, get serial port, ...
	final OpenSerialDataExplorer	application;																																			// interaction with application instance
	final Channels								channels;																																					// interaction with channels, source of all records
	final CSV2SerialAdapterDialog					dialog;
	final int											channelConfigNumber;
	final List<MeasurementControl>	measurementTypes= new ArrayList<MeasurementControl>();

	public CSV2SerialAdapterDialogTabItem(CTabFolder parentTabFolder, CSV2SerialAdapterDialog parentDialog, int useChannelConfigNumber, IDevice useDevice) {
		super(parentTabFolder, SWT.NONE);
		this.parent = parentTabFolder;
		this.dialog = parentDialog;
		this.device = useDevice;
		this.application = OpenSerialDataExplorer.getInstance();
		this.channels = Channels.getInstance();
		this.channelConfigNumber = useChannelConfigNumber;
		this.setText(this.device.getChannelName(channelConfigNumber));

		create();
	}

	void create() {
		mainTabComposite = new Composite(this.parent, SWT.NONE);
		GridLayout mainTabCompositeLayout = new GridLayout();
		mainTabCompositeLayout.makeColumnsEqualWidth = true;
		mainTabComposite.setLayout(mainTabCompositeLayout);
		this.setControl(mainTabComposite);
		{
			tabItemLabel = new Label(mainTabComposite, SWT.CENTER);
			GridData tabItemLabelLData = new GridData();
			tabItemLabelLData.horizontalAlignment = GridData.BEGINNING;
			tabItemLabelLData.verticalAlignment = GridData.BEGINNING;
			tabItemLabelLData.heightHint = 30;
			tabItemLabelLData.widthHint = 292;
			tabItemLabel.setLayoutData(tabItemLabelLData);
			tabItemLabel.setText(Messages.getString(MessageIds.OSDE_MSGT1801));
		}
		{
				// 0=voltageReceiver, 1=voltage, 2=current, 3=capacity, 4=power, 5=energy, 6=votagePerCell, 7=revolutionSpeed, 8=efficiency, 9=height, 10=slope, 11=a1Value, 12=a2Value, 13=a3Value
				for (int i = 0; i < device.getChannelMeasuremts(channelConfigNumber).size(); i++) {
					measurementTypes.add(new MeasurementControl(mainTabComposite, dialog, i, device.getChannelMeasuremts(channelConfigNumber).get(i), device));
				}
		}
		{
			buttonComposite = new Composite(mainTabComposite, SWT.NONE);
			GridData buttonCompositeLData = new GridData();
			buttonCompositeLData.verticalAlignment = GridData.BEGINNING;
			buttonCompositeLData.horizontalAlignment = GridData.BEGINNING;
			buttonCompositeLData.heightHint = 60;
			buttonCompositeLData.grabExcessHorizontalSpace = true;
			buttonComposite.setLayoutData(buttonCompositeLData);
			FormLayout buttonCompositeLayout = new FormLayout();
			buttonComposite.setLayout(buttonCompositeLayout);
			{
				inputFileButton = new Button(buttonComposite, SWT.PUSH | SWT.CENTER);
				FormData inputFileButtonLData = new FormData();
				inputFileButtonLData.width = 188;
				inputFileButtonLData.height = 26;
				inputFileButtonLData.left = new FormAttachment(183, 1000, 0);
				inputFileButtonLData.right = new FormAttachment(827, 1000, 0);
				inputFileButtonLData.top = new FormAttachment(150, 1000, 0);
				inputFileButtonLData.bottom = new FormAttachment(1016, 1000, 0);
				buttonCompositeLData.heightHint = 30;
				buttonCompositeLData.widthHint = 292;
				inputFileButton.setLayoutData(inputFileButtonLData);
				inputFileButton.setText(Messages.getString(MessageIds.OSDE_MSGT1802));
				inputFileButton.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent evt) {
						log.log(Level.FINEST, "inputFileButton.widgetSelected, event=" + evt); //$NON-NLS-1$
						if (isVisibilityChanged) {
							String msg = Messages.getString(osde.messages.MessageIds.OSDE_MSGI0041);
							if (application.openYesNoMessageDialog(dialog.getDialogShell(), msg) == SWT.YES) {
								log.log(Level.FINE, "SWT.YES"); //$NON-NLS-1$
								device.storeDeviceProperties();
							}
						}
						device.openCloseSerialPort();
					}
				});
			}
		}
	}
}
