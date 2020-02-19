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
import de.unihalle.informatik.Alida.annotations.ALDParametrizedClass;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.Alida.exceptions.ALDException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.segmentation.activecontours.exceptions.MTBActiveContourException;
import de.unihalle.informatik.MiToBo.segmentation.activecontours.exceptions.MTBActiveContourException.ExceptionType;
import de.unihalle.informatik.MiToBo.segmentation.basics.*;

/**
 * Active contour energy based on region mean separation.
 * <p>
 * This energy model tries to best separate the average intensity
 * values of the inner and outer regions of a contour:
 * 
 * {@latex.ilb %resolution{200} 
 * \\begin{equation} 
 *   E^{mean} = -\\frac{1}{2} \\cdot (\\mu_{in} - \\mu_{out})^2 
 * \\end{equation}}
 *  
 * The Gataeux derivative is given by
 *  
 * {@latex.ilb %resolution{200} 
 * \\begin{equation} 
 *   \\frac{ \\partial }{ \\partial c(s) } E^{mean} 
 *   	  =	- (\\mu_{in} - \\mu_{out}) \\cdot	
 *   \\left( \\frac{I(x,y) - \\mu_{in}}{A_{in}} + 
 *           \\frac{I(x,y) - \\mu_{out}}{A_{out}} \\right) 
 * \\end{equation}}
 *  
 * For details refer to:
 * <p>
 * Yezzi, Tsai, Willsky, "A Fully Global Approach to Image Segmentation via 
 * Coupled Curve Evolution Equations", JVCIR, 13, 195-216 (2002) 
 * 
 * @author moeller
 */
@ALDParametrizedClass
public class MTBActiveContourEnergy_MeanSep {

	/**
	 * Image to work on.
	 */
	@ALDClassParameter(label="Input Image", mode=ExpertMode.ADVANCED,
			dataIOOrder=-10)
	protected transient MTBImage inImg;
	
	/**
	 * Width of working image.
	 */
	protected transient int iWidth;
	
	/**
	 * Height of working image. 
	 */
	protected transient int iHeight;
	
	/**
	 * Depth of working image. 
	 */
	protected transient int iDepth;

	/**
	 * Total area/volume of working image. 
	 */
	protected transient int iSize;

	/**
	 * Dynamic range of image intensities.
	 */
	protected transient double inRange;
	
	/**
	 * Operator to calculate energy parameters.
	 * <p>
	 * Parameter are basically the region sizes and intensity averages.
	 */
	protected transient CalcSegmentationStatistics imgStatsOp = null;

	/**
   * Region sizes.
   */
  protected transient int [] areas = null;

  /**
   * Average intensities.
   */
  protected transient double [] means = null;

  /**
   * Default constructor.
   */
  public MTBActiveContourEnergy_MeanSep() {
  	super();
  }
  
	/**
   * Default constructor with parameter.
   * 
   * @param im	Image to work on.
   */
  public MTBActiveContourEnergy_MeanSep(MTBImage im) {
  	this.inImg = im;
  }
  
  /**
   * Method to properly initialize instances of this class.
   * @throws ALDOperatorException
   */
  protected void init() throws MTBActiveContourException {
		if (this.inImg == null)
			throw new MTBActiveContourException(ExceptionType.INITIALIZATION_ERROR,
					"[MTBActiveContourEnergy_MeanSep] init() - no image given!");
		this.iDepth = this.inImg.getSizeZ();
		this.iHeight= this.inImg.getSizeY();
		this.iWidth= this.inImg.getSizeX();
		this.iSize = this.iWidth * this.iHeight * this.iDepth;
		this.inRange = 
				this.inImg.getMinMaxDouble()[1] - this.inImg.getMinMaxDouble()[0];
		// do some savety checks
		if (this.iSize <= 1 || this.inRange == 0)
			throw new MTBActiveContourException(ExceptionType.INITIALIZATION_ERROR, 
					"[MTBActiveContourEnergy_MeanSep] init() - " + 
								"invalid image parameters, no segmentation possible!");
		// init the statistics operator
		try {
	    this.imgStatsOp = new CalcSegmentationStatistics(this.inImg);
    } catch (ALDOperatorException e) {
			throw new MTBActiveContourException(ExceptionType.INITIALIZATION_ERROR, 
					"[MTBActiveContourEnergy_MeanSep] init() - " + 
								"cannot calculate statistics, no segmentation possible! " +
								"Reason: " + e.getCommentString());
    }
		Vector<CalcSegmentationStatistics.CalcTargets> toCalc= 
				new Vector<CalcSegmentationStatistics.CalcTargets>();
				toCalc.add(CalcSegmentationStatistics.CalcTargets.classSize);
				toCalc.add(CalcSegmentationStatistics.CalcTargets.classMean);
				this.imgStatsOp.setTargets(toCalc);
  }
  
