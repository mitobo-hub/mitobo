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

import java.awt.geom.Point2D;
import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;
import java.util.TreeSet;
import java.util.Vector;

import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.Alida.annotations.Parameter.ParameterModificationMode;
import de.unihalle.informatik.Alida.datatypes.ALDDirectoryString;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException.OperatorExceptionType;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.Alida.helpers.ALDFilePathManipulator;
import de.unihalle.informatik.Alida.operator.events.ALDOperatorExecutionProgressEvent;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBContour2D;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBContour2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2D;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageByte;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageDouble;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageRGB;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageShort;
import de.unihalle.informatik.MiToBo.core.imageJ.RoiManagerAdapter;
import de.unihalle.informatik.MiToBo.core.imageJ.RoiWriter;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;
import de.unihalle.informatik.MiToBo.enhance.GlobalContrastStretching;
import de.unihalle.informatik.MiToBo.features.MorphologyAnalyzer2D;
import de.unihalle.informatik.MiToBo.features.MorphologyAnalyzer2DInProData;
import de.unihalle.informatik.MiToBo.features.MorphologyAnalyzer2D.FeatureNames;
import de.unihalle.informatik.MiToBo.features.MorphologyAnalyzer2DInProData.InProContourSegment;
import de.unihalle.informatik.MiToBo.filters.linear.GaussFilter;
import de.unihalle.informatik.MiToBo.filters.linear.anisotropic.GaussPDxxFilter2D;
import de.unihalle.informatik.MiToBo.filters.linear.anisotropic.OrientedFilter2DBatchAnalyzer;
import de.unihalle.informatik.MiToBo.filters.nonlinear.RankOperator;
import de.unihalle.informatik.MiToBo.filters.nonlinear.RankOperator.RankOpMode;
import de.unihalle.informatik.MiToBo.gui.MTBTableModel;
import de.unihalle.informatik.MiToBo.io.dirs.DirectoryTree;
import de.unihalle.informatik.MiToBo.io.images.ImageReaderMTB;
import de.unihalle.informatik.MiToBo.io.images.ImageWriterMTB;
import de.unihalle.informatik.MiToBo.morphology.BasicMorphology;
import de.unihalle.informatik.MiToBo.morphology.BasicMorphology.maskShape;
import de.unihalle.informatik.MiToBo.morphology.ComponentPostprocess;
import de.unihalle.informatik.MiToBo.morphology.ComponentPostprocess.ProcessMode;
import de.unihalle.informatik.MiToBo.morphology.SkeletonExtractor;
import de.unihalle.informatik.MiToBo.morphology.BinaryImageEndpointTools;
import de.unihalle.informatik.MiToBo.morphology.WatershedBinary;
import de.unihalle.informatik.MiToBo.segmentation.regions.filling.FillHoles2D;
import de.unihalle.informatik.MiToBo.segmentation.regions.labeling.LabelAreasToRegions;
import de.unihalle.informatik.MiToBo.segmentation.regions.labeling.LabelComponentsSequential;
import de.unihalle.informatik.MiToBo.segmentation.thresholds.ImgThreshNiblack;
import de.unihalle.informatik.MiToBo.segmentation.thresholds.ImgThreshNiblack.Mode;
import de.unihalle.informatik.MiToBo.visualization.colormappings.GrayscaleImageToHeatmap;
import ij.process.ImageProcessor;

/**
 * Operator for segmenting and analyzing pavement cell shape.
 * <p>
 * The operator first applies several preprocessing steps to a given 
 * input image, e.g., noise reduction by filtering, binarization, and
 * several morphological operations. The outcome of this first stage
 * are binary regions corresponding to the cells in the image. Note that
 * the input image can either be given by an 8-bit grayscale image or an
 * already presegmented binary image. Select the input mode accordingly.
 * <p>
 * Subsequently, the cell regions are filtered according to area, i.e.
 * too small regions are removed. In addition we seek to exclude regions
 * from further processing which originate from more than one physical
 * cell and have accidentally been merged into a single region during
 * preprocessing and segmentation.
 * <p>
 * The final stage comprises the calculation of a set of features for
 * each region suitable to characterize the region morphology. In this
 * stage we use the {@link MorphologyAnalyzer2D} operator.
 * 
 * @author moeller
 */
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.ALL, 
	level=Level.STANDARD, allowBatchMode = false)
public class PaCeQuant extends MTBOperator {

	/**
	 * Phases to run.
	 */
	public static enum OperatorPhasesToRun {
		/**
		 * Segmentation and feature extraction are both done.
		 */
		SEGMENTATION_AND_FEATURES,
		/**
		 * Only segmentation is performed on the input images.
		 */
		SEGMENTATION_ONLY,
		/**
		 * Only features are extracted.
		 */
		FEATURES_ONLY
	}
	
	/**
	 * Operation mode of the operator.
	 */
	public static enum OperationMode {
		/**
		 * Process a single image or ROI set provided directly.
		 */
		INTERACTIVE,
		/**
		 * Process images / ROIs in the given directory.
		 */
		BATCH
	}
	
	/**
	 * Formats for input of external segmentation results.
	 */
	public static enum SegmentationInputFormat {
		/**
		 * Binary image with cell regions in white.
		 */
		BINARY_IMAGE,
		/**
		 * Label image with a unique label for each cell.
		 */
		LABEL_IMAGE,
		/**
		 * ImageJ ROIs.
		 */
		IMAGEJ_ROIs
	}

	/**
	 * Image type.
	 */
	public static enum ImageType {
		/**
		 * Process a fluorescence image.
		 */
		FLUORESCENCE_IMAGE,
		/**
		 * Process an agarose imprint.
		 */
		AGAROSE_IMPRINT
	}

	/**
	 * Mode of calibrating pixel size.
	 */
	public static enum PixelCalibration {
		/**
		 * Automatically configure pixel calibration from input data.
		 */
		AUTO,
		/**
		 * User specifies pixel calibration. 
		 */
		USER
	}
	
	/**
	 * Border color.
	 */
	public static enum BorderBackgroundContrast {
		/**
		 * Dark borders on bright background.
		 */
		DARK_ON_BRIGHT,
		/**
		 * Bright borders on dark background.
		 */
		BRIGHT_ON_DARK
	}	
	
	/**
	 * Heuristic for closing border gaps.
	 */
	public static enum GapCloseMode {
		/**
		 * Do not try to close gaps.
		 */
		NONE,
		/**
		 * Use watershed transformation.
		 */
		WATERSHED,
		/**
		 * Simply link adjacent endpoints.
		 */
		NAIVE_HEURISTIC
	}
	
	/**
	 * Unit for measurements.
	 */
	public static enum MeasurementUnits {
		/**
		 * Measure length and areas in pixels.
		 */
		PIXELS,
		/**
		 * Measure length and areas in microns.
		 */
		MICRONS
	}

	/**
	 * Different types of lobes.
	 */
	public static enum LobeTypes {
		/**
		 * Lobe is of type 1-cell, i.e. has a single neighbor.
		 */
		TYPE_1,
		/**
		 * Lobe is of type 2-cell, i.e. has two neighbors.
		 */
		TYPE_2,
		/**
		 * Lobe type cannot be determined due to missing data in the neighborhood.
		 */
		UNDEFINED
	}
	
	/**
	 * Identifier string for this operator class.
	 */
	private static final String operatorID = "[PaCeQuant]";
	
	/**
	 * List of parameters relevant for segmentation phase.
	 */
	private static transient String[] segmentationParameters = {
			"phaseAInfo", "borderContrast", "gapMode",
			"thresholdUnits",	"minimalCellSize", "maximalCellSize"
	};
	
	/**
	 * List of parameters relevant for feature extraction phase.
	 */
	private static transient String[] featureParameters = {
			"phaseBInfo", "morphFeatureOp", "classifyLobes"
	};

	/**
	 * Select with phases to run.
	 */
	@Parameter(label = "Select phases to run", required = true, 
			direction = Parameter.Direction.IN, dataIOOrder = -10,
			mode = ExpertMode.STANDARD, callback = "switchPhaseConfigParameters",
			paramModificationMode = ParameterModificationMode.MODIFIES_INTERFACE,
			description = "Choose between segmentation and/or feature extraction.")
	private OperatorPhasesToRun phasesToRun = 
		OperatorPhasesToRun.SEGMENTATION_AND_FEATURES;

	/**
	 * Format of external segmentation input.
	 * <p>
	 * This parameter is only used if no segmentation is done by PaCeQuant. 
	 */
	@Parameter(label = "   Format of external segmentation data", required = true, 
			direction = Parameter.Direction.IN, dataIOOrder = -8,
			mode = ExpertMode.STANDARD, callback = "switchSegmentationFormatParameter",
			paramModificationMode = ParameterModificationMode.MODIFIES_INTERFACE,
			description = "Segmentation data format.")
	private SegmentationInputFormat segmentationInputFormat = 
			SegmentationInputFormat.LABEL_IMAGE;

	/**
	 * Mode of operation of the operator.
	 */
	@Parameter(label = "Operation Mode", required = true, 
		direction = Parameter.Direction.IN,	dataIOOrder = -5, 
		description = "Operation mode of the operator.",
		callback = "switchOpModeParameters",
		paramModificationMode = ParameterModificationMode.MODIFIES_INTERFACE)
	public OperationMode opMode = OperationMode.BATCH;
	
	/**
	 * Image type.
	 */
//	@Parameter(label = "Image Type", required = true, 
//		direction = Parameter.Direction.IN,	dataIOOrder = -4, 
//		description = "Type of image.")
	public ImageType imType = ImageType.FLUORESCENCE_IMAGE;

	/**
	 * Input directory where to find the images to process.
	 * <p>
	 * Depending on the chosen input type the image can either be given 
	 * by an 8-bit grayscale image or a binary image. In the first case
	 * the image is preprocessed and segmentation procedures are applied
	 * to identify individual cell regions. In the latter case it is 
	 * assumend that the cells have already been segmented and each
	 * cell is represented by a connected component, i.e. the 
	 * preprocessing and segmentation steps are skipped.
	 */
	@Parameter(label = "Input Directory", required = true, 
		direction = Parameter.Direction.IN, description = "Input directory.",
		dataIOOrder = -3)
	private ALDDirectoryString inDir = null;

	/**
	 * Input image to process.
	 * <p>
	 * Depending on the chosen input type the image can either be given 
	 * by an 8-bit grayscale image or a binary image. In the first case
	 * the image is preprocessed and segmentation procedures are applied
	 * to identify individual cell regions. In the latter case it is 
	 * assumend that the cells have already been segmented and each
	 * cell is represented by a connected component, i.e. the 
	 * preprocessing and segmentation steps are skipped.
	 */
	@Parameter(label = "Input Image", required = true, 
		direction = Parameter.Direction.IN, description = "Input image.",
		dataIOOrder = -3)
	private transient MTBImageByte inImg = null;

	/**
	 * Input image to process.
	 * <p>
	 * Depending on the chosen input type the image can either be given 
	 * by an 8-bit grayscale image or a binary image. In the first case
	 * the image is preprocessed and segmentation procedures are applied
	 * to identify individual cell regions. In the latter case it is 
	 * assumend that the cells have already been segmented and each
	 * cell is represented by a connected component, i.e. the 
	 * preprocessing and segmentation steps are skipped.
	 */
	@Parameter(label = "Input Regions", required = true, 
		direction = Parameter.Direction.IN, description = "Input image.",
		dataIOOrder = -3)
	private transient MTBRegion2DSet inRegions = null;

	/**
	 * Mode for calibrating physical pixel size.
	 */
	@Parameter(label = "Pixel calibration mode", required = true, 
		direction = Parameter.Direction.IN,	dataIOOrder = 0	, 
		description = "In AUTO mode data is extracted from input data.",
		callback = "switchPixelCalibParameters",
		paramModificationMode = ParameterModificationMode.MODIFIES_INTERFACE)
	private PixelCalibration pixCalibMode = PixelCalibration.AUTO;
	
	/**
	 * Physical size of a pixel.
	 */
	@Parameter(label = "Pixel length", required = true, 
		direction = Parameter.Direction.IN, 
		description = "Pixel length, note that we assume square pixels!", 
		dataIOOrder = 1)
	private double pixelLengthXY = 1.0;
	
	/**
	 * Physical pixel size units.
	 */
	@Parameter(label = "Size unit x/y", required = true, 
		direction = Parameter.Direction.IN, description = "Pixel units.", 
		dataIOOrder = 2)
	private MeasurementUnits unitXY = MeasurementUnits.PIXELS;
	
	/**
	 * Info string for segmentation phase configuration parameters.
	 */
	@Parameter(label = "Configure segmentation phase:", required = true, 
			direction = Parameter.Direction.IN, dataIOOrder = 3, info = true,
			mode = ExpertMode.STANDARD, description = "Info string.")
	private String phaseAInfo = 
		"<html><u>Configure segmentation phase:</u></html>";
	
	/**
	 * Border to background relation.
	 */
	@Parameter(label = "Border Contrast", required = true, 
		direction = Parameter.Direction.IN,	dataIOOrder = 4, 
		description = "Border to background relation.")
	public BorderBackgroundContrast borderContrast = 
		BorderBackgroundContrast.BRIGHT_ON_DARK;

	/**
	 * Mode for closing gaps.
	 */
	@Parameter(label = "Heuristic for Gap Closing", required = true, 
			direction = Parameter.Direction.IN, dataIOOrder = 6,
			description = "Choose mode for closing gaps.", 
			callback = "switchGapCloseMode",
			paramModificationMode = ParameterModificationMode.MODIFIES_INTERFACE)
	private GapCloseMode gapMode = GapCloseMode.WATERSHED;
	
	/**
	 * Maximal distance of gaps in naive mode to be closed.
	 */
	@Parameter(label = "  End-point distance for naive heuristic", 
			required = true, direction = Parameter.Direction.IN, dataIOOrder = 7,
			description = "Maximal distance of end-points to be linked.")
	private int naiveGapThreshold = 20;
	
	/**
	 * Units for size thresholds.
	 */
	@Parameter(label = "Unit for Size Thresholds", required = true, 
			direction = Parameter.Direction.IN, dataIOOrder = 8,
			description = "Unit of specified size thresholds.")
	private MeasurementUnits thresholdUnits = MeasurementUnits.PIXELS;
	
	/**
	 * Threshold for the minimal admissible cell size.
	 * <p>
	 * Cell regions falling below this threshold are ignored.
	 */
	@Parameter(label = "Minimal Size of Cells", required = true, 
			direction = Parameter.Direction.IN, dataIOOrder = 8,
			description = "Cells smaller than this threshold are discarded.")
	private double minimalCellSize = 2500;

	/**
	 * Threshold for the maximal admissible cell size.
	 * <p>
	 * Cell regions lying above this threshold are ignored.
	 */
	@Parameter(label = "Maximal Size of Cells", required = true, 
			direction = Parameter.Direction.IN, dataIOOrder = 9,
			description = "Cells larger than this threshold are discarded.")
	private double maximalCellSize = 1000000;

	/**
	 * Info string for segmentation phase configuration parameters.
	 */
	@Parameter(label = "Configure feature extraction phase:", required = true, 
			direction = Parameter.Direction.IN, dataIOOrder = 10, info = true,
			mode = ExpertMode.STANDARD, description = "Info string.")
	private String phaseBInfo = 
		"<html><u>Configure feature extraction phase:</u></html>";

