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

package de.unihalle.informatik.MiToBo.core.dataio.provider.cmdline;

import de.unihalle.informatik.Alida.annotations.ALDDataIOProvider;
import de.unihalle.informatik.Alida.dataio.provider.ALDDataIOCmdline;
import de.unihalle.informatik.Alida.dataio.provider.cmdline.ALDStandardizedDataIOCmdline;
import de.unihalle.informatik.Alida.dataio.provider.swing.components.ALDTableWindow;
import de.unihalle.informatik.Alida.exceptions.ALDDataIOManagerException;
import de.unihalle.informatik.Alida.exceptions.ALDDataIOProviderException;
import de.unihalle.informatik.Alida.exceptions.ALDDataIOProviderException.ALDDataIOProviderExceptionType;
import de.unihalle.informatik.MiToBo.apps.cells2D.*;
import de.unihalle.informatik.MiToBo.gui.MTBTableModel;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Field;
import java.util.*;

/**
 * Class for loading/saving MTBTableModel data objects via commandline.
 * 
 * @author moeller
 */
@ALDDataIOProvider
public class MTBTableModelDataIO extends ALDStandardizedDataIOCmdline {

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
	public Object parse(Field field, Class<?> cl, String valueString)
			throws ALDDataIOProviderException, ALDDataIOManagerException {
		throw new ALDDataIOProviderException( ALDDataIOProviderExceptionType.UNSPECIFIED_ERROR,
				"MTBTabelModelDataIO::readData not yet implemented");
	}

	@Override
	public String formatAsString(Object obj) throws ALDDataIOManagerException,
			ALDDataIOProviderException {
		if (!(obj instanceof DefaultTableModel)) {
			throw new ALDDataIOProviderException( ALDDataIOProviderExceptionType.OBJECT_TYPE_ERROR,
			"MTBTabelModelDataIO::writeData object to write is of wrong type <" +
					obj.getClass().getCanonicalName() + ">");
		}
		
		StringBuffer[] rows = ALDTableWindow.tableToString((DefaultTableModel)obj);
		StringBuffer resultString = new StringBuffer();
		for (StringBuffer s : rows) {
			resultString.append(s);
		}
		return resultString.toString();
	}
}
