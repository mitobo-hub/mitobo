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
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageWindow.BoundaryPadding;
import de.unihalle.informatik.MiToBo.core.datatypes.wrapper.MTBBooleanData;
import de.unihalle.informatik.MiToBo.filters.linear.anisotropic.GaussPDxxFilter2D;
import de.unihalle.informatik.MiToBo.filters.linear.anisotropic.OrientedFilter2D.ApplicationMode;
import de.unihalle.informatik.MiToBo.filters.linear.anisotropic.OrientedFilter2DBatchAnalyzer.JoinMode;
import de.unihalle.informatik.MiToBo.io.images.ImageReaderMTB;

/**
 * JUnit test class for {@link OrientedFilter2DBatchAnalyzer}.
 * <p>
 * This test class checks if the oriented batch filter operator is running correctly.
 * We only use the {@link GaussPDxxFilter2D} here, assuming that the filter itself
 * does not contain any errors. Note that the filter ships with its own JUnit test class.
 * 
 * @author Birgit Moeller
 */
public class TestOrientedFilter2DBatchAnalyzer {

	/**
	 * Local accuracy for numerical tests.
	 */
	private static final double accuracyFFT = 1.0e-6;

	/**
	 * Local accuracy for numerical tests in standard mode versus FFT.
	 */
	private static final double accuracyStandard = 1.0e-4;

	/**
	 * Class identifier string.
	 */
	private static final String classID = "[TestOrientedFilter2DBatchAnalyzer]";
	
	/**
	 * Test object.
	 */
	private OrientedFilter2DBatchAnalyzer testObject = null;
	
	/**
	 * Test image.
	 */
	private MTBImage inputImage;

	/**
	 * Result stack.
	 */
	private MTBImage resultStack;

	/**
	 * Result maximum image.
	 */
	private MTBImage resultMaxImage;

	/**
	 * Fixture.
	 */
	@Before
	public void initTestClass() {
		
		// read the label image with holes
		boolean thrown = false;
		ImageReaderMTB reader;
		try {
			reader = new ImageReaderMTB(TestOrientedFilter2DBatchAnalyzer.class.getResource(
					"leaf-gray.tif").getFile());
			reader.runOp(true);
			this.inputImage = reader.getResultMTBImage();
			
			reader = new ImageReaderMTB(TestOrientedFilter2DBatchAnalyzer.class.getResource(
					"leaf-gray-filterResult.tif").getFile());
			reader.runOp(true);
			this.resultStack = reader.getResultMTBImage();

			reader = new ImageReaderMTB(TestOrientedFilter2DBatchAnalyzer.class.getResource(
					"leaf-gray-maxResult.tif").getFile());
			reader.runOp(true);
			this.resultMaxImage = reader.getResultMTBImage();
		} catch (Exception e) {
			thrown = true;
		}
		assertFalse(classID + " could not read test images!", thrown);
	}

	/**
	 * Test application of operator with {@link GaussPDxxFilter2D} filter in FFT mode.
	 */
	@Test
	public void testOperatorFFT() {
		
		GaussPDxxFilter2D testFilter = null; 
		
		boolean thrown = false;
		try {
			this.testObject = new OrientedFilter2DBatchAnalyzer();
			this.testObject.setInputImage(this.inputImage);
			this.testObject.setAngleSampling(15);
			this.testObject.setMinAngle(0);
			this.testObject.setMaxAngle(180);
			this.testObject.setParameter("jMode", JoinMode.JOIN_MAXIMUM);
			
			testFilter = new GaussPDxxFilter2D();
			this.testObject.setOrientedFilter(testFilter);
		} catch (ALDOperatorException e) {
			e.printStackTrace();
			thrown = true;
		}
		assertFalse(classID + " could not initialize operator!", thrown);
		
		testFilter.setStandardDeviation(2.0);
		testFilter.setHeight(9);
		testFilter.enableNormalization();
		testFilter.setInvertMask(false);
		testFilter.setApplicationMode(ApplicationMode.FFT);
		
		// run test operator
		thrown = false;
		try {
			this.testObject.runOp();
		} catch (Exception e) {
			thrown = true;
		}
		assertFalse(classID + " problems running the operator!", thrown);
		
		MTBImage testResultStack = this.testObject.getFilterResponseStack();
		MTBImage testResultMaxImage = this.testObject.getResultImage();
		
		// compare stack
		for (int z=0; z<this.inputImage.getSizeZ(); ++z) {
			for (int y=0; y<this.inputImage.getSizeY(); ++y) {
				for (int x=0; x<this.inputImage.getSizeX(); ++x) {
					assertTrue("Pixel difference! Position " + "(" + x + "," + y + ") => " 
						+ " expected = " + this.resultStack.getValueDouble(x, y, z) 
							+ " , found = " + testResultStack.getValueDouble(x, y, z),
								Math.abs(this.resultStack.getValueDouble(x, y, z) 
										- testResultStack.getValueDouble(x, y, z)) < accuracyFFT);
				}			
			}
		}
		// compare max image
		for (int y=0; y<this.inputImage.getSizeY(); ++y) {
			for (int x=0; x<this.inputImage.getSizeX(); ++x) {
				assertTrue("Pixel difference! Position " + "(" + x + "," + y + ") => " 
						+ " expected = " + this.resultMaxImage.getValueDouble(x, y) 
						+ " , found = " + testResultMaxImage.getValueDouble(x, y),
						Math.abs(this.resultMaxImage.getValueDouble(x, y) 
								- testResultMaxImage.getValueDouble(x, y)) < accuracyFFT);
			}			
		}
	}
	
