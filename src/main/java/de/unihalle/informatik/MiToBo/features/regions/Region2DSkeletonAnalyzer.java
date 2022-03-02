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

package de.unihalle.informatik.MiToBo.features.regions;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;
import java.util.Vector;

import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.Alida.operator.events.ALDOperatorExecutionProgressEvent;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBContour2D;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBContour2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBNeuriteSkelGraph;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2D;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageByte;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageDouble;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageRGB;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;
import de.unihalle.informatik.MiToBo.gui.MTBTableModel;
import de.unihalle.informatik.MiToBo.morphology.DistanceTransform;
import de.unihalle.informatik.MiToBo.morphology.SkeletonExtractor;
import de.unihalle.informatik.MiToBo.morphology.DistanceTransform.DistanceMetric;
import de.unihalle.informatik.MiToBo.morphology.DistanceTransform.ForegroundColor;
import de.unihalle.informatik.MiToBo.segmentation.contours.extraction.ContourOnLabeledComponents;
import de.unihalle.informatik.MiToBo.segmentation.contours.extraction.ContourOnLabeledComponents.ContourType;
import de.unihalle.informatik.MiToBo.segmentation.regions.labeling.LabelComponentsSequential;
import de.unihalle.informatik.MiToBo.segmentation.thresholds.ImgThresh;

/**
 * Operator to analyze skeletons of a given set of regions.
 * <p>
 * The operator takes as input a label image, calculates the skeleton
 * for each region and finally calculates several measures on the 
 * skeletons, e.g., the number of branches, their average lengths, or 
 * the average distances of the branch endpoints to the background. The 
 * skeleton is calculated using ImageJ functions. Inside the skeleton
 * 8-neighborhood between pixels is given.
 * <p>
 * Note that the number of branches is determined by the number of 
 * endpoints to be found in the skeleton. An endpoint is a skeleton 
 * point having no or only a single neighboring pixel in the skeleton,
 * or a point with two neighbors in direct vicinity. The latter case
 * can only appear if within the skeleton 4-neighborhood is assumed.<br> 
 * If we find more than two endpoints the count is equal to the number 
 * of branches, and if we find only two endpoints or even only one, 
 * we only have a single branch in the skeleton. 
 * <p>
 * The distance of an endpoint to the background can be interpreted as a 
 * rough estimation of the curvature of the contour surrounding the 
 * endpoint. The curvature can be defined as the radius of a maximal 
 * circle located at the branch endpoint, and the distance to the 
 * background is approximately equal to the radius of this circle.
 * 
 * @author moeller
 */
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.ALL, 
	level=Level.STANDARD, allowBatchMode = false,
	shortDescription="Analyzes skeletons of a given set of regions.")
public class Region2DSkeletonAnalyzer extends MTBOperator {
	
	/**
	 * Operator class identifier.
	 */
	private static final String operatorID = "[Region2DSkeletonAnalyzer]";
	
	/**
	 * Definition of red color.
	 */
	private static final int red = 
			((255 & 0xff)<<16)+((0 & 0xff)<<8) + (0 & 0xff);

	/**
	 * Definition of yellow color.
	 */
	private static final int yellow = 
			((255 & 0xff)<<16)+((255 & 0xff)<<8) + (0 & 0xff);
	
	/**
	 * Definition of green color.
	 */
	private static final int green = 
			((0 & 0xff)<<16)+((255 & 0xff)<<8) + (0 & 0xff);

	/**
	 * Definition of blue color.
	 */
	private static final int blue = 
			((0 & 0xff)<<16)+((0 & 0xff)<<8) + (255 & 0xff);

	/**
	 * Set of region features calculated from region skeletons.
	 * <p>
	 * These names are also used as headers in the result table. 
	 */
	public static enum FeatureNames {
		/**
		 * Integer ID of region (for reference purposes). 
		 */
		RegionID,
		/**
		 * Number of branches. 
		 */
		BranchCount,
		/**
		 * Average lengths of branches.
		 */
		AvgBranchLength,
		/**
		 * Average distance of branch endpoints to background.
		 */
		AvgBranchEndpointDistance,
		/**
		 * Longest path in skeleton.
		 */
		LongestSkeletonPathLength,
		/**
		 * First quartile width of core region (non-branch section).
		 */
		MinCoreRegionWidth,
		/**
		 * Thrid quartile width of core region (non-branch section).
		 */
		MaxCoreRegionWidth,
		/**
		 * Radius of maximal empty circle inscribed in region.
		 */
		LargestEmptyCircle
	}

	/**
	 * Label image to process.
	 * <p>
	 * It is assumed that each region is marked by a unique label larger
	 * than zero. The background should have zero values.
	 */
	@Parameter(label = "Input Label Image", required = true, 
			direction = Parameter.Direction.IN, description = "Input image.",
			dataIOOrder = 0)
	private transient MTBImage inImg = null;

	/**
	 * Length of a pixel.
	 * <p>
	 * Note that we assume that a pixel is square, i.e. has an aspect ratio 
	 * of 1. If this is not the case, extracted values are not correct.
	 */
	@Parameter(label = "Pixel length", required = false, 
			direction = Parameter.Direction.IN, description = "Pixel length.",
			dataIOOrder = 0)
	private double pixelLength = 1.0;

