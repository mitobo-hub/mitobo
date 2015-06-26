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

package de.unihalle.informatik.MiToBo.morphology;

import java.awt.geom.Point2D;
import java.util.Vector;

import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.Alida.exceptions.ALDWorkflowException;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.Parameter.Direction;
import de.unihalle.informatik.Alida.annotations.Parameter.ParameterModificationMode;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;
import de.unihalle.informatik.MiToBo.segmentation.regions.labeling.LabelComponentsSequential;
import de.unihalle.informatik.MiToBo.visualization.drawing.DrawLine;

/**
 * operator for calculating the convex hulls of binary connected components<br/> 
 * in a given image or of a set of regions using the Jarvis march algorithm 
 * 
 * @author glass
 *
 */
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.ALL, level=Level.APPLICATION)
public class ConvexHullExtraction extends MTBOperator
{
	/**
	 * Type of input to work on.
	 */
	public static enum InputType {
		/**
		 * Input data is provided as image.
		 */
		IMAGE,
		/**
		 * Input data is provided as set of regions.
		 */
		REGIONS
	}

	/**
	 * Input type.
	 */
	@Parameter(label = "Input Type", required = true, supplemental = false, 
			dataIOOrder = 0, callback = "inputTypeChanged", direction = Direction.IN,
			description = "Type of input data.", 
			paramModificationMode = ParameterModificationMode.MODIFIES_INTERFACE)
	private InputType inType = InputType.IMAGE;

	@Parameter(label = "input image", required = true, dataIOOrder = 1,
			direction = Parameter.Direction.IN, supplemental = false, 
			description = "input image")
	private transient MTBImage inImg = null;
	
	/**
	 * Set of input regions.
	 */
	@Parameter(label = "Input Regions", required = true, dataIOOrder = 2, 
		direction = Parameter.Direction.IN, supplemental = false, 
		description = "Input regions.")
	private transient MTBRegion2DSet inRegions = null;

	/**
	 * Process each component individually (only active if {@link #inType} is 
	 * {@code InputType.IMAGE}).
	 */
	@Parameter(label = "individually", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "treat objects individually")
	private Boolean individually = new Boolean(true);
	
	/**
	 * Use 8-connectedness in component labeling (only active if {@link #inType} 
	 * is {@code InputType.IMAGE}).
	 */
	@Parameter(label = "objects are eightconnected", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "are foreground objects 8-connected (else 4-connected)")
	private Boolean eightconnected = new Boolean(true);
	
	@Parameter(label = "convexHulls", required = true, direction = Parameter.Direction.OUT, supplemental = false, description = "vertices of convex hulls")
	private Vector<Point2D.Double[]> convexHulls = null;
	
	@Parameter(label = "image containing convex hulls", required = true, direction = Parameter.Direction.OUT, supplemental = true, description = "image with convex hulls painted")
	private transient MTBImage hullImage = null;
	
	public ConvexHullExtraction() throws ALDOperatorException
	{
		
	}

	
	/**
	 * 
	 * @param img	input image
	 * @throws ALDOperatorException
	 */
	public ConvexHullExtraction(MTBImage img) throws ALDOperatorException
	{
		this.inImg = img;
	}
	
	
	/**
	 * 
	 * @param img input image
	 * @param indiv	should convex hull be computed for every single connected component
	 * @param eightconnect	is foreground 8-connected
	 * @throws ALDOperatorException
	 */
	public ConvexHullExtraction(MTBImage img, boolean indiv, boolean eightconnect) throws ALDOperatorException
	{
		this.inImg = img;
		this.individually = new Boolean(indiv);
		this.eightconnected = new Boolean(eightconnect);
	}
	
	/**
	 * Specify input image type.
	 * @param t	Type of input.
	 */
	public void setInputType(InputType t) {
		this.inType = t;
		this.inputTypeChanged();
	}
	
	/**
	 * Set input image (if {@link #inType} is {@code InputType.IMAGE}).
	 * @param img	Image to process.
	 */
	public void setInputImage(MTBImage img) {
		if (this.hasParameter("inImg"))
			this.inImg = img;
	}
	
	/**
	 * Set input regions (if {@link #inType} is {@code InputType.REGIONS}).
	 * @param img	Set of regions to process.
	 */
	public void setInputRegions(MTBRegion2DSet regs) {
		if (this.hasParameter("inRegions"))
			this.inRegions = regs;
	}
	
