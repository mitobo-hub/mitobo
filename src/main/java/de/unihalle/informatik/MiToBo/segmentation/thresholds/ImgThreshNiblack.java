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

package de.unihalle.informatik.MiToBo.segmentation.thresholds;

import java.awt.Rectangle;

import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.ExecutionMode;
import de.unihalle.informatik.Alida.annotations.Parameter.Direction;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.MiToBo.core.datatypes.images.*;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.operator.*;

import ij.*;
import ij.process.*;
import ij.gui.*;

/**
 * Image binarization based on local Niblack thresholding.
 * <p>
 * The implementation is based upon the following publications:
 * <li>
 * W. Niblack, <i>An Introduction to Digital Image Processing</i>, 
 * pp. 115-116, Prentice Hall, 1986.</li>
 * <li>
 * G. Leedham, C. Yan, et al., <i>Comparison of some thresholding 
 * algorithms for text/background segmentation in difficult document 
 * images</i>,<br> Proc. of the Seventh International Conference on 
 * Document Analysis and Recognition. Vol. 2. 2003</li>
 * <li>
 * Z. Zhang, C.L. Tan, <i>Recovery of Distorted Document Images from 
 * Bound Values</i>, Proc. of 6th International Conference on Document 
 * Analysis and Recognition, pp. 429-433, 2001.</li>
 * 
 * <p>
 * This class implements various versions of the Niblack thresholding 
 * approach. It calculates local thresholds according to the given input 
 * parameters and applies them to the given image. As result a binary 
 * image is returned.
 * <p>
 * Provided methods include conventional and enhanced Niblack 
 * thresholding, either applied in a sliding window manner or maskwise. 
 * In the latter case the windows are not overlapping. The maskwise 
 * application has particularly profen suitable for granule 
 * detection.<br>
 * The operator allows for activation of an additional local variance 
 * check. If activated, only windows are thresholded where the 
 * variance exceeds a certain threshold. All other windows are assumed 
 * to contain more or less homogeneous intensities and are classified as 
 * background.
 * <p>
 * In standard mode, as proposed by Niblack, the operator calculates a 
 * local threshold T based on the local mean m and standard deviation s 
 * in a local sliding window around pixel (x,y):
 * {@latex.ilb %preamble{\\usepackage{amssymb}}
 * 	\\begin{equation*}
 *  		T(x,y) &=& m(x,y) + k \\cdot s(x,y)
 *  \\end{equation*}}
 * k is a scaling constant which has to be chosen depending on the 
 * application at hand. As stated above, this mode can be combined with 
 * additional variance checks. 
 * <p>
 * In additon to the standard version of the Niblack approach the 
 * operator also implements an enhanced version as proposed by 
 * Zhang et al: 
 * {@latex.ilb %preamble{\\usepackage{amssymb}}
 * 	\\begin{equation*}
 *  		T(x,y) &=& m(x,y) \\cdot \\left( 1 + k \\cdot \\left(
 *  				1 - \\frac{s(x,y)}{R} \\right) \\right)
 *  \\end{equation*}}
 * This version can be selected in standard mode by setting the 
 * parameter R to a value different from -1. In the original paper 
 * default values of R = 100 and k = 0.1 are suggested. This mode can 
 * also be combined with local variance checks. 
 * 
 * @author moeller
 */
@ALDAOperator(genericExecutionMode=ExecutionMode.ALL)
public class ImgThreshNiblack extends MTBOperator {

	/**
	 * Input image.
	 */
	@Parameter( label= "Input image", required = true, 
			direction = Direction.IN, description = "Input image.")
	private transient MTBImage inImg = null;

	/**
	 * Binary result image.
	 */
	@Parameter( label= "Result image", required = true, 
			direction= Direction.OUT, description = "Output image.")
	private transient MTBImageByte outImg = null;

	/**
	 * Process mode.
	 */
	@Parameter( label= "Mode", required = true, direction = Direction.IN,
	    description = "Process Mode.")
	private Mode processMode = Mode.STD;

	/**
	 * Niblack scaling parameter k. 
	 */
	@Parameter( label= "K", direction = Direction.IN,
	    description = "Scaling factor K.", dataIOOrder = -8)
  private double scalingK = 0.0;

	/**
	 * Niblack factor R.
	 */
	@Parameter( label= "R", direction = Direction.IN,
	    description = "Enhancement factor R, disabled if -1.", 
	    dataIOOrder = -7)
  private double enhanceR = -1.0;

