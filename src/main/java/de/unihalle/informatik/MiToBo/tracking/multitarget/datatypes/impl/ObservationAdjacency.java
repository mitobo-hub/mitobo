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

package de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.impl;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Vector;

import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo;
import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo.ExportPolicy;
import de.unihalle.informatik.MiToBo.math.MathX;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.abstracts.MatchingAdjacencyMatrix;

/**
 * An adjacency matrix for observations in a time series used for greedyGourmet-partitioning. 
 * Observation of one time point form a partition. Edges are undirected.
 * 
 * @author Oliver Gress
 *
 */
@ALDMetaInfo(export=ExportPolicy.ALLOWED)
public class ObservationAdjacency extends MatchingAdjacencyMatrix {
	
	/** adjacency votes matrix: which observations are associated (same track) 
	 *  and how many particles vote for these associations 
	 */
	protected double[][] Zadj; 
	// adjacency votes matrix:
	//  Zadj[] ---------- z_1^2 ... z_M2^2 z_1^3 ... z_M3^3 .... z_1^T ... z_MT^T 
	//  Zadj[][]
	//   z_1^1
	//     .
	//     .
	//     .
	// z_M(T-1)^(T-1)
	
	
	protected int[] votesClutter;
	protected int[] votesTarget;
	
	
	/** total number of observations over all frames */
	protected int totalObservations;
	
	private int[] numObservations;
	
	/** cumulated number of previous observations for each time step,
	 *  required to determine the individual array sizes in the adjacency matrix
	 */
	private int[] cumulatedObservations;
	
	
	private PartitGraphNodeID[] nodes;
	
	/**
	 * Constructor that initializes the adjacency matrix with the given observations and
	 * sets edge weights according to the information from RBMCDA-samples
	 * @param observations vector of observations. Each entry in the vector corresponds to a time point.
	 * @param sampleinfos information from RBMCDA-samples
	 */
	public ObservationAdjacency(Vector<MultiState<MotionModelID>> observations,
			RBMCDASampleInfo<MotionModelID>[] sampleinfos) {
		
		int T = observations.size();
		
		this.totalObservations = 0;
		this.numObservations = new int[T];
		this.cumulatedObservations = new int[T];
		
		for (int t = 0; t < T; t++) {
			this.cumulatedObservations[t] = totalObservations;
			this.numObservations[t] = observations.get(t).getNumberOfStates();
			this.totalObservations += this.numObservations[t];
		}
		
		this.votesClutter = new int[totalObservations];
		this.votesTarget = new int[totalObservations];
		
		this.Zadj = new double[this.totalObservations - this.numObservations[0]][];
		
		// create nodeIDs
		this.nodes = new PartitGraphNodeID[this.totalObservations];
		
		int subgraphID = 0;
		for (int t = 0; t < T; t++) {
			for (int m = 0; m < this.numObservations[t]; m++) {
				this.nodes[subgraphID] = new PartitGraphNodeID(t,m,subgraphID);
				subgraphID++;
			}
		}
		
		// create adjacency votes matrix: 
		int idxZadj = 0;
		for (int t = 1; t < T; t++) {
			for (int m = 0; m < this.numObservations[t]; m++) {
				this.Zadj[idxZadj++] = new double[this.cumulatedObservations[t]];
			}
		}
		
		
		// normalize joint probability of the different samples using log probabilities to prevent weights=0
		double[] weights = new double[sampleinfos.length];
		double[] logp = new double[sampleinfos.length];
		double logpsum = 0.0;
		
		for (int t = 0; t < T; t++) {
			
			logpsum = 0.0;
		
			for (int i = 0; i < sampleinfos.length; i++) {
				logp[i] += sampleinfos[i].getCLogConditionalProb(t);
				logpsum = MathX.logSumP(logp[i], logpsum);

			}
			
			for (int i = 0; i < sampleinfos.length; i++) {
				logp[i] -= logpsum;
			}
		}
		
		for (int i = 0; i < sampleinfos.length; i++) {
			weights[i] = Math.exp(logp[i]);
		}
		
		
		Iterator<PartitGraphNodeID> obsIter = null;
		PartitGraphNodeID obs = null;
		
		for (int i = 0; i < sampleinfos.length; i++) {
			
			double weight = weights[i];

			// clutter observations (if any)
			if (sampleinfos[i].getTrack((short)0) != null) {
				
				obsIter = sampleinfos[i].getTrack((short)0).iterator();
				while (obsIter.hasNext()) {
					obs = obsIter.next();
					
					this.votesClutter[this.cumulatedObservations[obs.partitionID] + obs.nodeID] += weight;
				}
			}
			
			
			
			// target observations
			Iterator<Short> targetIDiter = sampleinfos[i].getEntireTargetIDs().iterator();
			while (targetIDiter.hasNext()) {
				short targetID = targetIDiter.next();
				
				if (sampleinfos[i].getTrack(targetID) != null) {
					obsIter = sampleinfos[i].getTrack(targetID).iterator();
					
					// TODO: Different strategies:
					// 1. old strategy (used by old implementation with DataAssociation array -> deprecated constructor)
					//  - connect only successive (in time) observations  #Implemented !!
					// 2. possibly new strategy !!!                       
					//  - connect all observations of one track           #Commented out !!
					// Vector<PartitGraphNodeID> previousObs = new Vector<PartitGraphNodeID>(sampleinfos[i].getTrack(targetID).size()-1);
				
					PartitGraphNodeID lastObs = null;
					
					while (obsIter.hasNext()) {
						
						obs = obsIter.next();
						
						this.votesTarget[this.cumulatedObservations[obs.partitionID] + obs.nodeID] += weight;
						
						if (lastObs != null) {

							this.Zadj[this.cumulatedObservations[obs.partitionID] - this.numObservations[0] + obs.nodeID]
							          [this.cumulatedObservations[lastObs.partitionID] + lastObs.nodeID] += weight; 
							
						}
						
						lastObs = obs;
						
//						for (int j = 0; j < previousObs.size(); j++)
//							this.Zadj[this.cumulatedObservations[obs.partitionID] - this.numObservations[0] + obs.nodeID]
//							          [this.cumulatedObservations[previousObs.get(j).partitionID] + previousObs.get(j).nodeID] += weight; 
//						
//						
//						
//						previousObs.add(obs);
					}
				}
			}
		}
	}
	
