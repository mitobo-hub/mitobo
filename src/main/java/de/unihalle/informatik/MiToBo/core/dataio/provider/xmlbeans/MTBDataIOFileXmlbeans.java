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
 * $Rev: 3901 $
 * $Date: 2011/10/28 15:22:44 $
 * $Author: posch $
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
import de.unihalle.informatik.Alida_xml.ALDXMLAnyType;
import de.unihalle.informatik.Alida_xml.ALDXMLObjectType;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBContour2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBPolygon2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2DSetBag;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion3DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.wrapper.MTBDoubleData;
import de.unihalle.informatik.MiToBo.core.datatypes.wrapper.MTBIntegerData;
import de.unihalle.informatik.MiToBo.core.datatypes.wrapper.MTBStringData;
import de.unihalle.informatik.MiToBo_xml.MTBXMLContour2DSetType;
import de.unihalle.informatik.MiToBo_xml.MTBXMLPolygon2DSetType;
import de.unihalle.informatik.MiToBo_xml.MTBXMLRegion2DSetBagType;
import de.unihalle.informatik.MiToBo_xml.MTBXMLRegion2DSetType;
import de.unihalle.informatik.MiToBo_xml.MTBXMLRegion3DSetType;

/**
 * DataIO provider for xmlbeans for classes that can 
 * only be read from and written to file.
 * <p> 
 * This class is meant to be extended for any such data class.<br>
 * Provides DataIO for the following classes:<br>
 * {@link de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2DSetBag}<br>
 * {@link de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2DSet}<br>
 * {@link de.unihalle.informatik.MiToBo.core.datatypes.MTBPolygon2DSet}<br>
 * {@link de.unihalle.informatik.MiToBo.core.datatypes.MTBContour2DSet}<br>
 * <p>
 * Note that some of the classes allow for interaction with the ROI manager 
 * of ImageJ, i.e. the class 
 * {@link de.unihalle.informatik.MiToBo.core.datatypes.MTBPolygon2DSet}.
 * It can be initialized with selections from the ROI manager, and resulting
 * polygons can also be added to the ROI manager.
 * 
 * @author posch
 */

@ALDDataIOProvider
public class MTBDataIOFileXmlbeans extends ALDStandardizedDataIOXmlbeans {

	@Override
	public Collection<Class<?>> providedClasses() {
		
		// add your class to the list !!
		
		LinkedList<Class<?>> l = new LinkedList<Class<?>>();
		l.add(MTBRegion2DSetBag.class);
		l.add(MTBRegion2DSet.class);
		l.add(MTBRegion3DSet.class);
		l.add(MTBPolygon2DSet.class);
		l.add(MTBContour2DSet.class);
		
		return l;
	}

