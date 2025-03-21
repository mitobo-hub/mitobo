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

package de.unihalle.informatik.MiToBo.morphology;

import ij.process.BinaryProcessor;
import ij.process.ByteProcessor;

import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.MiToBo.core.datatypes.images.*;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.operator.*;

/**
 * Extracts skeleton of foreground region. 
 * <p>
 * The foreground of the given image is defined by all pixels having
 * values larger than zero.
 * <p>
 * Note that this operator directly uses ImageJ 1.x functionality! In ImageJ
 * the skeletonization is implemented based on this paper: 
 * <p>
 * <b>Zhang TY, Suen CY (1984) <i>A fast parallel algorithm for thinning digital
 * patterns</i>. Commun ACM 27: 236–239</b>
 * <p>
 * For more details take a look at Section 29.8.9 on this page:<br>
 * <a href="https://imagej.nih.gov/ij/docs/guide/146-29.html#toc-Subsection-29.8">
 * 		https://imagej.nih.gov/ij/docs/guide/146-29.html#toc-Subsection-29.8</a>
 * 
 * @author moeller
 */
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.ALL,
		level=Level.APPLICATION)
public class SkeletonExtractor extends MTBOperator {

	/**
	 * Binary input image, pixels with value 0 are interpreted as background.
	 */
	@Parameter( label= "Input Image", required = true, dataIOOrder = 0,
			direction = Parameter.Direction.IN, description = "Input image.")
	private transient MTBImage inImg = null;

	/**
	 * Binary result image, skeleton in white, background in black.
	 */
	@Parameter( label= "Result Image", required = true,
			direction = Parameter.Direction.OUT, description = "Result image.")
	private transient MTBImageByte resultImg = null;

	/**
	 * Default constructor.
	 * @throws ALDOperatorException Thrown in case of failure.
	 */
	public SkeletonExtractor() throws ALDOperatorException {
		// nothing to do here
	}		

	/**
	 * Set input image to process.
	 * @param img Input image to process.
	 */
	public void setInputImage(MTBImage img) {
		this.inImg = img;
	}

	/**
	 * Returns the input image, null if not set.
	 * @return Input image.
	 */
	public MTBImage getInputImage() {
		return this.inImg;
	}

	/**
	 * Returns the skeleton image.
	 * @return Binary skeleton image.
	 */
	public MTBImageByte getResultImage() {
		return this.resultImg;
	}

	/**
	 * This method does the actual work.
	 */
	@Override
	protected void operate() {

		int width = this.inImg.getSizeX();
		int height = this.inImg.getSizeY();

		// transform input image to byte processor, image is inverted;
		// note that image is padded by one row/column on each side to compensate
		// ImageJ ignoring the outmost rows and columns on all sides
		ByteProcessor bP = new ByteProcessor(width+2, height+2);
		for (int x = 0; x < width+2; x++) {
			bP.putPixel(x, 0, 0);
			bP.putPixel(x, height+1, 0);
		}
		for (int y = 0; y < height+2; y++) {
			bP.putPixel(0, y, 0);
			bP.putPixel(width+1, y, 0);
		}
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				if (this.inImg.getValueInt(x, y) > 0)
					bP.putPixel(x+1, y+1, 0);
				else
					bP.putPixel(x+1, y+1, 255);
			}
		}
		BinaryProcessor bbP = new BinaryProcessor(bP);
		
		// create the skeleton using ImageJ's skeletonize() function
		bbP.skeletonize();

		// create skeleton image of type MTBImageByte
		this.resultImg = (MTBImageByte) MTBImage.createMTBImage(
				width, height, 1, 1, 1, MTBImageType.MTB_BYTE);
		this.resultImg.fillBlack();
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				if (bbP.getPixel(x+1, y+1) == 0) {
					this.resultImg.putValueInt(x, y, 255);
				}
			}
		}
	}
}