	/**
	 * Table with region skeleton features.
	 * <p>
	 * Each row contains one region, each column corresponds to a feature.
	 * The region IDs are identical to the labels in the input image.
	 * <p>
	 * The first two columns of the table corresponding to the region ID
	 * and the branch count contain values of type {@link Integer}, 
	 * the last two columns contain values of type {@link Double}.
	 */
	@Parameter(label = "Result Table of Skeleton Features", 
			dataIOOrder = 0, direction = Parameter.Direction.OUT,
			description = "Result table of skeleton features.")
	private transient MTBTableModel resultFeatureTable = null;

	/**
	 * Table with additional meta data of region skeleton features.
	 * <p>
	 * Each row contains one region, each column corresponds to some additional information.
	 * The region IDs are identical to the labels in the input image.
	 * <p>
	 * The first column of the table corresponds to the region ID.
	 */
	@Parameter(label = "Result Metadata Table of Skeleton Features", 
			dataIOOrder = 0, direction = Parameter.Direction.OUT,
			description = "Result table with skeleton feature meta data.")
	private transient MTBTableModel resultMetaDataTable = null;

	/**
	 * Enable/disable the creation of an image to visualize results.
	 */
	@Parameter(label = "Visualize analysis results?", dataIOOrder = 0,
			direction = Parameter.Direction.IN, supplemental = true,
			description = "If selected an image showing analysis results " 
					+ "is created.")
	private boolean visualizeAnalysisResults = false;

	/**
	 * Image illustrating analysis results.
	 */
	@Parameter(label = "Image showing analysis results", dataIOOrder = 1,
			direction = Parameter.Direction.OUT, 
			description = "Image illustrating the results of the analysis.")
	private transient MTBImageRGB analysisDisplayImg = null;

	/**
	 * Width of input label image.
	 */
	private transient int width;
	
	/**
	 * Height of input label image.
	 */
	private transient int height;

	/**
	 * Internal image to work on.
	 */
	private MTBImage workImage;
	
	/**
	 * Default constructor.
	 * 
	 * @throws ALDOperatorException Thrown on construction failure.
	 */
	public Region2DSkeletonAnalyzer() throws ALDOperatorException {
		// nothing to do here
	}

	/**
	 * Set input label image to process.
	 * 
	 * @param img Input label image.
	 */
	public void setInputLabelImage(MTBImage img) {
		this.inImg = img;
	}
	
	/**
	 * Set length of a pixel.
	 * @param pl	Length of a pixel.
	 */
	public void setPixelLength(double pl) {
		this.pixelLength = pl;
	}

	/**
	 * Enable/disable creation of image with analysis data.
	 * 
	 * @param flag If true, creation of image is enabled.
	 */
	public void setVisualizeAnalysisResults(boolean flag) {
		this.visualizeAnalysisResults = flag;
	}
	
	/**
	 * Get table of calculated features.
	 * 
	 * @return Table with feature values.
	 */
	public MTBTableModel getResultTable() {
		return this.resultFeatureTable;
	}

	/**
	 * Get table with meta data supplementing calculated features.
	 * 
	 * @return Table with meta data.
	 */
	public MTBTableModel getResultMetaDataTable() {
		return this.resultMetaDataTable;
	}

	/**
	 * Get the info image with analysis data visualized.
	 * <p>
	 * Note, the image is only available if 
	 * {@link #visualizeAnalysisResults} was set to true before.
	 * 
	 * @return Info image.
	 */
	public MTBImageRGB getInfoImage() {
		return this.analysisDisplayImg;
	}

	/**
	 * Get image with visualized analysis results.
	 * <p>
	 * This image is only available if the parameter 
	 * {@link #visualizeAnalysisResults} has been selected prior to 
	 * running the operator.
	 * 
	 * @return Image illustrating analysis results.
	 */
	public MTBImageRGB getAnalysisImage() {
		return this.analysisDisplayImg;
	}
	
