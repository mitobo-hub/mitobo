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

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.Random;

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
 * JUnit test class for {@link AwtColorDataIOXmlbeans}.
 * 
 * @author posch
 */
public class TestAwtColorDataIOXmlbeans {

	/**
	 * Fixture.
	 */
	@Before
	public void initTestClass() {
	}
	
	/**
	 * Test xmlbeans provider for AWTColo.
	 * 
	 * @throws IOException 
	 * @throws ALDDataIOManagerException 
	 * @throws ALDDataIOProviderException 
	 * @throws XmlException 
	 */
	@Test
	public void testAwtColorIO() 
			throws IOException, ALDDataIOProviderException, ALDDataIOManagerException, XmlException {
		File tmpFile = File.createTempFile("tmp", "xml");

		Random rndGen = new Random();
		
		Color color = new Color( rndGen.nextInt(256), rndGen.nextInt(256), rndGen.nextInt(256), rndGen.nextInt(256));
		ALDDataIOManagerXmlbeans.writeXml(tmpFile, color);

		Color colorRead = (Color) ALDDataIOManagerXmlbeans.readXml(tmpFile, Color.class);
	
		assertTrue("Got different string back: " + color + " vs " + colorRead, 
				color.equals( colorRead));
	}
}