	/**
	 * Test application of operator with {@link GaussPDxxFilter2D} filter in standard mode.
	 * <p>
	 * Reference results have been computed in FFT mode, so we need to use mirror padding
	 * and a slightly lower accuracy than in FFT mode due to numerical inaccuracies.
	 */
	@Test
	public void testOperatorStandardMirroring() {
		
		GaussPDxxFilter2D testFilter = null; 
		
		boolean thrown = false;
		try {
			this.testObject = new OrientedFilter2DBatchAnalyzer();
			this.testObject.setInputImage(this.inputImage);
			this.testObject.setAngleSampling(15);
			this.testObject.setMinAngle(0);
			this.testObject.setMaxAngle(180);
			this.testObject.setParameter("jMode", JoinMode.JOIN_MAXIMUM);
			
			testFilter = new GaussPDxxFilter2D();
			this.testObject.setOrientedFilter(testFilter);
		} catch (ALDOperatorException e) {
			e.printStackTrace();
			thrown = true;
		}
		assertFalse(classID + " could not initialize operator!", thrown);
		
		testFilter.setStandardDeviation(2.0);
		testFilter.setHeight(9);
		testFilter.enableNormalization();
		testFilter.setInvertMask(false);
		testFilter.setApplicationMode(ApplicationMode.STANDARD);
		testFilter.setPaddingVariant(BoundaryPadding.PADDING_MIRROR);
		
		// run test operator
		thrown = false;
		try {
			this.testObject.runOp();
		} catch (Exception e) {
			thrown = true;
		}
		assertFalse(classID + " problems running the operator!", thrown);
		
		MTBImage testResultStack = this.testObject.getFilterResponseStack();
		MTBImage testResultMaxImage = this.testObject.getResultImage();
		
		// compare stack
		for (int z=0; z<this.inputImage.getSizeZ(); ++z) {
			for (int y=0; y<this.inputImage.getSizeY(); ++y) {
				for (int x=0; x<this.inputImage.getSizeX(); ++x) {
					assertTrue("Pixel difference! Position " + "(" + x + "," + y + ") => " 
						+ " expected = " + this.resultStack.getValueDouble(x, y, z) 
							+ " , found = " + testResultStack.getValueDouble(x, y, z),
								Math.abs(this.resultStack.getValueDouble(x, y, z) 
										- testResultStack.getValueDouble(x, y, z)) < accuracyStandard);
				}			
			}
		}
		// compare max image
		for (int y=0; y<this.inputImage.getSizeY(); ++y) {
			for (int x=0; x<this.inputImage.getSizeX(); ++x) {
				assertTrue("Pixel difference! Position " + "(" + x + "," + y + ") => " 
						+ " expected = " + this.resultMaxImage.getValueDouble(x, y) 
						+ " , found = " + testResultMaxImage.getValueDouble(x, y),
						Math.abs(this.resultMaxImage.getValueDouble(x, y) 
								- testResultMaxImage.getValueDouble(x, y)) < accuracyStandard);
			}			
		}
	}
	
