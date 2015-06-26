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
 * $Rev: 5463 $
 * $Date: 2012-04-17 17:17:15 +0200 (Tue, 17 Apr 2012) $
 * $Author: moeller $
 * 
 */

package de.unihalle.informatik.MiToBo.core.dataio.provider.cmdline;

import java.awt.Color;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo;
import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo.ExportPolicy;
import de.unihalle.informatik.Alida.annotations.ALDDataIOProvider;
import de.unihalle.informatik.Alida.dataio.provider.cmdline.ALDStandardizedDataIOCmdline;
import de.unihalle.informatik.Alida.exceptions.ALDDataIOProviderException;
import de.unihalle.informatik.Alida.exceptions.ALDDataIOProviderException.ALDDataIOProviderExceptionType;

/**
 * Data I/O provider for Cmdline-OpRunner for <code>java.awt.Color</code> objects.
 * <p>
 * java.awt.Color object are represented by a string in the following format:<br>
 * <br>
 * "rINT:bINT:gINT[:aINT]"
 * <br><br>
 * where INT is an integer value in the range [0,255]. The specification of an alpha-value (aINT)
 * is optional and defaults to a255 if not specified.
 * <p>
 * TODO: Change to more readable format that is valid as argument for ALDOpRunner !!
 * 
 * @author Oliver Gress
 * 
 *
 */
@ALDDataIOProvider
public class AwtColorDataIOCmdline extends ALDStandardizedDataIOCmdline {

	@Override
	public Collection<Class<?>> providedClasses() {
		LinkedList<Class<?>> l = new LinkedList<Class<?>>();
		l.add(Color.class);
		return l;
	}

	/**
	 * A java.awt.Color object is created from a string, which holds color information
	 * in the following format:<br>
	 * <br>
	 * "rINT:bINT:gINT[:aINT]"
	 * <br><br>
	 * where INT is an integer value in the range [0,255]. The specification of an alpha-value (aINT)
	 * is optional and defaults to a255 if not specified.
	 * @throws ALDDataIOProviderException 
	 * 
	 */
	@Override
	public Object parse(Field field, Class<?> cl, String valueString) throws ALDDataIOProviderException {
		int r,g,b,a;
		
		// --- get RED ---
		Pattern p = Pattern.compile("(^\\s*|.*:\\s*)r(\\d+)(\\s*$|\\s*:.*)");
		Matcher m = p.matcher(valueString);
		
		if (m.matches()) {
			r = Integer.parseInt(m.group(2));
		}
		else 
			throw new ALDDataIOProviderException( ALDDataIOProviderExceptionType.SYNTAX_ERROR,
					"AwtColorDataIOCmdline::parse Cannot be parse red color from " + valueString);
		
		// --- get GREEN ---
		p = Pattern.compile("(^\\s*|.*:\\s*)g(\\d+)(\\s*$|\\s*:.*)");
		m = p.matcher(valueString);
		
		if (m.matches()) {
			g = Integer.parseInt(m.group(2));
		}
		else 
			throw new ALDDataIOProviderException( ALDDataIOProviderExceptionType.SYNTAX_ERROR,
					"AwtColorDataIOCmdline::parse Cannot be parse green color from " + valueString);
		
		// --- get BLUE ---
		p = Pattern.compile("(^\\s*|.*:\\s*)b(\\d+)(\\s*$|\\s*:.*)");
		m = p.matcher(valueString);
		
		if (m.matches()) {
			b = Integer.parseInt(m.group(2));			
		}
		else 
			throw new ALDDataIOProviderException( ALDDataIOProviderExceptionType.SYNTAX_ERROR,
					"AwtColorDataIOCmdline::parse Cannot be parse blue color from " + valueString);
		
		// --- get ALPHA ---
		p = Pattern.compile("(^\\s*|.*:\\s*)a(\\d+)(\\s*$|\\s*:.*)");
		m = p.matcher(valueString);
		
		if (m.matches())
			a = Integer.parseInt(m.group(2));
		else 
			a = 255;

		return new Color(r,g,b,a);
	}

	/**
	 * Converts an java.awt.Color object to a string-representation in the format "rINT:bINT:gINT:aINT".
	 * See {@link #readData(Field, Class, String)} for further format information.
	 */
	@Override
	public String formatAsString(Object obj) {
		Color c = (Color)obj;
		String s = "r" + c.getRed() + ":g" + c.getGreen() + ":b" + c.getBlue() + ":a" + c.getAlpha();
		return s;
	}

}
