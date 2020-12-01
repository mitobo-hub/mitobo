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

import java.awt.Color;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;
import java.util.regex.Matcher;

import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;

import de.unihalle.informatik.Alida.datatypes.ALDDirectoryString;
import de.unihalle.informatik.Alida.exceptions.ALDException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.Alida.helpers.ALDFilePathManipulator;
import de.unihalle.informatik.Alida.operator.events.ALDOperatorExecutionProgressEvent;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.Direction;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.Alida.annotations.Parameter.ParameterModificationMode;
import de.unihalle.informatik.MiToBo.apps.cytoskeleton.CytoskeletonFeatureExtractor.CellMaskFormat;
import de.unihalle.informatik.MiToBo.clustering.KMeans;
import de.unihalle.informatik.MiToBo.color.tools.DistinctColorListGenerator;
import de.unihalle.informatik.MiToBo.core.datatypes.images.*;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.operator.*;
import de.unihalle.informatik.MiToBo.gui.MTBTableModel;
import de.unihalle.informatik.MiToBo.io.dirs.DirectoryTree;
import de.unihalle.informatik.MiToBo.io.images.ImageReaderMTB;
import de.unihalle.informatik.MiToBo.io.images.ImageWriterMTB;
import de.unihalle.informatik.MiToBo.math.statistics.PCA;
import de.unihalle.informatik.MiToBo.math.statistics.PCA.ReductionMode;
import de.unihalle.informatik.MiToBo.visualization.plots.BoxWhiskerChartPlotter;
import de.unihalle.informatik.MiToBo.visualization.plots.StackedBarChartPlotter;

/**
 * Operator for extracting quantative global features of the cytoskeleton.
 *
 * @author moeller
 */
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.ALL,
	level=Level.APPLICATION, allowBatchMode=false,
	shortDescription="Extracts quantative global features of cytoskeletons.")
public class CytoskeletonAnalyzer2D extends MTBOperator {

	/**
	 * Identifier string for this operator class.
	 */
	private static final String operatorID = "[CytoskeletonAnalyzer2D]";
	
	/*
	 * Mandatory parameters.
	 */

	/**
	 * Input image directory.
	 * <p>
	 * All files in the directory are considered. If a file cannot be opened
	 * (e.g. because it is not an image) it is skipped.
	 */
	@Parameter( label= "Image file folder", required = true,
		dataIOOrder = -10, direction = Direction.IN,
		description = "Input image directory.", mode = ExpertMode.STANDARD)
	protected ALDDirectoryString imageDir = null;

	/**
	 * Format of provided cell boundaries.
	 */
	@Parameter( label= "Boundary file format", required = true, dataIOOrder = -8,
		direction = Direction.IN, description = "Format of cell boundary files.",
		mode = ExpertMode.STANDARD)
	protected CellMaskFormat maskFormat = CellMaskFormat.LABEL_IMAGE;

	/**
	 * Channel of input image containing stained cytoskeleton.
	 */
	@Parameter(label = "Cytoskeleton channel", required = true, 
		direction = Parameter.Direction.IN, dataIOOrder = -7, 
		description = "Channel with stained cytoskeleton, e.g., 1, 2 and so on.")
	private int cytoSkelChannel = 1;

	/**
	 * Flag for calculating features.
	 * <p>
	 * If false, features are assumed to be available already.
	 */
	@Parameter( label= "Calculate features?", required = true,
		dataIOOrder = 0, direction = Direction.IN,
		description = "Flag to enable/disable feature calculation.",
		mode = ExpertMode.STANDARD, callback = "calcFeatureFlagChanged",
		paramModificationMode = ParameterModificationMode.MODIFIES_INTERFACE)
	protected boolean doFeatureCalculation = true;

	/**
	 * Feature extractor to apply.
	 */
	@Parameter( label= "Feature Extractor", required = true,
		dataIOOrder = 1, direction = Direction.IN,
		description = "Select type of features to apply.",
		mode = ExpertMode.STANDARD)
	protected CytoskeletonFeatureExtractor featureExtractor =
		new CytoskeletonFeatureExtractorLBPsRIU();

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
		mode = ExpertMode.STANDARD, description = "Tile shift in x-direction.")
	protected int tileShiftX = 32;

	/**
	 * Tile size in y-direction.
	 */
	@Parameter( label= "Tile shift y", required = true,
		dataIOOrder = 6, direction = Parameter.Direction.IN,
		mode = ExpertMode.STANDARD, description = "Tile shift in y-direction.")
	protected int tileShiftY = 32;

	/**
	 * Number of clusters to be used in feature clustering.
	 */
	@Parameter( label= "Number of feature clusters", required = true,
		dataIOOrder = 10, direction = Parameter.Direction.IN,
		mode = ExpertMode.STANDARD, description = "Number of feature clusters.")
	protected int clusterNum = 6;

	/**
	 * Perform PCA in second stage?
	 */
	@Parameter( label= "Do PCA in stage II?", required = true,
		dataIOOrder = 11, direction = Parameter.Direction.IN,
		mode = ExpertMode.STANDARD,
		description = "Enable/disable PCA prior to hierarchical clustering.")
	protected boolean doPCA = true;

	/*
	 * Optional parameters.
	 */
	
	/**
	 * (Optional) directory with (cell) boundaries.
	 */
	@Parameter( label= "Cell boundary file folder", required = false, 
		dataIOOrder = 1, direction = Direction.IN, 
		description = "Cell mask directory.",	mode = ExpertMode.STANDARD)
	protected ALDDirectoryString maskDir = null;

	/*
	 * Result parameters.
	 */

	/**
	 * Resulting stacked bar plot of cluster distributions.
	 */
	@Parameter( label= "Resulting chart plots for each group",
		dataIOOrder = 1, direction = Direction.OUT,
		description = "Resulting chart plots.", mode = ExpertMode.STANDARD)
	protected Vector<JFreeChart> stackedBarCharts;

	/**
	 * Box-whisker plot of the group-wise cluster distributions.
	 */
	@Parameter( label= "Resulting box-whisker plot",
		dataIOOrder = 1, direction = Direction.OUT,
		description = "Resulting box-whisker plot.", mode = ExpertMode.STANDARD)
	protected Vector<JFreeChart> boxWhiskerCharts;

	/*
	 * some helper variables
	 */

	/// ***************************************
	/// Just temporarily! Will be removed soon!
	
