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

import Jama.Matrix;
import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo;
import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo.ExportPolicy;
import de.unihalle.informatik.MiToBo.math.distributions.impl.GaussianDistribution;
import de.unihalle.informatik.MiToBo.math.distributions.interfaces.EvaluatableDistribution;
import de.unihalle.informatik.MiToBo.math.distributions.interfaces.FirstOrderMoment;
import de.unihalle.informatik.MiToBo.math.distributions.interfaces.IndependentlyEvaluatableDistribution;
import de.unihalle.informatik.MiToBo.math.distributions.interfaces.SecondOrderCentralMoment;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.abstracts.AbstractMultiState;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.abstracts.AbstractMultiStateFactory;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.interfaces.Copyable;
import de.unihalle.informatik.MiToBo.tracking.multitarget.distributions.abstracts.AbstractMultiStateTransitionDistributionIndep;

/**
 * A simple multi state-transition density, which assumes independence of the single states and
 * multivariate Gaussian process noise. Further, the noise covariance matrices are identical for each state.
 * @author Oliver Gress
 *
 * @param <T> class type of the multi-states' discrete variables
 */
@ALDMetaInfo(export=ExportPolicy.ALLOWED)
public class MultiStateLinTransDistributionIndepGaussians<T extends Copyable<?>> extends AbstractMultiStateTransitionDistributionIndep<T> 
									implements EvaluatableDistribution<AbstractMultiState<T>>, 
											   IndependentlyEvaluatableDistribution<AbstractMultiState<T>>,
											   FirstOrderMoment<AbstractMultiState<T>>,
											   SecondOrderCentralMoment<Matrix[]> {

	/** state-transition matrix */
	protected Matrix[] F;
	
	/** Gaussian process noise covariance matrix */
	protected Matrix[] Q;
	
	/** multivariate gaussian density object for evaluation */
	protected GaussianDistribution[] gaussian;
	
	/**
	 * 
	 * @param F state-transition linear transform matrix
	 * @param Q Gaussian process noise covariance matrix
	 * @param X condition state
	 * @param factoryX factory to determine multi-target state layout
	 * @throws IllegalArgumentException if any dimensions of the input objects do not match
	 */
	public MultiStateLinTransDistributionIndepGaussians(Random rand,
								 Matrix F,
								 Matrix Q,
								 AbstractMultiState<T> X,
								 AbstractMultiStateFactory<T> factoryX) throws IllegalArgumentException {
		super(X, factoryX);
		
		if (!factoryX.validMultiState(X)) {
			throw new IllegalArgumentException("Invalid condition: X must match the DOF specification of factoryX.");
		}
		else if (Q.getRowDimension() != Q.getColumnDimension()
				|| Q.getRowDimension() != factoryX.getContinuousDOF()) {
			throw new IllegalArgumentException("Invalid noise covariance matrix Q: Q must be square and match the continuous DOF specification of factoryX.");
		}
		else if (F.getRowDimension() != F.getColumnDimension() 
				|| F.getColumnDimension() != factoryX.getContinuousDOF()) {
			throw new IllegalArgumentException("Invalid state-transition matrix F: F must be square and match the continuous DOF specification of factoryX.");
		}
		
		this.F = new Matrix[X.getNumberOfStates()];
		this.Q = new Matrix[X.getNumberOfStates()];
		
		for (int i = 0; i < X.getNumberOfStates(); i++) {
			this.F[i] = F;
			this.Q[i] = Q;

			this.gaussian[i] = new GaussianDistribution(F.times(X.getStateContinuous(i)), Q, rand);
		}
	}
	
	/**
	 * 
	 * @param F state-transition linear transform matrices
	 * @param Q Gaussian process noise covariance matrices
	 * @param X condition state
	 * @param factoryX factory to determine multi-target state layout
	 * @throws IllegalArgumentException if any dimensions of the input objects do not match
	 */
	public MultiStateLinTransDistributionIndepGaussians(Random rand,
								 Matrix[] F,
								 Matrix[] Q,
								 AbstractMultiState<T> X,
								 AbstractMultiStateFactory<T> factoryX) throws IllegalArgumentException {
		super(X, factoryX);
		
		if (!factoryX.validMultiState(X)) {
			throw new IllegalArgumentException("Invalid condition: X must match the DOF specification of factoryX.");
		}
		if (X.getNumberOfStates() != F.length || F.length != Q.length) {
			throw new IllegalArgumentException("Arrays F and Q must have same length as the number of states in X.");
		}
		
		this.F = new Matrix[X.getNumberOfStates()];
		this.Q = new Matrix[X.getNumberOfStates()];
		
		for (int i = 0; i < X.getNumberOfStates(); i++) {
			

			if (Q[i].getRowDimension() != Q[i].getColumnDimension()
					|| Q[i].getRowDimension() != factoryX.getContinuousDOF()) {
				throw new IllegalArgumentException("Invalid noise covariance matrix Q: Q must be square and match the continuous DOF specification of factoryX.");
			}
			else if (F[i].getRowDimension() != F[i].getColumnDimension() 
					|| F[i].getColumnDimension() != factoryX.getContinuousDOF()) {
				throw new IllegalArgumentException("Invalid state-transition matrix F: F must be square and match the continuous DOF specification of factoryX.");
			}
			
			this.F[i] = F[i];
			this.Q[i] = Q[i];

			this.gaussian[i] = new GaussianDistribution(F[i].times(X.getStateContinuous(i)), Q[i], rand);
		}
	}
	
	/**
	 * 
	 * @param F state-transition linear transform matrix
	 * @param Q Gaussian process noise covariance matrix
	 * @param X condition state
	 * @param factoryX factory to determine multi-target state layout
	 * @throws IllegalArgumentException if any dimensions of the input objects do not match
	 */
	public MultiStateLinTransDistributionIndepGaussians(Random rand,
								 Matrix F,
								 Matrix Q,
								 MultiStateDistributionIndepGaussians<T> distribX,
								 AbstractMultiStateFactory<T> factoryX) throws IllegalArgumentException {
		super(distribX.getMean(), factoryX);
		
		this.condX = distribX.getMean();
		
		if (!factoryX.validMultiState(this.condX)) {
			throw new IllegalArgumentException("Invalid condition: X must match the DOF specification of factoryX.");
		}
		else if (Q.getRowDimension() != Q.getColumnDimension()
				|| Q.getRowDimension() != factoryX.getContinuousDOF()) {
			throw new IllegalArgumentException("Invalid noise covariance matrix Q: Q must be square and match the continuous DOF specification of factoryX.");
		}
		else if (F.getRowDimension() != F.getColumnDimension() 
				|| F.getColumnDimension() != factoryX.getContinuousDOF()) {
			throw new IllegalArgumentException("Invalid state-transition matrix F: F must be square and match the continuous DOF specification of factoryX.");
		}
		
		this.F = new Matrix[this.condX.getNumberOfStates()];
		this.Q = new Matrix[this.condX.getNumberOfStates()];
		
		Matrix[] P = (Matrix[]) distribX.getCovariance().toArray();
		
		for (int i = 0; i < this.condX.getNumberOfStates(); i++) {
			this.F[i] = F;
			this.Q[i] = (F.times(P[i].times(F.transpose()))).plus(Q);

			this.gaussian[i] = new GaussianDistribution(F.times(this.condX.getStateContinuous(i)), this.Q[i], rand);
		}
	}
	
	/**
	 * 
	 * @param F state-transition linear transform matrices
	 * @param Q Gaussian process noise covariance matrices
	 * @param X condition state
	 * @param factoryX factory to determine multi-target state layout
	 * @throws IllegalArgumentException if any dimensions of the input objects do not match
	 */
	public MultiStateLinTransDistributionIndepGaussians(Random rand,
								 Matrix[] F,
								 Matrix[] Q,
								 MultiStateDistributionIndepGaussians<T> distribX,
								 AbstractMultiStateFactory<T> factoryX) throws IllegalArgumentException {
		super(distribX.getMean(), factoryX);
		
		this.condX = distribX.getMean();
		
		if (!factoryX.validMultiState(this.condX)) {
			throw new IllegalArgumentException("Invalid condition: X must match the DOF specification of factoryX.");
		}
		if (this.condX.getNumberOfStates() != F.length || F.length != Q.length) {
			throw new IllegalArgumentException("Arrays F and Q must have same length as the number of states in X.");
		}
		
		this.F = new Matrix[this.condX.getNumberOfStates()];
		this.Q = new Matrix[this.condX.getNumberOfStates()];

		Matrix[] P = (Matrix[])distribX.getCovariance().toArray();
		
		for (int i = 0; i < this.condX.getNumberOfStates(); i++) {
			

			if (Q[i].getRowDimension() != Q[i].getColumnDimension()
					|| Q[i].getRowDimension() != factoryX.getContinuousDOF()) {
				throw new IllegalArgumentException("Invalid noise covariance matrix Q: Q must be square and match the continuous DOF specification of factoryX.");
			}
			else if (F[i].getRowDimension() != F[i].getColumnDimension() 
					|| F[i].getColumnDimension() != factoryX.getContinuousDOF()) {
				throw new IllegalArgumentException("Invalid state-transition matrix F: F must be square and match the continuous DOF specification of factoryX.");
			}
			
			this.F[i] = F[i];
			this.Q[i] = (F[i].times(P[i].times(F[i].transpose()))).plus(Q[i]);

			this.gaussian[i] = new GaussianDistribution(this.F[i].times(this.condX.getStateContinuous(i)), this.Q[i], rand);
		}
	}
	
	@Override
	public AbstractMultiState<T> drawSample() {
		
		AbstractMultiState<T> X = this.factoryX.createEmptyMultiState();//.condX.copy();
		
		for (int i = 0; i < this.condX.getNumberOfStates(); i++) {
			X.insertState(this.gaussian[i].drawSample(), (T) this.condX.getStateDiscrete(i).copy());
		}
		
		return X;
	}


	@Override
	public AbstractMultiState<T> drawSample(int i, AbstractMultiState<T> X) {
		AbstractMultiState<T> Xnew = X.copy();
		
		Xnew.setState(i, this.gaussian[i].drawSample(), Xnew.getStateDiscrete(i));
			
		return Xnew;
	}


	@Override
	public double p(AbstractMultiState<T> X, int i) {
		return this.gaussian[i].p(X.getStateContinuous(i));
	}


	@Override
	public double p(AbstractMultiState<T> X) {
		if (!this.factoryX.validMultiState(X)) {
			throw new IllegalArgumentException("Invalid variable X: X must match the DOF specification of factoryX.");
		}

		double p = 1.0;
		for (int i = 0; i < X.getNumberOfStates(); i++) {
				
			p *= this.gaussian[i].p(X.getStateContinuous(i));
		}
	
		return p;
	}


	@Override
	public AbstractMultiState<T> getMean() {
		AbstractMultiState<T> X = this.factoryX.createEmptyMultiState();
		
		for (int i = 0; i < X.getNumberOfStates(); i++) {
			X.insertState(this.gaussian[i].getMean().copy(), (T) this.condX.getStateDiscrete(i).copy());
		}
		
		return X;
	}

	@Override
	public Matrix[] getCovariance() {
		return this.Q;
	}
	
	@Override
	public void setCondition(AbstractMultiState<T> X) {
		if (X.getNumberOfStates() != this.gaussian.length)
			throw new IllegalArgumentException("MultiStateLinTransDistributionIndepGaussians.setCondition(.): " +
					"Condition must have the the same number of states as the number of independent Gaussians.");
		
		this.condX = X;
		
		for (int i = 0; i < this.gaussian.length; i++) {

			this.gaussian[i].setMean(this.F[i].times(X.getStateContinuous(i))) ;
		}
	}
	
	public Matrix[] getTransitionMatrices() {
		return this.F;
	}

}
