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

package de.unihalle.informatik.MiToBo.segmentation.levelset.core;

import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.MiToBo.segmentation.levelset.core.DeltaApproxHelper.ApproxVersion;
import de.unihalle.informatik.MiToBo.segmentation.levelset.core.MTBLevelsetFunctionDerivable;

/**
 * Level set solver using variational calculus.
 * 
 * @author Martin Scharm
 * @author Michael Schneider
 * @author Birgit Moeller
 * 
 */
public abstract class LevelsetSolverDerivatives extends LevelsetSolver {

	/**
	 * Set of energies to apply.
	 */
	@Parameter(label = "Energies", required = true, dataIOOrder = 0, 
		direction = Parameter.Direction.IN, 
		description = "Level set energies.")
	protected MTBSet_LevelEnergyDerivable energySet = null;

  /**
   * Time step size in iterative optimization.
   */
  @Parameter(label = "Step Size", required = false, dataIOOrder = -10, 
 		direction = Parameter.Direction.IN, 
 		description = "Step size in optimization.")
  protected double deltaT = 1;
  
  /**
   * Helper to calculate values of approximated Dirac impulse function.
   */
  @Parameter( label = "Approximating Function", required = true, 
 		direction = Parameter.Direction.IN, dataIOOrder = 20,
 		description = "Function to use for approximation, see paper for details.")
  protected DeltaApproxHelper.ApproxVersion approxFunction = 
  	ApproxVersion.VERSION_1; 

  /**
   * Epsilon value for approximation of Heaviside function.
   */
  @Parameter(label = "Heaviside Approximation Parameter", required = false,  
 		direction = Parameter.Direction.IN, dataIOOrder = -9,
 		description = "Value of epsilon in approximation of Heaviside function.")
  protected double epsilon = 1;

  /*
   * some internally used member variables...
   */
  
	/**
	 * Level set function.
	 */
	protected MTBLevelsetFunctionDerivable phi;

	/**
	 * Helper to calculate approximated values for the Dirac function.
	 */
	protected DeltaApproxHelper deltaApproximator;
	
	/**
	 * Default constructor.
	 * @throws ALDOperatorException
	 */
	protected LevelsetSolverDerivatives() throws ALDOperatorException {
		// initialize delta approximation helper
		this.deltaApproximator =
			new DeltaApproxHelper(this.epsilon, this.approxFunction);
	}

	/**
	 * Get set of energies.
	 * @return	Energy set.
	 */
	public MTBSet_LevelEnergyDerivable getEnergySet() {
		return this.energySet;
	}
	
	/**
	 * Get step size.
	 * @return	Size of one time step.
	 */
	public double getDeltaT() {
		return this.deltaT;
	}
	
	/**
	 * Get reference to delta function approximator.
	 * @return	Reference to helper object.
	 */
	public DeltaApproxHelper getDeltaApproximator() {
		return this.deltaApproximator;
	}
	
	/**
	 * Get epsilon for Heaviside function approximation.
	 * @return	Value of epsilon.
	 */
	public double getEpsilon() {
		return this.epsilon;
	}
	
	/**
	 * Get current level set function.
	 * @return	Level set function.
	 */
	public MTBLevelsetFunctionDerivable getPhi() {
		return this.phi;
	}
}