	/**
	 * Operator to analyze morphology of cells.
	 */
	@Parameter(label = "Feature Extraction", required = true,
			direction = Parameter.Direction.IN, dataIOOrder = 11,
			mode = ExpertMode.STANDARD, description = "Feature extraction operator.")
	private MorphologyAnalyzer2D morphFeatureOp = null;
	
	/**
	 * Flag to classify lobes into different type classes.
	 */
	@Parameter(label = "Analyze lobe types?", required = true,
			direction = Parameter.Direction.IN, dataIOOrder = 12,
			mode = ExpertMode.STANDARD, description = "Enable/disable lobe types.")
	private boolean classifyLobes = false;

	/**
	 * Niblack threshold.
	 */
	@Parameter(label = "Niblack threshold", required = false, 
		direction = Parameter.Direction.IN,	dataIOOrder = -5,
		mode = ExpertMode.ADVANCED,
		description = "Threshold for variance check in Niblack binarization.")
	public double niblackVarianceThresh = 4.0; 

	/**
	 * Info string for segmentation phase configuration parameters.
	 */
	@Parameter(label = "Configure result data:", required = false,
			direction = Parameter.Direction.IN, dataIOOrder = 0, info = true,
			mode = ExpertMode.STANDARD, description = "Info string.")
	private String resultInfo = "<html><u>Configure result data:</u></html>";

	/**
	 * Flag to enable/disable drawing region IDs.
	 */
	@Parameter(label = "Draw region IDs to output images?", 
			required = false,	direction = Parameter.Direction.IN,	dataIOOrder = 10, 
			mode = ExpertMode.STANDARD,
			description = "Enable/disable drawing of region IDs.")
	private boolean drawRegionIDsToOutputImages = false;

	/**
	 * Flag to enable/disable showing additional and intermediate result
	 * images.
	 */
	@Parameter(label = "Show/save additional results?", 
			required = false,	supplemental = true, 
			direction = Parameter.Direction.IN,	dataIOOrder = 1, 
			mode = ExpertMode.STANDARD,
			description = "Enable/disable showing additional results.")
	private boolean showAdditionalResultImages = false;
	
	/**
	 * Flag to enable/disable showing feature visualization stack.
	 * <p>
	 * Attention, stack is large, saving to disk takes some time!
	 */
	@Parameter(label = "Show/save feature stack? (takes some time...)", 
			required = false,	supplemental = true, 
			direction = Parameter.Direction.IN,	dataIOOrder = 2, 
			mode = ExpertMode.STANDARD,
			description = "Enable/disable showing feature stack.")
	private boolean showResultFeatureStack = false;

	/**
	 * Set of detected cell regions.
	 */
	@Parameter(label = "Detected cell regions", 
			supplemental = false, 
			direction = Parameter.Direction.OUT, dataIOOrder = 0, 
			description = "Set of detected and filtered cell regions.")
	private transient MTBRegion2DSet resultCellRegions = null;	

	/**
	 * Table with region features.
	 * <p>
	 * Each row contains one region, each column corresponds to a feature.
	 */
	@Parameter(label = "Result Table of Feature Value", dataIOOrder = 1,
		direction = Parameter.Direction.OUT, 
		description = "Result table of region features.")
	private transient MTBTableModel resultFeatureTable = null;

	/**
	 * Overlay of input image with pseudo-colored cell regions.
	 */
	@Parameter(label = "Detected Cell Regions Image", dataIOOrder = 2, 
			direction = Parameter.Direction.OUT, 
			description = "Image of detected cell regions (overlay).")
	private transient MTBImageRGB resultCellOverlayImg = null;

	/**
	 * Label image of detected cell regions.
	 */
	@Parameter(label = "Label Image of Detected Cell Regions", 
			dataIOOrder = 3, direction = Parameter.Direction.OUT, 
			description = "Label image of detected cell regions.")
	private transient MTBImageShort resultCellLabelImg = null;

	/**
	 * Used threshold for minimal size of valid regions.
	 */
	@Parameter(label = "Minimal Size of Valid Regions", 
			dataIOOrder = 4, direction = Parameter.Direction.OUT, 
			description = "Calculated threshold for filtering regions.")
	private double resultMinRegionSizeThreshold;
	
	/**
	 * Used threshold for maximal size of valid regions.
	 */
	@Parameter(label = "Maximal Size of Valid Regions", 
			dataIOOrder = 5, direction = Parameter.Direction.OUT, 
			description = "Calculated threshold for filtering regions.")
	private double resultMaxRegionSizeThreshold;
	
	/**
	 * (Optional) set of tables with type information and features per lobe.
	 * <p>
	 * These data are only available if {@link #classifyLobes} is true.
	 */
	@Parameter(label = "Result Tables of Lobe Types and Features", 
			dataIOOrder = 6, direction = Parameter.Direction.OUT, 
			description = "Result tables of lobe types and features.")
	private transient Vector<MTBTableModel> resultLobeFeatureTables = null;

	/**
	 * Image showing lobe classification.
	 * <p>
	 * This result image is only available if {@link #classifyLobes} is true.
	 */
	@Parameter(label = "Result Image with Lobe Types", dataIOOrder = 7,
			direction = Parameter.Direction.OUT, 
			description = "Image displaying lobe types.")
	private transient MTBImageRGB resultLobeTypeImage = null;

	/**
	 * Additional result images condensed into a single stack
	 * (only available if {@link #showAdditionalResultImages} is true).
	 */
	@Parameter(label = "Additonal Result Images", dataIOOrder = 0,
			direction = Parameter.Direction.OUT, supplemental = true,
			description = "Additional result images (as stack).")
	private transient MTBImageRGB resultAdditionalImages = null;

	/**
	 * Stack of individual feature images as heat maps
	 * (only available if {@link #showResultFeatureStack} is true).
	 */
	@Parameter(label = "Result Feature Stack", dataIOOrder = 5,
			direction = Parameter.Direction.OUT, supplemental = true,
			description = "Result image stack with feature values.")
	private transient MTBImageRGB resultFeatureStack = null;

	/**
	 * Width of the input image.
	 */
	private transient int width;
	
	/**
	 * Height of the input image.
	 */
	private transient int height;
	
	/**
	 * Pixel length only used internally.
	 */
	private double pixelLengthXYinternal = 1.0;
	
	/**
	 * Unit for pixel length.
	 */
	private String pixelUnitString = "pixels";
	
	/**
	 * Gauss filtered input image (for internal use only).
	 */
	private transient MTBImage gaussFilterImg;
	
	/**
	 * Result of vesselness detection (for internal use only).
	 */
	private transient MTBImage vesselImg;
	
	/**
	 * Vector of additional result images.
	 */
	private transient Vector<MTBImageRGB> addResultImages;
	
	private transient static HashMap<String, String> featureNameMapper = new HashMap<>();

	/**
	 * Default constructor.
	 * @throws ALDOperatorException Thrown if construction fails.
	 */
	public PaCeQuant() throws ALDOperatorException {
		// make sure that the history is only extracted data-driven due to 
		// the for-loop inside this operator, otherwise history files grow with
		// each additional image being processed within the loop
		this.completeDAG = false;
		
		this.morphFeatureOp = new MorphologyAnalyzer2D();
		this.morphFeatureOp.setDeltaXY(new Double(this.pixelLengthXY));
		switch (this.unitXY)
		{
		case MICRONS:
			this.morphFeatureOp.setUnitXY("microns");
			break;
		case PIXELS:
			this.morphFeatureOp.setUnitXY("pixels");
			break;
		}
		this.morphFeatureOp.setCalcArea(true);
		this.morphFeatureOp.setCalcPerimeter(true);
		this.morphFeatureOp.setCalcLengthWidth(true);
		this.morphFeatureOp.setCalcCircularity(true);
		this.morphFeatureOp.setCalcEccentricity(true);
		this.morphFeatureOp.setCalcSolidity(true);
		this.morphFeatureOp.setCalcMarginRoughness(true);
		this.morphFeatureOp.setAnalyzeProtrusionsAndIndentations(true);
		this.morphFeatureOp.setMinimalCurvatureThreshold(1.0);
		this.morphFeatureOp.setGaussianSigmaCurvatureSmoothing(4.0);
		this.morphFeatureOp.setMinimalLobeLength(8);
		this.morphFeatureOp.setCalcSkeletonBranchFeatures(true);
		this.morphFeatureOp.setCalcConcavityData(true);
		this.morphFeatureOp.setConcavityMaskSize(11);
		this.morphFeatureOp.setCalcConvexHullMeasures(true);
		this.morphFeatureOp.setFractionalDigits(5);
		this.morphFeatureOp.setDrawCurvatureInfoImage(true);
		this.morphFeatureOp.setDrawSkeletonInfoImage(true);

		// initialize mapper table to map operator feature names to PaCeQuant convention
		featureNameMapper.put(
			MorphologyAnalyzer2D.FeatureNames.AvgDistBranchEndpointsToBackground.toString(), 
			"AvgEndpointDist"); 
		featureNameMapper.put(
			MorphologyAnalyzer2D.FeatureNames.MinCoreRegionWidth.toString(), 
			"MinCoreWidth"); 
		featureNameMapper.put(
			MorphologyAnalyzer2D.FeatureNames.MaxCoreRegionWidth.toString(), 
			"MaxCoreWidth"); 
		featureNameMapper.put(
			MorphologyAnalyzer2D.FeatureNames.NumberOfProtrusions.toString(), 
			"LobeCount"); 
		featureNameMapper.put(
			MorphologyAnalyzer2D.FeatureNames.NonProtrusionArea.toString(), 
			"NonLobeArea"); 
		featureNameMapper.put(
			MorphologyAnalyzer2D.FeatureNames.AvgLengthProtrusions.toString(), 
			"AvgLobeLength");
		featureNameMapper.put(
			MorphologyAnalyzer2D.FeatureNames.AvgLengthApicalProtrusions.toString(), 
			"AvgApicalLobeLength");
		featureNameMapper.put(
			MorphologyAnalyzer2D.FeatureNames.AvgLengthBasalProtrusions.toString(),
			"AvgBasalLobeLength");
		featureNameMapper.put(
			MorphologyAnalyzer2D.FeatureNames.AvgLengthBaselineProtrusions.toString(), 
			"AvgBasalLobeWidth");
		featureNameMapper.put(
			MorphologyAnalyzer2D.FeatureNames.AvgLengthEquatorProtrusions.toString(), 
			"AvgEquatorLobeWidth");

		this.setParameter("gapMode", GapCloseMode.WATERSHED);
		this.setParameter("phasesToRun", 
				OperatorPhasesToRun.SEGMENTATION_AND_FEATURES);
		this.setParameter("opMode", OperationMode.BATCH);
	}
	
