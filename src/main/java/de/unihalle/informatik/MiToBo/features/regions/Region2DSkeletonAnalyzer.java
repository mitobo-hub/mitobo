/*
 * This file is part of MiToBo, the Microscope Image Analysis Toolbox.
 *
 * Copyright (C) 2010 - 2014
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
import java.util.Vector;

import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.Alida.operator.events.ALDOperatorExecutionProgressEvent;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBNeuriteSkelGraph;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageByte;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageRGB;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;
import de.unihalle.informatik.MiToBo.gui.MTBTableModel;
import de.unihalle.informatik.MiToBo.morphology.DistanceTransform;
import de.unihalle.informatik.MiToBo.morphology.SkeletonExtractor;
import de.unihalle.informatik.MiToBo.morphology.DistanceTransform.DistanceMetric;
import de.unihalle.informatik.MiToBo.morphology.DistanceTransform.ForegroundColor;
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
	level=Level.STANDARD, allowBatchMode = false)
public class Region2DSkeletonAnalyzer extends MTBOperator {
	
	/**
	 * Operator class identifier.
	 */
	private static final String operatorID = "[Region2DSkeletonAnalyzer]";
	
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
		LongestSkeletonPathLength
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
	 * Default constructor.
	 * @throws ALDOperatorException Thrown on construction failure.
	 */
	public Region2DSkeletonAnalyzer() throws ALDOperatorException {
		// nothing to do here
	}

	/**
	 * Set input label image to process.
	 * @param img	Input label image.
	 */
	public void setInputLabelImage(MTBImage img) {
		this.inImg = img;
	}
	
	/**
	 * Enable/disable creation of image with analysis data.
	 * @param flag	If true, creation of image is enabled.
	 */
	public void setVisualizeAnalysisResults(boolean flag) {
		this.visualizeAnalysisResults = flag;
	}
	
	/**
	 * Get table of calculated features.
	 * @return	Table with feature values.
	 */
	public MTBTableModel getResultTable() {
		return this.resultFeatureTable;
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
		
		this.fireOperatorExecutionProgressEvent(
				new ALDOperatorExecutionProgressEvent(this, operatorID 
					+ " binarizing label image..."));

		ImgThresh thresholder = new ImgThresh(this.inImg, 0);
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
			int grayVal;
			for (int y=0; y<this.height; ++y) {
				for (int x=0; x<this.width; ++x) {
					grayVal = this.inImg.getValueInt(x, y);
					this.analysisDisplayImg.putValue(x, y, 
							grayVal, grayVal, grayVal);
					// mark skeleton pixels in yellow
					if (skelImg.getValueInt(x, y) > 0)
						this.analysisDisplayImg.putValueInt(x, y, yellow);
				}
			}
		}
		
		// search for maximum label in input image
		int maxLabel = 0;
		for (int y=0;y<this.height;++y) {
			for (int x=0;x<this.width;++x) {
				if (this.inImg.getValueInt(x, y) > maxLabel)
					maxLabel = this.inImg.getValueInt(x, y);
			}
		}

		// search for endpoints and trace skeletons for each region
		int[] longestPathLengths = new int[maxLabel+1];
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
						for (int dy=-1;dy<=1;++dy) {
							// ignore the pixel itself
							if (dx == 0 && dy == 0)
								continue;
							if (   x+dx >= 0 && x+dx < this.width 
									&& y+dy >= 0 && y+dy < this.height) {
								if (skelImg.getValueInt(x+dx, y+dy) > 0) {
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
						MTBNeuriteSkelGraph nsg = new MTBNeuriteSkelGraph(); 
						boolean done = nsg.buildSkeletonGraph(x, y, this.width, 
								this.height, skelImg, 255);
						if (done) {
							Vector<Point2D.Double> branch = nsg.getLongestPath(true);
							if (  branch.size() 
									> longestPathLengths[this.inImg.getValueInt(x, y)]) {
								longestPathLengths[this.inImg.getValueInt(x, y)] = 
									branch.size();
								longestPath.set(this.inImg.getValueInt(x, y)-1,branch);
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
				if (this.inImg.getValueInt(x, y) > 0)
					regionWithLabelFound[this.inImg.getValueInt(x, y)-1] = true;
			}
		}
		
		// count endpoints and trace branches from endpoint to branch point
		int length;
		for (int y=0;y<this.height;++y) {
			for (int x=0;x<this.width;++x) {
				if (   skelImg.getValueInt(x, y) > 0 
						&& memImg.getValueInt(x, y) > 0) {
					nCount = numberOfNeighbors(skelImg, x, y);
					if (nCount == 0) {
						// we have only a single skeleton point which is by our definition
						// also a branch (appears, e.g., in skeletons of perfect circles)
						++branchCounts[this.inImg.getValueInt(x, y)-1];
					}
					else if (nCount == 1) {
						// found an endpoint, mark as processed
						memImg.putValueInt(x, y, 0);
						++branchCounts[this.inImg.getValueInt(x, y)-1];
						
						// calculate branch length...
						length = traceBranch(skelImg, x, y);
						branchLengths[this.inImg.getValueInt(x, y)-1] += length;
						
						// ... and endpoint distance to background
						branchDistances[this.inImg.getValueInt(x, y)-1] += 
								distTransImg.getValueInt(x, y);
						
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
		
		// allocate and fill result table
		int regionCount = 0;
		for (int i=0;i<regionWithLabelFound.length;++i) {
			if (regionWithLabelFound[i])
				++regionCount;
		}
		this.resultFeatureTable = new MTBTableModel(regionCount, 5);
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
		int rowID = 0;
		for (int i=0; i<regionWithLabelFound.length; ++i) {
			if (!regionWithLabelFound[i])
				continue;
			this.resultFeatureTable.setValueAt(Integer.toString(i+1),rowID,0);
			this.resultFeatureTable.setValueAt(
					Integer.toString(branchCounts[i]), rowID, 1);
			this.resultFeatureTable.setValueAt(
					Double.toString(branchLengths[i]), rowID, 2);
			this.resultFeatureTable.setValueAt(
					Double.toString(branchDistances[i]), rowID, 3);
			this.resultFeatureTable.setValueAt(
					Integer.toString(longestPathLengths[i+1]), rowID, 4);
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
	 * @param img	Skeleton image
	 * @param x		x-coordinate where to start.
	 * @param y		y-coordinate where to start.
	 * @return	Length of the branch.
	 */
	private static int traceBranch(MTBImage img, int x, int y) {
		
		int width = img.getSizeX();
		int height = img.getSizeY();
		
		boolean tracingActive = true;
		int pointCount = 1;
		int nx = x, ny = y;

		// init working image and mark point as processed
		MTBImage memImg = img.duplicate();
		memImg.putValueInt(x, y, 0);

		// search next neighbor point
		boolean neighborFound = false;
		for (int dx=-1; !neighborFound && dx<=1; ++dx) {
			for (int dy=-1; !neighborFound && dy<=1; ++dy) {
				if (dx == 0 && dy == 0)
					continue;
				if (   nx+dx >= 0 && nx+dx < width 
						&& ny+dy >= 0 && ny+dy < height
						&& memImg.getValueInt(nx+dx, ny+dy) > 0) {
					neighborFound = true;
					nx = nx+dx;
					ny = ny+dy;
					++pointCount;
				}
			}
		}

		// continue tracing
		while(tracingActive) {
			// mark current point as processed
			memImg.putValueInt(nx, ny, 0);
			int nCount = numberOfNeighbors(memImg, nx, ny);
			if (nCount == 1) {
				// continue path
				neighborFound = false;
				for (int dx=-1; !neighborFound && dx<=1; ++dx) {
					for (int dy=-1; !neighborFound && dy<=1; ++dy)	 {
						if (dx == 0 && dy == 0)
							continue;
						if (   nx+dx >= 0 && nx+dx < width 
								&& ny+dy >= 0 && ny+dy < height
								&& memImg.getValueInt(nx+dx, ny+dy) > 0) {
							neighborFound = true;
							nx = nx+dx;
							ny = ny+dy;
							++pointCount;
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
		return pointCount;
	}
}

/*BEGIN_MITOBO_ONLINE_HELP
<p><a target="_blank" href="http://www2.informatik.uni-halle.de/agprbio/mitobo//api/de/unihalle/informatik/MiToBo/features/regions/Region2DSkeletonAnalyzer.html">API</a></p>

<p>
This operator calculates for a set of regions in a given image the region 
skeletons and subsequently extracts some feature measures from the skeletons. 
Measures currently implemented are, e.g.,
<ul>
<li> the number of branches of the region skeleton
<li> the average length of the branches
<li> the average distance of the branch endpoints to the background of the image
</ul>
The distance of a branch endpoint to the background can be interpreted as the 
radius of a maximal circle located at the branch endpoint and touching the 
boundary of the region. This radius can be regarded as an estimate for the 
local curvature of the region boundary, hence, the average radius of all branch
endpoints yield a measure for the 'roughness' of the boundary, i.e. how much
the boundary 'curves' on average.  
 
<h2>Usage:</h2>
<ul>
<li><p><b>Required input parameters:</b>
<ul>
<li><p><i>Input label image</i>:<br>
 an image containing the regions where each region is labeled with an individual
 label larger than zero and the background has label zero</p>
</ul>
<li><p><b>Supplemental input parameters:</b>
<ul>
<li><p><i>Visualize analysis results?</i><br> 
	this flag allows to enable the creation of an additional output image where
	the region skeletons, detected endpoints and the maximal circle per endpoint
	are depicted</p>
<li><p><i>Verbose</i>:<br>
 activates additional output during processing; usually the output is written 
 to console only and might not be accessible via the graphical UI
</ul>
</ul>

<li><p><b>Output parameters:</b>
<ul>
<li><p><i>Result Table of skeleton features</i>:<br>
 table with extracted features where each row refers to a region; the first
 column contains the region ID, i.e., the label of the region in the image,
 subsequent columns contain the various features
<li><p><i>Image with analysis results</i>:<br>
	if the corresponding option was selected an image visualizing the skeletons
	and detected endpoints and circles is provided
</ul>
</ul>

END_MITOBO_ONLINE_HELP*/
