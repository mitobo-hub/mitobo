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
package de.unihalle.informatik.MiToBo.tracking.multitarget.distributions.impl;

import java.util.Random;
import java.util.Vector;

import Jama.Matrix;
import de.unihalle.informatik.MiToBo.math.distributions.impl.GaussMixDistribution;
import de.unihalle.informatik.MiToBo.math.distributions.interfaces.FirstOrderMoment;
import de.unihalle.informatik.MiToBo.math.distributions.interfaces.SamplingDistribution;
import de.unihalle.informatik.MiToBo.math.distributions.interfaces.SecondOrderCentralMoment;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.abstracts.AbstractMultiState;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.abstracts.AbstractMultiStateFactory;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.interfaces.Copyable;
import de.unihalle.informatik.MiToBo.tracking.multitarget.distributions.abstracts.AbstractMultiObservationDistributionIndep;

/**
 * A simple multi observation density, which assumes independent Gaussian mixtures as the underlying distributions.
 * @author Oliver Gress
 *
 * @param <T> class type of the observations' and states' discrete variables
 */
public class MultiObsDistributionIndepGaussMix<T extends Copyable<?>>  extends AbstractMultiObservationDistributionIndep<T,T> 
				implements SamplingDistribution<AbstractMultiState<T>>, FirstOrderMoment<AbstractMultiState<T>>,
																SecondOrderCentralMoment<Matrix[]> {

		
	protected Vector<GaussMixDistribution> gaussmixtures;															
	protected Matrix H;
																	
	public MultiObsDistributionIndepGaussMix(Random rand,
			 Matrix H,
			 Vector<GaussMixDistribution> obsDistGaussMixtures,
			 AbstractMultiState<T> X,
			 AbstractMultiStateFactory<T> factoryX,
			 AbstractMultiStateFactory<T> factoryZ) throws IllegalArgumentException {
		super(X, factoryX, factoryZ);
		
		if (obsDistGaussMixtures.size() != X.getNumberOfStates()) {
			throw new IllegalArgumentException("MultiObsDistributionIndepGaussMix(): Vector of Gaussian mixtures must " +
					"contain the same number of elements as number of states in the condition X.");
		}
		
		this.gaussmixtures = obsDistGaussMixtures;
		this.H = H;
	}
				
	
	public GaussMixDistribution getGaussMixture(int i) {
		return this.gaussmixtures.get(i);
	}
	
	@Override
	public Matrix[] getCovariance() {
		Matrix[] covs = new Matrix[this.gaussmixtures.size()];
		for (int n = 0; n < covs.length; n++)
			covs[n] = this.gaussmixtures.get(n).getCovariance();
		
		return covs;
	}

	@SuppressWarnings("unchecked")
	@Override
	public AbstractMultiState<T> getMean() {
		AbstractMultiState<T> Z = this.factoryZ.createEmptyMultiState();
		
		for (int n = 0; n < this.condX.getNumberOfStates(); n++) {
			Matrix m = this.gaussmixtures.get(n).getMean();
			m.plusEquals(this.H.times(this.condX.getStateContinuous(n)));
			
			Z.insertState(m, (T)this.condX.getStateDiscrete(n).copy());
		}
		
		return Z;
	}

	@SuppressWarnings("unchecked")
	@Override
	public AbstractMultiState<T> drawSample() {
		
		AbstractMultiState<T> Z = this.factoryZ.createEmptyMultiState();
		
		for (int i = 0; i < this.condX.getNumberOfStates(); i++) {
			Z.insertState((this.gaussmixtures.get(i).drawSample()).plusEquals(this.H.times(this.condX.getStateContinuous(i))), 
					(T) this.condX.getStateDiscrete(i).copy());
		}
			
		return Z;
	}

	@Override
	public double p(AbstractMultiState<T> Z, int i, int j) {
		Matrix z = Z.getStateContinuous(i).copy();
		
		z.minusEquals(this.H.times(this.condX.getStateContinuous(j)));
		
		return this.gaussmixtures.get(j).p(z);
	}

	@Override
	public double p(AbstractMultiState<T> Z, int i) {
		Matrix z = Z.getStateContinuous(i).copy();
		
		z.minusEquals(this.H.times(this.condX.getStateContinuous(i)));
		
		return this.gaussmixtures.get(i).p(z);
	}

	@Override
	public double p(AbstractMultiState<T> Z) {
		
		AbstractMultiState<T> Zs = Z.copy();
		
		for (int i = 0; i < Z.getNumberOfStates(); i++) {
			Zs.getStateContinuous(i).minusEquals(this.H.times(this.condX.getStateContinuous(i)));
		}
		
		double p = 1;
		for (int i = 0; i < Zs.getNumberOfStates(); i++)
			p *= this.gaussmixtures.get(i).p(Zs.getStateContinuous(i));
		
		return p;
	}

	@Override
	public double log_p(AbstractMultiState<T> Z) {
		AbstractMultiState<T> Zs = Z.copy();
		
		for (int i = 0; i < Z.getNumberOfStates(); i++) {
			Zs.getStateContinuous(i).minusEquals(this.H.times(this.condX.getStateContinuous(i)));
		}
		
		double logp = 0;
		for (int i = 0; i < Zs.getNumberOfStates(); i++)
			logp += this.gaussmixtures.get(i).log_p(Zs.getStateContinuous(i));
		
		return logp;
	}

	@Override
	public double log_p(AbstractMultiState<T> Z, int i) {
		Matrix z = Z.getStateContinuous(i).copy();
		
		z.minusEquals(this.H.times(this.condX.getStateContinuous(i)));
		
		return this.gaussmixtures.get(i).log_p(z);
	}
	
	@Override
	public double log_p(AbstractMultiState<T> Z, int i, int j) {
		Matrix z = Z.getStateContinuous(i).copy();
		
		z.minusEquals(this.H.times(this.condX.getStateContinuous(j)));
		
		return this.gaussmixtures.get(j).log_p(z);
	}

	
	@Override
	public int getNumOfIndeps() {
		return this.gaussmixtures.size();
	}


}
