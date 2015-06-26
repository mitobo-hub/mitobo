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
import java.util.Vector;

import Jama.Matrix;
import de.unihalle.informatik.Alida.annotations.ALDClassParameter;
import de.unihalle.informatik.Alida.annotations.ALDDerivedClass;
import de.unihalle.informatik.Alida.annotations.ALDParametrizedClass;
import de.unihalle.informatik.MiToBo.core.datatypes.defines.MTBConstants;
import de.unihalle.informatik.MiToBo.math.MathX;
import de.unihalle.informatik.MiToBo.segmentation.snakes.datatypes.MTBSnake;
import de.unihalle.informatik.MiToBo.segmentation.snakes.optimize.SnakeOptimizerCoupled;
import de.unihalle.informatik.MiToBo.segmentation.snakes.optimize.SnakeOptimizerSingle;
import de.unihalle.informatik.MiToBo.segmentation.snakes.optimize.SnakeOptimizerSingleGreedy;
import de.unihalle.informatik.MiToBo.segmentation.snakes.optimize.SnakeOptimizerSingleVarCalc;

/**
 * Energy to avoid overlaps of snakes in joint optimization of multiple snakes.
 * <p>
 * This energy is defined as follows for a set of N snakes:
 * {@latex.ilb %preamble{\\usepackage{amssymb}}
 *      \\begin{equation*}
 *      E(s_1,\\ldots,s_N) = \\rho \\cdot 
 *         \\sum_{i=1}^N \\sum_{j=i+1}^N 
 *         		\\int_{s_i \\cap s_j} d\\sigma 
 *      \\end{equation*}}
 * It basically calculates for each pair of snakes the area of overlap of their
 * interiors. The overall energy value is given by the sum of all pairwise 
 * overlap areas. Rho is a weighting factor.
 * <p>
 * Note that using this energy with a single snake is not reasonable. 
 * The energy does not cause a single snake to move anyway, i.e., it strictly 
 * requires interaction of multiple snakes.
 * <p>
 * Paper: C. Zimmer and J.-C. Olivo-Marin, <i>Coupled Parametric Active 
 * Contours</i>, IEEE Trans. on PAMI, vol. 27, no. 11, pp. 1838-1842, 2005. 
 * 
 * @author moeller
 */
