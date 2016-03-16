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

package de.unihalle.informatik.MiToBo.segmentation.basics;

import java.util.Vector;

import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.ExecutionMode;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.Direction;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;

/**
 * Operator for doing statistical calculations on segmentations of images.
 * <p>
 * The segmentations and input images processed by this operator can have 
 * either 2 or 3 dimensions. In addition, multiple channels are supported.
 * In case of time series images being handed over to this operator only the
 * image for the first point in time is processed.  
 * <p> 
 * Note: 
 * It is assumed that the given segmentation covers at least the domain of the 
 * input image. There are no checks performed if this is really true!
 * 
 * @author moeller
 */
@ALDAOperator(genericExecutionMode=ExecutionMode.ALL)
public class CalcSegmentationStatistics extends MTBOperator {

	/**
	 * Statistical numbers provided by this class.
	 */
	public static enum CalcTargets {
		/**
		 * Size of individual regions.
		 */
		classSize,
		/**
		 * Average intensity values of all regions.
		 * <p>
		 * For a single region R the average intensity is calculated as follows:
		 * {@latex.ilb %preamble{\\usepackage{amssymb,amsmath}}
		 *      \\begin{equation*}
		 *      \\mu(R) = \\frac{1}{\\sum_{(x,y,z) \\in R} w(x,y,z)}
		 *      					\\sum_{(x,y,z) \\in R} w(x,y,z) \\cdot I(x,y,z) 
		 *      \\end{equation*}}
		 * w is a function assigning an individual weight to each pixel.
		 */
		classMean,
		/**
		 * Intensity variance within each region.
		 * <p>
		 * A region's intensity variance is calculated as follows:
		 * {@latex.ilb %preamble{\\usepackage{amssymb,amsmath}}
		 *      \\begin{equation*}
		 *      \\sigma(R) = \\frac{1}{\\sum_{(x,y,z) \\in R} w(x,y,z)}
		 *   					\\sum_{(x,y,z) \\in R} w(x,y,z) \\cdot (I(x,y,z)-\\mu(R))^2  
		 *      \\end{equation*}}
		 * w is a function assigning an individual weight to each pixel.
		 */
		classVar		
	}
	
  /**
   * Input image.
   */
  @Parameter(label="Input Image", direction = Direction.IN, required = true,
 		description = "Input image to work on.")
  private MTBImage image = null;

	/**
	 * Segmentation object.
	 */
	@Parameter(label="Segmentation", direction = Direction.IN,
		required = true, description = "Segmentation of image.")
  private MTBSegmentationInterface segmentation = null;

  /**
   * List of targets to calculate.
   */
  @Parameter(label="Calculation Targets", direction = Direction.IN,
  	required = true, description="List of targets to calculate.")
  private Vector<CalcTargets>  targetList = null;

  /**
   * List of calculated region sizes.
   */
  @Parameter(label="Region Sizes", direction = Direction.OUT,
  	description = "Calculated region sizes.")
  private int [] regionSizes = null;

  /**
   * List of calculated region average values.
   * <p>
   * The first dimension of the array covers the channel.
   */
  @Parameter(label="Region Means", direction = Direction.OUT,
  	description = "Calculated region averages.")
  private double [][] regionMeans = null;

  /**
   * List of calculated region variance values.
   * <p>
   * The first dimension of the array covers the channel.
   */
  @Parameter(label="Region Variances", direction = Direction.OUT,
  	description = "Calculated region variances.")
  private double [][] regionVars = null;

  /**
	 * Width of working image.
	 */
	private int iWidth;
	
	/**
	 * Height of working image. 
	 */
	private int iHeight;
	
	/**
	 * Depth of working image. 
	 */
	private int iDepth;

	/**
	 * Number of channels of working image. 
	 */
	private int iChannels;

	/**
   * Default constructor without arguments.
   * <p>
   * Note, there are parameters that need to be set prior to executing the
   * operator. Don't call runOp() before doing that!
   */
  public CalcSegmentationStatistics()
  	throws ALDOperatorException {
  	// nothing to do here...
  }

  /**
   * Default constructor.
   * 
   * @param im	Image to work on.
   */
  public CalcSegmentationStatistics(MTBImage im)
  	throws ALDOperatorException {
  	this.image= im;
  	this.iDepth= im.getSizeZ();
  	this.iHeight= im.getSizeY();
  	this.iWidth= im.getSizeX();
  	this.iChannels= im.getSizeC();
  }

  /**
   * Default constructor.
   * 
   * @param im	Image to work on.
   * @param seg Segmentation.
   */
  public CalcSegmentationStatistics(MTBImage im, MTBSegmentationInterface seg)
  	throws ALDOperatorException {
  	this.image= im;
  	this.iDepth= im.getSizeZ();
  	this.iHeight= im.getSizeY();
  	this.iWidth= im.getSizeX();
  	this.iChannels= im.getSizeC();
  	this.segmentation= seg;
  }
  
