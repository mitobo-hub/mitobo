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

package de.unihalle.informatik.MiToBo.features;

import ij.ImagePlus;
import ij.gui.NewImage;
import ij.process.ImageProcessor;

import java.awt.Polygon;
import java.awt.geom.Point2D;
import java.text.NumberFormat;
import java.util.Vector;

import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.Alida.annotations.Parameter.ParameterModificationMode;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.Alida.operator.events.ALDOperatorExecutionProgressEvent;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBContour2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2D;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageRGB;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;
import de.unihalle.informatik.MiToBo.features.contours.Contour2DConcavityCalculator;
import de.unihalle.informatik.MiToBo.features.contours.Contour2DCurvatureCalculator;
import de.unihalle.informatik.MiToBo.features.regions.Region2DSkeletonAnalyzer;
import de.unihalle.informatik.MiToBo.gui.MTBTableModel;
import de.unihalle.informatik.MiToBo.math.arrays.filter.GaussFilterDouble1D;
import de.unihalle.informatik.MiToBo.morphology.ConvexHullExtraction;
import de.unihalle.informatik.MiToBo.morphology.ConvexHullExtraction.InputType;
import de.unihalle.informatik.MiToBo.segmentation.contours.extraction.ContourOnLabeledComponents;
import de.unihalle.informatik.MiToBo.segmentation.contours.extraction.ContourOnLabeledComponents.ContourType;
import de.unihalle.informatik.MiToBo.segmentation.regions.labeling.LabelAreasToRegions;

/**
 * Operator to extract shape and region features for given regions.
 * <p>
 * Note that the operator assumes squared pixels. If the pixels are not 
 * squared several feature values will not be calculated correctly.
 * 
 * @author Markus Glass
 * @author Birgit Moeller
 */
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.ALL, 
	level=Level.APPLICATION, allowBatchMode = false)
public class MorphologyAnalyzer2D extends MTBOperator
{
	/**
	 * Identifier string for this operator class.
	 */
	private static final String operatorID = "[MorphologyAnalyzer2D]";

	/**
	 * Set of region features calculated from segmented cell regions. 
	 */
	public static enum FeatureNames {
		/**
		 * Integer ID of region (for reference purposes). 
		 */
		Object,
		/**
		 * Area in pixels. 
		 */
		Area,
		/**
		 * Length of boundary.
		 */
		Perimeter,
		/**
		 * Length of region's major axis.
		 */
		Length,
		/**
		 * Length of region's minor axis.
		 */
		Width,
		/**
		 * Relation of area to circle of equal size.
		 */
		Circularity,
		/**
		 * Elongation based on ratio of major to minor axes lengths.
		 */
		Eccentricity,
		/**
		 * Ratio of convex hull area to cell area.
		 */
		Solidity,
		/**
		 * Average deviation in tangential angles compared to circle.
		 */
		MarginRoughness,
		/**
		 * Average concavity of boundary pixels.
		 */
		AvgConcavity,
		/**
		 * Standard deviation of region's concavities.
		 */
		StdDevConcavity,
		/**
		 * Area of convex hull in pixels.
		 */
		ConvexHullArea,
		/**
		 * Perimeter of convex hull, i.e., number of contour pixels of convex
		 * hull given 8-neighborhood along contour.
		 */
		ConvexHullPerimeter,
		/**
		 * Ratio of convex hull perimeter and region perimeter.
		 */
		ConvexHullConvexity,
		/**
		 * Same as circularity, but with perimeter of convex hull instead of
		 * region perimeter.
		 */
		ConvexHullRoundness,
		/**
		 * Number of skeleton branches.
		 */
		BranchCount,
		/**
		 * Average length of branches.
		 */
		AvgBranchLength,	
		/**
		 * Length of longest path in skeleton.
		 */
		LongestPathLength,
		/**
		 * Average distance of branch endpoints to background.
		 */
		AvgDistBranchEndpointsToBackground,
		/**
		 * Number of protrusions.
		 */
		NumberOfProtrusions,
		/**
		 * Area of region part not belonging to any of the detected protrusions.
		 */
		NonProtrusionArea,	
		/**
		 * Average length of detected protrusions.
		 */
		AvgLengthProtrusions,
		/**
		 * Average length of apical parts of detected protrusions.
		 */
		AvgLengthApicalProtrusions,
		/**
		 * Average length of basal parts of detected protrusions.
		 */
		AvgLengthBasalProtrusions,
		/**
		 * Average length of baselines of detected protrusions.
		 */
		AvgLengthBaselineProtrusions,
		/**
		 * Average length of the equators of detected protrusions.
		 */
		AvgLengthEquatorProtrusions,
		/**
		 * Average length of detected indentations.
		 */
		AvgLengthIndentations,
		/**
		 * Average length of apical parts of detected indentations.
		 */
		AvgLengthApicalIndentations,
		/**
		 * Average length of basal parts of detected indentations.
		 */
		AvgLengthBasalIndentations,
		/**
		 * Average length of baselines of detected indentations.
		 */
		AvgLengthBaselineIndentations,
		/**
		 * Average length of the equators of detected indentations.
		 */
		AvgLengthEquatorIndentations,
		/**
		 * Average of pairwise distances between indentation midpoints.
		 */
		AvgDistIndentationMidPoints,
		/**
		 * Minimum of all distances of the indentation midpoints to their 
		 * nearest neighbors, i.e. the distance between any of two midpoints
		 * is at least this value.
		 */
		MinCoreRegionWidth,
		/**
		 * Maximum of all distances of the indentation midpoint to their 
		 * nearest neighbors, i.e. no midpoint has a larger distance to its 
		 * closest neighbor than this value.
		 */
		MaxCoreRegionWidth,
		/**
		 * Radius of largest circle that completely fits into the region boundary.
		 * <p>
		 * The radius is defined as the Euclidean distance of the point farest away
		 * from the boundary to the corresponding boundary. The point is determined
		 * as the global maximum of a distance transformation of the region. 
		 */
		RadiusMaxInscribedEmptyCircle
	}

	@Parameter(label = "Label image", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "label image", dataIOOrder = 0,
				callback = "getCalibration", paramModificationMode = ParameterModificationMode.MODIFIES_INTERFACE)
	private transient MTBImage inLabelImg = null;	// labeled input image stack, corresponding objects in different frames must be labeled with the same value
	
	@Parameter(label = "Regions", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "input regions", dataIOOrder = 1)
	private MTBRegion2DSet inRegions = null;
	
	//analysis parameters
	@Parameter(label = "Pixel length", required = false, 
			direction = Parameter.Direction.IN, supplemental = false, 
			description = "Pixel length, note that we assume square pixels!", 
			dataIOOrder = 2)
	private Double deltaXY = new Double(1.0);
	
	@Parameter(label = "Unit in x/y", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "unit x/y", dataIOOrder = 4)
	private String unitXY = "pixel";
	
	// which features should be calculated
	@Parameter(label = "Calculate area", required = false, 
		direction = Parameter.Direction.IN, supplemental = false, 
		description = "should object's areas be calculated", 
		dataIOOrder = 5, callback = "callbackArea",
		paramModificationMode = ParameterModificationMode.MODIFIES_INTERFACE)
	private boolean calcArea = true;
	
	@Parameter(label = "Calculate perimeter", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "should object's perimeters be calculated", dataIOOrder = 6)
	private boolean calcPerimeter = true;
	
	@Parameter(label = "Calculate length and width", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "should object's length and width (fitting ellipse's major/minor axes length) be calculated", dataIOOrder = 7)
	private boolean calcLengthWidth = true;
	
	@Parameter(label = "Calculate circularity", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "should object's circularities be calculated", dataIOOrder = 8)
	private boolean calcCircularity = true;
	
	@Parameter(label = "Calculate eccentricity", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "should object's eccentricities be calculated", dataIOOrder = 9)
	private boolean calcEccentricity = true;
	
	/**
	 * Flag to turn on/off calculation of solidity.
	 */
	@Parameter(label = "Calculate solidity", required = false, 
		direction = Parameter.Direction.IN, supplemental = false, 
		description = "Should object's solidity be calculated?", 
		dataIOOrder = 10, callback = "callbackSolidity",
		paramModificationMode = ParameterModificationMode.MODIFIES_INTERFACE)
	private boolean calcSolidity = true;

	/**
	 * Flag to turn on/off calculation of margin roughness.
	 */
	@Parameter(label = "Calculate margin roughness", required = false, 
		direction = Parameter.Direction.IN, supplemental = false, 
		description = "If true margin roughness is calculated.", 
		dataIOOrder = 11, callback = "callbackCurvature",
		paramModificationMode = ParameterModificationMode.MODIFIES_INTERFACE)
	private boolean calcMarginRoughness = true;

