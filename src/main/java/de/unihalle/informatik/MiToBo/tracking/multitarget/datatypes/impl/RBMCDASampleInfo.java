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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;

import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.abstracts.AbstractMultiState;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.interfaces.Copyable;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.interfaces.DataAssociation;

/**
 * RBMCDA-sample info object. Holds various information about an RBMCDA-sample.
 * 
 * 
 * @author Oliver Gress
 *
 * @param <T> type of discrete state variables
 */
public class RBMCDASampleInfo<T extends TargetID> implements Copyable<RBMCDASampleInfo<T>> {

	/** The probability of the association set of any time step represented by this sample
	 *  conditional on all previous associations in this sample and all observations up to
	 *  the specific time step:  P(C^t | C^{1:t-1}, Z^{1,t}) for all t
	 */
	private Vector<Double> logP_Ccond;
	
	/** The joint probability of the association sets of all time steps represented by this sample
	 *  conditional on all observations:  P(C^{1-t} | Z^{1,t})
	 */
	private double logP_Cjoint;
	
	/** The targetIDs of the existing target at the specific time */
	private Vector<SortedSet<Short>> existingTargetIDs;
	
	/** The targetIDs of all targets that have ever existed up to current time */
	private SortedSet<Short> entireTargetIDs;
	
	/** HashMap of the tracks represented by the sample. Keys correspond to targetIDs while the key 0
	 * corresponds to observations to clutter. The values are sets of PartitGraphNodeIDs which identify the
	 * the associated observations. The partition of a PartitGraphNodeID is the time index, the nodeID is the index
	 * of the observation in that time step. The subgraphID is set to the targetID or 0 in case of clutter.
	 */
	private HashMap<Short, SortedSet<PartitGraphNodeID>> tracks;
	
	
	private Vector<AbstractMultiState<T>> observations;
	
	
	public RBMCDASampleInfo() {
		this.logP_Ccond = new Vector<Double>();
		this.logP_Cjoint = 0;
		this.existingTargetIDs = new Vector<SortedSet<Short>>();
		this.entireTargetIDs = new TreeSet<Short>();
		this.tracks = new HashMap<Short, SortedSet<PartitGraphNodeID>>();
		this.observations = new Vector<AbstractMultiState<T>>();
	}

	public RBMCDASampleInfo(int capacity) {
		this.logP_Ccond = new Vector<Double>(capacity);
		this.logP_Cjoint = 0;
		this.existingTargetIDs = new Vector<SortedSet<Short>>(capacity);
		this.entireTargetIDs = new TreeSet<Short>();
		this.tracks = new HashMap<Short, SortedSet<PartitGraphNodeID>>();
		this.observations = new Vector<AbstractMultiState<T>>(capacity);
	}

	@SuppressWarnings("unchecked")
	public RBMCDASampleInfo(RBMCDASampleInfo<T> sampleinfo) {
		this.logP_Ccond = (Vector<Double>) sampleinfo.logP_Ccond.clone();
		this.logP_Cjoint = sampleinfo.logP_Cjoint;
		this.existingTargetIDs = (Vector<SortedSet<Short>>) sampleinfo.existingTargetIDs.clone();
		this.entireTargetIDs = new TreeSet<Short>(sampleinfo.entireTargetIDs);
		
		this.tracks = new HashMap<Short, SortedSet<PartitGraphNodeID>>(sampleinfo.tracks.size());
		Iterator<Short> keyIt = sampleinfo.tracks.keySet().iterator();
		short key;
		while (keyIt.hasNext()) {
			key = keyIt.next();
			this.tracks.put(key, new TreeSet<PartitGraphNodeID>((SortedSet<PartitGraphNodeID>) sampleinfo.tracks.get(key)));
		}
		
		this.observations = (Vector<AbstractMultiState<T>>) sampleinfo.observations.clone();
	}
	

	@Override
	public RBMCDASampleInfo<T> copy() {
		return new RBMCDASampleInfo<T>(this);
	}
	
