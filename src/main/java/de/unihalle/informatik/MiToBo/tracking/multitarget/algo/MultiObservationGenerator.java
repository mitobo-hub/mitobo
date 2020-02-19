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

import java.util.Random;
import java.util.Vector;

import Jama.Matrix;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.ExecutionMode;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.Direction;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.Alida.operator.ALDOperator;
import de.unihalle.informatik.MiToBo.math.LinearTransformGaussNoise;
import de.unihalle.informatik.MiToBo.math.distributions.impl.ExponentialDistribution;
import de.unihalle.informatik.MiToBo.math.distributions.impl.PoissonDistribution;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.impl.MotionModelID;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.impl.MultiState;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.impl.MultiStateFactory;

/**
 * Operator to create a (time) series of observations which can be used for evaluation of multi target tracking algorithms.
 * @author Oliver Gress
 *
 */
@ALDAOperator(genericExecutionMode=ExecutionMode.NONE,level=Level.STANDARD)
public class MultiObservationGenerator extends ALDOperator {

	
	@Parameter(label="observations", required=false, direction=Direction.OUT,
			description="Generated observations")
	protected Vector<MultiState<MotionModelID>> observations;
	
	@Parameter(label="pDetect", required=true, direction=Direction.IN,
			description="Probability of detecting a target")
	public double pDetect;
	
	@Parameter(label="lambdaClutter", required=true, direction=Direction.IN,
			description="Mean/variance of the Poisson distribution of the number of clutter observations")
	public double lambdaClutter;
	
	@Parameter(label="lambdaBirth", required=true, direction=Direction.IN,
			description="Mean/variance of the Poisson distribution of the number of newborn targets")
	public double lambdaBirth = 0;
	
	@Parameter(label="lambdaDeath", required=true, direction=Direction.IN,
			description="Parameter of the exponential distribution of the survival of nonassociated targets")
	public double lambdaDeath;
	
	@Parameter(label="delta_t", required=false, direction=Direction.IN,
			description="time interval between two frames")
	public double delta_t;

	@Parameter(label="xMin", required=true, direction=Direction.IN,
			description="x-min of the rectangular region where the observations reside in (e.g. for image creation)")
	public double xMin;
	
	@Parameter(label="yMin", required=true, direction=Direction.IN,
			description="y-min of the rectangular region where the observations reside in (e.g. for image creation)")
	public double yMin;
	
	@Parameter(label="xMax", required=true, direction=Direction.IN,
			description="x-max of the rectangular region where the observations reside in (e.g. for image creation)")
	public double xMax;
	
	@Parameter(label="yMax", required=true, direction=Direction.IN,
			description="y-max of the rectangular region where the observations reside in (e.g. for image creation)")
	public double yMax;
	
	@Parameter(label="minSqrtSize", required=true, direction=Direction.IN,
			description="Minimum radius for newborn observations")
	public double sqrtSizeMin;
	
	@Parameter(label="maxSqrtSize", required=true, direction=Direction.IN,
			description="Maximum sqrt(size) for newborn observations")
	public double sqrtSizeMax;
	
	@Parameter(label="nTimesteps", required=true, direction=Direction.IN,
			description="Number of time steps (frames)")
	public int nTimesteps;
	
	@Parameter(label="nInitialTargets", required=true, direction=Direction.IN,
			description="Number of initial targets")
	public short nInitialTargets;
	
	@Parameter(label="modelTransition", required=true, direction=Direction.IN,
			description="A 2x2 markov matrix with probabilities of changing the dynamic models from time t-1 to t")
	public Matrix modelTransition;
	
	@Parameter(label="qxy", required=true, direction=Direction.IN,
			description="Variance of the current x-/y-position in the process noise covariance matrix")
	public double qxy;
	
	@Parameter(label="qxy_", required=true, direction=Direction.IN,
			description="Variance of the last x-/y-position in the process noise covariance matrix")
	public double qxy_;
	
	@Parameter(label="qsize", required=true, direction=Direction.IN,
			description="Variance of sqrt(size) in the process noise covariance matrix")
	public double qsize;
	
