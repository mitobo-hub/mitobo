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

package cmdTools.tracking.multitarget;

import jargs.gnu.CmdLineParser;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;
import java.util.Random;
import java.util.Vector;

import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.formats.FormatException;

import org.apache.xmlbeans.XmlException;

import Jama.Matrix;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2DSetBag;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.io.images.ImageReaderMTB;
import de.unihalle.informatik.MiToBo.io.images.ImageWriterMTB;
import de.unihalle.informatik.MiToBo.tools.system.UserTime;
import de.unihalle.informatik.MiToBo.tracking.multitarget.algo.MultiObservationTrackerRBMCDAIMM;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatools.DataConverter;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatools.MultiStateIO;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.impl.MotionModelID;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.impl.MultiState;
import de.unihalle.informatik.MiToBo.visualization.drawing.DrawTracks2D;
import de.unihalle.informatik.MiToBo.visualization.drawing.DynamicColorLUT;

/**
 * Commandline tool for multi-target tracking with RBMCDA-framework described in [Gress,Posch, "...", VISAPP 2012]
 * 
 * @author Oliver Gress
 *
 */
public class RBMCDATracker {
	public static void main(String[] args) {

		System.out.print(" " +
				"<MiToBo>  Copyright (C) 2010  \n" +
				"This program comes with ABSOLUTELY NO WARRANTY; \n" +
				"This is free software, and you are welcome to redistribute it\n" +
				"under the terms of the GNU General Public License.\n\n\n");
		
		String thisclass = RBMCDATracker.class.toString();
		thisclass = thisclass.replaceAll("^class ", "");
		
		String usage = "USAGE: java "+ thisclass + "\n" +
					   "        [{-v,--verbose}] [{-h,--help}]\n" +
					   "        {--RandomSeed} randomSeed\n" +
					   "        {--NumSamples} numberOfRBMCDASamples\n" +
					   "        {--DeltaT} timeDeltaBetweenFrames(seconds)\n" +
					   "        {--XMin} lowerLimitInXDimension\n" +
					   "        {--XMax} upperLimitInXDimension\n" +
					   "        {--YMin} lowerLimitInYDimension\n" +
					   "        {--YMax} upperLimitInYDimension\n" +
					   "        {--PDetect} probabilityOfTargetDetection\n" +
					   "        {--LambdaBirth} lambdaOfTargetBirthPoissonDistribution\n" +
					   "        {--LambdaClutter} lambdaOfClutterPoissonDistribution\n" +
					   "        {--LambdaDeath} lambdaOfDeathExponentialDistribution\n" +
					   "        {--PModelTransRwRw} probabilityOfModelTransFromRwToRw\n" +
					   "        {--PModelTransRwFle} probabilityOfModelTransFromRwToFle\n" +
					   "        {--PModelTransFleRw} probabilityOfModelTransFromFleToRw\n" +
					   "        {--PModelTransFleFle} probabilityOfModelTransFromFleToFle\n" +
					   "        {--Rxy} varianceOfMeasurementNoiseInXandYDimension\n" +
					   "        {--Rsize} varianceOfMeasurementNoiseSqrtOfObservationSize\n" +
					   "        {--Qxy} varianceOfProcessNoiseInXandYDimension\n" +
					   "        {--QxyPrev} varianceOfProcessNoiseInXandYDimensionForPreviousState\n" +
					   "        {--Qsize} varianceOfProcessNoiseInSqrtOfObservationSize\n" +
					   "        [{--MaxNumNeighbors} maximumNumberOfNeighboringObservations]\n" +
					   "        [{--MaxDistNeighbors} maximumDistanceOfNeighboringObservations]\n" +
					   "        [{--NoNeighborsOldAlgo}]\n" +
					   "        [{--ESSPercentage} percentageOfNumberOfSampleBelowESSforResampling]\n" +
					   "        [{--DotGraphFile} dotGraphOutputFile]\n" +
					   "        [{-i,--InputImage} inputImage]\n" +
//					   "        [{-g,--GroundTruth}]\n" +
					   "        inputobservations_file output_basename"; 

	 	CmdLineParser parser = new CmdLineParser();
	
	 	// ---- program options
	 	CmdLineParser.Option verbose = parser.addBooleanOption('v', "verbose");
	 	CmdLineParser.Option help = parser.addBooleanOption('h', "help");
	 	CmdLineParser.Option inputImage = parser.addStringOption('i', "InputImage");
	 	CmdLineParser.Option groundTruth = parser.addBooleanOption('g', "GroundTruth");
	
	 	// ---- tracker parameters

	 	CmdLineParser.Option dotGraphFile = parser.addStringOption("DotGraphFile");
	 	CmdLineParser.Option numSamples = parser.addIntegerOption("NumSamples");
	 	
	 	// seed for random generator
	 	CmdLineParser.Option randomSeed = parser.addLongOption("RandomSeed");
	 	
	 	// domain specification
	 	CmdLineParser.Option deltaT = parser.addDoubleOption("DeltaT");
	 	CmdLineParser.Option xMin = parser.addDoubleOption("XMin");
	 	CmdLineParser.Option xMax = parser.addDoubleOption("XMax");
	 	CmdLineParser.Option yMin = parser.addDoubleOption("YMin");
	 	CmdLineParser.Option yMax = parser.addDoubleOption("YMax");
	 	
	 	// target properties
	 	CmdLineParser.Option pDetect = parser.addDoubleOption("PDetect");
	 	CmdLineParser.Option lambdaBirth = parser.addDoubleOption("LambdaBirth");
	 	CmdLineParser.Option lambdaClutter = parser.addDoubleOption("LambdaClutter");
	 	CmdLineParser.Option lambdaDeath = parser.addDoubleOption("LambdaDeath");

	 	// dynamic model transition probabilities
	 	CmdLineParser.Option pModelTransRwRw = parser.addDoubleOption("PModelTransRwRw");
	 	CmdLineParser.Option pModelTransRwFle = parser.addDoubleOption("PModelTransRwFle");
	 	CmdLineParser.Option pModelTransFleRw = parser.addDoubleOption("PModelTransFleRw");
	 	CmdLineParser.Option pModelTransFleFle = parser.addDoubleOption("PModelTransFleFle");

	 	// model noise variances
	 	CmdLineParser.Option rxy = parser.addDoubleOption("Rxy");
	 	CmdLineParser.Option rsize = parser.addDoubleOption("Rsize");
	 	CmdLineParser.Option qxy = parser.addDoubleOption("Qxy");
	 	CmdLineParser.Option qxyPrev = parser.addDoubleOption("QxyPrev");
	 	CmdLineParser.Option qsize = parser.addDoubleOption("Qsize");
	 	
	 	// resampling based on ESS
	 	CmdLineParser.Option essPercentage = parser.addDoubleOption("ESSPercentage");	
	 	
	 	// consider neighbor observations
	 	CmdLineParser.Option noNeighborsOldAlgo = parser.addBooleanOption("NoNeighborsOldAlgo");
	 	CmdLineParser.Option maxNumNeighbors = parser.addIntegerOption("MaxNumNeighbors");
	 	CmdLineParser.Option maxDistNeighbors = parser.addDoubleOption("MaxDistNeighbors");
	 	
        try {
        	parser.parse(args, Locale.US);
        }
        catch ( CmdLineParser.OptionException e ) {
        	System.err.println(e.getMessage());
        	System.out.println(usage);
        	System.exit(2);
        }
        
        if ((Boolean)parser.getOptionValue(help, Boolean.FALSE)) {
        	RBMCDATracker.printHelp();
        	System.exit(0);
        }
        
        String[] otherArgs = parser.getRemainingArgs();

        
        String inObservationsFile = null;
        String outBasename = null;
        
        if (otherArgs.length == 2) {
        	inObservationsFile = otherArgs[0];
        	outBasename = otherArgs[1];
        }
        else {
        	System.out.println(usage);
        	System.exit(1);
        }
        
        Boolean verboseValue = (Boolean)parser.getOptionValue(verbose, Boolean.FALSE);
        
        Boolean groundTruthValue = (Boolean)parser.getOptionValue(groundTruth, Boolean.FALSE);
        
        
        // read input observations/tracks
		Vector<MultiState<MotionModelID>> inObservations = null;
		try {
			inObservations = MultiStateIO.readMultiStates(inObservationsFile);
		} catch (XmlException e) {
		} catch (IOException e) {
		}
		
		MTBRegion2DSetBag inRegions = null;
		if (inObservations == null) {
			try {
				inRegions = new MTBRegion2DSetBag(inObservationsFile);
			} catch (XmlException e) {
			} catch (IOException e) {
			}
			
			if (inRegions != null) {
				try {
					inObservations = DataConverter.regionsToObservations(false, inRegions);
				} catch (ALDOperatorException e) {
				} catch (ALDProcessingDAGException e) {
				}
			}
			
		}

		if (inObservations == null) {
			System.err.println("Failed to read input observations.");
			System.exit(1);
		}
		
		String inImageFile = (String) parser.getOptionValue(inputImage);
		

		MultiObservationTrackerRBMCDAIMM tracker = null;
		try {
			tracker = new MultiObservationTrackerRBMCDAIMM();
		} catch (ALDOperatorException e1) {
			System.err.println("Error: " + e1.getMessage());
			e1.printStackTrace();
			System.exit(1);
		}
		
		double sqrtSizeMin = Double.POSITIVE_INFINITY;
		double sqrtSizeMax = Double.NEGATIVE_INFINITY;
		double sqrtSize;
		for (int t = 0; t < inObservations.size(); t++) {
			for (int m = 0; m < inObservations.get(t).getNumberOfStates(); m++) {
				sqrtSize = inObservations.get(t).getStateContinuous(m).get(2, 0);
				if (sqrtSize < sqrtSizeMin)
					sqrtSizeMin = sqrtSize;
				if (sqrtSize > sqrtSizeMax)
					sqrtSizeMax = sqrtSize;
			}
		}

		tracker.setInputObservations(inObservations);
		
		// interpret target IDs from input observations as groundtruth
		if (groundTruthValue)
			tracker.setGroundtruthObservations(inObservations);
		

		tracker.dotGraphFilename = (String) parser.getOptionValue(dotGraphFile);
		
		try {
			tracker.numSamples = (Integer) parser.getOptionValue(numSamples);
			
			// seed for random generator
			tracker.randomSeed = (Long) parser.getOptionValue(randomSeed);
		 	
		 	// domain specification
			tracker.delta_t = (Double) parser.getOptionValue(deltaT);
			tracker.xMin = (Double) parser.getOptionValue(xMin);
			tracker.yMin = (Double) parser.getOptionValue(yMin);
			tracker.xMax = (Double) parser.getOptionValue(xMax);
			tracker.yMax = (Double) parser.getOptionValue(yMax);
			tracker.sqrtSizeMin = sqrtSizeMin;
			tracker.sqrtSizeMax = sqrtSizeMax;
			
		 	// target properties
			tracker.pDetect = (Double) parser.getOptionValue(pDetect);
			tracker.lambdaBirth = (Double) parser.getOptionValue(lambdaBirth);
			tracker.lambdaClutter = (Double) parser.getOptionValue(lambdaClutter);
			tracker.lambdaDeath = (Double) parser.getOptionValue(lambdaDeath);
					
		 	// dynamic model transition probabilities
			tracker.modelTransition = new Matrix(2,2);
			tracker.modelTransition.set(0, 0, (Double) parser.getOptionValue(pModelTransRwRw));
			tracker.modelTransition.set(1, 0, (Double) parser.getOptionValue(pModelTransRwFle));
			tracker.modelTransition.set(0, 1, (Double) parser.getOptionValue(pModelTransFleRw));
			tracker.modelTransition.set(1, 1, (Double) parser.getOptionValue(pModelTransFleFle));
			
		 	// model noise variances
			tracker.rxy = (Double) parser.getOptionValue(rxy);
			tracker.rsize = (Double) parser.getOptionValue(rsize);
			// make sure uniform distribution of spot size is "wide" enough 
			// (variance at least as large as noise variance of observation model)
			double minRsizeUniformRange = Math.sqrt(3.0*tracker.rsize);
			if ((tracker.sqrtSizeMax - tracker.sqrtSizeMin)/2.0 < minRsizeUniformRange) {
				tracker.sqrtSizeMin = (tracker.sqrtSizeMax + tracker.sqrtSizeMin)/2.0 - minRsizeUniformRange;
				tracker.sqrtSizeMax = (tracker.sqrtSizeMax + tracker.sqrtSizeMin)/2.0 + minRsizeUniformRange;
			}
			
			tracker.qxy = (Double) parser.getOptionValue(qxy);
			tracker.qxy_ = (Double) parser.getOptionValue(qxyPrev);
			tracker.qsize = (Double) parser.getOptionValue(qsize);
			
			tracker.essPercentage = (Double) parser.getOptionValue(essPercentage, 0.5);
			
			tracker.noNeighborsOldAlgo = (Boolean) parser.getOptionValue(noNeighborsOldAlgo, false);
			tracker.maxNumNeighbors = (Integer) parser.getOptionValue(maxNumNeighbors, 0);
			tracker.maxDistNeighbors = (Double) parser.getOptionValue(maxDistNeighbors, 0.0);

		} catch (NullPointerException e) {

			System.err.println("Forgot to specify parameter? " + e.getMessage());
			System.exit(1);
		}
		
		
		try {
			tracker.setVerbose(verboseValue);
		} catch (ALDOperatorException e1) {
			
		}
		
		UserTime time = null;
		double trackingtime = -1;
		try {
			time = new UserTime();
			tracker.runOp(false);
			trackingtime = time.getElapsedTime();
		} catch (ALDOperatorException e1) {
			System.err.println("Tracking failed: " + e1.getMessage());
			System.exit(1);
		} catch (ALDProcessingDAGException e1) {
			System.err.println("Tracking failed: " + e1.getMessage());
			System.exit(1);
		}
		
		System.out.println("Tracking time: " + trackingtime + " seconds (" + time.getOperation() + ")");

		// write normalized joint association probabilities of samples 
		double[] sampleJointProbs = tracker.getSampleJointProbs();
		
		try {
			BufferedWriter w = new BufferedWriter(new FileWriter(outBasename + ".samples.probs"));
			
			for (int s = 0; s < sampleJointProbs.length; s++)
				w.write(s + "\t" + sampleJointProbs[s] + "\n");
			
			w.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		
		// write observations labeled from RBMCDA samples
		
		int digits = (int)Math.ceil(Math.log10(tracker.numSamples));
		for (int s = 0; s < tracker.numSamples; s++) {
			Vector<MultiState<MotionModelID>> labeledObs = tracker.getSampleObservations(s);
			
			String num = ""+s;
			while (num.length() < digits)
				num = "0"+num;
			
			try {
				MultiStateIO.writeMultiStates(labeledObs, outBasename + ".sample" + num + ".observations.xml");
			} catch (ALDProcessingDAGException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ALDOperatorException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			if (inRegions != null) {
				for (int t = 0; t < inRegions.size(); t++) {
					for (int m = 0; m < inRegions.get(t).size(); m++) {
						inRegions.get(t).get(m).setID(labeledObs.get(t).getStateDiscrete(m).ID);
					}
				}
				
				try {
					inRegions.write(outBasename + ".sample" + num + ".particles.xml");
				} catch (ALDProcessingDAGException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ALDOperatorException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		
		// write observations labeled with GreedyGourmetPartitioning
		Vector<MultiState<MotionModelID>> labeledObs = tracker.getOutputObservations();
		

		try {
			MultiStateIO.writeMultiStates(labeledObs, outBasename + ".gpp.observations.xml");
		} catch (ALDProcessingDAGException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ALDOperatorException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (inRegions != null) {
			for (int t = 0; t < inRegions.size(); t++) {
				for (int m = 0; m < inRegions.get(t).size(); m++) {
					inRegions.get(t).get(m).setID(labeledObs.get(t).getStateDiscrete(m).ID);
				}
			}
			
			try {
				inRegions.write(outBasename + ".gpp.particles.xml");
			} catch (ALDProcessingDAGException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ALDOperatorException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		
		
		// write image files if specified
		if (inImageFile != null) {
			try {
				ImageReaderMTB reader = new ImageReaderMTB(inImageFile);
				reader.runOp(false);

				MTBImage inImage = reader.getResultMTBImage();
				
				DynamicColorLUT colors = new DynamicColorLUT(new Random(123), 50);
				colors.setColor(0, ((0xff & 255)<<16)); // red for clutter
				
				DrawTracks2D drawer = new DrawTracks2D();
				drawer.inputImage = inImage;
				drawer.drawSpots = false;
				drawer.drawTrajectories = true;
				drawer.observations = tracker.getOutputObservations();
				drawer.trackcolors = colors;
				
				drawer.runOp(false);

				ImageWriterMTB writer = new ImageWriterMTB(drawer.getTrackImage(), outBasename + ".gpp.tracks.ome.tif");
				writer.setVerbose(verboseValue);
				writer.setOverwrite(true);
				writer.runOp(false);

				if (inRegions != null) {
					drawer.drawSpots = true;
					drawer.drawTrajectories = false;
					drawer.detectedRegions = inRegions;
					
					drawer.runOp(false);
	
					writer = new ImageWriterMTB(drawer.getTrackImage(), outBasename + ".gpp.particles.ome.tif");
					writer.setVerbose(verboseValue);
					writer.setOverwrite(true);
					writer.runOp(false);
				}
			} catch (ALDOperatorException e) {
				e.printStackTrace();
			} catch (ALDProcessingDAGException e) {
				e.printStackTrace();
			} catch (FormatException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (DependencyException e) {
				e.printStackTrace();
			} catch (ServiceException e) {
				e.printStackTrace();
			}
		}
	}
	
	private static void printHelp() {
		
		System.out.println("OVERVIEW:\n" +
				"    Generates a sequence of observations that might e.g. be used to evaluate multi-target tracking\n" +
				"    algorithms. Observations are generated using the model described in [...].");
		System.out.println("\nOPTIONS:");
		System.out.println("    -v,--verbose:\n" +
				"             Enable verbose mode\n\n" +
				"    -h,--help:\n" +
				"             Print this help. \n\n" +
				"    -d,--display:\n" +
				"             Display the generated sequence. \n\n" +
				"  Input Data:\n\n" +
				"    inputobservations_file:\n" +
				"             The observations used for tracking.\n" +
				"             The observations-file must contain data according to the MTBXMLMultiStateMMIDVector XML-schema\n" +
				"             found in MiToBo's share/xmlschemata/mtbxml directory.\n\n" +
				"  Optional Input Data:\n\n" +
				"    -i,--InputImage inputImage:\n" +
				"             Optional image to draw observations and trajectories in the context of image data.\n\n" +
				"  Mandatory Tracker-Parameters:\n\n" +
				"    --RandomSeed: randomSeed\n" +
				"             A seed (long integer) for the random number generator used.\n\n" +
				"    --NumSamples: numberOfRBMCDASamples\n" +
				"             The number of RBMCDA samples used.\n" +
				"             (Computation time scales linearily with the number of samples)\n\n" +
				"    --DeltaT timeDeltaBetweenFrames:\n" +
				"             The time between two frames (in seconds).\n\n" + 
				"    --XMin lowerLimitInXDimension:\n" +
				"             The lower limit in the x-dimension where observations may occur.\n\n" + 
				"    --XMax upperLimitInXDimension:\n" +
				"             The upper limit in the x-dimension where observations may occur.\n\n" + 
				"    --YMin lowerLimitInYDimension:\n" +
				"             The lower limit in the y-dimension where observations may occur.\n\n" + 
				"    --YMax upperLimitInYDimension:\n" +
				"             The upper limit in the y-dimension where observations may occur.\n\n" + 
				"    --PDetect probabilityOfTargetDetection:\n" +
				"             The probability to detect a target, i.e. to obtain an observation of an existing target.\n\n" + 
				"    --LambdaBirth lambdaOfNewbornTargetsPoissonDistribution:\n" +
				"             Parameter of the Poisson distribution of the number of newborn targets.\n\n" +
				"    --LambdaClutter lambdaOfClutterPoissonDistribution:\n" +
				"             Parameter of the Poisson distribution of the number of clutter observations.\n\n" +	
				"    --LambdaDeath lambdaOfDeathExponentialDistribution:\n" +
				"             Parameter of the Exponential distribution of target death.\n\n" +			
				"    --PModelTransRwRw probabilityOfModelTransFromRwToRw:\n" +
				"             Probability of switching dynamic model: Stay in RW-model.\n\n" +				
				"    --PModelTransRwFle probabilityOfModelTransFromRwToFle:\n" +
				"             Probability of switching dynamic model: Switch from RW- to FLE-model.\n\n" +			
				"    --PModelTransFleRw probabilityOfModelTransFromFleToRw:\n" +
				"             Probability of switching dynamic model: Switch from FLE- to RW-model.\n\n" +			
				"    --PModelTransFleFle probabilityOfModelTransFromFleToFle:\n" +
				"             Probability of switching dynamic model: Stay in FLE-model.\n\n" +		
				"    --Rxy varianceOfMeasurementNoiseInXandYDimension:\n" +
				"             Variance of Gaussian measurement noise in observation position (same in x- and y-dimension).\n\n" +		
				"    --Rsize varianceOfMeasurementNoiseSqrtOfObservationSize:\n" +
				"             Variance of Gaussian measurement noise in square-root of observation size.\n\n" +
				"    --Qxy varianceOfProcessNoiseInXandYDimension:\n" +
				"             Variance of Gaussian noise in target position (same in x- and y-dimension) in dynamic process.\n\n" +	
				"    --QxyPrev varianceOfProcessNoiseInXandYDimensionForPreviousState:\n" +
				"             Variance of Gaussian noise in a target's previous position in dynamic process.\n\n" +	
				"    --Qsize varianceOfProcessNoiseInSqrtOfObservationSize:\n" +
				"             Variance of Gaussian noise in square-root of target size in dynamic process.\n\n" +
				"  Optional Tracker-Parameters:\n\n" +			
				"    --MaxNumNeighbors maximumNumberOfNeighboringObservations:\n" +
				"             Maximum number of nearest-neighbor observations to be considered when the association.\n" +
				"             of an observation is sampled. Default is 0.\n\n" +	
				"    --MaxDistNeighbors maximumDistanceOfNeighboringObservations:\n" +
				"             Maximum distance of neighboring observations to be considered when the association.\n" +
				"             of an observation is sampled. Default is 0.\n\n" +	
				"    --NoNeighborsOldAlgo:\n" +
				"             Do not consider neighboring observations when the association of an observation is sampled.\n" +
				"             Use the \"old\" algorithm with slightly different results (due to numerical precision).\n\n" +
				"    --ESSPercentage percentageOfNumberOfSampleBelowESSforResampling:\n" +
				"             Percentage in range [0,1] of the number of RBMCDA samples. If Effective Sample Size drops below\n" +
				"             this percentage resampling of RBMCDA samples is triggered. Default is 0.5.\n\n" +
//				"    -g,--GroundTruth:\n" +
//				"             If this flag is specified, then the target-IDs associated to the observations in the\n" +
//				"             input-observations file are interpreted as groundtruth and used to evaluate tracking results.\n" +
//				"             Results are store.\n\n" +
				"  Output:\n\n" +
				"    output_basename:\n" +
				"             Basename for output files. Output files comprise observation-files with associations\n" +
				"             for each RBMCDA-sample as well as after greedyGourmet. Furthermore the probabilities of\n" +
				"             the individual RBMCDA-samples (joint probability of associations given all observations\n" +
				"             and images of observations and trajectories are written to file.\n\n" +			
				"  Optional Output:\n\n" +
				"    --DotGraphFile dotGraphOutputFile:\n" +
				"             Optional output of the track graph used for greedyGourmet-partitioning in the dot-language.\n\n");		
	}

}
