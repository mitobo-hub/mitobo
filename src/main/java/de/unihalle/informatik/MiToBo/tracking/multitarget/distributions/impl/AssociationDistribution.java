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

import java.io.IOException;
import java.io.OutputStream;
import java.util.Random;

import de.unihalle.informatik.MiToBo.math.MathX;
import de.unihalle.informatik.MiToBo.math.distributions.impl.GenericDiscreteDistribution;
import de.unihalle.informatik.MiToBo.math.distributions.interfaces.EvaluatableDistribution;
import de.unihalle.informatik.MiToBo.math.distributions.interfaces.LogEvaluatableDistribution;
import de.unihalle.informatik.MiToBo.math.distributions.interfaces.LogProbabilityDensityFunction;
import de.unihalle.informatik.MiToBo.math.distributions.interfaces.LogProbabilityMassFunction;
import de.unihalle.informatik.MiToBo.tracking.multitarget.algo.MultiObservationTrackerRBMCDAIMM;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.abstracts.AbstractMultiState;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.impl.DataAssociationExclusiveFactory;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.impl.TargetID;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.interfaces.DataAssociation;
import de.unihalle.informatik.MiToBo.tracking.multitarget.distributions.abstracts.AbstractAssociationDistribution;
import de.unihalle.informatik.MiToBo.tracking.multitarget.distributions.abstracts.AbstractMultiObservationDistributionIndep;

/**
 * Association distribution to sample association variables for a set of observations based 
 * on a model of how observations are formed.
 * <br><br>
 * Observations are comprised from existing targets that are detected with probability <code>P_D</code>, 
 * a number of observations of newborn targets distributed according to a distribution <code>nu</code>
 * and a number of clutter observations distributed according to a distribution <code>mu</code>.
 * <br><br>
 * The association variables of the individual observations are sampled sequentially and
 * their distributions are assumed to depend on the likelihood of the current observation
 * for a specific realization of the association variable and the probability of the association variable
 * given all previous associations. Note that the likelihood of observations that are not yet 
 * associated is not considered here!!
 * 
 * @author Oliver Gress
 *
 * @param <S> Type of discrete variables in the multi target observation
 * @param <T> Type of discrete variables in the multi target state
 */
