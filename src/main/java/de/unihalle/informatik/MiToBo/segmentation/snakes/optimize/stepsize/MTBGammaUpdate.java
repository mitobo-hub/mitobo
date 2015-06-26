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
import de.unihalle.informatik.Alida.annotations.ALDParametrizedClass;
import de.unihalle.informatik.MiToBo.segmentation.snakes.optimize.*;

/**
 * Class for adaptive step size calculation of the snake step size gamma.
 * <p>
 * The step size can be adapted by several methods, which can be found in the
 * derived classes in detail.
 * 
 * @author misiak
 */
@ALDParametrizedClass
public abstract class MTBGammaUpdate implements Cloneable {
	
	/**
	 * Flag to turn display of additional information on/off.
	 */
	@ALDClassParameter(label="Verbose", dataIOOrder = 100)
	protected boolean verbose = false;

	/**
	 * Current snake optimizer with all its parameters and fields like the 
	 * current snake, old snake, gamma values and energies.
	 */
	protected transient SnakeOptimizerSingleVarCalc optimizer;
	/**
	 * The new and adapted gamma values for all snake points.
	 */
	protected transient double[][] adaptedGamma;

	/**
	 * Method for gamma adaptation. Necessary for all subclasses. If no gamma
	 * adaptation should be made, the method is doing nothing.
	 * 
	 * @return New adapted gamma values.
	 */
	public abstract double[][] adaptGamma();

	/**
	 * Initializing routine which is called once before the gamma adaptation is
	 * actually used.
	 * <p>
	 * In this routine global parameter settings can be handled or other
	 * initialization stuff be done. The SnakeOptimizer will call this routine
	 * once before the actual use of the gamma update strategy. If no stuff needs
	 * to be done in advance the routine should at least return true.
	 * 
	 * @param opt
	 *          calling snake optimizer
	 * @return True if init was successful.
	 */
	public abstract boolean init(SnakeOptimizerSingleVarCalc opt);

	/**
	 * Method for short adaptation description name.
	 */
	@Override
	public abstract String toString();

	/**
	 * Clones this object.
	 * <p>
	 * Note that all internal variables should be cloned. As this is not 
	 * possible for the {@link SnakeOptimizerSingle} object the internal 
	 * reference should be left to null. It will be properly initialized 
	 * later when {@link SnakeOptimizerSingle.initOptimizer()} is invoked.
	 */
	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	@Override
	public abstract MTBGammaUpdate clone() throws CloneNotSupportedException;
}
