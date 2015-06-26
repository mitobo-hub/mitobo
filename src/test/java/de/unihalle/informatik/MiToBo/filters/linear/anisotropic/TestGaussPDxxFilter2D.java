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

package de.unihalle.informatik.MiToBo.filters.linear.anisotropic;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageDouble;
import de.unihalle.informatik.MiToBo.filters.linear.anisotropic.GaussPDxxFilter2D;

/**
 * JUnit test class for {@link GaussPDxxFilter2D}.
 * 
 * @author Birgit Moeller
 */
public class TestGaussPDxxFilter2D {

	/**
	 * Local accuracy for numerical tests.
	 */
	private static final double accuracy = 1.0e-10;
	
	/**
	 * Test object.
	 */
	private GaussPDxxFilter2D testObject;
	
	/**
	 * Fixture.
	 */
	@Before
	public void initTestClass() {
		boolean thrown = false;
		try {
	    this.testObject = new GaussPDxxFilter2D();
    } catch (ALDOperatorException e) {
    	thrown = true;
    }
		assertFalse("[TestGaussPDxxFilter2D] could not construct test object!",
				thrown);
	}

	/**
	 * Test calculation of filter masks.
	 */
	@Test
	public void testGetKernel() {
		
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
		 * z = (x.*x - 4) / (sqrt(2*pi)*32) .* exp(-(x.^2) ./ 8)
		 * z = z .* (abs(x)<=6)
		 * z = z .* (abs(y)<=4)
		 * 
		 * save -ascii /tmp/tab.txt z
		 */

		// test angles of 0 and 180
		MTBImageDouble kernel = this.testObject.getKernel(0.0);
		boolean[][] kernelMask = this.testObject.getKernelMask();
		double[] expectedKernelRow = new double[]{
			4.43184841e-03,  1.15029472e-02,  2.02466124e-02,  2.02371243e-02, 
			0.00000000e+00, -3.30061244e-02, -4.98677851e-02, -3.30061244e-02, 
			0.00000000e+00,  2.02371243e-02,  2.02466124e-02,  1.15029472e-02,
			4.43184841e-03};
		double[][] expectedKernel = new double[13][13];
		for (int i=0; i<2; ++i) {
			for (int j=0; j<13; ++j) {
				expectedKernel[i][j] = 0.0;
			}
		}
		for (int i=2; i<11; ++i) {
			for (int j=0; j<13; ++j) {
				expectedKernel[i][j] = expectedKernelRow[j];
			}
		}
		for (int i=11; i<13; ++i) {
			for (int j=0; j<13; ++j) {
				expectedKernel[i][j] = 0.0;
			}
		}
		double kernelMean = 0.0;
		int kernelCount = 0;
		for (int i=0; i<13; ++i) {
			for (int j=0; j<13; ++j) {
				if (kernelMask[j][i]) {
					kernelMean += expectedKernel[i][j];
					++kernelCount;
				}
			}
		}
		kernelMean = kernelMean/kernelCount;
		for (int i=0; i<13; ++i) {
			for (int j=0; j<13; ++j) {
				if (kernelMask[j][i]) {
					expectedKernel[i][j] = expectedKernel[i][j] - kernelMean;
				}
			}
		}
		assertTrue("Kernel dimension in x should be 13, got " + kernel.getSizeX(),
				kernel.getSizeX() == 13);
		assertTrue("Kernel dimension in y should be 13, got " + kernel.getSizeY(),
				kernel.getSizeY() == 13);
		for (int i=0; i<13; ++i) {
			for (int j=0; j<13; ++j) {
				assertTrue("Angle = " + 0.0 + " , kernel entry does not match: " + 
						i + " , " + j + " = " + kernel.getValueDouble(j, i) +
						" , expected " + expectedKernel[i][j],
						Math.abs(kernel.getValueDouble(j, i) - expectedKernel[i][j])
						< accuracy);
			}
		}
		kernel = this.testObject.getKernel(180.0);
		assertTrue("Kernel dimension in x should be 13, got " + kernel.getSizeX(),
				kernel.getSizeX() == 13);
		assertTrue("Kernel dimension in y should be 13, got " + kernel.getSizeY(),
				kernel.getSizeY() == 13);
		for (int i=0; i<13; ++i) {
			for (int j=0; j<13; ++j) {
				assertTrue("Angle = " + 0.0 + " , kernel entry does not match: " + 
						i + " , " + j + " = " + kernel.getValueDouble(j, i) +
						" , expected " + expectedKernel[i][j],
						Math.abs(kernel.getValueDouble(j, i) - expectedKernel[i][j])
						< accuracy);
			}
		}

		// test angles of 90 and 270 degrees
		kernel = this.testObject.getKernel(90.0);
		kernelMask = this.testObject.getKernelMask();
		double[] expectedKernelColumn = new double[]{
			4.43184841e-03,  1.15029472e-02,  2.02466124e-02,  2.02371243e-02, 
			0.00000000e+00, -3.30061244e-02, -4.98677851e-02, -3.30061244e-02, 
			0.00000000e+00,  2.02371243e-02,  2.02466124e-02,  1.15029472e-02,
			4.43184841e-03};
		expectedKernel = new double[13][13];
		for (int i=0; i<13; ++i) {
			for (int j=0; j<2; ++j) {
				expectedKernel[i][j] = 0.0;
			}
		}
		for (int i=0; i<13; ++i) {
			for (int j=2; j<11; ++j) {
				expectedKernel[i][j] = expectedKernelColumn[i];
			}
		}
		for (int i=0; i<13; ++i) {
			for (int j=11; j<13; ++j) {
				expectedKernel[i][j] = 0.0;
			}
		}
		kernelMean = 0.0;
		kernelCount = 0;
		for (int i=0; i<13; ++i) {
			for (int j=0; j<13; ++j) {
				if (kernelMask[j][i]) {
					kernelMean += expectedKernel[i][j];
					++kernelCount;
				}
			}
		}
		kernelMean = kernelMean/kernelCount;
		for (int i=0; i<13; ++i) {
			for (int j=0; j<13; ++j) {
				if (kernelMask[j][i]) {
					expectedKernel[i][j] = expectedKernel[i][j] - kernelMean;
				}
			}
		}
		assertTrue("Kernel dimension in x should be 13, got " + kernel.getSizeX(),
				kernel.getSizeX() == 13);
		assertTrue("Kernel dimension in y should be 13, got " + kernel.getSizeY(),
				kernel.getSizeY() == 13);
		for (int i=0; i<13; ++i) {
			for (int j=0; j<13; ++j) {
				assertTrue("Kernel entry does not match: " + 
					i + " , " + j + " = " + kernel.getValueDouble(j, i)
					+ " , expected " + expectedKernel[i][j],
					Math.abs(kernel.getValueDouble(j, i) - expectedKernel[i][j])
						< accuracy);
			}
		}
		kernel = this.testObject.getKernel(270.0);
		assertTrue("Kernel dimension in x should be 13, got " + kernel.getSizeX(),
				kernel.getSizeX() == 13);
		assertTrue("Kernel dimension in y should be 13, got " + kernel.getSizeY(),
				kernel.getSizeY() == 13);
		for (int i=0; i<13; ++i) {
			for (int j=0; j<13; ++j) {
				assertTrue("Kernel entry does not match: " + 
					i + " , " + j + " = " + kernel.getValueDouble(j, i)
					+ " , expected " + expectedKernel[i][j],
					Math.abs(kernel.getValueDouble(j, i) - expectedKernel[i][j])
						< accuracy);
			}
		}

		// test angle of 45 and 225 degrees
		kernel = this.testObject.getKernel(45.0);
		kernelMask = this.testObject.getKernelMask();
		expectedKernel = new double[][]{
			{ 0.00000000e+00, 0.00000000e+00, 0.00000000e+00, 0.00000000e+00, 
				6.39352240e-03, 1.19532801e-02, 0.00000000e+00, 0.00000000e+00, 
				0.00000000e+00, 0.00000000e+00,-0.00000000e+00,-0.00000000e+00, 
			 -0.00000000e+00},
			{ 0.00000000e+00, 0.00000000e+00, 0.00000000e+00, 6.39352240e-03, 
				1.19532801e-02, 1.83960906e-02, 2.22123181e-02, 0.00000000e+00, 
				0.00000000e+00,-0.00000000e+00,-0.00000000e+00,-0.00000000e+00, 
			 -0.00000000e+00},
			{ 0.00000000e+00, 0.00000000e+00, 6.39352240e-03, 1.19532801e-02, 
				1.83960906e-02, 2.22123181e-02, 1.83453329e-02, 3.55172593e-03, 
			 -0.00000000e+00,-0.00000000e+00,-0.00000000e+00,-0.00000000e+00, 
			 -0.00000000e+00},
			{ 0.00000000e+00, 6.39352240e-03, 1.19532801e-02, 1.83960906e-02, 
				2.22123181e-02, 1.83453329e-02, 3.55172593e-03,-1.94185350e-02, 
			 -4.09906426e-02,-0.00000000e+00,-0.00000000e+00,-0.00000000e+00, 
			  0.00000000e+00},
			{ 6.39352240e-03, 1.19532801e-02, 1.83960906e-02, 2.22123181e-02, 
				1.83453329e-02, 3.55172593e-03,-1.94185350e-02,-4.09906426e-02, 
			 -4.98677851e-02,-4.09906426e-02,-0.00000000e+00, 0.00000000e+00, 
			  0.00000000e+00},
			{ 1.19532801e-02, 1.83960906e-02, 2.22123181e-02, 1.83453329e-02, 
				3.55172593e-03,-1.94185350e-02,-4.09906426e-02,-4.98677851e-02, 
			 -4.09906426e-02,-1.94185350e-02, 3.55172593e-03, 0.00000000e+00, 
			  0.00000000e+00},
			{ 0.00000000e+00, 2.22123181e-02, 1.83453329e-02, 3.55172593e-03, 
			 -1.94185350e-02,-4.09906426e-02,-4.98677851e-02,-4.09906426e-02, 
			 -1.94185350e-02, 3.55172593e-03, 1.83453329e-02, 2.22123181e-02, 
			  0.00000000e+00},
			{ 0.00000000e+00, 0.00000000e+00, 3.55172593e-03,-1.94185350e-02, 
			 -4.09906426e-02,-4.98677851e-02,-4.09906426e-02,-1.94185350e-02, 
			  3.55172593e-03, 1.83453329e-02, 2.22123181e-02, 1.83960906e-02, 
			  1.19532801e-02},
			{ 0.00000000e+00, 0.00000000e+00,-0.00000000e+00,-4.09906426e-02, 
			 -4.98677851e-02,-4.09906426e-02,-1.94185350e-02, 3.55172593e-03, 
			  1.83453329e-02, 2.22123181e-02, 1.83960906e-02, 1.19532801e-02, 
			  6.39352240e-03},
			{ 0.00000000e+00,-0.00000000e+00,-0.00000000e+00,-0.00000000e+00, 
			 -4.09906426e-02,-1.94185350e-02, 3.55172593e-03, 1.83453329e-02, 
			  2.22123181e-02, 1.83960906e-02, 1.19532801e-02, 6.39352240e-03, 
			  0.00000000e+00},
			{-0.00000000e+00,-0.00000000e+00,-0.00000000e+00,-0.00000000e+00, 
			 -0.00000000e+00, 3.55172593e-03, 1.83453329e-02, 2.22123181e-02, 
			  1.83960906e-02, 1.19532801e-02, 6.39352240e-03, 0.00000000e+00, 
			  0.00000000e+00},
			{-0.00000000e+00,-0.00000000e+00,-0.00000000e+00,-0.00000000e+00, 
				0.00000000e+00, 0.00000000e+00, 2.22123181e-02, 1.83960906e-02, 
				1.19532801e-02, 6.39352240e-03, 0.00000000e+00, 0.00000000e+00, 
				0.00000000e+00},
			{-0.00000000e+00,-0.00000000e+00,-0.00000000e+00, 0.00000000e+00, 
				0.00000000e+00, 0.00000000e+00, 0.00000000e+00, 1.19532801e-02, 
				6.39352240e-03, 0.00000000e+00, 0.00000000e+00, 0.00000000e+00, 
				0.00000000e+00}};
		
		kernelMean = 0.0;
		kernelCount = 0;
		for (int i=0; i<13; ++i) {
			for (int j=0; j<13; ++j) {
				if (kernelMask[j][i]) {
					kernelMean += expectedKernel[i][j];
					++kernelCount;
				}
			}
		}
		kernelMean = kernelMean/kernelCount;
		for (int i=0; i<13; ++i) {
			for (int j=0; j<13; ++j) {
				if (kernelMask[j][i]) {
					expectedKernel[i][j] = expectedKernel[i][j] - kernelMean;
				}
			}
		}
		assertTrue("Kernel dimension in x should be 13, got " + kernel.getSizeX(),
				kernel.getSizeX() == 13);
		assertTrue("Kernel dimension in y should be 13, got " + kernel.getSizeY(),
				kernel.getSizeY() == 13);
		for (int i=0; i<13; ++i) {
			for (int j=0; j<13; ++j) {
				assertTrue("Kernel entry does not match: " + 
					i + " , " + j + " = " + kernel.getValueDouble(j, i) +
					" , expected " + expectedKernel[i][j],
					Math.abs(kernel.getValueDouble(j, i) - expectedKernel[i][j])
						< accuracy);
			}
		}
		kernel = this.testObject.getKernel(225.0);
		assertTrue("Kernel dimension in x should be 13, got " + kernel.getSizeX(),
				kernel.getSizeX() == 13);
		assertTrue("Kernel dimension in y should be 13, got " + kernel.getSizeY(),
				kernel.getSizeY() == 13);
		for (int i=0; i<13; ++i) {
			for (int j=0; j<13; ++j) {
				assertTrue("Kernel entry does not match: " + 
					i + " , " + j + " = " + kernel.getValueDouble(j, i) +
					" , expected " + expectedKernel[i][j],
					Math.abs(kernel.getValueDouble(j, i) - expectedKernel[i][j])
						< accuracy);
			}
		}

		// test angle of 10 degree
		kernel = this.testObject.getKernel(10.0);
		kernelMask = this.testObject.getKernelMask();
		expectedKernel = new double[][]{
			{ 0.00000000e+00, 0.00000000e+00, 0.00000000e+00, 0.00000000e+00, 
				0.00000000e+00, 0.00000000e+00,-0.00000000e+00,-0.00000000e+00, 
			 -0.00000000e+00,-0.00000000e+00, 0.00000000e+00, 0.00000000e+00, 
			  0.00000000e+00},
			{ 0.00000000e+00, 0.00000000e+00, 0.00000000e+00, 0.00000000e+00, 
			 	0.00000000e+00,-0.00000000e+00,-0.00000000e+00,-0.00000000e+00, 
			 -0.00000000e+00, 0.00000000e+00, 0.00000000e+00, 0.00000000e+00, 
			 	0.00000000e+00},
			{ 0.00000000e+00, 6.64354729e-03, 1.48746527e-02, 2.19842490e-02, 
			 	1.59042864e-02,-1.03368992e-02,-4.12865021e-02,-0.00000000e+00, 
			 -0.00000000e+00, 0.00000000e+00, 0.00000000e+00, 0.00000000e+00, 
			 	0.00000000e+00},
			{ 0.00000000e+00, 7.85821854e-03, 1.64829136e-02, 2.22529579e-02, 
			 	1.26478786e-02,-1.62705429e-02,-4.49340237e-02,-4.59330968e-02, 
			 -1.82344985e-02, 1.14286743e-02, 2.22362667e-02, 1.70000198e-02, 
			 	8.28502369e-03},
			{ 0.00000000e+00, 9.19672293e-03, 1.80245424e-02, 2.20239211e-02, 
			 	8.71878146e-03,-2.22256977e-02,-4.76403788e-02,-4.25818007e-02, 
			 -1.22742016e-02, 1.49099533e-02, 2.21227587e-02, 1.54070466e-02,
			 	7.02867298e-03},
			{ 0.00000000e+00, 1.06462100e-02, 1.94346662e-02, 2.12257721e-02, 
			 	4.16742710e-03,-2.80193322e-02,-4.93056665e-02,-3.83751003e-02, 
			 -6.45179020e-03, 1.77010969e-02, 2.15638043e-02, 1.37859203e-02, 
			  5.90087349e-03},
			{ 4.90373281e-03, 1.21858587e-02, 2.06405873e-02, 1.97990998e-02, 
			 -9.25893613e-04,-3.34637711e-02,-4.98677851e-02,-3.34637711e-02, 
			 -9.25893613e-04, 1.97990998e-02, 2.06405873e-02, 1.21858587e-02, 
			  4.90373281e-03},
			{ 5.90087349e-03, 1.37859203e-02, 2.15638043e-02, 1.77010969e-02, 
			 -6.45179020e-03,-3.83751003e-02,-4.93056665e-02,-2.80193322e-02, 
			  4.16742710e-03, 2.12257721e-02, 1.94346662e-02, 1.06462100e-02, 
			  0.00000000e+00},
			{ 7.02867298e-03, 1.54070466e-02, 2.21227587e-02, 1.49099533e-02, 
			 -1.22742016e-02,-4.25818007e-02,-4.76403788e-02,-2.22256977e-02,
			  8.71878146e-03, 2.20239211e-02, 1.80245424e-02, 9.19672293e-03, 
			  0.00000000e+00},
			{ 8.28502369e-03, 1.70000198e-02, 2.22362667e-02, 1.14286743e-02, 
			 -1.82344985e-02,-4.59330968e-02,-4.49340237e-02,-1.62705429e-02,
				1.26478786e-02, 2.22529579e-02, 1.64829136e-02, 7.85821854e-03,
				0.00000000e+00},
			{ 0.00000000e+00, 0.00000000e+00, 0.00000000e+00, 0.00000000e+00, 
			 -0.00000000e+00,-0.00000000e+00,-4.12865021e-02,-1.03368992e-02,
				1.59042864e-02, 2.19842490e-02, 1.48746527e-02, 6.64354729e-03, 
				0.00000000e+00},
			{ 0.00000000e+00, 0.00000000e+00, 0.00000000e+00, 0.00000000e+00, 
			 -0.00000000e+00,-0.00000000e+00,-0.00000000e+00,-0.00000000e+00,
				0.00000000e+00, 0.00000000e+00, 0.00000000e+00, 0.00000000e+00, 
				0.00000000e+00},
			{ 0.00000000e+00, 0.00000000e+00, 0.00000000e+00,-0.00000000e+00,
			 -0.00000000e+00,-0.00000000e+00,-0.00000000e+00, 0.00000000e+00,
				0.00000000e+00, 0.00000000e+00, 0.00000000e+00, 0.00000000e+00, 
				0.00000000e+00}};
		
		kernelMean = 0.0;
		kernelCount = 0;
		for (int i=0; i<13; ++i) {
			for (int j=0; j<13; ++j) {
				if (kernelMask[j][i]) {
					kernelMean += expectedKernel[i][j];
					++kernelCount;
				}
			}
		}
		kernelMean = kernelMean/kernelCount;
		for (int i=0; i<13; ++i) {
			for (int j=0; j<13; ++j) {
				if (kernelMask[j][i]) {
					expectedKernel[i][j] = expectedKernel[i][j] - kernelMean;
				}
			}
		}
		assertTrue("Kernel dimension in x should be 13, got " + kernel.getSizeX(),
				kernel.getSizeX() == 13);
		assertTrue("Kernel dimension in y should be 13, got " + kernel.getSizeY(),
				kernel.getSizeY() == 13);
		for (int i=0; i<13; ++i) {
			for (int j=0; j<13; ++j) {
				assertTrue("Kernel entry does not match: " + 
					i + " , " + j + " = " + kernel.getValueDouble(j, i) +
					" , expected " + expectedKernel[i][j],
					Math.abs(kernel.getValueDouble(j, i) - expectedKernel[i][j])
						< accuracy);
			}
		}
	}
}