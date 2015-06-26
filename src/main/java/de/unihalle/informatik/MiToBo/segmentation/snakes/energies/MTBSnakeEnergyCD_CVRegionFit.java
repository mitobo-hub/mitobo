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

package de.unihalle.informatik.MiToBo.segmentation.snakes.energies;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.Vector;

import Jama.Matrix;
import de.unihalle.informatik.Alida.annotations.ALDDerivedClass;
import de.unihalle.informatik.Alida.annotations.ALDParametrizedClass;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBPolygon2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.segmentation.activecontours.energies.MTBActiveContourEnergy_CVRegionFit;
import de.unihalle.informatik.MiToBo.segmentation.activecontours.exceptions.*;
import de.unihalle.informatik.MiToBo.segmentation.activecontours.exceptions.MTBActiveContourException.ExceptionType;
import de.unihalle.informatik.MiToBo.segmentation.snakes.datatypes.MTBSnake;
import de.unihalle.informatik.MiToBo.segmentation.snakes.optimize.SnakeOptimizerCoupled;
import de.unihalle.informatik.MiToBo.segmentation.snakes.optimize.SnakeOptimizerSingle;
import de.unihalle.informatik.MiToBo.segmentation.snakes.optimize.SnakeOptimizerSingleGreedy;
import de.unihalle.informatik.MiToBo.segmentation.snakes.optimize.SnakeOptimizerSingleVarCalc;
import de.unihalle.informatik.MiToBo.segmentation.snakes.optimize.SnakeOptimizerSingle.EnergyNormalizationMode;

/**
 * Snake energy based on Chan-Vese region fitting.
 * <p>
 * This energy models the inner and outer regions of a snake by a 
 * Gaussian model, i.e. each region is specified by a mean intensity 
 * value and deviations from this value are penalized.
 * <p>
 * The energy can also be used in joint optimization of multiple snakes. 
 * In this case the energy over all N snakes is defined as follows:
 * {@latex.ilb %preamble{\\usepackage{amssymb}}
 *      \\begin{eqnarray*}
 *      E(C_1,...,C_N) &=& \\lambda_{in} \\cdot \\sum_{i=1}^N 
 *       		\\int_{inside(C_i)} (I(x,y) - c_{in,i})^2 dx dy \\\\
 *       	&& \\hspace*{1cm} + \\lambda_{out} \\cdot  
 *       		\\int_{outside(C_i)\\cap...\\cap outside(C_N)} 
 *       				(I(x,y) - c_{out})^2 dx dy
 *      \\end{eqnarray*}}
 * <p>
 * Paper:
 * <ul>
 * <li> Chan and Vese, <i>Active Contours Without Edges</i>,
 * IEEE Transactions on Image Processing, vol. 10, no. 2, pp. 266-277, 
 * 2001.
 * <li> C. Zimmer and J.-C. Olivo-Marin, <i>Coupled Parametric Active 
 * Contours</i>, IEEE Trans. on PAMI, vol. 27, no. 11, pp. 1838-1842, 
 * 2005.
 * </ul> 
 * Note that this energy can also be used with multi-channel images,
 * i.e. vectorial pixel data. For details see documentation of the 
 * super class or the related paper:
 * <ul>
 * <li> Chan, Sandberg and Vese, <i>Active Contours without Edges for 
 * Vector-Valued Images</i>, Journal of Visual Communications and Image
 * Representation, vol. 11, pp. 130-141, 2000.
 * </ul>
 * 
 * @see MTBActiveContourEnergy_CVRegionFit
 * @author moeller
 */