	@Parameter(label="rxy", required=true, direction=Direction.IN,
			description="Variance of the current x-/y-position in the measurement noise covariance matrix")
	public double rxy;
	
	@Parameter(label="rsize", required=true, direction=Direction.IN,
			description="Variance of sqrt(size) in the measurement noise covariance matrix")
	public double rsize;
	
	@Parameter(label="randomSeed", required=true, direction=Direction.IN,
			description="A seed for the random number generator")
	public long randomSeed;
	
	@Parameter(label="genInfo", required=false, direction=Direction.OUT,
			description="Information about the generated observations")
	public GeneratorInfo genInfo = null;
	
	protected Random rand;
		
	protected ExponentialDistribution Pdeath;
	
	public int maxTargetID;

	
	public MultiObservationGenerator() throws ALDOperatorException {
	}

	
	public Vector<MultiState<MotionModelID>> getObservations() {
		return this.observations;
	}


	@Override
	protected void operate() throws ALDOperatorException,
			ALDProcessingDAGException {

		this.genInfo = new GeneratorInfo();
		this.genInfo.numInitialTargets = this.nInitialTargets;
		this.maxTargetID = this.nInitialTargets;
		
		this.rand = new Random(this.randomSeed);
		this.observations = new Vector<MultiState<MotionModelID>>(this.nTimesteps);
		
		
		LinearTransformGaussNoise[] dynamicModels = this.createDynamicModels();
		LinearTransformGaussNoise observationModel = this.createObservationModel();
		
		MultiStateFactory<MotionModelID> obsFactory = new MultiStateFactory<MotionModelID>(3);
		
		MultiState<MotionModelID> X = this.createInitialStates();
		
		for (int t = 0; t < this.nTimesteps; t++) {
			
			X = this.generateNextStates(X, dynamicModels);
			if (t == 0) {
				double tmpclut = this.lambdaClutter;
				this.lambdaClutter = 0;
			
				this.observations.add(this.generateObservations(X, observationModel, obsFactory));

				this.lambdaClutter = tmpclut;
			}
			else
				this.observations.add(this.generateObservations(X, observationModel, obsFactory));
		}
		
		this.genInfo.obsDomainAreaRatio /= (this.nTimesteps*(this.xMax-this.xMin)*(this.yMax-this.yMin));
	}
	
	protected LinearTransformGaussNoise[] createDynamicModels() {
		
		LinearTransformGaussNoise[] dynamicModels = new LinearTransformGaussNoise[2];
			
		Matrix F = new Matrix(5,5);
		
		// random walk
		F.set(0, 0, 1); F.set(0, 1, 0); F.set(0, 2, 0); F.set(0, 3, 0);
		F.set(1, 0, 0); F.set(1, 1, 1); F.set(1, 2, 0); F.set(1, 3, 0);
		F.set(2, 0, 1); F.set(2, 1, 0); F.set(2, 2, 0); F.set(2, 3, 0);
		F.set(3, 0, 0); F.set(3, 1, 1); F.set(3, 2, 0); F.set(3, 3, 0);
		F.set(4, 4, 1);
		
		Matrix Q = new Matrix(5,5);
		Q.set(0, 0, this.qxy);
		Q.set(1, 1, this.qxy);
		Q.set(2, 2, this.qxy_);
		Q.set(3, 3, this.qxy_);
		Q.set(4, 4, this.qsize);
		
		dynamicModels[0] = new LinearTransformGaussNoise(F,Q,this.rand);

		F = new Matrix(5,5);
		
		// first order linear extrapolation
		F.set(0, 0, 2); F.set(0, 1, 0); F.set(0, 2, -1); F.set(0, 3, 0);
		F.set(1, 0, 0); F.set(1, 1, 2); F.set(1, 2, 0); F.set(1, 3, -1);
		F.set(2, 0, 1); F.set(2, 1, 0); F.set(2, 2, 0); F.set(2, 3, 0);
		F.set(3, 0, 0); F.set(3, 1, 1); F.set(3, 2, 0); F.set(3, 3, 0);
		F.set(4, 4, 1);

		dynamicModels[1] = new LinearTransformGaussNoise(F,Q,this.rand);
		
		
		return dynamicModels;
	}
	
