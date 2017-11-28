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

package de.unihalle.informatik.MiToBo.apps.particles2D;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.Vector;

import loci.common.StatusEvent;
import loci.common.StatusListener;
import loci.common.StatusReporter;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.ALDDerivedClass;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.Direction;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.Alida.exceptions.ALDException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException.OperatorExceptionType;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.Alida.operator.events.ALDControlEvent;
import de.unihalle.informatik.Alida.operator.events.ALDControlEvent.ALDControlEventType;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBImageHistogram;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2D;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBTree;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBTreeNode;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBTreeNodeRegion2D;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageByte;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageRGB;
import de.unihalle.informatik.MiToBo.math.images.MTBImageArithmetics;
import de.unihalle.informatik.MiToBo.morphology.ImgDilate;
import de.unihalle.informatik.MiToBo.morphology.ImgErode;
import de.unihalle.informatik.MiToBo.segmentation.regions.filling.FillHoles2D;
import de.unihalle.informatik.MiToBo.segmentation.regions.labeling.LabelComponentsSequential;
import de.unihalle.informatik.MiToBo.segmentation.thresholds.CalcGlobalThreshOtsu;
import de.unihalle.informatik.MiToBo.segmentation.thresholds.ImgThresh;
import de.unihalle.informatik.MiToBo.segmentation.thresholds.ImgThreshNiblack;
import de.unihalle.informatik.MiToBo.transforms.UndecimatedWaveletTransform;
import de.unihalle.informatik.MiToBo.visualization.drawing.DrawRegion2DSet;
import de.unihalle.informatik.MiToBo.visualization.drawing.DrawRegion2DSet.DrawType;

/**
 * Detector for spotlike structures (bright on dark background) in 2D 
 * based on the undecimated wavelet transform.
 * <p>
 * The undecimated wavelet transform produces wavelet coefficient images 
 * that correspond to the results of filtering the original image with a 
 * bank of filters. There is one lowpass-filtered image, one 
 * highpass-filtered image and several bandpass filtered images, 
 * depending on the parameter <code>Jmax</code>. The different 
 * bands/filtered images are referenced by the 'scale'. 
 * The parameters <code>Jmin</code> and <code>Jmax</code> determine 
 * the lower and upper scale limit and, thus, control the range of 
 * wavelet images that are taken into account for particle detection. 
 * Scale 1 corresponds to the highpass filtered image and increasing 
 * scales correspond to decreasing frequency bands. 
 * The <code>scaleIntervalSize</code> parameter determines, how many 
 * wavelet images of adjacent scales are used to compute a wavelet 
 * correlation image, which is the multiplication of wavelet coefficients 
 * over adjacent scales at each pixel.
 * <p>
 * A short example: <code>Jmin=2, Jmax=4, scaleIntervalSize=2</code> <br>
 * This means that the bandpass filtered images of scale 2 and 3 are 
 * multiplied for one correlation image, and the bandpass filtered images 
 * of scale 3 and 4 for another.
 * <p>
 * Correlation images are then thresholded by <code>corrThreshold</code> 
 * to yield hypotheses of particle detections. Because multiple 
 * hypotheses can exist at the same location due to multiple correlation 
 * images, a kind of hypothesis testing is used to determine the more 
 * likely detection at a certain location.
 * <p>
 * The resulting set of particle detections can be extracted from the 
 * particle detector as a set of regions. In addition, in verbose mode 
 * the sets of computed original and binary correlation images are also 
 * available as image stacks. 
 * <p>
 * The input image may be transformed if it contains Poisson noise to 
 * simulate Gaussian noise using the parameter 
 * <code>poisson2gauss</code>.
 * <p>
 * For further details refer to:
 * <ul>
 * <li>O. Gress, B. Möller, N. Stöhr, S. Hüttelmaier, S. Posch, <i>
 * "Scale-adaptive wavelet-based particle detection in microscopy images"
 * </i>.<br> In Proc. Bildverarbeitung für die Medizin (BVM 2010), 
 * pages 266-270, March 2010, Springer.
 * </ul> 
 *  
 * @author Oliver Gress
 */
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.ALL,
		level=Level.STANDARD, allowBatchMode=false)
