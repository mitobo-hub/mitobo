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

import java.awt.Color;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Random;
import java.util.Vector;

import Jama.Matrix;
import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo;
import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo.ExportPolicy;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.ExecutionMode;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.Direction;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBGraph;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBGraphNode;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;
import de.unihalle.informatik.MiToBo.io.files.GraphvizWriter;
import de.unihalle.informatik.MiToBo.math.LinearTransformGaussNoise;
import de.unihalle.informatik.MiToBo.math.MathX;
import de.unihalle.informatik.MiToBo.math.distributions.impl.ExponentialDistribution;
import de.unihalle.informatik.MiToBo.math.distributions.impl.GaussMixDistribution;
import de.unihalle.informatik.MiToBo.math.distributions.impl.GaussianDistribution;
import de.unihalle.informatik.MiToBo.math.distributions.impl.PoissonDistribution;
import de.unihalle.informatik.MiToBo.math.distributions.impl.UniformDistribution;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.impl.DataAssociationExclusive;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.impl.MotionModelID;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.impl.MultiState;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.impl.MultiStateFactory;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.impl.ObservationAdjacency;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.impl.PartitGraphNodeID;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.impl.RBMCDASample;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.impl.RBMCDASampleInfo;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.interfaces.DataAssociation;
import de.unihalle.informatik.MiToBo.tracking.multitarget.distributions.impl.AssociationDistribution;
import de.unihalle.informatik.MiToBo.tracking.multitarget.distributions.impl.AssociationDistributionNN;
import de.unihalle.informatik.MiToBo.tracking.multitarget.distributions.impl.MultiObsDistributionIndepGaussians;
import de.unihalle.informatik.MiToBo.visualization.drawing.DynamicColorLUT;

/**
 * Tracking of multiple targets using Rao-Blackwellized Monte Carlo Data Association (RBMCDA) for observation-to-target
 * association and Interacting Multiple Models (IMM) filters for target state estimation.
 * 
 * @author Oliver Gress
 *
 */
@ALDMetaInfo(export=ExportPolicy.ALLOWED)
@ALDAOperator(genericExecutionMode=ExecutionMode.NONE,level=Level.STANDARD)
public class MultiObservationTrackerRBMCDAIMM extends MTBOperator {
	
	@Parameter(label="inputObservations", required=true, direction=Direction.IN,
			description="Input observations")
	protected Vector<MultiState<MotionModelID>> inputObservations;
	
	@Parameter(label="groundtruthObservations", required=false, direction=Direction.IN,
			description="groundtruth observations")
	protected Vector<MultiState<MotionModelID>> groundtruthObservations;
	
	@Parameter(label="outputObservations", required=false, direction=Direction.OUT,
			description="Output observations (MotionModelIDs set according to tracking results)")
	protected Vector<MultiState<MotionModelID>> outputObservations;
	
	@Parameter(label="obsAssocAdjacency", required=false, direction=Direction.OUT,
			description="Adjacency matrix of observation associations from all samples")
	protected ObservationAdjacency obsAssocAdjacency;
	
	@Parameter(label="dotGraphFilename", required=false, direction=Direction.IN,
			description="File to write the observation associations graph and computed subgraphs (tracks) to")
	public String dotGraphFilename = null;
	
	@Parameter(label="pDetect", required=true, direction=Direction.IN,
			description="Probability of detecting a target")
	public double pDetect;
	
	@Parameter(label="lambdaBirth", required=true, direction=Direction.IN,
			description="Mean/variance of the Poisson distribution of the number of newborn observed targets")
	public double lambdaBirth;
	
	@Parameter(label="lambdaClutter", required=true, direction=Direction.IN,
			description="Mean/variance of the Poisson distribution of the number of clutter observations")
	public double lambdaClutter;
	
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
	
	@Parameter(label="sqrtSizeMin", required=true, direction=Direction.IN,
			description="Minimum sqrt(size) (third component of observation vector)")
	public double sqrtSizeMin;
	
