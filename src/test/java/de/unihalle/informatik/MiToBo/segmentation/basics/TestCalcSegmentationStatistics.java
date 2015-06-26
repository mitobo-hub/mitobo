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

package de.unihalle.informatik.MiToBo.segmentation.basics;

import static org.junit.Assert.*;

import java.util.Vector;

import org.junit.Before;
import org.junit.Test;

import de.unihalle.informatik.Alida.exceptions.ALDException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.segmentation.basics.MTBSegmentation2D;
import de.unihalle.informatik.MiToBo.segmentation.basics.CalcSegmentationStatistics.CalcTargets;

/**
 * JUnit test class for {@link CalcSegmentationStatistics}.
 * 
 * @author moeller
 */
public class TestCalcSegmentationStatistics {

	private static final String IDS = "[CalcSegmentationStatistics]";
	
	private static final double accuracy = 10e-10;
	
	/**
	 * Dummy 2D image for tests.
	 */
	private MTBImage dummyImage2D;
	/**
	 * Dummy multi-channel 2D image for tests.
	 */
	private MTBImage dummyImage2DMultiChannel;
	
	/**
	 * Dummy 3D image for tests.
	 */
	private MTBImage dummyImage3D;
	/**
	 * Dummy 3D image for tests.
	 */
	private MTBImage dummyImage3DMultiChannel;

	/**
	 * Object to test.
	 */
	private CalcSegmentationStatistics testObject;
	
	/**
	 * Statistical numbers to calculate.
	 */
	private Vector<CalcSegmentationStatistics.CalcTargets> targets;
	
	/**
	 * Fixture.
	 */
	@Before
	public void initTestClass() {
		
		this.targets = new Vector<CalcSegmentationStatistics.CalcTargets>();
		this.targets.add(CalcTargets.classMean);
		this.targets.add(CalcTargets.classSize);
		this.targets.add(CalcTargets.classVar);
		
		// instantiate dummy images
		this.dummyImage2D = 
			MTBImage.createMTBImage(10, 10, 1, 1, 1, MTBImageType.MTB_BYTE);
		this.dummyImage2D.fillBlack();
		int counter=0;
		for (int i=0;i<10;++i) {
			for (int j=0;j<10;++j) {
				this.dummyImage2D.putValueInt(i, j, counter);
				++counter;
			}
		}

		this.dummyImage3D = 
			MTBImage.createMTBImage(10, 10, 10, 1, 1, MTBImageType.MTB_BYTE);
		this.dummyImage3D.fillBlack();
		counter=0;
		for (int z=0;z<10;++z) {
			for (int y=0;y<10;++y) {
				for (int x=0;x<10;++x) {
					this.dummyImage3D.putValueInt(x,y,z,counter);
					++counter;
				}
			}
		}
		
		this.dummyImage2DMultiChannel = 
				MTBImage.createMTBImage(10, 10, 1, 1, 3, MTBImageType.MTB_BYTE);
		this.dummyImage2DMultiChannel.fillBlack();
		counter=0;
		for (int i=0;i<10;++i) {
			for (int j=0;j<10;++j) {
				this.dummyImage2DMultiChannel.putValueInt(i, j, 0, 0, 0, counter);
				++counter;
			}
		}
		counter = 99;
		for (int i=0;i<10;++i) {
			for (int j=0;j<10;++j) {
				this.dummyImage2DMultiChannel.putValueInt(i, j, 0, 0, 1, counter);
				--counter;
			}
		}
		counter=0;
		for (int i=0;i<10;++i) {
			for (int j=0;j<10;++j) {
				this.dummyImage2DMultiChannel.putValueInt(j, i, 0, 0, 2, counter);
				++counter;
			}
		}

		this.dummyImage3DMultiChannel = 
			MTBImage.createMTBImage(10, 10, 10, 1, 3, MTBImageType.MTB_BYTE);
		this.dummyImage3DMultiChannel.fillBlack();
		counter=0;
		for (int z=0;z<10;++z) {
			for (int y=0;y<10;++y) {
				for (int x=0;x<10;++x) {
					this.dummyImage3DMultiChannel.putValueInt(x, y, z, 0, 0, counter);
					++counter;
				}
			}
		}
		counter = 999;
		for (int z=0;z<10;++z) {
			for (int y=0;y<10;++y) {
				for (int x=0;x<10;++x) {
					this.dummyImage3DMultiChannel.putValueInt(x, y, z, 0, 1, counter);
					--counter;
				}
			}
		}
		counter=0;
		for (int z=0;z<10;++z) {
			for (int y=0;y<10;++y) {
				for (int x=0;x<10;++x) {
					this.dummyImage3DMultiChannel.putValueInt(z, y, x, 0, 2, counter);
					++counter;
				}
			}
		}

		boolean thrown = false;
		try {
	    this.testObject = new CalcSegmentationStatistics();
    } catch (ALDOperatorException e) {
    	thrown = true;
    }
		assertFalse(IDS + " init of initializer failed!", thrown);
	}
	
