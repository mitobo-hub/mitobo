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

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Vector;

import org.apache.xmlbeans.XmlException;

import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2DSetBag;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatools.DataConverter;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatools.MultiStateIO;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.impl.MotionModelID;
import de.unihalle.informatik.MiToBo.tracking.multitarget.datatypes.impl.MultiState;

/**
 * Commandline tool to print information about a (time) sequence of observations and optionally their
 * association to targets/clutter.<br>
 * The input-file must contain data according to the MTBXMLMultiStateMMIDVector XML-schema found in 
 * MiToBo's share/xmlschemata/mtbxml directory.
 * 
 * @author Oliver Gress
 *
 */
public class ObservationsInfo {

	public static void main(String[] args) {
		System.out.print(" " +
				"<MiToBo>  Copyright (C) 2010  \n" +
				"This program comes with ABSOLUTELY NO WARRANTY; \n" +
				"This is free software, and you are welcome to redistribute it\n" +
				"under the terms of the GNU General Public License.\n\n\n");
		
		String thisclass = ObservationsInfo.class.toString();
		thisclass = thisclass.replaceAll("^class ", "");
		
		String usage = 	"USAGE: java "+ thisclass + "\n" +
						"        [{-h,--help}] [{-a,--associationInfo}]\n" +
		   				"        observationsfile"; 
		
		CmdLineParser parser = new CmdLineParser();
	
	 	CmdLineParser.Option help = parser.addBooleanOption('h', "help");
	 	CmdLineParser.Option assoc = parser.addBooleanOption('a', "associationInfo");
	 	
		try {
			parser.parse(args, Locale.US);
		}
		catch ( CmdLineParser.OptionException e ) {
			System.err.println(e.getMessage());
			System.out.println(usage);
			System.exit(2);
		}
		 
        Boolean helpValue = (Boolean)parser.getOptionValue(help, Boolean.FALSE);
        Boolean associationValue = (Boolean)parser.getOptionValue(assoc, Boolean.FALSE);    
        
        if (helpValue) {
        	ObservationsInfo.printHelp();
        	System.exit(0);
        }
        
		String[] otherArgs = parser.getRemainingArgs();
			
		if (otherArgs == null || otherArgs.length != 1) {
			System.out.println(usage);
			System.exit(2);
		}
		
        // read ground truth observations/tracks
		Vector<MultiState<MotionModelID>> observations = null;
		try {
			observations = MultiStateIO.readMultiStates(otherArgs[0]);
		} catch (XmlException e) {
		} catch (IOException e) {
		}
		
		MTBRegion2DSetBag regions = null;
		if (observations == null) {
			try {
				regions = new MTBRegion2DSetBag(otherArgs[0]);
			} catch (XmlException e) {
			} catch (IOException e) {
			}
			
			if (regions != null) {
				try {
					observations = DataConverter.regionsToObservations(false, regions);
				} catch (ALDOperatorException e) {
				} catch (ALDProcessingDAGException e) {
				}
			}
		}
		if (observations == null) {
			System.err.println("ERROR: Failed to read observations.");
			System.exit(1);
		}
		
		int T = observations.size();
		
		int[] numObs = new int[T];
		MultiState<MotionModelID> Z;
		int maxNum = -1;
		int minNum = -1;
		double maxDist = Double.NEGATIVE_INFINITY;
		double minDist = Double.POSITIVE_INFINITY;
		double dist;
		int sumObs = 0;
	//	int sumDists = 0;
	
		for (int t = 0; t < T; t++) {
			Z = observations.get(t);
			
			numObs[t] = Z.getNumberOfStates();
			sumObs += numObs[t];
			
			if (maxNum == -1 || maxNum < numObs[t])
				maxNum = numObs[t];
			if (minNum == -1 || minNum > numObs[t])
				minNum = numObs[t];
			
			for (int m = 0; m < numObs[t]; m++) {
				
				for (int mm = m+1; mm < numObs[t]; mm++) {
					
					dist = Z.getStateContinuous(m).minus(Z.getStateContinuous(mm)).normF();
				//	sumDists++;
					
					if (dist < minDist)
						minDist = dist;
					
					if (dist > maxDist)
						maxDist = dist;
				}
			}
		}		
		
		double[] mv = ObservationsInfo.meanAndVariance(numObs);
		
		System.out.println("--- Observations Info ---");
		System.out.println("Mean number of observations:		" + mv[0]);
		Arrays.sort(numObs);
		System.out.println("Median number of observations:		" + numObs[numObs.length/2]);
		System.out.println("Variance of number of observations:	" + mv[1]);
		System.out.println("Minimum number of observations:		" + minNum);
		System.out.println("Maximum number of observations:		" + maxNum);
		System.out.println("Total number of observations:		" + sumObs);
		System.out.println("Minimum distance of observations:	" + minDist);
		System.out.println("Maximum distance of observations:	" + maxDist);
	//	System.out.println("Total of distances: " + sumDists);
		
		if (associationValue) {
			
			int[] numclutter = new int[T];
			int[] numbirth = new int[T];
			
			int detections = 0; 
			int nondetections = 0;
			
			double[] numlastassoc = new double[T];
			double[] tau = new double[T];
			
			HashSet<Integer> ids = new HashSet<Integer>();
			
			for (int t = 0; t < T; t++) {
				tau[t] = t;
				
				for (int m = 0; m < observations.get(t).getNumberOfStates(); m++) {
					int id = observations.get(t).getStateDiscrete(m).ID;
					
					if (id == 0) {
						numclutter[t]++;
					}
					else {
						if (!ids.contains(id)) {
							ids.add(id);
							if (t > 0)
								numbirth[t]++;
						}
						
						// find next obs of target 'id'
						int t_ = -1;
						for (int tt = t+1; tt < T-1 && t_ == -1; tt++) {
							for (int mm = 0; mm < observations.get(tt).getNumberOfStates() && t_ == -1; mm++) {
								if (observations.get(tt).getStateDiscrete(mm).ID == id) {
									t_ = tt;
								}
							}
						}
						
						detections++;
						numlastassoc[0]++;
						
						if (t_ != -1) {
							nondetections += t_ - t - 1;
							numlastassoc[t_ - t]++;
						}
					}
				}
			}
			
			double[] mvclutter = ObservationsInfo.meanAndVariance(numclutter);
			double[] mvbirth = ObservationsInfo.meanAndVariance(numbirth);	
			
			System.out.println("\n--- Association Info ---");
			System.out.println("Mean number of clutter observations:			" + mvclutter[0]);
			System.out.println("Variance of the number of clutter observations:	" + mvclutter[1]);
			System.out.println("Mean number of newborn targets:					" + mvbirth[0]);
			System.out.println("Variance of the number of newborn targets:		" + mvbirth[1]);
			System.out.println("Relative frequency of target detections:		" + ((double)detections/((double)(detections+nondetections))));
		}
		
	}
	
