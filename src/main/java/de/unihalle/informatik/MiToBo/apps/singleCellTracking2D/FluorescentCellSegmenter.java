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


import java.util.Vector;

import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.Parameter.ExpertMode;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBRegion2DSet;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImageByte;
import de.unihalle.informatik.MiToBo.enhance.GammaCorrection2D;
import de.unihalle.informatik.MiToBo.morphology.ComponentPostprocess;
import de.unihalle.informatik.MiToBo.segmentation.levelset.nonPDE.*;
import de.unihalle.informatik.MiToBo.segmentation.regions.filling.FillHoles2D;
import de.unihalle.informatik.MiToBo.segmentation.regions.labeling.LabelComponentsSequential;
import de.unihalle.informatik.MiToBo.segmentation.thresholds.CalcGlobalThreshOtsu;
import de.unihalle.informatik.MiToBo.segmentation.thresholds.ImgThresh;
import de.unihalle.informatik.MiToBo.filters.linear.GaussFilter;
import de.unihalle.informatik.MiToBo.filters.linear.GaussFilter.SigmaInterpretation;
import de.unihalle.informatik.MiToBo.filters.nonlinear.*;


/**
 * This operator is for segmenting images or image sequences containing fluorescently labeled cells
 * 
 * @author glass
 */
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.ALL, level=Level.STANDARD)
public class FluorescentCellSegmenter extends CellSegmenter
{
	//TODO: rename median related stuff to Gaussian (also in related operators)
	
	@Parameter(label = "\u03C3", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "standard deviation of Gaussian filter mask", dataIOOrder = 1)
	private Integer sigma = 1;
	
	@Parameter(label = "maximum number of iterations", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "maximum number of iterations for level set evolution", mode=ExpertMode.ADVANCED, dataIOOrder = 2)
	private Integer maxIter = 1000;
	
	@Parameter(label = " \u03B3", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "value for \u03B3-correction for emphasizing cells", mode=ExpertMode.STANDARD, dataIOOrder = 3)
	private Double gamma = 0.3;

	
	public FluorescentCellSegmenter() throws ALDOperatorException
	{
		super();
	}
	
	public FluorescentCellSegmenter(MTBImage inImg) throws ALDOperatorException
	{
		super(inImg);
	}

	
	/**
	 * 
	 * @param frame input frame
	 * @return segmented frame
	 * @throws ALDOperatorException 
	 * @throws ALDProcessingDAGException 
	 */
	@Override
	protected MTBImage segment(MTBImage frame, MTBImage seedFrame) throws ALDOperatorException, ALDProcessingDAGException
	{			
		// label resulting regions
		LabelComponentsSequential labeler = new LabelComponentsSequential(seedFrame, false);
		labeler.runOp();
		MTBRegion2DSet regions = labeler.getResultingRegions();
		
		MTBLevelsetMembership ls = new MTBLevelsetMembership(sizeX, sizeY, regions, null, false);
//		MTBLevelsetMembership ls = new MTBLevelsetMembership(sizeX, sizeY, regions, null, true);	// multiphase
				
		MTBGenericEnergyNonPDE fittingEnergy = new MTBMeijeringFittingEnergyNonPDE(frame, ls);

		// compose level set energy
		Vector<MTBGenericEnergyNonPDE> energyVector = new Vector<MTBGenericEnergyNonPDE>();
		energyVector.add(fittingEnergy);
				
		MTBGenericEnergyNonPDE energy = new MTBEnergySumNonPDE("Compound Energy", energyVector);
		
		LevelsetSolveNonPDE solver = new LevelsetSolveNonPDE(energy, ls, maxIter, 0, null, true);
//		LevelsetSolveNonPDE solver = new LevelsetSolveNonPDE(energy, ls, maxIter, 0, null, false);	// no topology preservation
		
		solver.runOp();
		
		MTBImage processedFrame = solver.getResultImage();
		
		verbosePrintln("segmentation performed with " + solver.getNumIterations() + " iterations");
		
		// set object pixels to 255
		int sizeX = processedFrame.getSizeX();
		int sizeY = processedFrame.getSizeY();
		
		for(int y = 0; y < sizeY; y++)
		{
			for(int x = 0; x < sizeX; x++)
			{
				if(processedFrame.getValueDouble(x, y) != 0)
				{
					processedFrame.putValueDouble(x, y, 255);
				}
			}
		}
		
		return processedFrame;
	}
	
	
	/**
	 * coarse segmentation of input frame to localize cells
	 * 
	 * @param frame
	 * @throws ALDOperatorException 
	 * @throws ALDProcessingDAGException 
	 */
	protected MTBImage getSeedPoints(MTBImage frame) throws ALDOperatorException, ALDProcessingDAGException
	{	
		// gamma correct filter image	
		GammaCorrection2D gammaCorrection = new GammaCorrection2D(frame, gamma, 1);
		gammaCorrection.runOp();
		frame = gammaCorrection.getResultImage();
		
		
		// threshold result
		CalcGlobalThreshOtsu otsu = new CalcGlobalThreshOtsu(frame);
		otsu.setParameter("inType", CalcGlobalThreshOtsu.InputType.IMAGE);
		otsu.runOp(HidingMode.HIDE_CHILDREN);
		double thresh = otsu.getOtsuThreshold().getValue();
		
		verbosePrintln("threshold: " + thresh);
		
		ImgThresh thresholder = new ImgThresh(frame, thresh);
		thresholder.runOp();
		MTBImage binaryFrame = thresholder.getResultImage();
		
		// fill holes
		FillHoles2D fh = new FillHoles2D(binaryFrame);
		fh.runOp();
		binaryFrame = (MTBImageByte)fh.getResultImage();
	
		// remove small objects
		ComponentPostprocess postProcessor = new ComponentPostprocess((MTBImageByte)binaryFrame, ComponentPostprocess.ProcessMode.ERASE_SMALL_COMPS);
		postProcessor.setMinimalComponentSize(minSeedSize);
		postProcessor.runOp();	
		binaryFrame = postProcessor.getResultImage();
		
		return binaryFrame;
	}
	
	
	/**
	 * 
	 * @param frame
	 * @return preprocessed frame
	 * @throws ALDOperatorException
	 * @throws ALDProcessingDAGException
	 */
	protected MTBImage preprocess(MTBImage frame) throws ALDOperatorException, ALDProcessingDAGException
	{
		MTBImage filterFrame = frame.duplicate();
		
		// smooth input frame
//		RankOperator medianFilter = new RankOperator(filterFrame, RankOperator.RankOpMode.MEDIAN, medianRadius);
//		medianFilter.runOp();
//		
//		filterFrame = medianFilter.getResultImg();
		
		GaussFilter gf = new GaussFilter(filterFrame, sigma, sigma);
		gf.setSigmaInterpretation(SigmaInterpretation.PIXEL);
		gf.setResultImageType(MTBImage.MTBImageType.MTB_BYTE);
		gf.runOp();
		
		filterFrame = gf.getResultImg();
		
		return filterFrame;
	}
	
	
	/**
	 * 
	 * @param size
	 */
	public void setSigma(int sigma)
	{
		this.sigma = sigma;
	}
	
