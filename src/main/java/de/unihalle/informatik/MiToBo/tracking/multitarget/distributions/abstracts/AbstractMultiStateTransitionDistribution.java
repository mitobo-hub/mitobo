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
package de.unihalle.informatik.MiToBo.tracking.multitarget.distributions.abstracts;

import de.unihalle.informatik.MiToBo.math.distributions.interfaces.ConditionalDistribution;
import de.unihalle.informatik.MiToBo.math.distributions.interfaces.SamplingDistribution;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.abstracts.AbstractMultiState;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.abstracts.AbstractMultiStateFactory;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.interfaces.Copyable;

/**
 * Abstract class for multi-target state transition distributions. Used in the Bayesian tracking framework.
 * A distribution of this type represents the probability of a multi-target state X_t at time t given a certain multi-target 
 * state X_t-1 at time (t-1): p(X_t|X_t-1)
 * 
 * @author Oliver Gress
 *
 * @param <T> Type of discrete variables in the multi target state
 */
public abstract class AbstractMultiStateTransitionDistribution<T extends Copyable<?>> implements ConditionalDistribution<AbstractMultiState<T>>,
																		SamplingDistribution<AbstractMultiState<T>> {

	/** multi state condition on the density */
	protected AbstractMultiState<T> condX;
	
	/** multi state factory for condition variable, used for validity checks */
	protected AbstractMultiStateFactory<T> factoryX;

	
	/**
	 * Constructor to set the condition X, and the factories of multi state and multi observation variables 
	 * @param X
	 * @param factoryX
	 */
	public AbstractMultiStateTransitionDistribution(AbstractMultiState<T> X,
									AbstractMultiStateFactory<T> factoryX) {
		this.factoryX = factoryX;
		
		if (this.factoryX.validMultiState(X)) {
			this.condX = X;
		}
		else {
			throw new IllegalArgumentException("Invalid multistate condition. X must match factory specifications.");
		}
	}

	@Override
	public AbstractMultiState<T> getCondition() {
		return this.condX;
	}

	@Override
	public void setCondition(AbstractMultiState<T> X) throws IllegalArgumentException {
		if (this.factoryX.validMultiState(X)) {
			this.condX = X;
		}
		else {
			throw new IllegalArgumentException("Invalid multistate condition. X must match factory specifications.");
		}
	}

	@Override
	abstract public AbstractMultiState<T> drawSample();

}
