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

package de.unihalle.informatik.MiToBo.morphology;

import static org.junit.Assert.*;

import java.awt.geom.Point2D;

import org.junit.Before;
import org.junit.Test;

import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageByte;
import de.unihalle.informatik.MiToBo.morphology.DistanceTransform.DistanceMetric;
import de.unihalle.informatik.MiToBo.morphology.DistanceTransform.ForegroundColor;

/**
 * JUnit test class for {@link DistanceTransform}.
 * 
 * @author moeller
 */
public class TestDistanceTransform {

	/**
	 * Dummy image for tests.
	 */
	protected MTBImageByte dummyImage;
	
	/**
	 * Fixture.
	 */
	@Before
	public void initTestClass() {
		// create a test image...
		this.dummyImage = (MTBImageByte)MTBImage.createMTBImage(
			20, 20, 1, 1, 1, MTBImageType.MTB_BYTE);
		this.dummyImage.fillBlack();
		// draw two rectangles in the center of the image
		for (int y=5;y<8;++y)
			for (int x=5;x<15;++x)
				this.dummyImage.putValueInt(x, y, 255);
		for (int y=12;y<15;++y)
			for (int x=5;x<15;++x)
				this.dummyImage.putValueInt(x, y, 255);
	}
	
	/**
	 * Test if Champfer transformation works.
	 */
	@Test
	public void testDistanceTransform() {
		
		try {
			DistanceTransform tOp = new DistanceTransform();
			tOp.setInImg(this.dummyImage);
			tOp.setDistMetric(DistanceMetric.CITYBLOCK);
			tOp.setForeground(ForegroundColor.FG_WHITE);
			
			System.out.println("Running distance transformation...");

			tOp.runOp();
			MTBImage resultImg = tOp.getDistanceImage();

			System.out.println("Checking result...");
			
			// check if the result image is correct
			assertTrue("Result image should have size 20x20...", 
					resultImg.getSizeX() == 20 && resultImg.getSizeY() == 20);
			
			// inside rectangles distances should be zero
			for (int y=5;y<8;++y)
				for (int x=5;x<15;++x)
					assertTrue("Expected distance 0 at (" + x + "," + y + ")...",
							resultImg.getValueInt(x, y) == 0);
			for (int y=12;y<15;++y)
				for (int x=5;x<15;++x)
					assertTrue("Expected distance 0 at (" + x + "," + y + ")...",
							resultImg.getValueInt(x, y) == 0);
			
			// check pixels next to rectangles
			int x, y, dist;
			
			dist = 1;
			y = 4;
			for (x=5;x<15;++x)
				this.checkDist(resultImg, x, y, dist);
			dist = 2;
			x = 4;
			this.checkDist(resultImg, x, y, dist);
			x = 15;
			this.checkDist(resultImg, x, y, dist);

			dist = 1;
			y = 8;
			for (x=5;x<15;++x)
				this.checkDist(resultImg, x, y, dist);
			dist = 2;
			x = 4;
			this.checkDist(resultImg, x, y, dist);
			x = 15;
			this.checkDist(resultImg, x, y, dist);
			
			dist = 1;
			y = 11;
			for (x=5;x<15;++x)
				this.checkDist(resultImg, x, y, dist);
			dist = 2;
			x = 4;
			this.checkDist(resultImg, x, y, dist);
			x = 15;
			this.checkDist(resultImg, x, y, dist);

			dist = 1;
			y = 15;
			for (x=5;x<15;++x)
				this.checkDist(resultImg, x, y, dist);
			dist = 2;	
			x = 4;
			this.checkDist(resultImg, x, y, dist);
			x = 15;
			this.checkDist(resultImg, x, y, dist);
			
			// check image corners
			dist = 10;
			x = 0; y = 0;
			this.checkDist(resultImg, x, y, dist);
			x = 0; y = 19;
			this.checkDist(resultImg, x, y, dist);
			x = 19; y = 0;
			this.checkDist(resultImg, x, y, dist);
			x = 19; y = 19;
			this.checkDist(resultImg, x, y, dist);			

		} catch (Exception e) {
			e.printStackTrace();
			fail("Did not expect an exception to occur in " 
					+ "testDistanceTransform()...");
		}
		System.out.println("Done, everything fine!\n");
	}
	
