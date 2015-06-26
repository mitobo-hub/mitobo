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

package de.unihalle.informatik.MiToBo.segmentation.levelset.PDE.datatypes;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import org.junit.Test;

import de.unihalle.informatik.MiToBo.core.datatypes.MTBPoint3D;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.io.images.ImageReaderMTB;
import de.unihalle.informatik.MiToBo.segmentation.basics.MTBSegmentationInterface;
import de.unihalle.informatik.MiToBo.segmentation.basics.SegmentationInitializer.InputMode;
import de.unihalle.informatik.MiToBo.segmentation.basics.SegmentationInitializer;
import de.unihalle.informatik.MiToBo.segmentation.levelset.PDE.datatypes.MTBLevelsetFunctionPDE;

/**
 * JUnit test class for {@link MTBLevelsetFunctionPDE}.
 * 
 * @author moeller
 */
public class TestMTBLevelsetFunctionPDE {

	/**
	 * Identifier string for test class.
	 */
	private static final String IDS = "[TestMTBLevelsetFunctionPDE]";
	
	/**
	 * Object to test.
	 */
	private MTBLevelsetFunctionPDE testObj;
	
	/**
	 * 2D image with black circle on white background.
	 */
	private MTBImage testImage2DCircle;
	
	/**
	 * 2D image with black rectangle on white background.
	 */
	private MTBImage testImage2DRectangle;

	/**
	 * Initializer object.
	 */
	private SegmentationInitializer initOp;
	
	/**
	 * Width of current image to test.
	 */
	private int width;
	
	/**
	 * Height of current image to test.
	 */
	private int height;
	
	/**
	 * Test levelset function implementation on image with rectangle.
	 */
	@Test
	public void testLevelsetFunctionOnRectangle() {
		MTBSegmentationInterface seg;
		
		this.initRectangleImage();
		// init level set function
		boolean thrown = false;
		try {
			this.initOp = new SegmentationInitializer();
			this.initOp.setInputMode(InputMode.MODE_BINARY_IMAGE);
			this.initOp.setBinaryImage(this.testImage2DRectangle);
			this.initOp.runOp();
			seg = this.initOp.getSegmentation();
			this.testObj = 
				new MTBLevelsetFunctionPDE(this.width, this.height, 1, seg, false, 0);
			
			// check domain of levelset function
			assertTrue(IDS + " expected w = 400, got " + this.testObj.getSizeX(),
					this.testObj.getSizeX() == 400);
			assertTrue(IDS + " expected h = 400, got " + this.testObj.getSizeY(),
					this.testObj.getSizeY() == 400);		
			// init signed distance function without 
			this.testObj.signDistance(0); 
			// narrow band is not in use, assuming that we iterate over all pixels
			int pointCounter = 0;
			Iterator<MTBPoint3D> list; 
			for (list = this.testObj.getNarrowIterator(); list.hasNext();) {
				list.next();
				++pointCounter;
			}
			assertTrue(IDS + " expecting " + (400*400) + " pixels " +
				"in narrow band, got " + pointCounter + "...", pointCounter == 400*400);

			// change border, i.e. add 2-pixel-wide safety border
			this.testObj = 
				new MTBLevelsetFunctionPDE(this.width, this.height, 1, seg, false, 2);
			// check domain of levelset function
			assertTrue(IDS + " expected w = 400, got " + this.testObj.getSizeX(),
					this.testObj.getSizeX() == 400);
			assertTrue(IDS + " expected h = 400, got " + this.testObj.getSizeY(),
					this.testObj.getSizeY() == 400);		
			// init signed distance function without 
			this.testObj.signDistance(0); 
			// narrow band is not in use, assuming that we iterate over all pixels
			pointCounter = 0;
			for (list = this.testObj.getNarrowIterator(); list.hasNext();) {
				list.next();
				++pointCounter;
			}
			assertTrue(IDS + " expecting " + (396*396) + " pixels " +
				"in narrow band, got " + pointCounter + "...", pointCounter == 396*396);
			
			// the set of precursors is given by all inner and outer pixels along
			// borders of rectangle, their number is 1604
			HashMap<MTBPoint3D, Vector<MTBPoint3D>> map = 
				new HashMap<MTBPoint3D, Vector<MTBPoint3D>>(); 
			for (list = this.testObj.getNarrowIterator(); list.hasNext(); ) {
				MTBPoint3D p = list.next();
				MTBPoint3D pred = this.testObj.getPredecessorOnContour(
					(int)p.getX(), (int)p.getY(), (int)p.getZ());
				if (map.get(pred) == null) {
					map.put(pred, new Vector<MTBPoint3D>());
					map.get(pred).add(p);
				}
				else {
					map.get(pred).add(p);
				}
			}
			assertTrue(IDS + " expecting " + 1604 + " pixels being precursors, " +
				"got " + map.keySet().size() + "...", map.keySet().size() == 1604);
		} catch (Exception ex) {
			ex.printStackTrace();
			thrown = true;
		}
		assertFalse(IDS + " something went wrong on testing with rectangle...",
			thrown);
	}

