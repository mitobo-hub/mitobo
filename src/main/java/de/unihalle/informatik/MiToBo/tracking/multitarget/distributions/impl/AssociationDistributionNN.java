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

package de.unihalle.informatik.MiToBo.tracking.multitarget.distributions.impl;

import de.unihalle.informatik.MiToBo.core.datatypes.MTBTreeNode;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBTreeNodeData;
import de.unihalle.informatik.MiToBo.math.LogFaculty;
import de.unihalle.informatik.MiToBo.math.MathX;
import de.unihalle.informatik.MiToBo.math.distributions.impl.GenericDiscreteDistribution;
import de.unihalle.informatik.MiToBo.math.distributions.interfaces.LogProbabilityDensityFunction;
import de.unihalle.informatik.MiToBo.math.distributions.interfaces.LogProbabilityMassFunction;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.abstracts.AbstractMultiState;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.impl.TargetID;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.interfaces.DataAssociation;
import de.unihalle.informatik.MiToBo.tracking.multitarget.distributions.abstracts.AbstractMultiObservationDistributionIndep;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Random;
import java.util.Stack;
import java.util.Vector;


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
 * given all previous associations. Furthermore the likelihood of nearby observations is considered. The
 * number of the considered neighboring observations can be controlled by a maximum number as well as a
 * maximum Euklidean distance. Keep the combinatorial problem in mind that explodes when the number of
 * considered neighbors increases!!
 * 
 * @author Oliver Gress
 *
 * @param <S> Type of discrete variables in the multi target observation
 * @param <T> Type of discrete variables in the multi target state
 */
