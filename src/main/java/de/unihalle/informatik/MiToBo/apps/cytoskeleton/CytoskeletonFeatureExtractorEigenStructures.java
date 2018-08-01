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
import java.util.LinkedList;
import java.util.Vector;

import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException.OperatorExceptionType;
import de.unihalle.informatik.Alida.helpers.ALDFilePathManipulator;
import de.unihalle.informatik.Alida.operator.events.ALDOperatorExecutionProgressEvent;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.ALDDerivedClass;
import de.unihalle.informatik.MiToBo.core.datatypes.images.*;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.io.dirs.DirectoryTree;
import de.unihalle.informatik.MiToBo.io.images.ImageWriterMTB;
import de.unihalle.informatik.MiToBo.math.statistics.PCA;
import de.unihalle.informatik.MiToBo.math.statistics.PCA.ReductionMode;

/**
 * Operator for extracting Eigen structures as features for the 
 * {@link ActinAnalyzer2D}.
 * 
 * @author moeller
 */
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.SWING,
	level=Level.STANDARD, allowBatchMode=false)
@ALDDerivedClass
public class CytoskeletonFeatureExtractorEigenStructures 
	extends CytoskeletonFeatureExtractor {

	/*
	 * some helper variables
	 */
	
	/**
	 * Number of tiles in x-dimension.
	 */ 
	private transient int tileNumX;
	
	/**
	 * Number of tiles in y-dimension.
	 */ 
	private transient int tileNumY;

	/**
	 * Default constructor.
	 * @throws ALDOperatorException Thrown in case of failure.
	 */
	public CytoskeletonFeatureExtractorEigenStructures() 
			throws ALDOperatorException {
		this.operatorID = "[CytoskeletonFeatureExtractorEigenStructures]";
	}
	
	@Override
	protected void calculateFeatures() 
		throws ALDOperatorException, ALDProcessingDAGException {
		
		MTBImage img;
		ImageWriterMTB iWrite = new ImageWriterMTB();

  	if (this.verbose.booleanValue())
  		System.out.println(this.operatorID	+ " Calculating features...");
  	this.fireOperatorExecutionProgressEvent(
     	new ALDOperatorExecutionProgressEvent(this,
     		" calculating features..."));
	
  	DirectoryTree dirTree = 
 			new DirectoryTree(this.imageDir.getDirectoryName());
  	Vector<String> imageList = dirTree.getFileList();
		LinkedList<int[]> dataVects = new LinkedList<int[]>();
		LinkedList<String> files = new LinkedList<String>();
		LinkedList<String> fileBasenames = new LinkedList<String>();
		LinkedList<String> maskNames = new LinkedList<String>();
		LinkedList<byte[]> validTiles = new LinkedList<byte[]>();
  	for (String file : imageList) {
  		if (this.verbose.booleanValue())
  			System.out.println("\t Processing file " + file + "...");
  		this.fireOperatorExecutionProgressEvent(
  				new ALDOperatorExecutionProgressEvent(this,
  						" processing file " + file + "..."));
  		String basename = ALDFilePathManipulator.getFileName(file);
  		files.add(file);
  		fileBasenames.add(basename);

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
			maskImage = this.readMaskImage(basename, 
				0, 0, this.imageWidth-1, this.imageHeight-1);
			if (maskImage != null)
				maskNames.add(maskImage.getProperty("Filename"));
			else
				maskNames.add(null);
			
    	if (this.verbose.booleanValue())
    		System.out.println("\t\t - extracting data vectors...");
   		fireOperatorExecutionProgressEvent(
      	new ALDOperatorExecutionProgressEvent(this,
      		" extracting data vectors..."));
  		MTBImageTileAdapter tileAdapter = 
 				new MTBImageTileAdapter(img, this.tileSizeX, this.tileSizeY,
					this.tileShiftX, this.tileShiftY);
			this.tileNumX = tileAdapter.getTileCols();
			this.tileNumY = tileAdapter.getTileRows();
  		MTBImageTileAdapter tileAdapterMask = null;
  		if (maskImage != null) 	
  			tileAdapterMask = new MTBImageTileAdapter(maskImage, 
 					this.tileSizeX, this.tileSizeY, 
 						this.tileShiftX, this.tileShiftY);

  		// extract data vectors
			int x = 0, y = 0, c = 0;
			int width, height;
			int w, h, n;
			int tileNum = tileAdapter.getTileCols()*tileAdapter.getTileRows();
			byte[] vTiles = new byte[tileNum];
			for (MTBImage imageTile: tileAdapter) {
				boolean invalid = false;
				MTBImage maskTile;
				if (tileAdapterMask != null) {
					maskTile = tileAdapterMask.getTile(x,y);
					for (int yy = 0; !invalid && yy<maskTile.getSizeY(); ++yy) {
						for (int xx = 0; !invalid && xx<maskTile.getSizeX(); ++xx) {
							if (maskTile.getValueInt(xx, yy) == 0) {
								invalid = true;
							}
						}
					}
				}
				if (!invalid) {
					width = imageTile.getSizeX();
					height = imageTile.getSizeY();
					int[] vec = new int[width*height];
					n = 0;
					for (h=0; h<height; ++h) {
						for (w=0; w<width; ++w) {
							vec[n] = imageTile.getValueInt(w, h);
							++n;
						}
					}
					dataVects.add(vec);
				}
				if (invalid)
					vTiles[c] = 0;
				else
					vTiles[c] = 1;
				++c;
				++x;
				if (x == this.tileNumX) {
					++y;
					x = 0;
				}
			}
			validTiles.add(vTiles);
  	}
  	int vectorNum = dataVects.size();
  	int vectorDim = dataVects.get(0).length;
  	if (this.verbose.booleanValue()) {
  		System.out.println("\t Found " + vectorNum 
  				+ " data vectors in total,"	+ " dimension = " + vectorDim);
  	}
 		fireOperatorExecutionProgressEvent(
    	new ALDOperatorExecutionProgressEvent(this, " Found " + vectorNum 
    		+ " data vectors in total (dimension " + vectorDim + ")."));

 		// convert to data matrix
  	double[][] pcaInputData = new double[vectorDim][vectorNum];
  	int[] vec;
  	for (int m=0; m<vectorNum; ++m) {
  		vec = dataVects.get(m);
  		for (int n=0; n<vectorDim; ++n)
  			pcaInputData[n][m] = vec[n];
  	}
  	dataVects.clear();

  	// perform PCA
  	PCA pcaOp = new PCA();
  	pcaOp.setDataset(pcaInputData);
  	pcaOp.setMeanFreeData(false);
  	pcaOp.setReductionMode(ReductionMode.PERCENTAGE_VARIANCE);
  	pcaOp.setPercentageOfVariance(0.92);
  	pcaOp.setVerbose(this.verbose);
  	pcaOp.runOp();
  	double[][] resultData = pcaOp.getResultData();
  	double[][] eigenvects = pcaOp.getEigenvects();
  	double[] eigenvals = pcaOp.getEigenvalues();
  	if (this.verbose.booleanValue())
  		System.out.println(" done!");
  	fireOperatorExecutionProgressEvent(
  		new ALDOperatorExecutionProgressEvent(this, " ...done!"));
  	
  	// save result data
  	String outbase = this.outDir + "/";
  	String outfile = outbase + "eigenvals.txt";
  	FileWriter ow;
    try {
	    ow = new FileWriter(new File(outfile).getPath());
	  	for (int i=0;i<eigenvals.length;++i)
	  		ow.write(eigenvals[i] + "\n");
	  	ow.close();
    } catch (IOException e) {
    	System.err.println("Saving eigenvalues to file failed...!");
    }

  	// save eigenvectors
  	iWrite = new ImageWriterMTB();
  	MTBImageDouble eigenImg = (MTBImageDouble)MTBImage.createMTBImage(
  		this.tileShiftX, this.tileShiftY, 1, 1, 1, 
  			MTBImageType.MTB_DOUBLE);
  	for (int i=0; i<eigenvects[0].length; ++i) {
  		int n = 0;
			for (int h=0; h<this.tileShiftY; ++h) {
				for (int w=0; w<this.tileShiftX; ++w) {
					eigenImg.putValueDouble(w, h, eigenvects[n][i]);
					++n;
				}
			}
			String numStr = "";
			if (i < 10) numStr = "000";
			else if (i < 100) numStr = "00";
			else if (i < 1000) numStr = "0";
			outfile = outbase + "eigenvector-" + numStr + i + ".tif";
			iWrite.setFileName(outfile);
			iWrite.setInputMTBImage(
				eigenImg.convertType(MTBImageType.MTB_BYTE, true));
			iWrite.runOp(HidingMode.HIDDEN);
  	}
  	
  	// save feature vectors
  	int totalID = -1;
  	for (int i=0; i<files.size(); ++i) {
  		String file = files.get(i);
  		String base = fileBasenames.get(i);
  		String mask = maskNames.get(i);
			String outname= this.outDir + "/" + base + "-features.txt";
			byte[] vTiles = validTiles.get(i);
			if (this.verbose.booleanValue())
				System.out.println("\t Saving features to " + outname);
			String eventMsg = " saving features...";
			fireOperatorExecutionProgressEvent(
				new ALDOperatorExecutionProgressEvent(this, eventMsg));
			
      try {
	      ow = new FileWriter(new File(outname).getPath());
	      // write all relevant data into header
	      ow.write("# filename: " + file + "\n");
	      ow.write("# maskfile: " + mask + "\n");
	      ow.write("# tileSizeX: " + this.tileSizeX + "\n");
	      ow.write("# tileSizeY: " + this.tileSizeY + "\n");
	      ow.write("# tileShiftX: " + this.tileShiftX + "\n");
	      ow.write("# tileShiftY: " + this.tileShiftY + "\n");
	      ow.write("# tileCountX: " + this.tileNumX + "\n");
	      ow.write("# tileCountY: " + this.tileNumY + "\n");
	      ow.write("# tileCountTotal: " 
	      		+ this.tileNumX * this.tileNumY + "\n");
	      int invalidTiles = 0;
	      for (byte b: vTiles)
	      	if (b == 0) 
	      		++invalidTiles;
	      ow.write("# invalidTiles: " + invalidTiles + "\n");
	      for (int f=0; f<resultData.length; ++f)
	      	ow.write("f-" + f + "\t");
	      ow.write("\n");

	      // write actual data
	      for (int t=0; t<this.tileNumX * this.tileNumY; ++t) {
	      	// tile is valid
	      	if (vTiles[t] != 0) {
		      	// increment global column counter for valid tiles
		      	++totalID;
	      		for (int k=0; k<resultData.length; ++k)
	      			ow.write(Double.toString(resultData[k][totalID]) + "\t");
	      	}
	      	// tile is invalid
	      	else {
	      		for (int k=0; k<resultData.length; ++k)
	      			ow.write("NaN" + "\t");     			
	      	}
	      	ow.write("\n");     			
	      }
				ow.close();
      } catch (IOException e1) {
      	throw new ALDOperatorException(
      		OperatorExceptionType.OPERATE_FAILED,
      			"[ActinAnalyzer2D] could not save features to \"" 
      					+ outname + "\"!");
      }
		}
  }
}

/*BEGIN_MITOBO_ONLINE_HELP

This operator extracts Eigen features from given images.

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
