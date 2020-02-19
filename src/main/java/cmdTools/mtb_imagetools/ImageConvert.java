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
 * $Rev$
 * $Date$
 * $Author$
 * 
 */

package cmdTools.mtb_imagetools;

import jargs.gnu.CmdLineParser;

import java.io.IOException;
import java.util.HashMap;

import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.formats.FormatException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.io.images.ImageReaderMTB;
import de.unihalle.informatik.MiToBo.io.images.ImageWriterMTB;
import de.unihalle.informatik.MiToBo.io.tools.ImageIOUtils;
import de.unihalle.informatik.MiToBo.tools.image.ImageConverter;

/**
 * Commandline tool to convert image formats and datatypes.
 * 
 * @author Oliver Gress
 *
 */
public class ImageConvert {

  public static void main(String[] args) {
		System.out.print(" " +
				"<MiToBo>  Copyright (C) 2010  \n" +
				"This program comes with ABSOLUTELY NO WARRANTY; \n" +
				"This is free software, and you are welcome to redistribute it\n" +
		    "under the terms of the GNU General Public License.\n\n\n");
		
		String thisclass = ImageConvert.class.toString();
		thisclass = thisclass.replaceAll("^class ", "");
		
		String usage = "USAGE: java "+ thisclass + " [{-v,--verbose}] [{-h,--help}]\n" +
					   "        [{-t,--type} datatype] [{-s,--scale-values}] [{-c,--channels-are-rgb}]\n" +
					   "        [{-p,--compression} compression] [{-d,--codec} codec] [{-q,--quality} quality]\n" +
					   "        [{-f,--fps} frames_per_second] [{-i,--image-index} image_idx]\n" +
					   "        inputfile outputfile"; 
		
	 	CmdLineParser parser = new CmdLineParser();
		
	 	// ---- program options
	 	CmdLineParser.Option verbose = parser.addBooleanOption('v', "verbose");
	 	CmdLineParser.Option help = parser.addBooleanOption('h', "help");

	 	// ---- datatype conversion options
	 	CmdLineParser.Option type = parser.addStringOption('t', "type");
	 	CmdLineParser.Option scale = parser.addBooleanOption('s', "scale-values");
	 	CmdLineParser.Option channelsRGB = parser.addBooleanOption('c', "channels-are-rgb");

	 	// ---- writer options
	 	// image options
	 	CmdLineParser.Option comp = parser.addStringOption('p', "compression");
	 	// video options
	 	CmdLineParser.Option codec = parser.addStringOption('d', "codec");
	 	CmdLineParser.Option qual = parser.addStringOption('q', "quality");
	 	CmdLineParser.Option fps = parser.addIntegerOption('f', "fps");
		
	 	// ---- reader options
	 	CmdLineParser.Option imgidx = parser.addIntegerOption('i', "image-index");

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

	 	if (helpValue.booleanValue()) {

	 		System.out.println(usage + "\n");

	 		if (otherArgs == null || otherArgs.length == 0)
	 			ImageConvert.printHelp(null, null);
	 		else if (otherArgs.length == 1)
	 			ImageConvert.printHelp(otherArgs[0], null);
	 		else {
	 			if (otherArgs[0].equals(otherArgs[1])) {
	 				System.err.println("Please specify an outputfile name different from inputfile.");
	 				System.exit(1);
	 			}
	 			else	
	 				ImageConvert.printHelp(otherArgs[0], otherArgs[1]);
	 		}

	 		System.exit(0);
	 	}

	 	if (otherArgs.length != 2) {
	 		System.out.println(usage);
	 		System.exit(1);
	 	}

	 	String infile = otherArgs[0];
	 	String outfile = otherArgs[1];

	 	if (infile.equals(outfile)) {
	 		System.err.println("Please specify an outputfile name different from inputfile.");
	 		System.exit(1);
	 	}

	 	Boolean verboseValue = (Boolean)parser.getOptionValue(verbose, Boolean.FALSE);

	 	String typeValue = (String)parser.getOptionValue(type);
	 	Boolean scaleValue = (Boolean)parser.getOptionValue(scale, Boolean.FALSE);
	 	Boolean channelsRGBValue = (Boolean)parser.getOptionValue(channelsRGB, Boolean.FALSE);

	 	String compValue = (String)parser.getOptionValue(comp);
	 	String codecValue = (String)parser.getOptionValue(codec);
	 	String qualValue = (String)parser.getOptionValue(qual);
	 	Integer fpsValue = (Integer)parser.getOptionValue(fps);

	 	Integer imgidxValue= (Integer)parser.getOptionValue(imgidx, new Integer(0));

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

	 	int imgsToRead = 1;

	 	String[] fsplit = null;
	 	if (imgidxValue.intValue() < 0 && r.getImageCount() > 1) {
	 		imgsToRead = r.getImageCount();
	 		fsplit = ImageConvert.splitFilename(outfile);
	 	}



	 	for (int n = 0; n < imgsToRead; n++) {

	 		String filename = null;

	 		if (imgsToRead > 1) {
	 			r.setIndexOfImageToRead(n);

	 			filename = fsplit[0];

	 			if (r.getImageName(n) != null)
	 				filename += "_" + r.getImageName(n) + fsplit[1];
	 			else if (r.getImageID(n) != null)
	 				filename += "_" + r.getImageID(n) + fsplit[1];
	 			else
	 				filename += "_" + n + fsplit[1];

	 		}
	 		else {
	 			r.setIndexOfImageToRead(imgidxValue.intValue());
	 			filename = outfile;
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


	 		if (inImg == null) {
	 			System.err.println("Failed to read input image: Reason unknown.");
	 			System.exit(1);
	 		}

	 		MTBImage outImg = inImg;

	 		// convert image to different datatype if specified
	 		if (typeValue != null) {

	 			MTBImageType outtype = null;

	 			if (typeValue.equals("byte")) {
	 				outtype = MTBImageType.MTB_BYTE;
	 			}
	 			else if (typeValue.equals("short")) {
	 				outtype = MTBImageType.MTB_SHORT;
	 			}
	 			else if (typeValue.equals("int")) {
	 				outtype = MTBImageType.MTB_INT;
	 			}
	 			else if (typeValue.equals("float")) {
	 				outtype = MTBImageType.MTB_FLOAT;
	 			}
	 			else if (typeValue.equals("double")) {
	 				outtype = MTBImageType.MTB_DOUBLE;
	 			}
	 			else if (typeValue.equals("rgb")) {
	 				outtype = MTBImageType.MTB_RGB;
	 			}

	 			if (outtype != null) {
	 				try {
	 					ImageConverter conv = new ImageConverter(inImg, outtype, 
	 							scaleValue.booleanValue(), channelsRGBValue);
	 					conv.runOp(false);
	 					outImg = conv.getResultImg();
	 				} catch (ALDOperatorException e) {
	 					System.err.println("Failed to convert image: " + e.getMessage());
	 					System.exit(1);
	 				} catch (ALDProcessingDAGException e) {
	 					System.err.println("Failed to convert image: " + e.getMessage());
	 					System.exit(1);
	 				}

	 			}
	 			else {
	 				System.err.println("Failed to convert image because of unknown output datatype: " + typeValue);
	 				System.exit(1);
	 			}
	 		}


	 		// ---- initialize writer
	 		ImageWriterMTB w = null;
	 		try {
	 			w = new ImageWriterMTB(outImg, filename);
	 			w.setVerbose(verboseValue);
	 			w.setOverwrite(true);
	 		} catch (ALDOperatorException e) {
	 			System.err.println("Failed to create image writer: " + e.getMessage());
	 			System.exit(1);
	 		}

	 		// ---- writer options
	 		// compression
	 		if (compValue != null) {

	 			String[] comps = null;
	 			try {
	 				comps = w.getAvailableCompression();
	 			} catch (FormatException e) {
	 				System.err.println("Failed to obtain information on available compression for specified outfile: " + e.getMessage());
	 				System.exit(1);
	 			}

	 			if (comps != null) {

	 				boolean compvalid = false;
	 				for (String compr : comps) {
	 					if (compr.equals(compValue)) {
	 						compvalid = true;
	 						break;
	 					}
	 				}

	 				if (compvalid)
	 					w.setCompression(compValue);
	 				else
	 					System.err.println("Warning: Specified compression is invalid. Ignoring compression argument.");
	 			}
	 			else {
	 				System.err.println("Warning: Ignoring specified compression argument because writer does not support any compression options.");
	 			}
	 		}

	 		// codec
	 		if (codecValue != null) {

	 			HashMap<Integer, String> codecs = null;
	 			try {
	 				codecs = w.getAvailableCodecs();
	 			} catch (FormatException e) {
	 				System.err.println("Failed to obtain information on available codecs for specified outfile: " + e.getMessage());
	 				System.exit(1);
	 			}

	 			if (codecs != null) {

	 				Integer codecID = ImageIOUtils.getKey(codecs, codecValue);

	 				if (codecID != null)
	 					try {
	 						w.setCodec(codecID);
	 					} catch (FormatException e) {
	 						System.err.println("Failed to set the specified codec: " + e.getMessage());
	 						System.exit(1);
	 					}
	 					else
	 						System.err.println("Warning: Specified codec is invalid. Ignoring codec argument.");
	 			}
	 			else {
	 				System.err.println("Warning: Ignoring specified codec argument because writer does not support any codec options.");
	 			}
	 		}

	 		// quality
	 		if (qualValue != null) {

	 			HashMap<Integer, String> quals = null;
	 			try {
	 				quals = w.getAvailableQualities();
	 			} catch (FormatException e) {
	 				System.err.println("Failed to obtain information on available qualities for specified outfile: " + e.getMessage());
	 				System.exit(1);
	 			}

	 			if (quals != null) {

	 				Integer qualID = ImageIOUtils.getKey(quals, qualValue);

	 				if (qualID != null)
	 					try {
	 						w.setQuality(qualID);
	 					} catch (FormatException e) {
	 						System.err.println("Failed to set the specified quality: " + e.getMessage());
	 						System.exit(1);
	 					}
	 					else
	 						System.err.println("Warning: Specified quality is invalid. Ignoring quality argument.");
	 			}
	 			else {
	 				System.err.println("Warning: Ignoring specified quality argument because writer does not support any quality options.");
	 			}
	 		}

	 		if (fpsValue != null) {
	 			try {
	 				w.setFps(fpsValue);
	 			} catch (FormatException e) {
	 				System.err.println("Failed to set the specified frames-per-second: " + e.getMessage());
	 				System.exit(1);
	 			}
	 		}

			
	 		try {
	 			w.runOp(false);
	 		} catch (ALDOperatorException e) {
	             System.err.println("Failed to write image: " + e.getMessage());
	             System.exit(1);
	 		} catch (ALDProcessingDAGException e) {
	             System.err.println("Failed to write image: " + e.getMessage());
	             System.exit(1);
	 		}

	 	}
		
	 	System.exit(0);
	}
	
  protected static void printHelp(String infile, String outfile) {
		System.out.println("OVERVIEW:\n" +
				"    This program converts images to different formats and datatypes.\n" +
				"    The output format is determined by the output file extension.\n" +
				"    This program uses the Bio-Formats library (UW-Madison LOCI and Glencoe Software Inc.)\n" +
		    	"    to read and write a number of (bio-medical) image formats.");
		System.out.println("\nOPTIONS:");
		System.out.println("    -v,--verbose:\n" +
				"             Enable verbose mode\n\n" +
				"    -h,--help:\n" +
				"             Print this help. \n" +
				"             If inputfile and outputfile is specified, available image reader/option are\n" +
				"             printed in addition.\n\n" +
				"  Conversion Options:\n\n" +
				"    -t,--type datatype:\n" +
				"             Specify a new datatype for the ouput file.\n" +
				"             Valid datatypes are:\n" +
				"             * byte:   unsigned integer       (8Bit)\n" +
				"             * short:  unsigned integer      (16Bit)\n" +
				"             * int:    signed integer        (32Bit)\n" +
				"             * float:  floating point        (32Bit)\n" +
				"             * double: floating point        (64Bit)\n" +
				"             * rgb:    unsigned integer RGB (3x8Bit)\n\n" +
				"    -s,--scale-values:\n" +
				"             Enable scaling of image values if they exceed output datatype range.\n\n" +
				"    -c,--channels-are-rgb:\n" +
				"             Enables splitting of RGB-channels to true channes for conversion\n" +
				"             from RGB to gray-values. Otherwise RGB-values are averaged.\n" +
				"             Enables merging of channels to RGB-channels for conversion from\n" +
				"             gray-values to RGB. Otherwise each channel is interpreted as RGB\n" +
				"             with gray colors.\n\n" +
				"  Writer Options:\n\n" +
				"    -p,--compression compression:\n" +
				"             Specify an image compression type. The available compression types\n" +
				"             depend on the output file format. Use '-h inputfile outputfile'\n" +
				"             to print the available compressions for the specific format.\n" +
				"             If no compression is specified, the writer's default is used.\n\n" +
				"    -d,--codec codec:\n" +
				"             Specify a video codec. The available codecs depend on the output file format\n" +
				"             (only available for Quicktime). Use '-h inputfile outputfile'to print available\n" +
				"             codecs for the specific format.\n" +
				"             If no codec is specified, the writer's default is used.\n\n" +
				"    -q,--quality quality:\n" +
				"             Specify the video quality. The available qualities depend on the output file\n" +
				"             format (only available for Quicktime). Use '-h inputfile outputfile' to print\n" +
				"             available qualities for the specific format.\n" +
				"             If no quality is specified, the writer's default is used.\n\n" +
				"    -f,--fps frames_per_second:\n" +
				"            Specify the frames per second for video output.\n" +
				"            (This option seems to have no effect on AVI videos -> always 10fps)\n\n" +
				"  Reader Options:\n\n" +
				"    -i,--image-index image_idx:\n" +
				"            Some file formats may contain multiple images (sometimes called series).\n" +
				"            * The index may range from 0 to (#images-1). Only the specified image is\n" +
				"              is converted and written to disk. Use '-h inputfile [outputfile]' to\n" +
				"              print information on available images and indices.\n" +
				"            * If no index is specified, only the first image is converted and stored.\n" +
				"            * If index is set to -1, all images in the file are converted and written.\n" +
				"              The output filenames are extended by the current image's name, ID or index\n" +
	    	"              (by availability in this order).\n");


		if (infile != null) {
			ImageReaderMTB r = null;

			try {
				r = new ImageReaderMTB(infile);
			} catch (ALDOperatorException e) {
				System.err.println("Failed to create reader for infile: " + e.getMessage());
				System.exit(1);
			} catch (FormatException e) {
				System.err.println("Failed to create reader for infile: " + e.getMessage());
				System.exit(1);
			} catch (IOException e) {
				System.err.println("Failed to create reader for infile: " + e.getMessage());
				System.exit(1);
			} catch (DependencyException e) {
				System.err.println("Failed to create reader for infile: " + e.getMessage());
				System.exit(1);
			} catch (ServiceException e) {
				System.err.println("Failed to create reader for infile: " + e.getMessage());
				System.exit(1);
			}

			int nImgs = r.getImageCount();

			System.out.println("  Available images in file:\n");
			for (int n = 0; n < nImgs; n++) {
				System.out.print("    - Index " + n + ":  ");
				if (r.getImageID(n) != null)
					System.out.print("ImageID=" + r.getImageID(n));
				else
					System.out.print("ImageID=NONE");

				if (r.getImageName(n) != null)
					System.out.print("  ImageName=" + r.getImageName(n) + "\n");
				else
					System.out.print("  ImageName=NONE\n");
			}
			System.out.print("\n");
		}


		if (outfile != null) {

			ImageWriterMTB w = null;

			try {
				w = new ImageWriterMTB();
			} catch (ALDOperatorException e) {
				System.err.println("Failed to create writer for specified outfile: " + e.getMessage());
				System.exit(1);
			}

			w.setFileName(outfile);

			System.out.println("  Options for current writer:\n");

			String[] comps = null;
			try {
				comps = w.getAvailableCompression();
			} catch (FormatException e) {
				System.err.println("Failed to obtain information on available compression for specified outfile: " + e.getMessage());
				System.exit(1);
			}

			if (comps != null) {
				System.out.println("    - Available image compression types:");
				for (String comp : comps) {
					System.out.println("            * " + comp);
				}
				System.out.print("\n");
			}
			else {
				System.out.println("    - No image compression options available.\n");
			}

			HashMap<Integer,String> codecs = null;
			try {
				codecs = w.getAvailableCodecs();
			} catch (FormatException e) {
				System.err.println("Failed to obtain information on available codecs for specified outfile: " + e.getMessage());
				System.exit(1);
			}

			if (codecs != null) {
				System.out.println("    - Available video codecs:");
				for (String codec : codecs.values()) {
					System.out.println("            * " + codec);
				}
				System.out.print("\n");
			}
			else {
				System.out.println("    - No video codec options available.\n");
			}

			HashMap<Integer,String> quals = null;
			try {
				quals = w.getAvailableQualities();
			} catch (FormatException e) {
				System.err.println("Failed to obtain information on available qualities for specified outfile: " + e.getMessage());
				System.exit(1);
			}

			if (quals != null) {
				System.out.println("    - Available qualities:");
				for (String qual : quals.values()) {
					System.out.println("            * " + qual);
				}
				System.out.print("\n");
			}
			else {
				System.out.println("    - No quality options available.\n");
			}
		}
	}

	protected static String[] splitFilename(String filename) {

		String lowname = filename.toLowerCase();

		String[] s = new String[2];

		if (lowname.endsWith(".ome.tif") || lowname.endsWith(".ome.tiff")) {
			int idx1 = filename.lastIndexOf(".");
			int idx2 = filename.lastIndexOf(".", idx1-1);
			s[0] = filename.substring(0, idx2);
			s[1] = filename.substring(idx2);
		}
		else {
			int idx = filename.lastIndexOf(".");
			s[0] = filename.substring(0, idx);
			s[1] = filename.substring(idx);
		}

		return s;
	}
}
