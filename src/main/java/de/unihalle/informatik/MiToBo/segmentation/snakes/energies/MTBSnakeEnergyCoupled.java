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

package de.unihalle.informatik.MiToBo.segmentation.snakes.energies;

import de.unihalle.informatik.Alida.annotations.ALDParametrizedClass;
import de.unihalle.informatik.MiToBo.segmentation.activecontours.datatypes.MTBActiveContourEnergy;
import de.unihalle.informatik.MiToBo.segmentation.activecontours.exceptions.MTBSnakeException;
import de.unihalle.informatik.MiToBo.segmentation.snakes.optimize.SnakeOptimizerCoupled;

/**
 * Super class for energies working simultaneously on multiple snakes.
 * 
 * @author moeller
 */
@ALDParametrizedClass
public interface MTBSnakeEnergyCoupled extends MTBActiveContourEnergy {

	/**
	 * Init routine which is called once before the energy is actually used.
	 * <p>
	 * In this routine global parameter settings can be handled or other 
	 * initialization stuff be done. Snake optimizers are supposed to call this 
	 * routine once before they actual use of the energy.
	 * If no stuff needs to be done in advance the routine should at least 
	 * return true.
	 * 
	 * @param o		Calling snake optimizer.
	 * @return	true	if init was successful
	 */
	public abstract boolean initEnergy(SnakeOptimizerCoupled o) 
		throws MTBSnakeException;
	
  /**
   * Update internal state of energy object prior to querying 
   * derivatives and so on.
   */
  public abstract void updateStatus(SnakeOptimizerCoupled o) 
  	throws MTBSnakeException;
  
	/**
	 * Ask energy if contour points need to sorted counter-clockwise.
	 * @return	If true, the energy expects a counter-clockwise sorting.
	 */
	public abstract boolean requiresCounterClockwiseContourSorting();
	
	/**
	 * Ask energy if an overlap mask for all snakes jointly optimized is required.
	 * @return	If true, the energy expects an overlap mask to be available.
	 */
	public abstract boolean requiresOverlapMask();
}
