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

package de.unihalle.informatik.MiToBo.math.arrays.filter;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import de.unihalle.informatik.Alida.exceptions.ALDException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;

/**
 * JUnit test class for {@link GaussFilterDouble1D}.
 * 
 * @author Birgit Moeller
 */
public class TestGaussFilterDouble1D {

	/**
	 * Numerical accuracy for tests.
	 */
	private final static double accuracy = 1.0e-4;

	/**
	 * Fixture.
	 */
	@Before
	public void initTestClass() {
		// nothing to do here
	}

	/**
	 * Test Gaussian convolution.
	 */
	@Test
	public void testGaussFilter() {
		
		/*
		 * The test data used here can be found at the end of the file.
		 */
		
		double[] kernel;
		double[] kernel_external;
		double[] result;
		GaussFilterDouble1D gaussOp = null;
		
		boolean exceptionThrown = false;
		try {
	    gaussOp = new GaussFilterDouble1D();
    } catch (ALDOperatorException e) {
    	exceptionThrown = true;
    }
		assertFalse("[GaussFilterDouble1D] constructor threw an exception!?", exceptionThrown);
		if (gaussOp == null)
			return;

		// specify test data
		gaussOp.setInputArray(testData);
		
		/*
		 * assume periodic data		
		 */
		gaussOp.setDataIsPeriodic(true);
		
		// set standard deviation
		gaussOp.setSigma(3.0);
		exceptionThrown = false;
		try {
	    gaussOp.runOp();
    } catch (ALDException e) {
    	exceptionThrown = true;
    }
		assertFalse("[GaussFilterDouble1D] running operator failed!?", exceptionThrown);
		result = gaussOp.getResultArray();
		assertTrue("[GaussFilterDouble1D] sample size is 40, got output of size " + result.length, 
			result.length == 40);
		this.compareData(resultPeriodic, result);
		kernel = gaussOp.getKernel();
		this.compareData(gaussKernel, kernel);


		// provide an external kernel
		kernel_external = GaussFilterDouble1D.getGaussKernel(3.0);
		this.compareData(gaussKernel, kernel_external);
		// make sure that kernel functions are consistent
		this.compareData(kernel, kernel_external);
		
		gaussOp.setKernel(kernel_external);
		exceptionThrown = false;
		try {
	    gaussOp.runOp();
    } catch (ALDException e) {
    	exceptionThrown = true;
    }
		assertFalse("[GaussFilterDouble1D] running operator failed!?", exceptionThrown);
		
		// check global variables
		result = gaussOp.getResultArray();
		assertTrue("[GaussFilterDouble1D] sample size is 40, got output of size " + result.length, 
			result.length == 40);
		this.compareData(resultPeriodic, result);

		/*
		 * assume non-periodic data		
		 */
		gaussOp.setDataIsPeriodic(false);
		
		gaussOp.setSigma(3.0);
		exceptionThrown = false;
		try {
	    gaussOp.runOp();
    } catch (ALDException e) {
    	exceptionThrown = true;
    }
		assertFalse("[GaussFilterDouble1D] running operator failed!?", exceptionThrown);
		result = gaussOp.getResultArray();
		assertTrue("[GaussFilterDouble1D] sample size is 40, got output of size " + result.length, 
			result.length == 40);
		this.compareData(resultNonPeriodic, result);

		kernel_external = GaussFilterDouble1D.getGaussKernel(3.0);
		gaussOp.setKernel(kernel_external);
		exceptionThrown = false;
		try {
	    gaussOp.runOp();
    } catch (ALDException e) {
    	exceptionThrown = true;
    }
		assertFalse("[GaussFilterDouble1D] running operator failed!?", exceptionThrown);
		
		// check global variables
		result = gaussOp.getResultArray();
		assertTrue("[GaussFilterDouble1D] sample size is 40, got output of size " + result.length, 
			result.length == 40);
		this.compareData(resultNonPeriodic, result);
	}
	
	/**
	 * Compare contents of given arrays with defined accuracy.
	 * @param truth		Groundtruth data.
	 * @param result	Data to check against groundtruth.
	 */
	protected void compareData(double[] truth, double[] result) {
		for (int i=0; i<truth.length; ++i) {
			assertTrue("[GaussFilterDouble1D] got unexpected value in result[" + i + "] = " 
					+  result[i] + ", expected " + truth[i],
				Math.abs(result[i] - truth[i]) < TestGaussFilterDouble1D.accuracy); 
		}
	}
	
	/**
	 * Test data, randomly generated with Octave (size 1 x 40).
	 */
	private static final transient double[] testData = {
			5.623896, 7.658024, 1.785343, 4.351788, 6.041751, 
			3.991966, 4.021994, 0.289701, 9.253908, 7.815675, 
			1.517468, 1.209385, 5.732721, 7.206048, 3.508089, 
			9.950321, 1.061910, 6.835215, 0.910112, 7.016299, 
			7.411174, 8.291020, 2.206987, 7.485583, 2.181438, 
			1.341773, 7.266244, 7.954417, 1.196684, 8.003860, 
			9.291073, 0.846572, 1.909630, 1.202719, 9.438694, 
			0.532768, 9.720796, 5.820479, 1.700625, 0.308036
	};
	
	/**
	 * Gaussian kernel for a standard deviation of 3.0.
	 */
	private static final transient double[] gaussKernel = {
			0.10629, 0.14032, 0.16577, 0.17524, 0.16577, 0.14032, 0.10629
	};
	
	/**
	 * Result for periodic convolution with sigma = 3.0.
	 */
	private static final transient double[] resultPeriodic = {
			3.8764, 4.0470, 4.3977, 4.7201, 4.1015,
			4.1925, 4.8542, 4.7118, 4.2954, 4.4024,
			4.6617, 4.8781, 4.9931, 4.6347, 5.2673,
			5.1776, 5.1003, 5.0661, 5.5835, 5.0789,
			5.7958, 5.4108, 5.2365, 4.9510, 4.9016,
			4.3296, 4.9347, 5.3276, 5.4111, 5.3514,
			4.5045, 4.4274, 4.2099, 4.3063, 4.2824,
			4.6387, 4.4803, 4.6323, 4.3377, 4.3825
	};

	/**
	 * Result for non-periodic convolution with sigma = 3.0.
	 */
	private static final transient double[] resultNonPeriodic = {
			2.9681, 3.8230, 4.3650, 4.7201, 4.1015,
			4.1925, 4.8542, 4.7118, 4.2954, 4.4024,
			4.6617, 4.8781, 4.9931, 4.6347, 5.2673,
			5.1776, 5.1003, 5.0661, 5.5835, 5.0789,
			5.7958, 5.4108, 5.2365, 4.9510, 4.9016,
			4.3296, 4.9347, 5.3276, 5.4111, 5.3514,
			4.5045, 4.4274, 4.2099, 4.3063, 4.2824,
			4.6387, 4.4803, 4.0345, 2.7346, 2.1858
	};

}
