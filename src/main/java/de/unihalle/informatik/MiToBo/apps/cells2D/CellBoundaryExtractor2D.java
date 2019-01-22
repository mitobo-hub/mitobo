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

package de.unihalle.informatik.MiToBo.apps.cells2D;

import java.awt.geom.Point2D;
import java.io.File;
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
import de.unihalle.informatik.Alida.operator.ALDOperator;
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
import de.unihalle.informatik.MiToBo.core.imageJ.RoiWriter;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;
import de.unihalle.informatik.MiToBo.filters.linear.GaussFilter;
import de.unihalle.informatik.MiToBo.filters.linear.GaussFilter.SigmaInterpretation;
import de.unihalle.informatik.MiToBo.filters.linear.anisotropic.GaussPDxxFilter2D;
import de.unihalle.informatik.MiToBo.filters.linear.anisotropic.OrientedFilter2DBatchAnalyzer;
import de.unihalle.informatik.MiToBo.io.dirs.DirectoryTree;
import de.unihalle.informatik.MiToBo.io.images.ImageReaderMTB;
import de.unihalle.informatik.MiToBo.io.images.ImageWriterMTB;
import de.unihalle.informatik.MiToBo.math.graphs.DijkstraShortestPixelPathFinder;
import de.unihalle.informatik.MiToBo.math.graphs.DijkstraShortestPixelPathFinder.WeightModel;
import de.unihalle.informatik.MiToBo.morphology.BinaryImageEndpointTools;
import de.unihalle.informatik.MiToBo.morphology.ComponentPostprocess;
import de.unihalle.informatik.MiToBo.morphology.ComponentPostprocess.ProcessMode;
import de.unihalle.informatik.MiToBo.morphology.ImgOpen;
import de.unihalle.informatik.MiToBo.morphology.SkeletonExtractor;
import de.unihalle.informatik.MiToBo.morphology.SkeletonPostprocessor;
import de.unihalle.informatik.MiToBo.segmentation.regions.labeling.LabelComponentsSequential;
import de.unihalle.informatik.MiToBo.segmentation.thresholds.ImgThreshNiblack;
import de.unihalle.informatik.MiToBo.segmentation.thresholds.ImgThreshNiblack.Mode;
import de.unihalle.informatik.MiToBo.tools.image.ImageDimensionReducer;
import de.unihalle.informatik.MiToBo.tools.image.ImageDimensionReducer.ReducerMethod;

/**
 * Operator for segmenting cell boundaries in microtubuli experiments.
 * <p>
 * This operator basically applies a vesselness filter to extract 
 * boundaries of cells in microtubuli experiments. As tubular and
 * vessel-like structures not only appear along the cell boundaries 
 * the results require (manual) post-processing including gap closing,
 * removing false detections, skeleton extraction and spine removal.
 * 
 * @author Birgit Moeller
 */
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.ALL, 
	level=Level.APPLICATION, allowBatchMode = false)
public class CellBoundaryExtractor2D extends MTBOperator {

	/**
	 * Operation mode of the operator.
	 */
	public static enum OperationMode {
		/**
		 * Process images / ROIs in given directories.
		 */
		BATCH,
		/**
		 * Process a single image provided directly.
		 */
		SINGLE_IMAGE
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
	 * Identifier string for this operator class.
	 */
	private static final String operatorID = "[CellBoundaryExtractor2D]";
	
	/**
	 * Mode of operation of the operator.
	 */
	@Parameter(label = "Operation Mode", required = true, 
		direction = Parameter.Direction.IN,	dataIOOrder = -5, 
		description = "Operation mode of the operator.",
		callback = "switchOpModeParameters",
		paramModificationMode = ParameterModificationMode.MODIFIES_INTERFACE)
	public OperationMode operatorMode = OperationMode.BATCH;
	
	/**
	 * Input directory where to find the images to process in batch mode.
	 */
	@Parameter(label = "Input Directory", required = true, 
		direction = Parameter.Direction.IN, description = "Input directory.",
		dataIOOrder = -3)
	private ALDDirectoryString inDir = null;

	/**
	 * Input grayscale image/stack to process.
	 */
	@Parameter(label = "Input Image", required = true, 
		direction = Parameter.Direction.IN, description = "Input image.",
		dataIOOrder = -3)
	private transient MTBImage inImg = null;

	/**
	 * Channel of input image containing stained cell boundaries.
	 */
	@Parameter(label = "Cell Boundary Channel", required = true, 
		direction = Parameter.Direction.IN, dataIOOrder = -2, 
		description = "Boundary Channel, e.g., 1, 2 and so on.")
	private int boundaryChannel = 1;