	/**
	 * Flag to turn on/off analysis of protrusions and indentations.
	 */
	@Parameter(label = "Analyze protrusions and indentations", required = false, 
		direction = Parameter.Direction.IN, supplemental = false, 
		description = "If true protrusions/indentations are analyzed.",
		dataIOOrder = 12, callback = "callbackCurvature",
		paramModificationMode = ParameterModificationMode.MODIFIES_INTERFACE)
	private boolean analyzeProtrusionsIndentations = true;

	/**
	 * Threshold for minimal curvature.
	 */
	@Parameter(label = "    - Minimal curvature", 
		required = false, direction = Parameter.Direction.IN, 
		supplemental = false, dataIOOrder = 13,
		description = "Curvature below this value are handled as noise.") 
	private double minimalCurvature = 1.0;

	/**
	 * Standard deviation of Gaussian kernel in curvature smoothing.
	 */
	@Parameter(label = "    - Gaussian \u03C3 in curvature analysis", 
		required = false, direction = Parameter.Direction.IN, 
		supplemental = false, dataIOOrder = 14,
		description = "If true margin roughness is calculated.")
	private double gaussianSigma = 4.0;

	/**
	 * Minimal number of pixels a valid protrusion requires.
	 */
	@Parameter(label = "    - Minimal length of a protrusion section", 
		required = false, direction = Parameter.Direction.IN, 
		supplemental = false, dataIOOrder = 15,
		description = "Minimal pixel number a protrusion requires to be valid.")
	private int minProtrusionLength = 10;

	/**
	 * Minimal number of pixels a valid indentation requires.
	 * <p> 
	 * Note that too short protrusions are removed first, 
	 * and indentations are only checked afterwards.
	 */
	@Parameter(label = "    - Minimal length of an indentation section", 
		required = false, direction = Parameter.Direction.IN, 
		supplemental = false, dataIOOrder = 16,
		description = "Minimal pixel number an indentation requires to be valid.")
	private int minIndentationLength = 1;

	/**
	 * Flag to turn on/off calculation of skeleton branch features.
	 */
	@Parameter(label = "Calculate skeleton branch features", 
		required = false, dataIOOrder = 17,
		direction = Parameter.Direction.IN, supplemental = false, 
		description = "If true skeleton branches are analyzed.") 
	private boolean calcSkeletonBranchFeatures = true;

	/**
	 * Flag to turn on/off analysis of concavities.
	 */
	@Parameter(label = "Calculate concavity information", 
		required = false, direction = Parameter.Direction.IN, 
		supplemental = false, dataIOOrder = 18,
		description = "If true average concavity and standard deviation "
			+ "are calculated.", callback = "callbackConcavity",
		paramModificationMode = ParameterModificationMode.MODIFIES_INTERFACE)
	private boolean calcConcavityData = true;
	
	/**
	 * Masksize for concavity calculations.
	 * <p>
	 * Refer to operator {@link Contour2DConcavityCalculator} for details.
	 */
	@Parameter(label = "    - Concavity Masksize", 
		required = false, direction = Parameter.Direction.IN, 
		supplemental = false, dataIOOrder = 19,
		description = "Size of local mask in concavity calculations.") 
	private int concavityMaskSize = 11;

	/**
	 * Flag to turn on/off analysis of convex hull.
	 */
	@Parameter(label = "Calculate convex hull measures", 
		required = false, direction = Parameter.Direction.IN, 
		supplemental = false, dataIOOrder = 20,
		description = "If true some measures of the convex hull "
			+ "are calculated.")
	private boolean calcConvexHullMeasures = true;

	@Parameter(label = "Fractional digits", required = false, 
		direction = Parameter.Direction.IN, 
		supplemental = false, description = "fractional digits", 
		dataIOOrder = 21, mode=ExpertMode.ADVANCED)
	private Integer fracDigits = new Integer(3);
	
	/**
	 * Flag to enable drawing curvature information to info image.
	 */
	@Parameter(label = "Show curvature analysis info image?", 
		required = false, direction = Parameter.Direction.IN, 
		supplemental = true, dataIOOrder = 0,
		description = "If selected info image with curvature data is shown.") 
	private boolean createCurvatureInfoImage = false;	

	/**
	 * Info image for curvature analysis.
	 */
	@Parameter(label = "Curvature data image", 
		required = false, direction = Parameter.Direction.OUT, 
		supplemental = true, dataIOOrder = 10,
		description = "Curvature data image.") 
	private transient MTBImageRGB curvatureInfoImg = null;	
	
	/**
	 * Flag to enable drawing skeleton information to info image.
	 */
	@Parameter(label = "Show skeleton analysis info image?", 
		required = false, direction = Parameter.Direction.IN, 
		supplemental = true, dataIOOrder = 0,
		description = "If selected info image with skeleton data is shown.") 
	private boolean createSkeletonInfoImage = false;	

	/**
	 * Info image for skeleton analysis.
	 */
	@Parameter(label = "Skeleton data image", 
		required = false, direction = Parameter.Direction.OUT, 
		supplemental = true, dataIOOrder = 10,
		description = "Skeleton data image.") 
	private transient MTBImageRGB skeletonInfoImg = null;	

	// output parameter
	@Parameter(label = "results table", required = true, direction = Parameter.Direction.OUT, supplemental = false, description = "results table", dataIOOrder = 0)
	private MTBTableModel table = null;
	
	private NumberFormat nf = NumberFormat.getInstance();
	
	private int bgLabel = 0;			// label value for the background
			
	private Vector<Integer> labels;
	private Vector<Double> areas;
	private Vector<Double> perimeters;
	private Vector<Double> lengths;
	private Vector<Double> widths;
	private Vector<Double> circularities;
	private Vector<Double> eccentricities;
	private Vector<Double> solidities;
	private Vector<Double> marginRoughnessValues;
	private Vector<Double> avgConcavities;	
	private Vector<Double> stdDevConcavities;
	private Vector<Double> convexHullAreas;
	private Vector<Double> convexHullPerimeters;
	private Vector<Double> convexHullConvexities;
	private Vector<Double> convexHullRoundnessValues;
	private Vector<Double> branchCounts;
	private Vector<Double> avgBranchLengths;
	private Vector<Double> longestPathLengths;
	private Vector<Double> avgEndpointDistances;
	
	private Vector<Double> minCoreRegionWidths;
	private Vector<Double> maxCoreRegionWidths;
	private Vector<Double> radiiMaxInscribedEmptyCircles;

	private Vector<Integer> protrusionCounts;
	private Vector<Double> nonProtrusionAreas;
	
	private Vector<Double> avgProtrusionLengths;
	private Vector<Double> avgApicalProtrusionLengths;
	private Vector<Double> avgBasalProtrusionLengths;
	private Vector<Double> avgBaselineProtrusionLengths;
	private Vector<Double> avgEquatorProtrusionLengths;
	private Vector<Double> avgIndentationLengths;
	private Vector<Double> avgApicalIndentationLengths;
	private Vector<Double> avgBasalIndentationLengths;
	private Vector<Double> avgBaselineIndentationLengths;
	private Vector<Double> avgEquatorIndentationLengths;
	
	/**
	 * Vector with detailed result data on indentations and protrusions.
	 */
	private Vector<MorphologyAnalyzer2DInProData> inProDetails;
	
	private Vector<Double> avgLobeDepths;
	private Vector<Double> avgNeckDepths;
	
	private MTBRegion2DSet regions = null;
	private MTBImage labelImg = null;
	
	/**
	 * Width of currently processed image.
	 */
	private int width;
	/**
	 * Height of currently processed image.
	 */
	private int height;
	
	public MorphologyAnalyzer2D() throws ALDOperatorException
	{
		
	}
	
	/**
	 * constructor
	 * 
	 * @param labelImg input label image
	 */
	public MorphologyAnalyzer2D(MTBImage labelImg) throws ALDOperatorException
	{
		this.inLabelImg = labelImg;
	}
	
	
	public MorphologyAnalyzer2D(MTBRegion2DSet rs) throws ALDOperatorException
	{
		this.regions = rs;
	}
	
	
	@Override
	protected void operate() throws ALDOperatorException, ALDProcessingDAGException
	{
		this.fireOperatorExecutionProgressEvent(
				new ALDOperatorExecutionProgressEvent(this, operatorID 
					+ " initializing operator..."));

		// set internal regions and label image to null, necessary if operator 
		// is run multiple times
		this.regions = null;
		this.labelImg = null;

		this.nf.setMaximumFractionDigits(this.fracDigits.intValue());
		this.nf.setMinimumFractionDigits(this.fracDigits.intValue());
		this.nf.setGroupingUsed(false);
		
		if(this.inRegions == null)
		{
			if(this.inLabelImg != null)
			{
				this.regions = LabelAreasToRegions.getRegions(this.inLabelImg, this.bgLabel);
			}
			else
			{
				System.err.println(this.toString() + ": No input specified");
			}
		}
		else
		{
			this.regions = this.inRegions;
		}
		
		// in any case we will need a label image
		if (this.inLabelImg == null)
		{
			this.labelImg = MTBImage.createMTBImage(
					(int)(this.regions.getXmax()+0.5) + 1,
					(int)(this.regions.getYmax()+0.5) + 1, 1, 1, 1, 
					MTBImageType.MTB_SHORT);
			this.labelImg.fillBlack();
			int regionID = 1;
			for (MTBRegion2D r: this.regions) {
				r.setID(regionID);
				for (Point2D.Double p: r.getPoints()) {
					this.labelImg.putValueInt((int)p.x, (int)p.y, regionID);
				}
				++regionID;
			}
		}
		else
		{
			this.labelImg = this.inLabelImg;
		}
		
		this.width = this.labelImg.getSizeX();
		this.height = this.labelImg.getSizeY();
		
		// get objects' shape features
		getSimpleShapeFeatures();

		// create a results table
		makeTable();
	}
		
