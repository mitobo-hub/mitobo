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

import java.util.Vector;

import de.unihalle.informatik.Alida.annotations.ALDDerivedClass;
import de.unihalle.informatik.Alida.annotations.ALDParametrizedClass;
import de.unihalle.informatik.MiToBo.segmentation.snakes.datatypes.MTBSnake;
import de.unihalle.informatik.MiToBo.segmentation.snakes.datatypes.MTBSnakePoint2D;
import de.unihalle.informatik.MiToBo.segmentation.snakes.energies.*;
import de.unihalle.informatik.MiToBo.segmentation.snakes.optimize.SnakeOptimizerSingle;
import de.unihalle.informatik.MiToBo.segmentation.snakes.optimize.SnakeOptimizerSingleVarCalc;

/**
 * Class for gamma adaptation depending on the external energy values for each
 * snake control point.
 * 
 * Adaptation calculation: Gamma-function acts like an root square function.
 * High energy value means high gamma value an low energy value means low gamma
 * value.
 * 
 * This adaptation strategy was developed for the distance map as external snake
 * energy.
 * 
 * 
 * @author misiak
 * 
 */
@ALDDerivedClass
@ALDParametrizedClass
public class MTBGammaPtWiseExtEner extends MTBGammaUpdate {

		/**
		 * Standardconstructor
		 */
		public MTBGammaPtWiseExtEner() {
				// nothing to do here
				this.adaptedGamma = null;
		}

		@Override
		public boolean init(SnakeOptimizerSingleVarCalc opt) {
				this.optimizer = opt;
				return true;
		}

		/**
		 * Gamma adaptation using a point wise adaptation, depending on the current
		 * snake and the external energy force.
		 * 
		 * Adaptation calculation: Gamma-function acts like an root square function.
		 * High energy value means high gamma value an low energy value means low
		 * gamma value.
		 * 
		 * This adaptation strategy was developed for the distance map as external
		 * snake energy.
		 */
		@Override
		public double[][] adaptGamma() {
				if(this.verbose)
				System.out.println("  Pointwise ext. energy \u03B3-update!");
				/*
				 * Current snake from optimizer. The gamma values for all the control points
				 * of the snake should be adapted using the external energy force.
				 */
				MTBSnake snake = (MTBSnake)this.optimizer.getCurrentSnakes().elementAt(0);
				this.adaptedGamma = new double[snake.getPointNum() * 2][1];
				// get energies from optimizer
				Vector<MTBSnakeEnergyDerivable> energies = this.optimizer.getEnergies();
				/*
				 * External energy, which is used by the current snake optimization and
				 * should be used in the gamma adaptation.
				 */
				MTBSnakeEnergyCDIB_Distance extEnergy = null;
				for (int i = 0; i < energies.size(); i++) {
						if (energies.get(i) instanceof MTBSnakeEnergyCDIB_Distance) {
								extEnergy = (MTBSnakeEnergyCDIB_Distance) energies.get(i);
								break;
						}
				}
				for (int i = 0; i < snake.getPointNum(); ++i) {
						// get current snake point
						MTBSnakePoint2D curPoint = snake.getSnakePoints().elementAt(i);
						/*
						 * Do gamma adaptation dependent on external energy for the current snake
						 * point.
						 */
						double eValue = extEnergy.getValue_norm(curPoint.x, curPoint.y);
						if (eValue > 0.0) {
								this.adaptedGamma[i][0] = (Math.sqrt(eValue))*25.0;
								this.adaptedGamma[i + snake.getPointNum()][0] = (Math.sqrt(eValue))*25.0;
						} else {
								this.adaptedGamma[i][0] = 0.0;
								this.adaptedGamma[i + snake.getPointNum()][0] = 0.0;
						}
				}
				// return adapted gamma values
				return this.adaptedGamma;
		}

		@Override
		public String toString() {
				return ("MTBGammaPtWiseExtEner");
		}

		/**
		 * Clones this object.
		 * <p>
		 * Note that the {@link SnakeOptimizerSingle} object is left null! You have to
		 * call {@link SnakeOptimizer.initOptimizer()} before using the gamma updater.
		 */
		/*
		 * (non-Javadoc)
		 * 
		 * @seede.unihalle.informatik.MiToBo.segmentation.snakes.optimize.stepsize.
		 * MTBGammaUpdate#clone()
		 */
		@Override
		public MTBGammaPtWiseExtEner clone() {
				MTBGammaPtWiseExtEner newObj = new MTBGammaPtWiseExtEner();
				newObj.optimizer = null;
				if (this.adaptedGamma != null)
						newObj.adaptedGamma = this.adaptedGamma.clone();
				return newObj;
		}

}
