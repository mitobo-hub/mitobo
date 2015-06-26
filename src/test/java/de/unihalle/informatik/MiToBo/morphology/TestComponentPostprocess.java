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

import org.junit.Before;
import org.junit.Test;

import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageByte;
import de.unihalle.informatik.MiToBo.morphology.ComponentPostprocess.ProcessMode;

/**
 * JUnit test class for {@link ComponentPostprocess}.
 * 
 * @author moeller
 */
public class TestComponentPostprocess {

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
			100, 100, 1, 1, 1, MTBImageType.MTB_BYTE);
		this.dummyImage.fillBlack();
		// draw two rectangles in the center of the image
		for (int y=40;y<60;++y)
			for (int x=30;x<40;++x)
				this.dummyImage.putValueInt(x, y, 255);
		for (int y=40;y<60;++y)
			for (int x=60;x<70;++x)
				this.dummyImage.putValueInt(x, y, 255);
	}
	
	/**
	 * Test if Voronoi expansion works correctly.
	 */
	@Test
	public void testVoronoiExpansion() {
		try {
			ComponentPostprocess postOp = new ComponentPostprocess(
				this.dummyImage, ProcessMode.VORONOI_EXPAND);
			postOp.setMaximalVoronoiExpansionDistance(15);
			postOp.runOp();
			MTBImage resultImg = postOp.getResultImage();

			// check if the result image is correct
			for (int y=0;y<100;++y) {
				for (int x=0;x<100;++x) {
					double d1 = distToComponentOne(x,y);
					double d2 = distToComponentTwo(x,y);
					if (d1 < d2 && d1 < 15*15)
						assertTrue("Expecting (" + x + "," + y + ") to be filled "
								+ "with label of first component, but is " 
									+ resultImg.getValueInt(x,y) + "...",
									 resultImg.getValueInt(x, y) == 1);
					else if (d2 < d1 && d2 < 15*15)
						assertTrue("Expecting (" + x + "," + y + ") to be filled "
							+ "with label of second component, but is " 
								+ resultImg.getValueInt(x,y) + "...",
								 resultImg.getValueInt(x, y) == 2);
					else
						assertTrue("Assuming (" + x + "," + y + ") to be " 
							+ "background, but is " + resultImg.getValueInt(x,y) 
								+ "...", resultImg.getValueInt(x,y) == 0);
				}
			}
		} catch (Exception e) {
			fail("Did not expect an exception to occur...");
			e.printStackTrace();
		}
	}
	
	/**
	 * Calculates the distance of a point to the contour of the 2nd object.
	 *  
	 * @param x	x-coordinate of point in question.
	 * @param y	y-coordinate of point in question.
	 * @return	Squared distance of point to contour.
	 */
	private double distToComponentTwo(int x, int y) {

		int cx, cy;
		double dist;
		double minDist = Double.MAX_VALUE;
		
		cy=40;
		for (cx=60; cx<70; ++cx) {
			dist = (cx-x)*(cx-x) + (cy-y)*(cy-y);
			if (dist < minDist)
				minDist = dist;
		}
		cy=59;
		for (cx=60; cx<70; ++cx) {
			dist = (cx-x)*(cx-x) + (cy-y)*(cy-y);
			if (dist < minDist)
				minDist = dist;
		}
		cx=60;
		for (cy=40; cy<60; ++cy) {
			dist = (cx-x)*(cx-x) + (cy-y)*(cy-y);
			if (dist < minDist)
				minDist = dist;
		}
		cx=69;
		for (cy=40; cy<60; ++cy) {
			dist = (cx-x)*(cx-x) + (cy-y)*(cy-y);
			if (dist < minDist)
				minDist = dist;
		}
		
		return minDist;
	}
	
	/**
	 * Calculates the distance of a point to the contour of the 1st object.
	 *  
	 * @param x	x-coordinate of point in question.
	 * @param y	y-coordinate of point in question.
	 * @return	Squared distance of point to contour.
	 */
	private double distToComponentOne(int x, int y) {

		int cx, cy;
		double dist;
		double minDist = Double.MAX_VALUE;
		
		cy=40;
		for (cx=30; cx<40; ++cx) {
			dist = (cx-x)*(cx-x) + (cy-y)*(cy-y);
			if (dist < minDist)
				minDist = dist;
		}
		cy=59;
		for (cx=30; cx<40; ++cx) {
			dist = (cx-x)*(cx-x) + (cy-y)*(cy-y);
			if (dist < minDist)
				minDist = dist;
		}
		cx=30;
		for (cy=40; cy<60; ++cy) {
			dist = (cx-x)*(cx-x) + (cy-y)*(cy-y);
			if (dist < minDist)
				minDist = dist;
		}
		cx=39;
		for (cy=40; cy<60; ++cy) {
			dist = (cx-x)*(cx-x) + (cy-y)*(cy-y);
			if (dist < minDist)
				minDist = dist;
		}
		
		return minDist;
	}

}