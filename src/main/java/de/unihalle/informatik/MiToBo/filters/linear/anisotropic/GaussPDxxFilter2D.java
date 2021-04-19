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

import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.ExecutionMode;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.ALDDerivedClass;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException.OperatorExceptionType;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageDouble;
import de.unihalle.informatik.MiToBo.core.datatypes.wrapper.MTBBooleanData;

/**
 * 2D filter implementing second partial derivative of Gaussian in x.
 * <p>
 * This filter is often used in the context of segmenting vessel-like 
 * structures, e.g., refer to
 * <p>
 * Sofka and Stewart, "Retinal Vessel Centerline Extraction Using Multiscale
 * Matched Filters, Confidence and Edge Measures",<br> 
 * IEEE TMI, vol. 25, no. 12, pp. 1531-1546, December 2006
 * <p>
 * for an example of its application.  
 * 
 * @author Birgit Moeller
 */
@ALDDerivedClass
@ALDAOperator(genericExecutionMode=ExecutionMode.ALL, level=Level.APPLICATION)
public class GaussPDxxFilter2D extends OrientedFilter2D {

	/**
	 * Standard deviation of the Gaussian.
	 * <p>
	 * The mask width is derived from the standard deviation of the Gaussian,<br>
	 * i.e. the width is given by 
	 * {@latex.ilb %preamble{\\usepackage{amssymb, amsmath}}
	 * $w = 2 \\cdot 3 \\cdot (\\text{int})(\\sigma+0.5)+1$}.<br>
	 * Note that the minimum width is 3.
	 */
	@Parameter( label= "\u03C3 of Gaussian", required = false, dataIOOrder = 2,
		direction= Parameter.Direction.IN, mode=ExpertMode.STANDARD, 
	  description = "Std. deviation of Gaussian.")
	protected Double gaussStdDev = Double.valueOf(2.0);

	/**
	 * Height of the filter mask.
	 * <p>
	 * Note that the height of the mask must not be smaller than 3.
	 */
	@Parameter( label= "Mask Height", required = false, dataIOOrder = 3,
		direction= Parameter.Direction.IN, mode=ExpertMode.STANDARD, 
    description = "Height of the filter mask.")	
	protected Integer height = Integer.valueOf(9);

	/**
	 * Flag to invert filter mask.
	 */
	@Parameter( label= "Invert Mask", required = false, dataIOOrder = 5,
		direction= Parameter.Direction.IN, mode=ExpertMode.STANDARD, 
	  description = "If true, filter mask is inverted.")
	protected MTBBooleanData invertMask = new MTBBooleanData(false);

	/**
	 * Flag to normalize sum of kernel elements to zero.
	 */
	@Parameter( label= "Normalize Mask", required = false, dataIOOrder = 4,
		direction= Parameter.Direction.IN, mode=ExpertMode.STANDARD, 
	  description = "If true, mask is normalized to a sum of zero.")
	protected MTBBooleanData normalizeMask = new MTBBooleanData(true);

	/**
	 * Internal helper mask to mark valid/invalid kernel elements.
	 * <p>
	 * This variable is mainly used by the corresponding JUnit test class
	 * {@link TestGaussPDxxFilter2D}.
	 */
	private boolean[][] kernelMask;
	
	/**
	 * Default constructor.
	 */
	public GaussPDxxFilter2D() throws ALDOperatorException {
		super();
	}
	
