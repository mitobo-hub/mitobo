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

package de.unihalle.informatik.MiToBo.visualization.colormappings;

import java.awt.Color;

import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException.OperatorExceptionType;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.MiToBo.core.datatypes.images.*;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.operator.*;

/**
 * This operator converts a gray-scale image to a heat map image.
 * <p>
 * The given minimal value is mapped to the range minimum color, the given 
 * maximal value to the range maximum color. All gray values in between are 
 * mapped by linear interpolation between both colors.<br> 
 * Values which are smaller than the range minimum or larger than the maximum 
 * are by default mapped to the minimal and maximal colors, respectively.
 * If {@link #ignoreOutOfRangeValues} is selected they are left untouched. 
 * If in addition {@link GrayscaleImageToHeatmap#mapIgnoredValuesToBlack} is selected
 * they are mapped to black.  
 *
 * @author moeller
 */
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.ALL,
		level=Level.APPLICATION)
public class GrayscaleImageToHeatmap extends MTBOperator {

	/**
	 * Class identifier.
	 */
	private static final String classID = "[GrayScaleImageToHeatmap]";
	
	/**
	 * Available modes how to handle values out of range.
	 */
	public static enum OutOfRangeValuesHandlingMode {
		/**
		 * Leave values untouched.
		 */
		LEAVE_UNTOUCHED,
		/**
		 * Map values to black.
		 */
		MAP_TO_BLACK,
		/**
		 * Map values to range minimum and maximum colors.
		 */
		MAP_TO_MINMAX
	}
	
	/**
	 * Input image.
	 */
	@Parameter( label= "Input Image", required = true, dataIOOrder = 1, 
		direction = Parameter.Direction.IN, description = "Input image.")
	protected MTBImage inputImg = null;

	/**
	 * Minimum value of mapping range, will be mapped to first color.
	 */
	@Parameter( label= "Range Minimum", required = true, dataIOOrder = 2, 
		direction = Parameter.Direction.IN, description = "Minimum value to map.")
	protected double rangeMin = 0;	
	
	/**
	 * Color for minimal value.
	 */
	@Parameter( label= "Color of Range Minimum", required = true, dataIOOrder = 3, 
		direction = Parameter.Direction.IN, description = "Color of small values.")
	protected Color minColor = Color.RED;	

	/**
	 * Maximum value of mapping range, will be mapped to second color.
	 */
	@Parameter( label= "Range Maximum", required = true, dataIOOrder = 4, 
		direction = Parameter.Direction.IN, description = "Maximum value to map.")
	protected double rangeMax = Double.MAX_VALUE;	
	
	/**
	 * Color for maximal value.
	 */
	@Parameter( label= "Color of Range Maximum", required = true, dataIOOrder = 5, 
		direction = Parameter.Direction.IN, description = "Color of large values.")
	protected Color maxColor = Color.YELLOW;	

	/**
	 * Mode how to handle values out of range and masked pixels.
	 */
	@Parameter( label= "How to handle values out of range?", required = true, 
		dataIOOrder = 6, direction = Parameter.Direction.IN, 
		description = "Mode how values out of range are treated.")
	protected OutOfRangeValuesHandlingMode outOfRangeValueMode = 
		OutOfRangeValuesHandlingMode.MAP_TO_MINMAX;		
	
	/**
	 * Optional binary mask of additional pixels to ignore.
	 * <p>
	 * Pixels marked white are ignored, i.e. mapped to black or left untouched.
	 */
	@Parameter( label= "Ignore mask?", required = false, 
		dataIOOrder = 1, direction = Parameter.Direction.IN, 
		description = "Ignore mask, pixels with values > 0 are ignored.")
	protected MTBImageByte ignoreMask = null;
	
	/**
	 * Generated result image.
	 */
	@Parameter( label= "Result Image", dataIOOrder = 0,
		direction = Parameter.Direction.OUT, description = "Resulting RGB image.")
	private transient MTBImageRGB resultImg = null;

	/**
	 * Default constructor.
	 *  @throws ALDOperatorException
	 */
	public GrayscaleImageToHeatmap() throws ALDOperatorException {
		// nothing to do here
	}		

	/**
	 * Returns the result color heat map.
	 */
	public MTBImageRGB getHeatmapImage() {
		return this.resultImg;
	}
	
	/**
	 * Set input image.
	 * @param inimg	Input grayscale image.
	 */
	public void setInputImage(MTBImage inimg) {
		this.inputImg = inimg;
	}

	/**
	 * Specify minimum of heat map range.
	 * @param minval	Range minimum.
	 */
	public void setRangeMinimum(double minval) {
		this.rangeMin = minval;
	}

	/**
	 * Color for range minimum.
	 * @param c		Color to use.
	 */
	public void setColorRangeMinimum(Color c) {
		this.minColor = c;
	}

	/**
	 * Specify maximum of heat map range.
	 * @param maxval	Range maximum.
	 */
	public void setRangeMaximum(double maxval) {
		this.rangeMax = maxval;
	}

