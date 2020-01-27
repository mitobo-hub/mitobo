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

package de.unihalle.informatik.MiToBo.apps.cellMorphology;

import java.awt.Color;
import java.io.File;
import java.util.HashMap;
import java.util.Set;
import java.util.Vector;

import de.unihalle.informatik.Alida.dataio.ALDDataIOManagerCmdline;
import de.unihalle.informatik.Alida.dataio.provider.ALDDataIOCmdline;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException.OperatorExceptionType;
import de.unihalle.informatik.Alida.helpers.ALDFilePathManipulator;
import de.unihalle.informatik.Alida.operator.events.ALDOperatorExecutionProgressEvent;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.MiToBo.core.datatypes.images.*;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.operator.*;
import de.unihalle.informatik.MiToBo.gui.MTBTableModel;
import de.unihalle.informatik.MiToBo.io.dirs.DirectoryTree;
import de.unihalle.informatik.MiToBo.io.images.ImageReaderMTB;
import de.unihalle.informatik.MiToBo.io.images.ImageWriterMTB;

/**
 * This operator maps numerical feature values to objects in an indexed image.
 * <p>
 * The input images are assumed to be label images with the label corresponding
 * to an object ID in the given feature table file. The IDs are assumend to be
 * larger than zero.<br>
 * For each image the range of feature values in the selected column of the
 * feature table is determined, and the values are linearly mapped to the color 
 * range defined by the given minimal and maximal color.<br>
 * Finally, each pixel an image with a value larger than zero gets the color 
 * value corresponding to the color of its feature value as derived from the
 * feature value in the corresponding row of the feature table. 
 *
 * @author moeller
 */
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.ALL,
		level=Level.APPLICATION, allowBatchMode = false)
public class PaCeQuant_FeatureColorMapper extends MTBOperator {

	/**
	 * Class identifier.
	 */
	private static final String classID = "[PaCeQuant_FeatureColorMapper]";
	
	/**
	 * Input data.
	 */
	@Parameter(label = "Input Data", required = true,
		dataIOOrder = 1, direction = Parameter.Direction.IN,	
		description = "Input experiment directory and column selection, " 
				+ "all sub-folders named 'results' will be considered")
	private PaCeQuant_FeatureColorMapperInputData inData = null;
	
	/**
	 * Color for minimal value.
	 */
	@Parameter( label= "Color of Range Minimum", required = true, dataIOOrder = 7, 
		direction = Parameter.Direction.IN, description = "Color of small values.")
	protected Color minColor = Color.RED;	

	/**
	 * Color for maximal value.
	 */
	@Parameter( label= "Color of Range Maximum", required = true, dataIOOrder = 9, 
		direction = Parameter.Direction.IN, description = "Color of large values.")
	protected Color maxColor = Color.YELLOW;	

	/**
	 * Minimal value threshold.
	 */
	@Parameter( label= "Minimal value threshold", required = true, dataIOOrder = 10, 
		direction = Parameter.Direction.IN, description = "Values smaller than this one are ignored.")
	protected double minValue = Double.NEGATIVE_INFINITY;	

	/**
	 * Maximal value threshold.
	 */
	@Parameter( label= "Maximal value threshold", required = true, dataIOOrder = 11, 
		direction = Parameter.Direction.IN, description = "Values larger than this one are ignored.")
	protected double maxValue = Double.POSITIVE_INFINITY;	

	/**
	 * Default constructor.
	 *  @throws ALDOperatorException	Thrown in case of failure.
	 */
	public PaCeQuant_FeatureColorMapper() 
			throws ALDOperatorException {
	}		

	/**
	 * Set input data.
	 * @param indir	Input experiment folder.
	 * @param id	Selected column.
	 */
	public void setInputData(String indir, int id) {
		this.inData = new PaCeQuant_FeatureColorMapperInputData(indir, id);
	}

	/**
	 * Color for range minimum.
	 * @param c		Color to use.
	 */
	public void setColorRangeMinimum(Color c) {
		this.minColor = c;
	}