	@Override
  public void validateCustom() throws ALDOperatorException {
		if (this.height.intValue() < 3) 
			throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED, 
				"[GaussPDxxFilter2D] mask height is smaller than 3, " +
					"please set it to a larger value!");
	}
	
	@Override
	public GaussPDxxFilter2D clone() {
		GaussPDxxFilter2D newOp;
		try {
			newOp = new GaussPDxxFilter2D();
			// super class fields
			newOp.inputImg = this.inputImg;
			newOp.angle = this.angle;
			newOp.mode = this.mode;
			newOp.paddingVariant = this.paddingVariant;
			newOp.statusListeners = this.statusListeners;
			// local fields
			newOp.gaussStdDev = this.gaussStdDev;
			newOp.height = this.height;
			newOp.invertMask = this.invertMask;
			newOp.normalizeMask = this.normalizeMask;
			return newOp;
		} catch (ALDOperatorException e) {
			return null;
		}
	}

	/**
	 * Calculates Gaussian derivative kernel for given parameters.
	 * @param _angle		Rotation angle in degrees.
	 * @return	Kernel image.
	 */
	@Override
  public MTBImageDouble getKernel(double _angle) {

		// standard deviation of Gaussian kernel
		double sigma = this.gaussStdDev.doubleValue();

		int kernelWidth = 2 * 3 * (int)(sigma + 0.5) + 1;
		// make sure that mask has at least a width of 3
		if (kernelWidth < 3)
			kernelWidth = 3;
		int kernelWidthHalf = kernelWidth / 2;
		int kernelHeight = this.height.intValue();
		int kernelHeightHalf = kernelHeight/2;
		
		int kernelSize = (kernelWidth > kernelHeight) ? kernelWidth : kernelHeight;
		int kernelSizeHalf = kernelSize / 2;
		double radAngle = _angle / 180.0 * Math.PI;
		
		double sigma_2 = sigma * sigma;
		double sigma_5 = Math.pow(sigma,5.0);
		
		MTBImageDouble kernelImg = (MTBImageDouble)MTBImage.createMTBImage(
				kernelSize, kernelSize, 1, 1, 1,	MTBImageType.MTB_DOUBLE);
		double kernelMean = 0.0, val = 0.0;
		int kernelCount = 0;
		this.kernelMask = new boolean[kernelSize][kernelSize];
		for (int y = -kernelSizeHalf; y<= kernelSizeHalf; ++y) {
			for (int x = -kernelSizeHalf; x<= kernelSizeHalf; ++x) {
				double x_orig = Math.cos(radAngle)*x + Math.sin(radAngle)*y;
				double y_orig = -Math.sin(radAngle)*x + Math.cos(radAngle)*y;
				// check if coordinates fit into original mask domain, if not then zero
				if (   Math.abs(y_orig) > kernelHeightHalf + 1.0e-10
						|| Math.abs(x_orig) > kernelWidthHalf + 1.0e-10) {
					kernelImg.putValueDouble(x+kernelSizeHalf, y+kernelSizeHalf, 0);
					this.kernelMask[x+kernelSizeHalf][y+kernelSizeHalf] = false;
				}
				else {
					val = (x_orig*x_orig - sigma_2) / (Math.sqrt(2*Math.PI) * sigma_5) 
									* Math.exp(-x_orig*x_orig / (2*sigma_2));
					if (this.invertMask.getValue())
						val = val * -1;
					kernelImg.putValueDouble(x+kernelSizeHalf, y+kernelSizeHalf, val);
					this.kernelMask[x+kernelSizeHalf][y+kernelSizeHalf] = true;
					kernelMean += val;
					++kernelCount;
				}
			}
		}
		// subtract mean value to normalize sum of kernel elements to zero
		if (this.normalizeMask.getValue()) {
			kernelMean = kernelMean / kernelCount;
			for (int x = 0; x < kernelSize; ++x) {
				for (int y = 0; y < kernelSize; ++y) {
					if (this.kernelMask[x][y])
						kernelImg.putValueDouble(x, y, 
								kernelImg.getValueDouble(x, y) - kernelMean);
				}
			}
		}
		return kernelImg;
	}
	
	/**
	 * Enable/disable inversion of mask.
	 * @param b		Flag for inversion.
	 */
	public void setInvertMask(boolean b) {
		this.invertMask = new MTBBooleanData(b);
	}

	/** 
	 * Enable or disable mask inversion.
	 * <p>
	 * Using this method with MiToBo wrapper datatypes instead of passing over
	 * directly a boolean preserves consistency in the processing history.
	 * 
	 * @param m		Value for the mask inversion flag.
	 */
	public void setInvertMask(MTBBooleanData m) {
		this.invertMask = m;
	}

	/**
	 * Enable kernel normalization.
	 */
	public void enableNormalization() {
		this.normalizeMask = new MTBBooleanData(true);
	}

	/**
	 * Disable kernel normalization.
	 */
	public void disableNormalization() {
		this.normalizeMask = new MTBBooleanData(false);
	}

	/** 
	 * Enable or disable kernel normalization.
	 * <p>
	 * Using this method with MiToBo wrapper datatypes instead of passing over
	 * directly a boolean preserves consistency in the processing history.
	 * 
	 * @param kn	Value for the kernel normalization flag.
	 */
	public void setKernelNormalization(MTBBooleanData kn) {
		this.normalizeMask = kn;
	}
	
	/**
	 * Specify standard deviation of Gaussian.
	 * @param s		Standard deviation sigma.
	 */
	public void setStandardDeviation(double s) {
		this.gaussStdDev = Double.valueOf(s);
	}

	/**
	 * Specify height of filter mask.
	 * @param h		Height of mask.
	 */
	public void setHeight(int h) {
		this.height = Integer.valueOf(h);
	}
	
	/**
	 * Get mask of valid kernel elements.
	 * <p>
	 * Intended to be used for unit testing only.
	 * @return	Kernel mask.
	 */
	protected boolean[][] getKernelMask() {
		return this.kernelMask;		
	}
}

