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

package de.unihalle.informatik.MiToBo.apps.singleCellTracking2D;

import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.Alida.annotations.Parameter.ParameterModificationMode;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.Alida.exceptions.ALDWorkflowException;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;

/**
 * Operator for segmenting, tracking and analyzing 2D image sequences of fluorescently labeled cells
 * 
 * @author glass
 *
 */
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.ALL, level=Level.APPLICATION, allowBatchMode = false)
public class CellMigrationAnalyzer extends MTBOperator
{
	@Parameter(label = "input image", required = true, direction = Parameter.Direction.IN, supplemental = false, description = "input image", dataIOOrder = 0,
				callback = "getCalibration", paramModificationMode = ParameterModificationMode.MODIFIES_INTERFACE)
	private transient MTBImage inImg = null;
	
	// segmentation parameters
	@Parameter(label = "detection channel", required = true, direction = Parameter.Direction.IN, supplemental = false, description = "detection channel", dataIOOrder = 1)
	private Integer detectionChannel = 1;
	
	@Parameter(label = "minimum seed size", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "minimum size for seeds to be not discarded", dataIOOrder = 2, mode=ExpertMode.ADVANCED)
	private Integer minSeedSize = 300;
	
	@Parameter(label = "\u03C3", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "standard deviation of Gaussian filter mask", dataIOOrder = 3)
	private Integer sigma = 1;
	
	@Parameter(label = " \u03B3", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "value for \u03B3-correction for emphasizing cells", dataIOOrder = 4)
	private Double gamma = 0.3;
	
	@Parameter(label = "maximum number of iterations", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "maximum number of iterations for level set evolution", dataIOOrder = 5, mode=ExpertMode.ADVANCED)
	private Integer maxIter = 1000;
	
	@Parameter(label = "use mask channel", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "use mask channel", dataIOOrder = 6, mode=ExpertMode.ADVANCED,
				callback = "showMaskChannelTextbox", paramModificationMode = ParameterModificationMode.MODIFIES_INTERFACE)
	private Boolean useMask = false;
	
	@Parameter(label = "mask channel", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "mask channel", dataIOOrder = 7, mode=ExpertMode.ADVANCED)
	private Integer maskChannel = 2;
	
	@Parameter(label = "include bright objects from mask", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "include tracks that have sufficient signals in mask image (otherwise these tracks are excluded)", dataIOOrder = 8, mode=ExpertMode.ADVANCED)
	private Boolean include = true;
	
	@Parameter(label = "average factor", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "object's mean intensity must be at least mean image intensity multiplied with this factor to be included/ excluded", dataIOOrder = 9, mode=ExpertMode.ADVANCED)
	private Double avgFactor = 3.0;
	
	// postprocessing parameters
	@Parameter(label = "remove border touching objects", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "should border touching objects be removed", dataIOOrder = 10, mode=ExpertMode.ADVANCED)
	private Boolean removeBorderObjects = true;
	
	@Parameter(label = "minimum area (pixels)", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "minimum area (number of pixels) an object should have", dataIOOrder = 11)
	private Integer minArea = 500;
	
	// tracking parameters
	@Parameter(label = "determine gating distance automatically", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "determine gating distance automatically", dataIOOrder = 12, mode=ExpertMode.ADVANCED,
				callback = "showMaxDistTextbox", paramModificationMode = ParameterModificationMode.MODIFIES_INTERFACE)
	private Boolean useAutoDistance = true;
	
	@Parameter(label = "maximum distance (pixels)", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "maximum distance for two objects to be assigned to each other", dataIOOrder = 13, mode=ExpertMode.ADVANCED)
	private Double maxDist = 30.0;
	
	@Parameter(label = "maximum area change", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "maximum change in area (fraction) for two objects to be assigned to each other", dataIOOrder = 14)
	private Double maxAreaChange = 0.5;
	
	//analysis parameters
	@Parameter(label = "pixel length, x-direction", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "pixel length in x-direction", dataIOOrder = 15)
	private Double deltaX = 1.0;
	
	@Parameter(label = "pixel length, y-direction", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "pixel length in y-direction", dataIOOrder = 16)
	private Double deltaY = 1.0;
	
