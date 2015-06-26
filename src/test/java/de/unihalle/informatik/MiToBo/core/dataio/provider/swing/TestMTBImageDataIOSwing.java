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

package de.unihalle.informatik.MiToBo.core.dataio.provider.swing;

import static org.junit.Assert.*;

import javax.swing.JComponent;

import org.junit.Before;
import org.junit.Test;

import de.unihalle.informatik.Alida.dataio.ALDDataIOManagerSwing;
import de.unihalle.informatik.MiToBo.core.dataio.provider.swing.MTBImageDataIOSwing.ImageShowButton;
import de.unihalle.informatik.MiToBo.core.dataio.provider.swing.MTBImageDataIOSwing.ImageShowPanel;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageByte;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;

/**
 * JUnit test class for {@link ALDParametrizedClassDataIOSwing}.
 * 
 * @author moeller
 */
public class TestMTBImageDataIOSwing {

	/**
	 * Dummy image for tests.
	 */
	MTBImage dummyImage;
	
	/**
	 * Fixture.
	 */
	@Before
	public void initTestClass() {
		// instantiate test object
		this.dummyImage = 
				MTBImage.createMTBImage(10, 10, 10, 1, 1, MTBImageType.MTB_BYTE);
		// make sure that provider does not pop-up any window
		ALDDataIOManagerSwing.getInstance().setProviderInteractionLevel(
								ALDDataIOManagerSwing.ProviderInteractionLevel.ALL_FORBIDDEN);
	}
	
	/**
	 * Test if each image gets a title upon writing.
	 */
	@Test
	public void testTitleSettingsOnWritingData() {

		// some local variables
		JComponent guiElement;
		
		// instantiate object of provider class
		MTBImageDataIOSwing testObject = new MTBImageDataIOSwing();

		// write image via provider and fetch the GUI element
		guiElement = testObject.writeData(this.dummyImage, null);
		assertTrue("Expected a ImageShowPanel, got something else...!", 		
																		guiElement instanceof ImageShowPanel);
		// get the object associated with the button
		ImageShowButton butObj = ((ImageShowPanel)guiElement).getImageShowButton();
		Object imgObj = butObj.getImageObject();
		assertTrue("Image object should be of type MTBImageByte!", 
																						imgObj instanceof MTBImageByte);
		MTBImageByte testImage = (MTBImageByte)imgObj;
		assertTrue("Image should have a proper title, but it's null!", 
																							testImage.getTitle() != null);
		assertTrue("Image should have a proper title, but it's empty!", 
				                                    !testImage.getTitle().isEmpty());
		// make sure that the underlying ImagePlus has the same title!
		assertTrue("Underlying image has different title!", 
				testImage.getTitle().equals(testImage.getImagePlus().getTitle()));
		
		// set image title explicitly to null string and repeat tests
		this.dummyImage.setTitle(null);
		// write image via provider and fetch the GUI element
		guiElement = testObject.writeData(this.dummyImage, null);
		assertTrue("Expected a ImageShowPanel, got something else...!", 		
																		guiElement instanceof ImageShowPanel);
		// get the object associated with the button
		butObj = ((ImageShowPanel)guiElement).getImageShowButton();
		imgObj = butObj.getImageObject();
		assertTrue("Image object should be of type MTBImageByte!", 
																						imgObj instanceof MTBImageByte);
		testImage = (MTBImageByte)imgObj;
		assertTrue("Image should have a proper title, but it's null!", 
																							testImage.getTitle() != null);
		assertTrue("Image should have a proper title, but it's empty!", 
				                                    !testImage.getTitle().isEmpty());
	}
}