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

package de.unihalle.informatik.MiToBo.apps.actinAnalysis;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Vector;

import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException.OperatorExceptionType;
import de.unihalle.informatik.Alida.helpers.ALDFilePathManipulator;
import de.unihalle.informatik.Alida.operator.ALDOperator;
import de.unihalle.informatik.Alida.operator.events.ALDOperatorExecutionProgressEvent;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.ALDDerivedClass;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.MiToBo.core.datatypes.images.*;
import de.unihalle.informatik.MiToBo.features.FeatureCalculator;
import de.unihalle.informatik.MiToBo.features.TileFeatureCalculator;
import de.unihalle.informatik.MiToBo.features.texture.FeatureCalculatorHaralickMeasures;
import de.unihalle.informatik.MiToBo.features.texture.FeatureCalculatorHaralickMeasures.HaralickDirection;
import de.unihalle.informatik.MiToBo.gui.MTBTableModel;
import de.unihalle.informatik.MiToBo.io.dirs.DirectoryTree;
import de.unihalle.informatik.MiToBo.io.images.ImageReaderMTB;
import de.unihalle.informatik.MiToBo.io.images.ImageWriterMTB;

/**
 * Operator for extracting Haralick texture measures from 
 * co-occurrence matrices as features for the {@link ActinAnalyzer2D}.
 * 
 * @see FeatureCalculatorHaralickMeasures 
 * @author moeller
 */
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.SWING,
	level=Level.STANDARD, allowBatchMode=false)
