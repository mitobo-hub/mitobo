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

/* 
 * Most recent change(s):
 * 
 * $Rev$
 * $Date$
 * $Autho$
 * 
 */

package de.unihalle.informatik.MiToBo.morphology;


import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.MiToBo.core.datatypes.MTBStructuringElement;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage;
import de.unihalle.informatik.MiToBo.core.operator.MTBOperator;
import de.unihalle.informatik.MiToBo.math.images.MTBImageArithmetics;


/**
 * class implementing basic morphological operations (for use with grayscale as well as with binary images)<br/>
 * 
 * @author glass
 *
 */
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.ALL, level=Level.APPLICATION)
public class BasicMorphology extends MTBOperator
{
	@Parameter(label = "input image", required = true, direction = Parameter.Direction.IN, supplemental = false, description = "input image", dataIOOrder = 0)
	private transient MTBImage inImg = null;
	
	@Parameter(label = "mask size", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "side length of structuring element", dataIOOrder =1)
	private Integer maskSize = 3;
	
	@Parameter(label = "mask shape", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "shape of structuring element", dataIOOrder = 2)
	private maskShape shape = maskShape.SQUARE;
	
	@Parameter(label= "mode", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "type of operation", dataIOOrder = 3)
	private opMode mode = opMode.DILATE;
	
	@Parameter(label = "result image", required = true, direction = Parameter.Direction.OUT, supplemental = false, description = "resulting image")
	private transient MTBImage resultImg = null;
	
	public enum opMode 
	{
		DILATE, ERODE, CLOSE, OPEN, MORPH_GRADIENT, WHITE_TOPHAT, BLACK_TOPHAT
	}
	
	public enum maskShape
	{
		SQUARE, CIRCLE, HORIZONTAL_LINE, VERTICAL_LINE
	}
	
	/**
	 * shape of mask if mask != null
	 */
	private maskShape currentMaskShape = null;
	
	/**
	 * mask size of mask if mask != null
	 */
	private Integer currentMaskSize = null;
	
	private int sizeX;
	private int sizeY;
	private int sizeT;
	private int maskSizeX;
	private int maskSizeY;
	private int maskCenterX;
	private int maskCenterY;
	
//	@Parameter(label = "mask", required = false, direction = Parameter.Direction.IN, supplemental = false, description = "structuring element")
	private MTBStructuringElement mask = null;
	

	/**
	 * 
	 * @throws ALDOperatorException
	 */
	public BasicMorphology() throws ALDOperatorException
	{
		
	}
	
	/**
	 * 
	 * @param inImg	input image
	 * @param mask	structuring element used for the morphological operation
	 * @throws ALDOperatorException
	 */
	public BasicMorphology(MTBImage inImg, MTBStructuringElement mask) throws ALDOperatorException
	{
		this.inImg = inImg;
		this.mask = mask;
	}
	
	/**
	 * Constructor to create new BasicMorphology object with mask size, mode and shape of the structuring element.
	 * 
	 * @param _maskSize size of structuring element used for the morphological operation
	 * @param _mode mode of morphological operation
	 * @param _shape shape of structuring element used for the morphological operation
	 * @throws ALDOperatorException
	 * 
	 * @author Danny Misiak
	 */
	public BasicMorphology(Integer _maskSize, opMode _mode, maskShape _shape) throws ALDOperatorException
	{
			this.maskSize = _maskSize;
			this.mode = _mode;
			this.shape = _shape;
	}
	
	/**
	 * Set input image for morphological operation.
	 */
	public void setInImg(MTBImage _inImg)
	{
			this.inImg = _inImg;
	}