	@Parameter(label = "unit space", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "unit x/y", dataIOOrder = 17)
	private String unitXY = "pixel";
	
	@Parameter(label = "time between frames", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "time between frames", dataIOOrder = 18)
	private Double deltaT = 5.0;
	
	@Parameter(label = "unit time", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "unit time", dataIOOrder = 19)
	private String unitT = "min";
	
	@Parameter(label = "minimum track length", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "minimum track length to be considered", dataIOOrder = 20)
	private Integer minTrackLength = 24;
	
	@Parameter(label = "analyze trajectories", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "should the objects' trajectories be analyzed", dataIOOrder = 21)
	private Boolean analyzeTrajectories = true;
	
	@Parameter(label = "analyze shapes", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "should the objects' shapes be analyzed", dataIOOrder = 22)
	private Boolean analyzeShapes = false;
	
	@Parameter(label = "analyze intensities", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "should the objects' intensities be analyzed", dataIOOrder = 23)
	private Boolean analyzeIntensities = false;
	
	//visualization
	@Parameter(label = "show trajectory map", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "should a 2D map of the extracted trajectories be shown", dataIOOrder = 24)
	private Boolean showTrajectoryMap = false;
	
	@Parameter(label = "show overlay image", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "should the extracted trajectories be shown overlayed on the cells", dataIOOrder = 25)
	private Boolean showOverlayImage = false;
	
	@Parameter(label = "remove excluded objects", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "should objects that weren't analyzed be removed from the label image", dataIOOrder = 26)
	private Boolean removeExcluded = false;
	
	// output parameters
//	@Parameter(label = "segmentation result", required = true, direction = Parameter.Direction.OUT, supplemental = false, description = "resulting segmentation image")
	private transient MTBImage segmentationResult = null;
	
	@Parameter(label = "label result", required = true, direction = Parameter.Direction.OUT, supplemental = false, description = "resulting label image")
	private transient MTBImage labelResult = null;
	

	public CellMigrationAnalyzer() throws ALDOperatorException
	{
		
	}
	
	public CellMigrationAnalyzer(MTBImage img) throws ALDOperatorException
	{
		this.inImg = img;
	}

	@Override
	protected void operate() throws ALDOperatorException, ALDProcessingDAGException
	{	
		String title = inImg.getTitle();
		
		int sizeX = inImg.getSizeX();
		int sizeY = inImg.getSizeY();
		int sizeT = inImg.getSizeT();
		
		// segmentation of detection channel
		FluorescentCellSegmenter segmenter = new FluorescentCellSegmenter(inImg);
		segmenter.setChannel(detectionChannel);
		segmenter.setSigma(sigma);
		segmenter.setMinSeedSize(minSeedSize);
		segmenter.setMaxIteration(maxIter);
		segmenter.setGamma(gamma);
		segmenter.setVerbose(this.verbose);
				
		segmenter.runOp(HidingMode.HIDE_CHILDREN);
				
		segmentationResult = segmenter.getResultImage();
		
		// postprocessing
		CellSegmentationPostprocessing postprocessor = new CellSegmentationPostprocessing(segmentationResult);
		postprocessor.setMinimumObjectArea(minArea);
		postprocessor.setBorderExclusion(removeBorderObjects);
		postprocessor.setObjectsEightConnected(false);
		postprocessor.setVerbose(this.verbose);
		
		postprocessor.runOp(HidingMode.HIDE_CHILDREN);
		
		segmentationResult = postprocessor.getResultImage();
		segmentationResult.setTitle(title + "_segmentation");
		
		// association
		CellTrackerBipartite tracker = new CellTrackerBipartite(segmentationResult);
		tracker.setMaxAreaChange(maxAreaChange);
		tracker.useAutoDistanceDetermination(useAutoDistance);
		tracker.setMaxDistance(maxDist);
		tracker.setObjectsEightConnected(false);
		tracker.setVerbose(this.verbose);
		
		tracker.runOp(HidingMode.HIDE_CHILDREN);
		
		labelResult = tracker.getResultImage();
		labelResult.setTitle(title + "_tracking");
		
		// analysis
		MigrationAnalyzer analyzer;
		
		if(useMask)//(maskResult != null)
		{
			MTBImage maskImg = inImg.getImagePart(0, 0, 0, 0, maskChannel-1, sizeX, sizeY, 1, sizeT, 1);	
			
			analyzer = new MigrationAnalyzer(labelResult, maskImg);
//			analyzer = new MigrationAnalyzer(tracker.getResultImage(), maskImg);
			analyzer.includeMask(include);
			analyzer.setFactor(avgFactor);
		}
		else
		{
			analyzer = new MigrationAnalyzer(labelResult);
//			analyzer = new MigrationAnalyzer(tracker.getResultImage());
		}
		
		analyzer.setIntensityImage(inImg.getImagePart(0, 0, 0, 0, detectionChannel-1, sizeX, sizeY, 1, sizeT, 1));
		analyzer.setDeltaX(deltaX);
		analyzer.setDeltaY(deltaY);
		analyzer.setDeltaT(deltaT);
		analyzer.setUnitXY(unitXY);
		analyzer.setUnitT(unitT);
		analyzer.setMinTrackLength(minTrackLength);
		analyzer.analyzeTrajectories(analyzeTrajectories);
		analyzer.analyzeShapes(analyzeShapes);
		analyzer.analyzeIntensities(analyzeIntensities);
		analyzer.showTrajectoryMap(showTrajectoryMap);
		analyzer.showOverlayImage(showOverlayImage);
		analyzer.removeExcludedObjects(removeExcluded);
		
		analyzer.setVerbose(this.verbose);
		
		analyzer.runOp(HidingMode.HIDE_CHILDREN);
		
		labelResult = analyzer.getLabelImage();
		labelResult.setTitle(title + "_tracking");
		
		if(verbose)
		{
			segmentationResult.show();
		}
		
	}
	
	
	// ------------------------------ callback functions ------------------------------
	