	/**
	 * Size of local sliding window.
	 */
	@Parameter( label= "Window size", direction = Direction.IN,
	    description = "Local window size.", dataIOOrder = -15)
  private int winSize = 25;

	/**
	 * Threshold for variance check.
	 * <p>
	 * Image areas with a variance below the threshold are classified as 
	 * background because the Niblack criterion will most likely fail.
	 * @see #varCheckNB
	 */
	@Parameter( label= "Variance threshold", direction = Direction.IN,
			description = "Variance check threshold", dataIOOrder = -4)
  private double varCheckThresh = 50.0;

	/**
	 * Size of neighborhood for local variance checks.
	 * @see #varCheckThresh
	 */
	@Parameter( label= "Variance check neighborhood", 
			direction = Direction.IN, dataIOOrder = -5,
			description = "Variance check neighborhood size.")
  private int varCheckNB = 10;

	/**
	 * Optional mask for excluding image regions from processing.
	 */
	@Parameter( label= "Exclude Mask", direction = Direction.IN, 
			required= false, description = "Exclude mask", dataIOOrder = -10)
  private MTBImageByte mask = null;

  /**
   * Default constructor.
   * @throws ALDOperatorException Thrown in case of failure.
   */
  public ImgThreshNiblack() throws ALDOperatorException {
	// nothing to do here
  }

  /**
   * Niblack processing mode.
   * @author moeller
   */
  public static enum Mode {
	/**
	 * Standard Niblack applying a sliding window.
	 */
	STD,

	/**
	 * Standard Niblack with sliding window and additional variance check.
	 */
	STD_LOCVARCHECK,

	/**
	 * Maskwise application of Niblack (non-overlapping).
	 */
	MASKWISE,

	/**
	 * Niblack calculation on the whole image (same as 'MASKWISE' with masksize
	 * = image_size).
	 */
	WHOLE_IMAGE
  }

  /**
   * Constructor. A new empty meta data object is initialized.
   * 
   * @param _inImg
   *          Image to work on.
   * @param mode
   *          Mode for Niblack operator.
   * @param k
   *          Scaling factor for standard deviation.
   * @param R
   *          If unequal to -1, enhanced Niblack is applied.
   * @param wSize
   *          Size of sliding window for local thresholding.
   * @param vcNB
   *          Size of neighborhood for local variance check.
   * @param vcThresh
   *          Threshold for local variance check.
   * @param _mask
   *          Image mask for excluding image sections.
   * @throws ALDOperatorException Thrown in case of failure.
   */
  public ImgThreshNiblack(MTBImage _inImg, Mode mode, double k, double R,
	  int wSize, int vcNB, double vcThresh, MTBImageByte _mask)
	  throws ALDOperatorException {
	super();
	this.inImg = _inImg;
	this.processMode = mode;
	this.scalingK = k;
	this.enhanceR = R;
	this.winSize = wSize;
	this.varCheckThresh = vcThresh;
	this.varCheckNB = vcNB;
	this.mask = _mask;
  }

  /**
   * Get the resulting thresholded image.
   * 
   * @return Binary image.
   */
  public MTBImageByte getResultImage() {
  	return this.outImg;
  }

  /**
   * Set input image.
   * @param img	Image to process.
   */
  public void setInputImage(MTBImage img) {
  	this.inImg = img;
  }

  @Override
  protected void operate() {

  	MTBImageByte result = null;
  	switch (this.processMode) {
  	case STD:
  		result = this.applyNiblackThreshold(this.inImg, this.winSize,
  				this.scalingK, this.enhanceR);
  		break;
  	case STD_LOCVARCHECK:
  		result = this.applyNiblackThreshold_withLocalVarCheck(this.inImg,
  				this.winSize, this.scalingK, this.enhanceR,
  				this.varCheckNB, this.varCheckThresh);
  		break;
  	case MASKWISE:
  		result = this.applyNiblackThresholdMaskwise(this.inImg,
  				this.winSize, this.scalingK, this.enhanceR);
  		break;
  	case WHOLE_IMAGE:
  		result = this.applyNiblackThresholdWholeImage(this.inImg, 
  				this.scalingK, this.enhanceR);
  		break;
  	}
  	this.outImg = result;
  }