  /**
   * Default constructor.
   * 
   * @param im	Image to work on.
   * @param seg Image segmentation.
   * @param targets List of statistical numbers to calculate per region.
   */
  public CalcSegmentationStatistics(MTBImage im, MTBSegmentationInterface seg,
  																 Vector<CalcTargets> targets) 
  	throws ALDOperatorException {
  	this.image= im;
  	this.iDepth= im.getSizeZ();
  	this.iHeight= im.getSizeY();
  	this.iWidth= im.getSizeX();
  	this.iChannels= im.getSizeC();
  	this.segmentation= seg;
  	this.targetList= targets;
  }
  
  /**
   * Set the input image.
   * @param img	Image to process.
   */
  public void setInputImage(MTBImage img) {
  	this.image = img;
  	this.iWidth = img.getSizeX();
  	this.iHeight = img.getSizeY();
  	this.iDepth = img.getSizeZ();
  	this.iChannels = img.getSizeC();
  }
  
  /**
   * Specifiy image segmentation.
   */
  public void setSegmentation(MTBSegmentationInterface seg) {
  	this.segmentation = seg;
  }

  /**
   * Specify targets to calculate.
   */
  public void setTargets(Vector<CalcTargets> targets) {
  	this.targetList = targets;
  }

  /**
   * Get array of region areas indexed with class labels for first channel.
   */
  public int [] getRegionSizes() {
  	return this.regionSizes;
  }
  
  /**
   * Get average intensity array indexed with class labels for first channel.
   */
  public double [] getRegionMeans() {
  	return this.regionMeans[0];
  }
  
  /**
   * Get average intensity array indexed with class labels for all channels.
   * <p>
   * The first dimension of the array covers the channel.
   */
  public double [][] getRegionMeansAllChannels() {
  	return this.regionMeans;
  }

  /**
   * Get intensity variance array indexed with class labels for first channel.
   * <p>
   * The first dimension of the array covers the channel.
   */
  public double [] getRegionVars() {
  	return this.regionVars[0];
  }
  
  /**
   * Get intensity variance array indexed with class labels for all channels.
   * <p>
   * The first dimension of the array covers the channel.
   */
  public double [][] getRegionVarsAllChannels() {
  	return this.regionVars;
  }

  @Override
  protected void operate() {
		
		if (this.targetList.isEmpty())
			return;
		
		// update size of internal variables
		int regionNum = this.segmentation.getNumberOfClasses();
		int maxLabel = this.segmentation.getMaxLabel();
		int arraySize = (regionNum == maxLabel + 1) ? regionNum : maxLabel + 1;
		
		this.regionSizes = new int[arraySize];
		this.regionMeans = new double [this.iChannels][arraySize];
		this.regionVars = new double [this.iChannels][arraySize];
		double [] weightSum;
		
		// calculate average intensities and sizes based on visible pixels
		if (   this.targetList.contains(CalcTargets.classSize) 
				|| this.targetList.contains(CalcTargets.classMean)
				|| this.targetList.contains(CalcTargets.classVar)) {
			
			for (int c = 0; c < this.iChannels; ++c) {
				weightSum = new double [arraySize];
				for (int z = 0; z < this.iDepth; ++z) {
					for (int y = 0; y < this.iHeight; ++y) {
						for (int x = 0; x < this.iWidth; ++x) {

							if (!this.segmentation.isVisible(x, y, z))
								continue;

							int classLabel = this.segmentation.getClass(x, y, z);
							weightSum[classLabel] += this.segmentation.getWeight(x, y, z);
							this.regionMeans[c][classLabel] += 
								this.image.getValueInt(x, y, z, 0, c)  
									* this.segmentation.getWeight(x, y, z);
							// extract region sizes, but only for one channel
							if (c == 0)
								this.regionSizes[classLabel]++;
						}
					}
				}
				// compute final parameters
				for (int l = 0; l<arraySize; ++l) {
					if (weightSum[l] > 0)
						this.regionMeans[c][l] = this.regionMeans[c][l]/weightSum[l];
				}
			}
		}

		// calculate intensity variance of visible pixels
		if (this.targetList.contains(CalcTargets.classVar)) {

			for (int c = 0; c < this.iChannels; ++c) {
				weightSum = new double[arraySize];
				for (int z = 0; z < this.iDepth; ++z) {
					for (int y = 0; y < this.iHeight; ++y) {
						for (int x = 0; x < this.iWidth; ++x) {

							if (!this.segmentation.isVisible(x, y, z))
								continue;

							int classLabel = this.segmentation.getClass(x, y, z);
							weightSum[classLabel] += this.segmentation.getWeight(x, y, z);
							this.regionVars[c][classLabel] += 
								this.segmentation.getWeight(x, y, z) *
	 						 		(this.image.getValueInt(x, y, z, 0, c) 
	 						 				- this.regionMeans[c][classLabel])
	 						 	 *(this.image.getValueInt(x, y, z, 0, c) 
	 						 			  - this.regionMeans[c][classLabel]);
						}
					}
				}
				// compute final parameters
				for (int l = 0; l<arraySize; ++l) {
					if (weightSum[l] > 0)
						this.regionVars[c][l] = this.regionVars[c][l]/weightSum[l];
				}
			}
		}
  }	  
}
