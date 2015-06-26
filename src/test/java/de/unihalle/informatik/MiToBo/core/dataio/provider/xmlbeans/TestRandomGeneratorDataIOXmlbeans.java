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
import java.lang.reflect.Field;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

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
 * JUnit test class for {@link RandomGeneratorDataIOXmlbeans}.
 * 
 * @author posch
 */
public class TestRandomGeneratorDataIOXmlbeans {

	/**
	 * Fixture.
	 */
	@Before
	public void initTestClass() {
	}
	
	/**
	 * Test xmlbeans provider for Random.
	 * 
	 * @throws IOException 
	 * @throws ALDDataIOManagerException 
	 * @throws ALDDataIOProviderException 
	 * @throws XmlException 
	 * @throws NoSuchFieldException 
	 * @throws SecurityException 
	 * @throws IllegalAccessException 
	 * @throws IllegalArgumentException 
	 */
	@Test
	public void testAwtColorIO() 
			throws IOException, ALDDataIOProviderException, ALDDataIOManagerException, XmlException, SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		File tmpFile = File.createTempFile("tmp", "xml");

		Random rndGen = new Random();
		
		Long seed = rndGen.nextLong();

		Random random = new Random(seed);
		Class<?> cls = Random.class;
		Field fld = cls.getDeclaredField("seed");
		fld.setAccessible(true);
		AtomicLong al = (AtomicLong) fld.get(random);
		ALDDataIOManagerXmlbeans.writeXml(tmpFile, random);

		Random randomRead = (Random) ALDDataIOManagerXmlbeans.readXml(tmpFile, Random.class);
	
		fld = cls.getDeclaredField("seed");
		fld.setAccessible(true);
		AtomicLong alRead = (AtomicLong) fld.get(randomRead);

		assertTrue("Got different seed back: " + al + " vs " + alRead, 
				al.get() == alRead.get());
	}
}