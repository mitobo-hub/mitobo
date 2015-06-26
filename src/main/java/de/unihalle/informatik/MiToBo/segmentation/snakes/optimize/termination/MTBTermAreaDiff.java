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

package de.unihalle.informatik.MiToBo.segmentation.snakes.optimize.termination;

import de.unihalle.informatik.Alida.annotations.ALDClassParameter;
import de.unihalle.informatik.Alida.annotations.ALDDerivedClass;
import de.unihalle.informatik.Alida.annotations.ALDParametrizedClass;
import de.unihalle.informatik.MiToBo.segmentation.snakes.optimize.SnakeOptimizerSingle;
import de.unihalle.informatik.MiToBo.segmentation.snakes.optimize.SnakeOptimizer.Snake_status;

/**
 * Check upon snake termination by the area within the snake. 
 * <p>
 * Termination is done when the area difference of the former and the current 
 * snake is below a given fraction factor or if a maximum number of iterations 
 * is reached.
 * 
 * @author misiak
 */
@ALDDerivedClass
@ALDParametrizedClass
public class MTBTermAreaDiff extends MTBTermination {

	/**
	 * Area fraction factor.
	 */
	@ALDClassParameter(label="Area Fraction", dataIOOrder = 0)
	private double areaFraction = 0.001;
	/**
	 * Maximum number of iterations.
	 */
	@ALDClassParameter(label="Maximum Iteration Count", dataIOOrder = 1)
	private int maxIterations = 100;

	/**
	 * Default constructor.
	 */
	public MTBTermAreaDiff() {
		// nothing to do here
	}
		
	/**
	 * Constructor with area fraction below the snake iteration is terminated or
	 * if a maximum number of iterations is reached.
	 */
	public MTBTermAreaDiff(double _areaFraction, int _maxIterations) {
		this.areaFraction = _areaFraction;
		this.maxIterations = _maxIterations;
	}

	@Override
	public boolean init(SnakeOptimizerSingle opt) {
		this.optimizer = opt;
		return true;
	}

	/**
	 * Termination is done when the area difference of the former and the current
	 * snake is below a given fraction factor or if a maximum number of iterations
	 * is reached.
	 */
	@Override
	public Snake_status terminate() {
		
		// get image size of the image whereupon the current snake is optimized
		int width = 0;
		int height = 0;
		width = this.optimizer.getWorkingImage().getSizeX();
		height = this.optimizer.getWorkingImage().getSizeY();
		// get areas within the old and the current snake
		int[][] maskNew = 
				this.optimizer.getCurrentSnake().getBinaryMask(width, height);
		//				if (this.optimizer.getOldSnake() == null)
		//					// first iteration
		//					return SnakeOptimizerSingle.Snake_status.SNAKE_SUCCESS;
		int[][] maskOld = 
				this.optimizer.getPreviousSnake().getBinaryMask(width, height);
		int snakeSize_new = 0;
		int snakeSize_old = 0;
		for (int y = 0; y < height; ++y) {
			for (int x = 0; x < width; ++x) {
				if (maskOld[y][x] > 0)
					snakeSize_old++;
				if (maskNew[y][x] > 0)
					snakeSize_new++;
			}
		}
		if (this.verbose) {
			System.out.println("    Stop at " + this.maxIterations
				+ " iterations or if area fraction is <= " + this.areaFraction + " .");
		}
		// get area difference and check upon iteration count
		double areaChange = (double) snakeSize_new / (double) snakeSize_old;
		
		if (this.verbose) {
			System.out.println("Area change: " + areaChange);
			System.out.println("    Area fraction = " + Math.abs(1.0 - areaChange));
			System.out.println(this.areaFraction);
		}
		if (Math.abs(1.0 - areaChange) < this.areaFraction
				|| this.optimizer.getIterationCount() > this.maxIterations)
			return SnakeOptimizerSingle.Snake_status.SNAKE_DONE;
		return SnakeOptimizerSingle.Snake_status.SNAKE_SUCCESS;
	}

	/**
	 * Get area fraction.
	 * 
	 * @return Area Fraction.
	 */
	public double getAreaFraction() {
		return this.areaFraction;
	}

	/**
	 * Get maximum iteration count.
	 * 
	 * @return Maximum Iterations.
	 */
	public int getMaxIter() {
		return this.maxIterations;
	}

	@Override
	public String toString() {
		return ("MTBTermAreaDiff [" + "maxIterations=" + this.maxIterations 
			+ "areaDiff=" + this.areaFraction + "]");
	}

	/**
	 * Clones this object.
	 * <p>
	 * Note that the {@link SnakeOptimizerSingle} object is left null! 
	 * You have to call {@link SnakeOptimizer.initOptimizer()} before 
	 * using the gamma updater.
	 */
	/* (non-Javadoc)
	 * @see de.unihalle.informatik.MiToBo.segmentation.snakes.optimize.stepsize.MTBGammaUpdate#clone()
	 */
	@Override
	@SuppressWarnings("unused")
	public MTBTermAreaDiff clone() throws CloneNotSupportedException {
		MTBTermAreaDiff newObj = 
			new MTBTermAreaDiff(this.areaFraction, this.maxIterations);
		newObj.status = this.status;
		newObj.optimizer = null;
		return newObj;
	}
}
