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
    
    Copyright (c) 2008,2009,2010,2011,2012,2013,2014 Winfried Bruegmann
****************************************************************************************/
package gde.ui.menu;

import gde.config.Settings;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;
import gde.ui.tab.GraphicsWindow;

import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

/**
 * @author Winfried Brügmann
 * This class provides a context menu to tabulator area, curve graphics, compare window, etc. and enable selection of background color, ...
 */
public class TabAreaContextMenu {
	final static Logger						log	= Logger.getLogger(TabAreaContextMenu.class.getName());
	
	public final static int	TYPE_GRAPHICS	= GraphicsWindow.TYPE_NORMAL;
	public final static int	TYPE_COMPARE	= GraphicsWindow.TYPE_COMPARE;
	public final static int	TYPE_UTILITY	= GraphicsWindow.TYPE_UTIL;
	public final static int	TYPE_SIMPLE		= 3;														//only referenced in not specified else clause
	public final static int	TYPE_TABLE		= 4;														
	
	final DataExplorer						application;

	MenuItem											curveSelectionItem;
	MenuItem											displayGraphicsHeaderItem;
	MenuItem											displayGraphicsCommentItem;
	MenuItem											separatorView;
	MenuItem											copyTabItem;
	MenuItem											copyPrintImageItem;
	MenuItem											separatorCopy;
	MenuItem											outherAreaColorItem;
	MenuItem											innerAreaColorItem;
	MenuItem											borderColorItem;
	MenuItem											dateTimeItem;
	MenuItem											partialTableItem;
	boolean												isCreated = false;

	public TabAreaContextMenu() {
		this.application = DataExplorer.getInstance();
	}