	protected LinearTransformGaussNoise createObservationModel() {
		
		// state-to-observation-space transform matrix
		Matrix H = new Matrix(3,5);
		H.set(0, 0, 1);
		H.set(1, 1, 1);
		H.set(2, 4, 1);
		
		// measurement noise
		Matrix R = new Matrix(3,3);
		R.set(0, 0, this.rxy);
		R.set(1, 1, this.rxy);
		R.set(2, 2, this.rsize);
		
		return new LinearTransformGaussNoise(H,R,this.rand);
	}
	
	protected MultiState<MotionModelID> createInitialStates() {

		MultiStateFactory<MotionModelID> stateFactory = new MultiStateFactory<MotionModelID>(5);
		MultiState<MotionModelID> X0 = (MultiState<MotionModelID>) stateFactory.createEmptyMultiState();
		
		double pmodel0 = (this.modelTransition.get(0, 0) + this.modelTransition.get(0, 1))
					/(this.modelTransition.get(0, 0) + this.modelTransition.get(0, 1) + this.modelTransition.get(1, 0) + this.modelTransition.get(1, 1));
		
		double radius, area;
		
		for (short n = 0; n < this.nInitialTargets; n++) {
		
			Matrix x = null;
			MotionModelID mmID;
			
			do {
				byte mm = (this.rand.nextDouble() <= pmodel0) ? (byte)0 : (byte)1;
				mmID = new MotionModelID((short)(n+1), mm);
				
				x = new Matrix(5,1);
				
				radius = this.rand.nextDouble()*(this.sqrtSizeMax/Math.sqrt(Math.PI)-this.sqrtSizeMin/Math.sqrt(Math.PI)) + this.sqrtSizeMin/Math.sqrt(Math.PI);
				area = Math.PI*radius*radius;
				
				x.set(0, 0, this.rand.nextDouble()*((this.xMax-this.xMin)-2*radius) + radius + this.xMin);
				x.set(1, 0, this.rand.nextDouble()*((this.yMax-this.yMin)-2*radius) + radius + this.yMin);
				
				if (mm == 0) {
					x.set(2, 0, x.get(0, 0));
					x.set(3, 0, x.get(1, 0));
				}
				else {
					x.set(2, 0, x.get(0, 0) + (this.rand.nextDouble()*2 - 1)*this.delta_t);
					x.set(3, 0, x.get(1, 0) + (this.rand.nextDouble()*2 - 1)*this.delta_t);
				}
		
				if (Math.sqrt(area) > this.sqrtSizeMax)
					x.set(4, 0, this.sqrtSizeMax);
				else if (Math.sqrt(area) < this.sqrtSizeMin)
					x.set(4, 0, this.sqrtSizeMin);
				else
					x.set(4, 0, Math.sqrt(area));
				
			} while (this.stateConflict(x, X0));
			
			X0.insertState(x, mmID);

		}
		
		return X0;
	}
	
	
	protected MultiState<MotionModelID> generateObservations(MultiState<MotionModelID> states,
			LinearTransformGaussNoise obsModel, MultiStateFactory<MotionModelID> obsFactory) {

		MultiState<MotionModelID> Z = (MultiState<MotionModelID>) obsFactory.createEmptyMultiState();
		
		Matrix z = null;

		// target observations
		for (int n = 0; n < states.getNumberOfStates(); n++) {
			if (this.rand.nextDouble() < this.pDetect) {
			
				do {
					z = obsModel.transform(states.getStateContinuous(n));
					if (z.get(2, 0) > this.sqrtSizeMax)
						z.set(2, 0, this.sqrtSizeMax);
					else if (z.get(2, 0) < this.sqrtSizeMin)
						z.set(2, 0, this.sqrtSizeMin);

				} while (this.obsConflict(z, Z));

				this.genInfo.obsDomainAreaRatio += z.get(2, 0)*z.get(2, 0);
				
				Z.insertState(z, new MotionModelID(states.getStateDiscrete(n).ID, states.getStateDiscrete(n).mmID));
				states.getStateDiscrete(n).time = 0.0;
			}
			else {
				states.getStateDiscrete(n).time += this.delta_t;
			}
		}
		
		// clutter observations
		if (this.lambdaClutter > 0) {
			PoissonDistribution Pclutter = new PoissonDistribution(this.lambdaClutter, this.rand);
			int nClutter = Pclutter.drawSample();
			this.genInfo.numClutter += nClutter;
			
			double radius, area;
			for (int n = 0; n < nClutter; n++) {
	
				do {
					z = new Matrix(3,1);
					
					radius = this.rand.nextDouble()*(this.sqrtSizeMax/Math.sqrt(Math.PI)-this.sqrtSizeMin/Math.sqrt(Math.PI)) + this.sqrtSizeMin/Math.sqrt(Math.PI);
					area = Math.PI*radius*radius;
					
					z.set(0, 0, this.rand.nextDouble()*((this.xMax-this.xMin)-2*radius) + radius + this.xMin);
					z.set(1, 0, this.rand.nextDouble()*((this.yMax-this.yMin)-2*radius) + radius + this.yMin);
					
					if (Math.sqrt(area) > this.sqrtSizeMax)
						z.set(2, 0, this.sqrtSizeMax);
					else if (Math.sqrt(area) < this.sqrtSizeMin)
						z.set(2, 0, this.sqrtSizeMin);
					else
						z.set(2, 0, Math.sqrt(area));
					
				} while (this.obsConflict(z, Z));
				
				this.genInfo.obsDomainAreaRatio += z.get(2, 0)*z.get(2, 0);
				
				Z.insertState(z, new MotionModelID((short)0, (byte)-1));
			}
		}
		
		return Z;
	}
	
