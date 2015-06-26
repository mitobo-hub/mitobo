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
import de.unihalle.informatik.MiToBo.math.distributions.interfaces.FirstOrderMoment;
import de.unihalle.informatik.MiToBo.math.distributions.interfaces.SamplingDistribution;
import de.unihalle.informatik.MiToBo.math.distributions.interfaces.SecondOrderCentralMoment;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.abstracts.AbstractMultiState;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.abstracts.AbstractMultiStateFactory;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.interfaces.Copyable;
import de.unihalle.informatik.MiToBo.tracking.multitarget.distributions.abstracts.AbstractMultiObservationDistributionIndep;

/**
 * A simple multi observation density, which assumes independence of the single observations with
 * multivariate Gaussian noise. Further, the noise covariance matrices are identical for each object.
 * Number of targets in observation and state must be equal! 
 * @author Oliver Gress
 *
 * @param <T> class type of the observations' and states' discrete variables
 */
@ALDMetaInfo(export=ExportPolicy.ALLOWED)
public class MultiObsDistributionIndepGaussians<T extends Copyable<?>> extends AbstractMultiObservationDistributionIndep<T,T> 
					implements SamplingDistribution<AbstractMultiState<T>>, FirstOrderMoment<AbstractMultiState<T>>,
					SecondOrderCentralMoment<Matrix[]> {
	
	/** state-to-observation-space linear transform matrix */
	protected Matrix[] H;
	
	/** Gaussian measurement noise covariance matrix */
	protected Matrix[] R;
	
	/** multivariate gaussian density object for evaluation */
	protected GaussianDistribution[] gaussian;

	/**
	 * 
	 * @param H state-to-observation-space linear transform matrix
	 * @param R Gaussian noise covariance matrix
	 * @param X condition state
	 * @param factoryX factory to determine multi-target state layout
	 * @param factoryZ factory to determine multi-target observation layout
	 * @throws IllegalArgumentException if any dimensions of the input objects do not match
	 */
	public MultiObsDistributionIndepGaussians(Random rand,
								 Matrix H,
								 Matrix R,
								 AbstractMultiState<T> X,
								 AbstractMultiStateFactory<T> factoryX,
								 AbstractMultiStateFactory<T> factoryZ) throws IllegalArgumentException {
		super(X, factoryX, factoryZ);
		
		if (!factoryX.validMultiState(X)) {
			throw new IllegalArgumentException("Invalid condition: X must match the DOF specification of factoryX.");
		}
		else if (R.getRowDimension() != R.getColumnDimension()
				|| R.getRowDimension() != factoryZ.getContinuousDOF()) {
			throw new IllegalArgumentException("Invalid noise covariance matrix R: R must be square and match the continuous DOF specification of factoryZ.");
		}
		else if (H.getRowDimension() != factoryZ.getContinuousDOF() 
				|| H.getColumnDimension() != factoryX.getContinuousDOF()) {
			throw new IllegalArgumentException("Invalid state-to-observation-space matrix H: " +
					"H must match the continuous DOF specification of factoryZ in row dimension and " +
					"continuous DOF specification of factoryX in column dimension.");
		}
		
		
		this.H = new Matrix[X.getNumberOfStates()];
		this.R = new Matrix[X.getNumberOfStates()];
		
		this.gaussian = new GaussianDistribution[X.getNumberOfStates()];
		
		for (int i = 0; i < this.H.length; i++) {
			this.H[i] = H;
			this.R[i] = R;
			this.gaussian[i] = new GaussianDistribution(this.H[i].times(X.getStateContinuous(i)), R, rand);
		}
	}

	/**
	 * 
	 * @param H state-to-observation-space linear transform matrices
	 * @param R Gaussian noise covariance matrices
	 * @param X condition state
	 * @param factoryX factory to determine multi-target state layout
	 * @param factoryZ factory to determine multi-target observation layout
	 * @throws IllegalArgumentException if any dimensions of the input objects do not match
	 */
	public MultiObsDistributionIndepGaussians(Random rand,
								 Matrix[] H,
								 Matrix[] R,
								 AbstractMultiState<T> X,
								 AbstractMultiStateFactory<T> factoryX,
								 AbstractMultiStateFactory<T> factoryZ) throws IllegalArgumentException {
		super(X, factoryX, factoryZ);
		
		if (!factoryX.validMultiState(X)) {
			throw new IllegalArgumentException("Invalid condition: X must match the DOF specification of factoryX.");
		}

		if (X.getNumberOfStates() != H.length || H.length != R.length) {
			throw new IllegalArgumentException("Arrays H and R must have same length as the number of states in X.");
		}
		
		this.H = new Matrix[X.getNumberOfStates()];
		this.R = new Matrix[X.getNumberOfStates()];
		
		this.gaussian = new GaussianDistribution[X.getNumberOfStates()];
		
		for (int i = 0; i < this.H.length; i++) {
			
			if (R[i].getRowDimension() != R[i].getColumnDimension()
					|| R[i].getRowDimension() != factoryZ.getContinuousDOF()) {
				throw new IllegalArgumentException("Invalid noise covariance matrix R: R must be square and match the continuous DOF specification of factoryZ.");
			}
			else if (H[i].getRowDimension() != factoryZ.getContinuousDOF() 
					|| H[i].getColumnDimension() != factoryX.getContinuousDOF()) {
				throw new IllegalArgumentException("Invalid state-to-observation-space matrix H: " +
						"H must match the continuous DOF specification of factoryZ in row dimension and " +
						"continuous DOF specification of factoryX in column dimension.");
			}
			
			this.H[i] = H[i];
			this.R[i] = R[i];
			this.gaussian[i] = new GaussianDistribution(this.H[i].times(X.getStateContinuous(i)), R[i], rand);
		}
	}
	
	/**
	 * 
	 * @param H state-to-observation-space linear transform matrix
	 * @param R Gaussian noise covariance matrix
	 * @param distribX A Gaussian state distribution
	 * @param factoryX factory to determine multi-target state layout
	 * @param factoryZ factory to determine multi-target observation layout
	 * @throws IllegalArgumentException if any dimensions of the input objects do not match
	 */
	public MultiObsDistributionIndepGaussians(Random rand,
								 Matrix H,
								 Matrix R,
								 MultiStateDistributionIndepGaussians<T> distribX,
								 AbstractMultiStateFactory<T> factoryX,
								 AbstractMultiStateFactory<T> factoryZ) throws IllegalArgumentException {
		super(distribX.getMean(), factoryX, factoryZ);
		
		this.condX = distribX.getMean();
		
		if (!factoryX.validMultiState(this.condX)) {
			throw new IllegalArgumentException("Invalid condition: Factory of distribX must match the DOF specification of factoryX.");
		}
		else if (R.getRowDimension() != R.getColumnDimension()
				|| R.getRowDimension() != factoryZ.getContinuousDOF()) {
			throw new IllegalArgumentException("Invalid noise covariance matrix R: R must be square and match the continuous DOF specification of factoryZ.");
		}
		else if (H.getRowDimension() != factoryZ.getContinuousDOF() 
				|| H.getColumnDimension() != factoryX.getContinuousDOF()) {
			throw new IllegalArgumentException("Invalid state-to-observation-space matrix H: " +
					"H must match the continuous DOF specification of factoryZ in row dimension and " +
					"continuous DOF specification of factoryX in column dimension.");
		}
		
		this.H = new Matrix[this.condX.getNumberOfStates()];
		this.R = new Matrix[this.condX.getNumberOfStates()];
		
		Matrix[] P = (Matrix[])distribX.getCovariance().toArray();
		
		this.gaussian = new GaussianDistribution[this.condX.getNumberOfStates()];
		
		for (int i = 0; i < this.H.length; i++) {

			
			this.H[i] = H;
			this.R[i] = (this.H[i].times(P[i].times(this.H[i].transpose()))).plus(R);
			this.gaussian[i] = new GaussianDistribution(this.H[i].times(this.condX.getStateContinuous(i)), this.R[i], rand);
		}
		
		
	}
	
	/**
	 * 
	 * @param H state-to-observation-space linear transform matrices
	 * @param R Gaussian noise covariance matrices
	 * @param X condition state
	 * @param factoryX factory to determine multi-target state layout
	 * @param factoryZ factory to determine multi-target observation layout
	 * @throws IllegalArgumentException if any dimensions of the input objects do not match
	 */
	public MultiObsDistributionIndepGaussians(Random rand,
								 Matrix[] H,
								 Matrix[] R,
								 MultiStateDistributionIndepGaussians<T> distribX,
								 AbstractMultiStateFactory<T> factoryX,
								 AbstractMultiStateFactory<T> factoryZ) throws IllegalArgumentException {
		super(distribX.getMean(), factoryX, factoryZ);
		
		this.condX = distribX.getMean();
		
		if (!factoryX.validMultiState(this.condX)) {
			throw new IllegalArgumentException("Invalid condition: Factory of distribX must match the DOF specification of factoryX.");
		}

		if (this.condX.getNumberOfStates() != H.length || H.length != R.length) {
			throw new IllegalArgumentException("Arrays H and R must have same length as the number of states in X.");
		}
		
		this.H = new Matrix[this.condX.getNumberOfStates()];
		this.R = new Matrix[this.condX.getNumberOfStates()];
		
		Matrix[] P = (Matrix[]) distribX.getCovariance().toArray();
		
		this.gaussian = new GaussianDistribution[this.condX.getNumberOfStates()];
		
		for (int i = 0; i < this.H.length; i++) {
			
			if (R[i].getRowDimension() != R[i].getColumnDimension()
					|| R[i].getRowDimension() != factoryZ.getContinuousDOF()) {
				throw new IllegalArgumentException("Invalid noise covariance matrix R: R must be square and match the continuous DOF specification of factoryZ.");
			}
			else if (H[i].getRowDimension() != factoryZ.getContinuousDOF() 
					|| H[i].getColumnDimension() != factoryX.getContinuousDOF()) {
				throw new IllegalArgumentException("Invalid state-to-observation-space matrix H: " +
						"H must match the continuous DOF specification of factoryZ in row dimension and " +
						"continuous DOF specification of factoryX in column dimension.");
			}
			
			this.H[i] = H[i];
			this.R[i] = R[i];
			this.gaussian[i] = new GaussianDistribution(this.H[i].times(this.condX.getStateContinuous(i)), 
					  (this.H[i].times(P[i].times(this.H[i].transpose()))).plus(R[i]), rand);
		}
		
		
	}
	
	
	/**
	 * 
	 * @param H state-to-observation-space linear transform matrix
	 * @param R Gaussian noise covariance matrix
	 * @param transdistribX A Gaussian state distribution
	 * @param factoryX factory to determine multi-target state layout
	 * @param factoryZ factory to determine multi-target observation layout
	 * @throws IllegalArgumentException if any dimensions of the input objects do not match
	 */
	public MultiObsDistributionIndepGaussians(Random rand,
								 Matrix H,
								 Matrix R,
								 MultiStateLinTransDistributionIndepGaussians<T> transdistribX,
								 AbstractMultiStateFactory<T> factoryX,
								 AbstractMultiStateFactory<T> factoryZ) throws IllegalArgumentException {
		super(transdistribX.getMean(), factoryX, factoryZ);
		
		this.condX = transdistribX.getMean();
		
		if (!factoryX.validMultiState(this.condX)) {
			throw new IllegalArgumentException("Invalid condition: Factory of distribX must match the DOF specification of factoryX.");
		}
		else if (R.getRowDimension() != R.getColumnDimension()
				|| R.getRowDimension() != factoryZ.getContinuousDOF()) {
			throw new IllegalArgumentException("Invalid noise covariance matrix R: R must be square and match the continuous DOF specification of factoryZ.");
		}
		else if (H.getRowDimension() != factoryZ.getContinuousDOF() 
				|| H.getColumnDimension() != factoryX.getContinuousDOF()) {
			throw new IllegalArgumentException("Invalid state-to-observation-space matrix H: " +
					"H must match the continuous DOF specification of factoryZ in row dimension and " +
					"continuous DOF specification of factoryX in column dimension.");
		}
		
		this.H = new Matrix[this.condX.getNumberOfStates()];
		this.R = new Matrix[this.condX.getNumberOfStates()];
		
		Matrix[] P = transdistribX.getCovariance();
		
		this.gaussian = new GaussianDistribution[this.condX.getNumberOfStates()];
		
		for (int i = 0; i < this.H.length; i++) {

			
			this.H[i] = H;
			this.R[i] = (this.H[i].times(P[i].times(this.H[i].transpose()))).plus(R);
			this.gaussian[i] = new GaussianDistribution(this.H[i].times(this.condX.getStateContinuous(i)), this.R[i], rand);
		}
		
		
	}
	
	/**
	 * 
	 * @param H state-to-observation-space linear transform matrices
	 * @param R Gaussian noise covariance matrices
	 * @param X condition state
	 * @param factoryX factory to determine multi-target state layout
	 * @param factoryZ factory to determine multi-target observation layout
	 * @throws IllegalArgumentException if any dimensions of the input objects do not match
	 */
	public MultiObsDistributionIndepGaussians(Random rand,
								 Matrix[] H,
								 Matrix[] R,
								 MultiStateLinTransDistributionIndepGaussians<T> transdistribX,
								 AbstractMultiStateFactory<T> factoryX,
								 AbstractMultiStateFactory<T> factoryZ) throws IllegalArgumentException {
		super(transdistribX.getMean(), factoryX, factoryZ);
		
		this.condX = transdistribX.getMean();
		
		if (!factoryX.validMultiState(this.condX)) {
			throw new IllegalArgumentException("Invalid condition: Factory of distribX must match the DOF specification of factoryX.");
		}

		if (this.condX.getNumberOfStates() != H.length || H.length != R.length) {
			throw new IllegalArgumentException("Arrays H and R must have same length as the number of states in X.");
		}
		
		this.H = new Matrix[this.condX.getNumberOfStates()];
		this.R = new Matrix[this.condX.getNumberOfStates()];
		
		Matrix[] P = transdistribX.getCovariance();
		
		this.gaussian = new GaussianDistribution[this.condX.getNumberOfStates()];
		
		for (int i = 0; i < this.H.length; i++) {
			
			if (R[i].getRowDimension() != R[i].getColumnDimension()
					|| R[i].getRowDimension() != factoryZ.getContinuousDOF()) {
				throw new IllegalArgumentException("Invalid noise covariance matrix R: R must be square and match the continuous DOF specification of factoryZ.");
			}
			else if (H[i].getRowDimension() != factoryZ.getContinuousDOF() 
					|| H[i].getColumnDimension() != factoryX.getContinuousDOF()) {
				throw new IllegalArgumentException("Invalid state-to-observation-space matrix H: " +
						"H must match the continuous DOF specification of factoryZ in row dimension and " +
						"continuous DOF specification of factoryX in column dimension.");
			}
			
			this.H[i] = H[i];
			this.R[i] = R[i];
			this.gaussian[i] = new GaussianDistribution(this.H[i].times(this.condX.getStateContinuous(i)), 
					  (this.H[i].times(P[i].times(this.H[i].transpose()))).plus(R[i]), rand);
		}
		
		
	}
	
	public double logp(AbstractMultiState<T> Z) throws IllegalArgumentException {
		if (!factoryZ.validMultiState(Z)) {
			throw new IllegalArgumentException("Invalid variable Z: Z must match the DOF specification of factoryZ.");
		}
	
		double logp = 0.0;
		
		for (int i = 0; i < Z.getNumberOfStates(); i++) {
			logp += this.gaussian[i].log_p(Z.getStateContinuous(i));
		}
		
		return logp;
	}

	@Override
	public double p(AbstractMultiState<T> Z) throws IllegalArgumentException {
		if (!factoryZ.validMultiState(Z)) {
			throw new IllegalArgumentException("Invalid variable Z: Z must match the DOF specification of factoryZ.");
		}

		double p = 1.0;
		for (int i = 0; i < Z.getNumberOfStates(); i++) {

			p *= this.gaussian[i].p(Z.getStateContinuous(i));
		}
	
		return p;
	}
	
	@Override
	public double p(AbstractMultiState<T> Z, int i) {
		return this.gaussian[i].p(Z.getStateContinuous(i));
	}
	
	@Override
	public double p(AbstractMultiState<T> Z, int i, int j) {
		return this.gaussian[j].p(Z.getStateContinuous(i));
	}

	@Override
	public double log_p(AbstractMultiState<T> Z) {
		
		double logp = 0.0;
		
		for (int i = 0; i < Z.getNumberOfStates(); i++) {
			logp += this.gaussian[i].log_p(Z.getStateContinuous(i));
		}
		
		return logp;
	}
	
	@Override
	public double log_p(AbstractMultiState<T> Z, int i) {
		return this.gaussian[i].log_p(Z.getStateContinuous(i));
	}
	
	@Override
	public double log_p(AbstractMultiState<T> Z, int i, int j) {
		return this.gaussian[j].p(Z.getStateContinuous(i));
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public AbstractMultiState<T> getMean() {
		AbstractMultiState<T> Z = this.factoryZ.createEmptyMultiState();
		
		for (int i = 0; i < this.condX.getNumberOfStates(); i++) {
			Z.insertState(this.gaussian[i].getMean(), (T) this.condX.getStateDiscrete(i).copy());
		}

		return Z;
	}

	@SuppressWarnings("unchecked")
	@Override
	public AbstractMultiState<T> drawSample() {
		
		AbstractMultiState<T> Z = this.factoryZ.createEmptyMultiState();
		
		for (int i = 0; i < this.condX.getNumberOfStates(); i++) {
			Z.insertState(this.gaussian[i].drawSample(), (T) this.condX.getStateDiscrete(i).copy());
		}
			
		return Z;
	}

	@Override
	public Matrix[] getCovariance() {
		return this.R;
	}
	
	@Override
	public void setCondition(AbstractMultiState<T> X) {
		if (X.getNumberOfStates() != this.gaussian.length)
			throw new IllegalArgumentException("MultiObsDistributionIndepGaussians.setCondition(.): " +
					"Condition must have the the same number of states as the number of independent Gaussians.");
		
		this.condX = X;
		
		for (int i = 0; i < this.H.length; i++) {

			this.gaussian[i].setMean(this.H[i].times(X.getStateContinuous(i))) ;
		}
	}

	public Matrix[] getObservationMatrices() {
		return this.H;
	}

	@Override
	public int getNumOfIndeps() {
		return this.gaussian.length;
	}


}