  /**
   * This function implements the conventional (enhanced) Niblack binarization.
   * If R equals -1, the non-enhanced conventional variant is used.
   * 
   * @latex.block %preamble{\\usepackage{amssymb}} Niblack binarization:
   *              \\begin{eqnarray*} t &=& \\mu + k \\cdot ( \\mu \\cdot ( 1 -
   *              \\sigma / R ) ). \\\\ \\end{eqnarray*}
   * @param mimg	Input image to process.
   * @param w			Size of sliding window.
   * @param k			Niblack factor.
   * @param R			Niblack normalization constant.
   * @return Niblack thresholded image.
   */
  private MTBImageByte applyNiblackThreshold(MTBImage mimg, int w, 
  		double k, double R) {

  	int width = mimg.getSizeX();
  	int height = mimg.getSizeY();

  	// allocate result image
  	MTBImageByte result = (MTBImageByte) MTBImage.createMTBImage(
  			width, height, 1, 1, 1, MTBImageType.MTB_BYTE);

  	int wSize_2 = (int) (w / 2.0);

  	for (int y = 0; y < height; ++y) {
  		for (int x = 0; x < width; ++x) {

  			if (this.mask == null || this.mask.getValueInt(x, y) > 0) {

  				// global analysis
  				int minX = (x - wSize_2 < 1) ? 1 : x - wSize_2;
  				int maxX = 
  						(x + wSize_2 > width - 1) ? width - 1 : x + wSize_2;
  				int minY = (y - wSize_2 < 1) ? 1 : y - wSize_2;
  				int maxY = 
  						(y + wSize_2 > height - 1) ? height - 1 : y + wSize_2;

  				// calc mean and variance
  				double mean = 0;
  				double var = 0;
  				int counter = 0;
  				for (int yy = minY; yy < maxY; ++yy) {
  					for (int xx = minX; xx < maxX; ++xx) {
  						mean = mean + mimg.getValueDouble(xx, yy);
  						counter++;
  					}
  				}
  				mean = mean / counter;
  				for (int yy = minY; yy < maxY; ++yy) {
  					for (int xx = minX; xx < maxX; ++xx) {
  						var = var + (mimg.getValueDouble(xx, yy) - mean)
  								* (mimg.getValueDouble(xx, yy) - mean);
  					}
  				}
  				var = var / counter;

  				double t = -1;
  				if (R != -1) {
  					t = (mean + k * mean * (1 - Math.sqrt(var) / R));
  				} else {
  					t = (mean + k * Math.sqrt(var));
  				}
  				if (mimg.getValueInt(x, y) < t) {
  					result.putValueInt(x, y, 0);
  				} else {
  					result.putValueInt(x, y, 255);
  				}
  			} else {
  				result.putValueInt(x, y, 0);
  			}
  		}
  	}
  	return result;
  }

  /**
   * Enhanced Niblack binarization applied maskwise to the image, i.e. all
   * pixels inside the mask get the same threshold.
   * 
   * The threshold is calculated as {@latex.ilb %preamble
   * \\usepackage{amsmath}}\\begin{equation*} t = \\mu + k * ( \\mu \\cdot ( 1 -
   * \\sigma / R ) ), \\end{equation*}} if R does not equal -1. Otherwise, the
   * conventional variant {@latex.ilb %preamble \\usepackage{amsmath}}
   * \\begin{equation*} t = \\mu + k \\cdot \\sigma \\end{equation*}} is used.
   * 
   * The main difference is here, that compared to the original approach,
   * thresholds are not calculated pixelwise, but just maskwise. In detail, the
   * mask of given size w is shifted to non-overlapping locations, for each
   * location a threshold is calculated and then applied to all pixels within
   * the mask.
   * 
   * NOTE: The parameter mask is ignored for the moment!
   * 
   * @TODO // todo Consider mask in calculations!
   */
  private MTBImageByte applyNiblackThresholdMaskwise(MTBImage mimg, int w,
	  double k, double R) {

	int width = mimg.getSizeX();
	int height = mimg.getSizeY();

	// allocate result image
	MTBImageByte result = (MTBImageByte) MTBImage.createMTBImage(
			width, height, 1, 1, 1, MTBImageType.MTB_BYTE);

	// here it is easier to use ImagePlus
	// ==> due to ROI handling!
	ImagePlus inputImg = mimg.getImagePlus();
	ImageProcessor ip = inputImg.getProcessor();

	// run with sliding window over the whole image
	int pos = 0;
	for (int y = 0; y <= height - w; y += w) {
	  for (int x = 0; x <= width - w; x += w) {

		// current window area
		ip.setRoi(x, y, w, w);
		Rectangle roi = ip.getRoi();
		byte[] pixels = (byte[]) ip.getPixels();

		double mean = 0;
		double var = 0;
		int counter = 0;

		// calculate mean intensity value within window
		for (int i = roi.y; i < roi.y + roi.height; i++) {
		  for (int j = roi.x; j < roi.x + roi.width; j++) {
			pos = i * width + j;
			mean = mean + (pixels[pos] & 0xffff);
			counter++;
		  }
		}
		mean = mean / counter;

		// calculate intensity variance within window
		for (int i = roi.y; i < roi.y + roi.height; i++) {
		  for (int j = roi.x; j < roi.x + roi.width; j++) {
			pos = i * width + j;
			var = var + ((pixels[pos] & 0xffff) - mean)
			    * ((pixels[pos] & 0xffff) - mean);
		  }
		}
		var = var / counter;

		// local Niblack threshold
		double t = 0;
		if (R == -1)
		  t = mean + k * var;
		else
		  t = mean + k * (mean * (1 - var / R));

		// binarize the image
		for (int i = roi.y; i < roi.y + roi.height; i++) {
		  for (int j = roi.x; j < roi.x + roi.width; j++) {
			pos = i * width + j;
			if ((pixels[pos] & 0xffff) >= t && var > 40 * 40) {
			  result.putValueInt(j, i, 255);
			} else {
			  result.putValueInt(j, i, 0);
			}
		  }
		}
	  }
	}
	return result;
  }