	/**
	 * 
	 * @param maxIter
	 */
	public void setMaxIteration(int maxIter)
	{
		this.maxIter = maxIter;
	}
	
	
	/**
	 * 
	 * @param gamma
	 */
	public void setGamma(double gamma)
	{
		this.gamma = gamma;
	}
	
	
	/**
	 * @return segmentation result
	 */
	public MTBImage getResultImage()
	{
		return resultImg;
	}
	
}


/*BEGIN_MITOBO_ONLINE_HELP
<p><a target="_blank" href="http://www2.informatik.uni-halle.de/agprbio/mitobo//api/de/unihalle/informatik/MiToBo/apps/singleCellTracking2D/FluorescentCellSegmenter.html">API</a></p>

<ul>
	<li>
		<p>This operator is for segmenting images or image sequences containing fluorescently labeled cells</p>
	</li>
	<li>
		<p>Every frame is treated separately and the result is a binary image with the same dimensions as the input image</p>
	</li>
</ul>
<h2>Usage:</h2>

<h3>required parameters:</h3>

<ul>
	<li>
		<p><tt>input image</tt>
		<ul>
			<li>
				<p>image (sequence) to be segmented</p>
			</li>
		</ul>
		</p>
	</li>
</ul>

<h3>optional parameters:</h3>

<ul>
	<li>
		<p><tt>channel</tt>
		<ul>
			<li>
				<p>image channel to be segmented</p>
			</li>
		</ul>
		</p>
	</li>
	<li>
		<p><tt>minimum seed size</tt>
		<ul>
			<li>
				<p>minimum size (number of pixels) of seed objects to be considered as cells</p>
			</li>
		</ul>
		</p>
	</li>
	<li>
		<p><tt>seed image</tt>
		<ul>
			<li>
				<p>image that contains seeds (groups of pixels that don't have intensity value 0)</p>
			</li>
			<li>
				<p>seeds are used as initialization for level set segmentation</p>
			</li>
			<li>
				<p>if left empty, seeds are created automatically</p>
			</li>
		</ul>
		</p>
	</li>
	<li>
		<p><tt>median filter radius</tt>
		<ul>
			<li>
				<p>radius (pixels) of median filter used for noise reduction</p>
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
		<p><tt>&#947; (gamma)</tt>
		<ul>
			<li>
		<p>gamma correction with a value smaller than 1 is used to emphasize faintly fluorescing cells / cell parts</p>
	</li>
	<li>
		<p>if background noise is very high the value should be increased</p>
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

