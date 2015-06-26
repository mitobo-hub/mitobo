/*
 * This file is part of MiToBo, the Microscope Image Analysis Toolbox.
 *
 * Copyright (C) 2011
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

package de.unihalle.informatik.MiToBo.segmentation.activecontours.energies;

import java.util.Vector;

import de.unihalle.informatik.Alida.annotations.ALDClassParameter;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.Alida.exceptions.ALDException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.segmentation.activecontours.exceptions.MTBActiveContourException;
import de.unihalle.informatik.MiToBo.segmentation.activecontours.exceptions.MTBActiveContourException.ExceptionType;
import de.unihalle.informatik.MiToBo.segmentation.basics.MTBSegmentationInterface;
import de.unihalle.informatik.MiToBo.segmentation.basics.CalcSegmentationStatistics;
import de.unihalle.informatik.MiToBo.segmentation.basics.CalcSegmentationStatistics.CalcTargets;

/**
 * Active contour energy implementing Chan-Vese region fitting for 
 * gray-scale and vector-valued, i.e. multi-channel, images.
 * <p>
 * This energy models the inner and outer regions of a single contour by
 * a Gaussian model, i.e. each of the two regions is characterized by a 
 * mean intensity value, and deviations from this value are penalized. 
 * The energy is defined as follows:
 * {@latex.ilb %preamble{\\usepackage{amssymb}}
 *      \\begin{equation*}
 *      E(C) = \\int_{inside(C)} \\frac{1}{N} \\sum_{i=1}^N 
 *      	\\lambda_{in,i} \\cdot (I_i(x,y,z) - c_{in,i})^2 dx dy
 *       	   + \\int_{outside(C)} \\frac{1}{N} \\sum_{i=1}^N
 *        \\lambda_{out,i} \\cdot (I_i(x,y,z) - c_{out,i})^2 dx dy
 *      \\end{equation*}}
 * 
 * <p>
 * Note that class 0 is interpreted as background while class 1 is taken 
 * as foreground or contour interior. If there are additional labels 
 * present in the given segmentation they are ignored.
 * <p>
 * Papers: 
 * <ul>
 * <li> Chan and Vese, <i>Active Contours Without Edges</i>,
 * IEEE Transactions on Image Processing, vol. 10, no. 2, pp. 266-277, 
 * 2001.
 * <li> Chan, Sandberg and Vese, <i>Active Contours without Edges for 
 * Vector-Valued Images</i>, Journal of Visual Communications and Image
 * Representation, vol. 11, pp. 130-141, 2000.
 * </ul>
 * 
 * @author moeller
 */
public class MTBActiveContourEnergy_CVRegionFit {

	/**
	 * Image to work on.
	 */
	@ALDClassParameter(label="Input Image", mode=ExpertMode.ADVANCED,
		dataIOOrder=-10)
	protected transient MTBImage inImg;
	
	/**
	 * Weighting factors for inner region fit, one for each channel.
	 */
	@ALDClassParameter(label="Interior Lambdas per Channel",
		mode=ExpertMode.STANDARD, dataIOOrder=-5)
	protected double[] lambda_in = new double[]{0.5};

	/**
	 * Weighting factors for outer region fit, one for each channel.
	 */
	@ALDClassParameter(label="Exterior Lambdas per Channel",
		mode=ExpertMode.STANDARD,	dataIOOrder=-4)
	protected double[] lambda_out = new double[]{0.5};
	
	/**
	 * Operator to calculate energy parameters, i.e., some statistical 
	 * numbers on the regions of the segmentation.
	 */
	private transient CalcSegmentationStatistics imgStatsOp = null;
	
	/**
   * Region sizes, entry with index 0 is assumed to refer to background.
   */
  protected transient int [] areas = null;

  /**
   * Average intensities per region, first index refers to channel.
   */
  protected transient double [][] means = null;
  
  /**
   * Number of channels of the image. 
   */
  protected transient int iChannels;

  /**
   * Default constructor.
   */
  public MTBActiveContourEnergy_CVRegionFit() {
  	// nothing to do here
  }
  