	/**
	 * Color for range maximum.
	 * @param c		Color to use.
	 */
	public void setColorRangeMaximum(Color c) {
		this.maxColor = c;
	}
	
	/**
	 * Specify how to handle values out of range.
	 * @param mode	Handling mode.
	 */
	public void setOutOfRangeValueHandlingMode(OutOfRangeValuesHandlingMode m) {
		this.outOfRangeValueMode = m;
	}
	
	/**
	 * Specfiy additional mask of pixels to ignore.
	 * @param bImg	Binary mask.
	 */
	public void setIgnoreMask(MTBImageByte bImg) {
		this.ignoreMask = bImg;
	}

	@Override
  public void validateCustom() throws ALDOperatorException {
		if (this.inputImg instanceof MTBImageRGB)
			throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED, 
				classID + " Input image is already an RGB image, exiting!");
		if (this.rangeMin >= this.rangeMax)
			throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED, 
				classID + " Range is empty or rangeMin >= rangeMax!");
	}
	
	/**
	 * This method does the actual work.
	 * @throws ALDOperatorException 
	 * @throws ALDProcessingDAGException 
	 */
  @Override
	@SuppressWarnings("unused")
	protected void operate() 
			throws ALDOperatorException, ALDProcessingDAGException {
		
		int height = this.inputImg.getSizeY();
		int width = this.inputImg.getSizeX();
		int depth = this.inputImg.getSizeZ();
		int times = this.inputImg.getSizeT();
		int channels = this.inputImg.getSizeC();
		
		// allocate result image
		this.resultImg = (MTBImageRGB)MTBImage.createMTBImage(
				width, height, depth, times, channels, MTBImageType.MTB_RGB);

		int minR = this.minColor.getRed();
		int minG = this.minColor.getGreen();
		int minB = this.minColor.getBlue();
		
		int maxR = this.maxColor.getRed();
		int maxG = this.maxColor.getGreen();
		int maxB = this.maxColor.getBlue();

		// fill result image
		int valueI, newR, newG, newB;
		double ratio, valueD;
		double range = this.rangeMax - this.rangeMin;
		for (int c = 0; c < channels; ++c) {
			for (int t = 0; t < times; ++t) {
				for (int z = 0; z < depth; ++z) {
					for (int y = 0; y < height; ++y) {
						for (int x = 0; x < width; ++x) {
							valueD = this.inputImg.getValueDouble(x, y, z, t, c); 
							valueI = this.inputImg.getValueInt(x, y, z, t, c); 
							if (   this.ignoreMask != null
									&& this.ignoreMask.getValueInt(x, y, z, t, c) > 0) {
								switch(this.outOfRangeValueMode)
								{
								case LEAVE_UNTOUCHED:
									newR = valueI;
									newG = valueI;
									newB = valueI;							
									break;
								case MAP_TO_MINMAX:	
								case MAP_TO_BLACK:
								default:
									newR = 0;
									newG = 0;
									newB = 0;
									break;
								}								
							}
							else if (valueD < this.rangeMin) {
								switch(this.outOfRangeValueMode)
								{
								case MAP_TO_BLACK:
									newR = 0;
									newG = 0;
									newB = 0;
									break;
								case LEAVE_UNTOUCHED:
									newR = valueI;
									newG = valueI;
									newB = valueI;							
									break;
								case MAP_TO_MINMAX:	
									newR = minR;
									newG = minG;
									newB = minB;
									break;
								default:	
									newR = minR;
									newG = minG;
									newB = minB;
									break;
								}
							}
							else if (valueD > this.rangeMax) {
								switch(this.outOfRangeValueMode)
								{
								case MAP_TO_BLACK:
									newR = 0;
									newG = 0;
									newB = 0;
									break;
								case LEAVE_UNTOUCHED:
									newR = valueI;
									newG = valueI;
									newB = valueI;							
									break;
								case MAP_TO_MINMAX:	
									newR = maxR;
									newG = maxG;
									newB = maxB;
									break;
								default:	
									newR = maxR;
									newG = maxG;
									newB = maxB;
									break;
								}
							}
							else {
								ratio = (valueD - this.rangeMin) / range;
								
								// interpolate new color
								newR = minR + (int)(ratio*(maxR - minR) + 0.5);
								newG = minG + (int)(ratio*(maxG - minG) + 0.5);
								newB = minB + (int)(ratio*(maxB - minB) + 0.5);
							}
							this.resultImg.putValueR(x, y, z, t, c, newR); 
							this.resultImg.putValueG(x, y, z, t, c, newG); 
							this.resultImg.putValueB(x, y, z, t, c, newB);
						}
					}
				}
			}
		}
		String colorString = "[" + minR + "," + minG + "," + minB + "] -> " 
				+ "[" + maxR + "," + maxG + "," + maxB + "]";
		this.resultImg.setTitle("Heatmap " + colorString + " of <"
				+ this.inputImg.getTitle() + ">");
	}	
}