@ALDDerivedClass
public class ParticleDetectorUWT2D extends ParticleDetector 
	implements StatusReporter {
	
	/**
	 * Identifier for outputs in verbose mode.
	 */
	private final static String opIdentifier = "[ParticleDetectorUWT2D] ";
	
	/**
	 * Input image to process.
	 */
	@Parameter( label = "Input image", required = true, 
		direction = Parameter.Direction.IN,	mode = ExpertMode.STANDARD, 
		dataIOOrder = 1, description = "Input image.")
	private transient MTBImage inputImage = null;

	/**
	 * Minimal scale to consider.
	 */
	@Parameter( label= "Jmin", required = true, 
		direction = Parameter.Direction.IN, mode = ExpertMode.STANDARD, 
		dataIOOrder = 2, description = "Minimum scale index.")
	private Integer Jmin = new Integer(2);

	/**
	 * Maximum scale to consider.
	 */
	@Parameter( label= "Jmax", required = true, 
		direction = Parameter.Direction.IN, mode = ExpertMode.STANDARD, 
		dataIOOrder = 3, description = "Maximum scale index.")
	private Integer Jmax = new Integer(4);

	/**
	 * Size of scale interval for calculating wavelet correlation images.
	 */
	@Parameter( label= "Scale-interval size", required = true, 
		direction = Parameter.Direction.IN, mode=ExpertMode.STANDARD, 
		dataIOOrder = 4, 
		description = "Size of scale interval for correlation images.")
	private Integer scaleIntervalSize = new Integer(2);
	
	/**
	 * Threshold for correlation images.
	 */
	@Parameter( label= "Correlation threshold", required = true, 
		direction = Parameter.Direction.IN, mode = ExpertMode.STANDARD, 
		dataIOOrder = 5, 
		description = "Threshold for wavelet correlation images.")
	private Double corrThreshold = new Double(1.5);
	
	/**
	 * Minimal size of valid regions.
	 */
	@Parameter( label= "Minimum region size", required = true, 
		direction = Parameter.Direction.IN, mode = ExpertMode.STANDARD, 
		dataIOOrder = 6, description = "Minimum area of detected regions.")
	private int minRegionSize = 1;
	
	/**
	 * Flag to activate Poisson-to-Gaussian noise transform.
	 */
	@Parameter( label= "Apply Poisson-to-Gaussian noise transform", 
		required = true, direction = Parameter.Direction.IN, 
		mode = ExpertMode.ADVANCED, dataIOOrder = 7, 
		description = "Transform input image with poisson noise to " + 
			"image with Gaussian noise (J.-L. Starck et al).")
	private boolean poisson2gauss = true;

	/**
	 * Flag to use initial sigma = 0.5 for the wavelet transform
	 */
	@Parameter( label= "use initial sigma = 0.5 for the wavelet transform", 
		required = true, direction = Parameter.Direction.IN, 
		mode = ExpertMode.ADVANCED, dataIOOrder = 8, 
		description = "use initial sigma = 0.5 for the wavelet transform")
	private boolean initialSigmaOneHalve = false;

/**
	 * Optional mask to exclude particles in certain regions.
	 * <p>
	 * Particles in masked regions are ignored if mask is non-null.
	 */
	@Parameter( label = "Exclude mask", direction = Direction.IN, 
		mode = ExpertMode.ADVANCED, dataIOOrder = 9, required = false, 
		description = "Exclude mask.")
	private transient MTBImageByte excludeMask = null;

	/**
	 * Flag to activate calculation of additional result images.
	 */
	@Parameter( label= "Provide (binarized) correlation images", 
		supplemental = true, direction = Parameter.Direction.IN, 
		mode = ExpertMode.ADVANCED, dataIOOrder = 10, 
		description = "If enabled additional intermediate results are provided.")
	private boolean additionalResultsWanted = false;

	/**
	 * Number of detected regions.
	 */
	@Parameter( label = "#Regions", direction = Parameter.Direction.OUT, 
		mode = ExpertMode.STANDARD,	dataIOOrder = 1, 
		description = "Number of detected regions.")
	private transient int resultRegionCount = 0;

	/**
	 * Set of detected particle regions.
	 */
	@Parameter( label = "Resulting particle regions", 
		direction = Parameter.Direction.OUT, mode = ExpertMode.STANDARD, 
		dataIOOrder = 2, description = "Detected particle regions.")
	private transient MTBRegion2DSet resultingRegions = null;

	/**
	 * Detected regions plotted into input image.
	 */
	@Parameter( label = "Resulting particle regions", 
		direction = Parameter.Direction.OUT, mode = ExpertMode.STANDARD, 
		dataIOOrder = 3, description = "Overlay of regions onto input image.")
	private transient MTBImage resultOverlay = null;

	/**
	 * Binary mask of detected regions.
	 */
	@Parameter( label = "Binary particle region mask", 
		direction = Parameter.Direction.OUT, mode = ExpertMode.STANDARD, 
		dataIOOrder = 4, description = "Binary particle region mask.")
	private transient MTBImageByte resultMaskBinary = null;

	/**
	 * Stack of correlation images for different scale combinations.
	 * <p>
	 * This stack is only generated if {@link #additionalResultsWanted} is selected.
	 */
	@Parameter( label = "Correlation images", 
		direction = Parameter.Direction.OUT, mode = ExpertMode.STANDARD,	
		dataIOOrder = 1, supplemental = true,
		description = "Correlation images for different scale intervals.")
	private transient MTBImage correlationImages = null;
	
	/**
	 * Stack of binarized correlation images.
	 * <p>
	 * This stack is only generated if {@link #additionalResultsWanted} is selected.
	 */
	@Parameter( label = "Binarized correlation images", 
		direction = Parameter.Direction.OUT, mode = ExpertMode.STANDARD,	
		dataIOOrder = 2, supplemental = true,
		description = "Binarized correlation images.")
	private transient MTBImageByte binaryCorrelationImages = null;

	/** 
	 * Vector of installed {@link StatusListener} objects.
	 */
	protected Vector<StatusListener> m_statusListeners;
	
	/**
	 * Constructor.	
	 */
	public ParticleDetectorUWT2D() throws ALDOperatorException {
		this.m_statusListeners = new Vector<StatusListener>(1);
		this.operatorExecStatus = OperatorExecutionStatus.OP_EXEC_INIT;
	}

	
	/**
	 * Constructor with non-default parameters.
	 * @param img input image with bright granules on dark background
	 * @param _Jmin lowest scale index to be used for correlation image computations (scale is reciprocal to frequency properties)
	 * @param _Jmax highest scale index to be used for correlation image computations (scale is reciprocal to frequency properties)
	 * @param _corrThreshold threshold for wavelet correlation images to obtain detected region hypotheses 
	 * @param _scaleIntervalSize size of the interval of scales that are used to compute wavelet correlation images
	 * @param _minRegionSize minimum size of a region to be chosen as detection
	 * @param _poisson2gauss flag if image with poisson noise is transformed to simulate gaussian noise
	 */
	public ParticleDetectorUWT2D(MTBImage img, int _Jmin, int _Jmax, 
			double _corrThreshold, int _scaleIntervalSize, int _minRegionSize, 
			boolean _poisson2gauss) throws ALDOperatorException {
		this.m_statusListeners = new Vector<StatusListener>(1);
		
		this.inputImage = img;
		this.Jmin = new Integer(_Jmin);
		this.Jmax = new Integer(_Jmax);
		this.corrThreshold = new Double(_corrThreshold);
		this.scaleIntervalSize = new Integer(_scaleIntervalSize);
		this.minRegionSize = _minRegionSize;
		this.poisson2gauss = _poisson2gauss;
		this.operatorExecStatus = OperatorExecutionStatus.OP_EXEC_INIT;
	}	
	
	/**
	 * Constructor that sets the poisson2gauss flag to <code>true</code>
	 * @param img input image with bright granules on dark background
	 * @param Jmin lowest scale index to be used for correlation image computations (scale is reciprocal to frequency properties)
	 * @param Jmax highest scale index to be used for correlation image computations (scale is reciprocal to frequency properties)
	 * @param corrThreshold threshold for wavelet correlation images to obtain detected region hypotheses 
	 * @param scaleIntervalSize size of the interval of scales that are used to compute wavelet correlation images
	 * @param minRegionSize minimum size of a region to be chosen as detection
	 */
	public ParticleDetectorUWT2D(MTBImage img, int _Jmin, int _Jmax, 
			double _corrThreshold, int _scaleIntervalSize, int _minRegionSize) 
				throws ALDOperatorException {
		this.m_statusListeners = new Vector<StatusListener>(1);
		
		this.inputImage = img;
		this.Jmin = new Integer(_Jmin);
		this.Jmax = new Integer(_Jmax);
		this.corrThreshold = new Double(_corrThreshold);
		this.scaleIntervalSize = new Integer(_scaleIntervalSize);
		this.minRegionSize = _minRegionSize;
		this.operatorExecStatus = OperatorExecutionStatus.OP_EXEC_INIT;
	}	
	
	@Override
	public void validateCustom() throws ALDOperatorException {
		
		if (   this.Jmin.intValue() <= 0 || this.Jmax.intValue() <= 0 
				|| this.Jmax.intValue() < this.Jmin.intValue())
			throw new ALDOperatorException(
				OperatorExceptionType.VALIDATION_FAILED, 
					"ParticleDetectorUWT2D.validateCustom(): " +
						"Jmin, Jmax must be larger 0. Jmax must be >= Jmin.");
		
		if (this.scaleIntervalSize.intValue() <= 0)
			throw new ALDOperatorException(
				OperatorExceptionType.VALIDATION_FAILED, 
					"ParticleDetectorUWT2D.validateCustom(): " +
						"Scale interval size must be larger 0.");
		
		if (  this.scaleIntervalSize.intValue() 
				> this.Jmax.intValue() - this.Jmin.intValue() + 1) {
			throw new ALDOperatorException(
				OperatorExceptionType.VALIDATION_FAILED, 
					"ParticleDetectorUWT2D.validateCustom(): " +
						"Scale interval size must be <= (Jmax - Jmin + 1).");
		}
		
//		if (this.autoThreshold == true) {
//			throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED, "ParticleDetectorUWT2D.validateCustom(): " +
//			"Automatic determination of threshold is defect and thus not supported at the moment.");
//		}
	}
	
	@Override
  public boolean supportsStepWiseExecution() {
		return false;
	}
	
	@Override
	protected void operate() throws ALDOperatorException,
			ALDProcessingDAGException {

		// update operator status
		this.operatorExecStatus = OperatorExecutionStatus.OP_EXEC_RUNNING;
		
		// post ImageJ status
		String msg = opIdentifier + "running particle detection...";	
		this.notifyListeners(new StatusEvent(msg));

		if (this.verbose.booleanValue())
			System.out.println(opIdentifier 
				+ "calculating correlation images...");
		
		// compute correlation images
		MTBImage[] corrImgs = this.getCorrelationImages();
		
		// fill result stack with correlation images
		if (this.additionalResultsWanted) {
			this.correlationImages = MTBImage.createMTBImage(
				this.inputImage.getSizeX(), this.inputImage.getSizeY(), 1, 1, 
				corrImgs.length, MTBImageType.MTB_DOUBLE);
			this.correlationImages.setTitle("Correlation image(s) for <" + 
				this.inputImage.getTitle() + ">");
			for (int i=0; i<corrImgs.length; ++i) {
				MTBImage tmpImg = corrImgs[i];
				String title = "Correlation image, id = " + i;
				tmpImg.setTitle(title);
				this.correlationImages.setImagePart(tmpImg, 0, 0, 0, 0, i);
				this.correlationImages.setSliceLabel(title, 0, 0, i);
			}
		}
		
		if (this.verbose.booleanValue())
			System.out.println(opIdentifier + "done.");

		// check if operator has been paused or interrupted
		if (this.operatorStatus == OperatorControlStatus.OP_STOP) {
			this.operatorExecStatus = 
				OperatorExecutionStatus.OP_EXEC_TERMINATED;
			if (this.verbose.booleanValue())
				System.err.println(opIdentifier + "stopped!");
			return;
 		}
		if (this.operatorStatus == OperatorControlStatus.OP_PAUSE) {
			System.err.println(opIdentifier+"paused, waiting to continue...");
			this.operatorExecStatus = OperatorExecutionStatus.OP_EXEC_PAUSED;
			// post ImageJ status
			msg = opIdentifier + "processing paused...";	
			this.notifyListeners(new StatusEvent(msg));
			do {
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					// just ignore the exception
				}
			} while (this.operatorStatus != OperatorControlStatus.OP_RESUME);
			this.operatorExecStatus = OperatorExecutionStatus.OP_EXEC_RUNNING;
			System.err.println(opIdentifier + "running again...");
		}
		
		if (this.verbose.booleanValue())
			System.out.print(opIdentifier + "performing thresholding...");

		Vector<MTBRegion2D> regs;
		MTBImage binImg;
		
		// number of correlation images
		int Ncorr = corrImgs.length;
		
		int[] cnt = new int[Ncorr];
		
		// binary connected components images
		MTBImage[] binImgs = new MTBImage[Ncorr];
		
		// histogram objects
		MTBImageHistogram[] hists = new MTBImageHistogram[Ncorr];
		int bins = 1000;
		
		// correlation threshold
		double thresh = this.getCorrelationThreshold().doubleValue();
		
		for (int i = 0; i < Ncorr; i++) {
			
			cnt[i] = 0;
			
			// threshold correlation images
			//if (this.getAutoThreshold()) 
			//	binImgs[i] = this.threshImageStableParticleCount(corrImgs[i], "particle counts in img "+i);
			//else
			binImgs[i] = this.threshImage(corrImgs[i], thresh);
			
			// fill regions in connected components
			//binImgs[i] = this.fillHoles(binImgs[i]);
		    
			// compute normalized cumulative histograms of correlation images
			hists[i] = 
				this.getNormalizedCumulativeHistogram(corrImgs[i], bins);
		}
		
		// fill result stack with binarized correlation images
		if (this.additionalResultsWanted) {
			this.binaryCorrelationImages = 
				(MTBImageByte)MTBImage.createMTBImage(
					this.inputImage.getSizeX(), this.inputImage.getSizeY(), 1, 1, 
						binImgs.length, MTBImageType.MTB_BYTE);
			this.binaryCorrelationImages.setTitle(
				"Binarized correlation image(s) for "
					+ "<" + this.inputImage.getTitle() + ">");
			for (int i=0; i<binImgs.length; ++i) {
				MTBImage tmpImg = binImgs[i];
				String title = "Binarized correlation image, id = " + i;
				tmpImg.setTitle(title);
				this.binaryCorrelationImages.setImagePart(tmpImg, 0, 0, 0, 0, i);
				this.binaryCorrelationImages.setSliceLabel(title, 0, 0, i);
			}
		}

		if (this.verbose.booleanValue())
			System.out.println("done.");

		// check if operator has been paused or interrupted
		if (this.operatorStatus == OperatorControlStatus.OP_STOP) {
			this.operatorExecStatus = 
				OperatorExecutionStatus.OP_EXEC_TERMINATED;
			if (this.verbose.booleanValue())
				System.err.println(opIdentifier + "stopped!");
			return;
 		}
		if (this.operatorStatus == OperatorControlStatus.OP_PAUSE) {
			System.err.println(opIdentifier+"paused, waiting to continue...");
			this.operatorExecStatus = OperatorExecutionStatus.OP_EXEC_PAUSED;
			// post ImageJ status
			msg = opIdentifier + "processing paused...";	
			this.notifyListeners(new StatusEvent(msg));
			do {
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					// just ignore the exception
				}
			} while (this.operatorStatus != OperatorControlStatus.OP_RESUME);
			this.operatorExecStatus = OperatorExecutionStatus.OP_EXEC_RUNNING;
			System.err.println(opIdentifier + "running again...");
		}

		if (this.verbose.booleanValue())
			System.out.println(opIdentifier + "finding meaningful regions...");

		// build a tree of regions
		MTBTree regTree = this.getRegionsTree(binImgs);
		
		// get the most meaningful regions out of the tree
		Vector<MTBTreeNode> children = regTree.getRoot().getChilds();
		regs = new Vector<MTBRegion2D>(children.size());
		Vector<MTBTreeNode> mfulNodes;
		MTBTreeNode tn;
		
		for (int j = 0; j < children.size(); j++) {
			mfulNodes = 
				this.meaningfulNodes(children.get(j), corrImgs, hists, 1);
			
			for (int k = 0; k < mfulNodes.size(); k++) {
				tn = mfulNodes.get(k);
				cnt[((MTBTreeNodeRegion2D)tn.getData()).getLevel()]++;
				regs.add(((MTBTreeNodeRegion2D)tn.getData()).getRegion());
			}
		}
		
		int total = 0;
		for (int i = 0; i < Ncorr; i++) {
			if (this.verbose.booleanValue())
				System.out.println(opIdentifier + 
					"\t -> Number of regions from interval " + i + ": " + cnt[i]);
			total += cnt[i];
		}
		
		if (this.verbose.booleanValue())
			System.out.println(opIdentifier 
				+ "\t -> Number of all detections: " + total);
		
		binImg = this.regionsToBinImage(binImgs[0], null, regs);
		
		if (this.verbose.booleanValue())
			System.out.print(opIdentifier + "Filling holes...");
		binImg = this.fillHoles(binImg);
		if (this.verbose.booleanValue())
			System.out.println("done.");
	
		LabelComponentsSequential lcs = 
			new LabelComponentsSequential(binImg, true);
		lcs.runOp(true);
		
		MTBRegion2DSet regionSet = lcs.getResultingRegions();
		
		if (this.verbose.booleanValue())
			System.out.print(opIdentifier + "Deleting unwanted border " 
				+ "detections and detections of insufficient size...");
		binImg = this.processedRegionsToBinImage(binImg, regionSet, 
			this.getMinRegionSize());
		if (this.verbose.booleanValue())
			System.out.println("done.");
		
		// mask particles in excluded regions, if mask is available
		if (this.excludeMask != null) {
			for (int y=0; y<this.excludeMask.getSizeY(); ++y) {
				for (int x=0; x<this.excludeMask.getSizeX(); ++x) {
					if (this.excludeMask.getValueInt(x, y) > 0) {
						binImg.putValueInt(x, y, 0);
					}
				}
			}
		}
		
		lcs = new LabelComponentsSequential(binImg, true);
		lcs.runOp(true);
		
		regionSet = lcs.getResultingRegions();
		
		if (this.verbose.booleanValue())
			System.out.println(opIdentifier 
				+ "\t -> Number of detected regions: " + regionSet.size());
		
		this.setResults(regionSet);

		// plot regions to overlay image
		MTBImageRGB targetImg = (MTBImageRGB)MTBImage.createMTBImage(
				this.inputImage.getSizeX(), this.inputImage.getSizeY(), 
				this.inputImage.getSizeZ(), 1, 1, MTBImageType.MTB_RGB);
		for (int y=0; y<this.inputImage.getSizeY(); ++y) {
			for (int x=0; x<this.inputImage.getSizeX(); ++x) {
				targetImg.putValueR(x, y, this.inputImage.getValueInt(x, y));
				targetImg.putValueG(x, y, this.inputImage.getValueInt(x, y));
				targetImg.putValueB(x, y, this.inputImage.getValueInt(x, y));
			}
		}
		DrawRegion2DSet drawOp = new DrawRegion2DSet();
		drawOp.setInputRegions(regionSet);
		drawOp.setTargetImage(targetImg);
		drawOp.setCloneTargetImage(true);
		drawOp.setDrawType(DrawType.CONTOURS);
		drawOp.setColor(Color.orange);
		drawOp.runOp();
		this.resultOverlay = drawOp.getResultImage();
		
		// plot regions to binary mask image
		this.resultMaskBinary = (MTBImageByte)MTBImage.createMTBImage(
				this.inputImage.getSizeX(), this.inputImage.getSizeY(), 
				this.inputImage.getSizeZ(), 1, 1, MTBImageType.MTB_BYTE);
		this.resultMaskBinary.fillWhite();
		for (MTBRegion2D reg: this.resultingRegions) {
			for (Point2D.Double p: reg.getPoints()) {
				this.resultMaskBinary.putValueInt((int)p.x, (int)p.y, 0);
			}
		}
		
	
		if (this.verbose.booleanValue())
			System.out.println(opIdentifier + "Operations finished!");
	}
	
	
	/**
	 * Get the most meaningful regions from a (sub)tree. The tree of regions is assumed to contain different detections from different scales in the same image location.
	 * Detections are the regions which "explain" best this image part, the most meaningful regions.
	 * @param treeNode root of the (sub) tree
	 * @param corrImgs correlation images
	 * @param hists cumulative histograms
	 * @param mode 0: weighted mean, 1: unweighted mean, 2: min, 3 or else: max
	 * @return
	 */
	protected Vector<MTBTreeNode> meaningfulNodes(MTBTreeNode treeNode, MTBImage[] corrImgs, MTBImageHistogram[] hists, int mode) {
		// This method recursively traverses the (sub)tree and compares each node with its children, collecting the most meaningful nodes
		
		Vector<MTBTreeNode> children = treeNode.getChilds();
		Vector<MTBTreeNode> mfulNodes = new Vector<MTBTreeNode>(1);
		
		if (children.isEmpty()) {
			// node has no children, return this node
			
			mfulNodes.add(treeNode);
			return mfulNodes;
		}
		
		MTBRegion2D reg = ((MTBTreeNodeRegion2D)treeNode.getData()).getRegion();
		int lvl = ((MTBTreeNodeRegion2D)treeNode.getData()).getLevel();	
		
		for (int i = 0; i < children.size(); i++) {
			
			// recursively get only the most meaningful nodes starting with the children as root
			mfulNodes.addAll(meaningfulNodes(children.get(i), corrImgs, hists, mode));
		}
		
		// compute the region's normalized occurrence probability 
		double p = this.logPofRegion(reg, corrImgs[lvl], hists[lvl])/reg.getArea();
		
		
		// compute the meaningful children's normalized occurrence probability
		// there are different modes...
		MTBRegion2D regc = null;
		int lvlc;
		double pc = 0.0;
		double ac = 0.0;
		for (int i = 0; i < mfulNodes.size(); i++) {
			regc = ((MTBTreeNodeRegion2D)mfulNodes.get(i).getData()).getRegion();
			lvlc = ((MTBTreeNodeRegion2D)mfulNodes.get(i).getData()).getLevel();
			
			if (mode == 0) {
				// weighted mean mode
				
				pc += this.logPofRegion(regc, corrImgs[lvlc], hists[lvlc]);
				ac += regc.getArea();
			}
			else if (mode == 1) {
				// unweighted mean mode
				
				pc += this.logPofRegion(regc, corrImgs[lvlc], hists[lvlc])/regc.getArea();
				ac++;
			}
			else {
				if (i == 0) {
					pc = this.logPofRegion(regc, corrImgs[lvlc], hists[lvlc])/regc.getArea();
				}
				else {
					if (mode == 2) {
						// min mode
						
						ac = this.logPofRegion(regc, corrImgs[lvlc], hists[lvlc])/regc.getArea();
						if (ac < pc) {
							pc = ac;
						}
					}
					else {
						// max mode
						
						ac = this.logPofRegion(regc, corrImgs[lvlc], hists[lvlc])/regc.getArea();
						if (ac > pc) {
							pc = ac;
						}
					}
				}
				
			}
		}
		
		if (mode == 0 || mode == 1)
			pc /= ac;

		if (p < pc) {
			// if the probality of the actual region is smaller than the prob of its meaningful children
			// return only this region
			mfulNodes.clear();
			mfulNodes.add(treeNode);
		}
		
		return mfulNodes;
	}
	
	/**
	 * Build a tree of regions from binarized images. The binary images represent regions from different scales, fine scales have low indices in the array
	 * @param binaryImages
	 * @return
	 * @throws ALDOperatorException 
	 * @throws ALDProcessingDAGException 
	 */
	protected MTBTree getRegionsTree(MTBImage[] binaryImages) throws ALDOperatorException, ALDProcessingDAGException {
		
		int N = binaryImages.length;
		
		MTBTree regTree = null;

		Vector<MTBRegion2DSet> regs = new Vector<MTBRegion2DSet>(N);
		MTBImage[] labelImgs = new MTBImage[N];
		
		LabelComponentsSequential lcs;
		
		for (int i = 0; i < N; i++) {

			lcs = new LabelComponentsSequential(binaryImages[i], true);
			lcs.runOp(true);
		    regs.add(lcs.getResultingRegions());
		    
		    labelImgs[i] = this.labelImage(binaryImages[i], regs.get(i));
		}
		

		MTBTreeNodeRegion2D rootNodeData = new MTBTreeNodeRegion2D(null);
		rootNodeData.setLevel(-1);
		
		regTree = new MTBTree(rootNodeData);
		MTBTreeNode rootNode = regTree.getRoot();
		
		
		MTBRegion2D reg = null;
		MTBRegion2DSet rvec = null;
		
		Vector<Vector<MTBTreeNode>> vvtn = new Vector<Vector<MTBTreeNode>>(regs.size());
		Vector<MTBTreeNode> vtn = null;
		MTBTreeNode tn = null;
		MTBTreeNodeRegion2D tndata = null;
		
		// create tree node data vectors from the region vectors
		for (int i = 0; i < N; i++) {
			
			rvec = regs.get(i);
			vtn = new Vector<MTBTreeNode>(rvec.size());
			
			for (int j = 0; j < rvec.size(); j++) {
				tndata = new MTBTreeNodeRegion2D(rvec.get(j));
				tndata.setLevel(i);
				tn = new MTBTreeNode(tndata);
				vtn.add(tn);
			}

			rvec.clear();
			vvtn.add(vtn);
		}	
	
		// build the tree from bottom to top (leafs to root)
		Integer label;	
		for (int i = 0; i < N; i++) {
			
			vtn = vvtn.get(i);

			for (int j = 0; j < vtn.size(); j++) {
				tn = vtn.get(j);					
				reg = ((MTBTreeNodeRegion2D)tn.getData()).getRegion();
							
				if (i < N-1) {
					
					label = null;
					for (int k = i+1; k < N && (label == null); k++) {
						label = this.getLabelMostInRegion(reg, labelImgs[k]);
					
						if (label != null) {
							tn.setParent(vvtn.get(k).get(label.intValue()-1));
						}
					}
					
					if (tn.getParent() == null) {
						tn.setParent(rootNode);
					}
				}
				else {
					tn.setParent(rootNode);
				}	
			}
		}		
		
		return regTree;
	}
	
	
	/**
	 * Compute the correlation images as specified by the parameters.
	 * <p>
	 * @return Array of correlation images starting with lowest scale 
	 * 	interval which corresponds to the highest frequency interval.
	 * @throws ALDOperatorException 
	 * @throws ALDProcessingDAGException 
	 */
	protected MTBImage[] getCorrelationImages() 
			throws ALDOperatorException, ALDProcessingDAGException {
		
		MTBImage[] corrImgs = null;
		
		MTBImage img = this.inputImage;
		
		if (this.poisson2gauss) {
			if (this.verbose.booleanValue())
				System.out.print(opIdentifier 
					+ "performing Poisson-to-Gauss noise transformation...");
			img = poisson2gauss(img);
			if (this.verbose.booleanValue())
				System.out.println("done."); 
		}

		if (this.verbose.booleanValue())
			System.out.println(opIdentifier + "Running UWT transformation...");
		
		// DWT configuration
		UndecimatedWaveletTransform uwt = 
			         new UndecimatedWaveletTransform(img, this.getJmax().intValue(), true, ! initialSigmaOneHalve);
		uwt.setVerbose(this.verbose);
		uwt.setExcludeMask( this.excludeMask);

		for (int i = 0; i < this.m_statusListeners.size(); i++) {
			uwt.addStatusListener(this.m_statusListeners.get(i));
		}
		
		// compute DWT
		new Thread(new UWTRunner(uwt)).start();
		
		// wait for thread to complete...
		do {
			try {
        Thread.sleep(500);
        if (this.operatorStatus == OperatorControlStatus.OP_STOP)
        	uwt.handleALDControlEvent(
        		new ALDControlEvent(this, ALDControlEventType.STOP_EVENT));
    		if (this.operatorStatus == OperatorControlStatus.OP_PAUSE) {
    			System.err.println(opIdentifier 
    				+ "paused, waiting to continue...");
    			this.operatorExecStatus = 
    				OperatorExecutionStatus.OP_EXEC_PAUSED;
        	uwt.handleALDControlEvent(
         		new ALDControlEvent(this, ALDControlEventType.PAUSE_EVENT));
    			do {
    				try {
    					Thread.sleep(500);
    					// post ImageJ status
    					String msg = opIdentifier + "processing paused...";	
    					this.notifyListeners(new StatusEvent(msg));
    				} catch (InterruptedException e) {
    					// just ignore the exception
    				}
    			} while (   this.operatorStatus 
    					     != OperatorControlStatus.OP_RESUME);
    			this.operatorExecStatus = 
    				OperatorExecutionStatus.OP_EXEC_RUNNING;
        	uwt.handleALDControlEvent(
         		new ALDControlEvent(this, ALDControlEventType.RESUME_EVENT));
    			System.err.println(opIdentifier + "running again...");
    		}

      } catch (InterruptedException e) {
      	// just ignore the exception
      }
		} while (   uwt.getExecutionStatus() 
						 != OperatorExecutionStatus.OP_EXEC_TERMINATED);

		if (this.verbose.booleanValue())
			System.out.println(opIdentifier + "done.");

		// check if operator has been paused or interrupted
		if (this.operatorStatus == OperatorControlStatus.OP_STOP) {
			this.operatorExecStatus = 
				OperatorExecutionStatus.OP_EXEC_TERMINATED;
			if (this.verbose.booleanValue())
				System.err.println(opIdentifier + "stopped!");
			return corrImgs;
 		}
		if (this.operatorStatus == OperatorControlStatus.OP_PAUSE) {
			System.err.println(opIdentifier+"paused, waiting to continue...");
			this.operatorExecStatus = OperatorExecutionStatus.OP_EXEC_PAUSED;
			do {
				try {
					Thread.sleep(500);
					// post ImageJ status
					String msg = opIdentifier + "processing paused...";	
					this.notifyListeners(new StatusEvent(msg));
				} catch (InterruptedException e) {
					// just ignore the exception
				}
			} while (this.operatorStatus != OperatorControlStatus.OP_RESUME);
			this.operatorExecStatus = OperatorExecutionStatus.OP_EXEC_RUNNING;
			System.err.println(opIdentifier + "running again...");
		}

		if (this.verbose.booleanValue())
			System.out.print(opIdentifier + "Calculating images...");

		MTBImage[] dwtimgs = uwt.getUWT().toArray();
//		int Jmin = this.getJmin();
//		int Jmax = this.getJmax();
		int corrIntSize = this.getScaleIntervalSize().intValue();
			
		// number of correlation images 
		int numOfCorrImgs = 
			this.Jmax.intValue() - this.Jmin.intValue() + 1 - corrIntSize + 1;
		
		// correlation images array
		corrImgs = new MTBImage[numOfCorrImgs];
		
		// delete negative values
		ImgThresh it;
		for (int j = 0; j <= this.Jmax.intValue(); j++) {
			it = new ImgThresh(dwtimgs[j], 0.0,
						java.lang.Double.POSITIVE_INFINITY, 0.0);
			it.runOp(false);
			dwtimgs[j] = it.getResultImage();
		}

		
		MTBImageArithmetics mia = new MTBImageArithmetics();
		
		// compute correlation images
		for (int i = 0; i < numOfCorrImgs; i++) {
			// multiply the DWT images of the given scale interval
			
			corrImgs[i] = dwtimgs[this.Jmin.intValue() + i].duplicate();
			
			for (int j = 1; j < corrIntSize; j++) {
				corrImgs[i] = 
					mia.mult(corrImgs[i], dwtimgs[this.Jmin.intValue() + i + j]); 
			}
		}		
		
		if (this.verbose.booleanValue())
			System.out.println("done!");		
		return corrImgs;
	}
	
	/**
	 * Transforms image with poisson noise to image with gaussian noise
	 * J.-L. Starck et al., Multiresolution Support Applied to Image Filtering and Restoration
	 */
	protected static MTBImage poisson2gauss(MTBImage img) throws ALDOperatorException {
		MTBImage imgT = img.convertType(MTBImageType.MTB_DOUBLE, false);

		MTBImageArithmetics mia = new MTBImageArithmetics();
		
		imgT = mia.add(imgT, 3.0/8.0);
		imgT = mia.pow(imgT, 0.5);
		imgT = mia.mult(imgT, 2.0);

		return imgT;
	}
	
	/**
	 * Transforms image with gaussian noise to image with poisson noise
	 * J.-L. Starck et al., Multiresolution Support Applied to Image Filtering and Restoration
	 */
	protected static MTBImage gauss2poisson(MTBImage img) throws ALDOperatorException {
		MTBImage imgT = img.convertType(MTBImageType.MTB_DOUBLE, false);

		MTBImageArithmetics mia = new MTBImageArithmetics();
		
		imgT = mia.mult(imgT, 0.5);
		imgT = mia.pow(imgT, 2.0);
		imgT = mia.add(imgT, -3.0/8.0);
		
		return imgT;
	}
	
	
	
	/**
	 * Create a normalized cumulative histogram from an image
	 * @param img input image
	 * @param bins number of histogram bins
	 * @return normalized cumulative histogram
	 */
	protected MTBImageHistogram getNormalizedCumulativeHistogram(MTBImage img, int bins) {
		
		double[] minmax = img.getMinMaxDouble();
		
		// compute histogram:  we need P(X >= x)
		MTBImageHistogram hist = new MTBImageHistogram(img, bins, minmax[0], minmax[1]);
		
		// -> P(X <= x)
		hist.cumulateOnly();
		hist.normalize();
		
		// -> P(X >= x)
		double ppred = 0.0;
		double p;
		for (int i = 0; i < bins; i++) {
			p = hist.getBinValue(i);
			hist.setBinValue(i, 1.0 - ppred);
			ppred = p;
		}
		
		return hist;
	}
	

