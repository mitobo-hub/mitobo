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

package de.unihalle.informatik.MiToBo.visualization.colormappings;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Vector;

import de.unihalle.informatik.Alida.annotations.Parameter.ParameterModificationMode;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.Alida.helpers.ALDFilePathManipulator;
import de.unihalle.informatik.Alida.operator.events.ALDOperatorExecutionProgressEvent;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.datatypes.ALDDirectoryString;
import de.unihalle.informatik.Alida.datatypes.ALDFileString;
import de.unihalle.informatik.MiToBo.core.datatypes.images.*;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.operator.*;
import de.unihalle.informatik.MiToBo.io.dirs.DirectoryTree;
import de.unihalle.informatik.MiToBo.io.images.ImageReaderMTB;
import de.unihalle.informatik.MiToBo.io.images.ImageWriterMTB;
import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.formats.FormatException;

/**
 * This operator transforms the intensity values of a gray-scale image 
 * according to a given intensity mapping.
 * <p>
 * The map must be provided in a file where each line contains a pair of
 * tab-separated values. The first value is the value in the given image and
 * the second value is the new value to which the first one is to be mapped 
 * in the output image.
 * <p>
 * Image values not being present in the map will be ignored, i.e. either 
 * mapped to zero or left untouched depending on the value of the parameter 
 * {@link #ignoredValuesMode}.
 * <p>
 * Note that in single image mode the map file has to be specified explicitly 
 * while in directory mode it is assumed that the map files are located in the
 * directory which is to be processed. For each image named 'X.tif' the 
 * corresponding map file should be named 'X-map.txt'. 
 *
 * @author moeller
 */
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.ALL,
		level=Level.STANDARD)
public class GrayscaleToGrayscaleMapping extends MTBOperator {

	/**
	 * Class identifier.
	 */
	private static final String classID = "[GrayscaleToGrayscaleMapping]";
	
	/**
	 * Operation mode of the operator.
	 */
	public static enum OperationMode {
		/**
		 * Process a single image provided directly.
		 */
		SINGLE_IMAGE,
		/**
		 * Process all images in the given directory.
		 */
		DIRECTORY
	}

	/**
	 * Available modes how to handle ignored values.
	 */
	public static enum IgnoredValuesHandlingMode {
		/**
		 * Leave values untouched.
		 */
		LEAVE_UNTOUCHED,
		/**
		 * Map values to black.
		 */
		MAP_TO_BLACK
	}

	/**
	 * Mode of operation of the operator.
	 */
	@Parameter(label = "Operation Mode", required = true, 
		direction = Parameter.Direction.IN,	dataIOOrder = -5, 
		description = "Operation mode of the operator.",
		callback = "switchOpModeParameters",
		paramModificationMode = ParameterModificationMode.MODIFIES_INTERFACE)
	public OperationMode opMode = OperationMode.DIRECTORY;

	/**
	 * Input image.
	 */
	@Parameter( label= "Input Image", required = true, dataIOOrder = 1, 
		direction = Parameter.Direction.IN, description = "Input image.")
	protected MTBImage inputImg = null;

	/**
	 * Input directory.
	 */
	@Parameter(label = "Input Directory", required = true, 
		direction = Parameter.Direction.IN, description = "Input directory.",
		dataIOOrder = 1)
	private ALDDirectoryString inDir = null;
	
	/**
	 * Textfile with mapping.
	 */
	@Parameter( label= "Map file", required = true, 
			dataIOOrder = 2, direction = Parameter.Direction.IN, 
		description = "File with mapping information.")
	protected ALDFileString filePath;	
	
	/**
	 * Optional binary mask of additional pixels to ignore.
	 * <p>
	 * Pixels marked white are ignored, i.e. mapped to black or left untouched.
	 */
	@Parameter( label= "Ignore mask?", required = false, 
		dataIOOrder = 5, direction = Parameter.Direction.IN, 
		description = "Ignore mask, pixels with values > 0 are ignored.")
	protected MTBImageByte ignoreMask = null;
	
	/**
	 * Mode how to handle ignored values.
	 */
	@Parameter( label= "How to handle ignored values?", required = false, 
		dataIOOrder = 6, direction = Parameter.Direction.IN, 
		description = "Mode how ignored values are treated.")
	protected IgnoredValuesHandlingMode ignoredValuesMode = 
		IgnoredValuesHandlingMode.MAP_TO_BLACK;		

	/**
	 * Generated result image.
	 */
	@Parameter( label= "Result Image", dataIOOrder = 0,
		direction = Parameter.Direction.OUT, description = "Resulting image.")
	private transient MTBImage resultImg = null;

	/**
	 * Map for value mappings.
	 */
	private HashMap<Double, Double> map = null; 
	
	/**
	 * Default constructor.
	 *  @throws ALDOperatorException	Thrown in case of failure.
	 */
	public GrayscaleToGrayscaleMapping() throws ALDOperatorException {
		// nothing to do here
	}		

