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

package de.unihalle.informatik.MiToBo.segmentation.levelset.core.energies.derivable;

import de.unihalle.informatik.Alida.annotations.ALDParametrizedClass;
import de.unihalle.informatik.MiToBo.segmentation.levelset.core.DeltaApproxHelper;
import de.unihalle.informatik.MiToBo.segmentation.levelset.core.LevelsetSolverDerivatives;
import de.unihalle.informatik.MiToBo.segmentation.levelset.core.MTBLevelsetFunctionDerivable;
import de.unihalle.informatik.Alida.annotations.ALDDerivedClass;

/**
 * Energy minimizing the length of a level set's isocontour.
 * 
 * @author Michael Schneider
 */
@ALDDerivedClass
@ALDParametrizedClass
public class MTBLevelEnergyDerivable_Length 
	implements MTBLevelsetEnergyDerivable {

	/**
	 * Flag to indicate if the Heaviside function should be approximated.
	 */
	private boolean useHeavisideApprox = true;
	
	/**
	 * Helper object for calculating approximated values of Heaviside function.
	 */
	private DeltaApproxHelper deltaHelper;

	/**
	 * Default constructor.
	 */
	public MTBLevelEnergyDerivable_Length() {
		// nothing to do here
	}

	@Override
	public boolean initEnergy(LevelsetSolverDerivatives solver) {
		this.deltaHelper = solver.getDeltaApproximator();
		return true;
	}

	@Override
	public void updateStatus(MTBLevelsetFunctionDerivable phi) {
		// nothing to do here
	}

	@Override
	public double getDerivative(MTBLevelsetFunctionDerivable phi, 
			int x, int y, int z) {
		if (this.useHeavisideApprox) {
			return -this.deltaHelper.getValue(phi.get(x, y, z)) 
					* phi.getCurvature(x,y,z);
		}
		return -phi.getCurvature(x,y,z);
	}

	@Override
	public double getDerivativeWithoutDelta(
			MTBLevelsetFunctionDerivable phi, int x, int y, int z) {
		return -phi.getCurvature(x,y,z);
	}

	@Override
	public String validate() {
		return null;
	}

	@Override
	public void useHeavideApproximation(boolean flag) {
		this.useHeavisideApprox = flag;
	}
}