	protected MultiState<MotionModelID> generateNextStates(MultiState<MotionModelID> X, 
			LinearTransformGaussNoise[] dynamicModels) {
		
		if (Pdeath == null)
			Pdeath = new ExponentialDistribution(this.delta_t * this.lambdaDeath);
		
		MultiStateFactory<MotionModelID> stateFactory = (MultiStateFactory<MotionModelID>) X.getFactory();
		
		MultiState<MotionModelID> X_ = (MultiState<MotionModelID>) stateFactory.createEmptyMultiState();
		
		byte mmID;
		Matrix x_ = null;
		
		for (int n = 0; n < X.getNumberOfStates(); n++) {
			
			if (! (this.rand.nextDouble() <= Pdeath.P(X.getStateDiscrete(n).time))) { 
			
				do {
					mmID = (this.rand.nextDouble() < this.modelTransition.get(0, X.getStateDiscrete(n).mmID)) ? (byte)0 : (byte)1;
	
					x_ = dynamicModels[mmID].transform(X.getStateContinuous(n));
	
					if (x_.get(4, 0) > this.sqrtSizeMax)
						x_.set(4, 0, this.sqrtSizeMax);
					else if (x_.get(4, 0) < this.sqrtSizeMin)
						x_.set(4, 0, this.sqrtSizeMin);
					
				} while (this.stateConflict(x_, X_));

				X_.insertState(x_, new MotionModelID(X.getStateDiscrete(n).ID, mmID));
			}
			else {
				this.genInfo.numDeadTargets++;
			}
		}
		
		// newborn targets
		if (this.lambdaBirth > 0) {
			double pmodel0 = (this.modelTransition.get(0, 0) + this.modelTransition.get(0, 1))
					/(this.modelTransition.get(0, 0) + this.modelTransition.get(0, 1) + this.modelTransition.get(1, 0) + this.modelTransition.get(1, 1));
			
			PoissonDistribution numBirthsDistrib = new PoissonDistribution(this.lambdaBirth, this.rand);
			int numBirths = numBirthsDistrib.drawSample();
			MotionModelID mmid;
			for (short n = 0; n < numBirths; n++) {
				
				Matrix x = null;
				
				do {
					this.maxTargetID++;
					this.genInfo.numNewbornTargets++;
					
					byte mm = (this.rand.nextDouble() <= pmodel0) ? (byte)0 : (byte)1;
					mmid = new MotionModelID((short)(this.maxTargetID), mm);
					
					x = new Matrix(5,1);
					
					double radius = this.rand.nextDouble()*(this.sqrtSizeMax/Math.sqrt(Math.PI)-this.sqrtSizeMin/Math.sqrt(Math.PI)) + this.sqrtSizeMin/Math.sqrt(Math.PI);
					double area = Math.PI*radius*radius;

					
					x.set(0, 0, this.rand.nextDouble()*((this.xMax-this.xMin)-2*radius) + radius + this.xMin);
					x.set(1, 0, this.rand.nextDouble()*((this.yMax-this.yMin)-2*radius) + radius + this.yMin);
					
					if (mm == 0) {
						x.set(2, 0, x.get(0, 0));
						x.set(3, 0, x.get(1, 0));
					}
					else {
						x.set(2, 0, x.get(0, 0) + (this.rand.nextDouble()*2 - 1)*this.delta_t);
						x.set(3, 0, x.get(1, 0) + (this.rand.nextDouble()*2 - 1)*this.delta_t);
					}
			
					if (Math.sqrt(area) > this.sqrtSizeMax)
						x.set(4, 0, this.sqrtSizeMax);
					else if (Math.sqrt(area) < this.sqrtSizeMin)
						x.set(4, 0, this.sqrtSizeMin);
					else
						x.set(4, 0, Math.sqrt(area));
					
					
				} while (this.stateConflict(x, X_));
				
				X_.insertState(x, mmid);
	
			}
		}
		
		
		return X_;
	}
	
	
	protected boolean stateConflict(Matrix x, MultiState<MotionModelID> X) {
		
		double dist;
		double sqrtPI = Math.sqrt(Math.PI);
		
		for (int n = 0; n < X.getNumberOfStates(); n++) {
			dist = Math.pow(x.get(0, 0) - X.getStateContinuous(n).get(0, 0), 2)
					+ Math.pow(x.get(1, 0) - X.getStateContinuous(n).get(1, 0), 2);
			
			if (dist < x.get(4, 0)/sqrtPI + X.getStateContinuous(n).get(4, 0)/sqrtPI
					|| x.get(0, 0) < this.xMin || x.get(0, 0) > this.xMax || x.get(1, 0) < this.yMin || x.get(1, 0) > this.yMax)
				return true;
		}
		
		return false;
	}
	