//	protected MTBImage[] computeCorrelationImgs(MTBImage[] dwt, int Jmin, int Jmax) {
//		int Ncorr = Jmax - Jmin;
//
//		MTBImage[] corrImgs = new MTBImage[Ncorr];
//		
//		for (int i = 0; i < Ncorr; i++) {
//			corrImgs[i] = m_img.convertType(MTBImage.MTB_DOUBLE, true);
//		
//			for (int j = 0; j < m_sizeStack; j++) {
//				corrImgs[i].setActualSliceIndex(j);
//
//				for (int y = 0; y < m_sizeY; y++) {
//					for (int x = 0; x < m_sizeX; x++) {
//						corrImgs[i].putValueDouble(x, y, 1.0);
//					}
//				}
//			}
//			corrImgs[i].setActualSliceIndex(0);
//		}
//		
//		MTBImageArithmetics mia = new MTBImageArithmetics();
//
//		for (int i = 0; i < Ncorr; i++) {
//			
//			corrImgs[i] = mia.mult(corrImgs[i], dwt[Jmin + i]);
//			corrImgs[i] = mia.mult(corrImgs[i], dwt[Jmin + i + 1]);
//
//			corrImgs[i].setTitle("corr" + (Jmin+i) + "" + (Jmin+i+1));
//		}	
//		
//		return corrImgs;
//	}
	
	
	protected MTBImage fillHoles(MTBImage img) throws IllegalArgumentException, ALDOperatorException, ALDProcessingDAGException {
		FillHoles2D fh = new FillHoles2D(img);
		for (int i = 0; i < this.m_statusListeners.size(); i++) {
			fh.addStatusListener(this.m_statusListeners.get(i));
		}
		
		fh.runOp(false);
			
		MTBImage fhImg = fh.getResultImage();
		
		fhImg.setTitle(img.getTitle() + " FilledHoles");
		return fhImg;
	}
	
	protected MTBImage threshImgNiblack(MTBImage img, double k, int winsize) throws ALDOperatorException, ALDProcessingDAGException {

	//	this.fireStatusEvent(1, 1, "Niblack thresholding...");
		
		ImgThreshNiblack itn = new ImgThreshNiblack(img, ImgThreshNiblack.Mode.STD_LOCVARCHECK, k, -1, winsize, 7, 350, null);
		
		itn.runOp(false);
		return itn.getResultImage();

	}
	
	protected MTBImage getMaskFromNuclei(MTBImage nucleiImg) throws ALDOperatorException, ALDProcessingDAGException {
		MTBImage maskImg;
		
		CalcGlobalThreshOtsu otsuCalc = new CalcGlobalThreshOtsu(nucleiImg);
		
		otsuCalc.runOp(false);
		double thresh = otsuCalc.getOtsuThreshold().getValue().doubleValue();
			
		maskImg = MTBImage.createMTBImage(nucleiImg.getSizeX(),
											nucleiImg.getSizeY(),
											nucleiImg.getSizeZ(),
											nucleiImg.getSizeT(),
											nucleiImg.getSizeC(),
											MTBImageType.MTB_BYTE);
		
		// threshold
		ImgThresh it = new ImgThresh(nucleiImg, thresh, 0, 255);
		it.setDestinationImage(maskImg);
		
		it.runOp(false);
		
		// dilate
		ImgDilate id = new ImgDilate(maskImg, 9);			
		id.runOp(false);
				
		maskImg = id.getResultImage();
				
		// erode
		ImgErode ie = new ImgErode(maskImg, 9);
		ie.runOp(false);
		
		maskImg = ie.getResultImage();			
		
		// dilate
		id = new ImgDilate(maskImg, 7);			
		id.runOp(false);
				
		maskImg = id.getResultImage();
		
		return maskImg;
	}
	
	protected Integer getLabelMostInRegion(MTBRegion2D reg, MTBImage img) {
		Vector<Integer> labels = new Vector<Integer>();
		Vector<Integer> numOfLabels = new Vector<Integer>();
		
		Vector<Point2D.Double> pts = reg.getPoints();
		Point2D pt;
		Integer label;
		int idx;
		
		for (int j = 0; j < pts.size(); j++) {
			pt = pts.get(j);
			label = 
				new Integer(img.getValueInt((int)pt.getX(), (int)pt.getY()));
			
			if (label.intValue() > 0) {
				if (!labels.contains(label)) {
					labels.add(label);
					numOfLabels.add(new Integer(1));
				}
				else {
					idx = labels.indexOf(label);
					numOfLabels.set(idx, 
						new Integer(labels.get(idx).intValue() + 1));
				}
			}
		}	
		
		if (labels.isEmpty()) {
			return null;
		}
		int minidx = -1;
		int min = Integer.MAX_VALUE;
		for (int i = 0; i < numOfLabels.size(); i++) {
			if (numOfLabels.get(i).intValue() < min) {
				min = numOfLabels.get(i).intValue();
				minidx = i;
			}
		}
		
//		return new Integer(labels.get(minidx))	;
		return labels.get(minidx);
	}
	
	protected Vector<Integer> getLabelsInRegion(MTBRegion2D reg, MTBImage img) {
		Vector<Integer> labels = new Vector<Integer>();
		
		Vector<Point2D.Double> pts = reg.getPoints();
		Point2D pt;
		int label;
		
		for (int j = 0; j < pts.size(); j++) {
			pt = pts.get(j);
			label = img.getValueInt((int)pt.getX(), (int)pt.getY());
			
			//if (label > 0) {
				if (!labels.contains(new Integer(label))) {
					labels.add(new Integer(label));
				}
			//}
		}	
		
		return labels;
	}
	
	protected double meanOfRegion(MTBRegion2D reg, MTBImage img) {
		double mean = 0.0;
		Vector<Point2D.Double> pts = reg.getPoints();
		Point2D pt;
		
		for (int j = 0; j < pts.size(); j++) {
			pt = pts.get(j);
			mean += img.getValueDouble((int)pt.getX(), (int)pt.getY());
		}	
		mean /= pts.size();
		
		return mean;
	}
	
	protected double logPofRegion(MTBRegion2D reg, MTBImage img, MTBImageHistogram cumHist) {
		double p = 0.0;
		//double pmin = -1.0;
		Vector<Point2D.Double> pts = reg.getPoints();
		Point2D pt;
		
		for (int j = 0; j < pts.size(); j++) {
			pt = pts.get(j);
			
		//	if (pmin == -1)
			p += Math.log(cumHist.getBinValue(cumHist.getBinIndex(img.getValueDouble((int)pt.getX(), (int)pt.getY()))));
		//		pmin = Math.log(cumHist.getBinValue(cumHist.getBinIndex(img.getValueDouble((int)pt.getX(), (int)pt.getY()))));
		//	else {
		//		p = Math.log(cumHist.getBinValue(cumHist.getBinIndex(img.getValueDouble((int)pt.getX(), (int)pt.getY()))));
		//		if (p < pmin)
		//			pmin = p;
		//	}
		}	
		
		return p;
	}
	
	protected MTBImage threshImage(MTBImage img, double thresh) throws ALDOperatorException, ALDProcessingDAGException {
		
		MTBImage byteImg = MTBImage.createMTBImage(img.getSizeX(),
													img.getSizeY(),
													img.getSizeZ(),
													img.getSizeT(),
													img.getSizeC(),
													MTBImageType.MTB_BYTE);
		
		ImgThresh it = new ImgThresh(img, thresh, 255, 0);
		it.setDestinationImage(byteImg);
		
		it.runOp(false);
		
		byteImg.setTitle(img.getTitle() + " Thresh" + thresh);
		return byteImg;
	}
	