	@Deprecated
	public ObservationAdjacency(Vector<MultiState<MotionModelID>> observations,
			DataAssociationExclusive[][] associations) {
		
		int T = observations.size();
		
		this.totalObservations = 0;
		this.numObservations = new int[T];
		this.cumulatedObservations = new int[T];
		
		for (int t = 0; t < T; t++) {
			this.cumulatedObservations[t] = totalObservations;
			this.numObservations[t] = observations.get(t).getNumberOfStates();
			this.totalObservations += this.numObservations[t];
		}
		
		this.votesClutter = new int[totalObservations];
		this.votesTarget = new int[totalObservations];
		
		this.Zadj = new double[this.totalObservations - this.numObservations[0]][];
		
		// create nodeIDs
		this.nodes = new PartitGraphNodeID[this.totalObservations];
		
		int subgraphID = 0;
		for (int t = 0; t < T; t++) {
			for (int m = 0; m < this.numObservations[t]; m++) {
				this.nodes[subgraphID] = new PartitGraphNodeID(t,m,subgraphID);
				subgraphID++;
			}
		}
		
		// create adjacency votes matrix: 
		int idxZadj = 0;
		for (int t = 1; t < T; t++) {
			for (int m = 0; m < this.numObservations[t]; m++) {
				this.Zadj[idxZadj++] = new double[this.cumulatedObservations[t]];
			}
		}
		
//		for (int i = 0; i < associations.length; i++) {
//			
//			for (int t = 0; t < T; t++) {
//
//				short id;
//				
//				for (int m = 0; m < this.numObservations[t]; m++) {
//					
//					if (associations[i][t].getAssociatedTargets(m+1) != null)
//						id = (short) associations[i][t].getAssociatedTargets(m+1)[0];
//					else 
//						id = (short)0;
//
//					observations.get(t).getStateDiscrete(m).ID = id;
//				
//
//					if (id > 0) {
//						this.votesTarget[this.cumulatedObservations[t] + m] += 1;
//					}
//					else {
//						this.votesClutter[this.cumulatedObservations[t] + m] += 1;
//					}
//				}
//			}
//		}
		
		for (int i = 0; i < associations.length; i++) {
			
			for (int t = 0; t < T; t++) {

				short id;
				
				for (int m = 0; m < this.numObservations[t]; m++) {
					
				//	if (this.votesClutter[this.cumulatedObservations[t] + m]
				//	      < this.votesTarget[this.cumulatedObservations[t] + m]) {
					
						if (associations[i][t].getAssociatedTargets(m+1) != null)
							id = (short) associations[i][t].getAssociatedTargets(m+1)[0];
						else 
							id = (short)0;
	
						observations.get(t).getStateDiscrete(m).ID = id;
					
	
						if (id > 0) {
							this.votesTarget[this.cumulatedObservations[t] + m] += 1;
							
							// find last observation associated to target 'id'
							int tt = t-1;
							boolean found = false;
							
							while (!found && tt >= 0) {
								
								MultiState<MotionModelID> Zt = observations.get(tt);
								
								for (int mtt = 0; mtt < this.numObservations[tt]; mtt++) {
									
									if (Zt.getStateDiscrete(mtt).ID == id) {
										
										this.Zadj[this.cumulatedObservations[t] - this.numObservations[0] + m][this.cumulatedObservations[tt] + mtt] += 1;
										
										found = true;
										break;
									}
								}
								tt--;
							}
	
						}
						else {	
							this.votesClutter[this.cumulatedObservations[t] + m] += 1;
						}
					}
			//	}
			}
		}
	}
	
	
	public ObservationAdjacency(ObservationAdjacency oa) {
		this.Zadj = new double[oa.Zadj.length][];
		
		for (int i = 0; i < this.Zadj.length; i++)
			Arrays.copyOf(oa.Zadj[i], oa.Zadj[i].length);
		
		this.votesClutter = Arrays.copyOf(oa.votesClutter, oa.votesClutter.length);
		this.votesTarget = Arrays.copyOf(oa.votesTarget, oa.votesTarget.length);
		this.cumulatedObservations = Arrays.copyOf(oa.cumulatedObservations, oa.cumulatedObservations.length);
		this.numObservations = Arrays.copyOf(oa.numObservations, oa.numObservations.length);
		
		this.totalObservations = oa.totalObservations;
		this.nodes = Arrays.copyOf(oa.nodes, oa.nodes.length);
	}
	
	
	public int getVotesClutter(int t, int m) {
		return this.votesClutter[this.cumulatedObservations[t] + m];
	}
	
