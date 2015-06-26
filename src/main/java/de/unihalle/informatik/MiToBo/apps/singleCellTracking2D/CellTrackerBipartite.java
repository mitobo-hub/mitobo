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


package de.unihalle.informatik.MiToBo.apps.singleCellTracking2D;

import ij.IJ;

import java.awt.geom.Point2D;
import java.util.Vector;

import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.Alida.exceptions.ALDWorkflowException;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.Parameter.ParameterModificationMode;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2D;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;
import de.unihalle.informatik.MiToBo.math.optimization.MatchingBipartite_HungarianAlgorithm;
import de.unihalle.informatik.MiToBo.math.optimization.MatchingBipartite_HungarianAlgorithm.ScoreInterpretation;
import de.unihalle.informatik.MiToBo.segmentation.regions.labeling.LabelComponentsSequential;

/**
 * This operator assigns unique labels to regions representing individual cells in the input binary image sequence
 * 
 * @author glass
 *
 */
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.ALL, level=Level.STANDARD)
public class CellTrackerBipartite extends MTBOperator
{
	@Parameter(label = "binary input image", required = true, direction = Parameter.Direction.IN, supplemental = false, description = "binary input image", dataIOOrder = 0)
	private transient  MTBImage inImg = null;
	
	@Parameter(label = "determine gating distance automatically", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "determine gating distance automatically", dataIOOrder = 1,
			callback = "showMaxDistTextbox", paramModificationMode = ParameterModificationMode.MODIFIES_INTERFACE)
	private boolean useAutoDistance = true;
	
	@Parameter(label = "maximum distance (pixels)", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "maximum distance for two objects to be assigned to each other", dataIOOrder = 2)
	private double maxDist = 30;
	
	@Parameter(label = "maximum area change", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "maximum change in area (fraction) for two objects to be assigned to each other", dataIOOrder = 3)
	private double maxAreaChange = 0.5;
	
	@Parameter(label = "are objects 8-connected", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "are objects 8-connected", dataIOOrder = 4)
	private Boolean objects8Connected = false;
	
	@Parameter(label = "result image", required = true, direction = Parameter.Direction.OUT, supplemental = false, description = "result image")
	private transient MTBImage resultImg = null;
	
	private final int DISAPPEARED = -1;			// value indicating, that an object has disappeared
	
	private static double inf = Double.MAX_VALUE;
	
	private int sizeX;	// number of pixels in x-dimension
	private int sizeY;	// number of pixels in y-dimension
	private int sizeT;	// number of time frames
	
	private int totalNumberOfObjects;		// total number of objects detected up to the last processed frame
	
	MTBImage labelImg;
	
	private Vector<Integer> objectLabels;	// holds correspondencies from already collected tracks to labels from currently
											// found connected components
	
	private double currentMax = 0;	// maximum distance between two objects in the current considered frames
	
	
	/**
	 * 
	 * @throws ALDOperatorException
	 */
	public CellTrackerBipartite() throws ALDOperatorException
	{
		
	}
	
	
	/**
	 * 
	 * @param inImg
	 * @throws ALDOperatorException
	 */
	public CellTrackerBipartite(MTBImage inImg) throws ALDOperatorException
	{
		this.inImg = inImg;
	}

	@Override
	public void operate() throws ALDOperatorException, ALDProcessingDAGException
	{
		this.verbose = getVerbose();
		
		this.sizeX = inImg.getSizeX();
		this.sizeY = inImg.getSizeY();
		this.sizeT = inImg.getSizeT();
		
		if(useAutoDistance)
		{
			maxDist = determineGatingDistance();
			
			System.out.println("chosen gating distance: " + maxDist);
		}
		
		track();
	}
	
