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

package de.unihalle.informatik.MiToBo.tracking.multitarget.eval;

import java.util.Comparator;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.Vector;

import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo;
import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo.ExportPolicy;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.Direction;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.impl.MotionModelID;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.impl.MultiState;

@ALDMetaInfo(export=ExportPolicy.ALLOWED)
public class TrackEvaluator extends MTBOperator {

	@Parameter(label="groundtruthObservations", required=true, direction=Direction.IN,
			description="Ground truth observations")
	public Vector<MultiState<MotionModelID>> inputObservations;
	
	@Parameter(label="trackerOutputObservations", required=true, direction=Direction.IN,
			description="(Set of) observations from tracking")
	public Vector<Vector<MultiState<MotionModelID>>> trackerOutputObservations;
	
	@Parameter(label="trackEvalResult", required=false, direction=Direction.OUT,
			description="Results collected over all observations in trackerObservations")
	public Vector<TrackEvaluatorResult> trackEvalResult;
	
	public TrackEvaluator(Vector<MultiState<MotionModelID>> groundtruthObservations,
			Vector<Vector<MultiState<MotionModelID>>> trackerOutputObservations) throws ALDOperatorException {

		this.inputObservations = groundtruthObservations;
		this.trackerOutputObservations = trackerOutputObservations;
		this.trackEvalResult = new Vector<TrackEvaluatorResult>(trackerOutputObservations.size());
	}

	@Override
	protected void operate() throws ALDOperatorException,
			ALDProcessingDAGException {
		
		
		TreeSet<TrackSegment> groundtruthTrackSegments = new TreeSet<TrackSegment>();
		TreeSet<TrackSegment> trackerTrackSegments = new TreeSet<TrackSegment>();
		TreeSet<TrackObservation> groundtruthTrackStarts = new TreeSet<TrackObservation>();
		TreeSet<Short> groundtruthStartedTracks = new TreeSet<Short>();		
		TreeSet<TrackObservation> trackerTrackStarts = new TreeSet<TrackObservation>();
		TreeSet<Short> trackerStartedTracks = new TreeSet<Short>();		
		TreeSet<TrackObservation> groundtruthTrackEnds = new TreeSet<TrackObservation>();
		TreeSet<TrackObservation> trackerTrackEnds = new TreeSet<TrackObservation>();

		MultiState<MotionModelID> Z, Z_;
		short ID;
		boolean found;
		
		int groundtruthNumTarget = 0;
		int groundtruthNumClutter = 0;
		
		for (int t = 0; t < this.inputObservations.size(); t++) {
			Z = this.inputObservations.get(t);
			
			for (int m = 0; m < Z.getNumberOfStates(); m++) {
				ID = Z.getStateDiscrete(m).ID;
				
				if (ID != 0) {
					groundtruthNumTarget++;
					
					found = false;
					
					if (!groundtruthStartedTracks.contains(ID)) {
						groundtruthTrackStarts.add(new TrackObservation(t,m));
						groundtruthStartedTracks.add(ID);
					}
					
					// search for next observation of the current observation's target
					for (int tt = t+1; tt < this.inputObservations.size() && !found; tt++) {
						Z_ = this.inputObservations.get(tt);
						
						for (int mm = 0; mm < Z_.getNumberOfStates() && !found; mm++) {
							
							if (Z_.getStateDiscrete(mm).ID == ID) {	
								groundtruthTrackSegments.add(new TrackSegment(t,m,tt,mm));
								found = true;
							}
						}
					}
					
					if (!found) {
						groundtruthTrackEnds.add(new TrackObservation(t,m));
					}
				}
				else {
					groundtruthNumClutter++;
				}
			}
		}
		
		Vector<MultiState<MotionModelID>> trackerObservations;
		TrackEvaluatorResult result;
		
		for (int i = 0; i < this.trackerOutputObservations.size(); i++) {
			trackerObservations = this.trackerOutputObservations.get(i);
			
			trackerTrackSegments.clear();
			trackerTrackStarts.clear();
			trackerStartedTracks.clear();
			trackerTrackEnds.clear();
			
			result = new TrackEvaluatorResult();
			result.numClutterInput = groundtruthNumClutter;
			result.numTargetInput = groundtruthNumTarget;
			
			for (int t = 0; t < trackerObservations.size(); t++) {
				Z = trackerObservations.get(t);
				
				for (int m = 0; m < Z.getNumberOfStates(); m++) {
					ID = Z.getStateDiscrete(m).ID;
					
					if (ID != 0) {				
						result.numTargetTracker++;
						
						if (this.inputObservations.get(t).getStateDiscrete(m).ID != 0)
							result.TNclutter++;
						else
							result.FNclutter++;
						
						found = false;

						if (!trackerStartedTracks.contains(ID)) {
							trackerTrackStarts.add(new TrackObservation(t,m));
							trackerStartedTracks.add(ID);
						}
						
						// search for next observation of the current observation's target
						for (int tt = t+1; tt < trackerObservations.size() && !found; tt++) {
							Z_ = trackerObservations.get(tt);
							
							for (int mm = 0; mm < Z_.getNumberOfStates() && !found; mm++) {
								
								if (Z_.getStateDiscrete(mm).ID == ID) {	
									trackerTrackSegments.add(new TrackSegment(t,m,tt,mm));
									found = true;
								}
							}
						}
						
						if (!found) {
							trackerTrackEnds.add(new TrackObservation(t,m));
						}
					}
					else {
						result.numClutterTracker++;
						
						if (this.inputObservations.get(t).getStateDiscrete(m).ID == 0)
							result.TPclutter++;
						else
							result.FPclutter++;
					}
				}
			}
			
			
			Iterator<TrackSegment> tsiter = groundtruthTrackSegments.iterator();
			while (tsiter.hasNext()) {
				if (trackerTrackSegments.remove(tsiter.next()))
					result.TPtracksegment++;
				else
					result.FNtracksegment++;
			}
			result.FPtracksegment = trackerTrackSegments.size();
			
			Iterator<TrackObservation> toiter = groundtruthTrackStarts.iterator();
			while (toiter.hasNext()) {
				if (trackerTrackStarts.remove(toiter.next()))
					result.TPtrackstart++;
				else
					result.FNtrackstart++;
			}
			result.FPtrackstart = trackerTrackStarts.size();
			
			toiter = groundtruthTrackEnds.iterator();
			while (toiter.hasNext()) {
				if (trackerTrackEnds.remove(toiter.next()))
					result.TPtrackend++;
				else
					result.FNtrackend++;
			}
			result.FPtrackend = trackerTrackEnds.size();
			
			this.trackEvalResult.add(result);
		}
		
	}
	
