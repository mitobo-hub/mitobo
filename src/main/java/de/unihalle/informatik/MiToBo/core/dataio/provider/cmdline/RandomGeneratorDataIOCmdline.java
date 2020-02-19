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

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import de.unihalle.informatik.Alida.annotations.ALDDataIOProvider;
import de.unihalle.informatik.Alida.dataio.provider.cmdline.ALDStandardizedDataIOCmdline;
import de.unihalle.informatik.Alida.exceptions.ALDDataIOProviderException;
import de.unihalle.informatik.Alida.exceptions.ALDDataIOProviderException.ALDDataIOProviderExceptionType;

/**
 * Data I/O provider for ALDOpRunner (commandline) for <code>java.util.Random</code> objects.
 * <p>
 * When a random generator is to be created from an input string, it is initialized with a seed
 * if the string represents a Long integer. If the string cannot be parsed as Long, e.g. because it is not a numerical
 * value, a random generator is created without seed.
 * <br>
 * When a random generator has to be written, i.e. it will be formated as string, that string represents the random
 * generator's seed. Note that the current state of the random generator is not represented, i.e. if a new random generator
 * is constructed from this string, it will share the same seed, but will have the initial state of the old random generator.
 * <p>
 * TODO: Consider serialization
 * 
 * 
 * @author Oliver Gress
 *
 */
@ALDDataIOProvider
public class RandomGeneratorDataIOCmdline extends ALDStandardizedDataIOCmdline {

	@Override
	public Collection<Class<?>> providedClasses() {
		LinkedList<Class<?>> l = new LinkedList<Class<?>>();
		l.add(Random.class);
		return l;
	}

	/**
	 * Convert a string to a Random generator. If the string contains a Long integer, its value is 
	 * used as seed of the Random generator. If the string cannot be parsed as Long, no seed is used
	 * to construct the Random generator.
	 * @throws ALDDataIOProviderException 
	 */
	@Override
	public Object parse(Field field, Class<?> cl, String valueString) throws ALDDataIOProviderException {
		Random rand = null;
		
		try {
			rand = new Random(Long.parseLong(valueString));
		} catch (NumberFormatException e) {
			System.err.println("WARNING: Seed for Random generator cannot be parsed as Long. Creating unseeded Random generator.");
			throw new ALDDataIOProviderException( ALDDataIOProviderExceptionType.SYNTAX_ERROR,
					"RandomGeneratorDataIOCmdline::parse Seed for Random generator cannot be parsed as Long from " + valueString + "\n" +
							e.getMessage());
		}
		
		return rand;
	}

	/**
	 * Get a string representing the seed of the specified Random generator.
	 * @throws ALDDataIOProviderException 
	 */
	@Override
	public String formatAsString(Object obj) throws ALDDataIOProviderException {
		Random rand = (Random)obj;

		try {
			Class<?> cls = Class.forName("Random");
			Field fld = cls.getField("seed");
			AtomicLong al = (AtomicLong) fld.get(rand);
			
			return al.toString();
		} catch (Exception e) {
			throw new ALDDataIOProviderException( ALDDataIOProviderExceptionType.UNSPECIFIED_ERROR,
					"RandomGeneratorDataIOCmdline::formatAsString Cannot format " + obj + " as random numer\n" +
							e.getMessage());
		}
	}

}