	@Parameter(label="sqrtSizeMax", required=true, direction=Direction.IN,
			description="Maximum sqrt(size) (third component of observation vector)")
	public double sqrtSizeMax;

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
	
	@Parameter(label="numSamples", required=true, direction=Direction.IN,
			description="The number RBMCDA samples")
	public int numSamples = 100;
	
	@Parameter(label="ESS percentage", required=true, direction=Direction.IN,
			description="The percentage (range [0,1]) of number of samples below ESS to trigger resampling.")
	public double essPercentage = 0.5;
	
	@Parameter(label="No neighbors (old algo)", required=true, direction=Direction.IN,
			description="Do not consider any neighbor observations and compute with old algo.")
	public boolean noNeighborsOldAlgo = false;
	
	@Parameter(label="Max number of neighbors", required=true, direction=Direction.IN,
			description="The maximum number of neighboring observations considered")
	public int maxNumNeighbors = 0;
	
	@Parameter(label="Max distance of neighbors", required=true, direction=Direction.IN,
			description="The maximum number of neighboring observations considered")
	public double maxDistNeighbors = 0.0;
	
	@Parameter(label="randomSeed", required=true, direction=Direction.IN,
			description="A seed for the random number generator")
	public long randomSeed;
	
	protected Random rand;

	private double[] sampleJointProb = null;
	
//	public static int current_t = 0;
//	public static int current_i = 0;
	
	@Parameter(label="sampleInfo", required=false, direction=Direction.OUT,
			description="Info objects about the RBMCDA samples")
	private RBMCDASampleInfo<MotionModelID>[] sampleInfo = null;
	
	public DynamicColorLUT trackcolors;
	
	public MultiObservationTrackerRBMCDAIMM() throws ALDOperatorException {
		super(); 
	}

	public void setInputObservations(Vector<MultiState<MotionModelID>> inputObservations) {
		this.inputObservations = inputObservations;
	}
	
	public void setGroundtruthObservations(Vector<MultiState<MotionModelID>> groundtruthObservations) {
		this.groundtruthObservations = groundtruthObservations;
	}
	
