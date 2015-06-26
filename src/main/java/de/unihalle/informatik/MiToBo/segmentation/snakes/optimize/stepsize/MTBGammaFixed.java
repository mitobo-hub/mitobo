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

package de.unihalle.informatik.MiToBo.segmentation.snakes.optimize.stepsize;

import de.unihalle.informatik.Alida.annotations.ALDClassParameter;
import de.unihalle.informatik.Alida.annotations.ALDDerivedClass;
import de.unihalle.informatik.Alida.annotations.ALDParametrizedClass;
import de.unihalle.informatik.MiToBo.segmentation.snakes.datatypes.*;
import de.unihalle.informatik.MiToBo.segmentation.snakes.optimize.*;

/**
 * Class for gamma adaptation with a fixed decreasing factor for all gamma
 * values.
 * 
 * 
 * @author misiak
 */
@ALDDerivedClass
@ALDParametrizedClass
public class MTBGammaFixed extends MTBGammaUpdate {

	/**
	 * Fixed decrease factor for adaptation of all gamma values.
	 */
	@ALDClassParameter(label="Decrease Factor")
	private double decrease;

		/**
		 * Default constructor.
		 */
		public MTBGammaFixed() {
			this.adaptedGamma = null;
		}
		
		/**
		 * Constructor with fixed decreasing factor for adaptation.
		 * 
		 * @param decreaseFactor
		 *          decreasing value for gamma adaptation.
		 */
		public MTBGammaFixed(double decreaseFactor) {
				this.decrease = decreaseFactor;
				this.adaptedGamma = null;
		}

		@Override
		public boolean init(SnakeOptimizerSingleVarCalc opt) {
				this.optimizer = opt;
				return true;
		}

		/**
		 * Gamma adaptation using fixed decreasing factor for all gamma values.
		 * Adaptation calculation: oldValue - (oldValue * decreasingFactor).
		 */
		@Override
		public double[][] adaptGamma() {
			if (this.verbose)
				System.out.println("  Decreasing \u03B3 with fixed value " 
						+	this.decrease	+ " !");

			// get current gamma values from optimizer
			double[][] curGamma = this.optimizer.getCurGamma();
			// get current snake from optimizer
			MTBSnake snake = (MTBSnake)this.optimizer.getCurrentSnakes().elementAt(0);
			// initialize adapted gamma vector
			this.adaptedGamma = new double[snake.getPointNum() * 2][1];
			// calculate new adapted gamma values
			for (int i = 0; i < snake.getPointNum() * 2; i++) {
				this.adaptedGamma[i][0] = curGamma[0][0]
						- (curGamma[0][0] * this.decrease);
			}
			// return adapted gamma values
			return this.adaptedGamma;
		}

		@Override
		public String toString() {
				return ("MTBGammaFixed [decrease=" + this.decrease + "]");
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
	  public MTBGammaFixed clone() throws CloneNotSupportedException {
	  	MTBGammaFixed newObj = new MTBGammaFixed(this.decrease);
	  	newObj.optimizer = null;
	  	if (this.adaptedGamma != null)
	  		newObj.adaptedGamma = this.adaptedGamma.clone();
	  	return newObj;
		}
}
