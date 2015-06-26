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

package de.unihalle.informatik.MiToBo.tools.image;

import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException.OperatorExceptionType;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;

/**
 * Contrast-reduction of images by reducing number of gray-scale values.
 * <p>
 * This operator performs a reduction of the contrast in an image by reducing
 * the number of gray-scale values to, e.g., 4-bit or 2-bit. Different modes
 * for choosing the gray values in the result image are available. Currently
 * only byte and short gray-scale images are handled, passing color images 
 * or images of another type to the operator results in an error.
 * <p>
 * Note that the result image always shares the type of the input image, 
 * irrespective of the actual number of bits used for representing the values. 
 * In addition, it is not guaranteed that the whole range of available gray 
 * values is used. 
 * 
 * @author moeller
 */
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.ALL,
	level=Level.STANDARD, allowBatchMode=true)
public class ImageContrastReducer extends MTBOperator {

	/**
	 * Available target contrast modes.
	 */
	public static enum TargetContrast {
		/**
		 * 8-bit mode, result image contains a maximum of 256 different gray values. 
		 */
		BIT_8,
		/**
		 * 6-bit mode, result image contains a maximum of 64 different gray values. 
		 */
		BIT_6,
		/**
		 * 4-bit mode, result image contains a maximum of 16 different gray values. 
		 */
		BIT_4,
		/**
		 * 2-bit mode, result image contains a maximum of 4 different gray values. 
		 */
		BIT_2
	}
	
	/**
	 * Modes for choosing result values.
	 */
	public static enum ResultValueMode {
		/**
		 * Result values are given by center values of quantization intervals. 
		 */
		INTERVAL_CENTER,
		/**
		 * Result values are given by max values of quantization intervals. 
		 */
		INTERVAL_MAX,
		/**
		 * Result values are given by min values of quantization intervals. 
		 */
		INTERVAL_MIN,
		/**
		 * Result values are given values from 0 to number of gray values. 
		 */
		ZERO_TO_N
	}
	
	/**
	 * Input image.
	 */
	@Parameter( label = "Input Image", required = true, 
		direction = Parameter.Direction.IN, mode = ExpertMode.STANDARD, 
		dataIOOrder = 0, description = "Input image to transform.")
	protected MTBImage inImg = null;

	/**
	 * Target contrast of result image.
	 */
	@Parameter( label = "Target Contrast", required = true, 
		direction = Parameter.Direction.IN, mode = ExpertMode.STANDARD, 
		dataIOOrder = 1, description = "Target contrast, i.e., number of bits.")
	protected TargetContrast targetContrast = TargetContrast.BIT_4;

	/**
	 * Result value mode.
	 */
	@Parameter( label = "Result Value Mode", required = true, 
		direction = Parameter.Direction.IN, mode = ExpertMode.STANDARD, 
		dataIOOrder = 2, description = "Mode for selecting the result values.")
	protected ResultValueMode resultValueMode = ResultValueMode.INTERVAL_CENTER;

	/**
	 * Result image.
	 */
	@Parameter( label= "Result Image", required = true, 
		direction = Parameter.Direction.OUT, mode=ExpertMode.STANDARD, 
		dataIOOrder = 0, description = "Result image.")
	protected MTBImage resultImg = null;

	/**
	 * Default constructor. 
	 */
	public ImageContrastReducer() throws ALDOperatorException {
		// nothing to do here
	}

	@Override
	public void validateCustom() throws ALDOperatorException {
		if (this.inImg.getType().equals(MTBImageType.MTB_RGB))
			throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED,
				"[ImageContrastReducer] color images currently not supported!");
		if (this.inImg.getType().equals(MTBImageType.MTB_INT))
			throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED,
				"[ImageContrastReducer] integer images currently not supported!");
		if (	 this.inImg.getType().equals(MTBImageType.MTB_FLOAT)
				|| this.inImg.getType().equals(MTBImageType.MTB_DOUBLE))
			throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED,
				"[ImageContrastReducer] images with rational values " 
					+ "currently not supported!");
	}
	
	@SuppressWarnings("unused")
  @Override
	protected void operate() throws ALDOperatorException {

		// init some local variables
		int width = this.inImg.getSizeX();
		int height = this.inImg.getSizeY();
		
		// handle different kinds of images
		int divideBy = 0;
		if (this.inImg.getType().equals(MTBImageType.MTB_BYTE)) {

			if (this.targetContrast.equals(TargetContrast.BIT_8)) {
				// nothing to do, target contrast already given
				this.resultImg = this.inImg.duplicate();
				return;
			}

			this.resultImg = this.inImg.duplicate();
			if (this.targetContrast.equals(TargetContrast.BIT_6))
				divideBy = 4;
			else if (this.targetContrast.equals(TargetContrast.BIT_4))
				divideBy = 16;
			else if (this.targetContrast.equals(TargetContrast.BIT_2))
				divideBy = 64;
		}
		else if (this.inImg.getType().equals(MTBImageType.MTB_SHORT)) {
			this.resultImg = MTBImage.createMTBImage(
					width, height, 1, 1, 1, MTBImageType.MTB_SHORT);
			if (this.targetContrast.equals(TargetContrast.BIT_8))
				divideBy = 256;
			else if (this.targetContrast.equals(TargetContrast.BIT_6))
				divideBy = 1024;
			else if (this.targetContrast.equals(TargetContrast.BIT_4))
				divideBy = 4096;
			else if (this.targetContrast.equals(TargetContrast.BIT_2))
				divideBy = 16384;
		}
			
		switch(this.resultValueMode)
		{
		case INTERVAL_MIN:
			for (int y=0; y<height;++y) {
				for (int x=0; x<width; ++x) {
					this.resultImg.putValueInt(x, y, 
							(this.inImg.getValueInt(x, y)/divideBy)*divideBy);
				}
			} 
			break;
		case INTERVAL_MAX:
			for (int y=0; y<height;++y) {
				for (int x=0; x<width; ++x) {
					this.resultImg.putValueInt(x, y, 
							(this.inImg.getValueInt(x, y)/divideBy+1)*(divideBy)-1);
				}
			} 
			break;
		case INTERVAL_CENTER:
			for (int y=0; y<height;++y) {
				for (int x=0; x<width; ++x) {
					this.resultImg.putValueInt(x, y, 
							(this.inImg.getValueInt(x, y)/divideBy)*divideBy 
							+ (int)(divideBy/2.0));
				}
			} 
			break;
		case ZERO_TO_N:
			for (int y=0; y<height;++y) {
				for (int x=0; x<width; ++x) {
					this.resultImg.putValueInt(x, y, 
							this.inImg.getValueInt(x, y)/divideBy);
				}
			}
			break;
		}
	}

	/** 
	 * Set input image.
	 * @param img		Input image to process.
	 */
	public void setInImg(MTBImage img) {
		this.inImg = img;
	}

	/** 
	 * Get input image.
	 * @return Current input image.
	 */
	public MTBImage getInImg() {
		return this.inImg;
	}

	/** 
	 * Set target contrast.
	 * @param tc	Target contrast.
	 */
	public void setTargetContrast(TargetContrast tc) {
		this.targetContrast = tc;
	}

	/** 
	 * Set value selection mode.
	 * @param vsm		Mode for selecting result values.
	 */
	public void setResultValueSelectionMode(ResultValueMode vsm) {
		this.resultValueMode = vsm;
	}

	/**
	 * Get result image.
	 * @return Result image with reduced contrast.
	 */
	public MTBImage getResultImg() {
		return this.resultImg;
	}
}
