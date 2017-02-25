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

package de.unihalle.informatik.MiToBo.features;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * JUnit test class for {@link MorphologyAnalyzer2D}.
 * 
 * @author moeller
 */
public class TestMorphologyAnalyzer2D {

	/**
	 * Test direction array clean-up in lobe/neck detection.
	 */
	@Test
	public void testLobeNeckArrayCleanUp() {

		int[] testArray = 
				new int[]{-1,-1,-1,-1,1,1,1,1,1,1,1,-1,1,1,-1,-1,-1,-1};
		int[] expectedResult = 
				new int[]{-1,-1,-1,-1,1,1,1,1,1,1,1,-1,-1,-1,-1,-1,-1,-1};
		MorphologyAnalyzer2D.removeShortProtrusions(testArray, 3);		
		
		for (int i=0; i<expectedResult.length; ++i) {
			assertTrue("Wrong entry at position " + i + ": expected " 
				+ expectedResult[i] + ", got " + testArray[i] + "...",
					testArray[i] == expectedResult[i]);	
		}

		// ---
		
		testArray = 
				new int[]{-1,-1,-1,-1,1,1,1,-1,-1,-1,-1,-1,1};
		expectedResult = 
				new int[]{-1,-1,-1,-1,1,1,1,-1,-1,-1,-1,-1,-1};
		MorphologyAnalyzer2D.removeShortProtrusions(testArray, 3);		
		
		for (int i=0; i<expectedResult.length; ++i) {
			assertTrue("Wrong entry at position " + i + ": expected " 
				+ expectedResult[i] + ", got " + testArray[i] + "...",
					testArray[i] == expectedResult[i]);	
		}

		// ---
		
		testArray = 
				new int[]{1,1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,1,1};
		expectedResult = 
				new int[]{1,1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,1,1};
		MorphologyAnalyzer2D.removeShortProtrusions(testArray, 3);		
		
		for (int i=0; i<expectedResult.length; ++i) {
			assertTrue("Wrong entry at position " + i + ": expected " 
				+ expectedResult[i] + ", got " + testArray[i] + "...",
					testArray[i] == expectedResult[i]);	
		}

		// ---
		
		testArray = 
				new int[]{1,1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,1,-1};
		expectedResult = 
				new int[]{-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1};
		MorphologyAnalyzer2D.removeShortProtrusions(testArray, 3);		
		
		for (int i=0; i<expectedResult.length; ++i) {
			assertTrue("Wrong entry at position " + i + ": expected " 
				+ expectedResult[i] + ", got " + testArray[i] + "...",
					testArray[i] == expectedResult[i]);	
		}

		// ---
		
		testArray = 
				new int[]{1,1,1,-1,-1,1,1,1,1,1,1,-1,-1,1,1,1};
		expectedResult = 
				new int[]{1,1,1,-1,-1,1,1,1,1,1,1,-1,-1,1,1,1};
		MorphologyAnalyzer2D.removeShortProtrusions(testArray, 5);		
		
		for (int i=0; i<expectedResult.length; ++i) {
			assertTrue("Wrong entry at position " + i + ": expected " 
				+ expectedResult[i] + ", got " + testArray[i] + "...",
					testArray[i] == expectedResult[i]);	
		}

		// ---
		
		testArray = 
				new int[]{1,1,1,-1,-1,1,1,1,1,-1,-1,-1,-1,-1,-1,1,1};
		expectedResult = 
				new int[]{1,1,1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,1,1};
		MorphologyAnalyzer2D.removeShortProtrusions(testArray, 5);		
		
		for (int i=0; i<expectedResult.length; ++i) {
			assertTrue("Wrong entry at position " + i + ": expected " 
				+ expectedResult[i] + ", got " + testArray[i] + "...",
					testArray[i] == expectedResult[i]);	
		}
		
		// ---
		
		testArray = 
				new int[]{1,1,-1,-1,1,1,1,1,1,-1,-1,-1,-1,-1,-1,1,1};
		expectedResult = 
				new int[]{-1,-1,-1,-1,1,1,1,1,1,-1,-1,-1,-1,-1,-1,-1,-1};
		MorphologyAnalyzer2D.removeShortProtrusions(testArray, 5);		
		
		for (int i=0; i<expectedResult.length; ++i) {
			assertTrue("Wrong entry at position " + i + ": expected " 
				+ expectedResult[i] + ", got " + testArray[i] + "...",
					testArray[i] == expectedResult[i]);	
		}

		// ---
		
		testArray = 
				new int[]{-1,1,1,1,1,1,1,1,-1,-1,1,1,1,1,1,-1,-1,1,1,1,1,1,1,-1,-1};
		expectedResult = 
				new int[]{-1,1,1,1,1,1,1,1,-1,-1,1,1,1,1,1,-1,-1,1,1,1,1,1,1,-1,-1};
		MorphologyAnalyzer2D.removeShortProtrusions(testArray, 5);		
		
		for (int i=0; i<expectedResult.length; ++i) {
			assertTrue("Wrong entry at position " + i + ": expected " 
				+ expectedResult[i] + ", got " + testArray[i] + "...",
					testArray[i] == expectedResult[i]);	
		}

		// ---
		
		testArray = 
				new int[]{1,1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,1,1,1,1,1,1,-1,-1};
		expectedResult = 
				new int[]{-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,1,1,1,1,1,1,-1,-1};
		MorphologyAnalyzer2D.removeShortProtrusions(testArray, 5);		
		
		for (int i=0; i<expectedResult.length; ++i) {
			assertTrue("Wrong entry at position " + i + ": expected " 
				+ expectedResult[i] + ", got " + testArray[i] + "...",
					testArray[i] == expectedResult[i]);	
		}

	}
}