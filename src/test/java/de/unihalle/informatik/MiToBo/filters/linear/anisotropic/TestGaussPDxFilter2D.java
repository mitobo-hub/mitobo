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
import de.unihalle.informatik.MiToBo.filters.linear.anisotropic.GaussPDxFilter2D;

/**
 * JUnit test class for {@link GaussPDxFilter2D}.
 * 
 * @author Birgit Moeller
 */
public class TestGaussPDxFilter2D {

	/**
	 * Test object.
	 */
	private GaussPDxFilter2D testObject;
	
	/**
	 * Fixture.
	 */
	@Before
	public void initTestClass() {
		boolean thrown = false;
		try {
	    this.testObject = new GaussPDxFilter2D();
	    this.testObject.setInvertMask(true);
    } catch (ALDOperatorException e) {
    	thrown = true;
    }
		assertFalse("[TestGaussPDxFilter2D] could not construct test object!",
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
		 * z = x./(sqrt(2*pi)*8) .* exp(-(x.^2) ./ 8)
		 * z = z .* (abs(x)<=6)
		 * z = z .* (abs(y)<=4)
		 * 
		 * save -ascii /tmp/tab.txt z
		 */

		// test angle of 0
		double[] expectedKernelRow = new double[]{
			-3.32388631e-03, -1.09551878e-02, -2.69954833e-02, -4.85690984e-02, 
			-6.04926811e-02, -4.40081658e-02,  0.00000000e+00,  4.40081658e-02,
			 6.04926811e-02,  4.85690984e-02,  2.69954833e-02,  1.09551878e-02,
			 3.32388631e-03};
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
		MTBImageDouble kernel = this.testObject.getKernel(0.0);
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
						< 1.0e-10);
			}
		}

		// test angle of 180
		expectedKernelRow = new double[]{
			3.32388631e-03,  1.09551878e-02, 2.69954833e-02, 4.85690984e-02,
			6.04926811e-02,  4.40081658e-02, 0.00000000e+00,-4.40081658e-02,
		 -6.04926811e-02, -4.85690984e-02,-2.69954833e-02,-1.09551878e-02,
		 -3.32388631e-03};
		expectedKernel = new double[13][13];
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
		kernel = this.testObject.getKernel(180.0);
		assertTrue("Kernel dimension in x should be 13, got " + kernel.getSizeX(),
				kernel.getSizeX() == 13);
		assertTrue("Kernel dimension in y should be 13, got " + kernel.getSizeY(),
				kernel.getSizeY() == 13);
		for (int i=0; i<13; ++i) {
			for (int j=0; j<13; ++j) {
				assertTrue("Angle = " + 180.0 + " , kernel entry does not match: " + 
					i + " , " + j + " = " + kernel.getValueDouble(j, i) +
					" , expected " + expectedKernel[i][j],
					Math.abs(kernel.getValueDouble(j, i) - expectedKernel[i][j])
						< 1.0e-10);
			}
		}

		// test angle of 90 degrees
		double[] expectedKernelColumn = new double[]{
			-3.32388631e-03, -1.09551878e-02, -2.69954833e-02, -4.85690984e-02, 
			-6.04926811e-02, -4.40081658e-02,  0.00000000e+00,  4.40081658e-02,
			 6.04926811e-02,  4.85690984e-02,  2.69954833e-02,  1.09551878e-02,
			 3.32388631e-03};
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
		kernel = this.testObject.getKernel(90.0);
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
						< 1.0e-10);
			}
		}

		// test angle of 270 degrees
		expectedKernelColumn = new double[]{
				3.32388631e-03,  1.09551878e-02, 2.69954833e-02, 4.85690984e-02,
				6.04926811e-02,  4.40081658e-02, 0.00000000e+00,-4.40081658e-02,
			 -6.04926811e-02, -4.85690984e-02,-2.69954833e-02,-1.09551878e-02,
			 -3.32388631e-03};
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
						< 1.0e-10);
			}
		}

		// test angle of 45 degree
		expectedKernel = new double[][]{
			{-0.00000000e+00, -0.00000000e+00, -0.00000000e+00, -0.00000000e+00, 
			 -5.16674634e-03, -1.15445304e-02, -0.00000000e+00, -0.00000000e+00, 
			 -0.00000000e+00, -0.00000000e+00, -0.00000000e+00, -0.00000000e+00, 
			  0.00000000e+00},
		  {-0.00000000e+00, -0.00000000e+00, -0.00000000e+00, -5.16674634e-03, 
			 -1.15445304e-02, -2.22994292e-02, -3.69564254e-02, -0.00000000e+00, 
			 -0.00000000e+00, -0.00000000e+00, -0.00000000e+00,  0.00000000e+00, 
			  0.00000000e+00},
		  {-0.00000000e+00, -0.00000000e+00, -5.16674634e-03, -1.15445304e-02, 
			 -2.22994292e-02, -3.69564254e-02, -5.18884372e-02, -6.02747877e-02, 
			 -0.00000000e+00, -0.00000000e+00,  0.00000000e+00,  0.00000000e+00, 
			  0.00000000e+00},
 		  {-0.00000000e+00, -5.16674634e-03, -1.15445304e-02, -2.22994292e-02, 
			 -3.69564254e-02, -5.18884372e-02, -6.02747877e-02, -5.49239112e-02, 
			 -3.31254415e-02,  0.00000000e+00,  0.00000000e+00,  0.00000000e+00, 
			  0.00000000e+00},
			{-5.16674634e-03, -1.15445304e-02, -2.22994292e-02, -3.69564254e-02, 
			 -5.18884372e-02, -6.02747877e-02, -5.49239112e-02, -3.31254415e-02, 
			  1.10728726e-17,  3.31254415e-02,  0.00000000e+00,  0.00000000e+00, 
			  0.00000000e+00},
 		  {-1.15445304e-02, -2.22994292e-02, -3.69564254e-02, -5.18884372e-02, 
			 -6.02747877e-02, -5.49239112e-02, -3.31254415e-02,  5.53643631e-18, 
			  3.31254415e-02,  5.49239112e-02,  6.02747877e-02,  0.00000000e+00, 
			  0.00000000e+00},
 		  {-0.00000000e+00, -3.69564254e-02, -5.18884372e-02, -6.02747877e-02, 
			 -5.49239112e-02, -3.31254415e-02,  0.00000000e+00,  3.31254415e-02, 
			  5.49239112e-02,  6.02747877e-02,  5.18884372e-02,  3.69564254e-02, 
			  0.00000000e+00},
 		  {-0.00000000e+00, -0.00000000e+00, -6.02747877e-02, -5.49239112e-02, 
			 -3.31254415e-02, -5.53643631e-18,  3.31254415e-02,  5.49239112e-02, 
			  6.02747877e-02,  5.18884372e-02,  3.69564254e-02,  2.22994292e-02, 
			  1.15445304e-02},
			{-0.00000000e+00, -0.00000000e+00, -0.00000000e+00, -3.31254415e-02, 
			 -1.10728726e-17,  3.31254415e-02,  5.49239112e-02,  6.02747877e-02, 
			  5.18884372e-02,  3.69564254e-02,  2.22994292e-02,  1.15445304e-02, 
			  5.16674634e-03},
			{-0.00000000e+00, -0.00000000e+00, -0.00000000e+00, -0.00000000e+00, 
			  3.31254415e-02,  5.49239112e-02,  6.02747877e-02,  5.18884372e-02, 
			  3.69564254e-02,  2.22994292e-02,  1.15445304e-02,  5.16674634e-03, 
			  0.00000000e+00},
			{-0.00000000e+00, -0.00000000e+00, -0.00000000e+00,  0.00000000e+00, 
			  0.00000000e+00,  6.02747877e-02,  5.18884372e-02,  3.69564254e-02, 
			  2.22994292e-02,  1.15445304e-02,  5.16674634e-03,  0.00000000e+00, 
			  0.00000000e+00},
			{-0.00000000e+00, -0.00000000e+00,  0.00000000e+00,  0.00000000e+00, 
			  0.00000000e+00,  0.00000000e+00,  3.69564254e-02,  2.22994292e-02, 
			  1.15445304e-02,  5.16674634e-03,  0.00000000e+00,  0.00000000e+00, 
			  0.00000000e+00},
			{-0.00000000e+00,  0.00000000e+00,  0.00000000e+00,  0.00000000e+00, 
			  0.00000000e+00,  0.00000000e+00,  0.00000000e+00,  1.15445304e-02, 
			  5.16674634e-03,  0.00000000e+00,  0.00000000e+00,  0.00000000e+00, 
			  0.00000000e+00}};
		
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
		kernel = this.testObject.getKernel(45.0);
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

		// test angle of 225 degree
		expectedKernel = new double[][]{
			{ 0.00000000e+00,  0.00000000e+00,  0.00000000e+00,  0.00000000e+00, 
			  5.16674634e-03,  1.15445304e-02,  0.00000000e+00,  0.00000000e+00, 
			  0.00000000e+00,  0.00000000e+00,  0.00000000e+00,  0.00000000e+00, 
			 -0.00000000e+00},                                                                                                                                                                                  
			{ 0.00000000e+00,  0.00000000e+00,  0.00000000e+00,  5.16674634e-03, 
				1.15445304e-02,  2.22994292e-02,  3.69564254e-02,  0.00000000e+00, 
				0.00000000e+00,  0.00000000e+00,  0.00000000e+00, -0.00000000e+00, 
			 -0.00000000e+00},                                                                                                                                                                                            
			{ 0.00000000e+00,  0.00000000e+00,  5.16674634e-03,  1.15445304e-02, 
				2.22994292e-02,  3.69564254e-02,  5.18884372e-02,  6.02747877e-02, 
				0.00000000e+00,  0.00000000e+00, -0.00000000e+00, -0.00000000e+00, 
			 -0.00000000e+00},                                                                                                                                                                                           
			{	0.00000000e+00,  5.16674634e-03,  1.15445304e-02,  2.22994292e-02, 
				3.69564254e-02,  5.18884372e-02,  6.02747877e-02,  5.49239112e-02, 
				3.31254415e-02, -0.00000000e+00, -0.00000000e+00, -0.00000000e+00, 
			 -0.00000000e+00},                                                                                                                                                                                          
			{ 5.16674634e-03,  1.15445304e-02,  2.22994292e-02,  3.69564254e-02, 
				5.18884372e-02,  6.02747877e-02,  5.49239112e-02,  3.31254415e-02, 
			 -2.21457453e-17, -3.31254415e-02, -0.00000000e+00, -0.00000000e+00, 
			 -0.00000000e+00},                                                                                                                                                                                         
			{ 1.15445304e-02,  2.22994292e-02,  3.69564254e-02,  5.18884372e-02, 
				6.02747877e-02,  5.49239112e-02,  3.31254415e-02, -1.10728726e-17, 
			 -3.31254415e-02, -5.49239112e-02, -6.02747877e-02, -0.00000000e+00, 
			 -0.00000000e+00},                                                                                                                                                                                       
			{ 0.00000000e+00,  3.69564254e-02,  5.18884372e-02,  6.02747877e-02, 
				5.49239112e-02,  3.31254415e-02, -0.00000000e+00, -3.31254415e-02, 
			 -5.49239112e-02, -6.02747877e-02, -5.18884372e-02, -3.69564254e-02, 
			 -0.00000000e+00},                                                                                                                                                                                      
			{	0.00000000e+00,  0.00000000e+00,  6.02747877e-02,  5.49239112e-02, 
				3.31254415e-02,  1.10728726e-17, -3.31254415e-02, -5.49239112e-02, 
			 -6.02747877e-02, -5.18884372e-02, -3.69564254e-02, -2.22994292e-02, 
			 -1.15445304e-02},                                                                                                                                                                                      
			{	0.00000000e+00,  0.00000000e+00,  0.00000000e+00,  3.31254415e-02, 
				2.21457453e-17, -3.31254415e-02, -5.49239112e-02, -6.02747877e-02, 
			 -5.18884372e-02, -3.69564254e-02, -2.22994292e-02, -1.15445304e-02, 
			 -5.16674634e-03},                                                                                                                                                                                      
			{	0.00000000e+00,  0.00000000e+00,  0.00000000e+00,  0.00000000e+00, 
			 -3.31254415e-02, -5.49239112e-02, -6.02747877e-02, -5.18884372e-02, 
			 -3.69564254e-02, -2.22994292e-02, -1.15445304e-02, -5.16674634e-03, 
			 -0.00000000e+00},                                                                                                                                                                                     
			{ 0.00000000e+00,  0.00000000e+00,  0.00000000e+00, -0.00000000e+00, 
			 -0.00000000e+00, -6.02747877e-02, -5.18884372e-02, -3.69564254e-02, 
			 -2.22994292e-02, -1.15445304e-02, -5.16674634e-03, -0.00000000e+00, 
			 -0.00000000e+00},                                                                                                                                                                                    
			{	0.00000000e+00,  0.00000000e+00, -0.00000000e+00, -0.00000000e+00, 
			 -0.00000000e+00, -0.00000000e+00, -3.69564254e-02, -2.22994292e-02, 
			 -1.15445304e-02, -5.16674634e-03, -0.00000000e+00, -0.00000000e+00, 
			 -0.00000000e+00},                                                                                                                                                                                   
			{ 0.00000000e+00, -0.00000000e+00, -0.00000000e+00, -0.00000000e+00, 
			 -0.00000000e+00, -0.00000000e+00, -0.00000000e+00, -1.15445304e-02, 
			 -5.16674634e-03, -0.00000000e+00, -0.00000000e+00, -0.00000000e+00, 
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
						< 1.0e-6);
			}
		}

		// test angle of 10 degree
		expectedKernel = new double[][]{
			{-0.00000000e+00, -0.00000000e+00, -0.00000000e+00, -0.00000000e+00, 
			 -0.00000000e+00, -0.00000000e+00, -0.00000000e+00, -0.00000000e+00, 
			  0.00000000e+00,  0.00000000e+00,  0.00000000e+00,  0.00000000e+00, 
			  0.00000000e+00},                                                                                                                                                                                     
			{-0.00000000e+00, -0.00000000e+00, -0.00000000e+00, -0.00000000e+00, 
			 -0.00000000e+00, -0.00000000e+00, -0.00000000e+00,  0.00000000e+00, 
			  0.00000000e+00,  0.00000000e+00,  0.00000000e+00,  0.00000000e+00, 
			  0.00000000e+00},                                                                                                                                                                                      
			{-0.00000000e+00, -5.41588267e-03, -1.57795895e-02, -3.44468668e-02, 
			 -5.47091754e-02, -5.88660090e-02, -3.26106262e-02,  0.00000000e+00, 
			  0.00000000e+00,  0.00000000e+00,  0.00000000e+00,  0.00000000e+00, 
			  0.00000000e+00},                                                                                                                                                                                      
			{-0.00000000e+00, -6.67313326e-03, -1.85027171e-02, -3.82943688e-02, 
			 -5.71981549e-02, -5.65574306e-02, -2.51118689e-02,  2.25179668e-02, 
			  5.55726304e-02,  5.78856593e-02,  3.95643830e-02,  1.94583811e-02, 
			  7.13380888e-03},                                                                                                                                                                                      
			{-0.00000000e+00, -8.15219433e-03, -2.15002063e-02, -4.21464130e-02, 
			 -5.90627440e-02, -5.32140396e-02, -1.70597442e-02,  3.02165408e-02, 
			  5.82207590e-02,  5.55889911e-02,  3.57059321e-02,  1.66438587e-02, 
			  5.80603012e-03},                                                                                                                                                                                      
			{-0.00000000e+00, -9.87356101e-03, -2.47549784e-02, -4.59102315e-02, 
			 -6.01900800e-02, -4.88478669e-02, -8.62687214e-03,  3.72569029e-02, 
			  5.98434300e-02,  5.27474876e-02,  3.19070215e-02,  1.41091808e-02, 
			  4.68532385e-03},                                                                                                                                                                                      
			{-3.74910613e-03, -1.18548203e-02, -2.82379866e-02, -4.94816336e-02, 
			 -6.04786493e-02, -4.35032472e-02,  0.00000000e+00,  4.35032472e-02, 
			  6.04786493e-02,  4.94816336e-02,  2.82379866e-02,  1.18548203e-02, 
			  3.74910613e-03},                                                                                                                                                                                       
			{-4.68532385e-03, -1.41091808e-02, -3.19070215e-02, -5.27474876e-02, 
			 -5.98434300e-02, -3.72569029e-02,  8.62687214e-03,  4.88478669e-02, 
			  6.01900800e-02,  4.59102315e-02,  2.47549784e-02,  9.87356101e-03,
			  0.00000000e+00},                                                                                                                                                                                       
			{-5.80603012e-03, -1.66438587e-02, -3.57059321e-02, -5.55889911e-02, 
			 -5.82207590e-02, -3.02165408e-02,  1.70597442e-02,  5.32140396e-02, 
			  5.90627440e-02,  4.21464130e-02,  2.15002063e-02,  8.15219433e-03, 
			  0.00000000e+00},                                                                                                                                                                                       
			{-7.13380888e-03, -1.94583811e-02, -3.95643830e-02, -5.78856593e-02, 
			 -5.55726304e-02, -2.25179668e-02,  2.51118689e-02,  5.65574306e-02, 
			  5.71981549e-02,  3.82943688e-02,  1.85027171e-02,  6.67313326e-03, 
			  0.00000000e+00},                                                                                                                                                                                       
			{-0.00000000e+00, -0.00000000e+00, -0.00000000e+00, -0.00000000e+00, 
			 -0.00000000e+00, -0.00000000e+00,  3.26106262e-02,  5.88660090e-02, 
			  5.47091754e-02,  3.44468668e-02,  1.57795895e-02,  5.41588267e-03, 
			  0.00000000e+00},                                                                                                                                                                                       
			{-0.00000000e+00, -0.00000000e+00, -0.00000000e+00, -0.00000000e+00, 
			 -0.00000000e+00, -0.00000000e+00,  0.00000000e+00,  0.00000000e+00, 
			  0.00000000e+00,  0.00000000e+00,  0.00000000e+00,  0.00000000e+00, 
			  0.00000000e+00},                                                                                                                                                                                       
			{-0.00000000e+00, -0.00000000e+00, -0.00000000e+00, -0.00000000e+00, 
			 -0.00000000e+00,  0.00000000e+00,  0.00000000e+00,  0.00000000e+00, 
			  0.00000000e+00,  0.00000000e+00,  0.00000000e+00,  0.00000000e+00, 
			  0.00000000e+00}};
		
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
		kernel = this.testObject.getKernel(10.0);
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