	/**
	 * Check distance value for pixel.
	 * 
	 * @param resultImg	Distance image.
	 * @param x					x-position to check.
	 * @param y					y-position to check.
	 * @param dist			Expected distance.
	 */
	private void checkDist(MTBImage resultImg, int x, int y, int dist) {
		assertTrue("Expected distance " + dist 
			+ " at (" + x + "," + y + "), found " 
				+ resultImg.getValueInt(x, y) + "...",
					resultImg.getValueInt(x, y) == dist);
	}
	
	/**
	 * Test if precursor map is filled correctly.
	 */
	@Test
	public void testDistanceTransformPrecursorMap() {
		
		try {
			DistanceTransform tOp = new DistanceTransform();
			tOp.setInImg(this.dummyImage);
			tOp.setDistMetric(DistanceMetric.CITYBLOCK);
			tOp.setForeground(ForegroundColor.FG_WHITE);
			tOp.setPrecursorInfosEnabled(true);
			
			System.out.println("Running distance transformation...");
			
			tOp.runOp();
			Point2D.Double[][] map = tOp.getPrecursorMap();

			System.out.println("Checking precursor map...");
			
			// check if the map has correct size
			assertTrue("Map should have size 20x20...", 
					map.length == 20 && map[0].length == 20);
			
			// inside rectangles each pixel is its own precursor
			for (int y=5;y<8;++y)
				for (int x=5;x<15;++x)
					assertTrue("Expected to find pixel itself at (" 
						+ x + "," + y + ")...", 
							map[y][x].x == x && map[y][x].y == y);
			for (int y=12;y<15;++y)
				for (int x=5;x<15;++x)
					assertTrue("Expected to find pixel itself at (" 
						+ x + "," + y + ")...", 
							map[y][x].x == x && map[y][x].y == y);
			
			// check pixels next to rectangles
			int x, y;

			y = 4;
			for (x=5;x<15;++x)
				this.checkMapPixel(map, x, y, x, y+1);
			x = 4;
			this.checkMapPixel(map, x, y, x+1, y);
			x = 15;
			this.checkMapPixel(map, x, y, x, y+1);

			y = 8;
			for (x=5;x<15;++x)
				this.checkMapPixel(map, x, y, x, y-1);
			x = 4;
			this.checkMapPixel(map, x, y, x+1, y);
			x = 15;
			this.checkMapPixel(map, x, y, x-1, y);

			y = 11;
			for (x=5;x<15;++x)
				this.checkMapPixel(map, x, y, x, y+1);
			x = 4;
			this.checkMapPixel(map, x, y, x+1, y);
			x = 15;
			this.checkMapPixel(map, x, y, x, y+1);

			y = 15;
			for (x=5;x<15;++x)
				this.checkMapPixel(map, x, y, x, y-1);
			x = 4;
			this.checkMapPixel(map, x, y, x+1, y);
			x = 15;
			this.checkMapPixel(map, x, y, x-1, y);
			
			// check image corners
			x = 0; y = 0;
			this.checkMapPixel(map, x, y, x+1, y);
			x = 0; y = 19;
			this.checkMapPixel(map, x, y, x+1, y);
			x = 19; y = 0;
			this.checkMapPixel(map, x, y, x, y+1);
			x = 19; y = 19;
			this.checkMapPixel(map, x, y, x-1, y);

		} catch (Exception e) {
			e.printStackTrace();
			fail("Did not expect an exception to occur in " 
					+ "testDistanceTransformPrecursorMap()...");
		}
		System.out.println("Done, everything fine!\n");
	}
	
