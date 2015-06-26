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

package de.unihalle.informatik.MiToBo.math;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

/**
 * JUnit test class for {@link MathX}.
 * 
 * @author Birgit Moeller
 */
public class TestMathX {

	/**
	 * Numerical accuracy for tests.
	 */
	private final static double accuracy = 1.0e-4;

	/**
	 * Field of target factorial values.
	 */
	private int[] factorialTargetValues;
	
	/*
	 * You can find the definitions of the test data at the end of this file.
	 */
	
	/**
	 * Fixture.
	 */
	@Before
	public void initTestClass() {
		// pre-calculate factorials
		this.factorialTargetValues = new int[100];
		this.factorialTargetValues[0] = 1;
		this.factorialTargetValues[1] = 1;
		for (int n=2; n<100; ++n)
			this.factorialTargetValues[n] = this.factorialTargetValues[n-1] * n;
	}

	/**
	 * Test factorial function.
	 */
	@Test
	public void testFactorial() {

		int[] resultFactorials = new int[100];
		for (int i=0; i<100; ++i)
			resultFactorials[i] = MathX.factorial(i);

		// check results
		for (int i=0; i<100; ++i)
			assertTrue("[TestMathX] factorial(" + i + ") : expected " 
				+ this.factorialTargetValues[i] + " , got " + resultFactorials[i],
					this.factorialTargetValues[i] == resultFactorials[i]);
	}
}
	