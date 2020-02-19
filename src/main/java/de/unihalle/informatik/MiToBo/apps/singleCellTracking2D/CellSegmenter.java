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
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException.OperatorExceptionType;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;

/**
 * base class for cell segmentation
 * 
 * @author glass
 *
 */

public abstract class CellSegmenter extends MTBOperator
{
	@Parameter(label = "input image", required = true, direction = Parameter.Direction.IN, supplemental = false, description = "input image")
	protected transient MTBImage inImg = null;
	
	@Parameter(label = "channel", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "channel used for segmentation")
	protected Integer channel = 1;
	
	@Parameter(label = "seed image", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "seed point image")
	protected transient MTBImage seedImg = null;
	
	@Parameter(label = "minimum seed size", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "minimum size for seeds to be not discarded")
	protected Integer minSeedSize = 300;
	
	@Parameter(label = "result image", required = true, direction = Parameter.Direction.OUT, supplemental = false, description = "resulting image")
	protected transient MTBImage resultImg = null;
	
	
	
	protected int sizeX;
	protected int sizeY;
	protected int sizeZ;
	protected int sizeT;
	protected int sizeC;
	
	
	protected CellSegmenter() throws ALDOperatorException
	{
		super();
	}
	
	
	public CellSegmenter(MTBImage inImg) throws ALDOperatorException
	{
		this.inImg = inImg;
	}
	
	
	public void validateCustom() throws ALDOperatorException
	{
		if(channel < 1 || channel > inImg.getSizeC()) 
		{
			throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED, "invalid channel number");
		}
	}

	@Override
	protected void operate() throws ALDOperatorException, ALDProcessingDAGException
	{
		this.sizeX = inImg.getSizeX();
		this.sizeY = inImg.getSizeY();
		this.sizeT = inImg.getSizeT();
		this.sizeC = inImg.getSizeC();
		
		verbosePrintln("input image's dimensions: sizeX = " + sizeX + ", sizeY = " + sizeY + ", sizeT = " + sizeT + ", sizeC = " + sizeC);
		
		
		resultImg = MTBImage.createMTBImage(sizeX, sizeY, 1, sizeT, 1, MTBImage.MTBImageType.MTB_BYTE);
		resultImg.setCalibration(inImg.getCalibration());	// keep pixel dimensions and units
		resultImg.setTitle("segmentation result");
		
		MTBImage createdseedImg = null;	// temporary, only used for displaying reasons
		
		if(verbose)
		{
			if(seedImg == null)
			{
				createdseedImg = MTBImage.createMTBImage(sizeX, sizeY, 1, sizeT, 1, MTBImage.MTBImageType.MTB_BYTE);
				createdseedImg.setTitle("seeds");
			}
		}
		
		
		MTBImage currFrame;
		MTBImage seedFrame;
		
		IJ.showProgress(0);
		
		// all frames are treated separately
		for(int t = 0; t < sizeT; t++)
		{
			System.out.println("processing frame " + (t+1));
			
			currFrame = inImg.getImagePart(0, 0, 0, t, channel-1, sizeX, sizeY, 1, 1, 1);
			
			// 1st step: preprocess current frame
			currFrame = preprocess(currFrame);
			
			// 2nd step: extract seed points corresponding to the current frame
			if(seedImg == null)
			{
				seedFrame = getSeedPoints(currFrame);
				
				if(verbose)
				{
					createdseedImg.setCurrentSliceIndex(t);
					createdseedImg.setCurrentSlice(seedFrame);
				}
				
			}
			else
			{
				seedFrame = seedImg.getImagePart(0, 0, 0, t, 0, sizeX, sizeY, 1, 1, 1);
			}
			
			// 3rd step: segment current frame
			resultImg.setCurrentSliceIndex(t);
			resultImg.setCurrentSlice(segment(currFrame, seedFrame));
			
			IJ.showProgress(t, sizeT);
		}
		
		if(verbose)
		{
			if(seedImg == null)
			{
				createdseedImg.show();
			}
		}	
		
	}

	
	/**
	 * segment cells in a single frame of the input image
	 * 
	 * @param frame input frame
	 * @param seedFrame
	 * @return segmented frame
	 * @throws ALDOperatorException
	 * @throws ALDProcessingDAGException
	 */
	protected abstract MTBImage segment(MTBImage frame, MTBImage seedFrame) throws ALDOperatorException, ALDProcessingDAGException;
	
	
	/**
	 * coarse segementation of input frame to localize cells
	 * 
	 * @param frame
	 * @return image containing seed points for the given frame 
	 * @throws ALDOperatorException
	 * @throws ALDProcessingDAGException
	 */
	protected abstract MTBImage getSeedPoints(MTBImage frame) throws ALDOperatorException, ALDProcessingDAGException;
	
	
	/**
	 * 
	 * @param frame
	 * @return preprocessed frame
	 * @throws ALDOperatorException
	 * @throws ALDProcessingDAGException
	 */
	protected abstract MTBImage preprocess(MTBImage frame) throws ALDOperatorException, ALDProcessingDAGException;
	
	
	/**
	 * 
	 * @return result image
	 */
	public MTBImage getResultImage()
	{
		return resultImg;
	}
	
	
	/**
	 * 
	 * @param channel
	 */
	public void setChannel(int channel)
	{
		this.channel = channel;
	}
	
	
	/**
	 * 
	 * @param minSize
	 */
	public void setMinSeedSize(int minSize)
	{
		this.minSeedSize = minSize;
	}
	
	
	/**
	 * 
	 * @param seedImg
	 */
	public void setSeedPointImage(MTBImage seedImg)
	{
		this.seedImg = seedImg;
	}
	
	
	/**
	 * prints the given text if the verbose flag is set
	 * 
	 * @param s text to print
	 */
	protected void verbosePrintln(String s)
	{
		if(verbose)
		{
			System.out.println(s);
		}
	}
}
