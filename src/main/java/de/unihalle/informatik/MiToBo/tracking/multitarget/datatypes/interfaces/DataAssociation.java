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

package de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.interfaces;



/**
 * Interface for data association objects. A data association object
 * represents information, how observations and targets (states) are associated
 * in a multi target tracking scenario.
 * Targets and observations are assumed to be indexed from 1 to (number_of_states) 
 * and (number_of_observations) respectively. The index 0 is reserved for association
 * of clutter observations or undetected targets.
 * @author Oliver Gress
 *
 */
public interface DataAssociation {

	
	/**
	 * Set an association between target and observation.
	 */
	public void setAssociation(int target, int observation);
	
	/**
	 * Unset the association between target and observation.
	 */
	public void unsetAssociation(int target, int observation);
	
	/**
	 * Ask if the specified target and observation are associated
	 */
	public boolean areAssociated(int target, int observation);
	
	/**
	 * Get the number of targets that are associated with the specified observation
	 */
	public int numOfTargetAssocs(int observation);
	
	/**
	 * Get the number of observations that are associated with the specified target
	 */
	public int numOfObservationAssocs(int target);
	
	/**
	 * Get the indices of all targets that are associated with the specified observation.
	 * May be null if no states are associated to that observation.
	 */
	public int[] getAssociatedTargets(int observation);
	
	/**
	 * Get the indices of all observations that are associated with the specified state. 
	 * May be null if no observations are associated to that state.
	 */
	public int[] getAssociatedObservations(int target);
	
	/**
	 * Get the maximum target id associated by this data association object
	 */
	public int maxAssociatedTargetID();
	
}
