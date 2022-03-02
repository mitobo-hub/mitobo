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

package de.unihalle.informatik.MiToBo.apps.cellMorphology.paceQuant;

import java.awt.Color;
import java.io.File;
import java.util.HashMap;
import java.util.Set;
import java.util.Vector;

import org.jfree.chart.LegendItem;

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
import de.unihalle.informatik.MiToBo.apps.cellMorphology.paceQuant.provider.PaCeQuant_FeatureColorMapperInputData;
import de.unihalle.informatik.MiToBo.core.datatypes.images.*;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.operator.*;
import de.unihalle.informatik.MiToBo.gui.MTBTableModel;
import de.unihalle.informatik.MiToBo.io.dirs.DirectoryTree;
import de.unihalle.informatik.MiToBo.io.images.ImageReaderMTB;
import de.unihalle.informatik.MiToBo.io.images.ImageWriterMTB;
import ij.ImagePlus;
import ij.process.ImageProcessor;

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
 * <p>
 * The minimal and maximal size threshold can be used to exclude too small or 
 * too large cells from mapping. The thresholds are always applied to the 
 * cell areas and sizes below the minimal or above the maximal threshold are 
 * ignored.
 * <p>
 * All images ending with the given suffix are considered as potential 
 * candidates to be matched to found table files. The default suffix 
 * "grayscale-result.tif" has been set to allow for maximmal compatibility 
 * with PaCeQuant. It is assumed that corresponding table and image files share
 * the same file prefix, i.e., the operator seeks to find matching files by 
 * searching for the largest common prefix between table and image file name.  
 *
 * @author moeller
 */
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.ALL,
		level=Level.APPLICATION, allowBatchMode = false)
public class FeatureColorMapper extends MTBOperator {

	/**
	 * Class identifier.
	 */
	private static final String classID = "[PaCeQuant_FeatureColorMapper]";

	/**
	 * Input data.
	 */
	@Parameter(label = "Input Data", required = true, dataIOOrder = 1, 
			direction = Parameter.Direction.IN, 
			description = "Input experiment directory and column selection, "
					+ "all sub-folders named 'results' will be considered")
	private PaCeQuant_FeatureColorMapperInputData inData = null;

	/**
	 * Input image suffix.
	 */
	@Parameter(label = "Input image suffix", required = true, dataIOOrder = 2, 
			direction = Parameter.Direction.IN, 
			description = "Only images matching this suffix are processed.")
	protected String inputImgSuffix = "grayscale-result.tif";

	/**
	 * Color for minimal value.
	 */
	@Parameter(label = "Color of Range Minimum", required = true, dataIOOrder = 7, 
			direction = Parameter.Direction.IN, description = "Color of small values.")
	protected Color minColor = Color.RED;

	/**
	 * Color for maximal value.
	 */
	@Parameter(label = "Color of Range Maximum", required = true, dataIOOrder = 9, 
			direction = Parameter.Direction.IN, description = "Color of large values.")
	protected Color maxColor = Color.YELLOW;

	/**
	 * Minimal size threshold.
	 */
	@Parameter(label = "Minimal size threshold", required = true, dataIOOrder = 10, 
			direction = Parameter.Direction.IN, 
			description = "Values smaller than this one are ignored.")
	protected double minSizeValue = Double.NEGATIVE_INFINITY;

	/**
	 * Maximal value threshold.
	 */
	@Parameter(label = "Maximal size threshold", required = true, dataIOOrder = 11, 
			direction = Parameter.Direction.IN, 
			description = "Values larger than this one are ignored.")
	protected double maxSizeValue = Double.POSITIVE_INFINITY;

	/**
	 * Default constructor.
	 * 
	 * @throws ALDOperatorException Thrown in case of failure.
	 */
	public FeatureColorMapper() 
			throws ALDOperatorException {
	}		