	/**
	 * the actual tracking method
	 * 
	 * @throws ALDOperatorException
	 * @throws ALDProcessingDAGException
	 */
	private void track() throws ALDOperatorException, ALDProcessingDAGException
	{
		int n = inImg.getSizeT();
		
		verbosePrintln("start tracking of " + inImg.getTitle() + ", number of frames: " + n);
		
		// initialize neccessary objects
		objectLabels = new Vector<Integer>();
		
		labelImg = MTBImage.createMTBImage(sizeX, sizeY, 1, n, 1, MTBImage.MTBImageType.MTB_SHORT);
		labelImg.setCalibration(inImg.getCalibration());	// keep pixel dimensions and units
		
		MTBRegion2DSet currentRegions = label(inImg.getImagePart(0, 0, 0, 0, 0, sizeX, sizeY, 1, 1, 1));	// determine connected components from the first frame
		totalNumberOfObjects = currentRegions.size();
		
		// assign ids and correspondencies to the initial regions
		for(int i = 0; i < totalNumberOfObjects; i++)
		{
			currentRegions.elementAt(i).setID(i);
			objectLabels.add(i);
		}
		
		labelImg.setCurrentSliceIndex(0);
		labelImg.setCurrentSlice(drawRegions(currentRegions));
		
		// label and track individual regions in every frame
		for(int t = 1; t < n; t++)
		{
			verbosePrintln("processing frame " + t);
			
			MTBImage nextFrame = inImg.getImagePart(0, 0, 0, t, 0, sizeX, sizeY, 1, 1, 1);	// extract next frame
			
			MTBRegion2DSet nextRegions = label(nextFrame);	// extract connected components from the next frame
			
			byte[][] a = assign(currentRegions, nextRegions);	// associate regions from the current frame to regions from the next one
			
			MTBImage resultSlice = relabel(a, currentRegions.size(), nextRegions.size(), nextRegions);	// match current assignments to existing tracks
			
			labelImg.setCurrentSliceIndex(t);
			labelImg.setCurrentSlice(resultSlice);	// insert labeled frame to the result image

			currentRegions = nextRegions;
			
			IJ.showProgress(t, n);
		}
		
		this.resultImg = labelImg;
		this.resultImg.setTitle("tracking result");
		
		verbosePrintln("finished tracking of " + inImg.getTitle() + "!\n");
	}
	
	
	/**
	 * assign regions from one frame to regions from another frame
	 * 
	 * @param currentRegions	vector containing the n regions from one frame
	 * @param nextRegions    	vector containing the m regions from another frame
	 * @return 				 	((n+m) x (n+m))-table containing assignments (entry == 1) of the input regions<br/>
	 * 							rows: regions from the first frame, columns: regions from the second frame 
	 * @throws ALDOperatorException
	 * @throws ALDProcessingDAGException
	 */
	private byte[][] assign(MTBRegion2DSet currentRegions, MTBRegion2DSet nextRegions) throws ALDOperatorException, ALDProcessingDAGException
	{
		int n = currentRegions.size();
		int m = nextRegions.size();
		
		// no objects in the current frame
		if(n == 0)
		{
			byte[][] a = new byte[m][m];
			
			for(int i = 0; i < m; i++)
			{
				a[i][i] = 1;
			}
			
			return a;
		}
		
		// no objects in the next frame
		if(m == 0)
		{
			byte[][] a = new byte[n][n];
			
			for(int i = 0; i < n; i++)
			{
				a[i][i] = 1;
			}
			
			return a;
		}
		
		double[][] d = getDistMatrix(currentRegions, nextRegions);		// n x m table containing distances for every region pair
		double[][] f = getAreaFracMatrix(currentRegions, nextRegions);	// n x m table containing area fractions for every region pair
		
		// create square cost matrix
		int l = n+m;
		double[][] costs = new double[l][l];
		
		// assign dummies costs that are higher than any real cost
		for(int i = n; i < l; i++)
		{
			for(int j = 0; j < l; j++)
			{
				costs[i][j] = currentMax + 1;
			}
		}
		
		for(int j = m; j < l; j++)
		{
			for(int i = 0; i < l; i++)
			{
				costs[i][j] = currentMax + 1;
			}
		}
		
		//TODO: maybe put this directly to the calculation of the distances for speed up
		for(int i = 0; i < n; i++)
		{
			for(int j = 0; j < m; j++)
			{
				double frac = f[i][j];
				
				if(frac < 1)
				{
					frac = 1 / frac;
				}
						
//				if(f[i][j] < (1 - maxAreaChange) || f[i][j] > (1 + maxAreaChange) || d[i][j] > maxDist)
				if(frac > (1 + maxAreaChange) || d[i][j] > maxDist)
				{
					// TODO: remove duplicate queries
					
					// merging
					if(d[i][j] < maxDist && f[i][j] > 1 + maxAreaChange)
					{
						verbosePrintln("possible merging of " + (currentRegions.elementAt(i).getID() + 1));
					}
					
					// splitting
					if(d[i][j] < maxDist && f[i][j] < 1 - maxAreaChange)
					{
						// cell division
						if(currentRegions.elementAt(i).getCircularity() > 0.9)	// TODO: use increase in circularity instead
						{
							verbosePrintln("possible division of " + (currentRegions.elementAt(i).getID() + 1));
						}
						else
						{
							verbosePrintln("possible splitting of " + (currentRegions.elementAt(i).getID() + 1));
						}
					}
					
					costs[i][j] = inf;
				}
				else
				{
					costs[i][j] = d[i][j];
				}
			}
		}
		
		
		MatchingBipartite_HungarianAlgorithm matching = new MatchingBipartite_HungarianAlgorithm(costs, ScoreInterpretation.MINIMUM_IS_BEST);
		matching.runOp();

		return matching.getMatching();
	}
	
	

