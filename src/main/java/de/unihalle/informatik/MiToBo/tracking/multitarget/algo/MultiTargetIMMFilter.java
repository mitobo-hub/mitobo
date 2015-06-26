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

import java.util.HashMap;
import java.util.Random;
import java.util.Set;
import java.util.Vector;

import Jama.Matrix;
import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo;
import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo.ExportPolicy;
import de.unihalle.informatik.MiToBo.math.LinearTransformGaussNoise;
import de.unihalle.informatik.MiToBo.math.distributions.impl.ExponentialDistribution;
import de.unihalle.informatik.MiToBo.math.distributions.impl.GaussMixDistribution;
import de.unihalle.informatik.MiToBo.math.distributions.impl.GaussianDistribution;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.abstracts.AbstractMultiState;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.abstracts.AbstractMultiStateFactory;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.impl.DataAssociationExclusive;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.impl.MotionModelID;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.interfaces.DataAssociation;
import de.unihalle.informatik.MiToBo.tracking.multitarget.distributions.abstracts.AbstractMultiObservationDistributionIndep;
import de.unihalle.informatik.MiToBo.tracking.multitarget.distributions.impl.MultiObsDistributionIndepGaussMix;


/**
 * Multi-target Interacting Multiple Models (IMM) filter for varying number of targets.
 * 
 * @author Oliver Gress
 *
 */