	private class TrackSegment implements Comparator<TrackSegment>, Comparable<TrackSegment> {
		
		private int t1, m1, t2, m2;
		
		private TrackSegment(int t1, int m1, int t2, int m2) {
			if (t1 < t2) {
				this.t1 = t1;
				this.t2 = t2;
				this.m1 = m1;
				this.m2 = m2;
			}
			else if (t2 < t1) {
				this.t1 = t2;
				this.m1 = m2;
				this.t2 = t1;
				this.m2 = m1;
			}
			else {
				throw new IllegalArgumentException("Invalid track segment. Track segments must not be defined by observations from the same time step.");
			}
		}
		
		@Override
		public String toString() {
			return "(t1=" + this.t1 + ",m1=" + this.m1 + ")--(t2=" + this.t2 + ",m2=" + this.m2 + ")";
		}
		
		@Override
		public boolean equals(Object obj) {
			TrackSegment s = (TrackSegment)obj;
			return (this.t1 == s.t1 && this.m1 == s.m1 && this.t2 == s.t2 && this.m2 == s.m2);
		}

		@Override
		public int compare(TrackSegment arg0, TrackSegment arg1) {
			if (arg0.t1 < arg1.t1)
				return -1;
			else if (arg1.t1 < arg0.t1)
				return 1;
			else {
				if (arg0.m1 < arg1.m1)
					return -1;
				else if (arg1.m1 < arg0.m1)
					return 1;
				else {
					if (arg0.t2 < arg1.t2)
						return -1;
					else if (arg1.t2 < arg0.t2)
						return 1;
					else {
						if (arg0.m2 < arg1.m2)
							return -1;
						else if (arg1.m2 < arg0.m2)
							return 1;
						else {
							return 0;
						}
					}
				}		
			}
		}

		@Override
		public int compareTo(TrackSegment arg0) {
			return this.compare(this, arg0);
		}

	}

	private class TrackObservation implements Comparator<TrackObservation>, Comparable<TrackObservation> {
		private int t,m;
		
		private TrackObservation(int t, int m) {
			this.t = t;
			this.m = m;
		}
		
		public String toString() {
			return "(t="+this.t+",m="+this.m+")";
		}
		
		@Override
		public boolean equals(Object obj) {
			TrackObservation o = (TrackObservation)obj;
			return (this.t == o.t && this.m == o.m);
		}

		@Override
		public int compareTo(TrackObservation arg0) {
			return this.compare(this, arg0);
		}

		@Override
		public int compare(TrackObservation arg0, TrackObservation arg1) {
			if (arg0.t < arg1.t)
				return -1;
			else if (arg0.t > arg1.t)
				return 1;
			else {
				if (arg0.m < arg1.m)
					return -1;
				else if (arg0.m > arg1.m)
					return 1;
				else
					return 0;
			}
		}
	}
	
	public class TrackEvaluatorResult {

		/** time that elapsed for tracking */
		public double trackingtime = 0.0;
		
		/** time that elapsed for evaluation */
		public double evaltime = 0.0;
		
		public String typeoftime = "";
		
		/** number of clutter observations in input observations */
		public int numClutterInput = 0;
		/** number of clutter observations in tracking results */
		public int numClutterTracker = 0;
		/** number of target observations in input observations */
		public int numTargetInput = 0;
		/** number of target observations in tracker results */
		public int numTargetTracker = 0;
		