	/**
	 * Color for range maximum.
	 * @param c		Color to use.
	 */
	public void setColorRangeMaximum(Color c) {
		this.maxColor = c;
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
  @SuppressWarnings("unused")
	@Override
	protected void operate() 
			throws ALDOperatorException, ALDProcessingDAGException {
  	
  		DirectoryTree tableDirTree = 
  			new DirectoryTree(this.inData.getDirectoryName(), true);
  		this.fireOperatorExecutionProgressEvent(
  				new ALDOperatorExecutionProgressEvent(this, classID 
  					+ " processing table files in directory <" 
  						+ this.inData.getDirectoryName() + ">..."));
  		Vector<String> tabFiles = tableDirTree.getFileList();

	  	// search for relevant table files
  		HashMap<String, String> tabsToImgs = new HashMap<>();
  		for (String tab : tabFiles) {
			if (		tab.endsWith("table.txt")
					&& !tab.endsWith("lobe-table.txt")
					&& 	tab.contains(File.separator + "results" + File.separator)) {
  				String dir = ALDFilePathManipulator.getPath(tab);
  				DirectoryTree localDir = new DirectoryTree(dir, false);

					String shortTab = ALDFilePathManipulator.getFileName(tab);
					String imgFile = null;
					int maxLength = 0;
  				for (String img : localDir.getFileList()) {
						// don't consider other than image files
						if (!img.endsWith(".tif") || !img.contains("grayscale-result"))
							continue;
  					String shortImg = ALDFilePathManipulator.getFileName(img);
	  				int minLength = Math.min(shortTab.length(), shortTab.length());
  					for (int i = 0; i < minLength; i++) {
  						if (shortImg.charAt(i) != shortTab.charAt(i)) {
  							if (i+1 > maxLength) {
  								maxLength = i+1;
  								imgFile = img;
	  						}
  							break;
  						}
  					}
					}
					if (imgFile == null)
						this.fireOperatorExecutionProgressEvent(
							new ALDOperatorExecutionProgressEvent(this, classID 
								+ " -> [WARNING] no match found for " + tab + "!!!"));
					else {
						tabsToImgs.put(tab, imgFile);
		  	  	this.fireOperatorExecutionProgressEvent(
  	  				new ALDOperatorExecutionProgressEvent(this, classID 
								+ " -> found match: " + tab + " <-> " + imgFile));
					}
  			}
  		}

			int minR = this.minColor.getRed();
			int minG = this.minColor.getGreen();
			int minB = this.minColor.getBlue();

			int maxR = this.maxColor.getRed();
			int maxG = this.maxColor.getGreen();
			int maxB = this.maxColor.getBlue();

			MTBTableModel tm = null;
			double val;
			double vmin = Double.MAX_VALUE;
			double vmax = Double.MIN_VALUE;
  		Set<String> imgs = tabsToImgs.keySet();
  		try {
 		
    		// get a provider to read table models
  			ALDDataIOManagerCmdline mc = ALDDataIOManagerCmdline.getInstance();
  			ALDDataIOCmdline pc = (ALDDataIOCmdline)mc.getProvider(
				MTBTableModel.class, ALDDataIOCmdline.class);
  		
	    	this.fireOperatorExecutionProgressEvent(
    			new ALDOperatorExecutionProgressEvent(this, classID 
    				+ " parsing tables to determine value range of feature " 
    					+ "in column " + this.inData.getColumnID() + "..."));
  		
				// parse all tables and search for minimum and maximum
				Set<String> tableNames = tabsToImgs.keySet();
  			for (String tab: tableNames) {
  			
     			this.fireOperatorExecutionProgressEvent(
     				new ALDOperatorExecutionProgressEvent(this, classID 
     					+ " -> " + tab + "..."));

	  			tm = (MTBTableModel)pc.readData(null, MTBTableModel.class, "@" + tab);
  			
  				// get data of selected column
  				for (int i=0;i<tm.getRowCount();++i) {
						val = Double.valueOf((String)tm.getValueAt(i, 
						this.inData.getColumnID())).doubleValue();

						// check thresholds
						if (val > this.maxValue || val < this.minValue)
							continue; 

  					if (val > vmax) vmax = val;
  					if (val < vmin) vmin = val;
  				}
  			} 
				double range = vmax - vmin;
			
	    	this.fireOperatorExecutionProgressEvent(
   				new ALDOperatorExecutionProgressEvent(this, classID 
						+ "    => vmin: " + vmin + " / vmax: " + vmax));
			
		   	this.fireOperatorExecutionProgressEvent(
    			new ALDOperatorExecutionProgressEvent(this, classID 
    				+ " generating color maps..."));

				// process all images, i.e. pre-compute colors and then generate result
				ImageReaderMTB imRead = new ImageReaderMTB();
				ImageWriterMTB imWrite = new ImageWriterMTB();
				MTBImage img;
				MTBImageRGB result;
				String subfolderName;
				File subfolder;
  			for (String tab: tableNames) {

					String imgFile = tabsToImgs.get(tab);

					this.fireOperatorExecutionProgressEvent(
						new ALDOperatorExecutionProgressEvent(this, classID 
							+ " -> processing " + tab + " <-> " + imgFile + "..."));

					imRead.setFileName(imgFile);
					imRead.runOp();
					img = imRead.getResultMTBImage();
				
  				tm = (MTBTableModel)pc.readData(null, MTBTableModel.class, "@" + tab);

					// create sub-folder
					subfolderName = "featureColorMaps-" + 
						tm.getColumnName(this.inData.getColumnID()).split("_")[0];
					subfolder = new File(ALDFilePathManipulator.getPath(imgFile) 
						+ File.separator + subfolderName);
					if (!subfolder.exists()) {
						if (!subfolder.mkdir()) 
							throw new ALDOperatorException( 
								OperatorExceptionType.OPERATE_FAILED, "[PaCeQuant_FeatureColorMapper] "  
									+ " could not init result folder for color maps...!"); 
					}

	  			// get data of selected column
  				HashMap<Integer, Double> featureVals = new HashMap<>();
  				for (int i=0;i<tm.getRowCount();++i) {
  					featureVals.put(Integer.valueOf((String)tm.getValueAt(i, 0)),
  						Double.valueOf((String)tm.getValueAt(i, this.inData.getColumnID())));
  				}

	  			int height = img.getSizeY();
  				int width = img.getSizeX();
  				int depth = img.getSizeZ();
	  			int times = img.getSizeT();
  				int channels = img.getSizeC();
			
  				// allocate result image
  				result = (MTBImageRGB)img.duplicate().convertType(MTBImageType.MTB_RGB, true);
					result.fillBlack();

	  			int id, valueI, newR, newG, newB;
  				double featVal, ratio;
  				HashMap<Integer, Color> colors = new HashMap<>();
  				for (int i=0;i<tm.getRowCount();++i) {
  					// object ID
	  				id = Integer.valueOf((String)tm.getValueAt(i, 0)).intValue();
  				
  					// hack to compensate for wrong IDs due to little endian bug!
  					if (id >= 256)
  						id = id/256;

	  				// feature value
  					featVal = featureVals.get(id).doubleValue();

						// interpolate new color, black if out of range
						if (featVal < this.minValue || featVal > this.maxValue) {
							colors.put(id, new Color(0, 0, 0));
						}
						else {
							ratio = (featVal - vmin) / range;
  						newR = minR + (int)(ratio*(maxR - minR) + 0.5);
	  					newG = minG + (int)(ratio*(maxG - minG) + 0.5);
  						newB = minB + (int)(ratio*(maxB - minB) + 0.5);
							colors.put(id, new Color(newR, newG, newB));
						}
  				}
			
	  			// fill result image
  				for (int c = 0; c < channels; ++c) {
  					for (int t = 0; t < times; ++t) {
  						for (int z = 0; z < depth; ++z) {
  							for (int y = 0; y < height; ++y) {
  								for (int x = 0; x < width; ++x) {
  									valueI = img.getValueInt(x, y, z, t, c); 
  									// hack to compensate for wrong IDs due to little endian bug!
	  			  				if (valueI >= 256)
  				  					valueI = valueI/256;
  									if (!colors.containsKey(valueI)) {
  										result.putValueR(x, y, z, t, c, valueI); 
  										result.putValueG(x, y, z, t, c, valueI); 
  										result.putValueB(x, y, z, t, c, valueI);
  									}
	  								else {
  										result.putValueR(x, y, z, t, c, colors.get(valueI).getRed()); 
  										result.putValueG(x, y, z, t, c, colors.get(valueI).getGreen()); 
  										result.putValueB(x, y, z, t, c, colors.get(valueI).getBlue());
  									}
  								}
  							}
	  					}
  					}
  				}
  				// add title to image
	  			String colorString = "[" + minR + "," + minG + "," + minB + "] -> " 
  					+ "[" + maxR + "," + maxG + "," + maxB + "]";
  				result.setTitle(tm.getColumnName(this.inData.getColumnID()) + " - FeatureMap " 
  					+ colorString	+ " of <"	+ img.getTitle() + ">");
  			
	  			// save file
					String fileName = ALDFilePathManipulator.getFileName(imgFile);
					fileName = fileName + "-colorMap-" + tm.getColumnName(this.inData.getColumnID()) + ".tif";
					String outfile = ALDFilePathManipulator.getPath(imgFile) + File.separator + subfolderName
					  + File.separator + fileName;

					this.fireOperatorExecutionProgressEvent(
						new ALDOperatorExecutionProgressEvent(this, classID 
							+ " -> ... saving result to " + outfile + "..."));
	
  				imWrite.setFileName(outfile);
	  			imWrite.setInputMTBImage(result);
  				imWrite.runOp();
				}
	  	} catch (Exception e) {
  			throw new ALDOperatorException(OperatorExceptionType.OPERATE_FAILED, 
  				classID + " Processing input image and table failed, exiting!\n" 
  					+ e.getMessage());
  		}
	}	
}
