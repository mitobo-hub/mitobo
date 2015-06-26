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
import de.unihalle.informatik.MiToBo.segmentation.activecontours.datatypes.MTBActiveContourEnergyDerivable;
import de.unihalle.informatik.MiToBo.segmentation.activecontours.exceptions.MTBLevelsetException;
import de.unihalle.informatik.MiToBo.segmentation.levelset.core.LevelsetSolverDerivatives;
import de.unihalle.informatik.MiToBo.segmentation.levelset.core.MTBLevelsetFunctionDerivable;

/**
* Interface specifying level set energies to be used with techniques of the
* calculus of variations.
* <p>
* In general energies implementing this interface are required to allow for 
* calculation of partial derivatives at given positions.
*
* @author Martin Scharm
* @author Birgit Moeller
*/
@ALDParametrizedClass
public interface MTBLevelsetEnergyDerivable 
	extends MTBActiveContourEnergyDerivable {
	
  /**
   * Initializes the energy object according to given solver settings.
   * @param solver		Reference to solver which uses this energy.
   * @return	True, if initialization was successful.
   * @throws MTBLevelsetException
   */
  public abstract boolean initEnergy(LevelsetSolverDerivatives solver)
  	throws MTBLevelsetException;

  /**
   * Function to validate configuration of energy object.
   * @return	If null, everything is ok, otherwise an error message.
   */
  public abstract String validate();

  /**
	 * Function which updates the internal status of the energy.
	 * @param phi		Current level set function.
	 * @throws MTBLevelsetException
	 */
	public abstract void updateStatus(MTBLevelsetFunctionDerivable phi)
		throws MTBLevelsetException;
        
  /**
   * Get the energy derivative value, i.e. velocity, for position (x,y,z).
   * 
   * @param phi		Level set function.
   * @param x		x coordinate of requested position.
   * @param y		y coordinate of requested position.
   * @param z		z coordinate of requested position.
   * @return	Derivative value at given location.
   */
  public abstract double getDerivative(
  	MTBLevelsetFunctionDerivable phi, int x, int y, int z);
	
  /**
   * Get the energy derivative value, i.e. velocity, for position (x,y,z).
   * <p>
   * Here the value of the Dirac impulse function is ignored (if present).
   * 
   * @param phi		Level set function.
   * @param x		x coordinate of requested position.
   * @param y		y coordinate of requested position.
   * @param z		z coordinate of requested position.
   * @return	Derivative value without Dirac factor at given location.
   */
  public abstract double getDerivativeWithoutDelta(
    	MTBLevelsetFunctionDerivable phi, int x, int y, int z);
  		
  /**
   * Enable or disable approximation of Heaviside function.
   * @param flag	If true, approximation is enabled, otherwise disabled.
   */
  public abstract void useHeavideApproximation(boolean flag);
}
