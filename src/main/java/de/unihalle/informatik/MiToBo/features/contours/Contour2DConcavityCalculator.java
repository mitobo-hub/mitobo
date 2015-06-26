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

package de.unihalle.informatik.MiToBo.features.contours;

import java.awt.geom.Point2D;
import java.util.Vector;

import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.ExecutionMode;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBContour2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageByte;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageInt;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;
import de.unihalle.informatik.MiToBo.segmentation.contours.extraction.ContourOnLabeledComponents;
import de.unihalle.informatik.MiToBo.segmentation.contours.extraction.ContourOnLabeledComponents.ContourType;
import de.unihalle.informatik.MiToBo.segmentation.regions.labeling.LabelComponentsSequential;

/**
 * class for calculating concaveness of contour pixels according to the method proposed in:
 * 
 * Fernandez, G., Kunt, M. & Zryd, J.P. (1995) A new plant image segmentation algorithm. 
 * Proceedings of the Eighth International Conference on Image Analysis and Processing, 
 * San Remo, Italy, pp. 229-234.
 * 
 * @author glass
 *
 */
@ALDAOperator(genericExecutionMode=ExecutionMode.ALL)
public class Contour2DConcavityCalculator extends MTBOperator
{		
	
	@Parameter(label = "input image", required = true, direction = Parameter.Direction.IN, supplemental = false, description = "input image")
	private MTBImage inImg = null;
	
	@Parameter(label = "contours", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "contours whose concaveness has to be calculated")
	private MTBContour2DSet contours = null;
	
	@Parameter(label = "radius", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "radius of considered neighbors")
	private Integer radius = 2;
	
	@Parameter(label = "is foreground eightconnected", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "are foreground pixels 8-connected or 4-connected")
	private Boolean eightconnected = true;
	
	@Parameter(label = "result image", required = true, direction = Parameter.Direction.OUT, supplemental = false, description = "resulting image")
	private MTBImageInt resultImg = null;
	
	@Parameter(label = "concavenessValues", required = true, direction = Parameter.Direction.OUT, supplemental = false, description = "resulting concaveness values for each region")
	private Vector<int[]> concavenessValues = null;
	
	private MTBContour2DSet contourWorkSet;
	
	public Contour2DConcavityCalculator() throws ALDOperatorException
	{
		this.contours = null;
	}
	
	public Contour2DConcavityCalculator(MTBImage inImg) throws ALDOperatorException, ALDProcessingDAGException
	{
		this.inImg = inImg;
		this.contours = null;
	}
	
	public Contour2DConcavityCalculator(MTBImage inImg, int radius, boolean eightconnected) throws ALDOperatorException, ALDProcessingDAGException
	{
		this.inImg = inImg;
		this.radius = radius;
		this.eightconnected = eightconnected;
		this.contours = null;
	}
	
	
	public Contour2DConcavityCalculator(MTBImage inImg, MTBContour2DSet contours, int radius, boolean eightconnected) throws ALDOperatorException
	{
		this.inImg = inImg;
		this.contours = contours;
		this.radius = radius;
		this.eightconnected = eightconnected;
	}

	/**
	 * Specify optional input contours.
	 * @param cset	Set of contours.
	 */
	public void setContours(MTBContour2DSet cset) {
		this.contours = cset;
	}
	
	/**
	 * Specify radius of local mask.
	 * @param r	Radius of mask for concavity analysis.
	 */
	public void setRadius(int r) {
		this.radius = new Integer(r);
	}
	
	@Override
	protected void operate() throws ALDOperatorException, ALDProcessingDAGException
	{
		// extract contours of individual connected components
		if (contours == null) {
			LabelComponentsSequential labeler = new LabelComponentsSequential(inImg, eightconnected);
			labeler.runOp();
			MTBRegion2DSet regions = labeler.getResultingRegions();

			ContourOnLabeledComponents tracer = new ContourOnLabeledComponents((MTBImageByte)inImg.convertType(MTBImage.MTBImageType.MTB_BYTE, true), regions, ContourType.OUT_IN_CONTOUR, 1);
			tracer.runOp();
			contourWorkSet = tracer.getResultContours();
		}
		else {
			this.contourWorkSet = this.contours;
		}
		
		resultImg = (MTBImageInt)MTBImage.createMTBImage(
				inImg.getSizeX(), inImg.getSizeY(), 1, 1, 1, 
					MTBImage.MTBImageType.MTB_INT);
			
		concavenessValues = new Vector<int[]>();
		
		for(int i = 0; i < contourWorkSet.size(); i++)
		{
			Vector<Point2D.Double> points = contourWorkSet.elementAt(i).getPoints();
			int[] concValues = getConcaveness(points);
			
			concavenessValues.add(concValues.clone());
			
			for(int j = 0; j < points.size(); j++)
			{
				Point2D.Double p = points.elementAt(j);
				
				resultImg.putValueInt((int)p.x, (int)p.y, concValues[j]);
			}
		}
	}

	/**
	 * 
	 * @param points contour points
	 * @param inImg binary image
	 * @return array containing concaveness values for the given input contour points
	 */
	private int[] getConcaveness(Vector<Point2D.Double> points)
	{
		int n = points.size();
		
		int[] tempValues = new int[n];	// concaveness values for all contour points
		
		// 1st pass: calculate concaveness values for single contour points
		for(int i = 0; i < n; i++)
		{
			Point2D.Double p = points.elementAt(i);
			tempValues[i] = concaveness((int)p.x, (int)p.y);
		}
		
		
		int[] concValues = new int[n];	// concaveness values for all contour points
		
		
		if(n > 2)
		{
			// 2nd pass: add concaveness values of 3 neighboring points for higher stability
			concValues[0] = tempValues[n-1] + tempValues[0] + tempValues[1];
			concValues[n-1] = tempValues[n-2] + tempValues[n-1] + tempValues[0];
			
			for(int i = 1; i < n - 1; i++)
			{
				concValues[i] = tempValues[i-1] + tempValues[i] + tempValues[i+1];
			}
		}
		
		return concValues;
	}
	
	
	/**
	 * 
	 * @param x
	 * @param y
	 * @param img
	 * @param radius
	 * @return
	 */
	private int concaveness(int x, int y)
	{
		int w = inImg.getSizeX();
		int h = inImg.getSizeY();
		int c = 0;
		
		for(int dy = y - radius; dy <= y + radius; dy++)
		{
			for(int dx = x - radius; dx <= x + radius; dx++)
			{
				if(dy >= 0 && dy < h && dx >= 0 && dx < w && inImg.getValueDouble(dx, dy) != 0)
				{
					c++;
				}
			}
		}
		
		return c;
	}
	

	public MTBImage getResultImage()
	{
		return this.resultImg;
	}
	
	
	public Vector<int[]> getConcavenessValues()
	{
		return this.concavenessValues;
	}
}