	/**
	 * calculate several shape features in one method
	 * @throws ALDProcessingDAGException 
	 * @throws ALDOperatorException 
	 */
	private void getSimpleShapeFeatures() 
			throws ALDOperatorException, ALDProcessingDAGException
	{
		double factor = this.deltaXY.doubleValue() * this.deltaXY.doubleValue();
		
		this.labels = new Vector<Integer>();
		this.areas = new Vector<Double>();
		this.perimeters = new Vector<Double>();
		this.lengths = new Vector<Double>();
		this.widths = new Vector<Double>();
		this.circularities = new Vector<Double>();
		this.eccentricities = new Vector<Double>();
		this.solidities = new Vector<Double>();
		this.marginRoughnessValues = new Vector<Double>();
		this.avgConcavities = new Vector<Double>();	
		this.stdDevConcavities = new Vector<Double>();
		this.convexHullAreas = new Vector<Double>();
		this.convexHullPerimeters = new Vector<Double>();
		this.convexHullConvexities = new Vector<Double>();
		this.convexHullRoundnessValues = new Vector<Double>();
		this.branchCounts = new Vector<Double>();
		this.avgBranchLengths = new Vector<Double>();
		this.longestPathLengths = new Vector<Double>();
		this.avgEndpointDistances = new Vector<Double>();
		
		this.minCoreRegionWidths = new Vector<Double>();
		this.maxCoreRegionWidths = new Vector<Double>();
		this.radiiMaxInscribedEmptyCircles = new Vector<>();

		this.protrusionCounts = new Vector<Integer>();
		this.nonProtrusionAreas = new Vector<Double>();

		this.avgProtrusionLengths = new Vector<Double>();
		this.avgApicalProtrusionLengths = new Vector<Double>();
		this.avgBasalProtrusionLengths = new Vector<Double>();
		this.avgBaselineProtrusionLengths = new Vector<Double>();
		this.avgEquatorProtrusionLengths = new Vector<Double>();
		this.avgIndentationLengths = new Vector<Double>();
		this.avgApicalIndentationLengths = new Vector<Double>();
		this.avgBasalIndentationLengths = new Vector<Double>();
		this.avgBaselineIndentationLengths = new Vector<Double>();
		this.avgEquatorIndentationLengths = new Vector<Double>();

		this.inProDetails = 
				new Vector<MorphologyAnalyzer2DInProData>();

		this.avgLobeDepths = new Vector<Double>();
		this.avgNeckDepths = new Vector<Double>();

		this.fireOperatorExecutionProgressEvent(
				new ALDOperatorExecutionProgressEvent(this, operatorID 
					+ " calculating global features..."));

		for(int j = 0; j < this.regions.size(); j++)
		{
			MTBRegion2D cr = this.regions.elementAt(j);
			
			int cid = cr.getID();
			this.labels.add(new Integer(cid));
			
			// area
			if(this.calcArea || this.calcSolidity || this.calcConvexHullMeasures)
			{
				double a = cr.getArea() * factor;
				this.areas.add(new Double(a));
			}
			
			// perimeter
			if(this.calcPerimeter || this.calcConvexHullMeasures)
			{
				double p = 0;
				
				try
				{
					p = cr.getContour().getContourLength() * this.deltaXY.doubleValue();
				} 
				catch(ALDOperatorException e1)
				{
					e1.printStackTrace();
				} 
				catch(ALDProcessingDAGException e1)
				{
					e1.printStackTrace();
				}	
				this.perimeters.add(p);
			}
			
			// length and width
			if(this.calcLengthWidth)
			{
				double l = cr.getMajorAxisLength() * this.deltaXY.doubleValue();
				this.lengths.add(l);
				l = cr.getMinorAxisLength() * this.deltaXY.doubleValue();
				this.widths.add(l);			
			}
			
			// circularity
			if(this.calcCircularity)
			{
				double c = 0;
				
				try
				{
					c = cr.getCircularity();
				}
				catch(ALDOperatorException e1)
				{
					e1.printStackTrace();
				} 
				catch(ALDProcessingDAGException e1)
				{
					e1.printStackTrace();
				}
				this.circularities.add(c);
			}
			
			// eccentricity
			if(this.calcEccentricity)
			{
				double e = cr.getEccentricity();
				this.eccentricities.add(e);
			}
		}
		
		this.fireOperatorExecutionProgressEvent(
				new ALDOperatorExecutionProgressEvent(this, operatorID 
					+ " performing curvature analysis..."));

		if (this.calcMarginRoughness || this.analyzeProtrusionsIndentations) 
		{
			this.analyzeLocalCurvatures();
		}
			
		this.fireOperatorExecutionProgressEvent(
				new ALDOperatorExecutionProgressEvent(this, operatorID 
					+ " extracting concavity data..."));

		if (this.calcConcavityData) 
		{
			this.calculateConcavityData();
		}

		this.fireOperatorExecutionProgressEvent(
				new ALDOperatorExecutionProgressEvent(this, operatorID 
					+ " calculating solidities..."));

		// measure based on convex hull
		if (this.calcConvexHullMeasures || this.calcSolidity) {
			this.calculateConvexHulls();
		}
		
		// solidities
		if(this.calcSolidity)
		{
			this.calculateSolidityValues();
		}
		
		// convex hull convexities and roundness values
		if (this.calcConvexHullMeasures) {
			this.calculateConvexHullMeasures();
		}
		
		this.fireOperatorExecutionProgressEvent(
				new ALDOperatorExecutionProgressEvent(this, operatorID 
					+ " analyzing region skeletons..."));

		if (this.calcSkeletonBranchFeatures) {
      // analyze region skeletons
			Region2DSkeletonAnalyzer skeletonOp = 
					new Region2DSkeletonAnalyzer();
			skeletonOp.setInputLabelImage(this.labelImg);
			skeletonOp.setPixelLength(this.deltaXY.doubleValue());
			skeletonOp.setVisualizeAnalysisResults(
					this.createSkeletonInfoImage);
			skeletonOp.runOp(HidingMode.HIDE_CHILDREN);
			MTBTableModel skeletonData = skeletonOp.getResultTable();
			if (this.createSkeletonInfoImage)
				this.skeletonInfoImg = skeletonOp.getAnalysisImage();
			int branchCountIndex = skeletonData.findColumn(
				Region2DSkeletonAnalyzer.FeatureNames.BranchCount.toString());
			int branchLengthIndex = skeletonData.findColumn(
				Region2DSkeletonAnalyzer.FeatureNames.AvgBranchLength.
					toString());
			int branchDistIndex = skeletonData.findColumn(
				Region2DSkeletonAnalyzer.FeatureNames.AvgBranchEndpointDistance.
					toString());
			int branchLongestLengthIndex = skeletonData.findColumn(
				Region2DSkeletonAnalyzer.FeatureNames.LongestSkeletonPathLength.
				  toString());
			int minCoreRegionWidthIndex = skeletonData.findColumn(
				Region2DSkeletonAnalyzer.FeatureNames.MinCoreRegionWidth.toString());
			int maxCoreRegionWidthIndex = skeletonData.findColumn(
				Region2DSkeletonAnalyzer.FeatureNames.MaxCoreRegionWidth.toString());
			int radiiMaxEmptyCircleIndex = skeletonData.findColumn(
					Region2DSkeletonAnalyzer.FeatureNames.RadiusMaxInscribedEmptyCircle.toString());
			for (int i = 0; i<skeletonData.getRowCount(); ++i) {
				this.branchCounts.add(Double.valueOf(
					(String)skeletonData.getValueAt(i, branchCountIndex)));
				this.avgBranchLengths.add(Double.valueOf(
					(String)skeletonData.getValueAt(i, branchLengthIndex)));
				this.avgEndpointDistances.add(Double.valueOf(
					(String)skeletonData.getValueAt(i, branchDistIndex)));
				this.longestPathLengths.add(Double.valueOf(
					(String)skeletonData.getValueAt(i, branchLongestLengthIndex)));
				this.minCoreRegionWidths.add(Double.valueOf(
					(String)skeletonData.getValueAt(i, minCoreRegionWidthIndex)));
				this.maxCoreRegionWidths.add(Double.valueOf(
					(String)skeletonData.getValueAt(i, maxCoreRegionWidthIndex)));
				this.radiiMaxInscribedEmptyCircles.add(Double.valueOf(
					(String)skeletonData.getValueAt(i, radiiMaxEmptyCircleIndex)));
			}
		}
	}
	
