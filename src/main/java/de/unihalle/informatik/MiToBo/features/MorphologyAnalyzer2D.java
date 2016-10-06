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
import java.awt.geom.Line2D;
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
import de.unihalle.informatik.MiToBo.core.datatypes.MTBContour2D;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBContour2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBPolygon2D;
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
 * @author glass
 */
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.ALL, level=Level.STANDARD, allowBatchMode = true)
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
		 * Number of skeleton branches.
		 */
		BranchCount,
		/**
		 * Average length of branches.
		 */
		AvgBranchLength,	
		/**
		 * Average distance of branch endpoints to background.
		 */
		AvgLobeRadius,
		/**
		 * Number of lobes.
		 */
		LobeCount,
		/**
		 * Ratio of non-lobe area to cell area.
		 */
		NonLobeAreaRatio,	
		/**
		 * Average depth of lobes.
		 */
		AvgLobeDepth,
		/**
		 * Average depth of necks.
		 */
		AvgNeckDepth,
		/**
		 * Length of longest path in skeleton.
		 */
		LongestPathLength,
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
		ConvexHullRoundness
	}

	@Parameter(label = "label image", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "label image", dataIOOrder = 0,
				callback = "getCalibration", paramModificationMode = ParameterModificationMode.MODIFIES_INTERFACE)
	private transient MTBImage inLabelImg = null;	// labeled input image stack, corresponding objects in different frames must be labeled with the same value
	
	@Parameter(label = "regions", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "input regions", dataIOOrder = 1)
	private MTBRegion2DSet inRegions = null;
	
	//analysis parameters
	@Parameter(label = "Pixel length", required = false, 
			direction = Parameter.Direction.IN, supplemental = false, 
			description = "Pixel length, note that we assume square pixels!", 
			dataIOOrder = 2)
	private Double deltaXY = new Double(1.0);
	
	@Parameter(label = "unit x/y", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "unit x/y", dataIOOrder = 4)
	private String unitXY = "pixel";
	
	// which features should be calculated
	@Parameter(label = "calculate area", required = false, 
		direction = Parameter.Direction.IN, supplemental = false, 
		description = "should object's areas be calculated", 
		dataIOOrder = 5, callback = "callbackArea",
		paramModificationMode = ParameterModificationMode.MODIFIES_INTERFACE)
	private boolean calcArea = true;
	
	@Parameter(label = "calculate perimeter", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "should object's perimeters be calculated", dataIOOrder = 6)
	private boolean calcPerimeter = true;
	
	@Parameter(label = "calculate length and width", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "should object's length and width (fitting ellipse's major/minor axes length) be calculated", dataIOOrder = 7)
	private boolean calcLengthWidth = true;
	
	@Parameter(label = "calculate circularitie", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "should object's circularities be calculated", dataIOOrder = 8)
	private boolean calcCircularity = true;
	
	@Parameter(label = "calculate eccentricity", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "should object's eccentricities be calculated", dataIOOrder = 9)
	private boolean calcEccentricity = true;
	
	/**
	 * Flag to turn on/off calculation of solidity.
	 */
	@Parameter(label = "calculate solidity", required = false, 
		direction = Parameter.Direction.IN, supplemental = false, 
		description = "should object's solidity be calculated", 
		dataIOOrder = 10, callback = "callbackSolidity",
		paramModificationMode = ParameterModificationMode.MODIFIES_INTERFACE)
	private boolean calcSolidity = true;

	/**
	 * Flag to turn on/off calculation of margin roughness.
	 */
	@Parameter(label = "calculate margin roughness", required = false, 
		direction = Parameter.Direction.IN, supplemental = false, 
		description = "If true margin roughness is calculated.", 
		dataIOOrder = 11, callback = "callbackCurvature",
		paramModificationMode = ParameterModificationMode.MODIFIES_INTERFACE)
	private boolean calcMarginRoughness = true;

	/**
	 * Flag to turn on/off analysis of lobes and necks.
	 */
	@Parameter(label = "analyze lobes and necks", required = false, 
		direction = Parameter.Direction.IN, supplemental = false, 
		description = "If true lobes and necks are analyzed.",
		dataIOOrder = 12, callback = "callbackCurvature",
		paramModificationMode = ParameterModificationMode.MODIFIES_INTERFACE)
	private boolean analyzeLobesAndNecks = true;

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
	 * Flag to turn on/off calculation of skeleton branch features.
	 */
	@Parameter(label = "calculate skeleton branch features", 
		required = false, dataIOOrder = 15,
		direction = Parameter.Direction.IN, supplemental = false, 
		description = "If true skeleton branches are analyzed.") 
	private boolean calcSkeletonBranchFeatures = true;

	/**
	 * Flag to turn on/off analysis of concavities.
	 */
	@Parameter(label = "calculate concavity information", 
		required = false, direction = Parameter.Direction.IN, 
		supplemental = false, dataIOOrder = 16,
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
		supplemental = false, dataIOOrder = 17,
		description = "Size of local mask in concavity calculations.") 
	private int concavityMaskSize = 11;

	/**
	 * Flag to turn on/off analysis of convex hull.
	 */
	@Parameter(label = "calculate convex hull measures", 
		required = false, direction = Parameter.Direction.IN, 
		supplemental = false, dataIOOrder = 18,
		description = "If true some measures of the convex hull "
			+ "are calculated.")
	private boolean calcConvexHullMeasures = true;

	@Parameter(label = "fractional digits", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "fractional digits", dataIOOrder = 17, mode=ExpertMode.ADVANCED)
	private Integer fracDigits = 3;
	
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
	private Vector<Double> branchCounts;
	private Vector<Double> avgBranchLengths;
	private Vector<Double> avgEndpointDistances;
	private Vector<Integer> lobeCounts;
	private Vector<Double> avgLobeDepths;
	private Vector<Double> avgNeckDepths;
	private Vector<Double> nonLobeAreaRatios;
	private Vector<Double> longestPathLengths;
	private Vector<Double> convexHullAreas;
	private Vector<Double> convexHullPerimeters;
	private Vector<Double> convexHullConvexities;
	private Vector<Double> convexHullRoundnessValues;
	
	private MTBRegion2DSet regions = null;
	private MTBImage labelImg = null;
	
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

		this.nf.setMaximumFractionDigits(this.fracDigits);
		this.nf.setMinimumFractionDigits(this.fracDigits);
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
		
		// get objects' shape features
		getSimpleShapeFeatures();

		// create a results table
		makeTable();
		
		// set internal regions and label image to null to allow a subsequent run of the operator
		this.regions = null;
		this.labelImg = null;
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
		this.branchCounts = new Vector<Double>();
		this.avgBranchLengths = new Vector<Double>();
		this.avgEndpointDistances = new Vector<Double>();
		this.lobeCounts = new Vector<Integer>();
		this.avgLobeDepths = new Vector<Double>();
		this.avgNeckDepths = new Vector<Double>();
		this.nonLobeAreaRatios = new Vector<Double>();
		this.longestPathLengths = new Vector<Double>();
		this.convexHullAreas = new Vector<Double>();
		this.convexHullPerimeters = new Vector<Double>();
		this.convexHullConvexities = new Vector<Double>();
		this.convexHullRoundnessValues = new Vector<Double>();

		this.fireOperatorExecutionProgressEvent(
				new ALDOperatorExecutionProgressEvent(this, operatorID 
					+ " calculating areas, perimeters, lengths, widths, " 
						+ "circularities and eccentricities..."));

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

		if (this.calcMarginRoughness || this.analyzeLobesAndNecks) 
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
			for (int i = 0; i<skeletonData.getRowCount(); ++i) {
				this.branchCounts.add(Double.valueOf(
					(String)skeletonData.getValueAt(i, branchCountIndex)));
				this.avgBranchLengths.add(Double.valueOf(
					(String)skeletonData.getValueAt(i, branchLengthIndex)));
				this.avgEndpointDistances.add(Double.valueOf(
					(String)skeletonData.getValueAt(i, branchDistIndex)));
				this.longestPathLengths.add(Double.valueOf(
					(String)skeletonData.getValueAt(i, branchLongestLengthIndex)));
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
					FeatureNames.AvgLobeRadius.toString() + " ("+ this.unitXY + ")" );
			header.add(
					FeatureNames.LongestPathLength.toString() + " ("+ this.unitXY + ")");
		}
		if (this.calcConcavityData) 
		{
			header.add(FeatureNames.AvgConcavity.toString());
			header.add(FeatureNames.StdDevConcavity.toString());
		}
		if (this.analyzeLobesAndNecks) {
			header.add(FeatureNames.LobeCount.toString());
			header.add(
					FeatureNames.AvgLobeDepth.toString() + " ("+ this.unitXY + ")" );
			header.add(
					FeatureNames.AvgNeckDepth.toString() + " ("+ this.unitXY + ")" );
			header.add(FeatureNames.NonLobeAreaRatio.toString());
		}
		if (this.calcConvexHullMeasures) {
			header.add(FeatureNames.ConvexHullArea.toString() 
					+ " ("+ this.unitXY + "^2)");
			header.add(FeatureNames.ConvexHullPerimeter.toString() 
					+ " ("+ this.unitXY + ")");
			header.add(FeatureNames.ConvexHullConvexity.toString());
			header.add(FeatureNames.ConvexHullRoundness.toString());
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
			if (this.analyzeLobesAndNecks) {
				this.table.setValueAt(this.nf.format(
						this.lobeCounts.elementAt(i)), i, col);
				col++;
				this.table.setValueAt(this.nf.format(
						this.avgLobeDepths.elementAt(i)), i, col);
				col++;
				this.table.setValueAt(this.nf.format(
						this.avgNeckDepths.elementAt(i)), i, col);
				col++;
				this.table.setValueAt(this.nf.format(
						this.nonLobeAreaRatios.elementAt(i)), i, col);
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
	 * Set regions.
	 * @param regs	Set of regions to process.
	 */
	public void setRegionSet(MTBRegion2DSet regs) 
	{
		this.regions = regs;
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
	 * Turn on/off analysis of contour lobes and necks.
	 * <p>
	 * The analysis of lobes and necks subsumes the overall count of 
	 * lobes, the average depth of lobes and necks, and the ratio between 
	 * non-lobe area within the cell and its overall size.
	 * 
	 * @param flag	If true, margin roughness values are calculated.
	 */
	public void setAnalyzeLobesAndNecks(boolean flag)
	{
		this.analyzeLobesAndNecks = flag;
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
	 * <li> Number of lobes and necks:<br>
	 * 	The sum of all lobes and necks is equal to the number of 
	 *  inflection points found along the contour, and the number of lobes
	 *  and necks, respectively, is each half of the total count of 
	 *  inflection points.
	 * <li> Average lobe and neck depths:<br>
	 *  For each lobe and neck, respectively, a line is drawn between the 
	 *  two inflection points defining the lobe or neck. Subsequently for 
	 *  each point of the lobe or neck considered the distance to the line 
	 *  is calculated. The largest distance found for a single lobe or 
	 *  neck defines the depth of the lobe or neck.
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
		
		int width = this.labelImg.getSizeX();
		int height = this.labelImg.getSizeY();
		
		if (this.createCurvatureInfoImage) {
			this.curvatureInfoImg = (MTBImageRGB)MTBImage.createMTBImage(
					width, height, 1, 1, 1, MTBImageType.MTB_RGB);
			for (int y=0;y<height;++y) {
				for (int x=0;x<width;++x) {
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
		
		if (this.analyzeLobesAndNecks) {
	    Vector< Vector<Point2D.Double> > inflectionPointLists = 
	    		new Vector< Vector<Point2D.Double> >();
	    
	    double lobeDepthSum = 0, neckDepthSum = 0;
	    int lobeCount = 0;
	    int cellID = 0;
	    Vector<int[]> curveDirections = new Vector<int[]>();
	    
	    // iterate over all contours and map curvatures to directions:
	    // 1 = pos. curvature, -1 = neg. curvature, 0 = below threshold
	    for (double[] curvVals: curvatureValues) {
	    	lobeDepthSum = 0;
	    	neckDepthSum = 0;
	    	int[] dirs = new int[curvVals.length];
	    	for (int j=0; j<curvVals.length; ++j) {
	    		double curvVal = curvVals[j];
	    		if (curvVal > 1) {
	    			dirs[j] = 1;
	    		}
	    		else if (curvVal < -1.0){
	    			dirs[j] = -1;
	    		}
	    		else {
	    			dirs[j] = 0;
	    		}    		
	    	}
	    	// map pixels with no direction to direction of closed contour
	    	// pixel with a clear direction
	    	int[] fixedDirs = new int[curvVals.length];
	    	for (int j=0; j<curvVals.length; ++j) {
	    		int curvVal = dirs[j];
	    		if (curvVal != 0) {
	    			fixedDirs[j] = curvVal;
	    			continue;
	    		}
	    		// search for the next pixel with direction to the left
	    		boolean foundLeft = false;
	    		int idLeft = 0;
	    		for (int l=j-1; !foundLeft && l!=j ; --l) {
	    			if (l < 0)
	    				l = dirs.length + l;
	    			if (dirs[l] != 0) {
	    				idLeft = l;
	    				foundLeft = true;
	    			}
	    		}
	    		// search for the next pixel with direction to the right
	    		boolean foundRight = false;
	    		int idRight = 0;
	    		for (int l=j+1; !foundRight && l!=j ; ++l) {
	    			if (l >= dirs.length)
	    				l = l - dirs.length;
	    			if (dirs[l] != 0) {
	    				idRight = l;
	    				foundRight = true;
	    			}
	    		}
	    		// check which is closer and set direction accordingly
	    		if (Math.abs(j - idLeft) < Math.abs(j - idRight)) {
	    			fixedDirs[j] = dirs[idLeft];
	    		}
	    		else if (Math.abs(j - idLeft) > Math.abs(j - idRight)) {
	    			fixedDirs[j] = dirs[idRight];
	    		}
	    		else {
	    			if (  Math.abs(curvVals[idLeft]) 
	    					> Math.abs(curvVals[idRight])) {
	      			fixedDirs[j] = dirs[idLeft];    				
	    			}
	    			else {
	    				fixedDirs[j] = dirs[idRight];
	    			}
	    		}
	    	}
	    	
	    	// increase robustness: 
	    	// check pixel count of lobes/necks, if too small, 
	    	// remove lobe/neck by inverting sign of their curvature
	    	removeShortLobes(fixedDirs, 5);
	    	
	    	// count sign changes along contour
	    	MTBContour2D c = contours.elementAt(cellID);
	    	Vector<Point2D.Double> inflections = 
	    			new Vector<Point2D.Double>();
	    	int signChangeCounter = 0;
	    	int sign = fixedDirs[fixedDirs.length-1];
	    	for (int j=0; j<fixedDirs.length; ++j) {
	    		if (fixedDirs[j] != sign) {
	    			++signChangeCounter;
	    			sign *= -1;
	    			inflections.add(c.getPointAt(j));
	    		}
	    	}
	    	lobeCount = (int)(signChangeCounter/2.0);
	    	this.lobeCounts.add(new Integer(lobeCount));

	    	// remember contour directions and inflection points
	    	curveDirections.add(fixedDirs);
				inflectionPointLists.add(inflections);
	    	
				// plot result to image if requested
				if (this.createCurvatureInfoImage) {
					Vector<Point2D.Double> ps = c.getPoints();
					int j=0;
					for (Point2D.Double p: ps) {
						int px = (int)p.x;
						int py = (int)p.y;
						if (fixedDirs[j] > 0) {
							this.curvatureInfoImg.putValueR(px, py, 255);
							this.curvatureInfoImg.putValueG(px, py, 0);
							this.curvatureInfoImg.putValueB(px, py, 0);
						}
						else if (fixedDirs[j] < 0) {
							this.curvatureInfoImg.putValueR(px, py, 0);
							this.curvatureInfoImg.putValueG(px, py, 0);
							this.curvatureInfoImg.putValueB(px, py, 255);                       
						}
						else {
							this.curvatureInfoImg.putValueR(px, py, 255);
							this.curvatureInfoImg.putValueG(px, py, 255);
							this.curvatureInfoImg.putValueB(px, py, 255);                                               
						}
						++j;
					}
					int blue = ((0 & 0xff)<<16)+((0 & 0xff)<<8) + (255 & 0xff);
					for (int k=0; k<inflections.size()-1; ++k) {
						int sx = (int)inflections.get(k).x;
						int sy = (int)inflections.get(k).y;
						int ex = (int)inflections.get(k+1).x;
						int ey = (int)inflections.get(k+1).y;
						this.curvatureInfoImg.drawLine2D(sx, sy, ex, ey, blue);
					}
					int sx = (int)inflections.get(inflections.size()-1).x;
					int sy = (int)inflections.get(inflections.size()-1).y;
					int ex = (int)inflections.get(0).x;
					int ey = (int)inflections.get(0).y;
					this.curvatureInfoImg.drawLine2D(sx, sy, ex, ey, blue);
				}

	    	// create polygon defined by inflection points, parts of the 
	    	// cell area referring to lobes are outside while neck areas
	    	// are still inside
	    	MTBPolygon2D poly = new MTBPolygon2D(inflections, true);
	    	int[][] polyMask = poly.getBinaryMask(width, height);
	    	
	    	// estimate non-lobe area, which is polygon area without necks
	    	int nonLobeArea = 0;
	    	for (int y=0;y<height;++y) {
	    		for (int x=0;x<width;++x) {
	    			// check if pixel is inside polygon and inside cell
	    			if (   polyMask[y][x] > 0 
	    					&& this.labelImg.getValueInt(x, y) == (cellID+1)) {
	    				++nonLobeArea;
	    				// mark non-lobe area in image
	    				if (this.createCurvatureInfoImage) {
								this.curvatureInfoImg.putValueR(x, y, 125);
								this.curvatureInfoImg.putValueG(x, y, 125);
								this.curvatureInfoImg.putValueB(x, y, 125);                                               
	    				}
	    			}
	    		}
	    	}
	    	// calculate ratio of non-lobe area in cell
				this.nonLobeAreaRatios.add(new Double(
						nonLobeArea*this.deltaXY.doubleValue()*this.deltaXY.doubleValue()
					/ this.areas.get(cellID).doubleValue())); 

				// process each lobe/neck and calculate depth
	    	int t=0;
	    	double d, pDist;
	    	boolean go = true;
	    	boolean reachedEnd = false;
	    	boolean processLobe, processNeck;
				Point2D.Double ep;
	    	while (go && !reachedEnd) {
	    		
	    		// get contour point to analyze next
	    		Point2D.Double p = c.getPointAt(t);
	    		
	    		// check if point is inflection
	    		if (inflections.contains(p)) {
	    			// check if a lobe starts here
	    			if (fixedDirs[t] > 0) {
	    				// get index in list
	    				int is = inflections.indexOf(p);
	    				// get endpoint of current lobe/neck
	    				if (is + 1 < inflections.size()-1) {
	    					ep = inflections.get(is+1);
	    				}
	    				else {
	    					ep = inflections.get(0);
	    				}
	    				// init connecting line
	    				Line2D.Double connectLine = 
	    						new Line2D.Double(p.x, p.y, ep.x, ep.y);
	    				
	    				pDist = 0;
	    				
	    				++t;
	    				if (t >= fixedDirs.length) {
	    					t = fixedDirs.length - t;
	    					reachedEnd = true;
	    				}
	    				processLobe = true;
	    				while (processLobe) {
	    					if (fixedDirs[t] <= 0.5) {
	    						processLobe = false;
	    						lobeDepthSum += pDist;
	    					}
	    					else {
	  	    				// get next point
	    						p = c.getPointAt(t);
	    						// calculate distance of point to connecting line
	    						d = connectLine.ptLineDist(p);
	    						if (d > pDist) {
	    							pDist = d;
	    						}
	    						
	    						++t;
	        				if (t >= fixedDirs.length) {
	        					t = fixedDirs.length - t;
	        					reachedEnd = true;
	        				}
	    					}
	    				}
	    			}
	    			else {
	    				int is = inflections.indexOf(p);
	    				if (is + 1 < inflections.size()-1) {
	    					ep = inflections.get(is+1);
	    				}
	    				else {
	    					ep = inflections.get(0);
	    				}
	    				// init connecting line
	    				Line2D.Double connectLine = 
	    						new Line2D.Double(p.x, p.y, ep.x, ep.y);

	    				pDist = 0;

	    				// get next point
	    				++t;
	    				if (t >= fixedDirs.length) {
	    					t = fixedDirs.length - t;
	    					reachedEnd = true;
	    				}
	    				processNeck = true;
	    				while (processNeck) {
	    					if (fixedDirs[t] >= -0.5) {
	    						processNeck = false;
	    						neckDepthSum += pDist;
	    					}
	    					else {
	    						p = c.getPointAt(t);
	    						// calculate distance of point to connecting line
	    						d = connectLine.ptLineDist(p);
	    						if (d > pDist)
	    							pDist = d;

	    						++t;
	    						if (t >= fixedDirs.length) {
	    							t = fixedDirs.length - t;
	    							reachedEnd = true;
	    						}
	    					}
	    				}
	  				}
	    		}
	    		else {
	    			++t;
	    		}
	    		if (t >= c.getPointNum())
	    			go = false;
	    	}
	    	this.avgLobeDepths.add(
	    			new Double( (lobeDepthSum*this.deltaXY.doubleValue()) / lobeCount));
	    	this.avgNeckDepths.add(
	    			new Double( (neckDepthSum*this.deltaXY.doubleValue()) / lobeCount));
	    	++cellID;
	    }
		}
	}	
	
	/**
	 * Function to remove all sub-sequences of ones shorter than the given
	 * minimum length by replacing them with -1.
	 * 
	 * @param dirArray	Array to modify.
	 * @param minLength	Minimal required length of sequences of ones.
	 */
	protected static void removeShortLobes(int[] dirArray, int minLength) {
		
		// iterate over the array until nothing changes anymore
  	int startPos = 0;
  	boolean changedSomething = true;
  	while (changedSomething) {
  		changedSomething = false;
  		
  		// get sign of first entry
  		int sign = dirArray[0];
  		
  		// remember start position of current sequence ...
  		startPos = 0;
  		
  		// ... and its length
  		int pixCount = 1;
  		
  		int j=1;
  		for (j=1; j<dirArray.length; ++j) {
  			// count following entries with identical sign
  			if (dirArray[j] == sign) {
  				++pixCount;
  			}
  			else {
  				// sign changes, but was a run of '-1' -> not of interest
  				if (sign == -1) {
  					pixCount = 1;
  					sign *= -1;
  					startPos = j;
  				}
  				// sign changes, check if run was long enough
  				// (if run is prefix of array, skip for now, we will test it later)
  				else {
  					if (    pixCount >= minLength 
  							|| (startPos == 0 && dirArray[dirArray.length-1] == sign)) {
  						// everything ok, just continue
  						sign *= -1;
  						pixCount = 1;
  						startPos = j;
  					}
  					else {
  						// lobe too small, remove it
  						sign *= -1;
  						for (int m=0;m<pixCount;++m) {
  							dirArray[startPos+m] = sign; 
  						}
  						changedSomething = true;
  						break;
  					}
  				}
  			}
  		}
  		// if we are at the end and in a 1-run, check if we can continue it
  		// at the beginning of the array, if not, remove it (if too short)
  		if (sign == 1 && j == dirArray.length && pixCount < minLength) {
  			int z=0;
  			for (z=0; z<dirArray.length; ++z) {
  				if (dirArray[z] == sign) {
  					++pixCount;
  				}
  				else {
  					break;
  				}
  			}
  			if (pixCount < minLength) {
  				sign *= -1;
  				for (int m=startPos;m<dirArray.length;++m) {
						dirArray[m] = sign; 
					}
  				for (int y=0;y<z;++y) {
						dirArray[y] = sign; 
					}
  			}
  		}
  	}		
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
	 * {@link #analyzeLobesAndNecks} or {@link #calcMarginRoughness}.
	 */
	@SuppressWarnings("unused")
	private void callbackCurvature() 
	{
    try {
  		if (this.calcMarginRoughness || this.analyzeLobesAndNecks) {
    		if (!this.hasParameter("gaussianSigma"))
    			this.addParameter("gaussianSigma");
    		if (!this.hasParameter("minimalCurvature"))
    			this.addParameter("minimalCurvature");
    	}
    	else if (!this.calcMarginRoughness && !this.analyzeLobesAndNecks){
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
