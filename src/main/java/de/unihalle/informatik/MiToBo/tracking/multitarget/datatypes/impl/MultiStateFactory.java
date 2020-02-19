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

package de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.impl;

import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.abstracts.AbstractMultiState;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.abstracts.AbstractMultiStateFactory;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.interfaces.Copyable;
import Jama.Matrix;

/**
 * Multi-target state factory implementation.
 * 
 * @author Oliver Gress
 *
 * @param <T> type of the discrete variables
 */
public class MultiStateFactory<T extends Copyable<?>> extends AbstractMultiStateFactory<T> {

	public MultiStateFactory(int continuousDOF) {
		super(continuousDOF);
	}

	@Override
	public AbstractMultiState<T> createEmptyMultiState() {
		return new MultiState<T>(this);
	}

	@Override
	public AbstractMultiState<T> createMultiState(
			double[][] continuousStateVariables, T[] discreteStateVariables)
			throws IllegalArgumentException {

		MultiState<T> ms = new MultiState<T>(this);
		
		if (continuousStateVariables.length != discreteStateVariables.length) {
			throw new IllegalArgumentException("Size of (outer) arrays do not match.");
		}
		
		try {
			for (int i = 0; i < continuousStateVariables.length; i++) {
				ms.insertState(new Matrix(continuousStateVariables[i], continuousStateVariables[i].length), 
							   discreteStateVariables[i]);
			}
			
		} catch(IllegalArgumentException e) {
			throw e;
		}

		return ms;
	}

	@Override
	public AbstractMultiState<T> createMultiState(
			Matrix[] continuousStateVariables, T[] discreteStateVariables)
			throws IllegalArgumentException {

		MultiState<T> ms = new MultiState<T>(this);
		
		if (continuousStateVariables.length != discreteStateVariables.length) {
			throw new IllegalArgumentException("Size of (outer) arrays do not match.");
		}
		
		try {
			for (int i = 0; i < continuousStateVariables.length; i++) {
				ms.insertState(continuousStateVariables[i], discreteStateVariables[i]);
			}
			
		} catch(IllegalArgumentException e) {
			throw e;
		}

		return ms;
	}

}
