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
 * $Rev: 3874 $
 * $Date: 2011-05-27 16:26:40 +0200 (Fr, 27 Mai 2011) $
 * $Author: moeller $
 * 
 */

package cmdTools.cellAnalysis;

import ij.ImagePlus;
import jargs.gnu.CmdLineParser;

import java.awt.Color;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;

import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.formats.FormatException;
import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo;
import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo.ExportPolicy;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.MiToBo.apps.particles2D.HyperStackParticleDetectorUWT2D;
import de.unihalle.informatik.MiToBo.apps.particles2D.ParticleDetectorUWT2D;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2DSetBag;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.io.images.ImageReaderMTB;
import de.unihalle.informatik.MiToBo.io.images.ImageWriterMTB;
import de.unihalle.informatik.MiToBo.visualization.drawing.DrawRegion2DSet;
import de.unihalle.informatik.MiToBo.visualization.drawing.DrawRegion2DSet.DrawType;

/**
 * Commandline tool for particle detection based on the undecimated wavelet transform.
 * 
 * For further details see {@link de.unihalle.informatik.MiToBo.apps.particles2D.ParticleDetectorUWT2D} or refer to:
 * 
 * O. Gress, B. Möller, N. Stöhr, S. Hüttelmaier, S. Posch,
 * "Scale-adaptive wavelet-based particle detection in microscopy images".
 * In Proc. Bildverarbeitung für die Medizin (BVM 2010), pages 266-270, March 2010, Springer
 * 
 * 
 * @author Oliver Gress
 *
 */