	/**
	 * Test calculations for 2D single-channel image.
	 */
	@Test
	public void test2DImageSingleChannel() {
		
		/* 2D one channel image*/
		byte[][] bitfield = new byte[10][10];
		for (int j=0;j<10;++j)
			for (int i=0;i<10;++i)
				// every second pixel gets same class
				bitfield[j][i] = (byte)(((j*10+i)%2 == 0) ? 0 : 1);
		MTBSegmentation2D seg2D = new MTBSegmentation2D(10, 10, 2, bitfield);
		// configure test object
		this.testObject.setInputImage(this.dummyImage2D);
		this.testObject.setSegmentation(seg2D);
		this.testObject.setTargets(this.targets);
		boolean exceptionThrown = false;
		try {
	    this.testObject.runOp();
    } catch (ALDException e) {
    	exceptionThrown = true;
    }
		assertTrue(IDS + " unexpected exception on running operator!",
			!exceptionThrown);
		
		int[] regionSizes = this.testObject.getRegionSizes();
		double[] regionMeans = this.testObject.getRegionMeans();
		double[] regionVars = this.testObject.getRegionVars();
		
		assertTrue(IDS + " expecting two regions, got a different number...",
			regionSizes.length == 2);
		assertTrue(IDS + " expecting two mean values, got a different number...",
			regionMeans.length == 2);
		assertTrue(IDS + " expecting two variances, got a different number...",
			regionVars.length == 2);
		// first class is given by 0th, 2nd, 4th, ... pixels
		int count_1 = 0, count_2 = 0;
		double sum_1 = 0, sum_2 = 0;
		int index=0;
		for (int y=0;y<10;++y) {
			for (int x=0;x<10;++x) {
				if (index%2 == 0) {
					sum_1 += this.dummyImage2D.getValueInt(x, y);
					++count_1;
				}
				else {
					sum_2 += this.dummyImage2D.getValueInt(x, y);
					++count_2;		
				}
				++index;
			}
		}
		double mean_1 = sum_1 / count_1;
		double mean_2 = sum_2 / count_2;
		assertTrue(IDS + " expecting size of first class to be 50, got "
			+ regionSizes[0], 50 == regionSizes[0]);
		assertTrue(IDS + " expecting size of second class to be 50, got "
			+ regionSizes[1], 50 == regionSizes[1]);
		assertTrue(IDS + " expecting mean of first class to be " + mean_1 + ", "
			+ "got " + regionMeans[0], Math.abs(mean_1 - regionMeans[0]) < 10e-10);
		assertTrue(IDS + " expecting mean of second class to be " + mean_2 + ", "
			+ "got " + regionMeans[1], Math.abs(mean_2 - regionMeans[1]) < 10e-10);
		sum_1 = 0;
		sum_2 = 0;
		index = 0;
		count_1 = 0;
		count_2 = 0;
		for (int y=0;y<10;++y) {
			for (int x=0;x<10;++x) {
				if (index%2 == 0) {
					sum_1 += (this.dummyImage2D.getValueInt(x, y) - mean_1) 
							* (this.dummyImage2D.getValueInt(x, y) - mean_1);
					++count_1;
				}
				else {
					sum_2 += (this.dummyImage2D.getValueInt(x, y) - mean_2)
							* (this.dummyImage2D.getValueInt(x, y) - mean_2);
					++count_2;		
				}
				++index;
			}
		}
		double var_1 = sum_1 / count_1;
		double var_2 = sum_2 / count_2;
		assertTrue(IDS + " expecting variance of first class to be " + var_1 + ", "
			+ "got " + regionVars[0], Math.abs(var_1 - regionVars[0]) < 10e-10);
		assertTrue(IDS + " expecting variance of second class to be " + var_2 + ", "
			+ "got " + regionVars[1], Math.abs(var_2 - regionVars[1]) < 10e-10);
			
		// set upper half of the image invisible
		boolean[][] mask = new boolean[10][10];
		for (int y=0;y<10;++y)
			for (int x=0;x<10;++x)
				if (y<5)
					mask[y][x] = false;
				else	
					mask[y][x] = true;
		seg2D.setVisibilityMask(mask);
		exceptionThrown = false;
		try {
	    this.testObject.runOp();
    } catch (ALDException e) {
    	exceptionThrown = true;
    }
		assertTrue(IDS + " unexpected exception on running operator!",
			!exceptionThrown);
		
		regionSizes = this.testObject.getRegionSizes();
		regionMeans = this.testObject.getRegionMeans();
		regionVars = this.testObject.getRegionVars();
		
		assertTrue(IDS + " expecting two regions, got a different number...",
			regionSizes.length == 2);
		assertTrue(IDS + " expecting two mean values, got a different number...",
			regionMeans.length == 2);
		assertTrue(IDS + " expecting two variances, got a different number...",
			regionVars.length == 2);
		// first class is given by 0th, 2nd, 4th, ... pixels
		count_1 = 0;
		count_2 = 0;
		sum_1 = 0;
		sum_2 = 0;
		index=0;
		for (int y=0;y<10;++y) {
			for (int x=0;x<10;++x) {
				if (y<5)
					continue;
				if (index%2 == 0) {
					sum_1 += this.dummyImage2D.getValueInt(x, y);
					++count_1;
				}
				else {
					sum_2 += this.dummyImage2D.getValueInt(x, y);
					++count_2;		
				}
				++index;
			}
		}
		mean_1 = sum_1 / count_1;
		mean_2 = sum_2 / count_2;
		assertTrue(IDS + " expecting size of first class to be 25, got "
			+ regionSizes[0], 25 == regionSizes[0]);
		assertTrue(IDS + " expecting size of second class to be 25, got "
			+ regionSizes[1], 25 == regionSizes[1]);
		assertTrue(IDS + " expecting mean of first class to be " + mean_1 + ", "
			+ "got " + regionMeans[0], Math.abs(mean_1 - regionMeans[0]) < 10e-10);
		assertTrue(IDS + " expecting mean of second class to be " + mean_2 + ", "
			+ "got " + regionMeans[1], Math.abs(mean_2 - regionMeans[1]) < 10e-10);
		sum_1 = 0;
		sum_2 = 0;
		index = 0;
		count_1 = 0;
		count_2 = 0;
		for (int y=0;y<10;++y) {
			for (int x=0;x<10;++x) {
				if (y<5)
					continue;
				if (index%2 == 0) {
					sum_1 += (this.dummyImage2D.getValueInt(x, y) - mean_1) 
							* (this.dummyImage2D.getValueInt(x, y) - mean_1);
					++count_1;
				}
				else {
					sum_2 += (this.dummyImage2D.getValueInt(x, y) - mean_2)
							* (this.dummyImage2D.getValueInt(x, y) - mean_2);
					++count_2;		
				}
				++index;
			}
		}
		var_1 = sum_1 / count_1;
		var_2 = sum_2 / count_2;
		assertTrue(IDS + " expecting variance of first class to be " + var_1 + ", "
			+ "got " + regionVars[0], Math.abs(var_1 - regionVars[0]) < 10e-10);
		assertTrue(IDS + " expecting variance of second class to be " + var_2 + ", "
			+ "got " + regionVars[1], Math.abs(var_2 - regionVars[1]) < 10e-10);
	}
	