	/**
	 * create result table
	 */
	private void makeTable()
	{
		// initialize table
		Vector<String> header = new Vector<String>();
		header.add(FeatureNames.Object.toString());
		
		if(this.calcArea)
		{
			header.add(FeatureNames.Area + " ("+ this.unitXY + "^2)");
		}
		if(this.calcPerimeter)
		{
			header.add(FeatureNames.Perimeter + " ("+ this.unitXY + ")");
		}
		if(this.calcLengthWidth)
		{
			header.add(FeatureNames.Length + " ("+ this.unitXY + ")");
			header.add(FeatureNames.Width + " ("+ this.unitXY + ")");
		}
		if(this.calcCircularity)
		{
			header.add(FeatureNames.Circularity.toString());
		}
		if(this.calcEccentricity)
		{
			header.add(FeatureNames.Eccentricity.toString());
		}
		if(this.calcSolidity)
		{
			header.add(FeatureNames.Solidity.toString());
		}
		if (this.calcMarginRoughness) 
		{
			header.add(FeatureNames.MarginRoughness.toString());
		}
		if (this.calcSkeletonBranchFeatures) 
		{
			header.add(FeatureNames.BranchCount.toString());
			header.add(
					FeatureNames.AvgBranchLength.toString() + " ("+ this.unitXY + ")");
			header.add(
					FeatureNames.AvgDistBranchEndpointsToBackground.toString() + " ("+ this.unitXY + ")" );
			header.add(
					FeatureNames.LongestPathLength.toString() + " ("+ this.unitXY + ")");
			header.add(FeatureNames.MinCoreRegionWidth.toString()
					+ " ("+ this.unitXY + ")" );
			header.add(FeatureNames.MaxCoreRegionWidth.toString()
					+ " ("+ this.unitXY + ")" );
			header.add(FeatureNames.RadiusMaxInscribedEmptyCircle.toString()
					+ " ("+ this.unitXY + ")" );
		}
		if (this.calcConcavityData) 
		{
			header.add(FeatureNames.AvgConcavity.toString());
			header.add(FeatureNames.StdDevConcavity.toString());
		}
		if (this.calcConvexHullMeasures) {
			header.add(FeatureNames.ConvexHullArea.toString() 
					+ " ("+ this.unitXY + "^2)");
			header.add(FeatureNames.ConvexHullPerimeter.toString() 
					+ " ("+ this.unitXY + ")");
			header.add(FeatureNames.ConvexHullConvexity.toString());
			header.add(FeatureNames.ConvexHullRoundness.toString());
		}
		if (this.analyzeProtrusionsIndentations) {
			header.add(FeatureNames.NumberOfProtrusions.toString());
			header.add(FeatureNames.NonProtrusionArea.toString()
					+ " ("+ this.unitXY + "^2)");
			header.add(FeatureNames.AvgLengthProtrusions.toString() 
					+ " ("+ this.unitXY + ")" );
			header.add(FeatureNames.AvgLengthApicalProtrusions.toString()
					+ " ("+ this.unitXY + ")" );
			header.add(FeatureNames.AvgLengthBasalProtrusions.toString()
					+ " ("+ this.unitXY + ")" );
			header.add(FeatureNames.AvgLengthBaselineProtrusions.toString()
					+ " ("+ this.unitXY + ")" );
			header.add(FeatureNames.AvgLengthEquatorProtrusions.toString()
					+ " ("+ this.unitXY + ")" );
			header.add(FeatureNames.AvgLengthIndentations.toString() 
					+ " ("+ this.unitXY + ")" );
			header.add(FeatureNames.AvgLengthApicalIndentations.toString()
					+ " ("+ this.unitXY + ")" );
			header.add(FeatureNames.AvgLengthBasalIndentations.toString()
					+ " ("+ this.unitXY + ")" );
			header.add(FeatureNames.AvgLengthBaselineIndentations.toString()
					+ " ("+ this.unitXY + ")" );
			header.add(FeatureNames.AvgLengthEquatorIndentations.toString()
					+ " ("+ this.unitXY + ")" );
		}
		
		int n = this.regions.size();
		
		this.table = new MTBTableModel(n, header.size(), header);
		
		for(int i = 0; i < n; i++)
		{
			int col = 1;
			
			this.table.setValueAt(this.labels.elementAt(i), i, 0);
			
			if(this.calcArea)
			{
				this.table.setValueAt(this.nf.format(this.areas.elementAt(i)), i, col);
				col++;
			}
			if(this.calcPerimeter)
			{
				this.table.setValueAt(this.nf.format(this.perimeters.elementAt(i)), i, col);
				col++;
			}
			if(this.calcLengthWidth)
			{
				this.table.setValueAt(this.nf.format(this.lengths.elementAt(i)), i, col);
				col++;
				this.table.setValueAt(this.nf.format(this.widths.elementAt(i)), i, col);
				col++;
			}
			if(this.calcCircularity)
			{
				this.table.setValueAt(this.nf.format(this.circularities.elementAt(i)), i, col);
				col++;
			}
			if(this.calcEccentricity)
			{
				this.table.setValueAt(this.nf.format(this.eccentricities.elementAt(i)), i, col);
				col++;
			}
			if(this.calcSolidity)
			{
				this.table.setValueAt(this.nf.format(this.solidities.elementAt(i)), i, col);
				col++;
			}
			if (this.calcMarginRoughness) 
			{
				this.table.setValueAt(this.nf.format(
						this.marginRoughnessValues.elementAt(i)), i, col);
				col++;
			}
			if (this.calcSkeletonBranchFeatures) 
			{
				this.table.setValueAt(this.nf.format(
						this.branchCounts.elementAt(i)), i, col);
				col++;
				this.table.setValueAt(this.nf.format(
						this.avgBranchLengths.elementAt(i)), i, col);
				col++;
				this.table.setValueAt(this.nf.format(
						this.avgEndpointDistances.elementAt(i)), i, col);
				col++;
				this.table.setValueAt(this.nf.format(
						this.longestPathLengths.elementAt(i)), i, col);
				col++;
				this.table.setValueAt(this.nf.format(
						this.minCoreRegionWidths.elementAt(i)), i, col);
				col++;
				this.table.setValueAt(this.nf.format(
						this.maxCoreRegionWidths.elementAt(i)), i, col);
				col++;
				this.table.setValueAt(this.nf.format(
						this.radiiMaxInscribedEmptyCircles.elementAt(i)), i, col);
				col++;
			}
			if (this.calcConcavityData) 
			{
				this.table.setValueAt(this.nf.format(
						this.avgConcavities.elementAt(i)), i, col);
				col++;
				this.table.setValueAt(this.nf.format(
						this.stdDevConcavities.elementAt(i)), i, col);
				col++;
			}
			if (this.calcConvexHullMeasures) {
				this.table.setValueAt(this.nf.format(
						this.convexHullAreas.elementAt(i)), i, col);
				col++;
				this.table.setValueAt(this.nf.format(
						this.convexHullPerimeters.elementAt(i)), i, col);
				col++;
				this.table.setValueAt(this.nf.format(
						this.convexHullConvexities.elementAt(i)), i, col);
				col++;
				this.table.setValueAt(this.nf.format(
						this.convexHullRoundnessValues.elementAt(i)), i, col);
				col++;				
			}
			if (this.analyzeProtrusionsIndentations) {
				this.table.setValueAt(this.nf.format(
						this.protrusionCounts.elementAt(i)), i, col);
				col++;
				this.table.setValueAt(this.nf.format(
						this.nonProtrusionAreas.elementAt(i)), i, col);
				col++;
				this.table.setValueAt(this.nf.format(
						this.avgProtrusionLengths.elementAt(i)), i, col);
				col++;
				this.table.setValueAt(this.nf.format(
						this.avgApicalProtrusionLengths.elementAt(i)), i, col);
				col++;
				this.table.setValueAt(this.nf.format(
						this.avgBasalProtrusionLengths.elementAt(i)), i, col);
				col++;
				this.table.setValueAt(this.nf.format(
						this.avgBaselineProtrusionLengths.elementAt(i)), i, col);
				col++;
				this.table.setValueAt(this.nf.format(
						this.avgEquatorProtrusionLengths.elementAt(i)), i, col);
				col++;
				this.table.setValueAt(this.nf.format(
						this.avgIndentationLengths.elementAt(i)), i, col);
				col++;
				this.table.setValueAt(this.nf.format(
						this.avgApicalIndentationLengths.elementAt(i)), i, col);
				col++;
				this.table.setValueAt(this.nf.format(
						this.avgBasalIndentationLengths.elementAt(i)), i, col);
				col++;
				this.table.setValueAt(this.nf.format(
						this.avgBaselineIndentationLengths.elementAt(i)), i, col);
				col++;
				this.table.setValueAt(this.nf.format(
						this.avgEquatorIndentationLengths.elementAt(i)), i, col);
				col++;

//				this.table.setValueAt(this.nf.format(
//						this.avgLobeDepths.elementAt(i)), i, col);
//				col++;
//				this.table.setValueAt(this.nf.format(
//						this.avgNeckDepths.elementAt(i)), i, col);
//				col++;

			}
		}
	}
		