//	protected ALDDirectoryString outDir = null;
//	protected ALDDirectoryString featureDir = null;

	/// Just temporarily! Will be removed soon!
	/// ***************************************
	
	/**
	 * Width of the images taking first image as reference.
	 */
	private transient int imageWidth = -1;

	/**
	 * Height of the images taking first image as reference.
	 */
	private transient int imageHeight = -1;

	/**
	 * File separator character, operating system dependent.
	 */
	private static String fileSep = File.separator;
	
	/**
	 * List of group names, filled in {@link #clusterFeatures()}.
	 */
	private transient TreeSet<String> cellGroupNames;

	/**
	 * Map of cluster distributions per cell, filled in
	 * {@link #clusterFeatures()}.
	 */
	private transient HashMap<String, double[]> cellwiseDistros =
		new HashMap<String, double[]>();
	
	/**
	 * Sorted list of keys in {@link #cellwiseDistros} once it is filled.
	 */
	private LinkedList<String> cellwiseDistroKeys;

  /**
   * Group-wise cell distribution data, for each group the vector
   * contains a hash map with the following structure:
   * - the keys of the map are given by the cell IDs of the different
   *   cells (without basename which is equal to the group name)
   * - the values are represented again as a hash map where the keys
   *   are given by the cluster IDs and the values by the relative
   *   frequency of the corresponding cluster in the cell
   *
	 * Example:
	 * ---------
	 *
	 * cellGroups.get(0) = < 001-01, < [c1, vc1], ..., [c6, vc6] > > ,
	 *	                     ...
	 *	                   < 005-07, < [c1, vc1], ..., [c6, vc6] > >
	 * cellGroups.get(1) = < 004-01, < [c1, vc1], ..., [c6, vc6] > > ,
	 *	                     ...
	 *	                   < 008-01, < [c1, vc1], ..., [c6, vc6] > >
	 *
	 * The vector is filled in function clusterFeatures().
	 */
	private transient
		Vector< HashMap< String, HashMap<String, Double>> > cellGroups;

	/**
	 * Cluster distribution data, rows refer to clusters and columns to
	 * cells.
	 */
	private transient double[][] distroData;

	/**
	 * Group names linked to entries in {@link #distroData}.
	 */
	private transient String[] distroDataGroups;

	/**
	 * Dimension-reduced cluster distribution data.
	 */
	private transient double[][] subspaceData;

	/**
	 * Default constructor.
	 * @throws ALDOperatorException Thrown in case of failure.
	 */
	public CytoskeletonAnalyzer2D() throws ALDOperatorException {
		// configure operator to calculate features by default
		this.setParameter("doFeatureCalculation", new Boolean(true));
	}

	@Override
  protected void operate()
  		throws ALDOperatorException, ALDProcessingDAGException {

		// for each image calculate feature vectors (if requested)
		if (this.doFeatureCalculation) {
			if (this.verbose.booleanValue())
				System.out.println("Feature extractor: "
						+ this.featureExtractor.getName());
			
			// get a list of sub-folders of the given image directory
			DirectoryTree dt = 
					new DirectoryTree(this.imageDir.getDirectoryName(), true);
			Vector<String> subdirs = dt.getSubdirectoryList();
			for (String d: subdirs) {
				
				// skip folders named "results"
				if (d.contains("results"))
					continue;
				
				System.out.println("Processing sub-folder " + d + "...");
				
				// get list of images
				DirectoryTree st = new DirectoryTree(d, false);
				Vector<String> files = st.getFileList();
				
				if (files != null && !files.isEmpty()) {
					System.out.println("=> Found files: ");
					for (String f: files)
						System.out.println("\t" + f);

					// create output folder
					new File(d + fileSep + "results_features").mkdirs();
					
					// run the feature extraction
					this.featureExtractor.setImageDir(new ALDDirectoryString(d));
					this.featureExtractor.setCytoskeletonChannel(
							this.cytoSkelChannel);
					this.featureExtractor.setMaskDir(
							new ALDDirectoryString(d + fileSep + "results_segmentation"));
					this.featureExtractor.setMaskFormat(this.maskFormat);
					this.featureExtractor.setOutputDir(
							new ALDDirectoryString(d + fileSep + "results_features"));
					this.featureExtractor.setTileSizeX(this.tileSizeX);
					this.featureExtractor.setTileSizeY(this.tileSizeY);
					this.featureExtractor.setTileShiftX(this.tileShiftX);
					this.featureExtractor.setTileShiftY(this.tileShiftY);
					this.featureExtractor.setVerbose(this.verbose);
					this.featureExtractor.runOp();
					// remember dimensions of first image if image size is unknown so far
					if (this.imageHeight == -1)
						this.imageHeight = this.featureExtractor.getImageHeight();
					if (this.imageWidth == -1)
						this.imageWidth = this.featureExtractor.getImageWidth();
				}
			}
		}

		// cluster the features and analyze the distributions
		this.cellwiseDistros = new HashMap<String, double[]>();
		this.clusterFeatures();

		// perform PCA
		if (this.doPCA)
			this.doPCA();

		// calculate pairwise distances for further analysis
		this.calculateDistanceMatrices();
	}

	/**
	 * Performs the feature clustering via Weka's kMeans algorithm.
	 * <p>
	 * The method first reads the features from the given or formerly generated
	 * files, respectively, filters the feature vectors to exclude background
	 * tiles, and finally applies kMeans clustering. The feature files are
	 * expected to be located in the given feature directory.<br>
	 * The results are again saved to various files in the output directory:
	 * <ul>
	 * <li> *-clusters.tif: visualization of cluster assignments
	 * <li> *-clusters.ald: corresponding history file
	 * </ul>
	 * Subsequently, the cluster distributions are analyzed in more detail, i.e.,
	 * stacked bar plots and box-whisker plots are generated:
	 * <ul>
	 * <li> *-clusterDistro.txt: cluster distributions per image and cell
	 * <li> *-distributionChart.png: cell-wise distributions for each group
	 * <li> AllGroupsClusterStatsChart.png: box-whisker plot of distributions
	 * <li> AllImagesClusterStatistics.txt: cluster distributions for whole set
	 * </ul>
	 *
	 * @throws ALDOperatorException  			Thrown in case of failure.
	 * @throws ALDProcessingDAGException	Thrown in case of failure.
	 */
	private void clusterFeatures()
		throws ALDOperatorException, ALDProcessingDAGException {

		// cluster feature data
		if (this.verbose.booleanValue())
			System.out.println(operatorID + " Reading feature files...");
		this.fireOperatorExecutionProgressEvent(
     	new ALDOperatorExecutionProgressEvent(this,
     		" reading features files..."));

		int tileCountX = -1, tileCountY = -1;
		int tileCountTotal = -1, invalidTiles = -1;
		int tileSizeXFromFile = -1, tileSizeYFromFile = -1;
		int tileShiftXFromFile = -1, tileShiftYFromFile = -1;

		// get a list of sub-folders of the given image directory
		DirectoryTree dt = 
				new DirectoryTree(this.imageDir.getDirectoryName(), true);
		Vector<String> subdirs = dt.getSubdirectoryList();
		Vector<String> featureFileList = new Vector<String>();
		Vector<String> lines = new Vector<String>();
		String[] heads = null;
		for (String d: subdirs) {
			
			// skip irrelevant folders
			if (!d.contains("results_features"))
				continue;
			
			System.out.println("Processing sub-folder " + d + "...");
			
			// get list of files
			DirectoryTree st = new DirectoryTree(d, false);
			Vector<String> files = st.getFileList();
			
			if (files != null && !files.isEmpty()) {
				System.out.println("=> Found files: ");
				for (String f: files)
					System.out.println("\t" + f);

				for (String file : files) {

					// only process files with ending .txt
					if (!file.endsWith("features.txt"))
						continue;

					if (this.verbose.booleanValue())
						System.out.println("\t Processing feature file " + file + "...");
					this.fireOperatorExecutionProgressEvent(
							new ALDOperatorExecutionProgressEvent(this,
									" processing feature file " + file + "..."));

					// read feature data and fill table
					featureFileList.add(file);
					try {
						BufferedReader fRead =
								new BufferedReader(new FileReader(new File(file)));
						String line;
						// header of file contains some information we need:
						//
						// # filename: ...
						// # maskfile: ...
						// # tileSizeX: ...
						// # tileSizeY: ...
						// # tileShiftX: ...
						// # tileShiftY: ...
						// # tileCountX: ...
						// # tileCountY: ...
						// # tileCountTotal: ...
						// # invalidTiles: ...
						// # feature identifier: ...
						//

						// skip filename and maskfile
						fRead.readLine();
						fRead.readLine();
						// read tile sizes
						line = fRead.readLine();
						int sizeX = Integer.valueOf(line.split(" ")[2]).intValue();
						if (tileSizeXFromFile == -1)
							tileSizeXFromFile = sizeX;
						else
							if (tileSizeXFromFile != sizeX)
								System.err.println(operatorID + 
										" tile sizes in x of different images do not match!");
						line = fRead.readLine();
						int sizeY = Integer.valueOf(line.split(" ")[2]).intValue();
						if (tileSizeYFromFile == -1)
							tileSizeYFromFile = sizeX;
						else
							if (tileSizeYFromFile != sizeY)
								System.err.println(operatorID +
										" tile sizes in y of different images do not match!");
						// skip tile shifts
						line = fRead.readLine();
						tileShiftXFromFile =
								Integer.valueOf(line.split(" ")[2]).intValue();
						line = fRead.readLine();
						tileShiftYFromFile =
								Integer.valueOf(line.split(" ")[2]).intValue();
						// get count in x
						line = fRead.readLine();
						tileCountX = Integer.valueOf(line.split(" ")[2]).intValue();
						// get count in y
						line = fRead.readLine();
						tileCountY = Integer.valueOf(line.split(" ")[2]).intValue();
						// get total count
						line = fRead.readLine();
						tileCountTotal = Integer.valueOf(line.split(" ")[2]).intValue();
						// get invalid tile count
						line = fRead.readLine();
						invalidTiles = Integer.valueOf(line.split(" ")[2]).intValue();
						// read headers, should be same for all files, so do it just once
						if (heads == null)
							heads = fRead.readLine().split("\t");
						else
							// skip first line
							fRead.readLine();
						int counter = -1;
						int validTiles = 0;
						while ((line = fRead.readLine()) != null) {
							++counter;
							// skip lines containing NaNs and very homogeneous tiles
							if (   !line.contains("NaN") ) {
								lines.add(file + "\t" + counter + "\t" + line);
								++validTiles;
							}
						}
						fRead.close();

						// safety check
						if (validTiles != tileCountTotal - invalidTiles)
							System.err.println(operatorID + " number of feature vectors "
								+ "does not match number of valid tiles, expected "
									+ (tileCountTotal-invalidTiles) 
										+ ", got " + validTiles + "...");
					} catch (FileNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}

		if (this.verbose.booleanValue())
			System.out.println("\t Found " + lines.size() + " feature vectors.");
		this.fireOperatorExecutionProgressEvent(
				new ALDOperatorExecutionProgressEvent(this,
						" found " + lines.size() + " feature vectors in total."));

		@SuppressWarnings("null")
		MTBTableModel dataSet = new MTBTableModel(lines.size(), heads.length+1);
		dataSet.setColumnName(0, "Index");
		for (int i=0;i<heads.length;++i) {
			// make sure that headers are unique, i.e., append a running ID
			dataSet.setColumnName(i+1, heads[i]+"_"+i);
		}
		for (int i=0;i<lines.size();++i) {
			String[] vals = lines.get(i).split("\t");
			for (int j=1;j<vals.length;++j) {
				dataSet.setValueAt(Double.valueOf(vals[j]), i, j-1);
			}
		}

		// do clustering
		if (this.verbose.booleanValue())
			System.out.println("\t Running k-means with " + this.clusterNum
					+ " clusters...");
		this.fireOperatorExecutionProgressEvent(
				new ALDOperatorExecutionProgressEvent(this, " running k-means..."));

		KMeans clusterer = new KMeans();
		clusterer.setInputData(dataSet);
		clusterer.setClusterNum(this.clusterNum);
		clusterer.setExcludeList(new int[]{1,2});
		clusterer.setVerbose(this.verbose);
		clusterer.runOp();
		MTBTableModel labels = clusterer.getDataLabels();

		// initialize colors for clusters
		DistinctColorListGenerator cGen = new DistinctColorListGenerator();
		cGen.setColorNumber(this.clusterNum);
		cGen.runOp();
		Color[] clusterColors = cGen.getColorList();

		if (this.verbose.booleanValue())
			System.out.println(operatorID + " Visualizing cluster assignments...");
		this.fireOperatorExecutionProgressEvent(
			new ALDOperatorExecutionProgressEvent(this,
				" visualizing cluster assignments..."));

		// visualize cluster assignment for all images
		int lineID = 0;
		ImageWriterMTB iWriter = new ImageWriterMTB();
		for (String file : featureFileList) {

			String basename = ALDFilePathManipulator.removeExtension(file);
			basename = basename.replaceAll("-features", "");

			if (this.verbose.booleanValue())
				System.out.println("\t Processing feature file " + file + "...");
			this.fireOperatorExecutionProgressEvent(
				new ALDOperatorExecutionProgressEvent(this,
					"	processing feature file \"" + file + "\"..."));

			MTBImageRGB clusterImage = (MTBImageRGB)MTBImage.createMTBImage(
				tileCountX, tileCountY, 1, 1, 1, MTBImageType.MTB_RGB);
			clusterImage.fillBlack();
			int tileIDprev = -1;
			int tileID = 0;
			do {
				tileID = ((Double)labels.getValueAt(lineID, 0)).intValue();
				if (tileID < tileIDprev || lineID == labels.getRowCount()-1)
					break;
				int label = ((Integer)labels.getValueAt(lineID,
						dataSet.getColumnCount())).intValue();
				int x = tileID % tileCountX;
				int y = tileID / tileCountX;
				clusterImage.putValueR(x,y,clusterColors[label-1].getRed());
				clusterImage.putValueG(x,y,clusterColors[label-1].getGreen());
				clusterImage.putValueB(x,y,clusterColors[label-1].getBlue());
				tileIDprev = tileID;
				++lineID;
			}	while (true);

			// save the cluster images
			iWriter.setFileName(basename + "-clusters.tif");
			iWriter.setInputMTBImage(clusterImage);
			iWriter.runOp();
			clusterImage = null;
		}

		// calculate percental cluster distribution per image and per cell
		if (this.verbose.booleanValue())
			System.out.println(operatorID + 
					" Calculating cluster distributions...");
		this.fireOperatorExecutionProgressEvent(
			new ALDOperatorExecutionProgressEvent(this,
				" calculating cluster distributions..."));

		// init file for global cluster statistics
		String globalStatFile =	this.imageDir.getDirectoryName() 
				+ File.separator + "AllCellsClusterStats.txt";
		BufferedWriter gWriter = null;
		try {
	    gWriter= new BufferedWriter(new FileWriter(globalStatFile));
	    gWriter.write("# Data directory: " + this.imageDir + "\n");
	    gWriter.write("# Image\tCell-ID");
	    for (int i=0; i<this.clusterNum; ++i) {
	    	gWriter.write("\tc-" + i);
	    }
	    gWriter.write("\n");
		} catch (IOException e1) {
			// reset writer, if initialization was not successful
			try {
				if (gWriter != null)
					gWriter.close();
			} catch (IOException e) {
				// nothing to do here
			}
			gWriter = null;
    }

		// identify group names -> identical to sub-directories
		String gName;
		this.cellGroupNames =  new TreeSet<String>();
		for (String dir : subdirs) {
			
			// skip irrelevant folders
			if (!dir.contains("results_features"))
				continue;

			gName = dir.replace(File.separator + "results_features", "");
			gName = Paths.get(gName).getFileName().toString();
			this.cellGroupNames.add(gName);
		}
		
		this.cellGroups =	new Vector<HashMap<String,HashMap<String,Double>>>();
		for (int i= 0; i < this.cellGroupNames.size(); ++i)
			this.cellGroups.add(new HashMap<String, HashMap<String,Double>>());

		int label = 0, absCount = 0;
		double absWeight = 0;
		String baseName, groupName, maskFolder;
		lineID = 0;
		for (String file : featureFileList) {

			// if we don't know the image size yet (e.g., because feature files
			// already existed), read first line of feature file, open the 
			// corresponding image file and try to figure out its size
			if (this.imageHeight == -1 || this.imageWidth == -1) {
				if (this.verbose.booleanValue()) {
					System.out.println("\t ... don't know image size yet, "
							+ "trying to get directly from input image file...");
				}
				this.fireOperatorExecutionProgressEvent(
						new ALDOperatorExecutionProgressEvent(this,
								"\t ... don't know image size yet, "
										+ "trying to get directly from input image file..."));
				try {
					BufferedReader fRead = 
							new BufferedReader(new FileReader(new File(file)));
					String imgFile = fRead.readLine().split(" ")[2];
					ImageReaderMTB ir = new ImageReaderMTB(imgFile);
					ir.runOp();
					this.imageWidth = ir.getResultMTBImage().getSizeX();
					this.imageHeight = ir.getResultMTBImage().getSizeY();
					fRead.close();
					if (this.verbose.booleanValue()) {
						System.out.println("\t ... got it!");
					}
					this.fireOperatorExecutionProgressEvent(
							new ALDOperatorExecutionProgressEvent(this, "\t ... got it!"));
				} catch (Exception e) {
					// there is nothing which could be done in this case...
					if (this.verbose.booleanValue()) {
						System.out.println("\t ... not successful, no mask data available!");
					}
					this.fireOperatorExecutionProgressEvent(
							new ALDOperatorExecutionProgressEvent(this,
									"\t ... not successful, no mask data available!"));
				}
			}
			
			baseName = ALDFilePathManipulator.getFileName(file);
			baseName = baseName.replaceAll("-features", "");

			groupName = ALDFilePathManipulator.getPath(file);
			groupName = groupName.replaceAll(
					Matcher.quoteReplacement(File.separator) + "results_features", "");
			groupName = Paths.get(groupName).getFileName().toString();

			if (this.verbose.booleanValue())
				System.out.println("\t Processing feature file " + file + "...");
			this.fireOperatorExecutionProgressEvent(
				new ALDOperatorExecutionProgressEvent(this,
					"	processing feature file \"" + file + "\"..."));

			// read mask for cell-wise calculations
			maskFolder = file.replace("features", "segmentation");
			maskFolder = maskFolder.replaceAll(
					Paths.get(maskFolder).getFileName().toString(), "");

			MTBImage maskImage = CytoskeletonFeatureExtractor.readMaskImage(
				maskFolder,	baseName,	this.maskFormat, 
					0, 0,	this.imageWidth-1, this.imageHeight-1, 
						this.verbose.booleanValue());
			
			// contains cluster distribution per cell, the key refers to the cell ID
			// in the image, the double array contains the relative frequencies
			HashMap<Integer, double[]> labelDistros =
				new HashMap<Integer, double[]>();
			absCount = 0;
			absWeight = 0;
			int tileIDprev = -1;
			int tileID = 0;
			do {
				tileID = ((Double)labels.getValueAt(lineID, 0)).intValue();
				if (tileID < tileIDprev || lineID == labels.getRowCount()-1)
					break;
				label = ((Integer)labels.getValueAt(lineID,
					dataSet.getColumnCount())).intValue();
				int x = tileID % tileCountX;
				int y = tileID / tileCountX;
				int cellID = 1;
				if (maskImage != null) {
					cellID = maskImage.getValueInt(
							x*tileShiftXFromFile, y*tileShiftYFromFile);
				}
				// usually cellID should not be 0 if mask image is ok, however, if
				// this nevertheless happens, just skip the tile
				if (cellID != 0) {
					if (labelDistros.get(new Integer(cellID)) == null)
						labelDistros.put(
							new Integer(cellID),new double[this.clusterNum+1]);
					++labelDistros.get(new Integer(cellID))[label];
					++absCount;
					++absWeight;
				}
				tileIDprev = tileID;
				++lineID;
			}	while(true);

			String outfile = file.replaceAll("-features", "-clusterDistro");
			try {
				BufferedWriter fWriter = new BufferedWriter(new FileWriter(outfile));
				fWriter.write("# Directory = " + this.imageDir + "\n");
				fWriter.write("# File = " + baseName + "\n");
				fWriter.write("# Tiles total = " + tileCountTotal + "\n");
				fWriter.write("# Tiles valid = " + absCount + "\n");

				fWriter.write("\n# Results per image:\n");
				for (int j=1;j<=this.clusterNum;++j) {
					// calculate total amount of tiles per cluster in image
					Set<Integer> keys = labelDistros.keySet();
					double sum = 0;
					for (Integer key: keys) {
						sum += labelDistros.get(key)[j];
					}
					fWriter.write(j + "\t" + sum + "\t" + sum/absWeight + "\n");
				}
				fWriter.write("\n# Results per cell:\n");
				Set<Integer> keys = labelDistros.keySet();
				for (Integer key: keys) {
					double sum = 0;
					for (int j=1;j<=this.clusterNum;++j) {
						sum += labelDistros.get(key)[j];
					}

					// add data to global statistics file
					if (gWriter != null)
						gWriter.write(baseName + "\t" + key);

					// generate stacked bar plots for each group of cells
					// (assumption: longest prefixes shared by more than one file
					//              are suitable to identify cell groups)
					HashMap< String, Double> hash = new HashMap<String, Double>();
					double[] distro = new double[this.clusterNum];
					for (int j=1;j<=this.clusterNum;++j) {
						fWriter.write(key + "\t" + j + "\t" + labelDistros.get(key)[j]
								+ "\t" + labelDistros.get(key)[j]/sum + "\n");
						// add data to global statistics file and remember for later
						distro[j-1] = labelDistros.get(key)[j]/sum;
						if (gWriter != null)
							gWriter.write("\t" + distro[j-1]);
						// stacked bar plot
						hash.put("c"+j, new Double(distro[j-1]));
					}

					// store distribution for later PCA and box-whisker plot
					this.cellwiseDistros.put(
							groupName + "_" + baseName + "-" + key, distro);

					// add hash to correct group
					int j =0;
					for (String g: this.cellGroupNames) {
						if (groupName.equals(g)) {
							this.cellGroups.get(j).put(
									groupName + "_" + baseName + "-" + key, hash);
							break;
						}
						++j;
					}

					// add data to global statistics file
					if (gWriter != null)
						gWriter.write("\n");
				}
				fWriter.close();
			} catch (IOException e) {
				System.err.println(operatorID + " Something went wrong writing " +
						"cluster distribution " + outfile + ", skipping file...");
				continue;
			}
		}
		// close global statistics file
		if (gWriter != null)
			try {
				gWriter.close();
			} catch (IOException e) {
				// no chance to do something reasonable at this point...
			}

		// generate a stacked bar plot for each group of cells
		this.stackedBarCharts = new Vector<JFreeChart>();
		int i = 0;
		for (String g: this.cellGroupNames) {
	    StackedBarChartPlotter plot = new StackedBarChartPlotter();
			plot.setTitle("Cluster distributions for group " + g);
			plot.setData(this.cellGroups.get(i));
			plot.setXAxisLabel("Cell-ID");
			plot.setYAxisLabel("Cluster probability");
			plot.setTickLabelSize(7);
			plot.setCategoryColors(clusterColors);
			plot.runOp();
			JFreeChart stackedBarChart =
				(JFreeChart)plot.getParameter("stackedBarChart");
			this.stackedBarCharts.add(stackedBarChart);
			// save the plot also to a file
			String filename = "";
      try {
      	filename = this.imageDir.getDirectoryName() + File.separator + g 
      		+ File.separator + "results_features" + File.separator 
      			+ g + "-distributionChart.png";
      	System.out.println(filename);
	      ChartUtils.saveChartAsPNG(
	      	new File(filename), stackedBarChart, 640, 400);
      } catch (IOException e) {
      	// problem on saving the plot to file, skip plot
      	System.err.println(operatorID + " saving plot for "
      		+ "\"" + g + "\" to \"" + filename + "\"" + " failed, skipping!");
      	this.fireOperatorExecutionProgressEvent(
    			new ALDOperatorExecutionProgressEvent(this,
    				"	WARNING! Saving plot for " + "\"" + g + "\"failed, skipping!"));
      }
      ++i;
		}

		// remember (sorted!) keys of cellwise distributions
		Set<String> keys = this.cellwiseDistros.keySet();
		this.cellwiseDistroKeys = new LinkedList<>();
		for (String k: keys) {
			this.cellwiseDistroKeys.add(k);
		}
		Collections.sort(this.cellwiseDistroKeys);
		
		// create distribution datasets
		this.distroData =	new double[this.clusterNum][this.cellwiseDistros.size()];
		this.distroDataGroups = new String[this.cellwiseDistros.size()];
		i = 0;
		for (String k: this.cellwiseDistroKeys) {
			groupName = k.split("_")[0];
			this.distroDataGroups[i] = groupName;
			for (int n = 0; n<this.clusterNum; ++n) {
				this.distroData[n][i] = this.cellwiseDistros.get(k)[n];
			}
			++i;
		}

		// generate a box-whisker plot of the distributions
		HashMap< String, HashMap<String, LinkedList<Double>> > groupClusterData =
			new HashMap< String, HashMap<String,LinkedList<Double>> >();
		for (String s: this.cellGroupNames) {
			groupClusterData.put(s, new HashMap<String, LinkedList<Double>>());
		}

		// for each indicator (here: cluster ID) and each category (here:
		// cell group) generate list of distribution data
		String group;
		for (String k: this.cellwiseDistroKeys) {
			double[] cellDistro = this.cellwiseDistros.get(k);

			// figure out to which group the cell belongs
			group = k.split("_")[0];
			// update the distribution lists for all clusters in that group
			for (int n = 0; n<this.clusterNum; ++n) {
				// get the list
				LinkedList<Double> list =
					groupClusterData.get(group).get("c"+new Integer(n+1));
				if (list == null) {
					list = new LinkedList<Double>();
					groupClusterData.get(group).put("c"+new Integer(n+1), list);
				}
				list.add(new Double(cellDistro[n]));
			}
		}

		// generate box-whisker-plot of the distribution data for each group
		BoxWhiskerChartPlotter boxPlotter = new BoxWhiskerChartPlotter();
		boxPlotter.setTitle("Cluster statistics");
		boxPlotter.setData(groupClusterData);
		boxPlotter.setXAxisLabel("Cell population");
		boxPlotter.setYAxisLabel("Cluster frequencies");
		boxPlotter.setTickLabelSize(7);
		boxPlotter.setCategoryColors(clusterColors);
		boxPlotter.runOp();
		this.boxWhiskerCharts = new Vector<JFreeChart>();
		this.boxWhiskerCharts.add(boxPlotter.getChart());

		// save the plot also to a file
		String filename = "";
    try {
    	filename = this.imageDir.getDirectoryName() + File.separator 
    			+ "AllCellsClusterDistributionChart.png";
      ChartUtils.saveChartAsPNG(
      	new File(filename), boxPlotter.getChart(), 640, 400);
    } catch (IOException e) {
    	// problem on saving the plot to file, skip plot
    	System.err.println(operatorID + " saving plot "
    		+ "\"AllCellsClusterDistributionChart.png\" to \"" + filename + "\""
    			+ " failed, skipping!");
    	this.fireOperatorExecutionProgressEvent(
  			new ALDOperatorExecutionProgressEvent(this,
  				"	WARNING! Saving plot \"AllCellsClusterDistributionChart.png\""
  					+ " failed, skipping!"));
    }
	}

	/**
	 * Performs a Karhunen-Loeve transformation on the cluster distribution data.
	 * <p>
	 * The subspace dimension is chosen so that at least 95% of the data
	 * variance is represented within the resulting feature subspace.
	 * The result is saved to the file {@code AllCellsPCASubspaceStats.txt}.
	 */
	private void doPCA() {

		if (this.verbose.booleanValue())
			System.out.println(operatorID + 
				" Performing PCA on cluster distributions...");
		this.fireOperatorExecutionProgressEvent(
			new ALDOperatorExecutionProgressEvent(this,
				" performing PCA on cluster distributions..."));

		// apply the PCA
		this.subspaceData = null;
		try {
			PCA pcaOp = new PCA();
			pcaOp.setDataset(this.distroData);
			pcaOp.setReductionMode(ReductionMode.PERCENTAGE_VARIANCE);
			pcaOp.setPercentageOfVariance(0.95);
			pcaOp.setMeanFreeData(false);
			pcaOp.setVerbose(this.verbose);
			pcaOp.runOp();
			this.subspaceData = pcaOp.getResultData();
		} catch (ALDException e) {
			System.err.println(operatorID + " something went wrong during PCA..");
		}

		if (this.subspaceData != null) {

			// get dimensionality of subspace
			int subspaceDim = this.subspaceData.length;

			String globalStatFile =
				this.imageDir.getDirectoryName() + File.separator 
					+ "AllCellsPCASubspaceStats.txt";
			BufferedWriter gWriter = null;
			try {
				gWriter= new BufferedWriter(new FileWriter(globalStatFile));
				gWriter.write("# Data directory: " + this.imageDir + "\n");
				gWriter.write("# Image\tCell-ID");
				for (int j=0; j<subspaceDim; ++j) {
					gWriter.write("\tc-" + j);
				}
				gWriter.write("\n");

				// save result to file
				int j = 0;
				for (String k: this.cellwiseDistroKeys) {
					gWriter.write(k);
					for (int n = 0; n<subspaceDim; ++n) {
						gWriter.write("\t" + this.subspaceData[n][j]);
					}
					gWriter.write("\n");
					++j;
				}
				gWriter.close();
			} catch (IOException e1) {
				// reset writer, if initialization was not successful
				if (gWriter != null)
					try {
						gWriter.close();
					} catch (IOException e) {
						// nothing to do here
					}
				gWriter = null;
			}
		}
	}

	/**
	 * Calculates pairwise distance matrices and similarity network data.
	 * <p> 
	 * Calculates pairwise Euclidean distances between all feature vectors and
	 * also between average vectors of all groups. In addition, for all groups
	 * a similarity network is constructed with each group forming a node and 
	 * the edges representing the similarity between the groups. The bigger the
	 * weight of an edge the larger the similarity between two groups.
	 * <p>
	 * If PCA is enabled for stage II the distances are calculated from the
	 * subspace feature data as extracted by the PCA. If PCA is not enabled
	 * the cluster distribution data is used directly as base for the
	 * calculations.
	 * <p>
	 * The resulting distance matrices are saved to files with names
	 * {@code AllCellsDistanceData.txt} and {@code AllGroupsDistanceData.txt}
	 * in the root directory. The similarity network data is stored in a file 
	 * named {@code AllGroupsSimilarityNetworkData.txt}.
	 */
	private void calculateDistanceMatrices() {

		if (this.verbose.booleanValue())
			System.out.println(operatorID 
				+	" Calculating pair- and groupwise Euclidean feature distances...");
		this.fireOperatorExecutionProgressEvent(
			new ALDOperatorExecutionProgressEvent(this,
				" calculating pair- and groupwise Euclidean feature distances..."));

		int c, d, r;
		double squareSum = 0.0;
		double[][] featureData;

		// select feature set (depends on PCA being enabled or disabled)
		if (this.subspaceData != null)
			featureData = this.subspaceData;
		else
			featureData = this.distroData;

		// get dimensionality of feature space
		int featureSpaceDim = featureData.length;

		// calculate matrix with pairwise Euclidean distances
		int sampleCount = featureData[0].length;
		double[][] distMatrix = new double[sampleCount][sampleCount];
		// diagonal is 0
		for (r = 0; r < sampleCount; ++r)
			for (c = 0; c < sampleCount; ++c)
				distMatrix[r][c] = 0;
		// matrix is symmetric, i.e., calculate only lower triangle
		for (r = 1; r < sampleCount; ++r) {
			for (c = 0; c < r; ++c) {
				squareSum = 0.0;
				for (d = 0; d < featureSpaceDim; ++d)
					squareSum += (featureData[d][r] - featureData[d][c])
						* (featureData[d][r] - featureData[d][c]);
				distMatrix[r][c] = Math.sqrt(squareSum);
				distMatrix[c][r] = distMatrix[r][c];
			}
		}
		
		// save matrix to file
		String cellDistMatrixFile = this.imageDir.getDirectoryName() 
				+ File.separator + "AllCellsDistanceData.txt";
		BufferedWriter mWriter = null;
		try {
			mWriter= new BufferedWriter(new FileWriter(cellDistMatrixFile));
			r = 0;
			for (String key: this.cellwiseDistroKeys) {
				mWriter.write(key);
				for (c = 0; c < sampleCount; ++c) {
					mWriter.write("\t" + distMatrix[r][c]);
				}
				++r;
				mWriter.write("\n");
			}
			mWriter.flush();
			mWriter.close();
		} catch (IOException e) {
			System.err.println(operatorID + " something went wrong writing "
				+ "distance file, skipping...!");
			e.printStackTrace();
			if (mWriter != null)
				try {
					mWriter.close();
				} catch (IOException e1) {
					// nothing to do here
				}
		}
		
		// average vectors per group
		HashMap<String, double[]> groupAverageVectors = new HashMap<>();
		double data[];
		String gName;
		for (r = 0; r < sampleCount; ++r) {
			gName = this.distroDataGroups[r];
			if (groupAverageVectors.get(gName) == null) {
				data = new double[featureSpaceDim+1];
				groupAverageVectors.put(gName, data);
			}
			else
				data = groupAverageVectors.get(gName);
			for (d = 0; d < featureSpaceDim; ++d) {
				data[d] += featureData[d][r];
			}
			++data[featureSpaceDim];
		}
		
		Set<String> gKeys = groupAverageVectors.keySet();
		int groupDistMatrixDim = gKeys.size();
		for (String key: gKeys) {
			data = groupAverageVectors.get(key);
			for (d = 0; d < featureSpaceDim; ++d) {
				data[d] /= data[featureSpaceDim];
			}			
		}
		
		String gNames[] = new String[groupDistMatrixDim];
		int i=0;
		for (String key: gKeys) {
			gNames[i] = key;
			++i;
		}
		java.util.Arrays.sort(gNames);
		
		double groupDistMax = 0;
		double dc[], dr[];
		double[][] groupDistMatrix = 
				new double[groupDistMatrixDim][groupDistMatrixDim];
		// diagonal is 0
		for (r = 0; r < groupDistMatrixDim; ++r)
			for (c = 0; c < groupDistMatrixDim; ++c)
				groupDistMatrix[r][c] = 0;
		// matrix is symmetric, i.e., calculate only lower triangle
		for (r = 1; r < groupDistMatrixDim; ++r) {
			dr = groupAverageVectors.get(gNames[r]);
			for (c = 0; c < r; ++c) {
				dc = groupAverageVectors.get(gNames[c]);
				squareSum = 0.0;
				for (d = 0; d < featureSpaceDim; ++d)
					squareSum += (dr[d] - dc[d]) * (dr[d] - dc[d]);
				groupDistMatrix[r][c] = Math.sqrt(squareSum);
				groupDistMatrix[c][r] = groupDistMatrix[r][c];
				// search for maximum
				if (groupDistMatrix[r][c] > groupDistMax)
					groupDistMax = groupDistMatrix[r][c];
			}
		}

		// save group distance matrix to file
		String groupDistMatrixFile = this.imageDir.getDirectoryName() 
				+ File.separator + "AllGroupsDistanceData.txt";
		mWriter = null;
		try {
			mWriter= new BufferedWriter(new FileWriter(groupDistMatrixFile));
			r = 0;
			for (String k: gNames) {
				mWriter.write(k);
				for (c = 0; c < groupDistMatrixDim; ++c) {
					mWriter.write("\t" + groupDistMatrix[r][c]);
				}
				++r;
				mWriter.write("\n");
			}
			mWriter.flush();
			mWriter.close();
		} catch (IOException e) {
			System.err.println(operatorID + " something went wrong writing "
				+ "group distance file, skipping...!");
			e.printStackTrace();
			if (mWriter != null)
				try {
					mWriter.close();
				} catch (IOException e1) {
					// nothing to do here
				}
		}
		
		// save group similarity network to file
		String groupSimilarityNetworkFile = this.imageDir.getDirectoryName() 
				+ File.separator + "AllGroupsSimilarityNetworkData.txt";
		mWriter = null;
		try {
			mWriter= new BufferedWriter(new FileWriter(groupSimilarityNetworkFile));
			mWriter.write("node1" + "\t" + "node2" + "\t" + "value" + "\n");
			double weight;
			String node1, node2;
			for (r = 0; r < groupDistMatrixDim; ++r) {
				node1 = gNames[r];
				for (c = r+1; c < groupDistMatrixDim; ++c) {
					node2 = gNames[c];
					weight = 1.0 - groupDistMatrix[r][c] / groupDistMax;
					mWriter.write(node1 + "\t" + node2 + "\t" + weight + "\n");
				}
			}
			mWriter.flush();
			mWriter.close();
		} catch (IOException e) {
			System.err.println(operatorID + " something went wrong writing "
				+ "group similarity network file, skipping...!");
			e.printStackTrace();
			if (mWriter != null)
				try {
					mWriter.close();
				} catch (IOException e1) {
					// nothing to do here
				}
		}
	}

	/**
	 * Callback routine to change parameters on change of flag for 
	 * enable/disable feature calculation.
	 */
	@SuppressWarnings("unused")
	private void calcFeatureFlagChanged() {
		try {
			if (this.doFeatureCalculation) {
				if (!this.hasParameter("featureExtractor")) {
					this.addParameter("featureExtractor");
				}
			} else {
				if (this.hasParameter("featureExtractor")) {
					this.removeParameter("featureExtractor");
				}
			}
			// add logging messages (FATAL) as soon as logj4 is configured
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (ALDOperatorException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public String getDocumentation() {
		return "This operator performs an unsupervised analysis of actin microfilament\n" + 
				"structures in sets of microscopy images. It takes as input a directory where\n" + 
				"the images are expected, and a set of segmentation masks showing the boundaries\n" + 
				"of individual cells in the images. The operator first calculates a set of\n" + 
				"texture features from the images and then clusters the feature vectors to\n" + 
				"identify structural patterns shared among different cells. Finally, each cell\n" + 
				"is characterized in terms of a distribution vector describing the appearance\n" + 
				"of various structural patterns in the cell. These vectors can then be used to\n" + 
				"analyze commonalities and differences between individual cells or groups of\n" + 
				"cells.\n" + 
				"\n" + 
				"<ul>\n" + 
				"<li><p><b>input:</b>\n" + 
				"<ul>\n" + 
				"<li><p><i>Image file folder</i>:<br> directory where the images are read from,\n" + 
				"	all files in a format supported by the Bioformats library are considered\n" + 
				"	</p></li>\n" + 
				"<li><p><i>Boundary file format</i>:<br> expected format of the contour \n" + 
				"	data files\n" + 
				"	<ul>\n" + 
				"	<li>LABEL_IMAGE:<br> a gray-scale image is expected where the area of each\n" + 
				"		cell is marked with a single unique gray-scale value;<br>\n" + 
				"		the files should share the names of the input image files and have the\n" + 
				"		ending \"-mask.tif\"\n" + 
				"	<li>IJ_ROIS:<br> an ImageJ 1.x file of ROI manager regions is expected;<br>\n" + 
				"		the files should share the names of the input image files and have the\n" + 
				"		ending \"-mask.zip\" or \"-mask.roi\"\n" + 
				"	</ul>\n" + 
				"<li><p><i>Cytoskeleton channel</i><br> if image stacks are provided you need\n" + 
				"	to specify in which channel of the images the cytoskeleton information is \n" + 
				"	located; indices start with 1</p></li>\n" + 
				"<li><p><i>Calculate features?</i><br> by disabeling this option the \n" + 
				"	time-consuming feature calculation can be omitted, however, in this case a\n" + 
				"	sub-folder 'results_features' is expected to already contain the feature \n" + 
				"	files</p></li>\n" + 
				"<li><p><i>Feature extractor</i>:<br> operator used for quantifying texture\n" + 
				"	</p></li>\n" + 
				"<li><p><i>Tile size x/y and Tile shift x/y</i>:<br> global configuration of\n" + 
				"	texture quantification stage, i.e., size of sliding window and size of\n" + 
				"	shifts; if shifts are smaller than tile size window positions are overlapping \n" + 
				"	</p></li>\n" + 
				"<li><p><i>Number of feature clusters</i>:<br> number of clusters to be used for\n" + 
				"	feature vector clustering, should approximately refer to the expected number\n" + 
				"	of structural patterns appearing in the cells</p></li>\n" + 
				"<li><p><i>Do PCA in stage II?</i><br> enable or disable the PCA prior to the\n" + 
				"	pairwise distance calculations in stage II of the approach</p></li>\n" + 
				"<li><p><i>Optional: Cell boundary file folder</i><br> if the contour data is\n" + 
				"	not stored in group-wise sub-folders named 'results_segmentation' you can\n" + 
				"	specify alternative locations via this parameter</p></li>\n" + 
				"</ul>\n" + 
				"\n" + 
				"<li><p><b>output:</b>\n" + 
				"<ul>\n" + 
				"<li><p><i>Resulting chart plots for each group</i>:<br>\n" + 
				"	cluster distributions for each cell categorized into cell groups\n" + 
				"<li><p><i>Resulting box-whisker plot</i>:<br> box plot of the relative frequecies\n" + 
				"	of appearance of each	cluster for the different cell groups\n" + 
				"</ul>\n" + 
				"In addition to the output data directly displayed on termination of the operator\n" + 
				"some more result data files are written to the output directory. In particular,\n" + 
				"a file with the pairwise Euclidean distances between distribution vectors as \n" + 
				"well as between average vectors of the groups can be found there which can be \n" + 
				"further analyzed, e.g., with\n" + 
				"<a href=\"http://deim.urv.cat/~sgomez/multidendrograms.php\">MultiDendrograms</a>\n" + 
				"or our R script provided for that purpose. Also images visualizing the cluster \n" + 
				"distributions for each input image and each cell, respectively, are available.\n" + 
				"\n" + 
				"</ul>\n" + 
				"\n" + 
				"<p>\n" + 
				"For more details about the operator and additional information on the parameters\n" + 
				"refer to its webpage:\n" + 
				"<a href=\"http://mitobo.informatik.uni-halle.de/index.php/Applications/CytoskeletonAnalyzer2D\">\n" + 
				"http://mitobo.informatik.uni-halle.de/index.php/Applications/CytoskeletonAnalyzer2D</a>.\n";
	}
}