  /**
   * Default constructor with arguments.
   * 
   * @param im	Image to work on.
   * @param l_in	Weighting factors for inner region fit.
   * @param l_out	Weighting factors for outer region fit.
   * @throws ALDOperatorException 
   */
  public MTBActiveContourEnergy_CVRegionFit(MTBImage im,
  			double[] lin, double[] lout) {
    this.inImg = im;
  	this.lambda_in= lin;
  	this.lambda_out= lout;
  }

  /**
   * Method to properly initialize instances of this class.
   * @throws ALDOperatorException
   */
  protected void init() throws MTBActiveContourException {
  	// do some savety checks
		if (this.inImg == null)
			throw new MTBActiveContourException(
				ExceptionType.INITIALIZATION_ERROR,
					"[MTBActiveContourEnergy_CVRegionFit] init() - " + 
						"no image given!");
		this.iChannels = this.inImg.getSizeC();
		// if no lambdas are given, init them with all channels weighted 
		// equally
		if (   this.lambda_in == null 
				|| this.lambda_in.length < this.iChannels) {
			this.lambda_in = new double[this.iChannels];
			for (int c=0; c<this.iChannels; ++c)
				this.lambda_in[c] = 1.0/this.iChannels;
		}
		else {
			// check if all values are larger than zero
			for (int c=0; c<this.iChannels; ++c)
				if (this.lambda_in[c] < 0)
					throw new MTBActiveContourException(
						ExceptionType.INITIALIZATION_ERROR,
							"[MTBActiveContourEnergy_CVRegionFit] init() - " + 
								"at least one lambda parameter is smaller than zero!");
		}
		if (   this.lambda_out == null 
				|| this.lambda_out.length < this.iChannels) {
				this.lambda_out = new double[this.iChannels];
			for (int c=0; c<this.iChannels; ++c)
				this.lambda_out[c] = 1.0/this.iChannels;
		}
		else {
			// check if all values are larger than zero
			for (int c=0; c<this.iChannels; ++c)
				if (this.lambda_out[c] < 0)
					throw new MTBActiveContourException(
						ExceptionType.INITIALIZATION_ERROR,
							"[MTBActiveContourEnergy_CVRegionFit] init() - " + 
								"at least one lambda parameter is smaller than zero!");
		}
  	// init the operator for calculating segmentation statistics
  	try {
	    this.imgStatsOp = new CalcSegmentationStatistics(this.inImg);
    } catch (ALDOperatorException e) {
			throw new MTBActiveContourException(
				ExceptionType.INITIALIZATION_ERROR,
					"[MTBActiveContourEnergy_CVRegionFit] init() - " + 
							"could not calculate segmentation statistics! " + 
									e.getCommentString());
    }
  	Vector<CalcTargets> toCalc= 
  		new Vector<CalcSegmentationStatistics.CalcTargets>();
  	toCalc.add(CalcTargets.classMean);
  	this.imgStatsOp.setTargets(toCalc);
  }
  
  /**
   * Updates parameters according to given membership.
   * @param mem		Current segmentation object.
   */
  protected void updateParameters(MTBSegmentationInterface mem) 
  	throws MTBActiveContourException {
  	
  	// update the energy parameter
  	try {
  		this.imgStatsOp.setSegmentation(mem);
	    this.imgStatsOp.runOp(true);
	    this.areas = this.imgStatsOp.getRegionSizes();
	  	this.means = this.imgStatsOp.getRegionMeansAllChannels();
    } catch (ALDException e) {
    	throw new MTBActiveContourException(ExceptionType.UPDATE_ERROR, 
    		"[MTBActiveContourEnergy_CVRegionFit] " + 
    			"updateParameters() - update failed, " + 
    				"keeping old parameters..." +	e.getCommentString());
    }
  }

  /**
   * Returns the energy derivative value at position (x,y,z).
   */
  protected double getDerivative(
  	@SuppressWarnings("unused")MTBSegmentationInterface seg,
  		int x, int y, int z){

  	// calculate statistical numbers of interior and exterior class
  	double resultVal = 0;
  	for (int c=0; c<this.iChannels; ++c) {
  		double pixInt = this.inImg.getValueDouble(x, y, z, 0, c);
  		resultVal += this.lambda_in[c] * 
  			(pixInt - this.means[c][1]) * (pixInt - this.means[c][1]);
  		resultVal -= this.lambda_out[c] * 
  			(pixInt - this.means[c][0]) * (pixInt - this.means[c][0]);
  	}
  	return 1.0/this.iChannels * resultVal;
  }
  
