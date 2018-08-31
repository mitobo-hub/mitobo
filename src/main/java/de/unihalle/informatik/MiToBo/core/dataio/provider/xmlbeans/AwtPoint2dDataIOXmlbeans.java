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

import java.awt.geom.Point2D;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.LinkedList;
import org.apache.xmlbeans.XmlObject;
import de.unihalle.informatik.Alida.annotations.ALDDataIOProvider;
import de.unihalle.informatik.Alida.dataio.provider.xmlbeans.ALDStandardizedDataIOXmlbeans;
import de.unihalle.informatik.Alida.exceptions.ALDDataIOManagerException;
import de.unihalle.informatik.Alida.exceptions.ALDDataIOProviderException;
import de.unihalle.informatik.Alida.exceptions.ALDDataIOProviderException.ALDDataIOProviderExceptionType;
import de.unihalle.informatik.Alida_xml.ALDXMLAnyType;
import de.unihalle.informatik.Alida_xml.ALDXMLObjectType;
import de.unihalle.informatik.MiToBo_xml.MTBXMLPoint2DDoubleType;

/**
 * Xmlbeans provider for <code>java.awt.Color</code> objects.
 * 
 * @author posch
 * 
 *
 */
@ALDDataIOProvider
public class AwtPoint2dDataIOXmlbeans extends ALDStandardizedDataIOXmlbeans {

	@Override
	public Collection<Class<?>> providedClasses() {
		LinkedList<Class<?>> l = new LinkedList<Class<?>>();
		l.add(Point2D.Double.class);
		return l;
	}
	
	@Override
	public Object readData(Field field, Class<?> cl, ALDXMLObjectType aldXmlObject, Object obj)
			throws ALDDataIOProviderException, ALDDataIOManagerException {
		if (cl == null)
			throw new ALDDataIOProviderException(ALDDataIOProviderExceptionType.OBJECT_TYPE_ERROR, 
					"AwtPoint2dDataIOXmlbeans::readData cl == null");
		
		if ( aldXmlObject == null || aldXmlObject.isNil())
			return null;
		
		try {
			if (cl.equals( Point2D.Double.class) && 
					((ALDXMLAnyType)aldXmlObject).getClassName().equals( Point2D.Double.class.getName()) ) {
				MTBXMLPoint2DDoubleType xmlPoint2d = ((MTBXMLPoint2DDoubleType)((ALDXMLAnyType)aldXmlObject).getValue());
				return new Point2D.Double(xmlPoint2d.getX(), xmlPoint2d.getY());
			} else {
				throw new ALDDataIOProviderException(ALDDataIOProviderExceptionType.OBJECT_TYPE_ERROR, 
						"AwtPoint2dDataIOXmlbeans::readData cannot read object of type " +
								cl.getCanonicalName() + ">" +
								" from <" + aldXmlObject.toString() + ">\n");
			}
		} catch (Exception e) {
			throw new ALDDataIOProviderException(ALDDataIOProviderExceptionType.SYNTAX_ERROR, 
					"AwtPoint2dDataIOXmlbeans::readData wrong xml object of type <" +
							((ALDXMLAnyType)aldXmlObject).getValue().getClass().getName() +
							"> to read an object of class <" + cl.getName() + ">");

		}
	}

	@Override
	public ALDXMLObjectType writeData(Object obj)
			throws ALDDataIOManagerException, ALDDataIOProviderException {
		Class<?> cl = obj.getClass();
	    XmlObject xmlObject;
		if (cl.equals(Point2D.Double.class) ) {
			Point2D.Double point2d = (Point2D.Double)obj;
			MTBXMLPoint2DDoubleType xmlPoint2d = MTBXMLPoint2DDoubleType.Factory.newInstance();
			xmlPoint2d.setX(point2d.getX());
			xmlPoint2d.setY(point2d.getY());
			
			xmlObject = xmlPoint2d;
		} else {
			throw new ALDDataIOProviderException(ALDDataIOProviderExceptionType.OBJECT_TYPE_ERROR,
					"AwtPoint2dDataIOXmlbeans::writeData invalid class<" +
					cl.getName() + ">");
		}
		ALDXMLAnyType aldXmlObject = ALDXMLAnyType.Factory.newInstance();
		aldXmlObject.setClassName(cl.getName());
		aldXmlObject.setValue(xmlObject);

		return aldXmlObject;
	}
}