  /**
   * Updates internal parameters according to given segmentation.
   * @param seg		Current segmentation.
   */
	protected void updateParameters(MTBSegmentationInterface seg) 
		throws MTBActiveContourException {
  	// update the energy parameter
  	try {
  		this.imgStatsOp.setSegmentation(seg);
	    this.imgStatsOp.runOp(true);
	    this.areas = this.imgStatsOp.getRegionSizes();
	  	this.means = this.imgStatsOp.getRegionMeans();
    } catch (ALDException e) {
    	throw new MTBActiveContourException(ExceptionType.UPDATE_ERROR, 
    			"[MTBActiveContourEnergy_MeanSep] updateParameters() " 
        +	"- update failed, keeping old parameters..." + e.getCommentString());
    }
	}

  /**
   * Returns the energy derivative value at position (x,y,z).
   */
	protected double getDerivative(
		@SuppressWarnings("unused")MTBSegmentationInterface seg,int x,int y,int z){
		
  	// calculate statistical numbers of interior and exterior class
		double size_in = this.areas[1];
		double size_out = this.areas[0];
  	double mean_in = this.means[1];
  	double mean_out = this.means[0];
  	double pixInt = this.inImg.getValueDouble(x, y, z);

  	double inRegTerm =	(pixInt - mean_in) / size_in;
		double outRegTerm = (pixInt - mean_out) / size_out;

		// calculate final energy value
		return - ( mean_in - mean_out ) * (inRegTerm + outRegTerm);
	}

	@Override
  public String toString() {
	  return "MTBActiveContourEnergy - Means Separation";
  }

	/*
	 * Comments on normalizing the energy:
	 * ===================================
	 * 
	 * The maximum value results from the following assumptions:
	 * - the maximal difference between intensity value and average is limited
	 *   by the dynamic range of the image r, i.e. cannot be smaller than -r
	 *   and not larger than r
	 * - the minimal size of the interior or exterior regions is 1, their
	 *   maximal size cannot exceed the size N of the image minus 1
	 *
	 * Accordingly the sum in the second pair of brackets in the Gateaux 
	 * derivative lies in the interval [ -N/(N-1) * r, N/(N-1) * r ].
	 * <p>
	 * This result is multiplied with the negative difference of both region
	 * average intensity values, which again cannot lie outside of the 
	 * interval [-r, r].
	 * <p>
	 * In conclusion the derivative values will always lie in the interval 
	 * [- N/(N-1) * r^2, N/(N-1) * r^2 ].
	 * 
	 * In the normalized case the range of values depends on the actual size 
	 * of the image which determines the minimal and maximal value of the 
	 * derivatives. Since these values are usually smaller than the squared
	 * intensity range it might be of numerical advantage to use the unity 
	 * normalization. Nevertheless, in most applications there will still remain 
	 * the necessity to do further non-linear normalization. This is due to the 
	 * fact that the range of possible values is still large while the actually 
	 * requested energy values most probably cover only a small fraction of 
	 * that range.   
	 */
	
  /**
   * Returns the maximum possible derivative value this energy may yield. 
   * @return	Maximal derivative value.
   */
	protected double getEnergyDerivativeMaxVal() {
		return this.iSize / ( this.iSize - 1 );  	
	}

  /**
   * Returns the minimum possible derivative value this energy may yield. 
   * @return	Minimal derivative value.
   */
	public double getEnergyDerivativeMinVal() {
		return - this.iSize / ( this.iSize - 1 );
	}
}