	/**
	 * Border to background relation.
	 */
	@Parameter(label = "Border Contrast", required = true, 
		direction = Parameter.Direction.IN,	dataIOOrder = 3, 
		description = "Contrast of cell boundaries wrt to background.")
	public BorderBackgroundContrast borderContrast = 
		BorderBackgroundContrast.BRIGHT_ON_DARK;

	/**
	 * Threshold for the minimal admissible cell size.
	 * <p>
	 * Cell regions falling below this threshold are ignored.
	 */
	@Parameter(label = "Minimal Size of Cells", required = true, 
			direction = Parameter.Direction.IN, dataIOOrder = 4,
			description = "Cells smaller than this threshold are discarded.")
	private int minimalCellSize = 2500;

	/**
	 * Threshold for the maximal admissible cell size.
	 * <p>
	 * Cell regions lying above this threshold are ignored.
	 */
	@Parameter(label = "Maximal Size of Cells", required = true, 
			direction = Parameter.Direction.IN, dataIOOrder = 5,
			description = "Cells larger than this threshold are discarded.")
	private int maximalCellSize = 1000000;

	/**
	 * Flag to enable/disable showing additional and intermediate result
	 * images.
	 */
	@Parameter(label = "Show/save additional results?", required = false,
			supplemental = true, direction = Parameter.Direction.IN, 
			dataIOOrder = -1, mode = ExpertMode.STANDARD,
			description = "Enable/disable showing/saving additional results.")
	private boolean showAdditionalResultImages = false;
	
	/**
	 * Label image of detected cell regions.
	 */
	@Parameter(label = "Label Image of Detected Cell Regions", 
			dataIOOrder = 0, direction = Parameter.Direction.OUT, 
			description = "Label image of detected cell regions.")
	private transient MTBImage resultCellLabelImg = null;

	/**
	 * ROI set of detected cell regions.
	 */
	@Parameter(label = "Detected Cell Region Contours", 
			dataIOOrder = 1, direction = Parameter.Direction.OUT, 
			description = "Cell region contours in ImageJ ROI format.")
	private transient MTBRegion2DSet resultCellContours = null;
	
	/**
	 * Result of applying the vesselness filter to the input image.
	 */
	@Parameter(label = "Vessels", 
		dataIOOrder = 2, direction = Parameter.Direction.OUT, 
		description = "Result image of vesselness enhancement filter.")
	private transient MTBImageByte resultVesselImg = null;

	/**
	 * Binarized vesselness image.
	 */
	@Parameter(label = "Binarized Vesselness Image", 
			dataIOOrder = 4, direction = Parameter.Direction.OUT, 
			description = "Binarized vesselness image.")
	private transient MTBImageByte resultBinVesselImg;
	
	/**
	 * Filtered binary vesselness image.
	 */
	@Parameter(label = "Filtered Binary Vesselness Image", 
			dataIOOrder = 5, direction = Parameter.Direction.OUT, 
			description = "Filtered binary vesselness image.")
	private transient MTBImageByte resultBinFilteredImg;

	/**
	 * Initial skeleton image.
	 */
	@Parameter(label = "Initial skeleton image before post-processing.", 
			dataIOOrder = 6, direction = Parameter.Direction.OUT, 
			description = "Initial skeleton image.")
	private transient MTBImageByte resultInitialSkeletonImg;

	/**
	 * Skeleton image (of cell boundaries).
	 */
	@Parameter(label = "Skeleton Image", 
			dataIOOrder = 7, direction = Parameter.Direction.OUT, 
			description = "Skeleton image.")
	private transient MTBImageByte resultSkelImg;

	/**
	 * Width of the input image.
	 */
	private transient int width;
	
	/**
	 * Height of the input image.
	 */
	private transient int height;
	
	/*
	 * Some internal helper variables.
	 */
	
	/**
	 * Label image of extracted cell regions.
	 */
	private transient MTBImage cellLabelImg = null;
	
	/**
	 * Set of extracted regions. 
	 */
	private transient MTBRegion2DSet cellContours = null;

	/**
	 * Intermediate result: vesselness image.
	 */
	private transient MTBImageByte vesselImg = null;

	/**
	 * Intermediate result: binarized vesselness image.
	 */
	private transient MTBImageByte binVesselImg;
	
	/**
	 * Intermediate result: binarized vesselness image after filtering.
	 */
	private transient MTBImageByte binFilteredImg;

