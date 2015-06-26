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

package de.unihalle.informatik.MiToBo.math.images;

import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo;
import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo.ExportPolicy;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;
import de.unihalle.informatik.MiToBo.math.images.ImageArithmetics.ArithOp;

/**
 * This class that provides convenience functions for the {@link ImageArithmetics} class.
 * Operations with one input image result in images of the same type!!
 * Operations with two input images result in an image of type of the higher input image precision!!
 * If two input images do not have the same size the resulting image is null.
 * Each operation is elementwise.
 * 
 * @author gress
 *
 */
@ALDMetaInfo(export=ExportPolicy.ALLOWED)
public class MTBImageArithmetics {


	private MTBOperator callingOperator;
	
	/** 
	 *  Constructor. (calling operator is null)
	 */
	public MTBImageArithmetics() throws ALDOperatorException {
		this.callingOperator = null;
	}

	/** 
	 *  Constructor with calling operator. You can specify the MTBOperator that calls the image arithmetic functions. This operator is 
	 *  passed to the internal image arithmetic operator class for history recording
	 */
	public MTBImageArithmetics(MTBOperator callingOperator) throws ALDOperatorException {
		this.callingOperator = callingOperator;
	}
	
	/** 
	 * Raise image values to the power of 'exponent'
	 * @param img input image
	 * @param exponent 
	 * @return result image or null if operation was not successful
	 */
	public MTBImage pow(MTBImage img, double exponent) {
		return runOperation(callingOperator, ArithOp.POW_CONST, img, exponent);
	}
	
	/**
	 * Add a constant to each image value
	 * @param img input image
	 * @param constant
	 * @return result image or null if operation was not successful
	 */
	public MTBImage add(MTBImage img, double constant) {
		return runOperation(callingOperator, ArithOp.ADD_CONST, img, constant);
	}
	
	/** 
	 * Multiply each image value by a constant
	 * @param img input image
	 * @param constant
	 * @return result image or null if operation was not successful
	 */
	public MTBImage mult(MTBImage img, double constant) {
		return runOperation(callingOperator, ArithOp.MULT_CONST, img, constant);
	}
	
	/** 
	 * Invert the image.
	 * @param img input image
	 * @return result image or null if operation was not successful
	 */
	public MTBImage inv(MTBImage img) {
		return runOperation(callingOperator, ArithOp.INV, img, 0);
	}

	/** 
	 * Absolute values of the image elements.
	 * @param img input image
	 * @return result image or null if operation was not successful
	 */
	public MTBImage abs(MTBImage img) {
		return runOperation(callingOperator, ArithOp.ABS, img, 0);
	}
	
	/**
	 * Add the elements two images
	 * @param img1
	 * @param img2
	 * @return result image or null if operation was not successful
	 */
	public MTBImage add(MTBImage img1, MTBImage img2) {
		return runOperation(callingOperator, ArithOp.ADD, img1, img2);
	}
	
	/**
	 * Subtract the elements two images (img1 - img2)
	 * @param img1
	 * @param img2
	 * @return result image or null if operation was not successful
	 */
	public MTBImage sub(MTBImage img1, MTBImage img2) {
		return runOperation(callingOperator, ArithOp.SUB, img1, img2);
	}	
	
	/**
	 * Multiply the elements of two images
	 * @param img1
	 * @param img2
	 * @return result image or null if operation was not successful
	 */
	public MTBImage mult(MTBImage img1, MTBImage img2) {
		return runOperation(callingOperator, ArithOp.MULT, img1, img2);
	}	
	
	/**
	 * Divide the elements of two images (img1/img2)
	 * @param img1
	 * @param img2
	 * @return result image or null if operation was not successful
	 */
	public MTBImage div(MTBImage img1, MTBImage img2) {
		return runOperation(callingOperator, ArithOp.DIV, img1, img2);
	}	
	
	/**
	 * Pixelwise minimum
	 * @param img1
	 * @param img2
	 * @return result image or null if operation was not successful
	 */
	public MTBImage min(MTBImage img1, MTBImage img2) {
		return runOperation(callingOperator, ArithOp.MIN, img1, img2);
	}	
	
	/**
	 * Pixelwise maximum
	 * @param img1
	 * @param img2
	 * @return result image or null if operation was not successful
	 */
	public MTBImage max(MTBImage img1, MTBImage img2) {
		return runOperation(callingOperator, ArithOp.MAX, img1, img2);
	}
	
	/**
	 * Bitwise logical AND of two images.
	 * @param img1	First input image.
	 * @param img2	Second input image.
	 * @return result image or null if operation was not successful
	 */
	public MTBImage and(MTBImage img1, MTBImage img2) {
		return runOperation(callingOperator, ArithOp.AND, img1, img2);
	}

	/**
	 * Bitwise logical OR of two images.
	 * @param img1	First input image.
	 * @param img2	Second input image.
	 * @return result image or null if operation was not successful
	 */
	public MTBImage or(MTBImage img1, MTBImage img2) {
		return runOperation(callingOperator, ArithOp.OR, img1, img2);
	}
	
	/**
	 * Absolute difference of two images.
	 * @param img1	First input image.
	 * @param img2	Second input image.
	 * @return result image or null if operation was not successful
	 */
	public MTBImage absDiff(MTBImage img1, MTBImage img2) {
		return runOperation(callingOperator, ArithOp.ABS_DIFF, img1, img2);
	}

	/**
	 * Run an operation with one input image and one input constant
	 * @param op
	 * @param img
	 * @param constant
	 * @return result image or null if operation was not successful
	 */
	private MTBImage runOperation(MTBOperator callingOperator, ArithOp op, MTBImage img, double constant) {
		MTBImage resultImg = null;

		ImageArithmetics ia;
		try {
			
			ia = new ImageArithmetics(op, img, constant);
			ia.runOp(null);
			resultImg = ia.getResultImg();
			
		} catch (ALDOperatorException e) {
			e.printStackTrace();
		} catch (ALDProcessingDAGException e) {
			e.printStackTrace();
		}
		
		return resultImg;
	}
	
	/**
	 * Run an operation with two input images
	 * @param callingOperator
	 * @param op
	 * @param img
	 * @param constant
	 * @return result image or null if operation was not successful
	 */
	private MTBImage runOperation(MTBOperator callingOperator, ArithOp op, MTBImage img1, MTBImage img2) {
		MTBImage resultImg = null;

		ImageArithmetics ia;
		try {
			
			ia = new ImageArithmetics(op, img1, img2);
			ia.runOp(null);
			resultImg = ia.getResultImg();
			
		} catch (ALDOperatorException e) {
			e.printStackTrace();
		} catch (ALDProcessingDAGException e) {
			e.printStackTrace();
		}
		
		return resultImg;
	}
	

	
}
