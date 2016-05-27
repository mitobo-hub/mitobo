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

import ij.plugin.filter.EDM;
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
 * Watershed transformation on binary images. 
 * <p>
 * This operator implements the watershed transformation on binary 
 * images. It is based on ImageJ's watershed implementation (menu item
 * Process -> Binary -> Watershed). More details about how the algorithm 
 * works can be found in the corresponding section of 
 * <a href="http://rsbweb.nih.gov/ij/docs/guide/146-29.html#sub:Watershed">
 * ImageJ's User Guide</a>.
 *
 * @author Birgit Moeller
 */
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.ALL,
		level=Level.APPLICATION, allowBatchMode=true)
public class WatershedBinary extends MTBOperator {

	/**
	 * Binary input image to segment.
	 */
	@Parameter( label= "Input Image", required = true, dataIOOrder = 0,
			direction = Parameter.Direction.IN, description = "Input image.")
	private transient MTBImageByte inImg = null;
	
	/**
	 * Segmented binary image.
	 */
	@Parameter( label= "Result Image", dataIOOrder = 0,
			direction = Parameter.Direction.OUT, description = "Result image.")
	private transient MTBImageByte resultImg = null;

	/**
	 * Default constructor.
	 *  @throws ALDOperatorException Thrown in case of failure.
	 */
	public WatershedBinary() throws ALDOperatorException {
		// nothing to do here
	}		

	/**
	 * Constructor. 
	 * 
	 * @param inimg		Input image.
	 * @throws ALDOperatorException Thrown in case of failure.
	 */
	public WatershedBinary(MTBImageByte inimg) 
			throws ALDOperatorException {
		this.inImg = inimg;
	}

	/**
	 * Returns the result image.
	 * @return Segmented result image.
	 */
	public MTBImageByte getResultImage() {
		return this.resultImg;
	}

	/* (non-Javadoc)
	 * @see de.unihalle.informatik.Alida.operator.ALDOperator#operate()
	 */
	@Override
	protected void operate() {
		
		// assuming a binary image with black foreground
		int width = this.inImg.getSizeX();
		int height = this.inImg.getSizeY();

		// transform input image to byte processor, invert image
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
		
		// apply the watershed transformation
		new EDM().toWatershed(bbP);
		
		// copy result to result image
		this.resultImg = (MTBImageByte)MTBImage.createMTBImage(
				width, height, 1, 1, 1, MTBImageType.MTB_BYTE);
		this.resultImg.fillBlack();
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				if (bbP.getPixel(x, y) == 0) {
					this.resultImg.putValueInt(x, y, 255);
				}
			}
		}
		this.resultImg.setTitle("Watershed result of image <" 
				+ this.inImg.getTitle() + ">");
	}
}