//	protected MTBImage threshImageStableParticleCount(MTBImage img, String gnuplottitle) throws ALDOperatorException, ALDProcessingDAGException {
//		double thresh;
//		double eps = 0.1;
//		int steps = 100;
//		int[] pcount = new int[steps];
//		double[] threshs = new double[steps];
//		double[] minmax = img.getMinMaxDouble();
//		minmax[1] = 20;
//		System.out.println(minmax[0] + "  " + minmax[1]);
//		
//		MTBImage byteImg= null;
//		LabelComponentsSequential lcs = null;
//		ImgThresh it = null;
//		
//		for (int i = 0; i < steps; i++) {
//			
//			this.notifyListeners(new StatusEvent(i, steps, "Computing correlation threshold..."));
//			
//			byteImg = MTBImage.createMTBImage(img.getSizeX(),
//					img.getSizeY(),
//					img.getSizeZ(),
//					img.getSizeT(),
//					img.getSizeC(),
//					MTBImageType.MTB_BYTE);
//			
//			threshs[i] = (i+1)/(double)(steps+1)*(minmax[1] - minmax[0]) + minmax[0];
//			it = new ImgThresh(img, threshs[i],
//					255, 0);
//			it.setDestinationImage(byteImg);
//			it.runOp(false);
//			
//			lcs = new LabelComponentsSequential(byteImg, true);
//			lcs.runOp(false);
//			
//			pcount[i] = lcs.getResultingRegions().size();
//		}
//
//		this.notifyListeners(new StatusEvent(steps, steps, "Computing correlation threshold DONE"));
//		
//		thresh = 0.7;
//		double d = java.lang.Double.MAX_VALUE;
//		for (int i = 1; i < steps; i++) {
//			if (Math.abs(Math.abs(pcount[i] - pcount[i-1]) - d) < eps ) {
//				thresh = threshs[i];
//				break;
//			}
//			else d = Math.abs(pcount[i] - pcount[i-1]);
//		}
//		
//		double[] ab = new double[2];
//		int idx = this.idxByExp(pcount, threshs, ab, 2);
//		
//		thresh = threshs[idx];
//		System.out.println(thresh);
//		
//		
//		File f = new File("pcount.dat");
//		BufferedWriter cwriter = null;
//		try {
//			cwriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f)));
//		} catch (FileNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		
//		try {
//			double thresh2;
//			for (int i = 0; i < pcount.length; i++) {
//				thresh2 = threshs[i];
//				cwriter.write(i + " " + thresh2 + " " + pcount[i] + "\n");
//			}
//
//			cwriter.flush();
//
//			cwriter.close();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	
//		GnuPlot gp = null;
//		try {
//			gp = new GnuPlot();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		gp.cmd("set title '"+gnuplottitle+"'");
//		gp.cmd("set ylabel 'number of particles'");
//		gp.cmd("set xlabel 'threshold'");
//	//	gp.cmd("set xrange [" +(1.0/rotsteps * rot_maxangle)+ ":" + rot_maxangle + "]");
//		gp.cmd("plot 'pcount.dat' using 2:3 title \"pcount\" with points");
//	
//		
//		byteImg = MTBImage.createMTBImage(img.getSizeX(),
//													img.getSizeY(),
//													img.getSizeZ(),
//													img.getSizeT(),
//													img.getSizeC(),
//													MTBImageType.MTB_BYTE);
//		
//		//thresh = 0.7;
//		it = new ImgThresh(img, thresh, 255, 0);
//		it.setDestinationImage(byteImg);
//		
//		it.runOp(false);
//		
//		byteImg.setTitle(img.getTitle() + " Thresh" + thresh);
//		return byteImg;
//		
//	}
//	
//	protected int idxByExp(int[] pcount, double[] threshs, double[] ab, double maxareadev) {
//		int idx = -1;
//		
//		double b = Math.log(pcount[0]/(double)pcount[1])/(threshs[1] - threshs[0]);
//		double a = pcount[0]*Math.exp(b*threshs[0]);
//		ab[0] = a; ab[1] = b;
//		
//		double sumexp = 1;
//		double sumtest = 1;
//		
//		File f = new File("area.dat");
//		BufferedWriter cwriter = null;
//		try {
//			cwriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f)));
//		} catch (FileNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		
//		try {
//			
//			for (int i = 0; i < pcount.length; i++) {
//				sumexp += Math.exp(-threshs[i]);
//				sumtest += pcount[i]/a;
//				
//				cwriter.write(i + " " + threshs[i] + " " + sumexp + " " + sumtest + "\n");
//			//	System.out.println(i + " " + threshs[i] + " " + sumexp + " " + sumtest);
//				if (idx  == -1 && Math.abs(sumexp - sumtest) > maxareadev) {
//					idx = i;
//				}
//			}
//	
//			cwriter.flush();
//	
//			cwriter.close();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//			
//		
//		GnuPlot gp = null;
//		try {
//			gp = new GnuPlot();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		gp.cmd("set title 'area'");
//		gp.cmd("set ylabel 'number of particles'");
//		gp.cmd("set xlabel 'threshold'");
//	//	gp.cmd("set xrange [" +(1.0/rotsteps * rot_maxangle)+ ":" + rot_maxangle + "]");
//		gp.cmd("plot 'area.dat' using 2:3 title \"exp\" with points," +
//				"'area.dat' using 2:4 title \"test\" with points");
//		
//		
//		return idx;
//	}
	
	
	protected MTBImage labelImage(MTBImage img, MTBRegion2DSet regs) {
		MTBImage labelImg = MTBImage.createMTBImage(img.getSizeX(),
													img.getSizeY(),
													img.getSizeZ(),
													img.getSizeT(),
													img.getSizeC(),
													MTBImageType.MTB_INT);
		
		Vector<Point2D.Double> pts;
		Point2D pt;
		
		for (int i = 0; i < regs.size(); i++) {
			pts = regs.get(i).getPoints();
			regs.get(i).setID(i+1);
			
			for (int j = 0; j < pts.size(); j++) {
				pt = pts.get(j);
				labelImg.putValueInt((int)pt.getX(), (int)pt.getY(), i+1);
			}	
		}
		
		return labelImg;
	}
	
	/**
	 * Create binary image from a set of regions. If mask != null, pixels where mask is 0 are not considered.
	 * @param img only used to determine the result image size
	 * @param mask
	 * @param regs
	 * @return
	 */
	protected MTBImage regionsToBinImage(MTBImage img, MTBImage mask, Vector<MTBRegion2D> regs) {
		MTBImage binImg = MTBImage.createMTBImage(img.getSizeX(), 
				img.getSizeY(), 
				img.getSizeZ(), 
				img.getSizeT(), 
				img.getSizeC(), 
				MTBImageType.MTB_BYTE);
		
		Vector<Point2D.Double> pts;
		Point2D pt;
		
		for (int i = 0; i < regs.size(); i++) {
			pts = regs.get(i).getPoints();
			
			for (int j = 0; j < pts.size(); j++) {
				pt = pts.get(j);
				
				if (mask == null || mask.getValueInt((int)pt.getX(), (int)pt.getY()) > 0) {
					binImg.putValueInt((int)pt.getX(), (int)pt.getY(), 255);
				}

			}
		}
		
		return binImg;
	}
	
	
	/**
	 * Create a binary image from given regions.
	 * <p>
	 * Regions smaller than <code>_minRegionSize<code> are rejected.
	 * @param img		Input image.
	 * @param regs	Region set.
	 * @param _minRegionSize	Minimal size of regions considered.
	 * @return	Binary image with regions.
	 */
	protected MTBImage processedRegionsToBinImage(MTBImage img, 
			MTBRegion2DSet regs, int _minRegionSize) {
		MTBImage binImg = MTBImage.createMTBImage(img.getSizeX(), 
													img.getSizeY(), 
													img.getSizeZ(), 
													img.getSizeT(), 
													img.getSizeC(), 
													MTBImageType.MTB_BYTE);
		
		Vector<Point2D.Double> pts;
		Point2D pt;
		int bCnt;
		int iCnt;
		
		for (int i = 0; i < regs.size(); i++) {
			
			if (regs.get(i).getArea() >= _minRegionSize) {
			
				pts = regs.get(i).getPoints();	
				
				bCnt = 0;
				iCnt = 0;
				// test if region consists only of border pixels
				for (int j = 0; j < pts.size(); j++) {
					pt = pts.get(j);
					
					
					if ((int)pt.getX() > 0 && (int)pt.getX() < img.getSizeX()-1
					    && (int)pt.getY() > 0 && (int)pt.getY() < img.getSizeY()-1) {
						
						iCnt++;
					}
					else {
						bCnt++;
					}
				}
				
				if (bCnt == 0 || (double)iCnt/(double)bCnt >= 0.5) {
					for (int j = 0; j < pts.size(); j++) {
						pt = pts.get(j);
						
						binImg.putValueInt((int)pt.getX(), (int)pt.getY(), 255);
					}
				}
			}
		}
		
		return binImg;
	}
	
	
	protected void threshDWTJeffreys(MTBImage[] dwt, double scaleOneSigma, double[] sigmaScales) {
		for (int j = 1; j < dwt.length; j++) {
			
			this.threshDWTCoeffs(dwt[j], scaleOneSigma*sigmaScales[j-1]);
		}
	}
	
	protected MTBImage inverseATrousDWT(MTBImage[] dwt) throws ALDOperatorException {
		MTBImage invDWT = dwt[0].duplicate();

		MTBImageArithmetics mia = new MTBImageArithmetics();
		
		for (int j = 1; j < dwt.length; j++) {
			
			invDWT = mia.add(invDWT, dwt[j]);
		}
		
		return invDWT;
	}
	
	protected void threshDWTCoeffs(MTBImage img, double sigma) {
		int sizeX = img.getSizeX();
		int sizeY = img.getSizeY();
		double s2 = 3.0*sigma*sigma;
		double val, val2;

			
		for (int y = 0; y < sizeY; y++) {
			for (int x = 0; x < sizeX; x++) {
				val = img.getValueDouble(x, y);
				
			//	if (val > 0.0) {
					val2 = (val*val - s2);
					if (val2 < 0.0)
						val2 = 0.0;
					
					if (val > 0.0) 
						img.putValueDouble(x, y, val2/val);
					else
						img.putValueDouble(x, y, 0.0);
					
			//		if (val >= val2/val) {
			//			img.putValueDouble(x, y, val2);
			//		}
			//		else {
			//			img.putValueDouble(x, y, 0.0);
			//		}
			//	}
			//	else {
			//		img.putValueDouble(x, y, 0.0);
			//	}
			}
		}
	}
	
