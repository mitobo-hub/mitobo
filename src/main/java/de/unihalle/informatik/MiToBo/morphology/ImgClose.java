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

/* 
 * Most recent change(s):
 * 
 * $Rev$
 * $Date$
 * $Author$
 * 
 */

package de.unihalle.informatik.MiToBo.morphology;

import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.Direction;
import de.unihalle.informatik.MiToBo.core.datatypes.images.*;
import de.unihalle.informatik.MiToBo.core.operator.*;

/**
 * This class implements morphological closing on 2D binary/grayscale images.
 * <p>
 * If the given image only contains two pixel values it is interpreted as 
 * binary image. In the resulting image the background pixels will be set 
 * to the smaller value, while the foreground pixels will be set to the 
 * larger ones.
 * <p> 
 * The structuring element is a square matrix of size 'masksize' x 'masksize', 
 * with reference pixel in the center of the matrix.
 *
 * Attention: if masksize is even, errors may result due 
 *            to lack of operator symmetry
 *
 * @author moeller
 */
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.ALL,
		level=Level.APPLICATION)
public class ImgClose extends MTBOperator {

	@Parameter( label= "Masksize", required = true, dataIOOrder = 1,
			direction= Direction.IN, description = "Masksize")
	private int masksize = 3;

	@Parameter( label= "Input image", required = true, dataIOOrder = 0,
			direction= Direction.IN, description = "Input image")
	private transient MTBImage inImg = null;

	@Parameter( label= "Result image", required = true,
			direction= Direction.OUT, description = "Result image")
	private transient MTBImage resultImg = null;

	/**
	 * Default constructor.
	 *  @throws ALDOperatorException
	 */
	public ImgClose() throws ALDOperatorException {
		// nothing to do here
	}

	/**
	 * Constructor. 
	 * 
	 * @param inimg	Input image.
	 * @param ms	Size of square mask.
	 * @throws ALDOperatorException
	 */
	public ImgClose(MTBImage inimg, int ms) throws ALDOperatorException {
		this.inImg= inimg;
		this.masksize= ms;
	}

	/**
	 * Returns the input image, null if not set.
	 */
	public MTBImage getInputImage() {
		return this.inImg;
	}

	/**
	 * Returns the given mask size, 0 if not set.
	 */
	public int getMasksize() {
		return this.masksize;
	}
		
	/**
	 * Returns the opened image, null if not available.
	 */
	public MTBImage getResultImage() {
		return this.resultImg;
	}
		
	/**
	 * Set result image.
	 */
	private void setResultImage(MTBImage result) {
		this.resultImg = result;
	}
		
	/**
	 * This method does the actual work. 
	 * @throws ALDOperatorException 
	 * @throws ALDProcessingDAGException 
	 */
	@Override
	protected void operate() 
		throws ALDOperatorException, ALDProcessingDAGException {
		ImgDilate dil= new ImgDilate(this.getInputImage(),this.getMasksize());
		dil.runOp(null);
		ImgErode ero= new ImgErode(dil.getResultImage(), this.getMasksize());
		ero.runOp(null);
		MTBImage closedImg= ero.getResultImage();
		this.setResultImage(closedImg);
	}
}
