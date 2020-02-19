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

import ij.ImagePlus;
import jargs.gnu.CmdLineParser;

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;

import Jama.Matrix;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.io.images.ImageWriterMTB;
import de.unihalle.informatik.MiToBo.tracking.multitarget.algo.MultiObservationGenerator;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatools.MultiStateIO;
import de.unihalle.informatik.MiToBo.visualization.drawing.DrawTracks2D;

/**
 * Commandline tool to create a (time) series of observations which can be used 
 * for evaluation of multi-target tracking algorithms.
 * 
 * @author Oliver Gress
 *
 */
public class ObservationSeriesGenerator {

	public static void main(String[] args) {
		
		System.out.print(" " +
				"<MiToBo>  Copyright (C) 2010  \n" +
				"This program comes with ABSOLUTELY NO WARRANTY; \n" +
				"This is free software, and you are welcome to redistribute it\n" +
				"under the terms of the GNU General Public License.\n\n\n");
		
		String thisclass = ObservationSeriesGenerator.class.toString();
		thisclass = thisclass.replaceAll("^class ", "");
		
		String usage = "USAGE: java "+ thisclass + "\n" +
					   "        [{-v,--verbose}] [{-h,--help}] [{-d,--display}]\n" +
					   "        {--RandomSeed} randomSeed\n" +
					   "        {--NumFrames} numberOfFrames\n" +
					   "        {--DeltaT} timeDeltaBetweenFrames(seconds)\n" +
					   "        {--XMin} lowerLimitInXDimension\n" +
					   "        {--XMax} upperLimitInXDimension\n" +
					   "        {--YMin} lowerLimitInYDimension\n" +
					   "        {--YMax} upperLimitInYDimension\n" +
					   "        {--SqrtSizeMin} lowerLimitOfSqrtOfObservationSize\n" +
					   "        {--SqrtSizeMax} upperLimitInSqrtOfObservationSize\n" +
					   "        {--NumInitialTargets} numberOfTargetsInFirstFrame\n" +
					   "        {--PDetect} probabilityOfTargetDetection\n" +
					   "        {--LambdaBirth} lambdaOfNewbornTargetsPoissonDistribution\n" +
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
					   "        observations_file observations_info_file [observations_image_file]"; 

	 	CmdLineParser parser = new CmdLineParser();
	
	 	// ---- program options
	 	CmdLineParser.Option verbose = parser.addBooleanOption('v', "verbose");
	 	CmdLineParser.Option display = parser.addBooleanOption('d', "display");
	 	CmdLineParser.Option help = parser.addBooleanOption('h', "help");
	
	 	// ---- generator parameters
	 	// seed for random generator
	 	CmdLineParser.Option randomSeed = parser.addLongOption("RandomSeed");
	 	
	 	// domain specification
	 	CmdLineParser.Option numFrames = parser.addIntegerOption("NumFrames");
	 	CmdLineParser.Option deltaT = parser.addDoubleOption("DeltaT");
	 	CmdLineParser.Option xMin = parser.addDoubleOption("XMin");
	 	CmdLineParser.Option xMax = parser.addDoubleOption("XMax");
	 	CmdLineParser.Option yMin = parser.addDoubleOption("YMin");
	 	CmdLineParser.Option yMax = parser.addDoubleOption("YMax");
	 	CmdLineParser.Option sqrtSizeMin = parser.addDoubleOption("SqrtSizeMin");
	 	CmdLineParser.Option sqrtSizeMax = parser.addDoubleOption("SqrtSizeMax");
	 	
	 	// target properties
	 	CmdLineParser.Option numInitialTargets = parser.addIntegerOption("NumInitialTargets");
	 	CmdLineParser.Option pDetect = parser.addDoubleOption("PDetect");
	 	CmdLineParser.Option lambdaClutter = parser.addDoubleOption("LambdaClutter");
	 	CmdLineParser.Option lambdaBirth = parser.addDoubleOption("LambdaBirth");
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
	 	
        try {
        	parser.parse(args, Locale.US);
        }
        catch ( CmdLineParser.OptionException e ) {
        	System.err.println(e.getMessage());
        	System.out.println(usage);
        	System.exit(2);
        }
        
        if ((Boolean)parser.getOptionValue(help, Boolean.FALSE)) {
        	ObservationSeriesGenerator.printHelp();
        	System.exit(0);
        }
        
        String[] otherArgs = parser.getRemainingArgs();

        
        String outObservations = null;
        String outImage = null;
        String outObsInfo = null;
        
        if (otherArgs.length == 2) {
        	outObservations = otherArgs[0];
        	outObsInfo = otherArgs[1];
        }
        else if (otherArgs.length == 3) {
        	outObservations = otherArgs[0];
        	outObsInfo = otherArgs[1];
        	outImage = otherArgs[2];
        }
        else {
        	System.out.println(usage);
        	System.exit(1);
        }
        
        Boolean verboseValue = (Boolean)parser.getOptionValue(verbose, Boolean.FALSE);
 
        
		try {
			MultiObservationGenerator gen = new MultiObservationGenerator();
			
			gen.setVerbose(verboseValue);
			
			// seed for random generator
		 	gen.randomSeed = (Long) parser.getOptionValue(randomSeed);
		 	
		 	// domain specification
			gen.nTimesteps = (Integer) parser.getOptionValue(numFrames);
			gen.delta_t = (Double) parser.getOptionValue(deltaT);
			gen.xMin = (Double) parser.getOptionValue(xMin);
			gen.yMin = (Double) parser.getOptionValue(yMin);
			gen.xMax = (Double) parser.getOptionValue(xMax);
			gen.yMax = (Double) parser.getOptionValue(yMax);
			gen.sqrtSizeMin = (Double) parser.getOptionValue(sqrtSizeMin);
			gen.sqrtSizeMax = (Double) parser.getOptionValue(sqrtSizeMax);
			
		 	// target properties
			gen.nInitialTargets = ((Integer) parser.getOptionValue(numInitialTargets)).shortValue();
			gen.pDetect = (Double) parser.getOptionValue(pDetect);
			gen.lambdaClutter = (Double) parser.getOptionValue(lambdaClutter);
			gen.lambdaBirth = (Double) parser.getOptionValue(lambdaBirth);
			gen.lambdaDeath = (Double) parser.getOptionValue(lambdaDeath);
					
		 	// dynamic model transition probabilities
			gen.modelTransition = new Matrix(2,2);
			gen.modelTransition.set(0, 0, (Double) parser.getOptionValue(pModelTransRwRw));
			gen.modelTransition.set(1, 0, (Double) parser.getOptionValue(pModelTransRwFle));
			gen.modelTransition.set(0, 1, (Double) parser.getOptionValue(pModelTransFleRw));
			gen.modelTransition.set(1, 1, (Double) parser.getOptionValue(pModelTransFleFle));

		 	// model noise variances
			gen.rxy = (Double) parser.getOptionValue(rxy);
			gen.rsize = (Double) parser.getOptionValue(rsize);
			gen.qxy = (Double) parser.getOptionValue(qxy);
			gen.qxy_ = (Double) parser.getOptionValue(qxyPrev);
			gen.qsize = (Double) parser.getOptionValue(qsize);
			
			
			// run generator
			gen.runOp(false);
			
			// write generated observations to file
			MultiStateIO.writeMultiStates(gen.getObservations(), outObservations);
			
			
			//write to file
			BufferedWriter w = null;
			try {
				w = new BufferedWriter(new FileWriter(new File(outObsInfo)));
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			if (w != null) {
				try {
					w.write("# Information about generated observations:\n");
					w.write(gen.genInfo.toString() + "\n");

					w.flush();
					w.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			// if image filename was given, write observation track image to file
			if (outImage != null) {
				MTBImage obsImg = MTBImage.createMTBImage((int)(gen.xMax - gen.xMin +1), (int)(gen.yMax - gen.yMin + 1), 1, gen.nTimesteps, 1, MTBImageType.MTB_BYTE);
				
				DrawTracks2D draw = new DrawTracks2D();
				draw.setVerbose(verboseValue);
				
				draw.observations = gen.getObservations();
				draw.inputImage = obsImg;
				draw.drawTrajectories = true;
				
				draw.runOp(false);
				
				ImageWriterMTB imgwriter = new ImageWriterMTB(draw.getTrackImage(), outImage);
				imgwriter.setVerbose(verboseValue);
				imgwriter.setOverwrite(true);
				imgwriter.runOp(false);
				
				// display image if the option was given
				if ((Boolean)parser.getOptionValue(display, Boolean.FALSE)) {
					
					final ImagePlus resultImp = draw.getTrackImage().getImagePlus();

					resultImp.show();
					
					resultImp.getWindow().addWindowListener(new WindowListener() {
						
						@Override
						public void windowOpened(WindowEvent arg0) {
							// nothing to do
						}
						
						@Override
						public void windowIconified(WindowEvent arg0) {
							// nothing to do
						}
						
						@Override
						public void windowDeiconified(WindowEvent arg0) {
							// nothing to do
						}
						
						@Override
						public void windowDeactivated(WindowEvent arg0) {
							// nothing to do
						}
						
						@Override
						public void windowClosing(WindowEvent arg0) {
							// nothing to do
						}
						
						@Override
						public void windowClosed(WindowEvent arg0) {
							// quit program when display window is closed
							System.exit(0);
						}
						
						@Override
						public void windowActivated(WindowEvent arg0) {
							// nothing to do
						}
					});
				}
			}
		} catch (ALDOperatorException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (ALDProcessingDAGException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (NullPointerException e) {
		//	e.printStackTrace();
			System.err.println("Required parameter not specified! Terminating program. " + e.getMessage());
			System.exit(1);
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
				"  Observations-Generator-Parameters:\n\n" +
				"    --RandomSeed: randomSeed\n" +
				"             A seed (long integer) for the random number generator used.\n\n" +
				"    --NumFrames numberOfFrames:\n" +
				"             The number of time frames for which data is to be generated.\n\n" +
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
				"    --SqrtSizeMin lowerLimitOfSqrtOfObservationSize:\n" +
				"             The lower limit of the square-root of the size(=area) of observations.\n\n" + 
				"    --SqrtSizeMax upperLimitOfSqrtOfObservationSize:\n" +
				"             The upper limit of the square-root of the size(=area) of observations.\n\n" + 
				"    --NumInitialTargets numberOfTargetsInFirstFrame:\n" +
				"             The number of targets from which observations are generated in the first frame.\n\n" + 
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
				"  Output:\n\n" +
				"    observations_file:\n" +
				"             Output-file of the series of observations that were generated.\n" +
				"             The file contains data according to the MTBXMLMultiStateMMIDVector XML-schema\n" +
				"             found in MiToBo's share/xmlschemata/mtbxml directory.\n\n" +
				"    observations_info_file:\n" +
				"             Output-textfile with information about the generated data.\n\n" +
				"    observations_image_file:\n" +
				"             Optional output-image of the series of observations that were generated.\n\n");
		
		
	}
}