//	protected void threshMAD(MTBImage img) {
//		int sizeX = img.getSizeX();
//		int sizeY = img.getSizeY();
//		double[] vals = new double[img.getSizeX()*img.getSizeY()*img.getSizeZ()*img.getSizeT()*img.getSizeC()];
//		int cnt = 0;
//		
//		for (int y = 0; y < sizeY; y++) {
//			for (int x = 0; x < sizeX; x++) {
//				vals[cnt] = img.getValueDouble(x, y);
//				cnt++;
//			}
//		}
//
//		Arrays.sort(vals);
//		
//		double medVal = vals[vals.length/2];
//	//	System.out.println("Median " + medVal);
//		
//		
//		cnt = vals.length;
//		
//		for (int i = 0; i < cnt; i++) {
//			vals[i] = Math.abs(vals[i] - medVal);
//		}
//		
//		Arrays.sort(vals);
//		
//		double medSigma = vals[vals.length/2]/0.67;
//	//	System.out.println("Median deviation from median " + medSigma);
//		
//		double s2 = 3.0*medSigma*medSigma;
//		double val, val2;
//			
//		for (int y = 0; y < sizeY; y++) {
//			for (int x = 0; x < sizeX; x++) {
//				val = img.getValueDouble(x, y);
//				
//				if (val > 0.0) {
//					val2 = (val*val - s2);
//					if (val2 < 0.0)
//						val2 = 0.0;
//					
//					if (val >= val2/val) {
//						img.putValueDouble(x, y, val);
//					}
//					else {
//						img.putValueDouble(x, y, 0.0);
//					}
//				}
//				else {
//					img.putValueDouble(x, y, 0.0);
//				}
//			}
//		}
//		
//	}
	
