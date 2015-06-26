/*
 * This file is part of MiToBo, the Microscope Image Analysis Toolbox.
 *
 * Copyright (C) 2010 - @YEAR@
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Fore more information on MiToBo, visit
 *
 *    http://www.informatik.uni-halle.de/mitobo/
 *
 */

/* 
 * Most recent change(s):
 * 
 * $Rev$
 * $Date$
 * $Author$
 * 
 */

package de.unihalle.informatik.MiToBo.core.dataio.provider.swing;

import de.unihalle.informatik.Alida.annotations.ALDDataIOProvider;
import de.unihalle.informatik.Alida.dataio.provider.ALDDataIOSwing;
import de.unihalle.informatik.Alida.dataio.provider.swing.components.ALDSwingComponent;
import de.unihalle.informatik.Alida.dataio.provider.swing.components.ALDSwingComponentLabel;
import de.unihalle.informatik.Alida.dataio.provider.swing.components.ALDTableWindow;
import de.unihalle.informatik.Alida.exceptions.*;
import de.unihalle.informatik.Alida.exceptions.ALDDataIOProviderException.ALDDataIOProviderExceptionType;
import de.unihalle.informatik.Alida.operator.ALDParameterDescriptor;
import de.unihalle.informatik.MiToBo.apps.cells2D.*;
import de.unihalle.informatik.MiToBo.gui.MTBTableModel;

import javax.swing.*;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Field;
import java.util.*;

/**
 * Class for loading/saving MTBTableModel data objects.
 * 
 * @author moeller
 */
@ALDDataIOProvider
public class MTBTableModelDataIOSwing implements ALDDataIOSwing {

	/**
	 * Interface method to announce class for which IO is provided for.
	 * 
	 * @return  Collection of classes provided.
	 */
	@Override
	public Collection<Class<?>> providedClasses() {
		LinkedList<Class<?>> classes = new LinkedList<Class<?>>();
		classes.add(MTBTableModel.class);
		classes.add(Mica2DTableModel.class);
		return classes;
	}
	
	@Override
  public Object getInitialGUIValue(Field field, Class<?> cl, Object obj, 
  		ALDParameterDescriptor descr) {
		return null;
	}
	
	/** 
	 * Generic reading of table models.
	 */
	/* (non-Javadoc)
	 * @see de.unihalle.informatik.Alida.helpers.ALDDataIOSwing#createGUIElement(java.lang.Class, java.lang.Object)
	 */
	@Override
	public ALDSwingComponent createGUIElement(Field field, Class<?> cl, 
			Object obj, ALDParameterDescriptor descr) {
		return new ALDSwingComponentLabel(
				"Table model inputs are not supported yet!");
	}

  @Override
	@SuppressWarnings("unused")
  public void setValue(Field field, Class<?> cl, 
  		ALDSwingComponent guiElement, Object value) 
  	throws ALDDataIOProviderException {
	  // TODO Auto-generated method stub
	  
  }

	@Override
	public Object readData(Field field, Class<?> cl,
			ALDSwingComponent guiElem) {
		return null;
	}

	@Override
	public JComponent writeData(Object obj, ALDParameterDescriptor descr) 
			throws ALDDataIOProviderException {
		if (!(obj instanceof MTBTableModel))
			throw new ALDDataIOProviderException(
					ALDDataIOProviderExceptionType.OBJECT_TYPE_ERROR, 
					"MTBTabelModelDataIO: object to write has wrong type!");
		// return a button to show a window with the elements
		return new TableModelShowButton(obj);
	}

//	/**
//	 * GUI element for configuring collections.
//	 * <p>
//	 * This button has a collection configuration window attached to it 
//	 * where specific data is stored and accessable.
//	 * 
//	 * @author moeller
//	 */
//	private class CollectionConfigButton extends JButton {
//
//		/**
//		 * Collection configuration window.
//		 */
//		private CollectionConfigWindow confWin;
//
//		/**
//		 * Constructor.
//		 * 
//		 * @param field	Field of collection.
//		 * @param cl	Class of collection.
//		 * @param obj	Default object.
//		 */
//		public CollectionConfigButton(Field field, Class<?> cl, Object obj) {
//			super("Configure Collection...");
//			this.confWin = new CollectionConfigWindow(field, cl, obj);
//			this.setActionCommand("configButtonPressed");
//			this.addActionListener(this.confWin);
//		}
//		
//		/**
//		 * Gets the data from the configuration window.
//		 * 
//		 * @param field	Field of collection.
//		 * @param cl	Class of collection.
//		 * @param obj	Default object.
//		 * @return Current data.
//		 */
//		public Object readData(Field field, Class<?> cl) {
//			return this.confWin.readData(field, cl);
//		}
//	}	
	
	/**
	 * GUI element for displaying table model data.
	 * <p>
	 * This button opens a window with a table containing the data.
	 * 
	 * @author moeller
	 */
	private class TableModelShowButton extends JButton 
		implements ActionListener{

		/**
		 * Data to be displayed.
		 */
		private MTBTableModel data = null;

		/**
		 * Associated window.
		 */
		private ALDTableWindow tabwin = null;
		
		/**
		 * Constructor.
		 * @param obj Table object associated with button.
		 */
		public TableModelShowButton(Object obj) {
			super("Show data...");
			this.setActionCommand("showButtonPressed");
			this.addActionListener(this);
			this.data = (MTBTableModel)obj;
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			String cmd = e.getActionCommand(); 
			if (cmd.equals("showButtonPressed")) {
				if (this.data != null) {
					if (this.tabwin == null) {
						this.tabwin = new ALDTableWindow(this.data);
						this.tabwin.setTitle("MTBTableModel data:");
						this.tabwin.setVisible(true);
					}
					else
						this.tabwin.setVisible(true);
				}
				else {
					Object[] options = { "OK" };
					JOptionPane.showOptionDialog(null, 
							"There is no data to display!", "Warning", 
							JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
							null, options, options[0]);
				}	
			}	
		}
	}
}
