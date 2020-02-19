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
import java.util.Vector;

import de.unihalle.informatik.MiToBo.math.distributions.interfaces.SamplingDistribution;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.interfaces.Copyable;

/**
 * A distribution represented by weighted particles
 * @author Oliver Gress
 *
 * @param <G> type of a particle
 */
public class ParticleDistribution<G extends Copyable<? extends G>> implements SamplingDistribution<G> {

	protected G[] particles;
	protected double[] weights;
	protected double[] cweights;
	protected Random rand;
	protected boolean equalWeights;
	
	/**
	 * Constructor where all fields are initialized by <code>null</code>.
	 */
	protected ParticleDistribution() {
		this.particles = null;
		this.weights = null;
		this.cweights = null;
		
		this.rand = null;
		this.equalWeights = true;
	}
	
	/**
	 * Constructor with equally weighted particles that must be specified.	 
	 * @param rand random generator for sampling
	 * @param particles equally weighted particles
	 */
	public ParticleDistribution(Random rand, G[] particles) {
		this.particles = particles;
		this.weights = new double[particles.length];
		this.cweights = new double[particles.length];
		
		this.rand = rand;
		
		this.normalizeWeights();
		this.equalWeights = true;
	}
	
	/**
	 * Constructor with equally weighted particles that must be specified.	 
	 * @param rand random generator for sampling
	 * @param particles particles
	 * @param weights weights of the particles
	 * @throws IllegalArgumentException when particles- and weights-array do not have same length
	 */
	public ParticleDistribution(Random rand, G[] particles, double[] weights) throws IllegalArgumentException {
		
		if (particles.length != weights.length) {
			throw new IllegalArgumentException("Arrays of particles and weights must have equal size.");
		}
		
		this.particles = particles;
		this.weights = weights;
		this.cweights = new double[particles.length];
		
		this.rand = rand;
		
		this.normalizeWeights();
		this.equalWeights = false;
	}
	
	/**
	 * Constructor that samples <code>numParticles</code> from <code>density</code>.
	 * @param rand random generator for sampling
	 * @param density distribution to sample from
	 * @param numParticles number of particles to be sampled
	 */
	public ParticleDistribution(Random rand, SamplingDistribution<G> density, int numParticles) {
		Vector<G> particleVec = new Vector<G>(numParticles);
		
		for (int i = 0; i < numParticles; i++) {
			particleVec.add(density.drawSample());
		}
		
		this.particles = particleVec.toArray(this.particles);
		this.weights = new double[particles.length];
		this.cweights = new double[particles.length];
		
		this.rand = rand;
		
		this.normalizeWeights();
		this.equalWeights = true;
	}
	
	
	@Override
	public G drawSample() {
		double r = this.rand.nextDouble();
		
		int j;
		for (j = 0; j < this.particles.length; j++) {
			if (this.cweights[j] >= r) {
				break;
			}
		}
		
		return this.particles[j].copy();
	}
	
	/** Get number of particles */
	public int getNumOfParticles() {
		return this.particles.length;
	}
	
	/** Get particles */
	public G[] getParticles() {
		return this.particles;
	}
	
	/** Get idx-th particle */
	public G getParticle(int idx) {
		return this.particles[idx];
	}
	
	/** Set idx-th particle */
	public void setParticle(int idx, G particle) {
		this.particles[idx] = particle;
	}
	
	/** Get particle weights */
	public double[]	getWeights() {
		return this.weights;
	}
	
	/** Get weight of idx-th particle */
	public double getWeight(int idx) {
		return this.weights[idx];
	}
	
	/**
	 * Set weight for particle specified by idx. Don't forget to normalize after setting weights, because
	 * cumulative weight are not updated in this method.
	 * @param idx index of particle
	 * @param weight weight of particle
	 */
	public void setWeight(int idx, double weight) {
		this.weights[idx] = weight;
		this.equalWeights = false;
	}
	