	static double[] meanAndVariance(int[] nums) {
		double[] mv = new double[2];
		
		for (int t = 0; t < nums.length; t++) {
			mv[0] += nums[t];
		}
		mv[0] /= (double)nums.length;
		
		for (int t = 0; t < nums.length; t++) {
			mv[1] += (nums[t] - mv[0])*(nums[t] - mv[0]);
		}
		mv[1] /= ((double)nums.length-1);
		
		return mv;
	}
	
	private static void printHelp() {
		System.out.println(	"OVERVIEW:\n" +
							"    Print information about the observations (and their association to targets/clutter)\n" +
							"    given in the observationsfile.");
		System.out.println( "\nOPTIONS:");
		System.out.println( "    -h,--help:\n" +
							"             Print this help. \n\n" +
							"    -a,--associationInfo:\n" +
							"             Print information about target detection, target births and clutter observations\n" +
							"             on basis of the observations' target-IDs given in the file.\n" +
							"             Observations of the same target are identified by sharing the same target-ID,\n" +
							"             clutter observations by target-ID 0.\n\n" +
							"  Parameters:\n\n" +
							"    observationsfile:\n" +
							"             Specify a file that contains observations in a time sequence.\n" +
							"             The file must contain data according to the MTBXMLMultiStateMMIDVector XML-schema\n" +
							"             found in MiToBo's share/xmlschemata/mtbxml directory.\n" +
							"             (See de.unihalle.informatik.MiToBo.tracking.multitarget.datatools.MultiStateIO for IO-methods)\n\n");	
		
	}

}
