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

import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageByte;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;

/**
 * Hysteresis thresholding on gray-scale images.
 * <p>
 * This thresholding scheme applies two thresholds {@latex.inline $t_{l}$} and
 * {@latex.inline $t_{h}$} to the image. All pixels having an intensity value
 * equal or larger than {@latex.inline $t_{h}$} are assigned to the foreground. 
 * In addition, also pixels with an intensity value equal or higher to the lower
 * threshold are included in the foreground if they transitively link to a pixel 
 * with an intensity value equal or larger than {@latex.inline $t_{h}$}. 
 * <p>
 * This thresholding heuristic is, e.g., used in the Canny edge detector.
 * 
 * @author moeller
 */
@ALDAOperator(genericExecutionMode = ALDAOperator.ExecutionMode.ALL,
	level = Level.APPLICATION)
public class HysteresisThresholding extends MTBOperator {

	/**
	 * Image to process.
	 */
	@Parameter(label = "Input Image", required = true, dataIOOrder = 0,
		direction = Parameter.Direction.IN, description = "Input image.")
	protected transient MTBImage inImg = null;

	/**
	 * Upper threshold.
	 */
	@Parameter(label = "Upper Threshold", required = true, dataIOOrder = 1,
		direction = Parameter.Direction.IN, description = "Higher threshold.")
	protected Double threshHigh = new Double(0);

	/**
	 * Lower threshold.
	 */
	@Parameter(label = "Lower Threshold", required = true, dataIOOrder = 2,
		direction = Parameter.Direction.IN, description = "Lower threshold.")
	protected Double threshLow = new Double(0);

	/**
	 * Resulting binarized image.
	 */
	@Parameter(label = "Thresholded Image",	direction = Parameter.Direction.OUT, 
		description = "Thresholded binary image.")
	protected transient MTBImageByte resultImage = null;

	/**
	 * Standard constructor. A new empty operator object is initialized.
	 */
	public HysteresisThresholding() throws ALDOperatorException {
		// nothing to do here
	}

	/* (non-Javadoc)
	 * @see de.unihalle.informatik.Alida.operator.ALDOperator#operate()
	 */
	@Override
	protected void operate() {
		
		// define some local variables
		int width = this.inImg.getSizeX();
		int height = this.inImg.getSizeY();
		double th = this.threshHigh.doubleValue();
		double tl = this.threshLow.doubleValue();

		// allocate result image and init with zeros
		this.resultImage = (MTBImageByte)MTBImage.createMTBImage(
			width, height, 1, 1, 1, MTBImageType.MTB_BYTE);
		boolean[][] procMap = new boolean[height][width];
		// map to store which pixels have been processed already
		for (int y=0; y<height; ++y) {
			for (int x=0; x<width; ++x) {
				procMap[y][x] = false;
			}
		}
		
		// do hysteresis thresholding
		for (int y=0; y<height; ++y) {
			for (int x=0; x<width; ++x) {
				if (this.inImg.getValueDouble(x,y) >= th) {
					this.resultImage.putValueInt(x, y, 255);
					// recursively label neighbours
					labelNeighbors(this.inImg, this.resultImage, procMap, x, y, tl, th);
				}
				else if (this.inImg.getValueDouble(x, y) < tl) {
					this.resultImage.putValueInt(x, y, 0);	
				}
			}
		}
	}

	/**
	 * Function for recursive labeling of foreground pixels.
	 * <p>
	 * Refer to the hysteresis threshold algorithm for details.
	 * 
	 * @param img				Input image.
	 * @param result		Binary result image.
	 * @param procMap		Map to remember pixels already processed.
	 * @param x					Current x-position.
	 * @param y					Current y-position.
	 * @param tl				Lower threshold.
	 * @param th				Higher threshold.
	 */
	protected static void labelNeighbors(MTBImage img, MTBImage result,
			boolean[][] procMap, 	int x, int y, double tl, double th) {

		// get size of image
		int width= img.getSizeX();
		int height= img.getSizeY();
		
		// explore neighborhood
		for (int dy=-1;dy<=1;++dy) {
			for (int dx=-1;dx<=1;++dx) {
				int nx= x+dx;
				int ny= y+dy;
				
				if (nx<0 || nx>=width || ny<0 || ny>= height) 
					continue;
				
				if (   !procMap[ny][nx] 
						&& img.getValueDouble(nx,ny) >= tl) {
					result.putValueInt(nx, ny, 255);
					procMap[ny][nx] = true;
					labelNeighbors(img, result, procMap, nx, ny, tl, th);
				}
			}
		}
	}
	
	/**
	 * Set input image.
	 * @param image		Image to process.
	 */
	public void setInputImage(MTBImage image) {
		this.inImg = image;
	}
	
	/**
	 * Specify lower threshold.
	 * @param d		Threshold value.
	 */
	public void setLowerThreshold(double d) {
		this.threshLow = new Double(d);
	}
	
	/**
	 * Specify higher threshold.
	 * @param d		Threshold value.
	 */
	public void setHigherThreshold(double d) {
		this.threshHigh = new Double(d);
	}

	/**
	 * Returns the result image.
	 * @return	Binary result image.
	 */
	public MTBImageByte getResultImage() {
		return this.resultImage;
	}
}
