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

package de.unihalle.informatik.MiToBo.apps.plantCells.stromules;

import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.biomedical_imaging.ij.steger.Line;
import de.biomedical_imaging.ij.steger.Lines;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations .ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.Parameter.Direction;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.MiToBo.core.datatypes.images.*;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.operator.*;
import de.unihalle.informatik.MiToBo.morphology.ComponentPostprocess;
import de.unihalle.informatik.MiToBo.morphology.ComponentPostprocess.ProcessMode;
import de.unihalle.informatik.MiToBo.morphology.ConvexHullExtraction;
import de.unihalle.informatik.MiToBo.morphology.ConvexHullExtraction.InputType;
import de.unihalle.informatik.MiToBo.morphology.DistanceTransform;
import de.unihalle.informatik.MiToBo.morphology.DistanceTransform.DistanceMetric;
import de.unihalle.informatik.MiToBo.morphology.DistanceTransform.ForegroundColor;
import de.unihalle.informatik.MiToBo.morphology.SkeletonAnalysisHelper;
import de.unihalle.informatik.MiToBo.morphology.SkeletonExtractor;
import de.unihalle.informatik.MiToBo.segmentation.regions.labeling.LabelAreasToRegions;
import de.unihalle.informatik.MiToBo.segmentation.regions.labeling.LabelComponentsSequential;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBContour2D;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBPolygon2D;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBPolygon2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBQuadraticCurve2D;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2D;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2DSet;
import de.unihalle.informatik.MiToBo.segmentation.thresholds.ImgThreshNiblack;
import de.unihalle.informatik.MiToBo.visualization.drawing.DrawEllipse;
import de.unihalle.informatik.MiToBo.visualization.drawing.DrawRegion2DSet;
import de.unihalle.informatik.MiToBo.visualization.drawing.DrawRegion2DSet.DrawType;
import de.unihalle.informatik.MiToBo.filters.linear.anisotropic.GaussPDxxFilter2D;
import de.unihalle.informatik.MiToBo.filters.linear.anisotropic.OrientedFilter2DBatchAnalyzer;
import de.unihalle.informatik.MiToBo.filters.vesselness.StegerRidgeDetection2D;
import de.unihalle.informatik.MiToBo.math.MathGeometry;

import ij.IJ;

import java.awt.Color;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import loci.common.StatusEvent;
import loci.common.StatusListener;
import loci.common.StatusReporter;

/**
 * Operator to detect stromuli in microscope images of plastids.
 * 
 * @author Birgit Moeller
 */
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.ALL,
		level=Level.APPLICATION)
public class StromulesDetector2D extends MTBOperator implements StatusReporter {

	/**
	 * Identifier for outputs in verbose mode.
	 */
	private final static String opIdentifier = "[StromuliDetector2D] ";

	/**
	 * Heuristic to use for detecting stromuli.
	 */
	public static enum DetectMode {
		/**
		 * Detection based on morphological analysis of stromuli/plastid region.
		 */
		DETECT_MORPHOLOGY,
		/**
		 * Detection based on combining various statistical cues.
		 */
		DETECT_MULTICUE,
		DETECT_RIDGE
	}
	
	/**
	 * Gray-scale input image.
	 */
	@Parameter(label = "Input Image", required = true, dataIOOrder = 0,
			direction = Parameter.Direction.IN, description = "Input image.")
	private MTBImageByte inImg = null;
	
	/**
	 * Binary mask of pre-segmented plastid regions.
	 */
	@Parameter(label = "Plastid Mask", required = true, dataIOOrder = 1,
			direction = Parameter.Direction.IN, description = "Plastid mask.")
	private MTBImageByte plastidMask = null;
	
	/**
	 * Detection mode.
	 */
	@Parameter(label = "Detection Mode", required = true, dataIOOrder = 2,
			direction = Parameter.Direction.IN, description = "Detection mode.")
	private DetectMode mode = DetectMode.DETECT_RIDGE;
	
	/**
	 *  Enable/disable line multi-intersection check.
	 */
	@Parameter( label= "Apply line multi-intersection check?", 
		required = true, dataIOOrder = 12,
		direction= Parameter.Direction.IN, mode=ExpertMode.STANDARD, 
	  description = "Checks if a potential stromuli line intersects a region " 
	  		+ "at least twice, then it might be a reflection")
	protected boolean useMultiIntersectionCheck = false;

	/**
	 *  Enable/disable ellipse distance threshold.
	 */
	@Parameter( label= "Apply ellipse distance threshold?", 
		required = true, dataIOOrder = 13,
		direction= Parameter.Direction.IN, mode=ExpertMode.STANDARD, 
	  description = "Use Ellipse distance threshold.")
	protected boolean useEllipseDistThreshold = false;

	/**
	 *  Ellipse distance threshold.
	 */
	@Parameter( label= "Ellipse distance threshold", required = true, 
		dataIOOrder = 14,
		direction= Parameter.Direction.IN, mode=ExpertMode.STANDARD, 
	  description = "Ellipse distance threshold.")
	protected double ellipseDistThresh = 3.0;

	/**
	 *  Enable/disable angle criterion.
	 */
	@Parameter( label= "Apply angle criterion?", 
		required = true, dataIOOrder = 15,
		direction= Parameter.Direction.IN, mode=ExpertMode.STANDARD, 
	  description = "Apply stromuli angle criterion.")
	protected boolean useAngleCriterion = true;

	/**
	 *  Stromuli orientation angle criterion.
	 */
	@Parameter( label= "Stromuli angle threshold", required = true, 
		dataIOOrder = 16,
		direction= Parameter.Direction.IN, mode=ExpertMode.STANDARD, 
	  description = "Stromuli-tangent angle threshold (in degrees).")
	protected double stromuliAngleThreshold = 60.0;

	/**
	 * Line width.
	 */
	@Parameter( label= "Line Width", required = true, dataIOOrder = 20,
		direction= Parameter.Direction.IN, mode=ExpertMode.STANDARD, 
	  description = "Line width.")
	protected double lineWidth = 2.0;

	/**
	 *  Lowest grayscale value of the line.
	 */
	@Parameter( label= "Low Contrast", required = true, dataIOOrder = 21,
		direction= Parameter.Direction.IN, mode=ExpertMode.STANDARD, 
	  description = "Low contrast.")
	protected double lowContrast = 20;
	
	/**
	 * Highest grayscale value of the line.
	 */
	@Parameter( label= "High Contrast", required = true, dataIOOrder = 22,
		direction= Parameter.Direction.IN, mode=ExpertMode.STANDARD, 
	  description = "High contrast.")
	protected double highContrast = 90;

	/**
	 * Minimal line length.
	 */
	@Parameter( label= "Minimum Line Length", required = true, dataIOOrder = 23,
		direction= Parameter.Direction.IN, mode=ExpertMode.STANDARD, 
	  description = "Minimum line length.")
	protected double minLineLength = 0.0;

	/**
	 * Maximal line length.
	 */
	@Parameter( label= "Maximum Line Length", required = true, dataIOOrder = 24,
		direction= Parameter.Direction.IN, mode=ExpertMode.STANDARD, 
	  description = "Maximum line length.")
	protected double maxLineLength = 0.0;

	/**
	 * (Optional) set of detected plastid regions.
	 */
	@Parameter(label = "Plastid Regions", dataIOOrder = 0,
			direction = Direction.IN, required = false,
			description = "Resulting plastid region set.")
	private MTBRegion2DSet plastidRegions = null;

	/**
	 * Enable/disable display of additional intermediate results.
	 */
	@Parameter( label= "Show additional intermediate results?", 
		dataIOOrder = 0, supplemental = true,
		direction= Parameter.Direction.IN, mode=ExpertMode.STANDARD, 
	  description = "Enables display of additional result images.")
	protected boolean showAdditionalResults = false;

	//@Parameter( label= "minWidth", required = true, dataIOOrder = 2,
	//direction = Parameter.Direction.IN, description = "minWidth")
	int minW = 1;
	
	/**
	 * Set of detected plastid regions with stromuli.
	 */
	@Parameter(label = "Stromuli Regions", dataIOOrder = 1,
			direction = Direction.OUT, 
			description = "Resulting stromuli region set.")
	private MTBRegion2DSet stromuliRegions = null;

	/**
	 * Label image of detected plastid regions with stromuli. 
	 */
	@Parameter(label = "Result Label Image", dataIOOrder = 2,
			direction = Parameter.Direction.OUT, 
			description = "Label image of detected plastids with stromuli.")
	private MTBImageShort resultLabelImage = null;

	/**
	 * Size of input image in x dimension.
	 */
	private int xSize;
	/**
	 * Size of input image in y dimension.
	 */
	private int ySize;
	/**
	 * Size of input image in z dimension.
	 */
	private int zSize;
	/**
	 * Size of input image in t dimension.
	 */
	private int tSize;
	/**
	 * Size of input image in c dimension.
	 */
	private int cSize;

	private int degSampling = 10;
	
