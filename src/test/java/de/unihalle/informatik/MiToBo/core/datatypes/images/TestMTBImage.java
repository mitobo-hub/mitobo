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

package de.unihalle.informatik.MiToBo.core.datatypes.images;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;

/**
 * JUnit test class for {@link ALDParametrizedClassDataIOSwing}.
 * 
 * @author moeller
 */
public class TestMTBImage {

	/**
	 * Dummy image for tests.
	 */
	MTBImage dummyImage;
	
	/**
	 * Fixture.
	 */
	@Before
	public void initTestClass() {
		// nothing to do here
	}
	
	/**
	 * Test if new images always get a non-empty and non-null title string.
	 */
	@Test
	public void testTitleOfNewImages() {

		// create an image without title
		this.dummyImage = 
				MTBImage.createMTBImage(10, 10, 10, 1, 1, MTBImageType.MTB_BYTE);
		assertTrue("Dummy image should have a proper title, but it's null!", 
				this.dummyImage.getTitle() != null);
		assertTrue("Dummy image should have a proper title, but it's empty!", 
				!this.dummyImage.getTitle().isEmpty());
		
	}
	
	/**
	 * Test if the underlying ImagePlus shares same title once generated.
	 */
	@Test
	public void testMitoboImageAndImagePlusSynchronized() {
		
		// create various images without title and test them
		this.dummyImage = 
				MTBImage.createMTBImage(10, 10, 10, 1, 1, MTBImageType.MTB_BYTE);
		this.testSettingTitle(this.dummyImage);
		this.dummyImage = 
				MTBImage.createMTBImage(10, 10, 10, 1, 1, MTBImageType.MTB_DOUBLE);
		this.testSettingTitle(this.dummyImage);
		this.dummyImage = 
				MTBImage.createMTBImage(10, 10, 10, 1, 1, MTBImageType.MTB_FLOAT);
		this.testSettingTitle(this.dummyImage);
		this.dummyImage = 
				MTBImage.createMTBImage(10, 10, 10, 1, 1, MTBImageType.MTB_INT);
		this.testSettingTitle(this.dummyImage);
		this.dummyImage = 
				MTBImage.createMTBImage(10, 10, 10, 1, 1, MTBImageType.MTB_RGB);
		this.testSettingTitle(this.dummyImage);
		this.dummyImage = 
				MTBImage.createMTBImage(10, 10, 10, 1, 1, MTBImageType.MTB_SHORT);
		this.testSettingTitle(this.dummyImage);
	}
	
	/**
	 * Helper for setTitle() method.
	 * @param img		Image to test on.
	 */
	private void testSettingTitle(MTBImage img) {
		// make sure that the underlying ImagePlus has the same title!
		assertTrue("Underlying ImagePlus has different title!", 
				img.getTitle().equals(img.getImagePlus().getTitle()));
		
		// set image title explicitly to null string, should not change anything
		String oldTitle = img.getTitle();
		img.setTitle(null);
		String newTitle = img.getTitle();
		assertTrue("Title changed on setting it to null string!",
				oldTitle.equals(newTitle));

		// set image title to empty string and repeat test
		img.setTitle(new String());
		assertTrue("Underlying ImagePlus has different title!", 
				img.getTitle().equals(img.getImagePlus().getTitle()));

		// set image title to valid string and repeat test
		img.setTitle(new String("Dummy Title"));
		assertTrue("Underlying ImagePlus has different title!", 
				img.getTitle().equals(img.getImagePlus().getTitle()));
	}
}