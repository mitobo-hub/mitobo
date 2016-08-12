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

import java.awt.geom.Point2D;
import java.util.Vector;

import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2D;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;
import de.unihalle.informatik.MiToBo.segmentation.regions.labeling.LabelComponentsSequential;

/**
 * operator for doing common postprocessing tasks on already segmented cell images
 * 
 * @author glass
 *
 */
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.SWING, level=Level.STANDARD)
public class CellSegmentationPostprocessing extends MTBOperator
{
	@Parameter(label = "input image", required = true, direction = Parameter.Direction.IN, supplemental = false, description = "binary input image")
	private transient MTBImage inImg = null;
	
	@Parameter(label = "result image", required = true, direction = Parameter.Direction.OUT, supplemental = false, description = "result image")
	private transient MTBImage resultImg = null;
	
	@Parameter(label = "remove border touching objects", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "should border touching objects be removed")
	private Boolean removeBorderObjects = true;
	
	@Parameter(label = "minimum area (pixels)", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "minimum area (number of pixels) an object should have")
	private Integer minArea = 0;
	
	@Parameter(label = "maximum area (pixels)", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "maximum area (number of pixels) an object should have")
	private Integer maxArea = Integer.MAX_VALUE;
	
	@Parameter(label = "are objects 8-connected", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "are objects 8-connected (4-connected otherwise)")
	private Boolean objects8Connected = false;
	
	@Parameter(label = "foreground value", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "intensity value for remaining foreground object pixel")
	private Double fg_value = 255.0;
	
//	private static int BG_VALUE = 0;
	
	private int sizeX;
	private int sizeY;
	private int sizeZ;
	private int sizeT;
	private int sizeC;
	
	
	/**
	 * 
	 * @throws ALDOperatorException
	 */
	public CellSegmentationPostprocessing() throws ALDOperatorException
	{
		
	}
	
	
	/**
	 * 
	 * @param inImg input image
	 * @throws ALDOperatorException
	 */
	public CellSegmentationPostprocessing(MTBImage inImg) throws ALDOperatorException
	{
		this.inImg = inImg;
	}
	
	
	public void operate() throws ALDOperatorException, ALDProcessingDAGException
	{
		this.sizeX = inImg.getSizeX();
		this.sizeY = inImg.getSizeY();
		this.sizeZ = inImg.getSizeZ();
		this.sizeT = inImg.getSizeT();
		this.sizeC = inImg.getSizeC();
		
		resultImg = MTBImage.createMTBImage(sizeX, sizeY, sizeZ, sizeT, sizeC, inImg.getType());
		resultImg.setCalibration(inImg.getCalibration());	// keep pixel dimensions and units
		resultImg.setTitle("result");
		
		// extract all 2D frames of the input image
		for(int c = 0; c < sizeC; c++)
		{
			for(int t = 0; t < sizeT; t++)
			{
				for(int z = 0; z < sizeZ; z++)
				{
					MTBImage currentSlice = inImg.getImagePart(0, 0, z, t, c, sizeX, sizeY, 1, 1, 1);	// extract next slice
					
					LabelComponentsSequential labeler = new LabelComponentsSequential(currentSlice, objects8Connected);
					labeler.runOp();
					MTBRegion2DSet regions = labeler.getResultingRegions();
					//MTBRegion2DSet keepRegions = new MTBRegion2DSet(0, 0, sizeX, sizeY);	//  regions to delete 
					
					// exclude image borders touching objects if desired
					if(removeBorderObjects)
					{
						regions = excludeBorderRegions(regions);
					}
					
					// remove too small regions
					if(minArea > 1)
					{
						regions = excludeSmallRegions(regions);
					}
					
					// draw remaining regions to the output image
					drawRegions(regions, z, t, c);
					
					// remove too small regions
					if(maxArea < Integer.MAX_VALUE)
					{
						regions = excludeLargeRegions(regions);
					}
					
					// draw remaining regions to the output image
					drawRegions(regions, z, t, c);
				}
			}
		}
		
		
	}
	
	
	/**
	 * exclude regions that are adjacent to the image borders
	 * @param regions image regions
	 * 
	 * @return regions that are not touching the image borders
	 */
	private MTBRegion2DSet excludeBorderRegions(MTBRegion2DSet regions)
	{
		//TODO: maybe implement this with the help of morphological operations (edge off operator)
		MTBRegion2DSet keepRegions = new MTBRegion2DSet(0, 0, sizeX, sizeY);
		
		for(int i = 0; i < regions.size(); i++)
		{
			boolean border = false;
			
			Vector<Point2D.Double> points = regions.elementAt(i).getPoints();
			
			for(int j = 0; j < points.size(); j++)
			{
				Point2D.Double p = points.elementAt(j);
				
				if(p.x == 0 || p.x == sizeX-1 || p.y == 0 || p.y == sizeY-1)
				{
					border = true;
					break;
				}
			}
			
			if(!border)
			{
				keepRegions.add(regions.elementAt(i));
			}
		}
		
		return keepRegions;
	}
	
	
	/**
	 * exclude regions that are smaller than the predefined (Parameter minArea) minimum
	 * @param regions image regions
	 * 
	 * @return regions that are not too small
	 */
	private MTBRegion2DSet excludeSmallRegions(MTBRegion2DSet regions)
	{
		MTBRegion2DSet keepRegions = new MTBRegion2DSet(0, 0, sizeX, sizeY);
		
		for(int i = 0; i < regions.size(); i++)
		{
			MTBRegion2D reg = regions.elementAt(i);
			
			if(reg.getArea() >= minArea)
			{
				keepRegions.add(reg);
			}
		}
		
		return keepRegions;
	}
	
	
	/**
	 * exclude regions that are larger than the predefined (Parameter maxArea) maximum
	 * @param regions image regions
	 * 
	 * @return regions that are not too large
	 */
	private MTBRegion2DSet excludeLargeRegions(MTBRegion2DSet regions)
	{
		MTBRegion2DSet keepRegions = new MTBRegion2DSet(0, 0, sizeX, sizeY);
		
		for(int i = 0; i < regions.size(); i++)
		{
			MTBRegion2D reg = regions.elementAt(i);
			
			if(reg.getArea() <= maxArea)
			{
				keepRegions.add(reg);
			}
		}
		
		return keepRegions;
	}
	
