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

import ij.text.TextWindow;

import java.util.Vector;

import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.Alida.annotations.Parameter.ParameterModificationMode;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;

/**
 * Operator for analyzing the movement pattern of cells as well as changes in morphology and fluorescence intensity
 * 
 * @author glass
 *
 */
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.ALL, level=Level.STANDARD)
public class MigrationAnalyzer extends MTBOperator
{
	@Parameter(label = "label image", required = true, direction = Parameter.Direction.INOUT, supplemental = false, description = "labeled input image", dataIOOrder = 0,
				callback = "getCalibration", paramModificationMode = ParameterModificationMode.MODIFIES_INTERFACE)
	private transient MTBImage labelImg = null;
	
	@Parameter(label = "intensity image", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "additional intensity image", dataIOOrder = 1)
	private transient MTBImage intImg = null;
	
	@Parameter(label = "mask image", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "intensity image used to include or exclude certain objects", dataIOOrder = 2, mode=ExpertMode.ADVANCED,
				callback = "showMaskChannelTextbox", paramModificationMode = ParameterModificationMode.MODIFIES_INTERFACE)
	private transient  MTBImage maskImg = null;
	
	@Parameter(label = "include mask", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "include tracks that have corresponding signals in mask image (otherwise these tracks are excluded)", dataIOOrder = 3, mode=ExpertMode.STANDARD)
	private Boolean include = true;
	
	@Parameter(label = "average factor", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "object's mean intensity must be at least mean image intensity multiplied with this factor to be included/ excluded", dataIOOrder = 4, mode=ExpertMode.STANDARD)
	private Double factor = 3.0;
	
	@Parameter(label = "pixel length, x-direction", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "pixel length in x-direction", dataIOOrder = 5)
	private Double deltaX = 1.0;
	
	@Parameter(label = "pixel length, y-direction", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "pixel length in y-direction", dataIOOrder = 6)
	private Double deltaY = 1.0;
	
	@Parameter(label = "unit space", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "unit x/y", dataIOOrder = 7)
	private String unitXY = "pixel";
	
	@Parameter(label = "time between frames", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "time between frames", dataIOOrder = 8)
	private Double deltaT = 5.0;
	
	@Parameter(label = "unit time", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "unit time", dataIOOrder = 9)
	private String unitT = "min";
	
	@Parameter(label = "minimum track length", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "minimum track length to be considered", dataIOOrder = 10)
	private Integer minTrackLength = 24;
	
	@Parameter(label = "analyze trajectories", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "should the objects' trajectories be analyzed", dataIOOrder = 11)
	private Boolean analyzeTrajectories = true;
	
	@Parameter(label = "analyze shapes", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "should the objects' shapes be analyzed", dataIOOrder = 12)
	private Boolean analyzeShapes = false;
	
	@Parameter(label = "analyze intensities", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "should the objects' intensities be analyzed", dataIOOrder = 13)
	private Boolean analyzeIntensities = false;
	
	@Parameter(label = "show trajectory map", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "should a 2D map of the extracted trajectories be shown", dataIOOrder = 14)
	private Boolean showTrajectoryMap = false;
	
	@Parameter(label = "show overlay image", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "should the extracted trajectories be shown overlayed on the cells", dataIOOrder = 15)
	private Boolean showOverlayImage = false;

	@Parameter(label = "remove excluded objects", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "should objects that weren't analyzed be removed from the label image", dataIOOrder = 16)
	private Boolean removeExcluded = false;
	
	@Parameter(label = "track report", required = true, direction = Parameter.Direction.OUT, supplemental = false, description = "track report")
	private String trackReport = "";
	
	@Parameter(label = "shape report", required = true, direction = Parameter.Direction.OUT, supplemental = false, description = "shape report")
	private String shapeReport = "";
	
	@Parameter(label = "intensity report", required = true, direction = Parameter.Direction.OUT, supplemental = false, description = "intensity report")
	private String intReport = "";

	public MigrationAnalyzer() throws ALDOperatorException
	{
		
	}
	
	public MigrationAnalyzer(MTBImage labelImg) throws ALDOperatorException
	{
		this.labelImg = labelImg;
	}
	
	
	public MigrationAnalyzer(MTBImage labelImg, MTBImage maskImg) throws ALDOperatorException
	{
		this.labelImg = labelImg;
		this.maskImg = maskImg;
	}
	

