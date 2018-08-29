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

package de.unihalle.informatik.MiToBo.apps.cytoskeleton;

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
import de.unihalle.informatik.MiToBo.core.datatypes.images.*;
import de.unihalle.informatik.MiToBo.features.FeatureCalculator;
import de.unihalle.informatik.MiToBo.features.TileFeatureCalculator;
import de.unihalle.informatik.MiToBo.gui.MTBTableModel;
import de.unihalle.informatik.MiToBo.io.dirs.DirectoryTree;
import de.unihalle.informatik.MiToBo.io.images.ImageWriterMTB;

/**
 * Operator for extracting features for the {@link ActinAnalyzer2D}.
 * <p>
 * The features which are to be extracted by operators extending this
 * class should be specifically dedicated to filament like structures,
 * e.g., actin fiberes or microtubuli. Compared to 
 * {@link CytoskeletonFeatureExtractor} operators sub-classing this class 
 * are expected to work tile-wise on the input image.
 * 
 * @author moeller
 */
public abstract class CytoskeletonFeatureExtractorTiles 
	extends CytoskeletonFeatureExtractor {

	/**
	 * Default constructor.
	 * @throws ALDOperatorException Thrown in case of failure.
	 */
	public CytoskeletonFeatureExtractorTiles() throws ALDOperatorException {
		// nothing to be done here
	}
	
	/**
	 * Method to request operators for feature extraction to be applied.
	 * @return	List of operators to apply.
	 * @throws ALDOperatorException Thrown in case of failure.
	 */
	protected abstract Vector<FeatureCalculator> getFeatureOps() 
			throws ALDOperatorException; 
	
	@Override
	protected void calculateFeatures()  
		throws ALDOperatorException, ALDProcessingDAGException {
		
		MTBImage img;
		ImageWriterMTB iWrite = new ImageWriterMTB();

  	if (this.verbose.booleanValue())
  		System.out.println(this.operatorID + " Calculating features...");
  	this.fireOperatorExecutionProgressEvent(
     	new ALDOperatorExecutionProgressEvent(this,
     		" calculating features..."));

  	Vector<FeatureCalculator> featureOps = this.getFeatureOps();
  	
		TileFeatureCalculator tCalc = 
			new TileFeatureCalculator(this.tileSizeX, this.tileSizeY,
				this.tileShiftX, this.tileShiftY);
		tCalc.setFeatureOperators(featureOps);
		MTBTableModel features;
		int tilesX = -1, tilesY = -1;
		
		if (this.verbose.booleanValue())
  		System.out.println(this.operatorID + " Image directory= "
  				+ this.imageDir);
		
		DirectoryTree dirTree = 
			new DirectoryTree(this.imageDir.getDirectoryName(), false);
		Vector<String> imageList = dirTree.getFileList();
		for (String file : imageList) {
			if (this.verbose.booleanValue())
				System.out.println("\t Processing file " + file + "...");
			this.fireOperatorExecutionProgressEvent(
				new ALDOperatorExecutionProgressEvent(this,
					" processing file " + file + "..."));
			String basename = ALDFilePathManipulator.getFileName(file);

			try {
				img = this.readInputImageMaxProjectChannel(file);
				if (this.imageWidth == -1)
					this.imageWidth = img.getSizeX();
				if (this.imageHeight == -1)
					this.imageHeight = img.getSizeY();
      } catch (Exception e) {
      	System.err.println(this.operatorID + " Error reading file, " + 
      			"skipping " + file + "...");
      	continue;
      }
			
			// check if mask available
			MTBImage maskImage = null;
			String maskName = "undefined";
			maskImage = readMaskImage(this.maskDir.getDirectoryName(), basename, 
				this.maskFormat, 0, 0, this.imageWidth-1, this.imageHeight-1, 
					this.verbose.booleanValue());
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
						this.operatorID + " different tile sizes for images, exiting...");
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
      		OperatorExceptionType.OPERATE_FAILED,	this.operatorID 
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