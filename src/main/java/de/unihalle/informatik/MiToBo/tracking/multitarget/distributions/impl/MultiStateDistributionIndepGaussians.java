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
import de.unihalle.informatik.MiToBo.math.LinearTransformGaussNoise;
import de.unihalle.informatik.MiToBo.math.distributions.impl.GaussianDistribution;
import de.unihalle.informatik.MiToBo.math.distributions.interfaces.EvaluatableDistribution;
import de.unihalle.informatik.MiToBo.math.distributions.interfaces.FirstOrderMoment;
import de.unihalle.informatik.MiToBo.math.distributions.interfaces.IndependentSamplingDistribution;
import de.unihalle.informatik.MiToBo.math.distributions.interfaces.IndependentlyEvaluatableDistribution;
import de.unihalle.informatik.MiToBo.math.distributions.interfaces.SamplingDistribution;
import de.unihalle.informatik.MiToBo.math.distributions.interfaces.SecondOrderCentralMoment;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.abstracts.AbstractMultiState;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.interfaces.Copyable;

/**
 * A simple multi state density, which assumes independence of the single states with
 * multivariate Gaussian noise.
 * @author Oliver Gress
 *
 * @param <T> class type of the multi-states' discrete variables
 */
public class MultiStateDistributionIndepGaussians<T extends Copyable<?>> implements EvaluatableDistribution<AbstractMultiState<T>>, 
														IndependentlyEvaluatableDistribution<AbstractMultiState<T>>, 
														SamplingDistribution<AbstractMultiState<T>>, 
														IndependentSamplingDistribution<AbstractMultiState<T>>, 
														FirstOrderMoment<AbstractMultiState<T>>,
														SecondOrderCentralMoment<Vector<Matrix>>,
														Copyable<MultiStateDistributionIndepGaussians<T>> {

	protected AbstractMultiState<T> mean;
	protected Vector<Matrix> covs;
	protected Vector<GaussianDistribution> gaussians;
	protected Random rand;
	
	protected MultiStateDistributionIndepGaussians(int numOfIndepGaussians) {
		this.covs = new Vector<Matrix>(numOfIndepGaussians);
		this.gaussians = new Vector<GaussianDistribution>(numOfIndepGaussians);
	}
	
	/**
	 * Constructor with identical covariance matrices for all states
	 */
	public MultiStateDistributionIndepGaussians(AbstractMultiState<T> mean, Matrix covariance, Random rand) {
		this.mean = mean;
		this.covs = new Vector<Matrix>(mean.getNumberOfStates());
		
		this.rand = rand;
		
		this.gaussians = new Vector<GaussianDistribution>(mean.getNumberOfStates());
		
		for (int i = 0; i < mean.getNumberOfStates(); i++) {
			this.covs.add(covariance.copy());
			this.gaussians.add(new GaussianDistribution(this.mean.getStateContinuous(i), this.covs.get(i), rand));
		}
			
	}

	/**
	 * Constructor with different covariance matrix for each state
	 */
	public MultiStateDistributionIndepGaussians(AbstractMultiState<T> mean, Vector<Matrix> covariance, Random rand) {
		this.mean = mean;
		this.covs = covariance;

		this.rand = rand;

		this.gaussians = new Vector<GaussianDistribution>(mean.getNumberOfStates());
		
		for (int i = 0; i < mean.getNumberOfStates(); i++) {
			this.gaussians.add(new GaussianDistribution(this.mean.getStateContinuous(i), this.covs.get(i), rand));
		}	
	}
	
	/**
	 * Add an independent Gaussian state distribution 
	 */
	public int addIndepGaussian(GaussianDistribution stateCont, T stateDiscr) {
		this.gaussians.add(stateCont);
		this.covs.add(stateCont.getCovariance());
		this.mean.insertState(stateCont.getMean(), stateDiscr);
		
		return this.gaussians.size()-1;
	}
	
	/**
	 * Remove independent Gaussian state distribution (at index i)
	 */
	public int removeIndepGaussian(int i) {
		
		if (i >= 0 && i < this.gaussians.size()) {
			this.gaussians.remove(i);
			this.covs.remove(i);
			this.mean.removeState(i);
			
			return i;
		}
		else
			return -1;
	}
	
	
	@Override
	public double p(AbstractMultiState<T> X) {
	
		if (mean.getNumberOfStates() != X.getNumberOfStates() || mean.getNumberOfStates() == 0)
			return -1.0;
		else {
			double p = 1.0;
			
			for (int i = 0; i < mean.getNumberOfStates(); i++) {
				p *= this.gaussians.get(i).p(X.getStateContinuous(i));
			}
			
			return p;
		}
	}
	
	@Override
	public double p(AbstractMultiState<T> X, int i) {
		return this.gaussians.get(i).p(X.getStateContinuous(i));
	}

	@SuppressWarnings("unchecked")
	@Override
	public AbstractMultiState<T> drawSample() {
		
		AbstractMultiState<T> X = mean.getFactory().createEmptyMultiState();
		
		for (int i = 0; i < mean.getNumberOfStates(); i++) {
			X.insertState(this.gaussians.get(i).drawSample(), (T) mean.getStateDiscrete(i).copy());
		}
			
		return X;
	}

	@Override
	public AbstractMultiState<T> drawSample(int i, AbstractMultiState<T> X) {
		AbstractMultiState<T> Xnew = X.copy();
		
		Xnew.setState(i, this.gaussians.get(i).drawSample(), Xnew.getStateDiscrete(i));
			
		return Xnew;
	}
	
	@Override
	public AbstractMultiState<T> getMean() {
		return this.mean;
	}

	@Override
	public Vector<Matrix> getCovariance() {
		return this.covs;
	}
	
	public void predict(LinearTransformGaussNoise predictor) {
		for (int i = 0; i < this.gaussians.size(); i++) {
			this.gaussians.set(i, predictor.transform(this.gaussians.get(i)));
			this.mean.setState(i, this.gaussians.get(i).getMean(), this.mean.getStateDiscrete(i));
			this.covs.set(i, this.gaussians.get(i).getCovariance());
		}
	}
	
	public void predictIndep(int i, LinearTransformGaussNoise predictor) {
		this.gaussians.set(i, predictor.transform(this.gaussians.get(i)));
		this.mean.setState(i, this.gaussians.get(i).getMean(), this.mean.getStateDiscrete(i));
		this.covs.set(i, this.gaussians.get(i).getCovariance());
	}

	public void update(LinearTransformGaussNoise projector, AbstractMultiState<T> observations) {
		GaussianDistribution pZ; 
		Matrix S,P,K,x;
		
		for (int i = 0; i < this.gaussians.size(); i++) {
			pZ = projector.transform(this.gaussians.get(i));
			
			S = pZ.getCovariance();
			P = this.gaussians.get(i).getCovariance();
			K = P.times((projector.getTransformMatrix().transpose()).times(S.inverse()));
			P.minusEquals(K.times(S.times(K.transpose())));
		
			x = this.gaussians.get(i).getMean();
			x.plusEquals(K.times(observations.getStateContinuous(i).minus(pZ.getMean())));
		
		}
	}
	
	public void updateIndep(int i, LinearTransformGaussNoise projector, AbstractMultiState<T> observations) {
		GaussianDistribution pZ = projector.transform(this.gaussians.get(i));
		
		Matrix S = pZ.getCovariance();
		Matrix P = this.gaussians.get(i).getCovariance();
		Matrix K = P.times((projector.getTransformMatrix().transpose()).times(S.inverse()));
		P.minusEquals(K.times(S.times(K.transpose())));
		
		Matrix x = this.gaussians.get(i).getMean();
		x.plusEquals(K.times(observations.getStateContinuous(i).minus(pZ.getMean())));
		
	}	
	
	/**
	 * Update i-th Gaussian component with j-th observation
	 * @param i
	 * @param j
	 * @param projector
	 * @param observations
	 */
	public void updateIndep(int i, int j, LinearTransformGaussNoise projector, AbstractMultiState<T> observations) {
		GaussianDistribution pZ = projector.transform(this.gaussians.get(i));
		
		Matrix S = pZ.getCovariance();
		Matrix P = this.gaussians.get(i).getCovariance();
		Matrix K = P.times((projector.getTransformMatrix().transpose()).times(S.inverse()));
		P.minusEquals(K.times(S.times(K.transpose())));
		
		Matrix x = this.gaussians.get(i).getMean();
		x.plusEquals(K.times(observations.getStateContinuous(j).minus(pZ.getMean())));
		
	}

	@Override
	public MultiStateDistributionIndepGaussians<T> copy() {
		MultiStateDistributionIndepGaussians<T> distrib = new MultiStateDistributionIndepGaussians<T>(this.gaussians.size());
		
		distrib.mean = this.mean.copy();
		distrib.rand = this.rand;
		
		for (int i = 0; i < this.gaussians.size(); i++) {
			distrib.covs.add(this.covs.get(i).copy());
			distrib.gaussians.add(new GaussianDistribution(distrib.mean.getStateContinuous(i), distrib.covs.get(i), distrib.rand));
			
		}
		
		return distrib;
	}
}
