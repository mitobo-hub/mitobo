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
package de.unihalle.informatik.MiToBo.math.distributions.impl;

import java.util.Random;

import Jama.CholeskyDecomposition;
import Jama.Matrix;
import de.unihalle.informatik.MiToBo.math.distributions.interfaces.FirstOrderMoment;
import de.unihalle.informatik.MiToBo.math.distributions.interfaces.LogProbabilityDensityFunction;
import de.unihalle.informatik.MiToBo.math.distributions.interfaces.ProbabilityDensityFunction;
import de.unihalle.informatik.MiToBo.math.distributions.interfaces.SamplingDistribution;
import de.unihalle.informatik.MiToBo.math.distributions.interfaces.SecondOrderCentralMoment;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.interfaces.Copyable;

/**
 * A multivariate Gaussian distribution.
 * 
 * @author Oliver Gress
 *
 */
public class GaussianDistribution implements ProbabilityDensityFunction, LogProbabilityDensityFunction, SamplingDistribution<Matrix>,
		FirstOrderMoment<Matrix>, SecondOrderCentralMoment<Matrix>, Copyable<GaussianDistribution> {

	/** mean vector */
	protected Matrix mean;
	
	/** covariance matrix */
	protected Matrix cov;
	
	/** inverse covariance matrix */
	protected Matrix icov;
	
	/** random generator for sampling */
	protected Random rand;
	
	/** normalization factor */
	protected double normfactor;
	
	/** log of the normalization factor */
	protected double logfactor;

	protected Matrix L;
	
	/**
	 * Constructor for a Gaussian distribution of dimension DOF
	 * with the zero vector as mean, the unity matrix as covariance matrix
	 * and a new random generator for sampling
	 * @param DOF
	 */
	protected GaussianDistribution(int DOF) {
		this(DOF, new Random());
	}
	
	/**
	 * Constructor for a Gaussian distribution of dimension DOF
	 * with the zero vector as mean, the unity matrix as covariance matrix
	 * and a given random generator for sampling
	 * @param DOF
	 * @param rand
	 */
	public GaussianDistribution(int DOF, Random rand) {
		this.rand = rand;
		this.mean = new Matrix(DOF, 1);
		this.cov = new Matrix(DOF, DOF);
		for (int i = 0; i < DOF; i++)
			this.cov.set(i, i, 1.0);
		
		this.L = this.cov.copy();
		
		this.icov = this.cov.copy();
		
		this.normfactor = 1.0/(Math.pow(2*Math.PI, mean.getRowDimension()/2.0));
		this.logfactor = - mean.getRowDimension()/2.0*Math.log(2.0*Math.PI);
	}
	
	/**
	 * Gaussian distribution with given mean, covariance and a new random generator for sampling
	 * @param mean
	 * @param covariance
	 * @throws IllegalArgumentException
	 */
	public GaussianDistribution(Matrix mean, Matrix covariance) throws IllegalArgumentException {
		this(mean, covariance, new Random());
	}
	
	/**
	 * Gaussian distribution with given mean, covariance and random generator for sampling
	 * @param mean
	 * @param covariance
	 * @param rand
	 * @throws IllegalArgumentException
	 */
	public GaussianDistribution(Matrix mean, Matrix covariance, Random rand) throws IllegalArgumentException {
		
		if (mean.getColumnDimension() != 1
				|| mean.getRowDimension() != covariance.getColumnDimension()
				|| covariance.getRowDimension() != covariance.getColumnDimension()) {
			throw new IllegalArgumentException("Matrix sizes are invalid: mean must be of size Nx1, covariance of size NxN. " +
					"Actual sizes are: mean " + mean.getRowDimension() +"x"+ mean.getColumnDimension() +" and "+
					"covariance "+ covariance.getRowDimension() +"x"+ covariance.getColumnDimension());
		}
		
		this.rand = rand;
		this.mean = mean;
		this.cov = covariance;
		
		CholeskyDecomposition chol = new CholeskyDecomposition(this.cov);
		//if (!chol.isSPD()) {
		//	throw new IllegalArgumentException("Covariance matrix is not valid: Not positive semi-definite.");
		//}
		this.L = chol.getL();
		
		this.icov = this.cov.inverse();
		
		this.normfactor = 1.0/(Math.pow(2*Math.PI, mean.getRowDimension()/2.0) * Math.sqrt(this.cov.det()));
		this.logfactor = - mean.getRowDimension()/2.0*Math.log(2.0*Math.PI) - 0.5*Math.log(this.cov.det());
	}
	
	/**
	 * Returns the mean vector.
	 */
	@Override
	public Matrix getMean() {
		return this.mean;
	}
	
	public void setMean(Matrix mean) throws IllegalArgumentException {
		
		if (mean.getRowDimension() != this.mean.getRowDimension()) {
			throw new IllegalArgumentException("Invalid DOF: Dimensions must not change.");
		}
		
		this.mean = mean;
	}
	
	/**
	 * Returns the covariance matrix
	 * @return
	 */
	@Override
	public Matrix getCovariance() {
		return this.cov;
	}
	
	public Matrix getInverseCovariance() {
		return this.icov;
	}
	
	public void setCovariance(Matrix covariance) throws IllegalArgumentException {
		
		if (covariance.getRowDimension() != this.cov.getRowDimension()) {
			throw new IllegalArgumentException("Invalid DOF: Dimensions must not change.");
		}
		else if (covariance.getRowDimension() != covariance.getColumnDimension()) {
			throw new IllegalArgumentException("Covariance matrix must be square.");
		}
		
		this.cov = covariance;		
		
		CholeskyDecomposition chol = new CholeskyDecomposition(this.cov);
		if (!chol.isSPD()) {
			throw new IllegalArgumentException("Covariance matrix is not valid: Not positive semi-definite.");
		}
		this.L = chol.getL();
		
		this.icov = this.cov.inverse();
		
		this.normfactor = 1.0/(Math.pow(2*Math.PI, mean.getRowDimension()/2.0) * Math.sqrt(this.cov.det()));
		this.logfactor = - mean.getRowDimension()/2.0*Math.log(2.0*Math.PI) - 0.5*Math.log(this.cov.det());
	}
	
	@Override
	public double log_p(Matrix x) {
		Matrix x_ = x.plus(mean.times(-1.0));
		Matrix cx_ = this.icov.times(x_);
		Matrix xcx_ = x_.transpose().times(cx_);
		return logfactor - 0.5*xcx_.get(0, 0);
	}

	@Override
	public double p(Matrix x) {
		Matrix x_ = x.plus(mean.times(-1.0));
		Matrix cx_ = this.icov.times(x_);
		Matrix xcx_ = x_.transpose().times(cx_);
		return this.normfactor * Math.exp(-0.5*xcx_.get(0, 0));
	}

	@Override
	public Matrix drawSample() {
		
		Matrix x = new Matrix(mean.getRowDimension(), 1);
		
		for (int i = 0; i < x.getRowDimension(); i++) {
			x.set(i, 0, rand.nextGaussian());
		}
		
		x = L.times(x);
		x.plusEquals(this.mean);
		
		return x;
	}

	public double mahalanobis(Matrix x) {
		Matrix x_ = x.plus(mean.times(-1.0));
		Matrix cx_ = this.icov.times(x_);
		Matrix xcx_ = x_.transpose().times(cx_);
		return 0.5*xcx_.get(0, 0);
	}

	@Override
	public GaussianDistribution copy() {
		return new GaussianDistribution(this.mean.copy(), this.cov.copy(), this.rand);
	}
	
}