	/* (non-Javadoc)
	 * @see de.unihalle.informatik.Alida.operator.ALDOperator#operate()
	 */
	@Override
	protected void operate() 
			throws ALDOperatorException, ALDProcessingDAGException {

		// set some variables
		this.width = this.inImg.getSizeX();
		this.height = this.inImg.getSizeY();

		// relabel image to ensure consecutive labeling
		LabelComponentsSequential labler = new LabelComponentsSequential();
		labler.setInputImage(this.inImg);
		labler.setDiagonalNeighborsFlag(true);
		labler.runOp();
		this.workImage = labler.getLabelImage();

		// extracting boundaries
//		BordersOnLabeledComponents cExtractor = new BordersOnLabeledComponents();
//		cExtractor.setBorderType(BorderType.OUTER_BORDERS);
//		cExtractor.setInputImage((MTBImageByte)this.workImage.convertType(MTBImageType.MTB_BYTE, true));
//		cExtractor.runOp();
//		MTBBorder2DSet contours = cExtractor.getResultBorders();

		ContourOnLabeledComponents cExtractor = new ContourOnLabeledComponents();
		cExtractor.setContourType(ContourType.OUTER_CONTOUR);
		cExtractor.setInputImage((MTBImageByte)this.workImage.convertType(MTBImageType.MTB_BYTE, true));
		cExtractor.setInputRegions(labler.getResultingRegions());
		cExtractor.runOp();
		MTBContour2DSet contours = cExtractor.getResultContours();

		this.fireOperatorExecutionProgressEvent(
				new ALDOperatorExecutionProgressEvent(this, operatorID 
					+ " binarizing label image..."));

		ImgThresh thresholder = new ImgThresh(this.workImage, 0);
		thresholder.runOp(HidingMode.HIDE_CHILDREN);
		MTBImageByte binImg = (MTBImageByte)thresholder.getResultImage();
		
		this.fireOperatorExecutionProgressEvent(
				new ALDOperatorExecutionProgressEvent(this, operatorID 
					+ " performing distance transform..."));

		DistanceTransform dTrans = new DistanceTransform(binImg, 
				DistanceMetric.EUCLIDEAN, ForegroundColor.FG_BLACK);
		dTrans.runOp(HidingMode.HIDE_CHILDREN);
		MTBImage distTransImg = dTrans.getDistanceImage();

		this.fireOperatorExecutionProgressEvent(
				new ALDOperatorExecutionProgressEvent(this, operatorID 
					+ " extracting region skeletons..."));

		SkeletonExtractor skelOp = new SkeletonExtractor();
		skelOp.setInputImage(binImg);
		skelOp.runOp(HidingMode.HIDE_CHILDREN);
		MTBImageByte skelImg = skelOp.getResultImage();
		
		if (this.visualizeAnalysisResults) {
			// init info image
			this.analysisDisplayImg = (MTBImageRGB)MTBImage.createMTBImage(
					this.width, this.height, 1, 1, 1, MTBImageType.MTB_RGB);
			// fill image with value = 200
			int grayVal = 200;			
			for (int y=0; y<this.height; ++y) {
				for (int x=0; x<this.width; ++x) {
					this.analysisDisplayImg.putValue(x, y,
							grayVal, grayVal, grayVal);
				}
			}
			// mark skeleton in red
			for (int y=0; y<this.height; ++y) {
				for (int x=0; x<this.width; ++x) {
					grayVal = this.workImage.getValueInt(x, y);
					if (grayVal == 0) 
						this.analysisDisplayImg.putValue(x, y, 
							grayVal, grayVal, grayVal);
					else
						this.analysisDisplayImg.putValue(x, y, 125, 125, 125);						
					// mark skeleton pixels in red
					if (skelImg.getValueInt(x, y) > 0)
						this.analysisDisplayImg.putValueInt(x, y, red);
				}
			}
		}
		
		// search for maximum label in input image
		int maxLabel = 0;
		for (int y=0;y<this.height;++y) {
			for (int x=0;x<this.width;++x) {
				if (this.workImage.getValueInt(x, y) > maxLabel)
					maxLabel = this.workImage.getValueInt(x, y);
			}
		}

		// search for endpoints and trace skeletons for each region
		Vector<Point2D.Double> branch;
		double size;
		boolean done;
		
		double[] longestPathLengths = new double[maxLabel+1];
		ArrayList<Vector<Point2D.Double>> longestPath = 
				new ArrayList<Vector<Point2D.Double>>();
		for (int i=0; i<maxLabel; ++i)
			longestPath.add(new Vector<Point2D.Double>());
		int nCount = 0, nSum = 0;
		for (int y=0; y<this.height; ++y) {
			for (int x=0; x<this.width; ++x) {
				// only foreground pixels are relevant
				if (skelImg.getValueInt(x, y) > 0) {
					// count the neighbors, check if we have an endpoint
					nCount = 0; nSum = 0;
					for (int dx=-1;dx<=1;++dx) {
						
						if (x+dx < 0 || x+dx >= this.width)
							continue;
						
						for (int dy=-1;dy<=1;++dy) {
								
							if ( y+dy < 0 || y+dy >= this.height) 
								continue;
							
							// ignore the pixel itself
							if (dx == 0 && dy == 0)
								continue;
							
							if (skelImg.getValueInt(x+dx, y+dy) > 0) {
								++nCount;
								if (dx == -1) {
									if (dy == -1)
										nSum += 128;
									else if (dy == 0)
										nSum += 64;
									else // dy == 1
										nSum += 32;
								}
								else if (dx == 0) {
									if (dy == -1)
										nSum += 1;
									else if (dy == 1)
										nSum += 16;
								}
								else { // dx == 1
									if (dy == -1)
										nSum += 2;
									else if (dy == 0)
										nSum += 4;
									else // dy == 1
										nSum += 8;
								}
							}
						}
					}
					if (        nCount==1 
							|| (    nCount==2 
						     && (nSum==3  || nSum==6  || nSum==12  || nSum==24 
						      || nSum==48 || nSum==96 || nSum==192 || nSum==129))) {
						MTBNeuriteSkelGraph nsg = new MTBNeuriteSkelGraph(); 
						done = nsg.buildSkeletonGraph(x, y, this.width, 
								this.height, skelImg, 255);
						
						if (done) {
							branch = nsg.getLongestPath(true);
							size = this.calcBranchLength(branch);
							if (size > longestPathLengths[this.workImage.getValueInt(x, y)]) {
								longestPathLengths[this.workImage.getValueInt(x, y)] = size; 
								longestPath.set(this.workImage.getValueInt(x, y)-1,branch);
							}
						}
					}
				}
			}
		}
		
		// plot longest branch to image
		if (this.visualizeAnalysisResults) {
			for (int i=0;i<maxLabel;++i) {
				if (longestPath.get(i).size() > 0) {
					for (Point2D.Double p: longestPath.get(i)) { 
						this.analysisDisplayImg.putValueInt(
								(int)p.x, (int)p.y, blue);
					}
				}
			}
		}
		
		// init working image and some helper variables
		MTBImage memImg = skelImg.duplicate();
		int[] branchCounts = new int[maxLabel];
		double[] branchLengths = new double[maxLabel];
		double[] branchDistances = new double[maxLabel];
		boolean[] regionWithLabelFound = new boolean[maxLabel];
		
		// check which labels are present
		for (int i=0; i<maxLabel; ++i)
			regionWithLabelFound[i] = false;
		for (int y=0;y<this.height;++y) {
			for (int x=0;x<this.width;++x) {
				if (this.workImage.getValueInt(x, y) > 0)
					regionWithLabelFound[this.workImage.getValueInt(x, y)-1] = true;
			}
		}
		
		// count endpoints and trace branches from endpoint to branch point
		double length;
		for (int y=0;y<this.height;++y) {
			for (int x=0;x<this.width;++x) {
				if (   skelImg.getValueInt(x, y) > 0 
						&& memImg.getValueInt(x, y) > 0) {
					nCount = numberOfNeighbors(skelImg, x, y);
					if (nCount == 0) {
						// we have only a single skeleton point which is by our definition
						// also a branch (appears, e.g., in skeletons of perfect circles)
						++branchCounts[this.workImage.getValueInt(x, y)-1];
					}
					else if (nCount == 1) {
						// found an endpoint, mark as processed
						memImg.putValueInt(x, y, 0);
						++branchCounts[this.workImage.getValueInt(x, y)-1];
						
						// calculate branch length...
						length = traceBranch(skelImg, x, y, null);
						branchLengths[this.workImage.getValueInt(x, y)-1] += length;
						
						// ... and endpoint distance to background
						branchDistances[this.workImage.getValueInt(x, y)-1] += 
								distTransImg.getValueInt(x, y) * this.pixelLength;
						
						// visualize if requested
						if (this.visualizeAnalysisResults) {
							// mark endpoint in red
							this.analysisDisplayImg.putValue(x, y, 255, 0, 0);
							// draw endpoint distance circle in green
							this.analysisDisplayImg.drawCircle2D(x, y, 0, 
									distTransImg.getValueInt(x, y), green);
						}
					}	
				}
			}
		}
		// calculate real branch counts:
		// if there are more than two, endpoint count is equal to branch count,
		// if there are two or even less, we have only a single branch 
		// calculate average values
		for (int i=0;i<branchLengths.length;++i) {
			if (branchCounts[i] > 3)
				continue;
			if (branchCounts[i] < 2)
				branchCounts[i] = 1;
		}
		for (int i=0;i<branchLengths.length;++i) {
			branchLengths[i] /= branchCounts[i];
			branchDistances[i] /= branchCounts[i];
		}
		// if we have two endpoints, the branch is traced twice, so we need to 
		// divide by two for the average length and can only correct the count
		// now
		for (int i=0;i<branchLengths.length;++i) {
			if (branchCounts[i] == 2)
				branchCounts[i] = 1;
		}
		
		/* Extract dimensions of core region: 
		   The dimensions of the core region are extracted based on the skeleton
		   image. First the skeleton is extracted, then the distance of each
		   skeleton point to the closest background pixel is extracted. 
		   Subsequently local distance maxima along the skeleton are found,
		   and the minimal and maximal values of these maxima define the minimal
		   and maximal extension of the core region.  */ 
		
		// analyze input image for bounding boxes of individual regions
		int label;
		
		int[][] regionBoxes = new int[maxLabel+1][];
		for (int i=0;i<maxLabel+1;++i)
			regionBoxes[i] = null;
		for (int y=0;y<this.height;++y) {
			for (int x=0;x<this.width;++x) {
				label = this.workImage.getValueInt(x, y);
				
				// ignore background pixels
				if (label == 0)
					continue;
				
				if (regionBoxes[label] == null)
					// Array: [xmin, ymin, xmax, ymax]
					regionBoxes[label] = new int[]{x,y,x,y}; 
				else {
					int[] dims = regionBoxes[label];
					if (x < dims[0])
						dims[0] = x;
				  if (x > dims[2])
						dims[2] = x;
					if (y < dims[1])
						dims[1] = y;
					if (y > dims[3])
						dims[3] = y;
				}
			}
		}

		// enlarge bounding boxes if possible
		int dims[];
		int xmin, xmax, ymin, ymax;
		// start with 1, label 0 is background
		for (int i=1; i<regionBoxes.length; ++i) {
			dims = regionBoxes[i];
			xmin = dims[0]-1;
			xmax = dims[2]+1;
			ymin = dims[1]-1;
			ymax = dims[3]+1;
			if (xmin < 0) xmin = 0;
			if (ymin < 0) ymin = 0;
			if (xmax >= this.width) xmax = this.width-1;
			if (ymax >= this.height) ymax = this.height-1;
			dims[0] = xmin;
			dims[1] = ymin;
			dims[2] = xmax;
			dims[3] = ymax;
		}

		// skeleton image containing only pixels not belonging to a branch,
		// where all branch pixels are to be deleted
		MTBImageByte nonBranchPixelImg = 
			(MTBImageByte)skelImg.duplicate().convertType(MTBImageType.MTB_BYTE, true);
		
		// extract set of components from skeleton image
		labler.setInputImage(skelImg);
		labler.setDiagonalNeighborsFlag(true);
		labler.runOp();
		MTBRegion2DSet skeletonRegs = labler.getResultingRegions();
		
		// extract branches of the skeleton and delete corresponding pixels
		memImg = skelImg.duplicate();
		for (int y=0;y<this.height;++y) {
			for (int x=0;x<this.width;++x) {
				if (   skelImg.getValueInt(x, y) > 0 
						&& memImg.getValueInt(x, y) > 0) {
					nCount = numberOfNeighbors(skelImg, x, y);
					if (nCount == 1) {
						// found an endpoint, mark as processed
						memImg.putValueInt(x, y, 0);

						// calculate branch length...
						length = traceBranch(skelImg, x, y, nonBranchPixelImg);
					}
				}
			}
		}

		// check that for each skeleton region at least one pixel survived;
		// if not, re-add the complete skeleton to the image as the skeleton
		// of the region just consists of a single branch that should be kept
		for (MTBRegion2D r: skeletonRegs) {
			boolean pointFound = false;
			for (Point2D.Double p: r.getPoints()) {
				if (nonBranchPixelImg.getValueInt((int)p.x, (int)p.y) > 0) {
					pointFound = true;
					break;
				}
			}
			if (!pointFound) {
				for (Point2D.Double p: r.getPoints()) {
					nonBranchPixelImg.putValueInt((int)p.x, (int)p.y, 255);
				}				
			}
		}
		
		// determine radii of maximum inscribed circles by finding for each region 
		// skeleton point with maximum minimal distance to background; 
		// in parallel store distances of non-branch skeleton pixels to background
		double minSkelPointDist, dist;
		double[] radiiMaxInscribedCircles = new double[maxLabel+1];
		double[] maxDistPerRegion = new double[maxLabel+1];
		Point2D.Double localSkelMinPoint = null;
		Point2D.Double[] maxDistPoints = new Point2D.Double[maxLabel+1];
		MTBImageDouble nonBranchDistImg = (MTBImageDouble)MTBImage.createMTBImage(
		 		this.width, this.height, 1, 1, 1, MTBImageType.MTB_DOUBLE); 

		int px, py;
		MTBContour2D c;
		MTBImageDouble distImage = (MTBImageDouble)skelImg.convertType(
				MTBImageType.MTB_DOUBLE,  false);
		for (int y=0; y<this.height; ++y) {
			for (int x=0; x<this.width; ++x) {
				distImage.putValueDouble(x, y, -1);
			}
		}
		for (int y=0; y<this.height; ++y) {
			for (int x=0; x<this.width; ++x) {
				// only consider skeleton pixels
				if (skelImg.getValueInt(x, y) > 0) {
					for (int dy=-5; dy<=5; ++dy) {
						
						if (dy+y < 0 || dy+y >= this.workImage.getSizeY())
							continue;
						
						for (int dx=-5; dx<=5; ++dx) {
						
							if (dx+x < 0 || dx+x >= this.workImage.getSizeX()) 
								continue;
							
							label = this.workImage.getValueInt(dx+x, dy+y);
							if (label == 0)
								continue;
							
							minSkelPointDist = Double.MAX_VALUE;
							if (distImage.getValueDouble(dx+x, dy+y) != -1) {
								minSkelPointDist = distImage.getValueDouble(dx+x, dy+y);
							}
							else {
								c = contours.elementAt(label-1);
								for (Point2D.Double p: c.getPoints()) {
									
									// if contour point is already too far away, no new minimum will be found
									if ( (dx+x-p.x)*(dx+x-p.x) + (dy+y-p.y)*(dy+y-p.y) > minSkelPointDist + 4)
										continue;
									
									for (int by=-1;by<=1;++by) {
										
										py = (int)(p.y+by);
										if (py < 0 || py >= this.workImage.getSizeY())
											continue;

										for (int bx=-1;bx<=1;++bx) {
											
											px = (int)(p.x+bx);
											if (px < 0 || px >= this.workImage.getSizeX())
												continue;
											
											if (this.workImage.getValueInt(px, py) > 0)
												continue;
											
											dist = (dx+x-px)*(dx+x-px) + (dy+y-py)*(dy+y-py);
											if (dist < minSkelPointDist) {
												minSkelPointDist = dist;
												localSkelMinPoint = new Point2D.Double(dx+x, dy+y);
											}
										}
									}
								}
								distImage.putValueDouble(dx+x, dy+y, minSkelPointDist);
							}
							if (minSkelPointDist > maxDistPerRegion[label]) {
								maxDistPoints[label] = localSkelMinPoint;
								maxDistPerRegion[label] = minSkelPointDist;
							}
							// remember final non-squared distance (in pixels) for non-branch pixels
							if (dx==0 && dy==0 && nonBranchPixelImg.getValueInt(x, y) != 0)
								nonBranchDistImg.putValueDouble(x, y, Math.sqrt(minSkelPointDist));
						}
					}
				}
			}
		}
		
		for (int l=1; l<=maxLabel; ++l) {
			radiiMaxInscribedCircles[l] = 
					Math.sqrt(maxDistPerRegion[l]) * this.pixelLength;
			if (this.visualizeAnalysisResults) {
				this.analysisDisplayImg.drawPoint2D(
					(int)maxDistPoints[l].x, (int)maxDistPoints[l].y, 0, 0xFFFF00, 1);
				this.analysisDisplayImg.drawCircle2D(
					(int)maxDistPoints[l].x, (int)maxDistPoints[l].y, 0, 
						(int)Math.sqrt(maxDistPerRegion[l]), yellow);
			}
		}
		
		// calculate distances to the background for all non-branch pixels
		// MTBImageDouble nonBranchDistImg = (MTBImageDouble)MTBImage.createMTBImage(
		// 		this.width, this.height, 1, 1, 1, MTBImageType.MTB_DOUBLE); 
		// Point2D.Double[][] nonBranchDistImgPoints = 
		// 		new Point2D.Double[this.width][this.height];
		// double minDist;
		// Integer label;
		// for (int y=0;y<this.height;++y) {
		// 	for (int x=0;x<this.width; ++x) {
		// 		if (nonBranchPixelImg.getValueInt(x, y) != 0) {
		// 			// search only inside the bounding box of this region (plus one pix)
		// 			label = new Integer(this.inImg.getValueInt(x, y));
		// 			dims = regionBoxes.get(label);
		// 			xmin = dims[0];
		// 			xmax = dims[2];
		// 			ymin = dims[1];
		// 			ymax = dims[3];
		// 			minDist = Double.MAX_VALUE;
		// 			for (int yy=ymin; yy<=ymax; ++yy) {
		// 				for (int xx=xmin; xx<=xmax; ++xx) {
		// 					// ignore non-background pixels
		// 					if (binImg.getValueInt(xx, yy) != 0)
		// 						continue;
		// 					dist = (x-xx)*(x-xx) + (y-yy)*(y-yy);
		// 					if (dist < minDist) {
		// 						minDist = dist;
		// 						nonBranchDistImgPoints[x][y] = new Point2D.Double(xx, yy);
		// 					}
		// 				}
		// 			}
		// 			// remember final non-squared distance (in pixels)
		// 			nonBranchDistImg.putValueDouble(x, y, Math.sqrt(minDist));
		// 		}
		// 	}
		// }

		// collect set of distances along non-branch skeleton
		Double distVal;
		Integer labelID;
		HashMap<Integer, Double> quartileFirst = new HashMap<>();
		HashMap<Integer, Double> quartileThird = new HashMap<>();
		HashMap<Integer, ArrayList<Double>> distances = 
				new HashMap<Integer, ArrayList<Double>>();
		for (int y=0;y<this.height;++y) {
			for (int x=0;x<this.width; ++x) {
				if (nonBranchDistImg.getValueDouble(x, y) > 0) {
					labelID = Integer.valueOf(this.workImage.getValueInt(x, y));
					distVal = Double.valueOf(nonBranchDistImg.getValueDouble(x, y));
					if (!distances.containsKey(labelID)) {
						distances.put(labelID, new ArrayList<Double>());
					}
					distances.get(labelID).add(distVal);										
				}
			}
		}
		
		Object[] sortedArray;
		Set<Integer> keys = distances.keySet();
		int qFirstID, qThirdID;
		for (Integer k: keys) {
			sortedArray = distances.get(k).toArray(); 
			Arrays.sort(sortedArray);
			qFirstID = (int)(sortedArray.length * 1.0 / 4.0);
			qThirdID = (int)(sortedArray.length * 3.0 / 4.0);
			quartileFirst.put(k, (Double)sortedArray[qFirstID]);
			quartileThird.put(k, (Double)sortedArray[qThirdID]);
		}
		
		// allocate and fill result table
		int regionCount = 0;
		for (int i=0;i<regionWithLabelFound.length;++i) {
			if (regionWithLabelFound[i])
				++regionCount;
		}
		this.resultFeatureTable = new MTBTableModel(regionCount, 8);
		this.resultFeatureTable.setColumnName(0, 
				FeatureNames.RegionID.toString());
		this.resultFeatureTable.setColumnName(1, 
				FeatureNames.BranchCount.toString());
		this.resultFeatureTable.setColumnName(2, 
				FeatureNames.AvgBranchLength.toString());
		this.resultFeatureTable.setColumnName(3, 
				FeatureNames.AvgBranchEndpointDistance.toString());
		this.resultFeatureTable.setColumnName(4, 
				FeatureNames.LongestSkeletonPathLength.toString());
		this.resultFeatureTable.setColumnName(5, 
				FeatureNames.MinCoreRegionWidth.toString());
		this.resultFeatureTable.setColumnName(6, 
				FeatureNames.MaxCoreRegionWidth.toString());
		this.resultFeatureTable.setColumnName(7, 
				FeatureNames.LargestEmptyCircle.toString());
		int rowID = 0;
		for (int i=0; i<regionWithLabelFound.length; ++i) {
			if (!regionWithLabelFound[i]) {
				continue;
			}
			this.resultFeatureTable.setValueAt(Integer.toString(i+1),rowID,0);
			this.resultFeatureTable.setValueAt(
					Integer.toString(branchCounts[i]), rowID, 1);
			this.resultFeatureTable.setValueAt(
					Double.toString(branchLengths[i]), rowID, 2);
			this.resultFeatureTable.setValueAt(
					Double.toString(branchDistances[i]), rowID, 3);
			this.resultFeatureTable.setValueAt(
					Double.toString(longestPathLengths[i+1]), rowID, 4);
			// set core region dimensions, note that estimated distances 
			// refer to half of the actual widths, i.e. are multiplied by 2 here
			if (quartileFirst.get(Integer.valueOf(i+1)) == null)
				this.resultFeatureTable.setValueAt(Double.valueOf(Double.NaN), rowID, 5);
			else
				this.resultFeatureTable.setValueAt(Double.toString(
					quartileFirst.get(Integer.valueOf(i+1)).doubleValue() 
						* 2.0 * this.pixelLength), rowID, 5);
			if (quartileThird.get(Integer.valueOf(i+1)) == null)
				this.resultFeatureTable.setValueAt(Double.valueOf(Double.NaN), rowID, 6);
			else
				this.resultFeatureTable.setValueAt(Double.toString(
					quartileThird.get(Integer.valueOf(i+1)).doubleValue()
						* 2.0 * this.pixelLength), rowID, 6);
			this.resultFeatureTable.setValueAt(
				Double.toString(radiiMaxInscribedCircles[i+1]), rowID, 7);
			++rowID;
		}
		
		// fill meta data table
		this.resultMetaDataTable = new MTBTableModel(regionCount, 4);
		this.resultMetaDataTable.setColumnName(0, 
				FeatureNames.RegionID.toString());
		this.resultMetaDataTable.setColumnName(1, "LEC_x");
		this.resultMetaDataTable.setColumnName(2, "LEC_y");
		this.resultMetaDataTable.setColumnName(3, "LEC_r"); 
		rowID = 0;
		for (int i=0; i<regionWithLabelFound.length; ++i) {
			if (!regionWithLabelFound[i])
				continue;
			this.resultMetaDataTable.setValueAt(Integer.toString(i+1),rowID,0);
			this.resultMetaDataTable.setValueAt(
					Integer.toString(((int)maxDistPoints[i+1].x)), rowID, 1);
			this.resultMetaDataTable.setValueAt(
					Integer.toString(((int)maxDistPoints[i+1].y)), rowID, 2);
			this.resultMetaDataTable.setValueAt(
					Double.toString(Math.sqrt(maxDistPerRegion[i+1])), rowID, 3);
			++rowID;
		}

	}
	
