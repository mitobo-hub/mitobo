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

import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.Direction;
import de.unihalle.informatik.MiToBo.core.datatypes.images.*;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.operator.*;

/**
 * This class implements the tophat operator.
 * <p>
 *  Attention: if masksize is even, errors may result due 
 *             to lack of operator symmetry
 *
 * @author moeller
 */
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.ALL)
public class ImgTophat extends MTBOperator {

	@Parameter( label= "Masksize", required = true, direction= Direction.IN, 
			description = "Masksize")
	private int masksize = 3;

	@Parameter( label= "Input image", required = true, direction= Direction.IN,
			description = "Input image")
	private transient MTBImage inImg = null;

	@Parameter( label= "processMode", required = false, direction= Direction.IN,
			description = "Process mode")
	private tophatMode processMode = tophatMode.GWM_TOPHAT_CONVEX;

	@Parameter( label= "Result image", required = true, direction= Direction.OUT, 
			description = "Result image")
	private transient MTBImage resultImg = null;

	/**
	 * Available operating modes.
	 * 
	 * @author moeller
	 */
	public static enum tophatMode {
		/**
		 * Top hat operator for bright 'mountains' on dark ground.
		 */
		GWM_TOPHAT_CONVEX,
		/**
		 * Top hat operator for dark 'valleys' in bright ground.
		 */
		GWM_TOPHAT_CONCAVE
	}
	
	/**
	 * Default constructor.
	 * @throws ALDOperatorException
	 */
	public ImgTophat() throws ALDOperatorException {
		// nothing to do here
	}

	/**
	 * Constructor with parameters. 
	 * 
	 * @param ip		Image to work on. 
	 * @param op		Operator mode.
	 * @param msize	Size of squared mask.
	 * @throws ALDOperatorException 
	 */
	public ImgTophat(MTBImage ip, tophatMode op, int msize) 
		throws ALDOperatorException {
			this.inImg= ip;
			this.processMode= op;
			this.masksize= msize;
	}

	/**
	 * Get reference to the current input image.
	 * 
	 * @return	Input image to work on.
	 */
	public MTBImage getInputImage() {
		return this.inImg;
	}
	/**
	 * Get the desired operator mode.
	 * 
	 * @return	Modus of operator.
	 * @throws ALDProcessingDAGException 
	 */
	public tophatMode getMode() {
		return this.processMode;
	}
	/**
	 * Get current masksize.
	 * 
	 * @return	Masksize of dilation mask.
	 */
	public int getMasksize() {
		return this.masksize;
	}
	/**
	 * Get the result image after applying the operator.
	 * Attention, reference might be null.
	 * 
	 * @return	Reference to result image.
	 */
	public MTBImage getResultImage() {
		return this.resultImg;
	}
	
	/**
	 * Set the result image.
	 * 
	 * @param rimage	Result image.
	 * @throws ALDOperatorException 
	 */
	public void setResultImage(MTBImage rimage) {
		this.resultImg= rimage;
	}

	/**
	 * MTB input image to work on.
	 */
	MTBImage mtbImg;
	
	@Override
	protected void operate() 
		throws ALDOperatorException, ALDProcessingDAGException {
		
		// convert the parameters
		this.mtbImg= this.getInputImage();

		// prepare result image
		MTBImage result= null;

		// get the desired operator mode
		switch(this.getMode())
		{
		case GWM_TOPHAT_CONVEX:
			result= this.tophat_convex(this.mtbImg, this.getMasksize());
			break;
		case GWM_TOPHAT_CONCAVE:
			result= this.tophat_concave(this.mtbImg, this.getMasksize());
			break;
		}
		// set result image
		this.setResultImage(result);
	}
	
	/**
	 * Apply a top hat operator to the image for convexity detection.
	 * <p>
	 * The operation is performed assuming a squared structural 
	 * element with specified mask size and grayscale entries of 0.
	 * 
	 * @param img	Input image.
	 * @param msize	Size of mask.
	 * @return	Detection result (positiv answers = white).
	 * @throws ALDOperatorException 
	 * @throws ALDProcessingDAGException 
	 */
	private MTBImage tophat_convex(MTBImage img, int msize) 
		throws ALDOperatorException, ALDProcessingDAGException {
		ImgOpen opener = new ImgOpen(img, msize);
		opener.runOp(null);
		MTBImage openedImg= opener.getResultImage();

		// init diff image
		MTBImageType intype= img.getType();
		MTBImage diffImg= MTBImage.createMTBImage(img.getSizeX(), 
							img.getSizeY(), 1, 1, 1, intype); 
		
		// calculate difference values
		for (int y=0;y<img.getSizeY();++y) {
			for (int x=0;x<img.getSizeX();++x) {
				diffImg.putValueInt(x, y, 
					img.getValueInt(x, y)- openedImg.getValueInt(x, y));
			}
		}
		return diffImg;
	}

	/**
	 * Apply a top hat operator to the image for concavity detection.
	 * <p>
	 * The operation is performed assuming a squared structural 
	 * element with specified mask size and grayscale entries of 0.
	 * 
	 * @param img	Input image.
	 * @param msize	Size of mask.
	 * @return	Detection result (positiv answers = white).
	 */
	private MTBImage tophat_concave(MTBImage img, int msize) 
	throws ALDOperatorException, ALDProcessingDAGException {
		ImgClose closer = new ImgClose(img, msize);
		closer.runOp(null);
		MTBImage closedImg= closer.getResultImage();
		
		// init diff image
		MTBImageType intype = img.getType();
		MTBImage diffImg= MTBImage.createMTBImage(img.getSizeX(), 
							img.getSizeY(), 1, 1, 1, intype);
		
		// calculate difference image
		for (int y=0;y<img.getSizeY();++y) {
			for (int x=0;x<img.getSizeX();++x) {
				diffImg.putValueInt(x, y, 
					closedImg.getValueInt(x, y) - img.getValueInt(x, y));
			}
		}
		return diffImg;
	}
}