	/**
	 * Set input label image.
	 * @param img	Label image to process.
	 */
	public void setLabelImage(MTBImage img) 
	{
		this.inLabelImg = img;
	}
	
	/**
	 * Get the label image.
	 * <p>
	 * If a label image is provided this method returns the input image,
	 * otherwise the internally generated label image is returned.
	 * 
	 * @return Image with region labels.
	 */
	public MTBImage getLabelImage() {
		if (this.inLabelImg != null)
			return this.inLabelImg;
		return this.labelImg;		
	}
	
	/**
	 * Set regions.
	 * @param regs	Set of regions to process.
	 */
	public void setRegionSet(MTBRegion2DSet regs) 
	{
		this.inRegions = regs;
	}
	
	/**
	 * @return Physical pixel length.
	 */
	public Double getDeltaXY()
	{
		return this.deltaXY;
	}

	/**
	 * @param dXY physical pixel length in x-direction
	 */
	public void setDeltaXY(Double dXY)
	{
		this.deltaXY = dXY;
	}

	/**
	 * @return physical space unit
	 */
	public String getUnitXY()
	{
		return this.unitXY;
	}

	/**
	 * @param unitXY tphysical space unit
	 */
	public void setUnitXY(String unitXY)
	{
		this.unitXY = unitXY;
	}

	/**
	 * @return are areas to be calculated
	 */
	public boolean calcArea()
	{
		return this.calcArea;
	}

	/**
	 * @param calcAreas should areas be calculated
	 */
	public void setCalcArea(boolean calcArea)
	{
		this.calcArea = calcArea;
	}

	/**
	 * @return are perimeters to be calculated
	 */
	public boolean calcPerimeter()
	{
		return this.calcPerimeter;
	}

	/**
	 * @param calcPerimeters should perimeters be calculated
	 */
	public void setCalcPerimeter(boolean calcPerimeter)
	{
		this.calcPerimeter = calcPerimeter;
	}

	/**
	 * @return are lengths to be calculated
	 */
	public boolean calcLengthWidth()
	{
		return this.calcLengthWidth;
	}

	/**
	 * Turn of/off calculation of region length and width.
	 * <p>
	 * The region length and width are defined as the lengths of the major and 
	 * minor axes of the ellipse best fitting.
	 * <p>
	 * The method is based on using moments of the region:
	 * {@latex.ilb %preamble{\\usepackage{amssymb}}
	 * \\begin{eqnarray*} 	
	 * b(R) &=& 2 \\cdot \\sqrt{
	 * 	\\frac{2 \\cdot (\\mu_{2,0} + \\mu_{0,2} \\pm 
	 * 					\\sqrt{ (\\mu_{2,0} - \\mu_{0,2})^2 + 4 \\cdot \\mu_{1,1}^2 })}
	 *        {\\mu_{0,0}}}
	 * \\end{eqnarray*}}
	 * 
	 * @param flag If true lengths of minor and major axis are calculated.
	 * @see MTBRegion2D
	 */
	@SuppressWarnings("javadoc")
  public void setCalcLengthWidth(boolean flag)
	{
		this.calcLengthWidth = flag;
	}

	/**
	 * @return are circularities to be calculated
	 */
	public boolean calcCircularity()
	{
		return this.calcCircularity;
	}

	/**
	 * Turn of/off calculation of circularity.
	 * <p>
	 * The circularity is calculated according to the following equation:
	 * {@latex.ilb %preamble{\\usepackage{amssymb,amsmath}}
	 * \\begin{eqnarray*} 	
	 * c(R) &=& \\frac{4 \\cdot \\pi \\cdot A(R)}{U(R) \\cdot U(R)} \\\\
	 * A(R) &:=& \\text{area of the region}\\\\ 
	 * U(R) &:=& \\text{perimeter of the region}
	 * \\end{eqnarray*}}
	 * 
	 * @param flag If true, circularity is calculated.
	 * @see MTBRegion2D
	 */
	@SuppressWarnings("javadoc")
  public void setCalcCircularity(boolean flag)
	{
		this.calcCircularity = flag;
	}

	/**
	 * @return are eccentricities to be calculated
	 */
	public boolean calcEccentricity()
	{
		return this.calcEccentricity;
	}

	/**
	 * Turn of/off calculation of eccentricity.
	 * <p>
	 * The eccentricity is calculated according to the following equation:
	 * {@latex.ilb %preamble{\\usepackage{amssymb}}
	 * \\begin{eqnarray*} 	
	 * e(R) &=& \\frac{(\\mu_{2,0} - \\mu_{0,2})^2 + 4 \\cdot \\mu_{1,1}^2}
	 *                {(\\mu_{2,0} + \\mu_{0,2})^2}
	 * \\end{eqnarray*}}
	 * 
	 * @param flag If true eccentricities are calculated.
	 * @see MTBRegion2D
	 */
	@SuppressWarnings("javadoc")
  public void setCalcEccentricity(boolean flag)
	{
		this.calcEccentricity = flag;
	}

	/**
	 * Turn on/off calculation of solidities.
	 * <p>
	 * The solidity is calculated according to the following equation:
	 * {@latex.ilb %preamble{\\usepackage{amssymb,amsmath}}
	 * \\begin{eqnarray*} 	
	 * s(R) &=& \\frac{A(R)}{A_C(R)} \\\\
	 * A(R) &:=& \\text{area of the region}\\\\ 
	 * A_C(R) &:=& \\text{area of convex hull of the region}
	 * \\end{eqnarray*}}
	 * 
	 * @param flag 	If true, solidities are calculated.
	 */
	@SuppressWarnings("javadoc")
  public void setCalcSolidity(boolean flag)
	{
		this.calcSolidity = flag;
	}

	/**
	 * Turn on/off calculation of margin roughness values.
	 * @param flag	If true, margin roughness values are calculated.
	 */
	public void setCalcMarginRoughness(boolean flag) 
	{
		this.calcMarginRoughness = flag;
	}

	/**
	 * Turn on/off analysis of contour protrusions and indentations.
	 * <p>
	 * The analysis of protrusions and indentations subsumes the overall count of 
	 * protrusions, their average lengths and some more measures.
	 * 
	 * @param flag	If true, protrusion/indentation measures are extracted.
	 */
	public void setAnalyzeProtrusionsAndIndentations(boolean flag)
	{
		this.analyzeProtrusionsIndentations = flag;
	}

	/**
	 * Set minimal curvature threshold.
	 * 
	 * @param c	Minimal curvature.
	 */
	public void setMinimalCurvatureThreshold(double c) {
		this.minimalCurvature = c;
	}

	/**
	 * Set Gaussian standard deviation for smoothing curvature values.
	 * <p>
	 * The smoothing affects the calculation of margin roughness and 
	 * the analysis of lobes and necks.
	 * 
	 * @param s	Gaussian standard deviation to apply.
	 */
	public void setGaussianSigmaCurvatureSmoothing(double s) {
		this.gaussianSigma = s;
	}
	
	/**
	 * Set minimal pixel count a lobe requires.
	 * <p>
	 * Lobes with fewer pixels will be eliminated.
	 * 
	 * @param m	Minimal pixel count to apply.
	 */
	public void setMinimalLobeLength(int m) {
		this.minProtrusionLength = m;
	}

	/**
	 * Turn on/off calculation of skeleton branch features.
	 * <p>
	 * The skeleton branch features subsume the total number of skeleton 
	 * branches, their average length as well as the average radius of the 
	 * lobes (approximated as the distance of branch endpoints from the 
	 * background).
	 * 
	 * @param flag	If true, skeleton branch features are extracted.
	 */
	public void setCalcSkeletonBranchFeatures(boolean flag)
	{
		this.calcSkeletonBranchFeatures = flag;
	}