	/**
	 * Callback routine to change operator mode parameters.
	 */
	@SuppressWarnings("unused")
	private void switchOpModeParameters() {
		try {
			if (this.opMode == OperationMode.SINGLE_IMAGE) {
				if (this.hasParameter("inDir")) {
					this.removeParameter("inDir");
				}
				if (!this.hasParameter("inputImg")) {
					this.addParameter("inputImg");
				}
				if (!this.hasParameter("filePath")) {
					this.addParameter("filePath");
				}
			}
			else if (this.opMode == OperationMode.DIRECTORY) {
					if (this.hasParameter("inputImg")) {
							this.removeParameter("inputImg");
					}
					if (this.hasParameter("filePath")) {
						this.removeParameter("filePath");
					}
					if (!this.hasParameter("inDir")) {
							this.addParameter("inDir");
					}
			}
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ALDOperatorException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Returns the result image.
	 * @return Result image.
	 */
	public MTBImage getResultImage() {
		return this.resultImg;
	}
	
	/**
	 * Set input image.
	 * @param inimg	Input grayscale image.
	 */
	public void setInputImage(MTBImage inimg) {
		this.inputImg = inimg;
	}

	/**
	 * Specify mapping file.
	 * @param s 	File path.
	 */
	public void setMappingFile(String s) {
		this.filePath = new ALDFileString(s);
	}
	
	/**
	 * Specfiy additional mask of pixels to ignore.
	 * @param bImg	Binary mask.
	 */
	public void setIgnoreMask(MTBImageByte bImg) {
		this.ignoreMask = bImg;
	}

	@SuppressWarnings("unused")
	@Override
  public void validateCustom() throws ALDOperatorException {
		// nothing to do here
	}
	
	/**
	 * This method does the actual work.
	 * @throws ALDOperatorException 			Thrown in case of failure.
	 * @throws ALDProcessingDAGException 	Thrown in case of failure.
	 */
  @Override
	protected void operate() 
			throws ALDOperatorException, ALDProcessingDAGException {
  	
  	// figure out range of image values in directory mode
  	if (this.opMode.equals(OperationMode.DIRECTORY)) {
  		
			DirectoryTree rootDirTree = 
					new DirectoryTree(this.inDir.getDirectoryName(), false);

			this.fireOperatorExecutionProgressEvent(
					new ALDOperatorExecutionProgressEvent(this, classID 
							+ " processing directory <" 
							+ this.inDir.getDirectoryName() + ">..."));
			Vector<String> allFiles = rootDirTree.getFileList();
			ImageReaderMTB imRead = new ImageReaderMTB();
			ImageWriterMTB imWrite = new ImageWriterMTB();

			// iterate over all files
			for (String file : allFiles) {
				
				if (file.endsWith(".txt"))
					continue;
				
				try {
					imRead.setFileName(file);
					imRead.runOp();

					this.fireOperatorExecutionProgressEvent(
							new ALDOperatorExecutionProgressEvent(this, classID 
									+ " --> processing image <" + file + ">..."));

					MTBImage img = imRead.getResultMTBImage();
					String fileRoot = 
							ALDFilePathManipulator.removeExtension(file);
					
		    	// read mapping file
		    	this.map = new HashMap<>(); 
		    	try {
		  			BufferedReader bf = new BufferedReader(
		  					new FileReader(new File(fileRoot + "-map.txt")));
		  			String line;
		  			while ((line = bf.readLine()) != null) {
		  				String[] parts = line.split("\t");
		  				this.map.put(new Double(parts[0]), new Double(parts[1]));
		  			}
		  			bf.close();
		  		} catch (FileNotFoundException e) {
		  			// TODO Auto-generated catch block
		  			e.printStackTrace();
		  		} catch (IOException e) {
		  			// TODO Auto-generated catch block
		  			e.printStackTrace();
		  		}
					
					MTBImage result = this.processImage(img);
					
					// save result to directory
					imWrite.setFileName(fileRoot + "-transformed.tif");
					imWrite.setInputMTBImage(result);
					imWrite.runOp();
					
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (FormatException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (DependencyException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ServiceException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
  	}
  	// single image mode
  	else {
  		
    	// read mapping file
    	this.map = new HashMap<>(); 
    	try {
  			BufferedReader bf = new BufferedReader(
  					new FileReader(new File(this.filePath.getFileName())));
  			String line;
  			while ((line = bf.readLine()) != null) {
  				String[] parts = line.split("\t");
  				this.map.put(new Double(parts[0]), new Double(parts[1]));
  			}
  			bf.close();
  		} catch (FileNotFoundException e) {
  			// TODO Auto-generated catch block
  			e.printStackTrace();
  		} catch (IOException e) {
  			// TODO Auto-generated catch block
  			e.printStackTrace();
  		}
    	
  		this.resultImg = this.processImage(this.inputImg);
  	}
	}	
  
  /**
   * Convert a single image.
   * 
   * @param img		Image to convert.
   * @return	Result image.
   */
  protected MTBImage processImage(MTBImage img) {
  	
		int height = img.getSizeY();
		int width = img.getSizeX();
		int depth = img.getSizeZ();
		int times = img.getSizeT();
		int channels = img.getSizeC();
		
		// allocate result image
		MTBImage result = MTBImage.createMTBImage(
				width, height, depth, times, channels, MTBImageType.MTB_DOUBLE);

		// fill result image
		double valueD;
		double valueN;
		for (int c = 0; c < channels; ++c) {
			for (int t = 0; t < times; ++t) {
				for (int z = 0; z < depth; ++z) {
					for (int y = 0; y < height; ++y) {
						for (int x = 0; x < width; ++x) {
							valueD = img.getValueDouble(x, y, z, t, c); 
							valueN = 0;
							if (   this.ignoreMask != null
									&& this.ignoreMask.getValueInt(x, y, z, t, c) > 0) {
								switch(this.ignoredValuesMode)
								{
								case LEAVE_UNTOUCHED:
									valueN = valueD;
									break;
								case MAP_TO_BLACK:
								default:
									valueN = 0;
									break;
								}								
							}
							else {
								if (this.map.containsKey(new Double(valueD)))
									valueN = this.map.get(new Double(valueD)).doubleValue();
							}
							result.putValueDouble(x, y, z, t, c, valueN); 
						}
					}
				}
			}
		}
		result.setTitle("Transformed image of <" + img.getTitle() + ">");
		return result;  	
  }
}