//	protected double get3SigClippedStdDev(MTBImage img) {
//		int sizeX = img.getSizeX();
//		int sizeY = img.getSizeY();
//		//ImageStatistics is = img.getImagePlus().getStatistics(Measurements.MEAN + Measurements.STD_DEV);
//	//	double m1 = is.mean;
//	//	double s1 = is.stdDev;
//	//	System.out.println("ImagePlus> Mean: " + m1 + " Sigma: " + s1);
//		
//		double val;
//		double mu = 0.0;
//		double mu2 = 0.0;
//		double N = 0.0;
//		double sigma;
//		
//		for (int y = 0; y < sizeY; y++) {
//			for (int x = 0; x < sizeX; x++) {
//				val = img.getValueDouble(x, y);
//				mu += val;
//				mu2 += val*val;
//				N++;		
//			}
//		}
//	
//		mu /= N;
//		mu2 /= N;
//		sigma = Math.sqrt(mu2 - mu*mu);
//		
//	//	System.out.println("MTBImage> Mean: " + mu + " Sigma: " + sigma);
//
//		mu = 0.0;
//		mu2 = 0.0;
//		N = 0.0;
//
//		for (int y = 0; y < sizeY; y++) {
//			for (int x = 0; x < sizeX; x++) {
//				val = img.getValueDouble(x, y);
//				if (Math.abs(val - mu) <= 3.0*sigma) {
//					mu += val;
//					mu2 += val*val;
//					N++;
//				}
//			}
//		}
//
//		img.setActualSliceIndex(0);
//		
//		mu /= N;
//		mu2 /= N;
//		sigma = Math.sqrt(mu2 - mu*mu);
//		
//	//	System.out.println("MTBImage> Mean: " + mu + " Sigma: " + sigma);
//		
//		return sigma;	
//	}
//	
//	protected double getStdDev(MTBImage img) {
//		int sizeX = img.getSizeX();
//		int sizeY = img.getSizeY();
//			
//	//	ImageStatistics is = img.getImagePlus().getStatistics(Measurements.MEAN + Measurements.STD_DEV);
//	//	double m1 = is.mean;
//	//	double s1 = is.stdDev;
//	//	System.out.println("ImagePlus> Mean: " + m1 + " Sigma: " + s1);
//		
//		double val;
//		double mu = 0.0;
//		double mu2 = 0.0;
//		double N = 0.0;
//		double sigma;
//		
//		for (int y = 0; y < sizeY; y++) {
//			for (int x = 0; x < sizeX; x++) {
//				val = img.getValueDouble(x, y);
//				mu += val;
//				mu2 += val*val;
//				N++;		
//			}
//		}
//	
//		mu /= N;
//		mu2 /= N;
//		sigma = Math.sqrt(mu2 - mu*mu);
//		
//	//	System.out.println("MTBImage> Mean: " + mu + " Sigma: " + sigma);
//		
//		return sigma;	
//	}
	