	/**
	 * Set input data.
	 * @param indir	Input experiment folder.
	 * @param ids	Selected columns.
	 */
	public void setInputData(String indir, int[] ids) {
		this.inData = new PaCeQuant_FeatureColorMapperInputData(indir, ids);
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
	
	private transient int minR;
	private transient int minG;
	private transient int minB;
	private transient	int maxR;
	private transient	int maxG;
	private transient	int maxB;

	
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
  		int minLength, maxLength;
  		DirectoryTree localDir;
  		String dir, shortImg, shortTab, imgFile;
  		HashMap<String, String> tabsToImgs = new HashMap<>();
  		for (String tab : tabFiles) {
  			if (		tab.endsWith("table.txt")
  					&& !tab.endsWith("lobe-table.txt")) {
  				dir = ALDFilePathManipulator.getPath(tab);
  				localDir = new DirectoryTree(dir, false);

  				shortTab = ALDFilePathManipulator.getFileName(tab);
  				imgFile = null;
  				maxLength = 0;
  				for (String img : localDir.getFileList()) {
  					// don't consider other than image files
//  					if (!img.endsWith(".tif") || !img.contains("grayscale-result"))
  					if (!img.endsWith(this.inputImgSuffix))
  						continue;
  					shortImg = ALDFilePathManipulator.getFileName(img);
  					minLength = Math.min(shortTab.length(), shortImg.length());
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

			this.minR = this.minColor.getRed();
			this.minG = this.minColor.getGreen();
			this.minB = this.minColor.getBlue();

			this.maxR = this.maxColor.getRed();
			this.maxG = this.maxColor.getGreen();
			this.maxB = this.maxColor.getBlue();

			int featuresToMap = this.inData.getColumnIDs().length;

			MTBTableModel tm = null;
			double val;

			double[] vMins = new double[featuresToMap];
			double[] vMaxs = new double[featuresToMap];
			double[] ranges = new double[featuresToMap];
			for (int i=0; i<featuresToMap; ++i) {
				vMins[i] = Double.MAX_VALUE;
				vMaxs[i] = Double.MIN_VALUE;
			}

			int columnIDArea = -1;
			double cellArea;

			// double vmin = Double.MAX_VALUE;
			// double vmax = Double.MIN_VALUE;
  		Set<String> imgs = tabsToImgs.keySet();
  		try {
 		
    		// get a provider to read table models
  			ALDDataIOManagerCmdline mc = ALDDataIOManagerCmdline.getInstance();
  			ALDDataIOCmdline pc = (ALDDataIOCmdline)mc.getProvider(
				MTBTableModel.class, ALDDataIOCmdline.class);
			
				this.fireOperatorExecutionProgressEvent(
					new ALDOperatorExecutionProgressEvent(this, classID 
						+ " parsing tables to determine value ranges of features..."));

				// parse all tables and search for minimum and maximum
				Set<String> tableNames = tabsToImgs.keySet();
  			for (String tab: tableNames) {
  			
     			this.fireOperatorExecutionProgressEvent(
     				new ALDOperatorExecutionProgressEvent(this, classID 
     					+ " -> " + tab + "..."));

	  			tm = (MTBTableModel)pc.readData(null, MTBTableModel.class, "@" + tab);
				
					// in the first table, figure out index of area column
					if (columnIDArea == -1) {
						for (int i=0; i<tm.getColumnCount(); ++i) {
							if (tm.getColumnName(i).contains("Area")) {
								columnIDArea = i;
								break;
							}
						}
					}

  				// get data of selected column
  				for (int i=0;i<tm.getRowCount();++i) {

						// check size thresholds
						cellArea = Double.valueOf(
								(String)tm.getValueAt(i, columnIDArea)).doubleValue();
						if (cellArea > this.maxSizeValue || cellArea < this.minSizeValue)
							continue; 

						for (int c=0; c<featuresToMap; ++c) {
							int columnID = this.inData.getColumnIDs()[c];

							val = Double.valueOf((String)tm.getValueAt(i, columnID)).doubleValue();

							if (val > vMaxs[c]) vMaxs[c] = val;
							if (val < vMins[c]) vMins[c] = val;
						}
  				}
  			} 
				for (int c=0; c<featuresToMap; ++c) {
					ranges[c] = vMaxs[c] - vMins[c];
				}

		   	this.fireOperatorExecutionProgressEvent(
    			new ALDOperatorExecutionProgressEvent(this, classID 
    				+ " generating color maps..."));

				// process all images, i.e. pre-compute colors and then generate result
				ImageReaderMTB imRead = new ImageReaderMTB();
				ImageWriterMTB imWrite = new ImageWriterMTB();
				
				File subfolder;
				HashMap<Integer, Color> colors;
				HashMap<Integer, Double> featureVals;
				Integer id, valueI;
				MTBImage img;
				MTBImageRGB result;
				String colorString, fileName, outfile, subfolderName;

				int height, width, depth, times, channels;
  			int newR, newG, newB, value;
				double featVal, ratio;

				for (String tab: tableNames) {

					imgFile = tabsToImgs.get(tab);

					this.fireOperatorExecutionProgressEvent(
						new ALDOperatorExecutionProgressEvent(this, classID 
							+ " -> processing " + tab + " <-> " + imgFile + "..."));

					imRead.setFileName(imgFile);
					imRead.runOp();
					img = imRead.getResultMTBImage();
				
  				tm = (MTBTableModel)pc.readData(null, MTBTableModel.class, "@" + tab);

					for (int col=0; col<featuresToMap; ++col) {
						int columnID = this.inData.getColumnIDs()[col];

						// create sub-folder
						subfolderName = "featureColorMaps-" + 
							tm.getColumnName(columnID).split("_")[0];
						subfolder = new File(ALDFilePathManipulator.getPath(imgFile) 
							+ File.separator + subfolderName);
						if (!subfolder.exists()) {
							if (!subfolder.mkdir()) 
								throw new ALDOperatorException( 
									OperatorExceptionType.OPERATE_FAILED, 
										"[PaCeQuant_FeatureColorMapper] "  
											+ " could not init result folder for color maps...!"); 
							
							// generate and save legend
							this.saveColorLegend(subfolder.getPath(), tm, col, vMins, vMaxs, ranges);
						}

		  			// get data of selected column
  					featureVals = new HashMap<>();
  					for (int i=0;i<tm.getRowCount();++i) {
  						featureVals.put(Integer.valueOf((String)tm.getValueAt(i, 0)),
  							Double.valueOf((String)tm.getValueAt(i, columnID)));
  					}

		  			height = img.getSizeY();
  					width = img.getSizeX();
  					depth = img.getSizeZ();
	  				times = img.getSizeT();
  					channels = img.getSizeC();
			
  					// allocate result image
  					result = (MTBImageRGB)img.duplicate().convertType(
  							MTBImageType.MTB_RGB, true);
						result.fillBlack();

  					colors = new HashMap<>();
  					for (int i=0;i<tm.getRowCount();++i) {
  						// object ID
	  					id = Integer.valueOf((String)tm.getValueAt(i, 0));
							cellArea = Double.valueOf(
									(String)tm.getValueAt(i, columnIDArea)).doubleValue();
  				
		  				// feature value
  						featVal = featureVals.get(id).doubleValue();

							// interpolate new color, black if out of range
							if (cellArea < this.minSizeValue || cellArea > this.maxSizeValue) {
								colors.put(id, new Color(0, 0, 0));
							}
							else {
								ratio = (featVal - vMins[col]) / ranges[col];
	  						newR = this.minR + (int)(ratio*(this.maxR - this.minR) + 0.5);
		  					newG = this.minG + (int)(ratio*(this.maxG - this.minG) + 0.5);
  							newB = this.minB + (int)(ratio*(this.maxB - this.minB) + 0.5);
								colors.put(id, new Color(newR, newG, newB));
							}
  					}
			
		  			// fill result image
  					for (int c = 0; c < channels; ++c) {
  						for (int t = 0; t < times; ++t) {
  							for (int z = 0; z < depth; ++z) {
  								for (int y = 0; y < height; ++y) {
  									for (int x = 0; x < width; ++x) {
  										value = img.getValueInt(x, y, z, t, c);
  										valueI = new Integer(value);
  										if (!colors.containsKey(valueI)) {
  											result.putValueR(x, y, z, t, c, value); 
  											result.putValueG(x, y, z, t, c, value); 
  											result.putValueB(x, y, z, t, c, value);
  										}
	  									else {
  											result.putValueR(x, y, z, t, c, 
  													colors.get(valueI).getRed()); 
  											result.putValueG(x, y, z, t, c, 
  													colors.get(valueI).getGreen()); 
  											result.putValueB(x, y, z, t, c, 
  													colors.get(valueI).getBlue());
  										}
  									}
  								}
	  						}	
  						}
  					}
  					// add title to image
	  				colorString = "[" + this.minR + "," + this.minG + "," + this.minB + "] -> " 
  						+ "[" + this.maxR + "," + this.maxG + "," + this.maxB + "]";
  					result.setTitle(tm.getColumnName(columnID) + " - FeatureMap " 
  						+ colorString	+ " of <"	+ img.getTitle() + ">");
  			
		  			// save file
						fileName = ALDFilePathManipulator.getFileName(imgFile);
						fileName = fileName + "-colorMap-" + tm.getColumnName(columnID) + ".tif";
						outfile = ALDFilePathManipulator.getPath(imgFile) + File.separator 
								+ subfolderName + File.separator + fileName;

						this.fireOperatorExecutionProgressEvent(
							new ALDOperatorExecutionProgressEvent(this, classID 
								+ " -> ... saving result to " + outfile + "..."));
	
  					imWrite.setFileName(outfile);
	  				imWrite.setInputMTBImage(result);
						imWrite.runOp();
					}
				}
	  	} catch (Exception e) {
				e.printStackTrace();
  			throw new ALDOperatorException(OperatorExceptionType.OPERATE_FAILED, 
  				classID + " Processing input image and table failed, exiting!\n" 
  					+ e.getMessage());
  		}
	}	
  
  /**
   * Function to generate and save color legends to sub-directories.
   * @throws ALDOperatorException 
   */
  private void saveColorLegend(String subfolder, MTBTableModel tm, int col,
  		double[] vMins, double[] vMaxs, double[] ranges) throws ALDOperatorException {

  	// create sub-folder
  	String legendFileName = subfolder + File.separator + "colorLegend.tif";

  	MTBImageRGB imgLegend = (MTBImageRGB)MTBImage.createMTBImage(
  			300, 1000, 1, 1, 1, MTBImageType.MTB_RGB);

  	double v, ratio;
  	int row, newR, newG, newB;
  	for (int y=0; y<1000; ++y) {
  			v = vMins[col] + y * 0.001 * ranges[col];
  			ratio = (v - vMins[col]) / ranges[col];
  			newR = this.minR + (int)(ratio*(this.maxR - this.minR) + 0.5);
  			newG = this.minG + (int)(ratio*(this.maxG - this.minG) + 0.5);
  			newB = this.minB + (int)(ratio*(this.maxB - this.minB) + 0.5);
  			
  			row = 999-y;
  			for (int x=0; x < 200; ++x) {
  				imgLegend.putValueR(x, row, newR);
  				imgLegend.putValueG(x, row, newG);
  				imgLegend.putValueB(x, row, newB);
  			}
  	}
  	ImageProcessor ip= imgLegend.getImagePlus().getProcessor();
		ip.setColor(Color.white);
		ip.moveTo(210, 995);
		ip.drawString(Double.toString(vMins[col]));
		ip.moveTo(210, 20);
		ip.drawString(Double.toString(vMaxs[col]));
		imgLegend = (MTBImageRGB)MTBImage.createMTBImage(new ImagePlus("Legend image", ip));

  	// save file
  	this.fireOperatorExecutionProgressEvent(
  			new ALDOperatorExecutionProgressEvent(this, classID 
  					+ " -> ... saving legends file to " + legendFileName + "..."));

  	ImageWriterMTB imWrite;
		try {
			imWrite = new ImageWriterMTB();
	  	imWrite.setFileName(legendFileName);
	  	imWrite.setInputMTBImage(imgLegend);
	  	imWrite.runOp();
		} catch (Exception e) {
			e.printStackTrace();
			throw new ALDOperatorException(OperatorExceptionType.OPERATE_FAILED, 
  				classID + " Could not write color legend file " + legendFileName  
  					+ e.getMessage());
		}
  }
}
