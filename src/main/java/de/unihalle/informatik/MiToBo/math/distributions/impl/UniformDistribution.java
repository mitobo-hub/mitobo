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

import Jama.Matrix;
import de.unihalle.informatik.MiToBo.math.distributions.interfaces.FirstOrderMoment;
import de.unihalle.informatik.MiToBo.math.distributions.interfaces.LogProbabilityDensityFunction;
import de.unihalle.informatik.MiToBo.math.distributions.interfaces.ProbabilityDensityFunction;
import de.unihalle.informatik.MiToBo.math.distributions.interfaces.SamplingDistribution;

/**
 * A multivariate uniform distribution.
 * 
 * @author Oliver Gress
 *
 */
public class UniformDistribution implements ProbabilityDensityFunction, LogProbabilityDensityFunction,
		SamplingDistribution<Matrix>, FirstOrderMoment<Matrix> {
	
	protected Random rand;
	
	protected Matrix mean;
	
	protected double[] llimits, ulimits;
	
	protected double p;
	
	protected double log_p;
	
	protected double vol;
	
	protected int DOF;
	
	
	/**
	 * Constructor for uniform distribution with hypercube shape in dimension DOF, given lower and upper limits in the
	 * corresponding dimension and a random generator for sampling.
	 * Throws IllegalArgumentException, if the limit arrays are not of length DOF.
	 * @param DOF degrees of freedom
	 * @param lowerlimits lower limits for each dimension
	 * @param upperlimits upper limits for each dimension
	 * @param rand random generator
	 */
	public UniformDistribution(int DOF, double[] lowerlimits, double[] upperlimits, Random rand) {
	
		if (lowerlimits.length != DOF || upperlimits.length != DOF) 
			throw new IllegalArgumentException("UniformDistribution(..): Limit arrays must be of size DOF");
		
		this.DOF = DOF;
		this.rand = rand;
		this.mean = new Matrix(DOF, 1);
		this.llimits = lowerlimits;
		this.ulimits = upperlimits;
		
		this.vol = 1.0;
		for (int i = 0; i < DOF; i++) {
			this.mean.set(i, 0, (this.ulimits[i] + this.llimits[i])/2.0);
			this.vol *= Math.abs(this.ulimits[i] - this.llimits[i]);
		}
		this.p = 1.0/this.vol;
		this.log_p = -Math.log(this.vol);
	}
	
	@Override
	public double p(Matrix x) {
		for (int i = 0; i < this.DOF; i++) {
			
			if (x.get(i, 0) < this.llimits[i] || x.get(i, 0) > this.ulimits[i]) {
				return 0;
			}
		}
		
		return this.p;
	}
	
	@Override
	public double log_p(Matrix x) {
		for (int i = 0; i < this.DOF; i++) {
			
			if (x.get(i, 0) < this.llimits[i] || x.get(i, 0) > this.ulimits[i]) {
				return Double.NEGATIVE_INFINITY;
			}
		}
		
		return this.log_p;
	}

	@Override
	public Matrix getMean() {
		return this.mean;
	}

	@Override
	public Matrix drawSample() {
		Matrix s = new Matrix(this.DOF,1);
		
		for (int i = 0; i < this.DOF; i++) {
			if (this.rand.nextBoolean())
				s.set(i, 0, this.rand.nextDouble()*(this.ulimits[i] - this.llimits[i]) + this.llimits[i]);
			else
				s.set(i, 0, (1 - this.rand.nextDouble())*(this.ulimits[i] - this.llimits[i]) + this.llimits[i]);	
		}

		return s;
	}
	
	/** Get volume of the hypercube where p(x) > 0 */
	public double getVolume() {
		return this.vol;
	}

}
