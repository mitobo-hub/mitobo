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

import Jama.Matrix;
import de.unihalle.informatik.MiToBo.math.MathX;
import de.unihalle.informatik.MiToBo.math.distributions.interfaces.LogProbabilityDensityFunction;
import de.unihalle.informatik.MiToBo.math.distributions.interfaces.ProbabilityDensityFunction;

/**
 * A distribution represented by a mixture of distributions
 * @author Oliver Gress
 *
 */
public class MixtureDistribution implements ProbabilityDensityFunction, LogProbabilityDensityFunction {

	protected ProbabilityDensityFunction[] pdfs;
	
	public double[] weights;
	
	public MixtureDistribution(ProbabilityDensityFunction[] pdfs) {
		this.pdfs = pdfs;

		this.normalizeWeights();
	}
	
	/**
	 * Constructor
	 * @param pdfs individual distributions in the mixture
	 * @param weights weights of the individual distributions
	 * @throws IllegalArgumentException if pdf- and weight-array have different length
	 */
	public MixtureDistribution(ProbabilityDensityFunction[] pdfs, double[] weights) 
																throws IllegalArgumentException {
		
		if (pdfs.length != weights.length) {
			throw new IllegalArgumentException("AbstractMixtureDistribution(): Arrays of pdfs and weights must be of same size.");
		} 
		
		this.pdfs = pdfs;
		this.weights = weights;
		
		this.normalizeWeights();
	}
	
	@Override
	public double p(Matrix x) {
		double sum = 0.0;
		
		for (int i = 0; i < this.pdfs.length; i++) {
			sum += this.weights[i] * this.pdfs[i].p(x);
		}
		
		return sum;
	}
	
	@Override
	public double log_p(Matrix x) {
		double logsum = 0, logpi;
		
		for (int i = 0; i < this.pdfs.length; i++) {
			if (this.pdfs[i] instanceof LogProbabilityDensityFunction)
				logpi = Math.log(this.weights[i]) + ((LogProbabilityDensityFunction)this.pdfs[i]).log_p(x);
			else
				logpi = Math.log(this.weights[i]) + Math.log(this.pdfs[i].p(x));
			
			if (i == 0)
				logsum = logpi;
			else
				logsum = MathX.logSumP(logsum, logpi);
		}
		
		return logsum;
	}	

	/** Normalize weights */
	public void normalizeWeights() {
		
		double sum = 0.0;
		
		if (this.weights == null) {
			this.weights = new double[this.pdfs.length];
		}
		else {
			
			for (int i = 0; i < this.pdfs.length; i++) {
				sum += this.weights[i];
			}
		}
		
		if (sum == 0.0) {
			
			double w = 1.0/this.pdfs.length;
			
			for (int i = 0; i < this.pdfs.length; i++) {
				this.weights[i] = w;
			}
		}
		else if (sum != 1.0) {
			double w = 1.0/sum;
			
			for (int i = 0; i < this.pdfs.length; i++) {
				this.weights[i] *= w;
			}
		}
	}
	
	
	public int getNumOfComponents() {
		return this.pdfs.length;
	}
	
	public double getWeight(int idx) {
		return this.weights[idx];
	}
	
	public void setWeight(int idx, double weight) {
		this.weights[idx] = weight;
	}
	
	public double[] getWeights() {
		return this.weights;
	}
	
	public void setWeights(double[] weights) {		
		if (this.pdfs.length != weights.length) {
			throw new IllegalArgumentException("AbstractMixtureDistribution.setWeights(..): Size of weights-array must equal the number of components.");
		} 
		
		this.weights = weights;
		this.normalizeWeights();
	}
	
	public ProbabilityDensityFunction getPdf(int idx) {
		return this.pdfs[idx];
	}
	
	public void setPdf(int idx, ProbabilityDensityFunction pdf) {
		this.pdfs[idx] = pdf;
	}
	
}