@ALDDerivedClass
@ALDParametrizedClass
public class MTBSnakeEnergyCD_CVRegionFit 
	extends MTBActiveContourEnergy_CVRegionFit 
		implements MTBSnakeEnergyDerivable, MTBSnakeEnergyComputable,
			MTBSnakeEnergyCoupled {

	/**
	 * Interval boundaries of adapted energy interval.
	 * <p>
	 * energyRange[0] -> left boundary
	 * energyRange[1] -> right boundary
	 * energyRange[2] -> interval width
	 */
	private double [] energyRange = {0.0, 0.0, 0.0};
	
	/**
	 * Width of working image.
	 */
	private transient int iWidth;
	
	/**
	 * Height of working image. 
	 */
	private transient int iHeight;
	
	/**
	 * Mode of normalization.
	 */
	protected EnergyNormalizationMode normMode = 
		EnergyNormalizationMode.NORM_BALANCED_DERIVATIVES;
	
	/**
	 * Scaling factor to rescale image coordinates in range [0,1] or 
	 * several ranges to the original coordinates range like [1000, 1000] 
	 * in a image of size 1000 x 1000. Default scaling factor is 1.
	 */
	protected transient double scaleFactor = 1.0;

	/**
	 * Current snake the energy is based on.
	 */
	private transient MTBSnake currentSnk;
	
	/**
	 * Global exclude mask for image to be segmented.
	 */
	private transient boolean [][] excludeMask;
	
	/**
	 * Reference to a coupled snake optimizer.
	 * <p>
	 * This reference is only non-null if the energy is used in 
	 * conjunction with coupled snake optimization.
	 */
	private transient SnakeOptimizerCoupled cSnakeOpt = null;
	
	/**
	 * Current overlap mask for joint optimization of multiple snakes.
	 */
	private int[][] overlapMask;
	
	/**
	 * Default constructor.
	 */
	public MTBSnakeEnergyCD_CVRegionFit() {
		// nothing to do here
	}
	
  /**
   * Default constructor.
   * 
   * @param im	Image to work on.
   * @param l_in	Weighting factors for inner region fit.
   * @param l_out	Weighting factors for outer region fit.
   * @throws ALDOperatorException 
   * @throws ALDProcessingDAGException 
   */
  public MTBSnakeEnergyCD_CVRegionFit(MTBImage im, 
  		double[] lin, double[] lout) {
  	super(im, lin, lout);
  }
  
	@SuppressWarnings("unused")
  @Override
  public boolean initEnergy(SnakeOptimizerCoupled opt) 
		throws MTBSnakeException {
		this.cSnakeOpt = opt;
		return true;
	}

	@Override
  public boolean initEnergy(SnakeOptimizerSingle opt) 
		throws MTBSnakeException {
		if (this.inImg == null) {
			// get normalized (!) image from snake optimizer
			this.inImg = opt.getWorkingImage();
		}
		else {
			// make sure that the input image intensities are normalized
			this.inImg = opt.normalizeInputImage(this.inImg);
		}
		try {
	    super.init();
    } catch (MTBActiveContourException e) {
    	throw new MTBSnakeException(ExceptionType.INITIALIZATION_ERROR,
    		"MTBSnakeEnergyCD_CVRegionFit - " +
    			"init failed! Reason: " + e.getCommentString());
    }
  	this.iWidth = this.inImg.getSizeX();
  	this.iHeight = this.inImg.getSizeY();
  	this.normMode = opt.getNormalizationMode();
		// get energy minimum and maximum; minimum of Chan-Vese energy is 
  	// always smaller than zero and maximum always larger
		double eMin = this.getEnergyDerivativeMinVal();
		double eMax = this.getEnergyDerivativeMaxVal();
		if (Math.abs(eMin) > Math.abs(eMax)) {
			this.energyRange[0] = - Math.abs(eMin);
			this.energyRange[1] =   Math.abs(eMin);
		}
		else {
			this.energyRange[0] = - Math.abs(eMax);
			this.energyRange[1] =   Math.abs(eMax);
		}
		this.energyRange[2] = this.energyRange[1] - this.energyRange[0];
	  return true;
  }
	
  @Override
  public void updateStatus(SnakeOptimizerCoupled opt) {
  	this.overlapMask = opt.getCurrentOverlapMask();
	}
	
	@Override
  public void updateStatus(SnakeOptimizerSingle opt) 
  	throws MTBSnakeException {
  	// the greedy snake optimizer calls this function more than once in 
		// each iteration, thus, each time it is called also global overlap 
		// masks have to be updated (which would usually happen only once 
		// per iteration)
		if (   this.cSnakeOpt != null 
				&& opt instanceof SnakeOptimizerSingleGreedy) {
			this.cSnakeOpt.updateOverlapMask();
			this.overlapMask = this.cSnakeOpt.getCurrentOverlapMask();
		}
		this.currentSnk = opt.getCurrentSnake();
		this.excludeMask = opt.getExcludeMask();
		// set excluded pixels invalid - if there are some...
    boolean [][] vMask = new boolean[this.iHeight][this.iWidth];
    if (this.excludeMask != null) {
    	for (int y=0;y<this.iHeight;++y) {
    		for (int x=0;x<this.iWidth;++x) {
    			vMask[y][x] = !this.excludeMask[y][x];
    		}
    	}
    }
    else {
    	// all pixels are valid, non is excluded
    	for (int y=0;y<this.iHeight;++y) {
    		for (int x=0;x<this.iWidth;++x) {
    			vMask[y][x] = true;
    		}
    	}    	
    }
		// fetch current overlap mask
    if (this.overlapMask != null) {
    	int[][] snakeMask = this.currentSnk.getBinaryMask(
    		opt.getWorkingImage().getSizeX(),	
    			opt.getWorkingImage().getSizeY());	    
    	for (int y=0;y<this.iHeight;++y) {
    		for (int x=0;x<this.iWidth;++x) {
    			if (snakeMask[y][x] == 0 && this.overlapMask[y][x] != 0)
    				vMask[y][x] = false;
    		}
    	}
    }
		this.currentSnk.setVisibilityMask(vMask, 0, 0, 
			this.iWidth-1, this.iHeight-1);
		try {
			// do the actual update
	    this.updateParameters(this.currentSnk);
    } catch (MTBActiveContourException e) {
    	throw new MTBSnakeException(ExceptionType.INITIALIZATION_ERROR,
    		"MTBSnakeEnergyCD_CVRegionFit - " +
    			"update failed! Reason: " + e.getCommentString());
    }
	}
	
	/**
   * Updates the fitting term based on new region average values.
   */
	@Override
  public Matrix getDerivative_MatrixPart(
  		SnakeOptimizerSingleVarCalc opt) {
		
		int snakePointNum = this.currentSnk.getPointNum();
		Vector<Point2D.Double> polyPoints = this.currentSnk.getPoints();
		
		Matrix A = new Matrix(snakePointNum * 2, snakePointNum * 2);
		for (int counter = 0; counter < snakePointNum - 1; ++counter) {
			Point2D.Double p = polyPoints.get(counter);
			double eDerive = 0.0;
			int px = (int)(p.x*this.scaleFactor);
			int py = (int)(p.y*this.scaleFactor);
			eDerive = this.getDerivative(this.currentSnk, px, py, 0);
			double tau_i = 0.0;
			switch(this.normMode)
			{
			case NORM_BALANCED_DERIVATIVES:
				// calculate normalized final derivative value
				tau_i = (eDerive - this.energyRange[0])/this.energyRange[2]                                              
						* MTBSnakeEnergyDerivable.targetEnergyRange[2] 
								+ MTBSnakeEnergyDerivable.targetEnergyRange[0];
				break;
			case NORM_NONE:
			default:
				tau_i = eDerive;
				break;
			}
//			double oldVal= A.get(counter, snakePointNum + counter);
			A.set(counter, snakePointNum + counter, - tau_i);
//			oldVal= A.get(counter, snakePointNum + counter + 1);
			A.set(counter, snakePointNum + counter + 1, tau_i);
//			oldVal= A.get(snakePointNum + counter, counter);
			A.set(snakePointNum + counter, counter, tau_i);
//			oldVal= A.get(snakePointNum + counter, counter + 1);
			A.set(snakePointNum + counter, counter + 1, - tau_i);
		}
		// add last point
		Point2D.Double p = polyPoints.get(snakePointNum - 1);
		double eDerive = 0.0;
		int px = (int)(p.x*this.scaleFactor);
		int py = (int)(p.y*this.scaleFactor);
		eDerive = this.getDerivative(this.currentSnk, px, py, 0);
		double tau_i = 0.0;
		switch(this.normMode)
		{
		case NORM_BALANCED_DERIVATIVES:
			// calculate normalized final derivative value
			tau_i = (eDerive - this.energyRange[0])/this.energyRange[2]                                              
					* MTBSnakeEnergyDerivable.targetEnergyRange[2] 
							+ MTBSnakeEnergyDerivable.targetEnergyRange[0];
			break;
		case NORM_NONE:
		default:
			tau_i = eDerive;
			break;
		}
		// fill matrix - upper right block
//		double oldVal = A.get(snakePointNum - 1, snakePointNum);
		A.set(snakePointNum - 1, snakePointNum, tau_i);
//		oldVal = A.get(snakePointNum - 1, snakePointNum * 2 - 1);
		A.set(snakePointNum - 1, snakePointNum * 2 - 1, - tau_i);
		// fill matrix - lower left block
//		oldVal = A.get(2 * snakePointNum - 1, 0);
		A.set(2 * snakePointNum - 1, 0, - tau_i);
//		oldVal = A.get(2 * snakePointNum - 1, snakePointNum - 1);
		A.set(2 * snakePointNum - 1, snakePointNum - 1, tau_i);
		return A;
	}

	@Override
  public Matrix getDerivative_VectorPart(
  		SnakeOptimizerSingleVarCalc opt) {
	  return null;
  }

  @Override
  public double calcEnergy(SnakeOptimizerSingle opt) {
  	// distinguish between single snake and joint optimization of many 
  	// snakes
  	if (this.cSnakeOpt == null) {
  		// get current snake
  		MTBSnake snake= opt.getCurrentSnake();
  		// calculate energy
  		return super.calcEnergy(snake);
  	}
  	double inEnergy = 0.0;
  	// calculate energy fractions added by snake interiors
  	int snakeNum = this.cSnakeOpt.getSnakeNumber();
  	MTBPolygon2DSet snakes = this.cSnakeOpt.getCurrentSnakes();
  	for (int i=0; i<snakeNum; ++i) {
  		inEnergy += 
  			super.calcInteriorEnergy((MTBSnake)snakes.elementAt(i));
  	}
		double outEnergy = 0;
  	for (int c=0; c<this.iChannels; ++c) {
  		for (int y=0; y<this.iHeight; ++y) {
  			for (int x=0; x<this.iWidth; ++x) {
		  		if (this.overlapMask[y][x] == 0) {
		  			outEnergy += 
	  					(this.inImg.getValueInt(x, y, 0, 0, c) - this.means[c][0])
	  				* (this.inImg.getValueInt(x, y, 0, 0, c) - this.means[c][0]);
		  		}
				}
			}
		}
		return this.lambda_out[0] * outEnergy + inEnergy;
  }

	/**
	 * Set the scaling factor.
	 * 
	 * @param s
	 *          new scaling factor.
	 */
	@Override
  public void setScaleFactor(double s) {
		this.scaleFactor = s;
	}

	/**
	 * Get scaling factor.
	 * 
	 * @return Scaling factor.
	 */
	@Override
  public double getScaleFactor() {
		return this.scaleFactor;
	}

	@Override
  public String toString() {
		return "MTBSnakeEnergyCD_CVRegionFit: Chan-Vese Region Fitting "  
				+ "(lambda_in = " + this.lambda_in + 
					" , lamdba_out = " + this.lambda_out + ")";
  }

	@Override
  public boolean requiresCounterClockwiseContourSorting() {
		return true;
	}

	@Override
  public  boolean requiresOverlapMask() {
		return true;
	}
}
