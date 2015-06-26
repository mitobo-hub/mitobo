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

import de.unihalle.informatik.Alida.annotations.ALDParametrizedClass;
import de.unihalle.informatik.MiToBo.segmentation.snakes.energies.MTBSnakeEnergyCD_KassLength;
import de.unihalle.informatik.MiToBo.segmentation.snakes.optimize.*;

/**
 * Parameter adaptation for Kass et al. length penalty.
 * 
 * @author moeller
 */
@ALDParametrizedClass
public abstract class MTBSnakeEnergyCD_KassLength_ParamAdapt {

	/**
	 * Initial alpha value of the internal Kass energy.
	 */
	protected double initAlpha;

	/**
	 * Initialize the updater according to associated energy object.
	 */
	public abstract void init(MTBSnakeEnergyCD_KassLength ener);
	
	/**
	 * Update function for alpha values.
	 * 
	 * @param o
	 *          Current snake optimizer.
	 * @param curAlphas
	 *          Current alpha values.
	 * @return New updated alpha values.
	 */
	public abstract double[] alphaUpdate(SnakeOptimizerSingle o,
			double[] curAlphas);

	/**
	 * Get the maximum value for parameter alpha.
	 * 
	 * @return Maximum alpha value.
	 */
	public abstract Double getMaxAlpha();

	/**
	 * Get the minimum value for parameter alpha.
	 * 
	 * @return Minimum alpha value.
	 */
	public abstract Double getMinAlpha();

	@Override
	public abstract String toString();
}
