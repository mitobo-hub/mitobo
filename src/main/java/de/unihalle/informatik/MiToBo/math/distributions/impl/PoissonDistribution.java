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

import de.unihalle.informatik.MiToBo.math.distributions.interfaces.ConditionalDistribution;
import de.unihalle.informatik.MiToBo.math.distributions.interfaces.FirstOrderMoment;
import de.unihalle.informatik.MiToBo.math.distributions.interfaces.LogProbabilityMassFunction;
import de.unihalle.informatik.MiToBo.math.distributions.interfaces.ProbabilityMassFunction;
import de.unihalle.informatik.MiToBo.math.distributions.interfaces.SamplingDistribution;
import de.unihalle.informatik.MiToBo.math.distributions.interfaces.SecondOrderCentralMoment;

/**
 * Poisson distribution.
 * @author Oliver Gress
 *
 */
public class PoissonDistribution implements ProbabilityMassFunction, LogProbabilityMassFunction, ConditionalDistribution<Double>,
		FirstOrderMoment<Double>, SecondOrderCentralMoment<Double>, SamplingDistribution<Integer> {

	/** mean=variance of the distribution*/
	protected double lambda;

	protected Random rand;
	
	/**
	 * Constructor for Poisson distribution with parameter lambda (=mean=variance)
	 * @param lambda mean/variance of the distribution
	 * @param rand random generator for sampling
	 */
	public PoissonDistribution(double lambda, Random rand) {
		if (lambda <= 0)
			throw new IllegalArgumentException("PoissonDistribution.PoissonDistribution(.): lambda must be larger 0.");
		
		this.lambda = lambda;
		this.rand = rand;
	}

	@Override
	public double p(Integer k) {
		return Math.exp(this.log_p(k));
	}

	@Override
	public double log_p(Integer k) {
		if (k < 0)
			return Double.NEGATIVE_INFINITY;
		else {
			double sumlogk = 0;
			for (int i = 1; i <= k; i++)
				sumlogk += Math.log(i);
			
			return k*Math.log(this.lambda) - sumlogk - this.lambda;
		}
	}
	
	@Override
	public Double getCovariance() {
		return this.lambda;
	}
	
	@Override
	public Double getMean() {
		return this.lambda;
	}

	@Override
	public Double getCondition() {
		return this.lambda;
	}

	@Override
	public void setCondition(Double lambda) {
		if (lambda <= 0)
			throw new IllegalArgumentException("PoissonDistribution.setCondition(.): lambda must be larger 0.");
		
		this.lambda = lambda;
	}

	@Override
	public Integer drawSample() {
		// wikipedia "poisson distribution" 
		// algorithm by: Donald E. Knuth (1969). Seminumerical Algorithms. The Art of Computer Programming, Volume 2
		// TODO: this algo is linear in lambda and might run into numerical stability problems for large lambda!! 
		if (this.rand == null) 
			this.rand = new Random();
		
		
		double L = Math.exp(-this.lambda);
		int k = 0;
		double p = 1;
		
		do {
			k++;
			p *= this.rand.nextDouble();
		} while (p > L);
		
		return k-1;
	}

}
