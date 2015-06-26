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
 * $Rev: 5463 $
 * $Date: 2012-04-17 17:17:15 +0200 (Tue, 17 Apr 2012) $
 * $Author: moeller $
 * 
 */

package cmdTools.visualization;

import ij.ImagePlus;
import jargs.gnu.CmdLineParser;

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;

import cmdTools.tracking.multitarget.RBMCDATracker;

import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.formats.FormatException;
import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo;
import de.unihalle.informatik.Alida.admin.annotations.ALDMetaInfo.ExportPolicy;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.MiToBo.gui.SynchronizedImageWindows;
import de.unihalle.informatik.MiToBo.io.images.ImageReaderMTB;

/**
 * A simple commandline tool to display image files using MiToBo's image reader. You can zoom in and out with the '+' and '-' keys.
 * @author Oliver Gress
 *
 */
@ALDMetaInfo(export=ExportPolicy.MANDATORY)
public class DisplayImage {

	private static int openWindows = 0;
	
	public static void main(String[] args) {
		
		System.out.print(" " +
				"<MiToBo>  Copyright (C) 2010  \n" +
				"This program comes with ABSOLUTELY NO WARRANTY; \n" +
				"This is free software, and you are welcome to redistribute it\n" +
				"under the terms of the GNU General Public License.\n\n\n");
		
		String thisclass = DisplayImage.class.toString();
		thisclass = thisclass.replaceAll("^class ", "");
		
		String usage = "USAGE: java "+ thisclass + " [{-v,--verbose}] [{-h,--help}] [{-s,--sync-images}]\n"
					 + "       [{-n,--num-images}] [{-i,--image-index} imageindex] imagefile1 [imagefile2 ...]\n";
		
		
		CmdLineParser parser = new CmdLineParser();
		
	 	// ---- program options
	 	CmdLineParser.Option verbose = parser.addBooleanOption('v', "verbose");
	 	CmdLineParser.Option help = parser.addBooleanOption('h', "help");
	 	CmdLineParser.Option sync = parser.addBooleanOption('s', "sync-images");
	 	CmdLineParser.Option imgIdx = parser.addIntegerOption('i', "image-index");
	 	CmdLineParser.Option numImages = parser.addBooleanOption('n', "num-images");
	 	
        try {
        	parser.parse(args);
        }
        catch ( CmdLineParser.OptionException e ) {
        	System.err.println(e.getMessage());
        	System.out.println(usage);
        	System.exit(2);
        }
        
        Boolean verboseValue = (Boolean)parser.getOptionValue(verbose, Boolean.FALSE);
        Boolean helpValue = (Boolean)parser.getOptionValue(help, Boolean.FALSE);
        Boolean syncValue = (Boolean)parser.getOptionValue(sync, Boolean.FALSE);
        Integer imgIdxValue = (Integer)parser.getOptionValue(imgIdx, new Integer(0));
        Boolean numImagesValue = (Boolean)parser.getOptionValue(numImages, Boolean.FALSE);
        
        if (helpValue) {
        	DisplayImage.printHelp();
        	System.exit(0);
        }
        
        SynchronizedImageWindows syncwins = null;
        
        if (syncValue)
        	syncwins = new SynchronizedImageWindows();
        
        
        String[] otherArgs = parser.getRemainingArgs();
        
        if (otherArgs != null && otherArgs.length >= 1) {
        	
        	for (int i = 0; i < otherArgs.length; i++) {
        	
        		ImageReaderMTB reader = null;
      
        		try {
        			reader = new ImageReaderMTB(otherArgs[i]);
        			reader.setVerbose(verboseValue);
        			
				} catch (ALDOperatorException e) {
					System.err.println("Failed to create reader for '"+otherArgs[i]+"': " + e.getMessage());
				} catch (FormatException e) {
					System.err.println("Failed to create reader for '"+otherArgs[i]+"': " + e.getMessage());
				} catch (IOException e) {
					System.err.println("Failed to create reader for '"+otherArgs[i]+"': " + e.getMessage());
				} catch (DependencyException e) {
					System.err.println("Failed to create reader for '"+otherArgs[i]+"': " + e.getMessage());
				} catch (ServiceException e) {
					System.err.println("Failed to create reader for '"+otherArgs[i]+"': " + e.getMessage());
				}
			
				if (reader != null && numImagesValue) {
					
					int num = reader.getImageCount();
					System.out.println("Number of images in '"+otherArgs[i]+"': " + num);
					reader = null;
					
				}
				else if (reader != null) {
					
					int start = imgIdxValue;
					int end = imgIdxValue;
					
					if (imgIdxValue < 0) {
						// setup to display all images
						start = 0;
						end = reader.getImageCount()-1;
					}

					for (int idx = start; idx <= end; idx++) {
				
						reader.setIndexOfImageToRead(idx);
						
						try {
							reader.runOp(false);
						} catch (ALDProcessingDAGException e) {
							System.err.println("Failed to read image '"+otherArgs[i]+"': " + e.getMessage());
						} catch (ALDOperatorException e) {
							System.err.println("Failed to read image '"+otherArgs[i]+"': " + e.getMessage());
						}
					
					
						if (reader.getResultMTBImage() != null) {
							ImagePlus imP = reader.getResultMTBImage().getImagePlus();
							
							imP.show();
							DisplayImage.openWindows++;
							
							if (syncwins != null)
								syncwins.addImage(imP);
							
							imP.getWindow().addWindowListener(new WindowListener() {
								
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
									DisplayImage.openWindows--;
									if (DisplayImage.openWindows == 0)
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
			}
        }
        else {
        	System.out.println(usage);
        	System.exit(1);
        }
	}
	
	public static void printHelp() {
		
		System.out.println("OVERVIEW:\n" +
				"    Display images using MiToBo's image reader.");
		System.out.println("\nOPTIONS:");
		System.out.println("    -v,--verbose:\n" +
				"             Enable verbose mode\n\n" +
				"    -h,--help:\n" +
				"             Print this help. \n\n" +
				"  Parameters:\n\n" +
				"    imagefile1 [imagefile2 ...]:\n" +
				"             Specify one or more image files to display.\n" +
				"             If the image files contain more than one image, only the first (index=0) image is displayed\n" +
				"             of each file (see --image-index option).\n\n" +
				"    -n,--num-images:\n" +
				"             Print the number of images stored in each image file and exit.\n\n" +
				"    -i,--image-index imageindex:\n" +
				"             Display the image specified by 'imageindex' of each given image file.\n" +
				"             If 'imageindex' < 0, all images of each image file are displayed.\n" +
				"             Otherwise only the image with index 'imageindex' of each image file is displayed.\n\n" +
				"    -s,--sync-images:\n" +
				"             Synchronized image windows (i.e. all windows display the same 2D slice).\n\n");
		
	}
	
}