	/**
	 * Set how to treat the components, individually or as a whole.
	 * <p>
	 * Parameter is only active if {@link #inType} is {@code InputType.IMAGE}.
	 * @param flag	If true, each component is processed independently. 
	 */
	public void setIndividually(boolean flag) {
		if (this.hasParameter("individually"))
			this.individually = new Boolean(flag);
	}
	
	/**
	 * Set which neighborhood to use for labeling components.
	 * <p>
	 * Parameter is only active if {@link #inType} is {@code InputType.IMAGE}.
	 * @param flag	If true, 8-neighborhood is used, otherwise 4-neighborhood.
	 */
	public void setEightconnected(boolean flag) {
		if (this.hasParameter("eightconnected"))
			this.eightconnected = new Boolean(flag);
	}

	@Override
	public void operate() throws ALDOperatorException, ALDProcessingDAGException
	{
		this.verbose = getVerbose();

		this.convexHulls = new Vector<Point2D.Double[]>();

		if (this.inType == InputType.IMAGE) {
			int sizeX = this.inImg.getSizeX();
			int sizeY = this.inImg.getSizeY();

			this.hullImage = MTBImage.createMTBImage(sizeX, sizeY, 1, 1, 1, MTBImage.MTBImageType.MTB_BYTE);
			
			if(this.individually.booleanValue())	// compute convex hull for each binary connected component
			{
				LabelComponentsSequential labeler = new LabelComponentsSequential(this.inImg, this.eightconnected.booleanValue());
				labeler.runOp();
				MTBRegion2DSet regions = labeler.getResultingRegions();
				this.extractHullsOnRegions(regions);
			}
			else	// compute convex hull for all non-zero image pixels
			{
				Vector<Point2D.Double> pts = new Vector<Point2D.Double>();

				// collect all non-background pixels from the input image
				for(int y = 0; y < sizeY; y++)
				{
					for(int x = 0; x < sizeX; x++)
					{
						if(this.inImg.getValueDouble(x,y) != 0)
						{
							pts.add(new Point2D.Double(x,y)); 
						}
					}
				}

				Point2D.Double[] points = new Point2D.Double[pts.size()];	// convert Vector to array for faster computations

				for(int i = 0; i < pts.size(); i++)
				{
					points[i] = pts.elementAt(i);
				}

				Point2D.Double[] hullPoints = jarvisMarch(points);

				this.convexHulls.add(hullPoints);

				draw(hullPoints);
			}
		}
		else if (this.inType == InputType.REGIONS) {
			double xMin = this.inRegions.getXmin();
			double xMax = this.inRegions.getXmax();
			double yMin = this.inRegions.getYmin();
			double yMax = this.inRegions.getYmax();
			int width = (int)((xMax - xMin) + 1.0);
			int height = (int)((yMax - yMin) + 1.0);
			this.hullImage = MTBImage.createMTBImage(width, height, 1, 1, 1, 
					MTBImage.MTBImageType.MTB_BYTE);
			extractHullsOnRegions(this.inRegions);
		}
		
	}
	
	/**
	 * Get set of extracted convex hulls.
	 * @return	Set of hulls.
	 */
	public Vector<Point2D.Double[]> getResultingConvexHulls()
	{
		return this.convexHulls;
	}	

	/**
	 * Get image with convex hulls plotted.
	 * @return	Image with convex hulls.
	 */
	public MTBImage getResultingHullImage()
	{
		return this.hullImage;
	}	

	private void extractHullsOnRegions(MTBRegion2DSet regs) 
			throws ALDOperatorException, ALDProcessingDAGException {

		this.convexHulls = new Vector<Point2D.Double[]>();

		for(int i = 0; i < regs.size(); i++)
		{
			Vector<Point2D.Double> pts = regs.elementAt(i).getPoints();

			Point2D.Double[] points = new Point2D.Double[pts.size()];

			for(int j = 0; j < pts.size(); j++)
			{
				points[j] = pts.elementAt(j);
			}

			Point2D.Double[] hullPoints = jarvisMarch(points);

			this.convexHulls.add(hullPoints);

			draw(hullPoints);
		}
		
	}
	
