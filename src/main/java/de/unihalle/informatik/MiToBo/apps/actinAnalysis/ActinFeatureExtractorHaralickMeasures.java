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

import de.unihalle.informatik.Alida.datatypes.ALDDirectoryString;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException.OperatorExceptionType;
import de.unihalle.informatik.Alida.helpers.ALDFilePathManipulator;
import de.unihalle.informatik.Alida.operator.ALDOperator;
import de.unihalle.informatik.Alida.operator.events.ALDOperatorExecutionProgressEvent;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.Direction;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.MiToBo.apps.actinAnalysis.ActinAnalyzer2D.CellMaskFormat;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.images.*;
import de.unihalle.informatik.MiToBo.core.imageJ.RoiManagerAdapter;
import de.unihalle.informatik.MiToBo.core.operator.*;
import de.unihalle.informatik.MiToBo.features.FeatureCalculator;
import de.unihalle.informatik.MiToBo.features.TileFeatureCalculator;
import de.unihalle.informatik.MiToBo.features.texture.FeatureCalculatorHaralickMeasures;
import de.unihalle.informatik.MiToBo.features.texture.FeatureCalculatorHaralickMeasures.HaralickDirection;
import de.unihalle.informatik.MiToBo.gui.MTBTableModel;
import de.unihalle.informatik.MiToBo.io.dirs.DirectoryTree;
import de.unihalle.informatik.MiToBo.io.images.ImageReaderMTB;
import de.unihalle.informatik.MiToBo.io.images.ImageWriterMTB;
import de.unihalle.informatik.MiToBo.visualization.drawing.DrawRegion2DSet;
import de.unihalle.informatik.MiToBo.visualization.drawing.DrawRegion2DSet.DrawType;

/**
 * Operator for extracting Haralick texture measures from cooccurrence matrices
 * as features for the {@link ActinAnalyzer2D}.
 * 
 * @author moeller
 */
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.ALL,
	level=Level.STANDARD, allowBatchMode=false)
public class ActinFeatureExtractorHaralickMeasures extends MTBOperator {

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
	 * Input image directory.
	 * <p>
	 * All files in the directory are considered. If a file cannot be opened
	 * (e.g. because it is not an image) it is skipped. 
	 */
	@Parameter( label= "Image directory", required = true, 
		dataIOOrder = -10, direction = Direction.IN, 
		description = "Input image directory.", mode = ExpertMode.STANDARD)
	protected ALDDirectoryString imageDir = null;

	/**
	 * Directory with (cell) masks.
	 */
	@Parameter( label= "Mask directory", required = true, dataIOOrder = -9,
		direction = Direction.IN, description = "Cell mask directory.",
		mode = ExpertMode.STANDARD)
	protected ALDDirectoryString maskDir = null;

	/**
	 * Format of provided cell masks.
	 */
	@Parameter( label= "Mask format", required = true, dataIOOrder = -8,
		direction = Direction.IN, description = "Format of cell masks.",
		mode = ExpertMode.STANDARD)
	protected CellMaskFormat maskFormat = CellMaskFormat.LABEL_IMAGE;
	
	/**
	 * Output and working directory.
	 */
	@Parameter( label= "Output and working directory", required = true, 
		dataIOOrder = -7, direction = Direction.IN, 
		description = "Output and working directory.", mode = ExpertMode.STANDARD)
	protected ALDDirectoryString outDir = null;
	
	/**
	 * Tile size in x-direction.
	 */
	@Parameter( label= "Tile size x", required = true, 
		dataIOOrder = 3, direction = Parameter.Direction.IN, 
		mode = ExpertMode.STANDARD, description = "Tile size in x-direction.")
	protected int tileSizeX = 32;

	/**
	 * Tile size in y-direction.
	 */
	@Parameter( label= "Tile size y", required = true, 
		dataIOOrder = 4, direction = Parameter.Direction.IN, 
		mode = ExpertMode.STANDARD, description = "Tile size in y-direction.")
	protected int tileSizeY = 32;