	/**
	 * Turn on/off calculation of concavity information.
	 * <p>
	 * The method calculates the average concavity of all pixels on the 
	 * region boundary as well as the standard deviation of concavities.
	 * For more information on the estimation of local contour concavity
	 * refer to the operator {@link Contour2DConcavityCalculator}.
	 * 
	 * @param flag	If true, concavity data is calculated.
	 */
	public void setCalcConcavityData(boolean flag) 
	{
		this.calcConcavityData = flag;
	}
	
	/**
	 * Specify size of local mask for concavity calculations.
	 * <p>
	 * Refer to operator {@link Contour2DConcavityCalculator} for details.
	 *
	 * @param m	Size of mask to apply.
	 */
	public void setConcavityMaskSize(int m) {
		this.concavityMaskSize = m;
	}
	
	/**
	 * Turn on/off calculation of convex hull measures. 
	 * @param flag If true, convex hull measures are calculated.
	 */
	public void setCalcConvexHullMeasures(boolean flag) 
	{
		this.calcConvexHullMeasures = flag;
	}
	
	/**
	 * specify the number of fractional digits for the results table
	 * 
	 * @param digits
	 */
	public void setFractionalDigits(int digits)
	{
		this.fracDigits = digits;
	}
	
	/**
	 * Enable/disable display of curvature info image.
	 *
	 * @param flag 	If true image is drawn and shown, otherwise not.
	 */
	public void setDrawCurvatureInfoImage(boolean flag) {
		this.createCurvatureInfoImage = flag;
	}

	/**
	 * Enable/disable display of skeleton info image.
	 *
	 * @param flag 	If true image is drawn and shown, otherwise not.
	 */
	public void setDrawSkeletonInfoImage(boolean flag) {
		this.createSkeletonInfoImage = flag;
	}

	/**
	 * @return result table
	 */
	public MTBTableModel getTable()
	{
		return this.table;
	}
	
	/**
	 * Access detailed information on indentations and protrusions.
	 * <p>
	 * These data is only available if {@link #analyzeProtrusionsIndentations}
	 * has been set to true.
	 * 
	 * @return Vector of cell-wise indentation/protrusion information.
	 */
	public Vector<MorphologyAnalyzer2DInProData> getDetailedInProResults() {
		return this.inProDetails;
	}
	
	/**
	 * Access info image about curvature analysis.
	 * @return	Info image, only non-null if construction was enabled.
	 */
	public MTBImageRGB getCurvatureInfoImage() {
		return this.curvatureInfoImg;
	}
	
	/**
	 * Access info image about skeleton analysis.
	 * @return	Info image, only non-null if construction was enabled.
	 */
	public MTBImageRGB getSkeletonInfoImage() {
		return this.skeletonInfoImg;
	}

