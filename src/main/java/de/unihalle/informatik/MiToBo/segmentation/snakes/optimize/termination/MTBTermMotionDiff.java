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
import de.unihalle.informatik.MiToBo.core.datatypes.defines.MTBConstants;
import de.unihalle.informatik.MiToBo.segmentation.snakes.datatypes.MTBSnake;
import de.unihalle.informatik.MiToBo.segmentation.snakes.datatypes.MTBSnakePoint2D;
import de.unihalle.informatik.MiToBo.segmentation.snakes.optimize.SnakeOptimizerSingle;
import de.unihalle.informatik.MiToBo.segmentation.snakes.optimize.SnakeOptimizer.Snake_status;

/**
 * Check on snake termination by motion difference. 
 * <p>
 * Termination is done when the snake control point motion of the former and 
 * the current snake is below a given fraction factor or if a maximum number of 
 * iterations is reached.
 * 
 * @author misiak
 */
@ALDDerivedClass
@ALDParametrizedClass
public class MTBTermMotionDiff extends MTBTermination {

	/**
	 * Motion fraction factor.
	 */
	@ALDClassParameter(label="Motion Fraction")
	private double motionFraction = 0.05;
	/**
	 * Maximum number of iterations.
	 */
	@ALDClassParameter(label="Maximum Iteration Count")
	private int maxIterations = 100;

	/**
	 * Default constructor.
	 */
	public MTBTermMotionDiff() {
		// nothing to do here
	}

	/**
	 * Constructor with motion fraction factor and maximum iteration count. 
	 * Snake optimization is terminated if point motion of the snake is below the 
	 * motion fraction or the number of iterations has reached the maximum 
	 * iteration count.
	 * 
	 * @param _motionFraction
	 *          motion fraction
	 * @param _maxIterations
	 *          maximum iteration count
	 */
	public MTBTermMotionDiff(double _motionFraction, int _maxIterations) {
		this.motionFraction = _motionFraction;
		this.maxIterations = _maxIterations;
	}

	@Override
	public boolean init(SnakeOptimizerSingle opt) {
		this.optimizer = opt;
		return true;
	}

	/**
	 * Termination is done when the snake control point motion of the former and
	 * the current snake is below a given fraction factor or if a maximum number
	 * of iterations is reached.
	 */
	@Override
	public Snake_status terminate() {
		// get current snake
		MTBSnake curSnake = this.optimizer.getCurrentSnake();
		// get old snake
		MTBSnake oldSnake = this.optimizer.getPreviousSnake();
		// number of pints that have not changed since the last iteration step
		int num_NotChanged = 0;
		// check all snake points
		for (int j = 0; j < curSnake.getPointNum(); j++) {
			// get the current snake point
			MTBSnakePoint2D curPoint = curSnake.getSnakePoints().elementAt(j);
			// use only snake points that exist in the last iteration step
			if (curPoint.getOldId() >= 0) {
				MTBSnakePoint2D oldPoint = oldSnake.getSnakePoints().elementAt(
						curPoint.getOldId());
				// calculate distance of the current point with its position in the
				// last iteration step
				double dx = curPoint.x - oldPoint.x;
				double dy = curPoint.y - oldPoint.y;
				double dist = Math.sqrt(dx * dx + dy * dy);
				// increase counter if position has not changed
				if (dist < MTBConstants.epsilon) {
					num_NotChanged++;
				}
			}
		}
		// calculate current motion fraction
		double changeFrac = (((double) num_NotChanged) / curSnake.getPointNum());
		// get current iteration count
		int itCounter = this.optimizer.getIterationCount();
		
		if (this.verbose) {
			System.out.println("    Stop at " + this.maxIterations
					+ " iterations or if >= " + (this.motionFraction * 100)
					+ " % of the points are not moving.");
			System.out.println("    Points not moved = " + changeFrac);
		}
		
		/*
		 * Terminate if number of not changed points is greater or equal to the
		 * given number of point motion fraction or the maximum number of iterations
		 * has been reached
		 */
		if (changeFrac >= this.motionFraction) {
			if (this.verbose)
				System.out.println("\n--- Snake optimizer stopped: Snake done after "
					+ itCounter + " step(s) [minimal point motion reached].\n");
			return SnakeOptimizerSingle.Snake_status.SNAKE_DONE;
		} else if (itCounter >= this.maxIterations) {
			if (this.verbose)
				System.out.println("\n--- Snake optimizer stopped: Snake done after "
					+ itCounter + " step(s) [maximum number of iterations reached].\n");
			return SnakeOptimizerSingle.Snake_status.SNAKE_DONE;
		} else {
			return SnakeOptimizerSingle.Snake_status.SNAKE_SUCCESS;
		}
	}

	/**
	 * Get motion fraction.
	 * 
	 * @return Motion fraction.
	 */
	public double getMotionFraction() {
		return this.motionFraction;
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
		return ("MTBTermMotionDiff [maxIterations=" + this.maxIterations 
				+ ",motionFraction=" + this.motionFraction + "]");
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
	public MTBTermMotionDiff clone() throws CloneNotSupportedException {
		MTBTermMotionDiff newObj = 
			new MTBTermMotionDiff(this.motionFraction, this.maxIterations);
		newObj.status = this.status;
		newObj.optimizer = null;
		return newObj;
	}
}