@ALDDerivedClass
@ALDParametrizedClass
public class MTBSnakeEnergyCD_OverlapPenalty 
	implements MTBSnakeEnergyDerivable, MTBSnakeEnergyComputable, 
		MTBSnakeEnergyCoupled {

	/**
	 * Weighting factor and overlap penalty, respectively.
	 */
	@ALDClassParameter(label="Weighting Parameter")
	private double rho = 1.0;
	
	/**
	 * Reference to a coupled snake optimizer.
	 * <p>
	 * This reference is only non-null if the energy is used in conjunction with
	 * coupled snake optimization.
	 */
	private transient SnakeOptimizerCoupled cSnakeOpt = null;

	/**
	 * Number of snakes to be optimized.
	 */
	protected transient int snakeNum = 0;
	
	/**
	 * Maxmimum value of energy, dependent on number of snakes and rho.
	 */
	protected transient double maxEnergyVal = 0; 
	
	/**
	 * Scaling factor to rescale image coordinates in range [0,1] or several
	 * ranges to the original coordinates range like [1000, 1000] in a image of
	 * size 1000 x 1000. Default scaling factor is 1.
	 */
	protected double scaleFactor = 1.0;

	/**
	 * Overlap mask of current snake configuration, updated in each iteration.
	 */
	protected int[][] currentOverlapMask = null;
	
	/**
	 * Pre-computed factorial values.
	 * <p>
	 * At position n in this array the following value is stored:
	 * {@latex.ilb %preamble{\\usepackage{amssymb}}
	 *      \\begin{equation*} 
	 *         \\left( \\begin{array}{c}
	 *         		n \\\\ 2 
	 *         \\end{array} \\right) = \\frac{n!}{2! \\cdot (n-2)!} \\;\\;
	 *         \\text{for}\\;\\; n = 0,...,N 
	 *      \\end{equation*}}
	 * Here N is the number of snakes simultaneously optimized.
	 */
	protected static int[] factorialVals;
	
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
	
	/**
	 * Default constructor.
	 */
	public MTBSnakeEnergyCD_OverlapPenalty() {
		// nothing to do here
	}
	
  /**
   * Default constructor.
   * 
   * @param _rho	Overlap penalty coefficient.
   */
  public MTBSnakeEnergyCD_OverlapPenalty(double _rho, int snakes) {
  	this.rho= _rho;
  	this.snakeNum = snakes;
  }
  
  @Override
  public boolean initEnergy(SnakeOptimizerCoupled opt) {
  	this.cSnakeOpt = opt;
  	this.snakeNum = opt.getSnakeNumber();
  	this.maxEnergyVal = 
  		this.rho > MTBConstants.epsilon ? this.rho*this.snakeNum : 1.0;
		// pre-compute factorial values
		factorialVals = new int[this.snakeNum+1];
		for (int i = 0; i<= this.snakeNum; ++i)
			factorialVals[i] = MathX.factorial(i);
		return true;
	}

  @Override
  public boolean initEnergy(SnakeOptimizerSingle opt) {
		return true;
	}

  @Override
  public void updateStatus(SnakeOptimizerCoupled o) {
  	this.currentOverlapMask = o.getCurrentOverlapMask();
  }

  @Override
  public void updateStatus(SnakeOptimizerSingle o) {
  	// the greedy snake optimizer calls this function more than once in each
  	// iteration, hence, each time it is called also global overlap masks 
  	// have to be updated (which would usually happen only once per iteration)
		if (this.cSnakeOpt != null && o instanceof SnakeOptimizerSingleGreedy) {
			this.cSnakeOpt.updateOverlapMask();
			this.currentOverlapMask = this.cSnakeOpt.getCurrentOverlapMask();
		}
  }

	/**
   * Updates the region coupling energy term in matrix A.
   */
	@Override
  public Matrix getDerivative_MatrixPart(SnakeOptimizerSingleVarCalc opt) {

		// get snake data
		MTBSnake snake = (MTBSnake)opt.getCurrentSnakes().elementAt(0);
    Vector<Point2D.Double> polyPoints = snake.getPoints();
    int snakePointNum = polyPoints.size();
		Matrix A = new Matrix(snakePointNum * 2, snakePointNum * 2);

		// if there is no overlap mask available, we can do nothing...
	  if (this.currentOverlapMask == null) {
	  	return A;
	  }
	  	
	  // get mask of this snake
	  int[][] snakeMask;
    snakeMask = snake.getBinaryMask(opt.getWorkingImage().getSizeX(),
    		                            opt.getWorkingImage().getSizeY());	    
    // update matrix A
    for (int counter = 0; counter < snakePointNum - 1; ++counter) {
    	Point2D.Double p = polyPoints.get(counter);
    	// check in how many other snakes the point actually lies
    	double overlapCount = 
    		this.currentOverlapMask[(int)(p.y*this.scaleFactor)]
    				                   [(int)(p.x*this.scaleFactor)]
      - snakeMask[(int)(p.y*this.scaleFactor)][(int)(p.x*this.scaleFactor)];
//	    	double oldEntryA = -A.get(counter, snakePointNum + counter);
    	double newEntryA = (this.rho * overlapCount)/this.maxEnergyVal;
    	A.set(counter, snakePointNum + counter, -newEntryA);
    	A.set(counter, snakePointNum + counter + 1, newEntryA);
    	A.set(snakePointNum + counter, counter, newEntryA);
    	A.set(snakePointNum + counter, counter + 1, -newEntryA);
    }
    // add last point
    Point2D.Double p = polyPoints.get(snakePointNum - 1);
    // check in how many other snakes the point actually lies
    double overlapCount = 
     	  this.currentOverlapMask[(int)(p.y*this.scaleFactor)]
     	  		                   [(int)(p.x*this.scaleFactor)] 
      - snakeMask[(int)(p.y*this.scaleFactor)][(int)(p.x*this.scaleFactor)];
//	    double oldEntryA = A.get(snakePointNum - 1, snakePointNum);
  	double newEntryA = (this.rho * overlapCount)/this.maxEnergyVal;
    A.set(snakePointNum - 1, snakePointNum, newEntryA);
    A.set(snakePointNum - 1, snakePointNum * 2 - 1, -newEntryA);
    A.set(2 * snakePointNum - 1, 0, -newEntryA);
    A.set(2 * snakePointNum - 1, snakePointNum - 1, newEntryA);
    return A;
	}

	@Override
  public Matrix getDerivative_VectorPart(SnakeOptimizerSingleVarCalc opt) {
	  return null;
  }

	/**
	 * Calculates the current energy of the snake.
	 * <p>
	 * The energy is calculated based on the overlap mask of the current 
	 * configuration of all snakes. In this overlap mask at each pixel position 
	 * the absolute number of snakes covering that position is stored. 
	 * For extracting the absolute energy value from this data it is, thus, 
	 * necessary to derive the number of pairwise overlaps at a certain position. 
	 * This count is given by the binomial of the number of snakes overlapping at 
	 * a certain location over 2.
	 */
	@Override
  public double calcEnergy(SnakeOptimizerSingle opt) {
		// if no overlap mask is given, we cannot do anything...
	  if (this.currentOverlapMask == null) {
	  	System.err.println("Overlap mask is null!");
	  	return 0;
	  }
	  // get size of mask
	  int height = this.currentOverlapMask.length;
	  int width = this.currentOverlapMask[0].length;
	  
	  // calculate energy
	  double energy = 0;
	  for (int y=0;y<height;++y) {
	  	for (int x=0;x<width;++x) {
	  		if (this.currentOverlapMask[y][x] <= 1)
	  			continue;
	  		energy += factorialVals[this.currentOverlapMask[y][x]] 
	  				/ (2 * factorialVals[this.currentOverlapMask[y][x] - 2]);
	  	}
	  }
	  return this.rho * energy;
  }

	@Override
  public String toString() {
		return new String("MTBSnkEner: Snake overlap penalty (rho= "+this.rho+")");
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