	public void createMenu(Menu popupMenu, int type) {
		popupMenu.addMenuListener(new MenuListener() {
			public void menuShown(MenuEvent e) {
				int tabSelectionIndex = TabAreaContextMenu.this.application.getTabSelectionIndex();
				if (tabSelectionIndex == 0) {
					TabAreaContextMenu.this.curveSelectionItem.setSelection(TabAreaContextMenu.this.application.getMenuBar().curveSelectionMenuItem.getSelection());
					TabAreaContextMenu.this.displayGraphicsHeaderItem.setSelection(TabAreaContextMenu.this.application.getMenuBar().graphicsHeaderMenuItem.getSelection());
					TabAreaContextMenu.this.displayGraphicsCommentItem.setSelection(TabAreaContextMenu.this.application.getMenuBar().recordCommentMenuItem.getSelection());
				}
			}
			public void menuHidden(MenuEvent e) {
				//ignore
			}
		});
		if (!isCreated) {
			if (type == TYPE_GRAPHICS) { // -1 as index mean initialization phase
				this.curveSelectionItem = new MenuItem(popupMenu, SWT.CHECK);
				this.curveSelectionItem.setText(Messages.getString(MessageIds.GDE_MSGT0040));
				this.curveSelectionItem.addListener(SWT.Selection, new Listener() {
					public void handleEvent(Event e) {
						TabAreaContextMenu.log.log(Level.FINEST, "curveSelectionItem action performed! " + e); //$NON-NLS-1$
						boolean selection = TabAreaContextMenu.this.curveSelectionItem.getSelection();
						TabAreaContextMenu.this.application.setCurveSelectorEnabled(selection);
						TabAreaContextMenu.this.application.getMenuBar().curveSelectionMenuItem.setSelection(selection);
					}
				});
				this.displayGraphicsHeaderItem = new MenuItem(popupMenu, SWT.CHECK);
				this.displayGraphicsHeaderItem.setText(Messages.getString(MessageIds.GDE_MSGT0041));
				this.displayGraphicsHeaderItem.addListener(SWT.Selection, new Listener() {
					public void handleEvent(Event e) {
						TabAreaContextMenu.log.log(Level.FINEST, "toggleViewGraphicsHeaderItem action performed! " + e); //$NON-NLS-1$
						boolean selection = TabAreaContextMenu.this.displayGraphicsHeaderItem.getSelection();
						TabAreaContextMenu.this.application.getMenuBar().graphicsHeaderMenuItem.setSelection(selection);
						TabAreaContextMenu.this.application.enableGraphicsHeader(selection);
						TabAreaContextMenu.this.application.updateDisplayTab();
					}
				});
				this.displayGraphicsCommentItem = new MenuItem(popupMenu, SWT.CHECK);
				this.displayGraphicsCommentItem.setText(Messages.getString(MessageIds.GDE_MSGT0042));
				this.displayGraphicsCommentItem.addListener(SWT.Selection, new Listener() {
					public void handleEvent(Event e) {
						TabAreaContextMenu.log.log(Level.FINEST, "toggleViewGraphicsCommentItem action performed! " + e); //$NON-NLS-1$
						boolean selection = TabAreaContextMenu.this.displayGraphicsCommentItem.getSelection();
						TabAreaContextMenu.this.application.getMenuBar().recordCommentMenuItem.setSelection(selection);
						TabAreaContextMenu.this.application.enableRecordSetComment(selection);
						TabAreaContextMenu.this.application.updateDisplayTab();
					}
				});
				this.separatorView = new MenuItem(popupMenu, SWT.SEPARATOR);
			}

			this.copyTabItem = new MenuItem(popupMenu, SWT.PUSH);
			this.copyTabItem.setText(Messages.getString(MessageIds.GDE_MSGT0026).substring(0,Messages.getString(MessageIds.GDE_MSGT0026).lastIndexOf('\t')));
			this.copyTabItem.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					TabAreaContextMenu.log.log(Level.FINEST, "copyTabItem action performed! " + e); //$NON-NLS-1$
					TabAreaContextMenu.this.application.copyTabContentAsImage();
				}
			});
			
			if (type == TYPE_GRAPHICS || type == TYPE_COMPARE || type == TYPE_UTILITY) {
				this.copyPrintImageItem = new MenuItem(popupMenu, SWT.PUSH);
				this.copyPrintImageItem.setText(Messages.getString(MessageIds.GDE_MSGT0027).substring(0,Messages.getString(MessageIds.GDE_MSGT0027).lastIndexOf('\t')));
				this.copyPrintImageItem.addListener(SWT.Selection, new Listener() {
					public void handleEvent(Event e) {
						TabAreaContextMenu.log.log(Level.FINEST, "copyPrintImageItem action performed! " + e); //$NON-NLS-1$
						TabAreaContextMenu.this.application.copyGraphicsPrintImage();
					}
				});
			}
			
			if (type != TYPE_TABLE) {
				this.separatorCopy = new MenuItem(popupMenu, SWT.SEPARATOR);
				this.outherAreaColorItem = new MenuItem(popupMenu, SWT.PUSH);
				this.outherAreaColorItem.setText(Messages.getString(MessageIds.GDE_MSGT0462));
				this.outherAreaColorItem.addListener(SWT.Selection, new Listener() {
					public void handleEvent(Event e) {
						TabAreaContextMenu.log.log(Level.FINEST, "outherAreaColorItem action performed! " + e); //$NON-NLS-1$
						RGB rgb = TabAreaContextMenu.this.application.openColorDialog();
						if (rgb != null) {
							TabAreaContextMenu.this.application.setSurroundingBackground(TabAreaContextMenu.this.application.getTabSelectionIndex(), SWTResourceManager.getColor(rgb.red, rgb.green, rgb.blue));
						}
					}
				});
				this.innerAreaColorItem = new MenuItem(popupMenu, SWT.PUSH);
				this.innerAreaColorItem.setText(Messages.getString(MessageIds.GDE_MSGT0463));
				this.innerAreaColorItem.addListener(SWT.Selection, new Listener() {
					public void handleEvent(Event e) {
						TabAreaContextMenu.log.log(Level.FINEST, "innerAreaColorItem action performed! " + e); //$NON-NLS-1$
						RGB rgb = TabAreaContextMenu.this.application.openColorDialog();
						if (rgb != null) {
							TabAreaContextMenu.this.application.setInnerAreaBackground(TabAreaContextMenu.this.application.getTabSelectionIndex(), SWTResourceManager.getColor(rgb.red, rgb.green, rgb.blue));
						}
					}
				});
			}
			
			if (type == TYPE_GRAPHICS || type == TYPE_COMPARE || type == TYPE_UTILITY) {
				this.borderColorItem = new MenuItem(popupMenu, SWT.PUSH);
				this.borderColorItem.setText(Messages.getString(MessageIds.GDE_MSGT0464));
				this.borderColorItem.addListener(SWT.Selection, new Listener() {
					public void handleEvent(Event e) {
						TabAreaContextMenu.log.log(Level.FINEST, "borderColorItem action performed! " + e); //$NON-NLS-1$
						RGB rgb = TabAreaContextMenu.this.application.openColorDialog();
						if (rgb != null) {
							TabAreaContextMenu.this.application.setBorderColor(TabAreaContextMenu.this.application.getTabSelectionIndex(), SWTResourceManager.getColor(rgb.red, rgb.green, rgb.blue));
						}
					}
				});
			}

			if (type == TYPE_TABLE) {
				this.dateTimeItem = new MenuItem(popupMenu, SWT.CHECK);
				this.dateTimeItem.setText(Messages.getString(MessageIds.GDE_MSGT0436));
				this.dateTimeItem.setSelection(Settings.getInstance().isTimeFormatAbsolute());
				this.dateTimeItem.addListener(SWT.Selection, new Listener() {
					public void handleEvent(Event e) {
						TabAreaContextMenu.log.log(Level.FINEST, "dateTimeItem action performed! " + e); //$NON-NLS-1$
						TabAreaContextMenu.this.application.setAbsoluteDateTime(TabAreaContextMenu.this.dateTimeItem.getSelection());
					}
				});
				this.partialTableItem = new MenuItem(popupMenu, SWT.CHECK);
				this.partialTableItem.setText(Messages.getString(MessageIds.GDE_MSGT0704));
				this.partialTableItem.setSelection(Settings.getInstance().isPartialDataTable());
				this.partialTableItem.addListener(SWT.Selection, new Listener() {
					public void handleEvent(Event e) {
						TabAreaContextMenu.log.log(Level.FINEST, "partialTableItem action performed! " + e); //$NON-NLS-1$
						Settings.getInstance().setPartialDataTable(TabAreaContextMenu.this.partialTableItem.getSelection());
						TabAreaContextMenu.this.application.updateAllTabs(true, false);
					}
				});
			}
			isCreated = true;
		}
	}
}