	/**
	 * 
	 * @param img binary image
	 * @return set of regions from the binary image
	 * @throws ALDOperatorException 
	 * @throws ALDProcessingDAGException 
	 */
	private MTBRegion2DSet label(MTBImage img) throws ALDOperatorException, ALDProcessingDAGException
	{	
		LabelComponentsSequential labeler = new LabelComponentsSequential(img, objects8Connected);
		labeler.runOp();
		MTBRegion2DSet regions = labeler.getResultingRegions();
		
		return regions;
	}
	
	
	/**
	 * find and assign object labels to segmented objects in two frames
	 * 
	 * @param a assignment table
	 * @param n number of objects from the current frame
	 * @param m number of objects from the next frame
	 * @param nextRegions regions from the next frame
	 * @return frame where all connected components are labeled according to the assignments contained in a
	 */
	private MTBImage relabel(byte[][] a, int n, int m, MTBRegion2DSet nextRegions)
	{
		// copy content of objectLabels into temporary label vector
		Vector<Integer> tempLabels = new Vector<Integer>();
		
		for(int i = 0; i < objectLabels.size(); i++)
		{
			int ol = objectLabels.elementAt(i);
			tempLabels.add(ol);
		}
		
		int l = a.length;
		
		for(int i = 0; i < l; i++)
		{
			for(int j = 0; j < l; j++)
			{
				if(a[i][j] == 1)
				{
					if(i >= n && j >= m) //assignments between the dummy objects
					{
						
					}
					else if(i >= n)	// new cell (or cluster of cells) appeared
					{
						if(nextRegions.elementAt(j).getCenterOfMass_X() < maxDist || ((sizeX - 1) - nextRegions.elementAt(j).getCenterOfMass_X()) < maxDist
						 ||nextRegions.elementAt(j).getCenterOfMass_Y() < maxDist || ((sizeY - 1) - nextRegions.elementAt(j).getCenterOfMass_Y()) < maxDist)
						{
							verbosePrintln("new object possibly entered the field of view at (" + (int)nextRegions.elementAt(j).getCenterOfMass_X() + "," + (int)nextRegions.elementAt(j).getCenterOfMass_Y() + ")");
						}
						
						else
						{
							verbosePrintln("new object at (" + (int)nextRegions.elementAt(j).getCenterOfMass_X() + "," + (int)nextRegions.elementAt(j).getCenterOfMass_Y() + ") appeared");
						}
						
						tempLabels.add(j);	// add new label at the end of the label list
						nextRegions.elementAt(j).setID(totalNumberOfObjects);
										
						totalNumberOfObjects++;
						
						break;
					}
					else if(j >= m)	// cell disappeared 
					{	
						for(int k = 0; k < objectLabels.size(); k++)
						{
							if(objectLabels.elementAt(k) == i)	// get label that the object was assigned to in the last step
							{
								tempLabels.set(k, DISAPPEARED);
								verbosePrintln("object " + (k + 1) + " disappeared");
								
								break;
							}
						}

						break;
					}
					else	// match
					{
						// search for object id associated with the current label
						for(int k = 0; k < objectLabels.size(); k++)
						{
							if(objectLabels.elementAt(k) == i)	// get label that the object was assigned to in the last step
							{
								tempLabels.set(k, j);
								nextRegions.elementAt(j).setID(k);
								
								break;
							}
						}
						
						break;
					}
				}
			}
		}
		
		objectLabels = tempLabels;
		
		return drawRegions(nextRegions);
	}
	
	
	/**
	 * 
	 * @param currRegions
	 * @param nextRegions
	 * 
	 * @return distance matrix for the given region sets
	 */
	private double[][] getDistMatrix(MTBRegion2DSet currRegions, MTBRegion2DSet nextRegions)
	{
		currentMax = 0;
		
		int n = currRegions.size();
		int m = nextRegions.size();
		
		double[][] featureTable = new double[n][m];
		
		for(int i = 0; i < n; i++)
		{
			MTBRegion2D r = currRegions.elementAt(i);
			
			for(int j = 0; j < m; j++)
			{
				MTBRegion2D s = nextRegions.elementAt(j);
				
				double dist = getDistance(r,s);
				
				featureTable[i][j] = dist;
				
				if(dist > currentMax)
				{
					currentMax = dist;
				}
			}
		}
		
		return featureTable;
	}
	
	
	/**
	 * 
	 * @param currRegions
	 * @param nextRegions
	 * 
	 * @return matrix containing the fractions of the areas of the input regions
	 */
	private double[][] getAreaFracMatrix(MTBRegion2DSet currRegions, MTBRegion2DSet nextRegions)
	{
		int n = currRegions.size();
		int m = nextRegions.size();
		
		double[][] featureTable = new double[n][m];
		
		for(int i = 0; i < n; i++)
		{
			MTBRegion2D r = currRegions.elementAt(i);
			
			for(int j = 0; j < m; j++)
			{
				MTBRegion2D s = nextRegions.elementAt(j);
				
				double dist = getAreaFraction(r,s);
				
				featureTable[i][j] = dist;
			}
		}
		
		return featureTable;
	}
	
	
	/**
	 * 
	 * @param regions
	 * 
	 * @return image where the regions are drawn with an intensity value corresponding to their ids + 1
	 */
	private MTBImage drawRegions(MTBRegion2DSet regions)
	{
		MTBImage labelSlice = MTBImage.createMTBImage(sizeX, sizeY, 1, 1, 1, MTBImage.MTBImageType.MTB_SHORT);
		
		for(int i = 0; i < regions.size(); i++)
		{
			MTBRegion2D r = regions.elementAt(i);
			int color = r.getID() + 1;
			//System.out.print("id: " + id + ", ");
			
			Vector<Point2D.Double> points = r.getPoints();
			
			// draw the points belonging to the current region with it's corresponding id as intensity
			for(int j = 0; j < points.size(); j++)
			{
				Point2D.Double p = points.elementAt(j);
				labelSlice.putValueDouble((int)p.x, (int)p.y, color);
			}
		}
		
		return labelSlice;
	}
	
	
	/**
	 * 
	 * @param r
	 * @param s
	 * @return euclidean distance (pixels) between the centroids of r and s
	 */
	private double getDistance(MTBRegion2D r, MTBRegion2D s)
	{
		double crx = r.getCenterOfMass_X();
		double cry = r.getCenterOfMass_Y();
		double csx = s.getCenterOfMass_X();
		double csy = s.getCenterOfMass_Y();
		
		return(Math.sqrt((crx - csx) * (crx - csx) + (cry - csy) * (cry - csy)));
		
	}
	