	private double[] comXs;
	private double[] comYs;
	private double[] orient;
	private double[] minAxisLengths;
	private double[] maxAxisLengths;
	
	private Vector<MTBRegion2D> identifiedRegions = new Vector<>();
	
	private MTBRegion2DSet candidateRegions;
	
	private MTBImageRGB resultImgIntermediate = null;

	/** 
	 * Vector of installed {@link StatusListener} objects.
	 */
	protected Vector<StatusListener> m_statusListeners;
	
	/**
	 * Default constructor.
	 * @throws ALDOperatorException	Thrown in case of operate failure.
	 */
	public StromulesDetector2D() throws ALDOperatorException {
		this.m_statusListeners = new Vector<StatusListener>(1);
	}		
	 
	@Override
	protected void operate() 
			throws ALDOperatorException, ALDProcessingDAGException {

		// post ImageJ status
		String msg = opIdentifier + "running stromuli detection...";	
		this.notifyListeners(new StatusEvent(msg));

		// reset some internal variables
		this.identifiedRegions = new Vector<>();
		
		this.xSize = this.inImg.getSizeX();
		this.ySize = this.inImg.getSizeY();
		this.zSize = this.inImg.getSizeZ();
		this.tSize = this.inImg.getSizeT();
		this.cSize = this.inImg.getSizeC();
		
		if (this.showAdditionalResults) {
			this.resultImgIntermediate = (MTBImageRGB)MTBImage.createMTBImage(
				this.xSize, this.ySize, this.zSize, this.tSize, this.cSize, 
					MTBImageType.MTB_RGB);
			for (int y=0; y<this.ySize; ++y) {
				for (int x=0; x<this.xSize; ++x) {
					this.resultImgIntermediate.putValueR(
							x, y, this.inImg.getValueInt(x, y));
					this.resultImgIntermediate.putValueG(
							x, y, this.inImg.getValueInt(x, y));
					this.resultImgIntermediate.putValueB(
							x, y, this.inImg.getValueInt(x, y));
				}
			}
		}

//		MTBImageByte plastidImg = 
//				(MTBImageByte)MTBImage.createMTBImage(this.xSize, this.ySize, this.zSize, 
//						this.tSize,	this.cSize, MTBImageType.MTB_BYTE);
//		this.resultLabelImage = (MTBImageShort)MTBImage.createMTBImage(
//			this.xSize, this.ySize, this.zSize, this.tSize,	this.cSize, 
//				MTBImageType.MTB_SHORT);
		
//		this.plastidMask.show();
		
		// if no label plastid regions are given, label binary plastid region image
		if (this.plastidRegions == null) {
			LabelComponentsSequential lableOp = 
					new LabelComponentsSequential(this.plastidMask,true);
			lableOp.runOp();
			this.plastidRegions = lableOp.getResultingRegions();
		}
		
		// preprocess plastid regions to exclude/pre-classify inaccurate detections
		this.candidateRegions = this.preprocessPlastidRegions();
		
		// draw already identified regions to intermediate result image (in blue)
		if (this.showAdditionalResults) {
			MTBRegion2DSet iRegs = new MTBRegion2DSet();
			if (this.identifiedRegions.size() > 0) {
				for (MTBRegion2D reg: this.identifiedRegions) {
					iRegs.add(reg);
				}
			}
			DrawRegion2DSet drs = new DrawRegion2DSet(DrawType.CONTOURS, iRegs,
					this.resultImgIntermediate, false);
			drs.setColor(Color.blue);
			drs.runOp();
		}
		
		MTBImageShort centerImg = (MTBImageShort)MTBImage.createMTBImage(
			this.xSize, this.ySize, this.zSize, this.tSize, this.cSize, 
				MTBImageType.MTB_SHORT);
		int rc = 0;
		this.comXs = new double[this.candidateRegions.size()];
		this.comYs = new double[this.candidateRegions.size()];
		this.orient = new double[this.candidateRegions.size()];
		this.minAxisLengths = new double[this.candidateRegions.size()];
		this.maxAxisLengths = new double[this.candidateRegions.size()];
		for (MTBRegion2D r: this.candidateRegions) {
//			for (int y=-1; y<=1; ++y) {
//				for (int x=-1; x<=1; ++x) {
//					int dx = (int)r.getCenterOfMass_X() + x;
//					int dy = (int)r.getCenterOfMass_Y() + y;
//					if (dx<0 || dx>=this.xSize || dy<0 || dy>=this.ySize)
//						continue;
//					centerImg.putValueInt(dx, dy, rc+1);
					this.comXs[rc] = r.getCenterOfMass_X();
					this.comYs[rc] = r.getCenterOfMass_Y();
					this.orient[rc] = r.getOrientation();
					this.minAxisLengths[rc] = r.getMinorAxisLength();
					this.maxAxisLengths[rc] = r.getMajorAxisLength();
//				}
//			}
			++rc;
		}
		
		// post ImageJ status
		msg = opIdentifier + "determining plastid neighborhood ROIs...";	
		this.notifyListeners(new StatusEvent(msg));

		// draw given regions to label image

		DrawRegion2DSet dreg = new DrawRegion2DSet();
		dreg.setDrawType(DrawRegion2DSet.DrawType.LABEL_IMAGE);
		dreg.setInputRegions(this.candidateRegions);
		dreg.setImageType(MTBImageType.MTB_SHORT);
		dreg.setResultImageWidth(this.xSize);
		dreg.setResultImageHeight(this.ySize);
		dreg.runOp();
		MTBImageShort plastidRegionImage = (MTBImageShort)dreg.getResultImage();

		ComponentPostprocess postProc = new ComponentPostprocess(
				plastidRegionImage, ProcessMode.DILATE_TOPOLOGY_PRESERVING);
		postProc.setDiagonalNeighbors(true);
		postProc.setDilateMaskSize(20);
		postProc.runOp();
		MTBImageShort plastidRegionImageDilated = 
				(MTBImageShort)postProc.getResultImage();
		MTBRegion2DSet plastidRegionsDilated = 
				LabelAreasToRegions.getRegions(plastidRegionImageDilated, 0);
		
//		plastidRegionImage.show();
		//centerImg.setTitle("Image with plastid centers");
		
		// somehow the labels are not consistent, check explicitly...
//		for (MTBRegion2D r: this.plastidRegions) {
//			double dx = r.getCenterOfMass_X();
//			double dy = r.getCenterOfMass_Y();
//			int x = (int)dx;
//			int y = (int)dy;
//			int label = centerImg.getValueInt(x, y);
//			comXs[label-1] = dx;
//			comYs[label-1] = dy;
//		}

		msg = opIdentifier + "filtering ROIs for vessel-like structures...";	
		this.notifyListeners(new StatusEvent(msg));

		//2.0 Erstellen des Vesselbildes ###(ALT)###
		/*MPMFFilter2D vessel_filter = new MPMFFilter2D ();
		vessel_filter.setParameter("inputImg", inCopy_g);
		vessel_filter.setParameter("minWidth",minW);
		vessel_filter.setParameter("maxWidth",maxW);
		vessel_filter.setParameter("mode",MPMFFilter2D.VesselMode.valueOf("BRIGHT_ON_DARK_BACKGROUND"));
		vessel_filter.setVerbose(true);
		vessel_filter.runOp();*/
		//2.0 Erstellen des Vesselbildes ###(NEU)###
//		result_img.show();
		
//		for (int y=0; y<this.ySize; ++y) {
//			for (int x=0; x<this.xSize; ++x) {
//				if (plastidRegionImage.getValueInt(x, y)==0) {
//					plastidRegionImage.putValueDouble(x, y, 
//						result_img.getValueDouble(x, y));
//				}
//			}
//		}
//		plastidRegionImage.show();
				
//		double normFactor = Math.pow(sigma*sigma, 3.0/4.0);
//		double normFactor = 1.0;
//		for (int y=0; y<result_img.getSizeY(); ++y) 
//		{
//			for (int x=0; x<result_img.getSizeY(); ++x) 
//			{
//				result_img.putValueDouble(x, y, result_img.getValueDouble(x, y) * normFactor);
//			}
//		}
		

		/* BM end */

		msg = opIdentifier + "extracting stromuli candidates...";	
		this.notifyListeners(new StatusEvent(msg));

		switch(this.mode) 
		{
		case DETECT_MORPHOLOGY:
			GaussPDxxFilter2D xxFilter = new GaussPDxxFilter2D();
			xxFilter.setInvertMask(true);
			xxFilter.setHeight(13);
			xxFilter.setInputImage(this.inImg);
			OrientedFilter2DBatchAnalyzer bOp = 
					new OrientedFilter2DBatchAnalyzer();
			double sigma = this.minW/ 2.0;
			xxFilter.setStandardDeviation(sigma);
			bOp.setInputImage(this.inImg);
			bOp.setOrientedFilter(xxFilter);
			bOp.setAngleSampling(this.degSampling);
			bOp.runOp();
			MTBImage result_img = bOp.getResultImage();
			MTBImage result_stack = bOp.getFilterResponseStack();
			for (int i=0;i<this.comXs.length;++i) {
				for (int y=-1; y<=1; ++y) {
					for (int x=-1; x<=1; ++x) {
						int dx = (int)this.comXs[i] + x;
						int dy = (int)this.comYs[i] + y;
						if (dx<0 || dx>=this.xSize || dy<0 || dy>=this.ySize)
							continue;
						result_img.putValueInt(dx, dy, i+1);
					}
				}
			}
			this.detectStromuliMorphology(result_img);
			break;
		case DETECT_MULTICUE:
			xxFilter = new GaussPDxxFilter2D();
			xxFilter.setInvertMask(true);
			xxFilter.setHeight(13);
			xxFilter.setInputImage(this.inImg);
			bOp =	new OrientedFilter2DBatchAnalyzer();
			sigma = this.minW/ 2.0;
			xxFilter.setStandardDeviation(sigma);
			bOp.setInputImage(this.inImg);
			bOp.setOrientedFilter(xxFilter);
			bOp.setAngleSampling(this.degSampling);
			bOp.runOp();
			result_img = bOp.getResultImage();
			result_stack = bOp.getFilterResponseStack();
			for (int i=0;i<this.comXs.length;++i) {
				for (int y=-1; y<=1; ++y) {
					for (int x=-1; x<=1; ++x) {
						int dx = (int)this.comXs[i] + x;
						int dy = (int)this.comYs[i] + y;
						if (dx<0 || dx>=this.xSize || dy<0 || dy>=this.ySize)
							continue;
						result_img.putValueInt(dx, dy, i+1);
					}
				}
			}
			this.detectStromuliStatisticalModel(plastidRegionImageDilated, result_stack, this.comXs, this.comYs, this.orient);
			break;
		case DETECT_RIDGE:
			this.detectStromuliRidgeModel(plastidRegionImage, 
					plastidRegionImageDilated, plastidRegionsDilated);
			break;
		}
		
		// copy final stromuli regions to output region set
		double xMax = 0.0;
		double yMax = 0.0;
		Vector<MTBRegion2D> stromulis = new Vector<MTBRegion2D>();
		for(int k = 0; k<this.identifiedRegions.size();k++) {
			MTBRegion2D reg = this.identifiedRegions.get(k);
			double[] minmax = reg.getMinMaxCoordinates();
			if (minmax[2] > xMax)
				xMax = minmax[2];
			if (minmax[3] > yMax)
				yMax = minmax[3];
			stromulis.add(reg);
		}
				
		// set resulting region set as value of operator's output parameter
		this.stromuliRegions = 
				new MTBRegion2DSet(stromulis, 0, 0, xMax, yMax);

		msg = opIdentifier + "calculations completed!";	
		this.notifyListeners(new StatusEvent(msg));
	}
	