	@Override
	public Object readData(Field field, Class<?> cl, ALDXMLObjectType aldXmlObject, Object obj)
			throws ALDDataIOProviderException, ALDDataIOManagerException {
		if (cl == null)
			throw new ALDDataIOProviderException(ALDDataIOProviderExceptionType.OBJECT_TYPE_ERROR, 
					"MTBDataIOFileXmlbeans::readData cl == null");
		
		if ( aldXmlObject == null || aldXmlObject.isNil())
			return null;
		
		try {
			if (cl.equals(MTBRegion2DSetBag.class) && 
					aldXmlObject.getClassName().equals(MTBRegion2DSetBag.class.getName())) {
				MTBXMLRegion2DSetBagType xmlRegion2DSetBag = (MTBXMLRegion2DSetBagType)((ALDXMLAnyType)aldXmlObject).getValue();
				return new MTBRegion2DSetBag( xmlRegion2DSetBag);
			
			} else if (cl.equals(MTBRegion2DSet.class) &&
					 ((ALDXMLAnyType)aldXmlObject).getClassName().equals(MTBRegion2DSet.class.getName()) ) {
				MTBXMLRegion2DSetType xmlRegion2DSet = (MTBXMLRegion2DSetType)((ALDXMLAnyType)aldXmlObject).getValue();
				return new MTBRegion2DSet( xmlRegion2DSet);
				
			} else if (cl.equals(MTBRegion3DSet.class) && 
					((ALDXMLAnyType)aldXmlObject).getClassName().equals(MTBRegion3DSet.class.getName())) {
				MTBXMLRegion3DSetType xmlRegion3DSet = (MTBXMLRegion3DSetType)((ALDXMLAnyType)aldXmlObject).getValue();
				return new MTBRegion3DSet( xmlRegion3DSet);
				
			} else if (cl.equals(MTBPolygon2DSet.class) && 
					((ALDXMLAnyType)aldXmlObject).getClassName().equals(MTBPolygon2DSet.class.getName())) {
				MTBXMLPolygon2DSetType xmlPolygon2DSet = (MTBXMLPolygon2DSetType)((ALDXMLAnyType)aldXmlObject).getValue();
				MTBPolygon2DSet polygon2DSet = new MTBPolygon2DSet();
				polygon2DSet.read(xmlPolygon2DSet);
				return polygon2DSet;
				
			} else if (cl.equals(MTBContour2DSet.class) && 
					((ALDXMLAnyType)aldXmlObject).getClassName().equals(MTBPolygon2DSet.class.getName())) {
				MTBXMLContour2DSetType xmlContourSet = (MTBXMLContour2DSetType)((ALDXMLAnyType)aldXmlObject).getValue();
				MTBContour2DSet  contour2DSet = new MTBContour2DSet();
				contour2DSet.read( xmlContourSet);
				return null;

			} else {
				throw new ALDDataIOProviderException(ALDDataIOProviderExceptionType.OBJECT_TYPE_ERROR, 
						"MTBWrapperDataIOXmlbeans::readData cannot read object of type " +
								cl.getName() + ">" +
								" from <" + aldXmlObject.getClassName() + ">\n");
			}
		} catch (Exception e) {
			throw new ALDDataIOProviderException(ALDDataIOProviderExceptionType.SYNTAX_ERROR, 
					"MTBWrapperDataIOXmlbeans::readData wrong xml object of type <" +
							((ALDXMLAnyType)aldXmlObject).getValue().getClass().getName() +
							"> to read an object of class <" + cl.getName() + ">");

		}
	}

	@Override
	public ALDXMLObjectType writeData(Object obj)
			throws ALDDataIOManagerException, ALDDataIOProviderException {
		Class<?> cl = obj.getClass();

	    XmlObject xmlObject;
	    
		if (obj instanceof MTBRegion2DSetBag) {
			xmlObject = ((MTBRegion2DSetBag)obj).toXMLType();
		} else if (obj instanceof MTBRegion2DSet) {
			xmlObject = ((MTBRegion2DSet)obj).toXMLType();
		} else if (obj instanceof MTBRegion3DSet) {
			xmlObject = ((MTBRegion3DSet)obj).toXMLType();
		} else if (obj instanceof MTBPolygon2DSet) {
			try {
				xmlObject = ((MTBPolygon2DSet)obj).toXMLType();
			} catch (ClassNotFoundException e) {
				throw new ALDDataIOProviderException(ALDDataIOProviderExceptionType.OBJECT_TYPE_ERROR,
						"MTBDataIOFileXmlbeans::writeData invalid type of polygons in a  MTBPolygon2DSet");
			}
		} else if (obj instanceof MTBContour2DSet) {
			xmlObject = ((MTBContour2DSet)obj).toXMLType();
		} else {
			throw new ALDDataIOProviderException(ALDDataIOProviderExceptionType.OBJECT_TYPE_ERROR,
					"MTBDataIOFileXmlbeans::writeData invalid class <" +
					cl.getName() + ">");
		}
		ALDXMLAnyType aldXmlObject = ALDXMLAnyType.Factory.newInstance();
		aldXmlObject.setClassName(cl.getName());
		aldXmlObject.setValue(xmlObject);
		
		return aldXmlObject;
	}
}