	/**
	 * Intermediate result: initial contour skeleton.
	 */
	private transient MTBImageByte initialSkelImg;

	/**
	 * Intermediate result: final contour skeleton.
	 */
	private transient MTBImageByte skelImg;
	
	/**
	 * Default constructor.
	 * @throws ALDOperatorException Thrown if construction fails.
	 */
	public CellBoundaryExtractor2D() throws ALDOperatorException {
	}
	
	/**
	 * Callback routine to change operator mode parameters.
	 */
	@SuppressWarnings("unused")
	private void switchOpModeParameters() {
		try {
			if (this.operatorMode == OperationMode.SINGLE_IMAGE) {
				if (this.hasParameter("inDir")) {
					this.removeParameter("inDir");
				}
				if (!this.hasParameter("inImg")) {
					this.addParameter("inImg");
				}
			} else if (this.operatorMode == OperationMode.BATCH) {
				if (this.hasParameter("inImg")) 
					this.removeParameter("inImg");
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
	
	/* (non-Javadoc)
	 * @see de.unihalle.informatik.Alida.operator.ALDOperator#operate()
	 */
	@Override
	protected void operate() 
			throws ALDOperatorException, ALDProcessingDAGException {

		ALDOperator.setConstructionMode(
				ALDOperator.HistoryConstructionMode.NO_HISTORY);
		
		// either process a single image or a complete directory tree
		switch(this.operatorMode) 
		{
		case SINGLE_IMAGE:
		{
			// reset operator
			this.resultVesselImg = null;
			this.resultBinVesselImg = null;
			this.resultBinFilteredImg = null;
			this.resultInitialSkeletonImg = null;
			this.resultSkelImg = null;

			// process image
			this.processImage(this.inImg);
			
			// output label image
			this.resultCellLabelImg = this.cellLabelImg;
			this.resultCellLabelImg.setTitle("Result label image for <" 
					+ this.inImg.getTitle() + ">");

			// output cell contours
			this.resultCellContours = this.cellContours;
			
			// optional additional results
			if (this.showAdditionalResultImages) {
				this.resultVesselImg = this.vesselImg;
				this.resultBinVesselImg = this.binVesselImg;
				this.resultBinFilteredImg = this.binFilteredImg;
				this.resultInitialSkeletonImg = this.initialSkelImg;
				this.resultSkelImg = this.skelImg;
			}
			break;
		}
		case BATCH:
		{
			// reset operator
			this.resultVesselImg = null;
			this.resultBinVesselImg = null;
			this.resultBinFilteredImg = null;
			this.resultInitialSkeletonImg = null;
			this.resultSkelImg = null;

			DirectoryTree rootDirTree = 
					new DirectoryTree(this.inDir.getDirectoryName(), true);

			this.fireOperatorExecutionProgressEvent(
					new ALDOperatorExecutionProgressEvent(this, operatorID 
							+ " processing directory <" 
							+ this.inDir.getDirectoryName() + ">,"  	
							+ " searching for sub-directories..."));
			Vector<String> allDirs = rootDirTree.getSubdirectoryList();

			// iterate over all directories
			for (String dir : allDirs) {

				// skip result directories
				if (ALDFilePathManipulator.getFileName(dir).startsWith("results"))
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
				String resultDir = dir + File.separator + "results_segmentation"; 
				if (!new File(resultDir).exists()) {
					if (!new File(resultDir).mkdir())
						throw new ALDOperatorException(
								OperatorExceptionType.OPERATE_FAILED, operatorID 
								+ " could not init result folder... exiting!");
				}

				String outFile;
				for (String f: files) {
					try {
						imRead.setFileName(f);
						imRead.runOp();
						MTBImage im = imRead.getResultMTBImage();						
						this.processImage(im);

						String fileName = ALDFilePathManipulator.getFileName(f);

						// save label image
						outFile = 
							resultDir + File.separator + fileName + "-label.tif";
						imWrite.setFileName(outFile);
						imWrite.setInputMTBImage(this.cellLabelImg);
						imWrite.runOp();
						
						// save contour regions
						outFile = 
							resultDir + File.separator + fileName + "-allRois.zip";
				  	RoiWriter rw = new RoiWriter();
				  	MTBContour2DSet cSet = new MTBContour2DSet();
				  	for (MTBRegion2D reg : this.cellContours) {
				  		MTBContour2D creg = reg.getContour();
				  		cSet.add(creg);
				  	}
				  	rw.setOutputFile(outFile);
				  	rw.setData(cSet);
				  	rw.runOp();
				  	
						// optional additional results
						if (this.showAdditionalResultImages) {
							
							// save vessel image
							outFile = 
								resultDir + File.separator + fileName + "-vessels.tif";
							imWrite.setFileName(outFile);
							imWrite.setInputMTBImage(this.vesselImg);
							imWrite.runOp();

							// save binarized vessel image
							outFile = resultDir + File.separator + fileName 
									+ "-vessels-binarized.tif";
							imWrite.setFileName(outFile);
							imWrite.setInputMTBImage(this.binVesselImg);
							imWrite.runOp();

							// save binarized and filtered vessel image
							outFile = resultDir + File.separator + fileName 
									+ "-vessels-binarized-filtered.tif";
							imWrite.setFileName(outFile);
							imWrite.setInputMTBImage(this.binFilteredImg);
							imWrite.runOp();

							// save initial skeleton image
							outFile = resultDir + File.separator + fileName
									+ "-skeleton-initial.tif";
							imWrite.setFileName(outFile);
							imWrite.setInputMTBImage(this.initialSkelImg);
							imWrite.runOp();

							// save skeleton image
							outFile = resultDir + File.separator + fileName
									+ "-skeleton-final.tif";
							imWrite.setFileName(outFile);
							imWrite.setInputMTBImage(this.skelImg);
							imWrite.runOp();

						}
				  	
					} catch (Exception e) {
						e.printStackTrace();
					}
					
				}
			}
			break;
		}}
	}
	
	/**
	 * Main image analysis routine.
	 * 
	 * @param im	Image (stack) to process.
	 * @throws ALDOperatorException				Thrown in case of failure.
	 * @throws ALDProcessingDAGException	Thrown in case of failure.
	 */
	private void processImage(MTBImage im) 
			throws ALDOperatorException, ALDProcessingDAGException {
		
		this.fireOperatorExecutionProgressEvent(
				new ALDOperatorExecutionProgressEvent(this, operatorID 
					+ " initializing operator, processing image <" 
						+ im.getTitle() + ">..."));

		// init some variables
		this.width = im.getSizeX();
		this.height = im.getSizeY();

		// if input image has a z-size larger than 1,  
		// perform a maximum projection first
		MTBImage workImg = im;
		if (im.getSizeZ() > 1) {
			ImageDimensionReducer reduce = new ImageDimensionReducer(im, 
				false, false, true, false, false, ReducerMethod.MAX);
			reduce.runOp();
			workImg = reduce.getResultImg();
		}
		
		// extract cell boundary channel
		MTBImage boundaryImg = workImg.getSlice(0, 0, this.boundaryChannel-1);
		
		// enhance vessel-like structures, basically by applying a vesselness filter
		this.vesselImg = this.enhanceBoundaries(boundaryImg);

		// binarize vessel image and perform some morphological post-processing
		this.postProcessImage(this.vesselImg);
	}
	
	/**
	 * Enhance cell boundaries by applying a vesselness enhancement filter.
	 * 
	 * @param img		Input grayscale image to process.
	 * @return Gray-scale image with region boundaries enhanced.
	 * @throws ALDProcessingDAGException 	Thrown in case of failure.
	 * @throws ALDOperatorException 			Thrown in case of failure.
	 */
	private MTBImageByte enhanceBoundaries(MTBImage img) 
			throws ALDOperatorException, ALDProcessingDAGException {
		
		// apply a vesselness filter to the given image
		this.fireOperatorExecutionProgressEvent(
				new ALDOperatorExecutionProgressEvent(this, operatorID 
						+ " preprocessing and segmenting input image..."));
		MTBImageByte enhancedImg = this.applyVesselnessFilter(img);
		
		// store preprocessed image as intermediate result
		MTBImageByte workingImage = (MTBImageByte)enhancedImg.duplicate();
		if (this.showAdditionalResultImages) {
			MTBImageRGB resultWorkImg = 
					(MTBImageRGB)MTBImage.createMTBImage(workingImage.getSizeX(), 
							workingImage.getSizeY(), 1, 1, 1,	MTBImageType.MTB_RGB);
			resultWorkImg.setTitle("Preprocessed image.");
			for (int y=0; y<workingImage.getSizeY(); ++y) {
				for (int x=0; x<workingImage.getSizeX(); ++x) { 
					resultWorkImg.putValueR(x,y,workingImage.getValueInt(x, y));
					resultWorkImg.putValueG(x,y,workingImage.getValueInt(x, y));
					resultWorkImg.putValueB(x,y,workingImage.getValueInt(x, y));
				}
			}
		}
		
		return enhancedImg;
	}
	
	/**
	 * Gaussian smoothing and vesselness enhancement filtering.
	 * 
	 * @param img 	Image to process.
	 * @return	Filtered image.
	 * @throws ALDOperatorException		Thrown in case of processing failure.
	 * @throws ALDProcessingDAGException	
	 * 		Thrown in case of problems with processing history.
	 */
	private MTBImageByte applyVesselnessFilter(MTBImage img) 
			throws ALDOperatorException, ALDProcessingDAGException {
		
		// contrast enhancement of image, except for agarose imprints
		MTBImage enhancedImg = img;
		
		// apply noise filter
		this.fireOperatorExecutionProgressEvent(
			new ALDOperatorExecutionProgressEvent(this, operatorID 
				+ " -> applying Gaussian filter..."));

		// perform Gaussian smoothing, except for agarose imprints
		MTBImage gaussFilterImg = enhancedImg;
		GaussFilter gaussOp = new GaussFilter();
		gaussOp.setInputImg(enhancedImg);
		gaussOp.runOp(HidingMode.HIDE_CHILDREN);
		gaussFilterImg = gaussOp.getResultImg();
		
		// apply vesselness filter to detect cell boundaries
		this.fireOperatorExecutionProgressEvent(
			new ALDOperatorExecutionProgressEvent(this, operatorID 
				+ " -> applying vesselness filter..."));
		OrientedFilter2DBatchAnalyzer batchFilter = 
				new OrientedFilter2DBatchAnalyzer();
		batchFilter.setInputImage(gaussFilterImg);
		batchFilter.setAngleSampling(10);
		GaussPDxxFilter2D gFilter = new GaussPDxxFilter2D();
		gFilter.setHeight(15);
		gFilter.enableNormalization();
		if (this.borderContrast == BorderBackgroundContrast.DARK_ON_BRIGHT)
			gFilter.setInvertMask(false);
		else
			gFilter.setInvertMask(true);
		batchFilter.setOrientedFilter(gFilter);
		batchFilter.runOp(HidingMode.HIDE_CHILDREN);
		MTBImageDouble vImg = batchFilter.getResultImage();
		
		this.fireOperatorExecutionProgressEvent(
			new ALDOperatorExecutionProgressEvent(this, operatorID 
				+ " -> performing binarization and morphological processing..."));

		// eliminate negative filter responses and scale vesselness image to 
		// a maximum value of 255 (-> otherwise Niblack is not working...)
		double scaleFactor = 0;
		for (int y=0;y<this.height;++y) {
			for (int x=0;x<this.width;++x) {
				if (vImg.getValueDouble(x, y) < 0)
					vImg.putValueDouble(x, y, 0);
				else if (vImg.getValueDouble(x, y) > scaleFactor)
					scaleFactor = vImg.getValueDouble(x, y);
			}
		}
		for (int y=0;y<this.height;++y) {
			for (int x=0;x<this.width;++x) {
				vImg.putValueDouble(x, y, 
						vImg.getValueDouble(x, y)/scaleFactor * 255.0);
			}
		}
		return (MTBImageByte)vImg.convertType(MTBImageType.MTB_BYTE, true);
	}
	
	/**
	 * Post-process the vesselness filter result.
	 * @param img	Image to process.
	 * @throws ALDProcessingDAGException 	Thrown in case of failure.
	 * @throws ALDOperatorException 			Thrown in case of failure.
	 */
	private void postProcessImage(MTBImage img) 
			throws ALDOperatorException, ALDProcessingDAGException {

		ImgThreshNiblack niblackThresholder = new ImgThreshNiblack(
				img, Mode.STD_LOCVARCHECK, 0.0, -1.0, 21, 21, 3.0, null);
		niblackThresholder.runOp(HidingMode.HIDE_CHILDREN);
		this.binVesselImg = niblackThresholder.getResultImage();
		this.binVesselImg.setTitle("Binarized vesselness image");

		ComponentPostprocess cp =	new ComponentPostprocess(this.binVesselImg, 
				ProcessMode.ERASE_SMALL_COMPS);
		cp.setDiagonalNeighbors(true);
		cp.setMinimalComponentSize(200);
		cp.runOp();
		this.binFilteredImg = (MTBImageByte)cp.getResultImage();

		// extract skeleton of cell boundaries, skeleton is white
		this.fireOperatorExecutionProgressEvent(
				new ALDOperatorExecutionProgressEvent(this, operatorID 
						+ " -> extracting boundary skeleton..."));
		SkeletonExtractor skelExtractor = new SkeletonExtractor();
		skelExtractor.setInputImage(this.binFilteredImg);
		skelExtractor.runOp(HidingMode.HIDE_CHILDREN);
		this.initialSkelImg = 
				(MTBImageByte)skelExtractor.getResultImage().duplicate();
		this.skelImg = skelExtractor.getResultImage();

		// invert the skeleton image, cell boundaries get black
		for (int y=0; y<this.height; ++y)
			for (int x=0; x<this.width; ++x)
				this.skelImg.putValueInt(x, y, 
						255 - this.skelImg.getValueInt(x, y));

		// postprocess the boundary skeleton, i.e. remove spines
		SkeletonPostprocessor sp = new SkeletonPostprocessor();
		sp.setInputImage(this.skelImg);
		sp.runOp(HidingMode.HIDE_CHILDREN);
		this.skelImg = sp.getResultImage();

		// label open branches and invert image, skeleton gets white/branches gray 
		MTBImageByte branchLabelImg = (MTBImageByte)this.skelImg.duplicate();
		for (int y=0; y<this.height; ++y) {
			for (int x=0; x<this.width; ++x) {
				branchLabelImg.putValueInt(x, y, 255-branchLabelImg.getValueInt(x, y));
			}
		}
		Vector<Vector<Point2D.Double>> branches = 
				BinaryImageEndpointTools.findEndpointBranches(branchLabelImg);
		Vector<Point2D.Double> ePoints = new Vector<>();
		int branchCount = 1, px, py;
		for (Vector<Point2D.Double> br: branches) {
			ePoints.add(br.firstElement());
			for (Point2D.Double p: br) {
				px = (int)p.x;
				py = (int)p.y;
				if (branchLabelImg.getValueInt(px, py) == 255)
					branchLabelImg.putValueInt(px, py, branchCount);
			}
			++branchCount;
		}
		
		int maxEndDist = 45;
		int maxPixelDist = 30;
		DijkstraShortestPixelPathFinder ip = 
				new DijkstraShortestPixelPathFinder();
		// search for bright pixels
		ip.setInvertPixelValues(true);
		// consider only nodes with values above 150
		ip.setNodeThreshold(0);
		ip.setWeightModel(WeightModel.INTENSITY_CUBIC);
		
		// smooth vessel image for smoother paths
		GaussFilter gaussOp = new GaussFilter();
		gaussOp.setInputImg(this.vesselImg);
		gaussOp.setSigmaInterpretation(SigmaInterpretation.PIXEL);
		gaussOp.runOp(HidingMode.HIDE_CHILDREN);
		MTBImageByte smoothedVesselImg = 
				(MTBImageByte)gaussOp.getResultImg().convertType(
						MTBImageType.MTB_BYTE, true);

		int branchLabel = 0, npx, npy;
		double spIntensity, epIntensity;
		double nodeT = 150;
		Point2D.Double p, np, tp = new Point2D.Double();
		for (int ec=0; ec<ePoints.size(); ++ec) {
			p = ePoints.get(ec);
			px = (int)p.x;
			py = (int)p.y;
			branchLabel = branchLabelImg.getValueInt(px, py);
			spIntensity = smoothedVesselImg.getValueDouble(px, py);
			
			int minX = 
					(px - maxEndDist > 0) ? (px - maxEndDist) : 0; 
			int maxX = 
					(px + maxEndDist < this.width) ? (px+maxEndDist) : this.width-1; 
			int minY = 
					(py - maxEndDist > 0) ? (py - maxEndDist) : 0; 
			int maxY = 
					(py + maxEndDist < this.height) ? (py+maxEndDist) : this.height-1;
			
			int iW = maxX-minX+1;
			int iH = maxY-minY+1; 
			
			MTBImageByte clip = (MTBImageByte)MTBImage.createMTBImage(
					iW, iH, 1, 1, 1, MTBImageType.MTB_BYTE);
			int ny=0;
			for (int y = minY; y<= maxY; ++y) {
				int nx=0;
				for (int x = minX; x<= maxX; ++x) {
					clip.putValueInt(nx, ny, smoothedVesselImg.getValueInt(x, y));
					++nx;
				}			
				++ny;
			}
			
			boolean partnerFound = false;
			Vector<Point2D.Double> path;
			for (int nc=ec+1; nc < ePoints.size(); ++nc) {
				np = ePoints.get(nc);
				npx = (int)np.x;
				npy = (int)np.y;
				epIntensity = smoothedVesselImg.getValueDouble(npx, npy);
				
				// check if label is the same, if so, skip
				if (branchLabelImg.getValueInt(npx, npy) == branchLabel) {
					continue;
				}
				
				// check if potential partner is located in ROI
				if (npx >= minX && npx <= maxX && npy >= minY && npy <= maxY) {
					partnerFound = true;
					tp.x = npx-minX;
					tp.y = npy-minY;
					ip.setInputImage(clip);
					ip.setStartPixel(new Point2D.Double(px-minX, py-minY));
					
					nodeT = 150;
					if (spIntensity < nodeT)
						nodeT = spIntensity-10;
					if (epIntensity < nodeT)
						nodeT = epIntensity-10;
//					ip.setNodeThreshold(nodeT);
					ip.setEndPixel(tp);
					ip.runOp();
					path = ip.getResultPath();
					
					if (path != null && ip.getResultCosts()/path.size() < 1500000) {
						for (Point2D.Double pp: path) {
							this.skelImg.putValueInt((int)pp.x+minX, (int)pp.y+minY, 0);
						}
//						System.out.println("Endpoint path: " + p + " -> " + np + " = " 
//								+ (ip.getResultCosts()/path.size()));
					}
				}
			}
			// if there were no endpoints found, check for other skelett points in 
			// a significantly smaller vicinity, check them all and take path with
			// minimal cost
			if (!partnerFound) {
				minX = 
					(px - maxPixelDist > 0) ? (px - maxPixelDist) : 0; 
				maxX = 
					(px + maxPixelDist < this.width) ? (px+maxPixelDist) : this.width-1; 
				minY = 
					(py - maxPixelDist > 0) ? (py - maxPixelDist) : 0; 
				maxY = 
					(py + maxPixelDist < this.height) ? (py+maxPixelDist) : this.height-1;
				
				iW = maxX-minX+1;
				iH = maxY-minY+1; 

				Vector<Point2D.Double> candidates = new Vector<>();
				for (int y=minY; y<=maxY; ++y) {
					for (int x=minX; x<maxX; ++x) {
						if (   branchLabelImg.getValueInt(x, y) > 0
								&& branchLabelImg.getValueInt(x, y) != branchLabel)
							candidates.add(new Point2D.Double(x, y));
					}
				}

				clip = (MTBImageByte)MTBImage.createMTBImage(
						iW, iH, 1, 1, 1, MTBImageType.MTB_BYTE);
				ny=0;
				for (int y = minY; y<= maxY; ++y) {
					int nx=0;
					for (int x = minX; x<= maxX; ++x) {
						clip.putValueInt(nx, ny, smoothedVesselImg.getValueInt(x, y));
						++nx;
					}			
					++ny;
				}

				Vector<Point2D.Double> minPath = null;
				double cost, minCost = Double.MAX_VALUE;
				for (Point2D.Double c: candidates) {
					int cx = (int)c.x;
					int cy = (int)c.y;
					
					epIntensity = smoothedVesselImg.getValueDouble(cx, cy);
					
					nodeT = 150;
					if (spIntensity < nodeT)
						nodeT = spIntensity-10;
					if (epIntensity < nodeT)
						nodeT = epIntensity-10;
					
					ip.setInputImage(clip);
					ip.setStartPixel(new Point2D.Double(px-minX, py-minY));
					ip.setEndPixel(new Point2D.Double(cx-minX, cy-minY));
					ip.runOp();
					path = ip.getResultPath();
					cost = ip.getResultCosts();
					if (cost < minCost) {
						minCost = cost;
						minPath = path;
					}
				}
				if (minPath != null && minCost/minPath.size() < 1500000) {
					for (Point2D.Double pp: minPath) {
						this.skelImg.putValueInt((int)pp.x+minX, (int)pp.y+minY, 0);
					}
				}
			}
		}

		// smooth boundaries
		ImgOpen io = new ImgOpen(this.skelImg, 5);
		io.runOp();
		this.skelImg = (MTBImageByte)io.getResultImage();

		// invert image, boundaries become white
		for (int y=0; y<this.height; ++y)
			for (int x=0; x<this.width; ++x)
				this.skelImg.putValueInt(x, y, 
						255 - this.skelImg.getValueInt(x, y));

		// extract skeleton again to ensure 1-pixel width
		skelExtractor.setInputImage(this.skelImg);
		skelExtractor.runOp(HidingMode.HIDE_CHILDREN);
		this.skelImg = skelExtractor.getResultImage();
		
		// invert image, boundaries become black
		for (int y=0; y<this.height; ++y)
			for (int x=0; x<this.width; ++x)
				this.skelImg.putValueInt(x, y, 
						255 - this.skelImg.getValueInt(x, y));

		// remove remaining spines
		sp.setInputImage(this.skelImg);
		sp.setMaximalSpineLength(100);
		sp.runOp(HidingMode.HIDE_CHILDREN);
		this.skelImg = sp.getResultImage();
		
		// remove too small regions, but attention, boundaries use 8er-NB, so
		// foreground must not consider diagonal neighbors!
		cp =	new ComponentPostprocess(this.skelImg, 
				ProcessMode.ERASE_SMALL_COMPS);
		cp.setDiagonalNeighbors(false);
		cp.setMinimalComponentSize(250);
		cp.runOp();
		this.skelImg = (MTBImageByte)cp.getResultImage();
		
		// label final components
		LabelComponentsSequential labler = 
				new LabelComponentsSequential(this.skelImg, true);
		labler.setDiagonalNeighborsFlag(false);
		labler.runOp(HidingMode.HIDE_CHILDREN);
		this.cellLabelImg = labler.getLabelImage();
		this.cellContours = labler.getResultingRegions();
	}
	
	/**
	 * Close gaps by native linkage.
	 * 
	 * @param inSkelImg	Skeleton image.
	 * @return	Skeleton image with closed gaps.
	 */
	private MTBImageByte closeGapsNativeLinks(MTBImageByte inSkelImg) {

		MTBImageByte postProcessedImage =	
				(MTBImageByte)inSkelImg.duplicate();

		// find all endpoints
		Vector<Point2D.Double> endPts = findEndpoints(inSkelImg);

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
			if (minDist < 40) {
				postProcessedImage.drawLine2D(
						(int)endPts.get(i).x, (int)endPts.get(i).y, 
						(int)endPts.get(minID).x, (int)endPts.get(minID).y, 255);
			}
		}
		return postProcessedImage;
	}

	/**
	 * Detect end-points in binary image.
	 * 
	 * @param img	Input image.
	 * @return	Set of end-points.
	 */
	public static Vector<Point2D.Double> findEndpoints(MTBImageByte img) {

		int width = img.getSizeX();
		int height = img.getSizeY();

		Vector<Point2D.Double> endPoints = new Vector<Point2D.Double>();

		int nCount = 0, nSum = 0;
		for (int y=0; y<height; ++y) {
			for (int x=0; x<width; ++x) {

				// only foreground pixels are relevant
				if (img.getValueInt(x, y) > 0) {

					// count the neighbors, check if we have an endpoint
					nCount = 0; nSum = 0;
					for (int dx=-1;dx<=1;++dx) {
						for (int dy=-1;dy<=1;++dy) {

							// ignore the pixel itself
							if (dx == 0 && dy == 0)
								continue;

							if (   x+dx >= 0 && x+dx < width 
									&& y+dy >= 0 && y+dy < height) {

								/*
								 * To check for endpoints the neighborhood is encoded
								 * as follows. Each neighboring pixel gets a code 
								 * according to the following scheme where X is the
								 * pixel under consideration:
								 * 
								 *    128    1    2
								 *    
								 *     64    X    4
								 *     
								 *     32   16    8
								 *     
								 * The pixel in question is an endpoint if there is
								 * only a single neighbor, or if there are two neighbors
								 * located close to each other. Given the sum of the 
								 * codes for all neigbors, an endpoint is present if the 
								 * sum is equal to any sum of two subsequent code values
								 * in clockwise ordering, i.e., if it is equal to 3, 6,
								 * 12, 24, 48, 96, 192 or 129.
								 */
								if (img.getValueInt(x+dx, y+dy) > 0) {
									++nCount;
									if (dx == -1) {
										if (dy == -1)
											nSum += 128;
										if (dy == 0)
											nSum += 64;
										if (dy == 1)
											nSum += 32;
									}
									if (dx == 0) {
										if (dy == -1)
											nSum += 1;
										if (dy == 1)
											nSum += 16;
									}
									if (dx == 1) {
										if (dy == -1)
											nSum += 2;
										if (dy == 0)
											nSum += 4;
										if (dy == 1)
											nSum += 8;
									}
								}
							}
						}
					}
					if (        nCount==1 
							|| (    nCount==2 
							&& (nSum==3  || nSum==6  || nSum==12  || nSum==24 
							|| nSum==48 || nSum==96 || nSum==192 || nSum==129))) {
						endPoints.add(new Point2D.Double(x, y));
					}
				}
			}
		}
		return endPoints;
	}

}