@ALDMetaInfo(export=ExportPolicy.MANDATORY)
public class ParticleDetector2D {

	
	public static void main(String[] args) {
		System.out.print(" " +
				"<MiToBo>  Copyright (C) 2010  \n" +
				"This program comes with ABSOLUTELY NO WARRANTY; \n" +
				"This is free software, and you are welcome to redistribute it\n" +
				"under the terms of the GNU General Public License.\n\n\n");
		
		String usage = "USAGE: java cmdTools.cellAnalysis.ParticleDetector2D [{-v,--verbose}] [{-h,--help}] [{-d,--display}]\n" +
					   "        {-j,--Jmin} min_scale_idx {-J,--Jmax} max_scale_idx {-t,--thresh} threshold\n" +
					   "        {-s,--scale-interval-size} scale_interval_size [{-m,--min-size} min_particle_size]\n" +
					   "        [{-c,--channel} channel_idx] [{-p,--print-summary}] [{-o,--result-image} resultimage_file]\n" +
					   "        inputimage_file resultregions_file"; 
		
		
		
		 	CmdLineParser parser = new CmdLineParser();
		
		 	// ---- program options
		 	CmdLineParser.Option verbose = parser.addBooleanOption('v', "verbose");
		 	CmdLineParser.Option help = parser.addBooleanOption('h', "help");
		 	CmdLineParser.Option print = parser.addBooleanOption('p', "print-summary");
		
		 	// ---- particle detector parameters
		 	CmdLineParser.Option Jmin = parser.addIntegerOption('j', "Jmin");
		 	CmdLineParser.Option Jmax = parser.addIntegerOption('J', "Jmax");
		 	CmdLineParser.Option thresh = parser.addDoubleOption('t', "thresh");
		 	CmdLineParser.Option scaleIntervalSize = parser.addIntegerOption('s', "scale-interval-size");
		 	CmdLineParser.Option minSize = parser.addIntegerOption('m', "min-size");
		 	
		 	// ---- input-image options
		 	CmdLineParser.Option channelIdx = parser.addIntegerOption('c', "channel");
		 	
		 	// ---- result-image options
		 	CmdLineParser.Option display = parser.addBooleanOption('d', "display");
		 	CmdLineParser.Option resultimg = parser.addStringOption('o', "result-image");
		 	
	        try {
	        	parser.parse(args);
	        }
	        catch ( CmdLineParser.OptionException e ) {
	        	System.err.println(e.getMessage());
	        	System.out.println(usage);
	        	System.exit(2);
	        }
	        
	        String[] otherArgs = parser.getRemainingArgs();
	        
	        Boolean helpValue = (Boolean)parser.getOptionValue(help, Boolean.FALSE);

	        if (helpValue) {

	            System.out.println(usage + "\n");
	            ParticleDetector2D.printHelp();   	
	    	 	System.exit(0);
	        }
	        
	        
	        if (otherArgs.length != 2) {
	        	System.out.println(usage);
	        	System.exit(1);
	        }
	        else {
	        	if (otherArgs[0].equals(otherArgs[1])) {
                    System.err.println("Please specify an outputfile name different from inputfile.");
                    System.exit(1);
	        	}
	        }
	        
	        String infile = otherArgs[0];
	        String outfile = otherArgs[1];

	        
	        Boolean verboseValue = (Boolean)parser.getOptionValue(verbose, Boolean.FALSE);
	        
	        
	        // ---- read input image
	        
	        MTBImage inImg = null;
	        ImageReaderMTB r = null;
	
        	try {
				r = new ImageReaderMTB(infile);
	         	r.setVerbose(verboseValue);
			} catch (ALDOperatorException e) {
				System.err.println("Failed to create reader for input image: " + e.getMessage());
				System.exit(1);
			} catch (FormatException e) {
				System.err.println("Failed to create reader for input image: " + e.getMessage());
				System.exit(1);
			} catch (IOException e) {
				System.err.println("Failed to create reader for input image: " + e.getMessage());
				System.exit(1);
			} catch (DependencyException e) {
				System.err.println("Failed to create reader for input image: " + e.getMessage());
				System.exit(1);
			} catch (ServiceException e) {
				System.err.println("Failed to create reader for input image: " + e.getMessage());
				System.exit(1);
			}
		

         	try {
				r.runOp(false);
			} catch (ALDOperatorException e) {
				System.err.println("Failed to read input image: " + e.getMessage());
				System.exit(1);
			} catch (ALDProcessingDAGException e) {
				System.err.println("Failed to read input image: " + e.getMessage());
				System.exit(1);
			}
			
			inImg = r.getResultMTBImage();
			
			
			// ---- particle detection

		 	// get particle detector parameters
		 	int JminValue = (Integer) parser.getOptionValue(Jmin);
		 	int JmaxValue = (Integer) parser.getOptionValue(Jmax);
		 	double threshValue = (Double) parser.getOptionValue(thresh);
		 	int scaleIntervalSizeValue = (Integer) parser.getOptionValue(scaleIntervalSize);
		 	Integer minSizeValue = (Integer) parser.getOptionValue(minSize);
		 	if (minSizeValue == null)
		 		minSizeValue = 0;
		 	
		 	Integer channelIdxValue = (Integer) parser.getOptionValue(channelIdx);
		 	if (channelIdxValue == null)
		 		channelIdxValue = 0;
		 	if (channelIdxValue < 0 || channelIdxValue >= inImg.getSizeC()) {
		 		System.err.println("Channel index must be > 0 and < "+ inImg.getSizeC() + " (number of channels in input image).");
		 		System.exit(1);
		 	}
		 	
		 	// ---- result-image options
		 	boolean displayValue = (Boolean) parser.getOptionValue(display, Boolean.FALSE);
		 	String resultimgValue = (String) parser.getOptionValue(resultimg);
			
			
			// create particle detector
			ParticleDetectorUWT2D pdetector = null;

			try {
				pdetector = new ParticleDetectorUWT2D();
			} catch (ALDOperatorException e) {
				System.err.println("Failed to create 2D particle detector: " + e.getMessage());
				System.exit(1);
			}
			
			// set particle detector parameters
			pdetector.setJmin(JminValue);
			pdetector.setJmax(JmaxValue);
			pdetector.setCorrelationThreshold(threshValue);
			pdetector.setScaleIntervalSize(scaleIntervalSizeValue);
			pdetector.setMinRegionSize(minSizeValue);
			
			// create hyperstack particle detector operator (that runs over all slices of the input image)
			HyperStackParticleDetectorUWT2D hsdetector = null;
			
			try {
				hsdetector = new HyperStackParticleDetectorUWT2D(inImg, pdetector, channelIdxValue);
				hsdetector.setVerbose(verboseValue);
			} catch (ALDOperatorException e) {
				System.err.println("Failed to create hyperstack particle detector: " + e.getMessage());
				System.exit(1);
			}
			

			// run hyperstack particle detector
			try {
				hsdetector.runOp(false);
			} catch (ALDOperatorException e) {
				System.err.println("Execution of hyperstack particle detector failed: " + e.getMessage());
				System.exit(1);
			} catch (ALDProcessingDAGException e) {
				System.err.println("Execution of hyperstack particle detector failed: " + e.getMessage());
				System.exit(1);
			}
			
			MTBRegion2DSetBag regionsets = hsdetector.getResultingRegionsets();
			
			try {
				regionsets.write(outfile);
			} catch (ALDProcessingDAGException e) {
				System.err.println("Failed to write detected regions: " + e.getMessage());
				System.exit(1);
			} catch (ALDOperatorException e) {
				System.err.println("Failed to write detected regions: " + e.getMessage());
				System.exit(1);
			} catch (IOException e) {
				System.err.println("Failed to write detected regions: " + e.getMessage());
				System.exit(1);
			}
			
			
			if ((Boolean) parser.getOptionValue(print, Boolean.FALSE)) {
				// print detection summary
				double sizeMin = Double.POSITIVE_INFINITY;
				double sizeMax = Double.NEGATIVE_INFINITY;
				double size;
				int total = 0;
				
				System.out.println("Slice:\tNumber of detected particles");
				System.out.println("------------------------------------");
				for (int t = 0; t < regionsets.size(); t++) {
					System.out.println(regionsets.get(t).getInfo() + ":\t" + regionsets.get(t).size());
					total +=  regionsets.get(t).size();
					
					for (int m = 0; m < regionsets.get(t).size(); m++) {
						size = regionsets.get(t).get(m).getArea();
						if (size > sizeMax)
							sizeMax = size;
						if (size < sizeMin)
							sizeMin = size;
					}
				}
				System.out.println("------------------------------------");
				System.out.println("Total number of detected particles: " + total);
				System.out.println("Area of smallest particle: " + sizeMin);
				System.out.println("Area of largest particle: " + sizeMax);
			}

			if (displayValue || resultimgValue != null) {
				
				MTBImage resultImg = inImg.convertType(MTBImageType.MTB_RGB, true);
				resultImg.setTitle("Particle Detection in " + inImg.getTitle());
				
				
				DrawRegion2DSet drawer = null;
				
				try {
					drawer = new DrawRegion2DSet();
				} catch (ALDOperatorException e) {
					System.err.println("Failed to create region drawing operator: " + e.getMessage());
					System.exit(1);
				}
				
				drawer.setDrawType(DrawType.COLOR_IMAGE);
				drawer.setColor(new Color(255,0,0));
				drawer.setTargetImage(resultImg);
				
				for (int t = 0; t < resultImg.getSizeT(); t++) {
					for (int z = 0; z < resultImg.getSizeZ(); z++) {
						resultImg.setCurrentSliceCoords(z, t, channelIdxValue);
						drawer.setInputRegions(regionsets.get(t*resultImg.getSizeZ() + z));
						
						try {
							drawer.runOp(false);
						} catch (ALDOperatorException e) {
							System.err.println("Failed to draw regions to slice z="+z+",t="+t+"c="+channelIdxValue+
									": " + e.getMessage());
						} catch (ALDProcessingDAGException e) {
							System.err.println("Failed to draw regions to slice z="+z+",t="+t+"c="+channelIdxValue+
									": " + e.getMessage());
						}
					}
				}

				if (resultimgValue != null) {
					
					ImageWriterMTB writer = null;
					try {
						writer = new ImageWriterMTB(resultImg, resultimgValue);
					} catch (ALDOperatorException e) {
						System.err.println("Failed to write result image: " + e.getMessage());
					}
					
					try {

						writer.setOverwrite(true);
						writer.runOp(false);
					} catch (ALDOperatorException e) {
						System.err.println("Failed to write result image: " + e.getMessage());
					} catch (ALDProcessingDAGException e) {
						System.err.println("Failed to write result image: " + e.getMessage());
					} catch (NullPointerException e) {
						System.err.println("Failed to write result image: " + e.getMessage());
					}
					
				}
				
				if (displayValue) {
					final ImagePlus resultImp = resultImg.getImagePlus();

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
		
			
	}
	
	public static void printHelp() {
			
		System.out.println("OVERVIEW:\n" +
				"    This program detects 2D-particles like stress granules in microscopy images.\n" +
				"    The output file is xml-based and contains all the regions detected in any 2D-slice of the input image.\n" +
				"    For visualization of results an output image can be displayed and/or written to disk optionally.");
		System.out.println("\nOPTIONS:");
		System.out.println("    -v,--verbose:\n" +
				"             Enable verbose mode\n\n" +
				"    -h,--help:\n" +
				"             Print this help. \n\n" +
				"  Required Parameters:\n\n" +
				"    inputimage_file:\n" +
				"             Input image file that contains particles to detect.\n" +
				"             Detection is applied to every 2D-slice for multi-dimensional images.\n" +
				"             If multiple channels are available in the image file, consider the --channel option.\n\n" +
				"    resultregions_file:\n" +
				"             File with resulting particles. This file is based on XML and can be read with \n" +
				"             XMLBeans using the MTBXMLRegion2DSetBag.xsd scheme defined for MiToBo.\n" +
				"             It contains a set of pixels for each detected particle.\n\n" +
				"    -j,--Jmin min_scale_idx:\n" +
				"             The minimum scale index limits the range of wavelet coefficients to be used\n" +
				"             for detection. The minimum scale determines the highest bandpass used.\n\n" +
				"    -J,--Jmax max_scale_idx:\n" +
				"             The maximum scale index limits the range of wavelet coefficients to be used\n" +
				"             for detection. The maximum scale determines the lowest bandpass used.\n\n" +
				"    -t,--thresh threshold:\n" +
				"             Threshold for correlation images computed from wavelet coefficient images.\n\n" +
				"    -s,--scale-interval-size scale_interval_size:\n" +
				"             The scale interval size determines how many wavelet coefficient images of\n" +
				"             adjacent scales are combined to one correlation image. Any combination of this\n" +
				"             number of adjacent wavelet images within the range given by Jmin and Jmax results\n" +
				"             in such a correlation image. A kind of hypothesis testing retrieves the most\n" +
				"             likely particle regions from the various correlation images.\n\n" +
				"  Optional Parameters:\n\n" +
				"    -d,--display:\n" +
				"             Display an image that visualizes the results.\n\n" +
				"    -m,--min-size min_particle_size:\n" +
				"             The minimum size of detected particles' area in number of pixels.\n" +
				"             All particles with size smaller than this value are rejected.\n\n" +
				"    -c,--channel channel_idx:\n" +
				"             Index of the channel that contains the particles to be detected\n" +
				"             for images with multiple channels.\n" +
				"             Default is 0 (first channel).\n\n" +
				"    -o,--result-image resultimage_file:\n" +
				"            Filename to store an image that visualizes the results of detection.\n");
		
	}

}
