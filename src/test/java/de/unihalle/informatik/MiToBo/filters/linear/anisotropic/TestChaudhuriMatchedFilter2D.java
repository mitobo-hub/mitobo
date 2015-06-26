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
import de.unihalle.informatik.MiToBo.filters.linear.anisotropic.ChaudhuriMatchedFilter2D;

/**
 * JUnit test class for {@link ChaudhuriMatchedFilter2D}.
 * 
 * @author Birgit Moeller
 */
public class TestChaudhuriMatchedFilter2D {

	/**
	 * Test object.
	 */
	private ChaudhuriMatchedFilter2D testObject;
	
	/**
	 * Fixture.
	 */
	@Before
	public void initTestClass() {
		boolean thrown = false;
		try {
	    this.testObject = new ChaudhuriMatchedFilter2D();
    } catch (ALDOperatorException e) {
    	thrown = true;
    }
		assertFalse("[TestMatchedFilter2D] could not construct test object!",
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
		 * z = -exp(-((cos(angle/180*pi)*m + sin(angle/180*pi)*n).^2) / 8)
		 * x =  cos(angle/180*pi)*m + sin(angle/180*pi)*n
		 * y = -sin(angle/180*pi)*m + cos(angle/180*pi)*n
		 * z = z .* (abs(x)<=6)
		 * z = z .* (abs(y)<=4)
		 * 
		 * save -ascii /tmp/tab.txt z
		 */

		// test angles of 0 and 180, kernels are the same for both
		double[] expectedKernelRow = new double[]{
			-0.0111089965382423, -0.0439369336234074, -0.1353352832366127,
			-0.3246524673583497, -0.6065306597126334, -0.8824969025845955,  
			-1.0000000000000000,
			-0.8824969025845955, -0.6065306597126334, -0.3246524673583497,
			-0.1353352832366127, -0.0439369336234074, -0.0111089965382423};
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
				if (Math.abs(expectedKernel[i][j]) > 0) {
					kernelMean += expectedKernel[i][j];
					++kernelCount;
				}
			}
		}
		kernelMean = kernelMean/kernelCount;
		for (int i=0; i<13; ++i) {
			for (int j=0; j<13; ++j) {
				if (Math.abs(expectedKernel[i][j]) > 0) {
					expectedKernel[i][j] = expectedKernel[i][j] - kernelMean;
				}
			}
		}

		for (int angle = 0; angle < 200; angle += 180) {
			MTBImageDouble kernel = this.testObject.getKernel(angle);
			assertTrue("Kernel dimension in x should be 13, got " + kernel.getSizeX(),
					kernel.getSizeX() == 13);
			assertTrue("Kernel dimension in y should be 13, got " + kernel.getSizeY(),
					kernel.getSizeY() == 13);
			for (int i=0; i<13; ++i) {
				for (int j=0; j<13; ++j) {
					assertTrue("Angle = " + angle + " , kernel entry does not match: " + 
						i + " , " + j + " = " + kernel.getValueDouble(j, i),
						 Math.abs(kernel.getValueDouble(j, i) - expectedKernel[i][j])
							< 1.0e-15);
				}
			}
		}