	@Override
	public void operate() throws ALDOperatorException, ALDProcessingDAGException
	{
		TrajectoryExtraction2D trajectoryExtraction = new TrajectoryExtraction2D(labelImg, minTrackLength);
		
		if(maskImg == null)
		{
			trajectoryExtraction.extractCentroids();
		}
		else
		{
			trajectoryExtraction.extractCentroids(maskImg, include, factor);
		}
		
		Vector<Trajectory2D> trajectories = trajectoryExtraction.getTrajectories();
		Vector<Integer> excluded = trajectoryExtraction.getExcluded();
		
		if(analyzeTrajectories)
		{
			if(verbose)
			{
				System.out.println("analyzing trajectories ...");
			}
			
			TrackAnalyzer trackAnalyzer = new TrackAnalyzer(trajectories);
			trackAnalyzer.setMinTrackLength(minTrackLength);
			trackAnalyzer.setDeltaX(deltaX);
			trackAnalyzer.setDeltaY(deltaY);
			trackAnalyzer.setDeltaT(deltaT);
			trackAnalyzer.setUnitSpace(unitXY);
			trackAnalyzer.setUnitTime(unitT);
			trackAnalyzer.setExcluded(excluded);
			
			trackAnalyzer.runOp();
			
			trackReport = trackAnalyzer.getReport();
			
			TextWindow tw = new TextWindow(labelImg.getTitle() + "_trajectory_evaluation", trackReport, 600, 800);
			tw.setVisible(true);
		}
		
		if(analyzeShapes)
		{
			if(verbose)
			{
				System.out.println("analyzing shapes ...");
			}
			
			ShapeAnalyzer shapeAnalyzer = new ShapeAnalyzer(labelImg);
			shapeAnalyzer.setMinTrackLength(minTrackLength);
			shapeAnalyzer.setDeltaX(deltaX);
			shapeAnalyzer.setDeltaY(deltaY);
			shapeAnalyzer.setDeltaT(deltaT);
			shapeAnalyzer.setUnitSpace(unitXY);
			shapeAnalyzer.setUnitTime(unitT);
			shapeAnalyzer.setExcluded(excluded);
			
			shapeAnalyzer.runOp();
		
			shapeReport = shapeAnalyzer.getReport();
		
			TextWindow sw = new TextWindow(labelImg.getTitle() + "_shape_evaluation", shapeReport, 600, 800);
			sw.setVisible(true);
		}
		
		if(analyzeIntensities)
		{
			if(verbose)
			{
				System.out.println("analyzing intensities ...");
			}
			
			if(intImg != null)
			{
				IntensityAnalyzer intensityAnalyzer = new IntensityAnalyzer(intImg, labelImg);
				intensityAnalyzer.setMinTrackLength(minTrackLength);
				intensityAnalyzer.setDeltaX(deltaX);
				intensityAnalyzer.setDeltaY(deltaY);
				intensityAnalyzer.setDeltaT(deltaT);
				intensityAnalyzer.setUnitSpace(unitXY);
				intensityAnalyzer.setUnitTime(unitT);
				intensityAnalyzer.setExcluded(excluded);
				
				intensityAnalyzer.runOp();
				intReport = intensityAnalyzer.getReport();
				
				TextWindow iw = new TextWindow(labelImg.getTitle() + "_intensity_evaluation", intReport, 600, 800);
				iw.setVisible(true);
			}
		}
		
		try
		{
			if(showTrajectoryMap)
			{
				TrackVisualizer visualizer = new TrackVisualizer(labelImg, trajectories);
				visualizer.create2DTrajectoryImage(true, false).show();
			}
			
			if(showOverlayImage && intImg != null)
			{
				TrackVisualizer visualizer = new TrackVisualizer(intImg, trajectories);
				visualizer.createOverlayImage(true).show();
			}
			
		} 
		catch(ALDOperatorException e)
		{
			e.printStackTrace();
		}
		
		// delete (set 0) objects from label image that weren't analyzed
		if(removeExcluded)
		{
			int sizeT = labelImg.getSizeT();
			int sizeY = labelImg.getSizeY();
			int sizeX = labelImg.getSizeX();
			
			for(int t = 0; t < sizeT; t++)
			{
				for(int y = 0; y < sizeY; y++)
				{
					for(int x = 0; x < sizeX; x++)
					{
						if(excluded.contains(labelImg.getValueInt(x, y, 0, t, 0)))
						{
							labelImg.putValueInt(x, y, 0, t, 0, 0);
						}
						
					}
				}
			}	
		}
		
		labelImg.updateAndRepaintWindow();
	}
	
	
	/**
	 * 
	 * @param intImg
	 */
	public void setIntensityImage(MTBImage intImg)
	{
		this.intImg = intImg;
	}
	
	
	/**
	 * 
	 * @param deltaX
	 */
	public void setDeltaX(double deltaX)
	{
		this.deltaX = deltaX;
	}
	
	
	/**
	 * 
	 * @param deltaY
	 */
	public void setDeltaY(double deltaY)
	{
		this.deltaY = deltaY;
	}
	
	
	/**
	 * 
	 * @param deltaT
	 */
	public void setDeltaT(double deltaT)
	{
		this.deltaT = deltaT;
	}
	
	
	/**
	 * 
	 * @param unitXY
	 */
	public void setUnitXY(String unitXY)
	{
		this.unitXY = unitXY;
	}
	
	
	/**
	 * 
	 * @param unitT
	 */
	public void setUnitT(String unitT)
	{
		this.unitT = unitT;
	}
	
	
	/**
	 * 
	 * @param minTrackLength
	 */
	public void setMinTrackLength(int minTrackLength)
	{
		this.minTrackLength = minTrackLength;
	}
	
	
	public void includeMask(boolean include)
	{
		this.include = include;
	}
	
	
	public void removeExcludedObjects(boolean remove)
	{
		this.removeExcluded = remove;
	}
	
	
	public void setFactor(double factor)
	{
		this.factor = factor;
	}
	
