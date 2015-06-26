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

package de.unihalle.informatik.MiToBo.math.images;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.math.images.ImageStatistics.StatValue;

/**
 * JUnit test class for {@link ImageStatistics}.
 * 
 * @author moeller
 */
public class TestImageStatistics {

	/**
	 * Dummy image for tests.
	 */
	protected MTBImage dummyImage;
	
	/**
	 * Fixture.
	 */
	@Before
	public void initTestClass() {
		// create a test image...
		this.dummyImage = 
				MTBImage.createMTBImage(10, 10, 1, 1, 1, MTBImageType.MTB_INT);
		for (int y=0;y<10;++y)
			for (int x=0;x<10;++x)
				this.dummyImage.putValueInt(x, y, x*y);
	}
	
	/**
	 * Test if statistical calculations are ok.
	 */
	@Test
	public void testStatisticalCalcs() {
		try {
			double resultVal;
			ImageStatistics statOp;
			
			// minimal intensity
			statOp = new ImageStatistics(this.dummyImage, StatValue.INTENSITY_MIN);
			statOp.runOp();
			resultVal = statOp.getResultValue();
			assertTrue("Image intensity minimum is expected to be 0, but is " 
					+ resultVal, resultVal == 0);
			
			// maximal intensity
			statOp = new ImageStatistics(this.dummyImage, StatValue.INTENSITY_MAX);
			statOp.runOp();
			resultVal = statOp.getResultValue();
			assertTrue("Image intensity maximum is expected to be 81, but is " 
					+ resultVal, resultVal == 81);			

			// mean intensity
			statOp = new ImageStatistics(this.dummyImage, StatValue.INTENSITY_MEAN);
			statOp.runOp();
			resultVal = statOp.getResultValue();
			double wantedVal = calcIntensityMean(this.dummyImage);
			assertTrue("Image intensity mean is expected to be " + wantedVal +
					" but is " + resultVal, Math.abs(resultVal-wantedVal)<10e-10);			
			
			// intensity variance
			statOp = 
					new ImageStatistics(this.dummyImage, StatValue.INTENSITY_VARIANCE);
			statOp.runOp();
			resultVal = statOp.getResultValue();
			wantedVal = calcIntensityVariance(this.dummyImage);
			assertTrue("Image intensity variance is expected to be " + wantedVal +
					" but is " + resultVal, Math.abs(resultVal-wantedVal)<10e-10);	
		} catch (Exception e) {
			fail("Did not expect an exception to occur...");
			e.printStackTrace();
		}
	}
	
	/**
	 * Calculates the average intensity of the image.
	 * @return	Average intensity value.
	 */
	private static double calcIntensityMean(MTBImage img) {
		int pixCount = 0;
		double sum = 0.0;
		for (int z=0;z<img.getSizeZ();++z) {
			for (int y=0;y<img.getSizeY();++y) {
				for (int x=0;x<img.getSizeX();++x) {
					sum += img.getValueDouble(x, y, z);
					++pixCount;
				}
			}
		}
		return sum/pixCount;
	}

	/**
	 * Calculates the intensity variance of the image.
	 * @return	Intensity variance of the image.
	 */
	private static double calcIntensityVariance(MTBImage img) {
		double mean = calcIntensityMean(img);
		int pixCount = 0;
		double sum = 0.0;
		for (int z=0;z<img.getSizeZ();++z) {
			for (int y=0;y<img.getSizeY();++y) {
				for (int x=0;x<img.getSizeX();++x) {
					sum +=   (img.getValueDouble(x, y, z)-mean)
							   * (img.getValueDouble(x, y, z)-mean);
					++pixCount;
				}
			}
		}
		return sum/pixCount;
	}
}