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
package de.unihalle.informatik.MiToBo.math.distributions.impl;

import java.util.Random;

import Jama.Matrix;
import de.unihalle.informatik.MiToBo.math.distributions.interfaces.FirstOrderMoment;
import de.unihalle.informatik.MiToBo.math.distributions.interfaces.ProbabilityDensityFunction;
import de.unihalle.informatik.MiToBo.math.distributions.interfaces.SamplingDistribution;
import de.unihalle.informatik.MiToBo.math.distributions.interfaces.SecondOrderCentralMoment;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.interfaces.Copyable;

/**
 * A multivariate Gaussian mixture distribution.
 * 
 * @author Oliver Gress
 *
 */
public class GaussMixDistribution extends MixtureDistribution
				implements SamplingDistribution<Matrix>, FirstOrderMoment<Matrix>, SecondOrderCentralMoment<Matrix>, Copyable<GaussMixDistribution> {

	protected Random rand;
	
	/**
	 * Constructor with equally weighted Gaussian components
	 * @param pdfs Gaussian distributions
	 * @param rand random generator for sampling
	 */
	public GaussMixDistribution(GaussianDistribution[] pdfs, Random rand) {
		super(pdfs);
		this.rand = rand;
	}
	
	/**
	 * Constructor for weighted Gaussian components
	 * @param pdfs Gaussian distributions
	 * @param weights weights of Gaussian distributions
	 * @param rand random generator for sampling
	 * @throws IllegalArgumentException if length of pdfs- and weights-array differs
	 */
	public GaussMixDistribution(GaussianDistribution[] pdfs, double[] weights, Random rand)
										throws IllegalArgumentException {
		super(pdfs, weights);
		this.rand = rand;
	}
	
	@Override
	public GaussMixDistribution copy() {
		
		GaussMixDistribution gm = new GaussMixDistribution((GaussianDistribution[]) this.pdfs.clone(), this.weights.clone(), this.rand);
		
		for (int i = 0; i < this.pdfs.length; i++)
			gm.pdfs[i] = ((GaussianDistribution)this.pdfs[i]).copy();
		
		return gm;
	}

	@Override
	public Matrix getCovariance() {
		Matrix m = this.getMean();
		
		Matrix dm = ((GaussianDistribution)this.pdfs[0]).getMean().minus(m);
		
		Matrix P = ((GaussianDistribution)this.pdfs[0]).getCovariance().copy();
		P.plusEquals(dm.times(dm.transpose()));
		P.timesEquals(this.weights[0]);
		
		for (int i = 1; i < this.pdfs.length; i++) {
			dm = ((GaussianDistribution)this.pdfs[i]).getMean().minus(m);
			
			P.plusEquals((((GaussianDistribution)this.pdfs[i]).getCovariance().plus(dm.times(dm.transpose()))).times(this.weights[i]));
		}
		
		return P;
	}

	@Override
	public Matrix getMean() {
		Matrix mean = ((GaussianDistribution)this.pdfs[0]).getMean().copy();
		mean.timesEquals(this.weights[0]);
		
		for (int i = 1; i < this.pdfs.length; i++) {
			mean.plusEquals(((GaussianDistribution)this.pdfs[i]).getMean().times(this.weights[i]));
		}
		
		return mean;
	}

	@Override
	public void setPdf(int idx, ProbabilityDensityFunction pdf) throws IllegalArgumentException {
		
		if (!(pdf instanceof GaussianDistribution))
			throw new IllegalArgumentException("GaussMixDistribution.setPdf(..): pdf must be a Gaussian distribution.");
		
		this.pdfs[idx] = pdf;
	}

	@Override
	public Matrix drawSample() {
		GenericDiscreteDistribution d = new GenericDiscreteDistribution(this.weights, this.rand);
		int idx = d.drawSample();
		
		return ((GaussianDistribution)this.pdfs[idx]).drawSample();
	}

}