  /**
   * Calculates the absolute energy value for the given segmentation.
   * @param seg	Segmentation to consider.
   * @return	Absolute value of energy.
   */
  protected double calcEnergy(MTBSegmentationInterface seg) {
  	// process the whole image
  	double [][] classSums = 
  		new double[this.iChannels][seg.getMaxLabel()+1];
  	for (int c = 0; c < this.inImg.getSizeC(); ++c) {
  		for (int z = 0; z < this.inImg.getSizeZ(); ++z) {
  			for (int y = 0; y < this.inImg.getSizeY(); ++y) {
  				for (int x = 0; x < this.inImg.getSizeX(); ++x) {

  					if (!seg.isVisible(x, y, z))
  						continue;

  					int value = this.inImg.getValueInt(x, y, z, 0, c);
  					int label = seg.getClass(x, y, z);

  					classSums[c][label] += (value - this.means[c][label]) 
  																	* (value - this.means[c][label]);
  				}
    		}
      }
    }
    double regionHomoTerm = 0.0;
    for (int c = 0; c < this.inImg.getSizeC(); ++c) {
    	regionHomoTerm += this.lambda_out[c] * classSums[c][0];
    	regionHomoTerm += this.lambda_in[c] * classSums[c][1];
    }
    return 1.0/this.iChannels * regionHomoTerm;
  }
  
  /**
   * Calculates the interior part of energy value for the given 
   * segmentation.
   * 
   * @param seg	Segmentation to consider.
   * @return	Absolute value of interior energy.
   */
  protected double calcInteriorEnergy(MTBSegmentationInterface seg) {
  	// process the whole image
  	double [][] classSums = 
  		new double[this.iChannels][seg.getMaxLabel()+1];
  	for (int c = 0; c < this.inImg.getSizeC(); ++c) {
  		for (int z = 0; z < this.inImg.getSizeZ(); ++z) {
  			for (int y = 0; y < this.inImg.getSizeY(); ++y) {
  				for (int x = 0; x < this.inImg.getSizeX(); ++x) {

  					if (!seg.isVisible(x, y, z))
  						continue;

  					int value = this.inImg.getValueInt(x, y, z, 0, c);
  					int label = seg.getClass(x, y, z);

  					classSums[c][label] += (value - this.means[c][label]) 
  																	* (value - this.means[c][label]); 
  				}
  			}
  		}
  	}
    double regionHomoTerm = 0.0;
    for (int c = 0; c < this.inImg.getSizeC(); ++c) {
    	regionHomoTerm += this.lambda_in[c] * classSums[c][1];
    }
    return 1.0/this.iChannels * regionHomoTerm;
  }

  /**
   * Returns the maximum possible derivative value this energy may yield. 
   * @return	Maximal derivative value.
   */
  protected double getEnergyDerivativeMaxVal() {
  	double sum_lambda_in = 0, sum_lambda_out = 0;
  	for (int i=0; i<this.iChannels; ++i) {
  		sum_lambda_in += this.lambda_in[i]; 
  		sum_lambda_out += this.lambda_out[i]; 
  	}
  	return 
  		1.0 / this.iChannels * Math.max(sum_lambda_in, sum_lambda_out);
  }
  
  /**
   * Returns the minimum possible derivative value this energy may yield. 
   * @return	Minimal derivative value.
   */
  protected double getEnergyDerivativeMinVal() {
  	double sum_lambda_in = 0, sum_lambda_out = 0;
  	for (int i=0; i<this.iChannels; ++i) {
  		sum_lambda_in += this.lambda_in[i]; 
  		sum_lambda_out += this.lambda_out[i]; 
  	}
  	return 
  		-1.0 / this.iChannels * Math.max(sum_lambda_in, sum_lambda_out);
  }
  
  /**
   * Returns the average region intensities last calculated.
   * @return List of the regions' average intensity values.
   */
  protected double [][] getMeans() {
  	return this.means.clone();
  }
  
	@Override
  public String toString() {
	  return "MTBActiveContourEnergy - CVRegionFit";
  }
}