	/**
	 * Test calculations for 3D single-channel image.
	 */
	@Test
	public void test3DImageSingleChannel() {
		
		/* 3D one channel image*/
		int[][][] bitfield = new int[10][10][10];
		boolean[][][] visibilityMap = new boolean[10][10][10];
		double[][][] weightMap = new double[10][10][10];
		int index = 0;
		for (int z=0;z<10;++z) {
			for (int y=0;y<10;++y) {
				for (int x=0;x<10;++x) {
					// every second pixel gets same class
					if (index%2 == 0)
						bitfield[z][y][x] = 0;
					else
						bitfield[z][y][x] = 1;
					visibilityMap[z][y][x] = true;
					weightMap[z][y][x] = 1.0;
					++index;
				}
			}
		}
		MTBSegmentation3D seg3D = new MTBSegmentation3D(10, 10, 10, 2, bitfield,
			visibilityMap, weightMap);
		// configure test object
		this.testObject.setInputImage(this.dummyImage3D);
		this.testObject.setSegmentation(seg3D);
		this.testObject.setTargets(this.targets);
		boolean exceptionThrown = false;
		try {
	    this.testObject.runOp();
    } catch (ALDException e) {
    	exceptionThrown = true;
    }
		assertTrue(IDS + " unexpected exception on running operator!",
			!exceptionThrown);
		
		int[] regionSizes = this.testObject.getRegionSizes();
		double[] regionMeans = this.testObject.getRegionMeans();
		double[] regionVars = this.testObject.getRegionVars();
		
		assertTrue(IDS + " expecting two regions, got a different number...",
			regionSizes.length == 2);
		assertTrue(IDS + " expecting two mean values, got a different number...",
			regionMeans.length == 2);
		assertTrue(IDS + " expecting two variances, got a different number...",
			regionVars.length == 2);
		// first class is given by 0th, 2nd, 4th, ... pixels
		int count_1 = 0, count_2 = 0;
		double sum_1 = 0, sum_2 = 0;
		index=0;
		for (int z=0;z<10;++z) {
			for (int y=0;y<10;++y) {
				for (int x=0;x<10;++x) {
					if (index%2 == 0) {
						sum_1 += this.dummyImage3D.getValueInt(x, y, z);
						++count_1;
					}
					else {
						sum_2 += this.dummyImage3D.getValueInt(x, y, z);
						++count_2;		
					}
					++index;
				}
			}
		}
		double mean_1 = sum_1 / count_1;
		double mean_2 = sum_2 / count_2;
		assertTrue(IDS + " expecting size of first class to be 500, got "
			+ regionSizes[0], 500 == regionSizes[0]);
		assertTrue(IDS + " expecting size of second class to be 500, got "
			+ regionSizes[1], 500 == regionSizes[1]);
		assertTrue(IDS + " expecting mean of first class to be " + mean_1 + ", "
			+ "got " + regionMeans[0], Math.abs(mean_1 - regionMeans[0]) < 10e-10);
		assertTrue(IDS + " expecting mean of second class to be " + mean_2 + ", "
			+ "got " + regionMeans[1], Math.abs(mean_2 - regionMeans[1]) < 10e-10);
		sum_1 = 0;
		sum_2 = 0;
		index = 0;
		count_1 = 0;
		count_2 = 0;
		for (int z=0;z<10;++z) {
			for (int y=0;y<10;++y) {
				for (int x=0;x<10;++x) {
					if (index%2 == 0) {
						sum_1 += (this.dummyImage3D.getValueInt(x, y, z) - mean_1) 
								* (this.dummyImage3D.getValueInt(x, y, z) - mean_1);
						++count_1;
					}
					else {
						sum_2 += (this.dummyImage3D.getValueInt(x, y, z) - mean_2)
								* (this.dummyImage3D.getValueInt(x, y, z) - mean_2);
						++count_2;		
					}
					++index;
				}
			}
		}
		double var_1 = sum_1 / count_1;
		double var_2 = sum_2 / count_2;
		assertTrue(IDS + " expecting variance of first class to be " + var_1 + ", "
			+ "got " + regionVars[0], Math.abs(var_1 - regionVars[0]) < 10e-10);
		assertTrue(IDS + " expecting variance of second class to be " + var_2 + ", "
			+ "got " + regionVars[1], Math.abs(var_2 - regionVars[1]) < 10e-10);
			
		// set upper half of the image invisible
		boolean[][][] mask = new boolean[10][10][10];
		for (int z=0;z<10;++z) {
			for (int y=0;y<10;++y) {
				for (int x=0;x<10;++x) {
					if (y<5)
						mask[z][y][x] = false;
					else	
						mask[z][y][x] = true;
				}
			}
		}
		seg3D.setVisibilityMask(mask);
		exceptionThrown = false;
		try {
	    this.testObject.runOp();
    } catch (ALDException e) {
    	exceptionThrown = true;
    }
		assertTrue(IDS + " unexpected exception on running operator!",
			!exceptionThrown);
		
		regionSizes = this.testObject.getRegionSizes();
		regionMeans = this.testObject.getRegionMeans();
		regionVars = this.testObject.getRegionVars();
		
		assertTrue(IDS + " expecting two regions, got a different number...",
			regionSizes.length == 2);
		assertTrue(IDS + " expecting two mean values, got a different number...",
			regionMeans.length == 2);
		assertTrue(IDS + " expecting two variances, got a different number...",
			regionVars.length == 2);
		// first class is given by 0th, 2nd, 4th, ... pixels
		count_1 = 0;
		count_2 = 0;
		sum_1 = 0;
		sum_2 = 0;
		index=0;
		for (int z=0;z<10;++z) {
			for (int y=0;y<10;++y) {
				for (int x=0;x<10;++x) {
					if (y<5)
						continue;
					if (index%2 == 0) {
						sum_1 += this.dummyImage3D.getValueInt(x, y, z);
						++count_1;
					}
					else {
						sum_2 += this.dummyImage3D.getValueInt(x, y, z);
						++count_2;		
					}
					++index;
				}
			}
		}
		mean_1 = sum_1 / count_1;
		mean_2 = sum_2 / count_2;
		assertTrue(IDS + " expecting size of first class to be 250, got "
			+ regionSizes[0], 250 == regionSizes[0]);
		assertTrue(IDS + " expecting size of second class to be 250, got "
			+ regionSizes[1], 250 == regionSizes[1]);
		assertTrue(IDS + " expecting mean of first class to be " + mean_1 + ", "
			+ "got " + regionMeans[0], Math.abs(mean_1 - regionMeans[0]) < 10e-10);
		assertTrue(IDS + " expecting mean of second class to be " + mean_2 + ", "
			+ "got " + regionMeans[1], Math.abs(mean_2 - regionMeans[1]) < 10e-10);
		sum_1 = 0;
		sum_2 = 0;
		index = 0;
		count_1 = 0;
		count_2 = 0;
		for (int z=0;z<10;++z) {
			for (int y=0;y<10;++y) {
				for (int x=0;x<10;++x) {
					if (y<5)
						continue;
					if (index%2 == 0) {
						sum_1 += (this.dummyImage3D.getValueInt(x, y, z) - mean_1) 
								* (this.dummyImage3D.getValueInt(x, y, z) - mean_1);
						++count_1;
					}
					else {
						sum_2 += (this.dummyImage3D.getValueInt(x, y, z) - mean_2)
								* (this.dummyImage3D.getValueInt(x, y, z) - mean_2);
						++count_2;		
					}
					++index;
				}
			}
		}
		var_1 = sum_1 / count_1;
		var_2 = sum_2 / count_2;
		assertTrue(IDS + " expecting variance of first class to be " + var_1 + ", "
			+ "got " + regionVars[0], Math.abs(var_1 - regionVars[0]) < 10e-10);
		assertTrue(IDS + " expecting variance of second class to be " + var_2 + ", "
			+ "got " + regionVars[1], Math.abs(var_2 - regionVars[1]) < 10e-10);
	}

