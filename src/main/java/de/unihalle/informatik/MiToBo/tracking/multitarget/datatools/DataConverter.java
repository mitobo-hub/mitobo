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

package de.unihalle.informatik.MiToBo.tracking.multitarget.datatools;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import Jama.Matrix;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.Direction;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2D;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2DSetBag;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.impl.MotionModelID;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.impl.MultiState;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.impl.MultiStateFactory;

/**
 * An operator class to convert between data types.
 * The class provides static methods for simple use. The implementations of the abstract DataConverter class
 * are located within the static methods to reduce the number of operator class files (have a look at the implemented
 * conversion methods)
 * 
 * @author Oliver Gress
 *
 * @param <S> input data type
 * @param <T> output data type
 */
public abstract class DataConverter<S,T> extends MTBOperator {

	@Parameter(label="inputData", required=true, direction=Direction.IN,
			description="Data that has to be converted")
	public S inputData = null;
	
	@Parameter(label="outputData", required=false, direction=Direction.OUT,
			description="Converted data")
	public T outputData = null;	
	
	protected DataConverter() throws ALDOperatorException {
		super();
	}

	
	/**
	 * Convert regions to observations
	 */
	public static Vector<MultiState<MotionModelID>> regionsToObservations(boolean hidden, MTBRegion2DSetBag regionsets) 
																		throws ALDOperatorException, ALDProcessingDAGException {

		DataConverter<MTBRegion2DSetBag, Vector<MultiState<MotionModelID>>> converter = 
								new DataConverter<MTBRegion2DSetBag, Vector<MultiState<MotionModelID>>>() {
			
			@Override
			protected void operate() throws ALDOperatorException,
					ALDProcessingDAGException {
				
				this.outputData = new Vector<MultiState<MotionModelID>>(this.inputData.size());
				MultiStateFactory<MotionModelID> obsFactory = new MultiStateFactory<MotionModelID>(3);
				MultiState<MotionModelID> multiObs = null;
				MTBRegion2DSet regset = null;
				MTBRegion2D reg = null;
				Matrix obs = null;
				MotionModelID mmID = null;
				
				for (int t = 0; t < this.inputData.size(); t++) {
					
					multiObs = (MultiState<MotionModelID>) obsFactory.createEmptyMultiState();
					
					regset = this.inputData.get(t);
					
					for (int m = 0; m < regset.size(); m++) {
						reg = regset.get(m);
					
						obs = new Matrix(3,1);
						obs.set(0, 0, reg.getCenterOfMass_X());
						obs.set(1, 0, reg.getCenterOfMass_Y());
						obs.set(2, 0, Math.sqrt(reg.getArea()));
						
						mmID = new MotionModelID((short)reg.getID(), (byte)-1);
						
						multiObs.insertState(obs, mmID);
					}
					
					this.outputData.add(multiObs);
				}
			}
		};

		converter.inputData = regionsets;
		converter.runOp(hidden);
		
		return converter.outputData;
	}
	
	/**
	 * Convert observations to regions.
	 */
	public static MTBRegion2DSetBag observationsToRegions(boolean hidden, Vector<MultiState<MotionModelID>> observations,
														final double xmin, final double xmax, final double ymin, final double ymax) 
																		throws ALDOperatorException, ALDProcessingDAGException {

		DataConverter<Vector<MultiState<MotionModelID>>, MTBRegion2DSetBag> converter = 
									new DataConverter<Vector<MultiState<MotionModelID>>, MTBRegion2DSetBag>() {
		
			@Override
			protected void operate() throws ALDOperatorException, ALDProcessingDAGException {	
		
				this.outputData = new MTBRegion2DSetBag(this.inputData.size());
				MultiState<MotionModelID> multiObs = null;
				MTBRegion2DSet regset = null;
				MTBRegion2D reg = null;
				
				for (int t = 0; t < this.inputData.size(); t++) {
				
					multiObs = this.inputData.get(t);
				
					regset = new MTBRegion2DSet(xmin, ymin, xmax, ymax);
				
					for (int m = 0; m < multiObs.getNumberOfStates(); m++) {
						reg = new MTBRegion2D();
				
						double r = multiObs.getStateContinuous(m).get(2, 0)/Math.sqrt(Math.PI);
						double xc = multiObs.getStateContinuous(m).get(0, 0);
						double yc = multiObs.getStateContinuous(m).get(1, 0);
		
						for (int y = (int)Math.ceil(yc-r); y < (int)Math.floor(yc+r); y++) {
							for (int x = (int)Math.ceil(xc-r); x < (int)Math.floor(xc+r); x++) {
								if ((x-xc)*(x-xc) + (y-yc)*(y-yc) <= r*r
										&& x >= xmin && x <= xmax
										&& y >= ymin && y <= ymax)
									reg.addPixel(x, y);
							}
						}
				
						reg.setID(multiObs.getStateDiscrete(m).ID);
						regset.add(reg);
					}
					
					this.outputData.add(regset);
				}
			}
		};
		
		converter.inputData = observations;
		converter.runOp(hidden);
		
		return converter.outputData;
	}
	
	/**
	 * Obtain observations assigned to each target from observations with assigned target IDs
	 * @param observations observations with target IDs assigned (ID=0 means clutter)
	 * @param clutterObs if this map is not null, all clutter observations per time step are stored here (the map is cleared before)
	 * @return a map that for each target ID holds an array which stores for each time step the observation associated the target, or -1 if no observation was associated or -2 if targets was not yet observed or after last observation
	 */
	public static HashMap<Short, int[]> observationsToTracks(Vector<MultiState<MotionModelID>> observations, HashMap<Integer, Vector<Integer>> clutterObs) {
		
		HashMap<Short, int[]> tracks = new HashMap<Short, int[]>();
		int T = observations.size();
		int M;
		short id;
		MultiState<MotionModelID> Z;
		int[] track;
		
		if (clutterObs != null) {
			clutterObs.clear();
		}
		Vector<Integer> clutter = null;
		
		for (int t = 0; t < T; t++) {
			Z = observations.get(t);
			M = Z.getNumberOfStates();
			
			clutter = clutterObs.get(t);
			
			for (int m = 0; m < M; m++) {
				id = Z.getStateDiscrete(m).ID;
				
				if (id > 0) {
					
					if (!tracks.containsKey(id)) {
						track = new int[T];
						for (int tt = 0; tt < track.length; tt++)
							track[tt] = -1;
						
						track[t] = m;
						
						tracks.put(id, track);
					}
					else {
						tracks.get(id)[t] = m;
					}
				}
				else if (clutterObs != null) {
					
					if (clutter == null) {
						clutter = new Vector<Integer>();
						clutter.add(m);
						clutterObs.put(t, clutter);
					}
					else
						clutter.add(m);
				}
			}
		}
		
		Iterator<Short> iter = tracks.keySet().iterator();
		
		while (iter.hasNext()) {
			track = tracks.get(iter.next());
			
			for (int t = 0; t < track.length && track[t] == 0; t++)
					track[t] = -2;
					
			for (int t = track.length-1; t >= track.length && track[t] == 0; t--)
				track[t] = -2;
		}
		
		
		return tracks;
	}
	
}