	/**
	 * Add sample info of the current time step
	 * @param logP_C probability of the data association at current time step conditional on the previous associations as well as current and previous observations
	 * @param C associations of the current time step
	 * @param numObservations number of observations at the current time step
	 * @param existingTargetIDs targetIDs of currently existing targets
	 * @return the time index that was assigned for the added information (starting from 0)
	 */
	public int addCurrentInfo(double logP_C, DataAssociation C, 
					AbstractMultiState<T> observations, Set<Short> existingTargetIDs) {
		
		this.logP_Ccond.add(logP_C);
		this.logP_Cjoint += logP_C;
		
		this.existingTargetIDs.add(new TreeSet<Short>(existingTargetIDs));
		this.entireTargetIDs.addAll(existingTargetIDs);
		
		this.observations.add(observations);
		
		int t = this.logP_Ccond.size()-1;
		
		int[] targets;
		short id;
		SortedSet<PartitGraphNodeID> track = null;
		
		for (int m = 1; m <= observations.getNumberOfStates(); m++) {
			targets = C.getAssociatedTargets(m);
			
			if (targets == null)
				id = 0;
			else 
				id = (short) targets[0];
			
			track = this.tracks.get(id);
			
			if (track == null) {
				track = new TreeSet<PartitGraphNodeID>();
				track.add(new PartitGraphNodeID(t, m-1, id));
				this.tracks.put(id, track);
			}
			else {
				track.add(new PartitGraphNodeID(t, m-1, id));
			}
		}
		
		return t;
	}
	
	/**
	 * Get the probability of the associations at time t conditional on the previous associations and all observations up to time t.
	 * This method might throw an <code>ArrayIndexOutOfBounds</code>-Exception if <code>t</code> is larger than 'current time' (see {@link getCurrentTime}). 
	 */
	public double getCConditionalProb(int t) {
		return Math.exp(this.logP_Ccond.get(t));
	}
	
	/**
	 * Get the log of probability of the associations at time t conditional on the previous associations and all observations up to time t.
	 * This method might throw an <code>ArrayIndexOutOfBounds</code>-Exception if <code>t</code> is larger than 'current time' (see {@link getCurrentTime}). 
	 */
	public double getCLogConditionalProb(int t) {
		return this.logP_Ccond.get(t);
	}
	
	/**
	 * Get the joint probability of all associations up to 'current time' (see {@link getCurrentTime}) conditional on all observations up to that time.
	 */
	public double getCJointProb() {
		return Math.exp(this.logP_Cjoint);
	}
	
	/**
	 * Get the natural log of joint probability of all associations up to 'current time' (see {@link getCurrentTime}) conditional on all observations up to that time.
	 */
	public double getCLogJointProb() {
		return this.logP_Cjoint;
	}
	
	/**
	 * Get the current time index, i.e. the time index that was assigned to the latest added information
	 */
	public int getCurrentTime() {
		return this.logP_Ccond.size()-1;
	}
	
	/**
	 * Get the targetIDs of all targets existing at time t.
	 * This method might throw an <code>ArrayIndexOutOfBounds</code>-Exception if <code>t</code> is larger than 'current time' (see {@link getCurrentTime}). 
	 */
	public SortedSet<Short> getExistingTargetIDs(int t) {
		return this.existingTargetIDs.get(t);
	}
	
	/**
	 * Get the targetIDs of all targets that ever existed up to current time.
	 * This method might throw an <code>ArrayIndexOutOfBounds</code>-Exception if <code>t</code> is larger than 'current time' (see {@link getCurrentTime}). 
	 */
	public SortedSet<Short> getEntireTargetIDs() {
		return this.entireTargetIDs;
	}
	
	/**
	 * Get track of target given by <code>targetID</code> up to current time, where targetID may be 0 to obtain the clutter observations (which do not form a
	 * track). A track is comprised of all observations associated to the specified target. The PartitGraphNodeID reflects only the time (partition) and index
	 * (nodeID) the observation.
	 */
	public SortedSet<PartitGraphNodeID> getTrack(short targetID) {
		return this.tracks.get(targetID);
	}
	
	/**
	 * Get the observations of time t.
	 * This method might throw an <code>ArrayIndexOutOfBounds</code>-Exception if <code>t</code> is larger than 'current time' (see {@link getCurrentTime}).
	 */
	public AbstractMultiState<T> getObservations(int t) {
		return this.observations.get(t);
	}
}