@ALDDerivedClass
public class ActinFeatureExtractorHaralickMeasures 
	extends FilamentFeatureExtractor {

	/**
	 * Identifier string for this operator class.
	 */
	private static final String operatorID = 
			"[ActinFeatureExtractorHaralickMeasures]";

	/**
	 * Default directions for Haralick features.
	 */
	private static Vector<HaralickDirection> defaultDirections;
	
	// this initialization block is necessary to set default directions
	static {
		defaultDirections = 
			new Vector<FeatureCalculatorHaralickMeasures.HaralickDirection>();
		defaultDirections.add(HaralickDirection.EAST);
		defaultDirections.add(HaralickDirection.NORTH_EAST);
		defaultDirections.add(HaralickDirection.NORTH);
		defaultDirections.add(HaralickDirection.NORTH_WEST);		
	}

	/**
	 * Distance.
	 */
	@Parameter( label= "Haralick distance", required = true,
		direction = Parameter.Direction.IN, dataIOOrder=1,  
		mode=ExpertMode.STANDARD, description = "Desired distance.")
	protected int distance= 4;

	/**
	 * Directions for calculating cooccurrence matrices.
	 */
	@Parameter(label = "Haralick directions", required = true, 
		dataIOOrder = 2, direction = Parameter.Direction.IN, 
		mode = ExpertMode.STANDARD, description = "Directions for matrices.")
	protected Vector<HaralickDirection> directions = defaultDirections;

	/**
	 * Flag for isotropic calculations.
	 * <p>
	 * If flag is true the average of each feature for the four directions 
	 * EAST, NORTH_EAST, NORTH and NORTH_WEST is taken as result. 
	 * But, note that this causes the computation time to be increased by 
	 * a factor of four as well.
	 */
	@Parameter( label= "Isotropic calculations", required = true,
		direction = Parameter.Direction.IN, dataIOOrder=3,  
		description = "Flag to enable isotropic calculations.",
		mode=ExpertMode.ADVANCED)
	protected boolean isotropicCalcs = false;

	/**
	 * Default constructor.
	 * @throws ALDOperatorException Thrown in case of failure. 
	 */
	public ActinFeatureExtractorHaralickMeasures() 
			throws ALDOperatorException {
		// nothing to be done here
	}
	
	/**
	 * Specify distance for calculating cooccurrence matrices.
	 * @param d		Pixel distance to apply.
	 */
	public void setDistance(int d) {
		this.distance = d;
	}
	
	/**
	 * Set directions for which to calculate cooccurence matrices.
	 * @param dirs	List of directions.
	 */
	public void setHaralickDirections(Vector<HaralickDirection> dirs) {
		this.directions = dirs;
	}

	/**
	 * Enable/disable isotropic calculations.
	 * @param flag	If true, isotropic calculations are enabled.
	 */
	public void setFlagIsotropicCalculations(boolean flag) {
		this.isotropicCalcs = flag;
	}
	
	@Override
	protected void calculateFeatures() 
		throws ALDOperatorException, ALDProcessingDAGException {
		
		MTBImage img;
		ImageReaderMTB iRead = new ImageReaderMTB();
		ImageWriterMTB iWrite = new ImageWriterMTB();

		// check if vector of directions is null or empty, 
		// if so init with default
		if (this.directions == null || this.directions.isEmpty()) {
			this.directions = new 
				Vector<FeatureCalculatorHaralickMeasures.HaralickDirection>();
			this.directions.add(HaralickDirection.EAST);
		}
		
  	if (this.verbose.booleanValue())
  		System.out.println(operatorID + " Calculating features...");
  	this.fireOperatorExecutionProgressEvent(
     	new ALDOperatorExecutionProgressEvent(this,
     		" calculating features..."));
	
		// initialize the feature calculator
		Vector<FeatureCalculator> featureOps = 
				new Vector<FeatureCalculator>();
		FeatureCalculatorHaralickMeasures fOp = 
				new FeatureCalculatorHaralickMeasures();
		fOp.doIsotropicCalcutations(this.isotropicCalcs);
		fOp.setDistance(this.distance);
		fOp.setDirections(this.directions);
		fOp.setFlagThrinkMatrix(true);
		featureOps.add(fOp);

		TileFeatureCalculator tCalc = 
			new TileFeatureCalculator(this.tileSizeX, this.tileSizeY,
				this.tileShiftX, this.tileShiftY);
		tCalc.setFeatureOperators(featureOps);
		MTBTableModel features;
		int tilesX = -1, tilesY = -1;
		
		if (this.verbose.booleanValue())
  		System.out.println(operatorID + " Image directory= "
  				+ this.imageDir);
		
		DirectoryTree dirTree = 
			new DirectoryTree(this.imageDir.getDirectoryName());
		Vector<String> imageList = dirTree.getFileList();
		for (String file : imageList) {
			if (this.verbose.booleanValue())
				System.out.println("\t Processing file " + file + "...");
			this.fireOperatorExecutionProgressEvent(
				new ALDOperatorExecutionProgressEvent(this,
					" processing file " + file + "..."));
			String basename = ALDFilePathManipulator.getFileName(file);

			try {
        iRead.setFileName(file);
				iRead.runOp(HidingMode.HIDDEN);
				img = iRead.getResultMTBImage(); 
				if (this.imageWidth == -1)
					this.imageWidth = img.getSizeX();
				if (this.imageHeight == -1)
					this.imageHeight = img.getSizeY();
      } catch (Exception e) {
      	System.err.println(operatorID + " Error reading file, " + 
      			"skipping " + file + "...");
      	continue;
      }
			
			// check if mask available
			MTBImage maskImage = null;
			String maskName = "undefined";
			maskImage = this.readMaskImage(basename, 
				0, 0, this.imageWidth-1, this.imageHeight-1);
			if (maskImage != null)
				maskName = maskImage.getProperty("Filename");
			
    	if (this.verbose.booleanValue())
    		System.out.print(
    				"\t\t - initializing and running tile calculator...");
   		fireOperatorExecutionProgressEvent(
      	new ALDOperatorExecutionProgressEvent(this,
      		" initializing and running tile calculator..."));
			
			tCalc.setInputImage(img);
			if (maskImage != null)
				tCalc.setMask(maskImage);
			tCalc.runOp(HidingMode.HIDDEN);

    	if (this.verbose.booleanValue())
    		System.out.println(" ...done!");
    	fireOperatorExecutionProgressEvent(
      	new ALDOperatorExecutionProgressEvent(this, " ...done!"));

			// get number of resulting tiles and check that all images are same
			if (tilesX == -1 && tilesY == -1) {
				tilesX = tCalc.getTileCountX();
				tilesY = tCalc.getTileCountY();
			}
			else {
				if (   tilesX != tCalc.getTileCountX()
						|| tilesY != tCalc.getTileCountY()) {
					throw new ALDOperatorException(
						OperatorExceptionType.OPERATE_FAILED, 
						operatorID + " different tile sizes for images, exiting...");
				}
			}
			// save result data to output directory
			String outname= this.outDir + "/" +basename+ "-features.txt";
			String outhistname= 
					this.outDir + "/" +basename+ "-features-config";
			String outimgname= this.outDir + "/" +basename+ "-features.tif";
			if (this.verbose.booleanValue())
				System.out.println("\t\t - saving features to " + outname);
			String eventMsg = " saving features...";
			fireOperatorExecutionProgressEvent(
				new ALDOperatorExecutionProgressEvent(this, eventMsg));
			
			// try to save operator configuration
			try {
				ALDOperator.writeHistory(tCalc.getResult(), outhistname);
			} catch(Exception e) {
				e.printStackTrace();
			}
			
			features = tCalc.getResult().getResultTable();
			StringBuffer[] tab = features.tableToString();
      try {
  			FileWriter ow;
	      ow = new FileWriter(new File(outname).getPath());
	      // write all relevant data into header
	      ow.write("# filename: " + file + "\n");
	      ow.write("# maskfile: " + maskName + "\n");
	      ow.write("# tileSizeX: " + this.tileSizeX + "\n");
	      ow.write("# tileSizeY: " + this.tileSizeY + "\n");
	      ow.write("# tileShiftX: " + this.tileShiftX + "\n");
	      ow.write("# tileShiftY: " + this.tileShiftY + "\n");
	      ow.write("# tileCountX: " + tilesX + "\n");
	      ow.write("# tileCountY: " + tilesY + "\n");
	      ow.write("# tileCountTotal: " + tilesX * tilesY + "\n");
	      ow.write("# invalidTiles: " 
	      	+ tCalc.getResult().getInvalidTilesNum() + "\n");
				for (int i=0;i<tab.length;++i)
					ow.write(tab[i].toString());
				ow.close();
      } catch (IOException e1) {
      	throw new ALDOperatorException(
      		OperatorExceptionType.OPERATE_FAILED,	operatorID 
      			+ " could not save features to \"" + outname + "\"!");
      }
			if (this.verbose.booleanValue())
				System.out.println("\t\t - saving image to " + outimgname);
			fireOperatorExecutionProgressEvent(
      	new ALDOperatorExecutionProgressEvent(this, " saving image..."));
			iWrite.setInputMTBImage(tCalc.getResultImage());
			iWrite.setFileName(outimgname);
			iWrite.runOp();
		}
	}
}

