/*
 * This file is part of Alida, a Java library for 
 * Advanced Library for Integrated Development of Data Analysis Applications.
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
 * Fore more information on Alida, visit
 *
 *    http://www.informatik.uni-halle.de/alida/
 *
 */

package de.unihalle.informatik.MiToBo.segmentation.regions.filling;

import static org.junit.Assert.*;

import org.junit.Test;

import de.unihalle.informatik.Alida.operator.ALDOperator.HidingMode;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.io.images.ImageReaderMTB;
import de.unihalle.informatik.MiToBo.math.images.MTBImageArithmetics;

/**
 * JUnit test class for {@link FillHoles2D}.
 * 
 * @author posch
 */
public class TestFillHoles2D {

	
	/**
	 * Test if each image gets a title upon writing.
	 */
	@Test
	public void testFillHoles2D() {
		MTBImage labelImage, holeImage, filledImage, diffImage;

		ImageReaderMTB reader;
		try {
			
			// read the label image with holes
			reader = new ImageReaderMTB(TestFillHoles2D.class.getResource(
					"testLabelImage-withholes.tif").getFile());
			reader.runOp(true);
			holeImage = reader.getResultMTBImage();

			// now read the original label image without holes
			reader = new ImageReaderMTB(TestFillHoles2D.class.getResource(
					"testLabelImage.tif").getFile());
			reader.runOp(true);
			labelImage = reader.getResultMTBImage();

			/**
			 * test the label image with correct 8-neighborhood
			 */

			// fill the holes
			FillHoles2D filler = new FillHoles2D(holeImage);
			filler.setUseDiagonalNeighbors(true);
			filler.runOp(HidingMode.HIDDEN);
			filledImage = filler.getResultImage();
			
			// and compare them
			MTBImageArithmetics absDiffOp = new MTBImageArithmetics();
			diffImage = absDiffOp.absDiff(filledImage, labelImage);

			int[] minMax = diffImage.getMinMaxInt();
			assertTrue("absolute difference of label image should be black", 		
					minMax[0] == 0 && minMax[1] == 0);
			
			/**
			 * test the label image with incorrect 8-neighborhood, test should fail
			 */
			
			// fill the holes
			filler = new FillHoles2D(holeImage);
			filler.setUseDiagonalNeighbors(false);
			filler.runOp(HidingMode.HIDDEN);
			filledImage = filler.getResultImage();
			
			// now read the original label image without holes
			reader = new ImageReaderMTB(TestFillHoles2D.class.getResource(
					"testLabelImage.tif").getFile());
			reader.runOp(true);
			labelImage = reader.getResultMTBImage();

			// and compare them; we expect deviations
			absDiffOp = new MTBImageArithmetics();
			diffImage = absDiffOp.absDiff(filledImage, labelImage);

			minMax = diffImage.getMinMaxInt();
			assertFalse("absolute difference of label images should not be zero", 		
					minMax[0] == 0 && minMax[1] == 0);

			
			// next test the binary image
			
			// read the label image with holes
			reader = new ImageReaderMTB(TestFillHoles2D.class.getResource(
					"testBinaryImage-withholes.tif").getFile());
			reader.runOp(true);
			holeImage = reader.getResultMTBImage();

			// and fill the holes
			filler = new FillHoles2D(holeImage);
			filler.runOp(HidingMode.HIDDEN);
			filledImage = filler.getResultImage();
			
			// now read the original label image without holes
			reader = new ImageReaderMTB(TestFillHoles2D.class.getResource(
					"testBinaryImage.tif").getFile());
			reader.runOp(true);
			labelImage = reader.getResultMTBImage();

			// and compare them
			absDiffOp = new MTBImageArithmetics();
			diffImage = absDiffOp.absDiff(filledImage, labelImage);
			
			minMax = diffImage.getMinMaxInt();
			assertTrue("absolute difference image of binary should be black", 		
					minMax[0] == 0 && minMax[1] == 0);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}