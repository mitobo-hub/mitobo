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

package de.unihalle.informatik.MiToBo.filters.vesselness;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;

/**
 * JUnit test class for {@link MPMFFilter2D}.
 * 
 * @author Birgit Moeller
 */
public class TestMPMFFilter2D {

	/**
	 * Numerical accuracy for tests.
	 */
	private final static double accuracy = 1.0e-10;
	
	/**
	 * Test object.
	 */
	private MPMFFilter2D testObject;
	
	/**
	 * Fixture.
	 */
	@Before
	public void initTestClass() {
		boolean thrown = false;
		try {
	    this.testObject = new MPMFFilter2D();
    } catch (ALDOperatorException e) {
    	thrown = true;
    }
		assertFalse("[TestMPMFFilter2D] could not construct test object!",
				thrown);
	}

	/**
	 * Test filter response function.
	 */
	@Test
	public void testFilterResponse() {
		double expectedValue = 0.14104739588694;
		double result = MPMFFilter2D.normalizedFilterResponse(1.0, 1.0);
		assertTrue("Expected " + expectedValue + ", got " + result + "...",
			Math.abs(result - expectedValue) < accuracy);
		
		expectedValue = 0.012615662610101;
		result = MPMFFilter2D.normalizedFilterResponse(1.0, 3.0);
		assertTrue("Expected " + expectedValue + ", got " + result + "...",
			Math.abs(result - expectedValue) < accuracy);
		
		expectedValue = 0.0030091930067516;
		result = MPMFFilter2D.normalizedFilterResponse(1.0, 5.0);
		assertTrue("Expected " + expectedValue + ", got " + result + "...",
			Math.abs(result - expectedValue) < accuracy);
		
		expectedValue = 0.065552905835525;
		result = MPMFFilter2D.normalizedFilterResponse(3.0, 1.0);
		assertTrue("Expected " + expectedValue + ", got " + result + "...",
			Math.abs(result - expectedValue) < accuracy);
		
		expectedValue = 0.027144583994607;
		result = MPMFFilter2D.normalizedFilterResponse(3.0, 3.0);
		assertTrue("Expected " + expectedValue + ", got " + result + "...",
			Math.abs(result - expectedValue) < accuracy);
		
		expectedValue = 0.010456192554591;
		result = MPMFFilter2D.normalizedFilterResponse(3.0, 5.0);
		assertTrue("Expected " + expectedValue + ", got " + result + "...",
			Math.abs(result - expectedValue) < accuracy);
		
		expectedValue = 0.033643800602569;
		result = MPMFFilter2D.normalizedFilterResponse(5.0, 1.0);
		assertTrue("Expected " + expectedValue + ", got " + result + "...",
			Math.abs(result - expectedValue) < accuracy);

		expectedValue = 0.022498144238149;
		result = MPMFFilter2D.normalizedFilterResponse(5.0, 3.0);
		assertTrue("Expected " + expectedValue + ", got " + result + "...",
			Math.abs(result - expectedValue) < accuracy);

		expectedValue = 0.012615662610101;
		result = MPMFFilter2D.normalizedFilterResponse(5.0, 5.0);
		assertTrue("Expected " + expectedValue + ", got " + result + "...",
			Math.abs(result - expectedValue) < accuracy);
}	
	
	/**
	 * Test calculation of middle scale.
	 */
	@Test
	public void testFindMiddleScale() {
		
		/*
		 * All groundtruth results have been calculated with Octave 3.2.4 on a
		 * 64-bit machine running Xubuntu 12.04, Kernel 3.2.0-54-generic, using 
		 * the following commands (assuming sigma = 2, length = 9):
		 * 
		 * format long
		 * 
		 * m = ones(13,1) * [-6:6]
		 * n = [-6:6]' * ones(1,13)
		 * 
		 * x =  cos(angle/180*pi)*m + sin(angle/180*pi)*n
		 * y = -sin(angle/180*pi)*m + cos(angle/180*pi)*n
		 * z = x./(sqrt(2*pi)*8) .* exp(-(x.^2) ./ 8)
		 * z = z .* (abs(x)<=6)
		 * z = z .* (abs(y)<=4)
		 * 
		 * save -ascii /tmp/tab.txt z
		 */

		double expectedResult = 1.414213562373097;
		double mScale = this.testObject.findMiddleScale(1.0, 2.0);
		expectedResult = 1.732050807568877;
		mScale = this.testObject.findMiddleScale(1.0, 3.0);
		assertTrue("Result should be " + expectedResult + ", got " + mScale + "...",
			Math.abs(mScale - expectedResult) < accuracy);
		expectedResult = 2.000000000000003;
		mScale = this.testObject.findMiddleScale(1.0, 4.0);
		assertTrue("Result should be " + expectedResult + ", got " + mScale + "...",
			Math.abs(mScale - expectedResult) < accuracy);
		expectedResult = 2.23606797749979;		
		mScale = this.testObject.findMiddleScale(1.0, 5.0);
		assertTrue("Result should be " + expectedResult + ", got " + mScale + "...",
			Math.abs(mScale - expectedResult) < accuracy);
		expectedResult = 2.449489742783181;
		mScale = this.testObject.findMiddleScale(2.0, 3.0);
		assertTrue("Result should be " + expectedResult + ", got " + mScale + "...",
			Math.abs(mScale - expectedResult) < accuracy);
		expectedResult = 2.828427124746194;
		mScale = this.testObject.findMiddleScale(2.0, 4.0);
		assertTrue("Result should be " + expectedResult + ", got " + mScale + "...",
			Math.abs(mScale - expectedResult) < accuracy);
		expectedResult = 3.464101615137756;
		mScale = this.testObject.findMiddleScale(2.0, 6.0);
		assertTrue("Result should be " + expectedResult + ", got " + mScale + "...",
			Math.abs(mScale - expectedResult) < accuracy);
		expectedResult = 3.872983346207414;
		mScale = this.testObject.findMiddleScale(3.0, 5.0);
		assertTrue("Result should be " + expectedResult + ", got " + mScale + "...",
			Math.abs(mScale - expectedResult) < accuracy);
		expectedResult = 4.242640687119288;
		mScale = this.testObject.findMiddleScale(3.0, 6.0);
		assertTrue("Result should be " + expectedResult + ", got " + mScale + "...",
			Math.abs(mScale - expectedResult) < accuracy);
		expectedResult = 4.898979485566365;
		mScale = this.testObject.findMiddleScale(3.0, 8.0);
		assertTrue("Result should be " + expectedResult + ", got " + mScale + "...",
			Math.abs(mScale - expectedResult) < accuracy);
		expectedResult = 4.898979485566361;
		mScale = this.testObject.findMiddleScale(4.0, 6.0);
		assertTrue("Result should be " + expectedResult + ", got " + mScale + "...",
			Math.abs(mScale - expectedResult) < accuracy);
		expectedResult = 5.656854249492388; 
		mScale = this.testObject.findMiddleScale(4.0, 8.0);
		assertTrue("Result should be " + expectedResult + ", got " + mScale + "...",
			Math.abs(mScale - expectedResult) < accuracy);
	}
}