	public int getVotesTarget(int t, int m) {
		return this.votesTarget[this.cumulatedObservations[t] + m];
	}
	
	public void setVotesClutter(int t, int m, int value) {
		this.votesClutter[this.cumulatedObservations[t] + m] = value;
	}
	
	public void setVotesTarget(int t, int m, int value) {
		this.votesTarget[this.cumulatedObservations[t] + m] = value;
	}
	
	public double getVotesAdjacency(int t1, int m1, int t2, int m2) {
		
		if (t1 == t2)
			return 0;
		else {
			int tlow = t1;
			int mtlow = m1;
			int thigh = t2;
			int mthigh = m2;
			
			if (t1 > t2) {
				tlow = t2;
				mtlow = m2;
				thigh = t1;
				mthigh = m1;
			}
			
			return this.Zadj[this.cumulatedObservations[thigh] - this.numObservations[0] + mthigh][this.cumulatedObservations[tlow] + mtlow]; 
		}
	}
	
	public void setVotesAdjacency(int t1, int m1, int t2, int m2, double value) {
		
		if (t1 != t2) {
			int tlow = t1;
			int mtlow = m1;
			int thigh = t2;
			int mthigh = m2;
			
			if (t1 > t2) {
				tlow = t2;
				mtlow = m2;
				thigh = t1;
				mthigh = m1;
			}
			
			this.Zadj[this.cumulatedObservations[thigh] - this.numObservations[0] + mthigh][this.cumulatedObservations[tlow] + mtlow] = value; 
		}
		else {
			System.err.println("t1 and t2 must not be equal. Observations of one time instance cannot be associated.");
		}
	}
	
//	public int[] getMaxVotesSuccessor(int t, int m) {
//		
//		if (t >= this.cumulatedObservations.length-1) 
//			return null;
//		
//		int[] maxSuccessor = new int[2];
//		int max = 0;
//		int obsidx = this.cumulatedObservations[t] + m;
//		
//
//		//for (int i = this.cumulatedObservations[t]; i < this.Zadj.length; i++) {
//		
//		for (int tt = t+1; tt < this.numObservations.length; tt++) {	
//			for (int mm = 0; mm < this.numObservations[tt]; mm++) {
//			
//				if (this.Zadj[this.cumulatedObservations[tt] - this.numObservations[0] + mm][obsidx] > max) {
//					max = this.Zadj[this.cumulatedObservations[tt] - this.numObservations[0] + mm][obsidx];
//					maxSuccessor[0] = tt;
//					maxSuccessor[1] = mm;
//				}
//			}
//		}
//		
//		if (max > 0)
//			return maxSuccessor;
//		else
//			return null;
//		
//	}

	

	@Override
	public int numOfNodes() {
		return this.totalObservations;
	}


	@Override
	public double getWeight(PartitGraphNodeID nodeSrc, PartitGraphNodeID nodeTgt) {
		return this.getVotesAdjacency(nodeSrc.partitionID, nodeSrc.nodeID, nodeTgt.partitionID, nodeTgt.nodeID);
	}


	@Override
	public void setWeight(PartitGraphNodeID nodeSrc, PartitGraphNodeID nodeTgt, double weight) {
		this.setVotesAdjacency(nodeSrc.partitionID, nodeSrc.nodeID, nodeTgt.partitionID, nodeTgt.nodeID, weight);
	}


	@Override
	public PartitGraphNodeID[] getNodes() {
		return this.nodes;
	}


	@Override
	public boolean isDirected() {
		return false;
	}

	public double getMaxWeight() {
		double max = Double.NEGATIVE_INFINITY;
		
		for (int i = 0; i < this.Zadj.length; i++) {
			for (int j = 0; j < this.Zadj[i].length; j++) {
				if (this.Zadj[i][j] > max)
					max = this.Zadj[i][j];
			}
		}
		
		return max;
	}
	
	public double getMinWeight() {
		double min = Double.POSITIVE_INFINITY;
		
		for (int i = 0; i < this.Zadj.length; i++) {
			for (int j = 0; j < this.Zadj[i].length; j++) {
				if (this.Zadj[i][j] < min)
					min = this.Zadj[i][j];
			}
		}
		
		return min;
	}

}