	private MTBRegion2DSet preprocessPlastidRegions() 
			throws ALDOperatorException, ALDProcessingDAGException {

		// distance transformation of plastid mask
		DistanceTransform distOp = new DistanceTransform(this.plastidMask, 
				DistanceMetric.CITYBLOCK, ForegroundColor.FG_BLACK);
		distOp.runOp();
		MTBImage distImg = distOp.getDistanceImage();
		
		SkeletonExtractor skelOp = new SkeletonExtractor();
		skelOp.setInputImage(this.plastidMask);
		skelOp.runOp();
		MTBImageByte skelImg = skelOp.getResultImage();
		
		ConvexHullExtraction cvEx = new ConvexHullExtraction();
		cvEx.setInputType(InputType.REGIONS);
		cvEx.setInputRegions(this.plastidRegions);
		cvEx.runOp();
		Vector<Point2D.Double[]> convexHulls = cvEx.getResultingConvexHulls();
		MTBPolygon2DSet convexHullPolys = new MTBPolygon2DSet();

		MTBPolygon2D poly;
		for (Point2D.Double[] ps : convexHulls) {
			poly = new MTBPolygon2D();
			for (Point2D.Double p: ps) {
				poly.addPoint(p.x, p.y);
			}
			poly.setClosed();
			convexHullPolys.add(poly);
		}
		
//		MTBImage mask = this.inImg.duplicate();
//		mask.fillBlack();
//		DrawRegion2DSet drawer = new DrawRegion2DSet(DrawType.MASK_IMAGE, particleRegions);
//		drawer.setTargetImage(mask);
//		drawer.runOp();
//		drawer.getResultImage().show();

//		MTBImage resImg = this.inImg.duplicate();
//		DrawRegion2DSet drawer = new DrawRegion2DSet(DrawType.LABEL_IMAGE, this.plastidRegions);
//		drawer.setTargetImage(resImg);
//		drawer.runOp();
//		resImg = drawer.getResultImage();

//		DrawPolygon2DSet draw = new DrawPolygon2DSet(convexHullPolys);
//		draw.setInputImage((MTBImageRGB)resImg.convertType(MTBImageType.MTB_RGB, true));
//		draw.setColor("red");
//		draw.runOp();
//		MTBImageRGB resImgRGB = (MTBImageRGB)draw.getResultImage(); 

		MTBRegion2DSet remainingRegions = new MTBRegion2DSet();
		
		MTBImage processed = this.inImg.duplicate();
		processed.fillBlack();
		
		int smallDistCount, skelPixNum, id = 1;
		double solidity, convexHullArea, regionArea = 0;
		MTBPolygon2D regionPolygon;
		for (MTBRegion2D reg: this.plastidRegions) {
//			drawStringToImage(resImgRGB, Integer.toString(id), 255, 0, 0, 
//					(int)reg.getCenterOfMass_X(), (int)reg.getCenterOfMass_Y());

			double cx = reg.getCenterOfMass_X();
			double cy = reg.getCenterOfMass_Y();

//			double a = reg.getMajorAxisLength();
//			double b = reg.getMinorAxisLength();
//			double c = reg.getCircularity();

			regionPolygon = new MTBPolygon2D(reg.getContour().getPoints(), true);
			regionArea = Math.abs(regionPolygon.getSignedArea());

			convexHullArea = 
					Math.abs(convexHullPolys.elementAt(id-1).getSignedArea());
			solidity = regionArea / convexHullArea; 

//			int min = Integer.MAX_VALUE;
//			int max = 0;
//			double mean = 0;
//			for (Point2D.Double p: reg.getPoints()) {
//				int val = this.inImg.getValueInt((int)p.x, (int)p.y); 
//				if (val < min)
//					min = val;
//				if (val > max)
//					max = val;
//				mean += val;
//			}
//			mean /= reg.getPoints().size();
//			double var = 0;
//			for (Point2D.Double p: reg.getPoints()) {
//				int val = this.inImg.getValueInt((int)p.x, (int)p.y);
//				var += (val - mean) * (val - mean);
//			}
//			var /= reg.getPoints().size();
//
//			System.out.println(id + "\t" + cx + "\t" + cy + "\t" +regionArea + "\t"
//					+ areaCoeff + "\t" + polyArea + "\t" 
//					+ convHullCoeff + "\t"
//					+ ellHullCoeff + "\t" + min + "\t" + max + "\t" + mean + "\t" 
//					+ var + "\t" + a + "\t" + b + "\t" + c);			

			Vector<Point2D.Double> endPoints = new Vector<>();
			
			smallDistCount = 0;
			skelPixNum = 0;
//			System.out.println("Checking " + cx + " / " + cy + " , solidity = " + solidity);
			if (solidity < 0.85) {
				
				for (Point2D.Double p: reg.getPoints()) {
					int px = (int)p.x;
					int py = (int)p.y;
					if (skelImg.getValueInt(px, py) == 0)
						continue;
					++skelPixNum;
					int neighbors = 0;
					for (int dx=-1;dx<=1;++dx) {
						for (int dy=-1;dy<=1;++dy) {
							if (dx == 0 && dy == 0)
								continue;
							if (   px + dx >= 0 && px + dx < this.inImg.getSizeX() 
									&& py + dy >= 0 && py + dy < this.inImg.getSizeY()) {
								if (skelImg.getValueInt(px+dx, py+dy) > 0)
									++neighbors;								
							}
						}
					}
					if (neighbors == 1)
						endPoints.add(p);
				}
				int longestRun = 0;
				for (Point2D.Double p: endPoints) {
					
					int px = (int)p.x;
					int py = (int)p.y;
					Vector<Point2D.Double> branch = 
							SkeletonAnalysisHelper.traceBranch(skelImg, px, py);
					
					if (branch.size() < 5)
						continue;
					
					Point2D.Double sp = branch.get(0);
					Point2D.Double ep = branch.get(branch.size()-1);
					if (  Math.abs(sp.x - ep.x) + Math.abs(sp.y - ep.y)
					    < branch.size()/2.0) 
						continue;
					
					smallDistCount = 0;
					for (Point2D.Double bp: branch) {
						if (distImg.getValueDouble((int)bp.x, (int)bp.y) <= 4) {
							++smallDistCount;
						}
						else {
							if (smallDistCount > longestRun)
								longestRun = smallDistCount;
							smallDistCount = 0;
						}
					}
				}
				if (longestRun > 5)
					this.identifiedRegions.add(reg);
				else
					remainingRegions.add(reg);
			}
			else {
				remainingRegions.add(reg);
			}
			
			
			++id;
		}
//		resImgRGB.show();		
		return remainingRegions;
	}

