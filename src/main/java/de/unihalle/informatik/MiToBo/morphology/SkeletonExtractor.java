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

import java.util.Hashtable;
import java.util.Set;

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
 * Note that this operator directly uses ImageJ 1.x functionality!
 * 
 * @author moeller
 */
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.ALL,
		level=Level.APPLICATION)
public class SkeletonExtractor extends MTBOperator {

	@Parameter( label= "Input Image", required = true, dataIOOrder = 0,
			direction = Parameter.Direction.IN, description = "Input image")
	private transient MTBImage inImg = null;

	@Parameter( label= "Result Image", required = true,
			direction = Parameter.Direction.OUT, description = "Result image")
	private transient MTBImageByte resultImg = null;

	/**
	 * Default constructor.
	 *  @throws ALDOperatorException
	 */
	public SkeletonExtractor() throws ALDOperatorException {
		// nothing to do here
	}		

	/**
	 * Set input image to process.
	 */
	public void setInputImage(MTBImage img) {
		this.inImg = img;
	}

	/**
	 * Returns the input image, null if not set.
	 */
	public MTBImage getInputImage() {
		return this.inImg;
	}

	/**
	 * Returns the skeleton image.
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

		// transform input image to byte processor, image is inverted
		ByteProcessor bP = new ByteProcessor(width, height);
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				if (this.inImg.getValueInt(x, y) > 0)
					bP.putPixel(x, y, 0);
				else
					bP.putPixel(x, y, 255);
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
				if (bbP.getPixel(x, y) == 0) {
					this.resultImg.putValueInt(x, y, 255);
				}
			}
		}
	}
}
