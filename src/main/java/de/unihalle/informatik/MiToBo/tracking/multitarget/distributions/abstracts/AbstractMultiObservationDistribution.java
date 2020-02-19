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
package de.unihalle.informatik.MiToBo.tracking.multitarget.distributions.abstracts;

import de.unihalle.informatik.MiToBo.math.distributions.interfaces.ConditionalDistribution;
import de.unihalle.informatik.MiToBo.math.distributions.interfaces.EvaluatableDistribution;
import de.unihalle.informatik.MiToBo.math.distributions.interfaces.LogEvaluatableDistribution;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.abstracts.AbstractMultiState;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.abstracts.AbstractMultiStateFactory;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.interfaces.Copyable;

/**
 * Abstract class for multi target observation distributions. Used in the Bayesian tracking framework.
 * A distribution of this type represents the probability of a multi target observation given a certain multi target 
 * state X: p(Z|X)
 * 
 * @author Oliver Gress
 *
 * @param <S> Type of discrete variables in the multi target observation
 * @param <T> Type of discrete variables in the multi target state
 */
public abstract class AbstractMultiObservationDistribution<S extends Copyable<?>,T extends Copyable<?>> 
			implements EvaluatableDistribution<AbstractMultiState<S>>,  LogEvaluatableDistribution<AbstractMultiState<S>>,  
			ConditionalDistribution<AbstractMultiState<T>> {

	/** multi state condition on the density */
	protected AbstractMultiState<T> condX;
	
	/** multi state factory for condition variable, used for validity checks */
	protected AbstractMultiStateFactory<T> factoryX;
	
	/** multi state factory for observation variable, used for validity checks */
	protected AbstractMultiStateFactory<S> factoryZ;
	
	/**
	 * Constructor to set the condition conditionX, and the factories of multi state and multi observation variables 
	 * @param conditionX
	 * @param factoryX
	 * @param factoryZ
	 */
	public AbstractMultiObservationDistribution(AbstractMultiState<T> conditionX,
									AbstractMultiStateFactory<T> factoryX,
									AbstractMultiStateFactory<S> factoryZ) {
		this.factoryX = factoryX;
		this.factoryZ = factoryZ;
		
		if (this.factoryX.validMultiState(conditionX)) {
			this.condX = conditionX;
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
	public abstract double p(AbstractMultiState<S> Z);
	
	@Override
	public abstract double log_p(AbstractMultiState<S> Z);
}