	/**
	 * Test calculations for 2D multi-channel image.
	 */
	@Test
	public void test2DImageMultiChannel() {
		
		/* 2D one channel image*/
		byte[][] bitfield = new byte[10][10];
		for (int j=0;j<10;++j)
			for (int i=0;i<10;++i)
				// every second pixel gets same class
				bitfield[j][i] = (byte)(((j*10+i)%2 == 0) ? 0 : 1);
		MTBSegmentation2D seg2D = new MTBSegmentation2D(10, 10, 2, bitfield);
		// configure test object
		this.testObject.setInputImage(this.dummyImage2DMultiChannel);
		this.testObject.setSegmentation(seg2D);
		this.testObject.setTargets(this.targets);
		boolean exceptionThrown = false;
		try {
	    this.testObject.runOp();
    } catch (ALDException e) {
    	exceptionThrown = true;
    }
		assertTrue(IDS + " unexpected exception on running operator!",
			!exceptionThrown);
		
		int[] regionSizes = this.testObject.getRegionSizes();
		double[][] regionMeans = this.testObject.getRegionMeansAllChannels();
		double[][] regionVars = this.testObject.getRegionVarsAllChannels();
		
		assertTrue(IDS + " expecting two regions, got a different number...",
			regionSizes.length == 2);
		assertTrue(IDS + " expecting 3 mean components, got a different number...",
			regionMeans.length == 3);
		assertTrue(IDS + " expecting 2 mean values for first channel...",
			regionMeans[0].length == 2);
		assertTrue(IDS + " expecting 2 mean values for second channel...",
			regionMeans[1].length == 2);
		assertTrue(IDS + " expecting 2 mean values for third channel...",
			regionMeans[2].length == 2);
		assertTrue(IDS + " expecting 3 variance components, got something else...",
			regionVars.length == 3);
		assertTrue(IDS + " expecting 2 variance values for first channel...",
			regionVars[0].length == 2);
		assertTrue(IDS + " expecting 2 variance values for second channel...",
			regionVars[1].length == 2);
		assertTrue(IDS + " expecting 3 variance values for second channel...",
			regionVars[2].length == 2);
		// first class is given by 0th, 2nd, 4th, ... pixels
		int count_1 = 0, count_2 = 0;
		double sum_1_1 = 0, sum_1_2 = 0, sum_1_3 = 0;
		double sum_2_1 = 0, sum_2_2 = 0, sum_2_3 = 0;
		int index=0;
		for (int y=0;y<10;++y) {
			for (int x=0;x<10;++x) {
				if (index%2 == 0) {
					sum_1_1 += this.dummyImage2DMultiChannel.getValueInt(x, y, 0, 0, 0);
					sum_1_2 += this.dummyImage2DMultiChannel.getValueInt(x, y, 0, 0, 1);
					sum_1_3 += this.dummyImage2DMultiChannel.getValueInt(x, y, 0, 0, 2);
					++count_1;
				}
				else {
					sum_2_1 += this.dummyImage2DMultiChannel.getValueInt(x, y, 0, 0, 0);
					sum_2_2 += this.dummyImage2DMultiChannel.getValueInt(x, y, 0, 0, 1);
					sum_2_3 += this.dummyImage2DMultiChannel.getValueInt(x, y, 0, 0, 2);
					++count_2;		
				}
				++index;
			}
		}
		double mean_1_1 = sum_1_1 / count_1;
		double mean_1_2 = sum_1_2 / count_1;
		double mean_1_3 = sum_1_3 / count_1;
		double mean_2_1 = sum_2_1 / count_2;
		double mean_2_2 = sum_2_2 / count_2;
		double mean_2_3 = sum_2_3 / count_2;
		assertTrue(IDS + " expecting size of first class to be 50, got "
			+ regionSizes[0], 50 == regionSizes[0]);
		assertTrue(IDS + " expecting size of second class to be 50, got "
			+ regionSizes[1], 50 == regionSizes[1]);
		assertTrue(IDS + " expecting mean 1 of first class to be " + mean_1_1 +", "
			+ "got " + regionMeans[0][0], 
				Math.abs(mean_1_1 - regionMeans[0][0]) < 10e-10);
		assertTrue(IDS + " expecting mean 2 of first class to be " + mean_1_2 +", "
			+ "got " + regionMeans[1][0], 
				Math.abs(mean_1_2 - regionMeans[1][0]) < 10e-10);
		assertTrue(IDS + " expecting mean 3 of first class to be " + mean_1_3 +", "
			+ "got " + regionMeans[2][0], 
				Math.abs(mean_1_3 - regionMeans[2][0]) < 10e-10);
		assertTrue(IDS + " expecting mean 1 of second class to be " + mean_2_1 +", "
			+ "got " + regionMeans[0][1], 
				Math.abs(mean_2_1 - regionMeans[0][1]) < 10e-10);
		assertTrue(IDS + " expecting mean 2 of second class to be " + mean_2_2 +", "
			+ "got " + regionMeans[1][1], 
				Math.abs(mean_2_2 - regionMeans[1][1]) < 10e-10);
		assertTrue(IDS + " expecting mean 3 of second class to be " + mean_2_3 +", "
			+ "got " + regionMeans[2][1], 
				Math.abs(mean_2_3 - regionMeans[2][1]) < 10e-10);
		sum_1_1 = 0;
		sum_1_2 = 0;
		sum_1_3 = 0;
		sum_2_1 = 0;
		sum_2_2 = 0;
		sum_2_3 = 0;
		index = 0;
		count_1 = 0;
		count_2 = 0;
		for (int y=0;y<10;++y) {
			for (int x=0;x<10;++x) {
				if (index%2 == 0) {
					sum_1_1 += 
							(this.dummyImage2DMultiChannel.getValueInt(x,y,0,0,0)-mean_1_1) 
						* (this.dummyImage2DMultiChannel.getValueInt(x,y,0,0,0)-mean_1_1);
					sum_1_2 += 
							(this.dummyImage2DMultiChannel.getValueInt(x,y,0,0,1)-mean_1_2) 
						* (this.dummyImage2DMultiChannel.getValueInt(x,y,0,0,1)-mean_1_2);
					sum_1_3 += 
							(this.dummyImage2DMultiChannel.getValueInt(x,y,0,0,2)-mean_1_3) 
						* (this.dummyImage2DMultiChannel.getValueInt(x,y,0,0,2)-mean_1_3);
					++count_1;
				}
				else {
					sum_2_1 += 
							(this.dummyImage2DMultiChannel.getValueInt(x,y,0,0,0)-mean_2_1)
						* (this.dummyImage2DMultiChannel.getValueInt(x,y,0,0,0)-mean_2_1);
					sum_2_2 += 
							(this.dummyImage2DMultiChannel.getValueInt(x,y,0,0,1)-mean_2_2)
						* (this.dummyImage2DMultiChannel.getValueInt(x,y,0,0,1)-mean_2_2);
					sum_2_3 += 
							(this.dummyImage2DMultiChannel.getValueInt(x,y,0,0,2)-mean_2_3)
						* (this.dummyImage2DMultiChannel.getValueInt(x,y,0,0,2)-mean_2_3);
					++count_2;		
				}
				++index;
			}
		}
		double var_1_1 = sum_1_1 / count_1;
		double var_1_2 = sum_1_2 / count_1;
		double var_1_3 = sum_1_3 / count_1;
		double var_2_1 = sum_2_1 / count_2;
		double var_2_2 = sum_2_2 / count_2;
		double var_2_3 = sum_2_3 / count_2;
		assertTrue(IDS + " expecting variance 1 of first class to be " + var_1_1 
			+ ", got " + regionVars[0][0], 
				Math.abs(var_1_1 - regionVars[0][0]) < 10e-10);
		assertTrue(IDS + " expecting variance 2 of first class to be " + var_1_2 
			+ ", got " + regionVars[1][0], 
				Math.abs(var_1_2 - regionVars[1][0]) < 10e-10);
		assertTrue(IDS + " expecting variance 3 of first class to be " + var_1_3 
			+ ", got " + regionVars[2][0], 
				Math.abs(var_1_3 - regionVars[2][0]) < 10e-10);
		assertTrue(IDS + " expecting variance 1 of second class to be " + var_2_1 
			+ ", got " + regionVars[0][1], 
				Math.abs(var_2_1 - regionVars[0][1]) < 10e-10);
		assertTrue(IDS + " expecting variance 2 of second class to be " + var_2_2 
			+ ", got " + regionVars[1][1], 
				Math.abs(var_2_2 - regionVars[1][1]) < 10e-10);
		assertTrue(IDS + " expecting variance 3 of second class to be " + var_2_3 
			+ ", got " + regionVars[2][1], 
				Math.abs(var_2_3 - regionVars[2][1]) < 10e-10);
			
		// set upper half of the image invisible
		boolean[][] mask = new boolean[10][10];
		for (int y=0;y<10;++y)
			for (int x=0;x<10;++x)
				if (y<5)
					mask[y][x] = false;
				else	
					mask[y][x] = true;
		seg2D.setVisibilityMask(mask);
		exceptionThrown = false;
		try {
	    this.testObject.runOp();
    } catch (ALDException e) {
    	exceptionThrown = true;
    }
		assertTrue(IDS + " unexpected exception on running operator!",
			!exceptionThrown);
		
		regionSizes = this.testObject.getRegionSizes();
		regionMeans = this.testObject.getRegionMeansAllChannels();
		regionVars = this.testObject.getRegionVarsAllChannels();
		
		assertTrue(IDS + " expecting two regions, got a different number...",
				regionSizes.length == 2);
		assertTrue(IDS + " expecting 3 mean components, got a different number...",
				regionMeans.length == 3);
		assertTrue(IDS + " expecting 2 mean values for first channel...",
				regionMeans[0].length == 2);
		assertTrue(IDS + " expecting 2 mean values for second channel...",
				regionMeans[1].length == 2);
		assertTrue(IDS + " expecting 2 mean values for third channel...",
				regionMeans[2].length == 2);
		assertTrue(IDS + " expecting 3 variance components, got something else...",
				regionVars.length == 3);
		assertTrue(IDS + " expecting 2 variance values for first channel...",
				regionVars[0].length == 2);
		assertTrue(IDS + " expecting 2 variance values for second channel...",
				regionVars[1].length == 2);
		assertTrue(IDS + " expecting 3 variance values for second channel...",
				regionVars[2].length == 2);
		// first class is given by 0th, 2nd, 4th, ... pixels
		count_1 = 0;
		count_2 = 0;
		sum_1_1 = 0;
		sum_1_2 = 0;
		sum_1_3 = 0;
		sum_2_1 = 0;
		sum_2_2 = 0;
		sum_2_3 = 0;
		index=0;
		for (int y=0;y<10;++y) {
			for (int x=0;x<10;++x) {
				if (y<5)
					continue;
				if (index%2 == 0) {
					sum_1_1 += this.dummyImage2DMultiChannel.getValueInt(x, y, 0, 0, 0);
					sum_1_2 += this.dummyImage2DMultiChannel.getValueInt(x, y, 0, 0, 1);
					sum_1_3 += this.dummyImage2DMultiChannel.getValueInt(x, y, 0, 0, 2);
					++count_1;
				}
				else {
					sum_2_1 += this.dummyImage2DMultiChannel.getValueInt(x, y, 0, 0, 0);
					sum_2_2 += this.dummyImage2DMultiChannel.getValueInt(x, y, 0, 0, 1);
					sum_2_3 += this.dummyImage2DMultiChannel.getValueInt(x, y, 0, 0, 2);
					++count_2;		
				}
				++index;
			}
		}
		mean_1_1 = sum_1_1 / count_1;
		mean_1_2 = sum_1_2 / count_1;
		mean_1_3 = sum_1_3 / count_1;
		mean_2_1 = sum_2_1 / count_2;
		mean_2_2 = sum_2_2 / count_2;
		mean_2_3 = sum_2_3 / count_2;
		assertTrue(IDS + " expecting size of first class to be 25, got "
			+ regionSizes[0], 25 == regionSizes[0]);
		assertTrue(IDS + " expecting size of second class to be 25, got "
			+ regionSizes[1], 25 == regionSizes[1]);
		assertTrue(IDS + " expecting mean 1 of first class to be " + mean_1_1 +", "
			+ "got " + regionMeans[0][0], 
				Math.abs(mean_1_1 - regionMeans[0][0]) < 10e-10);
		assertTrue(IDS + " expecting mean 2 of first class to be " + mean_1_2 +", "
			+ "got " + regionMeans[1][0], 
				Math.abs(mean_1_2 - regionMeans[1][0]) < 10e-10);
		assertTrue(IDS + " expecting mean 3 of first class to be " + mean_1_3 +", "
			+ "got " + regionMeans[2][0], 
				Math.abs(mean_1_3 - regionMeans[2][0]) < 10e-10);
		assertTrue(IDS + " expecting mean 1 of second class to be " + mean_2_1 +", "
			+ "got " + regionMeans[0][1], 
				Math.abs(mean_2_1 - regionMeans[0][1]) < accuracy);
		assertTrue(IDS + " expecting mean 2 of second class to be " + mean_2_2 +", "
			+ "got " + regionMeans[1][1], 
				Math.abs(mean_2_2 - regionMeans[1][1]) < accuracy);
		assertTrue(IDS + " expecting mean 3 of second class to be " + mean_2_3 +", "
			+ "got " + regionMeans[2][1], 
				Math.abs(mean_2_3 - regionMeans[2][1]) < accuracy);
		sum_1_1 = 0;
		sum_1_2 = 0;
		sum_1_3 = 0;
		sum_2_1 = 0;
		sum_2_2 = 0;
		sum_2_3 = 0;
		index = 0;
		count_1 = 0;
		count_2 = 0;
		for (int y=0;y<10;++y) {
			for (int x=0;x<10;++x) {
				if (y<5)
					continue;
				if (index%2 == 0) {
					sum_1_1 += 
							(this.dummyImage2DMultiChannel.getValueInt(x,y,0,0,0)-mean_1_1) 
						* (this.dummyImage2DMultiChannel.getValueInt(x,y,0,0,0)-mean_1_1);
					sum_1_2 += 
							(this.dummyImage2DMultiChannel.getValueInt(x,y,0,0,1)-mean_1_2) 
						* (this.dummyImage2DMultiChannel.getValueInt(x,y,0,0,1)-mean_1_2);
					sum_1_3 += 
							(this.dummyImage2DMultiChannel.getValueInt(x,y,0,0,2)-mean_1_3) 
						* (this.dummyImage2DMultiChannel.getValueInt(x,y,0,0,2)-mean_1_3);
					++count_1;
				}
				else {
					sum_2_1 += 
							(this.dummyImage2DMultiChannel.getValueInt(x,y,0,0,0)-mean_2_1)
						* (this.dummyImage2DMultiChannel.getValueInt(x,y,0,0,0)-mean_2_1);
					sum_2_2 += 
							(this.dummyImage2DMultiChannel.getValueInt(x,y,0,0,1)-mean_2_2)
						* (this.dummyImage2DMultiChannel.getValueInt(x,y,0,0,1)-mean_2_2);
					sum_2_3 += 
							(this.dummyImage2DMultiChannel.getValueInt(x,y,0,0,2)-mean_2_3)
						* (this.dummyImage2DMultiChannel.getValueInt(x,y,0,0,2)-mean_2_3);
					++count_2;		
				}
				++index;
			}
		}
		var_1_1 = sum_1_1 / count_1;
		var_1_2 = sum_1_2 / count_1;
		var_1_3 = sum_1_3 / count_1;
		var_2_1 = sum_2_1 / count_2;
		var_2_2 = sum_2_2 / count_2;
		var_2_3 = sum_2_3 / count_2;
		assertTrue(IDS + " expecting variance 1 of first class to be " + var_1_1 
			+ ", got " + regionVars[0][0], 
				Math.abs(var_1_1 - regionVars[0][0]) < 10e-10);
		assertTrue(IDS + " expecting variance 2 of first class to be " + var_1_2 
			+ ", got " + regionVars[1][0], 
				Math.abs(var_1_2 - regionVars[1][0]) < 10e-10);
		assertTrue(IDS + " expecting variance 3 of first class to be " + var_1_3 
			+ ", got " + regionVars[2][0], 
				Math.abs(var_1_3 - regionVars[2][0]) < 10e-10);
		assertTrue(IDS + " expecting variance 1 of second class to be " + var_2_1 
			+ ", got " + regionVars[0][1], 
				Math.abs(var_2_1 - regionVars[0][1]) < 10e-10);
		assertTrue(IDS + " expecting variance 2 of second class to be " + var_2_2 
			+ ", got " + regionVars[1][1], 
				Math.abs(var_2_2 - regionVars[1][1]) < 10e-10);
		assertTrue(IDS + " expecting variance 3 of second class to be " + var_2_3 
			+ ", got " + regionVars[2][1], 
				Math.abs(var_2_3 - regionVars[2][1]) < 10e-10);
	}

