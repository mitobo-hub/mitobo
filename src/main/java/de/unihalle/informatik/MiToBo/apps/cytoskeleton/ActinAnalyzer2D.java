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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;
import java.util.Vector;

import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;

import de.unihalle.informatik.Alida.datatypes.ALDDirectoryString;
import de.unihalle.informatik.Alida.exceptions.ALDException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException.OperatorExceptionType;
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
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.images.*;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.imageJ.RoiManagerAdapter;
import de.unihalle.informatik.MiToBo.core.operator.*;
import de.unihalle.informatik.MiToBo.gui.MTBTableModel;
import de.unihalle.informatik.MiToBo.io.dirs.DirectoryTree;
import de.unihalle.informatik.MiToBo.io.images.ImageReaderMTB;
import de.unihalle.informatik.MiToBo.io.images.ImageWriterMTB;
import de.unihalle.informatik.MiToBo.math.statistics.PCA;
import de.unihalle.informatik.MiToBo.math.statistics.PCA.ReductionMode;
import de.unihalle.informatik.MiToBo.tools.strings.StringAnalysis;
import de.unihalle.informatik.MiToBo.visualization.drawing.DrawRegion2DSet;
import de.unihalle.informatik.MiToBo.visualization.drawing.DrawRegion2DSet.DrawType;
import de.unihalle.informatik.MiToBo.visualization.plots.BoxWhiskerChartPlotter;
import de.unihalle.informatik.MiToBo.visualization.plots.StackedBarChartPlotter;

/**
 * Operator for analyzing actin filament structures in 2D images.
 *
 * @author moeller
 */
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.ALL,
	level=Level.APPLICATION, allowBatchMode=false)
public class ActinAnalyzer2D extends MTBOperator {

	/**
	 * Type of features to characterize local structure.
	 */
//	public static enum FeatureType {
//		/**
//		 * Use Haralick features as structure measures.
//		 */
//		HARALICK_MEASURES,
//		/**
//		 * Use Eigen structures as structure measures.
//		 */
//		EIGEN_STRUCTURES
//	}

	/**
	 * Default directions for Haralick features.
	 */
//	private static Vector<HaralickDirection> defaultDirections;

	// this initialization block is necessary to set default directions
//	static {
//		defaultDirections =
//			new Vector<FeatureCalculatorHaralickMeasures.HaralickDirection>();
//		defaultDirections.add(HaralickDirection.EAST);
//		defaultDirections.add(HaralickDirection.NORTH_EAST);
//		defaultDirections.add(HaralickDirection.NORTH);
//		defaultDirections.add(HaralickDirection.NORTH_WEST);
//	}

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
	 * Flag for calculating features.
	 * <p>
	 * If set to false, the name of a directory containing the features
	 * must be provided.
	 */
	@Parameter( label= "Calculate features?", required = true,
		dataIOOrder = 0, direction = Direction.IN,
		description = "Flag to enable/disable feature calculation.",
		mode = ExpertMode.STANDARD, callback = "calcFeatureFlagChanged",
		paramModificationMode = ParameterModificationMode.MODIFIES_INTERFACE)
	protected boolean doFeatureCalculation = true;

	/**
	 * Type of features to apply.
	 */
//	@Parameter( label= "Type of features", required = true,
//		dataIOOrder = 1, direction = Direction.IN,
//		description = "Select type of features to apply.",
//		mode = ExpertMode.STANDARD)
//	protected FeatureType featureType = FeatureType.HARALICK_MEASURES;

	/**
	 * Feature extractor to apply.
	 */
	@Parameter( label= "Feature Extractor", required = true,
		dataIOOrder = 1, direction = Direction.IN,
		description = "Select type of features to apply.",
		mode = ExpertMode.STANDARD)
	protected CytoskeletonFeatureExtractor featureExtractor =
		new CytoskeletonFeatureExtractorHaralickMeasures();

	/**
	 * Feature directory.
	 */
	@Parameter( label= "Feature Input Directory", required = true,
		dataIOOrder = 2, direction = Direction.IN, mode = ExpertMode.STANDARD,
		description = "Feature directory, may be the same as output directory.")
	protected ALDDirectoryString featureDir = null;

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
//	@Parameter( label= "Haralick distance", required = true,
//		direction = Parameter.Direction.IN, dataIOOrder=7,
//		mode=ExpertMode.STANDARD, description = "Desired distance.")
//	protected int distance= 4;

	/**
	 * Set of directions.
	 */
//	@Parameter( label= "Set of directions", required = true,
//		direction = Parameter.Direction.IN, dataIOOrder=8,
//		mode=ExpertMode.STANDARD, description = "Desired directions.")
//	protected Vector<HaralickDirection> directions = defaultDirections;

