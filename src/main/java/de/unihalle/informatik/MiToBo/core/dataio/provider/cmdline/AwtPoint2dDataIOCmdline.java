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

package de.unihalle.informatik.MiToBo.core.dataio.provider.cmdline;

import java.awt.geom.Point2D;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.LinkedList;
import de.unihalle.informatik.Alida.annotations.ALDDataIOProvider;
import de.unihalle.informatik.Alida.dataio.provider.cmdline.ALDStandardizedDataIOCmdline;
import de.unihalle.informatik.Alida.exceptions.ALDDataIOProviderException;
import de.unihalle.informatik.Alida.exceptions.ALDDataIOProviderException.ALDDataIOProviderExceptionType;

/**
 * Data I/O provider for Cmdline-OpRunner for <code>java.awt.geom.Point2D.Double</code> objects.
 * <p>
 * java.awt.geom.Point2D.Double object are represented by a string in the following format:<br>
 * <br>
 * <code>(x;y)</code>
 * 
 * 
 * @author Stefan Posch
 * 
 *
 */
@ALDDataIOProvider
public class AwtPoint2dDataIOCmdline extends ALDStandardizedDataIOCmdline {

	@Override
	public Collection<Class<?>> providedClasses() {
		LinkedList<Class<?>> l = new LinkedList<Class<?>>();
		l.add(Point2D.Double.class);
		return l;
	}

	/**
	 * A java.awt.geom.Point2D.Double object is created from a string, which holds coordinates
	 * in the following format:<br>
	 * <br>
	 * "x;y"
	 * "rINT:bINT:gINT[:aINT]"
	 * <br><br>
	 * where INT is an integer value in the range [0,255]. The specification of an alpha-value (aINT)
	 * is optional and defaults to a255 if not specified.
	 * @throws ALDDataIOProviderException 
	 * 
	 */
	@Override
	public Object parse(Field field, Class<?> cl, String valueString) throws ALDDataIOProviderException {
		String [] elements = null;
		String trimmedStr = valueString.trim();

		if ( trimmedStr.startsWith("(")) {
			trimmedStr = trimmedStr.substring(1);
		} else {
			throw new ALDDataIOProviderException( ALDDataIOProviderExceptionType.SYNTAX_ERROR,
					"AwtPoint2dDataIOCmdline::parse Cannot be parse Point2D from " + valueString + " - does not start with (");
		}

		if (trimmedStr.endsWith(")")) {
			trimmedStr = trimmedStr.substring(0, trimmedStr.length()-1);
		} else {
			throw new ALDDataIOProviderException( ALDDataIOProviderExceptionType.SYNTAX_ERROR,
					"AwtPoint2dDataIOCmdline::parse Cannot be parse Point2D from " + valueString + " - does not end with )");
		}

		elements = trimmedStr.split(";");
		
		// split string into elements and do security check
		if (elements == null) {
			throw new ALDDataIOProviderException( ALDDataIOProviderExceptionType.SYNTAX_ERROR,
					"AwtPoint2dDataIOCmdline::parse Cannot be parse Point2D from " + valueString + " - contains no ;");
		}

		if ( elements.length != 2) {
			throw new ALDDataIOProviderException( ALDDataIOProviderExceptionType.SYNTAX_ERROR,
					"AwtPoint2dDataIOCmdline::parse Cannot be parse Point2D from " + valueString + " - contains " + 
							elements.length + " elements instead of two");
		}
		
		Double x = Double.valueOf( elements[0]);
		Double y = Double.valueOf( elements[1]);
		return new Point2D.Double(x, y);
	}

	/**
	 * Converts an java.awt.geom.Point2D.Double object to a string-representation in the format <code>(x;y)</code>.
	 * See {@link #readData(Field, Class, String)} for further format information.
	 */
	@Override
	public String formatAsString(Object obj) {
		Point2D.Double point = (Point2D.Double)obj;
		String s = "(" + point.getX() + ";" + point.getY() + ")";
		return s;
	}

}