		// test angles of 90 and 270, kernels are the same for both
		double[] expectedKernelColumn = new double[]{
			-0.0111089965382423, -0.0439369336234074, -0.1353352832366127,
			-0.3246524673583497, -0.6065306597126334, -0.8824969025845955,  
			-1.0000000000000000,
			-0.8824969025845955, -0.6065306597126334, -0.3246524673583497,
			-0.1353352832366127, -0.0439369336234074, -0.0111089965382423};
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
				if (Math.abs(expectedKernel[i][j]) > 0) {
					kernelMean += expectedKernel[i][j];
					++kernelCount;
				}
			}
		}
		kernelMean = kernelMean/kernelCount;
		for (int i=0; i<13; ++i) {
			for (int j=0; j<13; ++j) {
				if (Math.abs(expectedKernel[i][j]) > 0) {
					expectedKernel[i][j] = expectedKernel[i][j] - kernelMean;
				}
			}
		}

		for (int angle = 90; angle < 280; angle += 180) {
			MTBImageDouble kernel = this.testObject.getKernel(angle);
			assertTrue("Kernel dimension in x should be 13, got " + kernel.getSizeX(),
					kernel.getSizeX() == 13);
			assertTrue("Kernel dimension in y should be 13, got " + kernel.getSizeY(),
					kernel.getSizeY() == 13);
			for (int i=0; i<13; ++i) {
				for (int j=0; j<13; ++j) {
					assertTrue("Kernel entry does not match: " + 
						i + " , " + j + " = " + kernel.getValueDouble(j, i),
							Math.abs(kernel.getValueDouble(j, i) - expectedKernel[i][j])
							< 1.0e-15);
				}
			}
		}
		
		// test angles of 45 and 225, kernels are the same for both
		expectedKernel = new double[][]{
			{-0.00000000e+00, -0.00000000e+00, -0.00000000e+00, -0.00000000e+00, 
			 -1.83156389e-02, -4.67706224e-02, -0.00000000e+00, -0.00000000e+00, 
			 -0.00000000e+00, -0.00000000e+00, -0.00000000e+00, -0.00000000e+00, 
			 -0.00000000e+00},
			{-0.00000000e+00, -0.00000000e+00, -0.00000000e+00, -1.83156389e-02, 
			 -4.67706224e-02, -1.05399225e-01, -2.09611387e-01, -0.00000000e+00, 
			 -0.00000000e+00, -0.00000000e+00, -0.00000000e+00, -0.00000000e+00, 
			 -0.00000000e+00},
			{-0.00000000e+00, -0.00000000e+00, -1.83156389e-02, -4.67706224e-02, 
			 -1.05399225e-01, -2.09611387e-01, -3.67879441e-01, -5.69782825e-01, 
			 -0.00000000e+00, -0.00000000e+00, -0.00000000e+00, -0.00000000e+00, 
			 -0.00000000e+00},
			{-0.00000000e+00, -1.83156389e-02, -4.67706224e-02, -1.05399225e-01, 
			 -2.09611387e-01, -3.67879441e-01, -5.69782825e-01, -7.78800783e-01, 
			 -9.39413063e-01, -0.00000000e+00, -0.00000000e+00, -0.00000000e+00, 
			 -0.00000000e+00},
			{-1.83156389e-02, -4.67706224e-02, -1.05399225e-01, -2.09611387e-01, 
			 -3.67879441e-01, -5.69782825e-01, -7.78800783e-01, -9.39413063e-01, 
			 -1.00000000e+00, -9.39413063e-01, -0.00000000e+00, -0.00000000e+00, 
			 -0.00000000e+00},
			{-4.67706224e-02, -1.05399225e-01, -2.09611387e-01, -3.67879441e-01, 
			 -5.69782825e-01, -7.78800783e-01, -9.39413063e-01, -1.00000000e+00, 
			 -9.39413063e-01, -7.78800783e-01, -5.69782825e-01, -0.00000000e+00, 
			 -0.00000000e+00},
			{-0.00000000e+00, -2.09611387e-01, -3.67879441e-01, -5.69782825e-01, 
			 -7.78800783e-01, -9.39413063e-01, -1.00000000e+00, -9.39413063e-01, 
			 -7.78800783e-01, -5.69782825e-01, -3.67879441e-01, -2.09611387e-01, 
			 -0.00000000e+00},
			{-0.00000000e+00, -0.00000000e+00, -5.69782825e-01, -7.78800783e-01, 
			 -9.39413063e-01, -1.00000000e+00, -9.39413063e-01, -7.78800783e-01, 
			 -5.69782825e-01, -3.67879441e-01, -2.09611387e-01, -1.05399225e-01, 
			 -4.67706224e-02},
			{-0.00000000e+00, -0.00000000e+00, -0.00000000e+00, -9.39413063e-01, 
			 -1.00000000e+00, -9.39413063e-01, -7.78800783e-01, -5.69782825e-01, 
			 -3.67879441e-01, -2.09611387e-01, -1.05399225e-01, -4.67706224e-02, 
			 -1.83156389e-02},
			{-0.00000000e+00, -0.00000000e+00, -0.00000000e+00, -0.00000000e+00, 
			 -9.39413063e-01, -7.78800783e-01, -5.69782825e-01, -3.67879441e-01, 
			 -2.09611387e-01, -1.05399225e-01, -4.67706224e-02, -1.83156389e-02, 
			 -0.00000000e+00,},
			{-0.00000000e+00, -0.00000000e+00, -0.00000000e+00, -0.00000000e+00, 
			 -0.00000000e+00, -5.69782825e-01, -3.67879441e-01, -2.09611387e-01, 
			 -1.05399225e-01, -4.67706224e-02, -1.83156389e-02, -0.00000000e+00, 
			 -0.00000000e+00,},
			{-0.00000000e+00, -0.00000000e+00, -0.00000000e+00, -0.00000000e+00, 
			 -0.00000000e+00, -0.00000000e+00, -2.09611387e-01, -1.05399225e-01, 
			 -4.67706224e-02, -1.83156389e-02, -0.00000000e+00, -0.00000000e+00, 
			 -0.00000000e+00,},
			{-0.00000000e+00, -0.00000000e+00, -0.00000000e+00, -0.00000000e+00, 
			 -0.00000000e+00, -0.00000000e+00, -0.00000000e+00, -4.67706224e-02, 
			 -1.83156389e-02, -0.00000000e+00, -0.00000000e+00, -0.00000000e+00, 
			 -0.00000000e+00,}};
		kernelMean = 0.0;
		kernelCount = 0;
		for (int i=0; i<13; ++i) {
			for (int j=0; j<13; ++j) {
				if (Math.abs(expectedKernel[i][j]) > 0) {
					kernelMean += expectedKernel[i][j];
					++kernelCount;
				}
			}
		}
		kernelMean = kernelMean/kernelCount;
		for (int i=0; i<13; ++i) {
			for (int j=0; j<13; ++j) {
				if (Math.abs(expectedKernel[i][j]) > 0) {
					expectedKernel[i][j] = expectedKernel[i][j] - kernelMean;
				}
			}
		}

		for (int angle = 45; angle < 230; angle += 180) {
			MTBImageDouble kernel = this.testObject.getKernel(45);
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
						< 1.0e-6);
				}
			}
		}

		// test angle of 10
		expectedKernel = new double[][]{
			{-0.00000000e+00, -0.00000000e+00, -0.00000000e+00, -0.00000000e+00, 
			 -0.00000000e+00, -0.00000000e+00, -0.00000000e+00, -0.00000000e+00, 
			 -0.00000000e+00, -0.00000000e+00, -0.00000000e+00, -0.00000000e+00, 
			 -0.00000000e+00},
			{-0.00000000e+00, -0.00000000e+00, -0.00000000e+00, -0.00000000e+00, 
			 -0.00000000e+00, -0.00000000e+00, -0.00000000e+00, -0.00000000e+00, 
			 -0.00000000e+00, -0.00000000e+00, -0.00000000e+00, -0.00000000e+00, 
			 -0.00000000e+00},
			{-0.00000000e+00, -1.93294110e-02, -6.82866982e-02, -1.89301425e-01, 
			 -4.11786331e-01, -7.02894662e-01, -9.41475099e-01, -0.00000000e+00, 
			 -0.00000000e+00, -0.00000000e+00, -0.00000000e+00, -0.00000000e+00, 
			 -0.00000000e+00},
			{-0.00000000e+00, -2.45761114e-02, -8.31885354e-02, -2.20960205e-01, 
			 -4.60537422e-01, -7.53209972e-01, -9.66646029e-01, -9.73462344e-01, 
			 -7.69256397e-01, -4.77005461e-01, -2.32100385e-01, -8.86193674e-02, 
			 -2.65510502e-02},
			{-0.00000000e+00, -3.10122887e-02, -1.00581231e-01, -2.55976650e-01, 
			 -5.11191976e-01, -8.01065414e-01, -9.85036242e-01, -9.50466276e-01, 
			 -7.19650265e-01, -4.27569327e-01, -1.99338813e-01, -7.29251734e-02, 
			 -2.09345364e-02},
			{-0.00000000e+00, -3.88401210e-02, -1.20697012e-01, -2.94315233e-01,
			 -5.63156676e-01, -8.45563071e-01, -9.96237883e-01, -9.21043989e-01, 
			 -6.68186916e-01, -3.80378391e-01, -1.69915885e-01, -5.95596861e-02, 
			 -1.63821574e-02},
 		  {-1.27234517e-02, -4.82784629e-02, -1.43748128e-01, -3.35854550e-01, 
			 -6.15744512e-01, -8.85829496e-01, -1.00000000e+00, -8.85829496e-01, 
			 -6.15744512e-01, -3.35854550e-01, -1.43748128e-01, -4.82784629e-02, 
			 -1.27234517e-02},
 		  {-1.63821574e-02, -5.95596861e-02, -1.69915885e-01, -3.80378391e-01, 
			 -6.68186916e-01, -9.21043989e-01, -9.96237883e-01, -8.45563071e-01, 
			 -5.63156676e-01, -2.94315233e-01, -1.20697012e-01, -3.88401210e-02, 
			 -0.00000000e+00},
  		{-2.09345364e-02, -7.29251734e-02, -1.99338813e-01, -4.27569327e-01, 
			 -7.19650265e-01, -9.50466276e-01, -9.85036242e-01, -8.01065414e-01, 
			 -5.11191976e-01, -2.55976650e-01, -1.00581231e-01, -3.10122887e-02, 
			 -0.00000000e+00},
			{-2.65510502e-02, -8.86193674e-02, -2.32100385e-01, -4.77005461e-01, 
			 -7.69256397e-01, -9.73462344e-01, -9.66646029e-01, -7.53209972e-01, 
			 -4.60537422e-01, -2.20960205e-01, -8.31885354e-02, -2.45761114e-02, 
			 -0.00000000e+00},
			{-0.00000000e+00, -0.00000000e+00, -0.00000000e+00, -0.00000000e+00, 
			 -0.00000000e+00, -0.00000000e+00, -9.41475099e-01, -7.02894662e-01, 
			 -4.11786331e-01, -1.89301425e-01, -6.82866982e-02, -1.93294110e-02, 
			 -0.00000000e+00},
			{-0.00000000e+00, -0.00000000e+00, -0.00000000e+00, -0.00000000e+00, 
			 -0.00000000e+00, -0.00000000e+00, -0.00000000e+00, -0.00000000e+00, 
			 -0.00000000e+00, -0.00000000e+00, -0.00000000e+00, -0.00000000e+00, 
			 -0.00000000e+00},
			{-0.00000000e+00, -0.00000000e+00, -0.00000000e+00, -0.00000000e+00, 
			 -0.00000000e+00, -0.00000000e+00, -0.00000000e+00, -0.00000000e+00, 
			 -0.00000000e+00, -0.00000000e+00, -0.00000000e+00, -0.00000000e+00, 
			 -0.00000000e+00}};

		kernelMean = 0.0;
		kernelCount = 0;
		for (int i=0; i<13; ++i) {
			for (int j=0; j<13; ++j) {
				if (Math.abs(expectedKernel[i][j]) > 0) {
					kernelMean += expectedKernel[i][j];
					++kernelCount;
				}
			}
		}
		kernelMean = kernelMean/kernelCount;
		for (int i=0; i<13; ++i) {
			for (int j=0; j<13; ++j) {
				if (Math.abs(expectedKernel[i][j]) > 0) {
					expectedKernel[i][j] = expectedKernel[i][j] - kernelMean;
				}
			}
		}

		MTBImageDouble kernel = this.testObject.getKernel(10);
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
						< 1.0e-6);
			}
		}
	}
}