public class AssociationDistribution<S extends TargetID,T extends TargetID> extends
		AbstractAssociationDistribution<S, T> implements EvaluatableDistribution<DataAssociation>,
		LogEvaluatableDistribution<DataAssociation> {

	/** Distribution of the number of clutter observations */
	protected LogProbabilityMassFunction mu;
	/** (log) values of mu to avoid recomputation */
	protected double[] logMuValues;

	/** Distribution of the number of observations from newborn targets */
	protected LogProbabilityMassFunction nu;
	/** (log) values of nu to avoid recomputation */
	protected double[] logNuValues;
	
	/** Binomial distribution of number of observations associated to existing targets **/
	protected double[] logBinom;
	
	/** Number of observations in last call of drawSample() */
	protected int lastM;
	
	/** Number of targets in last call of drawSample() */
	protected int lastN;
	
	// arrays to hold values for iterative computation of data association prior
	protected double[] psi;
	protected double[] phi_0, phi_1;
	protected double[][] chi;
	
	/** Minimum of number of observations and number of targets */
	protected int minMN;
	
	/** Probability of target detection */
	protected double P_D;
	
	/** (log) propability of M observations given N existing targets and the current model configuration */
	protected double logP_MN;
	
	/** Last sample that was sampled */
	protected DataAssociation lastSample;
	
	/** (log) probability of the current set of association variables given observations and previous associations */
	protected double logP_C;
	
	/** Maximum number of observations in the time series */
	protected int M_max;
	
	/** Target-ID to start from for newborn targets */
	protected int newtargetID;
	
	/**
	 * Constructor.
	 * @param rand random generator for sampling
	 * @param Z the current observations
	 * @param observationDistrib distribution of the observations model
	 * @param spatialClutterDistrib spatial distribution of possible clutter appearance
	 * @param spatialNewbornDistrib spatial distribution of possible newborn appearance
	 * @param mu distribution of the number of clutter observations
	 * @param nu distribution of the number of observations from newborn targets
	 * @param P_D probability of target detection
	 */
	public AssociationDistribution(Random rand,
									AbstractMultiState<S> Z,
									AbstractMultiObservationDistributionIndep<S, T> observationDistrib,
									LogProbabilityDensityFunction spatialClutterDistrib,
									LogProbabilityDensityFunction spatialNewbornDistrib,
									LogProbabilityMassFunction mu,
									LogProbabilityMassFunction nu,
									double P_D) {
		
		super(rand, Z, observationDistrib, spatialClutterDistrib, spatialNewbornDistrib,
				new DataAssociationExclusiveFactory());
		
		if (P_D > 1.0 || P_D < 0.0) {
			throw new IllegalArgumentException("AssociationDistributionSMCCondModel: P_D must be in the range [0.0,1.0]");
		}
		
		this.mu = mu;
		this.nu = nu;
		this.P_D = P_D;
		this.newtargetID = 1;
		this.M_max = -1;
		this.lastM = -1;
		this.lastN = -1;
		this.logP_MN = Double.NaN;
		this.reset();
	}
	
	/**
	 * Constructor where the maximum number of observations in the time series is specified to avoid some
	 * re-computations.
	 * @param rand random generator for sampling
	 * @param Z the current observations
	 * @param observationDistrib distribution of the observations model
	 * @param spatialClutterDistrib spatial distribution of possible clutter appearance
	 * @param spatialNewbornDistrib spatial distribution of possible newborn appearance
	 * @param mu distribution of the number of clutter observations
	 * @param nu distribution of the number of observations from newborn targets
	 * @param P_D probability of target detection
	 * @param M_max maximum number of observations in the time series
	 */
	public AssociationDistribution(Random rand,
			AbstractMultiState<S> Z,
			AbstractMultiObservationDistributionIndep<S, T> observationDistrib,
			LogProbabilityDensityFunction spatialClutterDistrib,
			LogProbabilityDensityFunction spatialNewbornDistrib,
			LogProbabilityMassFunction mu,
			LogProbabilityMassFunction nu,
			double P_D,
			int M_max) {
		
		super(rand, Z, observationDistrib, spatialClutterDistrib, spatialNewbornDistrib,
		new DataAssociationExclusiveFactory());
		
		if (P_D > 1.0 || P_D < 0.0) {
			throw new IllegalArgumentException("AssociationDistributionSMCCondModel: P_D must be in the range [0.0,1.0]");
		}
		
		this.mu = mu;
		this.nu = nu;

		this.M_max = M_max;
		this.logMuValues = new double[this.M_max+1];
		this.logNuValues = new double[this.M_max+1];
		
		for (int b = 0; b <= this.M_max; b++) {
			this.logMuValues[b] = this.mu.log_p(b);
			this.logNuValues[b] = this.nu.log_p(b);
		}	
		
		this.P_D = P_D;
		this.newtargetID = 1;
		this.lastM = -1;
		this.lastN = -1;
		this.logP_MN = Double.NaN;
		this.reset();
	}

	/** Specify the starting target-ID for newborn targets */
	public void letNewbornTargetIDsStartFrom(int minNewTargetID) {
		this.newtargetID = minNewTargetID;
	}
	
	@Override
	public DataAssociation drawSample() {
		
		return this.drawSampleDebug(null, null);
	}

	@Override
	public DataAssociation drawSampleDebug(DataAssociation groundtruth,
			OutputStream ostream) {

		int minNewTargetID = this.newtargetID;
		
		AbstractMultiState<T> zmean = this.obsdistrib.getMean();
		
		// d(m,k_mminus;M,N)
		double d_m = -Math.log(this.M);	// d(1,0;M,N)
		
		// f_0 := f(k,k_{m-1}) and f_1 := f(k,k_{m-1} + 1)
		// init: k_{m-1} = 0
		double[] f_0 = new double[this.minMN+1];	// f(k,0)
		double[] f_1 = new double[this.minMN+1];	// f(k,1)
		double[] f_tmp = null; 						// helper reference
	
		for (int k = 0; k <= this.minMN; k++) {
			f_0[k] = this.logBinom[k];
			f_1[k] = Math.log(k) + this.logBinom[k];
		}
		
		// g_0 := g(b,b_{m-1}) and g_1 := g(b,b_{m-1} + 1)
		// init: b_{m-1} = 0
		double[] g_0 = new double[this.M+1];	// g(b,0)
		double[] g_1 = new double[this.M+1];	// g(b,1)
		double[] g_tmp = null;					// helper reference
		
		// h_0 := h(r,m,r_{m-1};M) and h_1 := h(r,m,r_{m-1} + 1;M)  !!  r := k+b; r_{m-1} = k_{m-1} + b_{m-1} 
		// init: r_{m-1} = 0
		double[] h_0 = new double[this.M+1];	// h(r,1,0;M)
		double[] h_1 = new double[this.M+1];	// h(r,1,1;M)
		double[] h_tmp = null;					// helper reference
		
		for (int b = 0; b <= this.M; b++) {
			g_0[b] = this.logNuValues[b];
			g_1[b] = Math.log(b) + this.logNuValues[b];
			h_0[b] = Math.log(this.M-b) + this.logMuValues[this.M-b];	// use running variable 'b' to represent 'r' here
			h_1[b] = this.logMuValues[this.M-b];			 	// use running variable 'b' to represent 'r' here
		}
		
		double logPcz = 0;	// log joint probability p(c^t_{1:m} | C^{1:t-1}, Z^{1:t})
		double logPc = this.logP_MN;
		
		int k_mminus = 0;	// k_{m-1}
		int b_mminus = 0;	// b_{m-1}
		
		// new association object of the given type for output
		DataAssociation C = this.assocfactory.createDataAssociation();
		
		// nominators of data association prior for association to clutter, existing and newborn target
		double q_clutter, q_exist, q_newborn;
		
		// variables for upper sum limits
		int k_lim, b_lim, r_lim;
		
		// variables for sums over b
		double bsum_clutter, bsum_exist, bsum_newborn;
		
		// array for which targets were already associated
		boolean[] associatedTargets = new boolean[this.N];
		
		// array for probability p(c^t_m | c^t_{1:m-1}, C^{1:t-1}, Z^{1:t})
		double[] pc_array = new double[this.N+2];
		// probability object p(c^t_m | c^t_{1:m-1}, C^{1:t-1}, Z^{1:t}) for sampling
		GenericDiscreteDistribution pc = null;
		
		// sample C by sequential sampling of c^t_m ~ p(c^t_m | c^t_{1:m-1}, C^{1:t-1}, Z^{1:t}) = p(z^t_m | c^t_m, ...)*p(c^t_m | c^t_{1:m-1}, M^t, ^{(i)}N^t)
		for (int m = 1; m <= this.M; m++) {
			
			// compute nominators of data association prior for association to clutter, existing and newborn target
			q_clutter = 0; 
			q_exist = 0; 
			q_newborn = 0;
			
			// clutter
			k_lim = Math.min(this.N, this.M-m+k_mminus);
			for (int k = k_mminus; k <= k_lim; k++) {
				
				bsum_clutter = 0;
				
				b_lim = this.M-m-k+k_mminus+b_mminus;
				
				for (int b = b_mminus; b <= b_lim; b++) {
					bsum_clutter = MathX.logSumP(bsum_clutter, g_0[b] + h_0[b+k]);
				}
				
				q_clutter = MathX.logSumP(q_clutter, f_0[k] + bsum_clutter);
			}
			
			// existing target
			k_lim = Math.min(this.N, this.M-m+k_mminus+1);
			for (int k = k_mminus+1; k <= k_lim; k++) {

				bsum_exist = 0; 
				
				b_lim = this.M-m-k+k_mminus+1+b_mminus;
				for (int b = b_mminus; b <= b_lim; b++) {
					bsum_exist = MathX.logSumP(bsum_exist, g_0[b] + h_1[b+k]);
				}
				
				q_exist = MathX.logSumP(q_exist, f_1[k] + bsum_exist);
			}
			
			// newborn target
			k_lim = Math.min(this.N, this.M-m+k_mminus);
			for (int k = k_mminus; k <= k_lim; k++) {
				
				bsum_newborn = 0;
				
				b_lim = this.M-m-k+k_mminus+b_mminus+1;
				
				for (int b = b_mminus+1; b <= b_lim; b++) {
					bsum_newborn = MathX.logSumP(bsum_newborn, g_1[b] + h_1[b+k]);
				}
				
				q_newborn = MathX.logSumP(q_newborn, f_0[k] + bsum_newborn);
			}
			
			
			q_clutter += d_m - logPc;
			q_exist += d_m - Math.log(this.N - k_mminus) - logPc;
			q_newborn += d_m - logPc;
		
			
			// construct p(c^t_m | c^t_{1:m-1}, C^{1:t-1}, Z^{1:t}) for sampling
			pc_array[0] = this.log_pzc[m-1][0] + q_clutter;
			for (int n = 1; n <= this.N; n++) {
				if (associatedTargets[n-1] == false)
					pc_array[n] = this.log_pzc[m-1][n] + q_exist;
				else
					pc_array[n] = Double.NEGATIVE_INFINITY;
			}
			pc_array[this.N+1] = this.log_pzc[m-1][this.N+1] + q_newborn;
		
			pc = new GenericDiscreteDistribution(pc_array, this.rand, true);
			
			// sample c^t_m
			int c_m = pc.drawSample();
			
			
			if (groundtruth != null) {
				
				if (ostream == null)
					ostream = System.err;
				
				
				int gtTgt[] = groundtruth.getAssociatedTargets(m);
				
				String s = null;
				if ((c_m == 0 && gtTgt != null)					
						|| (gtTgt == null && c_m != 0)
						|| (c_m > this.N && gtTgt[0] < minNewTargetID)
						|| (c_m > 0 && c_m <= this.N && zmean.getStateDiscrete(c_m-1).ID != gtTgt[0])) {
					
					s = "Observation " + m + " incorrectly associated to ";
					s += (c_m == 0 ? "0 (clutter)" : (c_m <= this.N ? zmean.getStateDiscrete(c_m-1).ID + " (" + c_m + ")" : this.newtargetID + " (newborn)"));
					s += " instead of ";
					
					if (gtTgt != null) {
						
						if (gtTgt[0] >= minNewTargetID) {
						
							s += gtTgt[0] + " (newborn)\n"; 
						}
						else {
							int tgtIdx = -1;
							for (int idx = 0; idx < zmean.getNumberOfStates(); idx++) {
								if (zmean.getStateDiscrete(idx).ID == gtTgt[0]) {
									tgtIdx = idx;
									break;
								}
							}
						
							s += gtTgt[0] + " (" + tgtIdx + ")\n"; 
						}
					}
					else {
						s += "0 (clutter)\n";
					}
					

					s += pc.toString() + "\n";
					
				}
				s=null;
				if (s != null)
					try {
						ostream.write(s.getBytes());
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
			}
			
			
			
			// update log joint probability p(c^t_{1:m} | C^{1:t-1}, Z^{1:t})
			logPcz += pc.log_p(c_m);
			
//			if (MultiObservationTrackerRBMCDAIMM.current_t == 7 && MultiObservationTrackerRBMCDAIMM.current_i == 2
//					&& (m == 14 || m == 15)) 
//				System.err.print(m + " "+ c_m + " " + pc.log_p(c_m) + "\n" + pc.toString() + "\n-----\n");
//			
			// update variables and arrays
			d_m -= Math.log(this.M - m);
			
			if (c_m == 0) {
				// clutter association
				h_tmp = h_1;
				
				r_lim = this.M - m + k_mminus + b_mminus;
				for (int r = k_mminus+b_mminus; r <= r_lim; r++) {
					h_tmp[r] = Math.log(this.M - r - m + k_mminus + b_mminus) + h_0[r];
				}
				
				h_1 = h_0;
				h_0 = h_tmp;
				
				logPc += q_clutter;
			}
			else if (c_m <= this.N) {
				// association to existing target
				C.setAssociation(zmean.getStateDiscrete(c_m-1).ID, m);
				associatedTargets[c_m-1] = true;
				
				f_tmp = f_0;
				
				k_lim = Math.min(this.N, this.M - m + k_mminus + 1);
				for (int k = k_mminus + 2; k <= k_lim; k++) {
					f_tmp[k] += Math.log(k - k_mminus) + Math.log(k - k_mminus - 1);
				}
				
				f_0 = f_1;
				f_1 = f_tmp;
				
				d_m -= Math.log(this.N - k_mminus);
				k_mminus++;
				
				logPc += q_exist;
			}
			else {
				// association to newborn target
				C.setAssociation(this.newtargetID++, m);
				
				g_tmp = g_0;
				
				b_lim = this.M - m + b_mminus + 1;
				for (int b = b_mminus + 2; b <= b_lim; b++) {
					g_tmp[b] += Math.log(b - b_mminus) + Math.log(b - b_mminus - 1);
				}
				
				g_0 = g_1;
				g_1 = g_tmp;
				
				b_mminus++;
				
				logPc += q_newborn;
			}
			
			
		}
		
		this.lastSample = C;
		this.logP_C = logPcz;
//		
//		if (MultiObservationTrackerRBMCDAIMM.current_t == 7 && MultiObservationTrackerRBMCDAIMM.current_i == 2) {
//			System.err.println("logPcz = " + logPcz + "\n" + C.toString() + "\nN="+this.N);
//		}
		
		return C;	
		
	}
	
	
	
	/**
	 * This method is here only valid for the latest DataAssociation sampled with <code>drawSample()</code>.
	 * If no DataAssociation was sampled before or the given DataAssociation x is a different object than
	 * the latest sampled DataAssociation or a new observation was set, this method return -1 !!
	 */
	@Override
	public double p(DataAssociation x) {
		if (this.lastSample == null || x != this.lastSample)
			return -1;
		else
			return Math.exp(this.logP_C);
	}
	
	/**
	 * This method is here only valid for the latest DataAssociation sampled with <code>drawSample()</code>.
	 * If no DataAssociation was sampled before or the given DataAssociation x is a different object than
	 * the latest sampled DataAssociation or a new observation was set, this method returns Double.NaN !!
	 */
	@Override
	public double log_p(DataAssociation x) {
		if (this.lastSample == null || x != this.lastSample)
			return Double.NaN;
		else
			return this.logP_C;
	}
	
	@Override
	public void setNewObservations(AbstractMultiState<S> Z,
			AbstractMultiObservationDistributionIndep<S, T> observationDistrib) {
		
		this.lastM = this.M;
		this.lastN = this.N;
		
		super.setNewObservations(Z, observationDistrib);
		
		this.reset();
	}
	
	protected void reset() {
		// min(M,N)
		this.minMN = (this.M < this.N) ? this.M : this.N;
		
		// store values of mu and nu in arrays to prevent duplicate computations if 
		// drawSample() is called multiple times
		// if M_max was set these arrays were filled in the constructor and 
		// are only computed once for this object!!
		if (this.M_max == -1) {
			this.logMuValues = new double[this.M+1];
			this.logNuValues = new double[this.M+1];
			
			for (int b = 0; b <= this.M; b++) {
				this.logMuValues[b] = this.mu.log_p(b);
				this.logNuValues[b] = this.nu.log_p(b);
			}	
		}
		
		// compute binomial distribution and store in array
		// for given N. If N does not differ from previous computations,
		// the existing array needs not to be recomputed
		if (this.logBinom == null || this.logBinom.length != this.N+1) {
			this.logBinom = new double[this.minMN+1];
			
			if (this.P_D < 1.0) {
				this.logBinom[0] = Math.log(1.0 - this.P_D) * this.N;
				double log_s = Math.log(this.P_D) - Math.log1p(- this.P_D);
				
				for (int k = 1; k <= this.minMN; k++)
					this.logBinom[k] = Math.log(this.N - k + 1) - Math.log(k) + log_s + this.logBinom[k-1];
			}
			else {
				for (int k = 0; k <= this.minMN; k++) {
					if (k != this.N)
						this.logBinom[k] = Double.NEGATIVE_INFINITY;
					else
						this.logBinom[k] = 0;
				}
			}
		}
		
		// compute P_MN. It needs not to be recomputed if M and N do not differ
		// from previous computations
		if (Double.isNaN(this.logP_MN) || this.lastM != this.M || this.lastN != this.N) {
			this.logP_MN = 0.0;
			
			for (int k = 0; k <= this.minMN; k++)
				for (int b = 0; b <= this.M-k; b++)
					this.logP_MN = MathX.logSumP(this.logP_MN, this.logBinom[k] + this.logNuValues[b] + this.logMuValues[M-k-b]);
		}
		
		this.lastSample = null;	
		this.logP_C = Double.NaN;
	}

}