	/**
	 * Calculates and analyses pixel-wise local curvatures.
	 * <p>
	 * The method extracts the following features:
	 * <ul>
	 * <li> Number of protrusions and indentations:<br>
	 * 	The sum of all protrusions and indentations is equal to the number of 
	 *  inflection points found along the contour, and the number of protrusions
	 *  and indentations, respectively, is each half of the total count of 
	 *  inflection points.
	 * <li> Protrusion and indentation measures:<br>
	 *  For each protrusion and indentation, respectively, various measures are
	 *  extracted. The baseline of a protrusion or indentation is defined to be
	 *  the line connecting the medial points of the neighboring protrusions in 
	 *  case of an indentation and of the neighboring indentations in case of a
	 *  protrusion. If this line intersects with the background its endpoints are
	 *  shifted along the region contour until a minimal number of backgroud 
	 *  pixels is touched while the shifts are kept as small as possible.
	 *  <p>
	 *  In addition to the baseline also the equator of protrusions and 
	 *  indentations is extracted by drawing a line between the two inflection 
	 *  points defining the protrusion or indentation, respectively. The total 
	 *  length of a protrusion or indentation, and the apical and basal lengths
	 *  are calculated by calculating the largest distance of any point of 
	 *  the baseline to the region contour. Then the intersection point of this
	 *  distance line with the equator is found which splits the distance line.
	 *  The length of the upper part of this line is denoted the apical length
	 *  of the protrusion/indentation and the length of the lower part is denoted
	 *  the basal length of the protrusion/indentation.  
	 * <li> Margin roughness:<br>
	 *  The implementation of margin roughness is derived from the
	 *  following paper <i>T. McLellan, J. Endler, The Relative Success of 
	 *  Some Methods for Measuring and Describing the Shape of Complex 
	 *  Objects, Systematic Biology, Vol. 47, No. 2, pp. 264-281, June 
	 *  1998</i>.<br><br>
	 *  The margin roughness analyzes local curvature in terms of the
	 *  differences in orientation of tangents at neighboring pixels on 
	 *  the contour. It first calculates pairwise orientation differences 
	 *  and the average of all differences along the contour. This average 
	 *  value is then compared to the average angular difference to be 
	 *  expected for tangents located on a perfect circle being sampled
	 *  with the same number of points as the given contour. Thus, the 
	 *  margin roughness yields a measure for the deviation of the contour 
	 *  curvature from an optimal circle.<br>
	 *  The margin roughness MR for a contour C is here defined as 
	 *  follows:<br><br>
	 *  {@latex.ilb %preamble{\\usepackage{amssymb,amsmath}}
	 *  \\begin{eqnarray*} 	
	 *  MR(C) &=& \\left( \\frac{1}{N} \\sum_{i=1}^{N} |A_i| \\right) 
	 *  			- \\frac{360}{N}\\\\
	 *  N &:=& \\text{Number of points on the contour} \\\\
	 *  A_i &:=& \\text{Difference in tangent orientations between point 
	 *  	$i$	and its neighbors}
	 *  \\end{eqnarray*}}	 
	 *  <br>
	 *  Note that the calculation of the tangent orientations is not the
	 *  same as in the original paper.<br>
	 *  Here we use an algorithm of Freeman,
	 *  see {@link Contour2DCurvatureCalculator} for details.  
	 * </ul>
	 * 
	 * @throws ALDOperatorException	Thrown in case of failure.
	 * @throws ALDProcessingDAGException
	 * 		Thrown in case of problems with the processing history.
	 */
	@SuppressWarnings("javadoc")
  private void analyzeLocalCurvatures() 
			throws ALDOperatorException, ALDProcessingDAGException {
		
		if (this.createCurvatureInfoImage) {
			this.curvatureInfoImg = (MTBImageRGB)MTBImage.createMTBImage(
					this.width, this.height, 1, 1, 1, MTBImageType.MTB_RGB);
			for (int y=0;y<this.height;++y) {
				for (int x=0;x<this.width;++x) {
					this.curvatureInfoImg.putValueR(x, y, 200);
					this.curvatureInfoImg.putValueG(x, y, 200);
					this.curvatureInfoImg.putValueB(x, y, 200);                                               					
				}
			}
			this.curvatureInfoImg.setTitle("Curvatures of <" +
					this.labelImg.getTitle() + ">, threshold = " + 1.0);
		}
		
		// extract contours
		ContourOnLabeledComponents contourOp = 
				new ContourOnLabeledComponents(this.regions, 
						ContourType.OUTER_CONTOUR, 1);
		contourOp.runOp(HidingMode.HIDDEN);
		MTBContour2DSet contours = contourOp.getResultContours();

		// calculate pixel-wise curvatures
		Contour2DCurvatureCalculator curvOp = 
				new Contour2DCurvatureCalculator(contours);
		curvOp.runOp(HidingMode.HIDE_CHILDREN);
		Vector<double[]> curvatureValues = 
				curvOp.getResultVectorOfCurvatures();

		// apply Gaussian smoothing to curvatures
		GaussFilterDouble1D gaussFilter = new GaussFilterDouble1D();
		gaussFilter.setSigma(this.gaussianSigma);
		int i=0;
		for (double[] values: curvatureValues) {
			gaussFilter.setInputArray(values);
			gaussFilter.runOp(HidingMode.HIDE_CHILDREN);
			curvatureValues.setElementAt(gaussFilter.getResultArray(), i);
			++i;
		}

		if (this.calcMarginRoughness) {
			// calculate margin roughness, i.e. the deviation of curvatures 
			// from curvatures to be found on an optimal circle
			double curvSum = 0;
			for (double[] values: curvatureValues) {
				curvSum = 0;
				for (double d: values)
					curvSum += Math.abs(d);
				double expectedValue = 360.0/values.length;
				this.marginRoughnessValues.add(
						new Double(curvSum/values.length - expectedValue));
			}
		}
		
		if (this.analyzeProtrusionsIndentations) {
			MorphologyAnalyzer2DInProHelper ipHelper;
			
//			// -----------------
//			// experimental code for lobe hierarchies
//			
//			Vector<MTBImageRGB> stacks = new Vector<>();
//			Vector<Integer> neckCount = new Vector<>();
//      MTBContour2D nc = contours.elementAt(0);
//      
//  		MTBImageRGB dimg = (MTBImageRGB)MTBImage.createMTBImage(
//					this.width, this.height, 1, 1, 1, MTBImageType.MTB_RGB);
//			for (int y=0;y<this.height;++y) {
//				for (int x=0;x<this.width;++x) {
//					dimg.putValueR(x, y, 200);
//					dimg.putValueG(x, y, 200);
//					dimg.putValueB(x, y, 200);                                               					
//				}
//			}
//			for (Point2D.Double p: nc.getPoints()) {
//				int px = (int)p.x;
//				int py = (int)p.y;
//				dimg.putValueR(px, py, 255);
//				dimg.putValueG(px, py, 255);
//				dimg.putValueB(px, py, 255);                                               					    				
//			}
//
//			ipHelper = 
//					new MorphologyAnalyzer2DInProHelper(this.width, this.height, 
//							this.deltaXY.doubleValue(), this.labelImg, dimg);
//			MorphologyAnalyzer2DInProData prevResult = 
//					ipHelper.doProtrusionIndentationAnalysis(contours, curvatureValues, 
//							this.minProtrusionLength).elementAt(0);
//			int prevNum = prevResult.protrusionSegs.size();
//      dimg.setTitle("Original : " + prevResult.protrusionSegs.size());
//			stacks.add(dimg);
//
//      int k=9;
//      boolean proceed = true;
//      while (proceed) {
//      	try {
//      		if (k > nc.getPointNum()/2)
//      			break;
//      		MTBContour2D ncc = nc.smoothContour(k);
//      		MTBContour2DSet s = new MTBContour2DSet();
//      		s.add(ncc);
//      		Contour2DCurvatureCalculator cOp = 
//      				new Contour2DCurvatureCalculator(s);
//      		cOp.setK(11);
//      		cOp.runOp(HidingMode.HIDE_CHILDREN);
//      		
//      		dimg = (MTBImageRGB)MTBImage.createMTBImage(
//    					this.width, this.height, 1, 1, 1, MTBImageType.MTB_RGB);
//    			for (int y=0;y<this.height;++y) {
//    				for (int x=0;x<this.width;++x) {
//    					dimg.putValueR(x, y, 200);
//    					dimg.putValueG(x, y, 200);
//    					dimg.putValueB(x, y, 200);                                               					
//    				}
//    			}
//    			for (Point2D.Double p: ncc.getPoints()) {
//    				int px = (int)p.x;
//    				int py = (int)p.y;
//  					dimg.putValueR(px, py, 255);
//  					dimg.putValueG(px, py, 255);
//  					dimg.putValueB(px, py, 255);                                               					    				
//    			}
//
//    			ipHelper = new MorphologyAnalyzer2DInProHelper(this.width, this.height, 
//    							this.deltaXY.doubleValue(), this.labelImg, dimg);
//      		Vector<MorphologyAnalyzer2DInProData> r = 
//      				ipHelper.doProtrusionIndentationAnalysis(s, cOp.getResultVectorOfCurvatures(), 
//							this.minProtrusionLength);
//      		MorphologyAnalyzer2DInProData ir = r.elementAt(0);
//      		if (ir.indentationSegs.size() > 1 && ir.protrusionSegs.size() < prevNum) {
//      			neckCount.add(new Integer(ir.protrusionSegs.size()));
//      			dimg.setTitle("k = " + k + " : " + ir.protrusionSegs.size());
//      			stacks.add(dimg);
//
//      			// analyze neckpoints
//      			boolean[] prevPointsUsed = new boolean[prevNum];
//      			Vector<Integer> matchIDs = new Vector<>();
//      			for (int p=0;p<prevNum;++p)
//      				prevPointsUsed[p] = false;
//
//      			for (int pp=0;pp<ir.indentationSegs.size();++pp) {
//      				Point2D.Double mpp = ir.indentationSegs.get(pp).midPoint;
//      				double minDist = Double.MAX_VALUE;
//      				int minID = -1;
//      				for (int ppp=0;ppp<prevNum;++ppp) {
//      					if (prevPointsUsed[ppp])
//      						continue;
//      					double dist = mpp.distance(prevResult.indentationSegs.get(ppp).midPoint);
//      					if (dist<minDist) {
//      						minDist = dist;
//      						minID = ppp;
//      					}
//      				}
//      				if (minID != -1)
//      					matchIDs.add(new Integer(minID));
//      			}
//    				int minSizeToDelete = Integer.MAX_VALUE;
//      			for (int ppp=0;ppp<prevNum;++ppp) {
//      				if (!matchIDs.contains(new Integer(ppp))) {
//      					if (prevResult.indentationSegs.get(ppp).segPoints.size() < minSizeToDelete)
//      						minSizeToDelete = prevResult.indentationSegs.get(ppp).segPoints.size();
//      				}
//      			}
//      			if (minSizeToDelete < 25) {
//      				for (int ppp=0;ppp<prevNum;++ppp) {
//      					if (!matchIDs.contains(new Integer(ppp))) {
//      						LinkedList<Point2D.Double> sp = 
//      								prevResult.indentationSegs.get(ppp).segPoints;
//      						for(Point2D.Double p: sp) {
//      							int px = (int)p.x;
//      							int py = (int)p.y;
//      							dimg.putValueR(px, py, 0);
//      							dimg.putValueG(px, py, 0);
//      							dimg.putValueB(px, py, 0);
//      						}
//      					}
//      				}    			
//      				prevNum = ir.protrusionSegs.size();
//      				prevResult = ir;
//      			}
//      			else {
//      				proceed = false;
//      			}
//      		}
//					if (ir.indentationSegs.size() <= 1) {
//						proceed = false;
//					}
//					k += 10;
//      	} catch (MTBDatatypeException e) {
//      		// ... won't happen
//      		System.err.println("Error!");
//      		e.printStackTrace();
//      	}
//      }
//      
//      MTBImageRGB stack = (MTBImageRGB)MTBImage.createMTBImage(
//					this.width, this.height, 1, 1, stacks.size(), MTBImageType.MTB_RGB);
//      int c=0;
//      for (MTBImageRGB im: stacks) {
//      	stack.setSlice(im, 0, 0, c);
//      	stack.setSliceLabel(im.getTitle(), 0, 0, c);
//      	++c;
//      }
//      stack.show();
//      
//      // -----------------
			
			ipHelper = 
					new MorphologyAnalyzer2DInProHelper(this.width, this.height, 
							this.deltaXY.doubleValue(), this.labelImg, this.curvatureInfoImg);
			Vector<MorphologyAnalyzer2DInProData> crs = 
					ipHelper.doProtrusionIndentationAnalysis(contours, curvatureValues, 
							this.minProtrusionLength, this.minIndentationLength);
						
			// copy results to vectors collecting analysis data
			for (MorphologyAnalyzer2DInProData cr: crs) {

				// remember complete result
				this.inProDetails.add(cr);
				
	    	this.protrusionCounts.add(
	    			new Integer(cr.numberOfProtrusions));
	    	this.avgEquatorProtrusionLengths.add(
	    			new Double(cr.avgEquatorProtrusionLength));
	    	this.avgEquatorIndentationLengths.add(
	    			new Double(cr.avgEquatorIndentationLength));
	    	this.avgProtrusionLengths.add(
	    			new Double(cr.avgProtrusionLength));
	    	this.avgBaselineProtrusionLengths.add(
	    			new Double(cr.avgBaselineProtrusionLength));
	    	this.avgApicalProtrusionLengths.add(
	    			new Double(cr.avgApicalProtrusionLength));
	    	this.avgBasalProtrusionLengths.add(
	    			new Double(cr.avgBasalProtrusionLength));
	    	this.nonProtrusionAreas.add(
	    			new Double(cr.nonProtrusionArea));
//	    	this.avgDistsIndentationMidPoints.add(
//	    			new Double(cr.avgDistsIndentationMidPoints));
//	    	this.minMinimalDistsIndentationMidPoints.add(
//	    			new Double(cr.minMinimalDistsIndentationMidPoints));
//	    	this.maxMinimalDistsIndentationMidPoints.add(
//	    			new Double(cr.maxMinimalDistsIndentationMidPoints));
	    	this.avgIndentationLengths.add(
	    			new Double(cr.avgIndentationLength));
	    	this.avgBaselineIndentationLengths.add(
	    			new Double(cr.avgBaselineIndentationLength));
	    	this.avgApicalIndentationLengths.add(
	    			new Double(cr.avgApicalIndentationLength));
	    	this.avgBasalIndentationLengths.add(
	    			new Double(cr.avgBasalIndentationLength));
			}
		} // end of part of analyzing curvatures, protrusions, indentations
	}	
	
