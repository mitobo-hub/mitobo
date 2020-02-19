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

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.LinkedList;

import de.unihalle.informatik.Alida.annotations.ALDDataIOProvider;
import de.unihalle.informatik.Alida.dataio.provider.cmdline.ALDStandardizedDataIOCmdline;
import de.unihalle.informatik.Alida.exceptions.ALDDataIOProviderException;
import de.unihalle.informatik.Alida.exceptions.ALDDataIOProviderException.ALDDataIOProviderExceptionType;
import de.unihalle.informatik.MiToBo.core.datatypes.wrapper.MTBDoubleData;
import de.unihalle.informatik.MiToBo.core.datatypes.wrapper.MTBIntegerData;
import de.unihalle.informatik.MiToBo.core.datatypes.wrapper.MTBStringData;

/**
 * Class for loading/saving primitive datatypes in Alida.
 * 
 * @author moeller
 *
 */
@ALDDataIOProvider
public class MTBWrapperDataIOCmdline extends ALDStandardizedDataIOCmdline {

    /**
     * Interface method to announce class for which IO is provided for
	 * field is ignored.
     * 
     * @return  Collection of classes provided
     */
	@Override
    public Collection<Class<?>> providedClasses() {
		LinkedList<Class<?>> classes = new LinkedList<Class<?>>();

		classes.add( MTBDoubleData.class);
		classes.add( MTBStringData.class);
		classes.add( MTBIntegerData.class);
		return classes;
	}
	
	/**
	 * Method to parse MTB wrapper data from a string.
	 * @throws ALDDataIOProviderException 
	 * 
	 */
	@Override
  	public Object parse(Field field, Class<?> cl, String iname) throws ALDDataIOProviderException {
		try {
	
			// MiToBo wrapper
			if (cl.equals(MTBDoubleData.class))
				return new MTBDoubleData( Double.valueOf(iname));
			if (cl.equals(MTBIntegerData.class))
				return new MTBIntegerData( Integer.valueOf(iname));
			if (cl.equals(MTBStringData.class))
				return new MTBStringData( iname);
	
			throw new ALDDataIOProviderException( ALDDataIOProviderExceptionType.OBJECT_TYPE_ERROR,
					"MTBWrapperDataIOCmdline::parse cannot parse, " + cl.getName() + " is not supported");
		} catch (Exception e) {
			throw new ALDDataIOProviderException( ALDDataIOProviderExceptionType.SYNTAX_ERROR,
					"MTBWrapperDataIOCmdline::parse cannot parse " + cl.getName() + " from " + iname + "\n" +
							e.getMessage());
		}
	}
	
	/**
	 * Format the given object value to a string
	 * @throws ALDDataIOProviderException 
	 */
	@Override
  	public String formatAsString(Object obj) throws ALDDataIOProviderException {
		String str = null;

		if ( obj.getClass().equals(MTBDoubleData.class))
			str = new String(((MTBDoubleData)obj).getValue().toString() + "\n");
		else if ( obj.getClass().equals(MTBIntegerData.class))
			str = new String(((MTBIntegerData)obj).getValue().toString() + "\n");
		else if ( obj.getClass().equals(MTBStringData.class))
			str = new String(((MTBStringData)obj).getString() + "\n");
		else 
			throw new ALDDataIOProviderException( ALDDataIOProviderExceptionType.OBJECT_TYPE_ERROR,
					"MTBWrapperDataIOCmdline::formatAsString cannot format, " + obj.getClass().getName() + " is not supported");

		return str;
	}
}