	/**
	 * Get a copy of the input observations with IDs set corresponding to the tracking results after GreedyGourmetPartitioning
	 * of the track graph constructed from the RBMCDA samples.
	 * Be aware that observations of a track with only a single observation are interpreted as clutter.
	 */
	public Vector<MultiState<MotionModelID>> getOutputObservations() {
		return this.outputObservations;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected void operate() throws ALDOperatorException,
			ALDProcessingDAGException {

		this.rand = new Random(this.randomSeed);
		
		int sizeT = this.inputObservations.size();
		
		// create output observations (because MotionModelIDs will be changed according to tracking results)
		this.outputObservations = new Vector<MultiState<MotionModelID>>(sizeT);
		for (int t = 0; t < sizeT; t++)
			this.outputObservations.add(this.inputObservations.get(t).copy());
		
		
		
		// estimate number of targets as the median of the number of observations 
		// (only used for clutter lambda and P_detect estimation if necessary)
		// get maximum number of
		int[] nObs = new int[sizeT];
		int Mmax = -1;
		for (int t = 0; t < nObs.length; t++) {
			nObs[t] = this.inputObservations.get(t).getNumberOfStates();
			if (nObs[t] > Mmax)
				Mmax = nObs[t];
		}
		Arrays.sort(nObs);
		
		int nTargets = nObs[nObs.length/2];
		
		// estimate lambda of clutter occurrence Poisson distribution as the mean deviation from estimated number of targets when #obs > #targets
		if (this.lambdaClutter == 0) {
			for (int t = 0; t < sizeT; t++) {
				if (nObs[t] > nTargets)
					this.lambdaClutter += nObs[t] - nTargets;
	
			}
			this.lambdaClutter /= (nObs.length);
		}
		
		if (this.verbose)
			System.out.println("Estimated parameters: \n" +
				"- #Targets (median of #Obs over time): " + nTargets + "\n" +
				"- Mean/Variance of clutter occurrence (mean dev from #Targets if not specified): " + this.lambdaClutter);
		

		if (this.pDetect < 0) {
			// if not specified, estimate from number of targets and number of observations (1 - relative number of non-detections)
			
			this.pDetect = 0;
			
			for (int t = 0; t < sizeT; t++) {
				if (nObs[t] < nTargets) {
					this.pDetect += nTargets - nObs[t];
				}
			}
			this.pDetect /= (nObs.length*nTargets);
			
			
			this.pDetect = 1.0 - this.pDetect;
			System.out.println("- Prob of detection (1 - relative number of less-than-#Targets observations): " + this.pDetect);
			
		}
		

		
		// get multi observation factory
		MultiStateFactory<MotionModelID> ZFactory = (MultiStateFactory<MotionModelID>) this.inputObservations.get(0).getFactory();
		
		// multi target state factory
		MultiStateFactory<MotionModelID> XFactory = new MultiStateFactory<MotionModelID>(5);
		
		
		// ---- create multi state prior [P(X_0)] ?? P(X_1) ??----
		
		// state prior mean (from observations of first time step)
		MultiState<MotionModelID> X0 = (MultiState<MotionModelID>) XFactory.createEmptyMultiState();
		MultiState<MotionModelID> Z1 = this.outputObservations.get(0);		
		
		for (int n = 0; n < Z1.getNumberOfStates(); n++) {
			Matrix x = new Matrix(5, 1);

			Matrix z = Z1.getStateContinuous(n);
				
			x.set(0, 0, z.get(0, 0));
			x.set(1, 0, z.get(1, 0));
			x.set(2, 0, z.get(0, 0));
			x.set(3, 0, z.get(1, 0));
			x.set(4, 0, z.get(2, 0));

			X0.insertState(x, new MotionModelID((short)(n+1), (byte)-1));
		}
		
		// state prior covariance matrix (from measurement noise variances)
		Matrix P0 = new Matrix(5,5);
		P0.set(0, 0, this.rxy);
		P0.set(1, 1, this.rxy);
		P0.set(2, 2, this.rxy);
		P0.set(3, 3, this.rxy);
		P0.set(4, 4, this.rsize);
		

		// state prior for imm
		Vector<GaussMixDistribution> immPrior = new Vector<GaussMixDistribution>(X0.getNumberOfStates());
		
		for (int n = 0; n < X0.getNumberOfStates(); n++) {
			
			GaussianDistribution[] gm = new GaussianDistribution[2];
			gm[0] = new GaussianDistribution(X0.getStateContinuous(n).copy(), P0.copy(), this.rand);
			gm[1] = gm[0].copy();
			
			immPrior.add(new GaussMixDistribution(gm, this.rand));
		}
		
		// ---- Measurement model ----
		
		// state-to-observation-space transform matrix
		Matrix H = new Matrix(3,5);
		H.set(0, 0, 1);
		H.set(1, 1, 1);
		H.set(2, 4, 1);
		
		// measurement noise
		Matrix R = new Matrix(3,3);
		R.set(0, 0, this.rxy);
		R.set(1, 1, this.rxy);
		R.set(2, 2, this.rxy);
		
		// observation model
		MultiObsDistributionIndepGaussians<MotionModelID> obsModel 
			= new MultiObsDistributionIndepGaussians<MotionModelID>(this.rand, H, R, X0, XFactory, ZFactory);

		
		// ---- Association distribution model (no knowledge about the characteristics of observations / prior?) ----
		
		// limits of observation space for uniform distribution of spatial clutter as well as newborn distributions
		double[] llimits = new double[3];
		double[] ulimits = new double[3];
		llimits[0] = this.xMin; llimits[1] = this.yMin; llimits[2] = this.sqrtSizeMin;
		ulimits[0] = this.xMax; ulimits[1] = this.yMax; ulimits[2] = this.sqrtSizeMax;
		
		// data association (prior) model
		AssociationDistribution<MotionModelID, MotionModelID> da = null;
		
		if (this.noNeighborsOldAlgo) {
			da = new AssociationDistribution<MotionModelID, MotionModelID>(
						this.rand,
						this.outputObservations.get(0),
						obsModel,
						new UniformDistribution(ZFactory.getContinuousDOF(), llimits, ulimits, this.rand),
						new UniformDistribution(ZFactory.getContinuousDOF(), llimits, ulimits, this.rand),
						new PoissonDistribution(this.lambdaClutter, this.rand),
						new PoissonDistribution(this.lambdaBirth, this.rand),
						this.pDetect,
						Mmax
				);
		}
		else {
			da = new AssociationDistributionNN<MotionModelID, MotionModelID>(
					this.rand,
					this.outputObservations.get(0),
					obsModel,
					new UniformDistribution(ZFactory.getContinuousDOF(), llimits, ulimits, this.rand),
					new UniformDistribution(ZFactory.getContinuousDOF(), llimits, ulimits, this.rand),
					new PoissonDistribution(this.lambdaClutter, this.rand),
					new PoissonDistribution(this.lambdaBirth, this.rand),
					this.pDetect,
					Mmax,
					this.maxNumNeighbors,
					this.maxDistNeighbors
			);
		}
		
		
		// ---- Dynamics models ----
		// array to hold different models:
		// [0] -> Random Walk
		// [1] -> FLE
		LinearTransformGaussNoise[] dynamicModels = new LinearTransformGaussNoise[2];
		
		// random walk
		Matrix F = new Matrix(5,5);
		F.set(0, 0, 1); F.set(0, 1, 0); F.set(0, 2, 0); F.set(0, 3, 0);
		F.set(1, 0, 0); F.set(1, 1, 1); F.set(1, 2, 0); F.set(1, 3, 0);
		F.set(2, 0, 1); F.set(2, 1, 0); F.set(2, 2, 0); F.set(2, 3, 0);
		F.set(3, 0, 0); F.set(3, 1, 1); F.set(3, 2, 0); F.set(3, 3, 0);
		F.set(4, 4, 1);
		
		// process noise covariance matrix for random walk
		Matrix Q = new Matrix(5,5);
		Q.set(0, 0, this.qxy);
		Q.set(1, 1, this.qxy);
		Q.set(2, 2, this.qxy_);
		Q.set(3, 3, this.qxy_);
		Q.set(4, 4, this.qsize);
		
		dynamicModels[0] = new LinearTransformGaussNoise(F,Q,this.rand);
		
		
		// first order linear extrapolation (FLE)
		F = new Matrix(5,5);
		F.set(0, 0, 2); F.set(0, 1, 0); F.set(0, 2, -1); F.set(0, 3, 0);
		F.set(1, 0, 0); F.set(1, 1, 2); F.set(1, 2, 0); F.set(1, 3, -1);
		F.set(2, 0, 1); F.set(2, 1, 0); F.set(2, 2, 0); F.set(2, 3, 0);
		F.set(3, 0, 0); F.set(3, 1, 1); F.set(3, 2, 0); F.set(3, 3, 0);
		F.set(4, 4, 1);

		// process noise covariance matrix for FLE
		Q = new Matrix(5,5);
		Q.set(0, 0, this.qxy);
		Q.set(1, 1, this.qxy);
		Q.set(2, 2, this.qxy_);
		Q.set(3, 3, this.qxy_);
		Q.set(4, 4, this.qsize);
		
		dynamicModels[1] = new LinearTransformGaussNoise(F,Q,this.rand);

		
		// Exponential distribution for the death of targets if they do not become associated
		ExponentialDistribution deathDist = new ExponentialDistribution(this.delta_t * this.lambdaDeath);
		
		// Linear transform to obtain vectors in state-space from observations (used for birthing of new targets from observations)
		Matrix stateFromObs = new Matrix(5,3);
		stateFromObs.set(0, 0, 1);
		stateFromObs.set(1, 1, 1);
		stateFromObs.set(2, 0, 1);
		stateFromObs.set(3, 1, 1);
		stateFromObs.set(4, 2, 1);
		
		// Prototype multi-target IMM-filter used in the RBMCDA tracker
		MultiTargetIMMFilter imm
				= new MultiTargetIMMFilter(immPrior, new LinearTransformGaussNoise(H,R,this.rand), dynamicModels,
						this.modelTransition, this.delta_t, deathDist, immPrior.get(0).copy(), stateFromObs, XFactory, ZFactory);

		// prototype RBMCDA state
		RBMCDASample<MotionModelID> rbmcdainit = new RBMCDASample<MotionModelID>(imm);
		rbmcdainit.maxTargetID = Z1.getNumberOfStates()+1;

		// init RBMCDA-IMM filter
		MultiTargetRBMCDA<MotionModelID> rbmcda = new MultiTargetRBMCDA<MotionModelID>(this.rand, rbmcdainit, this.numSamples, da);
		rbmcda.setESSPercentage(this.essPercentage);
		rbmcda.ostream = System.err;
		
		DataAssociation gtAssoc = null;
		MultiState<MotionModelID> gtZ = null;
		
		// ---- RBMCDA-IMM tracking ----
		for (int t = 0; t < sizeT; t++) {
			
			if (this.getVerbose())
				System.out.println("Tracking at time " + t);
			
		//	MultiObservationTrackerRBMCDAIMM.current_t = t;
			// predict state density
			rbmcda.predict();
			
			// construct groundtruth data association if groundtruth is given
			gtAssoc = null;
			if (this.groundtruthObservations != null) {
				//System.err.println("Update at time t="+t+":");
				
				gtAssoc = new DataAssociationExclusive();
				gtZ = this.groundtruthObservations.get(t);
				
				for (int m = 0; m < gtZ.getNumberOfStates(); m++) {
					if (gtZ.getStateDiscrete(m).ID > 0)
						gtAssoc.setAssociation(gtZ.getStateDiscrete(m).ID, m+1);
				}
			}
			
			// correct the predicted state density using the observation	
			rbmcda.update(this.outputObservations.get(t), gtAssoc);
		}
	
		
		// retrieve sample info objects of all RBMCDA samples
		this.sampleInfo = (RBMCDASampleInfo<MotionModelID>[]) Array.newInstance(rbmcda.getParticle(0).getSampleInfo().getClass(), 
																				 rbmcda.getNumOfParticles());
		
		for (int i = 0; i < rbmcda.getNumOfParticles(); i++) {
			this.sampleInfo[i] = rbmcda.getParticle(i).getSampleInfo();
		}
		
		// storage for observation-to-target associations, conditional probabilities, joint probabilities
		this.sampleJointProb = new double[this.numSamples];
		
		double[] logp = new double[this.numSamples];
		double logpsum = 0.0;
		
		for (int t = 0; t < sizeT; t++) {
			
			logpsum = 0.0;
		
			for (int i = 0; i < this.numSamples; i++) {
				logp[i] += this.sampleInfo[i].getCLogConditionalProb(t);
				logpsum = MathX.logSumP(logp[i], logpsum);
			}
			
			for (int i = 0; i < this.numSamples; i++) {
				logp[i] -= logpsum;
			}
		}
		
		for (int i = 0; i < this.numSamples; i++) {
			this.sampleJointProb[i] = Math.exp(logp[i]);
		}
		
		double sumprobs = 0;
		for (int i = 0; i < this.numSamples; i++) {
			sumprobs += this.sampleJointProb[i];
		}
		for (int i = 0; i < this.numSamples; i++) {
			this.sampleJointProb[i] /= sumprobs;
		}

		double essInv = 0.0;
		
		for (int i = 0; i < this.sampleJointProb.length; i++) {
			essInv += this.sampleJointProb[i]*this.sampleJointProb[i];
		//	System.err.println(this.sampleJointProb[i] + "///");
		}
		double ess = 1.0/essInv;
		
		double[] sortedJointProb = Arrays.copyOf(this.sampleJointProb, this.sampleJointProb.length);
		Arrays.sort(sortedJointProb);
		
		double thresh = sumprobs;
		thresh -= sortedJointProb[sortedJointProb.length-1];
		for (int i = 2; i <= ess-1 && i <= sortedJointProb.length; i++) {
			thresh -= sortedJointProb[sortedJointProb.length-i];
		}

		// create graph for observation-to-observation (over time) associations
		// edge weights equal the weighted votes for an association by the RBMCDA samples
		this.obsAssocAdjacency = new ObservationAdjacency(this.outputObservations, this.sampleInfo);
		
		PartitGraphNodeID[] onodes = this.obsAssocAdjacency.getNodes();
		
		
		int wcnt = 0;
		int zcnt = 0;
		for (int k = 0; k < onodes.length; k++) {
			for (int l = k+1; l < onodes.length; l++) {
				if (onodes[k].partitionID != onodes[l].partitionID) {
					double w = this.obsAssocAdjacency.getWeight(onodes[k], onodes[l]);
					
					if (w > 0) {
						wcnt++;
						
						if (w < thresh){
							zcnt++;
							this.obsAssocAdjacency.setWeight(onodes[k], onodes[l], 0.0);
						}
					}
				}
			}
		}
		
		System.out.println("Effective sample size: " + ess + " => thresh=" + thresh);//factor+"*"+thresh+"="+threshfactor*thresh);
		System.out.println(zcnt + " out of " +wcnt+ " nonzero weights set to zero");// (maxweight=" +maxweight+
				//" threshold=threshfactor*maxweight="+threshfactor*maxweight);
		
		// find subgraphs, i.e. tracks, with maximum one observation for each time step
		GreedyGourmetPartitioning ggp = new GreedyGourmetPartitioning(this.obsAssocAdjacency, true, 0.0);
		Vector<MTBGraph> trackgraphs = ggp.computeSubgraphs();
		
		// write graph and subgraphs to dot-graph-file if specified
		if (this.trackcolors == null) {
			this.trackcolors = new DynamicColorLUT(this.rand);
			this.trackcolors.setColor(0, (255 << 16)); // clutter color (red)
		}
		
		GraphvizWriter<PartitGraphNodeID> gvwriter = null;
		if (this.dotGraphFilename != null) {
			gvwriter = new GraphvizWriter<PartitGraphNodeID>(this.obsAssocAdjacency, this.dotGraphFilename);
			gvwriter.setWeightAsEdgeThickness(true);
		}
		
		// set trackIDs in output observations
		for (int t = 0; t < this.outputObservations.size(); t++) {
			MultiState<MotionModelID> obs = this.outputObservations.get(t);
			
			for (int m = 0; m < obs.getNumberOfStates(); m++) {
				obs.getStateDiscrete(m).ID = (short)0;
			}	
		}

		for (int i = 0; i < trackgraphs.size(); i++) {
			Vector<MTBGraphNode<?>> nodes = trackgraphs.get(i).getNodes();
			
			if (nodes.size() > 1) {
				for (int j = 0; j < nodes.size(); j++) {
					PartitGraphNodeID node = (PartitGraphNodeID) nodes.get(j).getData();
					
					this.outputObservations.get(node.partitionID).getStateDiscrete(node.nodeID).ID = (short)(i + 1);
				}
				
				if (gvwriter != null)
					gvwriter.addSubgraph(trackgraphs.get(i), new Color(this.trackcolors.getColor(i+1)));
			}
			else if (nodes.size() == 1) {
				// interpret as clutter
				
				PartitGraphNodeID node = (PartitGraphNodeID) nodes.get(0).getData();
				//if (this.obsAssocAdjacency.getVotesClutter(node.partitionID, node.nodeID)
				//		>= this.obsAssocAdjacency.getVotesTarget(node.partitionID, node.nodeID)) {
					this.outputObservations.get(node.partitionID).getStateDiscrete(node.nodeID).ID = (short)0;
					
					
					if (gvwriter != null)
						gvwriter.addSubgraph(trackgraphs.get(i), new Color(this.trackcolors.getColor(0)));
//				}
//				else {
//					this.outputObservations.get(node.partitionID).getStateDiscrete(node.nodeID).ID = (short)(i + 1);
//					
//					if (gvwriter != null)
//						gvwriter.addSubgraph(trackgraphs.get(i), new Color(this.trackcolors.getColor(i+1)));
//				}
				
			}
		}
		
		if (gvwriter != null) {
			gvwriter.setFilename(this.dotGraphFilename);
			try {
				gvwriter.runOp(false);
			} catch (ALDProcessingDAGException e) {
				System.err.println("Warning: Failed to write dot-file: " + e.getMessage());
			}
		}

	}
	
	/**
	 * Get the probability of the associations of <code>i</code>-th sample at the time <code>t</code> conditional on previous associations and all
	 * observations up to time <code>t</code>.
	 */
	public double getSampleConditionalProb(int i, int t) {
		return this.sampleInfo[i].getCConditionalProb(t);
	}

	/**
	 * Get the joint probability of all associations of the <code>i</code>-th sample conditional on all observations,
	 * normalized by all samples' probabilities.
	 */
	public double getSampleJointProb(int i) {
		return this.sampleJointProb[i];
	}
	
	/**
	 * Get the joint probability of all associations of all samples conditional on all observations,
	 * normalized by all samples' probabilities.
	 */
	public double[] getSampleJointProbs() {
		return this.sampleJointProb;
	}
	
	/**
	 * Get a copy of the observations with IDs set corresponding to the i-th sample's tracking results.
	 * Be aware that observations of a track with only a single observation are interpreted as clutter in contrast
	 * to the sample info objects returned by {@link getSampleInfo}.
	 */
	public Vector<MultiState<MotionModelID>> getSampleObservations(int i) {
		Vector<MultiState<MotionModelID>> particleObs = new Vector<MultiState<MotionModelID>>(this.outputObservations.size());
		MultiState<MotionModelID> Z = null;
		
		// copy observations
		for (int t = 0; t < this.outputObservations.size(); t++) {
			Z = this.outputObservations.get(t).copy();
			
			particleObs.add(Z);
		}
		Iterator<PartitGraphNodeID> obsIter = null;
		PartitGraphNodeID obs = null;
		
		// clutter observations (if any)
		if (this.sampleInfo[i].getTrack((short)0) != null) {
			
			obsIter = this.sampleInfo[i].getTrack((short)0).iterator();

			while (obsIter.hasNext()) {
				obs = obsIter.next();
				
				particleObs.get(obs.partitionID).getStateDiscrete(obs.nodeID).ID = (short)0;
			}
		}
		
		// target observations
		Iterator<Short> targetIDiter = this.sampleInfo[i].getEntireTargetIDs().iterator();
		while (targetIDiter.hasNext()) {
			short targetID = targetIDiter.next();
			
			if (this.sampleInfo[i].getTrack(targetID) != null) {
				obsIter = this.sampleInfo[i].getTrack(targetID).iterator();

				// tracks with a single observation are interpreted as clutter
				if (this.sampleInfo[i].getTrack(targetID).size() == 1)
					targetID = 0;
				
				
				while (obsIter.hasNext()) {
					
					obs = obsIter.next();
					
					particleObs.get(obs.partitionID).getStateDiscrete(obs.nodeID).ID = targetID;
				}
			}
		
		}
		
		return particleObs;
	}
	
	/**
	 * Get the sample info object of the i-th sample. Here tracks with single observations are available in contrast
	 * to sample observations returned by {@link getSampleObservations}.
	 */
	public RBMCDASampleInfo<MotionModelID> getSampleInfo(int i) {
		return (this.sampleInfo == null) ? null : this.sampleInfo[i];
	}

}