	protected boolean obsConflict(Matrix z, MultiState<MotionModelID> Z) {
		
		double dist;
		double sqrtPI = Math.sqrt(Math.PI);
		
		for (int n = 0; n < Z.getNumberOfStates(); n++) {
			dist = Math.pow(z.get(0, 0) - Z.getStateContinuous(n).get(0, 0), 2)
					+ Math.pow(z.get(1, 0) - Z.getStateContinuous(n).get(1, 0), 2);
			
			if (dist < z.get(2, 0)/sqrtPI + Z.getStateContinuous(n).get(2, 0)/sqrtPI
					|| z.get(0, 0) < this.xMin || z.get(0, 0) > this.xMax || z.get(1, 0) < this.yMin || z.get(1, 0) > this.yMax)
				return true;
		}
		
		return false;
	}
	
	public class GeneratorInfo {
		public int numInitialTargets = 0;
		public int numNewbornTargets = 0;
		public int numDeadTargets = 0;
		public int numClutter = 0;
		
		public double obsDomainAreaRatio = 0;
		
		public String toString() {
			String s = "";
			
			s += "NumInitialTargets: " + this.numInitialTargets + "\n";
			s += "NumNewbornTargets: " + this.numNewbornTargets + "\n";
			s += "NumDeadTargets: " + this.numDeadTargets + "\n";
			s += "Observation-domain-area ratio: " + this.obsDomainAreaRatio;
	
			return s;
		}
	}
}