	/**
	 * Callback routine to change operator mode parameters.
	 */
	@SuppressWarnings("unused")
	private void switchOpModeParameters() {
		try {
			if (this.opMode == OperationMode.INTERACTIVE) {
				if (this.hasParameter("inDir")) {
					this.removeParameter("inDir");
				}
				if (this.phasesToRun.equals(OperatorPhasesToRun.FEATURES_ONLY)) {
					// allow for regions or images as input from the ImageJ GUI
					if (this.segmentationInputFormat.equals(
							SegmentationInputFormat.IMAGEJ_ROIs)) {
						if (!this.hasParameter("inRegions")) {
							this.addParameter("inRegions");
						}
						if (this.hasParameter("inImg")) {
							this.removeParameter("inImg");
						}
					}
					else {
						if (!this.hasParameter("inImg")) {
							this.addParameter("inImg");
						}
						if (this.hasParameter("inRegions")) {
							this.removeParameter("inRegions");
						}
					}
				}
				// segmentation included
				else {
					if (!this.hasParameter("inImg")) {
						this.addParameter("inImg");
					}
					if (this.hasParameter("inRegions")) {
						this.removeParameter("inRegions");
					}					
				}
			} else if (this.opMode == OperationMode.BATCH) {
				if (this.hasParameter("inImg")) 
					this.removeParameter("inImg");
				if (this.hasParameter("inRegions")) 
					this.removeParameter("inRegions");
				// add directory parameter
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
	 * Callback routine called if pixel calibration mode parameter changes.
	 */
	@SuppressWarnings("unused")
	private void switchPixelCalibParameters() {
		try {
			if (this.pixCalibMode.equals(PixelCalibration.AUTO)) {
				if (this.hasParameter("pixelLengthXY"))
					this.removeParameter("pixelLengthXY");
				if (this.hasParameter("unitXY"))
					this.removeParameter("unitXY");
			} 
			else {
				if (!this.hasParameter("pixelLengthXY"))
					this.addParameter("pixelLengthXY");
				if (!this.hasParameter("unitXY")) {
					this.addParameter("unitXY");
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
	 * Callback routine called if format parameter changes.
	 */
	@SuppressWarnings("unused")
	private void switchSegmentationFormatParameter() {
		if (this.opMode.equals(OperationMode.BATCH))
			return;
		try {
			if (this.segmentationInputFormat == SegmentationInputFormat.IMAGEJ_ROIs) {
				if (this.hasParameter("inImg"))
					this.removeParameter("inImg");
				if (!this.hasParameter("inRegions")) {
					this.addParameter("inRegions");
				}
			} else {
				if (!this.hasParameter("inImg"))
					this.addParameter("inImg");
				if (this.hasParameter("inRegions")) {
					this.removeParameter("inRegions");
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
	 * Callback routine to change phase parameters.
	 */
	@SuppressWarnings("unused")
	private void switchPhaseConfigParameters() {
		try {
			if (this.phasesToRun == OperatorPhasesToRun.SEGMENTATION_AND_FEATURES) {
				if (this.hasParameter("segmentationInputFormat"))
					this.removeParameter("segmentationInputFormat");
				// only allow images as input data type
				if (this.hasParameter("inRegions")) {
					this.removeParameter("inRegions");
				}
				if (this.opMode.equals(OperationMode.INTERACTIVE)) {
					if (!this.hasParameter("inImg"))
						this.addParameter("inImg");
				}
				else {
					if (!this.hasParameter("inDir")) {
						this.addParameter("inDir");
					}
				}
				for (String s: segmentationParameters) {
					if (!this.hasParameter(s)) {
						this.addParameter(s);
					}
				}
				for (String s: featureParameters) {
					if (!this.hasParameter(s)) {
						this.addParameter(s);
					}
				}
			} 
			else if (this.phasesToRun == OperatorPhasesToRun.SEGMENTATION_ONLY) {
				if (this.hasParameter("segmentationInputFormat"))
					this.removeParameter("segmentationInputFormat");
				// only allow images as input data type
				if (this.hasParameter("inRegions")) {
					this.removeParameter("inRegions");
				}
				if (this.opMode.equals(OperationMode.INTERACTIVE)) {
					if (!this.hasParameter("inImg"))
						this.addParameter("inImg");
				}
				else {
					if (!this.hasParameter("inDir")) {
						this.addParameter("inDir");
					}
				}
				for (String s: featureParameters) {
					if (this.hasParameter(s)) {
						this.removeParameter(s);
					}
				}
				for (String s: segmentationParameters) {
					if (!this.hasParameter(s)) {
						this.addParameter(s);
					}
				}
			} 
			else if (this.phasesToRun == OperatorPhasesToRun.FEATURES_ONLY) {
				if (!this.hasParameter("segmentationInputFormat"))
					this.addParameter("segmentationInputFormat");
				// switch between input formats
				switch(this.segmentationInputFormat)
				{
				case BINARY_IMAGE:
				case LABEL_IMAGE:
					if (this.hasParameter("inRegions")) {
						this.removeParameter("inRegions");
					}
					if (this.opMode.equals(OperationMode.INTERACTIVE)) {
						if (!this.hasParameter("inImg"))
							this.addParameter("inImg");		
						if (this.hasParameter("inDir")) {
							this.removeParameter("inDir");
						}
					}
					else {
						if (this.hasParameter("inImg"))
							this.removeParameter("inImg");		
						if (!this.hasParameter("inDir")) {
							this.addParameter("inDir");
						}						
					}
					break;
				case IMAGEJ_ROIs:
					if (!this.hasParameter("inRegions")) {
						this.addParameter("inRegions");
					}
					if (this.hasParameter("inImg"))
						this.removeParameter("inImg");		
					if (this.hasParameter("inDir")) {
						this.removeParameter("inDir");
					}											
					break;
				}
				for (String s: segmentationParameters) {
					if (this.hasParameter(s)) {
						this.removeParameter(s);
					}
				}
				// handle additional dynamic parameters
				if (this.hasParameter("naiveGapThreshold")) {
					this.removeParameter("naiveGapThreshold");
				}
				for (String s: featureParameters) {
					if (!this.hasParameter(s)) {
						this.addParameter(s);
					}
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
	 * Callback routine called if gap closing mode parameter changes.
	 */
	@SuppressWarnings("unused")
	private void switchGapCloseMode() {
		try {
			if (   this.gapMode.equals(GapCloseMode.NONE)
					|| this.gapMode.equals(GapCloseMode.WATERSHED)) {
				if (this.hasParameter("naiveGapThreshold"))
					this.removeParameter("naiveGapThreshold");
			} 
			else {
				if (!this.hasParameter("naiveGapThreshold"))
					this.addParameter("naiveGapThreshold");
			}
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ALDOperatorException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void validateCustom() throws ALDOperatorException {
		if (this.inImg != null) {
			this.pixelLengthXYinternal = this.inImg.getStepsizeX();
			double lengthY = this.inImg.getStepsizeY();
			if (Math.abs(this.pixelLengthXYinternal - lengthY) > 0.0000001)
				throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED, 
						operatorID + "Image does not have square pixels! Exiting!");
		}
	}
	
	/* (non-Javadoc)
	 * @see de.unihalle.informatik.Alida.operator.ALDOperator#operate()
	 */
	@Override
	protected void operate() 
			throws ALDOperatorException, ALDProcessingDAGException {

		MTBImage imgToProcess = null;
		MTBImage labelImgToProcess = null;
		MTBImageByte binarySegmentationImage;		
		MTBRegion2DSet regionsToProcess = null;
		switch(this.opMode) 
		{
			/*
			 * Directly interact with the ImageJ GUI, get data from there.
			 */
			case INTERACTIVE:
			{
				// segment the input image, if requested
				this.fireOperatorExecutionProgressEvent(
						new ALDOperatorExecutionProgressEvent(this, operatorID 
								+ "Running interactive processing mode..."));
				
				// init some variables and init operator
				this.reinitOperator();
				
				// run segmentation
				if (   this.phasesToRun.equals(
									OperatorPhasesToRun.SEGMENTATION_AND_FEATURES)
						|| this.phasesToRun.equals(
									OperatorPhasesToRun.SEGMENTATION_ONLY)) {

					this.fireOperatorExecutionProgressEvent(
							new ALDOperatorExecutionProgressEvent(this, operatorID 
									+ " segmenting image <" + this.inImg.getTitle() + ">..."));

					this.width = this.inImg.getSizeX();
					this.height = this.inImg.getSizeY();
					
					// check for pixel calibration mode and set calibration values
					if (this.pixCalibMode.equals(PixelCalibration.AUTO)) {
						this.pixelLengthXYinternal = this.inImg.getStepsizeX();
						this.pixelUnitString = this.inImg.getUnitX();
						
						// safety check for square pixels
						double lengthY = this.inImg.getStepsizeY();
						if (  Math.abs(this.pixelLengthXYinternal - lengthY)
								> 0.0000001) {
							// write a warning to standard error and skip image
							System.err.println(operatorID 
								+	" --> ATTENTION! Image does not have square pixels! "
									+ "Exiting!");
							return;
						}
					}
					else {						
						this.pixelLengthXYinternal = this.pixelLengthXY;
						switch(this.unitXY)
						{
						case MICRONS:
							this.pixelUnitString = "microns";
							break;
						case PIXELS:
							this.pixelUnitString = "pixels";
							break;
						}
					}
					
					imgToProcess = this.inImg;
					SegmentationResult segResult = this.runSegmentationPhase(this.inImg);
					regionsToProcess = segResult.resultRegs;
					labelImgToProcess = segResult.resultLableImgWithoutIDs;
				
					// handle result data
					if (this.drawRegionIDsToOutputImages) {
						this.resultCellLabelImg = segResult.resultLabelImg;
						this.resultCellOverlayImg = segResult.resultOverlayImg;
					}
					else {
						this.resultCellLabelImg = segResult.resultLableImgWithoutIDs;
						this.resultCellOverlayImg = segResult.resultOverlayImgWithoutIDs;
					}
					this.resultCellRegions = segResult.resultRegs;
					
				}
				// only features are to be calculated
				else { 
					switch(this.segmentationInputFormat)
					{
					case BINARY_IMAGE:

						this.fireOperatorExecutionProgressEvent(
								new ALDOperatorExecutionProgressEvent(this, operatorID 
									+ " skipping segmentation, just extracting features for " 
										+ "image <" + this.inImg.getTitle() + ">..."));
						
						this.width = this.inImg.getSizeX();
						this.height = this.inImg.getSizeY();
						
						// check for pixel calibration mode and set calibration values
						if (this.pixCalibMode.equals(PixelCalibration.AUTO)) {
							this.pixelLengthXYinternal = this.inImg.getStepsizeX();
							this.pixelUnitString = this.inImg.getUnitX();
							
							// safety check for square pixels
							double lengthY = this.inImg.getStepsizeY();
							if (  Math.abs(this.pixelLengthXYinternal - lengthY)
									> 0.0000001) {
								// write a warning to standard error and skip image
								System.err.println(operatorID 
									+	" --> ATTENTION! Image does not have square pixels! "
										+ "Exiting!");
								return;
							}
						}
						else {						
							this.pixelLengthXYinternal = this.pixelLengthXY;
							switch(this.unitXY)
							{
							case MICRONS:
								this.pixelUnitString = "microns";
								break;
							case PIXELS:
								this.pixelUnitString = "pixels";
								break;
							}
						}
						
						binarySegmentationImage = this.inImg;
						imgToProcess = this.inImg;
						LabelComponentsSequential labler = 
								new LabelComponentsSequential(this.inImg, true);
						labler.runOp();
						labelImgToProcess = labler.getLabelImage();
						regionsToProcess = labler.getResultingRegions();
						break;
						
					case LABEL_IMAGE:
						
						this.fireOperatorExecutionProgressEvent(
								new ALDOperatorExecutionProgressEvent(this, operatorID 
									+ " skipping segmentation, just extracting features for " 
										+ "image <" + this.inImg.getTitle() + ">..."));

						this.width = this.inImg.getSizeX();
						this.height = this.inImg.getSizeY();
						
						// check for pixel calibration mode and set calibration values
						if (this.pixCalibMode.equals(PixelCalibration.AUTO)) {
							this.pixelLengthXYinternal = this.inImg.getStepsizeX();
							this.pixelUnitString = this.inImg.getUnitX();
							
							// safety check for square pixels
							double lengthY = this.inImg.getStepsizeY();
							if (  Math.abs(this.pixelLengthXYinternal - lengthY)
									> 0.0000001) {
								// write a warning to standard error and skip image
								System.err.println(operatorID 
									+	" --> ATTENTION! Image does not have square pixels! "
										+ "Exiting!");
								return;
							}
						}
						else {						
							this.pixelLengthXYinternal = this.pixelLengthXY;
							switch(this.unitXY)
							{
							case MICRONS:
								this.pixelUnitString = "microns";
								break;
							case PIXELS:
								this.pixelUnitString = "pixels";
								break;
							}
						}
						
						binarySegmentationImage = (MTBImageByte)this.inImg.duplicate();
						for (int y=0; y<this.height; ++y)
							for (int x=0; x<this.width; ++x) 
								if (binarySegmentationImage.getValueInt(x, y) > 0)
									binarySegmentationImage.putValueInt(x, y, 255);
						imgToProcess = binarySegmentationImage;
						labelImgToProcess = this.inImg;
						regionsToProcess = LabelAreasToRegions.getRegions(this.inImg, 0);
						break;
						
					case IMAGEJ_ROIs:
						
						this.fireOperatorExecutionProgressEvent(
								new ALDOperatorExecutionProgressEvent(this, operatorID 
									+ " extracting features for given ROIs..."));

						binarySegmentationImage = (MTBImageByte)MTBImage.createMTBImage(
							(int)(this.inRegions.getXmax()+0.5) + 1,
								(int)(this.inRegions.getYmax()+0.5) + 1, 1, 1, 1,	
									MTBImageType.MTB_BYTE);
						binarySegmentationImage.setTitle("Binary image of input ROIs");
						this.width = binarySegmentationImage.getSizeX();
						this.height = binarySegmentationImage.getSizeY();
						
						// inside ImageJ ROI files no calibration data is available!
						if (this.pixCalibMode.equals(PixelCalibration.AUTO)) {
							this.pixelLengthXYinternal = 1;
							this.pixelUnitString = "pixels";
						}
						else {
							this.pixelLengthXYinternal = this.pixelLengthXY;
							switch(this.unitXY)
							{
							case MICRONS:
								this.pixelUnitString = "microns";
								break;
							case PIXELS:
								this.pixelUnitString = "pixels";
								break;
							}
						}

						binarySegmentationImage.fillBlack();
						for (MTBRegion2D r: this.inRegions) {
							for (Point2D.Double p: r.getPoints()) {
								binarySegmentationImage.putValueInt((int)p.x, (int)p.y, 255);
							}
						}
						imgToProcess = binarySegmentationImage;
						regionsToProcess = this.inRegions;
						break;
						
					}
				}
				// extract features from the binary image
				if (   this.phasesToRun.equals(
									OperatorPhasesToRun.SEGMENTATION_AND_FEATURES)
						|| this.phasesToRun.equals(OperatorPhasesToRun.FEATURES_ONLY)) {
					this.resultFeatureTable = this.runFeatureExtractionPhase(
							imgToProcess, regionsToProcess, labelImgToProcess);
				}

				// fill the additional results stack
				if (this.showAdditionalResultImages) {
					this.prepareAdditionalResultDataStack(imgToProcess.getTitle());
				}

				this.fireOperatorExecutionProgressEvent(
						new ALDOperatorExecutionProgressEvent(this, operatorID 
							+ " ...calculations completed!"));
				break;
			}
			
			/*
			 * Process data in a given directory and save result data there, too.
			 */
			case BATCH:
			{
				DirectoryTree rootDirTree = 
						new DirectoryTree(this.inDir.getDirectoryName(), true);
				
				this.fireOperatorExecutionProgressEvent(
						new ALDOperatorExecutionProgressEvent(this, operatorID 
							+ " processing directory <" 
								+ this.inDir.getDirectoryName() + ">,"  	
									+ " searching for sub-directories..."));
				Vector<String> allDirs = rootDirTree.getSubdirectoryList();
				
				// add the root directory itself
				allDirs.add(this.inDir.getDirectoryName());
				
				// iterate over all directories
				for (String dir : allDirs) {

					// skip result directories
					if (dir.contains("results"))
						continue;

					// get files of current directory
					DirectoryTree dirTree = new DirectoryTree(dir, false);
					ImageReaderMTB imRead = new ImageReaderMTB();
					ImageWriterMTB imWrite = new ImageWriterMTB();
					
					// check if there is at least one file in the directory,
					// if not, jump to next directory
					Vector<String> files = dirTree.getFileList();
					if (files.isEmpty())
						continue;
					
					this.fireOperatorExecutionProgressEvent(
							new ALDOperatorExecutionProgressEvent(this, operatorID 
								+ " -> processing directory <" + dir + ">..."));

					// make a new sub-directory for the results
					String resultDir = dir + File.separator + "results"; 
					if (!new File(resultDir).exists()) {
						if (!new File(resultDir).mkdir())
							throw new ALDOperatorException(
									OperatorExceptionType.OPERATE_FAILED, operatorID 
									+ " could not init result folder... exiting!");
					}
					String resultDirRois = resultDir + File.separator + "roiFiles_singleCells"; 
					if (!new File(resultDirRois).exists()) {
						if (!new File(resultDirRois).mkdir())
							throw new ALDOperatorException(
									OperatorExceptionType.OPERATE_FAILED, operatorID 
									+ " could not init result folder for ROIs... exiting!");
					}
					
					// reset operator - just in case...
					this.reinitOperator();
					
					// check what needs to be done...
					if (   this.phasesToRun.equals(
										OperatorPhasesToRun.SEGMENTATION_AND_FEATURES)
							|| this.phasesToRun.equals(
										OperatorPhasesToRun.SEGMENTATION_ONLY)) {

						this.fireOperatorExecutionProgressEvent(
								new ALDOperatorExecutionProgressEvent(this, operatorID 
										+ " going to segment images..."));
						this.fireOperatorExecutionProgressEvent(
								new ALDOperatorExecutionProgressEvent(this, operatorID 
										+ " ... considering only image files!"));

						for (String file : files) {

							try {
								
								this.fireOperatorExecutionProgressEvent(
										new ALDOperatorExecutionProgressEvent(this, operatorID 
												+ " segmenting image <" + file + ">..."));

								imRead.setFileName(file);
								imRead.runOp();

								this.fireOperatorExecutionProgressEvent(
										new ALDOperatorExecutionProgressEvent(this, operatorID 
												+ " --> reading image <" + file + ">..."));

								MTBImage img = imRead.getResultMTBImage();
								// make sure that you don't work on other than byte images
								if (!img.getType().equals(MTBImageType.MTB_BYTE))
									img = img.convertType(MTBImageType.MTB_BYTE, true);

								// check for pixel calibration mode
								if (this.pixCalibMode.equals(PixelCalibration.AUTO)) {
									this.pixelLengthXYinternal = img.getStepsizeX();
									this.pixelUnitString = img.getUnitX();
									
									// safety check for square pixels
									double lengthY = img.getStepsizeY();
									if (Math.abs(this.pixelLengthXYinternal-lengthY)>0.0000001) {
										// write a warning to standard error and skip image
										System.err.println(operatorID 
											+	" --> ATTENTION! Image does not have square pixels! "
												+ "Skipping the image!");
										continue;
									}
								}
								else {						
									this.pixelLengthXYinternal = this.pixelLengthXY;
									switch(this.unitXY)
									{
									case MICRONS:
										this.pixelUnitString = "microns";
										break;
									case PIXELS:
										this.pixelUnitString = "pixels";
										break;
									}
								}

								// init some variables and init operator
								this.reinitOperator();

								this.width = img.getSizeX();
								this.height = img.getSizeY();
								
								imgToProcess = img;
								SegmentationResult segResult = 
										this.runSegmentationPhase(img);
								regionsToProcess = segResult.resultRegs;
								labelImgToProcess = segResult.resultLableImgWithoutIDs;
								
								// handle some more result data
								if (this.drawRegionIDsToOutputImages) {
									this.resultCellLabelImg = segResult.resultLabelImg;
									this.resultCellOverlayImg = segResult.resultOverlayImg;
								}
								else {
									this.resultCellLabelImg = segResult.resultLableImgWithoutIDs;
									this.resultCellOverlayImg = 
											segResult.resultOverlayImgWithoutIDs;
								}
								this.resultCellRegions = segResult.resultRegs;

								String fileRoot = 
										ALDFilePathManipulator.removeExtension(img.getTitle());

								// save overlay image
								imWrite.setFileName(resultDir + File.separator 
										+ fileRoot + "-color-result.tif");
								imWrite.setInputMTBImage(this.resultCellOverlayImg);
								imWrite.runOp();

								// save gray-scale overlay image
								imWrite.setFileName(resultDir + File.separator 
										+ fileRoot + "-grayscale-result.tif");
								imWrite.setInputMTBImage(this.resultCellLabelImg);
								imWrite.runOp();

								// save regions to files
								this.saveRegionData(resultDir,fileRoot,this.resultCellRegions);
								
								// check if also features have to be extracted, if so, do so
								if (this.phasesToRun.equals(
											OperatorPhasesToRun.SEGMENTATION_AND_FEATURES)) {
									this.resultFeatureTable = this.runFeatureExtractionPhase(
											imgToProcess, regionsToProcess, labelImgToProcess);

									// save result table
									this.resultFeatureTable.saveTable(new File(resultDir 
											+ File.separator + fileRoot + "-table.txt"));
									
									// optionally save lobe type image and data table
									if (this.classifyLobes) {
										imWrite.setFileName(resultDir + File.separator 
												+ fileRoot + "-lobe-types.tif");
										imWrite.setInputMTBImage(this.resultLobeTypeImage);
										imWrite.runOp();										
										
										MTBTableModel lobeTab; 
										String c;
										for (int i=0; i<regionsToProcess.size(); ++i) {
											lobeTab = this.resultLobeFeatureTables.elementAt(i);
											c = String.format("%03d", new Integer(i+1));
											lobeTab.saveTable(new File(resultDir + File.separator 
												+ fileRoot + "-cell-" + c + "-lobe-table.txt"));
										}
									}

								}
								
								// save additional result images if requested
								if (this.showResultFeatureStack) {
									imWrite.setFileName(resultDir + File.separator 
											+ fileRoot + "-feature-stack.tif");
									imWrite.setInputMTBImage(this.resultFeatureStack);
									imWrite.runOp();
								}
								if (this.showAdditionalResultImages) {
									this.prepareAdditionalResultDataStack(img.getTitle());
									imWrite.setFileName(resultDir + File.separator 
											+ fileRoot + "-intermediate-result-stack.tif");
									imWrite.setInputMTBImage(this.resultAdditionalImages);
									imWrite.runOp();
								}

							}
							catch (Exception ex) {
								// just skip the file, maybe not an image...
								System.err.println(operatorID + " skipping file \"" + file 
										+ "\", either not an image file or format unknown...");
								ex.printStackTrace();
							}				
							// make sure that no results are displayed in directory mode
							this.reinitOperator();
							
						} // end of for-loop over all files
					} // end of if-clause for segmentation stage
					
					// just extract features
					else {
						
						this.fireOperatorExecutionProgressEvent(
								new ALDOperatorExecutionProgressEvent(this, operatorID 
										+ " going to extract features..."));

						RoiManagerAdapter roiReader = RoiManagerAdapter.getInstance();

						String fileRoot;
						for (String file : files) {
							
							try {
								this.fireOperatorExecutionProgressEvent(
										new ALDOperatorExecutionProgressEvent(this, operatorID 
												+ " extracting features for <" + file + ">..."));

								fileRoot = ALDFilePathManipulator.getFileName(file);

								switch(this.segmentationInputFormat)
								{
								case BINARY_IMAGE:
								{
									imRead.setFileName(file);
									imRead.runOp();

									this.fireOperatorExecutionProgressEvent(
											new ALDOperatorExecutionProgressEvent(this, operatorID 
													+ " --> reading image <" + file + ">..."));

									MTBImageByte img = (MTBImageByte)imRead.getResultMTBImage().
											convertType(MTBImageType.MTB_BYTE, true);

									// check for pixel calibration mode
									if (this.pixCalibMode.equals(PixelCalibration.AUTO)) {
										this.pixelLengthXYinternal = img.getStepsizeX();
										this.pixelUnitString = img.getUnitX();
										
										// safety check for square pixels
										double lengthY = img.getStepsizeY();
										if (  Math.abs(this.pixelLengthXYinternal - lengthY)
												> 0.0000001) {
											// write a warning to standard error and skip image
											System.err.println(operatorID 
												+	" --> ATTENTION! Image does not have square pixels! "
													+ "Skipping the image!");
											continue;
										}
									}
									else {						
										this.pixelLengthXYinternal = this.pixelLengthXY;
										switch(this.unitXY)
										{
										case MICRONS:
											this.pixelUnitString = "microns";
											break;
										case PIXELS:
											this.pixelUnitString = "pixels";
											break;
										}
									}
									
									this.width = img.getSizeX();
									this.height = img.getSizeY();								
									binarySegmentationImage = img;
									imgToProcess = img;
									LabelComponentsSequential labler = 
											new LabelComponentsSequential(img, true);
									labler.runOp();
									labelImgToProcess = labler.getLabelImage();
									regionsToProcess = labler.getResultingRegions();
									
									// save extracted region data to file
									this.saveRegionData(resultDir, fileRoot, regionsToProcess);

									break;
								}
								case LABEL_IMAGE:
								{
									imRead.setFileName(file);
									imRead.runOp();

									MTBImage img = imRead.getResultMTBImage();

									// check for pixel calibration mode
									if (this.pixCalibMode.equals(PixelCalibration.AUTO)) {
										this.pixelLengthXYinternal = img.getStepsizeX();
										this.pixelUnitString = img.getUnitX();
										
										// safety check for square pixels
										double lengthY = img.getStepsizeY();
										if (  Math.abs(this.pixelLengthXYinternal - lengthY)
												> 0.0000001) {
											// write a warning to standard error and skip image
											System.err.println(operatorID 
												+	" --> ATTENTION! Image does not have square pixels! "
													+ "Skipping the image!");
											continue;
										}
									}
									else {						
										this.pixelLengthXYinternal = this.pixelLengthXY;
										switch(this.unitXY)
										{
										case MICRONS:
											this.pixelUnitString = "microns";
											break;
										case PIXELS:
											this.pixelUnitString = "pixels";
											break;
										}
									}
									
									this.width = img.getSizeX();
									this.height = img.getSizeY();
									binarySegmentationImage = (MTBImageByte)img.convertType(
											MTBImageType.MTB_BYTE, true);
									for (int y=0; y<this.height; ++y)
										for (int x=0; x<this.width; ++x) 
											if (binarySegmentationImage.getValueInt(x, y) > 0)
												binarySegmentationImage.putValueInt(x, y, 255);
									imgToProcess = binarySegmentationImage;
									labelImgToProcess = img;
									regionsToProcess = LabelAreasToRegions.getRegions(img, 0);

									// save extracted region data to file
									this.saveRegionData(resultDir, fileRoot, regionsToProcess);

									break;
								}
								case IMAGEJ_ROIs:
								{					
									// skip files with wrong ending
									if (!file.endsWith(".zip") && !file.endsWith(".roi")) {
										this.fireOperatorExecutionProgressEvent(
												new ALDOperatorExecutionProgressEvent(this, operatorID 
														+ " => skipping, wrong file ending!"));
										continue;
									}

									MTBRegion2DSet regions = 
											roiReader.getRegionSetFromRoiFile(file);

									// check for pixel calibration mode, but inside ImageJ ROI files 
									// no calibration data is available!
									if (this.pixCalibMode.equals(PixelCalibration.AUTO)) {
										this.pixelLengthXYinternal = 1;
										this.pixelUnitString = "pixels";
									}
									else {
										this.pixelLengthXYinternal = this.pixelLengthXY;
										switch(this.unitXY)
										{
										case MICRONS:
											this.pixelUnitString = "microns";
											break;
										case PIXELS:
											this.pixelUnitString = "pixels";
											break;
										}
									}

									binarySegmentationImage = 
										(MTBImageByte)MTBImage.createMTBImage(
											(int)(regions.getXmax()+0.5) + 1,
												(int)(regions.getYmax()+0.5) + 1, 1, 1, 1,	
													MTBImageType.MTB_BYTE);
									binarySegmentationImage.setTitle("Binary image of input ROIs");
									this.width = binarySegmentationImage.getSizeX();
									this.height = binarySegmentationImage.getSizeY();
									binarySegmentationImage.fillBlack();
									for (MTBRegion2D r: regions) {
										for (Point2D.Double p: r.getPoints()) {
											binarySegmentationImage.putValueInt(
													(int)p.x, (int)p.y, 255);
										}
									}
									imgToProcess = binarySegmentationImage;
									regionsToProcess = regions;
									break;
								}
								} // end of switch

								if (regionsToProcess != null && regionsToProcess.size() > 0) {
									
									this.resultFeatureTable = this.runFeatureExtractionPhase(
											imgToProcess, regionsToProcess, labelImgToProcess);
									
									// save result table
									this.resultFeatureTable.saveTable(new File(resultDir 
											+ File.separator + fileRoot + "-table.txt"));

									// save gray-scale overlay image
									imWrite.setFileName(resultDir + File.separator 
											+ fileRoot + "-grayscale-result.tif");
									imWrite.setInputMTBImage(this.resultCellLabelImg);
									imWrite.runOp();
									
									// optionally save lobe type image and data table
									if (this.classifyLobes) {
										imWrite.setFileName(resultDir + File.separator 
												+ fileRoot + "-lobe-types.tif");
										imWrite.setInputMTBImage(this.resultLobeTypeImage);
										imWrite.runOp();										
										
										MTBTableModel lobeTab; 
										String c;
										for (int i=0; i<regionsToProcess.size(); ++i) {
											lobeTab = this.resultLobeFeatureTables.elementAt(i);
											c = String.format("%03d", new Integer(i+1));
											lobeTab.saveTable(new File(resultDir + File.separator 
												+ fileRoot + "-cell-" + c + "-lobe-table.txt"));
										}
									}
									
									// save additional result images if requested
									if (this.showResultFeatureStack) {
										imWrite.setFileName(resultDir + File.separator 
												+ fileRoot + "-feature-stack.tif");
										imWrite.setInputMTBImage(this.resultFeatureStack);
										imWrite.runOp();
									}
									if (this.showAdditionalResultImages) {
										this.prepareAdditionalResultDataStack(file);
										imWrite.setFileName(resultDir + File.separator 
												+ fileRoot + "-intermediate-result-stack.tif");
										imWrite.setInputMTBImage(this.resultAdditionalImages);
										imWrite.runOp();
									}
								}
							}
							catch (Exception ex) {
								// just skip the file, maybe not an image...
								System.err.println(operatorID + " skipping file \"" + file 
										+ "\", not in an expected format...");
								ex.printStackTrace();
							}
						
							// reset operator
							this.reinitOperator();
							
						} // end of for-loop over all files
					} // end of else-clause for feature extraction only
				} // end of for-loop over all directories
				this.reinitOperator();
			} // end of catch-clause for batch processing
		} // end of switch directive for operator mode
		
		// send final status message
		this.fireOperatorExecutionProgressEvent(
			new ALDOperatorExecutionProgressEvent(this, operatorID 
				+ " completed calculations, all done!"));
	}
	
  /**
   * Re-initialize the operator just in case it was already run before.
   */
  protected void reinitOperator() {
		this.addResultImages = new Vector<MTBImageRGB>();
		this.resultAdditionalImages = null;
		this.resultFeatureStack = null;	
		this.resultCellLabelImg = null;
		this.resultCellOverlayImg = null;
		this.resultCellRegions = null;
		this.resultFeatureStack = null;
		this.resultFeatureTable = null;
		this.resultLobeFeatureTables = null;
		this.resultLobeTypeImage = null;
  }
		
  /**
   * Save region set to output file.
   * <p>
   * This method saves each region of the set to an individual output file 
   * and in addition all regions together to a common zip file.
   * 
   * @param resultDir		Directory where to save the files.
   * @param fileRoot		Common root of all file names.
   * @param regions			Set of regions to save.
   * @throws ALDOperatorException				Thrown in case of failure.
   * @throws ALDProcessingDAGException	Thrown in case of failure.
   */
  protected void saveRegionData(String resultDir, String fileRoot, 
  		MTBRegion2DSet regions) 
  	throws ALDOperatorException, ALDProcessingDAGException {

  	RoiWriter rw = new RoiWriter();

  	int roiID = 1;
  	String roiString;
  	MTBContour2DSet cSet = new MTBContour2DSet();
  	for (MTBRegion2D reg : regions) {
  		MTBContour2D creg = reg.getContour();
  		cSet.add(creg);

  		// save each single region to a ROI file
  		roiString = String.format("%03d", new Integer(roiID));
				
  		rw.setOutputFile(resultDir + File.separator + "roiFiles_singleCells" + File.separator +
  				fileRoot + "-roi_" + roiString + ".roi");
  		rw.setData(creg);
  		rw.runOp();
  		
  		++roiID;
  	}

  	// save the complete set to a zip file as well
  	rw.setOutputFile(resultDir + File.separator +
  			fileRoot + "-allRois.zip");
  	rw.setData(cSet);
  	rw.runOp();
  }
  
  protected void prepareAdditionalResultDataStack(String title) {
		this.resultAdditionalImages= (MTBImageRGB)MTBImage.createMTBImage(
				this.width, this.height, this.addResultImages.size(), 1, 1, 
				MTBImageType.MTB_RGB);
		this.resultAdditionalImages.setTitle("Stack of intermediate "
				+ "result images for <" + title + ">");
		int sc = 0;
		for (MTBImageRGB im: this.addResultImages) {
			this.resultAdditionalImages.setSlice(im, sc, 0, 0);
			this.resultAdditionalImages.setSliceLabel(im.getTitle(),sc,0,0);
			++sc;
		}
  }
  
	/**
	 * Method that processes a single image.
	 * @param img	Image to process.
	 * @return Cell region segmentation result.
	 * @throws ALDProcessingDAGException Thrown in case of failure.
	 * @throws ALDOperatorException Thrown in case of failure.
	 */
	protected SegmentationResult runSegmentationPhase(MTBImage img) 
		throws ALDOperatorException, ALDProcessingDAGException {

		SegmentationResult segResult = new SegmentationResult();
		
		// perform the segmentation
		MTBImageByte binSegResult = this.segmentInputImage(img);
	
		// store preprocessed image as intermediate result
		if (this.showAdditionalResultImages) {
			MTBImageRGB resultWorkImg = 
					(MTBImageRGB)MTBImage.createMTBImage(binSegResult.getSizeX(), 
							binSegResult.getSizeY(), 1, 1, 1,	MTBImageType.MTB_RGB);
			resultWorkImg.setTitle("Preprocessed image with detected cells.");
			for (int y=0; y<binSegResult.getSizeY(); ++y) {
				for (int x=0; x<binSegResult.getSizeX(); ++x) { 
					resultWorkImg.putValueR(x,y,binSegResult.getValueInt(x, y));
					resultWorkImg.putValueG(x,y,binSegResult.getValueInt(x, y));
					resultWorkImg.putValueB(x,y,binSegResult.getValueInt(x, y));
				}
			}
			this.addResultImages.add(resultWorkImg);
		}
		
		// filter cell regions from the image
		this.fireOperatorExecutionProgressEvent(
				new ALDOperatorExecutionProgressEvent(this, operatorID 
					+ " filtering for valid cell regions..."));
		MTBRegion2DSet validRegions = this.filterValidCellRegions(binSegResult);
		
		// init and fill result images
		MTBImageRGB overlayImg = (MTBImageRGB)MTBImage.createMTBImage(
				this.width, this.height, 1, 1, 1, MTBImageType.MTB_RGB);
		overlayImg.setTitle("Pseudo-colored cell regions " 
				+ "of image <" + img.getTitle() + "> with IDs");
		MTBImageRGB overlayImgWONumbers = (MTBImageRGB)MTBImage.createMTBImage(
				this.width, this.height, 1, 1, 1, MTBImageType.MTB_RGB);
		overlayImgWONumbers.setTitle("Pseudo-colored cell regions " 
				+ "of image <" + img.getTitle() + ">");
		for (int y = 0; y < this.height; y++) {
			for (int x = 0; x < this.width; x++) {
				overlayImg.putValueR(x, y, img.getValueInt(x, y));
				overlayImg.putValueG(x, y, img.getValueInt(x, y));
				overlayImg.putValueB(x, y, img.getValueInt(x, y));
				overlayImgWONumbers.putValueR(x, y, img.getValueInt(x, y));
				overlayImgWONumbers.putValueG(x, y, img.getValueInt(x, y));
				overlayImgWONumbers.putValueB(x, y, img.getValueInt(x, y));
			}
		}
		MTBImageShort labelImg =	(MTBImageShort)MTBImage.createMTBImage(
				this.width, this.height, 1, 1, 1, MTBImageType.MTB_SHORT);
		labelImg.setTitle("Valid cell regions label image");
		labelImg.fillBlack();
		MTBImageShort labelImageWONumbers = (MTBImageShort)labelImg.duplicate();
		
		MTBImageRGB binImg = 	(MTBImageRGB)MTBImage.createMTBImage(
				this.width, this.height, 1, 1, 1, MTBImageType.MTB_RGB);
		binImg.setTitle("Valid cell regions binary image");
		binImg.fillBlack();
		if (this.showAdditionalResultImages) {
			this.addResultImages.add(binImg);
		}
		
		// iterate over valid regions and draw them into the images
		int regionID = 0;
		int x, y, red, green, blue;
		Random rand = new Random();
		for (MTBRegion2D r: validRegions) {
			++regionID;
			// generate pseudo-color
			red = rand.nextInt(256);
			green = rand.nextInt(256);
			blue = rand.nextInt(256);
			// draw region points
			for (Point2D.Double p: r.getPoints()) {
				x = (int)p.x;
				y = (int)p.y;
				overlayImg.putValueR(x, y, red);
				overlayImg.putValueG(x, y, green);
				overlayImg.putValueB(x, y, blue);
				overlayImgWONumbers.putValueR(x, y, red);
				overlayImgWONumbers.putValueG(x, y, green);
				overlayImgWONumbers.putValueB(x, y, blue);
				labelImg.putValueInt(x, y, regionID);
				labelImageWONumbers.putValueInt(x, y, regionID);
				binImg.putValueR(x, y, 255);
				binImg.putValueG(x, y, 255);
				binImg.putValueB(x, y, 255);
			}
			
			// mark center of mass by region index
			int comX = (int)r.getCenterOfMass_X();
			int comY = (int)r.getCenterOfMass_Y();
			//			drawStringToImage(this.resultCellOverlayImg, Integer.toString(regionID), 
			//					255, 255, 255, comX-5, comY);
			drawStringToImage(overlayImg, 
					Integer.toString(regionID),	0, 0, 0, comX-5, comY);
			// ... and add IDs only to output result label image
			drawStringToImage(labelImg, 
					Integer.toString(regionID), 255-regionID, comX-5, comY);
		}
		
		// copy to result object
		segResult.resultBinaryImg = (MTBImageByte)binImg.convertType(
				MTBImageType.MTB_BYTE, true);
		segResult.resultOverlayImg = overlayImg;
		segResult.resultOverlayImgWithoutIDs = overlayImgWONumbers;
		segResult.resultLabelImg = labelImg;
		segResult.resultLableImgWithoutIDs = labelImageWONumbers;
		segResult.resultRegs = validRegions;
		
		return segResult;
	}
		
	/**
	 * Extracts features from given set of regions and corresponding images
	 * 
	 * @param img						Input image to process.
	 * @param validRegions	Corresponding set of regions.
	 * @param labelImg			Corresponding label image.
	 * @return Feature table, for details see also {@link MorphologyAnalyzer2D}.
	 * @throws ALDOperatorException				Thrown in case of failure.
	 * @throws ALDProcessingDAGException	Thrown in case of failure.
	 */
	protected MTBTableModel runFeatureExtractionPhase(MTBImage img,
  		MTBRegion2DSet validRegions, MTBImage labelImg) 
		throws ALDOperatorException, ALDProcessingDAGException {
		
		// calculate features for every region
		this.fireOperatorExecutionProgressEvent(
				new ALDOperatorExecutionProgressEvent(this, operatorID 
					+ " calculating region features..."));
		MTBTableModel featureTable = 
				this.calculateRegionFeatures(validRegions, labelImg);
		int featureNum = featureTable.getColumnCount();
		
		// initialize feature stack and visualize results
		if (this.showResultFeatureStack) {
			
			this.fireOperatorExecutionProgressEvent(
					new ALDOperatorExecutionProgressEvent(this, operatorID 
						+ " generating feature stack..."));
			
			MTBImageDouble tmpStack = (MTBImageDouble)MTBImage.createMTBImage(
					this.width, this.height, featureNum, 1, 1, MTBImageType.MTB_DOUBLE);
			this.resultFeatureStack = (MTBImageRGB)MTBImage.createMTBImage(
					this.width, this.height, featureNum, 1, 1, MTBImageType.MTB_RGB);
			this.resultFeatureStack.setTitle("Stack of feature values for " 
					+ "<" + img.getTitle() + ">");
			for (int c=0; c<featureNum; ++c) {
				this.resultFeatureStack.setSliceLabel(
						featureTable.getColumnName(c), c, 0, 0);
			}
			
			GrayscaleImageToHeatmap heatmapper = new GrayscaleImageToHeatmap();
			MTBImageByte ignoreMask = (MTBImageByte)MTBImage.createMTBImage(
					this.width, this.height, 1, 1, 1, MTBImageType.MTB_BYTE);
			ignoreMask.fillWhite();
			
			int label = -1;
			double featureVal;
			double[] minVals = new double[featureNum];
			for (int i=0; i<featureNum; ++i)
				minVals[i] = Double.MAX_VALUE;
			double[] maxVals = new double[featureNum];
			Vector<Point2D.Double> ps;
			for  (MTBRegion2D reg: validRegions) {
				// iterate over all features
				++label;
				ps = reg.getPoints();

				// first entry is of type Integer
				int id = ((Integer)featureTable.getValueAt(
						label, 0)).intValue();
				for (Point2D.Double p: ps) {
					this.resultFeatureStack.putValueR(
							(int)p.x, (int)p.y, 0, 0, 0, id);
					this.resultFeatureStack.putValueG(
							(int)p.x, (int)p.y, 0, 0, 0, id);
					this.resultFeatureStack.putValueB(
							(int)p.x, (int)p.y, 0, 0, 0, id);
					// remember that point belongs to a region
					ignoreMask.putValueInt((int)p.x, (int)p.y, 0);
				}
				for (int c=1; c<featureNum; ++c) {
					featureVal = ((Double)featureTable.getValueAt(
							label, c)).doubleValue();
					if (featureVal < minVals[c])
						minVals[c] = featureVal;
					if (featureVal > maxVals[c])
						maxVals[c] = featureVal;
					
					ps = reg.getPoints();
					for (Point2D.Double p: ps) {						
						tmpStack.putValueDouble((int)p.x, (int)p.y, c, 0, 0, featureVal);
					}
				}
			}
			// convert feature images to heat maps
			for (int c=1; c<featureNum; ++c) {
				// convert feature value image to heatmap
				try {
					heatmapper.setInputImage(tmpStack.getSlice(c, 0, 0));
					heatmapper.setRangeMinimum(minVals[c]);
					heatmapper.setRangeMaximum(maxVals[c]);
					heatmapper.setIgnoreMask(ignoreMask);
					heatmapper.runOp();
					this.resultFeatureStack.setSlice(heatmapper.getHeatmapImage(),c,0,0);
				}
				catch (ALDOperatorException ex) {
					this.fireOperatorExecutionProgressEvent(
						new ALDOperatorExecutionProgressEvent(this, operatorID 
							+ " skipping feature " + featureTable.getColumnName(c) 
								+ ", empty range [" +  minVals[c] + "," + maxVals[c] + "]"));
				}
			}
		}
		return featureTable;
	}
	
	/**
	 * Preprocessing and segmentation of input image.
	 * @param img Image to process.
	 * 
	 * @return	Preprocessed image, i.e. binary image with cell regions.
	 * @throws ALDOperatorException	Thrown in case of processing failure.
	 * @throws ALDProcessingDAGException	
	 * 		Thrown in case of problems with processing history.
	 */
	private MTBImageByte segmentInputImage(MTBImage img) 
			throws ALDOperatorException, ALDProcessingDAGException {

		// perform image intensity stretching
		this.fireOperatorExecutionProgressEvent(
				new ALDOperatorExecutionProgressEvent(this, operatorID 
					+ " -> running contrast enhancement..."));

		GlobalContrastStretching stretchOp = 
				new GlobalContrastStretching(img);
		stretchOp.runOp();
		MTBImageByte enhancedImg = (MTBImageByte)stretchOp.getResultImage();
		MTBImageRGB enhancedImgRGB =
				(MTBImageRGB)enhancedImg.convertType(MTBImageType.MTB_RGB,true);
		enhancedImgRGB.setTitle("Input image after contrast stretching");
		if (this.showAdditionalResultImages)
			this.addResultImages.add(enhancedImgRGB);
		
		// apply noise filter
		this.fireOperatorExecutionProgressEvent(
			new ALDOperatorExecutionProgressEvent(this, operatorID 
				+ " -> applying Gaussian filter..."));

		// perform Gaussian smoothing, except for agarose imprints
		this.gaussFilterImg = enhancedImg;
		if (!(this.imType == ImageType.AGAROSE_IMPRINT)) {
			GaussFilter gaussOp = new GaussFilter();
			gaussOp.setInputImg(enhancedImg);
			gaussOp.runOp(HidingMode.HIDE_CHILDREN);
			this.gaussFilterImg = gaussOp.getResultImg();
			MTBImageRGB gaussFilterResult = 
					(MTBImageRGB)this.gaussFilterImg.convertType(
							MTBImageType.MTB_RGB, true);
			gaussFilterResult.setTitle("Input image after Gauss filtering");
			if (this.showAdditionalResultImages)
				this.addResultImages.add(gaussFilterResult);
		}
		
		// apply vesselness filter to detect cell boundaries
		this.fireOperatorExecutionProgressEvent(
			new ALDOperatorExecutionProgressEvent(this, operatorID 
				+ " -> applying vesselness filter..."));
		OrientedFilter2DBatchAnalyzer batchFilter = 
				new OrientedFilter2DBatchAnalyzer();
		batchFilter.setInputImage(this.gaussFilterImg);
		batchFilter.setAngleSampling(10);
		GaussPDxxFilter2D gFilter = new GaussPDxxFilter2D();
		gFilter.setHeight(9);
		gFilter.enableNormalization();
		if (this.borderContrast == BorderBackgroundContrast.DARK_ON_BRIGHT)
			gFilter.setInvertMask(false);
		else
			gFilter.setInvertMask(true);
		batchFilter.setOrientedFilter(gFilter);
		batchFilter.runOp(HidingMode.HIDE_CHILDREN);
		this.vesselImg = batchFilter.getResultImage();
		MTBImageRGB vesselFilterResult =
			(MTBImageRGB)this.vesselImg.convertType(MTBImageType.MTB_RGB,true);
		vesselFilterResult.setTitle("Result of vesselness filtering");
		if (this.showAdditionalResultImages)
			this.addResultImages.add(vesselFilterResult);
		
		this.fireOperatorExecutionProgressEvent(
			new ALDOperatorExecutionProgressEvent(this, operatorID 
				+ " -> performing binarization and morphological processing..."));

		// eliminate negative filter responses
		for (int y=0;y<this.height;++y) {
			for (int x=0;x<this.width;++x) {
				if (this.vesselImg.getValueDouble(x, y) < 0)
					this.vesselImg.putValueDouble(x, y, 0);
			}
		}
				
		// do some non-linear smoothing
		RankOperator rankOp = 
				new RankOperator(this.vesselImg, RankOpMode.MEDIAN, 1);
		rankOp.runOp();
		this.vesselImg = rankOp.getResultImg();
		
		// binarize vesselness image, cell boundaries become foreground
		ImgThreshNiblack niblackThresholder = new ImgThreshNiblack(
			this.vesselImg, Mode.STD_LOCVARCHECK, 0.0, -1.0, 25, 25, 
				this.niblackVarianceThresh, null);
		niblackThresholder.runOp(HidingMode.HIDE_CHILDREN);
		MTBImageByte binVesselImg = niblackThresholder.getResultImage();
		binVesselImg.setTitle("Binarized vesselness image");
		if (this.showAdditionalResultImages) {
			this.addResultImages.add((MTBImageRGB)(
				binVesselImg.convertType(MTBImageType.MTB_RGB, false)));
			this.addResultImages.get(this.addResultImages.size()-1).
				setTitle("Binarized vesselness image");
		}

		// perform dilation on the binary image
		BasicMorphology morphOp = new BasicMorphology(binVesselImg, null);
		morphOp.setMask(maskShape.CIRCLE, 7);
		morphOp.setMode(BasicMorphology.opMode.DILATE);
		morphOp.runOp(HidingMode.HIDE_CHILDREN);
		MTBImageByte dilVesselImg = (MTBImageByte)morphOp.getResultImage();
		
		// perform erosion on the binary image (borders are white)
		morphOp.setMask(maskShape.CIRCLE, 5);
		morphOp.setInImg(dilVesselImg);
		morphOp.setMode(BasicMorphology.opMode.ERODE);
		morphOp.runOp(HidingMode.HIDE_CHILDREN);
		MTBImageByte eroVesselImg = (MTBImageByte)morphOp.getResultImage();
		
		// remove too small components, most likely noise
		ComponentPostprocess cPost = new ComponentPostprocess(
				eroVesselImg, ProcessMode.ERASE_SMALL_COMPS);
		cPost.setMinimalComponentSize(100);
		cPost.runOp();
		eroVesselImg = (MTBImageByte)cPost.getResultImage();
		
		if (this.showAdditionalResultImages) {
			this.addResultImages.add((MTBImageRGB)(
				eroVesselImg.convertType(MTBImageType.MTB_RGB, false)));
			this.addResultImages.get(this.addResultImages.size()-1).
				setTitle("Post-processed binary vesselness image");
		}
		
		MTBImageByte skelImg = eroVesselImg;

		// try to close gaps along cell borders
		switch(this.gapMode)
		{
		case WATERSHED:
			{
				this.fireOperatorExecutionProgressEvent(
						new ALDOperatorExecutionProgressEvent(this, operatorID 
							+ " -> closing gaps by watershed..."));
				// extract skeleton of cell boundaries
				this.fireOperatorExecutionProgressEvent(
						new ALDOperatorExecutionProgressEvent(this, operatorID 
								+ " -> extracting boundary skeleton..."));
				SkeletonExtractor skelExtractor = new SkeletonExtractor();
				skelExtractor.setInputImage(eroVesselImg);
				skelExtractor.runOp(HidingMode.HIDE_CHILDREN);
				if (this.showAdditionalResultImages) {
					this.addResultImages.add((MTBImageRGB)(
							skelExtractor.getResultImage().convertType(
									MTBImageType.MTB_RGB, false)));
					this.addResultImages.get(this.addResultImages.size()-1).
						setTitle("Skeleton image before gap closing");
				}
				skelImg = closeGapsWatershed(skelExtractor.getResultImage());
				break;
			}
		case NAIVE_HEURISTIC:
			{
				this.fireOperatorExecutionProgressEvent(
						new ALDOperatorExecutionProgressEvent(this, operatorID 
								+ " -> closing gaps by native linking..."));
				// extract skeleton of cell boundaries
				this.fireOperatorExecutionProgressEvent(
						new ALDOperatorExecutionProgressEvent(this, operatorID 
								+ " -> extracting boundary skeleton..."));
				SkeletonExtractor skelExtractor = new SkeletonExtractor();
				skelExtractor.setInputImage(eroVesselImg);
				skelExtractor.runOp(HidingMode.HIDE_CHILDREN);
				if (this.showAdditionalResultImages) {
					this.addResultImages.add((MTBImageRGB)(
							skelExtractor.getResultImage().convertType(
									MTBImageType.MTB_RGB, false)));
					this.addResultImages.get(this.addResultImages.size()-1).
						setTitle("Skeleton image before gap closing");
				}
				skelImg = closeGapsNativeLinks(skelExtractor.getResultImage());
				break;
			}
		case NONE:
		default:
		}
		
		// invert image, boundaries are now black
		for (int y=0; y<this.height; ++y)
			for (int x=0; x<this.width; ++x)
				skelImg.putValueInt(x, y, 
						255 - skelImg.getValueInt(x, y));

		if (this.showAdditionalResultImages) {
			this.addResultImages.add((MTBImageRGB)(
				skelImg.convertType(MTBImageType.MTB_RGB, false)));
			this.addResultImages.get(this.addResultImages.size()-1).
				setTitle("Inverted skeleton image after gap closing");
		}

		// erode the skeleton to extend boundaries
		morphOp.setMask(maskShape.CIRCLE, 3);
		morphOp.setInImg(skelImg);
		morphOp.setMode(BasicMorphology.opMode.ERODE);
		morphOp.runOp(HidingMode.HIDE_CHILDREN);
		skelImg = (MTBImageByte)morphOp.getResultImage();

		// fill holes in region components
		this.fireOperatorExecutionProgressEvent(
				new ALDOperatorExecutionProgressEvent(this, operatorID 
					+ " -> filling holes..."));
		FillHoles2D holeFiller = new FillHoles2D(skelImg);
		holeFiller.runOp(HidingMode.HIDE_CHILDREN);
		MTBImageByte filledImg = (MTBImageByte)holeFiller.getResultImage();
		if (this.showAdditionalResultImages) {
			this.addResultImages.add((MTBImageRGB)(
				filledImg.convertType(MTBImageType.MTB_RGB, false)));
			this.addResultImages.get(this.addResultImages.size()-1).
				setTitle("Result after erosion and hole filling");
		}

		// invert image again, boundaries white again
		for (int y=0; y<this.height; ++y)
			for (int x=0; x<this.width; ++x)
				filledImg.putValueInt(x, y, 
						255 - filledImg.getValueInt(x, y));

		// extract skeleton of cell boundaries
		this.fireOperatorExecutionProgressEvent(
			new ALDOperatorExecutionProgressEvent(this, operatorID 
				+ " -> extracting boundary skeleton..."));
		SkeletonExtractor skelExtractor = new SkeletonExtractor();
		skelExtractor.setInputImage(filledImg);
		skelExtractor.runOp(HidingMode.HIDE_CHILDREN);
		skelImg = skelExtractor.getResultImage();
		
		// invert the skeleton image, cell boundaries get black
		for (int y=0; y<this.height; ++y)
			for (int x=0; x<this.width; ++x)
				skelImg.putValueInt(x, y, 255 - skelImg.getValueInt(x, y));
		skelImg.setTitle("Skeleton image before post-processing");
		MTBImageRGB skelImgResult = 
			(MTBImageRGB)skelImg.convertType(MTBImageType.MTB_RGB, false);
		skelImgResult.setTitle("Skeleton image before post-processing");
		if (this.showAdditionalResultImages)
			this.addResultImages.add(skelImgResult);
		
		// postprocess the boundary skeleton, i.e. remove spines
		SkeletonPostprocessor p = new SkeletonPostprocessor();
		p.setInputImage(skelImg);
		p.runOp(HidingMode.HIDE_CHILDREN);
		skelImg = p.getResultImage();
		skelImg.setTitle("Skeleton image after post-processing");
		skelImgResult = 
			(MTBImageRGB)skelImg.convertType(MTBImageType.MTB_RGB, false);
		skelImgResult.setTitle("Skeleton image after post-processing");
		if (this.showAdditionalResultImages)
			this.addResultImages.add(skelImgResult);
		
		// erode the skeleton to extend boundaries
		morphOp.setMask(maskShape.CIRCLE, 5);
		morphOp.setInImg(skelImg);
		morphOp.setMode(BasicMorphology.opMode.ERODE);
		morphOp.runOp(HidingMode.HIDE_CHILDREN);
		return (MTBImageByte)morphOp.getResultImage();
	}
	
	/**
	 * Method to close gaps by watershed transformation.
	 * 
	 * @param skelImg Input skeleton image of cell borders.
	 * @return	Post-processed skeleton image.
	 * @throws ALDOperatorException	Thrown in case of failure.
	 * @throws ALDProcessingDAGException Thrown in case of failure.
	 */
	@SuppressWarnings("null")
  private MTBImageByte closeGapsWatershed(MTBImageByte skelImg) 
			throws ALDOperatorException, ALDProcessingDAGException {
		
		// close gaps in borders by binary watershed segmentation
		this.fireOperatorExecutionProgressEvent(
				new ALDOperatorExecutionProgressEvent(this, operatorID 
						+ " -> closing gaps by watershed transform..."));
		WatershedBinary watershedOp = new WatershedBinary(skelImg);
		watershedOp.runOp();
		MTBImageByte waterImg = watershedOp.getResultImage();

		// mark which border pixels have been added
		MTBImageByte postWaterImg = (MTBImageByte)waterImg.duplicate();
		for (int y = 0; y < this.height; y++) {
			for (int x = 0; x < this.width; x++) {
				for (int dy = -1; dy <=1; dy++) {
					for (int dx = -1; dx <=1; dx++) {
						if (waterImg.getValueInt(x, y) == 0)
							continue;
						if (   x+dx < 0 || y+dy < 0 
								|| x+dx >= this.width || y+dy >= this.height)
							continue;
						if (skelImg.getValueInt(x+dx, y+dy) == 255) {
							postWaterImg.putValueInt(x, y, 0);
						}
					}
				}
			}
		}

		MTBImageRGB skelPtImg = null;
		if (this.showAdditionalResultImages) {
			skelPtImg = (MTBImageRGB)(
					skelImg.convertType(MTBImageType.MTB_RGB, false));
			// mark new border pieces
			for (int y = 0; y < this.height; y++) {
				for (int x = 0; x < this.width; x++) {
					if (postWaterImg.getValueInt(x, y) == 255) {
						skelPtImg.putValueR(x, y, 0);
						skelPtImg.putValueG(x, y, 255);
						skelPtImg.putValueB(x, y, 0);
					}
				}
			}
		}

		this.fireOperatorExecutionProgressEvent(
				new ALDOperatorExecutionProgressEvent(this, operatorID 
						+ " -> detecting skeleton endpoints..."));

		// find endpoints in skeleton image
		MTBImageByte endPtImg = (MTBImageByte)skelImg.duplicate();
		endPtImg.fillBlack();
		Vector<Vector<Point2D.Double>> epts = 
				BinaryImageEndpointTools.findEndpointBranches(skelImg);
		for (Vector<Point2D.Double> v: epts) {
			endPtImg.putValueInt(
					(int)v.elementAt(0).x, (int)v.elementAt(0).y, 255);
			if (this.showAdditionalResultImages) {
				skelPtImg.putValueR(
						(int)v.elementAt(0).x, (int)v.elementAt(0).y, 255);
				skelPtImg.putValueG(
						(int)v.elementAt(0).x, (int)v.elementAt(0).y, 0);
				skelPtImg.putValueB(
						(int)v.elementAt(0).x, (int)v.elementAt(0).y, 0);
			}
		}

		// check properties of new border pieces, eliminate if doubtful
		LabelComponentsSequential labler = 
				new LabelComponentsSequential(postWaterImg, true);
		labler.runOp();
		MTBRegion2DSet comps = labler.getResultingRegions();
		MTBImage compImg = labler.getLabelImage();
		Vector<Point2D.Double> compEndPts = 
				BinaryImageEndpointTools.findEndpoints(postWaterImg);

		// mark endpoints in component image
		for (Point2D.Double p: compEndPts)
			compImg.putValueInt((int)p.x, (int)p.y, 100);

		MTBImageByte postProcessedBorderImg = 
				(MTBImageByte)waterImg.duplicate();

		int pImgWidth = this.vesselImg.getSizeX();
		int pImgHeight = this.vesselImg.getSizeY();
		double imgMean = 0;
		double imgStdDev = 0;
		for (int y=0; y<pImgHeight; ++y) {
			for (int x=0; x<pImgWidth; ++x) {
				imgMean += this.gaussFilterImg.getValueDouble(x, y);
			}		
		}
		imgMean /= pImgHeight * pImgWidth;
		for (int y=0; y<pImgHeight; ++y) {
			for (int x=0; x<pImgWidth; ++x) {
				imgStdDev += 
						(this.gaussFilterImg.getValueDouble(x, y) - imgMean)
						* (this.gaussFilterImg.getValueDouble(x, y) - imgMean);
			}		
		}
		imgStdDev /= pImgHeight * pImgWidth;
		imgStdDev = Math.sqrt(imgStdDev);

		boolean[][] memoryMap = new boolean[pImgHeight][pImgWidth];

		this.fireOperatorExecutionProgressEvent(
				new ALDOperatorExecutionProgressEvent(this, operatorID 
						+ " -> checking closed gaps for plausibility..."));

		int x, y;
		int validEndpoints = 0;
		int nbRadius = 3;
		Vector<Point2D.Double> closeEndpoints = 
				new Vector<Point2D.Double>();
		for (MTBRegion2D c: comps) {

			// check if endpoints are located close to skeleton endpoints
			closeEndpoints.clear();
			validEndpoints = 0;

			// delete border if it is too long
			if (c.getPoints().size() > 40) {
				for (Point2D.Double p: c.getPoints()) {
					postProcessedBorderImg.putValueInt((int)p.x, (int)p.y, 0);
				}
			}
			
			for (Point2D.Double p: c.getPoints()) {
				x = (int)p.x;
				y = (int)p.y;

				// endpoints are marked with 100
				if (compImg.getValueInt(x, y) == 100) {

					// endpoint found, check if it is close to skeleton endpoint
					if (this.showAdditionalResultImages) {
						skelPtImg.putValueR(x, y, 255);
						skelPtImg.putValueG(x, y, 255);
						skelPtImg.putValueB(x, y, 0);
					}

					for (int dy=-nbRadius; dy<=nbRadius; ++dy) {
						for (int dx=-nbRadius; dx<=nbRadius; ++dx) {
							if (   x+dx>=0 && x+dx<compImg.getSizeX()
									&& y+dy>=0 && y+dy<compImg.getSizeY()) {
								if (endPtImg.getValueInt(x+dx, y+dy) != 0) {
									++validEndpoints;
									closeEndpoints.add(new Point2D.Double(x+dx, y+dy));
									dx = nbRadius+1;
									dy = nbRadius+1;
								}
							}
						}
					}
				}
			}

			// if no valid endpoints could be found, delete border piece
			if (validEndpoints == 0) {
				for (Point2D.Double p: c.getPoints()) {
					postProcessedBorderImg.putValueInt((int)p.x, (int)p.y, 0);
				}
			}
			// if there is one endpoint close, only keep if there is not
			// enough intensity around
			else if (validEndpoints == 1) {

				int branchLength = -1;
				for (Vector<Point2D.Double> pts : epts) {
					if (pts.firstElement().equals(
							closeEndpoints.firstElement())) {
						branchLength = pts.size();
						break;
					}						
				}

				// if gap is at most half as long as branch, keep closed
				if ((double)c.getArea()/(double)branchLength < 0.5)
					continue;

				// otherwise only keep gap closed, if crossed area is dark...
				for (y=0; y<pImgHeight; ++y) {
					for (x=0; x<pImgWidth; ++x) {
						memoryMap[y][x] = false;
					}
				}
				double imgSum = 0;
				int count=0;
				for (Point2D.Double p: c.getPoints()) {
					for (int dy=-2;dy<=2; ++dy) {
						for (int dx=-2;dx<=2; ++dx) {
							int xx = (int)p.x + dx;
							int yy = (int)p.y + dy;
							if (   xx < 0 || xx >= pImgWidth 
									|| yy < 0 || yy >= pImgHeight)
								continue;
							if (memoryMap[yy][xx])
								continue;
							imgSum += this.gaussFilterImg.getValueDouble(xx, yy);
							memoryMap[yy][xx] = true;
							++count;
						}
					}
				}

				if ((      this.borderContrast 
						== BorderBackgroundContrast.BRIGHT_ON_DARK
						&&     imgSum/count < imgMean - 0*imgStdDev)
						||
						(      this.borderContrast 
								== BorderBackgroundContrast.DARK_ON_BRIGHT
								&&     imgSum/count > imgMean + 0*imgStdDev)) {
					// border is lacking contrast, delete section
					for (Point2D.Double p: c.getPoints()) {
						postProcessedBorderImg.putValueInt((int)p.x, (int)p.y, 0);
					}
				}
			}
			else if (validEndpoints == 2) {
				// only keep link if at least one connected branches is long
				int branchLength_1 = -1;
				for (Vector<Point2D.Double> pts : epts) {
					if (pts.firstElement().equals(
							closeEndpoints.firstElement())) {
						branchLength_1 = pts.size();
						break;
					}						
				}
				int branchLength_2 = -1;
				for (Vector<Point2D.Double> pts : epts) {
					if (pts.firstElement().equals(
							closeEndpoints.lastElement())) {
						branchLength_2 = pts.size();
						break;
					}						
				}
				if (branchLength_1 < 3 && branchLength_2 < 3) {
					for (Point2D.Double p: c.getPoints()) {
						postProcessedBorderImg.putValueInt((int)p.x, (int)p.y, 0);
					}
				}
			}
		}
		if (this.showAdditionalResultImages) {
			skelPtImg.setTitle("Intial Watershed transformation result");
			this.addResultImages.add(skelPtImg);
			MTBImageRGB finalWatershedResult = 
				(MTBImageRGB)postProcessedBorderImg.convertType(
						MTBImageType.MTB_RGB, false);
			finalWatershedResult.setTitle(
					"Final Watershed transformation result");	
			this.addResultImages.add(finalWatershedResult);
		}
		return postProcessedBorderImg;
	}
	
	/**
	 * Method to close gaps by linking adjacent endpoints.
	 * 
	 * @param skelImg Input skeleton image of cell borders.
	 * @return	Post-processed skeleton image.
	 */
  private MTBImageByte closeGapsNativeLinks(MTBImageByte skelImg) {
  	

  	MTBImageByte postProcessedImage =	(MTBImageByte)skelImg.duplicate();
  	
  	// find all endpoints
  	Vector<Point2D.Double> endPts = 
  			BinaryImageEndpointTools.findEndpoints(skelImg);
  	
  	// search nearest neighbor for all points
  	int minID = 0;
  	double dist;
  	double minDist = Double.MAX_VALUE;
  	for (int i=0; i<endPts.size(); ++i) {
    	minDist = Double.MAX_VALUE;
  		for (int j=i+1; j<endPts.size(); ++j) {
  			dist = endPts.get(i).distance(endPts.get(j));
  			if (dist < minDist) {
  				minDist = dist;
  				minID = j;
  			}
  		}
    	if (minDist < 20) {
    		postProcessedImage.drawLine2D(
    				(int)endPts.get(i).x, (int)endPts.get(i).y, 
    				(int)endPts.get(minID).x, (int)endPts.get(minID).y, 255);
    	}
  	}
  	return postProcessedImage;
  }
	
	/**
	 * Postprocesses cell regions and applies some filtering.
	 * <p>
	 * The method mainly targets at discarding invalid regions, e.g., 
	 * regions which are too small, touch the image border, or which most 
	 * likely refer to more than one cell and have accidentally been 
	 * merged.
	 * 
	 * @param image	Binary input image (regions white, background black).
	 * @return	Set of valid cell regions.
	 * @throws ALDOperatorException	Thrown in case of operation failure.
	 * @throws ALDProcessingDAGException
	 * 		Thrown in case of problems with the processing history.
	 */
	private MTBRegion2DSet filterValidCellRegions(MTBImageByte image) 
			throws ALDOperatorException, ALDProcessingDAGException {
		
		// define some local variables
		MTBImageByte tmpImg;
		
		// throw away too small and too large regions
		this.fireOperatorExecutionProgressEvent(
				new ALDOperatorExecutionProgressEvent(this, operatorID 
					+ " -> erasing too small and too large regions..."));
		
		// store values to output parameter
		this.resultMinRegionSizeThreshold = this.minimalCellSize;
		this.resultMaxRegionSizeThreshold = this.maximalCellSize;

		int minSizeThreshold = 0;
		int maxSizeThreshold = 0;
		if (this.thresholdUnits == MeasurementUnits.PIXELS) {
			minSizeThreshold = (int)this.minimalCellSize;
			maxSizeThreshold = (int)this.maximalCellSize;
		}
		else {
			minSizeThreshold = (int)(this.minimalCellSize / 
					(this.pixelLengthXYinternal*this.pixelLengthXYinternal));
			maxSizeThreshold = (int)(this.maximalCellSize / 
					(this.pixelLengthXYinternal*this.pixelLengthXYinternal));
		}
		
		ComponentPostprocess postOp = 
				new ComponentPostprocess(image, ProcessMode.ERASE_SMALL_COMPS);
		postOp.setMinimalComponentSize(minSizeThreshold);
		postOp.runOp(HidingMode.HIDE_CHILDREN);
		tmpImg = (MTBImageByte) postOp.getResultImage();
		postOp = 
				new ComponentPostprocess(tmpImg, ProcessMode.ERASE_LARGE_COMPS);
		postOp.setMaximalComponentSize(maxSizeThreshold);
		postOp.runOp(HidingMode.HIDE_CHILDREN);
		tmpImg = (MTBImageByte) postOp.getResultImage();
				
		// fill holes in region components
		this.fireOperatorExecutionProgressEvent(
				new ALDOperatorExecutionProgressEvent(this, operatorID 
					+ " -> filling holes..."));
		FillHoles2D holeFiller = new FillHoles2D(tmpImg);
		holeFiller.runOp(HidingMode.HIDE_CHILDREN);
		tmpImg = (MTBImageByte)holeFiller.getResultImage();
		
		// label regions
		this.fireOperatorExecutionProgressEvent(
				new ALDOperatorExecutionProgressEvent(this, operatorID 
					+ " -> running sequential component labeling..."));
		LabelComponentsSequential labler = 
				new LabelComponentsSequential(tmpImg, true);
		labler.runOp(HidingMode.HIDE_CHILDREN);
		MTBRegion2DSet regions = labler.getResultingRegions();
		
		// filter regions which are touching the image border
		this.fireOperatorExecutionProgressEvent(
				new ALDOperatorExecutionProgressEvent(this, operatorID 
					+ " -> finally filtering regions, found " + regions.size() 
						+ " so far..."));

		MTBRegion2DSet validRegions = new MTBRegion2DSet(regions.getXmin(), 
				regions.getYmin(), regions.getXmax(), regions.getYmax());
		
		boolean touchingBorder = false;
		for (MTBRegion2D r: regions) {
			
			// eliminate region if touching image border; 
			// border is assumed to be of width 15
			touchingBorder = false;
			for (Point2D.Double p: r.getPoints()) {
				if (   (int)p.x < 10 || (int)p.x > this.width-11 
						|| (int)p.y < 10 || (int)p.y > this.height-11) {
					touchingBorder = true;
					break;
				}
			}
			if (touchingBorder)
				continue;
			
			// add region to list of valid cell regions
			validRegions.add(r);
		}
		// return result
		this.fireOperatorExecutionProgressEvent(
				new ALDOperatorExecutionProgressEvent(this, operatorID 
					+ " ==> detected " + validRegions.size() + " cell regions"));
		return validRegions;
	}
	
	/**
	 * Extracts features for all detected and valid cell regions.
	 * 
	 * @param regions		Set of valid regions.
	 * @param labelImg 	Corresponding label image of region set.
	 * @return Feature table.
	 * @throws ALDOperatorException	Thrown in case of failure.
	 * @throws ALDProcessingDAGException	
	 * 		Thrown in case of problems with the processing history.
	 */
	private MTBTableModel calculateRegionFeatures(MTBRegion2DSet regions,
				MTBImage labelImg)
			throws ALDOperatorException, ALDProcessingDAGException {

		// run feature extraction
		this.morphFeatureOp.setDeltaXY(new Double(this.pixelLengthXYinternal));
		this.morphFeatureOp.setUnitXY(this.pixelUnitString);
		this.morphFeatureOp.setRegionSet(regions);
		this.morphFeatureOp.setLabelImage(labelImg);
		this.morphFeatureOp.setDrawCurvatureInfoImage(
				this.showAdditionalResultImages);
		this.morphFeatureOp.setDrawSkeletonInfoImage(
				this.showAdditionalResultImages);				
		this.morphFeatureOp.runOp(HidingMode.HIDE_CHILDREN);
		MTBTableModel morphTab = this.morphFeatureOp.getTable();
		
		// process label image for lobe types
		MTBImageShort finalLabelImg = null;
		if (labelImg != null)
			finalLabelImg = 
				(MTBImageShort)labelImg.convertType(MTBImageType.MTB_SHORT, true);
		else
			finalLabelImg = 
				(MTBImageShort)this.morphFeatureOp.getLabelImage().convertType(
						MTBImageType.MTB_SHORT, true);
		Vector<MorphologyAnalyzer2DInProData> ipRes =
				this.morphFeatureOp.getDetailedInProResults();
		
		// init result data structures for lobe type classification, if requested
		if (this.classifyLobes) {
			this.resultLobeTypeImage = (MTBImageRGB)MTBImage.createMTBImage(
					this.width, this.height, 1, 1, 1, MTBImageType.MTB_RGB);
			this.resultLobeTypeImage.fillWhite();

			// enlarge regions to close gaps in between
			ComponentPostprocess pp = new ComponentPostprocess();
			pp.setInputImage(finalLabelImg);
			pp.setProcessMode(ProcessMode.DILATE_TOPOLOGY_PRESERVING);
			pp.setDilateMaskSize(5);
			pp.runOp();
			finalLabelImg = (MTBImageShort)pp.getResultImage();
			
			// detect pixel positions where three (non-background) regions touch
			TreeSet<Integer> labels = new TreeSet<>();
			Vector<Point2D.Double> threeContactPoints = new Vector<>();
			for (int y=0;y<finalLabelImg.getSizeY();++y) {
				for (int x=0;x<finalLabelImg.getSizeX();++x) {
					labels.clear();
					// just check 3x3 neighborhood
					for (int dy=-1;dy<=1;++dy) {
						for (int dx=-1;dx<=1;++dx) {
							if (   x+dx>=0 && x+dx<this.width 
									&& y+dy>=0 && y+dy<this.height) {
								labels.add(new Integer(finalLabelImg.getValueInt(x+dx, y+dy))); 
							}
						}
					}
					// remove the null from label set, if element of the set
					labels.remove(new Integer(0));
					// if there are still 3 labels left, we found a 3-contact-point
					if (labels.size() == 3)
						threeContactPoints.add(new Point2D.Double(x,y));
				}
			}
			
			// vector for lobe type data
			this.resultLobeFeatureTables = new Vector<MTBTableModel>();
			Vector<String> header = new Vector<String>();
			header.add(FeatureNames.Object.toString());
			header.add("Type");
			header.add("EquatorLength ("+ this.pixelUnitString + ")");
			header.add("BaselineLength ("+ this.pixelUnitString + ")");
			header.add("ApicalLength ("+ this.pixelUnitString + ")");
			header.add("BasalLength ("+ this.pixelUnitString + ")");
			header.add("TotalLength ("+ this.pixelUnitString + ")");
			header.add("ApicalContourLong ("+ this.pixelUnitString + ")");
			header.add("ApicalContourShort ("+ this.pixelUnitString + ")");
		
			// iterate over all regions, and for each region over all lobes,
			// and count for each lobe the number of adjacent labels
			int rID = 1;
			int nbSize = 7;
			for (MorphologyAnalyzer2DInProData mipd: ipRes) {

				LinkedList<InProContourSegment> segs = mipd.getProtrusionSegments();

				MTBTableModel lobeTab = 
						new MTBTableModel(segs.size(), header.size(), header);

				int segID = 0;
				double leftLength, rightLength;
				for (InProContourSegment ipc: segs) {
		
					leftLength= 0;
					rightLength= 0;

					labels.clear();
					for (Point2D.Double p: ipc.initialSegmentPoints) {
						int px = (int)p.x;
						int py = (int)p.y;
						for (int dy=-nbSize;dy<=nbSize;++dy) {
							for (int dx=-nbSize;dx<=nbSize;++dx) {
								if (   px+dx>=0 && px+dx<this.width 
										&& py+dy>=0 && py+dy<this.height)
									labels.add(
											new Integer(finalLabelImg.getValueInt(px+dx, py+dy)));
							}						
						}
					}
					LobeTypes lobeClass = LobeTypes.UNDEFINED;
					int color = 0;
					// type 1: two regions, no background
					if (labels.size() == 2 && !labels.contains(new Integer(0))) {
						color = 1;
						lobeClass = LobeTypes.TYPE_1;
					}
					// type 2: three regions, no background 
					else if (labels.size() == 3 && !labels.contains(new Integer(0))) {
						color = 2;
						lobeClass = LobeTypes.TYPE_2;
						
						// search for contact point closest to contour
						int minID=-1;
						double minDist = Double.MAX_VALUE;
						int id = 0;
						for (Point2D.Double p: ipc.initialSegmentPoints) {
							for (Point2D.Double t: threeContactPoints) {
								if (p.distance(t) < minDist) {
									minDist = p.distance(t);
									minID = id;
								}
							}
							++id;
						}
						// draw 3-contact-point to result image
						this.resultLobeTypeImage.drawCircle2D(
							(int)ipc.initialSegmentPoints.get(minID).x, 
								(int)ipc.initialSegmentPoints.get(minID).y, 0, 2, 0);
						// calculate segment lengths
						for (int i=0; i<minID; ++i) {
							leftLength += ipc.initialSegmentPoints.get(i).distance(
									ipc.initialSegmentPoints.get(i+1));
						}
						leftLength *= this.pixelLengthXYinternal;
						for (int i=minID; i<ipc.initialSegmentPoints.size()-1; ++i) {
							rightLength += ipc.initialSegmentPoints.get(i).distance(
									ipc.initialSegmentPoints.get(i+1));
						}
						rightLength *= this.pixelLengthXYinternal;		
					}
					// undefined: cell close to unsegmented background
					else {
						color = 0;
						lobeClass = LobeTypes.UNDEFINED;
					}
					for (Point2D.Double p: ipc.initialSegmentPoints) {
						int px = (int)p.x;
						int py = (int)p.y;
						switch(color) 
						{
						case 0:
							this.resultLobeTypeImage.putValueR(px, py, color);
							this.resultLobeTypeImage.putValueG(px, py, color);
							this.resultLobeTypeImage.putValueB(px, py, color);
							break;
						case 1:
							this.resultLobeTypeImage.putValueR(px, py, 0);
							this.resultLobeTypeImage.putValueG(px, py, 0);
							this.resultLobeTypeImage.putValueB(px, py, 255);
							break;
						case 2:
							this.resultLobeTypeImage.putValueR(px, py, 255);
							this.resultLobeTypeImage.putValueG(px, py, 120);
							this.resultLobeTypeImage.putValueB(px, py, 80);
							break;
						}
					}
					lobeTab.setValueAt(new Integer(segID+1), segID, 0);
					lobeTab.setValueAt(lobeClass.toString(), segID, 1);			
					lobeTab.setValueAt(new Double(ipc.getEquatorLength()), segID, 2);
					lobeTab.setValueAt(new Double(ipc.getBaselineLength()), segID, 3);
					lobeTab.setValueAt(new Double(ipc.getApicalLength()), segID, 4);
					lobeTab.setValueAt(new Double(ipc.getBasalLength()), segID, 5);
					lobeTab.setValueAt(new Double(ipc.getTotalLength()), segID, 6);
					if (   lobeClass.equals(LobeTypes.TYPE_1)
							|| lobeClass.equals(LobeTypes.UNDEFINED)) {
						lobeTab.setValueAt(new Double(Double.NaN), segID, 7);			
						lobeTab.setValueAt(new Double(Double.NaN), segID, 8);									
					}
					else {
						if (leftLength > rightLength) {
							lobeTab.setValueAt(new Double(leftLength), segID, 7);			
							lobeTab.setValueAt(new Double(rightLength), segID, 8);
						}
						else {
							lobeTab.setValueAt(new Double(rightLength), segID, 7);			
							lobeTab.setValueAt(new Double(leftLength), segID, 8);							
						}
					}
					++segID;
				}
				// store table to result vector
				this.resultLobeFeatureTables.add(lobeTab);
						
				segs = mipd.getIndentationSegments();
				for (InProContourSegment ipc: segs) {
					for (Point2D.Double p: ipc.initialSegmentPoints) {
						int px = (int)p.x;
						int py = (int)p.y;
						this.resultLobeTypeImage.putValueR(px, py, 200);
						this.resultLobeTypeImage.putValueG(px, py, 200);
						this.resultLobeTypeImage.putValueB(px, py, 200);
					}				
				}
				
				// draw ID to image in black
				if (this.drawRegionIDsToOutputImages) {
					drawStringToImage(this.resultLobeTypeImage, Integer.toString(rID), 
						0, 0, 0, (int)(regions.get(rID-1).getCenterOfMass_X()), 	
							(int)(regions.get(rID-1).getCenterOfMass_Y()));
				}
				++rID;
			}
		} // end of lobe type classification part
		
		// if just features are extracted and no image was given, we don't have
		// a label image until now, so get one from morphology analyzer
		if (this.resultCellLabelImg == null) {
			this.resultCellLabelImg = (MTBImageShort)
					this.morphFeatureOp.getLabelImage().convertType(
							MTBImageType.MTB_SHORT, true);
			// mark center of masses by region IDs
			if (this.drawRegionIDsToOutputImages) {
				int regionID = 1;
				for (MTBRegion2D r: regions) {
					int comX = (int)r.getCenterOfMass_X();
					int comY = (int)r.getCenterOfMass_Y();
					drawStringToImage(this.resultCellLabelImg, 
							Integer.toString(regionID), 255-regionID, comX-5, comY);
					++regionID;
				}
			}
		}
		
		if (this.showAdditionalResultImages) {
			MTBImageRGB curvInfoImg = this.morphFeatureOp.getCurvatureInfoImage();
			curvInfoImg.setTitle("Curvature info image");
			this.addResultImages.add(curvInfoImg);
			MTBImageRGB skelInfoImg = this.morphFeatureOp.getSkeletonInfoImage();
			skelInfoImg.setTitle("Skeleton features info image");
			this.addResultImages.add(skelInfoImg);
		}
		
		// initialize result feature table, region IDs range from 1 to N
		int featureNum = 29;
		MTBTableModel featureTable = new MTBTableModel(regions.size(), 
				featureNum);
		// select relevant columns and update names
		String colName, featureName;
		for (int c=0; c<featureNum; ++c) {
			colName = morphTab.getColumnName(c);
			featureName = colName.split(" ")[0];
			if (featureNameMapper.get(featureName) != null)
				colName = morphTab.getColumnName(c).replace(
					featureName, featureNameMapper.get(featureName));
			featureTable.setColumnName(c, colName.replace(" ", "_"));
		}

		// copy and convert data
		for (int r=0; r<morphTab.getRowCount(); ++r) {
			featureTable.setValueAt(new Integer(r+1), r, 0);
			for (int c=1; c<featureNum; ++c) {
				String morphValue = (String)morphTab.getValueAt(r, c);
				// convert numbers to English style with '.' instead of ','
				morphValue = morphValue.replace(",",".");
				try {
					featureTable.setValueAt(new Double(morphValue), r, c);
				} catch (NumberFormatException nex) {
					featureTable.setValueAt(new Double(Double.NaN), r, c);
				}
			}
		}
		// post-process skeleton features, a skeleton with only two-endpoints
		// or less is not a branch for us
		int branchCountIndex = -1;
		int branchLengthIndex = -1;
		for (int c=1; c<morphTab.getColumnCount(); ++c) {
			if (morphTab.getColumnName(c).startsWith(
						MorphologyAnalyzer2D.FeatureNames.BranchCount.toString())) {
				branchCountIndex = c;
			}
			if (morphTab.getColumnName(c).startsWith(
					MorphologyAnalyzer2D.FeatureNames.AvgBranchLength.toString())) {
				branchLengthIndex = c;
			}
		}
		for (int r=0; r<morphTab.getRowCount(); ++r) {
			double count = ((Double)featureTable.getValueAt(
					r, branchCountIndex)).doubleValue();
			if (count >= 2) 
				continue;
			featureTable.setValueAt(new Double(0), r, branchCountIndex);
			featureTable.setValueAt(new Double(Double.NaN), r, branchLengthIndex);			
		}
		return featureTable;
	}
	
	/**
	 * Draws string at given position into image. 
	 * 
	 * @param img		Image where to draw the string into.
	 * @param s			String to draw.
	 * @param r			Red color ratio.
	 * @param g			Green color ratio.
	 * @param b			Blue color ratio.
	 * @param xPos	Position of string in x.
	 * @param yPos	Position of string in y.
	 */
	protected static void drawStringToImage(MTBImageRGB img, String s, 
			int r, int g, int b, int xPos, int yPos) {		
		// red channel
		MTBImageByte imgtmp= (MTBImageByte)img.getChannelR();
		ImageProcessor ip= imgtmp.getImagePlus().getProcessor();
		ip.moveTo(xPos, yPos);
		ip.setColor(r);
		ip.drawString(s);
		// green channel
		imgtmp= (MTBImageByte)img.getChannelG();
		ip= imgtmp.getImagePlus().getProcessor();
		ip.moveTo(xPos, yPos);
		ip.setColor(g);
		ip.drawString(s);
		// blue channel
		imgtmp= (MTBImageByte)img.getChannelB();
		ip= imgtmp.getImagePlus().getProcessor();
		ip.moveTo(xPos, yPos);
		ip.setColor(b);
		ip.drawString(s);
	}
	
	/**
	 * Draws string at given position into image. 
	 * 
	 * @param img		Image where to draw the string into.
	 * @param s			String to draw.
	 * @param g			Grayscale value.
	 * @param xPos	Position of string in x.
	 * @param yPos	Position of string in y.
	 */
	protected static void drawStringToImage(MTBImageShort img, String s, 
			int g, int xPos, int yPos) {		
		// red channel
		ImageProcessor ip= img.getImagePlus().getProcessor();
		ip.moveTo(xPos, yPos);
		ip.setColor(g);
		ip.drawString(s);
	}

	/**
	 * Internal class for representing the result data of segmenting an image.
	 */
	private class SegmentationResult {
		
		/**
		 * Set of segmented regions.
		 */
		public MTBRegion2DSet resultRegs;
		
		/**
		 * Image overlay of detected regions over input image. 
		 */
		public MTBImageRGB resultOverlayImg;
		
		/**
		 * Image overlay of detected regions over input image without ID strings. 
		 */
		public MTBImageRGB resultOverlayImgWithoutIDs;

		/**
		 * Result image with labels of segmented regions.
		 */
		public MTBImageShort resultLabelImg;

		/**
		 * Label image without region ID strings.
		 */
		public MTBImageShort resultLableImgWithoutIDs;

		/**
		 * Binary image with segmented regions in foreground (white).
		 */
		public MTBImageByte resultBinaryImg;
	}
	
	/**
	 * Helper operator to post-process border skeletons.
	 * <p>
	 * This operator mainly removes spines from the border skeletons 
	 * and eliminates regions including very long intrusions, i.e.
	 * spines which are longer than the given threshold or even branch.
	 */
	private class SkeletonPostprocessor extends MTBOperator {
		
		/**
		 * Identifier string for this operator class.
		 */
		private static final String opID = "\t [SkeletonPostprocessor]";

		/**
		 * Input skeleton image to process.
		 * <p>
		 * The operator expects a binary image with the skeleton pixels 
		 * having black color on a white background.
		 */
		@Parameter(label = "Input Image", required = true,
				direction = Parameter.Direction.IN, description = "Input image.",
				dataIOOrder = 0)
		private transient MTBImageByte inputImg = null;

		/**
		 * Maximal allowed length of a branch to be accepted as spine.
		 */
		@Parameter(label = "Maximal Length of Spines", required = true,
				direction = Parameter.Direction.IN, 
				description = "Max. spine length.", dataIOOrder = 1)
		private int maxSpineLength = 40;

		/**
		 * Postprocessed skeleton image.
		 */
		@Parameter(label = "Postprocessed Image", dataIOOrder = 0,
				direction = Parameter.Direction.OUT, 
				description = "Postprocessed skeleton image.")
		private transient MTBImageByte postprocessedImg = null;

		/**
		 * Default constructor.
		 * @throws ALDOperatorException Thrown on instantiation failures.
		 */
		public SkeletonPostprocessor() throws ALDOperatorException {
			// nothing to do here
		}

		/**
		 * Set input image to process.
		 * <p>
		 * Expecting skeleton to be black on white background.
		 * 
		 * @param img	Input skeleton image.
		 */
		public void setInputImage(MTBImageByte img) {
			this.inputImg = img;
		}
		
		/**
		 * Set maximal length of spines.
		 * @param maxLength	Maximal length of spines.
		 */
		@SuppressWarnings("unused")
    public void setMaximalSpineLength(int maxLength) {
			this.maxSpineLength = maxLength;
		}
		
		/**
		 * Returns postprocessed skeleton image.
		 * @return	Result image.
		 */
		public MTBImageByte getResultImage() {
			return this.postprocessedImg;
		}
		
		/* (non-Javadoc)
		 * @see de.unihalle.informatik.Alida.operator.ALDOperator#operate()
		 */
		@Override
		protected void operate() 
				throws ALDOperatorException, ALDProcessingDAGException {

			// remove spines
			this.fireOperatorExecutionProgressEvent(
					new ALDOperatorExecutionProgressEvent(this, opID 
						+ " removing spines..."));

			// init the result image
			this.postprocessedImg = (MTBImageByte)this.inputImg.duplicate();
			
			LabelComponentsSequential cl = 
					new LabelComponentsSequential(this.inputImg, false);
			cl.runOp();
			MTBImage labelImg = cl.getLabelImage();
			
			MTBRegion2DSet regs = cl.getResultingRegions();
			for (MTBRegion2D r: regs) {
				int minX = (int)(r.getMinMaxCoordinates()[0]);
				int minY = (int)(r.getMinMaxCoordinates()[1]);
				int maxX = (int)(r.getMinMaxCoordinates()[2]);
				int maxY = (int)(r.getMinMaxCoordinates()[3]);
				MTBImageByte regImg = r.toMTBImageByte(null, maxX+1, maxY+1);
				int regImgWidth = regImg.getSizeX();
				int regImgHeight = regImg.getSizeY();
				
				// if region touches border, just ignore
				if (   minX == 0 || maxX == this.inputImg.getSizeX()-1 
						|| minY == 0 || maxY == this.inputImg.getSizeY()-1)
					continue;
				
				int label = labelImg.getValueInt(
						(int)r.getPoints().get(0).x,(int)r.getPoints().get(0).y);
				
				MTBImageByte spineImg = 
						(MTBImageByte)regImg.duplicate(HidingMode.HIDDEN);
				spineImg.fillBlack();
				int nx, ny;
				for (int y=1; y<regImgHeight-1; ++y) {
					for (int x=1; x<regImgWidth-1; ++x) {
						if (regImg.getValueInt(x, y) > 0)
							continue;
						// black pixel, check if it is close to region
						boolean closeToRegion = false;
						for (int dx=-1;dx<=1 && !closeToRegion; ++dx) {
							for (int dy=-1;dy<=1 && !closeToRegion; ++dy) {
								if (dx==0 && dy==0)
									continue;
								nx = x+dx;
								ny = y+dy;
								if (   nx<0 || nx>=labelImg.getSizeX()
										|| ny<0 || ny>=labelImg.getSizeY())
									continue;
								if (labelImg.getValueInt(nx, ny) == label)
									closeToRegion = true;
							}
						}
						if (!closeToRegion)
							continue;
						
						boolean foreignerFound = false;
						for (int dx=-1;dx<=1 && !foreignerFound; ++dx) {
							for (int dy=-1;dy<=1 && !foreignerFound; ++dy) {
								if (dx==0 && dy==0)
									continue;
								nx = x+dx;
								ny = y+dy;
								if (   nx<0 || nx>=labelImg.getSizeX()
										|| ny<0 || ny>=labelImg.getSizeY())
									continue;
								if (   labelImg.getValueInt(nx, ny) != 0
										&& labelImg.getValueInt(nx, ny) != label) {
									foreignerFound = true;
								}
							}
						}
						if (!foreignerFound)
							spineImg.putValueInt(x, y, 255);
					}
				}
				
				// find endpoints (value=100) and branch points (value=200)
				MTBImageByte ebImg = 
						(MTBImageByte)spineImg.duplicate(HidingMode.HIDDEN);
				ebImg.fillBlack();
				int neighborCount = 0;
				for (int y=1; y<spineImg.getSizeY()-1; ++y) {
					for (int x=1; x<spineImg.getSizeX()-1; ++x) {
						if (spineImg.getValueInt(x, y) == 0)
							continue;
						neighborCount=0;
						for (int dx=-1; dx<=1; ++dx) {
							for (int dy=-1; dy<=1; ++dy) {
								if (dx==0 && dy==0)
									continue;
								nx = x+dx;
								ny = y+dy;
								if (   nx<0 || nx>=spineImg.getSizeX()
										|| ny<0 || ny>=spineImg.getSizeY())
									continue;
								if (spineImg.getValueInt(nx, ny) == 255)
									++neighborCount;
							}
						}
						if (neighborCount == 1)
							ebImg.putValueInt(x, y, 100);
						else if (neighborCount > 2)
							ebImg.putValueInt(x, y, 200);
					}
				}
				
				// label image
				cl.setInputImage(spineImg);
				cl.setDiagonalNeighborsFlag(true);
				cl.runOp();
				MTBRegion2DSet spineRegions = cl.getResultingRegions();

				boolean spineSurviving = false;
				for (MTBRegion2D sr : spineRegions) {
					Vector<Point2D.Double> srp = sr.getPoints();
					int branchPointCount = 0;
					for (Point2D.Double p: srp) {
						if (ebImg.getValueInt((int)p.x, (int)p.y) == 200)
							++branchPointCount;						
					}
					
					// check region
					if (   sr.getArea() < this.maxSpineLength 
							&& branchPointCount == 0) {
						for (Point2D.Double p: srp) {
							this.postprocessedImg.putValueInt((int)p.x,(int)p.y,255);
						}						
					}
					else {
						spineSurviving = true;
						break;
					}
				}
				
				// there are long and/or branching spines remaining, kill region
				if (spineSurviving) {
					for (Point2D.Double p: r.getPoints()) {
						this.postprocessedImg.putValueInt((int)p.x, (int)p.y, 0);
					}
				}
			}
		}
	}
}