	private double getAreaFraction(MTBRegion2D r, MTBRegion2D s)
	{
		double a1 = r.getArea();
		double a2 = s.getArea();
		
		return (a2 / a1);
	}
	
	
	public MTBImage getResultImage()
	{
		return this.resultImg;
	}
	
	
	public void useAutoDistanceDetermination(boolean useAutoDistance)
	{
		this.useAutoDistance = useAutoDistance;
	}
	
	public void setMaxDistance(double t)
	{
		this.maxDist = t;
	}
	
	
	public void setMaxAreaChange(double t)
	{
		this.maxAreaChange = t;
	}
	
	
	public void setObjectsEightConnected(boolean eightconnected)
	{
		objects8Connected = eightconnected;
	}
	
	
	/**
	 * prints the given text if the verbose flag is set
	 * 
	 * @param s text to print
	 */
	private void verbosePrintln(String s)
	{
		if(verbose)
		{
			System.out.println(s);
		}
	}
	
	
	/**
	 * automatic gating distance determination according to:<br/>
	 * <br/>
	 * "Automated and semi-automated cell tracking: addressing portability challenges"<br/>
	 * KAN, A. and CHAKRAVORTY, R. and BAILEY, J. and LECKIE, C. and MARKHAM, J. and DOWLING, M.R.<br/>
	 * Journal of Microscopy. Vol 244. Number 2. 2011
	 * 
	 * @throws ALDProcessingDAGException 
	 * @throws ALDOperatorException 
	 * 
	 */
	private int determineGatingDistance() throws ALDOperatorException, ALDProcessingDAGException
	{	
		int rMin = 1;			// minimum distance tested
		int rMax = sizeX / 10;	// maximum distance tested	// 1500
		int deltaR = 1;			// stepsize of tested distances
		
//		double N_all = 0;	// number of all possible links in the whole video
		double N_t = 0;		// estimated number of true links in the whole video
//		double N_f = 0;		// estimated number of false links in the whole video (N_all - N_t)
		
		Vector<double[][]> distMatrices = new Vector<double[][]>();	// distance matrices from regions of consecutive frames
		Vector<double[][]> distMatricesAuto = new Vector<double[][]>();	// distance matrices from regions of the same frames
			
		MTBImage currFrame = inImg.getSlice(0, 0, 0);	// extract 1st frame
		
		MTBRegion2DSet currentRegions = label(currFrame);	// extract connected components from the 1st frame
		
		// estimate number of true links and calculate distances
		for(int t = 1; t < sizeT; t++)
		{
			MTBImage nextFrame = inImg.getSlice(0, t, 0);
			MTBRegion2DSet nextRegions = label(nextFrame);	// extract connected components from the next frame
			
			// N_t is estimated as the number of all object occurences in all frames
			N_t += nextRegions.size();
			
			// N_all is the number of all possible links
//			N_all += currentRegions.size() * nextRegions.size();
			
			distMatrices.add(getDistMatrix(currentRegions, nextRegions));
			distMatricesAuto.add(getDistMatrix(nextRegions, nextRegions));
			
			currentRegions = nextRegions;
		}
		
//		N_f = N_all - N_t;
		
//		System.out.println("N_all: " + N_all + ", N_t: " + N_t + ", N_f: " + N_f);
		System.out.println("N_t: " + N_t);
		
		double Pmax = 0;
		int mr = 0;
		
		// try increasing distances 
		for(int r = rMin; r < rMax; r+= deltaR)
		{
			double n_all = 0;	// number of all possible links in the whole video with a distance less than r
			double n_f = 0;		// estimated number of false links in the whole video with a distance less than r
//			double P_all = 0;	// probability that length of any link is less or equal than a certain distance R (P_all(r <= R))
			double P_t = 0;		// estimated probability that length of a true link is less or equal than a certain distance R (P_t(r <= R))
//			double P_f = 0;		// estimated probability that length of a false link is less or equal than a certain distance R (P_f(r <= R))
			
			// accumulate values from all frames
			for(int t = 0; t < sizeT - 1; t++)
			{
				double[][] distMatrix = distMatrices.elementAt(t);
				
				
				int n = 0;
				int m = 0;
				
				
				if(distMatrix.length > 0)
				{
					n = distMatrix.length;
					m = distMatrix[0].length;
				}
				
				for(int y = 0; y < n; y++)
				{
					for(int x = 0; x < m; x++)
					{
						if(distMatrix[y][x] < r)
						{
							n_all++;
						}
					}
				}
				
				// number of false links is estimated as the number of possible links at a given r in the same frame
				double[][] distMatrixAuto = distMatricesAuto.elementAt(t);
				
				n = distMatrixAuto.length;
				
				for(int y = 0; y < n; y++)
				{
					for(int x = 0; x < n; x++)
					{
						if(x != y && distMatrixAuto[y][x] < r)
						{
							n_f++;
						}
					}
				}
				
			}
			
//			P_all = n_all / N_all;
//			P_f = n_f / N_f;
			
//			P_t = (N_all * P_all - (N_all - N_t) * P_f) / N_t;
			P_t = (n_all - n_f) / N_t;	// simplified formula
			
			
			if(P_t > Pmax)
			{
				Pmax = P_t;
				mr = r;
			}
			
//			System.out.println("r: " + r);
//			System.out.println("n_all: " + n_all + ", n_f: " + n_f);
//			System.out.println("P_all: " + P_all + ", P_f: " + P_f + ", P_t: " + P_t);
//			System.out.println("P_t: " + P_t);
			verbosePrintln(r + "\t" + P_t);
			
			if(P_t >= 1)
			{
				return r;
			}
		}
		
		return mr;	// if P_t doesn't reach 1 than use the maximum value instead 
	}