	private void detectStromuliRidgeModel(MTBImageShort roiImg, 
		MTBImageShort roiImgDilated, MTBRegion2DSet dilatedROIs) 
		throws ALDOperatorException, ALDProcessingDAGException {

		if (this.inImg.getImagePlus().isInvertedLut()) {
			System.out.println("Invert LUT");
			IJ.run(this.inImg.getImagePlus(), "Invert LUT", "");
		}

		String msg = opIdentifier + " -> running Steger detection...";	
		this.notifyListeners(new StatusEvent(msg));

		// detect ridge lines
		StegerRidgeDetection2D stegerOp = new StegerRidgeDetection2D();
		stegerOp.setInputImage(this.inImg);
		stegerOp.setLineWidth(this.lineWidth);
		stegerOp.setLowContrast(this.lowContrast);
		stegerOp.setHighContrast(this.highContrast);
		stegerOp.setMinLineLength(this.minLineLength);
		stegerOp.setMaxLineLength(this.maxLineLength);
		stegerOp.setDarkLine(false);
		stegerOp.setCorrectPosition(true);
		stegerOp.setEstimateWidth(true);
		stegerOp.setExtendLine(true);
		stegerOp.runOp();
		Lines lines = stegerOp.getResultLines();

		// some variables
		int ex, ey, eex, eey;
		double cx, cy;

		// copy input image into the background
		if (this.showAdditionalResults) {
			MTBImageRGB resultImgStegerLines = (MTBImageRGB)MTBImage.createMTBImage(
				this.xSize, this.ySize, this.zSize, this.tSize, this.cSize, 
					MTBImageType.MTB_RGB);
			for (int y=0; y<this.ySize; ++y) {
				for (int x=0; x<this.xSize; ++x) {
					resultImgStegerLines.putValueR(x, y, this.inImg.getValueInt(x, y));
					resultImgStegerLines.putValueG(x, y, this.inImg.getValueInt(x, y));
					resultImgStegerLines.putValueB(x, y, this.inImg.getValueInt(x, y));
				}
			}
			StegerRidgeDetection2D.drawResultsToImage(resultImgStegerLines, 
					lines, null, false, false);
			resultImgStegerLines.setTitle(
					"Intermediate result image: line detection result");
			resultImgStegerLines.show();
			
			// draw centers of mass to intermediate result image
			for (int i=0;i<this.comXs.length;++i) {
				for (int y=-1; y<=1; ++y) {
					for (int x=-1; x<=1; ++x) {
						int dx = (int)this.comXs[i] + x;
						int dy = (int)this.comYs[i] + y;
						if (dx<0 || dx>=this.xSize || dy<0 || dy>=this.ySize)
							continue;
						this.resultImgIntermediate.putValueInt(dx, dy, 0xffff00);
					}
				}
			}
		}

		Line l;
		boolean found = true;
		double px, py;
		float xc[], yc[], lineAngles[], widthL[], widthR[];
		int label, np;


		double minP = Double.MAX_VALUE;
		double maxP = 0.0;
		double stromuliProb = 0;
		double radDiff = Math.toRadians(30.0);

		msg = opIdentifier + " -> mapping lines to regions...";	
		this.notifyListeners(new StatusEvent(msg));

		// check which line is overlapping with which dilated region
		HashMap<Integer, TreeSet<Integer>> regionIDsToLineIDs = new HashMap<>();
		for (MTBRegion2D r: dilatedROIs)
			regionIDsToLineIDs.put(new Integer(r.getID()), new TreeSet<Integer>());

		for (int i=0; i<lines.size(); ++i) {
			l = lines.get(i);
			np = l.getNumber();
			xc = l.getXCoordinates();
			yc = l.getYCoordinates();

//			System.out.println("Checking " + xc[0] + " , " + yc[0] + " -> " 
//					+ xc[np-1] + " , " + yc[np-1] + "...");					
			
			for (int j=0; j<np; j++) {
				px = xc[j];
				py = yc[j];
				label = roiImgDilated.getValueInt((int)px, (int)py); 
				if (label > 0) {
					regionIDsToLineIDs.get(new Integer(label)).add(new Integer(i));
					cx = this.comXs[label-1];
					cy = this.comYs[label-1];
//					System.out.println(" => overlapping region " + cx + " , " + cy);
				}
			}
		}

		// list of "stromuli" lines
		Lines validLines = new Lines(0);

		
		// process each region, check for lines and their characteristics
		boolean stromuliRegion = false;
		boolean stromuli = false;
		MTBRegion2DSet final_regions = new MTBRegion2DSet();
		Set<Integer> regionIDs = regionIDsToLineIDs.keySet();
		
		DrawEllipse de = null;
		for (Integer rid: regionIDs) {
			int regionIndex = rid.intValue();
			TreeSet<Integer> lineIDs = regionIDsToLineIDs.get(rid);

			// no overlapping line found
			if (lineIDs.isEmpty())
				continue;
			
			// orientation of region (relative to x-axis), map to [0, pi]
			stromuliRegion = false;
			cx = this.comXs[regionIndex-1];
			cy = this.comYs[regionIndex-1];
			ex = (int)(cx + 0.5*this.maxAxisLengths[regionIndex-1] * Math.cos(this.orient[regionIndex-1]));
			ey = (int)(cy + 0.5*this.maxAxisLengths[regionIndex-1] * Math.sin(this.orient[regionIndex-1]));
			eex = (int)(cx - 0.5*this.maxAxisLengths[regionIndex-1] * Math.cos(this.orient[regionIndex-1]));
			eey = (int)(cy - 0.5*this.maxAxisLengths[regionIndex-1] * Math.sin(this.orient[regionIndex-1]));
			double regionAngle = this.orient[regionIndex-1];			
			if (regionAngle < 0)
				regionAngle += Math.PI;
			
			// init ellipse object
			double[] params = new double[5];
			params[0] = cx;
			params[1] = cy;
			params[2] = 0.5*this.maxAxisLengths[regionIndex-1];
			params[3] = 0.5*this.minAxisLengths[regionIndex-1];
			params[4] = Math.toDegrees(this.orient[regionIndex-1]);
			MTBQuadraticCurve2D ell = new MTBQuadraticCurve2D(params, false);
			
			if (this.showAdditionalResults) {
				de = new DrawEllipse();
				de.setInputImage(this.resultImgIntermediate);
				de.setEllipse(ell);
//				if (stromuliRegion)
//					de.setColor(Color.GREEN);
//				else
					de.setColor(Color.WHITE);
				de.runOp();
				this.resultImgIntermediate = (MTBImageRGB)de.getResultImage();
			}
			
			LinkedList<Line> stromulis = new LinkedList<>();
			LinkedList<boolean[]> stromuliOutpoints = new LinkedList<>();
			for (Integer lid: lineIDs) {

				stromuli = false;
				
				int lineIndex = lid.intValue();
				l = lines.get(lineIndex);
				np = l.getNumber();

				xc = l.getXCoordinates();
				yc = l.getYCoordinates();

//				System.out.println();
//				System.out.println("--- next line ---");
//				System.out.println(" region: " + cx + " , " + cy + " , angle = " + regionAngle);
//						
//				System.out.println(" - Line " + l.getID() + " : " 
//						+ xc[0] + " , " + yc[0] + " -> " + xc[np-1] + " , " + yc[np-1]);					

				// skip lines shorter than 5 pixels
				if (np < 5) {
					continue;
				}
				
				widthL = l.getLineWidthL();
				widthR = l.getLineWidthR();
				float[] assyms = l.getAsymmetry();
				
				float meanAssym = 0;
				
				int[] widthConsistencies = new int[np];
				for (int j=0; j<np; j++) {
					if (Math.abs(widthL[j] - widthR[j]) > 0.5)
						widthConsistencies[j] = 0;
					else	
						widthConsistencies[j] = 1;
//					if (assyms[j] > 0.1)
//						widthConsistencies[j] = 0;
//					else
//						widthConsistencies[j] = 1;
				}
				
				// find longest run
				int run = 0;
				for (int j=0; j<np; j++) {
					if (widthConsistencies[j] == 1)
						++run;
					else
						run=0;
				}
				
				// angles of normals per pixel relative to y-axis in range [0,2*pi],
				// 0 or 180 degree means normal is parallel to y-axis, i.e. line is
				// horizontally aligned
				lineAngles = l.getAngle();
				
				stromuliProb = 0;
				double minAngle = Double.MAX_VALUE;
				double maxAngle = 0;
				for (int j=0; j<np; j++) {
					px = xc[j];
					py = yc[j];

					// map line normals to range of [0, pi]
					double lAng = lineAngles[j];
					if (lAng > Math.PI)
						lAng -= Math.PI;
					
					if (lAng > maxAngle)
						maxAngle = lAng;
					if (lAng < minAngle)
						minAngle = lAng;
					
					double angleDiff = 
							Math.abs(Math.PI/2.0 - Math.abs(lAng - regionAngle)); 

//					System.out.println(String.format( "%.2f", px ) 
//						+ " , " + String.format( "%.2f", py ) + ":\t" 
//							+ lineAngles[j] + ",\t" + lAng + ",\t diff = " + angleDiff);
					
					// rotate line point relative to region main axis
					Point2D.Double rp = 
							MathGeometry.rotatePoint2D(px-cx, py-cy, -regionAngle);
					
					// calculate stromuli probability
//					stromuliProb += Math.exp(-8.0*angleDiff) 
//							* 2.0/Math.PI*Math.atan2(Math.abs(rp.y), Math.abs(rp.x));					
					stromuliProb += angleDiff;					
				}
				
				stromuliProb /= np;
				
//				System.out.println(xc[0] + " , " + yc[0] + " -> " + stromuliProb);					

//				if (stromuliProb < radDiff) {
					
//					System.out.println(xc[0] + " , " + yc[0] + " -> " + stromuliProb);					

				boolean[] outPoint = new boolean[np];
				double a = 0.5*this.maxAxisLengths[regionIndex-1];
				double b = 0.5*this.minAxisLengths[regionIndex-1];
				
				// remember point of line closest to ellipse contour
				Point2D.Double closestPointLine = null;
				int closestPointLineIndex = -1;
				Point2D.Double closestPointEllipse = null;
				
				int crossings = 0;
				// -1 = outside, 1 = inside
				int location = 0;
				double minContourDist = Double.MAX_VALUE;
				for (int j=0; j<np; j++) {
					
					px = xc[j];
					py = yc[j];
					Point2D.Double p = new Point2D.Double(px,py);

					Point2D.Double rp = 
							MathGeometry.rotatePoint2D(px-cx, py-cy, -regionAngle);

					double ec = (rp.x)*(rp.x)/(a*a) + (rp.y)*(rp.y)/(b*b);

					// check if line segment is crossing ellipse more than once
					if (location == 0) {
						// point lies outside
						if (ec > 1.0) {
							location = -1;
						}
						// point lies inside
						else {
							location = 1;
						}
					}
					else {
						// point lies outside
						if (ec > 1.0) {
							if (location == 1) {
								location = -1;
								++crossings;
							}
						}						
						// point lies inside
						else {
							if (location == -1) {
								location = 1;
								++crossings;
							}							
						}
					}
					
					if (ec > 1.0 && ec < 4) {
						outPoint[j] = true;
						
						// search for the line point closest to the contour
						double md;
						try {
							md = ell.getDistanceEuclideanPointToEllipse(p);
							if (md < minContourDist ) {
								minContourDist = md; 	
								closestPointEllipse = ell.getClosestPointOnEllipse(p);
								closestPointLine =  p;
								closestPointLineIndex = j;
							}
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} 
						
					}
					else {
						outPoint[j] = false;
					}
				}

				// skip lines "going through", i.e. potential reflections
				if (this.useMultiIntersectionCheck && crossings >= 2)
					continue;

				if (closestPointLineIndex == -1 || minContourDist > 4)
					// line segment too far away
					continue;
				
//				System.out.println();
//				System.out.println("=================================");
//				System.out.println("CP line index = " + closestPointLineIndex);
//				System.out.println("CPL = " + closestPointLine);
//				System.out.println("CPE = " + closestPointEllipse);
				
				if (this.showAdditionalResults) {
					int cplx = (int)closestPointLine.x;
					int cply = (int)closestPointLine.y;
					int cpex = (int)closestPointEllipse.x;
					int cpey = (int)closestPointEllipse.y;
					if (cplx >= 0 && cplx < this.resultImgIntermediate.getSizeX() && cply >= 0 && cply < this.resultImgIntermediate.getSizeY()) {
						this.resultImgIntermediate.putValueR(cplx, cply, 0); 
						this.resultImgIntermediate.putValueG(cplx, cply, 0); 
						this.resultImgIntermediate.putValueB(cplx, cply, 255);
					}
					if (   cpex >= 0 && cpex < this.resultImgIntermediate.getSizeX() 
							&& cpey >= 0 && cpey < this.resultImgIntermediate.getSizeY()) {
						this.resultImgIntermediate.putValueR(cpex, cpey, 0); 
						this.resultImgIntermediate.putValueG(cpex, cpey, 0); 
						this.resultImgIntermediate.putValueB(cpex, cpey, 255);
					}
				}
				
				// check length of runs in both directions
				int r_rb = -1;
				int l_rb = -1;
				boolean a_run = false;
				boolean d_run = false;
				
				// ascend line
				int dj = closestPointLineIndex + 1;
				run = 1;
				while (dj < np && outPoint[dj]) {
					++run;
					++dj;
				}
				if (run >= 3) {
					a_run = true;
				}
				
				// descend line
				dj = closestPointLineIndex - 1;
				run = 1;
				while (dj >= 0 && outPoint[dj]) {
					++run;
					--dj;
				}
				if (run >= 3) {
					d_run = true;
				}
				
				boolean angleOK = false;
				if (this.useAngleCriterion) {
					double tangentOrientation = Double.NaN;
					double stromuliOrientation = Double.NaN;
					if (a_run || d_run) {

						// calculate tangent direction at closest point
						tangentOrientation = 
								ell.getTangentOrientation(closestPointEllipse);

						double ax=0, ay=0, dx=0, dy=0;
						if (a_run && d_run) {
							ax = xc[closestPointLineIndex+2];
							ay = yc[closestPointLineIndex+2];
							dx = xc[closestPointLineIndex-2];
							dy = yc[closestPointLineIndex-2];
						}
						else if (a_run) {
							ax = xc[closestPointLineIndex+2];
							ay = yc[closestPointLineIndex+2];
							dx = xc[closestPointLineIndex];
							dy = yc[closestPointLineIndex];
						}
						else if (d_run) {
							ax = xc[closestPointLineIndex];
							ay = yc[closestPointLineIndex];
							dx = xc[closestPointLineIndex-2];
							dy = yc[closestPointLineIndex-2];
						}

						stromuliOrientation = 
								Math.toDegrees(Math.atan2(ay-dy,ax-dx)) + 180;
						stromuliOrientation = 
								(stromuliOrientation > 180) ? 
										stromuliOrientation - 180 : stromuliOrientation;

//						System.out.println("LA = " + stromuliOrientation);
//						System.out.println("TA = " + tangentOrientation);

						// check if line and tangent are more or less orthogonal 
						if (  Math.abs(tangentOrientation - stromuliOrientation) 
								> this.stromuliAngleThreshold) {
//							System.out.println("Diff = " + Math.abs(tangentOrientation - stromuliOrientation));
							angleOK = true;
						}
					}
				}

				boolean distanceOK = false;
				if (this.useEllipseDistThreshold) {
					
					Point2D.Double sp = new Point2D.Double(ex, ey);
					Point2D.Double ep = new Point2D.Double(eex, eey);
					double maxDist = this.ellipseDistThresh;
				
//					double ellipseRatio = 2.0/3.0;
//					if (b/a > ellipseRatio) { 
//						double aDist = (b/a - ellipseRatio) / ellipseRatio 
//								* (Math.sqrt(a*a + b*b) - 3 ) + 3;
//						if(aDist > maxDist) 
//							maxDist = aDist;
//					}
//				
////				System.out.println("\t -> maxDist = " + maxDist + " , a = " + a + " , b = " + b);
//				
////				double minDist = Double.MAX_VALUE;
				
					for (int j=0; j<np && !stromuli; j++) {

						if (outPoint[j]) {
							px = xc[j];
							py = yc[j];
						
						// calculate distance to ellipse end-points
//						if (sp.distance(px, py) < minDist)
//							minDist = sp.distance(px, py);
//						if (ep.distance(px, py) < minDist)
//							minDist = ep.distance(px, py);
						
							if (   sp.distance(px, py) < maxDist 
									|| ep.distance(px, py) < maxDist) {
							
//							System.out.println("\t\t => found " + px + " , " + py + " = " 
//									+ sp.distance(px, py) + " / " + ep.distance(px, py));

								// determine length of run: 
								// as there is no fixed point order check both directions
								dj = j+1;
								run = 1;
								while (dj < np && outPoint[dj]) {
									++run;
									++dj;
								}
								if (run >= 3) {
									distanceOK = true;
								}
								else {
									dj = j-1;
									run = 1;
									while (dj >= 0 && outPoint[dj]) {
										++run;
										--dj;
									}
									if (run >= 3)
										distanceOK = true;								
								}
							}
						}
					}
				}
				
				if (this.useAngleCriterion && this.useEllipseDistThreshold) {
					if (angleOK && distanceOK)
						stromuli = true;
				}
				else if (this.useAngleCriterion) {
					if (angleOK)
						stromuli = true;
				}
				else if (this.useEllipseDistThreshold) {
					if (distanceOK)
						stromuli = true;
				}

				stromuliRegion = stromuliRegion || stromuli;
				
				if (stromuli) {
					stromulis.add(l);
					stromuliOutpoints.add(outPoint);
				}
				else {
					if (this.showAdditionalResults) {
//						for (int j=0; j<np; j++) {
//							px = xc[j];
//							py = yc[j];
//							if (outPoint[j] && this.resultImgIntermediate.getValueB((int)px, (int)py) != 255) {
//								this.resultImgIntermediate.putValueR((int)px, (int)py, 255);
//								this.resultImgIntermediate.putValueG((int)px, (int)py, 0);
//								this.resultImgIntermediate.putValueB((int)px, (int)py, 0);
//							}
//							else if (this.resultImgIntermediate.getValueB((int)px, (int)py) != 255) {
//								this.resultImgIntermediate.putValueR((int)px, (int)py, 255);
//								this.resultImgIntermediate.putValueG((int)px, (int)py, 0);
//								this.resultImgIntermediate.putValueB((int)px, (int)py, 0);							
//							}
//						}
						int red = (((255 & 0xff) << 16)	+ ((0 & 0xff) << 8) + (0 & 0xff));

						px = xc[0];
						py = yc[0];
						if (this.resultImgIntermediate.getValueB((int)px, (int)py) != 255) {
							this.resultImgIntermediate.putValueR((int)px, (int)py, 255);
							this.resultImgIntermediate.putValueG((int)px, (int)py, 0);
							this.resultImgIntermediate.putValueB((int)px, (int)py, 0);
						}
						for (int j=1; j<np; j++) {
							int ppx = (int)xc[j-1];
							int ppy = (int)yc[j-1];
							px = xc[j];
							py = yc[j];
							if (this.resultImgIntermediate.getValueB((int)px, (int)py) != 255) {
								this.resultImgIntermediate.drawLine2D(ppx, ppy, (int)px, (int)py, 
										0, 0, 0, red);
							}						
						}
					}
				}

			} // all lines
			
			// draw ellipse here...
//			cx = this.comXs[regionIndex-1];
//			cy = this.comYs[regionIndex-1];
//			ex = (int)(cx + 0.5*this.maxAxisLengths[regionIndex-1] * Math.cos(this.orient[regionIndex-1]));
//			ey = (int)(cy + 0.5*this.maxAxisLengths[regionIndex-1] * Math.sin(this.orient[regionIndex-1]));
//			eex = (int)(cx - 0.5*this.maxAxisLengths[regionIndex-1] * Math.cos(this.orient[regionIndex-1]));
//			eey = (int)(cy - 0.5*this.maxAxisLengths[regionIndex-1] * Math.sin(this.orient[regionIndex-1]));
			int nx = (int)(cx - 0.5*this.minAxisLengths[regionIndex-1] * Math.sin(this.orient[regionIndex-1]));
			int ny = (int)(cy + 0.5*this.minAxisLengths[regionIndex-1] * Math.cos(this.orient[regionIndex-1]));
//			res.drawLine2D((int)cx, (int)cy, ex, ey, 0xffffff);
//			res.drawLine2D((int)cx, (int)cy, nx, ny, 0xffffff);
					
			if (this.showAdditionalResults) {
				
				if (stromuliRegion) {
					de.setInputImage(this.resultImgIntermediate);
					de.setEllipse(ell);
					de.setColor(Color.GREEN);
					de.runOp();
					this.resultImgIntermediate = (MTBImageRGB)de.getResultImage();
				}

				if (ex >= 0 && ex < this.resultImgIntermediate.getSizeX() && ey >= 0 && ey < this.resultImgIntermediate.getSizeY()) {
					this.resultImgIntermediate.putValueR(ex, ey, 251); 
					this.resultImgIntermediate.putValueG(ex, ey, 147); 
					this.resultImgIntermediate.putValueB(ex, ey,  13);
				}
				if (   eex >= 0 && eex < this.resultImgIntermediate.getSizeX() 
						&& eey >= 0 && eey < this.resultImgIntermediate.getSizeY()) {
					this.resultImgIntermediate.putValueR(eex, eey, 251); 
					this.resultImgIntermediate.putValueG(eex, eey, 147); 
					this.resultImgIntermediate.putValueB(eex, eey,  13);
				}

				// draw stromulis
				int magenta = (((255 & 0xff) << 16)	+ ((0 & 0xff) << 8) + (255 & 0xff));
				int yellow = (((255 & 0xff) << 16)	+ ((255 & 0xff) << 8) + (0 & 0xff));
				for (int i=0;i<stromulis.size(); ++i) {

					Line line = stromulis.get(i);
					boolean[] outPoint = stromuliOutpoints.get(i);
					np = line.getNumber();
					xc = line.getXCoordinates();
					yc = line.getYCoordinates();

					px = xc[0];
					py = yc[0];
					if (outPoint[0]) {
						this.resultImgIntermediate.putValueR((int)px, (int)py, 255);
						this.resultImgIntermediate.putValueG((int)px, (int)py, 0);
						this.resultImgIntermediate.putValueB((int)px, (int)py, 255);
					}						
					else {
						this.resultImgIntermediate.putValueR((int)px, (int)py, 255);
						this.resultImgIntermediate.putValueG((int)px, (int)py, 255);
						this.resultImgIntermediate.putValueB((int)px, (int)py, 0);							
					}
					for (int j=1; j<np; j++) {
						int ppx = (int)xc[j-1];
						int ppy = (int)yc[j-1];
						px = xc[j];
						py = yc[j];
						if (outPoint[j]) {
							this.resultImgIntermediate.drawLine2D(ppx, ppy, (int)px, (int)py, 
									0, 0, 0, magenta);
						}						
						else {
							this.resultImgIntermediate.drawLine2D(ppx, ppy, (int)px, (int)py, 
									0, 0, 0, yellow);
						}
					}
				}
			}

			if (stromuliRegion)
				this.identifiedRegions.add(this.candidateRegions.get(regionIndex-1));
			
		} // each region
		
		msg = opIdentifier + " -> region processing completed.";	
		this.notifyListeners(new StatusEvent(msg));

		if (this.showAdditionalResults) {
			this.resultImgIntermediate.setTitle("Intermediate result image: stromuli criteria checks");
			this.resultImgIntermediate.show();
		}
	}
	
