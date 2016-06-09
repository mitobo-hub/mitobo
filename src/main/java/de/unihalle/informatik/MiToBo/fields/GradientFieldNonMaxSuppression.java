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

package de.unihalle.informatik.MiToBo.fields;

import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageDouble;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;

/**
 * Non-Maximum-Suppression and (optional) hysteresis thresholding on 
 * gradient fields.
 * <p>
 * The input image is expected to include at least two channels, the
 * first one with the components of the gradient field in x-direction 
 * and the second one with the components of the field in y-direction.
 * <p>
 * The operator first extracts pixel-wise gradient magnitudes and 
 * directions from the given input image channels and then removes all 
 * non-maximal gradients, e.g. sets the corresponding pixel positions
 * in the result magnitude image to zero. Note that the gradient 
 * direction is discritized into intervals of 45 degrees for this 
 * procedure.
 * <p>
 * After non-maximum suppression an optional hysteresis thresholding
 * may be applied which removes all pixels with gradient magnitudes
 * below a lower threshold or lying between the upper and lower 
 * threshold but not being linked by a path to a pixel exceeding the
 * upper threshold.
 * <p> 
 * As result a gradient magnitude image is returned which only contains
 * values different from zero at positions with locally maximal 
 * gradients and which survived the (optional) hysteresis thresholding
 * step.
 * 
 * @author Birgit Moeller
 */
@ALDAOperator(genericExecutionMode = ALDAOperator.ExecutionMode.ALL,
	level = Level.STANDARD, allowBatchMode = true)
public class GradientFieldNonMaxSuppression extends MTBOperator {

	/**
	 * Image with input 2D gradient field.
	 */
	@Parameter(label = "Gradient Vector Field Image", required = true,
			direction = Parameter.Direction.IN, dataIOOrder = 0,
			description = "Gradient field image.")
	private transient MTBImage vectorFieldImage = null;

	/**
	 * Flag to enable/disable additional hysteresis thresholding.
	 */
	@Parameter(label = "Do hysteresis thresholding?", required = true,
			direction = Parameter.Direction.IN, dataIOOrder = 1,
			description = "Enable/disable additional hysteresis thresholding.")
	private boolean doHysteresisThresholding = false;

	/**
	 * Resulting thinned (and thresholded) magnitude image.
	 */
	@Parameter(label = "Thinned and thresholded magnitude image.",  
			direction = Parameter.Direction.OUT, 
			description = "Resulting magnitude image")
	private transient MTBImageDouble resultImage = null;

	/**
	 * Standard constructor. A new empty operator object is initialized.
	 * @throws ALDOperatorException Thrown in case of failure.
	 */
	public GradientFieldNonMaxSuppression() throws ALDOperatorException {
		// nothing to do here
	}

	/* (non-Javadoc)
	 * @see de.unihalle.informatik.Alida.operator.ALDOperator#operate()
	 */
	@Override
	protected void operate() {
		MTBImageDouble maxSuppressedImage= this.doNMS();
		if (this.doHysteresisThresholding) {
			this.resultImage= this.doHS(maxSuppressedImage);
		}
		else {
			this.resultImage = maxSuppressedImage;
		}
		String title = "Non-Maximum-Suppression result for <" 
				+ this.vectorFieldImage.getTitle() + ">";
		if (this.doHysteresisThresholding)
			title += " (incl. hysteresis thresholding)";
		this.resultImage.setTitle(title);
	}

	/**
	 * Applies non-maximum-suppression to the input data.
	 * @return	Filtered image with gradient magnitudes.
	 */
	protected MTBImageDouble doNMS() {

		// get size of field and image, respectively
		int width= this.vectorFieldImage.getSizeX();
		int height= this.vectorFieldImage.getSizeY();
		
		MTBImageDouble gradMagImage = 
			(MTBImageDouble)MTBImage.createMTBImage(width, height, 1, 1, 1, 
					MTBImageType.MTB_DOUBLE);
		double xVal, yVal, mag, dir;
		for (int y=0; y<height; ++y) {
			for (int x=0; x<width; ++x) {
				xVal = this.vectorFieldImage.getValueDouble(x, y, 0, 0, 0);
				yVal = this.vectorFieldImage.getValueDouble(x, y, 0, 0, 1);
				mag = Math.sqrt(xVal * xVal + yVal * yVal);
				gradMagImage.putValueDouble(x, y, mag);
			}
		}
		MTBImageDouble dirImage = (MTBImageDouble)MTBImage.createMTBImage(
				width, height, 1, 1, 1, MTBImageType.MTB_DOUBLE);
		for (int y=0; y<height; ++y) {
			for (int x=0; x<width; ++x) {
				xVal = this.vectorFieldImage.getValueDouble(x, y, 0, 0, 0);
				yVal = this.vectorFieldImage.getValueDouble(x, y, 0, 0, 1);
				dir = (yVal >= 0.0 ? 
								Math.atan2(yVal, xVal) 
							: (2 * Math.PI + Math.atan2(yVal, xVal)));
				dirImage.putValueDouble(x, y, dir);
			}
		}

		// allocate result image
		MTBImageDouble magImage = (MTBImageDouble)MTBImage.createMTBImage(
				width, height, 1, 1, 1, MTBImageType.MTB_DOUBLE);
		double next, prev;
		for (int y=1; y<height-1; ++y) {
			for (int x=1; x<width-1; ++x) {
				
				double magnitude = gradMagImage.getValueDouble(x,y);
				double rad = dirImage.getValueDouble(x, y);
				
				// convert angle to degrees
				double angle = 180.0 / Math.PI * rad;
				
				// check neighbor magnitudes in gradient direction
				next= 0;
				prev= 0;
				if (angle <= 22.5 || angle > 337.5) { 
					next= gradMagImage.getValueDouble(x+1,y);
					prev= gradMagImage.getValueDouble(x-1,y);
				}
				if (angle <= 67.5 && angle > 22.5) { 
					next= gradMagImage.getValueDouble(x+1,y+1);
					prev= gradMagImage.getValueDouble(x-1,y-1);
				}
				if (angle <= 112.5 && angle > 67.5) {
					next= gradMagImage.getValueDouble(x,y+1);
					prev= gradMagImage.getValueDouble(x,y-1);
				}
				if (angle <= 157.5 && angle > 112.5) { 
					next= gradMagImage.getValueDouble(x-1,y+1);
					prev= gradMagImage.getValueDouble(x+1,y-1);
				}
				if (angle <= 202.5 && angle > 157.5) {
					next= gradMagImage.getValueDouble(x-1,y);
					prev= gradMagImage.getValueDouble(x+1,y);
				}
				if (angle <= 247.5 && angle > 202.5) {
					next= gradMagImage.getValueDouble(x-1,y-1);
					prev= gradMagImage.getValueDouble(x+1,y+1);
				}
				if (angle <= 292.5 && angle > 247.5) {
					next= gradMagImage.getValueDouble(x,y-1);
					prev= gradMagImage.getValueDouble(x,y+1);
				}
				if (angle <= 337.5 && angle > 292.5) { 
					next= gradMagImage.getValueDouble(x+1,y-1);
					prev= gradMagImage.getValueDouble(x-1,y+1);
				}
				
				if (magnitude <= next || magnitude < prev)
					magImage.putValueDouble(x, y, 0.0);
				else
					magImage.putValueDouble(x, y, magnitude);
			}
		}
		return magImage;
	}
	