	/**
	 * 
	 * @param analyzeTrajectories
	 */
	public void analyzeTrajectories(boolean analyzeTrajectories)
	{
		this.analyzeTrajectories = analyzeTrajectories;
	}
	
	
	/**
	 * 
	 * @param analyzeShapes
	 */
	public void analyzeShapes(boolean analyzeShapes)
	{
		this.analyzeShapes = analyzeShapes;
	}
	
	
	/**
	 * 
	 * @param analyzeIntensities
	 */
	public void analyzeIntensities(boolean analyzeIntensities)
	{
		this.analyzeIntensities = analyzeIntensities;
	}
	
	
	/**
	 * 
	 * @param showTrajectories
	 */
	public void showTrajectoryMap(boolean showTrajectories)
	{
		this.showTrajectoryMap = showTrajectories;
	}
	
	
	/**
	 * 
	 * @param showOverlay
	 */
	public void showOverlayImage(boolean showOverlay)
	{
		this.showOverlayImage = showOverlay;
	}
	
	
	/**
	 * 
	 * @return label image
	 */
	public MTBImage getLabelImage()
	{
		return this.labelImg;
	}
	
	
	/**
	 * 
	 * @return tracking report
	 */
	public String getTrackReport()
	{
		return this.trackReport;
	}
	
	
	/**
	 * 
	 * @return shape report
	 */
	public String getShapeReport()
	{
		return this.shapeReport;
	}
	
	
	/**
	 * 
	 * @return intensity report
	 */
	public String getIntensityReport()
	{
		return this.intReport;
	}

	
	// ------------------------------ callback functions ------------------------------
	
