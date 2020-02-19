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

import java.util.Vector;

import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.abstracts.AbstractMultiState;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.interfaces.Copyable;

import Jama.Matrix;

/**
 * Multi-target state implementation. Continuous and discrete parts of states are stored in separate Java-Vectors.
 * @author Oliver Gress
 *
 * @param <T> type of the discrete variables
 */
public class MultiState<T extends Copyable<?>> extends AbstractMultiState<T> {

	/** vector of the target states (continuous variables) */
	Vector<Matrix> statesCont;
	
	/** vector of the target states (discrete variables) */
	Vector<T> statesDiscr;
	
	MultiState(MultiStateFactory<T> factory) {
		super(factory);
		
		this.statesCont = new Vector<Matrix>();
		this.statesDiscr = new Vector<T>();
	}
	
	@Override
	public int getNumberOfStates() {
		return this.statesCont.size();
	}

	@Override
	public Matrix getStateContinuous(int idx) {
		return this.statesCont.get(idx);
	}

	@Override
	public T getStateDiscrete(int idx) {
		return this.statesDiscr.get(idx);
	}

	@Override
	public void removeState(int idx) {
		this.statesCont.remove(idx);
		this.statesDiscr.remove(idx);
	}

	@Override
	public void insertState(Matrix stateContinuous, T stateDiscrete) throws IllegalArgumentException {
		
		if (stateContinuous.getRowDimension() != this.factory.getContinuousDOF()
				|| stateContinuous.getColumnDimension() != 1) {
			throw new IllegalArgumentException("State DOF does not match the input requirements. Matrix must be "+ this.factory.getContinuousDOF()
					+"x1.");
		}
		
		this.statesCont.add(stateContinuous);
		this.statesDiscr.add(stateDiscrete);
	}

	@Override
	public void setState(int idx, Matrix stateContinuous, T stateDiscrete) throws IllegalArgumentException {

		if (stateContinuous.getRowDimension() != this.factory.getContinuousDOF()
				|| stateContinuous.getColumnDimension() != 1) {
			throw new IllegalArgumentException("State DOF does not match the input requirements. Matrix must be "+ this.factory.getContinuousDOF()
					+"x1.");
		}
		
		this.statesCont.set(idx, stateContinuous);
		this.statesDiscr.set(idx, stateDiscrete);
	}

	@SuppressWarnings("unchecked")
	@Override
	public MultiState<T> copy() {
		MultiState<T> ms = new MultiState<T>((MultiStateFactory<T>)this.factory);
		
		for (int i = 0; i < this.statesCont.size(); i++) {
			ms.statesCont.add(this.statesCont.get(i).copy());
			ms.statesDiscr.add((T) this.statesDiscr.get(i).copy());
		}

		return ms;
	}
	
}
