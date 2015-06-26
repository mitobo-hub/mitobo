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
import java.util.Set;
import java.util.TreeSet;

import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo;
import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo.ExportPolicy;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.interfaces.DataAssociation;

/**
 * A class that represents exlusive data association. The idea here is that a target
 * can generate at most one observation. A target thus can be associated to at most one observation.
 * This data association type is based on an arrays of association labels (target indices), one for
 * each observation and one for each target respectively. Every target and observation is associated
 * with the label 0, meaning clutter for observations and undetected for targets.
 * The <code>unsetAssociation</code>-method sets the association label of target and observation to 0.
 * @author Oliver Gress
 *
 */
@ALDMetaInfo(export=ExportPolicy.ALLOWED)
public class DataAssociationExclusive implements DataAssociation {

	protected HashMap<Integer,Integer> assocObs;
	
	/**
	 * Constructor
	 */
	public DataAssociationExclusive() {
		
		this.assocObs = new HashMap<Integer, Integer>();
		
	}

	
	@Override
	public void setAssociation(int target, int observation) {
		
		if (this.assocObs.containsValue(target) && (this.assocObs.get(observation) == null || this.assocObs.get(observation) != target)) {
			throw new IllegalArgumentException("DataAssociationExclusive.setAssociation(.): Cannot associate target " + target 
					+ " and observation " + observation + ". Target was associated before.");
		}
		else if (this.assocObs.containsKey(observation)) {
			throw new IllegalArgumentException("DataAssociationExclusive.setAssociation(.): Cannot associate target " + target 
					+ " and observation " + observation + ". Observation is already associated to target "
					+ this.assocObs.get(observation) +".");
		}
		
		this.assocObs.put(observation, target);

	}

	/**
	 * Unsets the association of the specified observation and state.
	 */
	@Override
	public void unsetAssociation(int target, int observation) {
		if (this.assocObs.get(observation) == target)
			this.assocObs.remove(observation);
		else
			System.err.println("WARNING: DataAssociationExclusive.unsetAssociation(.): Cannot unset association. Target " + target
					+ " and observation " + observation + " are not associated.");

	}

	/**
	 * Determine if target and observation are associated. If target == 0 and observation == 0, this
	 * method returns false
	 */
	@Override
	public boolean areAssociated(int target, int observation) {
		
		return (this.assocObs.containsKey(observation) && this.assocObs.get(observation) == target);

	}

	@Override
	public int numOfTargetAssocs(int observation) {
		if (this.assocObs.containsKey(observation))
			return 1;
		else
			return 0;

	}

	@Override
	public int numOfObservationAssocs(int target) {
		if (this.assocObs.containsValue(target))
			return 1;
		else
			return 0;

	}

	@Override
	public int[] getAssociatedTargets(int observation) {
		
		if (this.assocObs.containsKey(observation)) {
			int[] targets = new int[1];
			targets[0] = this.assocObs.get(observation);
			return targets;
		}
		else
			return null;

	}

	@Override
	public int[] getAssociatedObservations(int target) {
		
		if (this.assocObs.containsValue(target)) {
			int[] observations = new int[0];
			
			Set<Integer> keys = this.assocObs.keySet();
			
			for (Integer key : keys) {
				if (this.assocObs.get(key) == target) {
					observations[0] = key;
					break;
				}
			}
			
			return observations;
		}
		else
			return null;

	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof DataAssociationExclusive)) {
			return false;
		}
		else if (this == o) 
			return true;
		else {
			DataAssociationExclusive da = (DataAssociationExclusive)o;
			
			Set<Integer> keys = da.assocObs.keySet();
			
			if (keys.size() < this.assocObs.keySet().size())
				keys = this.assocObs.keySet();
			
			Integer i1, i2;
			for (Integer key : keys) {
				i1 = this.assocObs.get(key);
				i2 = da.assocObs.get(key);
			
				if (i1==null ^ i2==null)
					return false;
				
				if (i1 != i2)
					return false;
				
			}
			return true;
					
		}
	}

	
	@Override
	public String toString() {
		String s = "";
		
//		s += "#Observations: " + this.nObs + "\n";
//		s += "#Targets: " + this.nTargets + "\n";
		s += "Obs->Target: ";
		
		Set<Integer> keys = this.assocObs.keySet();
		
		for (Integer obs : keys) {
			s += (obs + "->" + this.assocObs.get(obs) + " ");
		}
		

		return s;
	}


	@Override
	public int maxAssociatedTargetID() {
		TreeSet<Integer> sortedValues = new TreeSet<Integer>(this.assocObs.values());
		return (sortedValues.size() == 0 ? -1 : sortedValues.last());
	}
}
