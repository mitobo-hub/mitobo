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

package de.unihalle.informatik.MiToBo.core.dataio.provider.xmlbeans;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.LinkedList;

import org.apache.xmlbeans.XmlDouble;
import org.apache.xmlbeans.XmlInt;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlString;

import de.unihalle.informatik.Alida.annotations.ALDDataIOProvider;
import de.unihalle.informatik.Alida.dataio.provider.xmlbeans.ALDStandardizedDataIOXmlbeans;
import de.unihalle.informatik.Alida.exceptions.ALDDataIOManagerException;
import de.unihalle.informatik.Alida.exceptions.ALDDataIOProviderException;
import de.unihalle.informatik.Alida.exceptions.ALDDataIOProviderException.ALDDataIOProviderExceptionType;
import de.unihalle.informatik.Alida_xml.ALDXMLObjectType;
import de.unihalle.informatik.MiToBo.core.datatypes.wrapper.MTBDoubleData;
import de.unihalle.informatik.MiToBo.core.datatypes.wrapper.MTBIntegerData;
import de.unihalle.informatik.MiToBo.core.datatypes.wrapper.MTBStringData;

import de.unihalle.informatik.Alida_xml.ALDXMLAnyType;


/**
 * Class for loading/saving primitive datatypes in Alida.
 * 
 * @author moeller
 *
 */
@ALDDataIOProvider
public class MTBWrapperDataIOXmlbeans extends ALDStandardizedDataIOXmlbeans {

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
	
	@Override
	public Object readData(Field field, Class<?> cl, ALDXMLObjectType aldXmlObject, Object obj)
			throws ALDDataIOProviderException, ALDDataIOManagerException {
		if (cl == null)
			throw new ALDDataIOProviderException(ALDDataIOProviderExceptionType.OBJECT_TYPE_ERROR, 
					"MTBWrapperDataIOXmlbeans::readData cl == null");
		
		if ( aldXmlObject == null || aldXmlObject.isNil())
			return null;
		
		try {
			if (cl.equals(MTBDoubleData.class) && 
					((ALDXMLAnyType)aldXmlObject).getClassName().equals(MTBDoubleData.class.getName()) ) {
				return new MTBDoubleData( ((XmlDouble)((ALDXMLAnyType)aldXmlObject).getValue()).getDoubleValue());
			} else if (cl.equals(MTBStringData.class) && 
					((ALDXMLAnyType)aldXmlObject).getClassName().equals(MTBStringData.class.getName()) ) {
				return new MTBStringData(((XmlString)((ALDXMLAnyType)aldXmlObject).getValue()).getStringValue());
			} else if (cl.equals(MTBIntegerData.class) && 
					((ALDXMLAnyType)aldXmlObject).getClassName().equals(MTBIntegerData.class.getName()) ) {
				return new MTBIntegerData(((XmlInt)((ALDXMLAnyType)aldXmlObject).getValue()).getIntValue());
			} else {
				throw new ALDDataIOProviderException(ALDDataIOProviderExceptionType.OBJECT_TYPE_ERROR, 
						"MTBWrapperDataIOXmlbeans::readData cannot read object of type " +
								cl.getCanonicalName() + ">" +
								" from <" + aldXmlObject.toString() + ">\n");
			}
		} catch (Exception e) {
			throw new ALDDataIOProviderException(ALDDataIOProviderExceptionType.SYNTAX_ERROR, 
					"MTBWrapperDataIOXmlbeans::readData wrong xml object of type <" +
							((ALDXMLAnyType)aldXmlObject).getValue().getClass().getName() +
							"> to read an object of class <" + cl.getName() + ">");

		}
	}

	@Override
	public ALDXMLObjectType writeData(Object obj) throws ALDDataIOProviderException, ALDDataIOManagerException {
		Class<?> cl = obj.getClass();
	    XmlObject xmlObject;
		if (cl.equals(MTBDoubleData.class) ) {
			XmlDouble xmlDouble = XmlDouble.Factory.newInstance();
			xmlDouble.setDoubleValue(((MTBDoubleData)obj).getValue());
			xmlObject = xmlDouble;
		} else if (cl.equals(MTBStringData.class)) {
			XmlString xmlString = XmlString.Factory.newInstance();
			xmlString.setStringValue(((MTBStringData)obj).getString());
			xmlObject = xmlString;
		} else if (cl.equals(MTBIntegerData.class) ) {
			XmlInt xmlInt = XmlInt.Factory.newInstance();
			xmlInt.setIntValue(((MTBIntegerData)obj).getValue());
			xmlObject = xmlInt;
		} else {
			throw new ALDDataIOProviderException(ALDDataIOProviderExceptionType.OBJECT_TYPE_ERROR,
					"MTBWrapperDataIOXmlbeans::writeData invalid class<" +
					cl.getName() + ">");
		}
		ALDXMLAnyType aldXmlObject = ALDXMLAnyType.Factory.newInstance();
		aldXmlObject.setClassName(cl.getName());
		aldXmlObject.setValue(xmlObject);

		return aldXmlObject;
	}
}
