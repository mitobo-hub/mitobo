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

import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo;
import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo.ExportPolicy;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.interfaces.Copyable;
import Jama.Matrix;

/**
 * Abstract class to hold the states of multiple targets. A target's state is comprised of a continuous part, i.e. a
 * column vector represented by a JAMA matrix, as well as a discrete part, which e.g. can hold target ID and further information.
 * 
 * @author Oliver Gress
 *
 * @param <T> discrete part of a target's state
 */
@ALDMetaInfo(export=ExportPolicy.ALLOWED)
public abstract class AbstractMultiState<T extends Copyable<?>> implements Copyable<AbstractMultiState<T>> {
	
	/** factory that holds information how a target's state is composed */
	protected AbstractMultiStateFactory<T> factory;
	
	/**
	 * Constructor that assigns a factory
	 */
	public AbstractMultiState(AbstractMultiStateFactory<T> factory) {
		this.factory = factory;
	}
	
	/** Get continuous dimensions of target state in continuous */
	public int getContinuousDOF() {
		return this.factory.getContinuousDOF();
	}
	
	/** Get associated factory */
	public AbstractMultiStateFactory<T> getFactory() {
		return this.factory;
	}
	
	/** Get number of states */
	abstract public int getNumberOfStates();

	/** Get the continuous part of the idx-th state (indices start from 0) */
	abstract public Matrix getStateContinuous(int idx);

	/** Get the discrete part of the idx-th state (indices start from 0) */
	abstract public T getStateDiscrete(int idx);
	
	/** Insert an additional state */
	abstract public void insertState(Matrix stateContinuous, T stateDiscrete) throws IllegalArgumentException;
	
	/** Set the idx-th state (indices start from 0) */
	abstract public void setState(int idx, Matrix stateContinuous, T stateDiscrete) throws IllegalArgumentException;
	
	/** Remove the idx-th state (indices start from 0) */
	abstract public void removeState(int idx);
	
	/** Copy this multi-state */
	abstract public AbstractMultiState<T> copy();
}
