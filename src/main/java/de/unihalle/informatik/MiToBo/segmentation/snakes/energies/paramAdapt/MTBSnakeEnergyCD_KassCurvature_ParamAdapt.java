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
import de.unihalle.informatik.MiToBo.segmentation.snakes.energies.MTBSnakeEnergyCD_KassCurvature;
import de.unihalle.informatik.MiToBo.segmentation.snakes.optimize.*;

/**
 * Parameter adaptation for Kass et al. curvature penalty.
 * 
 * @author moeller
 */
@ALDParametrizedClass
public abstract class MTBSnakeEnergyCD_KassCurvature_ParamAdapt {

	/**
	 * Initial beta value of the Kass curvature energy.
	 */
	protected double initBeta;

	/**
	 * Initialize the updater according to associated energy object.
	 */
	public abstract void init(MTBSnakeEnergyCD_KassCurvature ener);

	/**
	 * Update function for beta values.
	 * 
	 * @param o
	 *          current snake optimizer
	 * @param curBetas
	 *          current beta values
	 * @return New updated beta values.
	 */
	public abstract double[] betaUpdate(SnakeOptimizerSingle o,
	    double[] curBetas);

	/**
	 * Get the maximum value for parameter beta.
	 * 
	 * @return Maximum beta value.
	 */
	public abstract Double getMaxBeta();

	/**
	 * Get the minimum value for parameter beta.
	 * 
	 * @return Minimum beta value.
	 */
	public abstract Double getMinBeta();

	@Override
	public abstract String toString();
}