	// ------------------------------ callback functions ------------------------------
	
	@SuppressWarnings("unused")
	private void showMaxDistTextbox()
	{
		try
		{
			if(useAutoDistance)
			{
				if(this.hasParameter("maxDist"))
				{
					this.removeParameter("maxDist");
				}
			}
			else
			{
				if(!this.hasParameter("maxDist"))
				{
					this.addParameter("maxDist");
				}
			}
		}
		catch(ALDOperatorException e)
		{
			e.printStackTrace();
		}
	}
	
}


/*BEGIN_MITOBO_ONLINE_HELP
<p><a target="_blank" href="http://www2.informatik.uni-halle.de/agprbio/mitobo//api/de/unihalle/informatik/MiToBo/apps/singleCellTracking2D/CellTrackerBipartite.html">API</a></p>

<ul>
	<li>
		<p>This operator assigns unique labels to regions representing individual cells in the input binary image sequence</p>
	</li>
	<li>
		<p>As result a label image sequence is given</p>
	</li>
</ul>

<h2>Usage:</h2>

<h3>required parameters:</h3>

<ul>
	<li>
		<p><tt>binary input image</tt>
		<ul>
			<li>
				<p>image sequence to be tracked</p>
			</li>
		</ul>
		</p>
	</li>
	<li>
		<p><tt>determine gating distance automatically</tt>
		<ul>
			<li>
				<p>should the gating distance be determined automatically</p>
			</li>
			<li>
				<p>if not, the maximum distance (pixels) is used</p>
			</li>
		</ul>
		</p>
	</li>
</ul>

<h3>optional parameters:</h3>

<ul>
	<li>
		<p><tt>maximum area change</tt>
		<ul>
			<li>
				<p>if the fraction of the areas of two regions from subsequent frames differ more than this value, these regions are not considered to belong to the same cell</p>
			</li>
		</ul>
		</p>
	</li>
	<li>
		<p><tt>maximum distance (pixels)</tt>
		<ul>
			<li>
				<p>if the centroid distance of two regions from subsequent frames exceeds this value, these regions are not considered to belong to the same cell</p>
			</li>
			<li>
				<p>only used if automatic gating distance determination is deactivated!</p>
			</li>
		</ul>
		</p>
	</li>
	<li>
		<p><tt>are objects 8-connected</tt>
		<ul>
			<li>
				<p>if activated, cell objects will be considered to have eight-connectivity and four-connectivity otherwise</p>
			</li>
		</ul>
		</p>
	</li>
</ul>

<h3>supplemental parameters:</h3>

<ul>
	<li>
		<p><tt>Verbose</tt>
		<ul>
			<li>
				<p>output some additional information</p>
			</li>
		</ul>
		</p>
	</li>
</ul>
END_MITOBO_ONLINE_HELP*/
