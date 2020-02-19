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

package de.unihalle.informatik.MiToBo.segmentation.snakes.energies.paramAdapt;

import de.unihalle.informatik.Alida.annotations.ALDDerivedClass;
import de.unihalle.informatik.Alida.annotations.ALDParametrizedClass;
import de.unihalle.informatik.MiToBo.segmentation.snakes.datatypes.MTBSnake;
import de.unihalle.informatik.MiToBo.segmentation.snakes.energies.MTBSnakeEnergyCD_KassLength;
import de.unihalle.informatik.MiToBo.segmentation.snakes.optimize.*;

/**
 * Dummy parameter adaptation for Kass et al. length penalty energy.
 * <p>
 * This class simply adapts the size of the vector, assuming the first 
 * value as the default.
 * 
 * @author moeller
 */
@ALDDerivedClass
@ALDParametrizedClass
public class MTBSnakeEnergyCD_KassLength_ParamAdaptNone extends
    MTBSnakeEnergyCD_KassLength_ParamAdapt {

		/**
		 * Default constructor.
		 */
		public MTBSnakeEnergyCD_KassLength_ParamAdaptNone() {
			// nothing to do here
		}

		/**
		 * Construct a new updater.
		 */
		public MTBSnakeEnergyCD_KassLength_ParamAdaptNone(double _iAlpha) {
				this.initAlpha = _iAlpha;
		}

		@Override
		public void init(MTBSnakeEnergyCD_KassLength ener) {
			this.initAlpha = ener.getInitAlpha();
		}
		
		/**
		 * Update function for alphas.
		 */
		@Override
		public double[] alphaUpdate(SnakeOptimizerSingle o, double[] alphas) {
				MTBSnake s = o.getCurrentSnake();
				if (s == null)
					return alphas;
				int pNum = s.getPointNum();
				if (pNum > 0) {
						double[] newAlphas = new double[pNum];
						for (int i = 0; i < pNum; ++i) {
								newAlphas[i] = alphas[0];
						}
						return newAlphas;
				}
				return alphas;
		}

		@Override
		public Double getMaxAlpha() {
				return (new Double(this.initAlpha));
		}

		@Override
		public Double getMinAlpha() {
				return (new Double(this.initAlpha));
		}

		@Override
		public String toString() {
				return new String("KassLength_ParamUpdate: none");
		}
}
