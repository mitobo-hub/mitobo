/*
 * This file is part of Alida, a Java library for 
 * Advanced Library for Integrated Development of Data Analysis Applications.
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
 * Fore more information on Alida, visit
 *
 *    http://www.informatik.uni-halle.de/alida/
 *
 */

package de.unihalle.informatik.MiToBo.core.dataio.provider.xmlbeans;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.apache.xmlbeans.XmlException;
import org.junit.Before;
import org.junit.Test;

import de.unihalle.informatik.Alida.dataio.ALDDataIOManagerSwing;
import de.unihalle.informatik.Alida.dataio.ALDDataIOManagerXmlbeans;
import de.unihalle.informatik.Alida.exceptions.ALDDataIOManagerException;
import de.unihalle.informatik.Alida.exceptions.ALDDataIOProviderException;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.datatypes.wrapper.MTBDoubleData;
import de.unihalle.informatik.MiToBo.core.datatypes.wrapper.MTBIntegerData;
import de.unihalle.informatik.MiToBo.core.datatypes.wrapper.MTBStringData;

/**
 * JUnit test class for {@link MTBWrapperDataIOXmlbeans}.
 * 
 * @author posch
 */
public class TestMTBWrapperDataIOXmlbeans {

	/**
	 * Fixture.
	 */
	@Before
	public void initTestClass() {
	}
	
	/**
	 * Test xmlbeans provider for MTB wrapper data types
	 * as define in {@link MTBWrapperDataIOXmlbeans}.
	 * 
	 * @throws IOException 
	 * @throws ALDDataIOManagerException 
	 * @throws ALDDataIOProviderException 
	 * @throws XmlException 
	 */
	@Test
	public void testWrapperDataIO() throws IOException, ALDDataIOProviderException, ALDDataIOManagerException, XmlException {
		File tmpFile = File.createTempFile("tmp", "xml");
		
		MTBDoubleData mtbDouble = new MTBDoubleData(Math.PI);
		ALDDataIOManagerXmlbeans.writeXml(tmpFile, mtbDouble);
		MTBDoubleData mtbDoubleRead = (MTBDoubleData) ALDDataIOManagerXmlbeans.readXml(tmpFile, MTBDoubleData.class);
		
		assertTrue("Got different double back: " + mtbDouble.getValue() + " vs " + mtbDoubleRead.getValue(), 
				mtbDouble.getValue().equals( mtbDoubleRead.getValue()));

		MTBIntegerData mtbInteger = new MTBIntegerData(4711);
		ALDDataIOManagerXmlbeans.writeXml(tmpFile, mtbInteger);
		MTBIntegerData mtbIntegerRead = (MTBIntegerData) ALDDataIOManagerXmlbeans.readXml(tmpFile, MTBIntegerData.class);
		
		assertTrue("Got different Integer back: " + mtbInteger.getValue() + " vs " + mtbIntegerRead.getValue(), 
				mtbInteger.getValue().equals( mtbIntegerRead.getValue()));

		MTBStringData mtbString = new MTBStringData("hello world");
		ALDDataIOManagerXmlbeans.writeXml(tmpFile, mtbString);
		MTBStringData mtbStringRead = (MTBStringData) ALDDataIOManagerXmlbeans.readXml(tmpFile, MTBStringData.class);
		
		assertTrue("Got different string back: " + mtbString.getString() + " vs " + mtbStringRead.getString(), 
				mtbString.getString().equals( mtbStringRead.getString()));
	}
}