	/**
	 * Test if closest object pixel map is filled correctly.
	 */
	@Test
	public void testDistanceTransformClosestObjectPixelMap() {
		
		try {
			DistanceTransform tOp = new DistanceTransform();
			tOp.setInImg(this.dummyImage);
			tOp.setDistMetric(DistanceMetric.CITYBLOCK);
			tOp.setForeground(ForegroundColor.FG_WHITE);
			tOp.setPrecursorInfosEnabled(true);
			
			System.out.println("Running distance transformation...");
			
			tOp.runOp();
			Point2D.Double[][] map = tOp.getClosestObjectPixelMap();

			System.out.println("Checking closest object pixel map...");
			
			// check if the map has correct size
			assertTrue("Map should have size 20x20...", 
					map.length == 20 && map[0].length == 20);
			
			// inside rectangles each pixel is its own closest pixel
			for (int y=5;y<8;++y)
				for (int x=5;x<15;++x)
					assertTrue("Expected to find pixel itself at (" 
						+ x + "," + y + ")...", 
							map[y][x].x == x && map[y][x].y == y);
			for (int y=12;y<15;++y)
				for (int x=5;x<15;++x)
					assertTrue("Expected to find pixel itself at (" 
						+ x + "," + y + ")...", 
							map[y][x].x == x && map[y][x].y == y);
			
			// check pixels next to rectangles
			int x, y;

			y = 4;
			for (x=5;x<15;++x)
				this.checkMapPixel(map, x, y, x, y+1);
			x = 4;
			this.checkMapPixel(map, x, y, x+1, y+1);
			x = 15;
			this.checkMapPixel(map, x, y, x-1, y+1);

			y = 8;
			for (x=5;x<15;++x)
				this.checkMapPixel(map, x, y, x, y-1);
			x = 4;
			this.checkMapPixel(map, x, y, x+1, y-1);
			x = 15;
			this.checkMapPixel(map, x, y, x-1, y-1);

			y = 11;
			for (x=5;x<15;++x)
				this.checkMapPixel(map, x, y, x, y+1);
			x = 4;
			this.checkMapPixel(map, x, y, x+1, y+1);
			x = 15;
			this.checkMapPixel(map, x, y, x-1, y+1);

			y = 15;
			for (x=5;x<15;++x)
				this.checkMapPixel(map, x, y, x, y-1);
			x = 4;
			this.checkMapPixel(map, x, y, x+1, y-1);
			x = 15;
			this.checkMapPixel(map, x, y, x-1, y-1);
			
			// some more sample pixels
			x = 5; y = 2;
			this.checkMapPixel(map, x, y, 5, 5);
			x = 2; y = 6;
			this.checkMapPixel(map, x, y, 5, 6);
			x = 5; y = 9;
			this.checkMapPixel(map, x, y, 5, 7);
			x = 10; y = 10;
			this.checkMapPixel(map, x, y, 10, 12);
			x = 10; y = 11;
			this.checkMapPixel(map, x, y, 10, 12);
			
			// check image corners
			x = 0; y = 0;
			this.checkMapPixel(map, x, y, 5, 5);
			x = 0; y = 19;
			this.checkMapPixel(map, x, y, 5, 14);
			x = 19; y = 0;
			this.checkMapPixel(map, x, y, 14, 5);
			x = 19; y = 19;
			this.checkMapPixel(map, x, y, 14, 14);

		} catch (Exception e) {
			e.printStackTrace();
			fail("Did not expect an exception to occur in " 
					+ "testDistanceTransformClosestObjectPixelMap()...");
		}
		System.out.println("Done, everything fine!\n");
	}

	/**
	 * Check map entry of given pixel.
	 * 
	 * @param map				Map.
	 * @param x					x-position to check.
	 * @param y					y-position to check.
	 * @param px				Expected map x-coordinate.
	 * @param py				Expected map x-coordinate.
	 */
	private void checkMapPixel(Point2D.Double[][] map, 
			int x, int y, int px, int py) {
		assertTrue("Expected to find at (" + x + "," + y + ") " 
			+ "pixel (" + px + "," + py +"), but found " + 
				"(" + map[y][x].x + "," + map[y][x].y + ")...", 
			 	map[y][x].x == px && map[y][x].y == py);
	}

}