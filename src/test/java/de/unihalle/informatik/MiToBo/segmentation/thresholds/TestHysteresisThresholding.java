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

package de.unihalle.informatik.MiToBo.segmentation.thresholds;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import de.unihalle.informatik.Alida.exceptions.ALDException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageByte;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;

/**
 * JUnit test class for {@link HysteresisThresholding}.
 * 
 * @author Birgit Moeller
 */
public class TestHysteresisThresholding {

	/**
	 * Test object.
	 */
	private HysteresisThresholding testObject;
	
	/**
	 * Fixture.
	 */
	@Before
	public void initTestClass() {
		boolean thrown = false;
		try {
	    this.testObject = new HysteresisThresholding();
    } catch (ALDOperatorException e) {
    	thrown = true;
    }
		assertFalse("[TestHysteresisThresholding] could not construct test object!",
			thrown);
	}

	/**
	 * Test thresholding heuristic.
	 */
	@Test
	public void testThresholding() {
		
		// test image of size 7x7
		int[][] testData = new int[][]{
			{ 20,  30,  25,  40,  10,   2,  37}, 
			{ 12, 180, 160, 220, 175, 199, 201},
			{  0, 130,  10, 149,  14,  15,  70},
			{  2,  70,  23,  42, 100,   9,   9},
			{133,  32,  33,  34,   0,   1,   0},
			{ 45, 101,   1,   2,   3,   4,   5},
			{ 21,  22,  23, 255,   0, 150,   0}};
		
		MTBImageByte testImage = (MTBImageByte)MTBImage.createMTBImage(
			7, 7, 1, 1, 1, MTBImageType.MTB_BYTE);
		for (int y=0; y<7; ++y) {
			for (int x=0; x<7; ++x) {
				testImage.putValueInt(x, y, testData[y][x]);
			}
		}
		
		// expected result for thresholds 70 and 150
		int[][] expectedResult = new int[][]{
				{  0,   0,   0,   0,   0,   0,   0}, 
				{  0, 255, 255, 255, 255, 255, 255},
				{  0, 255,   0, 255,   0,   0, 255},
				{  0, 255,   0,   0, 255,   0,   0},
				{255,   0,   0,   0,   0,   0,   0},
				{  0, 255,   0,   0,   0,   0,   0},
				{  0,   0,   0, 255,   0, 255,   0}};
		
		this.testObject.setInputImage(testImage);
		this.testObject.setLowerThreshold(70);
		this.testObject.setHigherThreshold(150);
		boolean success = true;
		try {
	    this.testObject.runOp();
	    MTBImage result = this.testObject.getResultImage();
			for (int y=0; y<7; ++y) {
				for (int x=0; x<7; ++x) {
					assertTrue("x = " + x + " , y = " + y + " , image value = " +
						testImage.getValueInt(x, y) + " : expected " + expectedResult[y][x] 
							+ " , got " + result.getValueInt(x, y), 
								expectedResult[y][x] == result.getValueInt(x, y));
				}
			}
    } catch (ALDException e) {
    	success = false;
    }
		assertTrue("[TestHysteresisThresholding] running operator failed!",success);
		
		// expected result for thresholds 150 and 199
		expectedResult = new int[][]{
				{  0,   0,   0,   0,   0,   0,   0}, 
				{  0, 255, 255, 255, 255, 255, 255},
				{  0,   0,   0,   0,   0,   0,   0},
				{  0,   0,   0,   0,   0,   0,   0},
				{  0,   0,   0,   0,   0,   0,   0},
				{  0,   0,   0,   0,   0,   0,   0},
				{  0,   0,   0, 255,   0,   0,   0}};

		this.testObject.setInputImage(testImage);
		this.testObject.setLowerThreshold(150);
		this.testObject.setHigherThreshold(199);
		success = true;
		try {
	    this.testObject.runOp();
	    MTBImage result = this.testObject.getResultImage();
			for (int y=0; y<7; ++y) {
				for (int x=0; x<7; ++x) {
					assertTrue("x = " + x + " , y = " + y + " , image value = " +
						testImage.getValueInt(x, y) + " : expected " + expectedResult[y][x] 
							+ " , got " + result.getValueInt(x, y), 
								expectedResult[y][x] == result.getValueInt(x, y));
				}
			}
    } catch (ALDException e) {
    	success = false;
    }
		assertTrue("[TestHysteresisThresholding] running operator failed!",success);

	}
}