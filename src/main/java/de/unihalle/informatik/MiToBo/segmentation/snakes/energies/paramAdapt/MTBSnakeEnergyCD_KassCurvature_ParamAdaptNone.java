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

package de.unihalle.informatik.MiToBo.segmentation.snakes.energies.paramAdapt;

import de.unihalle.informatik.Alida.annotations.ALDDerivedClass;
import de.unihalle.informatik.Alida.annotations.ALDParametrizedClass;
import de.unihalle.informatik.MiToBo.segmentation.snakes.datatypes.MTBSnake;
import de.unihalle.informatik.MiToBo.segmentation.snakes.energies.MTBSnakeEnergyCD_KassCurvature;
import de.unihalle.informatik.MiToBo.segmentation.snakes.optimize.*;

/**
 * Dummy parameter adaptation for Kass et al. curvature energy.
 * <p>
 * This class simply adapts the size of the vector, assuming the first value
 * as the default.
 * 
 * @author moeller
 */
@ALDDerivedClass
@ALDParametrizedClass
public class MTBSnakeEnergyCD_KassCurvature_ParamAdaptNone extends
    MTBSnakeEnergyCD_KassCurvature_ParamAdapt {

		/**
		 * Default constructor.
		 */
		public MTBSnakeEnergyCD_KassCurvature_ParamAdaptNone() {
			// nothing to do here
		}

		/**
		 * Construct a new updater.
		 */
		public MTBSnakeEnergyCD_KassCurvature_ParamAdaptNone(double _iBeta) {
				this.initBeta = _iBeta;
		}

		@Override
		public void init(MTBSnakeEnergyCD_KassCurvature ener) {
			this.initBeta = ener.getInitBeta();
		}

		/**
		 * Update function for betas.
		 */
		@SuppressWarnings("null")
		@Override
		public double[] betaUpdate(SnakeOptimizerSingle o, double[] betas) {
				MTBSnake s = o.getCurrentSnake();
				int pNum = s.getPointNum();
				if (s != null && pNum > 0) {
						double[] newBetas = new double[pNum];
						for (int i = 0; i < pNum; ++i) {
								newBetas[i] = betas[0];
						}
						return newBetas;
				}
				return null;
		}

		@Override
		public Double getMaxBeta() {
				return (new Double(this.initBeta));
		}

		@Override
		public Double getMinBeta() {
				return (new Double(this.initBeta));
		}

		@Override
		public String toString() {
				return new String("Kass_ParamUpdate: none");
		}
}