		// clutter associations
		public int TPclutter = 0;
		public int FPclutter = 0;
		public int TNclutter = 0;
		public int FNclutter = 0;
		
		// observation-to-observation association
		public int TPtracksegment = 0;
		public int FNtracksegment = 0;
		public int FPtracksegment = 0;
		
		// track starts
		public int TPtrackstart = 0;
		public int FNtrackstart = 0;
		public int FPtrackstart = 0;
		
		// track ends
		public int TPtrackend = 0;
		public int FNtrackend = 0;
		public int FPtrackend= 0;	
		
		public void add(TrackEvaluatorResult r) {
			this.numClutterInput += r.numClutterInput;
			this.numClutterTracker += r.numClutterTracker;
			this.numTargetInput += r.numTargetInput;
			this.numTargetTracker += r.numTargetTracker;
			this.TPclutter += r.TPclutter;
			this.TNclutter += r.TNclutter;
			this.FPclutter += r.FPclutter;
			this.FNclutter += r.FNclutter;
			this.TPtracksegment += r.TPtracksegment;
			this.FNtracksegment += r.FNtracksegment;
			this.FPtracksegment += r.FPtracksegment;
			this.TPtrackstart += r.TPtrackstart;
			this.FNtrackstart += r.FNtrackstart;
			this.FPtrackstart += r.FPtrackstart;
			this.TPtrackend += r.TPtrackend;
			this.FNtrackend += r.FNtrackend;
			this.FPtrackend += r.FPtrackend;
		}
		
		public double getRecallClutter() {
			return (this.TPclutter/(double)(this.TPclutter + this.FNclutter));
		}
		
		public double getPrecisionClutter() {
			return (this.TPclutter/(double)(this.TPclutter + this.FPclutter));
		}
		
		public double getRecallTrackSegment() {
			return (this.TPtracksegment/(double)(this.TPtracksegment + this.FNtracksegment));
		}
		
		public double getPrecisionTrackSegment() {
			return (this.TPtracksegment/(double)(this.FPtracksegment + this.TPtracksegment));
		}
		
		public double getRecallTrackStart() {
			return (this.TPtrackstart/(double)(this.TPtrackstart + this.FNtrackstart));
		}
		
		public double getPrecisionTrackStart() {
			return (this.TPtrackstart/(double)(this.FPtrackstart + this.TPtrackstart));
		}
		
		public double getRecallTrackEnd() {
			return (this.TPtrackend/(double)(this.TPtrackend + this.FNtrackend));
		}
		
		public double getPrecisionTrackEnd() {
			return (this.TPtrackend/(double)(this.FPtrackend + this.TPtrackend));
		}
		
		
		@Override
		public String toString() {
			String s = "";

			s += "TrackingTime: " + this.trackingtime + "s\n";
			s += "EvaluationTime: " + this.evaltime + "s\n";
			s += "TypeOfTime: " + this.typeoftime + "\n\n";
			
			s += "NumTargetInput: " + this.numTargetInput + "\n";
			s += "NumClutterInput: " + this.numClutterInput + "\n";
			s += "NumTargetTracker: " + this.numTargetTracker + "\n";
			s += "NumClutterTracker: " + this.numClutterTracker + "\n\n";

			s += "TPclutter: " + this.TPclutter + "\n";
			s += "TNclutter: " + this.TNclutter + "\n";
			s += "FPclutter: " + this.FPclutter + "\n";
			s += "FNclutter: " + this.FNclutter + "\n\n";
			
			s += "TPtracksegment: " + this.TPtracksegment + "\n";
			s += "FPtracksegment: " + this.FPtracksegment + "\n";
			s += "FNtracksegment: " + this.FNtracksegment + "\n\n";
			
			s += "TPtrackstart: " + this.TPtrackstart + "\n";
			s += "FPtrackstart: " + this.FPtrackstart + "\n";
			s += "FNtrackstart: " + this.FNtrackstart + "\n\n";

			s += "TPtrackend: " + this.TPtrackend + "\n";
			s += "FPtrackend: " + this.FPtrackend + "\n";
			s += "FNtrackend: " + this.FNtrackend + "\n\n";
			
			s += "Recall clutter: " + this.getRecallClutter() + "\n";
			s += "Precision clutter: " + this.getPrecisionClutter() + "\n\n";
			s += "Recall tracksegment: " + this.getRecallTrackSegment() + "\n";
			s += "Precision tracksegment: " + this.getPrecisionTrackSegment() + "\n\n";
			s += "Recall trackstart: " + this.getRecallTrackStart() + "\n";
			s += "Precision trackstart: " + this.getPrecisionTrackStart() + "\n\n";
			s += "Recall trackend: " + this.getRecallTrackEnd() + "\n";
			s += "Precision trackend: " +  + this.getPrecisionTrackEnd();
			
			return s;
		}
	}
}