	/**
	 * Flag for isotropic calculations.
	 * <p>
	 * If flag is true the average of each feature for the four directions EAST,
	 * NORTH_EAST, NORTH and NORTH_WEST is taken as result. But, note that this
	 * causes the computation time to be increased by a factor of four as well.
	 */
//	@Parameter( label= "Isotropic calculations", required = true,
//		direction = Parameter.Direction.IN, dataIOOrder=9,
//		description = "Flag to enable isotropic calculations.",
//		mode=ExpertMode.ADVANCED)
//	protected boolean isotropicCalcs = false;

	/**
	 * Number of clusters to be used in feature clustering.
	 */
	@Parameter( label= "Number of feature clusters", required = true,
		dataIOOrder = 10, direction = Parameter.Direction.IN,
		mode = ExpertMode.ADVANCED, description = "Number of feature clusters.")
	protected int clusterNum = 6;

	/**
	 * Perform PCA in second stage?
	 */
	@Parameter( label= "Do PCA in stage II?", required = true,
		dataIOOrder = 11, direction = Parameter.Direction.IN,
		mode = ExpertMode.ADVANCED,
		description = "Enable/disable PCA prior to hierarchical clustering.")
	protected boolean doPCA = true;

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

	/**
	 * Width of the images taking first image as reference.
	 */
	private transient int imageWidth = -1;

	/**
	 * Height of the images taking first image as reference.
	 */
	private transient int imageHeight = -1;

	/**
	 * List of group names, filled in {@link #clusterFeatures()}.
	 */
	private transient Vector<String> cellGroupNames;

	/**
	 * Map of cluster distributions per cell, filled in
	 * {@link #clusterFeatures()}.
	 */
	private transient HashMap<String, double[]> cellwiseDistros =
		new HashMap<String, double[]>();

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
	 * Dimension-reduced cluster distribution data.
	 */
	private transient double[][] subspaceData;

	/**
	 * Default constructor.
	 * @throws ALDOperatorException Thrown in case of failure.
	 */
	public ActinAnalyzer2D() throws ALDOperatorException {
		// configure operator to calculate features by default
		this.setParameter("doFeatureCalculation", true);
	}

