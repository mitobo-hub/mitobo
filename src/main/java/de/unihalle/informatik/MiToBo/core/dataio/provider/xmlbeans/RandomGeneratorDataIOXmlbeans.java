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

package de.unihalle.informatik.MiToBo.core.dataio.provider.xmlbeans;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.xmlbeans.XmlLong;
import org.apache.xmlbeans.XmlObject;

import de.unihalle.informatik.Alida.annotations.ALDDataIOProvider;
import de.unihalle.informatik.Alida.dataio.provider.xmlbeans.ALDStandardizedDataIOXmlbeans;
import de.unihalle.informatik.Alida.exceptions.ALDDataIOManagerException;
import de.unihalle.informatik.Alida.exceptions.ALDDataIOProviderException;
import de.unihalle.informatik.Alida.exceptions.ALDDataIOProviderException.ALDDataIOProviderExceptionType;
import de.unihalle.informatik.Alida_xml.ALDXMLAnyType;
import de.unihalle.informatik.Alida_xml.ALDXMLObjectType;

/**
 * Xmlbeans  provider for<code>java.util.Random</code> objects.
 * <p>
 * When a random generator is to be read , it is initialized with a seed as read from xml.
 * <br>
 * When a random generator has to be written, its seed is saved to the xml file as along. Note that the current state of the random generator is not represented, i.e. if a new random generator
 * is constructed from this string, it will share the same seed, but will have the initial state of the old random generator.
 *  
 * @author posch
 *
 */

@ALDDataIOProvider
public class RandomGeneratorDataIOXmlbeans extends ALDStandardizedDataIOXmlbeans {

	@Override
	public Collection<Class<?>> providedClasses() {
		LinkedList<Class<?>> l = new LinkedList<Class<?>>();
		l.add(Random.class);
		return l;
	}

	@Override
	public Object readData(Field field, Class<?> cl, ALDXMLObjectType aldXmlObject, Object obj)
			throws ALDDataIOProviderException, ALDDataIOManagerException {
		if (cl == null)
			throw new ALDDataIOProviderException(ALDDataIOProviderExceptionType.OBJECT_TYPE_ERROR, 
					"RandomGeneratorDataIOXmlbeans::readData cl == null");

		if ( aldXmlObject == null || aldXmlObject.isNil())
			return null;

		if (cl.equals(Random.class) && 
				((ALDXMLAnyType)aldXmlObject).getClassName().equals(Random.class.getName()) ) {
			XmlLong xmlLong = ((XmlLong)((ALDXMLAnyType)aldXmlObject).getValue());
			Random random = new Random();
			try {
				Class<?> cls = Random.class;
				Field fld = cls.getDeclaredField("seed");
				fld.setAccessible(true);
				fld.set(random, new AtomicLong( xmlLong.getLongValue()));

				return random;
			} catch (Exception e) {
				throw new ALDDataIOProviderException( ALDDataIOProviderExceptionType.UNSPECIFIED_ERROR,
						"RandomGeneratorDataIOXmlbeans::readData Cannot access " + obj + " as random numer\n" +
								e.getMessage());
			}
		} else {
			throw new ALDDataIOProviderException(ALDDataIOProviderExceptionType.OBJECT_TYPE_ERROR, 
					"RandomGeneratorDataIOXmlbeans::readData cannot read object of type " +
							cl.getCanonicalName() + ">" +
							" from <" + ((ALDXMLAnyType)aldXmlObject).getClassName() + ">\n");
		}
	}

	@Override
	public ALDXMLObjectType writeData(Object obj)
			throws ALDDataIOManagerException, ALDDataIOProviderException {
		Class<?> cl = obj.getClass();
		XmlObject xmlObject;
		if (cl.equals(Random.class) ) {
			Random rand = (Random)obj;

			XmlLong xmlLong = XmlLong.Factory.newInstance();
			try {
				Class<?> cls = Random.class;
				Field fld = cls.getDeclaredField("seed");
				fld.setAccessible(true);
				AtomicLong al = (AtomicLong) fld.get(rand);
				xmlLong.setLongValue(al.get());;
			} catch (Exception e) {
				throw new ALDDataIOProviderException( ALDDataIOProviderExceptionType.UNSPECIFIED_ERROR,
						"RandomGeneratorDataIOXmlbeans::writeData Cannot format " + obj + " as random numer\n" +
								e.getMessage());
			}

			xmlObject = xmlLong;
		} else {
			throw new ALDDataIOProviderException(ALDDataIOProviderExceptionType.OBJECT_TYPE_ERROR,
					"RandomGeneratorDataIOXmlbeans::writeData invalid class<" +
					cl.getName() + ">");
		}
		ALDXMLAnyType aldXmlObject = ALDXMLAnyType.Factory.newInstance();
		aldXmlObject.setClassName(cl.getName());
		aldXmlObject.setValue(xmlObject);

		return aldXmlObject;
	}
}