	/**
	 * Counts the number of neighboring skeleton pixels.
	 * <p>
	 * The 8-neighborhood is considered here.
	 * 
	 * @param img	Skeleton image.
	 * @param x		x-coordinate of pixel to examine.
	 * @param y		y-coordinate of pixel to examine.
	 * @return	Number of neighbors found.
	 */
	private static int numberOfNeighbors(MTBImage img, int x, int y) {
		// init some local variables
		int width = img.getSizeX();
		int height = img.getSizeY();
		int nCount = 0;
		for (int dx=-1;dx<=1;++dx) {
			for (int dy=-1;dy<=1;++dy) {
				// skip pixel itself
				if (dx == 0 && dy == 0)
					continue;
				if (   x+dx >= 0 && x+dx < width && y+dy >= 0 && y+dy < height 
						&& img.getValueInt(x+dx, y+dy) > 0) {
					++nCount;
				}
			}
		}
		return nCount;
	}
	
	/**
	 * Traces a skeleton branch and extracts its length.
	 * <p>
	 * The tracing starts at the given position and ends at the next 
	 * branch point, i.e., the next pixel having more than one neighbor.
	 * 
	 * @param img									Skeleton image.
	 * @param x										x-coordinate where to start.
	 * @param y										y-coordinate where to start.
	 * @param nonBranchPixelImg 	Image where to erase branch pixels (optional).
	 * @return	Length of the branch.
	 */
	private double traceBranch(MTBImage img, int x, int y, 
			MTBImage nonBranchPixelImg) {
		
		int iwidth = img.getSizeX();
		int iheight = img.getSizeY();
		
		boolean tracingActive = true;
		double length = 0;
		int nx = x, ny = y;

		// init working image and mark point as processed
		MTBImage memImg = img.duplicate();
		memImg.putValueInt(x, y, 0);
		if (nonBranchPixelImg != null)
			nonBranchPixelImg.putValueInt(x, y, 0);

		// search next neighbor point
		boolean neighborFound = false;
		for (int dx=-1; !neighborFound && dx<=1; ++dx) {
			for (int dy=-1; !neighborFound && dy<=1; ++dy) {
				if (dx == 0 && dy == 0)
					continue;
				if (   nx+dx >= 0 && nx+dx < iwidth 
						&& ny+dy >= 0 && ny+dy < iheight
						&& memImg.getValueInt(nx+dx, ny+dy) > 0) {
					neighborFound = true;
					nx = nx+dx;
					ny = ny+dy;
					if (dx == 0 || dy == 0)
						length += this.pixelLength;
					else
						length += Math.sqrt(2.0) * this.pixelLength;
				}
			}
		}

		// continue tracing
		while(tracingActive) {
			// mark current point as processed
			memImg.putValueInt(nx, ny, 0);
			if (nonBranchPixelImg != null)
				nonBranchPixelImg.putValueInt(nx, ny, 0);
			int nCount = numberOfNeighbors(memImg, nx, ny);
			if (nCount == 1) {
				// continue path
				neighborFound = false;
				for (int dx=-1; !neighborFound && dx<=1; ++dx) {
					for (int dy=-1; !neighborFound && dy<=1; ++dy)	 {
						if (dx == 0 && dy == 0)
							continue;
						if (   nx+dx >= 0 && nx+dx < iwidth 
								&& ny+dy >= 0 && ny+dy < iheight
								&& memImg.getValueInt(nx+dx, ny+dy) > 0) {
							neighborFound = true;
							nx = nx+dx;
							ny = ny+dy;
							if (dx == 0 || dy == 0)
								length += this.pixelLength;
							else
								length += Math.sqrt(2.0) * this.pixelLength;
						}
					}
				}
			}
			else if (nCount == 0) {
				tracingActive = false;
			}
			else {
				// either reached another endpoint or a branch point
				tracingActive = false;
			}
		}
		return length;
	}
	