	@SuppressWarnings("unused")
	private void getCalibration()
	{
		try
		{
			if(this.inImg != null)
			{
				this.deltaX = inImg.getCalibration().pixelWidth;
				this.deltaY = inImg.getCalibration().pixelHeight;
				this.unitXY = inImg.getCalibration().getXUnit();
				
				if(inImg.getSizeC() < 2)
				{
					this.detectionChannel = 1;
					
					if(this.hasParameter("detectionChannel"))
					{
						this.removeParameter("detectionChannel");
					}
					
					this.useMask = false;
					
					if(this.hasParameter("useMask"))
					{
						this.removeParameter("useMask");
					}
					
					if(this.hasParameter("maskChannel"))
					{
						this.removeParameter("maskChannel");
					}
					
					this.include = false;
					
					if(this.hasParameter("include"))
					{
						this.removeParameter("include");
					}
					
					if(this.hasParameter("avgFactor"))
					{
						this.removeParameter("avgFactor");
					}
					
					
				}
				else
				{	
					if(!this.hasParameter("detectionChannel"))
					{
						this.addParameter("detectionChannel");
					}
					
					if(!this.hasParameter("useMask"))
					{
						this.addParameter("useMask");
					}
					
					if(useMask)
					{
						if(!this.hasParameter("maskChannel"))
						{
							this.addParameter("maskChannel");
						}
						
						if(!this.hasParameter("include"))
						{
							this.addParameter("include");
						}
						
						if(!this.hasParameter("avgFactor"))
						{
							this.addParameter("avgFactor");
						}
					}
					
				}
			}
		}
		catch(ALDOperatorException e)
		{
			e.printStackTrace();
		}
		
	}
	
	
	@SuppressWarnings("unused")
	private void showMaskChannelTextbox()
	{
		try
		{
			if(useMask)
			{
				if(!this.hasParameter("maskChannel"))
				{
					this.addParameter("maskChannel");
				}
				
				if(!this.hasParameter("include"))
				{
					this.addParameter("include");
				}
				
				if(!this.hasParameter("avgFactor"))
				{
					this.addParameter("avgFactor");
				}
			}
			else
			{
				if(this.hasParameter("maskChannel"))
				{
					this.removeParameter("maskChannel");
				}
				
				if(this.hasParameter("include"))
				{
					this.removeParameter("include");
				}
				
				if(this.hasParameter("avgFactor"))
				{
					this.removeParameter("avgFactor");
				}
			}
			
		}
		catch(ALDOperatorException e)
		{
			e.printStackTrace();
		}
		
	}
	
	
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
<p><a target="_blank" href="http://www2.informatik.uni-halle.de/agprbio/mitobo//api/de/unihalle/informatik/MiToBo/apps/singleCellTracking2D/CellMigrationAnalyzer.html">API</a></p>

<ul>
	<li>
		<p>Operator for segmenting, tracking and analyzing 2D image sequences of fluorescently labeled cells</p>
	</li>
</ul>
<h2>Usage:</h2>
<h3>required parameters:</h3>

<ul>
	<li>
		<p><tt>input image</tt>
	<ul>
		<li>
			<p>image (sequence) to be analyzed</p>
		</li>
	</ul>
	</p>
	</li>
</ul>
<ul>
	<li>
		<p><tt>detection channel</tt>
	<ul>
		<li>
			<p>channel used for segmentation (whole cells should be stained in this channel)</p>
		</li>
	</ul>
	</p>
	</li>
</ul>
<h3>optional parameters:</h3>

<ul>
	<li>
		<p><tt>minimum seed size [Advanced View]</tt>
	<ul>
		<li>
			<p>minimum size (number of pixels) of seed objects to be considered as cells</p>
		</li>
	</ul>
	</p>
	</li>
	<li>
		<p><tt>&#963; (sigma)</tt>
	<ul>
		<li>
			<p>standard deviation of Gaussian filter used for noise reduction</p>
		</li>
	</ul>
	</p>
	</li>
	<li>
		<p><tt>&#947; (gamma)</tt>
	<ul>
		<li>
			<p>gamma correction with a value smaller than 1 is used to emphasize faintly fluorescing cells/ cell parts</p>
		</li>
		<li>
			<p>if background noise is very high the value should be increased</p>
		</li>
	</ul>
	</p>
	</li>
	<li>
		<p><tt>maximum number of iterations [Advanced View]</tt>
	<ul>
		<li>
			<p>maximum number of iterations for level set segmentation</p>
		</li>
	</ul>
	</p>
	</li>
	<li>
		<p><tt>use mask channel [Advanced View]</tt>
	<ul>
		<li>
			<p>should an additional fluorescence channel be used for excluding certain cells</p>
		</li>
	</ul>
	</p>
	</li>
	<li>
		<p><tt>mask channel [Advanced View]</tt>
	<ul>
		<li>
			<p>additional fluorescence channel used for excluding certain cells (<tt>use mask channel</tt> must be activated)</p>
		</li>
	</ul>
	</p>
	</li>
	<li>
		<p><tt>include bright objects from mask [Advanced View]</tt>
	<ul>
		<li>
			<p>if activated bright cells are excluded from the analysis, else dark cells are excluded (refered to the <tt>mask channel</tt>; <tt>use mask channel</tt> must be activated, too)</p>
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
		<p><tt>remove border touching objects</tt>
		<ul>
			<li>
				<p>objects that are connected to the image borders will be discarded, if activated</p>
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
			<p>objects with a smaller area will not be analyzed (but they do appear on the resulting label image)</p>
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
			<p>if not, the <tt>maximum distance (pixels)</tt> is used</p>
		</li>
	</ul>
	</p>
	</li>
	<li>
		<p><tt>maximum distance (pixels)</tt>
	<ul>
		<li>
			<p>maximum distance (gating distance) a cell is assumed to move between two consecutive frames</p>
		</li>
		<li>
			<p>if the centroid distance of two regions from subsequent frames exceeds this value, these regions are not considered to belong to the same cell</p>
		</li>
		<li>
			<p>only used if automatic gating distance determination is deactivated</p>
		</li>
	</ul>
	</p>
	</li>
	<li>
		<p><tt>maximum area change</tt>
	<ul>
		<li>
			<p>maximum relative change in area a cell is assumed to undergo between two consecutive frames</p>
		</li>
		<li>
			<p>if the fraction of the areas of two regions from subsequent frames differ more than this value, these regions are not considered to belong to the same cell</p>
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
			<p>should image intensities be analyzed</p>
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
END_MITOBO_ONLINE_HELP*/