  /**
   * This function implements the conventional Niblack binarization, 
   * enhanced with a local variance check.
   * 
   * If parameter R does not equal -1 the method applies the threshold
   * {@latex.inline %preamble \\usepackage{amsmath} 
   * \\begin{equation*} 
   * 		t = \\mu + k * ( \\mu \\cdot ( 1 - \\sigma / R ) )
   * \\end{equation*}}. 
   * If R equals -1, the conventional variant is used, i.e., 
   * {@latex.inline %preamble \\usepackage{amsmath}
   * \\begin{equation*} 
   * 		t = \\mu + k \\cdot \\sigma 
   * \\end{equation*}}.
   * In addition, a local variance check in a 7x7-windows around each 
   * pixel takes place. Only if the local variance exceeds a certain 
   * threshold the Niblack threshold is considered, otherwise the pixel 
   * is set to zero, i.e. background.
   * 
   * This function is specifically dedicated to image with large 
   * homogeneous regions where standard Niblack thresholding results in 
   * errorneous detection results.
   */
  private MTBImageByte applyNiblackThreshold_withLocalVarCheck(
  		MTBImage mimg, int w, double k, double R, int varCheckNeighborhood,
  		double localVarThresh) {

  	int width = mimg.getSizeX();
  	int height = mimg.getSizeY();

  	// allocate result image
  	MTBImageByte result = (MTBImageByte) MTBImage.createMTBImage(
  			width, height, 1, 1, 1, MTBImageType.MTB_BYTE);

  	// calculate half of window size
  	int wSize_2 = (int) (w / 2.0);

  	for (int y = 0; y < height; ++y) {
  		for (int x = 0; x < width; ++x) {
  			if (this.mask == null || this.mask.getValueInt(x, y) > 0) {

  				// global analysis
  				int minX = (x - wSize_2 < 1) ? 1 : x - wSize_2;
  				int maxX = 
  					(x + wSize_2 > width - 1) ? width - 1 : x + wSize_2;
  				int minY = (y - wSize_2 < 1) ? 1 : y - wSize_2;
  				int maxY = 
  					(y + wSize_2 > height - 1) ? height - 1 : y + wSize_2;

  				// calc mean and variance
  				double mean = 0;
  				double var = 0;
  				int counter = 0;
  				for (int yy = minY; yy < maxY; ++yy) {
  					for (int xx = minX; xx < maxX; ++xx) {
  						mean = mean + (mimg.getValueDouble(xx, yy));
  						counter++;
  					}
  				}
  				mean = mean / counter;
  				for (int yy = minY; yy < maxY; ++yy) {
  					for (int xx = minX; xx < maxX; ++xx) {
  						var = var + ((mimg.getValueDouble(xx, yy)) - mean)
  								* ((mimg.getValueDouble(xx, yy)) - mean);
  					}
  				}
  				var = var / counter;

  				// local variance check - self-made
  				int maskSize_2 = varCheckNeighborhood / 2;
  				minX = (x - maskSize_2 < 1) ? 1 : x - maskSize_2;
  				maxX = 
  					(x + maskSize_2 > width - 1) ? width - 1 : x + maskSize_2;
  				minY = (y - maskSize_2 < 1) ? 1 : y - maskSize_2;
  				maxY = 
  					(y + maskSize_2 > height - 1) ? height - 1 : y + maskSize_2;

  				// calc mean and variance
  				double meanLocal = 0;
  				counter = 0;
  				for (int xx = minX; xx < maxX; ++xx) {
  					for (int yy = minY; yy < maxY; ++yy) {
  						meanLocal = meanLocal + (mimg.getValueDouble(xx, yy));
  						counter++;
  					}
  				}
  				meanLocal = meanLocal / counter;

  				if (  Math.abs(((mimg.getValueDouble(x, y)) - meanLocal)) 
  						> localVarThresh) {
  					double t = -1;
  					if (R != -1)
  						t = (mean + k * mean * (1 - Math.sqrt(var) / R));
  					else
  						t = (mean + k * Math.sqrt(var));

  					if (mimg.getValueDouble(x, y) < t) {
  						result.putValueInt(x, y, 0);
  					} else {
  						result.putValueInt(x, y, 255);
  					}
  				} else {
  					result.putValueInt(x, y, 0);
  				}
  			} else {
  				result.putValueInt(x, y, 0);
  			}
  		}
  	}
  	return result;
  }

