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

import java.util.Arrays;
import java.util.Random;

import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo;
import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo.ExportPolicy;
import de.unihalle.informatik.MiToBo.math.MathX;
import de.unihalle.informatik.MiToBo.math.distributions.interfaces.LogProbabilityMassFunction;
import de.unihalle.informatik.MiToBo.math.distributions.interfaces.ProbabilityMassFunction;
import de.unihalle.informatik.MiToBo.math.distributions.interfaces.SamplingDistribution;


/**
 * A generic discrete distribution
 * @author Oliver Gress
 *
 */
@ALDMetaInfo(export=ExportPolicy.ALLOWED)
public class GenericDiscreteDistribution implements SamplingDistribution<Integer>,
		ProbabilityMassFunction, LogProbabilityMassFunction {

	
	protected Random rand;
	
	/** Tells if the distribution is internally represented by the natural logarithm of the probability values */
	private boolean LOG;
	
	/** probability mass function */
	protected double[] pmf;
	
	/** cumulative distribution function, used for sampling */
	protected double[] cdf;
	
	/**
	 * Constructor. The weights represent the discrete distribution of values 0 to <code>weigths.length-1</code>. 
	 * Weights must not sum to 0 and must not be negative. If weights do not sum to 1, they get normalized.
	 * @param weights proportional to the probabilities of events 0 to <code>weigths.length-1</code>.
	 * @param rand a random generator used for sampling
	 */
	public GenericDiscreteDistribution(double[] weights, Random rand) {
		
		this.LOG = false;
		
		this.rand = rand;
		
		this.pmf = Arrays.copyOf(weights, weights.length);
		for (int i = 0; i < this.pmf.length; i++) {
			if (this.pmf[i] < 0)
				throw new IllegalArgumentException("GenericDiscreteDistribution: Weights must not be negative: Weight["+i+"]="+this.pmf[i]);
		}
		
		this.cdf = new double[this.pmf.length];
		
		this.cdf[0] = this.pmf[0];
		for (int i = 1; i < this.cdf.length; i++) {
			this.cdf[i] = this.cdf[i-1] + this.pmf[i];
		}
		
		if (this.cdf[this.cdf.length-1] == 0.0) {
			
			throw new IllegalArgumentException("GenericDiscreteDistribution: Input weights must not sum to zero!!");
		}

		// normalize
		for (int i = 0; i < this.pmf.length; i++) 
			this.pmf[i] /= this.cdf[this.cdf.length-1];

	}
	
	/**
	 * Constructor. The weights might represent natural logarithm of the probabilities of values 0 to <code>weigths.length-1</code>. 
	 * If so, (log-)weights must not sum to <code>Double.NEGATIVE_INFINITY</code>. If (log-)weights do not sum to 0, they get normalized.
	 * If weights are not logarithms of probabilities, see {@link GenericDiscreteDistribution(double[] weights, Random rand)}.
	 * @param weights (the natural logarithm of values proportional to) the probabilities of events 0 to <code>weigths.length-1</code>.
	 * @param rand a random generator used for sampling
	 * @param weightsAreLog determines if weights are interpreted as log probabilities
	 */
	public GenericDiscreteDistribution(double[] weights, Random rand, boolean weightsAreLog) {
		
		this.LOG = weightsAreLog;
		
		this.rand = rand;
		
		// store [log(] p(k) [)]
		this.pmf = Arrays.copyOf(weights, weights.length);
		for (int i = 0; i < this.pmf.length; i++) {
			if (!this.LOG && this.pmf[i] < 0)
				throw new IllegalArgumentException("GenericDiscreteDistribution: Weights must not be negative: Weight["+i+"]="+this.pmf[i]);
		}
		
		// cumulative [log] distribution:   [log(] p(0) + p(1) + ... + p(k) [)]
		this.cdf = new double[this.pmf.length];
		
		this.cdf[0] = this.pmf[0];
		
		if (this.LOG) {
			for (int i = 1; i < this.cdf.length; i++) {
				this.cdf[i] = MathX.logSumP(this.cdf[i-1], this.pmf[i]);
			}
			
			if (Double.isInfinite(this.cdf[this.cdf.length-1])) {
				throw new IllegalArgumentException("GenericDiscreteDistribution: Logarithmic input weights must not sum to Negative Infinity!!");
			}
	
			// normalize log-pmf
			for (int i = 0; i < this.pmf.length; i++) {
				this.pmf[i] -= this.cdf[this.cdf.length-1];
			}
		}
		else {
			for (int i = 1; i < this.cdf.length; i++) {
				this.cdf[i] = this.cdf[i-1] + this.pmf[i];
			}
			
			if (this.cdf[this.cdf.length-1] == 0.0) {
				
				throw new IllegalArgumentException("GenericDiscreteDistribution: Input weights must not sum to zero!!");
			}

			// normalize
			for (int i = 0; i < this.pmf.length; i++) 
				this.pmf[i] /= this.cdf[this.cdf.length-1];
		}
		
		
	}
	
	
	
	@Override
	public double p(Integer k) {
		if (k >= 0 && k < this.pmf.length) {
			if (this.LOG)
				return Math.exp(this.pmf[k]);
			else
				return this.pmf[k];
		}
		else
			return 0;
	}
	
	@Override
	public double log_p(Integer k) {

		if (k >= 0 && k < this.pmf.length) {
			if (this.LOG) 
				return this.pmf[k];
			else		               	
				return Math.log(this.pmf[k]);
		}
		else
			return Double.NEGATIVE_INFINITY;
	
	}	

	@Override
	public Integer drawSample() {
		double u;
		if (this.LOG) {
			u = Math.log(this.rand.nextDouble()) + this.cdf[this.cdf.length-1];
		}
		else {
			u = this.rand.nextDouble() * this.cdf[this.cdf.length-1];
		}
		
		int i = 0;
		
		while (u >= this.cdf[i] && i < this.cdf.length) {
			i++;
		}
		
		return i;
	}
	
	public String toString() {
		String s = "";
		
		for (int i = 0; i < this.pmf.length; i++) {
			if (i == 0)
				s += "p("+i+")="+this.p(i);
			else
				s += " p("+i+")="+this.p(i);
		}
		
		return s;
	}

}