	/**
	 * Calculates the lenght of the given branch.
	 * <p>
	 * The length is calculated by tracing the point list and summing up the 
	 * distances between consecutive pixels. 
	 * 
	 * @param points	Branch in terms of an ordered list of branch points.
	 * @return	Length of branch.
	 */
	private double calcBranchLength(Vector<Point2D.Double> points) {
		double sqrtTwo = Math.sqrt(2.0);
		double length = 0;
		int thisX, thisY;
		int prevX = (int)points.get(0).x;
		int prevY = (int)points.get(0).y;
		for (int i=1; i<points.size(); ++i) {
			thisX = (int)points.get(i).x;
			thisY = (int)points.get(i).y;
			if (thisX == prevX || thisY == prevY) {
				length += this.pixelLength;
			}
			else {
				length += sqrtTwo * this.pixelLength;
			}
			prevX = thisX;
			prevY = thisY;
		}
		return length;
	}
	
	@Override
	public String getDocumentation() {
		return "<p>\r\n" + 
				"This operator calculates for a set of regions in a given image the region \r\n" + 
				"skeletons and subsequently extracts some feature measures from the skeletons. \r\n" + 
				"Measures currently implemented are, e.g.,\r\n" + 
				"<ul>\r\n" + 
				"<li> the number of branches of the region skeleton\r\n" + 
				"<li> the average length of the branches\r\n" + 
				"<li> the average distance of the branch endpoints to the background of the image\r\n" + 
				"</ul>\r\n" + 
				"The distance of a branch endpoint to the background can be interpreted as the \r\n" + 
				"radius of a maximal circle located at the branch endpoint and touching the \r\n" + 
				"boundary of the region. This radius can be regarded as an estimate for the \r\n" + 
				"local curvature of the region boundary, hence, the average radius of all branch\r\n" + 
				"endpoints yield a measure for the 'roughness' of the boundary, i.e. how much\r\n" + 
				"the boundary 'curves' on average.  \r\n" + 
				" \r\n" + 
				"<h2>Usage:</h2>\r\n" + 
				"<ul>\r\n" + 
				"<li><p><b>Required input parameters:</b>\r\n" + 
				"<ul>\r\n" + 
				"<li><p><i>Input label image</i>:<br>\r\n" + 
				" an image containing the regions where each region is labeled with an individual\r\n" + 
				" label larger than zero and the background has label zero</p>\r\n" + 
				"</ul>\r\n" + 
				"<li><p><b>Supplemental input parameters:</b>\r\n" + 
				"<ul>\r\n" + 
				"<li><p><i>Visualize analysis results?</i><br> \r\n" + 
				"	this flag allows to enable the creation of an additional output image where\r\n" + 
				"	the region skeletons, detected endpoints and the maximal circle per endpoint\r\n" + 
				"	are depicted</p>\r\n" + 
				"<li><p><i>Verbose</i>:<br>\r\n" + 
				" activates additional output during processing; usually the output is written \r\n" + 
				" to console only and might not be accessible via the graphical UI\r\n" + 
				"</ul>\r\n" + 
				"</ul>\r\n" + 
				"\r\n" + 
				"<li><p><b>Output parameters:</b>\r\n" + 
				"<ul>\r\n" + 
				"<li><p><i>Result Table of skeleton features</i>:<br>\r\n" + 
				" table with extracted features where each row refers to a region; the first\r\n" + 
				" column contains the region ID, i.e., the label of the region in the image,\r\n" + 
				" subsequent columns contain the various features\r\n" + 
				"<li><p><i>Image with analysis results</i>:<br>\r\n" + 
				"	if the corresponding option was selected an image visualizing the skeletons\r\n" + 
				"	and detected endpoints and circles is provided\r\n" + 
				"</ul>\r\n" + 
				"</ul>\r\n";
	}
}