	/**
	 * Initializes test image with rectangle.
	 */
	private void initRectangleImage() {
		// create a test image with white background and a black rectangle
		// in the center sized 200 x 200
		this.testImage2DRectangle = 
			MTBImage.createMTBImage(400, 400, 1, 1, 1, MTBImageType.MTB_BYTE);
		this.testImage2DRectangle.fillWhite();
		for (int y=0; y<400; ++y) {
			for (int x=0; x<400; ++x) {
				if ((x>=100 && x<=300) && (y>=100 && y<=300)) {
					this.testImage2DRectangle.putValueInt(x, y, 0);
				}
			}
		}
		this.width = this.testImage2DRectangle.getSizeX();
		this.height = this.testImage2DRectangle.getSizeY();
	}

	/**
	 * Tests levelset function properties given image with circle as test data.
	 */
	@Test
	public void testLevelsetFunctionOnCircle() {
		this.initCircleImage();

		boolean thrown = false;
		try {
			this.initOp = new SegmentationInitializer();
			this.initOp.setInputMode(InputMode.MODE_BINARY_IMAGE);
			this.initOp.setBinaryImage(this.testImage2DCircle);
			this.initOp.runOp();
			MTBSegmentationInterface seg = this.initOp.getSegmentation();
			// init phi without safety border
			this.testObj = 
				new MTBLevelsetFunctionPDE(this.width, this.height, 1, seg, false, 0);
			assertTrue(IDS + " expected w = 450, got " + this.testObj.getSizeX(),
				this.testObj.getSizeX() == 450);
			assertTrue(IDS + " expected h = 370, got " + this.testObj.getSizeY(),
				this.testObj.getSizeY() == 370);		

			// init signed distance function without narrow band
			this.testObj.signDistance(0); 
			// narrow band is not in use, assuming that we iterate over all pixels...
			int pointCounter = 0;
			Iterator<MTBPoint3D> list; 
			for (list = this.testObj.getNarrowIterator(); list.hasNext();) {
				list.next();
				++pointCounter;
			}
			assertTrue(IDS + " expecting " + (this.width*this.height) + " pixels " +
				"in narrow band, got " + pointCounter + "...", 
				pointCounter == (this.width*this.height));
		} catch (Exception ex) {
			ex.printStackTrace();
			thrown = true;
		}
		assertFalse(IDS + " could not init circle test...", thrown);
	}
	
	/**
	 * Initializes test image with circle from file system.
	 */
	private void initCircleImage() {
		boolean thrown = false;
		String path = "target/test-classes/images/binaryCircle.pgm";
		try {
			// read the image
			ImageReaderMTB iRead = new ImageReaderMTB(path);
			iRead.runOp();
			this.testImage2DCircle = iRead.getResultMTBImage();
			this.width = this.testImage2DCircle.getSizeX();
			this.height = this.testImage2DCircle.getSizeY();
		} catch (Exception ex) {
			ex.printStackTrace();
			thrown = true;
		}
		assertFalse(IDS + " could not read circle image, path = <" + path + ">", 
				thrown);
	}
}