	/**
	 * Test application of operator with {@link GaussPDxxFilter2D} filter in parallel FFT mode.
	 */
	@Test
	public void testOperatorFFTParallel() {
		
		GaussPDxxFilter2D testFilter = null; 

		/*
		 * Re-run operator in parallel mode.
		 */
		
		boolean thrown = false;
		try {
			this.testObject = new OrientedFilter2DBatchAnalyzer();
			this.testObject.setInputImage(this.inputImage);
			this.testObject.setAngleSampling(15);
			this.testObject.setMinAngle(0);
			this.testObject.setMaxAngle(180);
			this.testObject.setParameter("jMode", JoinMode.JOIN_MAXIMUM);
			
			testFilter = new GaussPDxxFilter2D();
			this.testObject.setOrientedFilter(testFilter);
		} catch (ALDOperatorException e) {
			e.printStackTrace();
			thrown = true;
		}
		assertFalse(classID + " could not initialize operator!", thrown);
		
		testFilter.setStandardDeviation(2.0);
		testFilter.setHeight(9);
		testFilter.enableNormalization();
		testFilter.setInvertMask(false);
		testFilter.setApplicationMode(ApplicationMode.FFT);
		
		this.testObject.setRunParallel(new MTBBooleanData(true));
		
		// run test operator
		thrown = false;
		try {
			this.testObject.runOp();
		} catch (Exception e) {
			thrown = true;
		}
		assertFalse(classID + " problems running the operator in parallel!", thrown);

		MTBImage testResultStack = this.testObject.getFilterResponseStack();
		MTBImage testResultMaxImage = this.testObject.getResultImage();
		
		// compare stack
		for (int z=0; z<this.inputImage.getSizeZ(); ++z) {
			for (int y=0; y<this.inputImage.getSizeY(); ++y) {
				for (int x=0; x<this.inputImage.getSizeX(); ++x) {
					assertTrue("Pixel difference! Position " + "(" + x + "," + y + ") => " 
						+ " expected = " + this.resultStack.getValueDouble(x, y, z) 
							+ " , found = " + testResultStack.getValueDouble(x, y, z),
								Math.abs(this.resultStack.getValueDouble(x, y, z) 
										- testResultStack.getValueDouble(x, y, z)) < accuracyFFT);
				}			
			}
		}
		// compare max image
		for (int y=0; y<this.inputImage.getSizeY(); ++y) {
			for (int x=0; x<this.inputImage.getSizeX(); ++x) {
				assertTrue("Pixel difference! Position " + "(" + x + "," + y + ") => " 
						+ " expected = " + this.resultMaxImage.getValueDouble(x, y) 
						+ " , found = " + testResultMaxImage.getValueDouble(x, y),
						Math.abs(this.resultMaxImage.getValueDouble(x, y) 
								- testResultMaxImage.getValueDouble(x, y)) < accuracyFFT);
			}			
		}
	}
	
	/**
	 * Test application of operator with {@link GaussPDxxFilter2D} filter in parallel mode.
	 * <p>
	 * In parallel mode operator is invoked in standard convolutional mode with mirror padding.
	 * The ImgLib2 FFT mode is by itself parallelized and might interfere with filter parallelization.
	 */
	@Test
	public void testOperatorStandardMirroringParallel() {
		
		GaussPDxxFilter2D testFilter = null; 

		/*
		 * Re-run operator in parallel mode.
		 */
		
		boolean thrown = false;
		try {
			this.testObject = new OrientedFilter2DBatchAnalyzer();
			this.testObject.setInputImage(this.inputImage);
			this.testObject.setAngleSampling(15);
			this.testObject.setMinAngle(0);
			this.testObject.setMaxAngle(180);
			this.testObject.setParameter("jMode", JoinMode.JOIN_MAXIMUM);
			
			testFilter = new GaussPDxxFilter2D();
			this.testObject.setOrientedFilter(testFilter);
		} catch (ALDOperatorException e) {
			e.printStackTrace();
			thrown = true;
		}
		assertFalse(classID + " could not initialize operator!", thrown);
		
		testFilter.setStandardDeviation(2.0);
		testFilter.setHeight(9);
		testFilter.enableNormalization();
		testFilter.setInvertMask(false);
		testFilter.setApplicationMode(ApplicationMode.STANDARD);
		testFilter.setPaddingVariant(BoundaryPadding.PADDING_MIRROR);
		
		this.testObject.setRunParallel(new MTBBooleanData(true));
		
		// run test operator
		thrown = false;
		try {
			this.testObject.runOp();
		} catch (Exception e) {
			thrown = true;
		}
		assertFalse(classID + " problems running the operator in parallel!", thrown);

		MTBImage testResultStack = this.testObject.getFilterResponseStack();
		MTBImage testResultMaxImage = this.testObject.getResultImage();
		
		// compare stack
		for (int z=0; z<this.inputImage.getSizeZ(); ++z) {
			for (int y=0; y<this.inputImage.getSizeY(); ++y) {
				for (int x=0; x<this.inputImage.getSizeX(); ++x) {
					assertTrue("Pixel difference! Position " + "(" + x + "," + y + ") => " 
						+ " expected = " + this.resultStack.getValueDouble(x, y, z) 
							+ " , found = " + testResultStack.getValueDouble(x, y, z),
								Math.abs(this.resultStack.getValueDouble(x, y, z) 
										- testResultStack.getValueDouble(x, y, z)) < accuracyStandard);
				}			
			}
		}
		// compare max image
		for (int y=0; y<this.inputImage.getSizeY(); ++y) {
			for (int x=0; x<this.inputImage.getSizeX(); ++x) {
				assertTrue("Pixel difference! Position " + "(" + x + "," + y + ") => " 
						+ " expected = " + this.resultMaxImage.getValueDouble(x, y) 
						+ " , found = " + testResultMaxImage.getValueDouble(x, y),
						Math.abs(this.resultMaxImage.getValueDouble(x, y) 
								- testResultMaxImage.getValueDouble(x, y)) < accuracyStandard);
			}			
		}
	}
}