	private void detectStromuliStatisticalModel(MTBImageShort roiImg,
			MTBImage result_stack, double[] comXs, double[] comYs, double[] orient) {

		/* BM start */
		
		System.out.println("Clipping image...");

		// set pixels outside plastid regions and negative filter responses to zero
		for (int i=0; i<result_stack.getSizeC(); ++i) {
			for (int y=0; y<this.ySize; ++y) {
				for (int x=0; x<this.xSize; ++x) {
					if (roiImg.getValueInt(x, y) == 0) { // || this.plastidMask.getValueInt(x, y) > 0) {
						result_stack.putValueInt(x, y, 0, 0, i, 0);
					}
					else {
						if (result_stack.getValueDouble(x, y, 0, 0, i) < 0)
							result_stack.putValueDouble(x, y, 0, 0, i, 0);
					}
				}
			}
		}

		double stackMin = result_stack.getMinMaxDouble()[0]; 	
		double stackMax = result_stack.getMinMaxDouble()[1]; 
		
		System.out.println("min = " + stackMin + " , max = " + stackMax);
		
		
//		MTBImageByte dirImage = (MTBImageByte)MTBImage.createMTBImage(
//				xSize, ySize, zSize, tSize, cSize, MTBImageType.MTB_BYTE);

		double weightSigma = 0.5;
		double[] weights = new double[(int)(2*weightSigma) + 1];
		for (int s=0; s<weights.length; ++s) {
			weights[s] = 1.0 / (Math.sqrt(2.0*Math.PI) * weightSigma) 
					* Math.exp(-s*s/(2.0*weightSigma*weightSigma));
		}
		double sum = 0;
		for (int s=0; s<weights.length; ++s) {
			sum += weights[s];
		}
		for (int s=0; s<weights.length; ++s) {
			weights[s] = weights[s] / sum;
		}

		System.out.println("Getting probs...");

//		for (int y=0; y<this.ySize; ++y) {
//			for (int x=0; x<this.xSize; ++x) {
//				
//				if (roiImg.getValueInt(x, y) == 0)
//					continue;
//
//				sum = 0;
//				for (int i=0; i<result_stack.getSizeC(); ++i) {
//					result_stack.putValueDouble(x, y, 0, 0, i, 
//							Math.exp(-result_stack.getValueDouble(x, y, 0, 0, i)));
//					sum += Math.exp(-result_stack.getValueDouble(x, y, 0, 0, i));
//				}
//				for (int i=0; i<result_stack.getSizeC(); ++i) {
//					result_stack.putValueDouble(x, y, 0, 0, i, 
//							result_stack.getValueDouble(x, y, 0, 0, i) / sum);
//				}
//			}
//		}

		for (int i=0; i<result_stack.getSizeC(); ++i) {
			for (int y=0; y<this.ySize; ++y) {
				for (int x=0; x<this.xSize; ++x) {

					if (roiImg.getValueInt(x, y) == 0)
						continue;

					result_stack.putValueDouble(x, y, 0, 0, i, 
							result_stack.getValueDouble(x, y, 0, 0, i) / stackMax);
				}
			}
		}

//		System.out.println("After probs: min = " + result_stack.getMinMaxDouble()[0] + " , max = " + result_stack.getMinMaxDouble()[1]);

		System.out.println("Applying weighting...");
		
		for (int y=0; y<this.ySize; ++y) {
			for (int x=0; x<this.xSize; ++x) {

				if (roiImg.getValueInt(x, y) == 0)
					continue;

				try {
				
				// TODO re-implement weighting scheme

				// calculate orientation to plastid center point closest to 
				// stromuli, directions lie between 0 and 180 degree
				int label = roiImg.getValueInt(x, y);
				double comX = comXs[label-1];
				double comY = comYs[label-1];
				double angleRad = Math.atan2((y-comY),(x-comX));

				// main axis orientation
				angleRad = orient[label-1];

				int ex = (int)(comX + 10.0 * Math.cos(angleRad));
				int ey = (int)(comY + 10.0 * Math.sin(angleRad));

				Line2D l = new Line2D.Double(comX, comY, ex, ey);
				double d = l.ptLineDist(x, y);
				if (d > 2) {
					for (int i=0; i<result_stack.getSizeC(); ++i)
						result_stack.putValueInt(x, y, 0, 0, i, 0);
				}

				if (angleRad < 0)
					angleRad += 2*Math.PI;
				double angleDeg = 180.0/Math.PI * angleRad;
				if (angleDeg >= 180)
					angleDeg -= 180;


				//					dirImage.putValueInt(x, y, (int)angleDeg);

				// find best-matching direction
				double deg2 = this.degSampling / 2;
				int degBins = 180/this.degSampling;
				int refDir = ((int)((angleDeg+deg2)/this.degSampling) 
						+ 180/this.degSampling/2)%degBins;

//				System.out.println(x + " , " + y + " -> " + label + " : " + comX + " , " + comY + " , a = " + angleDeg + ", r=" + (refDir*degSampling+90));

				// iterate over all directions in the result stack,
				// i = 0 refers to structures with orientation 90 degrees
//				for (int i=0; i<degBins; ++i) {
//					int weightID = Math.abs(i-refDir);
//					if ( weightID >= weights.length )
////							|| result_stack.getValueDouble(x, y, 0, 0, i) < 0)
//						result_stack.putValueDouble(x, y, 0, 0, i, 0);
//					else
//						result_stack.putValueDouble(x, y, 0, 0, i, 
//								result_stack.getValueDouble(x, y, 0, 0, i) 
//								* weights[weightID]);
//				}
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
		}
		
		System.out.println("After weights: min = " + result_stack.getMinMaxDouble()[0] + " , max = " + result_stack.getMinMaxDouble()[1]);

		System.out.println("Showing result stack...");
		
//		dirImage.setTitle("Direction image");
//		dirImage.show();
		result_stack.show();
		MTBImageDouble res = (MTBImageDouble)MTBImage.createMTBImage(
			this.xSize, this.ySize, this.zSize, this.tSize, this.cSize, 
				MTBImageType.MTB_DOUBLE);
		for (int y=0; y<this.ySize; ++y) {
			for (int x=0; x<this.xSize; ++x) {
				
				if (roiImg.getValueInt(x, y) == 0)
					continue;

				double max = 0;
				sum = 0;
				for (int i=0; i<result_stack.getSizeC(); ++i) {
					// max
//					if (result_stack.getValueDouble(x, y, 0, 0, i) > max)
//						max = result_stack.getValueDouble(x, y, 0, 0, i);
					// sum
					sum += result_stack.getValueDouble(x, y, 0, 0, i);
				}
//				System.out.println(sum);
				res.putValueDouble(x, y, sum);
			}
		}
//		for (int i=0;i<comXs.length;++i) {
//			int cx = (int)comXs[i];
//			int cy = (int)comYs[i];
//			int ex = (int)(cx + 10.0 * Math.cos(orient[i]));
//			int ey = (int)(cy + 10.0 * Math.sin(orient[i]));
//			for (int y=-1; y<=1; ++y) {
//				for (int x=-1; x<=1; ++x) {
//					int dx = (int)comXs[i] + x;
//					int dy = (int)comYs[i] + y;
//					if (dx<0 || dx>=this.xSize || dy<0 || dy>=this.ySize)
//						continue;
//					res.putValueInt(dx, dy, i+1);
//				}
//			}
//			res.drawLine2D(cx, cy, ex, ey, i+1);
//		}
		res.setTitle("Stromuli result image");
		res.show();
//		this.stromuliRegions = null;
	}
	
	/**
	 * Stromuli detection method based on analyzing the morphological shape
	 * of the joined region of plastid and stromuli.
	 * 
	 * @param maxFilterResult						Image of maximal vessel filter responses.
	 * @throws ALDOperatorException				Thrown in case of failure.
	 * @throws ALDProcessingDAGException	Thrown in case of failure.
	 */
	private void detectStromuliMorphology(MTBImage maxFilterResult) 
			throws ALDOperatorException, ALDProcessingDAGException {

		MTBImageByte plastidStromuliImg = (MTBImageByte)MTBImage.createMTBImage(
				this.xSize, this.ySize, this.zSize, this.tSize,	this.cSize, 
					MTBImageType.MTB_BYTE);

		//2.1 Niblack-Filter auf Vesselbild
		
		//MTBImage firstslice = vessel_filter.getResponseStack().getCurrentSlice();
		ImgThreshNiblack nibl = new ImgThreshNiblack();
		
		//nibl.setInputImage(firstslice);
		//outImg3 = vessel_filter.getResponseStack();
		
		MTBImage result_img = maxFilterResult;
		double mm[] = result_img.getMinMaxDouble();
		for (int y=0; y<result_img.getSizeY(); ++y) {
			for (int x=0; x<result_img.getSizeX(); ++x) {
				result_img.putValueDouble(x, y, 
						result_img.getValueDouble(x, y) * 255.0/mm[1]);
			}
		}
		
		nibl.setInputImage(result_img);
		
		nibl.setParameter("processMode",
				ImgThreshNiblack.Mode.valueOf("STD_LOCVARCHECK"));
		nibl.setParameter("enhanceR", new Double(-1.0));
		nibl.setParameter("scalingK", new Double(0.5));
		nibl.setParameter("winSize", new Integer(25));
		nibl.setParameter("varCheckNB", new Integer(20));
		nibl.setParameter("varCheckThresh", new Double(23));
		
		nibl.runOp();
		MTBImageByte vessels = nibl.getResultImage();
		vessels.setTitle("Vessel-Detektion");
//		vessels.show();
		
		//2.2 Erstellung Vessel_Regions
		LabelComponentsSequential labler = new LabelComponentsSequential(vessels,true);
		labler.runOp();
		MTBImageInt vessels_regions = (MTBImageInt)labler.getLabelImage();
//		MTBRegion2DSet vessels_RSet = labler.getResultingRegions();
		
		//3.0 Verschmelzung 2.2 mit 1.1
		boolean [] greenlight = 
				new boolean [vessels.getSizeX()*vessels.getSizeY()];
			for (int y=0; y<vessels.getSizeY(); ++y) 
			{
				for (int x=0; x<vessels.getSizeX(); ++x) 
				{
					if (this.plastidMask.getValueInt(x, y)==255)
					{
						plastidStromuliImg.putValueInt(x, y, 255);
//						if (vessels_regions.getValueInt(x, y)!=0) 
//							greenlight[vessels_regions.getValueInt(x, y)]=true;
					}
					else plastidStromuliImg.putValueInt(x, y, 0);
					if (this.plastidMask.getValueInt(x, y)==255)
					{
						if (vessels_regions.getValueInt(x, y)!=0) 
							greenlight[vessels_regions.getValueInt(x, y)]=true;
					}
				}
			}
			for (int y=0; y<vessels.getSizeY(); ++y) 
			{
				for (int x=0; x<vessels.getSizeX(); ++x) 
				{
					if (greenlight[vessels_regions.getValueInt(x, y)])
					{
						plastidStromuliImg.putValueInt(x, y, 255);
						
					}
				}
			}
			
//			plastidStromuliImg.setTitle("Plastids with Stromuli candidates");
//			plastidStromuliImg.show();
				
			// check which regions have got additional pixels, these
			// are potential regions with stromuli
			
		//4.0 Ermittlung von potentiellen Stromulis
		LabelComponentsSequential labler2 = new LabelComponentsSequential(plastidStromuliImg,true);
		labler2.runOp();
		MTBImage strom_regions = labler2.getLabelImage();
//		ArrayList <Double> norm_maxDis = new ArrayList<Double> ();
		MTBRegion2DSet regionlist_s = new MTBRegion2DSet(strom_regions,0);
		
		//4.1 Verbesserung über Size
		MTBRegion2DSet final_regions = new MTBRegion2DSet();
		
//		ArrayList <Double> n_sizer = new ArrayList<Double> ();
		int mw_points =0;
		double var_points =0;
		for ( int z = 0; z < regionlist_s.size();z++)
		{
			mw_points+=regionlist_s.get(z).getPoints().size();
		}
		mw_points = mw_points/regionlist_s.size();
		for ( int z = 0; z < regionlist_s.size();z++)
		{
			var_points +=Math.pow(regionlist_s.get(z).getPoints().size()-mw_points,2);
		}
		var_points = var_points/regionlist_s.size();
		for ( int z = 0; z < regionlist_s.size();z++)
		{
			if (regionlist_s.get(z).getPoints().size() <= 0)
			{
				continue;
			}
			double size_stud= (regionlist_s.get(z).getPoints().size() - mw_points)/Math.sqrt(var_points);
			if (size_stud >=2) continue;
			// BM: check if region has grown
			int newPixelCount = 0;
			for (Point2D.Double p : regionlist_s.get(z).getPoints()) {
				if (this.plastidMask.getValueInt((int)p.x, (int)p.y) == 0) {
					// region has grown							
					++newPixelCount;
				}
			}
			if (newPixelCount >= 4)
				final_regions.add(regionlist_s.get(z));
			
		}
		
		/* 
		 * below: additional checks by analyzing projection histogram;
		 * 				exact methodology is unclear and probably errorprone... 
		 */
		
		//5.0 Projektion
//		ArrayList <MTBImageHistogram> plastidhistos = new ArrayList<MTBImageHistogram >();
//		for ( int z = 0; z < final_regions.size();z++)
//		{
//			
//			
//			
//			Point2D.Double center2 = new Point2D.Double (final_regions.get(z).getCenterOfMass_X(),final_regions.get(z).getCenterOfMass_Y());
//			double winkel =(final_regions.get(z).getOrientation());
//
//			System.out.println("Region: #### " + z + " ##### " + winkel + " #### " + "( " + center2.getX() + " , " + center2.getY() + " | "+ final_regions.get(z).getMajorAxisLength()+" )");
//
//			ArrayList <Integer> gamma_list = new ArrayList<Integer> ();
//			for (int u =0; u < final_regions.get(z).getPoints().size(); u++)
//			{
//				Point2D.Double cpoint =final_regions.get(z).getPoints().get(u);
//				double gamma =0.0;
//				double epsi = 0.0; 
//	            double a1 = 0, b1 = 0, c1 = 0;
//	            double a2 = 0, b2 = 0, c2 = 0;
//	            double D=0, D1=0, D2=0;
//	            
//	            
//	               
//	                a1 = Math.cos(winkel);
//	                b1 = - Math.sin(winkel);
//	                c1 = cpoint.getX()- center2.getX();
//	                
//	                
//	                a2 =  Math.sin(winkel);
//	                b2 =  Math.cos(winkel);
//	                c2 = cpoint.getY()-center2.getY();
//	                
//	                //Verarbeitungen:
//	                D = a1 * b2 - a2 * b1; 
//	                
//	                //Selektion und Ausgabe:
//	                if (D == 0)
//	                {
//	                        System.out.println("Keine reelle Loesung moeglich!");
//	                }
//	                else
//	                {
//	                        D1 = c1 * b2 - c2 * b1; D2 = a1 * c2 - a2 * c1;
//	                                        gamma = D1/D;   epsi= D2/D;
//	
//	                }
//	                
//	               
//	                int X_wert= (int) Math.round(center2.getX()+ Math.cos(winkel) * gamma);
//	                int Y_wert = (int) Math.round(center2.getY()+ Math.sin(winkel) * gamma);
//	                //System.out.println("( " + (X_wert - (int)bbox[0]) + " , " + (Y_wert - (int)bbox[1])+ " )");
//	                gamma_list.add((int) Math.round(gamma));
//	              
//	                
//			}
//			Collections.sort(gamma_list);
//			int last_val=gamma_list.get(gamma_list.size()-1);
//			int first_val=gamma_list.get(0);
//			int val_length =Math.abs(last_val)+Math.abs(first_val);
//			int shift = Math.abs(first_val);
//			
//			double vals [] = new double[val_length+1];
//			for ( int l1: gamma_list)
//			{
//				vals[l1+shift] = vals[l1+shift]+1;
//				
//			}
//			int counter =0;
//			if (vals.length <= 1) plastidhistos.add(null);
//			else
//			{
//				MTBImageHistogram a= new MTBImageHistogram(vals, vals.length, 0, vals.length-1);
//				for (int l = 0; l<vals.length; l++)
//				{
//					a.setBinValue(l, vals[l]);
//					//System.out.println(l+ " : "+vals[l]);
//					counter++;
//				}
//				plastidhistos.add(a);
//			}
//	
//			
//            
//			
//			
//			
//		}
//		
		//6 Auswertung Histogramme
//		int reg_count =  0;
//		boolean [] stromu_green = new boolean [plastidhistos.size()];
//		for (MTBImageHistogram b:plastidhistos)
//		{
//			if (b == null) {reg_count++; continue;}
//			else
//			{
//			MTBImageHistogram dummy = b;
//			CalcGlobalThreshOtsu calcThres = new CalcGlobalThreshOtsu(dummy);
//			calcThres.runOp();
//			int thres = (int)calcThres.getOtsuThreshold().getValue().doubleValue();
//			double class_sum1 =0;
//			double class_sum2= 0;
//			int max_pos_c1 =thres;
//			int max_pos_c2 = -1;
//			double max_c1 =-1.0;
//			double max_c2 =-1.0;
//			for (int lz1 = 0; lz1<=thres; lz1++)
//			{
//				if (b.getBinValue(lz1) > max_c1) 
//				{
//					max_c1 = b.getBinValue(lz1); 
//					if (max_pos_c1 < max_pos_c1) max_pos_c1 = lz1;
//				}
//				class_sum1= class_sum1 + b.getBinValue(lz1);
//				
//			}
//			for (int lz2 = thres+1; lz2<b.getSize(); lz2++)
//			{
//				if (b.getBinValue(lz2) > max_c2) 
//				{
//					max_c2 = b.getBinValue(lz2); 
//					if (max_pos_c2 > max_pos_c2) max_pos_c2 = lz2;
//				}
//				class_sum2= class_sum2 + b.getBinValue(lz2);
//			}
//			double class_mean1 =class_sum1/thres;
//			double class_mean2 =class_sum2/(b.getSize()-thres);
//			int distance_maxps = -1;
//			distance_maxps = Math.abs(max_pos_c2 - max_pos_c1);
//			
//			System.out.println(reg_count+" |||Class_means: Class1= " +class_mean1 + " | Class2=" + class_mean2 + "|Distance=|"+ distance_maxps + "|max"+ max_c1+ ","+max_c2);
//			double mean_thres = Math.abs(class_mean1 - class_mean2);
//			double max_thres = Math.abs(max_c1 - max_c2);
//			if (mean_thres >= thres_mittel) stromu_green[reg_count] = true;
//			if (max_thres <= thres_max) stromu_green[reg_count] = false;
//			
//			reg_count++;
//			
//			}
//			
//		}
		//7 Ausgabe
		
//		for (int j1= 0; j1<inImg.getSizeX();j1++)
//		{
//			for (int j2=0;j2<inImg.getSizeY();j2++)
//			{
//				outImg2.putValueInt(j1, j2, inImg.getValueInt(j1, j2));
//			}
//			
//			
//		}
		
		// visualize final stromuli regions
		for(int k = 0; k<final_regions.size();k++) {
			for (int v= 0;v<final_regions.get(k).getPoints().size();v++) {
				this.resultLabelImage.putValueInt(
						(int)final_regions.get(k).getPoints().get(v).getX(), 
						(int)final_regions.get(k).getPoints().get(v).getY(), k);
			}
		}
		
		// copy final stromuli regions to output region set
		double xMax = 0.0;
		double yMax = 0.0;
		Vector<MTBRegion2D> stromulis = new Vector<MTBRegion2D>();
		for(int k = 0; k<final_regions.size();k++) {
			MTBRegion2D reg = final_regions.get(k);
			double[] minmax = reg.getMinMaxCoordinates();
			if (minmax[2] > xMax)
				xMax = minmax[2];
			if (minmax[3] > yMax)
				yMax = minmax[3];
			stromulis.add(reg);
		}
		
		// set resulting region set as value of operator's output parameter
		this.stromuliRegions = 
				new MTBRegion2DSet(stromulis, 0, 0, xMax, yMax);
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

}