/*BEGIN_MITOBO_ONLINE_HELP

This operator calculates a set of texture features from the images,
particularly Haralick texture measures.
<ul>
<li><p><b>input:</b>
<ul>
<li><p><i>Image directory</i>:<br> directory where the images are read 
  from, all image files are considered;<br> please refer to the webpage 
  for further information on how the file names should be 
  formatted </p></li>
<li><p><i>Mask directory</i>:<br> directory where the segmentation 
	information for the images is read from; the directory can be identical 
	to the image directory</p></li>
<li><p><i>Mask format</i>:<br> expected format of the segmentation data 
  files
	<ul>
	<li>LABEL_IMAGE:<br> a gray-scale image is expected where the area of 
	  each cell is marked with a single unique gray-scale value;<br>
		the files should share the names of the input image files and have 
		the	ending "-mask.tif"
	<li>IJ_ROIS:<br> an ImageJ 1.x file of ROI manager regions is 
		expected;<br> the files should share the names of the input image 
		files and have the ending "-mask.zip" or "-mask.roi"
	</ul>
<li><p><i>Output and working directory</i>:<br> directory for 
  intermediate and final results
</ul>
</ul>

<p>
For more details about the operator and the corresponding 
ActinAnalyzer2D refer to its webpage: 
<a href="http://www2.informatik.uni-halle.de/agprbio/mitobo/index.php/Applications/ActinAnalyzer2D">
http://www2.informatik.uni-halle.de/agprbio/mitobo/index.php/Applications/ActinAnalyzer2D</a>.

END_MITOBO_ONLINE_HELP*/
