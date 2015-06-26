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

import ij.io.ImageWriter;

import java.io.IOException;

import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.formats.FormatException;

import org.junit.Before;
import org.junit.Test;

import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.operator.ALDOperator.HidingMode;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageByte;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.io.images.ImageReaderMTB;
import de.unihalle.informatik.MiToBo.io.images.ImageWriterMTB;
import de.unihalle.informatik.MiToBo.math.images.ImageArithmetics;
import de.unihalle.informatik.MiToBo.math.images.ImageArithmetics.ArithOp;
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
			// first test the label image
			
			// read the label image with holes
			reader = new ImageReaderMTB("./share/testimages/testLabelImage-withholes.tif");
			reader.runOp(true);
			holeImage = (MTBImageByte)reader.getResultMTBImage();

			// and fill the holes
			FillHoles2D filler = new FillHoles2D(holeImage);
			filler.runOp(HidingMode.HIDDEN);
			filledImage = filler.getResultImage();
			
			// now read the original label image without holes
			reader = new ImageReaderMTB("./share/testimages/testLabelImage.tif");
			reader.runOp(true);
			labelImage = (MTBImageByte)reader.getResultMTBImage();

			// and compare them
			MTBImageArithmetics absDiffOp = new MTBImageArithmetics();
			diffImage = absDiffOp.absDiff(filledImage, labelImage);

			int[] minMax = diffImage.getMinMaxInt();
			assertTrue("absolute difference of label image should be black", 		
					minMax[0] == 0 && minMax[1] == 0);
			
			// next test the binary image
			
			// read the label image with holes
			reader = new ImageReaderMTB("./share/testimages/testBinaryImage-withholes.tif");
			reader.runOp(true);
			holeImage = (MTBImageByte)reader.getResultMTBImage();

			// and fill the holes
			filler = new FillHoles2D(holeImage);
			filler.runOp(HidingMode.HIDDEN);
			filledImage = filler.getResultImage();
			
			// now read the original label image without holes
			reader = new ImageReaderMTB("./share/testimages/testBinaryImage.tif");
			reader.runOp(true);
			labelImage = (MTBImageByte)reader.getResultMTBImage();

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