//	protected MTBImage createGaussianNoiseImage(double mean, double sigma, double clippingFactor, int bins, int sizeX, int sizeY, int sizeZ, int sizeT, int sizeC) {
//		MTBImage gImg = MTBImage.createMTBImage(sizeX, sizeY, sizeZ, sizeT, sizeC, MTBImageType.MTB_DOUBLE);
//		int sizeStack = gImg.getSizeStack();
//		
//		double[] dist = new double[bins];
//		double cs = - clippingFactor*sigma;
//		double gFactor = 1.0/(Math.sqrt(2.0*Math.PI)*sigma);
//		
//		double lastVal = 0.0;
//		
//		double X;
//		
//		// cumulative distribution
//		for (int i = 0; i < bins; i++) {
//			X = ((double)i/(double)(bins-1))*2.0*cs - cs;
//			
//			dist[i] = lastVal + gFactor*Math.exp(-0.5*(X*X)/(sigma*sigma));
//			lastVal = dist[i];
//		}
//		
//		// normalization
////		for (int i = 0; i < bins; i++) {
////			dist[i] /= dist[bins-1];
////		}		
//		double sample;
//		for (int i = 0; i < sizeStack; i++) {
//			gImg.setActualSliceIndex(i);
//			
//			for (int y = 0; y < sizeY; y++) {
//				for (int x = 0; x < sizeX; x++) {
//					sample = getSample(dist);
//					sample = sample*2.0*cs - cs - mean;
//					gImg.putValueDouble(x, y, sample);
//				}
//			}
//		}
//		gImg.setActualSliceIndex(0);	
//		
//		return gImg;
//	}
	
	/**
	 * Returns a sample in the range [0, 1] from a cumulative distribution given by the array cdf
	 * @param cdf cumulative distribution array
	 * @return
	 */
	protected double getSample(double[] cdf) {
		double x = Math.random();
		
		int i = 0;
		while (i < cdf.length && cdf[i] < x) {
			i++;
		}
		
		return (double)i/(double)cdf.length;
	}

	
	// ----- StatusReporter interface
	
	@Override
	public void addStatusListener(StatusListener statuslistener) {	
		this.m_statusListeners.add(statuslistener);	
	}

	@Override
	public void notifyListeners(StatusEvent e) {
		for (int i = 0; i < this.m_statusListeners.size(); i++) {
			this.m_statusListeners.get(i).statusUpdated(e);
		}
	}

	@Override
	public void removeStatusListener(StatusListener statuslistener) {
		this.m_statusListeners.remove(statuslistener);
	}	
	
	
	/**
	 * Get input image
	 */
	public MTBImage getInputImage() {
		return this.inputImage;
	}
	
	/**
	 * Set input image
	 */
	public void setInputImage(MTBImage img) {
		this.inputImage = img;
	}	
	
	/**
	 * Get Jmin
	 */
	public Integer getJmin() {
		return this.Jmin;
	}
	
	/**
	 * Set Jmin
	 */
	public void setJmin(int _Jmin) {
		this.Jmin = new Integer(_Jmin);
	}
	
	/**
	 * Get Jmax
	 */
	public Integer getJmax() {
		return this.Jmax;
	}	
	
	/**
	 * Set Jmax
	 */
	public void setJmax(int _Jmax) {
		this.Jmax = new Integer(_Jmax);
	}
	
	/**
	 * Get threshold for thresholding wavelet correlation images
	 */
	public Double getCorrelationThreshold() {
		return this.corrThreshold;
	}
	
	/**
	 * Set threshold for thresholding wavelet correlation images
	 */
	public void setCorrelationThreshold(double corrThresh) {
		this.corrThreshold = new Double(corrThresh);
	}
	
	/**
	 * Get the size of the scale interval for correlation images
	 */
	public Integer getScaleIntervalSize() {
		return this.scaleIntervalSize;
	}
	
	/**
	 * Set the size of the scale interval for correlation images
	 */
	public void setScaleIntervalSize(int _scaleIntervalSize) {
		this.scaleIntervalSize = new Integer(_scaleIntervalSize);
	}
	
	/**
	 * Get the minimum size of detected regions. All regions smaller than this size are rejected
	 */
	public int getMinRegionSize() {
		return this.minRegionSize;
	}
	
	/**
	 * Set the minimum size of detected regions. All regions smaller than this size are rejected
	 */
	public void setMinRegionSize(int _minRegionSize) {
		this.minRegionSize = _minRegionSize;
	}	
	
	/**
	 * Get flag if input image with poisson noise is to be transformed to image with gaussian noise following
	 * J.-L. Starck et al., Multiresolution Support Applied to Image Filtering and Restoration
	 */
	public boolean getPoisson2Gauss() {
		return this.poisson2gauss;
	}
	
	/**
	 * Set flag if input image with poisson noise is to be transformed to image with gaussian noise following
	 * J.-L. Starck et al., Multiresolution Support Applied to Image Filtering and Restoration
	 */
	public void setPoisson2Gauss(boolean _poisson2gauss) {
		this.poisson2gauss = _poisson2gauss;
	}	

	/**
	 * Specify exclude mask.
	 */
	public void setExcludeMask(MTBImageByte mask) {
		this.excludeMask = mask;
	}

	/**
	 * Get resulting regions (each detetection corresponds to a region)
	 */
	public MTBRegion2DSet getResults() {
		return this.resultingRegions;
	}
	
	/**
	 * Set detected regions
	 */
	protected void setResults(MTBRegion2DSet detectedRegions) {
		this.resultRegionCount = detectedRegions.size();
		this.resultingRegions = detectedRegions;
	}
	
	/**
	 * Get resulting correlation image stack.
	 * <p>
	 * Note that {@link #additionalResultsWanted} needs to be selected to
	 * get a non-null result here.
	 * 
	 * @return Stack of correlation images.
	 */
	public MTBImage getResultCorrelationStack() {
		return this.correlationImages;
	}
	
	/**
	 * Get resulting binarized correlation image stack.
	 * <p>
	 * Note that {@link #additionalResultsWanted} needs to be selected to
	 * get a non-null result here.
	 * 
	 * @return Stack of binarized correlation images.
	 */
	public MTBImage getResultBinaryCorrelationStack() {
		return this.binaryCorrelationImages;
	}

	/**
	 * Thread class to run UWT wavelet transformation threaded.
	 * 
	 * @author moeller
	 */
	protected static class UWTRunner implements Runnable {
		
		/**
		 * Reference to (external) UWT operator.
		 */
		UndecimatedWaveletTransform uwt;
		
		/**
		 * Default constructor.
		 * @param u	UWT operator to be run.
		 */
		public UWTRunner(UndecimatedWaveletTransform u) {
			this.uwt = u;			
		}
		
    @Override
    public void run() {
    	try {
	      this.uwt.runOp();
      } catch (ALDException e) {
      	System.err.println(opIdentifier + "something went wrong " 
      		+ "while running undecimated wavelet transform, exiting...");
      }
    }
	}
}
