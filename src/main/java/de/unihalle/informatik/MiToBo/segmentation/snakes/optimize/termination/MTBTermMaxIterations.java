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

/* 
 * Most recent change(s):
 * 
 * $Rev$
 * $Date$
 * $Author$
 * 
 */

package de.unihalle.informatik.MiToBo.segmentation.snakes.optimize.termination;

import de.unihalle.informatik.Alida.annotations.ALDClassParameter;
import de.unihalle.informatik.Alida.annotations.ALDDerivedClass;
import de.unihalle.informatik.Alida.annotations.ALDParametrizedClass;
import de.unihalle.informatik.MiToBo.segmentation.snakes.optimize.SnakeOptimizerSingle;
import de.unihalle.informatik.MiToBo.segmentation.snakes.optimize.SnakeOptimizer.Snake_status;

/**
 * Check upon snake termination by a given number of maximal iterations of the
 * snake. Termination is done when a maximum number of snake iterations is
 * reached.
 * 
 * @author misiak
 */
@ALDDerivedClass
@ALDParametrizedClass
public class MTBTermMaxIterations extends MTBTermination {

		/**
		 * Maximum number of iterations.
		 */
		@ALDClassParameter(label="Maximum Iteration Count")
		private int maxIterations = 100;

		/**
		 * Default constructor.
		 */
		public MTBTermMaxIterations() {
			// nothing to do here
		}
		
		/**
		 * Constructor with maximum number of iterations on which the snake
		 * optimization is terminated.
		 */
		public MTBTermMaxIterations(int _maxIterations) {
				this.maxIterations = _maxIterations;
		}

		@Override
		public boolean init(SnakeOptimizerSingle opt) {
				this.optimizer = opt;
				return true;
		}

		/**
		 * Termination is done when a maximum number of snake iterations is reached.
		 */
		@Override
		public Snake_status terminate() {
			// get current iteration count
			int itCounter = this.optimizer.getIterationCount();
			if (this.verbose)
				System.out.println("  Stop at " + this.maxIterations + " iterations.");
			// check if maximum iteration count is reached
			if (itCounter >= this.maxIterations) {
				if (this.verbose)
					System.out.println("\n--- Snake optimizer stopped: Snake done after "
						+ itCounter + " step(s) [maximum number of iterations reached].\n");
				return SnakeOptimizerSingle.Snake_status.SNAKE_DONE;
			}
			return SnakeOptimizerSingle.Snake_status.SNAKE_SUCCESS;
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
				return ("MTBTermMaxIteration [maxIterations="+this.maxIterations+"]");
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
	  public MTBTermMaxIterations clone() throws CloneNotSupportedException {
	  	MTBTermMaxIterations newObj = 
	  		new MTBTermMaxIterations(this.maxIterations);
	  	newObj.status = this.status;
	  	newObj.optimizer = null;
	  	return newObj;
		}
}