public class AssociationDistributionNN<S extends TargetID,T extends TargetID> extends
		AssociationDistribution<S, T> {

	/** Stores for each observation the k-nearest observations that will be 
	 * associated after the corresponding observation.
	 */
	protected ObsDistance[][] kNearestObs;
	
	/** An object to compute and store log(n!) */
	protected LogFaculty logFac;
	
	/** Maximum number of neighboring observations */
	protected int maxNumNeighbors;
	
	/** Maximum distance of neighboring observations */
	protected double maxDistNeighbors;
	
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
	 * @param maxNumNeighbors maximum number of neighboring observations to be considered for each association
	 * @param maxDistNeighbors maximum Euklidean distance of neighboring observations to be considered for each association
	 */
	public AssociationDistributionNN(Random rand, AbstractMultiState<S> Z,
			AbstractMultiObservationDistributionIndep<S, T> observationDistrib,
			LogProbabilityDensityFunction spatialClutterDistrib,
			LogProbabilityDensityFunction spatialNewbornDistrib,
			LogProbabilityMassFunction mu, LogProbabilityMassFunction nu,
			double P_D,
			int maxNumNeighbors,
			double maxDistNeighbors) {
		super(rand, Z, observationDistrib, spatialClutterDistrib,
				spatialNewbornDistrib, mu, nu, P_D);
		
		this.maxNumNeighbors = maxNumNeighbors;
		this.maxDistNeighbors = maxDistNeighbors;
		this.logFac = new LogFaculty(Z.getNumberOfStates());
		
		this.reset();

		this.kNearestObs = this.kNearestObservations(this.maxNumNeighbors, this.maxDistNeighbors);
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
	 * @param maxNumNeighbors maximum number of neighboring observations to be considered for each association
	 * @param maxDistNeighbors maximum Euklidean distance of neighboring observations to be considered for each association
	 * @param M_max maximum number of observations in the time series
	 */
	public AssociationDistributionNN(Random rand, AbstractMultiState<S> Z,
			AbstractMultiObservationDistributionIndep<S, T> observationDistrib,
			LogProbabilityDensityFunction spatialClutterDistrib,
			LogProbabilityDensityFunction spatialNewbornDistrib,
			LogProbabilityMassFunction mu, LogProbabilityMassFunction nu,
			double P_D, int M_max,
			int maxNumNeighbors,
			double maxDistNeighbors) {
		super(rand, Z, observationDistrib, spatialClutterDistrib,
				spatialNewbornDistrib, mu, nu, P_D, M_max);
		
		this.maxNumNeighbors = maxNumNeighbors;
		this.maxDistNeighbors = maxDistNeighbors;
		this.logFac = new LogFaculty(M_max);
		
		this.reset();

		this.kNearestObs = this.kNearestObservations(this.maxNumNeighbors, this.maxDistNeighbors);
	}
	
	@Override
	public DataAssociation drawSample() {

		return this.drawSampleDebug(null, null);
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public DataAssociation drawSampleDebug(DataAssociation groundtruth,
			OutputStream ostream) {

		int minNewTargetID = this.newtargetID;
		
		AbstractMultiState<T> zmean = this.obsdistrib.getMean();
		
		
		double logPc = this.logP_MN;	// logarithm of the denominator p(c_{1:m-1} | M,N) in p(c_m | c_{1:m-1},M,N)=p(c_{1:m-1},c_m | M,N) / p(c_{1:m-1} | M,N)
		double logPcz = 0;	// log of joint distribution p(c^t_{1:m} | C^{1:t-1}, Z^{1:t})
		
		int k_mminus = 0;	// k_{m-1}
		int b_mminus = 0;	// b_{m-1}
		
		// new association object of the given type for output
		DataAssociation C = this.assocfactory.createDataAssociation();
		
		// log probabilities of data association prior for association to clutter, existing and newborn target
		double log_p_clutter, log_p_exist, log_p_newborn;
		
		// array for which targets were already associated
		boolean[] associatedTargets = new boolean[this.N];
		Vector<Integer> availableTargets = new Vector<Integer>(this.N);
		for (int n = 1; n <= this.N; n++)
			availableTargets.add(n);
		
		// array for log probability p(c^t_m | c^t_{1:m-1}, C^{1:t-1}, Z^{1:t})
		double[] pc_array = new double[this.N+2];
		
		// probability object p(c^t_m | c^t_{1:m-1}, C^{1:t-1}, Z^{1:t}) for sampling
		GenericDiscreteDistribution pc = null;

		MTBTreeNode qTreeNode = null, node_clutter = null, node_newborn = null, node_exist = null;
		
		Stack<Integer> stackKnearestObs = new Stack<Integer>();
		
		// sample C by sequential sampling of c^t_m ~ p(c^t_m | c^t_{1:m-1}, C^{1:t-1}, Z^{1:t}) = p(z^t_m | c^t_m, ...)*p(c^t_m | c^t_{1:m-1}, M^t, ^{(i)}N^t)
		for (int m = 1; m <= this.M; m++) {

			ObsDistance[] knearestObs = this.kNearestObs[m-1];
			int numKnearest = (knearestObs != null) ? knearestObs.length : 0;
			
			stackKnearestObs.clear();
			
			if (numKnearest > 0) {
				for (int i = knearestObs.length-1; i >= 0; i--)
					stackKnearestObs.push(knearestObs[i].obsIdx);	
			}
		
			
			// compute nominators of data association prior for association to clutter, existing and newborn target

			node_clutter = (qTreeNode != null && qTreeNode.getChilds().size() > 1) ? qTreeNode.getChilds().get(0) : null;
			node_newborn = (qTreeNode != null && qTreeNode.getChilds().size() > 1) ? qTreeNode.getChilds().get(1) : null;
			node_exist = (qTreeNode != null && qTreeNode.getChilds().size() > 2) ? qTreeNode.getChilds().get(2) : null;
			
			// clutter
			node_clutter = this.compute_qAhead(m, m+numKnearest, k_mminus, b_mminus, logPc, node_clutter);
			((ProbTreeData)node_clutter.getData()).c = AType.CLUTTER;
			
			// existing target
			node_newborn = this.compute_qAhead(m, m+numKnearest, k_mminus, b_mminus+1, logPc, node_newborn);
			((ProbTreeData)node_newborn.getData()).c = AType.NEWBORN;
			
			// newborn target
			node_exist = this.compute_qAhead(m, m+numKnearest, k_mminus+1, b_mminus, logPc, node_exist);
			if (node_exist != null)
				((ProbTreeData)node_exist.getData()).c = AType.EXISTING;
			
			
			// log p(c_m | c_{1:m-1},M,N)
			log_p_clutter = ((ProbTreeData)node_clutter.getData()).log_p;
			log_p_newborn = ((ProbTreeData)node_newborn.getData()).log_p;
			if (node_exist != null)
				log_p_exist = ((ProbTreeData)node_exist.getData()).log_p;
			else
				log_p_exist = Double.NEGATIVE_INFINITY;

			// compute values of log p(c^t_m | c^t_{1:m-1}, C^{1:t-1}, Z^{1:t}) for each c^t_m
			// - clutter
			pc_array[0] = this.log_pzc[m-1][0] + log_p_clutter + this.compute_pczAhead(node_clutter, availableTargets, stackKnearestObs);		

			// - existing targets
			for (int n = 1; n <= this.N; n++) {
				if (associatedTargets[n-1] == false) {
					int idx = availableTargets.indexOf(n);
					availableTargets.removeElement(n);
					
					pc_array[n] = this.log_pzc[m-1][n] + log_p_exist + this.compute_pczAhead(node_exist, availableTargets, stackKnearestObs);
					
					availableTargets.add(idx, n);
				}
				else
					pc_array[n] = Double.NEGATIVE_INFINITY;
			}	
			
			// - newborn target
			pc_array[this.N+1] = this.log_pzc[m-1][this.N+1] + log_p_newborn + this.compute_pczAhead(node_newborn, availableTargets, stackKnearestObs);
			
			// p(c^t_m | c^t_{1:m-1}, C^{1:t-1}, Z^{1:t})
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
				
				
				s = null;
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
//					&& (m == 14 || m == 15)) {
//				System.err.print(m + " "+ c_m + " " + pc.log_p(c_m) + "\n" + pc.toString() + "\n-----\n");
//			}
			
			if (c_m == 0) {
				// clutter association

				qTreeNode = node_clutter;
				logPc += log_p_clutter;
			}
			else if (c_m <= this.N) {
				// association to existing target
				C.setAssociation(zmean.getStateDiscrete(c_m-1).ID, m);
				associatedTargets[c_m-1] = true;
				availableTargets.removeElement(c_m);

				qTreeNode = node_exist;
				logPc += log_p_exist;
				
				k_mminus++;
			}
			else {
				// association to newborn target
				C.setAssociation(this.newtargetID++, m);

				qTreeNode = node_newborn;
				logPc += log_p_newborn;
				
				b_mminus++;
			}
			
			
		}
		
		this.lastSample = C;
		this.logP_C = logPcz;
		
//		if (MultiObservationTrackerRBMCDAIMM.current_t == 7 && MultiObservationTrackerRBMCDAIMM.current_i == 2) {
//			System.err.println("\nlogPcz = " + logPcz + "\n" + C.toString() + "\nN="+this.N);
//		}
		
		return C;	
	}
	
	/**
	 * Compute probability of all possible associations of neighboring observations including their
	 * likelihood
	 * @param pcTree
	 * @param availableTargets
	 * @param observations
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private double compute_pczAhead(MTBTreeNode pcTree, Vector<Integer> availableTargets, Stack<Integer> observations) {
		
		if (pcTree.getChilds().size() > 0 && observations.size() > 0) {

			int m = observations.pop();
			
			// clutter			
			MTBTreeNode child = pcTree.getChilds().get(0);
			
			double sum = this.log_pzc[m][0] + ((ProbTreeData)child.getData()).log_p + this.compute_pczAhead(child, availableTargets, observations);

			
			// newborn target
			child = pcTree.getChilds().get(1);
			
			sum = MathX.logSumP(sum, this.log_pzc[m][this.N+1] + ((ProbTreeData)child.getData()).log_p + this.compute_pczAhead(child, availableTargets, observations));
			
			
			// existing targets
			if (pcTree.getChilds().size() == 3) {
				
			child = pcTree.getChilds().get(2);
			
				for (int n = 0; n < availableTargets.size(); n++) {
					int tgt = availableTargets.elementAt(n);
					availableTargets.removeElementAt(n);
					
					sum = MathX.logSumP(sum, this.log_pzc[m][tgt] + ((ProbTreeData)child.getData()).log_p + this.compute_pczAhead(child, availableTargets, observations));
					
					availableTargets.add(n, tgt);
				}
			}

			
			observations.push(m);
			
			return sum;
		}
		else {
			return 0;
		}
	}

	/**
	 * Compute tree of data association prior probabilities depending on previous associations.
	 * @param m
	 * @param mmax
	 * @param kmin
	 * @param bmin
	 * @param logPc_previous
	 * @param subtree
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private MTBTreeNode compute_qAhead(int m, int mmax, int kmin, int bmin, double logPc_previous, MTBTreeNode subtree) {

		if (kmin > this.N)
			return null;
		
		MTBTreeNode node = null;
		ProbTreeData data = null;
		
		if (subtree == null) {
			
			data = new ProbTreeData();
			
			int kmax = (this.N <= this.M-m+kmin) ? this.N : this.M-m+kmin;
			int bmax;
			
			data.log_p = 0;
			double sk = 0;
			double sb;
			for (int k = kmin; k <= kmax; k++) {
				sk = this.logFac.getLogFacultyFraction(k, k-kmin) + this.logBinom[k];
				sb = 0;
				bmax = this.M-m-k+kmin+bmin;
				
				for (int b = bmin; b <= bmax; b++) {
					sb = MathX.logSumP(sb, this.logFac.getLogFacultyFraction(b, b-bmin) + this.logNuValues[b]
					                  + this.logFac.getLogFacultyFraction(this.M-k-b, this.M-k-b-m+kmin+bmin) 
					                  + this.logMuValues[this.M-k-b]);
				}
				data.log_p = MathX.logSumP(data.log_p, sk + sb);
			}
			
			data.log_p += this.logFac.getLogFacultyFraction(this.M-m, this.M)
						+ this.logFac.getLogFacultyFraction(this.N-kmin, this.N);

			data.log_p -= logPc_previous;
			data.m = m;
			
			node = new MTBTreeNode(data);
		}
		else {
			node = subtree;
			data = (ProbTreeData) node.getData();
		}
		
		if (m < mmax) {
			
			if (node.getChilds().size() == 0) {
	
				// clutter
				MTBTreeNode node_clutter = this.compute_qAhead(m+1, mmax, kmin, bmin, data.log_p, null);
				((ProbTreeData)node_clutter.getData()).c = AType.CLUTTER;
				
				// newborn target
				MTBTreeNode node_newborn =  this.compute_qAhead(m+1, mmax, kmin, bmin+1, data.log_p, null);
				((ProbTreeData)node_newborn.getData()).c = AType.NEWBORN;
				
				// existing target
				MTBTreeNode node_exist = this.compute_qAhead(m+1, mmax, kmin+1, bmin, data.log_p, null);

				node.addChild(node_clutter);
				node.addChild(node_newborn);
				
				if (node_exist != null) {
					((ProbTreeData)node_exist.getData()).c = AType.EXISTING;
					node.addChild(node_exist);
				}
			}
			else {
				
				// clutter
				this.compute_qAhead(m+1, mmax, kmin, bmin, data.log_p, node.getChilds().get(0));
		
				// newborn target
				this.compute_qAhead(m+1, mmax, kmin, bmin+1, data.log_p, node.getChilds().get(1));
				
				if (node.getChilds().size() == 3) {
					// existing target
					this.compute_qAhead(m+1, mmax, kmin+1, bmin, data.log_p, node.getChilds().get(2));
				}
			}
		}
		
		return node;
	}	
	
	
	@Override
	public void setNewObservations(AbstractMultiState<S> Z,
			AbstractMultiObservationDistributionIndep<S, T> observationDistrib) {

		super.setNewObservations(Z, observationDistrib);
		
		this.kNearestObs = this.kNearestObservations(this.maxNumNeighbors, this.maxDistNeighbors);
	}
	
	/**
	 * Get the (k) nearest observations for each observation in this.Z, i.e. for each observation z_m all following
	 * observations z_{m:M} are sorted by Euklidean distance and stored in an array. If <code>k</code> is larger 0
	 * only the k-nearest observations are returned, if <code>maxDist</code> is larger 0 only observations in that range
	 * are returned. Both parameters may be specified.
	 */
	@SuppressWarnings("unchecked")
	protected ObsDistance[][] kNearestObservations(int maxNumNeighbors, double maxDistNeighbors) {

		ObsDistance[][] kNearestObs = new AssociationDistributionNN.ObsDistance[this.M][];
		
		// find nearest observation (index) next to the current observation

		if (maxNumNeighbors != 0) {
			for (int m = 0; m < this.M-1; m++) {
				ObsDistance[] dists = new AssociationDistributionNN.ObsDistance[this.M-1-m];
				
				for (int mm = m+1; mm < this.M; mm++) {
					dists[mm-m-1] = new ObsDistance((this.Z.getStateContinuous(m).minus(this.Z.getStateContinuous(mm))).normF(), mm);
				}
				Arrays.sort(dists);
				
				int numObs = dists.length;
				if (maxNumNeighbors > 0 && maxNumNeighbors < numObs) {
					numObs = maxNumNeighbors;
				}
				if (maxDistNeighbors > 0) {
					for (int i = 0; i < numObs; i++) {
						if (dists[i].dist > maxDistNeighbors) {
							numObs = i;
							break;
						}
					}
				}
				else if (maxDistNeighbors == 0)
					numObs = 0;
				
				if (numObs == 0)
					kNearestObs[m] = null;
				else if (numObs == dists.length)
					kNearestObs[m] = dists;
				else
					kNearestObs[m] = Arrays.copyOfRange(dists, 0, numObs);	
			}
		}
		return kNearestObs;
	}
	
	private class ObsDistance implements Comparable<ObsDistance> {

		public double dist;
		public int obsIdx;
		
		@Override
		public int compareTo(ObsDistance o) {
			if (this.dist < o.dist)
				return -1;
			else if (this.dist > o.dist)
				return 1;
			else
				return 0;
		}
	
		public ObsDistance(double dist, int obsIdx) {
			this.dist = dist;
			this.obsIdx = obsIdx;
		}
	}
	
	
	public enum AType {
		CLUTTER, EXISTING, NEWBORN
	}
	
	private class ProbTreeData extends MTBTreeNodeData {

		public double log_p;
		public int m;
		public AType c;
			
		@Override
		public ProbTreeData clone() {
			ProbTreeData nDat = new ProbTreeData();
			nDat.log_p = this.log_p;
			nDat.m = this.m;
			nDat.c = this.c;
			return nDat;
		}
	
		@Override
		public void printData() {
			System.out.println("m=" + this.m + " c="+this.c + " log_p="+this.log_p);
		}
		
	}
}
