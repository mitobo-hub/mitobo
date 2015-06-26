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

package de.unihalle.informatik.MiToBo.color.visualization;

import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDProcessingDAGException;
import de.unihalle.informatik.Alida.operator.ALDOperator;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.MiToBo.color.conversion.HSIToRGBPixelConverter;
import de.unihalle.informatik.MiToBo.core.datatypes.images.*;
import de.unihalle.informatik.MiToBo.core.datatypes.images.MTBImage.MTBImageType;
import de.unihalle.informatik.MiToBo.core.operator.*;

/**
 * This operator visualizes a 2D array using a specified color mapping.
 * <p>
 * Negative and positive values are shown highly saturated in the corresponding
 * color for the negative and positive value ranges, respectively. Values close 
 * to zero appear almost white, i.e. least saturated.<br>
 * The default color map is a red-blue color map, i.e. negative values are 
 * shown in blue, positive ones in red.
 *
 * @author moeller
 */
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.ALL,
		level=Level.APPLICATION)
public class ArrayToColorMapImage extends MTBOperator {

	/**
	 *	Possible colors to colorize value ranges.
	 */
	public static enum RangeColor {
		RED,
		GREEN,
		BLUE,
		YELLOW,
		CYAN,
		MAGENTA
	}
	
	/**
	 * Input array to visualize.
	 * <p>
	 * If the array has h rows and w columns, the result image gets a size
	 * of w x h. 
	 */
	@Parameter( label= "Input Array", required = true, dataIOOrder = 1, 
		direction = Parameter.Direction.IN, description = "Input array.")
	protected double[][] inputArray = null;

	/**
	 * Color for positive values.
	 */
	@Parameter( label= "Positive range color", required = false, dataIOOrder = 1, 
		direction = Parameter.Direction.IN, description = "Positive color.")
	protected RangeColor colorPositive = RangeColor.RED;	
	
	/**
	 * Color for negative values.
	 */
	@Parameter( label= "Negative range color", required = false, dataIOOrder = 2, 
		direction = Parameter.Direction.IN, description = "Negative color.")
	protected RangeColor colorNegative = RangeColor.BLUE;	

	/**
	 * Optional target image for result.
	 * <p>
	 * If left unset a new image is generated.
	 */
	@Parameter( label= "Target image", required = false, dataIOOrder = 3, 
		direction = Parameter.Direction.IN, description = "Target image.")
	protected transient MTBImageRGB targetImage = null;	

	/**
	 * Generated result image.
	 */
	@Parameter( label= "Result Image", required = true,
		direction = Parameter.Direction.OUT, description = "Resulting RGB image.")
	private transient MTBImageRGB resultImg = null;

	/**
	 * Default constructor.
	 *  @throws ALDOperatorException
	 */
	public ArrayToColorMapImage() throws ALDOperatorException {
		// nothing to do here
	}		

	/**
	 * Constructor. 
	 * @param array		Input array.
	 * @throws ALDOperatorException
	 */
	public ArrayToColorMapImage(double[][] array) throws ALDOperatorException {
		this.inputArray = array;
	}

	/**
	 * Returns the result color image.
	 */
	public MTBImageRGB getResultImage() {
		return this.resultImg;
	}
	
	/**
	 * Set target image.
	 * @param tImage	Image where to plot the result.
	 */
	public void setTargetImage(MTBImageRGB tImage) {
		this.targetImage = tImage;
	}
	
	/**
	 * Color for positive values.
	 * @param c		Color to use.
	 */
	public void setPositiveColor(RangeColor c) {
		this.colorPositive = c;
	}

	/**
	 * Color for negative values.
	 * @param c		Color to use.
	 */
	public void setNegativeColor(RangeColor c) {
		this.colorNegative = c;
	}