	@Override
  protected void operate()
  		throws ALDOperatorException, ALDProcessingDAGException {

		// for each image calculate feature vectors (if requested)
		if (this.doFeatureCalculation) {
//			switch(this.featureType)
//			{
//			case HARALICK_MEASURES:
//				ActinFeatureExtractorHaralickMeasures haralickOp =
//					new ActinFeatureExtractorHaralickMeasures();
//				haralickOp.setImageDir(this.imageDir);
//				haralickOp.setMaskDir(this.maskDir);
//				haralickOp.setMaskFormat(this.maskFormat);
//				haralickOp.setOutputDir(this.outDir);
//				haralickOp.setTileSizeX(this.tileSizeX);
//				haralickOp.setTileSizeY(this.tileSizeY);
//				haralickOp.setTileShiftX(this.tileShiftX);
//				haralickOp.setTileShiftY(this.tileShiftY);
//				haralickOp.setDistance(this.distance);
//				haralickOp.setHaralickDirections(this.directions);
//				haralickOp.setFlagIsotropicCalculations(this.isotropicCalcs);
//				haralickOp.setVerbose(this.verbose);
//				haralickOp.runOp();
//				break;
//			case EIGEN_STRUCTURES:
//				ActinFeatureExtractorEigenStructures eigenOp =
//					new ActinFeatureExtractorEigenStructures();
//				eigenOp.setImageDir(this.imageDir);
//				eigenOp.setMaskDir(this.maskDir);
//				eigenOp.setMaskFormat(this.maskFormat);
//				eigenOp.setOutputDir(this.outDir);
//				eigenOp.setTileSizeX(this.tileSizeX);
//				eigenOp.setTileSizeY(this.tileSizeY);
//				eigenOp.setTileShiftX(this.tileShiftX);
//				eigenOp.setTileShiftY(this.tileShiftY);
//				eigenOp.setVerbose(this.verbose);
//				eigenOp.runOp();
//				break;
//			}
			if (this.verbose.booleanValue())
				System.out.println("Feature extractor: "
						+ this.featureExtractor.getName());
			this.featureExtractor.setImageDir(this.imageDir);
			this.featureExtractor.setMaskDir(this.maskDir);
			this.featureExtractor.setMaskFormat(this.maskFormat);
			this.featureExtractor.setOutputDir(this.outDir);
			this.featureExtractor.setTileSizeX(this.tileSizeX);
			this.featureExtractor.setTileSizeY(this.tileSizeY);
			this.featureExtractor.setTileShiftX(this.tileShiftX);
			this.featureExtractor.setTileShiftY(this.tileShiftY);
			this.featureExtractor.setVerbose(this.verbose);
			this.featureExtractor.runOp();
		}

		// cluster the features and analyze the distributions
		this.cellwiseDistros = new HashMap<String, double[]>();
		this.clusterFeatures();

		// perform PCA
		if (this.doPCA)
			this.doPCA();

		// calculate pairwise distances for further analysis
		this.calculatePairwiseDistances();
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
			System.out.println("[ActinAnalyzer2D] Reading feature files...");
		this.fireOperatorExecutionProgressEvent(
     	new ALDOperatorExecutionProgressEvent(this,
     		" reading features files..."));

		int tileCountX = -1, tileCountY = -1;
		int tileCountTotal = -1, invalidTiles = -1;
		int tileSizeXFromFile = -1, tileSizeYFromFile = -1;
		int tileShiftXFromFile = -1, tileShiftYFromFile = -1;

		String fDir = (this.doFeatureCalculation ?
				this.outDir : this.featureDir).getDirectoryName();
		DirectoryTree dirTree = new DirectoryTree(fDir);
		Vector<String> imageList = dirTree.getFileList();
		Vector<String> lines = new Vector<String>();
		String[] heads = null;
		Vector<String> featureFileList = new Vector<String>();
		for (String file : imageList) {

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
						System.err.println("[ActinAnalyzer2D] "
							+ "tile sizes in x of different images do not match!");
				line = fRead.readLine();
				int sizeY = Integer.valueOf(line.split(" ")[2]).intValue();
				if (tileSizeYFromFile == -1)
					tileSizeYFromFile = sizeX;
				else
					if (tileSizeYFromFile != sizeY)
						System.err.println("[ActinAnalyzer2D] "
							+ "tile sizes in y of different images do not match!");
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
					System.err.println("[ActinAnalyzer2D] number of feature vectors "
						+ "does not match number of valid tiles, expected "
							+ (tileCountTotal-invalidTiles) + ", got " + validTiles + "...");
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		if (this.verbose.booleanValue())
			System.out.println("\t Found " + lines.size() + " feature vectors.");
		this.fireOperatorExecutionProgressEvent(
     	new ALDOperatorExecutionProgressEvent(this,
     		" found " + lines.size() + " feature vectors."));

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
			System.out.println("[ActinAnalyzer2D] Visualizing cluster assignments...");
		this.fireOperatorExecutionProgressEvent(
			new ALDOperatorExecutionProgressEvent(this,
				" visualizing cluster assignments..."));

		// visualize cluster assignment for all images
		int lineID = 0;
		ImageWriterMTB iWriter = new ImageWriterMTB();
		for (String file : featureFileList) {

			String basename = ALDFilePathManipulator.getFileName(file);
			basename = basename.replaceAll("-features", "");

			if (this.verbose.booleanValue())
				System.out.println("\t Processing feature file " + file + "...");
			this.fireOperatorExecutionProgressEvent(
				new ALDOperatorExecutionProgressEvent(this,
					"	processing feature file \"" + basename + "\"..."));

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
			iWriter.setFileName(this.outDir + File.separator
					+ basename + "-clusters.tif");
			iWriter.setInputMTBImage(clusterImage);
			iWriter.runOp();
			clusterImage = null;
		}

		// calculate percental cluster distribution per image and per cell
		if (this.verbose.booleanValue())
			System.out.println("[ActinAnalyzer2D] "
				+ "Calculating cluster distributions...");
		this.fireOperatorExecutionProgressEvent(
			new ALDOperatorExecutionProgressEvent(this,
				" calculating cluster distributions..."));

		// init file for global cluster statistics
		String globalStatFile =
			this.outDir + File.separator + "AllImagesClusterStatistics.txt";
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
			gWriter = null;
    }

  	// identify cell groups
  	Vector<String> basenames =  new Vector<String>();
		for (String file : featureFileList) {
			// remove suffix added before
			String prefix =
				ALDFilePathManipulator.getFileName(file).replaceAll("-features", "");
			// search for underscore as identifier, part in front of it is group name
			String[] splitName = prefix.split("_");
			if (splitName.length != 2)
				throw new ALDOperatorException(OperatorExceptionType.OPERATE_FAILED,
					"[ActinAnalyzer2D] can't identify cell groups, no underscore found!");
			basenames.add(splitName[0]);
		}
		// extract an unordered list of cell group names, i.e., image prefixes
		this.cellGroupNames =
				StringAnalysis.getLongestCommonPrefixes(basenames);
		// check if all names belong to one equivalence group
		for (String filename: basenames) {
			boolean groupFound = false;
			for (String group: this.cellGroupNames) {
				if (filename.startsWith(group)) {
					groupFound = true;
					break;
				}
			}
			if (!groupFound)
				this.cellGroupNames.add(filename);
		}

		this.cellGroups =	new Vector<HashMap<String,HashMap<String,Double>>>();
		for (int i= 0; i < this.cellGroupNames.size(); ++i)
			this.cellGroups.add(new HashMap<String, HashMap<String,Double>>());

		int label = 0, absCount = 0;
		double absWeight = 0;
		lineID = 0;
		for (String file : featureFileList) {

			String basename = ALDFilePathManipulator.getFileName(file);
			basename = basename.replaceAll("-features", "");

			if (this.verbose.booleanValue())
				System.out.println("\t Processing feature file " + file + "...");
			this.fireOperatorExecutionProgressEvent(
				new ALDOperatorExecutionProgressEvent(this,
					"	processing feature file \"" + basename + "\"..."));

			// read mask for cell-wise calculations
			MTBImage maskImage = this.readMaskImage(basename,
				0, 0,	this.imageWidth-1, this.imageHeight-1);

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

			String outfile = this.outDir + File.separator
					+ basename + "-clusterDistro.txt";
			try {
				BufferedWriter fWriter = new BufferedWriter(new FileWriter(outfile));
				fWriter.write("# Directory = " + this.imageDir + "\n");
				fWriter.write("# File = " + basename + "\n");
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
						gWriter.write(basename + "\t" + key);

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
					this.cellwiseDistros.put(basename + "-" + key, distro);

					// add hash to correct group
					int j =0;
					for (String g: this.cellGroupNames) {
						if (basename.startsWith(g)) {
							this.cellGroups.get(j).put(
								basename.replace(g + "_", "") + "-" + key, hash);
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
				System.err.println("[ActinAnalyzer2D] Something went wrong writing " +
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
      	filename = this.outDir + File.separator + g + "-distributionChart.png";
	      ChartUtils.saveChartAsPNG(
	      	new File(filename), stackedBarChart, 640, 400);
      } catch (IOException e) {
      	// problem on saving the plot to file, skip plot
      	System.err.println("[ActinAnalyzer2D] saving plot for "
      		+ "\"" + g + "\" to \"" + filename + "\"" + " failed, skipping!");
      	this.fireOperatorExecutionProgressEvent(
    			new ALDOperatorExecutionProgressEvent(this,
    				"	WARNING! Saving plot for " + "\"" + g + "\"failed, skipping!"));
      }
      ++i;
		}

		// create distribution dataset
		this.distroData =	new double[this.clusterNum][this.cellwiseDistros.size()];
		i = 0;
		for (String k: this.cellwiseDistros.keySet()) {
			for (int n = 0; n<this.clusterNum; ++n)
				this.distroData[n][i] = this.cellwiseDistros.get(k)[n];
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
		for (String k: this.cellwiseDistros.keySet()) {
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
    	filename = this.outDir +File.separator+ "AllGroupsClusterStatsChart.png";
      ChartUtils.saveChartAsPNG(
      	new File(filename), boxPlotter.getChart(), 640, 400);
    } catch (IOException e) {
    	// problem on saving the plot to file, skip plot
    	System.err.println("[ActinAnalyzer2D] saving plot "
    		+ "\"AllGroupsClusterStatsChart.png\" to \"" + filename + "\""
    			+ " failed, skipping!");
    	this.fireOperatorExecutionProgressEvent(
  			new ALDOperatorExecutionProgressEvent(this,
  				"	WARNING! Saving plot \"AllGroupsClusterStatsChart.png\""
  					+ " failed, skipping!"));
    }
	}

	/**
	 * Performs a Karhunen-Loeve transformation on the cluster distribution data.
	 * <p>
	 * The subspace dimension is chosen so that at least 95% of the data
	 * variance is represented within the resulting feature subspace.
	 * The result is saved to the file "AllImagesSubspaceFeatures.txt".
	 */
	private void doPCA() {

		if (this.verbose.booleanValue())
			System.out.println("[ActinAnalyzer2D] " +
				"Performing PCA on cluster distributions...");
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
			System.err.println("[ActinAnalyzer2D] something went wrong during PCA..");
		}

		if (this.subspaceData != null) {

			// get dimensionality of subspace
			int subspaceDim = this.subspaceData.length;

			String globalStatFile =
				this.outDir + File.separator + "AllImagesSubspaceFeatures.txt";
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
				for (String k: this.cellwiseDistros.keySet()) {
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
				gWriter = null;
			}
		}
	}

	/**
	 * Calculates pairwise Euclidean distances between all feature vectors.
	 * <p>
	 * If PCA is enabled for stage II the distances are calculated from the
	 * subspace feature data as extracted by the PCA. If PCA is not enabled
	 * the cluster distribution data is used directly as base for the
	 * calculations.
	 * <p>
	 * The resulting distance matrix is saved to a file with name
	 * {@code AllImagesSubspaceFeatures.txt} in the given output directory.
	 */
	private void calculatePairwiseDistances() {

		if (this.verbose.booleanValue())
			System.out.println("[ActinAnalyzer2D] " +
				"Calculating pairwise Euclidean feature distances...");
		this.fireOperatorExecutionProgressEvent(
			new ALDOperatorExecutionProgressEvent(this,
				" calculating pairwise Euclidean feature distances..."));

		double[][] featureData;

		// select feature set (depends on PCA being enabled or disabled)
		if (this.subspaceData != null)
			featureData = this.subspaceData;
		else
			featureData = this.distroData;

		// get dimensionality of feature space
		int subspaceDim = featureData.length;

		String globalStatFile =
				this.outDir + File.separator + "AllImagesSubspaceFeatures.txt";
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
			for (String k: this.cellwiseDistros.keySet()) {
				gWriter.write(k);
				for (int n = 0; n<subspaceDim; ++n) {
					gWriter.write("\t" + featureData[n][j]);
				}
				gWriter.write("\n");
				++j;
			}
			gWriter.close();
		} catch (IOException e1) {
			// reset writer, if initialization was not successful
			gWriter = null;
		}

		// calculate matrix with pairwise Euclidean distances
		int sampleCount = featureData[0].length;
		double[][] distMatrix = new double[sampleCount][sampleCount];
		// diagonal is 0
		for (int r = 0; r < sampleCount; ++r)
			for (int c = 0; c < sampleCount; ++c)
				distMatrix[r][c] = 0;
		// matrix is symmetric, i.e., calculate only lower triangle
		for (int r = 1; r < sampleCount; ++r) {
			for (int c = 0; c < r; ++c) {
				double squareSum = 0.0;
				for (int d=0; d<subspaceDim; ++d)
					squareSum += (featureData[d][r] - featureData[d][c])
						* (featureData[d][r] - featureData[d][c]);
				distMatrix[r][c] = Math.sqrt(squareSum);
				distMatrix[c][r] = distMatrix[r][c];
			}
		}

		// save matrix to file
		String multiDendroFile =
				this.outDir + File.separator + "AllImagesPairwiseDistanceData.txt";
		BufferedWriter mWriter = null;
		try {
			mWriter= new BufferedWriter(new FileWriter(multiDendroFile));
			int r = 0;
			for (String k: this.cellwiseDistros.keySet()) {
				mWriter.write(k);
				for (int c = 0; c < sampleCount; ++c) {
					mWriter.write("\t" + distMatrix[r][c]);
				}
				++r;
				mWriter.write("\n");
			}
			mWriter.close();
		} catch (IOException e) {
			System.err.println("[ActinAnalyzer] something went wrong writing "
				+ "distance file, skipping...!");
			e.printStackTrace();
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
	private MTBImage readMaskImage(String basename,
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
						regionDrawOp.runOp();
						maskImage = regionDrawOp.getResultImage();
						// save the label image to the output directory
						String outMaskName= this.outDir.getDirectoryName() + File.separator
							+ basename + "-mask.tif";
						ImageWriterMTB imgWriter =
							new ImageWriterMTB(maskImage, outMaskName);
						imgWriter.setOverwrite(true);
						imgWriter.runOp();
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

	/**
	 * Callback routine to change parameters on change of flag for enable/disable feature calculation.
	 */
	@SuppressWarnings("unused")
	private void calcFeatureFlagChanged() {
		try {
			if (this.doFeatureCalculation) {
				if (this.hasParameter("featureDir")) {
					this.removeParameter("featureDir");
				}

				if (!this.hasParameter("featureExtractor")) {
					this.addParameter("featureExtractor");
				}
			} else {
				if (this.hasParameter("featureExtractor")) {
					this.removeParameter("featureExtractor");
				}

				if (!this.hasParameter("featureDir")) {
					this.addParameter("featureDir");
				}
			}
			// add logging messages (FATAL) as soon as logj4 is configured
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ALDOperatorException e) {
			e.printStackTrace();
		}
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