	/**
	 * Does the hysteresis thresholding.
	 * @param magImg	Image with gradient magnitudes.
	 * @return	Result image.
	 */
	protected MTBImageDouble doHS(MTBImageDouble magImg) {
		
		int width= magImg.getSizeX();
		int height= magImg.getSizeY();
		
		// allocate result image and init with zeros
		MTBImageDouble hsImage = (MTBImageDouble)MTBImage.createMTBImage(
				width, height, 1, 1, 1, MTBImageType.MTB_DOUBLE);
		hsImage.fillBlack();

		// get real image maximum
		double max= 0;
		for (int y=1; y<height-1; ++y) {
			for (int x=1; x<width-1; ++x) {
				if (magImg.getValueDouble(x,y)>max)
					max= magImg.getValueDouble(x,y);
			}
		}
		
		// calculate thresholds
		double highThresh= max * 0.1;
		double lowThresh= 0.3 * highThresh;
		
		// do hysterese thresholding
		for (int y=1; y<height-1; ++y) {
			for (int x=1; x<width-1; ++x) {
				if (magImg.getValueDouble(x,y) >= highThresh) {
					hsImage.putValueDouble(x,y,magImg.getValueDouble(x,y));
					// recursively label neighbours
					GradientFieldNonMaxSuppression.labelNeighbors(magImg, hsImage, x, y, 
							lowThresh, highThresh);
				}
				else if (magImg.getValueDouble(x, y) <= lowThresh) {
					hsImage.putValueDouble(x,y,0);	
				}
			}
		}
		for (int y=1; y<height-1; ++y) {
			for (int x=1; x<width-1; ++x) {
				if (magImg.getValueDouble(x,y) >= highThresh)
					hsImage.putValueDouble(x,y,magImg.getValueDouble(x,y));
				else if (magImg.getValueDouble(x,y) <= lowThresh) {
					hsImage.putValueDouble(x,y,0);	
				}
			}
		}
		return hsImage;
	}

	/**
	 * Function for recursive labeling of contour pixels.
	 * <p>
	 * Refer to the hysteresis threshold algorithm for details.
	 * 
	 * @param magImg	Image with gradient magnitudes.
	 * @param hsImg		Current hysteresis filtered image.
	 * @param x				Current x-position.
	 * @param y				Current y-position.
	 * @param lt			Lower threshold.
	 * @param ht			Higher threshold.
	 */
	protected static void labelNeighbors(MTBImage magImg, MTBImage hsImg,
			int x, int y, double lt, double ht) {

		// get size of image
		int width= magImg.getSizeX();
		int height= magImg.getSizeY();
		
		// explore neighborhood
		for (int dy=-1;dy<=1;++dy) {
			for (int dx=-1;dx<=1;++dx) {
				int nx= x+dx;
				int ny= y+dy;
				
				if (nx<0 || nx>=width || ny<0 || ny>= height) 
					continue;
				
				if (   hsImg.getValueDouble(nx,ny) == 0 
						&& magImg.getValueDouble(nx,ny) > lt) {
					hsImg.putValueDouble(nx,ny,magImg.getValueDouble(nx,ny));
					GradientFieldNonMaxSuppression.labelNeighbors(
							magImg, hsImg, nx, ny, lt, ht);
				}
			}
		}
	}
	
	/**
	 * Returns the result image.
	 * @return	Result image with thinned gradient magnitudes.
	 */
	public MTBImageDouble getResultImage() {
		return this.resultImage;
	}
}