  @Deprecated
  public ImagePlus applyNiblackThresholdWholeImage(ImageProcessor ip, double k,
	  double R, ImagePlus _mask) {
	ImagePlus input = NewImage.createShortImage("", ip.getWidth(), ip
	    .getHeight(), 1, NewImage.FILL_BLACK);
	ImageProcessor inputIP = input.getProcessor();
	inputIP.copyBits(ip, 0, 0, Blitter.COPY);
	MTBImage minput = MTBImage.createMTBImage(input);
	MTBImageByte byteMask = null;
	if (_mask != null) {
	  MTBImage m = MTBImage.createMTBImage(_mask);
	  byteMask = (MTBImageByte) m;
	}
	this.mask = byteMask;
	MTBImageByte res = this.applyNiblackThresholdWholeImage(minput, k, R);
	return res.getImagePlus();
  }

  /**
   * This function implements the conventional (enhanced) Niblack binarization.
   * If R equals -1, the non-enhanced conventional variant is used. Here no
   * sliding window is required since the whole image area is taken as input
   * image region. Consequently this method does a global binarization applying
   * a single threshold on each image pixel.
   * 
   * @latex.block %preamble{\\usepackage{amssymb}} Niblack binarization:
   *              \\begin{eqnarray*} t &=& \\mu + k \\cdot ( \\mu \\cdot ( 1 -
   *              \\sigma / R ) ). \\\\ \\end{eqnarray*}
   * @param ip
   *          input image processor
   * @param k
   *          Niblack factor
   * @param R
   *          Niblack normalization constant
   * @return Niblack thresholded image.
   */
  private MTBImageByte applyNiblackThresholdWholeImage(MTBImage mimg, 
  		double k, double R) {

  	int width = mimg.getSizeX();
  	int height = mimg.getSizeY();

  	// allocate result image
  	MTBImageByte result = (MTBImageByte) MTBImage.createMTBImage(
  			width, height, 1, 1, 1, MTBImageType.MTB_BYTE);

  	// calculate the global threshold from mean and variance
  	double mean = 0;
  	double var = 0;
  	int counter = 0;

  	for (int yy = 0; yy < height; ++yy) {
  		for (int xx = 0; xx < width; ++xx) {

  			// check if pixel is valid
  			if (this.mask == null || this.mask.getValueInt(xx, yy) > 0) {
  				mean = mean + (mimg.getValueDouble(xx, yy));
  				counter++;
  			}
  		}
  	}
  	mean = mean / counter;
  	for (int yy = 0; yy < height; ++yy) {
  		for (int xx = 0; xx < width; ++xx) {

  			// check if pixel is valid
  			if (this.mask == null || this.mask.getValueInt(xx, yy) > 0) {
  				var = var + (mimg.getValueDouble(xx, yy) - mean)
  						* (mimg.getValueDouble(xx, yy) - mean);
  			}
  		}
  	}
  	var = var / counter;

  	// get threshold
  	double t = -1;
  	if (R != -1) {
  		t = (mean + k * mean * (1 - Math.sqrt(var) / R));
  	} else {
  		t = (mean + k * Math.sqrt(var));
  	}
  	for (int y = 0; y < height; ++y) {
  		for (int x = 0; x < width; ++x) {

  			if (this.mask == null || this.mask.getValueInt(x, y) > 0) {

  				if (mimg.getValueInt(x, y) < t) {
  					result.putValueInt(x, y, 0);
  				} else {
  					result.putValueInt(x, y, 255);
  				}
  			} else {
  				result.putValueInt(x, y, 0);
  			}
  		}
  	}
  	return result;
  }
}