	/**
	 * Normalize the particle weights to sum to 1. 
	 * If all weights are 0, equal weights are given to all particles!
	 */
	public void normalizeWeights() {
		double sum = this.getWeightsSum();	
		
		if (sum == 0.0) {
			// weights are not initialized, give equal weights
			
		//	System.out.println("Sum == 0");
			
			double w = 1.0/this.particles.length;
			
			this.weights[0] = w;
			this.cweights[0] = w;
			
			for (int i = 1; i < this.particles.length; i++) {
				this.weights[i] = w;
				this.cweights[i] = this.cweights[i-1] + w;
			}
			
			this.equalWeights = true;
		}
		else if (sum != 1.0) {
			// normalize weights, if unnormalized
			
	//		double w = 1.0/sum;
			this.weights[0] /= sum;//*= w;
			this.cweights[0] = this.weights[0];

	//		System.out.println(sum + " " + this.weights[0]);
			
			for (int i = 1; i < this.particles.length; i++) {
				this.weights[i] /= sum; //*= w;
				this.cweights[i] = this.cweights[i-1] + this.weights[i];
			}
		}	
	}
	
	/** Resample this distribution and equalize weights */
	public void resample() {
		Vector<G> particleVec = new Vector<G>(this.particles.length);
		
		this.normalizeWeights();
		
		
		// Fast systematic resampling O(N) [J. Carpenter et al., "An improved particle filter for non-linear problems", 1999]
//		double[] cT = new double[this.cweights.length+1];
//		
//		cT[0] = -Math.log(1.0-rand.nextDouble());
//		for (int j = 1; j < this.cweights.length+1; j++) {
//			cT[j] = cT[j-1] - Math.log(1.0-rand.nextDouble());
//		}
		
//		int i = 0; int j = 0; double Q;
//		while (/*i < this.cweights.length &&*/ j <= this.cweights.length) {
//			Q = (j == 0 ? 0.0 : this.cweights[j-1]);
//			
//			if (Q * cT[cT.length-1] > cT[i]) {
//				i++;
//				particleVec.add(particles[j-1].copy());
//			}
//			else {
//				j++;
//			}
//		}
		
//		int i = 0; int j = 1;
//		while (i < this.cweights.length) {
//			if (this.cweights[j]*cT[cT.length-1] > cT[i]) {
//				i++;
//				particleVec.add(particles[j].copy());
//			}
//			else {
//				j++;
//			}
//		}
		
		for (int i = 0; i < this.particles.length; i++) {
			particleVec.add(this.drawSample());
		}
		
//		if (this.particles.length != particleVec.size())
//			System.err.println("Resampling generated a different number of samples: " + particleVec.size());
		
		this.particles = particleVec.toArray(this.particles);
		
		double w = 1.0/this.particles.length;
		
		this.weights[0] = w;
		this.cweights[0] = w;
		
		for (int k = 1; k < this.particles.length; k++) {
			this.weights[k] = w;
			this.cweights[k] = this.cweights[k-1] + w;
		}
		
		this.equalWeights = true;
	}
	
	/** Get sum of weights */
	double getWeightsSum() {
		double sum = 0.0;
		for (int i = 0; i < this.particles.length; i++) {
			sum += this.weights[i];
		}
		return sum;
	}
	
	/** Returns true if all particle weights are equal */
	public boolean equalWeights() {
		return this.equalWeights;
	}
	
	/** Set the "equal weights"-flag */
	public void setEqualWeightsFlag(boolean equalWeights) {
		this.equalWeights = equalWeights;
	}
	
	/**
	 * Compute effective sample size (ESS). If many particles have weights approximately 0
	 * the distribution resampling might be required e.g. for good exploration of the random variable's space
	 * in case of a particle filter. High variance in the weight is a good indicator for this situation. The 
	 * ESS returns values from 1 to N (number of particles), where 1 corresponds to a single particle with weight 1 and N to
	 * weights 1/N for each particle. Resampling is suggested if ESS drops below a certain threshold. 
	 */
	public double computeESS() {
		this.normalizeWeights();
		
		double essInv = 0.0;
		
		for (int i = 0; i < this.weights.length; i++)
			essInv += this.weights[i]*this.weights[i];
		
		return 1.0/essInv;
	}
	
}
