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

package de.unihalle.informatik.MiToBo.tracking.multitarget.algo;

import java.io.OutputStream;
import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.TreeSet;
import java.util.Vector;

import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo;
import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo.ExportPolicy;
import de.unihalle.informatik.MiToBo.math.MathX;
import de.unihalle.informatik.MiToBo.math.distributions.impl.ParticleDistribution;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.abstracts.AbstractMultiState;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.impl.DataAssociationExclusive;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.impl.RBMCDASample;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.impl.TargetID;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.interfaces.DataAssociation;
import de.unihalle.informatik.MiToBo.tracking.multitarget.distributions.impl.AssociationDistribution;

/**
 * Rao-Blackwellized Monte Carlo Data Association following:<br>
 * S. Saerkkae, A. Vehtari and J. Lampinen, "Rao-Blackwellized particle filter for multiple target tracking",
 * Information Fusion, Vol 8, No 1, 2007, pages 2-15
 * <br><br>
 * Monte Carlo sampling in the space of association variables while state distributions are kept in closed form.
 * Association variables determine association of observations to targets.
 * 
 * @author Oliver Gress
 *
 * @param <T>
 */
@ALDMetaInfo(export=ExportPolicy.ALLOWED)
public class MultiTargetRBMCDA<T extends TargetID> extends ParticleDistribution<RBMCDASample<T>> 
	implements MultiTargetPredictionFilter<AbstractMultiState<T>> {

	protected AssociationDistribution<T, T> assocDistrib;
	
	protected double esspercentage = 0.5;
	
	public OutputStream ostream = null;
	
	protected Vector<HashMap<Integer,Integer>> groundtruthToSampleTargetIDs;
	
	@SuppressWarnings("unchecked")
	public MultiTargetRBMCDA(Random rand, RBMCDASample<T> initialState, int numParticles,
			AssociationDistribution<T, T> assocDistrib) {
		super();
		
		this.assocDistrib = assocDistrib;

		this.particles = (RBMCDASample<T>[]) Array.newInstance(initialState.getClass(), numParticles);
		
		this.particles[0] = initialState;
		for (int i = 1; i < numParticles; i++) {
			this.particles[i] = initialState.copy();
		}
		
		this.weights = new double[this.particles.length];
		this.cweights = new double[this.particles.length];
		
		this.rand = rand;
		
		this.normalizeWeights();
		
		this.groundtruthToSampleTargetIDs = new Vector<HashMap<Integer,Integer>>(numParticles);
		for (int i = 0; i < numParticles; i++)
			groundtruthToSampleTargetIDs.add(new HashMap<Integer,Integer>());
	}
	
	public MultiTargetRBMCDA(Random rand, RBMCDASample<T>[] initialStateParticles,
			AssociationDistribution<T, T> assocDistrib) {
		super();
		
		this.assocDistrib = assocDistrib;

		this.particles = initialStateParticles;
		this.weights = new double[this.particles.length];
		this.cweights = new double[this.particles.length];
		
		this.rand = rand;
		
		this.normalizeWeights();
		
		this.groundtruthToSampleTargetIDs = new Vector<HashMap<Integer,Integer>>(initialStateParticles.length);
		for (int i = 0; i < initialStateParticles.length; i++)
			groundtruthToSampleTargetIDs.add(new HashMap<Integer,Integer>());
	}
	
	@Override
	public AbstractMultiState<T> getMean() {
		AbstractMultiState<T> mean = this.particles[0].filter.getMean().copy();	
		AbstractMultiState<T> X;
		
		for (int n = 0; n < mean.getNumberOfStates(); n++)
			mean.getStateContinuous(n).timesEquals(this.weights[0]);
		
		for (int i = 0; i < this.particles.length; i++) {
			X = this.particles[i].filter.getMean();
			
			for (int n = 0; n < mean.getNumberOfStates(); n++)
				mean.getStateContinuous(n).plusEquals(X.getStateContinuous(n).times(this.weights[i]));
		}
		
		return mean;
	}

	/** Not implemented, always returns <code>null</code> */
	@Override
	public MultiTargetPredictionFilter<AbstractMultiState<T>> copy() {
		return null;
	}
	
	@Override
	public void predict() {
		for (int i = 0; i < this.particles.length; i++)
			this.particles[i].filter.predict();
		
	}

	
	/**
	 * The DataAssociation object may be null and is interpreted as groundtruth if given.
	 */
	@Override
	public void update(AbstractMultiState<T> observation,
			DataAssociation association) {

		
		HashMap<Integer,Integer> gtToSample = null;
		
		// sample associations for each particle and update target state distributions accordingly
		for (int i = 0; i < this.particles.length; i++) {
			
		//	MultiObservationTrackerRBMCDAIMM.current_i = i;
			
			// set the lowest targetID for newborn targets in the current particle in the association sampling distribution 
			this.assocDistrib.letNewbornTargetIDsStartFrom(this.particles[i].maxTargetID+1);
			
			// give the predicted observation distribution of the current particle to the association sampling distribution
			this.assocDistrib.setNewObservations(observation, this.particles[i].filter.getObservationDistribution());
			
			DataAssociation da = null;
			
			if (association != null) {
				
				gtToSample = this.groundtruthToSampleTargetIDs.get(i);
				
				DataAssociation daGT = null; 
					
				if (!gtToSample.isEmpty()) {
					daGT = new DataAssociationExclusive();
					
					for (int m = 0; m < observation.getNumberOfStates(); m++) {
						if (association.getAssociatedTargets(m+1) != null) {
							if (gtToSample.get(association.getAssociatedTargets(m+1)[0]) != null) {
								try {
									daGT.setAssociation(gtToSample.get(association.getAssociatedTargets(m+1)[0]), m+1);
								} catch (Exception e) {
									System.err.println(m+1 + "#X#" + association.getAssociatedTargets(m+1)[0] + "#Y#" +
											daGT + "#1#" + gtToSample + "#2#"+ association + "#3#" + e.getMessage());
									e.printStackTrace();
									System.exit(1);
								}
							}
						}
					}
				}
				else {
					for (int m = 0; m < observation.getNumberOfStates(); m++) {
						if (association.getAssociatedTargets(m+1) != null)
							gtToSample.put(association.getAssociatedTargets(m+1)[0], association.getAssociatedTargets(m+1)[0]);
					}
					
					daGT = association;
				}
				
				// sample the observation-to-target associations for this particle
				da = this.assocDistrib.drawSampleDebug(daGT, this.ostream);
				
				for (int m = 0; m < observation.getNumberOfStates(); m++) {
					if (association.getAssociatedTargets(m+1) != null && da.getAssociatedTargets(m+1) != null) {
						if (gtToSample.containsValue(da.getAssociatedTargets(m+1)[0])) {
							Iterator<Integer> iter = gtToSample.keySet().iterator();
							
							while (iter.hasNext()) {
								int key = iter.next();
								
								if (gtToSample.get(key) != null && gtToSample.get(key) == da.getAssociatedTargets(m+1)[0]) {
									gtToSample.put(key, null);
									break;
								}
							}
							
						}
						gtToSample.put(association.getAssociatedTargets(m+1)[0], da.getAssociatedTargets(m+1)[0]);
					}
				}
			}
			else {
				da = this.assocDistrib.drawSample();
			}

			// update the maximum targetID that ever occurred in this particle
			int currentMaxTargetID = da.maxAssociatedTargetID();
			if (currentMaxTargetID > this.particles[i].maxTargetID)
				this.particles[i].maxTargetID = currentMaxTargetID;
			
			// update state distribution of targets in the current particle according to sampled associations with current observations
			this.particles[i].filter.update(observation, da);
			
			// retrieve the targetIDs of targets that exist in the current particle (after update, i.e. targets may have born or died)
			TreeSet<Short> existingTargetIDs = new TreeSet<Short>();
			AbstractMultiState<T> X_ = this.particles[i].filter.getMean();
			for (int n = 0; n < X_.getNumberOfStates(); n++) {
				existingTargetIDs.add(X_.getStateDiscrete(n).ID);
			}
				
			// store information on the current tracking step in the current particle's information object
			this.particles[i].getSampleInfo().addCurrentInfo(this.assocDistrib.log_p(da), da, 
					observation, existingTargetIDs);
			
			// set weight of the current particle as the logarithmic probability of data associations
			this.weights[i] = this.particles[i].getSampleInfo().getCLogJointProb();//this.assocDistrib.log_p(da);
			//System.err.println(i+ " " + this.weights[i]);
		}	
		
		// normalize logarithmic weights and transform to linear scale
		double logsum = 0;
		for (int i = 0; i < this.particles.length; i++) {
	//		System.err.print(this.weights[i] + " ");
			logsum = MathX.logSumP(this.weights[i], logsum);
		}
	//	System.err.println("\n"+logsum + " " + Math.exp(logsum));
		for (int i = 0; i < this.particles.length; i++) {
			this.weights[i] = Math.exp(this.weights[i] - logsum);
		}
		
		
		// resample if ESS is low
	//	System.err.println("ESS=" + this.computeESS());
		if (this.computeESS() < this.particles.length*this.esspercentage) {
	//		System.err.println("Resampling... ESS=" + this.computeESS());
			this.resample();
		}
	}
	
	/**
	 * If this percentage of number of samples is below the current effective sample size (ESS), the samples are resampled. Default is 0.5.
	 * @return percentage of the number of samples
	 */
	public double getESSPercentage() {
		return this.esspercentage;
	}
	
	/**
	 * Set percentage of number of samples used to determine if resampling is required, i.e. if it is below the current effective sample size (ESS). Default is 0.5.
	 * @param essPercentage percentage in range [0,1] of the number of samples. 0=no resampling ... 1=resamping in every time step.
	 */
	public void setESSPercentage(double essPercentage) {
		this.esspercentage = essPercentage;
	}

}