	/**
	 * Jarvis march convex hull computation
	 * 
	 * @param points points for which a convex hull should be computed
	 * @return point array containing the vertices of the convex hull polygon
	 */
	private Point2D.Double[] jarvisMarch(Point2D.Double[] points)
    {
        int i = indexOfLowestPoint(points);
        int h = 0;
        
        do
        {
            swap(points, h, i);
            
            i = indexOfRightmostPoint(points, points[h]);
            h++;
        }
        while (i > 0);
        
        if(this.verbose.booleanValue())
        {
        	System.out.println("number of points on convex hull: " + h);
        }
        
        Point2D.Double[] hullPoints = new Point2D.Double[h];
        
		for(int j = 0; j < h; j++)
		{
			hullPoints[j] = points[j];
		}
		
		return hullPoints;
    }

	
	/**
	 * 
	 * @param points
	 * @return	index of point with smallest y-coordinate, if there is more than one such point, the one with
	 * 		   	the smallest x-coordinate value is chosen
	 */
    private int indexOfLowestPoint(Point2D.Double[] points)
    {
        int min = 0;
        
        for (int i = 0; i < points.length; i++)
        {
            if (points[i].y < points[min].y || points[i].y == points[min].y && points[i].x < points[min].x)
            {
                min = i;
            }
        }
        
        return min;
    }

    
    /**
     * 
     * @param points
     * @param q
     * @return
     */
    private int indexOfRightmostPoint(Point2D.Double[] points, Point2D.Double q)
    {
    	int n = points.length;
        int i = 0;
       
        for(int j = 0; j < n; j++)
        { 
        	if(isLess(relTo(points[j], q), relTo(points[i], q)))
            {
            	i = j;
            }   
        }
        
        return i;
    }

    
    /**
     * exchange point with index i and point with index j
     * 
     * @param points 
     * @param i
     * @param j
     */
    private void swap(Point2D.Double[] points, int i, int j)
    {
        Point2D.Double t = points[i];
        points[i] = points[j];
        points[j] = t;
    }
	
    
    /**
     * @param p
     * @param q
     * @return coordinates of point p, if coordinate system is shifted by -q
     */
    private Point2D.Double relTo(Point2D.Double p, Point2D.Double q)
    {
        return new Point2D.Double(p.x - q.x, p.y - q.y);
    }
    
    
    /**
     * 
     * @param p
     * @param q
     */
    private boolean isLess(Point2D.Double p, Point2D.Double q)
    {
    	double f = p.x * q.y - q.x * p.y;
    	
        return f > 0 || f == 0 && isFurther(p,q);
    }
    
    
    /**
     * 
     * @param p
     * @param q
     */
    private boolean isFurther(Point2D.Double p, Point2D.Double q)
    {
    	return(Math.abs(p.x) + Math.abs(p.y) > Math.abs(q.x) + Math.abs(q.y));
    }
    
    
    private void draw(Point2D.Double[] hullPoints) throws ALDOperatorException, ALDProcessingDAGException
    {
    	int n = hullPoints.length;
    	
    	for(int i = 0; i < n - 1; i++)
		{
			Point2D.Double p = hullPoints[i];
			Point2D.Double q = hullPoints[i+1];
			DrawLine drawer = new DrawLine(this.hullImage, p, q);
			drawer.runOp();
		}
    	
    	Point2D.Double p = hullPoints[n-1];
		Point2D.Double q = hullPoints[0];
		DrawLine drawer = new DrawLine(this.hullImage, p, q);
		drawer.runOp();
    }

		/**
		 * Callback routine to change parameters on change of input type.
		 */
		private void inputTypeChanged() {
				try {
						if (this.inType == InputType.IMAGE) {
								if (this.hasParameter("inRegions")) {
										this.removeParameter("inRegions");
								}

								if (!this.hasParameter("inImg")) {
										this.addParameter("inImg");
								}
								if (!this.hasParameter("individually")) {
									this.addParameter("individually");
								}
								if (!this.hasParameter("eightconnected")) {
									this.addParameter("eightconnected");
								}
						} else if (this.inType == InputType.REGIONS) {
								if (this.hasParameter("inImg")) {
										this.removeParameter("inImg");
								}
								if (this.hasParameter("individually")) {
									this.removeParameter("individually");
								}
								if (this.hasParameter("eightconnected")) {
									this.removeParameter("eightconnected");
								}

								if (!this.hasParameter("inRegions")) {
										this.addParameter("inRegions");
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