	/**
	 * Tile shift in x-direction.
	 */
	@Parameter( label= "Tile shift x", required = true, 
		dataIOOrder = 5, direction = Parameter.Direction.IN, 
		mode = ExpertMode.ADVANCED, description = "Tile shift in x-direction.")
	protected int tileShiftX = 32;

	/**
	 * Tile size in y-direction.
	 */
	@Parameter( label= "Tile shift y", required = true, 
		dataIOOrder = 6, direction = Parameter.Direction.IN, 
		mode = ExpertMode.ADVANCED, description = "Tile shift in y-direction.")
	protected int tileShiftY = 32;

	/**
	 * Distance.
	 */
	@Parameter( label= "Haralick distance", required = true,
		direction = Parameter.Direction.IN, dataIOOrder=7,  
		mode=ExpertMode.STANDARD, description = "Desired distance.")
	protected int distance= 4;

	/**
	 * Directions for calculating cooccurrence matrices.
	 */
	@Parameter( label= "Haralick directions", required = true, 
		dataIOOrder = 8, direction = Parameter.Direction.IN, 
		mode = ExpertMode.STANDARD, description = "Directions for matrices.")
	protected Vector<HaralickDirection> directions = defaultDirections;

	/**
	 * Flag for isotropic calculations.
	 * <p>
	 * If flag is true the average of each feature for the four directions EAST, 
	 * NORTH_EAST, NORTH and NORTH_WEST is taken as result. But, note that this
	 * causes the computation time to be increased by a factor of four as well.
	 */
	@Parameter( label= "Isotropic calculations", required = true,
		direction = Parameter.Direction.IN, dataIOOrder=9,  
		description = "Flag to enable isotropic calculations.",
		mode=ExpertMode.ADVANCED)
	protected boolean isotropicCalcs = false;

	/*
	 * some helper variables
	 */
	
	// width of the images taking first image as reference
	private transient int imageWidth = -1;
	
	// height of the images taking first image as reference
	private transient int imageHeight = -1;

	/**
	 * Default constructor.
	 */
	public ActinFeatureExtractorHaralickMeasures() throws ALDOperatorException {
		// nothing to be done here
	}
	
	/**
	 * Specify input image directory.
	 * @param iDir	Directory with images.
	 */
	public void setImageDir(ALDDirectoryString iDir) {
		this.imageDir = iDir;
	}
	
	/**
	 * Specify input mask directory.
	 * @param mDir	Directory with masks.
	 */
	public void setMaskDir(ALDDirectoryString mDir) {
		this.maskDir = mDir;
	}
	
	/**
	 * Specify input mask format.
	 * @param mFormat	Format of mask files, i.e. label images or ImageJ ROI files.
	 */
	public void setMaskFormat(CellMaskFormat mFormat) {
		this.maskFormat = mFormat;
	}
	
	/**
	 * Specify output directory.
	 * @param oDir	Output directory for feature files.
	 */
	public void setOutputDir(ALDDirectoryString oDir) {
		this.outDir = oDir;
	}
	
	/**
	 * Specify size of tiles in x-direction.
	 * @param tSizeX	Tile size in x.
	 */
	public void setTileSizeX(int tSizeX) {
		this.tileSizeX = tSizeX;
	}

	/**
	 * Specify size of tiles in y-direction.
	 * @param tSizeY	Tile size in y.
	 */
	public void setTileSizeY(int tSizeY) {
		this.tileSizeY = tSizeY;
	}

	/**
	 * Specify shift of tiles in x-direction.
	 * @param tShiftX	Tile shift in x.
	 */
	public void setTileShiftX(int tShiftX) {
		this.tileShiftX = tShiftX;
	}

