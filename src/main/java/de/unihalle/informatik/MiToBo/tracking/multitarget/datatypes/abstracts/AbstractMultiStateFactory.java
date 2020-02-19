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
 * $Rev: 5288 $
 * $Date: 2012-03-29 10:27:02 +0200 (Thu, 29 Mar 2012) $
 * $Author: gress $
 * 
 */

package de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.abstracts;

import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.interfaces.Copyable;
import Jama.Matrix;

/**
 * Factory class for creating new multi state objects
 * @author Oliver Gress
 *
 * @param <T> type of the discrete state variables
 */
public abstract class AbstractMultiStateFactory<T extends Copyable<?>> {
	
	/** degrees of freedom of the continuous variables of a single state */
	protected int cDOF;
	
	
	/**
	 * Constructor with specification of the variables of a single state
	 * @param continuousDOF number of continuous state variables
	 * @param discreteDOF number of discrete state variables
	 */
	public AbstractMultiStateFactory(int continuousDOF) {
		this.cDOF = continuousDOF;
	}

	/**
	 * Get number of continuous state variables
	 * @return
	 */
	public int getContinuousDOF() {
		return this.cDOF;
	}
	
	/**
	 * Test if the specified multistate is valid for this factory. This means that continuous and discrete DOF of
	 * the multi state must be equal to the DOFs specified in this factory.
	 * 
	 * @param multistate
	 * @return true if DOFs of multi state and factory match
	 */
	public boolean validMultiState(AbstractMultiState<T> multistate) {
		return (multistate.getContinuousDOF() == this.cDOF);
	}
	
	/**
	 * Create an empty multi state object
	 * @return multi state object with no states
	 */
	abstract public AbstractMultiState<T> createEmptyMultiState();
	
	/**
	 * Create a multi state object initialized by the specified data
	 * @param continuousStateVariables array of double arrays specifying the continuous variable values of each single state
	 * @param discreteStateVariables array of type T specifying the discrete variable values of each single state
	 * @return initialized multi state object
	 * @throws IllegalArgumentException thrown if the outer dimension of the two arrays does not match or if the size of the single state arrays have invalid size
	 */
	abstract public AbstractMultiState<T> createMultiState(double[][] continuousStateVariables, T[] discreteStateVariables) throws IllegalArgumentException;
	
	/**
	 * Create a multi state object initialized by the specified data
	 * @param continuousStateVariables array of Matrix objects specifying the continuous variable values of each single state
	 * @param discreteStateVariables array of type T specifying the discrete variable values of each single state
	 * @return initialized multi state object
	 * @throws IllegalArgumentException thrown if the outer dimension of the two arrays does not match or if the size of the single state arrays have invalid size
	 */
	abstract public AbstractMultiState<T> createMultiState(Matrix[] continuousStateVariables, T[] discreteStateVariables) throws IllegalArgumentException;
}