	/**
	 * draw regions to the output image
	 * @param keepRegions 2D regions to draw
	 * @param z slice number
	 * @param t frame number
	 * @param c channel number
	 */
	private void drawRegions(MTBRegion2DSet keepRegions, int z, int t, int c)
	{
		for(int i = 0; i < keepRegions.size(); i++)
		{
			Vector<Point2D.Double> points = keepRegions.elementAt(i).getPoints();
			
			for(int j =  0; j < points.size(); j++)
			{
				Point2D.Double p = points.elementAt(j);
				
				resultImg.putValueDouble((int)p.x, (int)p.y, z, t, c, fg_value);
			}
		}
	}
	
	
	/**
	 * should border touching objects be excluded
	 * @param excludeBorderObjects
	 */
	public void setBorderExclusion(boolean excludeBorderObjects)
	{
		this.removeBorderObjects = excludeBorderObjects;
	}
	
	
	/**
	 * set the minimum area (number of pixels) a region must have in order to be kept
	 * @param minArea
	 */
	public void setMinimumObjectArea(int minArea)
	{
		this.minArea = minArea;
	}
	
	
	/**
	 * set the maximum area (number of pixels) a region must have in order to be kept
	 * @param maxArea
	 */
	public void setMaximumObjectArea(int maxArea)
	{
		this.maxArea = maxArea;
	}
	
	
	/**
	 * are regions to be considered 8-connected (4-connected otherwise)
	 * @param eightconnected
	 */
	public void setObjectsEightConnected(boolean eightconnected)
	{
		this.objects8Connected = eightconnected;
	}
	
	
	/**
	 * set the intensity value of the remaining foreground regions
	 * @param val
	 */
	public void setFG_Value(double val)
	{
		this.fg_value = val;
	}
	
	
	/**
	 * @return result image
	 */
	public MTBImage getResultImage()
	{
		return this.resultImg;
	}
	
	
}

/*BEGIN_MITOBO_ONLINE_HELP
<p><a target="_blank" href="http://www2.informatik.uni-halle.de/agprbio/mitobo//api/de/unihalle/informatik/MiToBo/apps/singleCellTracking2D/CellSegmentationPostprocessing.html">API</a></p>

<ul>
	<li>
		<p>This operator does some modifications on already segmented binary images</p>
	</li>
	<li>
		<p>The result is a modified binary image</p>
	</li>
</ul>
<h2>Usage:</h2>

<h3>required parameters:</h3>

<ul>
	<li>
		<p><tt>input image</tt>
	<ul>
		<li>
			<p>binary image (sequence) to be postprocessed</p>
		</li>
	</ul>
		</p>
	</li>
</ul>

<h3>optional parameters:</h3>

<ul>
	<li>
		<p><tt>foreground value</tt>
	<ul>
		<li>
			<p>intensity value of cell objects</p>
		</li>
	</ul>
		</p>
	</li>
	<li>
		<p><tt>minimum area (pixels)</tt>
		<ul>
			<li>
				<p>minimum area (number of pixels) of objects to be retained</p>
			</li>
			<li>
				<p>objects with a smaller area will be discarded</p>
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
	<li>
		<p><tt>remove border touching objects</tt>
		<ul>
			<li>
				<p>if activated, objects that are connected to the image borders will be discarded</p>
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