	/**
	 * This method does the actual work.
	 * @throws ALDOperatorException 
	 * @throws ALDProcessingDAGException 
	 */
	@Override
	protected void operate() 
			throws ALDOperatorException, ALDProcessingDAGException {
		int height = this.inputArray.length;
		int width = this.inputArray[0].length;
		
		// allocate result image
		if (this.targetImage == null)
			this.resultImg = (MTBImageRGB)MTBImage.createMTBImage(
				width, height, 1, 1, 1, MTBImageType.MTB_RGB);
		else 
			this.resultImg = this.targetImage;

		// find minimal and maximal values in array	
		double maxVal = Double.MIN_VALUE;
		double minVal = Double.MAX_VALUE;
		for (int y = 0; y < height; ++y) {
			for (int x = 0; x < width; ++x) {
				if (this.inputArray[y][x] < minVal)  
					minVal = this.inputArray[y][x];
				if (this.inputArray[y][x] > maxVal) {
					maxVal = this.inputArray[y][x];
				}
			}
		}

		double range = (Math.abs(maxVal) > Math.abs(minVal)) ? 
			Math.abs(maxVal):Math.abs(minVal);

		// disable history logging temporarily
		int oldHistoryConstructionMode = ALDOperator.getConstructionMode();
		ALDOperator.setConstructionMode(1);
			
		// convert array entries first to HSI, then to RGB color values
		double h, s = 1.0, i = 1.0;
		double[] hsi = new double[3];
		hsi[1] = s;
		hsi[2] = i;
		HSIToRGBPixelConverter conv = new HSIToRGBPixelConverter();
		double desaturationFrac;
		for (int y = 0; y < height; ++y) {
			for (int x = 0; x < width; ++x) {
				if (this.inputArray[y][x] > 0) {
					h = convertRangeColorToHue(this.colorPositive);
					desaturationFrac = 1 - this.inputArray[y][x]/range;
				}
				else {
					h = convertRangeColorToHue(this.colorNegative);
					desaturationFrac = 1 - Math.abs(this.inputArray[y][x])/range;
				}
				s = 1.0 - 3.0/(1.0 + 2.0*desaturationFrac) * desaturationFrac;
				i = 1.0/3.0 * (1.0 + 2.0*desaturationFrac);
				hsi[0] = h;
				hsi[1] = s;
				hsi[2] = i;
				conv.setHSIInput(hsi);
				conv.runOp(HidingMode.HIDDEN);
				double RGB[] = conv.getResultRGB();
				this.resultImg.putValueR(x, y, (int)(RGB[0]*255.0 + 0.5)); 
				this.resultImg.putValueG(x, y, (int)(RGB[1]*255.0 + 0.5)); 
				this.resultImg.putValueB(x, y, (int)(RGB[2]*255.0 + 0.5)); 
			}
		}
		// ensable history logging again
		ALDOperator.setConstructionMode(oldHistoryConstructionMode);
	}
	
	/**
	 * Converts given color to hue value.
	 * @param c		Color identifier.
	 * @return	Hue value.
	 */
	protected static double convertRangeColorToHue(RangeColor c) {
		switch(c)
		{
		case RED: return 0;
		case BLUE: return 2.0/3.0;
		case GREEN: return 1.0/3.0;
		case CYAN: return 0.5;
		case MAGENTA: return 0.5*5.0/3.0;
		case YELLOW: return 0.5/3.0;
		default: return 0;
		}
	}
	
//	public static void main(String [] args) {
//		double[][] testArray = new double[100][100];
//		for (int y=0;y<100;++y) {
//			for (int x=0;x<100;++x) {
//				testArray[y][x] = x - 50 + y - 50;
//			}
//		}
//		try {
//			ArrayToColorMapImage arrVis = new ArrayToColorMapImage(testArray);
//			arrVis.runOp();
//			arrVis.getResultImage().show();
//			arrVis.setPositiveColor(RangeColor.MAGENTA);
//			arrVis.setNegativeColor(RangeColor.YELLOW);
//			arrVis.runOp();
//			arrVis.getResultImage().show();
//			arrVis.setPositiveColor(RangeColor.RED);
//			arrVis.setNegativeColor(RangeColor.GREEN);
//			arrVis.runOp();
//			arrVis.getResultImage().show();
//		} catch (Exception ex) {}
//	}
}