	/**
	 * Test calculations for 3D multi-channel image.
	 */
	@Test
	public void test3DImageMultiChannel() {
		
		/* 3D one channel image*/
		int[][][] bitfield = new int[10][10][10];
		boolean[][][] visibilityMap = new boolean[10][10][10];
		double[][][] weightMap = new double[10][10][10];
		int index = 0;
		for (int z=0;z<10;++z) {
			for (int y=0;y<10;++y) {
				for (int x=0;x<10;++x) {
					// every second pixel gets same class
					if (index%2 == 0)
						bitfield[z][y][x] = 0;
					else
						bitfield[z][y][x] = 1;
					visibilityMap[z][y][x] = true;
					weightMap[z][y][x] = 1.0;
					++index;
				}
			}
		}
		MTBSegmentation3D seg3D = new MTBSegmentation3D(10, 10, 10, 2, bitfield,
			visibilityMap, weightMap);
		// configure test object
		this.testObject.setInputImage(this.dummyImage3DMultiChannel);
		this.testObject.setSegmentation(seg3D);
		this.testObject.setTargets(this.targets);
		boolean exceptionThrown = false;
		try {
	    this.testObject.runOp();
    } catch (ALDException e) {
    	exceptionThrown = true;
    }
		assertTrue(IDS + " unexpected exception on running operator!",
			!exceptionThrown);
		
		int[] regionSizes = this.testObject.getRegionSizes();
		double[][] regionMeans = this.testObject.getRegionMeansAllChannels();
		double[][] regionVars = this.testObject.getRegionVarsAllChannels();
		
		assertTrue(IDS + " expecting two regions, got a different number...",
			regionSizes.length == 2);
		assertTrue(IDS + " expecting 3 mean components, got a different number...",
			regionMeans.length == 3);
		assertTrue(IDS + " expecting 2 mean values for first channel...",
			regionMeans[0].length == 2);
		assertTrue(IDS + " expecting 2 mean values for second channel...",
			regionMeans[1].length == 2);
		assertTrue(IDS + " expecting 2 mean values for third channel...",
			regionMeans[2].length == 2);
		assertTrue(IDS + " expecting 3 variance components, got something else...",
			regionVars.length == 3);
		assertTrue(IDS + " expecting 2 variance values for first channel...",
			regionVars[0].length == 2);
		assertTrue(IDS + " expecting 2 variance values for second channel...",
			regionVars[1].length == 2);
		assertTrue(IDS + " expecting 3 variance values for second channel...",
			regionVars[2].length == 2);
		// first class is given by 0th, 2nd, 4th, ... pixels
		double sum_1_1 = 0, sum_1_2 = 0, sum_1_3 = 0;
		double sum_2_1 = 0, sum_2_2 = 0, sum_2_3 = 0;
		int count_1 = 0, count_2 = 0;
		index=0;
		for (int z=0;z<10;++z) {
			for (int y=0;y<10;++y) {
				for (int x=0;x<10;++x) {
					if (index%2 == 0) {
						sum_1_1 += this.dummyImage3DMultiChannel.getValueInt(x, y, z, 0, 0);
						sum_1_2 += this.dummyImage3DMultiChannel.getValueInt(x, y, z, 0, 1);
						sum_1_3 += this.dummyImage3DMultiChannel.getValueInt(x, y, z, 0, 2);
						++count_1;
					}
					else {
						sum_2_1 += this.dummyImage3DMultiChannel.getValueInt(x, y, z, 0, 0);
						sum_2_2 += this.dummyImage3DMultiChannel.getValueInt(x, y, z, 0, 1);
						sum_2_3 += this.dummyImage3DMultiChannel.getValueInt(x, y, z, 0, 2);
						++count_2;		
					}
					++index;
				}
			}
		}
		double mean_1_1 = sum_1_1 / count_1;
		double mean_1_2 = sum_1_2 / count_1;
		double mean_1_3 = sum_1_3 / count_1;
		double mean_2_1 = sum_2_1 / count_2;
		double mean_2_2 = sum_2_2 / count_2;
		double mean_2_3 = sum_2_3 / count_2;
		assertTrue(IDS + " expecting size of first class to be 500, got "
			+ regionSizes[0], 500 == regionSizes[0]);
		assertTrue(IDS + " expecting size of second class to be 500, got "
			+ regionSizes[1], 500 == regionSizes[1]);
		assertTrue(IDS + " expecting mean 1 of first class to be " + mean_1_1 +", "
			+ "got " + regionMeans[0][0], 
				Math.abs(mean_1_1 - regionMeans[0][0]) < 10e-10);
		assertTrue(IDS + " expecting mean 2 of first class to be " + mean_1_2 +", "
			+ "got " + regionMeans[1][0], 
				Math.abs(mean_1_2 - regionMeans[1][0]) < 10e-10);
		assertTrue(IDS + " expecting mean 3 of first class to be " + mean_1_3 +", "
			+ "got " + regionMeans[2][0], 
				Math.abs(mean_1_3 - regionMeans[2][0]) < 10e-10);
		assertTrue(IDS + " expecting mean 1 of second class to be " + mean_2_1 +", "
			+ "got " + regionMeans[0][1], 
				Math.abs(mean_2_1 - regionMeans[0][1]) < 10e-10);
		assertTrue(IDS + " expecting mean 2 of second class to be " + mean_2_2 +", "
			+ "got " + regionMeans[1][1], 
				Math.abs(mean_2_2 - regionMeans[1][1]) < 10e-10);
		assertTrue(IDS + " expecting mean 3 of second class to be " + mean_2_3 +", "
			+ "got " + regionMeans[2][1], 
				Math.abs(mean_2_3 - regionMeans[2][1]) < 10e-10);
		sum_1_1 = 0;
		sum_1_2 = 0;
		sum_1_3 = 0;
		sum_2_1 = 0;
		sum_2_2 = 0;
		sum_2_3 = 0;
		index = 0;
		count_1 = 0;
		count_2 = 0;
		for (int z=0;z<10;++z) {
			for (int y=0;y<10;++y) {
				for (int x=0;x<10;++x) {
					if (index%2 == 0) {
						sum_1_1 += 
								(this.dummyImage3DMultiChannel.getValueInt(x,y,z,0,0)-mean_1_1) 
							* (this.dummyImage3DMultiChannel.getValueInt(x,y,z,0,0)-mean_1_1);
						sum_1_2 += 
								(this.dummyImage3DMultiChannel.getValueInt(x,y,z,0,1)-mean_1_2) 
							* (this.dummyImage3DMultiChannel.getValueInt(x,y,z,0,1)-mean_1_2);
						sum_1_3 += 
								(this.dummyImage3DMultiChannel.getValueInt(x,y,z,0,2)-mean_1_3) 
							* (this.dummyImage3DMultiChannel.getValueInt(x,y,z,0,2)-mean_1_3);
						++count_1;
					}
					else {
						sum_2_1 += 
								(this.dummyImage3DMultiChannel.getValueInt(x,y,z,0,0)-mean_2_1)
							* (this.dummyImage3DMultiChannel.getValueInt(x,y,z,0,0)-mean_2_1);
						sum_2_2 += 
								(this.dummyImage3DMultiChannel.getValueInt(x,y,z,0,1)-mean_2_2)
							* (this.dummyImage3DMultiChannel.getValueInt(x,y,z,0,1)-mean_2_2);
						sum_2_3 += 
								(this.dummyImage3DMultiChannel.getValueInt(x,y,z,0,2)-mean_2_3)
							* (this.dummyImage3DMultiChannel.getValueInt(x,y,z,0,2)-mean_2_3);
						++count_2;		
					}
					++index;
				}
			}
		}
		double var_1_1 = sum_1_1 / count_1;
		double var_1_2 = sum_1_2 / count_1;
		double var_1_3 = sum_1_3 / count_1;
		double var_2_1 = sum_2_1 / count_2;
		double var_2_2 = sum_2_2 / count_2;
		double var_2_3 = sum_2_3 / count_2;
		assertTrue(IDS + " expecting variance 1 of first class to be " + var_1_1 
			+ ", got " + regionVars[0][0], 
				Math.abs(var_1_1 - regionVars[0][0]) < 10e-10);
		assertTrue(IDS + " expecting variance 2 of first class to be " + var_1_2 
			+ ", got " + regionVars[1][0], 
				Math.abs(var_1_2 - regionVars[1][0]) < 10e-10);
		assertTrue(IDS + " expecting variance 3 of first class to be " + var_1_3 
			+ ", got " + regionVars[2][0], 
				Math.abs(var_1_3 - regionVars[2][0]) < 10e-10);
		assertTrue(IDS + " expecting variance 1 of second class to be " + var_2_1 
			+ ", got " + regionVars[0][1], 
				Math.abs(var_2_1 - regionVars[0][1]) < 10e-10);
		assertTrue(IDS + " expecting variance 2 of second class to be " + var_2_2 
			+ ", got " + regionVars[1][1], 
				Math.abs(var_2_2 - regionVars[1][1]) < 10e-10);
		assertTrue(IDS + " expecting variance 3 of second class to be " + var_2_3 
			+ ", got " + regionVars[2][1], 
				Math.abs(var_2_3 - regionVars[2][1]) < 10e-10);
			
		// set upper half of the image invisible
		boolean[][][] mask = new boolean[10][10][10];
		for (int z=0;z<10;++z)
			for (int y=0;y<10;++y)
				for (int x=0;x<10;++x)
					if (y<5)
						mask[z][y][x] = false;
					else	
						mask[z][y][x] = true;
		seg3D.setVisibilityMask(mask);
		exceptionThrown = false;
		try {
	    this.testObject.runOp();
    } catch (ALDException e) {
    	exceptionThrown = true;
    }
		assertTrue(IDS + " unexpected exception on running operator!",
			!exceptionThrown);
		
		regionSizes = this.testObject.getRegionSizes();
		regionMeans = this.testObject.getRegionMeansAllChannels();
		regionVars = this.testObject.getRegionVarsAllChannels();
		
		assertTrue(IDS + " expecting two regions, got a different number...",
				regionSizes.length == 2);
		assertTrue(IDS + " expecting 3 mean components, got a different number...",
				regionMeans.length == 3);
		assertTrue(IDS + " expecting 2 mean values for first channel...",
				regionMeans[0].length == 2);
		assertTrue(IDS + " expecting 2 mean values for second channel...",
				regionMeans[1].length == 2);
		assertTrue(IDS + " expecting 2 mean values for third channel...",
				regionMeans[2].length == 2);
		assertTrue(IDS + " expecting 3 variance components, got something else...",
				regionVars.length == 3);
		assertTrue(IDS + " expecting 2 variance values for first channel...",
				regionVars[0].length == 2);
		assertTrue(IDS + " expecting 2 variance values for second channel...",
				regionVars[1].length == 2);
		assertTrue(IDS + " expecting 3 variance values for second channel...",
				regionVars[2].length == 2);
		// first class is given by 0th, 2nd, 4th, ... pixels
		count_1 = 0;
		count_2 = 0;
		sum_1_1 = 0;
		sum_1_2 = 0;
		sum_1_3 = 0;
		sum_2_1 = 0;
		sum_2_2 = 0;
		sum_2_3 = 0;
		index=0;
		for (int z=0;z<10;++z) {
			for (int y=0;y<10;++y) {
				for (int x=0;x<10;++x) {
					if (y<5)
						continue;
					if (index%2 == 0) {
						sum_1_1 += this.dummyImage3DMultiChannel.getValueInt(x, y, z, 0, 0);
						sum_1_2 += this.dummyImage3DMultiChannel.getValueInt(x, y, z, 0, 1);
						sum_1_3 += this.dummyImage3DMultiChannel.getValueInt(x, y, z, 0, 2);
						++count_1;
					}
					else {
						sum_2_1 += this.dummyImage3DMultiChannel.getValueInt(x, y, z, 0, 0);
						sum_2_2 += this.dummyImage3DMultiChannel.getValueInt(x, y, z, 0, 1);
						sum_2_3 += this.dummyImage3DMultiChannel.getValueInt(x, y, z, 0, 2);
						++count_2;		
					}
					++index;
				}
			}
		}
		mean_1_1 = sum_1_1 / count_1;
		mean_1_2 = sum_1_2 / count_1;
		mean_1_3 = sum_1_3 / count_1;
		mean_2_1 = sum_2_1 / count_2;
		mean_2_2 = sum_2_2 / count_2;
		mean_2_3 = sum_2_3 / count_2;
		assertTrue(IDS + " expecting size of first class to be 250, got "
			+ regionSizes[0], 250 == regionSizes[0]);
		assertTrue(IDS + " expecting size of second class to be 250, got "
			+ regionSizes[1], 250 == regionSizes[1]);
		assertTrue(IDS + " expecting mean 1 of first class to be " + mean_1_1 +", "
			+ "got " + regionMeans[0][0], 
				Math.abs(mean_1_1 - regionMeans[0][0]) < 10e-10);
		assertTrue(IDS + " expecting mean 2 of first class to be " + mean_1_2 +", "
			+ "got " + regionMeans[1][0], 
				Math.abs(mean_1_2 - regionMeans[1][0]) < 10e-10);
		assertTrue(IDS + " expecting mean 3 of first class to be " + mean_1_3 +", "
			+ "got " + regionMeans[2][0], 
				Math.abs(mean_1_3 - regionMeans[2][0]) < 10e-10);
		assertTrue(IDS + " expecting mean 1 of second class to be " + mean_2_1 +", "
			+ "got " + regionMeans[0][1], 
				Math.abs(mean_2_1 - regionMeans[0][1]) < accuracy);
		assertTrue(IDS + " expecting mean 2 of second class to be " + mean_2_2 +", "
			+ "got " + regionMeans[1][1], 
				Math.abs(mean_2_2 - regionMeans[1][1]) < accuracy);
		assertTrue(IDS + " expecting mean 3 of second class to be " + mean_2_3 +", "
			+ "got " + regionMeans[2][1], 
				Math.abs(mean_2_3 - regionMeans[2][1]) < accuracy);
		sum_1_1 = 0;
		sum_1_2 = 0;
		sum_1_3 = 0;
		sum_2_1 = 0;
		sum_2_2 = 0;
		sum_2_3 = 0;
		index = 0;
		count_1 = 0;
		count_2 = 0;
		for (int z=0;z<10;++z) {
			for (int y=0;y<10;++y) {
				for (int x=0;x<10;++x) {
					if (y<5)
						continue;
					if (index%2 == 0) {
						sum_1_1 += 
								(this.dummyImage3DMultiChannel.getValueInt(x,y,z,0,0)-mean_1_1) 
							* (this.dummyImage3DMultiChannel.getValueInt(x,y,z,0,0)-mean_1_1);
						sum_1_2 += 
								(this.dummyImage3DMultiChannel.getValueInt(x,y,z,0,1)-mean_1_2) 
							* (this.dummyImage3DMultiChannel.getValueInt(x,y,z,0,1)-mean_1_2);
						sum_1_3 += 
								(this.dummyImage3DMultiChannel.getValueInt(x,y,z,0,2)-mean_1_3) 
							* (this.dummyImage3DMultiChannel.getValueInt(x,y,z,0,2)-mean_1_3);
						++count_1;
					}
					else {
						sum_2_1 += 
								(this.dummyImage3DMultiChannel.getValueInt(x,y,z,0,0)-mean_2_1)
							* (this.dummyImage3DMultiChannel.getValueInt(x,y,z,0,0)-mean_2_1);
						sum_2_2 += 
								(this.dummyImage3DMultiChannel.getValueInt(x,y,z,0,1)-mean_2_2)
							* (this.dummyImage3DMultiChannel.getValueInt(x,y,z,0,1)-mean_2_2);
						sum_2_3 += 
								(this.dummyImage3DMultiChannel.getValueInt(x,y,z,0,2)-mean_2_3)
							* (this.dummyImage3DMultiChannel.getValueInt(x,y,z,0,2)-mean_2_3);
						++count_2;		
					}
					++index;
				}
			}
		}
		var_1_1 = sum_1_1 / count_1;
		var_1_2 = sum_1_2 / count_1;
		var_1_3 = sum_1_3 / count_1;
		var_2_1 = sum_2_1 / count_2;
		var_2_2 = sum_2_2 / count_2;
		var_2_3 = sum_2_3 / count_2;
		assertTrue(IDS + " expecting variance 1 of first class to be " + var_1_1 
			+ ", got " + regionVars[0][0], 
				Math.abs(var_1_1 - regionVars[0][0]) < 10e-10);
		assertTrue(IDS + " expecting variance 2 of first class to be " + var_1_2 
			+ ", got " + regionVars[1][0], 
				Math.abs(var_1_2 - regionVars[1][0]) < 10e-10);
		assertTrue(IDS + " expecting variance 3 of first class to be " + var_1_3 
			+ ", got " + regionVars[2][0], 
				Math.abs(var_1_3 - regionVars[2][0]) < 10e-10);
		assertTrue(IDS + " expecting variance 1 of second class to be " + var_2_1 
			+ ", got " + regionVars[0][1], 
				Math.abs(var_2_1 - regionVars[0][1]) < 10e-10);
		assertTrue(IDS + " expecting variance 2 of second class to be " + var_2_2 
			+ ", got " + regionVars[1][1], 
				Math.abs(var_2_2 - regionVars[1][1]) < 10e-10);
		assertTrue(IDS + " expecting variance 3 of second class to be " + var_2_3 
			+ ", got " + regionVars[2][1], 
				Math.abs(var_2_3 - regionVars[2][1]) < 10e-10);
	}
}