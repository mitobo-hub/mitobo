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
import de.unihalle.informatik.MiToBo.tracking.multitarget.eval.TrackEvaluator;

/**
 * Commandline tools to compare observation-files, i.e. the target-IDs of the individual observation,
 * to evaluate the quality of target-association in one file with respect to the other that is interpreted
 * as groundtruth.
 * 
 * @author Oliver Gress
 *
 */
public class EvalTracks {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.print(" " +
				"<MiToBo>  Copyright (C) 2010  \n" +
				"This program comes with ABSOLUTELY NO WARRANTY; \n" +
				"This is free software, and you are welcome to redistribute it\n" +
				"under the terms of the GNU General Public License.\n\n\n");
		
		String thisclass = EvalTracks.class.toString();
		thisclass = thisclass.replaceAll("^class ", "");
		
		String usage = "USAGE: java "+ thisclass + " [{-h,--help}]\n" +
		   			"        groundtruth_observations_file tracker_observations_file"; 
		
	 	CmdLineParser parser = new CmdLineParser();
	 	CmdLineParser.Option help = parser.addBooleanOption('h', "help");
        try {
        	parser.parse(args, Locale.US);
        }
        catch ( CmdLineParser.OptionException e ) {
        	System.err.println(e.getMessage());
        	System.out.println(usage);
        	System.exit(2);
        }
        
        if ((Boolean)parser.getOptionValue(help, Boolean.FALSE)) {
        	EvalTracks.printHelp();
        	System.exit(0);
        }
        
        String[] otherArgs = parser.getRemainingArgs();
		
        if (otherArgs == null || otherArgs.length != 2) {
        	System.out.println(usage);
        	System.exit(2);
        }
 
        
        // read ground truth observations/tracks
		Vector<MultiState<MotionModelID>> gtObservations = null;
		try {
			gtObservations = MultiStateIO.readMultiStates(otherArgs[0]);
		} catch (XmlException e) {
		} catch (IOException e) {
		}
		
		MTBRegion2DSetBag gtRegions = null;
		if (gtObservations == null) {
			try {
				gtRegions = new MTBRegion2DSetBag(otherArgs[0]);
			} catch (XmlException e) {
			} catch (IOException e) {
			}
			
			if (gtRegions != null) {
				try {
					gtObservations = DataConverter.regionsToObservations(false, gtRegions);
				} catch (ALDOperatorException e) {
				} catch (ALDProcessingDAGException e) {
				}
			}
		}
		if (gtObservations == null) {
			System.err.println("Failed to read ground truth observations.");
			System.exit(1);
		}
		
        
        // read tracker observations/tracks
		Vector<MultiState<MotionModelID>> trackerObservations = null;
		try {
			trackerObservations = MultiStateIO.readMultiStates(otherArgs[1]);
		} catch (XmlException e) {
		} catch (IOException e) {
		}
		
		MTBRegion2DSetBag inRegions = null;
		if (trackerObservations == null) {
			try {
				inRegions = new MTBRegion2DSetBag(otherArgs[1]);
			} catch (XmlException e) {
			} catch (IOException e) {
			}
			
			if (inRegions != null) {
				try {
					trackerObservations = DataConverter.regionsToObservations(false, inRegions);
				} catch (ALDOperatorException e) {
				} catch (ALDProcessingDAGException e) {
				}
			}
		}
		if (trackerObservations == null) {
			System.err.println("Failed to read tracker observations.");
			System.exit(1);
		}
        
		Vector<Vector<MultiState<MotionModelID>>> trackerObservationsVec = new Vector<Vector<MultiState<MotionModelID>>>(1);
		trackerObservationsVec.add(trackerObservations);
        TrackEvaluator eval = null;
        
        try {
			eval = new TrackEvaluator(gtObservations, trackerObservationsVec);
			eval.runOp(false);
		} catch (ALDOperatorException e) {
			System.err.println("Failed to evaluate tracks: " + e.getMessage());
			System.exit(1);
		} catch (ALDProcessingDAGException e) {
			System.err.println("Failed to evaluate tracks: " + e.getMessage());
			System.exit(1);
		}
		
		System.out.println(eval.trackEvalResult.get(0).toString());
        
	}
	
	private static void printHelp() {
		System.out.println(	"OVERVIEW:\n" +
							"    Compare groundtruth-trajectories with trajectories from tracking on basis\n" +
							"    of target-IDs given by observation-files and print evaluation results.");
		System.out.println( "\nOPTIONS:");
		System.out.println( "    -h,--help:\n" +
							"             Print this help. \n\n" +
							"  Parameters:\n\n" +
							"    groundtruth_observations_file:\n" +
							"             Specify a file that contains observations in a time sequence, where target-IDs\n" +
							"             are interpreted as groundtruth.\n" +
							"             The file must contain data according to the MTBXMLMultiStateMMIDVector XML-schema\n" +
							"             found in MiToBo's share/xmlschemata/mtbxml directory.\n\n" +
							"    tracker_observations_file:\n" +
							"             Specify a file that contains observations in a time sequence, where target-IDs\n" +
							"             of the observations were assigned by a tracker.\n" +
							"             The file must contain data according to the MTBXMLMultiStateMMIDVector XML-schema\n" +
							"             found in MiToBo's share/xmlschemata/mtbxml directory.\n\n");	
		
	}

}