	@Override
	public void operate() throws ALDOperatorException, ALDProcessingDAGException
	{
//		if(mask == null || this.maskSize != this.currentMaskSize || this.shape != this.currentMaskShape)
//		{
//			mask = generateStructuringElement();
//			this.currentMaskShape = this.shape;
//			this.currentMaskSize = this.maskSize;
//		}
		
		if(mask == null)
		{
			mask = generateStructuringElement();
			this.currentMaskShape = this.shape;
			this.currentMaskSize = this.maskSize;
		}
		
		if((currentMaskSize != null && maskSize != currentMaskSize) || (currentMaskShape != null && shape != currentMaskShape))
		{
			mask = generateStructuringElement();
			this.currentMaskShape = this.shape;
			this.currentMaskSize = this.maskSize;
		}
		
		this.sizeX = inImg.getSizeX();
		this.sizeY = inImg.getSizeY();
		this.sizeT = inImg.getSizeT();
		this.maskSizeX = mask.getSizeX();
		this.maskSizeY = mask.getSizeY();
		this.maskCenterX = mask.getCenterX();
		this.maskCenterY = mask.getCenterY();
		
		if(verbose)
		{
			System.out.println("sizeX: " + sizeX);
			System.out.println("sizeY: " + sizeY);
			System.out.println("maskSizeX: " + maskSizeX);
			System.out.println("maskSizeY: " + maskSizeY);
			System.out.println("maskCenterX: " + maskCenterX);
			System.out.println("maskCenterY: " + maskCenterY);
		}
		
		switch(mode)
		{
			case DILATE:
				resultImg = dilate(inImg);
				break;
			case ERODE:
				resultImg = erode(inImg);
				break;
			case CLOSE:
				resultImg = close(inImg);
				break;
			case OPEN:
				resultImg = open(inImg);
				break;
			case MORPH_GRADIENT:
				resultImg = morphGradient(inImg);
				break;
			case WHITE_TOPHAT:
				resultImg = whiteTophat(inImg);
				break;
			case BLACK_TOPHAT:
				resultImg = blackTophat(inImg);
				break;
			default:
				break;
		}
	}
	
	
	/**
	 * @param img
	 * @return dilated image
	 */
	private MTBImage dilate(MTBImage img)
	{
		MTBImage dilImg = MTBImage.createMTBImage(sizeX, sizeY, 1, sizeT, 1, img.getType());
		dilImg.setTitle("dilation of " + img.getTitle());
		
		MTBStructuringElement reflMask = this.mask.reflect();	// reflect structuring element around its origin
		int maskCenterX = reflMask.getCenterX();
		int maskCenterY = reflMask.getCenterY();
		int maskSizeX = reflMask.getSizeX();
		int maskSizeY = reflMask.getSizeY();
		
		for(int t = 0; t < sizeT; t++)
		{
			for(int y = 0; y < sizeY; y++)
			{
				for(int x = 0; x < sizeX; x++)
				{
					double maxVal = Double.MIN_VALUE;
					
					// move center of structuring element over each image pixel
					for(int dy = -maskCenterY; dy < maskSizeY - maskCenterY; dy++)
					{
						for(int dx = -maskCenterX; dx < maskSizeX - maskCenterX; dx++)
						{
							double currVal = Double.MIN_VALUE;
							
							int offX = x + dx;
							int offY = y + dy;
							
							if(!(offX < 0 || offX >= sizeX || offY < 0 || offY >= sizeY))
							{
//								if(mask.getValue(dx + maskCenterX, dy + maskCenterY) != 0)
								if(reflMask.getValue(dx + maskCenterX, dy + maskCenterY) != 0)
								{
									currVal = img.getValueDouble(x+dx, y+dy, 0, t, 0);
								}
								
								if(currVal > maxVal)
								{
									maxVal = currVal;
								}
							}
								
						}
					}
					
					dilImg.putValueDouble(x, y, 0, t, 0, maxVal);
				}
			}
		}
		
		
		return dilImg;
	}
	
	
	/**
	 * @param img
	 * @return eroded image
	 */
	private MTBImage erode(MTBImage img)
	{
		MTBImage erodeImg = MTBImage.createMTBImage(sizeX, sizeY, 1, sizeT, 1, img.getType());
		erodeImg.setTitle("erosion of " + img.getTitle());
		
		for(int t = 0; t < sizeT; t++)
		{
			for(int y = 0; y < sizeY; y++)
			{
				for(int x = 0; x < sizeX; x++)
				{
					double minVal = Double.MAX_VALUE;
					
					// move center of structuring element over each image pixel
					for(int dy = -maskCenterY; dy < maskSizeY - maskCenterY; dy++)
					{
						for(int dx = -maskCenterX; dx < maskSizeX - maskCenterX; dx++)
						{
							double currVal = Double.MAX_VALUE;
							
							int offX = x + dx;
							int offY = y + dy;
							
							if(!(offX < 0 || offX >= sizeX || offY < 0 || offY >= sizeY))
							{
								if(mask.getValue(dx + maskCenterX, dy + maskCenterY) != 0)
								{
									currVal = img.getValueDouble(x+dx, y+dy, 0, t, 0);
								}
								
								if(currVal < minVal)
								{
									minVal = currVal;
								}
							}
								
						}
					}
					
					erodeImg.putValueDouble(x, y, 0, t, 0, minVal);
				}
			}
		}
		
		
		return erodeImg;
	}
	
	
	/**
	 * @param img
	 * @return closed image
	 */
	private MTBImage close(MTBImage img)
	{
		MTBImage dilImg = dilate(img);
		MTBImage closeImg = erode(dilImg);
		
		closeImg.setTitle("closing of " + img.getTitle());
		
		return closeImg;
	}
	
	
	/**
	 * @param img
	 * @return opened image
	 */
	private MTBImage open(MTBImage img)
	{
		MTBImage erodeImg = erode(img);
		MTBImage openImg = dilate(erodeImg);
		
		openImg.setTitle("opening of " + img.getTitle());
		
		return openImg;
	}
	
	
	/**
	 * 
	 * @param img
	 * @return morphological gradient, i.e. difference of dilation and erosion of<br/>
	 * input image
	 * @throws ALDOperatorException
	 */
	private MTBImage morphGradient(MTBImage img) throws ALDOperatorException
	{
		MTBImage dilImg = dilate(img);
		MTBImage erodeImg = erode(img);
		
		MTBImageArithmetics imageArithmetics = new MTBImageArithmetics();
		
		MTBImage gradImg = imageArithmetics.sub(dilImg, erodeImg);
		gradImg.setTitle("morphological gradient of " + img.getTitle());
		
		return gradImg;
	}
	
	
	/**
	 * calculates the white tophat transform, i.e. difference of image and the opening of the image <br/>
	 * (also called opening tophat)
	 * 
	 * @param img
	 * @return tophat processed image
	 * @throws ALDOperatorException 
	 */
	private MTBImage whiteTophat(MTBImage img) throws ALDOperatorException
	{
		MTBImage openImg = open(img);
		
		MTBImageArithmetics imageArithmetics = new MTBImageArithmetics();
		
		MTBImage wthImg = imageArithmetics.sub(img, openImg);
		wthImg.setTitle("white top hat of " + img.getTitle());
		
		return wthImg;
	}
	
	
	/**
	 * calculates the black tophat transform, i.e. difference of the closing of the image and the image <br/>
	 * (also called closing tophat)
	 * 
	 * @param img
	 * @return black tophat processed image
	 * @throws ALDOperatorException
	 */
	private MTBImage blackTophat(MTBImage img) throws ALDOperatorException
	{
		MTBImage closeImg = close(img);
		
		MTBImageArithmetics imageArithmetics = new MTBImageArithmetics();
		
		MTBImage bthImg = imageArithmetics.sub(closeImg, img);
		bthImg.setTitle("black top hat of " + img.getTitle());
		
		return bthImg;
	}
	
	/**
	 * 
	 * @param mode morphological operation to apply
	 */
	public void setMode(opMode mode)
	{
		this.mode = mode;
	}
	
	
	public void setMask(maskShape shape, int size)
	{
		this.shape = shape;
		this.maskSize = size;
	}
	
	/**
	 * 
	 * @param mask structuring element to use
	 */
	public void setMask(MTBStructuringElement mask)
	{
		this.mask = mask;
	}
	
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
	 * @return structuring element used for the morphological operation
	 */
	private MTBStructuringElement generateStructuringElement()
	{
		switch(shape)
		{
			case SQUARE:
				return MTBStructuringElement.createQuadraticElement(maskSize);
			case CIRCLE:
				return MTBStructuringElement.createCircularElement(maskSize);
			case HORIZONTAL_LINE:
				return MTBStructuringElement.createHorizontalLineElement(maskSize);
			case VERTICAL_LINE:
				return MTBStructuringElement.createVerticalLineElement(maskSize);
			default:
				return null;
				
		}
	}

}
