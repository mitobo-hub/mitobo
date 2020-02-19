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

package de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.impl;

import de.unihalle.informatik.MiToBo.tracking.multitarget.algo.MultiTargetPredictionFilterIndep;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.interfaces.Copyable;

/**
 * Representation of a RBMCDA-sample.
 * A RBMCDA-sample holds a multi-target prediction filter as well as an RBMCDASampleInfo-object, which stores
 * information like associations and association probabilities.
 * 
 * @author Oliver Gress
 *
 * @param <T> type of discrete state variables
 */
public class RBMCDASample<T extends TargetID> implements Copyable<RBMCDASample<T>>{
	
	/**
	 * The current state distribution of targets in this sample estimated by a prediction filter
	 */
	public MultiTargetPredictionFilterIndep<T> filter;

	
	/** Properties represented by this sample. Information about tracks, existing targets for
	 * any timestep, all targets that existed at any time, conditional and joint probabilities of the
	 * associations in this sample etc. */
	private RBMCDASampleInfo<T> sampleinfo;
	
	
	/**
	 * The maximum target ID that occurred in this sample up to the current time step
	 */
	public int maxTargetID;
	
	/**
	 * Constructor.
	 */
	public RBMCDASample() {
		this.filter = null;
		this.maxTargetID = -1;
		
		this.sampleinfo = new RBMCDASampleInfo<T>();
	}
	
	/** 
	 * Constructor with an initial prediction filter.
	 */
	public RBMCDASample(MultiTargetPredictionFilterIndep<T> filter) {
		this.filter = filter;
		this.maxTargetID = -1;
		
		this.sampleinfo = new RBMCDASampleInfo<T>();
	}

	@Override
	public RBMCDASample<T> copy() {
		RBMCDASample<T> cp = new RBMCDASample<T>(/*this.association, */this.filter.copy());
		cp.maxTargetID = this.maxTargetID;
		cp.sampleinfo = this.sampleinfo.copy();
		return cp;
	}
	
	/** 
	 * Get the info object of this RBMCDA-sample 
	 */
	public RBMCDASampleInfo<T> getSampleInfo() {
		return this.sampleinfo;
	}
	
}