		@SuppressWarnings("unused")
		private void getCalibration()
		{
			
			if(this.labelImg != null)
			{
				this.deltaX = labelImg.getCalibration().pixelWidth;
				this.deltaY = labelImg.getCalibration().pixelHeight;
				this.unitXY = labelImg.getCalibration().getXUnit();
			}
		}
		
		
		@SuppressWarnings("unused")
		private void showMaskChannelTextbox()
		{
			try
			{
				if(maskImg != null)
				{
					if(!this.hasParameter("include"))
					{
						this.addParameter("include");
					}
					
					if(!this.hasParameter("factor"))
					{
						this.addParameter("factor");
					}
				}
				else
				{
					
					if(this.hasParameter("include"))
					{
						this.removeParameter("include");
					}
					
					if(this.hasParameter("factor"))
					{
						this.removeParameter("factor");
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
		<p>Operator for analyzing the movement pattern of cells as well as changes in morphology and fluorescence intensity</p>
	</li>
</ul>

<h2>Usage:</h2>

<h3>required parameters:</h3>

<ul>
	<li>
		<p><tt>label image</tt>
		<ul>
			<li>
				<p>image sequence where each cell region has a unique label</p>
			</li>
		</ul>
		</p>
	</li>
</ul>

<h3>optional parameters:</h3>

<ul>
	<li>
		<p><tt>intensity image</tt>
		<ul>
			<li>
				<p>image sequence containing the cell intensities</p>
			</li>
			<li>
				<p>only needed if intensities should be analyzed</p>
			</li>
		</ul>
		</p>
	</li>
	<li>
		<p><tt>mask image [Advanced View]</tt>
		<ul>
			<li>
				<p>image sequence containing additional fluorescence intensities that are used to exclude certain cells</p>
			</li>
			<li>
				<p>only needed if cells showing or lacking this fluorescence should be discarded from the analysis</p>
			</li>
		</ul>
		</p>
	</li>
	<li>
		<p><tt>include mask [Advanced View]</tt>
		<ul>
			<li>
				<p>if activated, only those tracks that have corresponding signals in mask image are included in the analysis (otherwise only these tracks are excluded)</p>
			</li>
		</ul>
		</p>
	</li>
	<li>
		<p><tt>average factor [Advanced View]</tt>
	<ul>
		<li>
			<p>only cells with average intensity above / below <tt>average factor</tt> times average intensity of the whole frame are excluded/ included(depending on whether <tt>include bright objects from mask</tt> is activated or not)</p>
		</li>
	</ul>
	</p>
	</li>
	<li>
		<p><tt>pixel length, x-direction</tt>
		<ul>
			<li>
				<p>physical length of a pixel in x-direction</p>
			</li>
		</ul>
		</p>
	</li>
	<li>
		<p><tt>pixel length, y-direction</tt>
		<ul>
			<li>
				<p>physical length of a pixel in y-direction</p>
			</li>
		</ul>
		</p>
	</li>
	<li>
		<p><tt>unit space</tt> 
		<ul>
			<li>
				<p>unit of measurement for pixel size</p>
			</li>
		</ul>
		</p>
	</li>
	<li>
		<p><tt>time between frames</tt>
		<ul>
			<li>
				<p>time elapsed between the acqusition of two consecutive frames</p>
			</li>
		</ul>
		</p>
	</li>
	<li>
		<p><tt>unit time</tt>
	<ul>
		<li>
			<p>unit of measurement for the time</p>
		</li>
	</ul>
		</p>
	</li>
	<li>
		<p><tt>minimum track length</tt>
		<ul>
			<li>
				<p>minimum length (number of consecutive frames) of a track to be considered for analysis</p>
			</li>
		</ul>
		</p>
	</li>
	<li>
		<p><tt>analyze trajectories</tt>
		<ul>
			<li>
				<p>should cell trajectories be analyzed</p>
			</li>
		</ul>
		</p>
	</li>
	<li>
		<p><tt>analyze shapes</tt>
		<ul>
			<li>
				<p>should cell shapes be analyzed</p>
			</li>
		</ul>
		</p>
	</li>
	<li>
		<p><tt>analyze intensities</tt>
		<ul>
			<li>
				<p>should image intensities be analysized</p>
			</li>
			<li>
				<p>requires an intensity image corresponding to the label image</p>
			</li>
		</ul>
		</p>
	</li>
	<li>
		<p><tt>show trajectory map</tt>
		<ul>
			<li>
				<p>should a 2D map of the trajectories be created</p>
			</li>
		</ul>
		</p>
	</li>
	<li>
		<p><tt>show overlay image</tt>
	<ul>
		<li>
			<p>should a new image sequence with cells and inpainted trajectories be created</p>
		</li>
		<li>
			<p>requires the intensity image</p>
		</li>
	</ul>
	</p>
	</li>
	<li>
		<p><tt>remove excluded objects</tt>
	<ul>
		<li>
			<p>if activated, all cells that weren't analyzed (e.g. because their tracks were smaller than <tt>minimum track length</tt> are not displayed in the resulting label image)</p>
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
