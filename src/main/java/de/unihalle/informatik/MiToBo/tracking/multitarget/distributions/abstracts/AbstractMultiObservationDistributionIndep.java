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

import Jama.Matrix;
import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo;
import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo.ExportPolicy;
import de.unihalle.informatik.MiToBo.math.distributions.interfaces.FirstOrderMoment;
import de.unihalle.informatik.MiToBo.math.distributions.interfaces.IndependentlyEvaluatableDistribution;
import de.unihalle.informatik.MiToBo.math.distributions.interfaces.LogIndependentlyEvaluatableDistribution;
import de.unihalle.informatik.MiToBo.math.distributions.interfaces.SecondOrderCentralMoment;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.abstracts.AbstractMultiState;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.abstracts.AbstractMultiStateFactory;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.interfaces.Copyable;

/**
 * Abstract class for multi target observation distributions. Used in the Bayesian tracking framework.
 * A distribution of this type represents the probability of a multi target observation given a certain multi target 
 * state X: p(Z|X)
 * The distribution can be evaluated independently for components in Z
 * 
 * @author Oliver Gress
 *
 * @param <S> Type of discrete variables in the multi target observation
 * @param <T> Type of discrete variables in the multi target state
 */
@ALDMetaInfo(export=ExportPolicy.ALLOWED)
public abstract class AbstractMultiObservationDistributionIndep<S extends Copyable<?>,T extends Copyable<?>> extends
		AbstractMultiObservationDistribution<S,T> implements IndependentlyEvaluatableDistribution<AbstractMultiState<S>>,
		LogIndependentlyEvaluatableDistribution<AbstractMultiState<S>>, FirstOrderMoment<AbstractMultiState<T>>,
		SecondOrderCentralMoment<Matrix[]>
		{

	/**
	 * Constructor to set the condition conditionX, and the factories of multi state and multi observation variables 
	 * @param conditionX
	 * @param factoryX
	 * @param factoryZ
	 */
	public AbstractMultiObservationDistributionIndep(AbstractMultiState<T> conditionX,
			AbstractMultiStateFactory<T> factoryX,
			AbstractMultiStateFactory<S> factoryZ) {
		super(conditionX, factoryX, factoryZ);
		
	}
	
	/**
	 * Evaluate the density independently for observation i in Z conditional on state j in X
	 * @param x
	 * @param i
	 * @param j
	 * @return
	 */
	public abstract double p(AbstractMultiState<S> Z, int i, int j);
	
	/**
	 * Evaluate the density independently for observation i in Z conditional on state i in X
	 */
	@Override
	public abstract double p(AbstractMultiState<S> Z, int i);

	@Override
	public abstract double p(AbstractMultiState<S> Z);
	
	
	/**
	 * Evaluate the density independently for observation i in Z conditional on state j in X
	 * @param x
	 * @param i
	 * @param j
	 * @return
	 */
	public abstract double log_p(AbstractMultiState<S> Z, int i, int j);
	
	/**
	 * Evaluate the density independently for observation i in Z conditional on state i in X
	 */
	@Override
	public abstract double log_p(AbstractMultiState<S> Z, int i);

	@Override
	public abstract double log_p(AbstractMultiState<S> Z);	
	

	public abstract int getNumOfIndeps();

}