@ALDMetaInfo(export=ExportPolicy.ALLOWED)
public class MultiTargetIMMFilter implements
		MultiTargetPredictionFilterIndep<MotionModelID> {
	
	/** state distribution: independent Gaussian mixtures for each target */
	protected Vector<GaussMixDistribution> pstate;  
	
	/** observation distribution: independent Gaussian mixtures for each target */
	protected Vector<GaussMixDistribution> pobs;
	
	protected AbstractMultiState<MotionModelID> meanX;
	
	
	private MultiObsDistributionIndepGaussMix<MotionModelID> p_obs;
	
	/** dynamic models */
	protected LinearTransformGaussNoise[] predictors;
	
	/** observation model */
	protected LinearTransformGaussNoise projector;
	
	//protected DataAssociationMethod<MotionModelID,MotionModelID> associator;
	
	protected Random rand;
	
	/** Markov matrix probability of dynamic model switching:
	 *  A matrix with size (#dynamicmodels x #dynamicmodels).
	 *  The matrix element (i,j) specifies the probability
	 *  P(M^t = j | M^{t-1} = i) of switching the dynamic
	 *  model from i in time step t-1 to j in time step t
	 */
	protected Matrix markov; 

	protected AbstractMultiStateFactory<MotionModelID> factoryX, factoryZ;
	
	protected double delta_t;
	
	protected ExponentialDistribution pdeath;
	
	protected HashMap<Short,Integer> targetIDtoIdx; 
	
	protected GaussMixDistribution newbornStateDistrib;
	protected Matrix newbornStateFromObs;
	
	
	
	/**
	 * Copy constructor.
	 */
	public MultiTargetIMMFilter(MultiTargetIMMFilter imm) {
		this.factoryX = imm.factoryX;
		this.factoryZ = imm.factoryZ;
		
		this.pstate = new Vector<GaussMixDistribution>(imm.pstate.size());
		for (int n = 0; n < imm.pstate.size(); n++) {
			this.pstate.add(imm.pstate.get(n).copy());
		}
		
		this.projector = imm.projector;
		this.predictors = imm.predictors;
		
		this.pobs = new Vector<GaussMixDistribution>(imm.pobs.size());
		for (int n = 0; n < imm.pobs.size(); n++) {
			this.pobs.add(imm.pobs.get(n).copy());
		}
		
		this.p_obs = new MultiObsDistributionIndepGaussMix<MotionModelID>(this.rand, this.projector.getTransformMatrix(),
				this.pobs, imm.p_obs.getCondition().copy(), factoryX, factoryZ);
		
		this.rand = imm.rand;
		this.markov = imm.markov.copy();
		this.meanX = imm.meanX.copy();
		
		if (imm.pdeath != null)
			this.pdeath = new ExponentialDistribution(imm.pdeath.getLambda());
		else 
			this.pdeath = null;
		
		this.delta_t = imm.delta_t;
		
		this.targetIDtoIdx = new HashMap<Short,Integer>(imm.targetIDtoIdx.size());
		Set<Short> keys = imm.targetIDtoIdx.keySet();
		
		for (Short key : keys) {
			this.targetIDtoIdx.put(key, imm.targetIDtoIdx.get(key));
		}
		
		this.newbornStateDistrib = imm.newbornStateDistrib.copy();
		this.newbornStateFromObs = imm.newbornStateFromObs.copy();
	}
	
	/**
	 * Constructor that initializes the internal random generator with seed 1.
	 * @param initialStateDistrib distributions of the different independent targets' states
	 * @param observationModel observation model
	 * @param dynamicsModels multiple dynamic models
	 * @param markov model transition matrix: each column holds the probabilities to switch from the dynamic model of column-index to the corresponding model of row-index
	 * @param delta_t time step
	 * @param targetDeathDistrib distribution of the death/survival of targets depending on last time of association
	 * @param newbornStateDistrib distribution of newborn targets
	 * @param stateFromObs a matrix to transform an observation (vector) to a state vector
	 * @param factoryX multi-state factory
	 * @param factoryZ multi-observation factory
	 */
	public MultiTargetIMMFilter(Vector<GaussMixDistribution> initialStateDistrib,
			LinearTransformGaussNoise observationModel,
			LinearTransformGaussNoise[] dynamicsModels,
			Matrix markov,
			double delta_t,
			ExponentialDistribution targetDeathDistrib,
			GaussMixDistribution newbornStateDistrib,
			Matrix stateFromObs,
			AbstractMultiStateFactory<MotionModelID> factoryX,
			AbstractMultiStateFactory<MotionModelID> factoryZ) {
		
		this.factoryX = factoryX;
		this.factoryZ = factoryZ;
		
		this.delta_t = delta_t;
		this.pdeath = targetDeathDistrib;
		
		this.markov = markov;
		
		this.targetIDtoIdx = new HashMap<Short,Integer>(initialStateDistrib.size());
		
		this.newbornStateDistrib = newbornStateDistrib;
		
		this.newbornStateFromObs = stateFromObs;
		
		this.rand = new Random(1);
		
		// test markov matrix for validity
		if (markov.getColumnDimension() != dynamicsModels.length || markov.getRowDimension() != dynamicsModels.length) {
			throw new IllegalArgumentException("MultiTargetIMMFilter.MultiTargetIMMFilter(.): markov must be a matrix of transition probabilities of size " +
					dynamicsModels.length + "x" + dynamicsModels.length + " (length of dynamic models array).");
		}

		// normalize markov matrix if necessary
		for (int j = 0; j < markov.getColumnDimension(); j++) {
			double sum = 0.0;
			
			for (int i = 0; i < markov.getRowDimension(); i++)
				sum += markov.get(i,j);
			
			if (sum == 0) {
				System.err.println("WARNING MultiTargetIMMFilter.MultiTargetIMMFilter(.): Probabilities in column of markov matrix are zero. " +
					"Assuming uniform distribution.");
				for (int i = 0; i < markov.getRowDimension(); i++)
					this.markov.set(i,j,1.0/markov.getRowDimension());
			}
			else if (sum != 1.0) {
				System.err.println("WARNING MultiTargetIMMFilter.MultiTargetIMMFilter(.): Probabilities in column of markov matrix must sum to 1 (sum=" + sum + "). " +
						"Values get normalized.");
				for (int i = 0; i < markov.getRowDimension(); i++)
					this.markov.set(i,j,this.markov.get(i,j)/sum);
			}	
		}
		
		this.pstate = initialStateDistrib;
			
		// test Gaussian mixtures of state distribution for validity
		// (Gaussian mixtures must have equal number of components as the number of dynamic models)
		for (int n = 0; n < this.pstate.size(); n++) {
			if (this.pstate.get(n).getNumOfComponents() != dynamicsModels.length) {
				throw new IllegalArgumentException("MultiTargetIMMFilter.MultiTargetIMMFilter(.): Initial state distribution must only contain Gaussian mixtures with number of " 
						+ "components equal to the number of dynamic models.");
			}
		}
		
		
		this.projector = observationModel;
		this.predictors = dynamicsModels;
		
		this.pobs = new Vector<GaussMixDistribution>(this.pstate.size());
		
		for (int n = 0; n < initialStateDistrib.size(); n++) {
			
			GaussianDistribution[] gm = new GaussianDistribution[this.pstate.get(n).getNumOfComponents()];
			
			for (int i = 0; i < gm.length; i++) {
				
				gm[i] = this.projector.transform((GaussianDistribution)this.pstate.get(n).getPdf(i));
			}	
			
			this.pobs.add(new GaussMixDistribution(gm, this.pstate.get(n).getWeights().clone(), this.rand));
		}

		
		AbstractMultiState<MotionModelID> zeroX = factoryX.createEmptyMultiState();
		this.meanX = factoryX.createEmptyMultiState();
		
		for (int n = 0; n < initialStateDistrib.size(); n++) {
			zeroX.insertState(new Matrix(factoryX.getContinuousDOF(),1), new MotionModelID((short)(n+1),(byte)-1));
			this.meanX.insertState(initialStateDistrib.get(n).getMean(), new MotionModelID((short)(n+1),(byte)-1));
			
			this.targetIDtoIdx.put((short)(n+1), n);
		}
		
		this.p_obs = new MultiObsDistributionIndepGaussMix<MotionModelID>(this.rand, this.projector.getTransformMatrix(),
				this.pobs, zeroX, factoryX, factoryZ);
		
	}

	/**
	 * Constructor.
	 * @param initialStateDistrib distributions of the different independent targets' states
	 * @param observationModel observation model
	 * @param dynamicsModels multiple dynamic models
	 * @param markov model transition matrix: each column holds the probabilities to switch from the dynamic model of column-index to the corresponding model of row-index
	 * @param delta_t time step
	 * @param targetDeathDistrib distribution of the death/survival of targets depending on last time of association
	 * @param newbornStateDistrib distribution of newborn targets
	 * @param stateFromObs a matrix to transform an observation (vector) to a state vector
	 * @param factoryX multi-state factory
	 * @param factoryZ multi-observation factory
	 * @param rand random generator
	 */
	public MultiTargetIMMFilter(Vector<GaussMixDistribution> initialStateDistrib,
			LinearTransformGaussNoise observationModel,
			LinearTransformGaussNoise[] dynamicsModels,
			Matrix markov,
			double delta_t,
			ExponentialDistribution targetDeathDistrib,
			GaussMixDistribution newbornStateDistrib,
			Matrix stateFromObs,
			AbstractMultiStateFactory<MotionModelID> factoryX,
			AbstractMultiStateFactory<MotionModelID> factoryZ,
			Random rand) {
		this(initialStateDistrib, observationModel, dynamicsModels, markov, delta_t, targetDeathDistrib, newbornStateDistrib,
				stateFromObs, factoryX, factoryZ);
		
		this.factoryX = factoryX;
		this.factoryZ = factoryZ;
		
		this.delta_t = delta_t;
		this.pdeath = targetDeathDistrib;
		
		this.markov = markov;
		
		this.targetIDtoIdx = new HashMap<Short,Integer>(initialStateDistrib.size());
		
		this.newbornStateDistrib = newbornStateDistrib;
		
		this.newbornStateFromObs = stateFromObs;
		
		this.rand = rand;
		
		// test markov matrix for validity
		if (markov.getColumnDimension() != dynamicsModels.length || markov.getRowDimension() != dynamicsModels.length) {
			throw new IllegalArgumentException("MultiTargetIMMFilter.MultiTargetIMMFilter(.): markov must be a matrix of transition probabilities of size " +
					dynamicsModels.length + "x" + dynamicsModels.length + " (length of dynamic models array).");
		}

		// normalize markov matrix if necessary
		for (int j = 0; j < markov.getColumnDimension(); j++) {
			double sum = 0.0;
			
			for (int i = 0; i < markov.getRowDimension(); i++)
				sum += markov.get(i,j);
			
			if (sum == 0) {
				System.err.println("WARNING MultiTargetIMMFilter.MultiTargetIMMFilter(.): Probabilities in column of markov matrix are zero. " +
					"Assuming uniform distribution.");
				for (int i = 0; i < markov.getRowDimension(); i++)
					this.markov.set(i,j,1.0/markov.getRowDimension());
			}
			else if (sum != 1.0) {
				System.err.println("WARNING MultiTargetIMMFilter.MultiTargetIMMFilter(.): Probabilities in column of markov matrix must sum to 1 (sum=" + sum + "). " +
						"Values get normalized.");
				for (int i = 0; i < markov.getRowDimension(); i++)
					this.markov.set(i,j,this.markov.get(i,j)/sum);
			}	
		}
		
		this.pstate = initialStateDistrib;
			
		// test Gaussian mixtures of state distribution for validity
		// (Gaussian mixtures must have equal number of components as the number of dynamic models)
		for (int n = 0; n < this.pstate.size(); n++) {
			if (this.pstate.get(n).getNumOfComponents() != dynamicsModels.length) {
				throw new IllegalArgumentException("MultiTargetIMMFilter.MultiTargetIMMFilter(.): Initial state distribution must only contain Gaussian mixtures with number of " 
						+ "components equal to the number of dynamic models.");
			}
		}
		
		
		this.projector = observationModel;
		this.predictors = dynamicsModels;
		
		this.pobs = new Vector<GaussMixDistribution>(this.pstate.size());
		
		for (int n = 0; n < initialStateDistrib.size(); n++) {
			
			GaussianDistribution[] gm = new GaussianDistribution[this.pstate.get(n).getNumOfComponents()];
			
			for (int i = 0; i < gm.length; i++) {
				
				gm[i] = this.projector.transform((GaussianDistribution)this.pstate.get(n).getPdf(i));
			}	
			
			this.pobs.add(new GaussMixDistribution(gm, this.pstate.get(n).getWeights().clone(), this.rand));
		}

		
		AbstractMultiState<MotionModelID> zeroX = factoryX.createEmptyMultiState();
		this.meanX = factoryX.createEmptyMultiState();
		
		for (int n = 0; n < initialStateDistrib.size(); n++) {
			zeroX.insertState(new Matrix(factoryX.getContinuousDOF(),1), new MotionModelID((short)(n+1),(byte)-1));
			this.meanX.insertState(initialStateDistrib.get(n).getMean(), new MotionModelID((short)(n+1),(byte)-1));
			
			this.targetIDtoIdx.put((short)(n+1), n);
		}
		
		this.p_obs = new MultiObsDistributionIndepGaussMix<MotionModelID>(this.rand, this.projector.getTransformMatrix(),
				this.pobs, zeroX, factoryX, factoryZ);
	}
	
	@Override
	public void predict() {

		this.pobs.clear();
		AbstractMultiState<MotionModelID> zeroX = factoryX.createEmptyMultiState();
		
		// for each target
		for (int n = 0; n < this.pstate.size(); n++) {
			
			GaussMixDistribution gm = this.pstate.get(n); // P(x_n^{t-1} | Z^{1:t-1}) [Gaussian mixture]
			
			double[] mu_i = gm.getWeights();           // P(M^{t-1} = i | Z^{1:t-1}
			double[] mu_ij = new double[mu_i.length];  // P(M^{t-1} = i | M^t = j, Z^{1:t-1}) 
			                                           //    = P(M^t = j | M^{t-1} = i) * P(M^{t-1} = i | Z^{1:t-1}) / P(M^t = j | Z^{1:t-1})
			double[] mu_j = new double[mu_i.length];   // P(M^t = j | Z^{1:t-1})
			
			// array for the predicted Gaussian component considering model j for state transition from t-1 to t
			GaussianDistribution[] gauss_j = new GaussianDistribution[mu_i.length];
			
			// for each dynamic model
			for (int j = 0; j < mu_j.length; j++) {
				
				// compute mu_ij and mu_j
				mu_j[j] = 0.0;
				for (int i = 0; i < mu_i.length; i++) {
					mu_ij[i] = this.markov.get(i,j) * mu_i[i];
					mu_j[j] += mu_ij[i];
				}
				
				
				// compute P(x_n^t | M^t = j, Z^{1:t-1}) and merge mixture components
				gm.setWeights(mu_ij); // this method also normalizes the weights
				gauss_j[j] = this.predictors[j].transform(new GaussianDistribution(gm.getMean(), gm.getCovariance(), this.rand));
			}
			
			// P(x_n^t | Z^{1:t-1}) as Gaussian mixture
			for (int j = 0; j < mu_j.length; j++)
				gm.setPdf(j, gauss_j[j]);
			
			gm.setWeights(mu_j);
			
			
			// create predicted observation distribution for target n
			GaussianDistribution[] gm_obs = new GaussianDistribution[gm.getNumOfComponents()];
				
			for (int j = 0; j < gm_obs.length; j++) {
				
				gm_obs[j] = this.projector.transform((GaussianDistribution)gm.getPdf(j));
			}

			this.pobs.add(new GaussMixDistribution(gm_obs, gm.getWeights().clone(), this.rand));
			
			this.meanX.setState(n, gm.getMean(), this.meanX.getStateDiscrete(n));
			
			zeroX.insertState(new Matrix(this.factoryX.getContinuousDOF(),1), this.meanX.getStateDiscrete(n));
		}
		
		this.p_obs = new MultiObsDistributionIndepGaussMix<MotionModelID>(this.rand, this.projector.getTransformMatrix(),
				this.pobs, zeroX, factoryX, factoryZ);
	}


	@Override
	public void update(AbstractMultiState<MotionModelID> observation, DataAssociation association) {
		
		if (association instanceof DataAssociationExclusive) {
			
			int[] targets; // associated targets: maximum one target associated (DataAssociationExclusive)
			int n;         // target index
			short targetID;
			
			Matrix Ht = this.projector.getTransformMatrix().transpose();
			
			int born = 0;
			int existassoc = 0;
			
			// update state distribution for each target associated with one of the observations
			for (int m = 0; m < observation.getNumberOfStates(); m++) {
				
				targets = association.getAssociatedTargets(m+1);
				
				if (targets != null) {
					// observation is associated to one target exclusively
					targetID = (short)targets[0];
					
					// associated with existing target
					if (this.targetIDtoIdx.containsKey(targetID)) {
						n = this.targetIDtoIdx.get(targetID);
					
						GaussMixDistribution gm_state = this.pstate.get(n);
						GaussMixDistribution gm_obs = this.pobs.get(n);
						double[] mu_j = new double[gm_state.getNumOfComponents()];
						
						Matrix z = observation.getStateContinuous(m);
						Matrix x,P,Sinv,K,x_upd,P_upd;
						
						// update each component of the Gaussian mixture state distribution
						for (int j = 0; j < mu_j.length; j++) {
							
							x = ((GaussianDistribution)gm_state.getPdf(j)).getMean();
							P = ((GaussianDistribution)gm_state.getPdf(j)).getCovariance();
							Sinv = (((GaussianDistribution)gm_obs.getPdf(j)).getCovariance()).inverse();
							
							mu_j[j] = gm_obs.getPdf(j).p(z) * gm_obs.getWeight(j);
							
							K = P.times(Ht.times(Sinv));
							
							x_upd = x.plus(K.times(z.minus(this.projector.getTransformMatrix().times(x))));
							P_upd = P.minus(K.times(this.projector.getTransformMatrix().times(P)));
	
							// update Gaussian component
							gm_state.setPdf(j, new GaussianDistribution(x_upd, P_upd, this.rand));
						}
	
						// update weights
						gm_state.setWeights(mu_j);
						
						this.meanX.setState(n, gm_state.getMean(), this.meanX.getStateDiscrete(n));
						this.meanX.getStateDiscrete(n).time = -this.delta_t;
						
						observation.getStateDiscrete(m).ID = targetID;
						
						existassoc++;
					}
					else { // newborn target
						n = this.meanX.getNumberOfStates();
						this.targetIDtoIdx.put(targetID, n);
						
						GaussMixDistribution gm_state = this.newbornStateDistrib.copy();
						
						for (int i = 0; i < gm_state.getNumOfComponents(); i++) {
							((GaussianDistribution)gm_state.getPdf(i)).setMean(
									this.newbornStateFromObs.times(observation.getStateContinuous(m)));
						}
						
						this.meanX.insertState(gm_state.getMean(), new MotionModelID(targetID, (byte)-1));
						this.meanX.getStateDiscrete(n).time = -this.delta_t;
						
						this.pstate.add(gm_state);
						
						
						observation.getStateDiscrete(m).ID = targetID;
						
						born++;
					}
				}
				else {
					observation.getStateDiscrete(m).ID = (short)0;
				}
				
			}

		//	System.out.println(existassoc + " existing targets associated.");
		//	System.out.println(born + " targets born.");
			
			// update time to last association
			for (n = 0; n < this.meanX.getNumberOfStates(); n++) {
				this.meanX.getStateDiscrete(n).time += this.delta_t;
			}
			
			// death of targets
			this.letTargetsDie();
			
		}
		else {
			throw new IllegalArgumentException("Data association method must return DataAssociationExclusive objects.");
		}
		
	}



	@Override
	public AbstractMultiState<MotionModelID> getMean() {
		return this.meanX;
	}

	@Override
	public MultiTargetPredictionFilterIndep<MotionModelID> copy() {
		return new MultiTargetIMMFilter(this);
	}

	@Override
	public AbstractMultiObservationDistributionIndep<MotionModelID, MotionModelID> getObservationDistribution() {
		return this.p_obs;
	}

	/**
	 * Let targets die randomly using the specified distribution of target death and the internal random generator.
	 * Used by RBMCDA!!
	 */
	protected void letTargetsDie() {
		int died = 0;
		
		for (int n = this.meanX.getNumberOfStates()-1; n >= 0; n--) {
			
			double P_death = this.pdeath.P(this.meanX.getStateDiscrete(n).time);
			
			if (this.rand.nextDouble() <= P_death) {
				this.meanX.removeState(n);
				this.pstate.remove(n);
				this.pobs.remove(n);
				this.p_obs.getCondition().removeState(n);
				died++;
			}
		}
		
	//	System.out.println(died + " targets died.");
		
		// update IDtoIdx hash
		if (died > 0) {
			this.targetIDtoIdx.clear();
			
			for (int n = 0; n < this.meanX.getNumberOfStates(); n++) {
				this.targetIDtoIdx.put(this.meanX.getStateDiscrete(n).ID, n);
			}
		}
	}
	
	public Set<Short> getExistingTargetIDs() {
		return this.targetIDtoIdx.keySet();
	}

}