	/**
	 * Specify shift of tiles in y-direction.
	 * @param tShiftY	Tile shift in y.
	 */
	public void setTileShiftY(int tShiftY) {
		this.tileShiftY = tShiftY;
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
  protected void operate() 
  		throws ALDOperatorException, ALDProcessingDAGException {
		
		int histConstructionMode = ALDOperator.getConstructionMode();
		ALDOperator.setConstructionMode(2);
		this.calculateFeatures();
		ALDOperator.setConstructionMode(histConstructionMode);
	}
	
	/**
	 * Performs the feature calculation.
	 * <p>
	 * The features are saved to files in the given feature directory.
	 * If it is null, the output directory is used. Note that both directories
	 * can be the same.<br>
	 * For each image four files are saved:
	 * <ul>
	 * <li> *-features.tif: stack with visualizations of individual features 
	 * <li> *-features.ald: history file corresponding to feature stack
	 * <li> *-features.txt: textual features, each row refers to a specific tile,
	 *                      each column to an individual feature
	 * <li> *-features-config.ald: history of the tile feature calculator, 
	 *                      particularly containing the feature configuration                     
	 * </ul> 
	 * 
	 * @throws ALDOperatorException
	 * @throws ALDProcessingDAGException
	 */
	private void calculateFeatures() 
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
  		System.out.println("[ActinAnalyzer2D] Calculating features...");
  	this.fireOperatorExecutionProgressEvent(
     	new ALDOperatorExecutionProgressEvent(this,
     		" calculating features..."));
	
		// initialize the feature calculator
		Vector<FeatureCalculator> featureOps = new Vector<FeatureCalculator>();
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
  		System.out.println("[ActinAnalyzer2D] Image directory= "+this.imageDir);
		
		DirectoryTree dirTree = 
			new DirectoryTree(this.imageDir.getDirectoryName());
		Vector<String> imageList = dirTree.getFileList();
		for (String file : imageList) {
			// skip all files not ending with .tif
			if (!file.endsWith(".tif"))
				continue;
			if (this.verbose.booleanValue())
				System.out.println("\t Processing file " + file + "...");
			this.fireOperatorExecutionProgressEvent(
				new ALDOperatorExecutionProgressEvent(this,
					" processing file " + file + "..."));
			String basename = ALDFilePathManipulator.getFileName(file);
			basename = basename.substring(0, basename.length()-3);

			try {
        iRead.setFileName(file);
				iRead.runOp();
				img = iRead.getResultMTBImage(); 
				if (this.imageWidth == -1)
					this.imageWidth = img.getSizeX();
				if (this.imageHeight == -1)
					this.imageHeight = img.getSizeY();
      } catch (Exception e) {
      	System.err.println("[ActinAnalyzer2D] Error reading file, " + 
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
    		System.out.print("\t\t - initializing and running tile calculator...");
   		fireOperatorExecutionProgressEvent(
      	new ALDOperatorExecutionProgressEvent(this,
      		" initializing and running tile calculator..."));
			
			tCalc.setInputImage(img);
			if (maskImage != null)
				tCalc.setMask(maskImage);
			tCalc.runOp();

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
						"[ActinAnalyzer2D] different tile sizes for images, exiting...");
				}
			}
			// save result data to output directory
			String outname= this.outDir + "/" +basename+ "-features.txt";
			String outhistname= this.outDir + "/" +basename+ "-features-config";
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
      	throw new ALDOperatorException(OperatorExceptionType.OPERATE_FAILED,
      		"[ActinAnalyzer2D] could not save features to \"" + outname + "\"!");
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
	
	/**
	 * Read mask data from disk if available.
	 * <p>
	 * The method reads segmentation data from file. It considers the specified
	 * mask format, i.e., if a label image is to be read or ImageJ 1.x ROIs. In 
	 * the latter case it automatically differentiates between files ending with
	 * '.zip', i.e., containing more than one region, and files ending with 
	 * '.roi' which contain exactly a single region.
	 * 
	 * @param basename		Basename of the corresponding image file.
	 * @param xmin	Minimum x-value of input image domain.
	 * @param ymin Minimum y-value of input image domain.
	 * @param xmax Maximum x-value of input image domain, i.e. image width - 1.
	 * @param ymax Maximum y-value of input image domain, i.e. image height - 1.
	 * @return	Mask image, null if appropriate file could not be found.
	 * @throws ALDOperatorException
	 */
	 MTBImage readMaskImage(String basename, 
				double xmin, double ymin,	double xmax, double ymax) 
			throws ALDOperatorException {
		ImageReaderMTB iRead = new ImageReaderMTB();
		MTBImage maskImage = null;
		String maskName = "";
		if (this.maskDir != null) {
			switch(this.maskFormat)
			{
			case LABEL_IMAGE:
				maskName= this.maskDir.getDirectoryName() + File.separator 
					+ basename + "-mask.tif";
				if (this.verbose.booleanValue())
					System.out.print("\t\t - searching mask " + maskName + "...");
				fireOperatorExecutionProgressEvent(
						new ALDOperatorExecutionProgressEvent(this,
								" searching mask " + maskName + "..."));

				if ((new File(maskName)).exists()) {
					try {
						iRead.setFileName(maskName);
						iRead.runOp();
						maskImage = iRead.getResultMTBImage();
						if (this.verbose.booleanValue())
							System.out.println("found!");
						fireOperatorExecutionProgressEvent(
								new ALDOperatorExecutionProgressEvent(this,	" ... found!"));
					} catch (Exception e) {
						if (this.verbose.booleanValue())
							System.out.println("not found!");
						System.err.println("[ActinAnalyzer2D] Error reading mask " + 
								maskName + ", ignoring mask...");
						fireOperatorExecutionProgressEvent(
								new ALDOperatorExecutionProgressEvent(this," ... not found!"));
					}
				}
				else {
					if (this.verbose.booleanValue())
						System.out.println("mask not found!");
					fireOperatorExecutionProgressEvent(
							new ALDOperatorExecutionProgressEvent(this,
									" ... mask not found!"));
				}
				break;
			case IJ_ROIS:
				String maskName_A= this.maskDir.getDirectoryName() + File.separator 
					+ basename + "-mask.zip";
				String maskName_B= this.maskDir.getDirectoryName() + File.separator 
						+ basename + "-mask.roi";
				maskName = null;
				if ((new File(maskName_A)).exists()) 
					maskName = maskName_A;
				else 
					if ((new File(maskName_B)).exists())
						maskName = maskName_B;
				if (this.verbose.booleanValue())
					System.out.print("\t\t - searching IJ ROI file " + maskName + "...");
				fireOperatorExecutionProgressEvent(
						new ALDOperatorExecutionProgressEvent(this,
								" searching IJ ROI file " + maskName + "..."));

				if (maskName != null) {
					try {
						MTBRegion2DSet regions = 
							RoiManagerAdapter.getInstance().getRegionSetFromRoiFile(
								maskName, xmin, ymin, xmax, ymax);
						if (this.verbose.booleanValue())
							System.out.println("found!");
						fireOperatorExecutionProgressEvent(
							new ALDOperatorExecutionProgressEvent(this,	" ... found!"));
						// convert region set to label image
						DrawRegion2DSet regionDrawOp = new DrawRegion2DSet(
							DrawType.LABEL_IMAGE, regions);
						regionDrawOp.runOp(HidingMode.HIDDEN);
						maskImage = regionDrawOp.getResultImage();
						// save the label image to the output directory
						String outMaskName= this.outDir.getDirectoryName() + File.separator 
							+ basename + "-mask.tif";
						ImageWriterMTB imgWriter = 
							new ImageWriterMTB(maskImage, outMaskName);
						imgWriter.setOverwrite(true);
						imgWriter.runOp(HidingMode.HIDDEN);
					} catch (Exception e) {
						if (this.verbose.booleanValue())
							System.out.println("not found!");
						System.err.println("[ActinAnalyzer2D] Error reading IJ ROIs " + 
							maskName + ", ignoring segmentation...");
						fireOperatorExecutionProgressEvent(
							new ALDOperatorExecutionProgressEvent(this," ... not found!"));
					}
				}
				else {
					if (this.verbose.booleanValue())
						System.out.println("mask / ROIs not found!");
					fireOperatorExecutionProgressEvent(
							new ALDOperatorExecutionProgressEvent(this,
									" ... mask / ROIs not found!"));
				}
				break;
			}
		}
		if (maskImage != null)
			maskImage.setProperty("Filename", maskName);
		return maskImage;
	}
}

/*BEGIN_MITOBO_ONLINE_HELP

This operator performs an unsupervised analysis of actin microfilament 
structures in sets of microscopy images. It takes as input a directory where 
the images are expected, and a set of segmentation masks showing the boundaries
of individual cells in the images. The operator first calculates a set of 
texture features from the images and then clusters the feature vectors to 
identify structural patterns shared among different cells. Finally, each cell
is characterized in terms of a distribution vector describing the appearance
of various structural patterns in the cell. These vectors can then be used to 
analyze commonalities and differences between individual cells or groups of 
cells.

<ul>
<li><p><b>input:</b>
<ul>
<li><p><i>Image directory</i>:<br> directory where the images are read from,
	all files ending on ".tif" are considered;<br> please refer to the webpage for
	further information on how the file names should be formatted </p></li>
<li><p><i>Mask directory</i>:<br> directory where the segmentation information for 
	the images is read from; the directory can be identical to the 
	image directory</p></li>
<li><p><i>Mask format</i>:<br> expected format of the segmentation data files
	<ul>
	<li>LABEL_IMAGE:<br> a gray-scale image is expected where the area of each 
		cell is marked with a single unique gray-scale value;<br>
		the files should share the names of the input image files and have the
		ending "-mask.tif"
	<li>IJ_ROIS:<br> an ImageJ 1.x file of ROI manager regions is expected;<br>		
		the files should share the names of the input image files and have the
		ending "-mask.zip" or "-mask.roi"
	</ul>
<li><p><i>Output and working directory</i>:<br> directory for intermediate and
	final results
<li><p><i>Calculate features?</i><br> by disabeling this option the time-consuming
	feature calculation can be omitted, however, in this case the feature
	directory is expected to already contain the feature files
<li><p><i>Feature directory</i>:<br> directory where the calculated features are
	saved to and from where they are read if feature calculations are omitted
<li><p><i>Number of feature clusters</i>:<br> number of clusters to be used for
	feature vector clustering, should approximately refer to the expected number 
	of structural patterns appearing in the cells
<li><p><i>Do PCA in stage II?</i><br> enable or disable the PCA prior to the 	
	pairwise distance calculations in stage II of the approach
</ul>

<li><p><b>output:</b>
<ul>
<li><p><i>Resulting chart plots for each group</i>:<br> 
	cluster distributions for each cell categorized into cell groups
<li><p><i>Resulting box-whisker plot</i>:<br> box plot of the relative frequecies
	of appearance of each	cluster for the different cell groups
</ul>
In addition to the output data directly displayed on termination of the operator
some more result data files are written to the output directory. In particular,
a file with the pairwise Euclidean distances between distribution vectors can 
be found there which can be further analyzed, e.g., with 
<a href="http://deim.urv.cat/~sgomez/multidendrograms.php">MultiDendrograms</a>.
Also images visualizing the cluster distributions for each input image and 
each cell, respectively, are available.

</ul>

<p>
For more details about the operator and additional information on the parameters
refer to its webpage: 
<a href="http://www2.informatik.uni-halle.de/agprbio/mitobo/index.php/Applications/ActinAnalyzer2D">
http://www2.informatik.uni-halle.de/agprbio/mitobo/index.php/Applications/ActinAnalyzer2D</a>.

END_MITOBO_ONLINE_HELP*/