	/**
	 * Extracts average concavity and standard deviation of concavities.
	 * <p>
	 * For details about concavity calculation refer to 
	 * {@link Contour2DConcavityCalculator}.
	 * 
	 * @throws ALDOperatorException	Thrown in case of failure.
	 * @throws ALDProcessingDAGException
	 * 		Thrown in case of problems with processing history.
	 */
	private void calculateConcavityData() 
			throws ALDOperatorException, ALDProcessingDAGException {
//		int width = this.labelImg.getSizeX();
//		int height = this.labelImg.getSizeY();
//		MTBImage binaryLabelImg = MTBImage.createMTBImage(width, height, 
//				1, 1, 1, MTBImageType.MTB_BYTE);
//		binaryLabelImg.fillBlack();
//		for (int y=0; y<height; ++y)
//			for (int x=0; x<width; ++x)
//				if (this.labelImg.getValueInt(x, y) > 0)
//					binaryLabelImg.putValueInt(x, y, 255);
		// extract contours
		ContourOnLabeledComponents contExtractor = new ContourOnLabeledComponents();
		contExtractor.setInputRegions(this.regions);
		contExtractor.setContourType(ContourType.OUTER_CONTOUR);
		contExtractor.runOp(HidingMode.HIDE_CHILDREN);
		// run concavity operator
		Contour2DConcavityCalculator concavityOp = 
				new Contour2DConcavityCalculator(this.labelImg);
		concavityOp.setContours(contExtractor.getResultContours());
		concavityOp.setRadius(this.concavityMaskSize);
		concavityOp.runOp(HidingMode.HIDE_CHILDREN);
		Vector<int[]> concavities = concavityOp.getConcavenessValues();

		// calculate average concavities and their standard deviations
		int concavitySum;
		for (int[] concavityValues: concavities) {
			concavitySum = 0;
			for (int d: concavityValues)
				concavitySum += d;
			this.avgConcavities.add(
					new Double(concavitySum/concavityValues.length));
		}
		int id = 0;
		for (int[] concavityValues: concavities) {
			concavitySum = 0;
			double avg = this.avgConcavities.get(id).doubleValue(); 
			for (int d: concavityValues)
				concavitySum += (d-avg)*(d-avg);
			this.stdDevConcavities.add(new Double(
					Math.sqrt(concavitySum/concavityValues.length)));
			++id;
		}		
	}
	
	/**
	 * Calculates convex hull areas and perimeters.
	 * @throws ALDOperatorException	Thrown in case of failure.
	 * @throws ALDProcessingDAGException
	 * 		Thrown in case of problems with processing history.
	 */
	private void calculateConvexHulls() 
			throws ALDOperatorException, ALDProcessingDAGException {
		ConvexHullExtraction convHullOp = new ConvexHullExtraction();
		convHullOp.setInputType(InputType.REGIONS);
		convHullOp.setInputRegions(this.regions);
		convHullOp.runOp(HidingMode.HIDE_CHILDREN);
		Vector<Point2D.Double[]> hulls = convHullOp.getResultingConvexHulls();
		MTBImage hullImage = convHullOp.getResultingHullImage();

		int width = hullImage.getSizeX();
		int height = hullImage.getSizeY();
		ImagePlus img = NewImage.createByteImage("", width, height, 1,
				NewImage.FILL_WHITE);
		ImageProcessor ip = img.getProcessor();
		for (Point2D.Double[] ps: hulls) {

			// calculate convex hull perimeter
			double hullPerimeter = 0;
			double px = ps[ps.length-1].x;
			double py = ps[ps.length-1].y;
			for (int i=0; i<ps.length; ++i) {
				double tx = ps[i].x;
				double ty = ps[i].y;
				hullPerimeter += Math.sqrt((tx-px)*(tx-px) + (ty-py)*(ty-py));
				px = tx;
				py = ty;
			}
			
			// fill image white
			for (int y=0;y<height; ++y)
				for (int x=0;x<width; ++x)
					ip.putPixelValue(x, y, 255);

			int[] xps = new int[ps.length];
			int[] yps = new int[ps.length];
			int n = 0;
			for (Point2D.Double p : ps) {
				xps[n] = (int) (p.x);
				yps[n] = (int) (p.y);
				n++;
			}
			Polygon awtPoly = new Polygon(xps, yps, ps.length);

			// plot filled polygon in black on white background
			ip.fillPolygon(awtPoly);

			// determine size of hull area
			int hullArea = 0;
			for (int y = 0; y < height; ++y) {
				for (int x = 0; x < width; ++x) {
					if (ip.getPixel(x, y) == 0) {
						// polygon inner area
						++hullArea;
					}
				}
			}
			this.convexHullAreas.add(new Double(hullArea 
					* this.deltaXY.doubleValue() * this.deltaXY.doubleValue()));
			this.convexHullPerimeters.add(
					new Double(hullPerimeter * this.deltaXY.doubleValue()));
		}
	}
	
	/**
	 * Calculates solidity values.
	 */
	private void calculateSolidityValues() {
		// calculate solidity values for all regions
		double area, hullArea;
		for (int i=0; i<this.regions.size(); ++i) {
			area = this.areas.elementAt(i).doubleValue();
			hullArea = this.convexHullAreas.elementAt(i).doubleValue();
			double solidity = area/hullArea;
			// make sure that solidity is not larger than 1.0 (which might happen 
			// in practice due to discritization, but is not possible in theory)
			this.solidities.add(new Double(solidity > 1.0 ? 1.0 : solidity));
		}
	}
	
	/**
	 * Calculates convex hull convexities and roundnesses.
	 */
	private void calculateConvexHullMeasures() {
		// calculate convexities and roundness values
		double hullPerimeter, perimeter, area;
		for (int i=0; i<this.regions.size(); ++i) {
			area = this.areas.elementAt(i).doubleValue();
			perimeter = this.perimeters.elementAt(i).doubleValue();
			hullPerimeter = this.convexHullPerimeters.elementAt(i).doubleValue();
			this.convexHullConvexities.add(new Double(hullPerimeter/perimeter));
			this.convexHullRoundnessValues.add(
					new Double(4.0*Math.PI*area/(hullPerimeter*hullPerimeter)));
		}
	}

	/*
	 * Callback functions.
	 */

	@SuppressWarnings("unused")
	private void getCalibration()
	{
		if(this.inLabelImg != null)
		{
			this.deltaXY = new Double(this.inLabelImg.getCalibration().pixelWidth);
			this.unitXY = this.inLabelImg.getCalibration().getXUnit();
		}
	}
	
	/**
	 * Callback function called in case of changes of parameters
	 * {@link #analyzeProtrusionsIndentations} or {@link #calcMarginRoughness}.
	 */
	@SuppressWarnings("unused")
	private void callbackCurvature() 
	{
    try {
  		if (this.calcMarginRoughness || this.analyzeProtrusionsIndentations) {
    		if (!this.hasParameter("gaussianSigma"))
    			this.addParameter("gaussianSigma");
    		if (!this.hasParameter("minimalCurvature"))
    			this.addParameter("minimalCurvature");
    	}
    	else if (!this.calcMarginRoughness && !this.analyzeProtrusionsIndentations){
    		if (this.hasParameter("gaussianSigma"))
    			this.removeParameter("gaussianSigma");
    		if (this.hasParameter("minimalCurvature"))
    			this.removeParameter("minimalCurvature");
    	}
    } catch (ALDOperatorException e) {
    	// TODO Auto-generated catch block
    	e.printStackTrace();
    }
	}

	/**
	 * Callback function called in case of changes of parameter
	 * {@link #calcArea}.
	 */
	@SuppressWarnings("unused")
	private void callbackArea() 
	{
    try {
  		if (!this.calcArea) {
  			this.setParameter("analyzeLobesAndNecks", new Boolean(false));
  			this.setParameter("calcSolidity", new Boolean(false));
    	}
    } catch (ALDOperatorException e) {
    	// TODO Auto-generated catch block
    	e.printStackTrace();
    }
	}

	/**
	 * Callback function called in case of changes of parameter
	 * {@link #calcSolidity}.
	 */
	@SuppressWarnings("unused")
	private void callbackSolidity() 
	{
    try {
  		if (this.calcSolidity) {
  			this.setParameter("calcArea", new Boolean(true));
    	}
    } catch (ALDOperatorException e) {
    	// TODO Auto-generated catch block
    	e.printStackTrace();
    }
	}

	/**
	 * Callback function called in case of changes of parameter
	 * {@link #calcConcavityData}.
	 */
	@SuppressWarnings("unused")
	private void callbackConcavity() 
	{
    try {
    	if (this.calcConcavityData) {
    		if (!this.hasParameter("concavityMaskSize"))
    			this.addParameter("concavityMaskSize");
    	}
    	else {
    		if (this.hasParameter("concavityMaskSize"))
    			this.removeParameter("concavityMaskSize");    		
    	}
    } catch (ALDOperatorException e) {
    	// TODO Auto-generated catch block
    	e.printStackTrace();
    }
	}
}
