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

package de.unihalle.informatik.MiToBo.color.conversion;

import de.unihalle.informatik.Alida.exceptions.ALDOperatorException;
import de.unihalle.informatik.Alida.exceptions.ALDOperatorException.OperatorExceptionType;
import de.unihalle.informatik.Alida.annotations.ALDAOperator;
import de.unihalle.informatik.Alida.annotations.ALDAOperator.Level;
import de.unihalle.informatik.Alida.annotations.Parameter;
import de.unihalle.informatik.MiToBo.core.datatypes.defines.MTBConstants;
import de.unihalle.informatik.MiToBo.core.operator.*;

/**
 * Operator to convert a single HSI value to RGB.
 * <p>
 * In comparison to the HSV color space here in the HSI space hue, saturation 
 * and intensity are not completely decoupled, i.e. changes in one of the 
 * components may also influence others.<br>
 * This operator expects all three value H, S and I to be normalized to a 
 * range of [0,1]. The returned values for R, G and B are also each normalized 
 * to a range of [0,1]. The implementation is based on
 * <p>
 * Gonzalez/Woods, "Digital Image Processing", pp. 235, Addison-Wesley, 1992
 * <p>
 * Note that if I = 0 the RGB color black is returned, and if I = 1 white.
 * If S = 0 an RGB gray value corresponding to the given intensity is returned.
 * And if the calculation results in RGB values larger than 1, i.e. in values
 * out of range, the values are clipped to a maximum of 1.0.
 * 
 * @author moeller
 */
@ALDAOperator(genericExecutionMode=ALDAOperator.ExecutionMode.ALL,
		level=Level.APPLICATION)
public class HSIToRGBPixelConverter extends MTBOperator {

	/**
	 * Input HSI values.
	 */
	@Parameter( label= "HSI Input", required = true, dataIOOrder = -1, 
		direction = Parameter.Direction.IN, description = "HSI input values.")
	protected double[] hsiInput = null;

	/**
	 * Output RGB values.
	 */
	@Parameter( label= "RGB Output", dataIOOrder = -1,
		direction = Parameter.Direction.OUT, description = "RGB output values.")
	protected double[] rgbOutput = null;

	/**
	 * Default constructor.
	 * @throws ALDOperatorException	Thrown in case of failure.
	 */
	public HSIToRGBPixelConverter() throws ALDOperatorException {
		// nothing to do here
	}		

	/**
	 * Constructor. 
	 * @param hsi 	HSI value to convert.
	 * @throws ALDOperatorException	Thrown in case of failure.
	 */
	public HSIToRGBPixelConverter(double[] hsi) throws ALDOperatorException {
		this.hsiInput = hsi;
	}

	@Override
	public void validateCustom() throws ALDOperatorException {
		if (this.hsiInput[0] < 0 || this.hsiInput[0] > 1) 
			throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED,
				"[HSIToRGBPixelConverter] hue input value out of range [0,1]!");
		if (this.hsiInput[1] < 0 || this.hsiInput[1] > 1) 
			throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED,
				"[HSIToRGBPixelConverter] hue input value out of range [0,1]!");
		if (this.hsiInput[2] < 0 || this.hsiInput[2] > 1) 
			throw new ALDOperatorException(OperatorExceptionType.VALIDATION_FAILED,
				"[HSIToRGBPixelConverter] hue input value out of range [0,1]!");
	}
	
	/**
	 * Specify HSI value to be converted.
	 * @param hsiVal		HSI value.
	 */
	public void setHSIInput(double[] hsiVal) {
		this.hsiInput = hsiVal;
	}
	
	/**
	 * Returns the result RGB color.
	 * @return Result array of RGB values.
	 */
	public double[] getResultRGB() {
		return this.rgbOutput;
	}

	/**
	 * This method does the actual work.
	 */
	@Override
	protected void operate() {
		double h = this.hsiInput[0];
		double s = this.hsiInput[1];
		double i = this.hsiInput[2];
		double r, g, b;
		
		// if intensity is zero, color is black
		if (i <= MTBConstants.epsilon) {
			this.rgbOutput = new double[]{0.0,0.0,0.0};
			return;
		}
		
		// if intensity is 1, color is white 
		if (i == 1.0) {
			this.rgbOutput = new double[]{1.0,1.0,1.0};
			return;
		}

		// if saturation is zero, color is gray
		if (s <= MTBConstants.epsilon) {
			this.rgbOutput = new double[]{i,i,i};
			return;
		}

		// convert hue from [0,1] to range of [0,2*PI]
		double H = 2 * Math.PI * h;
		
		// Hue between 0 and 120 degrees
		if (0 <= H && H < 2.0*Math.PI/3.0) {
			r = 1.0/3.0 * (1.0 +  s * Math.cos(H)	/ Math.cos(Math.PI/3.0-H) );
			b = 1.0/3.0 * (1.0 - s);
			g = 1.0 - (r + b); 
		}
		// Hue between 120 and 240 degrees
		else if (2.0*Math.PI/3.0 <= H && H < 2.0*2.0*Math.PI/3.0) {
			H = H - 2.0*Math.PI/3.0;
			g = 1.0/3.0 * (1.0 +  (s * Math.cos(H)) / Math.cos(Math.PI/3.0-H) );
			r = 1.0/3.0 * (1.0 - s);
			b = 1.0 - (r + g); 			
		}
		// Hue between 240 and 360 degrees
		else { 
			H = H - 4.0*Math.PI/3.0;
			b = 1.0/3.0 * (1.0 +  (s * Math.cos(H))	/ Math.cos(Math.PI/3.0-H));
			g = 1.0/3.0 * (1.0 - s);
			r = 1.0 - (g + b); 
		} 
		// normalize output
		r = 3.0 * i * r;
		g = 3.0 * i * g;
		b = 3.0 * i * b;
		
		// check for color fractions out of range, clip back to valid values
		// (according to 
		//    http://fourier.eng.hmc.edu/e161/lectures/ColorProcessing/node3.html)
		if (r > 1)
			r = 1;
		if (g > 1)
			g = 1;
		if (b > 1)
			b = 1;
		
		// fill output array
		this.rgbOutput = new double[]{r, g, b};
	}
}
